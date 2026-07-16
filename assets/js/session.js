/* =====================================================
   RUPE
   Archivo: session.js
   Descripcion:
   Apoya la sesion del usuario en pantallas privadas y
   permite cerrar sesion desde el frontend.
===================================================== */

const RUPE_SESSION_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

async function obtenerSesionActual() {
    const response = await fetch(`${RUPE_SESSION_API}/auth/sesion`, {
        method: "GET",
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error("Sesion no activa");
    }

    return response.json();
}

function obtenerUsuarioLocal() {
    const usuarioGuardado = sessionStorage.getItem("rupeUsuario");

    if (!usuarioGuardado) {
        return null;
    }

    try {
        return JSON.parse(usuarioGuardado);
    } catch (error) {
        sessionStorage.removeItem("rupeUsuario");
        return null;
    }
}

function pintarUsuario(usuario) {
    const nombre = usuario.nombreCompleto || "Usuario";
    const rol = usuario.rol || "";

    document.querySelectorAll("[data-user-name]").forEach(function(elemento) {
        elemento.textContent = nombre;
    });

    document.querySelectorAll("[data-user-role]").forEach(function(elemento) {
        elemento.textContent = rol;
    });
}

async function protegerPaginaPrivada(rolEsperado) {
    let usuario = null;

    try {
        usuario = await obtenerSesionActual();
        sessionStorage.setItem("rupeUsuario", JSON.stringify(usuario));
    } catch (error) {
        sessionStorage.removeItem("rupeUsuario");
        window.location.href = "login.html";
        return;
    }

    const rolesPermitidos = Array.isArray(rolEsperado) ? rolEsperado : [rolEsperado].filter(Boolean);

    if (rolesPermitidos.length && !rolesPermitidos.includes(usuario.rol)) {
        window.location.href = usuario.rol === "ADMINISTRADOR" ? "dashboard-admin.html" : "dashboard_usuario.html";
        return;
    }

    pintarUsuario(usuario);
}

async function cerrarSesionRupe(event) {
    if (event) {
        event.preventDefault();
    }

    try {
        await fetch(`${RUPE_SESSION_API}/auth/logout`, {
            method: "POST",
            credentials: "include"
        });
    } catch (error) {
        // Si el backend no responde, se limpia la sesion local de todos modos.
    }

    sessionStorage.removeItem("rupeUsuario");
    window.location.href = "index.html";
}

function configurarCerrarSesion() {
    document.querySelectorAll("[data-logout]").forEach(function(link) {
        link.addEventListener("click", cerrarSesionRupe);
    });
}

document.addEventListener("DOMContentLoaded", configurarCerrarSesion);
