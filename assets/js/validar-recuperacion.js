const RUPE_VALIDACION_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

let avisoValidacionActual = null;

document.addEventListener('DOMContentLoaded', () => {
    cargarValidacionRecuperacion();
    document.getElementById('formValidarRecuperacion')?.addEventListener('submit', confirmarValidacionRecuperacion);
});

async function cargarValidacionRecuperacion() {
    const id = new URLSearchParams(window.location.search).get('id');
    if (!id) {
        mostrarValidacionRecuperacion('No se recibio el identificador del avistamiento.', 'error');
        return;
    }
    try {
        const respuesta = await fetch(`${RUPE_VALIDACION_API}/avistamientos/mis`, { credentials: 'include' });
        const datos = await leerRespuestaValidacion(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo cargar el avistamiento.');
        avisoValidacionActual = (Array.isArray(datos) ? datos : [])
            .find((aviso) => String(aviso.idAvistamiento) === String(id));
        if (!avisoValidacionActual) throw new Error('Avistamiento no encontrado para el usuario activo.');
        pintarValidacionRecuperacion(avisoValidacionActual);
    } catch (error) {
        mostrarValidacionRecuperacion(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarValidacionRecuperacion(aviso) {
    asignarValidacion('tituloValidacion', `Validar resguardo de ${aviso.nombreMascota || 'perrito'}`);
    asignarValidacion('subtituloValidacion', `Reporte relacionado: ${aviso.folio || 'Sin folio'}`);
    asignarValidacion('folioReporte', aviso.folio || 'Sin folio');
    asignarValidacion('nombrePerrito', aviso.nombreMascota || 'Sin dato');
    asignarValidacion('razaPerrito', 'Consulta el detalle del perrito');
    asignarValidacion('colorPerrito', 'Consulta el detalle del perrito');
    asignarValidacion('fechaAviso', formatearFechaValidacion(aviso.fechaAvistamiento));
    asignarValidacion('tipoAviso', aviso.resguardado ? 'Posible resguardo' : 'Avistamiento ciudadano');
    asignarValidacion('ubicacionAviso', [aviso.colonia, aviso.municipio].filter(Boolean).join(', ') || 'Sin ubicacion');
    asignarValidacion('estadoContacto', aviso.validadoDueno ? contactoValidacion(aviso) : 'Protegido');
    asignarValidacion('descripcionAviso', aviso.descripcion || aviso.referencias || 'Sin descripcion');

    const fotoRegistrada = document.getElementById('fotoRegistrada');
    const fotoResguardo = document.getElementById('fotoResguardo');
    if (fotoRegistrada) fotoRegistrada.src = normalizarFotoValidacion(aviso.fotoMascotaUrl);
    if (fotoResguardo) fotoResguardo.src = normalizarFotoValidacion(aviso.fotoAvistamientoUrl || aviso.fotoMascotaUrl);
}

async function confirmarValidacionRecuperacion(event) {
    event.preventDefault();
    const decision = document.getElementById('decision')?.value || '';
    const comentario = document.getElementById('comentario')?.value.trim() || '';
    const autorizarContacto = document.getElementById('autorizarContacto')?.checked;

    if (!decision) {
        mostrarValidacionRecuperacion('Selecciona si la evidencia corresponde o no a tu perrito.', 'warn');
        return;
    }
    if (!autorizarContacto) {
        mostrarValidacionRecuperacion('Debes confirmar la validacion antes de continuar.', 'warn');
        return;
    }
    await enviarDecisionRecuperacion(decision, comentario);
}

async function descartarRecuperacion() {
    if (!confirm('¿Deseas marcar este resguardo como no coincidente?')) return;
    await enviarDecisionRecuperacion('no', 'Descartado desde validacion de recuperacion.');
}

async function enviarDecisionRecuperacion(decision, comentario) {
    if (!avisoValidacionActual) return;
    try {
        const respuesta = await fetch(`${RUPE_VALIDACION_API}/avistamientos/${encodeURIComponent(avisoValidacionActual.idAvistamiento)}/validacion`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ decision, comentario })
        });
        const datos = await leerRespuestaValidacion(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo registrar la validacion.');
        mostrarValidacionRecuperacion(datos.mensaje || 'Validacion registrada correctamente.', 'success');
        window.location.href = 'mis-avistamientos.html';
    } catch (error) {
        mostrarValidacionRecuperacion(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function contactoValidacion(aviso) {
    return [aviso.nombreResguardante, aviso.correoResguardante, aviso.telefonoResguardante].filter(Boolean).join(' · ') || 'Sin contacto';
}

function asignarValidacion(id, valor) {
    const nodo = document.getElementById(id);
    if (nodo) nodo.textContent = valor;
}

function normalizarFotoValidacion(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http') || url.startsWith('assets/')) return url;
    return `${RUPE_VALIDACION_API.replace('/api', '')}${url}`;
}

function formatearFechaValidacion(valor) {
    if (!valor) return 'Sin fecha';
    const fecha = new Date(`${valor}T00:00:00`);
    if (Number.isNaN(fecha.getTime())) return 'Sin fecha';
    return fecha.toLocaleDateString('es-MX');
}

async function leerRespuestaValidacion(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function mostrarValidacionRecuperacion(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}
