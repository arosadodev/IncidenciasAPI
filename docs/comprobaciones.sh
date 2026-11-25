#!/usr/bin/env bash
#
# Comprobaciones de la API de incidencias usando curl y jq.
#
# Uso:
#   1. Arranca el backend (./mvnw spring-boot:run) con la base de datos vacía.
#   2. Ejecuta:  ./docs/comprobaciones.sh
#
# El script registra los usuarios de prueba, asigna el primer ROLE_ADMIN
# directamente en la base de datos (como pide el enunciado) y comprueba todos
# los permisos de la tabla del enunciado.
#
# Variables de entorno opcionales:
#   API=http://localhost:8080        URL del backend
#   MYSQL_HOST=127.0.0.1 MYSQL_PORT=3307 MYSQL_USER=root MYSQL_PASSWORD=1234
#   MYSQL_DB=incidencias             Datos de conexión para promover el primer admin
#   JWT_SECRET=...                   Clave de firma (debe coincidir con la del backend)
#   SIN_MYSQL=1                      Si no puedes usar el cliente mysql, crea el admin a mano
#
set -uo pipefail

API="${API:-http://localhost:8080}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-1234}"
MYSQL_DB="${MYSQL_DB:-incidencias}"
JWT_SECRET="${JWT_SECRET:-clave-de-desarrollo-incidencias-cambiame-en-produccion-2026}"
PASSWORD="${PASSWORD:-password123}"

ANA="ana@centro.es"
LUIS="luis@centro.es"
TECNICO="tecnico@centro.es"
ADMIN="admin@centro.es"

correctas=0
fallidas=0

comprobar() { # descripcion esperado obtenido
    if [[ "$2" == "$3" ]]; then
        echo "  OK    $1 -> HTTP $3"
        correctas=$((correctas + 1))
    else
        echo "  FALLO $1 -> esperado HTTP $2, obtenido HTTP $3"
        fallidas=$((fallidas + 1))
    fi
}

