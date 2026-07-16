const RUPE_ADMIN_BITACORA_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const BITACORA_POR_PAGINA = 10;

let bitacoraAdmin = [];
let bitacoraFiltrada = [];
let paginaBitacora = 1;

document.addEventListener('DOMContentLoaded', () => {
    configurarBitacoraAdmin();
    cargarBitacoraAdmin();
});

function configurarBitacoraAdmin() {
    [
        'buscarBitacoraAdmin',
        'filtroModuloBitacora',
        'filtroResultadoBitacora',
        'filtroRiesgoBitacora',
        'fechaBitacoraDesde',
        'fechaBitacoraHasta'
    ].forEach((id) => {
        document.getElementById(id)?.addEventListener('input', () => {
            paginaBitacora = 1;
            aplicarFiltrosBitacora();
        });
    });

    document.getElementById('limpiarFiltrosBitacora')?.addEventListener('click', limpiarFiltrosBitacora);
    document.getElementById('bitacoraAnterior')?.addEventListener('click', () => cambiarPaginaBitacora(-1));
    document.getElementById('bitacoraSiguiente')?.addEventListener('click', () => cambiarPaginaBitacora(1));
    document.getElementById('exportarBitacoraCsv')?.addEventListener('click', exportarBitacoraCsv);
    document.getElementById('exportarBitacoraExcel')?.addEventListener('click', exportarBitacoraExcel);
}

async function cargarBitacoraAdmin() {
    const tbody = document.getElementById('tablaBitacoraAdmin');
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_BITACORA_API}/admin/bitacora`, { credentials: 'include' });
        const datos = await leerRespuestaBitacora(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo cargar la bitácora.');

        bitacoraAdmin = Array.isArray(datos) ? datos : [];
        cargarModulosBitacora();
        pintarAlertasSeguridadBitacora();
        aplicarFiltrosBitacora();
    } catch (error) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="10">${escaparBitacora(error.message || 'No se pudo conectar con el backend.')}</td></tr>`;
        actualizarContadorBitacora(0, 0);
        mostrarBitacoraAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function cargarModulosBitacora() {
    const select = document.getElementById('filtroModuloBitacora');
    if (!select) return;
    const modulos = [...new Set(bitacoraAdmin.map((evento) => evento.modulo).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b, 'es'));
    select.innerHTML = '<option value="">Todos</option>' + modulos
        .map((modulo) => `<option value="${escaparBitacora(modulo)}">${escaparBitacora(modulo)}</option>`)
        .join('');
}

function aplicarFiltrosBitacora() {
    const texto = normalizarBitacora(document.getElementById('buscarBitacoraAdmin')?.value || '');
    const modulo = document.getElementById('filtroModuloBitacora')?.value || '';
    const resultado = document.getElementById('filtroResultadoBitacora')?.value || '';
    const riesgo = document.getElementById('filtroRiesgoBitacora')?.value || '';
    const desde = document.getElementById('fechaBitacoraDesde')?.value || '';
    const hasta = document.getElementById('fechaBitacoraHasta')?.value || '';

    bitacoraFiltrada = bitacoraAdmin.filter((evento) => {
        const fechaEvento = obtenerFechaBitacora(evento.fechaHora);
        const analisis = analizarRiesgoBitacora(evento);
        const coincideTexto = !texto || normalizarBitacora([
            evento.correo,
            evento.accion,
            evento.modulo,
            evento.resultado,
            analisis.riesgo,
            analisis.medida,
            evento.ip,
            evento.descripcion
        ].join(' ')).includes(texto);
        const coincideModulo = !modulo || evento.modulo === modulo;
        const coincideResultado = !resultado || String(evento.resultado || '').toUpperCase() === resultado;
        const coincideRiesgo = !riesgo || analisis.riesgo === riesgo;
        const coincideDesde = !desde || (fechaEvento && fechaEvento >= new Date(`${desde}T00:00:00`));
        const coincideHasta = !hasta || (fechaEvento && fechaEvento <= new Date(`${hasta}T23:59:59`));
        return coincideTexto && coincideModulo && coincideResultado && coincideRiesgo && coincideDesde && coincideHasta;
    });

    const totalPaginas = Math.max(1, Math.ceil(bitacoraFiltrada.length / BITACORA_POR_PAGINA));
    paginaBitacora = Math.min(paginaBitacora, totalPaginas);
    const inicio = (paginaBitacora - 1) * BITACORA_POR_PAGINA;
    pintarBitacora(bitacoraFiltrada.slice(inicio, inicio + BITACORA_POR_PAGINA));
    actualizarPaginacionBitacora(totalPaginas);
    actualizarContadorBitacora(bitacoraFiltrada.length, bitacoraAdmin.length);
}

