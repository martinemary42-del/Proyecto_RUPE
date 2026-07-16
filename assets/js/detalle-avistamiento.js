const RUPE_DETALLE_AVISO_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

let avistamientoActual = null;

document.addEventListener('DOMContentLoaded', cargarDetalleAvistamiento);

async function cargarDetalleAvistamiento() {
    const parametros = new URLSearchParams(window.location.search);
    const idAvistamiento = parametros.get('id');
    if (!idAvistamiento) {
        mostrarDetalleAvistamiento('No se recibio el identificador del avistamiento.', 'error');
        return;
    }

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_AVISO_API}/avistamientos/mis`, { credentials: 'include' });
        const datos = await leerRespuestaDetalleAvistamiento(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo cargar el avistamiento.');
        avistamientoActual = (Array.isArray(datos) ? datos : [])
            .find((aviso) => String(aviso.idAvistamiento) === String(idAvistamiento));
        if (!avistamientoActual) throw new Error('Avistamiento no encontrado para el usuario activo.');
        pintarDetalleAvistamiento(avistamientoActual);
    } catch (error) {
        mostrarDetalleAvistamiento(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarDetalleAvistamiento(aviso) {
    const tipo = aviso.resguardado ? 'Posible resguardo' : 'Avistamiento ciudadano';
    const ubicacion = [aviso.colonia, aviso.municipio, aviso.estado].filter(Boolean).join(', ');
    asignarTexto('tituloAvistamiento', `${tipo} · ${aviso.folioAvistamiento || aviso.folio || 'Sin folio'}`);
    asignarTexto('subtituloAvistamiento', `Aviso relacionado con ${aviso.nombreMascota || 'tu reporte'}.`);
    asignarTexto('estatusAvistamiento', aviso.estatus || 'Pendiente');
    asignarTexto('folioAvistamiento', aviso.folio || aviso.folioAvistamiento || 'Sin folio');
    asignarTexto('nombrePerrito', aviso.nombreMascota || 'Sin dato');
    asignarTexto('tipoAvistamiento', tipo);
    asignarTexto('fechaAvistamiento', formatearFechaDetalle(aviso.fechaAvistamiento));
    asignarTexto('estadoAvistamiento', aviso.estado || 'Sin dato');
    asignarTexto('municipioAvistamiento', aviso.municipio || 'Sin dato');
    asignarTexto('coloniaAvistamiento', aviso.colonia || 'Sin dato');
    asignarTexto('cpAvistamiento', aviso.codigoPostal || 'Sin dato');
    asignarTexto('referenciasAvistamiento', aviso.referencias || ubicacion || 'Sin referencias');
    asignarTexto('descripcionAvistamiento', aviso.descripcion || 'Sin descripcion');
    asignarTexto('resguardoAvistamiento', aviso.resguardado ? 'Si' : 'No');
    asignarTexto('contactoLiberado', contactoAvistamiento(aviso));

    const foto = document.getElementById('fotoAvistamiento');
    if (foto) {
        foto.src = normalizarFotoDetalle(aviso.fotoAvistamientoUrl || aviso.fotoMascotaUrl);
        foto.alt = `Fotografia del avistamiento de ${aviso.nombreMascota || 'perrito'}`;
    }

    const estatusElemento = document.getElementById('estatusAvistamiento');
    if (estatusElemento) {
        estatusElemento.classList.remove('estatus-pendiente', 'estatus-validado', 'estatus-descartado');
        const estatus = String(aviso.estatus || '').toLowerCase();
        if (estatus.includes('valid')) estatusElemento.classList.add('estatus-validado');
        else if (estatus.includes('descart')) estatusElemento.classList.add('estatus-descartado');
        else estatusElemento.classList.add('estatus-pendiente');
    }
}

async function validarAvistamiento() {
    await enviarDecisionAvistamiento('si');
}

async function descartarAvistamiento() {
    await enviarDecisionAvistamiento('no');
}

async function enviarDecisionAvistamiento(decision) {
    if (!avistamientoActual) return;
    const texto = decision === 'si'
        ? 'Confirmar que este avistamiento corresponde a tu perrito.'
        : 'Descartar este avistamiento como no coincidente.';
    if (!confirm(texto)) return;
    try {
        const respuesta = await fetch(`${RUPE_DETALLE_AVISO_API}/avistamientos/${encodeURIComponent(avistamientoActual.idAvistamiento)}/validacion`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ decision, comentario: decision === 'si' ? 'Validado desde detalle de avistamiento.' : 'Descartado desde detalle de avistamiento.' })
        });
        const datos = await leerRespuestaDetalleAvistamiento(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo registrar la validacion.');
        mostrarDetalleAvistamiento(datos.mensaje || 'Validacion registrada correctamente.', 'success');
        avistamientoActual = datos;
        pintarDetalleAvistamiento(datos);
    } catch (error) {
        mostrarDetalleAvistamiento(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function contactoAvistamiento(aviso) {
    if (!aviso.resguardado) return 'No aplica';
    if (!aviso.validadoDueno) return 'Pendiente de validacion';
    return [aviso.nombreResguardante, aviso.correoResguardante, aviso.telefonoResguardante].filter(Boolean).join(' · ') || 'Sin contacto';
}

function asignarTexto(id, valor) {
    const nodo = document.getElementById(id);
    if (nodo) nodo.textContent = valor;
}

function normalizarFotoDetalle(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http') || url.startsWith('assets/')) return url;
    return `${RUPE_DETALLE_AVISO_API.replace('/api', '')}${url}`;
}

function formatearFechaDetalle(valor) {
    if (!valor) return 'Sin fecha';
    const fecha = new Date(`${valor}T00:00:00`);
    if (Number.isNaN(fecha.getTime())) return 'Sin fecha';
    return fecha.toLocaleDateString('es-MX');
}

async function leerRespuestaDetalleAvistamiento(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function mostrarDetalleAvistamiento(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}
