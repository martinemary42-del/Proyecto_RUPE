const RUPE_ADMIN_CATALOGOS_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

const catalogosConfig = {
    tipo: { titulo: 'Tipo mascota', descripcion: 'Solo consulta. RUPE se mantiene enfocado en perritos.', editable: false },
    raza: { titulo: 'Raza', descripcion: 'Razas disponibles para registrar perritos.', editable: true, padre: 'tipo', labelPadre: 'Tipo de mascota', ejemplo: 'Ejemplo: Chihuahua', ayudaNombre: 'Escribe la raza completa. Ejemplos: Chihuahua, Labrador Retriever, Mestizo.', ayuda: 'Selecciona el tipo de mascota al que pertenece la raza. En este proyecto se usara Perro.' },
    color: { titulo: 'Color', descripcion: 'Colores principales usados en el registro de perritos.', editable: true, ejemplo: 'Ejemplo: Blanco con cafe', ayudaNombre: 'Usa colores generales y faciles de buscar. Ejemplos: Negro, Blanco, Cafe, Blanco con cafe.' },
    estado: { titulo: 'Estado', descripcion: 'Estados permitidos para reportes y avistamientos.', editable: true, ejemplo: 'Ejemplo: Ciudad de Mexico', ayudaNombre: 'Escribe el nombre oficial de la entidad federativa. Ejemplos: Ciudad de Mexico, Estado de Mexico, Hidalgo.' },
    municipio: { titulo: 'Municipio/Alcaldia', descripcion: 'Municipios o alcaldias vinculados a un estado.', editable: true, padre: 'estado', labelPadre: 'Estado', ejemplo: 'Ejemplo: Benito Juarez', ayudaNombre: 'Escribe el nombre oficial del municipio o alcaldia. No captures codigo postal aqui.', ayuda: 'Selecciona el estado al que pertenece. El codigo postal se captura solamente al registrar colonias.' },
    colonia: { titulo: 'Colonia', descripcion: 'Colonias vinculadas a municipio o alcaldia.', editable: true, padre: 'municipio', labelPadre: 'Municipio o alcaldia', extra: 'Codigo postal', labelExtra: 'Codigo postal', ejemploExtra: 'Ejemplo: 03100', ayudaExtra: 'Captura 5 digitos. Este campo solo aplica para colonias.', ejemplo: 'Ejemplo: Del Valle Centro', ayudaNombre: 'Escribe el nombre completo de la colonia y evita abreviaturas.', ayuda: 'Selecciona el municipio o alcaldia y captura el codigo postal de 5 digitos.' },
    estatus: { titulo: 'Estatus', descripcion: 'Solo consulta. Afecta la lógica de reportes.', editable: false },
    rol: { titulo: 'Rol', descripcion: 'Solo consulta. Afecta permisos del sistema.', editable: false }
};

let catalogoActual = 'tipo';
let registrosCatalogo = [];
let registrosFiltradosCatalogo = [];
let catalogosCache = {};

document.addEventListener('DOMContentLoaded', () => {
    configurarCatalogosAdmin();
    cargarCatalogoAdmin('tipo');
});

function configurarCatalogosAdmin() {
    document.querySelectorAll('#tabsCatalogosAdmin [data-catalogo]').forEach((boton) => {
        boton.addEventListener('click', () => cargarCatalogoAdmin(boton.dataset.catalogo));
    });
    document.getElementById('buscarCatalogoAdmin')?.addEventListener('input', filtrarCatalogosAdmin);
    document.getElementById('filtroEstadoCatalogoAdmin')?.addEventListener('input', filtrarCatalogosAdmin);
    document.getElementById('limpiarFiltrosCatalogos')?.addEventListener('click', limpiarFiltrosCatalogos);
    document.getElementById('btnNuevoCatalogo')?.addEventListener('click', abrirModalCatalogo);
    document.getElementById('btnExportarCatalogosExcel')?.addEventListener('click', exportarCatalogosExcel);
    document.getElementById('cerrarModalCatalogo')?.addEventListener('click', cerrarModalCatalogo);
    document.getElementById('formCatalogoAdmin')?.addEventListener('submit', guardarCatalogoAdmin);
    document.getElementById('nombreCatalogoAdmin')?.addEventListener('input', avisarNombreSimilarCatalogo);
}