function pintarBitacora(eventos) {
    const tbody = document.getElementById('tablaBitacoraAdmin');
    if (!tbody) return;
    if (!eventos.length) {
        tbody.innerHTML = '<tr><td colspan="10">No hay eventos con esos filtros.</td></tr>';
        return;
    }

    tbody.innerHTML = eventos.map((evento) => {
        const analisis = analizarRiesgoBitacora(evento);
        // El riesgo se calcula en pantalla para no guardar datos sensibles extra en la base.
        return `
            <tr>
                <td>${formatearFechaBitacora(evento.fechaHora)}</td>
                <td>${escaparBitacora(evento.correo || 'sistema')}</td>
                <td>${escaparBitacora(evento.modulo)}</td>
                <td>${escaparBitacora(evento.accion)}</td>
                <td><span class="status-pill ${claseResultadoBitacora(evento.resultado)}">${escaparBitacora(evento.resultado)}</span></td>
                <td><span class="status-pill ${claseRiesgoBitacora(analisis.riesgo)}">${escaparBitacora(analisis.riesgo)}</span></td>
                <td>${escaparBitacora(evento.ip || 'sin dato')}</td>
                <td>${escaparBitacora(evento.descripcion || 'Sin descripción')}</td>
                <td>${escaparBitacora(analisis.medida)}</td>
                <td>${botonRevisionUsuario(evento)}</td>
            </tr>
        `;
    }).join('');
}

function botonRevisionUsuario(evento) {
    if (!evento.idUsuario) return '<span class="admin-muted">No aplica</span>';
    const url = `admin-usuarios.html?usuario=${encodeURIComponent(evento.idUsuario)}`;
    return `<a class="module-btn secondary admin-action-btn" href="${url}" title="Abrir usuarios con filtro por ID">Revisar</a>`;
}

function claseResultadoBitacora(resultado) {
    const valor = String(resultado || '').toUpperCase();
    if (valor.includes('FALL') || valor.includes('ERROR')) return 'danger';
    if (valor.includes('BLOQ') || valor.includes('ADVERT')) return 'warn';
    return '';
}

function claseRiesgoBitacora(riesgo) {
    if (riesgo === 'ALTO') return 'danger';
    if (riesgo === 'MEDIO') return 'warn';
    return '';
}

function analizarRiesgoBitacora(evento) {
    const texto = normalizarBitacora([
        evento.modulo,
        evento.accion,
        evento.resultado,
        evento.descripcion
    ].join(' '));
    const resultado = normalizarBitacora(evento.resultado);

    if (texto.includes('bloque') || resultado.includes('bloque')) {
        return {
            riesgo: 'ALTO',
            medida: 'Mantener bloqueo temporal, revisar IP y solicitar CAPTCHA.'
        };
    }
    if (texto.includes('fall') || texto.includes('error') || texto.includes('credencial') || texto.includes('captcha')) {
        return {
            riesgo: 'MEDIO',
            medida: 'Verificar intentos repetidos y reforzar validación antes de desbloquear.'
        };
    }
    if (texto.includes('export') || texto.includes('excel') || texto.includes('csv')) {
        return {
            riesgo: 'MEDIO',
            medida: 'Confirmar que la exportación corresponda a una actividad administrativa autorizada.'
        };
    }
    if (texto.includes('catalog') || texto.includes('activar') || texto.includes('desactivar')) {
        return {
            riesgo: 'MEDIO',
            medida: 'Revisar que el cambio de catálogo esté justificado y conservar trazabilidad.'
        };
    }
    if (texto.includes('login') || texto.includes('sesion') || texto.includes('acceso')) {
        return {
            riesgo: 'BAJO',
            medida: 'Mantener monitoreo; revisar si se repite en corto tiempo.'
        };
    }
    return {
        riesgo: 'BAJO',
        medida: 'Sin acción inmediata; conservar el registro para auditoría.'
    };
}

function pintarAlertasSeguridadBitacora() {
    const contenedor = document.getElementById('alertasSeguridadBitacora');
    if (!contenedor) return;
    const alertas = calcularAlertasSeguridadBitacora();
    contenedor.innerHTML = alertas.map((alerta) => `
        <article class="security-alert-card ${alerta.clase}">
            <strong>${Number(alerta.total || 0).toLocaleString('es-MX')}</strong>
            <span>${escaparBitacora(alerta.titulo)}</span>
            <small>${escaparBitacora(alerta.detalle)}</small>
        </article>
    `).join('');
}

