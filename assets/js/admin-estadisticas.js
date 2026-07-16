const RUPE_ADMIN_ESTADISTICAS_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

let resumenEstadisticas = {};
let reportesEstadisticas = [];
let reportesFiltradosEstadisticas = [];
let zonasEstadisticas = [];

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('exportarEstadisticasCsv')?.addEventListener('click', () => exportarEstadisticas('csv'));
    document.getElementById('exportarEstadisticasExcel')?.addEventListener('click', exportarExcelBackend);
    document.getElementById('actualizarEstadisticas')?.addEventListener('click', cargarEstadisticasAdmin);
    ['buscarEstadisticasZona', 'filtroEstadisticasEstatus', 'fechaEstadisticasDesde', 'fechaEstadisticasHasta'].forEach((id) => {
        document.getElementById(id)?.addEventListener('input', aplicarFiltrosEstadisticas);
    });
    document.getElementById('limpiarFiltrosEstadisticas')?.addEventListener('click', limpiarFiltrosEstadisticas);
    cargarEstadisticasAdmin();
});

async function cargarEstadisticasAdmin() {
    try {
        const [resumenResp, reportesResp] = await Promise.all([
            fetch(`${RUPE_ADMIN_ESTADISTICAS_API}/admin/resumen`, { credentials: 'include' }),
            fetch(`${RUPE_ADMIN_ESTADISTICAS_API}/admin/reportes`, { credentials: 'include' })
        ]);
        const resumen = await leerRespuestaEstadisticas(resumenResp);
        const reportes = await leerRespuestaEstadisticas(reportesResp);
        if (!resumenResp.ok) throw new Error(resumen.mensaje || resumen.error || 'No se pudo cargar el resumen.');
        if (!reportesResp.ok) throw new Error(reportes.mensaje || reportes.error || 'No se pudieron cargar los reportes.');
        resumenEstadisticas = resumen || {};
        reportesEstadisticas = Array.isArray(reportes) ? reportes : [];
        aplicarFiltrosEstadisticas();
        mostrarEstadisticas('Estadisticas actualizadas correctamente.', 'success');
    } catch (error) {
        mostrarEstadisticas(error.message || 'No se pudo conectar con el backend.', 'error');
        pintarErrorEstadisticas(error.message || 'No se pudo cargar la informacion.');
    }
}

function aplicarFiltrosEstadisticas() {
    const texto = normalizarEstadisticas(document.getElementById('buscarEstadisticasZona')?.value || '');
    const estatus = document.getElementById('filtroEstadisticasEstatus')?.value || '';
    const desde = document.getElementById('fechaEstadisticasDesde')?.value || '';
    const hasta = document.getElementById('fechaEstadisticasHasta')?.value || '';

    reportesFiltradosEstadisticas = reportesEstadisticas.filter((reporte) => {
        const contenido = normalizarEstadisticas([reporte.folio, reporte.mascota, reporte.raza, reporte.zona, etiquetaTipoReporte(reporte.tipoReporte)].join(' '));
        const coincideTexto = !texto || contenido.includes(texto);
        const coincideEstatus = !estatus || String(reporte.estatus || '').toUpperCase() === estatus;
        const fecha = String(reporte.fechaRegistro || reporte.fechaExtravio || '').slice(0, 10);
        const coincideDesde = !desde || fecha >= desde;
        const coincideHasta = !hasta || fecha <= hasta;
        return coincideTexto && coincideEstatus && coincideDesde && coincideHasta;
    });

    zonasEstadisticas = calcularZonasEstadisticas(reportesFiltradosEstadisticas);
    pintarResumenEstadisticas();
    pintarGraficasEstadisticas();
    pintarTablaZonas();
    pintarTablaVisitas();
    actualizarContadorEstadisticas();
}

