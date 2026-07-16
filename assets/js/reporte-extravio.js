const RUPE_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_BACKEND_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';
let mascotasReporte = [];

document.addEventListener('DOMContentLoaded', () => {
    // Inicializa el flujo del dueño: mascotas, ubicaciones, vista previa, recomendaciones y envio.
    cargarMascotasDelDueno();
    cargarCatalogosUbicacion();
    configurarRangoFechaReporte();
    prepararPreviewFotografia();
    prepararPreviewTexto();
    prepararEnvioReporte();
});

async function cargarMascotasDelDueno() {
    const selectMascota = document.getElementById('id_mascota');
    if (!selectMascota) return;

    selectMascota.innerHTML = '<option value="">Cargando perritos registrados...</option>';

    try {
        const respuesta = await fetch(`${RUPE_API_BASE}/mascotas/mis`, {
            method: 'GET',
            credentials: 'include'
        });
        const mascotas = await leerRespuesta(respuesta);
        if (!respuesta.ok) throw new Error(mascotas.mensaje || 'No se pudieron cargar tus perritos.');

        mascotasReporte = Array.isArray(mascotas) ? mascotas : [];
        selectMascota.innerHTML = '<option value="">Selecciona un perrito registrado</option>';
        if (mascotasReporte.length === 0) {
            selectMascota.innerHTML = '<option value="">Primero registra un perrito</option>';
            mostrarAvisoReporteExtravio('Primero registra un perrito para poder generar el reporte.', 'warn');
            return;
        }

        mascotasReporte.forEach((mascota) => {
            const option = document.createElement('option');
            option.value = mascota.idMascota;
            option.textContent = `${mascota.nombre} - ${mascota.raza || 'Sin raza'}`;
            option.dataset.nombre = mascota.nombre || '';
            option.dataset.foto = normalizarFotoUrl(mascota.fotoPrincipalUrl);
            selectMascota.appendChild(option);
        });

        selectMascota.addEventListener('change', actualizarPreviewMascotaSeleccionada);
        seleccionarMascotaDesdeUrl();
        actualizarPreviewMascotaSeleccionada();
    } catch (error) {
        selectMascota.innerHTML = '<option value="">No se pudieron cargar los perritos</option>';
        mostrarAvisoReporteExtravio(error.message || 'Verifica que el backend este encendido.', 'error');
    }
}

function seleccionarMascotaDesdeUrl() {
    const parametros = new URLSearchParams(window.location.search);
    const idMascota = parametros.get('idMascota');
    const selectMascota = document.getElementById('id_mascota');
    if (idMascota && selectMascota) {
        selectMascota.value = idMascota;
    }
}

function actualizarPreviewMascotaSeleccionada() {
    const selectMascota = document.getElementById('id_mascota');
    const opcion = selectMascota?.options[selectMascota.selectedIndex];
    const mascota = mascotasReporte.find(item => String(item.idMascota) === String(selectMascota?.value));
    const previewNombre = document.getElementById('previewNombre');

    if (previewNombre) {
        previewNombre.textContent = mascota?.nombre || opcion?.dataset.nombre || 'Nombre del perrito';
    }

    mostrarFotoPreview(mascota?.fotoPrincipalUrl || opcion?.dataset.foto || '');
}

async function cargarCatalogosUbicacion() {
    try {
        const respuesta = await fetch(`${RUPE_API_BASE}/catalogos/ubicaciones`, { credentials: 'include' });
        const contenido = await leerRespuesta(respuesta);
        if (!respuesta.ok) throw new Error(contenido.mensaje || 'No se pudieron cargar ubicaciones');
        configurarUbicaciones(contenido.estados || []);
    } catch (error) {
        mostrarAvisoReporteExtravio('No se pudieron cargar los catalogos de ubicacion.', 'error');
    }
}

function configurarUbicaciones(estados) {
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

    estadoSelect.onchange = () => {
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
        actualizarPreviewUbicacion();
    };

    municipioSelect.onchange = () => {
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
        actualizarPreviewUbicacion();
    };

    coloniaSelect.onchange = () => {
        const opcion = coloniaSelect.options[coloniaSelect.selectedIndex];
        cpInput.value = opcion?.dataset.cp || '';
        actualizarPreviewUbicacion();
    };
}