async function cargarCatalogoAdmin(catalogo) {
    catalogoActual = catalogo;
    const config = catalogosConfig[catalogoActual];
    // Cada pestaña representa un catálogo distinto; se limpian filtros para evitar búsquedas arrastradas.
    limpiarFiltrosCatalogos(false);
    document.querySelectorAll('#tabsCatalogosAdmin [data-catalogo]').forEach((b) => b.classList.toggle('active', b.dataset.catalogo === catalogoActual));
    document.getElementById('tituloCatalogoAdmin').textContent = config.titulo;
    document.getElementById('descripcionCatalogoAdmin').textContent = config.descripcion;
    document.getElementById('btnNuevoCatalogo').disabled = !config.editable;
    document.getElementById('btnNuevoCatalogo').textContent = config.editable ? 'Nuevo registro' : 'Solo consulta';
    const ayuda = document.getElementById('ayudaCatalogoAdmin');
    if (ayuda) ayuda.textContent = config.ayuda || 'Este dato permite mantener ordenadas las listas usadas por formularios, reportes y estadisticas.';

    try {
        const respuesta = await fetch(`${RUPE_ADMIN_CATALOGOS_API}/admin/catalogos/${catalogoActual}`, { credentials: 'include' });
        const datos = await leerRespuestaCatalogo(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo cargar el catalogo.');
        registrosCatalogo = Array.isArray(datos) ? datos : [];
        catalogosCache[catalogoActual] = registrosCatalogo;
        filtrarCatalogosAdmin();
        enfocarBusquedaCatalogo();
    } catch (error) {
        document.getElementById('tablaCatalogosAdmin').innerHTML = `<tr><td colspan="5">${escaparCatalogo(error.message)}</td></tr>`;
        mostrarCatalogo(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function filtrarCatalogosAdmin() {
    const texto = normalizarCatalogo(document.getElementById('buscarCatalogoAdmin')?.value || '');
    const estado = document.getElementById('filtroEstadoCatalogoAdmin')?.value || '';
    registrosFiltradosCatalogo = registrosCatalogo.filter((item) => {
        const estadoItem = item.activo ? 'ACTIVO' : 'INACTIVO';
        const coincideTexto = !texto || normalizarCatalogo([item.nombre, item.relacion, item.padre, item.extra, estadoItem].join(' ')).includes(texto);
        const coincideEstado = !estado || estado === estadoItem;
        return coincideTexto && coincideEstado;
    });
    pintarCatalogosAdmin();
}

function pintarCatalogosAdmin() {
    const tbody = document.getElementById('tablaCatalogosAdmin');
    if (!tbody) return;
    if (!registrosFiltradosCatalogo.length) {
        tbody.innerHTML = '<tr><td colspan="5">No hay registros con esos filtros.</td></tr>';
    } else {
        tbody.innerHTML = registrosFiltradosCatalogo.map((item) => `
            <tr>
                <td>${item.id}</td>
                <td>${escaparCatalogo(item.nombre)}</td>
                <td>${escaparCatalogo(informacionRelacionadaCatalogo(item))}</td>
                <td><span class="status-pill ${item.activo ? '' : 'warn'}">${item.activo ? 'Activo' : 'Inactivo'}</span></td>
                <td>${item.editable ? `<button class="module-btn secondary admin-action-btn" data-id="${item.id}" data-activo="${!item.activo}">${item.activo ? 'Desactivar' : 'Activar'}</button>` : '<span class="status-pill warn">Solo lectura</span>'}</td>
            </tr>
        `).join('');
        tbody.querySelectorAll('[data-id]').forEach((boton) => boton.addEventListener('click', cambiarEstatusCatalogo));
    }
    document.getElementById('contadorCatalogosAdmin').textContent = `Mostrando ${registrosFiltradosCatalogo.length} de ${registrosCatalogo.length} registros.`;
}

async function abrirModalCatalogo() {
    const config = catalogosConfig[catalogoActual];
    if (!config.editable) {
        mostrarCatalogo('Este catalogo es solo de consulta.', 'warn');
        return;
    }
    document.getElementById('modalCatalogoTitulo').textContent = `Nuevo registro: ${config.titulo}`;
    const nombreInput = document.getElementById('nombreCatalogoAdmin');
    nombreInput.value = '';
    nombreInput.placeholder = config.ejemplo || 'Escribe el nombre del registro';
    const ayudaNombre = document.getElementById('ayudaNombreCatalogo');
    if (ayudaNombre) ayudaNombre.textContent = config.ayudaNombre || 'Escribe un nombre claro y sin abreviaturas para evitar registros repetidos.';
    actualizarSugerenciasCatalogo();

    const extraInput = document.getElementById('extraCatalogoAdmin');
    const grupoExtra = document.getElementById('grupoExtraCatalogo');
    const ayudaExtra = document.getElementById('ayudaExtraCatalogo');
    extraInput.value = '';
    grupoExtra.hidden = true;
    grupoExtra.style.display = 'none';
    extraInput.placeholder = '';
    if (ayudaExtra) ayudaExtra.textContent = '';

    await configurarCamposRelacionados(config);
    document.getElementById('modalCatalogoAdmin').hidden = false;
}

async function configurarCamposRelacionados(config) {
    // El campo extra se reserva para colonias porque ahi se captura el codigo postal.
    const grupoPadre = document.getElementById('grupoPadreCatalogo');
    const grupoExtra = document.getElementById('grupoExtraCatalogo');
    const extraInput = document.getElementById('extraCatalogoAdmin');
    const ayudaExtra = document.getElementById('ayudaExtraCatalogo');
    const mostrarExtra = catalogoActual === 'colonia';

    grupoPadre.hidden = !config.padre;
    grupoExtra.hidden = !mostrarExtra;
    grupoExtra.style.display = mostrarExtra ? '' : 'none';
    extraInput.required = false;

    if (mostrarExtra) {
        document.getElementById('labelExtraCatalogo').textContent = 'Codigo postal';
        extraInput.placeholder = config.ejemploExtra || 'Ejemplo: 03100';
        if (ayudaExtra) ayudaExtra.textContent = config.ayudaExtra || 'Captura 5 digitos. Este campo solo aplica para colonias.';
    } else {
        extraInput.value = '';
        extraInput.placeholder = '';
        if (ayudaExtra) ayudaExtra.textContent = '';
    }

    if (!config.padre) return;

    document.getElementById('labelPadreCatalogo').textContent = config.labelPadre;
    if (!catalogosCache[config.padre]) {
        const respuesta = await fetch(`${RUPE_ADMIN_CATALOGOS_API}/admin/catalogos/${config.padre}`, { credentials: 'include' });
        const datos = await leerRespuestaCatalogo(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || 'No se pudo cargar relacion.');
        catalogosCache[config.padre] = datos;
    }
    const select = document.getElementById('padreCatalogoAdmin');
    select.innerHTML = '<option value="">Selecciona una opcion</option>';
    catalogosCache[config.padre].filter((i) => i.activo).forEach((item) => {
        const option = document.createElement('option');
        option.value = item.id;
        option.textContent = item.nombre;
        select.appendChild(option);
    });
}
function cerrarModalCatalogo() {
    document.getElementById('modalCatalogoAdmin').hidden = true;
}

async function guardarCatalogoAdmin(event) {
    event.preventDefault();
    const config = catalogosConfig[catalogoActual];
    const nombre = document.getElementById('nombreCatalogoAdmin').value;
    const idPadre = config.padre ? Number(document.getElementById('padreCatalogoAdmin').value) : null;
    const extra = catalogoActual === 'colonia' ? document.getElementById('extraCatalogoAdmin').value : null;
    const similar = buscarNombreSimilarCatalogo(nombre);
    if (similar) {
        mostrarCatalogo(`Existe un registro parecido: "${similar}". Revisa el catalogo antes de crear uno nuevo.`, "warn");
        return;
    }
    if (config.padre && !idPadre) {
        mostrarCatalogo('Selecciona el dato asociado requerido.', 'warn');
        return;
    }
    if (catalogoActual === 'colonia' && extra && !/^\d{5}$/.test(extra.trim())) {
        mostrarCatalogo('El codigo postal debe tener 5 digitos.', 'warn');
        return;
    }
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_CATALOGOS_API}/admin/catalogos/${catalogoActual}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ nombre, idPadre, extra })
        });
        const datos = await leerRespuestaCatalogo(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo guardar el registro.');
        mostrarCatalogo('Registro guardado correctamente.', 'success');
        cerrarModalCatalogo();
        await cargarCatalogoAdmin(catalogoActual);
    } catch (error) {
        mostrarCatalogo(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function cambiarEstatusCatalogo(event) {
    const boton = event.currentTarget;
    const activo = boton.dataset.activo === 'true';
    const confirmar = await mostrarVentanaCatalogo({
        titulo: activo ? 'Activar registro' : 'Desactivar registro',
        mensaje: `Deseas ${activo ? 'activar' : 'desactivar'} este registro? La desactivacion conserva el dato para trazabilidad historica.`,
        tipo: 'warn',
        acciones: [
            { texto: activo ? 'Activar' : 'Desactivar' },
            { texto: 'Cancelar', secundario: true }
        ]
    });
    if (confirmar !== 0) return;
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_CATALOGOS_API}/admin/catalogos/${catalogoActual}/${boton.dataset.id}/estatus?activo=${activo}`, {
            method: 'PUT',
            credentials: 'include'
        });
        const datos = await leerRespuestaCatalogo(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo actualizar el registro.');
        await mostrarCatalogo('Estatus actualizado correctamente.', 'success');
        await cargarCatalogoAdmin(catalogoActual);
    } catch (error) {
        mostrarCatalogo(error.message || 'No se pudo actualizar.', 'error');
    }
}
async function exportarCatalogosExcel() {
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_CATALOGOS_API}/admin/catalogos/excel`, { credentials: 'include' });
        if (!respuesta.ok) {
            const datos = await leerRespuestaCatalogo(respuesta);
            throw new Error(datos.mensaje || datos.error || 'No se pudo generar el Excel.');
        }
        const blob = await respuesta.blob();
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'catalogos-rupe.xlsx';
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(url);
        mostrarCatalogo('Excel de catálogos generado correctamente.', 'success');
    } catch (error) {
        mostrarCatalogo(error.message || 'No se pudo exportar el Excel.', 'error');
    }
}