function pintarResumenEstadisticas() {
    const resumenFiltrado = calcularResumenFiltrado();
    const valores = {
        mascotasRegistradas: resumenEstadisticas.mascotasRegistradas || 0,
        reportesActivos: resumenFiltrado.activos,
        reportesExtravio: resumenFiltrado.extravios,
        reportesRobo: resumenFiltrado.robos,
        mascotasRecuperadas: resumenFiltrado.recuperados,
        avistamientos: resumenFiltrado.avistamientos,
        avistamientosPendientesValidacion: resumenEstadisticas.avistamientosPendientesValidacion || 0,
        reportesPendientesRenovacion: resumenFiltrado.pendientesRenovacion,
        usuariosBloqueados: resumenEstadisticas.usuariosBloqueados || 0,
        usuariosBajaPropia: resumenEstadisticas.usuariosBajaPropia || 0,
        visitasTotales: resumenEstadisticas.visitasTotales || 0,
        mensajesContactoTotal: resumenEstadisticas.mensajesContactoTotal || 0,
        mensajesContactoPendientes: resumenEstadisticas.mensajesContactoPendientes || 0,
        mensajesContactoAtendidos: resumenEstadisticas.mensajesContactoAtendidos || 0
    };
    document.querySelectorAll('[data-stat]').forEach((nodo) => {
        const clave = nodo.dataset.stat;
        nodo.textContent = Number(valores[clave] || 0).toLocaleString('es-MX');
    });
}

function calcularResumenFiltrado() {
    return reportesFiltradosEstadisticas.reduce((acc, reporte) => {
        const estatus = String(reporte.estatus || '').toUpperCase();
        if (estatus === 'ACTIVO') acc.activos += 1;
        if (String(reporte.tipoReporte || 'EXTRAVIO').toUpperCase() === 'ROBO') acc.robos += 1;
        else acc.extravios += 1;
        if (estatus.includes('RECUPER')) acc.recuperados += 1;
        if (reporte.requiereRenovacion || Number(reporte.diasParaVencer) < 0) acc.pendientesRenovacion += 1;
        acc.avistamientos += Number(reporte.avistamientos || 0);
        return acc;
    }, { activos: 0, extravios: 0, robos: 0, recuperados: 0, pendientesRenovacion: 0, avistamientos: 0 });
}

function pintarGraficasEstadisticas() {
    pintarAnillos('graficaEstatusReportes', agruparPorEstatus(reportesFiltradosEstadisticas), 'No hay reportes registrados.');
    pintarAnillos('graficaTipoReportes', agruparPorTipoReporte(reportesFiltradosEstadisticas), 'No hay tipos de reporte para mostrar.');

    const topAvistamientos = [...reportesFiltradosEstadisticas]
        .sort((a, b) => Number(b.avistamientos || 0) - Number(a.avistamientos || 0))
        .slice(0, 8)
        .map((r) => ({ etiqueta: r.folio || `Reporte ${r.idReporte}`, valor: Number(r.avistamientos || 0) }));
    pintarRanking('graficaAvistamientosReportes', topAvistamientos, 'No hay avistamientos registrados.');

    pintarAnillos('graficaVigenciaReportes', agruparPorVigencia(reportesFiltradosEstadisticas), 'No hay vigencias para mostrar.');
    pintarRanking('graficaZonasReportes', zonasEstadisticas.slice(0, 8).map((z) => ({ etiqueta: z.zona, valor: z.reportes })), 'No hay zonas para mostrar.');

    const visitas = Array.isArray(resumenEstadisticas.visitasPorPagina) ? resumenEstadisticas.visitasPorPagina : [];
    pintarRanking('graficaVisitasPaginas', visitas.slice(0, 8).map((v) => ({ etiqueta: v.pagina || 'Sin pagina', valor: Number(v.total || 0) })), 'No hay visitas registradas.');

    pintarBarras('graficaUsuariosSeguridad', [
        { etiqueta: 'Usuarios activos', valor: Number(resumenEstadisticas.usuariosActivos || 0) },
        { etiqueta: 'Usuarios inactivos', valor: Number(resumenEstadisticas.usuariosInactivos || 0) },
        { etiqueta: 'Cuentas bloqueadas', valor: Number(resumenEstadisticas.usuariosBloqueados || 0) },
        { etiqueta: 'Bajas solicitadas por usuarios', valor: Number(resumenEstadisticas.usuariosBajaPropia || 0) }
    ], 'No hay datos de usuarios para mostrar.');
}

