/* =====================================================
   DASHBOARD USUARIO - RUPE
   Muestra resumen real del dueño y alertas pendientes.
===================================================== */

const RUPE_DASH_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', iniciarDashboardUsuario);

async function iniciarDashboardUsuario() {
    try {
        const [mascotas, reportes, avistamientos, notificaciones] = await Promise.all([
            consultarDashboard('/mascotas/mis'),
            consultarDashboard('/reportes/mis'),
            consultarDashboard('/avistamientos/mis'),
            consultarDashboard('/notificaciones/mis')
        ]);

        actualizarTarjetasDashboard(mascotas, reportes, avistamientos);
        pintarActividadReciente(reportes);
        mostrarAlertaPendientes(avistamientos, notificaciones);
    } catch (error) {
        if (typeof mostrarMensaje === 'function') {
            mostrarMensaje(error.message || 'No se pudo cargar el resumen del usuario.', 'error');
        }
    }
}

async function consultarDashboard(endpoint) {
    const respuesta = await fetch(`${RUPE_DASH_API_BASE}${endpoint}`, {
        credentials: 'include'
    });
    const contenido = await leerRespuestaDashboard(respuesta);
    if (!respuesta.ok) {
        throw new Error(contenido.mensaje || contenido.error || 'No se pudo consultar el resumen.');
    }
    return Array.isArray(contenido) ? contenido : [];
}

function actualizarTarjetasDashboard(mascotas, reportes, avistamientos) {
    const reportesActivos = reportes.filter((reporte) => normalizarTexto(reporte.estatus).includes('activo')).length;
    const recuperados = reportes.filter((reporte) => normalizarTexto(reporte.estatus).includes('recuper')).length;

    setStat('perritos', mascotas.length);
    setStat('reportesActivos', reportesActivos);
    setStat('avistamientos', avistamientos.length);
    setStat('recuperados', recuperados);
}

function pintarActividadReciente(reportes) {
    const tabla = document.querySelector('[data-dashboard-recent]');
    if (!tabla) return;

    tabla.querySelectorAll('.recent-row:not(.recent-head)').forEach((row) => row.remove());
    const recientes = reportes.slice(0, 3);

    if (recientes.length === 0) {
        tabla.insertAdjacentHTML('beforeend', `
            <div class="recent-row">
                <span>Sin reportes</span>
                <span>-</span>
                <span class="status review">Sin actividad</span>
                <span>Genera tu primer reporte de extravio.</span>
            </div>
        `);
        return;
    }

    recientes.forEach((reporte) => {
        const estatus = normalizarTexto(reporte.estatus);
        const clase = estatus.includes('recuper') ? 'recovered' : estatus.includes('activo') ? 'active' : 'review';
        tabla.insertAdjacentHTML('beforeend', `
            <div class="recent-row">
                <span>${escaparDashboard(reporte.folio || 'Sin folio')}</span>
                <span>${escaparDashboard(reporte.nombreMascota || 'Sin dato')}</span>
                <span class="status ${clase}">${escaparDashboard(reporte.estatus || 'Pendiente')}</span>
                <span>${escaparDashboard(reporte.mensaje || 'Reporte actualizado')}</span>
            </div>
        `);
    });
}

function mostrarAlertaPendientes(avistamientos, notificaciones) {
    const avisosPendientes = avistamientos.filter((aviso) => !aviso.validadoDueno && normalizarTexto(aviso.estatus).includes('pendiente')).length;
    const notificacionesPendientes = notificaciones.filter((notificacion) => normalizarTexto(notificacion.estatus) !== 'leida').length;
    const total = Math.max(avisosPendientes, notificacionesPendientes);

    if (total <= 0 || sessionStorage.getItem('rupeDashboardAlertaVista') === '1') {
        return;
    }

    sessionStorage.setItem('rupeDashboardAlertaVista', '1');
    const modal = document.createElement('div');
    modal.className = 'dashboard-alert-overlay';
    modal.innerHTML = `
        <div class="dashboard-alert-box">
            <i class="fas fa-bell"></i>
            <h2>Tienes avisos pendientes</h2>
            <p>RUPE encontro ${total} aviso(s) o notificacion(es) pendiente(s) de revisar.</p>
            <div class="dashboard-alert-actions">
                <button type="button" data-alert-close>Revisar despues</button>
                <a href="mis-avistamientos.html">Ver avistamientos</a>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    modal.querySelector('[data-alert-close]').addEventListener('click', () => modal.remove());
}

function setStat(nombre, valor) {
    const elemento = document.querySelector(`[data-dashboard-stat="${nombre}"]`);
    if (elemento) elemento.textContent = valor;
}

function normalizarTexto(valor) {
    return String(valor || '').toLowerCase();
}

async function leerRespuestaDashboard(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function escaparDashboard(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