function informacionRelacionadaCatalogo(item) {
    if (item.catalogo === 'raza') return item.padre ? `Tipo de mascota: ${item.padre}` : 'Tipo de mascota: Perro';
    if (item.catalogo === 'municipio') return [item.padre ? `Estado: ${item.padre}` : '', item.extra ? `Zona: ${item.extra}` : ''].filter(Boolean).join(' / ') || 'Sin dato asociado';
    if (item.catalogo === 'colonia') return [item.padre ? `Municipio: ${item.padre}` : '', item.extra ? `CP: ${item.extra}` : ''].filter(Boolean).join(' / ') || 'Sin dato asociado';
    if (item.extra) return item.extra;
    return 'Sin dato asociado';
}

function actualizarSugerenciasCatalogo() {
    const datalist = document.getElementById('sugerenciasCatalogoAdmin');
    if (!datalist) return;
    datalist.innerHTML = registrosCatalogo
        .filter((item) => item.activo)
        .map((item) => `<option value="${escaparCatalogo(item.nombre)}"></option>`)
        .join('');
}

function avisarNombreSimilarCatalogo() {
    const ayudaNombre = document.getElementById('ayudaNombreCatalogo');
    if (!ayudaNombre) return;
    const similar = buscarNombreSimilarCatalogo(document.getElementById('nombreCatalogoAdmin')?.value || '');
    if (similar) {
        ayudaNombre.textContent = `Revisa este dato: se parece a "${similar}", que ya existe en el catalogo.`;
        return;
    }
    ayudaNombre.textContent = catalogosConfig[catalogoActual].ayudaNombre || 'Escribe un nombre claro y sin abreviaturas para evitar registros repetidos.';
}

