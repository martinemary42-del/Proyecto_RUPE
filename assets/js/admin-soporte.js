const RUPE_ADMIN_SOPORTE_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const SOPORTE_POR_PAGINA = 10;
const SOPORTE_LIMITE_TEXTO = 500;

let mensajesSoporte = [];
let mensajesSoporteFiltrados = [];
let paginaSoporte = 1;
let mensajeSoporteEnRevision = null;

document.addEventListener('DOMContentLoaded', () => {
    configurarSoporteAdmin();
    cargarSoporteAdmin();
});

function configurarSoporteAdmin() {
    ['buscarSoporteAdmin', 'filtroEstatusSoporte', 'filtroAsuntoSoporte'].forEach((id) => {
        document.getElementById(id)?.addEventListener('input', () => {
            paginaSoporte = 1;
            aplicarFiltrosSoporte();
        });
    });
    document.getElementById('limpiarFiltrosSoporte')?.addEventListener('click', limpiarFiltrosSoporte);
    document.getElementById('soporteAnterior')?.addEventListener('click', () => cambiarPaginaSoporte(-1));
    document.getElementById('soporteSiguiente')?.addEventListener('click', () => cambiarPaginaSoporte(1));
    document.getElementById('exportarSoporteCsv')?.addEventListener('click', exportarSoporteCsv);
    document.getElementById('exportarSoporteExcel')?.addEventListener('click', exportarSoporteExcel);
    document.getElementById('cerrarModalRespuestaSoporte')?.addEventListener('click', cerrarModalRespuestaSoporte);
    document.getElementById('modalRespuestaSoporte')?.addEventListener('click', (event) => {
        if (event.target.id === 'modalRespuestaSoporte') cerrarModalRespuestaSoporte();
    });
    document.getElementById('formRespuestaSoporte')?.addEventListener('submit', guardarRespuestaSoporte);
    document.getElementById('respuestaAdminSoporte')?.addEventListener('input', actualizarContadorRespuestaSoporte);
}

async function cargarSoporteAdmin() {
    const tbody = document.getElementById('tablaSoporteAdmin');
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_SOPORTE_API}/admin/soporte`, { credentials: 'include' });
        const datos = await leerRespuestaSoporte(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudieron cargar los mensajes.');

        mensajesSoporte = Array.isArray(datos) ? datos : [];
        cargarAsuntosSoporte();
        aplicarFiltrosSoporte();
    } catch (error) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="6">${escaparSoporte(error.message || 'No se pudo conectar con el backend.')}</td></tr>`;
        actualizarContadorSoporte(0, 0);
        mostrarSoporteAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function cargarAsuntosSoporte() {
    const select = document.getElementById('filtroAsuntoSoporte');
    if (!select) return;
    const asuntos = [...new Set(mensajesSoporte.map((m) => m.asunto).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b, 'es'));
    select.innerHTML = '<option value="">Todos</option>' + asuntos
        .map((asunto) => `<option value="${escaparSoporte(asunto)}">${textoAsuntoSoporte(asunto)}</option>`)
        .join('');
}

function aplicarFiltrosSoporte() {
    const texto = normalizarSoporte(document.getElementById('buscarSoporteAdmin')?.value || '');
    const estatus = document.getElementById('filtroEstatusSoporte')?.value || '';
    const asunto = document.getElementById('filtroAsuntoSoporte')?.value || '';

    mensajesSoporteFiltrados = mensajesSoporte.filter((mensaje) => {
        const coincideTexto = !texto || normalizarSoporte([
            mensaje.nombre,
            mensaje.correo,
            mensaje.telefono,
            mensaje.asunto,
            mensaje.mensaje,
            mensaje.respuestaAdmin,
            mensaje.estatus
        ].join(' ')).includes(texto);
        const coincideEstatus = !estatus || String(mensaje.estatus || '').toUpperCase() === estatus;
        const coincideAsunto = !asunto || mensaje.asunto === asunto;
        return coincideTexto && coincideEstatus && coincideAsunto;
    });

    const totalPaginas = Math.max(1, Math.ceil(mensajesSoporteFiltrados.length / SOPORTE_POR_PAGINA));
    paginaSoporte = Math.min(paginaSoporte, totalPaginas);
    const inicio = (paginaSoporte - 1) * SOPORTE_POR_PAGINA;
    pintarSoporte(mensajesSoporteFiltrados.slice(inicio, inicio + SOPORTE_POR_PAGINA));
    actualizarPaginacionSoporte(totalPaginas);
    actualizarContadorSoporte(mensajesSoporteFiltrados.length, mensajesSoporte.length);
}

