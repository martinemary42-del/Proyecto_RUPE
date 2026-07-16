/* =====================================================
   DETALLE PERRITO - RUPE
   Carga el detalle real de la mascota seleccionada.
===================================================== */

const RUPE_DETALLE_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_DETALLE_BACKEND_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';

document.addEventListener('DOMContentLoaded', cargarDetallePerrito);

async function cargarDetallePerrito() {
    const parametros = new URLSearchParams(window.location.search);
    const idPerrito = parametros.get('id');
    if (!idPerrito) {
        mostrarAviso('No se recibio el identificador del perrito.', 'error');
        return;
    }

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_API_BASE}/mascotas/${idPerrito}`, {
            credentials: 'include'
        });
        const perrito = await leerRespuesta(respuesta);
        if (!respuesta.ok) {
            throw new Error(perrito.mensaje || perrito.error || 'No se pudo consultar el perrito.');
        }
        pintarDetalle(perrito);
        prepararEnlaces(idPerrito);
    } catch (error) {
        mostrarAviso(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarDetalle(perrito) {
    const headerTitulo = document.querySelector('.perrito-card-header h2');
    const headerDescripcion = document.querySelector('.perrito-card-header p');
    const foto = document.querySelector('.detalle-foto img');
    const estatus = document.querySelector('.estatus');

    if (headerTitulo) headerTitulo.textContent = 'Detalle de ' + (perrito.nombre || 'perrito');
    if (headerDescripcion) headerDescripcion.textContent = 'Registro activo dentro del sistema RUPE.';
    if (foto) {
        foto.src = urlFotoPerritoDetalle(perrito.fotoPrincipalUrl);
        foto.alt = 'Fotografia principal del perrito ' + (perrito.nombre || '');
    }
    if (estatus) estatus.textContent = perrito.activo ? 'Registrado' : 'Inactivo';

    reemplazarDetalle('Nombre', perrito.nombre);
    reemplazarDetalle('Sexo', perrito.sexo);
    reemplazarDetalle('Tipo de mascota', perrito.tipoMascota);
    reemplazarDetalle('Edad aproximada', perrito.edadAproximada);
    reemplazarDetalle('Raza', perrito.raza);
    reemplazarDetalle('Descripcion de mezcla', perrito.mezclaRaza);
    reemplazarDetalle('Descripción de mezcla', perrito.mezclaRaza);
    reemplazarDetalle('Color principal', perrito.color);
    reemplazarDetalle('Descripcion de color', perrito.descripcionColor);
    reemplazarDetalle('Descripción de color', perrito.descripcionColor);
    reemplazarDetalle('Collar o placa', perrito.collarPlaca);
    reemplazarDetalle('Fecha de registro', formatearFecha(perrito.fechaRegistro));
    reemplazarDetalle('Estado', perrito.estadoReferencia);
    reemplazarDetalle('Municipio / Alcaldia', perrito.municipioReferencia);
    reemplazarDetalle('Municipio / Alcaldía', perrito.municipioReferencia);
    reemplazarDetalle('Colonia', perrito.coloniaReferencia);
    reemplazarDetalle('Codigo postal', perrito.codigoPostal);
    reemplazarDetalle('Código postal', perrito.codigoPostal);
    reemplazarDetalle('Calle', perrito.calle);
    reemplazarDetalle('Numero', perrito.numero);
    reemplazarDetalle('Número', perrito.numero);
    reemplazarBloque('Señas particulares', perrito.senasParticulares || 'Sin senas particulares registradas.');
    reemplazarBloque('Condición médica', perrito.condicionMedica || 'Sin condicion medica registrada.');
}

function urlFotoPerritoDetalle(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http')) return url;
    return `${RUPE_DETALLE_BACKEND_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}

function prepararEnlaces(idPerrito) {
    document.querySelectorAll('.detalle-acciones a').forEach((enlace) => {
        if (enlace.href.includes('editar-perrito.html')) {
            enlace.href = `editar-perrito.html?id=${encodeURIComponent(idPerrito)}`;
        }
        if (enlace.href.includes('reporte-extravio.html')) {
            enlace.href = `reporte-extravio.html?idMascota=${encodeURIComponent(idPerrito)}`;
        }
    });
}

function reemplazarDetalle(etiqueta, valor) {
    document.querySelectorAll('.detalle-item').forEach((item) => {
        const span = item.querySelector('span');
        const strong = item.querySelector('strong');
        if (span && strong && normalizar(span.textContent) === normalizar(etiqueta)) {
            strong.textContent = valor || 'Sin dato';
        }
    });
}

function reemplazarBloque(etiqueta, valor) {
    document.querySelectorAll('.detalle-bloque').forEach((bloque) => {
        const span = bloque.querySelector('span');
        const parrafo = bloque.querySelector('p');
        if (span && parrafo && normalizar(span.textContent) === normalizar(etiqueta)) {
            parrafo.textContent = valor || 'Sin dato';
        }
    });
}

function formatearFecha(fecha) {
    if (!fecha) return 'Sin dato';
    const fechaObjeto = new Date(fecha);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin dato';
    return fechaObjeto.toLocaleDateString('es-MX');
}

function normalizar(texto) {
    return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
}

function mostrarAviso(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}

async function leerRespuesta(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}
