/* =====================================================
   RUPE - Avistamiento ciudadano
   Carga ubicaciones y registra el avistamiento en backend.
===================================================== */

const RUPE_AVISTAMIENTO_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_AVISTAMIENTO_MAX_FOTO_MB = 5;

let avisoResguardoMostrado = false;

document.addEventListener('DOMContentLoaded', () => {
    // Inicializa el formulario publico: folio desde QR, fechas permitidas, catalogos y envio.
    prepararTurnstileAvistamiento();
    cargarFolioDesdeUrl();
    configurarRangoFechaAvistamiento();
    cargarUbicacionesAvistamiento();
    prepararResguardo();
    prepararEnvioAvistamiento();
});

function prepararTurnstileAvistamiento() {
    const caja = document.querySelector('.turnstile-box');
    const widget = document.querySelector('.cf-turnstile');
    if (!caja || !widget) return;

    if (esEntornoLocalAvistamiento()) {
        // En local se evita cargar Cloudflare para no generar errores de origen; el backend decide si valida CAPTCHA.
        widget.remove();
        const aviso = document.createElement('div');
        aviso.className = 'turnstile-local-note';
        aviso.textContent = 'Verificacion simulada en entorno local. En produccion se carga Cloudflare Turnstile con llaves reales.';
        caja.appendChild(aviso);
        return;
    }

    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js';
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);
}

function configurarRangoFechaAvistamiento() {
    const fecha = document.getElementById('fecha');
    if (!fecha) return;
    aplicarRangoFechaAvistamiento(fecha);
}

function aplicarRangoFechaAvistamiento(input) {
    // La propuesta limita avistamientos a los ultimos 15 dias para evitar reportes obsoletos o futuros.
    const hoy = new Date();
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - 15);
    input.min = formatoFechaInputAvistamiento(inicio);
    input.max = formatoFechaInputAvistamiento(hoy);
}

function validarFechaPermitidaAvistamiento(valor) {
    if (!valor) {
        mostrarAvisoAvistamiento('Selecciona la fecha del avistamiento.', 'warn');
        return false;
    }
    const fecha = new Date(`${valor}T00:00:00`);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - 15);
    if (fecha < inicio || fecha > hoy) {
        mostrarAvisoAvistamiento('La fecha del avistamiento debe estar dentro de los ultimos 15 dias y no puede ser futura.', 'warn');
        return false;
    }
    return true;
}

function formatoFechaInputAvistamiento(fecha) {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
}

function cargarFolioDesdeUrl() {
    const folioInput = document.getElementById('folio');
    const parametros = new URLSearchParams(window.location.search);
    const folio = parametros.get('folio');
    if (folioInput && folio) {
        folioInput.value = folio;
    }
}

async function cargarUbicacionesAvistamiento() {
    try {
        // Consulta catalogos oficiales para evitar que el ciudadano escriba ubicaciones no normalizadas.
        const respuesta = await fetch(`${RUPE_AVISTAMIENTO_API}/catalogos/ubicaciones`, { credentials: 'include' });
        const contenido = await leerRespuestaAvistamiento(respuesta);
        if (!respuesta.ok) throw new Error(contenido.mensaje || 'No se pudieron cargar ubicaciones');
        configurarUbicacionesAvistamiento(contenido.estados || []);
    } catch (error) {
        // Aviso no bloqueante: puede ocurrir si el usuario abre la pagina antes de que Spring Boot termine de iniciar.
        if (typeof mostrarMensaje === 'function') {
            mostrarMensaje('No se pudieron cargar los catalogos de ubicacion. Verifica que Spring Boot este activo.', 'warn');
        }
    }
}

