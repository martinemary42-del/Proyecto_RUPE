const RUPE_LOST_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_LOST_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';

const RUPE_LOST_PAGE_SIZE = 12;
let paginaReportes = 0;
let paginaAvistamientos = 0;
let folioActual = '';
let temporizadorBusquedaExtraviados = null;
let filtrosExtraviados = { texto: '', raza: '', senas: '', sexo: '', zona: '', tipo: 'todos' };

// Punto de entrada de la pagina publica: lee el folio de la URL, carga datos y activa filtros.
document.addEventListener('DOMContentLoaded', () => {
    const parametros = new URLSearchParams(window.location.search);
    folioActual = parametros.get('folio') || '';
    filtrosExtraviados.texto = folioActual;
    const input = document.getElementById('folioBusqueda');
    if (input && folioActual) input.value = folioActual;

    cargarReportesExtraviados(0);
    cargarAvistamientosSinFolio(0);
    configurarBusquedaExtraviados();
});

async function cargarReportesExtraviados(pagina = 0) {
    const contenedor = document.getElementById('listaReportesExtraviados');
    if (!contenedor) return;

    try {
        contenedor.innerHTML = tarjetaCargaExtraviados('Cargando reportes', 'Consultando reportes activos.');
        const params = new URLSearchParams({
            pagina: String(Math.max(pagina, 0)),
            tamanio: String(RUPE_LOST_PAGE_SIZE)
        });
        // Los filtros viajan como query params para que el backend haga la busqueda paginada.
        if (filtrosExtraviados.texto) params.set('texto', filtrosExtraviados.texto);
        if (filtrosExtraviados.raza) params.set('raza', filtrosExtraviados.raza);
        if (filtrosExtraviados.senas) params.set('senas', filtrosExtraviados.senas);
        if (filtrosExtraviados.sexo) params.set('sexo', filtrosExtraviados.sexo);
        if (filtrosExtraviados.zona) params.set('zona', filtrosExtraviados.zona);

        const respuesta = await fetch(`${RUPE_LOST_API}/publico/reportes-activos-paginados?${params.toString()}`);
        const datos = await leerRespuestaExtraviados(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || 'No se pudieron consultar los reportes.');

        paginaReportes = datos.pagina || 0;
        pintarReportesExtraviados(datos);
    } catch (error) {
        contenedor.innerHTML = `
            <article class="module-card lost-card-empty">
                <i class="fas fa-triangle-exclamation"></i>
                <h3>No fue posible cargar reportes</h3>
                <p>Verifica que Spring Boot esté activo en el puerto 8080.</p>
            </article>
        `;
    }
}

function configurarBusquedaExtraviados() {
    const input = document.getElementById('folioBusqueda');
    const boton = document.getElementById('btnBuscarFolio');
    if (!input || !boton) return;

    const buscar = () => {
        actualizarFiltrosExtraviados();
        // Mantiene la URL sincronizada con el folio/texto buscado para poder compartir el enlace.
        const url = new URL(window.location.href);
        if (filtrosExtraviados.texto) {
            url.searchParams.set('folio', filtrosExtraviados.texto);
        } else {
            url.searchParams.delete('folio');
        }
        window.history.replaceState({}, '', url);
        aplicarVistaPorTipo();
        cargarReportesExtraviados(0);
        cargarAvistamientosSinFolio(0);
    };

    document.getElementById('btnLimpiarFiltros')?.addEventListener('click', limpiarFiltrosExtraviados);
    boton.addEventListener('click', buscar);

    // Busqueda reactiva: cualquier filtro visible actualiza reportes y avistamientos sin esperar el boton.
    ['folioBusqueda', 'razaBusqueda', 'senasBusqueda', 'zonaBusqueda'].forEach((id) => {
        document.getElementById(id)?.addEventListener('input', () => {
            clearTimeout(temporizadorBusquedaExtraviados);
            temporizadorBusquedaExtraviados = setTimeout(buscar, 450);
        });
    });

    ['sexoBusqueda', 'tipoBusqueda'].forEach((id) => {
        document.getElementById(id)?.addEventListener('change', () => {
            clearTimeout(temporizadorBusquedaExtraviados);
            buscar();
        });
    });

    input.addEventListener('keydown', (evento) => {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            clearTimeout(temporizadorBusquedaExtraviados);
            buscar();
        }
    });
}