function pintarBarras(id, datos, vacio) {
    const contenedor = document.getElementById(id);
    if (!contenedor) return;
    if (!datos.length || datos.every((d) => Number(d.valor || 0) === 0)) {
        contenedor.textContent = vacio;
        return;
    }
    const maximo = Math.max(...datos.map((d) => Number(d.valor || 0)), 1);
    contenedor.innerHTML = datos.map((dato) => {
        const valor = Number(dato.valor || 0);
        const ancho = Math.max(3, Math.round((valor / maximo) * 100));
        return `<div class="stats-bar-row">
            <div class="stats-bar-head"><strong>${escaparEstadisticas(dato.etiqueta)}</strong><span>${valor.toLocaleString('es-MX')}</span></div>
            <div class="stats-bar-track"><span class="stats-bar-fill" style="width:${ancho}%"></span></div>
        </div>`;
    }).join('');
}

function pintarAnillos(id, datos, vacio) {
    const contenedor = document.getElementById(id);
    if (!contenedor) return;
    const total = datos.reduce((suma, dato) => suma + Number(dato.valor || 0), 0);
    if (!datos.length || total === 0) {
        contenedor.textContent = vacio;
        return;
    }
    contenedor.innerHTML = `<div class="stats-ring-grid">${datos.map((dato, index) => {
        const valor = Number(dato.valor || 0);
        const porcentaje = Math.round((valor / total) * 100);
        return `<div class="stats-ring-card">
            <div class="stats-ring ring-${index % 5}" style="--valor:${porcentaje}">
                <span>${porcentaje}%</span>
            </div>
            <strong>${escaparEstadisticas(dato.etiqueta)}</strong>
            <small>${valor.toLocaleString('es-MX')} registro${valor === 1 ? '' : 's'}</small>
        </div>`;
    }).join('')}</div>`;
}

function pintarRanking(id, datos, vacio) {
    const contenedor = document.getElementById(id);
    if (!contenedor) return;
    const utiles = datos.filter((dato) => Number(dato.valor || 0) > 0);
    if (!utiles.length) {
        contenedor.textContent = vacio;
        return;
    }
    const maximo = Math.max(...utiles.map((d) => Number(d.valor || 0)), 1);
    contenedor.innerHTML = utiles.map((dato, index) => {
        const valor = Number(dato.valor || 0);
        const ancho = Math.max(6, Math.round((valor / maximo) * 100));
        return `<div class="stats-rank-row">
            <span class="stats-rank-index">${index + 1}</span>
            <div class="stats-rank-body">
                <div class="stats-rank-head"><strong>${escaparEstadisticas(dato.etiqueta)}</strong><span>${valor.toLocaleString('es-MX')}</span></div>
                <div class="stats-rank-track"><span style="width:${ancho}%"></span></div>
            </div>
        </div>`;
    }).join('');
}

function agruparPorEstatus(reportes) {
    const mapa = new Map();
    reportes.forEach((reporte) => {
        const estatus = String(reporte.estatus || 'SIN_ESTATUS').toUpperCase();
        mapa.set(estatus, (mapa.get(estatus) || 0) + 1);
    });
    return [...mapa.entries()].map(([etiqueta, valor]) => ({ etiqueta, valor }));
}

function agruparPorTipoReporte(reportes) {
    const mapa = new Map([
        ['Extravío', 0],
        ['Robo de mascota', 0]
    ]);
    reportes.forEach((reporte) => {
        const etiqueta = etiquetaTipoReporte(reporte.tipoReporte);
        mapa.set(etiqueta, (mapa.get(etiqueta) || 0) + 1);
    });
    return [...mapa.entries()].map(([etiqueta, valor]) => ({ etiqueta, valor }));
}

