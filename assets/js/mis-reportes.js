/* =====================================================
   MIS REPORTES - RUPE
   Consulta los reportes reales del usuario activo.
===================================================== */

const RUPE_REPORTES_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

const buscarFolio = document.getElementById('buscarFolio');
const filtroEstatus = document.getElementById('filtroEstatus');
const filtroTipoReporte = document.getElementById('filtroTipoReporte');
const listaReportes = document.querySelector('.lista-reportes');
const sinResultados = document.getElementById('sinResultados');
let reportesUsuario = [];
let avistamientosPorReporte = new Map();

document.addEventListener('DOMContentLoaded', iniciarMisReportes);

function iniciarMisReportes() {
    buscarFolio?.addEventListener('input', filtrarReportes);
    filtroEstatus?.addEventListener('change', filtrarReportes);
    filtroTipoReporte?.addEventListener('change', filtrarReportes);
    cargarMisReportes();
}

async function cargarMisReportes() {
    try {
        if (listaReportes) {
            listaReportes.innerHTML = '<p class="reporte-cargando">Cargando reportes...</p>';
        }

        const respuesta = await fetch(`${RUPE_REPORTES_API_BASE}/reportes/mis`, {
            credentials: 'include'
        });
        const contenido = await leerRespuestaReportes(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudieron consultar los reportes.');
        }

        reportesUsuario = Array.isArray(contenido) ? contenido : [];
        await consultarAvistamientosParaConteo();
        pintarReportes(reportesUsuario);
        filtrarReportes();
    } catch (error) {
        if (listaReportes) listaReportes.innerHTML = '';
        mostrarAvisoReportes(error.message || 'No se pudo conectar con el backend.', 'error');
        actualizarSinResultados(true);
    }
}

async function consultarAvistamientosParaConteo() {
    avistamientosPorReporte = new Map();
    try {
        const respuesta = await fetch(`${RUPE_REPORTES_API_BASE}/avistamientos/mis`, {
            credentials: 'include'
        });
        const avistamientos = await leerRespuestaReportes(respuesta);
        if (!respuesta.ok || !Array.isArray(avistamientos)) return;

        avistamientos.forEach((aviso) => {
            if (!aviso.idReporte) return;
            const total = avistamientosPorReporte.get(aviso.idReporte) || 0;
            avistamientosPorReporte.set(aviso.idReporte, total + 1);
        });
    } catch (error) {
        // El conteo es informativo; si falla, el listado de reportes sigue funcionando.
    }
}

function pintarReportes(reportes) {
    if (!listaReportes) return;
    listaReportes.innerHTML = '';

    reportes.forEach((reporte) => {
        const estatus = normalizarEstatus(reporte.estatus);
        const pendienteRenovacion = requiereReactivacionReporte(reporte);
        const item = document.createElement('article');
        item.className = 'reporte-item';
        item.dataset.folio = (reporte.folio || '').toLowerCase();
        item.dataset.estatus = estatus;
        item.dataset.nombre = normalizarTextoReporte(reporte.nombreMascota || '');
        item.dataset.tipo = normalizarTipoReporte(reporte.tipoReporte);

        item.innerHTML = `
            <div class="reporte-icono ${estatus}">
                <i class="fas ${iconoEstatus(estatus)}"></i>
            </div>

            <div class="reporte-datos">
                <span class="estatus estatus-${estatus}">${textoEstatus(reporte.estatus)}</span>
                ${pendienteRenovacion ? '<span class="estatus estatus-vencido">Publicacion vencida</span>' : ''}
                <h3>${escapar(reporte.folio || 'Sin folio')}</h3>
                <p><strong>Tipo de reporte:</strong> ${escapar(etiquetaTipoReporte(reporte.tipoReporte))}</p>
                <p><strong>Perrito:</strong> ${escapar(reporte.nombreMascota || 'Sin dato')}</p>
                <p><strong>Fecha de extravio:</strong> ${formatearFecha(reporte.fechaExtravio)}</p>
                <p><strong>Zona:</strong> ${escapar(zonaReporte(reporte))}</p>
                <p><strong>Avistamientos:</strong> ${avistamientosPorReporte.get(reporte.idReporte) || 0} registrados</p>
                <p><strong>Vigencia:</strong> ${textoVigenciaReporte(reporte)}</p>
            </div>

            <div class="reporte-acciones">
                <a href="detalle-reporte.html?id=${encodeURIComponent(reporte.idReporte)}" class="btn-accion">
                    <i class="fas fa-eye"></i>
                    Ver detalle
                </a>
                ${estatus === 'activo' ? `<button type="button" class="btn-accion btn-pdf" data-id="${encodeURIComponent(reporte.idReporte)}" data-folio="${escaparAtributo(reporte.folio || '')}">
                    <i class="fas fa-file-pdf"></i>
                    Descargar cartel
                </button>` : `<button type="button" class="btn-accion btn-pdf" disabled title="Cartel no disponible para reportes recuperados">
                    <i class="fas fa-lock"></i>
                    Cartel cerrado
                </button>`}

                <a href="mis-avistamientos.html?idReporte=${encodeURIComponent(reporte.idReporte)}" class="btn-accion btn-aviso">
                    <i class="fas fa-location-dot"></i>
                    Ver avistamientos
                </a>

                ${estatus === 'activo' && pendienteRenovacion ? `
                    <button type="button" class="btn-accion btn-renovar" data-id="${encodeURIComponent(reporte.idReporte)}">
                        <i class="fas fa-rotate"></i>
                        Reactivar publicacion
                    </button>
                ` : ''}

                ${estatus === 'activo' ? `
                    <button type="button" class="btn-accion btn-recuperado" data-id="${encodeURIComponent(reporte.idReporte)}">
                        <i class="fas fa-circle-check"></i>
                        Marcar recuperado
                    </button>
                ` : ''}
            </div>
        `;

        listaReportes.appendChild(item);
    });

    listaReportes.querySelectorAll('.btn-pdf').forEach((boton) => {
        boton.addEventListener('click', () => descargarPDF(boton.dataset.id, boton.dataset.folio));
    });
    listaReportes.querySelectorAll('.btn-renovar').forEach((boton) => {
        boton.addEventListener('click', () => renovarBusqueda(boton.dataset.id, boton));
    });
    listaReportes.querySelectorAll('.btn-recuperado').forEach((boton) => {
        boton.addEventListener('click', () => marcarRecuperado(boton.dataset.id, boton));
    });
}

