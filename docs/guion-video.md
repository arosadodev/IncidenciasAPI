# Guion del vídeo (YouTube en modo oculto)

Duración objetivo: 8–10 minutos. Todo lo que se muestra está verificado con
`docs/comprobaciones.sh` y con la colección de Postman del directorio
`postman/`.

## Antes de grabar

1. Arranca el backend: `./mvnw spring-boot:run`.
2. Arranca el frontend: `python3 -m http.server 5500 --directory frontend`
   y abre <http://localhost:5500>.
3. Ten abierto Postman con la colección `postman/IncidenciasAPI.postman_collection.json`
   y ejecuta primero la carpeta **01 - Auth** (guarda los tokens en variables).
4. Abre en el IDE estos archivos para poder enseñarlos:
   `config/SecurityConfig.java`, `service/IncidenciaService.java`,
   `service/JwtService.java`.

## 0:00 · Presentación (30 s)

- API REST de incidencias del centro + frontend en HTML/JS.
- Stack: Spring Boot 4.1.1, Java 21, Maven, Spring Data JPA, MySQL, Spring
  Security con JWT (jjwt + oauth2-resource-server), BCrypt.
- Estructura de paquetes: `controller`, `service`, `repository`, `entity`,
  `DTO`, `config`, `exception`.

## 0:30 · Arranque y base de datos (40 s)

- Al arrancar, la aplicación crea los usuarios de prueba: `admin@centro.es`,
  `tecnico@centro.es`, `ana@centro.es` y `luis@centro.es`, todos con contraseña
  `1234` (cifrada con BCrypt en el código).

- Enseña `application.properties`: URL `jdbc:mysql://localhost:3307/incidencias`
  con `createDatabaseIfNotExist=true` y `ddl-auto=update`; no hay que crear la
  base de datos ni las tablas a mano.
- Arranca con `./mvnw spring-boot:run` y enseña en la consola que Hibernate crea
  las tablas `usuarios` e `incidencias`.

## 1:10 · Registro y login en el frontend (60 s)

- Inicia sesión con `ana@centro.es` / `1234` y, si quieres, registra un usuario
  nuevo (`marta@centro.es`) desde la interfaz.
- Fíjate en que la respuesta trae `"rol":"ROLE_USER"` aunque se envíe otro rol:
  el registro no acepta roles privilegiados.
- Inicia sesión, abre las herramientas del navegador → Application →
  Local storage → `incidencias_token`, y pega el token en jwt.io para enseñar
  `sub`, `id`, `roles`, `nombre`, `exp` e `iss`.

## 2:10 · Crear una incidencia (50 s)

- Crea "Aire acondicionado averiado" desde la interfaz.
- Enseña en Postman `POST /incidencias` enviando un `autor` falso en el cuerpo:
  la respuesta devuelve como autor a la persona del token.
- Código: `IncidenciaService.crear(...)` → `usuarioRepository.findByEmailIgnoreCase(autenticacion.getName())`.

## 3:00 · Reto: no puedo ver incidencias ajenas (60 s)

- Con el token de Ana, `GET /incidencias` → solo aparecen las de Ana.
- Con el token de Ana, `GET /incidencias/{id_de_Luis}` → **403**.
- Cambia el número de la URL varias veces: siempre 403 para incidencias ajenas.
- Código: `IncidenciaService.exigirAcceso(...)` (compara
  `incidencia.getAutor().getEmail()` con el email del token) y
  `IncidenciaRepository.findByAutorEmailIgnoreCase...` para el listado.
- Con el token del técnico, la misma URL → 200.

## 4:10 · Pregunta 1: diferencia entre 401 y 403 (90 s)

Provoca las dos respuestas en Postman y enseña el código.

1. **401** — sin token o con token inválido/caducado:
   - `GET /incidencias` sin cabecera `Authorization` → 401 (sin cuerpo, con
     cabecera `WWW-Authenticate`).
   - `GET /incidencias` con un token firmado con otra clave → 401.
   - Código: `JwtService`/`SecurityConfig.jwtDecoder()` valida firma, emisor y
     caducidad. Cuando falla, la petición no llega al controlador: la corta el
     `BearerTokenAuthenticationEntryPoint` que configura
     `oauth2ResourceServer(...)` en `SecurityConfig`.
2. **403** — autenticado pero sin permiso:
   - Con el token de Ana, `DELETE /incidencias/{id}` → 403.
   - Con el token de Ana, `GET /usuarios` → 403.
   - Código: la regla está en `SecurityConfig`
     (`.requestMatchers(...).hasRole("ADMIN")`); responde el
     `AccessDeniedHandler` por defecto de Spring Security.
   - El otro 403 (incidencia ajena) lo lanza `IncidenciaService.exigirAcceso(...)`
     con `AccessDeniedException`, que Spring Security también convierte en 403.

Frase para el vídeo: "el 401 dice *no sé quién eres*, el 403 dice *sé quién
eres, pero no puedes hacer esto*".

## 5:40 · Administración (60 s)

- Entra como `admin@centro.es`.
- `GET /usuarios` → 200; con el token de Ana → 403.
- `PATCH /usuarios/{id}/rol` con `{"rol":"ROLE_TECNICO"}` → 200.
- `PATCH /usuarios/{id}/rol` con `{"rol":"ROLE_DIOS"}` → 400 (lista blanca en
  `UsuarioController.ROLES_ADMITIDOS`).
- `DELETE /incidencias/{id}` con admin → 204; con el autor (ROLE_USER) → 403.
- Enseña en el frontend cómo aparece el panel de usuarios solo al admin.

## 6:40 · Pregunta 2: autor al crear (30 s)

- Enseña `IncidenciaRequest` (solo `titulo` y `descripcion`).
- Enseña `IncidenciaService.crear(...)`: el autor sale de
  `Authentication.getName()` (claim `sub` del JWT), no del cuerpo.
- En Postman, manda `"autor":{"id":2}` en el `POST` y comprueba que se ignora.

## 7:10 · Pregunta 3: cambio de rol con token válido (120 s)

- Demuéstralo: inicia sesión como `tecnico@centro.es` (era ROLE_USER), guarda el
  token, haz que el admin le asigne ROLE_TECNICO y usa **el token viejo** para
  `PATCH /incidencias/{id}/estado` → 403. Con un token nuevo → 200.
- Explicación: el JWT es autocontenido; el servidor no consulta la base de
  datos en cada petición para saber el rol, así que hasta que el token caduca
  (o se vuelve a iniciar sesión) se mantienen los permisos antiguos.
- Soluciones (ventajas e inconvenientes) — resumen del README:
  1. Tokens de corta duración + refresh token con rotación.
  2. Cargar el rol de la base de datos en cada petición (filtro propio).
  3. Versión de permisos en el token (`tokenVersion`) comparada con la BD.
  4. Lista de revocación por `jti` (blacklist) con TTL.
  5. Sesiones en servidor (stateful).
- Propuesta: `tokenVersion` + tokens de acceso cortos + refresh, y responder el
  resto del vídeo con esa decisión.

## 8:30 · Cierre (20 s)

- Repositorio, README, AGENTS.md y `docs/comprobaciones.sh` (30 comprobaciones
  automáticas).
- Recuerda: las restricciones del frontend son solo comodidad; la seguridad
  está en el backend.