function pintarSoporte(mensajes) {
    // La tabla muestra resumenes para no romper el layout; el texto completo se abre en ventana modal.
    const tbody = document.getElementById('tablaSoporteAdmin');
    if (!tbody) return;
    if (!mensajes.length) {
        tbody.innerHTML = '<tr><td colspan="6">No hay mensajes con esos filtros.</td></tr>';
        return;
    }

    tbody.innerHTML = mensajes.map((mensaje) => {
        const atendido = String(mensaje.estatus || '').toUpperCase() === 'ATENDIDO';
        const respuesta = mensaje.respuestaAdmin ? `<small class="soporte-resumen-linea"><strong>Respuesta:</strong> ${escaparSoporte(recortarSoporte(mensaje.respuestaAdmin, 110))}</small>` : '<small class="admin-muted">Sin respuesta registrada</small>';
        return `
            <tr>
                <td>${formatearFechaSoporte(mensaje.fechaRegistro)}</td>
                <td>${escaparSoporte(mensaje.nombre)}<br><small>${escaparSoporte(mensaje.correo)}${mensaje.telefono ? ' · ' + escaparSoporte(mensaje.telefono) : ''}</small></td>
                <td>${textoAsuntoSoporte(mensaje.asunto)}</td>
                <td class="soporte-mensaje-cell">
                    <span>${escaparSoporte(recortarSoporte(mensaje.mensaje, 130))}</span>
                    ${respuesta}
                    <button type="button" class="soporte-link" data-soporte-ver="${mensaje.idMensajeContacto}">Ver completo</button>
                </td>
                <td><span class="status-pill ${atendido ? '' : 'warn'}">${escaparSoporte(mensaje.estatus)}</span></td>
                <td>
                    <button type="button" class="module-btn secondary admin-action-btn"
                        data-soporte-id="${mensaje.idMensajeContacto}" ${atendido ? 'disabled' : ''}>
                        ${atendido ? 'Atendido' : 'Responder'}
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    tbody.querySelectorAll('[data-soporte-id]').forEach((boton) => {
        boton.addEventListener('click', () => abrirModalRespuestaSoporte(boton.dataset.soporteId));
    });
    tbody.querySelectorAll('[data-soporte-ver]').forEach((boton) => {
        boton.addEventListener('click', () => abrirModalLecturaSoporte(boton.dataset.soporteVer));
    });
}

function abrirModalLecturaSoporte(idMensajeContacto) {
    const mensaje = mensajesSoporte.find((item) => String(item.idMensajeContacto) === String(idMensajeContacto));
    if (!mensaje) return;
    mostrarVentanaSoporte({
        titulo: 'Mensaje de soporte',
        mensaje: `Contacto: ${mensaje.nombre || 'Sin nombre'}\nCorreo: ${mensaje.correo || 'Sin correo'}\nTelefono: ${mensaje.telefono || 'No registrado'}\nAsunto: ${textoAsuntoSoportePlano(mensaje.asunto)}\n\nMensaje:\n${mensaje.mensaje || 'Sin mensaje'}\n\nRespuesta administrativa:\n${mensaje.respuestaAdmin || 'Sin respuesta registrada.'}`,
        tipo: 'info',
        textoBoton: 'Cerrar'
    });
}

function abrirModalRespuestaSoporte(idMensajeContacto) {
    const mensaje = mensajesSoporte.find((item) => String(item.idMensajeContacto) === String(idMensajeContacto));
    if (!mensaje) return;
    mensajeSoporteEnRevision = mensaje;
    document.getElementById('idMensajeRespuestaSoporte').value = mensaje.idMensajeContacto;
    document.getElementById('resumenMensajeSoporte').value = [
        `Contacto: ${mensaje.nombre || 'Sin nombre'}`,
        `Correo: ${mensaje.correo || 'Sin correo'}`,
        `Asunto: ${textoAsuntoSoportePlano(mensaje.asunto)}`,
        '',
        mensaje.mensaje || 'Sin mensaje'
    ].join('\n');
    document.getElementById('respuestaAdminSoporte').value = mensaje.respuestaAdmin || '';
    actualizarContadorRespuestaSoporte();
    document.getElementById('modalRespuestaSoporte').hidden = false;
    document.getElementById('respuestaAdminSoporte').focus();
}

function cerrarModalRespuestaSoporte() {
    const modal = document.getElementById('modalRespuestaSoporte');
    if (modal) modal.hidden = true;
    mensajeSoporteEnRevision = null;
}

async function guardarRespuestaSoporte(event) {
    event.preventDefault();
    // La respuesta administrativa queda limitada a 500 caracteres para conservar trazabilidad clara.
    const idMensajeContacto = document.getElementById('idMensajeRespuestaSoporte')?.value;
    const respuestaAdmin = document.getElementById('respuestaAdminSoporte')?.value || '';
    const limpia = respuestaAdmin.trim();
    if (limpia.length < 10) {
        mostrarSoporteAdmin('Para cerrar soporte escribe una respuesta o accion de al menos 10 caracteres.', 'warn');
        return;
    }
    if (limpia.length > SOPORTE_LIMITE_TEXTO) {
        mostrarSoporteAdmin(`La respuesta no debe superar ${SOPORTE_LIMITE_TEXTO} caracteres.`, 'warn');
        return;
    }
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_SOPORTE_API}/admin/soporte/${encodeURIComponent(idMensajeContacto)}/atendido`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ respuestaAdmin: limpia })
        });
        const datos = await leerRespuestaSoporte(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo actualizar el mensaje.');
        await mostrarSoporteAdmin('Mensaje marcado como atendido y respuesta registrada.', 'success');
        cerrarModalRespuestaSoporte();
        await cargarSoporteAdmin();
    } catch (error) {
        mostrarSoporteAdmin(error.message || 'No se pudo actualizar el mensaje.', 'error');
    }
}

