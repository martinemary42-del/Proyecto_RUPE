
const RUPE_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    cargarCatalogosFormulario();

    const form = document.querySelector('.perrito-form');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        limpiarAvisoFormulario(form);

        const required = Array.from(form.querySelectorAll('[required]'));
        const invalid = required.find(field => !field.value || (field.type === 'checkbox' && !field.checked));
        if (invalid) {
            invalid.focus();
            informar(form, 'Revisa los campos obligatorios antes de guardar.', 'warn');
            return;
        }

        const boton = form.querySelector('button[type="submit"]');
        const textoOriginal = boton ? boton.textContent : '';

        try {
            if (boton) {
                boton.disabled = true;
                boton.textContent = 'Guardando...';
            }

            const resultado = await enviarRegistroPerrito(form, false);
            let contenido = resultado.contenido;

            if (resultado.respuesta.status === 409) {
                const confirmarDuplicado = confirm(
                    (contenido.mensaje || 'Ya existe un registro parecido.') + '\\n\\n' +
                    'Si realmente se trata de otra mascota, puedes continuar con el registro. Si es el mismo perrito, revisa tus registros o reportes anteriores para conservar la trazabilidad.'
                );
                if (!confirmarDuplicado) {
                    informar(form, 'Registro cancelado. Revisa tus perritos registrados antes de crear uno nuevo.', 'warn');
                    return;
                }
                const segundoIntento = await enviarRegistroPerrito(form, true);
                contenido = segundoIntento.contenido;
                if (!segundoIntento.respuesta.ok) {
                    throw new Error(contenido.mensaje || contenido.error || contenido.message || 'No se pudo registrar el perrito.');
                }
            } else if (!resultado.respuesta.ok) {
                throw new Error(contenido.mensaje || contenido.error || contenido.message || 'No se pudo registrar el perrito.');
            }

            const destino = 'consultar-perritos.html';
            await mostrarVentanaRegistroPerrito(
                contenido.mensaje || 'Perrito registrado correctamente.',
                'El registro se guardo correctamente. Revisa que la fotografia, señas particulares y ubicacion sean claras. Si el perrito se extravia, genera el reporte desde Mis Perritos para crear el folio, QR y cartel.'
            );
            window.location.href = destino;
        } catch (error) {
            informar(form, error.message || 'No se pudo conectar con el backend.', 'error');
        } finally {
            if (boton) {
                boton.disabled = false;
                boton.textContent = textoOriginal;
            }
        }
    });
});

async function enviarRegistroPerrito(form, confirmarDuplicado) {
    const datos = new FormData(form);
    datos.set('idTipoMascota', valorSeleccionado('id_tipo_mascota'));
    datos.set('idRaza', valorSeleccionado('id_raza'));
    datos.set('idColor', valorSeleccionado('id_color'));
    datos.set('edad', valorCampo('edad') || valorCampo('edad_aproximada'));
    datos.set('mezcla', valorCampo('mezcla') || valorCampo('mezcla_raza'));
    datos.set('descripcionColor', valorCampo('descripcion_color'));
    datos.set('senas', valorCampo('senas') || valorCampo('senas_particulares'));
    datos.set('condicionMedica', valorCampo('condicion_medica'));
    datos.set('collarPlaca', valorCampo('collar_placa'));
    datos.set('idEstado', valorSeleccionado('id_estado'));
    datos.set('idMunicipio', valorSeleccionado('id_municipio'));
    datos.set('idColonia', valorSeleccionado('id_colonia'));
    datos.set('confirmarDuplicado', confirmarDuplicado ? 'true' : 'false');

    const respuesta = await fetch(`${RUPE_API_BASE}/mascotas`, {
        method: 'POST',
        body: datos,
        credentials: 'include'
    });

    return {
        respuesta,
        contenido: await leerRespuesta(respuesta)
    };
}

async function cargarCatalogosFormulario() {
    try {
        const [tipos, razas, colores, ubicaciones] = await Promise.all([
            obtenerCatalogo('/catalogos/tipos-mascota'),
            obtenerCatalogo('/catalogos/razas'),
            obtenerCatalogo('/catalogos/colores'),
            obtenerCatalogo('/catalogos/ubicaciones')
        ]);

        llenarSelect('id_tipo_mascota', tipos, 'Selecciona');
        llenarSelect('id_raza', razas, 'Selecciona');
        llenarSelect('id_color', colores, 'Selecciona');
        configurarUbicaciones(ubicaciones.estados || []);
    } catch (error) {
        const form = document.querySelector('.perrito-form');
        informar(form, 'No se pudieron cargar los catalogos. Verifica que el backend este encendido.', 'error');
    }
}