function buscarNombreSimilarCatalogo(nombre) {
    const limpio = normalizarCatalogo(nombre);
    if (limpio.length < 4) return null;
    const similares = registrosCatalogo.filter((item) => item.activo).map((item) => item.nombre);
    return similares.find((existente) => {
        const actual = normalizarCatalogo(existente);
        if (actual === limpio) return true;
        if (Math.abs(actual.length - limpio.length) > 2) return false;
        return distanciaCatalogo(actual, limpio) <= (Math.max(actual.length, limpio.length) >= 8 ? 2 : 1);
    }) || null;
}

function distanciaCatalogo(a, b) {
    const matriz = Array.from({ length: a.length + 1 }, () => Array(b.length + 1).fill(0));
    for (let i = 0; i <= a.length; i++) matriz[i][0] = i;
    for (let j = 0; j <= b.length; j++) matriz[0][j] = j;
    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            const costo = a[i - 1] === b[j - 1] ? 0 : 1;
            matriz[i][j] = Math.min(matriz[i - 1][j] + 1, matriz[i][j - 1] + 1, matriz[i - 1][j - 1] + costo);
        }
    }
    return matriz[a.length][b.length];
}

function limpiarFiltrosCatalogos(aplicarFiltro = true) {
    document.getElementById('buscarCatalogoAdmin').value = '';
    document.getElementById('filtroEstadoCatalogoAdmin').value = '';
    if (aplicarFiltro) filtrarCatalogosAdmin();
    enfocarBusquedaCatalogo();
}