function configurarUbicacionesAvistamiento(estados) {
    const estadoSelect = document.getElementById('id_estado');
    const municipioSelect = document.getElementById('id_municipio');
    const coloniaSelect = document.getElementById('id_colonia');
    const cpInput = document.getElementById('cp');
    if (!estadoSelect || !municipioSelect || !coloniaSelect || !cpInput) return;

    estadoSelect.innerHTML = '<option value="">Selecciona un estado</option>';
    municipioSelect.innerHTML = '<option value="">Primero selecciona un estado</option>';
    coloniaSelect.innerHTML = '<option value="">Primero selecciona municipio/alcaldia</option>';
    cpInput.value = '';

    estados.forEach((estado) => {
        const option = document.createElement('option');
        option.value = estado.id;
        option.textContent = estado.nombre;
        estadoSelect.appendChild(option);
    });

    estadoSelect.addEventListener('change', () => {
        const estado = estados.find(item => String(item.id) === estadoSelect.value);
        municipioSelect.innerHTML = '<option value="">Selecciona municipio/alcaldia</option>';
        coloniaSelect.innerHTML = '<option value="">Primero selecciona municipio/alcaldia</option>';
        cpInput.value = '';
        (estado?.municipios || []).forEach((municipio) => {
            const option = document.createElement('option');
            option.value = municipio.id;
            option.textContent = municipio.nombre;
            municipioSelect.appendChild(option);
        });
    });

    municipioSelect.addEventListener('change', () => {
        const estado = estados.find(item => String(item.id) === estadoSelect.value);
        const municipio = (estado?.municipios || []).find(item => String(item.id) === municipioSelect.value);
        coloniaSelect.innerHTML = '<option value="">Selecciona colonia</option>';
        cpInput.value = '';
        (municipio?.colonias || []).forEach((colonia) => {
            const option = document.createElement('option');
            option.value = colonia.id;
            option.textContent = colonia.nombre;
            option.dataset.cp = colonia.codigoPostal || '';
            coloniaSelect.appendChild(option);
        });
    });

    coloniaSelect.addEventListener('change', () => {
        const opcion = coloniaSelect.options[coloniaSelect.selectedIndex];
        cpInput.value = opcion?.dataset.cp || '';
    });
}

function prepararResguardo() {
    const radios = document.querySelectorAll('input[name="resguardado"]');
    const contacto = document.querySelector('.contacto-resguardo');

    function actualizar() {
        // Los datos de contacto solo son obligatorios cuando el ciudadano tiene al perrito resguardado.
        const seleccionado = document.querySelector('input[name="resguardado"]:checked');
        const resguardado = seleccionado && seleccionado.value === 'si';
        if (contacto) {
            contacto.style.display = resguardado ? 'block' : 'none';
        }
        ['nombre_resguardante', 'correo_resguardante', 'telefono_resguardante'].forEach((id) => {
            const campo = document.getElementById(id);
            if (campo) campo.required = Boolean(resguardado);
        });

        if (resguardado && !avisoResguardoMostrado) {
            avisoResguardoMostrado = true;
            mostrarVentanaAvistamiento({
                titulo: 'Resguardo responsable',
                mensaje: 'Antes de entregar al perrito, solicita evidencia: fotos anteriores, cartilla o comprobante veterinario, señas particulares, collar, fecha y lugar de extravio. No entregues al perrito solo por gusto o insistencia; usa RUPE para dejar trazabilidad del reclamo.',
                tipo: 'warn',
                textoBoton: 'Entendido'
            });
        }
    }

    radios.forEach((radio) => radio.addEventListener('change', actualizar));
    actualizar();
}