async function obtenerCatalogo(ruta) {
    const respuesta = await fetch(`${RUPE_API_BASE}${ruta}`, { credentials: 'include' });
    const contenido = await leerRespuesta(respuesta);
    if (!respuesta.ok) throw new Error(contenido.mensaje || 'Error al consultar catalogo');
    return contenido;
}

function llenarSelect(id, datos, placeholder) {
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

function configurarUbicaciones(estados) {
    const estadoSelect = document.getElementById('id_estado');
    const municipioSelect = document.getElementById('id_municipio');
    const coloniaSelect = document.getElementById('id_colonia');
    const cpInput = document.getElementById('cp');
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
        const estado = estados.find(item => String(item.id) === estadoSelect.value);
        municipioSelect.innerHTML = '<option value="">Selecciona municipio/alcaldia</option>';
        coloniaSelect.innerHTML = '<option value="">Primero selecciona municipio/alcaldia</option>';
        cpInput.value = '';

        (estado?.municipios || []).forEach((municipio) => {
            const option = document.createElement('option');
            option.value = municipio.id;
            option.textContent = municipio.nombre;
            municipioSelect.appendChild(option);
        });
    };

    municipioSelect.onchange = () => {
        const estado = estados.find(item => String(item.id) === estadoSelect.value);
        const municipio = (estado?.municipios || []).find(item => String(item.id) === municipioSelect.value);
        coloniaSelect.innerHTML = '<option value="">Selecciona colonia</option>';
        cpInput.value = '';

        (municipio?.colonias || []).forEach((colonia) => {
            const option = document.createElement('option');
            option.value = colonia.id;
            option.textContent = colonia.nombre;
            option.dataset.cp = colonia.codigoPostal || '';
            coloniaSelect.appendChild(option);
        });
    };

    coloniaSelect.onchange = () => {
        const opcion = coloniaSelect.options[coloniaSelect.selectedIndex];
        cpInput.value = opcion?.dataset.cp || '';
    };
}

function valorCampo(id) {
    return document.getElementById(id)?.value || '';
}

function textoSeleccionado(id) {
    const select = document.getElementById(id);
    return select?.options[select.selectedIndex]?.textContent || '';
}

function valorSeleccionado(id) {
    return document.getElementById(id)?.value || '';
}

function informar(form, mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') {
        mostrarMensaje(mensaje, tipo);
    }
    if (!form) return;
    limpiarAvisoFormulario(form);
    const aviso = document.createElement('div');
    aviso.className = `form-inline-message ${tipo || 'info'}`;
    aviso.textContent = mensaje;
    aviso.style.margin = '0 0 16px';
    aviso.style.padding = '12px 14px';
    aviso.style.borderRadius = '8px';
    aviso.style.fontWeight = '700';
    aviso.style.background = tipo === 'error' ? '#fde8e8' : tipo === 'ok' ? '#e8f7ee' : '#fff6d8';
    aviso.style.color = tipo === 'error' ? '#991b1b' : tipo === 'ok' ? '#166534' : '#7a4b00';
    form.prepend(aviso);
}

function limpiarAvisoFormulario(form) {
    form?.querySelector('.form-inline-message')?.remove();
}

function mostrarVentanaRegistroPerrito(titulo, mensaje) {
    return new Promise((resolve) => {
        const existente = document.querySelector('.registro-modal-overlay');
        if (existente) existente.remove();

        const overlay = document.createElement('div');
        overlay.className = 'registro-modal-overlay';
        overlay.innerHTML = `
            <div class="registro-modal" role="dialog" aria-modal="true" aria-labelledby="registroModalTitulo">
                <div class="registro-modal-icon"><i class="fas fa-circle-check"></i></div>
                <h2 id="registroModalTitulo">${escaparHtmlRegistro(titulo)}</h2>
                <p>${escaparHtmlRegistro(mensaje)}</p>
                <button type="button" class="btn-perrito" id="btnAceptarRegistro">Aceptar</button>
            </div>
        `;
        document.body.appendChild(overlay);

        const aceptar = overlay.querySelector('#btnAceptarRegistro');
        aceptar?.focus();
        aceptar?.addEventListener('click', () => {
            overlay.remove();
            resolve();
        });
    });
}

function escaparHtmlRegistro(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
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

