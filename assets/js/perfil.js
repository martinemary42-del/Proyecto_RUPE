/* =====================================================
   PERFIL - RUPE
   Consulta y actualiza los datos reales del usuario.
===================================================== */

const RUPE_PERFIL_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const PATRON_CORREO_RUPE = /^[^\s@]+@[^\s@]+\.(com|com\.mx|mx|org|net|edu|gob\.mx)$/i;

const formPerfil = document.getElementById('formPerfil');
const telefonoInput = document.getElementById('telefono');
const btnDesactivarCuenta = document.getElementById('btnDesactivarCuenta');

document.addEventListener('DOMContentLoaded', cargarPerfil);
formPerfil?.addEventListener('submit', guardarPerfil);
telefonoInput?.addEventListener('input', validarTelefonoEnCaptura);
btnDesactivarCuenta?.addEventListener('click', desactivarCuentaPropia);

async function cargarPerfil() {
    try {
        const respuesta = await fetch(`${RUPE_PERFIL_API_BASE}/usuarios/perfil`, {
            credentials: 'include'
        });
        const perfil = await leerRespuestaPerfil(respuesta);
        if (!respuesta.ok) throw new Error(perfil.mensaje || perfil.error || 'No se pudo cargar el perfil.');

        const partes = separarNombre(perfil.nombreCompleto);
        document.getElementById('nombre').value = partes.nombre;
        document.getElementById('apellidos').value = partes.apellidos;
        document.getElementById('correo').value = perfil.correo || '';
        document.getElementById('telefono').value = perfil.telefono || '';
        mostrarMensajePerfil('Perfil cargado correctamente.', 'info');
    } catch (error) {
        mostrarAvisoPerfil(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function guardarPerfil(event) {
    event.preventDefault();

    const nombre = document.getElementById('nombre').value.trim();
    const apellidos = document.getElementById('apellidos').value.trim();
    const correo = document.getElementById('correo').value.trim();
    const telefono = document.getElementById('telefono').value.trim();
    const passwordActual = document.getElementById('passwordActual').value.trim();
    const passwordNueva = document.getElementById('passwordNueva').value.trim();
    const passwordConfirmar = document.getElementById('passwordConfirmar').value.trim();
    const confirmacion = document.getElementById('confirmacion').checked;

    if (!nombre || !apellidos || !correo) {
        mostrarAvisoPerfil('Completa nombre, apellidos y correo.', 'error');
        return;
    }
    if (!PATRON_CORREO_RUPE.test(correo)) {
        mostrarAvisoPerfil('Ingresa un correo valido. Ejemplo: usuario@correo.com', 'error');
        return;
    }
    if (telefono && !/^\d{10}$/.test(telefono)) {
        mostrarAvisoPerfil('El telefono debe contener solo numeros y exactamente 10 digitos.', 'error');
        return;
    }
    if (passwordNueva || passwordConfirmar || passwordActual) {
        if (!passwordActual) {
            mostrarAvisoPerfil('Ingresa tu contrasena actual para cambiarla.', 'error');
            return;
        }
        if (passwordNueva.length < 8) {
            mostrarAvisoPerfil('La nueva contrasena debe tener al menos 8 caracteres.', 'error');
            return;
        }
        if (passwordNueva !== passwordConfirmar) {
            mostrarAvisoPerfil('La confirmacion de contrasena no coincide.', 'error');
            return;
        }
    }
    if (!confirmacion) {
        mostrarAvisoPerfil('Confirma que la informacion actualizada es correcta.', 'error');
        return;
    }

    try {
        mostrarMensajePerfil('Guardando cambios...', 'info');
        const cuerpo = {
            nombreCompleto: `${nombre} ${apellidos}`.trim(),
            correo,
            telefono,
            passwordActual,
            passwordNueva
        };

        const respuesta = await fetch(`${RUPE_PERFIL_API_BASE}/usuarios/perfil`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(cuerpo)
        });
        const perfil = await leerRespuestaPerfil(respuesta);
        if (!respuesta.ok) throw new Error(perfil.mensaje || perfil.error || 'No se pudo actualizar el perfil.');

        document.getElementById('passwordActual').value = '';
        document.getElementById('passwordNueva').value = '';
        document.getElementById('passwordConfirmar').value = '';
        document.getElementById('confirmacion').checked = false;
        sessionStorage.setItem('rupeUsuario', JSON.stringify(perfil));
        mostrarVentanaPerfil('Perfil actualizado correctamente.', 'Tus datos fueron guardados correctamente en RUPE.');
    } catch (error) {
        mostrarAvisoPerfil(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function desactivarCuentaPropia() {
    const acepta = confirm('Esta accion desactivara tu cuenta y cerrara la sesion actual. Tus registros se conservaran para seguimiento. Deseas continuar?');
    if (!acepta) return;

    const confirmacion = prompt('Para confirmar, escribe DESACTIVAR en mayusculas.');
    if (confirmacion !== 'DESACTIVAR') {
        mostrarAvisoPerfil('No se desactivo la cuenta porque la confirmacion no coincide.', 'error');
        return;
    }

    btnDesactivarCuenta.disabled = true;
    btnDesactivarCuenta.textContent = 'Desactivando...';

    try {
        const respuesta = await fetch(`${RUPE_PERFIL_API_BASE}/usuarios/perfil`, {
            method: 'DELETE',
            credentials: 'include'
        });
        const data = await leerRespuestaPerfil(respuesta);
        if (!respuesta.ok) {
            throw new Error(data.mensaje || data.error || 'No se pudo desactivar la cuenta.');
        }

        sessionStorage.removeItem('rupeUsuario');
        mostrarAvisoPerfil(data.mensaje || 'Cuenta desactivada correctamente.', 'success');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1600);
    } catch (error) {
        btnDesactivarCuenta.disabled = false;
        btnDesactivarCuenta.textContent = 'Desactivar mi cuenta';
        mostrarAvisoPerfil(error.message, 'error');
    }
}

function validarTelefonoEnCaptura(event) {
    const valorOriginal = event.target.value;
    const soloNumeros = valorOriginal.replace(/\D/g, '').slice(0, 10);

    if (valorOriginal !== soloNumeros) {
        event.target.value = soloNumeros;
        mostrarMensajePerfil('El telefono solo permite numeros.', 'warn');
    }
}

function separarNombre(nombreCompleto) {
    const partes = String(nombreCompleto || '').trim().split(/\s+/);
    if (partes.length <= 1) return { nombre: partes[0] || '', apellidos: '' };
    return {
        nombre: partes.slice(0, 1).join(' '),
        apellidos: partes.slice(1).join(' ')
    };
}

async function leerRespuestaPerfil(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function mostrarAvisoPerfil(mensaje, tipo) {
    mostrarMensajePerfil(mensaje, tipo);
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    }
}

function mostrarMensajePerfil(mensaje, tipo) {
    if (!formPerfil) return;
    let contenedor = formPerfil.querySelector('.form-backend-message');
    if (!contenedor) {
        contenedor = document.createElement('div');
        contenedor.className = 'form-backend-message';
        formPerfil.prepend(contenedor);
    }
    contenedor.textContent = mensaje;
    contenedor.dataset.tipo = tipo || 'info';
}

function mostrarVentanaPerfil(titulo, mensaje) {
    const anterior = document.querySelector('.dueno-modal-overlay');
    if (anterior) anterior.remove();

    const overlay = document.createElement('div');
    overlay.className = 'dueno-modal-overlay';
    overlay.style.position = 'fixed';
    overlay.style.inset = '0';
    overlay.style.zIndex = '99999';
    overlay.style.display = 'flex';
    overlay.style.alignItems = 'center';
    overlay.style.justifyContent = 'center';
    overlay.style.padding = '20px';
    overlay.style.background = 'rgba(35, 0, 20, .58)';
    overlay.innerHTML = `
        <div class="dueno-modal" role="dialog" aria-modal="true" style="width:min(460px,100%);padding:28px;border-radius:14px;background:#fff;text-align:center;box-shadow:0 20px 50px rgba(0,0,0,.24)">
            <i class="fas fa-circle-check"></i>
            <h2>${escaparHtmlPerfil(titulo || 'Operacion realizada')}</h2>
            <p>${escaparHtmlPerfil(mensaje || '')}</p>
            <button type="button" style="width:100%;margin-top:22px;padding:14px;border:0;border-radius:10px;background:#611232;color:#fff;font-weight:700;cursor:pointer">Aceptar</button>
        </div>
    `;
    document.body.appendChild(overlay);
    overlay.querySelector('button')?.focus();
    overlay.querySelector('button')?.addEventListener('click', () => overlay.remove());
}

function escaparHtmlPerfil(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