function prepararEnvioAvistamiento() {
    const form = document.getElementById('formAvistamiento');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const invalido = Array.from(form.querySelectorAll('[required]'))
            .find(field => !field.value || (field.type === 'checkbox' && !field.checked));
        if (invalido) {
            invalido.focus();
            mostrarAvisoAvistamiento('Revisa los campos obligatorios antes de enviar.', 'warn');
            return;
        }

        if (!validarFechaPermitidaAvistamiento(form.fecha.value)) {
            return;
        }
        if (!validarFotoAvistamiento(form.foto_avistamiento)) {
            return;
        }
        if (!validarDescripcionAvistamiento(form.descripcion?.value || '')) {
            form.descripcion?.focus();
            return;
        }
        if (!validarContactoResguardo(form)) {
            return;
        }

        const token = document.querySelector('[name="cf-turnstile-response"]')?.value || '';
        if (!token && !esEntornoLocalAvistamiento()) {
            // CAPTCHA obligatorio porque este formulario es publico y puede recibir intentos automatizados.
            await mostrarAvisoAvistamiento('Completa la verificacion de seguridad antes de enviar.', 'warn');
            return;
        }

        const boton = form.querySelector('button[type="submit"]');
        const textoOriginal = boton?.textContent || '';

        try {
            if (boton) {
                boton.disabled = true;
                boton.textContent = 'Enviando reporte...';
            }

            const datos = new FormData(form);
            // Se envian ids de catalogo; el backend resuelve nombres y codigo postal por relacion.
            datos.set('idEstado', valorSeleccionadoAvistamiento('id_estado'));
            datos.set('idMunicipio', valorSeleccionadoAvistamiento('id_municipio'));
            datos.set('idColonia', valorSeleccionadoAvistamiento('id_colonia'));
            // La descripcion contiene senas visibles del perrito; se usa tambien para buscar avisos sin folio.
            datos.set('descripcion', form.descripcion?.value.trim() || '');
            datos.set('nombreResguardante', form.nombre_resguardante?.value.trim() || '');
            datos.set('correoResguardante', form.correo_resguardante?.value.trim() || '');
            datos.set('telefonoResguardante', form.telefono_resguardante?.value.trim() || '');
            datos.set('turnstileToken', token || 'local-dev-turnstile');

            const respuesta = await fetch(`${RUPE_AVISTAMIENTO_API}/avistamientos`, {
                method: 'POST',
                body: datos,
                credentials: 'include'
            });
            const contenido = await leerRespuestaAvistamiento(respuesta);
            if (!respuesta.ok) {
                throw new Error(contenido.mensaje || contenido.error || 'No se pudo registrar el avistamiento.');
            }

            const mensajeExito = contenido.mensaje || 'Gracias. Tu avistamiento fue registrado correctamente. RUPE notificara al propietario si el aviso esta relacionado con un folio activo.';
            mostrarConfirmacionFinalAvistamiento({
                titulo: 'Avistamiento registrado',
                mensaje: `${mensajeExito}\n\nPor seguridad del usuario y del perrito, no entregues ni acuerdes traslados fuera de RUPE hasta que el propietario valide la coincidencia. Si tienes al perrito resguardado, solicita evidencia como fotos anteriores, cartilla veterinaria o senas particulares antes de cualquier entrega.`,
                textoBoton: 'Aceptar y volver a perritos extraviados',
                destino: 'perritos-extraviados.html'
            });
            window.setTimeout(() => {
                window.location.assign('perritos-extraviados.html');
            }, 8000);
            return;
        } catch (error) {
            mostrarAvisoAvistamiento(error.message || 'No se pudo conectar con el backend.', 'error');
        } finally {
            if (boton) {
                boton.disabled = false;
                boton.textContent = textoOriginal;
            }
        }
    });
}