function cambiarPaginaSoporte(delta) {
    paginaSoporte += delta;
    aplicarFiltrosSoporte();
}

function actualizarPaginacionSoporte(totalPaginas) {
    const contenedor = document.getElementById('paginacionSoporteAdmin');
    const texto = document.getElementById('paginaSoporteAdmin');
    const anterior = document.getElementById('soporteAnterior');
    const siguiente = document.getElementById('soporteSiguiente');
    if (!contenedor || !texto || !anterior || !siguiente) return;
    contenedor.hidden = mensajesSoporteFiltrados.length <= SOPORTE_POR_PAGINA;
    texto.textContent = `Pagina ${paginaSoporte} de ${totalPaginas}`;
    anterior.disabled = paginaSoporte <= 1;
    siguiente.disabled = paginaSoporte >= totalPaginas;
}

function actualizarContadorSoporte(totalFiltrado, totalGeneral) {
    const contador = document.getElementById('contadorSoporteAdmin');
    if (contador) contador.textContent = `Mostrando ${totalFiltrado} de ${totalGeneral} mensajes recibidos.`;
}

function actualizarContadorRespuestaSoporte() {
    const textarea = document.getElementById('respuestaAdminSoporte');
    const contador = document.getElementById('contadorRespuestaSoporte');
    if (contador && textarea) contador.textContent = `${textarea.value.length}/${SOPORTE_LIMITE_TEXTO} caracteres`;
}

function limpiarFiltrosSoporte() {
    ['buscarSoporteAdmin', 'filtroEstatusSoporte', 'filtroAsuntoSoporte'].forEach((id) => {
        const control = document.getElementById(id);
        if (control) control.value = '';
    });
    paginaSoporte = 1;
    aplicarFiltrosSoporte();
}

