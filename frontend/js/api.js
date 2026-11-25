// Capa de acceso a la API.
//
// Se encarga de :
//  - guardar, leer y borrar el JWT de localStorage,
//  - enviar la cabecera Authorization: Bearer en las rutas protegidas,
//  - convertir las respuestas de error de la API en un Error con mensaje.

const sesion = {
  token: () => localStorage.getItem(CLAVE_TOKEN),
  guardar: (token) => localStorage.setItem(CLAVE_TOKEN, token),
  cerrar: () => localStorage.removeItem(CLAVE_TOKEN),
};

// El backend devuelve {"estado": 403, "mensaje": "..."} o, en los errores de
// validación, un mapa campo -> mensaje. Esta función entiende ambos formatos.
function mensajeDeError(datos) {
  if (!datos) return "";
  if (typeof datos.mensaje === "string") return datos.mensaje;
  if (typeof datos.error === "string") return datos.error;
  if (typeof datos === "object") {
    return Object.values(datos).filter((valor) => typeof valor === "string").join(". ");
  }
  return "";
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

    let detalle = "";
    if (respuesta.headers.get("content-type")?.includes("application/json")) {
      try {
        detalle = mensajeDeError(await respuesta.json());
      } catch (e) {
        detalle = "";
      }
    }

    const error = new Error(detalle || `Error ${respuesta.status}`);
    error.estado = respuesta.status;
    throw error;
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