function mostrarConfirmacionFinalAvistamiento({ titulo, mensaje, textoBoton, destino }) {
    try {
        const destinoFinal = destino || 'perritos-extraviados.html';
        document.querySelector('.avistamiento-modal-overlay')?.remove();
        document.querySelector('.rupe-toast')?.remove();
        mostrarAvisoPersistenteAvistamiento(titulo, 'Registro correcto. Seras redirigido a perritos extraviados en unos segundos.');

        const overlay = document.createElement('div');
        overlay.className = 'avistamiento-modal-overlay avistamiento-modal-final';
        overlay.style.position = 'fixed';
        overlay.style.inset = '0';
        overlay.style.zIndex = '999999';
        overlay.style.display = 'flex';
        overlay.style.alignItems = 'center';
        overlay.style.justifyContent = 'center';
        overlay.style.padding = '20px';
        overlay.style.background = 'rgba(35, 0, 20, .68)';
        overlay.innerHTML = `
            <div class="avistamiento-modal success" role="dialog" aria-modal="true" aria-labelledby="avistamientoFinalTitulo">
                <i class="fas fa-circle-check"></i>
                <h2 id="avistamientoFinalTitulo">${escaparHtmlAvistamiento(titulo || 'Avistamiento registrado')}</h2>
                <p>${escaparHtmlAvistamiento(mensaje || '').replaceAll('\n', '<br>')}</p>
                <small style="display:block;margin-top:12px;color:#555;font-weight:700;">Redireccion automatica en unos segundos.</small>
                <button type="button">${escaparHtmlAvistamiento(textoBoton || 'Aceptar')}</button>
            </div>
        `;
        document.body.appendChild(overlay);

        const modal = overlay.querySelector('.avistamiento-modal');
        if (modal) {
            modal.style.width = 'min(540px, 100%)';
            modal.style.padding = '30px';
            modal.style.borderRadius = '14px';
            modal.style.background = '#fff';
            modal.style.textAlign = 'center';
            modal.style.boxShadow = '0 24px 60px rgba(0,0,0,.30)';
            modal.style.lineHeight = '1.6';
            modal.style.borderTop = '7px solid #1f8f4d';
        }

        const icono = overlay.querySelector('i');
        if (icono) {
            icono.style.color = '#1f8f4d';
            icono.style.fontSize = '3rem';
            icono.style.marginBottom = '12px';
        }

        const boton = overlay.querySelector('button');
        if (boton) {
            boton.style.width = '100%';
            boton.style.marginTop = '22px';
            boton.style.padding = '14px';
            boton.style.border = 'none';
            boton.style.borderRadius = '10px';
            boton.style.background = '#611232';
            boton.style.color = '#fff';
            boton.style.fontWeight = '700';
            boton.style.cursor = 'pointer';
            boton.focus();
            boton.addEventListener('click', () => {
                window.location.href = destinoFinal;
            });
        }

        window.setTimeout(() => {
            window.location.href = destinoFinal;
        }, 8000);
    } catch (error) {
        window.location.href = destino || 'perritos-extraviados.html';
    }
}