function etiquetaTipoReporte(tipo) {
    return String(tipo || 'EXTRAVIO').toUpperCase() === 'ROBO' ? 'Robo de mascota' : 'Extravío';
}

function agruparPorVigencia(reportes) {
    const mapa = new Map([
        ['Vigentes', 0],
        ['Por vencer', 0],
        ['Pendientes de renovacion', 0],
        ['No aplica', 0]
    ]);
    reportes.forEach((reporte) => {
        const estatus = String(reporte.estatus || '').toUpperCase();
        if (estatus !== 'ACTIVO') {
            mapa.set('No aplica', mapa.get('No aplica') + 1);
        } else if (reporte.requiereRenovacion || Number(reporte.diasParaVencer) < 0) {
            mapa.set('Pendientes de renovacion', mapa.get('Pendientes de renovacion') + 1);
        } else if (Number(reporte.diasParaVencer) <= 5) {
            mapa.set('Por vencer', mapa.get('Por vencer') + 1);
        } else {
            mapa.set('Vigentes', mapa.get('Vigentes') + 1);
        }
    });
    return [...mapa.entries()].map(([etiqueta, valor]) => ({ etiqueta, valor }));
}

function calcularZonasEstadisticas(reportes) {
    const mapa = new Map();
    reportes.forEach((reporte) => {
        const zona = reporte.zona || 'Sin zona registrada';
        if (!mapa.has(zona)) {
            mapa.set(zona, { zona, reportes: 0, avistamientos: 0, activos: 0, recuperados: 0 });
        }
        const item = mapa.get(zona);
        item.reportes += 1;
        item.avistamientos += Number(reporte.avistamientos || 0);
        const estatus = String(reporte.estatus || '').toUpperCase();
        if (estatus === 'ACTIVO') item.activos += 1;
        if (estatus.includes('RECUPER')) item.recuperados += 1;
    });
    return [...mapa.values()].sort((a, b) => b.reportes - a.reportes || b.avistamientos - a.avistamientos);
}

function pintarTablaZonas() {
    const tbody = document.getElementById('tablaEstadisticasZona');
    if (!tbody) return;
    if (!zonasEstadisticas.length) {
        tbody.innerHTML = '<tr><td colspan="5">No hay reportes para calcular zonas.</td></tr>';
        return;
    }
    tbody.innerHTML = zonasEstadisticas.map((zona) => `<tr>
        <td>${escaparEstadisticas(zona.zona)}</td>
        <td>${zona.reportes.toLocaleString('es-MX')}</td>
        <td>${zona.avistamientos.toLocaleString('es-MX')}</td>
        <td>${zona.activos.toLocaleString('es-MX')}</td>
        <td>${zona.recuperados.toLocaleString('es-MX')}</td>
    </tr>`).join('');
}

function pintarTablaVisitas() {
    const tbody = document.getElementById('tablaEstadisticasVisitas');
    if (!tbody) return;
    const visitas = Array.isArray(resumenEstadisticas.visitasPorPagina) ? resumenEstadisticas.visitasPorPagina : [];
    if (!visitas.length) {
        tbody.innerHTML = '<tr><td colspan="2">Aun no hay visitas registradas.</td></tr>';
        return;
    }
    tbody.innerHTML = visitas.map((visita) => `<tr>
        <td>${escaparEstadisticas(visita.pagina || 'Sin pagina')}</td>
        <td>${Number(visita.total || 0).toLocaleString('es-MX')}</td>
    </tr>`).join('');
}

