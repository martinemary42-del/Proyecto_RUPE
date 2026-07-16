const RUPE_ADMIN_REPORTES_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const REPORTES_POR_PAGINA = 10;

let reportesAdmin = [];
let reportesFiltrados = [];
let paginaReportes = 1;

document.addEventListener('DOMContentLoaded', () => {
    configurarReportesAdmin();
    cargarReportesAdmin();
});

function configurarReportesAdmin() {
    ['buscarReporteAdmin', 'filtroEstatusReporteAdmin', 'filtroVigenciaReporteAdmin'].forEach((id) => {
        document.getElementById(id)?.addEventListener('input', () => {
            paginaReportes = 1;
            aplicarFiltrosReportes();
        });
    });
    document.getElementById('limpiarFiltrosReportes')?.addEventListener('click', limpiarFiltrosReportes);
    document.getElementById('reportesAnterior')?.addEventListener('click', () => cambiarPaginaReportes(-1));
    document.getElementById('reportesSiguiente')?.addEventListener('click', () => cambiarPaginaReportes(1));
    document.getElementById('exportarReportesCsv')?.addEventListener('click', exportarReportesCsv);
    document.getElementById('exportarReportesExcel')?.addEventListener('click', exportarReportesExcel);
    document.getElementById('cerrarModalReporte')?.addEventListener('click', cerrarResumenReporte);
    document.getElementById('modalReporteAdmin')?.addEventListener('click', (event) => {
        if (event.target.id === 'modalReporteAdmin') cerrarResumenReporte();
    });
}

async function cargarReportesAdmin() {
    const tbody = document.getElementById('tablaReportesAdmin');
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_REPORTES_API}/admin/reportes`, { credentials: 'include' });
        const datos = await leerRespuestaReportesAdmin(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudieron cargar los reportes.');
        reportesAdmin = Array.isArray(datos) ? datos : [];
        aplicarFiltrosReportes();
    } catch (error) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="8">${escaparReporteAdmin(error.message || 'No se pudo conectar con el backend.')}</td></tr>`;
        mostrarReporteAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function aplicarFiltrosReportes() {
    const texto = normalizarReporteAdmin(document.getElementById('buscarReporteAdmin')?.value || '');
    const estatus = document.getElementById('filtroEstatusReporteAdmin')?.value || '';
    const vigencia = document.getElementById('filtroVigenciaReporteAdmin')?.value || '';

    reportesFiltrados = reportesAdmin.filter((reporte) => {
        const coincideTexto = !texto || normalizarReporteAdmin([
            reporte.folio, etiquetaTipoReporte(reporte.tipoReporte), reporte.mascota, reporte.raza, reporte.zona, reporte.estatus
        ].join(' ')).includes(texto);
        const coincideEstatus = !estatus || String(reporte.estatus || '').toUpperCase() === estatus;
        const coincideVigencia = !vigencia || estadoVigenciaReporte(reporte).clave === vigencia;
        return coincideTexto && coincideEstatus && coincideVigencia;
    });

    const totalPaginas = Math.max(1, Math.ceil(reportesFiltrados.length / REPORTES_POR_PAGINA));
    paginaReportes = Math.min(paginaReportes, totalPaginas);
    const inicio = (paginaReportes - 1) * REPORTES_POR_PAGINA;
    pintarReportesAdmin(reportesFiltrados.slice(inicio, inicio + REPORTES_POR_PAGINA));
    actualizarPaginacionReportes(totalPaginas);
    actualizarContadorReportes();
}