function enfocarBusquedaCatalogo() {
    const buscar = document.getElementById('buscarCatalogoAdmin');
    if (buscar) buscar.focus();
}

async function leerRespuestaCatalogo(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function normalizarCatalogo(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/\s+/g, ' ').trim();
}

function mostrarCatalogo(mensaje, tipo) {
    return mostrarVentanaCatalogo({
        titulo: tipo === 'success' ? 'Operacion realizada' : tipo === 'error' ? 'No fue posible continuar' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaCatalogo({ titulo, mensaje, tipo = 'info', textoBoton = 'Aceptar', acciones = [] }) {
    // Modal reutilizable para mantener una experiencia consistente y evitar alert/confirm nativos.
    return new Promise((resolve) => {
        document.querySelector('.catalogo-feedback-overlay')?.remove();
        const overlay = document.createElement('div');
        overlay.className = 'catalogo-feedback-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(17,24,39,.58);display:flex;align-items:center;justify-content:center;z-index:1200;padding:18px;';
        const color = tipo === 'success' ? '#198754' : tipo === 'error' ? '#b42318' : tipo === 'warn' ? '#8a6500' : '#8a1538';
        const icono = tipo === 'success' ? 'fa-circle-check' : tipo === 'error' ? 'fa-circle-xmark' : 'fa-circle-info';
        const botones = acciones.length
            ? acciones.map((accion, index) => `<button type="button" data-accion="${index}" style="border-radius:8px;background:${accion.secundario ? '#fff' : color};color:${accion.secundario ? color : '#fff'};font-weight:700;padding:11px 18px;cursor:pointer;min-width:118px;border:${accion.secundario ? '1px solid ' + color : '0'};">${escaparCatalogo(accion.texto)}</button>`).join('')
            : `<button type="button" data-aceptar style="border:0;border-radius:8px;background:${color};color:#fff;font-weight:700;padding:11px 22px;cursor:pointer;min-width:130px;">${escaparCatalogo(textoBoton)}</button>`;
        overlay.innerHTML = `
            <div role="dialog" aria-modal="true" style="width:min(440px,100%);background:#fff;border-radius:10px;box-shadow:0 24px 70px rgba(0,0,0,.24);padding:26px;text-align:center;font-family:Montserrat,Arial,sans-serif;">
                <i class="fa-solid ${icono}" style="font-size:42px;color:${color};margin-bottom:14px;"></i>
                <h2 style="margin:0 0 10px;color:#2b2b2b;font-size:1.35rem;">${escaparCatalogo(titulo)}</h2>
                <p style="margin:0 0 20px;color:#4b5563;line-height:1.55;">${escaparCatalogo(mensaje)}</p>
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

function escaparCatalogo(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
