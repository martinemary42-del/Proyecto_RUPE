/* =====================================================
   EDITAR PERRITO - RUPE
   Carga y actualiza la mascota seleccionada.
===================================================== */

const RUPE_EDITAR_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_EDITAR_BACKEND_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';
const parametrosEditar = new URLSearchParams(window.location.search);
const idMascotaEditar = parametrosEditar.get('id');
const formEditarPerrito = document.getElementById('formMascota');
let ubicacionesEditar = [];
let perritoEditarActual = null;

document.addEventListener('DOMContentLoaded', iniciarEdicionPerrito);

async function iniciarEdicionPerrito() {
    if (!idMascotaEditar) {
        mostrarAvisoEditar('No se recibio el identificador del perrito.', 'error');
        return;
    }

    await cargarCatalogosEditar();
    await cargarDetalleEditar();
    if (formEditarPerrito) {
        formEditarPerrito.addEventListener('submit', guardarCambiosPerrito);
        document.getElementById('foto_principal')?.addEventListener('change', mostrarNuevaFotoSeleccionada);
    }
}

async function cargarCatalogosEditar() {
    try {
        const [tipos, razas, colores, ubicaciones] = await Promise.all([
            obtenerCatalogoEditar('/catalogos/tipos-mascota'),
            obtenerCatalogoEditar('/catalogos/razas'),
            obtenerCatalogoEditar('/catalogos/colores'),
            obtenerCatalogoEditar('/catalogos/ubicaciones')
        ]);

        llenarSelectEditar('id_tipo_mascota', tipos, 'Selecciona');
        llenarSelectEditar('id_raza', razas, 'Selecciona');
        llenarSelectEditar('id_color', colores, 'Selecciona');
        configurarUbicacionesEditar(ubicaciones.estados || []);
    } catch (error) {
        mostrarAvisoEditar('No se pudieron cargar los catalogos. Verifica que el backend este encendido.', 'error');
    }
}

