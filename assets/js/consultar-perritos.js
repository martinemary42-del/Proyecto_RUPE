/* =====================================================
   CONSULTAR PERRITOS - RUPE
   Carga perritos reales del usuario desde Spring Boot.
===================================================== */

const RUPE_CONSULTA_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_CONSULTA_BACKEND_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';

const buscarNombre = document.getElementById('buscarNombre');
const filtroEstatus = document.getElementById('filtroEstatus');
const listaPerritos = document.getElementById('listaPerritos');
const sinResultados = document.getElementById('sinResultados');

let perritosCargados = [];

document.addEventListener('DOMContentLoaded', () => {
    buscarNombre?.addEventListener('input', filtrarPerritos);
    filtroEstatus?.addEventListener('change', filtrarPerritos);
    cargarPerritos();
});

async function cargarPerritos() {
    if (!listaPerritos) return;
    listaPerritos.innerHTML = '<p class="estado-carga">Cargando perritos registrados...</p>';

    try {
        const respuesta = await fetch(`${RUPE_CONSULTA_API_BASE}/mascotas/mis`, {
            credentials: 'include'
        });
        const contenido = await leerRespuesta(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudieron cargar tus perritos.');
        }

        perritosCargados = Array.isArray(contenido) ? contenido : [];
        renderizarPerritos(perritosCargados);
    } catch (error) {
        listaPerritos.innerHTML = '';
        mostrarAviso(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function renderizarPerritos(perritos) {
    if (!listaPerritos) return;
    listaPerritos.innerHTML = '';

    if (!perritos.length) {
        if (sinResultados) sinResultados.style.display = 'block';
        return;
    }

    if (sinResultados) sinResultados.style.display = 'none';
    perritos.forEach((perrito) => {
        listaPerritos.appendChild(crearTarjetaPerrito(perrito));
    });
}

function crearTarjetaPerrito(perrito) {
    const article = document.createElement('article');
    article.className = 'perrito-item';
    article.dataset.nombre = (perrito.nombre || '').toLowerCase();
    article.dataset.estatus = 'registrado';

    const ubicacion = [perrito.coloniaReferencia, perrito.municipioReferencia, perrito.estadoReferencia]
        .filter(Boolean)
        .join(', ');

    article.innerHTML = `
        <div class="perrito-foto">
            <img src="${escaparAtributo(urlFotoPerrito(perrito.fotoPrincipalUrl))}" alt="Fotografia de ${escapar(perrito.nombre || 'perrito')}" onerror="this.onerror=null;this.src='assets/img/logo/logo-rupe.png';">
        </div>
        <div class="perrito-datos">
            <span class="estatus estatus-registrado">Registrado</span>
            <h3>${escapar(perrito.nombre || 'Sin nombre')}</h3>
            <p><strong>Raza:</strong> ${escapar(perrito.raza || 'Sin dato')}</p>
            <p><strong>Color:</strong> ${escapar(perrito.color || 'Sin dato')}</p>
            <p><strong>Sexo:</strong> ${escapar(perrito.sexo || 'Sin dato')}</p>
            <p><strong>Señas:</strong> ${escapar(resumirTextoPerrito(perrito.senasParticulares || 'Sin señas capturadas', 90))}</p>
            <p><strong>Ubicacion:</strong> ${escapar(ubicacion || 'Sin dato')}</p>
        </div>
        <div class="perrito-acciones">
            <a href="detalle-perrito.html?id=${perrito.idMascota}" class="btn-accion">
                <i class="fas fa-eye"></i>
                Ver detalle
            </a>
            <a href="editar-perrito.html?id=${perrito.idMascota}" class="btn-accion">
                <i class="fas fa-pen"></i>
                Editar
            </a>
            <a href="reporte-extravio.html?idMascota=${perrito.idMascota}" class="btn-accion btn-extravio">
                <i class="fas fa-triangle-exclamation"></i>
                Reportar extravio o robo
            </a>
            <button type="button" class="btn-baja" data-id="${perrito.idMascota}" data-nombre="${escaparAtributo(perrito.nombre || 'perrito')}">
                <i class="fas fa-trash-can"></i>
                Borrar registro
            </button>
        </div>
    `;

    const botonDesactivar = article.querySelector('.btn-baja');
    botonDesactivar?.addEventListener('click', () => desactivarPerrito(perrito.idMascota, perrito.nombre || 'perrito', botonDesactivar));

    return article;
}

function filtrarPerritos() {
    const textoBusqueda = (buscarNombre?.value || '').toLowerCase().trim();
    const estatusSeleccionado = filtroEstatus?.value || '';
    const tarjetas = listaPerritos?.querySelectorAll('.perrito-item') || [];
    let totalVisibles = 0;

    tarjetas.forEach((perrito) => {
        const nombre = perrito.dataset.nombre || '';
        const estatus = perrito.dataset.estatus || '';
        const coincideNombre = nombre.includes(textoBusqueda);
        const coincideEstatus = estatusSeleccionado === '' || estatus === estatusSeleccionado;

        if (coincideNombre && coincideEstatus) {
            perrito.style.display = 'grid';
            totalVisibles++;
        } else {
            perrito.style.display = 'none';
        }
    });

    if (sinResultados) {
        sinResultados.style.display = totalVisibles === 0 ? 'block' : 'none';
    }
}

function limpiarFiltros() {
    if (buscarNombre) buscarNombre.value = '';
    if (filtroEstatus) filtroEstatus.value = '';
    filtrarPerritos();
}

async function desactivarPerrito(idMascota, nombrePerrito, boton) {
    const confirmar = confirm(
        'Deseas borrar el registro de ' + nombrePerrito + '?\\n\\n' +
        'No se eliminara fisicamente de la base de datos. Dejara de verse en Mis Perritos, pero se conservara para historial, reportes y estadisticas.'
    );

    if (!confirmar || boton?.disabled) return;

    const textoOriginal = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Borrando...';

    try {
        const respuesta = await fetch(`${RUPE_CONSULTA_API_BASE}/mascotas/${idMascota}/desactivar`, {
            method: 'PUT',
            credentials: 'include'
        });
        const contenido = await leerRespuesta(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo borrar el registro.');
        }

        perritosCargados = perritosCargados.filter((perrito) => perrito.idMascota !== idMascota);
        renderizarPerritos(perritosCargados);
        mostrarAviso(contenido.mensaje || 'Registro borrado de la vista correctamente. El historial se conserva para trazabilidad.', 'success');
    } catch (error) {
        boton.disabled = false;
        boton.innerHTML = textoOriginal;
        mostrarAviso(error.message || 'No se pudo conectar con el backend.', 'error');
    }
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

function escapar(valor) {
    return String(valor)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function escaparAtributo(valor) {
    return escapar(valor).replaceAll('`', '&#096;');
}

function resumirTextoPerrito(valor, maximo) {
    const texto = String(valor || '').trim();
    if (texto.length <= maximo) return texto;
    return `${texto.slice(0, maximo - 3)}...`;
}

function urlFotoPerrito(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http')) return url;
    return `${RUPE_CONSULTA_BACKEND_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}