function mostrarAvisoPersistenteAvistamiento(titulo, mensaje) {
    const form = document.getElementById('formAvistamiento');
    if (!form) return;
    document.getElementById('avistamientoExitoPersistente')?.remove();
    const aviso = document.createElement('div');
    aviso.id = 'avistamientoExitoPersistente';
    aviso.style.margin = '0 0 18px';
    aviso.style.padding = '16px';
    aviso.style.borderRadius = '12px';
    aviso.style.border = '1px solid #1f8f4d';
    aviso.style.borderLeft = '6px solid #1f8f4d';
    aviso.style.background = '#e9f8ef';
    aviso.style.color = '#0f5132';
    aviso.innerHTML = `
        <strong>${escaparHtmlAvistamiento(titulo || 'Avistamiento registrado')}</strong>
        <p style="margin:6px 0 0;line-height:1.5;">${escaparHtmlAvistamiento(mensaje || '')}</p>
    `;
    form.prepend(aviso);
    aviso.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function validarFotoAvistamiento(input) {
    const archivo = input?.files?.[0];
    if (!archivo) {
        input?.focus();
        mostrarAvisoAvistamiento('Agrega una fotografia clara del perrito para poder registrar el avistamiento.', 'warn');
        return false;
    }

    const tiposPermitidos = ['image/jpeg', 'image/png', 'image/webp'];
    if (!tiposPermitidos.includes(archivo.type)) {
        input.value = '';
        mostrarAvisoAvistamiento('La fotografia debe ser JPG, PNG o WebP.', 'warn');
        return false;
    }

    const pesoMb = archivo.size / (1024 * 1024);
    if (pesoMb > RUPE_AVISTAMIENTO_MAX_FOTO_MB) {
        mostrarAvisoAvistamiento('La fotografia sigue pesando mas de 5 MB despues de optimizarse. Intenta con otra imagen mas ligera.', 'warn');
        return false;
    }

    return true;
}

function validarContactoResguardo(form) {
    const resguardado = form.querySelector('input[name="resguardado"]:checked')?.value === 'si';
    const nombre = form.nombre_resguardante?.value.trim() || '';
    const correo = form.correo_resguardante?.value.trim() || '';
    const telefono = form.telefono_resguardante?.value.trim() || '';

    if (resguardado && (!nombre || !correo || !telefono)) {
        mostrarAvisoAvistamiento('Si tienes al perrito en resguardo, captura nombre, correo y telefono de contacto.', 'warn');
        return false;
    }
    if (correo && !correoValidoAvistamiento(correo)) {
        form.correo_resguardante?.focus();
        mostrarAvisoAvistamiento('Captura un correo de contacto valido. Ejemplo: nombre@dominio.com', 'warn');
        return false;
    }
    if (telefono && !/^\d{10}$/.test(telefono)) {
        form.telefono_resguardante?.focus();
        mostrarAvisoAvistamiento('El telefono debe contener 10 digitos numericos.', 'warn');
        return false;
    }
    return true;
}

function correoValidoAvistamiento(correo) {
    // Se pide dominio completo para evitar capturas incompletas como usuario@correo.co cuando se pretendia .com.
    const valor = String(correo || '').trim().toLowerCase();
    return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{3,}$/.test(valor);
}

function validarDescripcionAvistamiento(valor) {
    const descripcion = String(valor || '').trim();
    if (descripcion.length < 10) {
        mostrarAvisoAvistamiento('Describe al perrito con al menos 10 caracteres: color, collar, manchas o senas visibles.', 'warn');
        return false;
    }
    if (descripcion.length > 1000) {
        mostrarAvisoAvistamiento('La descripcion del perrito no debe superar 1000 caracteres.', 'warn');
        return false;
    }
    return true;
}

function textoSeleccionadoAvistamiento(id) {
    const select = document.getElementById(id);
    return select?.options[select.selectedIndex]?.textContent || '';
}

function valorSeleccionadoAvistamiento(id) {
    return document.getElementById(id)?.value || '';
}

function mostrarAvisoAvistamiento(mensaje, tipo) {
    // En este formulario publico los mensajes deben permanecer visibles hasta que el usuario confirme.
    return mostrarVentanaAvistamiento({
        titulo: tipo === 'error' ? 'No fue posible continuar' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function esEntornoLocalAvistamiento() {
    return ['localhost', '127.0.0.1'].includes(window.location.hostname);
}

function mostrarVentanaAvistamiento({ titulo, mensaje, tipo, textoBoton }) {
    return new Promise((resolve) => {
        const anterior = document.querySelector('.avistamiento-modal-overlay');
        if (anterior) anterior.remove();

        const overlay = document.createElement('div');
        overlay.className = 'avistamiento-modal-overlay';
        overlay.style.position = 'fixed';
        overlay.style.inset = '0';
        overlay.style.zIndex = '99999';
        overlay.style.display = 'flex';
        overlay.style.alignItems = 'center';
        overlay.style.justifyContent = 'center';
        overlay.style.padding = '20px';
        overlay.style.background = 'rgba(35, 0, 20, .58)';
        overlay.innerHTML = `
            <div class="avistamiento-modal ${tipo || 'info'}" role="dialog" aria-modal="true" aria-labelledby="avistamientoModalTitulo">
                <i class="fas ${tipo === 'success' ? 'fa-circle-check' : 'fa-triangle-exclamation'}"></i>
                <h2 id="avistamientoModalTitulo">${escaparHtmlAvistamiento(titulo || 'Aviso RUPE')}</h2>
                <p>${escaparHtmlAvistamiento(mensaje || '')}</p>
                <button type="button">${escaparHtmlAvistamiento(textoBoton || 'Aceptar')}</button>
            </div>
        `;
        document.body.appendChild(overlay);

        const modal = overlay.querySelector('.avistamiento-modal');
        if (modal) {
            modal.style.width = 'min(460px, 100%)';
            modal.style.padding = '28px';
            modal.style.borderRadius = '14px';
            modal.style.background = '#fff';
            modal.style.textAlign = 'center';
            modal.style.boxShadow = '0 20px 50px rgba(0,0,0,.24)';
        }

        const boton = overlay.querySelector('button');
        if (boton) {
            boton.style.width = '100%';
            boton.style.marginTop = '22px';
            boton.style.padding = '14px';
            boton.style.border = 'none';
            boton.style.borderRadius = '10px';
            boton.style.background = '#611232';
            boton.style.color = '#fff';
            boton.style.fontWeight = '700';
            boton.style.cursor = 'pointer';
        }
        boton?.focus();
        boton?.addEventListener('click', () => {
            overlay.remove();
            resolve();
        });
    });
}

function mostrarVentanaRedireccionAvistamiento({ titulo, mensaje, tipo, textoBoton, destino }) {
    return new Promise((resolve) => {
        const anterior = document.querySelector('.avistamiento-modal-overlay');
        if (anterior) anterior.remove();

        const destinoFinal = destino || 'perritos-extraviados.html';
        const overlay = document.createElement('div');
        overlay.className = 'avistamiento-modal-overlay';
        overlay.style.position = 'fixed';
        overlay.style.inset = '0';
        overlay.style.zIndex = '99999';
        overlay.style.display = 'flex';
        overlay.style.alignItems = 'center';
        overlay.style.justifyContent = 'center';
        overlay.style.padding = '20px';
        overlay.style.background = 'rgba(35, 0, 20, .58)';
        const mensajeHtml = escaparHtmlAvistamiento(mensaje || '').replaceAll('\n', '<br>');
        overlay.innerHTML = `
            <div class="avistamiento-modal ${tipo || 'info'}" role="dialog" aria-modal="true" aria-labelledby="avistamientoModalTitulo">
                <i class="fas ${tipo === 'success' ? 'fa-circle-check' : 'fa-triangle-exclamation'}"></i>
                <h2 id="avistamientoModalTitulo">${escaparHtmlAvistamiento(titulo || 'Aviso RUPE')}</h2>
                <p>${mensajeHtml}</p>
                <button type="button">${escaparHtmlAvistamiento(textoBoton || 'Aceptar')}</button>
            </div>
        `;
        document.body.appendChild(overlay);

        const modal = overlay.querySelector('.avistamiento-modal');
        if (modal) {
            modal.style.width = 'min(520px, 100%)';
            modal.style.padding = '28px';
            modal.style.borderRadius = '14px';
            modal.style.background = '#fff';
            modal.style.textAlign = 'center';
            modal.style.boxShadow = '0 20px 50px rgba(0,0,0,.24)';
            modal.style.lineHeight = '1.55';
        }

        const boton = overlay.querySelector('button');
        if (boton) {
            boton.style.width = '100%';
            boton.style.marginTop = '22px';
            boton.style.padding = '14px';
            boton.style.border = 'none';
            boton.style.borderRadius = '10px';
            boton.style.background = '#611232';
            boton.style.color = '#fff';
            boton.style.fontWeight = '700';
            boton.style.cursor = 'pointer';
        }
        boton?.focus();
        boton?.addEventListener('click', () => {
            overlay.remove();
            resolve();
            window.location.href = destinoFinal;
        });

        window.setTimeout(() => {
            if (document.body.contains(overlay)) {
                overlay.remove();
                resolve();
                window.location.href = destinoFinal;
            }
        }, 12000);
    });
}

function escaparHtmlAvistamiento(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function leerRespuestaAvistamiento(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}