async function cargarDetalleEditar() {
    try {
        const respuesta = await fetch(`${RUPE_EDITAR_API_BASE}/mascotas/${idMascotaEditar}`, {
            credentials: 'include'
        });
        const perrito = await leerRespuestaEditar(respuesta);
        if (!respuesta.ok) {
            throw new Error(perrito.mensaje || perrito.error || 'No se pudo consultar el perrito.');
        }
        perritoEditarActual = perrito;
        pintarFormulario(perrito);
    } catch (error) {
        mostrarAvisoEditar(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarFormulario(perrito) {
    asignarValor('nombre', perrito.nombre);
    asignarValor('sexo', perrito.sexo);
    asignarValor('id_tipo_mascota', perrito.tipoMascota || 'Perro');
    asignarValor('edad_aproximada', perrito.edadAproximada);
    asignarValor('id_raza', perrito.raza);
    asignarValor('mezcla_raza', perrito.mezclaRaza);
    asignarValor('id_color', perrito.color);
    asignarValor('descripcion_color', perrito.descripcionColor);
    asignarValor('senas_particulares', perrito.senasParticulares);
    asignarValor('condicion_medica', perrito.condicionMedica);
    asignarValor('collar_placa', perrito.collarPlaca);
    seleccionarUbicacionActual(perrito);
    asignarValor('calle', perrito.calle);
    asignarValor('numero', perrito.numero);
    actualizarFotoActual(perrito.fotoPrincipalUrl);

    const cancelar = document.querySelector('.btn-secundario');
    if (cancelar) cancelar.href = `detalle-perrito.html?id=${encodeURIComponent(idMascotaEditar)}`;
}

function configurarUbicacionesEditar(estados) {
    ubicacionesEditar = estados;
    const estadoSelect = document.getElementById('id_estado');
    const municipioSelect = document.getElementById('id_municipio');
    const coloniaSelect = document.getElementById('id_colonia');
    const cpInput = obtenerCampoCodigoPostal();
    if (!estadoSelect || !municipioSelect || !coloniaSelect || !cpInput) return;

    estadoSelect.innerHTML = '<option value="">Selecciona un estado</option>';
    municipioSelect.innerHTML = '<option value="">Primero selecciona un estado</option>';
    coloniaSelect.innerHTML = '<option value="">Primero selecciona municipio/alcaldia</option>';
    cpInput.value = '';

    estados.forEach((estado) => {
        const option = document.createElement('option');
        option.value = estado.id;
        option.textContent = estado.nombre;
        estadoSelect.appendChild(option);
    });

    estadoSelect.onchange = () => {
        llenarMunicipiosEditar();
    };

    municipioSelect.onchange = () => {
        llenarColoniasEditar();
    };

    coloniaSelect.onchange = () => {
        const opcion = coloniaSelect.options[coloniaSelect.selectedIndex];
        cpInput.value = opcion?.dataset.cp || '';
    };
}

function seleccionarUbicacionActual(perrito) {
    const estadoSelect = document.getElementById('id_estado');
    if (!estadoSelect || !ubicacionesEditar.length) {
        asignarValor('id_estado', perrito.estadoReferencia);
        asignarValor('id_municipio', perrito.municipioReferencia);
        asignarValor('id_colonia', perrito.coloniaReferencia);
        asignarValor('cp', perrito.codigoPostal);
        return;
    }

    seleccionarPorTextoOValor(estadoSelect, perrito.estadoReferencia);
    llenarMunicipiosEditar(perrito.municipioReferencia, perrito.coloniaReferencia, perrito.codigoPostal);
}

function llenarMunicipiosEditar(municipioActual = '', coloniaActual = '', cpActual = '') {
    const estadoSelect = document.getElementById('id_estado');
    const municipioSelect = document.getElementById('id_municipio');
    const coloniaSelect = document.getElementById('id_colonia');
    const cpInput = obtenerCampoCodigoPostal();
    const estado = ubicacionesEditar.find(item => String(item.id) === estadoSelect?.value);
    if (!municipioSelect || !coloniaSelect || !cpInput) return;

    municipioSelect.innerHTML = '<option value="">Selecciona municipio/alcaldia</option>';
    coloniaSelect.innerHTML = '<option value="">Primero selecciona municipio/alcaldia</option>';
    cpInput.value = '';

    (estado?.municipios || []).forEach((municipio) => {
        const option = document.createElement('option');
        option.value = municipio.id;
        option.textContent = municipio.nombre;
        municipioSelect.appendChild(option);
    });

    if (municipioActual) {
        seleccionarPorTextoOValor(municipioSelect, municipioActual);
        llenarColoniasEditar(coloniaActual, cpActual);
    }
}

function llenarColoniasEditar(coloniaActual = '', cpActual = '') {
    const estadoSelect = document.getElementById('id_estado');
    const municipioSelect = document.getElementById('id_municipio');
    const coloniaSelect = document.getElementById('id_colonia');
    const cpInput = obtenerCampoCodigoPostal();
    const estado = ubicacionesEditar.find(item => String(item.id) === estadoSelect?.value);
    const municipio = (estado?.municipios || []).find(item => String(item.id) === municipioSelect?.value);
    if (!coloniaSelect || !cpInput) return;

    coloniaSelect.innerHTML = '<option value="">Selecciona colonia</option>';
    cpInput.value = '';

    (municipio?.colonias || []).forEach((colonia) => {
        const option = document.createElement('option');
        option.value = colonia.id;
        option.textContent = colonia.nombre;
        option.dataset.cp = colonia.codigoPostal || '';
        coloniaSelect.appendChild(option);
    });

    if (coloniaActual) {
        seleccionarPorTextoOValor(coloniaSelect, coloniaActual);
        const opcion = coloniaSelect.options[coloniaSelect.selectedIndex];
        cpInput.value = opcion?.dataset.cp || cpActual || '';
    }
}

async function guardarCambiosPerrito(event) {
    event.preventDefault();

    const nombre = obtenerValor('nombre');
    const senas = obtenerValor('senas_particulares');
    const confirmacion = document.getElementById('confirmacion')?.checked;

    if (!nombre) {
        mostrarAvisoEditar('El nombre del perrito es obligatorio.', 'error');
        return;
    }

    if (!senas) {
        mostrarAvisoEditar('Las senas particulares son obligatorias.', 'error');
        return;
    }

    if (!confirmacion) {
        mostrarAvisoEditar('Debes confirmar que los datos actualizados son correctos.', 'error');
        return;
    }

    try {
        const datos = new FormData(formEditarPerrito);
        datos.set('idTipoMascota', valorSeleccionadoEditar('id_tipo_mascota'));
        datos.set('edad', obtenerValor('edad_aproximada'));
        datos.set('idRaza', valorSeleccionadoEditar('id_raza'));
        datos.set('mezcla', obtenerValor('mezcla_raza'));
        datos.set('idColor', valorSeleccionadoEditar('id_color'));
        datos.set('descripcionColor', obtenerValor('descripcion_color'));
        datos.set('senas', obtenerValor('senas_particulares'));
        datos.set('condicionMedica', obtenerValor('condicion_medica'));
        datos.set('collarPlaca', obtenerValor('collar_placa'));
        datos.set('idEstado', valorSeleccionadoEditar('id_estado'));
        datos.set('idMunicipio', valorSeleccionadoEditar('id_municipio'));
        datos.set('idColonia', valorSeleccionadoEditar('id_colonia'));
        datos.set('cp', obtenerCampoCodigoPostal()?.value || '');

        const respuesta = await fetch(`${RUPE_EDITAR_API_BASE}/mascotas/${idMascotaEditar}`, {
            method: 'PUT',
            body: datos,
            credentials: 'include'
        });
        const resultado = await leerRespuestaEditar(respuesta);
        if (!respuesta.ok) {
            throw new Error(resultado.mensaje || resultado.error || 'No se pudieron guardar los cambios.');
        }
        await mostrarVentanaEditar(
            'Cambios guardados',
            resultado.mensaje || 'La informacion del perrito se actualizo correctamente. Revisa el detalle para confirmar que los datos quedaron como esperabas.'
        );
        window.location.href = `detalle-perrito.html?id=${encodeURIComponent(idMascotaEditar)}`;
    } catch (error) {
        mostrarAvisoEditar(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function obtenerCatalogoEditar(ruta) {
    const respuesta = await fetch(`${RUPE_EDITAR_API_BASE}${ruta}`, { credentials: 'include' });
    const contenido = await leerRespuestaEditar(respuesta);
    if (!respuesta.ok) throw new Error(contenido.mensaje || 'Error al consultar catalogo');
    return contenido;
}

function llenarSelectEditar(id, datos, placeholder) {
    const select = document.getElementById(id);
    if (!select) return;
    select.innerHTML = `<option value="">${placeholder}</option>`;
    datos.forEach((item) => {
        const option = document.createElement('option');
        option.value = item.id;
        option.textContent = item.nombre;
        select.appendChild(option);
    });
}

function asignarValor(id, valor) {
    const campo = document.getElementById(id);
    if (!campo) return;

    if (campo.tagName === 'SELECT') {
        seleccionarPorTextoOValor(campo, valor);
        return;
    }
    campo.value = valor || '';
}

function seleccionarPorTextoOValor(select, valor) {
    const texto = valor || '';
    const opcionExistente = Array.from(select.options).find((opcion) => {
        return normalizarEditar(opcion.value) === normalizarEditar(texto)
            || normalizarEditar(opcion.textContent) === normalizarEditar(texto);
    });

    if (opcionExistente) {
        select.value = opcionExistente.value;
        return;
    }

    if (texto) {
        const opcion = new Option(texto, texto, true, true);
        select.add(opcion);
    }
}

function obtenerCampoCodigoPostal() {
    return document.getElementById('cp');
}

function obtenerValor(id) {
    return document.getElementById(id)?.value.trim() || '';
}

function textoSeleccionadoEditar(id) {
    const select = document.getElementById(id);
    return select?.options[select.selectedIndex]?.textContent || '';
}

function valorSeleccionadoEditar(id) {
    return document.getElementById(id)?.value || '';
}

function normalizarEditar(texto) {
    return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
}

function mostrarAvisoEditar(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}

function mostrarVentanaEditar(titulo, mensaje) {
    return new Promise((resolve) => {
        const anterior = document.querySelector('.dueno-modal-overlay');
        if (anterior) anterior.remove();

        const overlay = document.createElement('div');
        overlay.className = 'dueno-modal-overlay';
        overlay.style.position = 'fixed';
        overlay.style.inset = '0';
        overlay.style.zIndex = '99999';
        overlay.style.display = 'flex';
        overlay.style.alignItems = 'center';
        overlay.style.justifyContent = 'center';
        overlay.style.padding = '20px';
        overlay.style.background = 'rgba(35, 0, 20, .58)';
        overlay.innerHTML = `
            <div class="dueno-modal" role="dialog" aria-modal="true" aria-labelledby="editarModalTitulo">
                <i class="fas fa-circle-check"></i>
                <h2 id="editarModalTitulo">${escaparHtmlEditar(titulo)}</h2>
                <p>${escaparHtmlEditar(mensaje)}</p>
                <button type="button" class="btn-aceptar-modal">Aceptar</button>
            </div>
        `;
        document.body.appendChild(overlay);

        const modal = overlay.querySelector('.dueno-modal');
        if (modal) {
            modal.style.width = 'min(460px, 100%)';
            modal.style.padding = '28px';
            modal.style.borderRadius = '14px';
            modal.style.background = '#fff';
            modal.style.textAlign = 'center';
            modal.style.boxShadow = '0 20px 50px rgba(0,0,0,.24)';
        }

        const boton = overlay.querySelector('.btn-aceptar-modal');
        if (boton) {
            boton.style.width = '100%';
            boton.style.marginTop = '22px';
            boton.style.padding = '14px';
            boton.style.border = 'none';
            boton.style.borderRadius = '10px';
            boton.style.background = '#611232';
            boton.style.color = '#fff';
            boton.style.fontWeight = '700';
            boton.style.cursor = 'pointer';
            boton.focus();
            boton.addEventListener('click', () => {
                overlay.remove();
                resolve();
            });
        }
    });
}

function escaparHtmlEditar(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function leerRespuestaEditar(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}


function actualizarFotoActual(url) {
    const img = document.querySelector('.foto-actual img');
    if (!img) return;
    img.src = urlFotoEditar(url);
}

function mostrarNuevaFotoSeleccionada() {
    const archivo = document.getElementById('foto_principal')?.files?.[0];
    if (!archivo) {
        actualizarFotoActual(perritoEditarActual?.fotoPrincipalUrl);
        return;
    }
    const img = document.querySelector('.foto-actual img');
    if (img) img.src = URL.createObjectURL(archivo);
}

function urlFotoEditar(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('blob:') || url.startsWith('http')) return url;
    return `${RUPE_EDITAR_BACKEND_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}
