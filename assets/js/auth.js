/* =====================================================
   RUPE
   Archivo: auth.js
   Descripcion:
   Conecta registro e inicio de sesion con el backend
   Spring Boot mediante peticiones fetch.
===================================================== */

const RUPE_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

function mostrarMensajeFormulario(form, mensaje, tipo = "info") {
    let contenedor = form.querySelector(".form-backend-message");

    if (!contenedor) {
        contenedor = document.createElement("div");
        contenedor.className = "form-backend-message";
        form.prepend(contenedor);
    }

    contenedor.textContent = mensaje;
    contenedor.dataset.tipo = tipo;
}

async function leerRespuesta(response) {
    const texto = await response.text();

    if (!texto) {
        return {};
    }

    try {
        return JSON.parse(texto);
    } catch (error) {
        return { mensaje: texto };
    }
}

async function enviarJson(endpoint, datos) {
    const response = await fetch(`${RUPE_API_BASE}${endpoint}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify(datos)
    });

    const body = await leerRespuesta(response);

    if (!response.ok) {
        throw new Error(body.mensaje || "No fue posible completar la solicitud.");
    }

    return body;
}

let intentosFallidosLogin = 0;

function passwordCumplePolitica(password) {
    // La misma regla se valida en backend: longitud, mayuscula, minuscula, numero y caracter especial.
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,40}$/.test(password);
}

function mostrarCaptchaLogin(form) {
    const captchaBox = document.getElementById("loginCaptchaBox");
    if (captchaBox) {
        captchaBox.style.display = "block";
    }
    mostrarMensajeFormulario(form, "Por seguridad, completa el CAPTCHA para continuar.", "warn");
}

function cambiarEstadoEnvio(form, enviando, textoEnviando) {
    const boton = form.querySelector('button[type="submit"]');
    if (!boton) return;

    if (!boton.dataset.textoOriginal) {
        boton.dataset.textoOriginal = boton.textContent.trim();
    }

    boton.disabled = enviando;
    boton.textContent = enviando ? textoEnviando : boton.dataset.textoOriginal;
}

function obtenerRutaPorRol(rol) {
    if (rol === "ADMINISTRADOR") {
        return "dashboard-admin.html";
    }

    if (rol === "DUENO") {
        return "dashboard_usuario.html";
    }

    return "index.html";
}

function configurarRegistro() {
    const form = document.getElementById("formRegistroUsuario");

    if (!form) {
        return;
    }

    form.addEventListener("submit", async function(event) {
        event.preventDefault();

        const password = form.password.value.trim();
        const confirmarPassword = form.querySelector('[name="confirmar-password"]').value.trim();

        if (password !== confirmarPassword) {
            mostrarMensajeFormulario(form, "Las contrasenas no coinciden.", "error");
            return;
        }

        if (!passwordCumplePolitica(password)) {
            mostrarMensajeFormulario(form, "La contrasena debe tener de 8 a 40 caracteres e incluir mayuscula, minuscula, numero y caracter especial.", "error");
            return;
        }

        const captchaToken = form.querySelector('[name="cf-turnstile-response"]')?.value || "";

        if (!captchaToken) {
            mostrarMensajeFormulario(form, "Completa la verificacion de seguridad.", "error");
            return;
        }

        const datos = {
            nombreCompleto: form.nombre.value.trim(),
            correo: form.correo.value.trim(),
            telefono: form.telefono.value.trim(),
            password: password,
            captchaToken: captchaToken
        };

        try {
            cambiarEstadoEnvio(form, true, "Creando cuenta...");
            const usuario = await enviarJson("/auth/registro", datos);
            sessionStorage.setItem("rupeUsuario", JSON.stringify(usuario));
            mostrarMensajeFormulario(form, "Cuenta creada correctamente. Redirigiendo al inicio de sesion...", "success");

            setTimeout(function() {
                window.location.href = "login.html";
            }, 900);
        } catch (error) {
            const mensaje = error.message === "Failed to fetch"
                ? "No se pudo conectar con el backend. Verifica que Spring Boot este encendido en http://localhost:8080."
                : error.message;
            mostrarMensajeFormulario(form, mensaje, "error");
            cambiarEstadoEnvio(form, false);
        }
    });
}

function configurarLogin() {
    const form = document.getElementById("formLogin");

    if (!form) {
        return;
    }

    form.addEventListener("submit", async function(event) {
        event.preventDefault();

        const captchaToken = form.querySelector('[name="cf-turnstile-response"]')?.value || "";
        const captchaVisible = document.getElementById("loginCaptchaBox")?.style.display !== "none";

        if (captchaVisible && !captchaToken) {
            mostrarMensajeFormulario(form, "Completa la verificacion de seguridad antes de iniciar sesion.", "error");
            return;
        }

        const datos = {
            correo: form.correo.value.trim(),
            password: form.password.value.trim(),
            captchaToken: captchaToken
        };

        try {
            cambiarEstadoEnvio(form, true, "Validando...");
            const usuario = await enviarJson("/auth/login", datos);
            intentosFallidosLogin = 0;
            sessionStorage.setItem("rupeUsuario", JSON.stringify(usuario));
            mostrarMensajeFormulario(form, "Sesion iniciada correctamente. Redirigiendo...", "success");

            setTimeout(function() {
                const redirect = new URLSearchParams(window.location.search).get("redirect");
                window.location.href = redirect || obtenerRutaPorRol(usuario.rol);
            }, 700);
        } catch (error) {
            const mensaje = error.message === "Failed to fetch"
                ? "No se pudo conectar con el backend. Verifica que Spring Boot este encendido en http://localhost:8080."
                : error.message;
            intentosFallidosLogin++;
            if (mensaje.toLowerCase().includes("captcha") || intentosFallidosLogin >= 3) {
                cambiarEstadoEnvio(form, false);
                mostrarCaptchaLogin(form);
                return;
            }
            mostrarMensajeFormulario(form, mensaje, "error");
            cambiarEstadoEnvio(form, false);
        }
    });
}


async function redirigirSiYaTieneSesion() {
    const redirect = new URLSearchParams(window.location.search).get("redirect");
    if (!redirect) return;

    try {
        const response = await fetch(`${RUPE_API_BASE}/auth/sesion`, {
            method: "GET",
            credentials: "include"
        });
        if (!response.ok) return;
        window.location.href = redirect;
    } catch (error) {
        // Si no hay sesion, el usuario inicia sesion normalmente.
    }
}

document.addEventListener("DOMContentLoaded", function() {
    redirigirSiYaTieneSesion();
    configurarRegistro();
    configurarLogin();
});
