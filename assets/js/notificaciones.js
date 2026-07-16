/* =====================================================
   NOTIFICACIONES - RUPE
   Consulta avisos reales generados por el sistema.
===================================================== */

const RUPE_NOTIF_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

const filtroTipo = document.getElementById('filtroTipo');
const filtroEstado = document.getElementById('filtroEstado');
const listaNotificaciones = document.querySelector('.lista-notificaciones');
const sinResultados = document.getElementById('sinResultados');
let notificacionesUsuario = [];

document.addEventListener('DOMContentLoaded', iniciarNotificaciones);

function iniciarNotificaciones() {
    filtroTipo?.addEventListener('change', filtrarNotificaciones);
    filtroEstado?.addEventListener('change', filtrarNotificaciones);
    cargarNotificaciones();
}

async function cargarNotificaciones() {
    try {
        listaNotificaciones.innerHTML = '<p class="notificacion-cargando">Cargando notificaciones...</p>';
        const respuesta = await fetch(`${RUPE_NOTIF_API_BASE}/notificaciones/mis`, {
            credentials: 'include'
        });
        const contenido = await leerRespuestaNotificaciones(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudieron consultar las notificaciones.');
        }

        notificacionesUsuario = Array.isArray(contenido) ? contenido : [];
        pintarNotificaciones(notificacionesUsuario);
        filtrarNotificaciones();
    } catch (error) {
        listaNotificaciones.innerHTML = '';
        mostrarAvisoNotificaciones(error.message || 'No se pudo conectar con el backend.', 'error');
        actualizarSinResultados(true);
    }
}

function pintarNotificaciones(notificaciones) {
    listaNotificaciones.innerHTML = '';
    notificaciones.forEach((notificacion) => {
        const tipo = normalizarTipo(notificacion.tipo);
        const estado = normalizarEstado(notificacion.estatus);
        const item = document.createElement('article');
        item.className = `notificacion-item ${estado === 'no-leida' ? 'no-leida' : ''}`;
        item.dataset.tipo = tipo;
        item.dataset.estado = estado;

        item.innerHTML = `
            <div class="notificacion-icono icono-${tipo}">
                <i class="fas ${iconoTipo(tipo)}"></i>
            </div>

            <div class="notificacion-datos">
                <span class="estatus estatus-${estado}">${estado === 'no-leida' ? 'No leida' : 'Leida'}</span>
                <h3>${escapar(notificacion.asunto || 'Notificacion RUPE')}</h3>
                <p>${escapar(notificacion.mensaje || 'Sin mensaje')}</p>
                <small>${formatearFechaHora(notificacion.fechaEnvio)} · Folio ${escapar(notificacion.folio || 'Sin folio')}</small>
            </div>

            <div class="notificacion-acciones">
                <a href="mis-avistamientos.html?folio=${encodeURIComponent(notificacion.folio || '')}&idReporte=${encodeURIComponent(notificacion.idReporte || '')}&idNotificacion=${encodeURIComponent(notificacion.idNotificacion || '')}" class="btn-accion" data-ver-aviso="${notificacion.idNotificacion || ''}">
                    <i class="fas fa-eye"></i>
                    Ver aviso
                </a>
                ${estado === 'no-leida' ? `
                    <button type="button" class="btn-leida" data-id="${notificacion.idNotificacion}">
                        <i class="fas fa-check"></i>
                        Marcar leida
                    </button>
                ` : `
                    <button type="button" class="btn-leida" disabled>
                        <i class="fas fa-circle-check"></i>
                        Revisada
                    </button>
                `}
            </div>
        `;
        listaNotificaciones.appendChild(item);
    });
    listaNotificaciones.querySelectorAll('[data-ver-aviso]').forEach((enlace) => {
        enlace.addEventListener('click', async (event) => {
            const id = enlace.dataset.verAviso;
            if (!id) return;
            event.preventDefault();
            await marcarLeida(id, { silencioso: true });
            window.location.href = enlace.href;
        });
    });


    listaNotificaciones.querySelectorAll('[data-id]').forEach((boton) => {
        boton.addEventListener('click', () => marcarLeida(boton.dataset.id));
    });
}

function filtrarNotificaciones() {
    const tipoSeleccionado = filtroTipo?.value || '';
    const estadoSeleccionado = filtroEstado?.value || '';
    const notificaciones = document.querySelectorAll('.notificacion-item');
    let totalVisibles = 0;

    notificaciones.forEach((notificacion) => {
        const tipo = notificacion.dataset.tipo;
        const estado = notificacion.dataset.estado;
        const coincideTipo = tipoSeleccionado === '' || tipo === tipoSeleccionado;
        const coincideEstado = estadoSeleccionado === '' || estado === estadoSeleccionado;

        notificacion.style.display = coincideTipo && coincideEstado ? 'grid' : 'none';
        if (coincideTipo && coincideEstado) totalVisibles++;
    });

    actualizarSinResultados(totalVisibles === 0);
}

function limpiarFiltros() {
    if (filtroTipo) filtroTipo.value = '';
    if (filtroEstado) filtroEstado.value = '';
    filtrarNotificaciones();
}

async function marcarLeida(idNotificacion, opciones = {}) {
    try {
        const respuesta = await fetch(`${RUPE_NOTIF_API_BASE}/notificaciones/${encodeURIComponent(idNotificacion)}/leida`, {
            method: 'PUT',
            credentials: 'include'
        });
        const contenido = await leerRespuestaNotificaciones(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo marcar la notificacion.');
        }
        if (!opciones.silencioso) {
            mostrarAvisoNotificaciones('Notificacion marcada como leida.', 'success');
            await cargarNotificaciones();
        }
    } catch (error) {
        mostrarAvisoNotificaciones(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function actualizarSinResultados(mostrar) {
    if (sinResultados) sinResultados.style.display = mostrar ? 'block' : 'none';
}

function normalizarTipo(tipo) {
    const valor = String(tipo || '').toLowerCase();
    if (valor.includes('resguardo')) return 'resguardo';
    if (valor.includes('recuper')) return 'recuperacion';
    if (valor.includes('sistema')) return 'sistema';
    return 'avistamiento';
}

function normalizarEstado(estatus) {
    return String(estatus || '').toUpperCase() === 'LEIDA' ? 'leida' : 'no-leida';
}

function iconoTipo(tipo) {
    if (tipo === 'resguardo') return 'fa-house-circle-check';
    if (tipo === 'recuperacion') return 'fa-circle-check';
    if (tipo === 'sistema') return 'fa-envelope-circle-check';
    return 'fa-location-dot';
}

function formatearFechaHora(fecha) {
    if (!fecha) return 'Sin fecha';
    const fechaObjeto = new Date(fecha);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin fecha';
    return fechaObjeto.toLocaleString('es-MX', {
        dateStyle: 'short',
        timeStyle: 'short'
    });
}

function escapar(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function leerRespuestaNotificaciones(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function mostrarAvisoNotificaciones(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}