function exportarExcelBackend() {
    const params = new URLSearchParams();
    const texto = document.getElementById('buscarEstadisticasZona')?.value || '';
    const estatus = document.getElementById('filtroEstadisticasEstatus')?.value || '';
    const desde = document.getElementById('fechaEstadisticasDesde')?.value || '';
    const hasta = document.getElementById('fechaEstadisticasHasta')?.value || '';
    if (texto) params.set('texto', texto);
    if (estatus) params.set('estatus', estatus);
    if (desde) params.set('desde', desde);
    if (hasta) params.set('hasta', hasta);
    const query = params.toString() ? `?${params.toString()}` : '';
    window.location.href = `${RUPE_ADMIN_ESTADISTICAS_API}/admin/estadisticas/excel${query}`;
    mostrarEstadisticas('Excel real .xlsx solicitado al backend.', 'success');
}

function exportarEstadisticas(tipo) {
    const resumenFiltrado = calcularResumenFiltrado();
    const estatus = agruparPorEstatus(reportesFiltradosEstadisticas);
    const vigencia = agruparPorVigencia(reportesFiltradosEstadisticas);
    const visitas = Array.isArray(resumenEstadisticas.visitasPorPagina) ? resumenEstadisticas.visitasPorPagina : [];
    const filas = [
        ['Indicador', 'Valor'],
        ['Perritos registrados', resumenEstadisticas.mascotasRegistradas || 0],
        ['Reportes activos filtrados', resumenFiltrado.activos],
        ['Reportes por extravio filtrados', resumenFiltrado.extravios],
        ['Reportes por robo filtrados', resumenFiltrado.robos],
        ['Perritos recuperados filtrados', resumenFiltrado.recuperados],
        ['Avistamientos filtrados', resumenFiltrado.avistamientos],
        ['Avistamientos pendientes de validacion', resumenEstadisticas.avistamientosPendientesValidacion || 0],
        ['Pendientes de renovacion filtrados', resumenFiltrado.pendientesRenovacion],
        ['Cuentas bloqueadas temporalmente', resumenEstadisticas.usuariosBloqueados || 0],
        ['Bajas solicitadas por usuarios', resumenEstadisticas.usuariosBajaPropia || 0],
        ['Visitas publicas', resumenEstadisticas.visitasTotales || 0],
        ['Mensajes de contacto', resumenEstadisticas.mensajesContactoTotal || 0],
        ['Mensajes pendientes de soporte', resumenEstadisticas.mensajesContactoPendientes || 0],
        ['Mensajes atendidos de soporte', resumenEstadisticas.mensajesContactoAtendidos || 0],
        [],
        ['Reportes por estatus'],
        ['Estatus', 'Cantidad'],
        ...estatus.map((e) => [e.etiqueta, e.valor]),
        [],
        ['Reportes por tipo'],
        ['Tipo', 'Cantidad'],
        ...agruparPorTipoReporte(reportesFiltradosEstadisticas).map((t) => [t.etiqueta, t.valor]),
        [],
        ['Vigencia de reportes'],
        ['Vigencia', 'Cantidad'],
        ...vigencia.map((v) => [v.etiqueta, v.valor]),
        [],
        ['Zona', 'Reportes', 'Avistamientos', 'Activos', 'Recuperados'],
        ...zonasEstadisticas.map((z) => [z.zona, z.reportes, z.avistamientos, z.activos, z.recuperados]),
        [],
        ['Pagina', 'Visitas'],
        ...visitas.map((v) => [v.pagina, v.total])
    ];
    if (tipo === 'xls') {
        descargarArchivo(tablaHtmlExcelConGraficas(filas, { estatus, tipo: agruparPorTipoReporte(reportesFiltradosEstadisticas), vigencia, zonas: zonasEstadisticas, visitas }), 'estadisticas-rupe-visual.xls', 'application/vnd.ms-excel;charset=utf-8;');
        mostrarEstadisticas('Excel con tablas y graficas visuales generado correctamente.', 'success');
        return;
    }
    const csv = filas.map((fila) => fila.map(celdaCsvEstadisticas).join(',')).join('\n');
    descargarArchivo(csv, 'estadisticas-rupe.csv', 'text/csv;charset=utf-8;');
    mostrarEstadisticas('CSV de estadisticas generado correctamente.', 'success');
}