function actualizarFiltrosExtraviados() {
    filtrosExtraviados = {
        texto: document.getElementById('folioBusqueda')?.value.trim() || '',
        raza: document.getElementById('razaBusqueda')?.value.trim() || '',
        senas: document.getElementById('senasBusqueda')?.value.trim() || '',
        sexo: document.getElementById('sexoBusqueda')?.value || '',
        zona: document.getElementById('zonaBusqueda')?.value.trim() || '',
        tipo: document.getElementById('tipoBusqueda')?.value || 'todos'
    };
    folioActual = filtrosExtraviados.texto;
}

function limpiarFiltrosExtraviados() {
    ['folioBusqueda', 'razaBusqueda', 'senasBusqueda', 'zonaBusqueda'].forEach((id) => {
        const campo = document.getElementById(id);
        if (campo) campo.value = '';
    });
    const sexo = document.getElementById('sexoBusqueda');
    const tipo = document.getElementById('tipoBusqueda');
    if (sexo) sexo.value = '';
    if (tipo) tipo.value = 'todos';
    actualizarFiltrosExtraviados();
    window.history.replaceState({}, '', 'perritos-extraviados.html');
    aplicarVistaPorTipo();
    cargarReportesExtraviados(0);
    cargarAvistamientosSinFolio(0);
}

function filtrosActivosExtraviados() {
    return Boolean(filtrosExtraviados.texto || filtrosExtraviados.raza || filtrosExtraviados.senas || filtrosExtraviados.sexo || filtrosExtraviados.zona || filtrosExtraviados.tipo !== 'todos');
}

function aplicarVistaPorTipo() {
    const mostrarReportes = filtrosExtraviados.tipo === 'todos' || filtrosExtraviados.tipo === 'extravio' || filtrosExtraviados.tipo === 'robo';
    const mostrarAvistamientos = filtrosExtraviados.tipo === 'todos' || filtrosExtraviados.tipo === 'avistamiento' || filtrosExtraviados.tipo === 'resguardo';
    const reportes = document.getElementById('reportesConFolio') || document.getElementById('listaReportesExtraviados');
    const avistamientos = document.getElementById('avistamientosSinFolio');
    if (reportes) reportes.style.display = mostrarReportes ? '' : 'none';
    if (avistamientos) avistamientos.style.display = mostrarAvistamientos ? '' : 'none';
}


function pintarReportesExtraviados(pagina) {
    const contenedor = document.getElementById('listaReportesExtraviados');
    const textoFiltro = document.getElementById('textoFiltro');
    if (!contenedor) return;

    const reportes = Array.isArray(pagina.contenido) ? pagina.contenido : [];
    if (textoFiltro) {
        textoFiltro.textContent = filtrosActivosExtraviados()
            ? `Resultado de búsqueda: ${pagina.totalElementos || 0} reporte(s) formal(es) encontrado(s).`
            : `Se muestran ${reportes.length} de ${pagina.totalElementos || 0} reportes de extravío activos registrados en RUPE.`;
    }

    if (!reportes.length) {
        contenedor.innerHTML = `
            <article class="module-card lost-card-empty">
                <i class="fas fa-magnifying-glass"></i>
                <h3>Sin coincidencias</h3>
                <p>No se encontró un reporte activo con ese folio. Revisa el dato capturado.</p>
            </article>
        `;
        pintarPaginacionExtraviados('paginacionReportes', pagina, cargarReportesExtraviados);
        aplicarVistaPorTipo();
        return;
    }

    // Se escapan textos y atributos antes de insertarlos para evitar HTML no deseado en datos publicos.
    contenedor.innerHTML = reportes.map((reporte) => `
        <article class="module-card lost-dog-card" data-folio="${escaparAtributoExtraviados(reporte.folio)}">
            <img src="${escaparAtributoExtraviados(urlFotoExtraviados(reporte.fotoPrincipalUrl))}" alt="Fotografía de ${escaparAtributoExtraviados(reporte.nombreMascota || 'perrito extraviado')}">
            <div class="lost-card-body">
                <span class="status-pill danger">${escaparExtraviados(etiquetaTipoFormalExtraviados(reporte.tipoReporte))}</span>
                <h3>${escaparExtraviados(reporte.nombreMascota || 'Sin nombre')}</h3>
                <p><strong>Folio:</strong> ${escaparExtraviados(reporte.folio || 'Sin folio')}</p>
                <p><strong>Raza:</strong> ${escaparExtraviados(reporte.razaMascota || 'No registrada')}</p>
                <p><strong>Señas:</strong> ${escaparExtraviados(resumirTextoExtraviados(reporte.senasParticulares || 'Sin señas capturadas', 110))}</p>
                <p><strong>Zona:</strong> ${escaparExtraviados(zonaExtraviados(reporte))}</p>
                <p><strong>Fecha:</strong> ${escaparExtraviados(formatearFechaExtraviados(reporte.fechaExtravio))}</p>
                <a class="module-btn lost-card-action" href="reportar-avistamiento.html?folio=${encodeURIComponent(reporte.folio || '')}">Reportar avistamiento</a>
            </div>
        </article>
    `).join('');
    pintarPaginacionExtraviados('paginacionReportes', pagina, cargarReportesExtraviados);
}

