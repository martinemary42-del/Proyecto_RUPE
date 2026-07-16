const RUPE_REC_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.recover-form');
    if (!form) return;
    form.id = 'formRecuperacionPassword';
    agregarCamposRestablecimiento(form);
    form.addEventListener('submit', solicitarRecuperacion);
});

function passwordRecuperacionCumplePolitica(password) {
    // Misma politica usada en registro y backend: 8 a 40 caracteres con complejidad minima.
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,40}$/.test(password);
}

function cambiarEstadoBotonRecuperacion(boton, enviando, textoEnviando) {
    if (!boton) return;
    if (!boton.dataset.textoOriginal) {
        boton.dataset.textoOriginal = boton.textContent.trim();
    }
    boton.disabled = enviando;
    boton.textContent = enviando ? textoEnviando : boton.dataset.textoOriginal;
}

function agregarCamposRestablecimiento(form) {
    const bloque = document.createElement('div');
    bloque.id = 'bloqueRestablecerPassword';
    bloque.className = 'recover-card recover-reset-card';
    bloque.style.display = 'none';
    bloque.innerHTML = `
        <div class="recover-card-header">
            <img src="assets/img/logo/logo-rupe.png" alt="Logo RUPE">
            <h2>Crear nueva contraseña</h2>
            <p>Usa el token temporal generado para este prototipo local.</p>
        </div>
        <div class="form-group">
            <label for="tokenRecuperacion">Token de recuperación</label>
            <input type="text" id="tokenRecuperacion" placeholder="Token temporal" autocomplete="one-time-code">
        </div>
        <div class="form-group">
            <label for="nuevaPassword">Nueva contraseña</label>
            <input type="password" id="nuevaPassword" minlength="8" maxlength="40" autocomplete="new-password" placeholder="Nueva contraseña robusta">
            <small>Debe incluir mayúscula, minúscula, número y carácter especial.</small>
        </div>
        <div class="form-group">
            <label for="confirmarNuevaPassword">Confirmar contraseña</label>
            <input type="password" id="confirmarNuevaPassword" autocomplete="new-password" placeholder="Confirma la contraseña">
        </div>
        <button type="button" class="btn-recover" id="btnRestablecerPassword">Restablecer contraseña</button>
    `;
    form.insertAdjacentElement('afterend', bloque);
    document.getElementById('btnRestablecerPassword')?.addEventListener('click', restablecerPassword);
}

async function solicitarRecuperacion(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const boton = form.querySelector('button[type="submit"]');
    const correo = form.correo.value.trim();
    const captchaToken = form.querySelector('[name="cf-turnstile-response"]')?.value || '';
    if (!captchaToken) {
        mostrarRecuperacion('Completa el CAPTCHA antes de continuar.', 'error');
        return;
    }
    try {
        cambiarEstadoBotonRecuperacion(boton, true, 'Generando solicitud...');
        const respuesta = await fetch(`${RUPE_REC_API}/auth/recuperacion-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correo, captchaToken })
        });
        const datos = await leerRecuperacion(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo generar la recuperación.');
        mostrarRecuperacion(datos.mensaje || 'Solicitud registrada.', 'success');
        if (datos.tokenDesarrollo) {
            document.getElementById('bloqueRestablecerPassword').style.display = 'block';
            document.getElementById('tokenRecuperacion').value = datos.tokenDesarrollo;
        }
        if (window.turnstile) window.turnstile.reset();
    } catch (error) {
        mostrarRecuperacion(error.message || 'No se pudo conectar con el backend.', 'error');
    } finally {
        cambiarEstadoBotonRecuperacion(boton, false);
    }
}

async function restablecerPassword() {
    const boton = document.getElementById('btnRestablecerPassword');
    const token = document.getElementById('tokenRecuperacion').value.trim();
    const password = document.getElementById('nuevaPassword').value.trim();
    const confirmar = document.getElementById('confirmarNuevaPassword').value.trim();
    if (password !== confirmar) {
        mostrarRecuperacion('Las contraseñas no coinciden.', 'error');
        return;
    }
    if (!passwordRecuperacionCumplePolitica(password)) {
        mostrarRecuperacion('La contraseña debe tener de 8 a 40 caracteres e incluir mayúscula, minúscula, número y carácter especial.', 'error');
        return;
    }
    try {
        cambiarEstadoBotonRecuperacion(boton, true, 'Restableciendo...');
        const respuesta = await fetch(`${RUPE_REC_API}/auth/restablecer-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, password })
        });
        if (!respuesta.ok) {
            const datos = await leerRecuperacion(respuesta);
            throw new Error(datos.mensaje || datos.error || 'No se pudo restablecer la contraseña.');
        }
        mostrarRecuperacion('Contraseña restablecida correctamente. Ya puedes iniciar sesión.', 'success');
        setTimeout(() => window.location.href = 'login.html', 900);
    } catch (error) {
        mostrarRecuperacion(error.message || 'No se pudo conectar con el backend.', 'error');
        cambiarEstadoBotonRecuperacion(boton, false);
    }
}

async function leerRecuperacion(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function mostrarRecuperacion(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}