function pintarReportesAdmin(reportes) {
    const tbody = document.getElementById('tablaReportesAdmin');
    if (!tbody) return;
    if (!reportes.length) {
        tbody.innerHTML = '<tr><td colspan="8">No hay reportes con esos filtros.</td></tr>';
        return;
    }

    tbody.innerHTML = reportes.map((reporte) => {
        const vigencia = estadoVigenciaReporte(reporte);
        const estatus = String(reporte.estatus || 'SIN_ESTATUS');
        // El tipo permite separar casos de extravio y robo sin mostrar datos privados del dueno.
        return `
            <tr>
                <td>${escaparReporteAdmin(reporte.folio)}</td>
                <td>${escaparReporteAdmin(etiquetaTipoReporte(reporte.tipoReporte))}</td>
                <td>${escaparReporteAdmin(reporte.mascota)}<br><small>${escaparReporteAdmin(reporte.raza)}</small></td>
                <td>${escaparReporteAdmin(reporte.zona)}</td>
                <td><span class="status-pill ${claseEstatusReporte(estatus)}">${escaparReporteAdmin(estatus)}</span></td>
                <td><span class="status-pill ${vigencia.clase}">${vigencia.texto}</span></td>
                <td>${Number(reporte.avistamientos || 0).toLocaleString('es-MX')}</td>
                <td><button class="module-btn secondary admin-action-btn" data-reporte-id="${reporte.idReporte}">Ver resumen</button></td>
            </tr>
        `;
    }).join('');

    tbody.querySelectorAll('[data-reporte-id]').forEach((boton) => {
        boton.addEventListener('click', () => abrirResumenReporte(boton.dataset.reporteId));
    });
}

function abrirResumenReporte(idReporte) {
    const reporte = reportesAdmin.find((item) => String(item.idReporte) === String(idReporte));
    if (!reporte) return;
    const vigencia = estadoVigenciaReporte(reporte);
    const contenido = document.getElementById('contenidoResumenReporte');
    const modal = document.getElementById('modalReporteAdmin');
    if (!contenido || !modal) return;

    contenido.innerHTML = `
        <div class="admin-summary-list">
            <p><strong>Folio:</strong> ${escaparReporteAdmin(reporte.folio)}</p>
            <p><strong>Tipo:</strong> ${escaparReporteAdmin(etiquetaTipoReporte(reporte.tipoReporte))}</p>
            <p><strong>Perrito:</strong> ${escaparReporteAdmin(reporte.mascota)} (${escaparReporteAdmin(reporte.raza)})</p>
            <p><strong>Zona:</strong> ${escaparReporteAdmin(reporte.zona)}</p>
            <p><strong>Estatus:</strong> ${escaparReporteAdmin(reporte.estatus)}</p>
            <p><strong>Fecha de extravío:</strong> ${formatearFechaAdmin(reporte.fechaExtravio)}</p>
            <p><strong>Fecha de registro:</strong> ${formatearFechaAdmin(reporte.fechaRegistro)}</p>
            <p><strong>Vencimiento:</strong> ${formatearFechaAdmin(reporte.fechaVencimiento)} (${vigencia.texto})</p>
            <p><strong>Renovaciones:</strong> ${Number(reporte.renovaciones || 0)}</p>
            <p><strong>Avistamientos:</strong> ${Number(reporte.avistamientos || 0)}</p>
        </div>
        <p class="admin-note">Este resumen es solo de monitoreo. No muestra teléfono, correo ni datos privados del dueño.</p>
    `;
    modal.hidden = false;
}

function cerrarResumenReporte() {
    const modal = document.getElementById('modalReporteAdmin');
    if (modal) modal.hidden = true;
}

function estadoVigenciaReporte(reporte) {
    const estatus = String(reporte.estatus || '').toUpperCase();
    if (estatus !== 'ACTIVO') return { clave: 'VIGENTE', texto: 'No aplica', clase: '' };
    if (reporte.requiereRenovacion || Number(reporte.diasParaVencer) < 0) {
        return { clave: 'PENDIENTE', texto: 'Pendiente de renovación', clase: 'danger' };
    }
    if (Number(reporte.diasParaVencer) <= 5) {
        return { clave: 'POR_VENCER', texto: `${reporte.diasParaVencer} días`, clase: 'warn' };
    }
    return { clave: 'VIGENTE', texto: `${reporte.diasParaVencer} días`, clase: '' };
}

function claseEstatusReporte(estatus) {
    const valor = String(estatus || '').toUpperCase();
    if (valor.includes('RECUPER')) return '';
    if (valor.includes('CERR')) return 'danger';
    return 'warn';
}

function cambiarPaginaReportes(delta) {
    paginaReportes += delta;
    aplicarFiltrosReportes();
}