async function cargarAvistamientosSinFolio(pagina = 0) {
    const gridReportes = document.getElementById('listaReportesExtraviados');
    if (!gridReportes) return;

    let seccion = document.getElementById('avistamientosSinFolio');
    if (!seccion) {
        // La seccion se crea dinamicamente para mostrar avisos ciudadanos sin folio RUPE.
        seccion = document.createElement('section');
        seccion.className = 'module-panel lost-results-section';
        seccion.id = 'avistamientosSinFolio';
        seccion.innerHTML = `
            <div class="lost-section-heading">
                <div>
                    <span class="status-pill warn">Sin folio RUPE</span>
                    <h2>Avistamientos ciudadanos sin folio</h2>
                    <p>Estos avisos fueron registrados sin folio RUPE. Se muestran como apoyo ciudadano y no sustituyen un reporte de extravío del propietario.</p>
                </div>
            </div>
            <div class="lost-section-note">
                <strong>Criterio de visualización:</strong> los avistamientos sin resguardo se muestran durante 45 días; si el perrito está en resguardo permanecen visibles mientras el aviso esté activo.
            </div>
            <div class="module-grid lost-dogs-grid" id="listaAvistamientosSinFolio"></div>
            <div class="lost-pagination" id="paginacionAvistamientos"></div>
        `;
        gridReportes.insertAdjacentElement('afterend', seccion);
    }

    const contenedor = document.getElementById('listaAvistamientosSinFolio');
    try {
        contenedor.innerHTML = tarjetaCargaExtraviados('Cargando avistamientos', 'Consultando avisos ciudadanos.');
        const params = new URLSearchParams({
            pagina: String(Math.max(pagina, 0)),
            tamanio: String(RUPE_LOST_PAGE_SIZE)
        });
        if (filtrosExtraviados.texto) params.set('texto', filtrosExtraviados.texto);
        if (filtrosExtraviados.senas) params.set('senas', filtrosExtraviados.senas);
        if (filtrosExtraviados.zona) params.set('zona', filtrosExtraviados.zona);
        if (filtrosExtraviados.tipo === 'resguardo' || filtrosExtraviados.tipo === 'avistamiento') {
            params.set('tipo', filtrosExtraviados.tipo);
        }
        const respuesta = await fetch(`${RUPE_LOST_API}/publico/avistamientos-sin-folio-paginados?${params.toString()}`);
        const datos = await leerRespuestaExtraviados(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || 'No se pudieron consultar avistamientos.');

        paginaAvistamientos = datos.pagina || 0;
        pintarAvistamientosSinFolio(datos);

        const parametros = new URLSearchParams(window.location.search);
        if (parametros.get('mostrar') === 'avistamientos') {
            seccion.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    } catch (error) {
        contenedor.innerHTML = `
            <article class="module-card lost-card-empty">
                <i class="fas fa-triangle-exclamation"></i>
                <h3>No fue posible cargar avistamientos</h3>
                <p>Verifica que Spring Boot esté activo.</p>
            </article>
        `;
    }
}

function pintarAvistamientosSinFolio(pagina) {
    const contenedor = document.getElementById('listaAvistamientosSinFolio');
    const avistamientos = Array.isArray(pagina.contenido) ? pagina.contenido : [];
    const parametros = new URLSearchParams(window.location.search);
    const idAvistamientoNuevo = parametros.get('avistamiento');

    if (!avistamientos.length) {
        contenedor.innerHTML = `
            <article class="module-card lost-card-empty">
                <i class="fas fa-location-dot"></i>
                <h3>Sin avistamientos sin folio</h3>
                <p>Cuando alguien reporte un avistamiento sin folio aparecerá en esta sección.</p>
            </article>
        `;
        pintarPaginacionExtraviados('paginacionAvistamientos', pagina, cargarAvistamientosSinFolio);
        aplicarVistaPorTipo();
        return;
    }

    // No se muestran datos personales del resguardante; solo folio, zona, fecha, referencia y estado de resguardo.
    contenedor.innerHTML = avistamientos.map((aviso) => `
        <article class="module-card lost-dog-card ${String(aviso.idAvistamiento) === String(idAvistamientoNuevo) ? 'lost-dog-card-new' : ''}">
            <img src="${escaparAtributoExtraviados(urlFotoExtraviados(aviso.fotoAvistamientoUrl))}" alt="Fotografía del avistamiento ciudadano">
            <div class="lost-card-body">
                <span class="status-pill warn">${aviso.resguardado ? 'En resguardo' : 'Avistamiento'}</span>
                <h3>Avistamiento ciudadano</h3>
                <p><strong>Folio aviso:</strong> ${escaparExtraviados(aviso.folioAvistamiento || `AV-${new Date().getFullYear()}-${String(aviso.idAvistamiento).padStart(6, '0')}`)}</p>
                <p><strong>Zona:</strong> ${escaparExtraviados(zonaExtraviados(aviso))}</p>
                <p><strong>Fecha:</strong> ${escaparExtraviados(formatearFechaExtraviados(aviso.fechaAvistamiento))}</p>
                <p><strong>Referencias:</strong> ${escaparExtraviados(aviso.referencias || 'Sin referencias')}</p>
                <p><strong>Descripción:</strong> ${escaparExtraviados(resumirTextoExtraviados(aviso.descripcion || 'Sin descripción', 110))}</p>
                <p><strong>Resguardo:</strong> ${aviso.resguardado ? 'Sí' : 'No'}</p>
                <a class="module-btn lost-card-action" href="login.html?redirect=${encodeURIComponent(`mis-avistamientos.html?reclamarAvistamiento=${aviso.idAvistamiento}`)}">Creo que es mi mascota</a>
            </div>
        </article>
    `).join('');
    pintarPaginacionExtraviados('paginacionAvistamientos', pagina, cargarAvistamientosSinFolio);
}

function pintarPaginacionExtraviados(idContenedor, pagina, callback) {
    let contenedor = document.getElementById(idContenedor);
    if (!contenedor && idContenedor === 'paginacionReportes') {
        const grid = document.getElementById('listaReportesExtraviados');
        contenedor = document.createElement('div');
        contenedor.id = idContenedor;
        contenedor.className = 'lost-pagination';
        grid?.insertAdjacentElement('afterend', contenedor);
    }
    if (!contenedor) return;

    const totalElementos = Number(pagina.totalElementos || 0);
    const totalPaginas = Math.max(Number(pagina.totalPaginas || 0), 1);
    const paginaActual = Number(pagina.pagina || 0);

    // La paginacion queda visible aunque solo exista una pagina para que el usuario confirme el total consultado.
    if (!totalElementos) {
        contenedor.innerHTML = '<span class="lost-pagination-info">Sin registros para paginar.</span>';
        return;
    }

    contenedor.innerHTML = `
        <button type="button" class="module-btn secondary" ${paginaActual <= 0 ? 'disabled' : ''} data-page="${paginaActual - 1}">Anterior</button>
        <span class="lost-pagination-info">Pagina ${paginaActual + 1} de ${totalPaginas} | ${totalElementos} registro(s)</span>
        <button type="button" class="module-btn secondary" ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''} data-page="${paginaActual + 1}">Siguiente</button>
    `;

    contenedor.querySelectorAll('button[data-page]').forEach((boton) => {
        boton.addEventListener('click', () => callback(Number(boton.dataset.page)));
    });
}

function tarjetaCargaExtraviados(titulo, texto) {
    return `
        <article class="module-card lost-card-empty">
            <i class="fas fa-spinner fa-spin"></i>
            <h3>${escaparExtraviados(titulo)}</h3>
            <p>${escaparExtraviados(texto)}</p>
        </article>
    `;
}

function urlFotoExtraviados(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http')) return url;
    return `${RUPE_LOST_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}

function zonaExtraviados(reporte) {
    return [reporte.colonia, reporte.municipio].filter(Boolean).join(', ') || 'Zona no capturada';
}

function formatearFechaExtraviados(fecha) {
    if (!fecha) return 'No registrada';
    const [anio, mes, dia] = String(fecha).split('-');
    return anio && mes && dia ? `${dia}/${mes}/${anio}` : fecha;
}

function etiquetaTipoFormalExtraviados(tipo) {
    return String(tipo || '').toUpperCase() === 'ROBO' ? 'Robo' : 'Extraviado';
}

function resumirTextoExtraviados(valor, maximo) {
    const texto = String(valor || '').trim();
    if (texto.length <= maximo) return texto;
    return `${texto.slice(0, maximo - 3)}...`;
}

async function leerRespuestaExtraviados(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function escaparExtraviados(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function escaparAtributoExtraviados(valor) {
    return escaparExtraviados(valor).replaceAll('`', '&#096;');
}