async function exportarSoporteCsv() {
    // El BOM permite que Excel abra correctamente acentos y columnas desde CSV.
    const encabezados = ['Fecha registro', 'Nombre', 'Correo', 'Telefono', 'Asunto', 'Mensaje recibido', 'Estatus', 'Fecha atencion', 'Respuesta administrativa'];
    const filas = mensajesSoporteFiltrados.map((m) => [
        formatearFechaSoporte(m.fechaRegistro),
        limpiarExportacionSoporte(m.nombre),
        limpiarExportacionSoporte(m.correo),
        limpiarExportacionSoporte(m.telefono),
        textoAsuntoSoportePlano(m.asunto),
        limpiarExportacionSoporte(m.mensaje),
        limpiarExportacionSoporte(m.estatus),
        formatearFechaSoporte(m.fechaAtencion),
        limpiarExportacionSoporte(m.respuestaAdmin)
    ]);
    const csv = [encabezados, ...filas].map((fila) => fila.map(celdaSoporteCsv).join(',')).join('\r\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'mensajes-soporte-rupe.csv';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
    await mostrarSoporteAdmin('CSV de soporte generado correctamente.', 'success');
}

async function exportarSoporteExcel() {
    window.location.href = `${RUPE_ADMIN_SOPORTE_API}/admin/soporte/excel`;
    await mostrarSoporteAdmin('Excel de soporte solicitado al backend.', 'success');
}

function textoAsuntoSoporte(asunto) {
    return escaparSoporte(textoAsuntoSoportePlano(asunto));
}

function textoAsuntoSoportePlano(asunto) {
    const mapa = {
        reporte: 'Duda sobre reporte',
        avistamiento: 'Duda sobre avistamiento',
        cuenta: 'Cuenta de usuario',
        sugerencia: 'Sugerencia'
    };
    return mapa[asunto] || asunto || 'Sin asunto';
}

function celdaSoporteCsv(valor) {
    return `"${String(valor ?? '').replaceAll('"', '""')}"`;
}

function limpiarExportacionSoporte(valor) {
    return String(valor || '').replace(/\s+/g, ' ').trim();
}

async function leerRespuestaSoporte(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function formatearFechaSoporte(valor) {
    if (!valor) return 'Sin fecha';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return 'Sin fecha';
    return fecha.toLocaleString('es-MX', { dateStyle: 'short', timeStyle: 'short' });
}

function mostrarSoporteAdmin(mensaje, tipo) {
    return mostrarVentanaSoporte({
        titulo: tipo === 'success' ? 'Operacion realizada' : tipo === 'error' ? 'No fue posible continuar' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaSoporte({ titulo, mensaje, tipo = 'info', textoBoton = 'Aceptar', acciones = [] }) {
    // Ventana unica para lectura, confirmaciones y mensajes de aceptacion del modulo soporte.
    return new Promise((resolve) => {
        document.querySelector('.soporte-modal-overlay')?.remove();
        const overlay = document.createElement('div');
        overlay.className = 'soporte-modal-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(17,24,39,.58);display:flex;align-items:center;justify-content:center;z-index:1200;padding:18px;';
        const color = tipo === 'success' ? '#198754' : tipo === 'error' ? '#b42318' : tipo === 'warn' ? '#8a6500' : '#8a1538';
        const icono = tipo === 'success' ? 'fa-circle-check' : tipo === 'error' ? 'fa-circle-xmark' : 'fa-circle-info';
        const botones = acciones.length
            ? acciones.map((accion, index) => `<button type="button" class="soporte-modal-accion" data-accion="${index}" style="border:0;border-radius:8px;background:${accion.secundario ? '#fff' : color};color:${accion.secundario ? color : '#fff'};font-weight:700;padding:11px 18px;cursor:pointer;min-width:118px;border:${accion.secundario ? '1px solid ' + color : '0'};">${escaparSoporte(accion.texto)}</button>`).join('')
            : `<button type="button" class="soporte-modal-aceptar" style="border:0;border-radius:8px;background:${color};color:#fff;font-weight:700;padding:11px 22px;cursor:pointer;min-width:130px;">${escaparSoporte(textoBoton)}</button>`;

        overlay.innerHTML = `
            <div role="dialog" aria-modal="true" aria-labelledby="soporteModalTitulo" style="width:min(520px,100%);max-height:88vh;overflow:auto;background:#fff;border-radius:10px;box-shadow:0 24px 70px rgba(0,0,0,.24);padding:26px;text-align:center;font-family:Montserrat,Arial,sans-serif;">
                <i class="fa-solid ${icono}" style="font-size:42px;color:${color};margin-bottom:14px;"></i>
                <h2 id="soporteModalTitulo" style="margin:0 0 10px;color:#2b2b2b;font-size:1.35rem;">${escaparSoporte(titulo)}</h2>
                <p style="white-space:pre-wrap;margin:0 0 20px;color:#4b5563;line-height:1.55;text-align:left;">${escaparSoporte(mensaje)}</p>
                <div style="display:flex;gap:10px;justify-content:center;flex-wrap:wrap;">${botones}</div>
            </div>
        `;

        const cerrar = (valor = null) => {
            overlay.remove();
            resolve(valor);
        };
        overlay.querySelector('.soporte-modal-aceptar')?.addEventListener('click', () => cerrar(true));
        overlay.querySelectorAll('.soporte-modal-accion').forEach((boton) => {
            boton.addEventListener('click', () => cerrar(Number(boton.dataset.accion)));
        });
        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) cerrar(null);
        });
        document.body.appendChild(overlay);
        overlay.querySelector('button')?.focus();
    });
}

function normalizarSoporte(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function recortarSoporte(valor, limite) {
    const texto = String(valor || '').replace(/\s+/g, ' ').trim();
    return texto.length > limite ? `${texto.slice(0, limite - 1)}...` : texto;
}

function escaparSoporte(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}