function prepararEnvioReporte() {
    const form = document.getElementById('formReporteExtravio');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const invalid = Array.from(form.querySelectorAll('[required]'))
            .find(field => !field.value || (field.type === 'checkbox' && !field.checked));
        if (invalid) {
            invalid.focus();
            mostrarAvisoReporteExtravio('Revisa los campos obligatorios antes de generar el reporte.', 'warn');
            return;
        }

        if (!validarFechaPermitida(form.fecha_extravio.value, 'La fecha de extravio debe estar dentro de los ultimos 15 dias y no puede ser futura.')) {
            return;
        }

        if (!(await confirmarVigenciaReporte())) {
            return;
        }

        const boton = form.querySelector('button[type="submit"]');
        const textoOriginal = boton?.textContent || '';
        try {
            if (boton) {
                boton.disabled = true;
                boton.textContent = 'Generando reporte...';
            }
            const datos = new FormData(form);
            datos.set('idMascota', form.id_mascota.value);
            datos.set('fechaExtravio', form.fecha_extravio.value);
            datos.set('tipoReporte', form.tipo_reporte?.value || 'EXTRAVIO');
            datos.set('idEstado', form.id_estado.value);
            datos.set('idMunicipio', form.id_municipio.value);
            datos.set('idColonia', form.id_colonia.value);

            const respuesta = await fetch(`${RUPE_API_BASE}/reportes`, {
                method: 'POST',
                body: datos,
                credentials: 'include'
            });
            const contenido = await leerRespuesta(respuesta);
            if (!respuesta.ok) throw new Error(contenido.mensaje || contenido.error || 'No se pudo registrar el reporte.');

            document.getElementById('previewFolio').textContent = `Folio: ${contenido.folio}`;
            await mostrarAvisoReporteExtravio(
                contenido.mensaje || `Reporte generado con folio ${contenido.folio}`,
                'success'
            );
            window.location.href = `detalle-reporte.html?id=${encodeURIComponent(contenido.idReporte)}`;
        } catch (error) {
            mostrarAvisoReporteExtravio(error.message || 'No se pudo conectar con el backend.', 'error');
        } finally {
            if (boton) {
                boton.disabled = false;
                boton.textContent = textoOriginal;
            }
        }
    });
}

function configurarRangoFechaReporte() {
    const fecha = document.getElementById('fecha_extravio');
    if (!fecha) return;
    aplicarRangoFecha(fecha);
}

function aplicarRangoFecha(input) {
    const hoy = new Date();
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - 15);
    input.min = formatoFechaInput(inicio);
    input.max = formatoFechaInput(hoy);
}

function validarFechaPermitida(valor, mensaje) {
    if (!valor) {
        mostrarAvisoReporteExtravio('Selecciona una fecha para continuar.', 'warn');
        return false;
    }
    const fecha = new Date(`${valor}T00:00:00`);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - 15);
    if (fecha < inicio || fecha > hoy) {
        mostrarAvisoReporteExtravio(mensaje, 'warn');
        return false;
    }
    return true;
}

function formatoFechaInput(fecha) {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
}

function prepararPreviewFotografia() {
    const fotoCartel = document.getElementById('foto_cartel');
    if (!fotoCartel) return;

    fotoCartel.addEventListener('change', () => {
        const archivo = fotoCartel.files[0];
        if (!archivo) {
            actualizarPreviewMascotaSeleccionada();
            return;
        }
        mostrarFotoPreview(URL.createObjectURL(archivo));
    });
}

function mostrarFotoPreview(url) {
    const previewFoto = document.getElementById('previewFoto');
    const previewFotoPlaceholder = document.getElementById('previewFotoPlaceholder');
    if (!previewFoto || !previewFotoPlaceholder) return;

    const fotoUrl = normalizarFotoUrl(url);
    if (!fotoUrl) {
        previewFoto.removeAttribute('src');
        previewFoto.style.display = 'none';
        previewFotoPlaceholder.style.display = 'flex';
        return;
    }

    previewFoto.src = fotoUrl;
    previewFoto.style.display = 'block';
    previewFotoPlaceholder.style.display = 'none';
}