async function renovarBusqueda(idReporte, boton) {
    const confirmar = await mostrarConfirmacionReportes('Reactivar publicacion', 'Confirma que tu perrito sigue extraviado y deseas reactivar la publicacion por 30 dias mas.', 'Reactivar publicacion');
    if (!confirmar || boton?.disabled) return;

    const textoOriginal = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Renovando...';

    try {
        const respuesta = await fetch(`${RUPE_REPORTES_API_BASE}/reportes/${encodeURIComponent(idReporte)}/renovar`, {
            method: 'PUT',
            credentials: 'include'
        });
        const contenido = await leerRespuestaReportes(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo renovar el reporte.');
        }
        mostrarAvisoReportes(contenido.mensaje || 'Busqueda renovada correctamente.', 'success');
        await cargarMisReportes();
    } catch (error) {
        boton.disabled = false;
        boton.innerHTML = textoOriginal;
        mostrarAvisoReportes(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function marcarRecuperado(idReporte, boton) {
    const confirmar = await mostrarConfirmacionReportes('Confirmar recuperacion', 'Confirma que el perrito fue recuperado. El reporte quedara como recuperado, se cerrara el seguimiento publico y ya no podra descargarse nuevamente el cartel.', 'Marcar recuperado');
    if (!confirmar || boton?.disabled) return;

    const textoOriginal = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cerrando...';

    try {
        const respuesta = await fetch(`${RUPE_REPORTES_API_BASE}/reportes/${encodeURIComponent(idReporte)}/recuperar`, {
            method: 'PUT',
            credentials: 'include'
        });
        const contenido = await leerRespuestaReportes(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo cerrar el reporte.');
        }
        mostrarAvisoReportes(contenido.mensaje || 'Reporte marcado como recuperado.', 'success');
        await cargarMisReportes();
    } catch (error) {
        boton.disabled = false;
        boton.innerHTML = textoOriginal;
        mostrarAvisoReportes(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function filtrarReportes() {
    const textoFolio = normalizarTextoReporte(buscarFolio?.value || '');
    const estatusSeleccionado = filtroEstatus?.value || '';
    const tipoSeleccionado = filtroTipoReporte?.value || '';
    const reportes = document.querySelectorAll('.reporte-item');
    let totalVisibles = 0;

    reportes.forEach((reporte) => {
        const folio = normalizarTextoReporte(reporte.dataset.folio || '');
        const nombre = reporte.dataset.nombre || '';
        const tipo = reporte.dataset.tipo || '';
        const estatus = reporte.dataset.estatus || '';
        // La busqueda no consulta datos privados: solo folio, nombre visible, tipo y estatus del reporte.
        const coincideFolio = folio.includes(textoFolio) || nombre.includes(textoFolio);
        const coincideEstatus = estatusSeleccionado === '' || estatus === estatusSeleccionado;
        const coincideTipo = tipoSeleccionado === '' || tipo === tipoSeleccionado;

        if (coincideFolio && coincideEstatus && coincideTipo) {
            reporte.style.display = 'grid';
            totalVisibles++;
        } else {
            reporte.style.display = 'none';
        }
    });

    actualizarSinResultados(totalVisibles === 0);
}

function limpiarFiltros() {
    if (buscarFolio) buscarFolio.value = '';
    if (filtroEstatus) filtroEstatus.value = '';
    if (filtroTipoReporte) filtroTipoReporte.value = '';
    filtrarReportes();
}

async function descargarPDF(idReporte, folio) {
    const reporte = reportesUsuario.find((item) => String(item.idReporte) === String(idReporte));
    if (reporte && normalizarEstatus(reporte.estatus) !== 'activo') {
        mostrarAvisoReportes('El cartel solo puede descargarse mientras el reporte esta activo. Este reporte ya fue cerrado o recuperado.', 'error');
        return;
    }
    if (!idReporte) {
        mostrarAvisoReportes('No se encontro el identificador del reporte.', 'error');
        return;
    }

    try {
        const respuesta = await fetch(`${RUPE_REPORTES_API_BASE}/reportes/${encodeURIComponent(idReporte)}/cartel`, {
            credentials: 'include'
        });

        if (!respuesta.ok) {
            const contenido = await leerRespuestaReportes(respuesta);
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo descargar el cartel PDF.');
        }

        const archivo = await respuesta.blob();
        const url = URL.createObjectURL(archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `cartel-${folio || idReporte}.pdf`;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(url);
        mostrarAvisoReportes('Cartel PDF descargado correctamente.', 'success');
    } catch (error) {
        mostrarAvisoReportes(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function actualizarSinResultados(mostrar) {
    if (sinResultados) {
        sinResultados.style.display = mostrar ? 'block' : 'none';
    }
}

function normalizarEstatus(estatus) {
    const texto = (estatus || '').toLowerCase();
    if (texto.includes('recuper')) return 'recuperado';
    if (texto.includes('cerr')) return 'cerrado';
    return 'activo';
}

function textoEstatus(estatus) {
    const normalizado = normalizarEstatus(estatus);
    return normalizado.charAt(0).toUpperCase() + normalizado.slice(1);
}

function iconoEstatus(estatus) {
    if (estatus === 'recuperado') return 'fa-circle-check';
    if (estatus === 'cerrado') return 'fa-lock';
    return 'fa-triangle-exclamation';
}

function requiereReactivacionReporte(reporte) {
    if (normalizarEstatus(reporte.estatus) !== 'activo') return false;
    if (reporte.requiereRenovacion) return true;
    if (reporte.diasParaVencer == null) return false;
    return Number(reporte.diasParaVencer) <= 0;
}
function textoVigenciaReporte(reporte) {
    if (normalizarEstatus(reporte.estatus) !== 'activo') return 'Reporte cerrado';
    if (requiereReactivacionReporte(reporte)) return 'Publicacion vencida, pendiente de reactivar';
    if (reporte.diasParaVencer == null) return 'Sin dato';
    if (reporte.diasParaVencer < 0) return 'Vencido';
    if (reporte.diasParaVencer === 0) return 'Vence hoy';
    return `${reporte.diasParaVencer} dias restantes`;
}

function etiquetaTipoReporte(tipoReporte) {
    return String(tipoReporte || '').toUpperCase() === 'ROBO' ? 'Robo de mascota' : 'Extravio o perdida';
}

function normalizarTipoReporte(tipoReporte) {
    return String(tipoReporte || '').toUpperCase() === 'ROBO' ? 'robo' : 'extravio';
}

function normalizarTextoReporte(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function zonaReporte(reporte) {
    return [reporte.colonia, reporte.municipio, reporte.estado].filter(Boolean).join(', ') || 'Sin ubicacion';
}

function formatearFecha(fecha) {
    if (!fecha) return 'Sin dato';
    const fechaObjeto = new Date(`${fecha}T00:00:00`);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin dato';
    return fechaObjeto.toLocaleDateString('es-MX');
}

function escapar(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function escaparAtributo(valor) {
    return escapar(valor).replaceAll('`', '&#096;');
}

function mostrarAvisoReportes(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}

async function leerRespuestaReportes(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function mostrarConfirmacionReportes(titulo, mensaje, textoConfirmar) {
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
            <div class="dueno-modal" role="dialog" aria-modal="true" style="width:min(460px,100%);padding:28px;border-radius:14px;background:#fff;text-align:center;box-shadow:0 20px 50px rgba(0,0,0,.24)">
                <i class="fas fa-circle-question"></i>
                <h2>${escapar(titulo || 'Confirmar accion')}</h2>
                <p>${escapar(mensaje || '')}</p>
                <button type="button" class="btn-confirmar-modal" style="width:100%;margin-top:22px;padding:14px;border:0;border-radius:10px;background:#611232;color:#fff;font-weight:700;cursor:pointer">${escapar(textoConfirmar || 'Aceptar')}</button>
                <button type="button" class="btn-cancelar-modal" style="width:100%;margin-top:12px;padding:14px;border:1px solid #b7a57a;border-radius:10px;background:#fff;color:#611232;font-weight:700;cursor:pointer">Cancelar</button>
            </div>`;
        document.body.appendChild(overlay);
        overlay.querySelector('.btn-confirmar-modal')?.addEventListener('click', () => { overlay.remove(); resolve(true); });
        overlay.querySelector('.btn-cancelar-modal')?.addEventListener('click', () => { overlay.remove(); resolve(false); });
    });
}



