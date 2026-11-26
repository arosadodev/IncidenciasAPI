// Capa de acceso a la API.
//
// Se encarga de:
//  - guardar, leer y borrar el JWT de localStorage,
//  - enviar la cabecera Authorization: Bearer en las rutas protegidas,
//  - convertir las respuestas de error de la API en un Error con mensaje.

const sesion = {
  token: () => localStorage.getItem(CLAVE_TOKEN),
  guardar: (token) => localStorage.setItem(CLAVE_TOKEN, token),
  cerrar: () => localStorage.removeItem(CLAVE_TOKEN),
};

// Mensajes para cuando la API responde sin cuerpo (401 y 403 de Spring Security
// llegan vacíos) o con el texto genérico de Spring Boot.
const MENSAJES_POR_ESTADO = {
  400: "La petición no es válida",
  401: "No autenticado: inicia sesión",
  403: "No tienes permisos para esta operación",
  404: "No existe el recurso solicitado",
  409: "El recurso ya existe",
  500: "Error interno del servidor",
};

const MENSAJES_GENERICOS = new Set([
  "Bad Request",
  "Unauthorized",
  "Forbidden",
  "Not Found",
  "Conflict",
  "Internal Server Error",
]);

// La API devuelve {"error": "..."}, un mapa campo -> mensaje (errores de
// validación) o nada (401/403 por defecto de Spring Security).
function mensajeDeError(datos, estado) {
  if (datos && typeof datos === "object") {
    if (typeof datos.mensaje === "string" && datos.mensaje) return datos.mensaje;

    const campos = Object.entries(datos).filter(
      ([clave, valor]) =>
        typeof valor === "string" && !["error", "timestamp", "status", "path"].includes(clave)
    );
    if (campos.length) {
      return campos.map(([campo, texto]) => `${campo}: ${texto}`).join(". ");
    }

    if (typeof datos.error === "string" && datos.error && !MENSAJES_GENERICOS.has(datos.error)) {
      return datos.error;
    }
  }

  return MENSAJES_POR_ESTADO[estado] ?? `Error ${estado}`;
}

async function peticion(ruta, opciones = {}, publica = false) {
  const token = publica ? null : sesion.token();

  if (!publica && !token) {
    window.dispatchEvent(new Event("sesion-caducada"));
    throw new Error("Inicia sesión para continuar");
  }

  const headers = new Headers(opciones.headers);
  if (opciones.body) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let respuesta;
  try {
    respuesta = await fetch(API_URL + ruta, { ...opciones, headers });
  } catch (e) {
    throw new Error("No se puede conectar con la API. ¿Está arrancado el backend?");
  }

  if (!respuesta.ok) {
    // 401: el token no es válido o ha caducado -> se cierra la sesión
    if (!publica && respuesta.status === 401 && sesion.token() === token) {
      sesion.cerrar();
      window.dispatchEvent(new Event("sesion-caducada"));
      throw new Error("La sesión ha caducado. Inicia sesión de nuevo");
    }

    let detalle = null;
    if (respuesta.headers.get("content-type")?.includes("application/json")) {
      try {
        detalle = await respuesta.json();
      } catch (e) {
        detalle = null;
      }
    }

    throw new Error(mensajeDeError(detalle, respuesta.status));
  }

  return respuesta.status === 204 ? null : respuesta.json();
}

const enviar = (metodo, ruta, cuerpo, publica = false) =>
  peticion(ruta, { method: metodo, body: JSON.stringify(cuerpo) }, publica);

const api = {
  auth: {
    login: (datos) => enviar("POST", "/auth/login", datos, true),
    registro: (datos) => enviar("POST", "/auth/register", datos, true),
  },
  incidencias: {
    // El backend devuelve solo las del usuario si su rol es ROLE_USER
    listar: () => peticion("/incidencias"),
    obtener: (id) => peticion(`/incidencias/${id}`),
    crear: (datos) => enviar("POST", "/incidencias", datos),
    cambiarEstado: (id, estado) => enviar("PATCH", `/incidencias/${id}/estado`, { estado }),
    eliminar: (id) => peticion(`/incidencias/${id}`, { method: "DELETE" }),
  },
  usuarios: {
    listar: () => peticion("/usuarios"),
    cambiarRol: (id, rol) => enviar("PATCH", `/usuarios/${id}/rol`, { rol }),
  },
};
