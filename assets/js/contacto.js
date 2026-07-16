const RUPE_CONTACTO_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formContacto');
    const mensaje = document.getElementById('mensajeContacto');
    const contador = document.getElementById('contadorContacto');

    mensaje?.addEventListener('input', () => {
        if (contador) contador.textContent = `${mensaje.value.length}/500 caracteres`;
    });

    form?.addEventListener('submit', async (event) => {
        event.preventDefault();

        const nombre = document.getElementById('nombreContacto').value.trim();
        const correo = document.getElementById('correoContacto').value.trim();
        const telefono = document.getElementById('telefonoContacto').value.trim();
        const asunto = document.getElementById('asuntoContacto').value;
        const texto = mensaje.value.trim();
        const privacidad = document.getElementById('privacidadContacto').checked;
        const turnstileToken = document.querySelector('[name="cf-turnstile-response"]')?.value || '';

        if (!nombre || !correo || !asunto || !texto) {
            mostrarContacto('Completa los campos obligatorios antes de enviar.', 'warn');
            return;
        }
        if (!correoContactoValido(correo)) {
            mostrarContacto('Captura un correo electronico valido. Ejemplo: nombre@dominio.com', 'warn');
            return;
        }
        if (telefono && !/^\d{10}$/.test(telefono)) {
            mostrarContacto('El telefono debe contener 10 digitos numericos.', 'warn');
            return;
        }
        if (!privacidad) {
            mostrarContacto('Acepta el aviso de privacidad para continuar.', 'warn');
            return;
        }
        if (!turnstileToken) {
            mostrarContacto('Completa la verificacion de seguridad antes de enviar.', 'warn');
            return;
        }

        try {
            const respuesta = await fetch(`${RUPE_CONTACTO_API}/contacto`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nombre, correo, telefono, asunto, mensaje: texto, turnstileToken })
            });
            const datos = await leerRespuestaContacto(respuesta);
            if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo enviar el mensaje.');

            await mostrarContacto(datos.mensaje || 'Mensaje enviado correctamente. El equipo RUPE dara seguimiento desde soporte.', 'success');
            form.reset();
            if (window.turnstile) window.turnstile.reset();
            if (contador) contador.textContent = '0/500 caracteres';
        } catch (error) {
            mostrarContacto(error.message || 'No se pudo conectar con el backend.', 'error');
        }
    });
});

async function leerRespuestaContacto(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function mostrarContacto(mensaje, tipo) {
    return mostrarVentanaContacto({
        titulo: tipo === 'success' ? 'Mensaje enviado' : tipo === 'error' ? 'No fue posible enviar' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaContacto({ titulo, mensaje, tipo = 'info', textoBoton = 'Aceptar' }) {
    return new Promise((resolve) => {
        document.querySelector('.contacto-modal-overlay')?.remove();

        const overlay = document.createElement('div');
        overlay.className = 'contacto-modal-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(17,24,39,.58);display:flex;align-items:center;justify-content:center;z-index:9999;padding:18px;';

        const color = tipo === 'success' ? '#198754' : tipo === 'error' ? '#b42318' : '#8a1538';
        const icono = tipo === 'success' ? 'fa-circle-check' : tipo === 'error' ? 'fa-circle-xmark' : 'fa-circle-info';

        overlay.innerHTML = `
            <div role="dialog" aria-modal="true" aria-labelledby="contactoModalTitulo" style="width:min(440px,100%);background:#fff;border-radius:10px;box-shadow:0 24px 70px rgba(0,0,0,.24);padding:26px;text-align:center;font-family:Montserrat,Arial,sans-serif;">
                <i class="fa-solid ${icono}" style="font-size:46px;color:${color};margin-bottom:14px;"></i>
                <h2 id="contactoModalTitulo" style="margin:0 0 10px;color:#2b2b2b;font-size:1.35rem;">${escaparHtmlContacto(titulo)}</h2>
                <p style="margin:0 0 20px;color:#4b5563;line-height:1.55;">${escaparHtmlContacto(mensaje)}</p>
                <button type="button" class="contacto-modal-aceptar" style="border:0;border-radius:8px;background:${color};color:#fff;font-weight:700;padding:11px 22px;cursor:pointer;min-width:130px;">${escaparHtmlContacto(textoBoton)}</button>
            </div>
        `;

        const cerrar = () => {
            overlay.remove();
            resolve();
        };

        overlay.querySelector('.contacto-modal-aceptar').addEventListener('click', cerrar);
        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) cerrar();
        });
        document.addEventListener('keydown', function cerrarConEscape(event) {
            if (event.key === 'Escape') {
                document.removeEventListener('keydown', cerrarConEscape);
                cerrar();
            }
        });

        document.body.appendChild(overlay);
        overlay.querySelector('.contacto-modal-aceptar').focus();
    });
}

function escaparHtmlContacto(valor) {
    return String(valor || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function correoContactoValido(correo) {
    // Se exige dominio completo para reducir capturas incompletas en el formulario publico.
    const valor = String(correo || '').trim().toLowerCase();
    return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{3,}$/.test(valor);
}