function actualizarPaginacionReportes(totalPaginas) {
    const contenedor = document.getElementById('paginacionReportesAdmin');
    const texto = document.getElementById('paginaReportesAdmin');
    const anterior = document.getElementById('reportesAnterior');
    const siguiente = document.getElementById('reportesSiguiente');
    if (!contenedor || !texto || !anterior || !siguiente) return;
    contenedor.hidden = reportesFiltrados.length <= REPORTES_POR_PAGINA;
    texto.textContent = `Página ${paginaReportes} de ${totalPaginas}`;
    anterior.disabled = paginaReportes <= 1;
    siguiente.disabled = paginaReportes >= totalPaginas;
}

function actualizarContadorReportes() {
    const contador = document.getElementById('contadorReportesAdmin');
    if (contador) contador.textContent = `Mostrando ${reportesFiltrados.length} de ${reportesAdmin.length} reportes registrados.`;
}

function limpiarFiltrosReportes() {
    ['buscarReporteAdmin', 'filtroEstatusReporteAdmin', 'filtroVigenciaReporteAdmin'].forEach((id) => {
        const control = document.getElementById(id);
        if (control) control.value = '';
    });
    paginaReportes = 1;
    aplicarFiltrosReportes();
}

function exportarReportesCsv() {
    const encabezados = ['Folio', 'Tipo', 'Mascota', 'Raza', 'Zona', 'Estatus', 'Vencimiento', 'Renovaciones', 'Avistamientos'];
    const filas = reportesFiltrados.map((r) => [
        r.folio, etiquetaTipoReporte(r.tipoReporte), r.mascota, r.raza, r.zona, r.estatus, r.fechaVencimiento, r.renovaciones, r.avistamientos
    ]);
    const csv = [encabezados, ...filas].map((fila) => fila.map(celdaCsv).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'reportes-rupe-admin.csv';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
    mostrarReporteAdmin('CSV de reportes generado correctamente.', 'success');
}

function exportarReportesExcel() {
    // Exportacion editable para revision academica: usa los reportes filtrados sin exponer datos privados del dueño.
    const encabezados = ['Folio', 'Tipo', 'Mascota', 'Raza', 'Zona', 'Estatus', 'Vencimiento', 'Renovaciones', 'Avistamientos'];
    const filas = reportesFiltrados.map((r) => [
        r.folio,
        etiquetaTipoReporte(r.tipoReporte),
        r.mascota,
        r.raza,
        r.zona,
        r.estatus,
        r.fechaVencimiento,
        r.renovaciones,
        r.avistamientos
    ]);
    const excel = generarExcelXmlReportes('Reportes RUPE', [encabezados, ...filas]);
    const blob = new Blob([excel], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'reportes-rupe-admin.xls';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
    mostrarReporteAdmin('Excel de reportes generado correctamente.', 'success');
}

function generarExcelXmlReportes(nombreHoja, filas) {
    const filasXml = filas.map((fila) => `<Row>${fila.map((celda) => `<Cell><Data ss:Type="String">${escaparXmlReporte(celda)}</Data></Cell>`).join('')}</Row>`).join('');
    return `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Worksheet ss:Name="${escaparXmlReporte(nombreHoja)}">
  <Table>${filasXml}</Table>
 </Worksheet>
</Workbook>`;
}

function escaparXmlReporte(valor) {
    return String(valor ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&apos;');
}

function celdaCsv(valor) {
    return `"${String(valor ?? '').replaceAll('"', '""')}"`;
}

async function leerRespuestaReportesAdmin(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function formatearFechaAdmin(valor) {
    if (!valor) return 'Sin dato';
    const fecha = String(valor).includes('T') ? new Date(valor) : new Date(`${valor}T00:00:00`);
    if (Number.isNaN(fecha.getTime())) return 'Sin dato';
    return fecha.toLocaleDateString('es-MX');
}

function mostrarReporteAdmin(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}

function normalizarReporteAdmin(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function etiquetaTipoReporte(tipoReporte) {
    return String(tipoReporte || '').toUpperCase() === 'ROBO' ? 'Robo de mascota' : 'Extravio o perdida';
}

function escaparReporteAdmin(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