function tablaHtmlExcelConGraficas(filas, graficas) {
    const estilos = `<style>
        table{border-collapse:collapse;font-family:Arial,sans-serif}
        td,th{border:1px solid #cccccc;padding:6px 8px}
        .titulo{background:#611232;color:#ffffff;font-weight:bold}
        .barra{background:#f0e3e9;width:260px}
        .relleno{background:#611232;color:#ffffff;height:18px}
    </style>`;
    return `${estilos}<h2>RUPE - Estadisticas</h2>
        ${tablaHtmlExcel(filas)}
        <h3>Graficas visuales</h3>
        ${tablaGraficaExcel('Reportes por estatus', graficas.estatus)}
        ${tablaGraficaExcel('Reportes por tipo', graficas.tipo)}
        ${tablaGraficaExcel('Vigencia de reportes', graficas.vigencia)}
        ${tablaGraficaExcel('Zonas con mas reportes', graficas.zonas.slice(0, 10).map((z) => ({ etiqueta: z.zona, valor: z.reportes })))}
        ${tablaGraficaExcel('Visitas por pagina', graficas.visitas.slice(0, 10).map((v) => ({ etiqueta: v.pagina || 'Sin pagina', valor: Number(v.total || 0) })))}`;
}

function tablaGraficaExcel(titulo, datos) {
    const maximo = Math.max(...datos.map((d) => Number(d.valor || 0)), 1);
    const filas = datos.map((dato) => {
        const valor = Number(dato.valor || 0);
        const ancho = Math.max(4, Math.round((valor / maximo) * 100));
        return `<tr><td>${escaparEstadisticas(dato.etiqueta)}</td><td>${valor}</td><td class="barra"><div class="relleno" style="width:${ancho}%">${valor}</div></td></tr>`;
    }).join('');
    return `<table><tr><td class="titulo" colspan="3">${escaparEstadisticas(titulo)}</td></tr><tr><th>Dato</th><th>Total</th><th>Grafica</th></tr>${filas}</table><br>`;
}

function tablaHtmlExcel(filas) {
    return `<table>${filas.map((fila) => `<tr>${fila.map((celda) => `<td>${escaparEstadisticas(celda)}</td>`).join('')}</tr>`).join('')}</table>`;
}

function descargarArchivo(contenido, nombre, tipo) {
    const blob = new Blob([contenido], { type: tipo });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
}

function actualizarContadorEstadisticas() {
    const contador = document.getElementById('contadorEstadisticasAdmin');
    if (contador) contador.textContent = `Mostrando ${reportesFiltradosEstadisticas.length} de ${reportesEstadisticas.length} reportes registrados.`;
}

function limpiarFiltrosEstadisticas() {
    ['buscarEstadisticasZona', 'filtroEstadisticasEstatus', 'fechaEstadisticasDesde', 'fechaEstadisticasHasta'].forEach((id) => {
        const control = document.getElementById(id);
        if (control) control.value = '';
    });
    aplicarFiltrosEstadisticas();
}

function pintarErrorEstadisticas(mensaje) {
    const zona = document.getElementById('tablaEstadisticasZona');
    const visitas = document.getElementById('tablaEstadisticasVisitas');
    if (zona) zona.innerHTML = `<tr><td colspan="5">${escaparEstadisticas(mensaje)}</td></tr>`;
    if (visitas) visitas.innerHTML = `<tr><td colspan="2">${escaparEstadisticas(mensaje)}</td></tr>`;
}

async function leerRespuestaEstadisticas(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function celdaCsvEstadisticas(valor) {
    return `"${String(valor ?? '').replaceAll('"', '""')}"`;
}

function mostrarEstadisticas(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}

function normalizarEstadisticas(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function escaparEstadisticas(valor) {
    return String(valor ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
