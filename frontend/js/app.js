// Lógica de la interfaz.
//
// Lee el rol y los datos del usuario del propio JWT (payload) para mostrar unas
// opciones u otras. Ojo: esto es solo comodidad visual; los permisos reales los
// comprueba el backend en cada petición.

const $ = (id) => document.getElementById(id);

const ESTADOS = ["ABIERTA", "EN_PROCESO", "RESUELTA"];

const ETIQUETA_ROL = {
  ROLE_USER: "Usuario",
  ROLE_TECNICO: "Técnico",
  ROLE_ADMIN: "Administrador",
};

let usuario = null; // {id, nombre, email, roles, exp}
let incidencias = [];
let incidenciaAbierta = null;

/* ---------------- Utilidades ---------------- */

function base64UrlADatos(parte) {
  const base64 = parte.replace(/-/g, "+").replace(/_/g, "/");
  const relleno = "=".repeat((4 - (base64.length % 4)) % 4);
  const binario = atob(base64 + relleno);
  const bytes = Uint8Array.from(binario, (caracter) => caracter.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

// Lee el payload del JWT sin verificar la firma (el backend ya la verificó).
function leerToken(token) {
  try {
    const payload = base64UrlADatos(token.split(".")[1]);
    return {
      id: payload.id,
      nombre: payload.nombre,
      email: payload.sub,
      roles: payload.roles ?? [],
      exp: payload.exp,
    };
  } catch (e) {
    return null;
  }
}

const tieneRol = (rol) => Boolean(usuario && usuario.roles.includes(rol));
const esAdmin = () => tieneRol("ROLE_ADMIN");
const puedeCambiarEstado = () => esAdmin() || tieneRol("ROLE_TECNICO");

function fecha(iso) {
  if (!iso) return "-";
  return new Date(iso).toLocaleString("es-ES", { dateStyle: "short", timeStyle: "short" });
}

function mostrarMensaje(texto, error = false) {
  const contenedor = $("mensaje");
  contenedor.textContent = texto;
  contenedor.className = "mensaje" + (error ? " error" : "");
  clearTimeout(mostrarMensaje.temporizador);
  mostrarMensaje.temporizador = setTimeout(() => contenedor.classList.add("oculto"), 6000);
}

// Ejecuta una acción y muestra por pantalla el error que devuelva la API.
async function ejecutar(accion, mensajeOk) {
  try {
    const resultado = await accion();
    if (mensajeOk) mostrarMensaje(mensajeOk);
    return resultado;
  } catch (error) {
    mostrarMensaje(error.message, true);
    throw error;
  }
}

function crearEtiquetaEstado(estado) {
  const span = document.createElement("span");
  span.className = "etiqueta estado-" + estado.toLowerCase();
  span.textContent = estado;
  return span;
}

/* ---------------- Sesión ---------------- */

function mostrarAcceso() {
  usuario = null;
  incidenciaAbierta = null;
  incidencias = [];
  $("aplicacion").classList.add("oculto");
  $("sesion").classList.add("oculto");
  $("acceso").classList.remove("oculto");
  $("panel-login").classList.remove("oculto");
  $("panel-registro").classList.add("oculto");
}

function mostrarAplicacion() {
  $("acceso").classList.add("oculto");
  $("aplicacion").classList.remove("oculto");
  $("sesion").classList.remove("oculto");
  $("usuario-nombre").textContent = usuario.nombre + " (" + usuario.email + ")";
  $("usuario-rol").textContent = usuario.roles.map((rol) => ETIQUETA_ROL[rol] ?? rol).join(", ");
  $("panel-usuarios").classList.toggle("oculto", !esAdmin());
  $("titulo-incidencias").textContent = puedeCambiarEstado() ? "Todas las incidencias" : "Mis incidencias";
  $("ayuda-listado").textContent = puedeCambiarEstado()
    ? "Puedes consultar cualquier incidencia y cambiar su estado."
    : "Solo ves las incidencias que has creado. Si pides una ajena por su URL, la API responde 403.";
}

function iniciarSesionConToken(token) {
  const datos = leerToken(token);
  const caducado = !datos || !datos.exp || datos.exp * 1000 <= Date.now();

  if (caducado) {
    sesion.cerrar();
    mostrarAcceso();
    mostrarMensaje("La sesión ha caducado. Inicia sesión de nuevo", true);
    return;
  }

  usuario = datos;
  mostrarAplicacion();
  cargarIncidencias();
  if (esAdmin()) cargarUsuarios();

  mostrarMensaje(
    "Sesión iniciada como " + usuario.nombre + ". Token válido hasta las " +
    new Date(usuario.exp * 1000).toLocaleTimeString("es-ES")
  );
}

/* ---------------- Incidencias ---------------- */

async function cargarIncidencias() {
  try {
    incidencias = await api.incidencias.listar();
    pintarIncidencias();
  } catch (error) {
    mostrarMensaje(error.message, true);
  }
}

function pintarIncidencias() {
  const cuerpo = $("tabla-incidencias");
  cuerpo.replaceChildren();

  for (const incidencia of incidencias) {
    const fila = document.createElement("tr");

    const celdaId = document.createElement("td");
    celdaId.textContent = incidencia.id;

    const celdaTitulo = document.createElement("td");
    celdaTitulo.textContent = incidencia.titulo;

    const celdaEstado = document.createElement("td");
    celdaEstado.append(crearEtiquetaEstado(incidencia.estado));

    const celdaAutor = document.createElement("td");
    celdaAutor.textContent = incidencia.autor.nombre;

    const celdaFecha = document.createElement("td");
    celdaFecha.textContent = fecha(incidencia.fechaCreacion);

    const celdaAcciones = document.createElement("td");
    const boton = document.createElement("button");
    boton.type = "button";
    boton.textContent = "Abrir";
    boton.addEventListener("click", () => abrirDetalle(incidencia.id));
    celdaAcciones.append(boton);

    fila.append(celdaId, celdaTitulo, celdaEstado, celdaAutor, celdaFecha, celdaAcciones);
    cuerpo.append(fila);
  }

  $("sin-incidencias").classList.toggle("oculto", incidencias.length > 0);
}

async function abrirDetalle(id) {
  let incidencia;
  try {
    incidencia = await api.incidencias.obtener(id);
  } catch (error) {
    // Aquí se ve el 403 del reto: la URL no da acceso a incidencias ajenas
    mostrarMensaje(error.message, true);
    return;
  }

  incidenciaAbierta = incidencia;
  $("detalle-titulo").textContent = "#" + incidencia.id + " " + incidencia.titulo;
  $("detalle-estado").replaceChildren(crearEtiquetaEstado(incidencia.estado));
  $("detalle-autor").textContent = incidencia.autor.nombre + " (" + incidencia.autor.email + ")";
  $("detalle-creacion").textContent = fecha(incidencia.fechaCreacion);
  $("detalle-actualizacion").textContent = fecha(incidencia.fechaActualizacion);
  $("detalle-descripcion").textContent = incidencia.descripcion;
  $("detalle-nuevo-estado").value = incidencia.estado;

  $("detalle-acciones").classList.toggle("oculto", !puedeCambiarEstado() && !esAdmin());
  $("detalle-guardar-estado").classList.toggle("oculto", !puedeCambiarEstado());
  $("detalle-nuevo-estado").parentElement.classList.toggle("oculto", !puedeCambiarEstado());
  $("detalle-eliminar").classList.toggle("oculto", !esAdmin());

  $("dialogo-detalle").showModal();
}

async function guardarEstado() {
  if (!incidenciaAbierta) return;
  const estado = $("detalle-nuevo-estado").value;

  try {
    await ejecutar(
      () => api.incidencias.cambiarEstado(incidenciaAbierta.id, estado),
      "Estado actualizado a " + estado
    );
    $("dialogo-detalle").close();
    await cargarIncidencias();
  } catch (error) {
    // 403 si el token es de un usuario que no es técnico ni admin
  }
}

async function eliminarIncidencia() {
  if (!incidenciaAbierta) return;
  if (!confirm("¿Seguro que quieres eliminar la incidencia #" + incidenciaAbierta.id + "?")) return;

  try {
    await ejecutar(
      () => api.incidencias.eliminar(incidenciaAbierta.id),
      "Incidencia eliminada"
    );
    $("dialogo-detalle").close();
    await cargarIncidencias();
  } catch (error) {
    // 403 si el usuario no es administrador
  }
}

/* ---------------- Usuarios (solo ADMIN) ---------------- */

async function cargarUsuarios() {
  let usuarios;
  try {
    usuarios = await api.usuarios.listar();
  } catch (error) {
    mostrarMensaje(error.message, true);
    return;
  }

  const cuerpo = $("tabla-usuarios");
  cuerpo.replaceChildren();

  for (const u of usuarios) {
    const fila = document.createElement("tr");

    const celdaId = document.createElement("td");
    celdaId.textContent = u.id;

    const celdaNombre = document.createElement("td");
    celdaNombre.textContent = u.nombre;

    const celdaEmail = document.createElement("td");
    celdaEmail.textContent = u.email;

    const celdaRol = document.createElement("td");
    celdaRol.textContent = ETIQUETA_ROL[u.rol] ?? u.rol;

    const celdaCambio = document.createElement("td");
    const select = document.createElement("select");
    for (const rol of ["ROLE_USER", "ROLE_TECNICO", "ROLE_ADMIN"]) {
      const opcion = document.createElement("option");
      opcion.value = rol;
      opcion.textContent = ETIQUETA_ROL[rol];
      select.append(opcion);
    }
    select.value = u.rol;

    const boton = document.createElement("button");
    boton.type = "button";
    boton.textContent = "Cambiar";
    boton.addEventListener("click", () => cambiarRol(u, select.value));

    celdaCambio.append(select, boton);
    fila.append(celdaId, celdaNombre, celdaEmail, celdaRol, celdaCambio);
    cuerpo.append(fila);
  }
}

async function cambiarRol(usuarioCambiado, rol) {
  if (rol === usuarioCambiado.rol) {
    mostrarMensaje("El usuario ya tiene ese rol", true);
    return;
  }

  try {
    await ejecutar(
      () => api.usuarios.cambiarRol(usuarioCambiado.id, rol),
      "Rol de " + usuarioCambiado.email + " actualizado a " + (ETIQUETA_ROL[rol] ?? rol)
    );
    await cargarUsuarios();
    if (usuarioCambiado.id === usuario.id) {
      mostrarMensaje(
        "Has cambiado tu propio rol: tu token actual mantiene los permisos antiguos " +
        "hasta que vuelvas a iniciar sesión", true
      );
    }
  } catch (error) {
    // 400 si el rol no es válido, 403 si el token no es de administrador
  }
}

/* ---------------- Arranque y eventos ---------------- */

function conectarEventos() {
  $("ir-registro").addEventListener("click", (evento) => {
    evento.preventDefault();
    $("panel-login").classList.add("oculto");
    $("panel-registro").classList.remove("oculto");
  });

  $("ir-login").addEventListener("click", (evento) => {
    evento.preventDefault();
    $("panel-registro").classList.add("oculto");
    $("panel-login").classList.remove("oculto");
  });

  $("form-login").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
      const respuesta = await api.auth.login(datos);
      sesion.guardar(respuesta.token);
      evento.target.reset();
      iniciarSesionConToken(respuesta.token);
    } catch (error) {
      mostrarMensaje(error.message, true);
    }
  });

  $("form-registro").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
      await api.auth.registro(datos);
      evento.target.reset();
      $("panel-registro").classList.add("oculto");
      $("panel-login").classList.remove("oculto");
      $("form-login").email.value = datos.email;
      mostrarMensaje("Cuenta creada. Inicia sesión con tu email y contraseña");
    } catch (error) {
      mostrarMensaje(error.message, true);
    }
  });

  $("cerrar-sesion").addEventListener("click", () => {
    sesion.cerrar();
    mostrarAcceso();
    mostrarMensaje("Sesión cerrada");
  });

  window.addEventListener("sesion-caducada", () => {
    mostrarAcceso();
  });

  $("nueva-incidencia").addEventListener("click", () => {
    $("form-incidencia").reset();
    $("dialogo-crear").showModal();
  });

  $("cancelar-crear").addEventListener("click", () => $("dialogo-crear").close());

  $("form-incidencia").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
      await ejecutar(
        () => api.incidencias.crear({ titulo: datos.titulo, descripcion: datos.descripcion }),
        "Incidencia creada"
      );
      $("dialogo-crear").close();
      await cargarIncidencias();
    } catch (error) {
      $("dialogo-crear").close();
    }
  });

  $("detalle-guardar-estado").addEventListener("click", guardarEstado);
  $("detalle-eliminar").addEventListener("click", eliminarIncidencia);
  $("detalle-cerrar").addEventListener("click", () => $("dialogo-detalle").close());
  $("recargar-usuarios").addEventListener("click", cargarUsuarios);
}

document.addEventListener("DOMContentLoaded", () => {
  conectarEventos();

  const token = sesion.token();
  if (token) {
    iniciarSesionConToken(token);
  } else {
    mostrarAcceso();
  }
});
