const RUPE_ADMIN_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', cargarDashboardAdmin);

async function cargarDashboardAdmin() {
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_API}/admin/resumen`, { credentials: 'include' });
        const datos = await leerRespuestaAdmin(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo cargar el panel administrativo.');

        // El panel muestra conteos agregados; no expone correos, telefonos ni datos personales.
        ['usuariosActivos', 'usuariosInactivos', 'mascotasRegistradas', 'reportesActivos', 'reportesExtravio', 'reportesRobo', 'reportesPendientesRenovacion', 'mascotasRecuperadas', 'avistamientos', 'avistamientosPendientesValidacion', 'usuariosBloqueados', 'visitasTotales', 'mensajesContactoPendientes']
            .forEach((campo) => pintarStatAdmin(campo, datos[campo]));
        pintarVisitasAdmin(datos.visitasPorPagina || []);
        pintarAlertasAdmin(datos);
        avisarMensajesContacto(datos.mensajesContactoPendientes || 0);
    } catch (error) {
        mostrarAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarAlertasAdmin(datos) {
    const contenedor = document.getElementById('alertasAdminPanel');
    if (!contenedor) return;
    const alertas = [
        { total: Number(datos.reportesPendientesRenovacion || 0), texto: 'reportes pendientes de renovacion o vencidos', href: 'admin-reportes.html', clase: 'warn' },
        { total: Number(datos.avistamientosPendientesValidacion || 0), texto: 'avistamientos pendientes de validacion del dueno', href: 'admin-estadisticas.html', clase: 'warn' },
        { total: Number(datos.mensajesContactoPendientes || 0), texto: 'mensajes de soporte pendientes', href: 'admin-soporte.html', clase: 'warn' },
        { total: Number(datos.usuariosBloqueados || 0), texto: 'cuentas bloqueadas temporalmente', href: 'admin-usuarios.html', clase: 'danger' }
    ].filter((alerta) => alerta.total > 0);

    if (!alertas.length) {
        contenedor.innerHTML = '<p class="admin-note">No hay alertas administrativas pendientes.</p>';
        return;
    }

    contenedor.innerHTML = alertas.map((alerta) => `
        <a class="admin-alert-item ${alerta.clase}" href="${alerta.href}">
            <strong>${alerta.total.toLocaleString('es-MX')}</strong>
            <span>${escaparAdmin(alerta.texto)}</span>
        </a>
    `).join('');
}

async function avisarMensajesContacto(total) {
    const cantidad = Number(total || 0);
    if (cantidad <= 0) return;
    const yaAvisado = sessionStorage.getItem('rupe_admin_soporte_avisado');
    if (yaAvisado === String(cantidad)) return;
    sessionStorage.setItem('rupe_admin_soporte_avisado', String(cantidad));
    const mensaje = cantidad === 1
        ? 'Tienes 1 mensaje nuevo de contacto pendiente de revisar.'
        : `Tienes ${cantidad} mensajes nuevos de contacto pendientes de revisar.`;
    const accion = await mostrarVentanaAdmin({
        titulo: 'Soporte pendiente',
        mensaje,
        tipo: 'info',
        acciones: [
            { texto: 'Revisar soporte' },
            { texto: 'Despues', secundario: true }
        ]
    });
    if (accion === 0) window.location.href = 'admin-soporte.html';
}

function pintarStatAdmin(campo, valor) {
    const elemento = document.querySelector(`[data-admin-stat="${campo}"]`);
    if (elemento) elemento.textContent = Number(valor || 0).toLocaleString('es-MX');
}

function pintarVisitasAdmin(visitas) {
    const tbody = document.getElementById('tablaVisitasAdmin');
    if (!tbody) return;
    if (!visitas.length) {
        tbody.innerHTML = '<tr><td colspan="2">Todavia no hay visitas registradas.</td></tr>';
        return;
    }
    tbody.innerHTML = visitas.map((visita) => `
        <tr>
            <td>${escaparAdmin(visita.pagina || 'Sin dato')}</td>
            <td>${Number(visita.total || 0).toLocaleString('es-MX')}</td>
        </tr>
    `).join('');
}

async function leerRespuestaAdmin(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function mostrarAdmin(mensaje, tipo) {
    return mostrarVentanaAdmin({
        titulo: tipo === 'success' ? 'Operacion realizada' : tipo === 'error' ? 'No fue posible continuar' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaAdmin({ titulo, mensaje, tipo = 'info', textoBoton = 'Aceptar', acciones = [] }) {
    return new Promise((resolve) => {
        document.querySelector('.admin-feedback-overlay')?.remove();
        const overlay = document.createElement('div');
        overlay.className = 'admin-feedback-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(17,24,39,.58);display:flex;align-items:center;justify-content:center;z-index:1200;padding:18px;';
        const color = tipo === 'success' ? '#198754' : tipo === 'error' ? '#b42318' : '#8a1538';
        const icono = tipo === 'success' ? 'fa-circle-check' : tipo === 'error' ? 'fa-circle-xmark' : 'fa-circle-info';
        const botones = acciones.length
            ? acciones.map((accion, index) => `<button type="button" data-accion="${index}" style="border-radius:8px;background:${accion.secundario ? '#fff' : color};color:${accion.secundario ? color : '#fff'};font-weight:700;padding:11px 18px;cursor:pointer;min-width:118px;border:${accion.secundario ? '1px solid ' + color : '0'};">${escaparAdmin(accion.texto)}</button>`).join('')
            : `<button type="button" data-aceptar style="border:0;border-radius:8px;background:${color};color:#fff;font-weight:700;padding:11px 22px;cursor:pointer;min-width:130px;">${escaparAdmin(textoBoton)}</button>`;
        overlay.innerHTML = `
            <div role="dialog" aria-modal="true" style="width:min(440px,100%);background:#fff;border-radius:10px;box-shadow:0 24px 70px rgba(0,0,0,.24);padding:26px;text-align:center;font-family:Montserrat,Arial,sans-serif;">
                <i class="fa-solid ${icono}" style="font-size:42px;color:${color};margin-bottom:14px;"></i>
                <h2 style="margin:0 0 10px;color:#2b2b2b;font-size:1.35rem;">${escaparAdmin(titulo)}</h2>
                <p style="margin:0 0 20px;color:#4b5563;line-height:1.55;">${escaparAdmin(mensaje)}</p>
                <div style="display:flex;gap:10px;justify-content:center;flex-wrap:wrap;">${botones}</div>
            </div>
        `;
        const cerrar = (valor = null) => { overlay.remove(); resolve(valor); };
        overlay.querySelector('[data-aceptar]')?.addEventListener('click', () => cerrar(true));
        overlay.querySelectorAll('[data-accion]').forEach((boton) => boton.addEventListener('click', () => cerrar(Number(boton.dataset.accion))));
        overlay.addEventListener('click', (event) => { if (event.target === overlay) cerrar(null); });
        document.body.appendChild(overlay);
        overlay.querySelector('button')?.focus();
    });
}

function escaparAdmin(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}