function calcularAlertasSeguridadBitacora() {
    const eventos = bitacoraAdmin || [];
    const riesgos = eventos.map(analizarRiesgoBitacora);
    const altos = riesgos.filter((item) => item.riesgo === 'ALTO').length;
    const medios = riesgos.filter((item) => item.riesgo === 'MEDIO').length;
    const fallidos = eventos.filter((evento) => normalizarBitacora([evento.resultado, evento.accion, evento.descripcion].join(' ')).includes('fall')).length;
    const bloqueados = eventos.filter((evento) => normalizarBitacora([evento.resultado, evento.accion, evento.descripcion].join(' ')).includes('bloque')).length;
    const exportaciones = eventos.filter((evento) => normalizarBitacora([evento.accion, evento.descripcion].join(' ')).match(/export|excel|csv/)).length;
    const ipsRepetidas = contarRepetidos(eventos.map((evento) => evento.ip).filter(Boolean), 3);

    return [
        {
            total: altos,
            titulo: 'Riesgo alto',
            detalle: 'Bloqueos o eventos que requieren revisión prioritaria.',
            clase: altos ? 'danger' : ''
        },
        {
            total: medios,
            titulo: 'Riesgo medio',
            detalle: 'Fallos, exportaciones o cambios administrativos sensibles.',
            clase: medios ? 'warn' : ''
        },
        {
            total: fallidos,
            titulo: 'Eventos fallidos',
            detalle: 'Posibles errores de acceso, CAPTCHA o validación.',
            clase: fallidos ? 'warn' : ''
        },
        {
            total: bloqueados,
            titulo: 'Bloqueos',
            detalle: 'Cuentas o acciones detenidas por control de seguridad.',
            clase: bloqueados ? 'danger' : ''
        },
        {
            total: exportaciones,
            titulo: 'Exportaciones',
            detalle: 'Descargas administrativas que deben quedar auditadas.',
            clase: exportaciones ? 'warn' : ''
        },
        {
            total: ipsRepetidas,
            titulo: 'IP repetidas',
            detalle: 'Direcciones con tres o más eventos registrados.',
            clase: ipsRepetidas ? 'warn' : ''
        }
    ];
}

function contarRepetidos(valores, minimo) {
    const mapa = new Map();
    valores.forEach((valor) => {
        const clave = String(valor || '').trim();
        if (!clave) return;
        mapa.set(clave, (mapa.get(clave) || 0) + 1);
    });
    return [...mapa.values()].filter((total) => total >= minimo).length;
}

function cambiarPaginaBitacora(delta) {
    paginaBitacora += delta;
    aplicarFiltrosBitacora();
}

function actualizarPaginacionBitacora(totalPaginas) {
    const contenedor = document.getElementById('paginacionBitacoraAdmin');
    const texto = document.getElementById('paginaBitacoraAdmin');
    const anterior = document.getElementById('bitacoraAnterior');
    const siguiente = document.getElementById('bitacoraSiguiente');
    if (!contenedor || !texto || !anterior || !siguiente) return;
    contenedor.hidden = bitacoraFiltrada.length <= BITACORA_POR_PAGINA;
    texto.textContent = `Página ${paginaBitacora} de ${totalPaginas}`;
    anterior.disabled = paginaBitacora <= 1;
    siguiente.disabled = paginaBitacora >= totalPaginas;
}

function actualizarContadorBitacora(totalFiltrado, totalGeneral) {
    const contador = document.getElementById('contadorBitacoraAdmin');
    if (contador) contador.textContent = `Mostrando ${totalFiltrado} de ${totalGeneral} eventos registrados.`;
}

function limpiarFiltrosBitacora() {
    [
        'buscarBitacoraAdmin',
        'filtroModuloBitacora',
        'filtroResultadoBitacora',
        'filtroRiesgoBitacora',
        'fechaBitacoraDesde',
        'fechaBitacoraHasta'
    ].forEach((id) => {
        const control = document.getElementById(id);
        if (control) control.value = '';
    });
    paginaBitacora = 1;
    aplicarFiltrosBitacora();
}

function exportarBitacoraCsv() {
    const encabezados = ['Fecha', 'Correo', 'Modulo', 'Accion', 'Resultado', 'Riesgo', 'IP', 'Descripcion', 'Medida sugerida'];
    const filas = bitacoraFiltrada.map((evento) => [
        // El CSV incluye el analisis para que la revision externa sea mas sencilla.
        formatearFechaBitacora(evento.fechaHora),
        evento.correo,
        evento.modulo,
        evento.accion,
        evento.resultado,
        analizarRiesgoBitacora(evento).riesgo,
        evento.ip,
        evento.descripcion,
        analizarRiesgoBitacora(evento).medida
    ]);
    const csv = [encabezados, ...filas].map((fila) => fila.map(celdaBitacoraCsv).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'bitacora-seguridad-rupe.csv';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
    mostrarBitacoraAdmin('CSV de bitácora generado correctamente.', 'success');
}

function exportarBitacoraExcel() {
    window.location.href = `${RUPE_ADMIN_BITACORA_API}/admin/bitacora/excel`;
    mostrarBitacoraAdmin('Excel de bitácora solicitado al backend.', 'success');
}

function celdaBitacoraCsv(valor) {
    return `"${String(valor ?? '').replaceAll('"', '""')}"`;
}

async function leerRespuestaBitacora(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function obtenerFechaBitacora(valor) {
    if (!valor) return null;
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? null : fecha;
}

function formatearFechaBitacora(valor) {
    const fecha = obtenerFechaBitacora(valor);
    if (!fecha) return 'Sin fecha';
    return fecha.toLocaleString('es-MX', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function mostrarBitacoraAdmin(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}

function normalizarBitacora(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function escaparBitacora(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