http() { # metodo ruta [token] [cuerpo]  -> deja el cuerpo en LAST_BODY y el código en LAST_CODE
    local metodo=$1 ruta=$2 token=${3:-} cuerpo=${4:-}
    local args=(-s -w $'\n%{http_code}' -X "$metodo" "$API$ruta")
    [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
    [[ -n "$cuerpo" ]] && args+=(-H 'Content-Type: application/json' -d "$cuerpo")
    local salida
    salida=$(curl "${args[@]}")
    LAST_CODE=${salida##*$'\n'}
    LAST_BODY=${salida%$'\n'*}
}

registrar() { # email nombre
    http POST /auth/register "" "{\"email\":\"$1\",\"nombre\":\"$2\",\"password\":\"$PASSWORD\",\"rol\":\"ROLE_ADMIN\"}"
    if [[ "$LAST_CODE" == "201" ]]; then
        echo "  creado $1 ($LAST_BODY)"
    else
        echo "  $1 ya existía o no se pudo crear -> HTTP $LAST_CODE"
    fi
}

login() { # email -> token
    http POST /auth/login "" "{\"email\":\"$1\",\"password\":\"$PASSWORD\"}"
    if [[ "$LAST_CODE" != "200" ]]; then
        echo "No se pudo hacer login con $1: $LAST_BODY" >&2
        exit 1
    fi
    echo "$LAST_BODY" | jq -r '.token'
}

firmar_token() { # email secreto caducado(0|1) -> JWT firmado a mano para probar casos inválidos
    python3 - "$1" "$2" "$3" <<'PY'
import base64, hashlib, hmac, json, sys, time

email, secreto, caducado = sys.argv[1], sys.argv[2].encode(), sys.argv[3] == "1"

def b64(datos):
    return base64.urlsafe_b64encode(datos).rstrip(b"=").decode()

ahora = int(time.time())
cabecera = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
cuerpo = b64(json.dumps({
    "sub": email, "iss": "App-Incidencias", "nombre": "Prueba",
    "iat": ahora - (7200 if caducado else 0),
    "exp": ahora - (3600 if caducado else 3600),
    "id": 1, "roles": ["ROLE_USER"],
}, separators=(",", ":")).encode())
firma = b64(hmac.new(secreto, f"{cabecera}.{cuerpo}".encode(), hashlib.sha256).digest())
print(f"{cabecera}.{cuerpo}.{firma}")
PY
}

roles_del_token() { # token -> "ROLE_USER" (lee el claim roles sin verificar la firma)
    python3 - "$1" <<'PY'
import base64, json, sys

cuerpo = sys.argv[1].split(".")[1]
cuerpo += "=" * (-len(cuerpo) % 4)
print(",".join(json.loads(base64.urlsafe_b64decode(cuerpo)).get("roles", [])))
PY
}

echo "== Preparando usuarios en $API =="
registrar "$ANA" "Ana"
registrar "$LUIS" "Luis"
registrar "$TECNICO" "Tecnico"
registrar "$ADMIN" "Admin"

if [[ "${SIN_MYSQL:-0}" != "1" ]]; then
    echo "== Asignando el primer ROLE_ADMIN directamente en la base de datos =="
    mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" \
        -e "UPDATE usuarios SET rol='ROLE_ADMIN' WHERE email='$ADMIN';
            UPDATE usuarios SET rol='ROLE_USER' WHERE email IN ('$ANA', '$LUIS', '$TECNICO');" 2>/dev/null \
        && echo "  admin@centro.es es ahora ROLE_ADMIN y el resto son ROLE_USER" \
        || echo "  AVISO: no se pudo conectar a MySQL; promueve el admin a mano (ver db/roles.sql)"
fi

TOK_ANA=$(login "$ANA")
TOK_LUIS=$(login "$LUIS")
TOK_TECNICO_VIEJO=$(login "$TECNICO")
TOK_ADMIN=$(login "$ADMIN")

echo "== 1. Sin token no se accede a rutas protegidas =="
http GET /incidencias
comprobar "GET /incidencias sin token" 401 "$LAST_CODE"

http GET /incidencias/1
comprobar "GET /incidencias/1 sin token" 401 "$LAST_CODE"

http GET /usuarios
comprobar "GET /usuarios sin token" 401 "$LAST_CODE"

echo "== 2. Token inválido o caducado =="
http GET /incidencias "esto.no.es.un.jwt"
comprobar "token con formato inválido" 401 "$LAST_CODE"

http GET /incidencias "$(firmar_token "$ANA" "$JWT_SECRET" 1)"
comprobar "token caducado" 401 "$LAST_CODE"

http GET /incidencias "$(firmar_token "$ANA" "otra-clave-que-no-es-la-de-la-aplicacion-2026" 0)"
comprobar "token firmado con otra clave" 401 "$LAST_CODE"

echo "== 3. Cada usuario crea sus incidencias (el autor sale del token) =="
http POST /incidencias "$TOK_ANA" '{"titulo":"Avería en el aula 12","descripcion":"El proyector no enciende","estado":"RESUELTA","autor":{"id":2}}'
comprobar "Ana crea incidencia" 201 "$LAST_CODE"
ID_ANA=$(echo "$LAST_BODY" | jq -r '.id')
AUTOR_ANA=$(echo "$LAST_BODY" | jq -r '.autor.email')
ESTADO_ANA=$(echo "$LAST_BODY" | jq -r '.estado')
[[ "$AUTOR_ANA" == "$ANA" && "$ESTADO_ANA" == "ABIERTA" ]] \
    && { echo "  OK    autor=$AUTOR_ANA y estado=$ESTADO_ANA (se ignoraron autor y estado del cliente)"; correctas=$((correctas + 1)); } \
    || { echo "  FALLO autor=$AUTOR_ANA estado=$ESTADO_ANA"; fallidas=$((fallidas + 1)); }

http POST /incidencias "$TOK_LUIS" '{"titulo":"Ordenador roto","descripcion":"El equipo 5 del aula 3 no arranca"}'
comprobar "Luis crea incidencia" 201 "$LAST_CODE"
ID_LUIS=$(echo "$LAST_BODY" | jq -r '.id')

echo "== 4. Ana solo ve las suyas y no puede abrir las de Luis cambiando la URL =="
http GET /incidencias "$TOK_ANA"
comprobar "Ana consulta su listado" 200 "$LAST_CODE"
AJENAS=$(echo "$LAST_BODY" | jq '[.[] | select(.autor.email != "ana@centro.es")] | length')
[[ "$AJENAS" == "0" ]] \
    && { echo "  OK    el listado de Ana no contiene incidencias ajenas ($(echo "$LAST_BODY" | jq 'length') incidencias)"; correctas=$((correctas + 1)); } \
    || { echo "  FALLO el listado de Ana contiene $AJENAS incidencias ajenas"; fallidas=$((fallidas + 1)); }

http GET "/incidencias/$ID_ANA" "$TOK_ANA"
comprobar "Ana consulta su propia incidencia" 200 "$LAST_CODE"

http GET "/incidencias/$ID_LUIS" "$TOK_ANA"
comprobar "Ana consulta la incidencia de Luis cambiando la URL" 403 "$LAST_CODE"

echo "== 5. El admin consulta cualquier incidencia =="
http GET "/incidencias/$ID_LUIS" "$TOK_ADMIN"
comprobar "admin consulta la incidencia de Luis" 200 "$LAST_CODE"
http GET /incidencias "$TOK_ADMIN"
comprobar "admin consulta el listado completo" 200 "$LAST_CODE"

echo "== 6. Cambiar el estado =="
http PATCH "/incidencias/$ID_ANA/estado" "$TOK_ANA" '{"estado":"EN_PROCESO"}'
comprobar "Ana (ROLE_USER) intenta cambiar el estado" 403 "$LAST_CODE"

echo "== 7. El admin convierte a $TECNICO en ROLE_TECNICO =="
http GET /usuarios "$TOK_ANA"
comprobar "Ana intenta consultar usuarios" 403 "$LAST_CODE"

http PATCH "/usuarios/3/rol" "$TOK_ANA" '{"rol":"ROLE_TECNICO"}'
comprobar "Ana intenta cambiarse el rol a sí misma" 403 "$LAST_CODE"

http GET /usuarios "$TOK_ADMIN"
comprobar "admin consulta usuarios" 200 "$LAST_CODE"

http PATCH "/usuarios/3/rol" "$TOK_ADMIN" '{"rol":"ROLE_PODEROSO"}'
comprobar "admin envía un rol no admitido" 400 "$LAST_CODE"

http GET /usuarios "$TOK_ADMIN"
ID_TECNICO=$(echo "$LAST_BODY" | jq -r --arg e "$TECNICO" '.[] | select(.email == $e) | .id')

http PATCH "/usuarios/$ID_TECNICO/rol" "$TOK_ADMIN" '{"rol":"ROLE_TECNICO"}'
comprobar "admin asigna ROLE_TECNICO" 200 "$LAST_CODE"

TOK_TECNICO=$(login "$TECNICO")

echo "== 8. Un token antiguo mantiene el rol con el que se emitió =="
if [[ "$(roles_del_token "$TOK_TECNICO_VIEJO")" == "ROLE_USER" ]]; then
    http PATCH "/incidencias/$ID_ANA/estado" "$TOK_TECNICO_VIEJO" '{"estado":"EN_PROCESO"}'
    comprobar "token emitido antes del cambio de rol (roles del token: ROLE_USER)" 403 "$LAST_CODE"
else
    echo "  (se omite: el token antiguo ya tenía otro rol al arrancar; reinicia con BD limpia para verlo)"
fi

http GET "/incidencias/$ID_ANA" "$TOK_TECNICO"
comprobar "técnico consulta una incidencia de Ana" 200 "$LAST_CODE"

http GET /incidencias "$TOK_TECNICO"
AUTORES=$(echo "$LAST_BODY" | jq -r '[.[] | .autor.email] | unique | join(", ")')
[[ "$AUTORES" == *"$ANA"* && "$AUTORES" == *"$LUIS"* ]] \
    && { echo "  OK    el técnico ve incidencias de varios autores: $AUTORES"; correctas=$((correctas + 1)); } \
    || { echo "  FALLO el técnico no ve incidencias de varios autores: $AUTORES"; fallidas=$((fallidas + 1)); }

http PATCH "/incidencias/$ID_ANA/estado" "$TOK_TECNICO" '{"estado":"EN_PROCESO"}'
comprobar "técnico con token nuevo cambia el estado" 200 "$LAST_CODE"
ESTADO_NUEVO=$(echo "$LAST_BODY" | jq -r '.estado')
[[ "$ESTADO_NUEVO" == "EN_PROCESO" ]] \
    && { echo "  OK    estado=$ESTADO_NUEVO"; correctas=$((correctas + 1)); } \
    || { echo "  FALLO estado=$ESTADO_NUEVO"; fallidas=$((fallidas + 1)); }

echo "== 9. Eliminar incidencias =="
http DELETE "/incidencias/$ID_LUIS" "$TOK_LUIS"
comprobar "autor (ROLE_USER) intenta eliminar su incidencia" 403 "$LAST_CODE"

http DELETE "/incidencias/$ID_LUIS" "$TOK_TECNICO"
comprobar "técnico intenta eliminar una incidencia" 403 "$LAST_CODE"

http DELETE "/incidencias/$ID_LUIS" "$TOK_ADMIN"
comprobar "admin elimina la incidencia" 204 "$LAST_CODE"

http GET "/incidencias/$ID_LUIS" "$TOK_ADMIN"
comprobar "la incidencia eliminada ya no existe" 404 "$LAST_CODE"

echo
echo "Resultado: $correctas comprobaciones correctas, $fallidas fallidas"
[[ "$fallidas" -eq 0 ]]