function normalizarFotoUrl(url) {
    if (!url) return '';
    if (url.startsWith('blob:') || url.startsWith('http')) return url;
    return `${RUPE_BACKEND_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}

function prepararPreviewTexto() {
    const fecha = document.getElementById('fecha_extravio');
    const referencias = document.getElementById('referencias');
    const tipoReporte = document.getElementById('tipo_reporte');
    fecha?.addEventListener('change', () => {
        document.getElementById('previewFecha').textContent = fecha.value || 'Sin capturar';
    });
    referencias?.addEventListener('input', () => {
        document.getElementById('previewReferencias').textContent = referencias.value || 'Sin capturar';
    });
    tipoReporte?.addEventListener('change', actualizarAvisoTipoReporte);
    actualizarAvisoTipoReporte();
}

function actualizarPreviewUbicacion() {
    const previewUbicacion = document.getElementById('previewUbicacion');
    const estado = textoSeleccionado(document.getElementById('id_estado'));
    const municipio = textoSeleccionado(document.getElementById('id_municipio'));
    const colonia = textoSeleccionado(document.getElementById('id_colonia'));
    if (!previewUbicacion) return;

    const partes = [colonia, municipio, estado].filter(valor => valor && !valor.includes('Selecciona') && !valor.includes('Primero'));
    previewUbicacion.textContent = partes.length ? partes.join(', ') : 'Sin capturar';
}

function textoSeleccionado(select) {
    return select?.options[select.selectedIndex]?.textContent || '';
}

async function leerRespuesta(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}


function confirmarVigenciaReporte() {
    return mostrarConfirmacionReporteExtravio({
        titulo: 'Confirmar vigencia del reporte',
        mensaje: 'El reporte permanecera activo durante 30 dias. Si tu perrito sigue extraviado, podras renovar la busqueda desde tu cuenta. Si ya fue recuperado, deberas cerrar el reporte para mantener actualizada la plataforma.',
        textoConfirmar: 'Generar reporte',
        textoCancelar: 'Revisar datos'
    });
}

function actualizarAvisoTipoReporte() {
    const tipo = document.getElementById('tipo_reporte')?.value || 'EXTRAVIO';
    const alerta = document.querySelector('.fraud-alert p');
    if (!alerta) return;
    alerta.textContent = tipo === 'ROBO'
        ? 'Marcaste posible robo de mascota. RUPE puede ayudarte a documentar y difundir el caso, pero conserva evidencias y considera acudir a la autoridad competente. No realices pagos ni compartas datos sensibles fuera de la plataforma.'
        : 'RUPE es un sistema gratuito. No se solicitan recompensas, pagos ni depósitos por la recuperación de perritos. Evita compartir datos personales fuera de la plataforma.';
}

function mostrarAvisoReporteExtravio(mensaje, tipo = 'info') {
    return mostrarVentanaReporteExtravio({
        titulo: tipo === 'success' || tipo === 'ok' ? 'Operacion realizada' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarConfirmacionReporteExtravio({ titulo, mensaje, textoConfirmar, textoCancelar }) {
    return mostrarVentanaReporteExtravio({
        titulo,
        mensaje,
        tipo: 'warn',
        textoBoton: textoConfirmar || 'Aceptar',
        textoCancelar: textoCancelar || 'Cancelar',
        confirmacion: true
    });
}

function mostrarVentanaReporteExtravio({ titulo, mensaje, tipo, textoBoton, textoCancelar, confirmacion = false }) {
    return new Promise((resolve) => {
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
            <div class="dueno-modal" role="dialog" aria-modal="true" aria-labelledby="reporteExtravioModalTitulo">
                <i class="fas ${tipo === 'success' || tipo === 'ok' ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i>
                <h2 id="reporteExtravioModalTitulo">${escaparHtmlReporteExtravio(titulo || 'Aviso RUPE')}</h2>
                <p>${escaparHtmlReporteExtravio(mensaje || '')}</p>
                <div class="dueno-modal-actions">
                    ${confirmacion ? `<button type="button" class="btn-cancelar">${escaparHtmlReporteExtravio(textoCancelar || 'Cancelar')}</button>` : ''}
                    <button type="button" class="btn-confirmar">${escaparHtmlReporteExtravio(textoBoton || 'Aceptar')}</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        const modal = overlay.querySelector('.dueno-modal');
        if (modal) {
            modal.style.width = 'min(460px, 100%)';
            modal.style.padding = '28px';
            modal.style.borderRadius = '14px';
            modal.style.background = '#fff';
            modal.style.textAlign = 'center';
            modal.style.boxShadow = '0 20px 50px rgba(0,0,0,.24)';
        }

        const acciones = overlay.querySelector('.dueno-modal-actions');
        if (acciones) {
            acciones.style.display = 'grid';
            acciones.style.gridTemplateColumns = confirmacion ? '1fr 1fr' : '1fr';
            acciones.style.gap = '10px';
            acciones.style.marginTop = '22px';
        }

        overlay.querySelectorAll('button').forEach((boton) => {
            boton.style.padding = '14px';
            boton.style.borderRadius = '10px';
            boton.style.fontWeight = '700';
            boton.style.cursor = 'pointer';
        });

        const cancelar = overlay.querySelector('.btn-cancelar');
        const confirmar = overlay.querySelector('.btn-confirmar');
        if (cancelar) {
            cancelar.style.border = '1px solid #611232';
            cancelar.style.background = '#fff';
            cancelar.style.color = '#611232';
            cancelar.addEventListener('click', () => {
                overlay.remove();
                resolve(false);
            });
        }
        if (confirmar) {
            confirmar.style.border = 'none';
            confirmar.style.background = '#611232';
            confirmar.style.color = '#fff';
            confirmar.focus();
            confirmar.addEventListener('click', () => {
                overlay.remove();
                resolve(true);
            });
        }
    });
}

function escaparHtmlReporteExtravio(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
