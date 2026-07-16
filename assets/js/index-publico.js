const RUPE_PUBLICO_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_PUBLICO_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';

document.addEventListener('DOMContentLoaded', () => {
    cargarResumenPublico();
    cargarReportesRecientes();
    configurarBusquedaFolio();
});

async function cargarResumenPublico() {
    try {
        const respuesta = await fetch(`${RUPE_PUBLICO_API}/publico/resumen`);
        const resumen = await leerRespuestaPublica(respuesta);
        if (!respuesta.ok) throw new Error(resumen.mensaje || 'No se pudo consultar el resumen.');

        setStatPublica('perritosRegistrados', resumen.perritosRegistrados);
        setStatPublica('reportesActivos', resumen.reportesActivos);
        setStatPublica('perritosRecuperados', resumen.perritosRecuperados);
        setStatPublica('avistamientosCiudadanos', resumen.avistamientosCiudadanos);
    } catch (error) {
        mostrarAvisoPublico('No se pudieron cargar las estadísticas oficiales.', 'warn');
    }
}

async function cargarReportesRecientes() {
    const contenedor = document.getElementById('reportesRecientes');
    if (!contenedor) return;

    try {
        const [respuestaReportes, respuestaAvistamientos] = await Promise.all([
            fetch(`${RUPE_PUBLICO_API}/publico/reportes-activos`),
            fetch(`${RUPE_PUBLICO_API}/publico/avistamientos-sin-folio`)
        ]);
        const reportes = await leerRespuestaPublica(respuestaReportes);
        const avistamientos = await leerRespuestaPublica(respuestaAvistamientos);
        if (!respuestaReportes.ok) throw new Error(reportes.mensaje || 'No se pudieron consultar reportes.');
        if (!respuestaAvistamientos.ok) throw new Error(avistamientos.mensaje || 'No se pudieron consultar avistamientos.');

        const elementos = [
            ...normalizarReportesPublicos(Array.isArray(reportes) ? reportes : []),
            ...normalizarAvistamientosPublicos(Array.isArray(avistamientos) ? avistamientos : [])
        ].sort((a, b) => new Date(b.fechaOrden) - new Date(a.fechaOrden));

        if (!elementos.length) {
            contenedor.innerHTML = `
                <div class="dog-card dog-card-empty">
                    <img src="assets/img/logo/logo-rupe.png" alt="Sin reportes activos">
                    <h4>Sin reportes</h4>
                <span>Aun no hay reportes o avistamientos activos</span>
                    <div class="dog-status">RUPE</div>
                </div>
            `;
            return;
        }

        iniciarCarruselPublico(contenedor, elementos);
    } catch (error) {
        contenedor.innerHTML = `
            <div class="dog-card dog-card-empty">
                <img src="assets/img/logo/logo-rupe.png" alt="Error al cargar reportes">
                <h4>No disponible</h4>
                <span>Verifica que el backend este encendido</span>
                <div class="dog-status">RUPE</div>
            </div>
        `;
    }
}

function normalizarReportesPublicos(reportes) {
    return reportes.map((reporte) => ({
        tipo: 'folio',
        titulo: reporte.nombreMascota || 'Sin nombre',
        subtitulo: reporte.folio || 'Reporte con folio',
        zona: zonaPublica(reporte),
        fecha: reporte.fechaExtravio,
        fechaOrden: reporte.fechaExtravio || '1900-01-01',
        foto: reporte.fotoPrincipalUrl,
        estado: 'Extraviado',
        enlace: `perritos-extraviados.html?folio=${encodeURIComponent(reporte.folio || '')}`
    }));
}

function normalizarAvistamientosPublicos(avistamientos) {
    return avistamientos.map((aviso) => ({
        tipo: 'sin-folio',
        titulo: aviso.resguardado ? 'Posible resguardo' : 'Avistamiento',
        subtitulo: aviso.folioAvistamiento || `AV-${new Date().getFullYear()}-${String(aviso.idAvistamiento).padStart(6, '0')}`,
        zona: zonaPublica(aviso),
        fecha: aviso.fechaAvistamiento,
        fechaOrden: aviso.fechaAvistamiento || '1900-01-01',
        foto: aviso.fotoAvistamientoUrl,
        estado: aviso.resguardado ? 'En resguardo' : 'Avistamiento',
        enlace: `perritos-extraviados.html?mostrar=avistamientos&avistamiento=${encodeURIComponent(aviso.idAvistamiento)}`
    }));
}

function iniciarCarruselPublico(contenedor, elementos) {
    let indice = 0;
    const visibles = 3;

    const pintar = () => {
        const grupo = [];
        for (let i = 0; i < Math.min(visibles, elementos.length); i += 1) {
            grupo.push(elementos[(indice + i) % elementos.length]);
        }
        contenedor.innerHTML = grupo.map(tarjetaCarruselPublico).join('');
        indice = (indice + 1) % elementos.length;
    };

    pintar();
    if (elementos.length > visibles) {
        setInterval(pintar, 4500);
    }
}

function tarjetaCarruselPublico(item) {
    // El inicio solo muestra una tarjeta compacta; el detalle se consulta en Perritos Extraviados.
    return `
        <a class="dog-card dog-card-link dog-card-${item.tipo}" href="${escaparAtributoPublico(item.enlace)}">
            <div class="dog-card-photo">
                <img src="${escaparAtributoPublico(urlFotoPublica(item.foto))}" alt="Fotografia de ${escaparAtributoPublico(item.titulo)}">
                <div class="dog-status">${escaparPublico(item.estado)}</div>
            </div>
            <div class="dog-card-body">
                <h4>${escaparPublico(item.titulo)}</h4>
                <span>${escaparPublico(item.subtitulo)}</span>
                <small>Ver datos para reconocerlo</small>
            </div>
        </a>
    `;
}

function configurarBusquedaFolio() {
    const input = document.querySelector('.hero-search input');
    const boton = document.querySelector('.hero-search button');
    if (!input || !boton) return;

    // El buscador del inicio redirige a la pagina publica, donde se filtra por folio, nombre, zona o señas.
    const buscar = () => buscarTextoPublico(input.value);
    boton.addEventListener('click', buscar);
    input.addEventListener('keydown', (evento) => {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            buscar();
        }
    });
}

function buscarTextoPublico(valor) {
    const texto = String(valor || '').trim();
    if (!texto) {
        mostrarAvisoPublico('Ingresa un folio, nombre o seña visible para buscar.', 'warn');
        return;
    }
    window.location.href = `perritos-extraviados.html?folio=${encodeURIComponent(texto)}`;
}

function setStatPublica(nombre, valor) {
    const elemento = document.querySelector(`[data-stat="${nombre}"]`);
    if (elemento) elemento.textContent = Number(valor || 0).toLocaleString('es-MX');
}

function urlFotoPublica(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http')) return url;
    return `${RUPE_PUBLICO_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}

function zonaPublica(reporte) {
    return [reporte.colonia, reporte.municipio].filter(Boolean).join(', ') || 'Zona no capturada';
}

function textoEstatusPublico(estatus) {
    const valor = String(estatus || '').toLowerCase();
    if (valor.includes('recuper')) return 'Recuperado';
    return 'Extraviado';
}

async function leerRespuestaPublica(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function mostrarAvisoPublico(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    }
}

function escaparPublico(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function escaparAtributoPublico(valor) {
    return escaparPublico(valor).replaceAll('`', '&#096;');
}
