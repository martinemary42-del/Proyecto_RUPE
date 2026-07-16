/* =====================================================
   MIS AVISTAMIENTOS - RUPE
   Lista avisos reales vinculados a los reportes del dueño.
===================================================== */

const RUPE_AVIS_API_BASE = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

const buscarFolio = document.getElementById('buscarFolio');
const filtroTipo = document.getElementById('filtroTipo');
const filtroEstatus = document.getElementById('filtroEstatus');
const listaAvistamientos = document.getElementById('listaAvistamientos');
const sinResultados = document.getElementById('sinResultados');
const panelValidacion = document.getElementById('panelValidacion');
let avistamientosUsuario = [];
let avistamientoSeleccionadoId = null;

document.addEventListener('DOMContentLoaded', iniciarMisAvistamientos);

function iniciarMisAvistamientos() {
    buscarFolio?.addEventListener('input', filtrarAvistamientos);
    filtroTipo?.addEventListener('change', filtrarAvistamientos);
    filtroEstatus?.addEventListener('change', filtrarAvistamientos);
    aplicarFiltrosDesdeUrl();
    cargarMisAvistamientos();
    prepararReclamoDesdeUrl();
    marcarNotificacionDesdeUrl();
}

async function cargarMisAvistamientos() {
    try {
        listaAvistamientos.innerHTML = '<p class="avistamiento-cargando">Cargando avistamientos...</p>';
        const respuesta = await fetch(`${RUPE_AVIS_API_BASE}/avistamientos/mis`, {
            credentials: 'include'
        });
        const contenido = await leerRespuestaAvistamientos(respuesta);
        if (!respuesta.ok) throw new Error(contenido.mensaje || contenido.error || 'No se pudieron consultar los avistamientos.');

        avistamientosUsuario = Array.isArray(contenido) ? contenido : [];
        pintarAvistamientos(avistamientosUsuario);
        filtrarAvistamientos();
        enfocarAvistamientoDesdeUrl();
    } catch (error) {
        listaAvistamientos.innerHTML = '';
        mostrarAvisoAvistamientos(error.message || 'No se pudo conectar con el backend.', 'error');
        actualizarSinResultados(true);
    }
}

function pintarAvistamientos(avistamientos) {
    listaAvistamientos.innerHTML = '';
    avistamientos.forEach((aviso) => {
        const tipoReporteAviso = normalizarTipoReporteAviso(aviso.tipoReporte);
        const tipo = tipoReporteAviso || (aviso.resguardado ? 'resguardo' : 'avistamiento');
        const estatus = normalizarEstatus(aviso.estatus, aviso.validadoDueno);
        const item = document.createElement('article');
        item.className = 'avistamiento-item';
        item.dataset.id = aviso.idAvistamiento;
        item.dataset.idReporte = aviso.idReporte || '';
        item.dataset.folio = normalizarTextoAvistamiento(aviso.folio || '');
        item.dataset.tipo = tipo;
        item.dataset.estatus = estatus;

        item.innerHTML = `
            <div class="avistamiento-icono ${tipo === 'resguardo' ? 'icono-resguardo' : 'icono-avistamiento'}">
                <i class="fas ${tipo === 'resguardo' ? 'fa-house-circle-check' : 'fa-eye'}"></i>
            </div>

            <div class="avistamiento-datos">
                <span class="estatus estatus-${estatus}">${textoEstatus(estatus)}</span>
                <h3>${escapar(aviso.folio || aviso.folioAvistamiento || 'Sin folio')} · ${escapar(aviso.nombreMascota || 'Perrito')}</h3>
                <p><strong>Tipo:</strong> ${etiquetaTipoAviso(tipo, aviso.resguardado)}</p>
                <p><strong>Fecha:</strong> ${formatearFecha(aviso.fechaAvistamiento)}</p>
                <p><strong>Ubicacion:</strong> ${escapar(zonaAviso(aviso))}</p>
                <p><strong>Descripcion:</strong> ${escapar(aviso.descripcion || 'Sin descripcion')}</p>
            </div>

            <div class="avistamiento-acciones">
                <button type="button" class="btn-accion" data-ver="${aviso.idAvistamiento}">
                    <i class="fas fa-eye"></i>
                    Ver detalle
                </button>
                <a href="detalle-reporte.html?id=${encodeURIComponent(aviso.idReporte || '')}" class="btn-accion">
                    <i class="fas fa-file-lines"></i>
                    Ver reporte
                </a>
            </div>
        `;

        listaAvistamientos.appendChild(item);
    });

    listaAvistamientos.querySelectorAll('[data-ver]').forEach((boton) => {
        boton.addEventListener('click', () => abrirPanelValidacion(Number(boton.dataset.ver)));
    });
}

function filtrarAvistamientos() {
    const folioTexto = normalizarTextoAvistamiento(buscarFolio?.value || '');
    const idReporteUrl = new URLSearchParams(window.location.search).get('idReporte') || '';
    const tipoSeleccionado = filtroTipo?.value || '';
    const estatusSeleccionado = filtroEstatus?.value || '';
    let totalVisibles = 0;

    document.querySelectorAll('.avistamiento-item').forEach((avistamiento) => {
        const folio = avistamiento.dataset.folio || '';
        const tipo = avistamiento.dataset.tipo || '';
        const estatus = avistamiento.dataset.estatus || '';
        const coincideFolio = folio.includes(folioTexto);
        const coincideReporteUrl = !idReporteUrl || String(avistamiento.dataset.idReporte || '') === String(idReporteUrl);
        const coincideTipo = tipoSeleccionado === '' || tipo === tipoSeleccionado;
        const coincideEstatus = estatusSeleccionado === '' || estatus === estatusSeleccionado;

        avistamiento.style.display = coincideFolio && coincideTipo && coincideEstatus && coincideReporteUrl ? 'grid' : 'none';
        if (coincideFolio && coincideTipo && coincideEstatus && coincideReporteUrl) totalVisibles++;
    });

    actualizarSinResultados(totalVisibles === 0);
}

function limpiarFiltros() {
    if (buscarFolio) buscarFolio.value = '';
    if (filtroTipo) filtroTipo.value = '';
    if (filtroEstatus) filtroEstatus.value = '';
    filtrarAvistamientos();
}

function abrirPanelValidacion(id) {
    const aviso = avistamientosUsuario.find((item) => item.idAvistamiento === id);
    if (!aviso) {
        mostrarAvisoAvistamientos('No se encontro la informacion del avistamiento.', 'error');
        return;
    }

    avistamientoSeleccionadoId = aviso.idAvistamiento;
    const tipo = aviso.resguardado ? 'Resguardo' : 'Avistamiento';
    document.getElementById('panelTipo').textContent = tipo;
    document.getElementById('panelTitulo').textContent = `${tipo} · ${aviso.folio || 'Sin folio'}`;
    document.getElementById('panelSubtitulo').textContent = `Aviso relacionado con ${aviso.nombreMascota || 'tu perrito'}.`;
    asignarImagenAviso('fotoRegistrada', aviso.fotoMascotaUrl);
    asignarImagenAviso('fotoCiudadano', aviso.fotoAvistamientoUrl);
    document.getElementById('panelFolio').textContent = aviso.folio || 'Sin folio';
    document.getElementById('panelPerrito').textContent = aviso.nombreMascota || 'Sin dato';
    document.getElementById('panelAviso').textContent = tipo;
    document.getElementById('panelFecha').textContent = formatearFecha(aviso.fechaAvistamiento);
    document.getElementById('panelUbicacion').textContent = zonaAviso(aviso);
    document.getElementById('panelEstatus').textContent = textoEstatus(normalizarEstatus(aviso.estatus, aviso.validadoDueno));
    document.getElementById('panelDescripcion').textContent = aviso.descripcion || 'Sin descripcion';
    actualizarContactoResguardante(aviso);
    document.getElementById('decisionValidacion').value = '';
    document.getElementById('comentarioValidacion').value = '';
    document.getElementById('autorizarContacto').checked = false;
    panelValidacion.classList.add('activo');
    panelValidacion.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function cerrarPanelValidacion() {
    panelValidacion?.classList.remove('activo');
}

async function confirmarDesdePanel() {
    const decision = document.getElementById('decisionValidacion').value;
    const comentario = document.getElementById('comentarioValidacion').value.trim();
    const autorizar = document.getElementById('autorizarContacto').checked;

    if (!avistamientoSeleccionadoId) {
        mostrarAvisoAvistamientos('Abre primero el detalle de un avistamiento.', 'error');
        return;
    }
    if (!decision) {
        enfocarCampoValidacion('decisionValidacion');
        mostrarAvisoAvistamientos('Selecciona si la evidencia corresponde o no a tu perrito antes de continuar.', 'error');
        return;
    }
    if (!autorizar) {
        enfocarCampoValidacion('autorizarContacto');
        mostrarAvisoAvistamientos('Marca la casilla de confirmacion para registrar tu decision.', 'error');
        return;
    }

    await enviarValidacionAvistamiento(decision, comentario);
}

async function descartarDesdePanel() {
    const decision = document.getElementById('decisionValidacion').value;
    const comentario = document.getElementById('comentarioValidacion').value.trim();
    const autorizar = document.getElementById('autorizarContacto').checked;

    if (!avistamientoSeleccionadoId) {
        mostrarAvisoAvistamientos('Abre primero el detalle de un avistamiento.', 'error');
        return;
    }
    if (!decision) {
        enfocarCampoValidacion('decisionValidacion');
        mostrarAvisoAvistamientos('Selecciona “No corresponde a mi perrito” antes de descartar.', 'error');
        return;
    }
    if (decision !== 'no') {
        enfocarCampoValidacion('decisionValidacion');
        mostrarAvisoAvistamientos('Para descartar, selecciona la opcion “No corresponde a mi perrito”.', 'error');
        return;
    }
    if (!autorizar) {
        enfocarCampoValidacion('autorizarContacto');
        mostrarAvisoAvistamientos('Marca la casilla de confirmacion para registrar el descarte.', 'error');
        return;
    }

    await enviarValidacionAvistamiento('no', comentario || 'Descartado por el propietario.');
}


function enfocarCampoValidacion(idCampo) {
    const campo = document.getElementById(idCampo);
    if (!campo) return;
    campo.scrollIntoView({ behavior: 'smooth', block: 'center' });
    campo.focus?.();
}

async function enviarValidacionAvistamiento(decision, comentario) {
    try {
        const respuesta = await fetch(`${RUPE_AVIS_API_BASE}/avistamientos/${encodeURIComponent(avistamientoSeleccionadoId)}/validacion`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ decision, comentario })
        });
        const contenido = await leerRespuestaAvistamientos(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo registrar la validacion.');
        }

        await marcarNotificacionDesdeUrl(true);
        mostrarAvisoAvistamientos('Validacion registrada correctamente.', 'success');
        await cargarMisAvistamientos();
        const avisoActualizado = avistamientosUsuario.find((item) => item.idAvistamiento === avistamientoSeleccionadoId);
        if (decision === 'si' && avisoActualizado) {
            abrirPanelValidacion(avistamientoSeleccionadoId);
        } else {
            cerrarPanelValidacion();
        }
    } catch (error) {
        mostrarAvisoAvistamientos(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}


function actualizarContactoResguardante(aviso) {
    const contenedor = document.getElementById('contactoProtegido');
    if (!contenedor) return;

    const puedeMostrar = aviso.resguardado && aviso.validadoDueno
        && (aviso.nombreResguardante || aviso.correoResguardante || aviso.telefonoResguardante);

    if (!puedeMostrar) {
        contenedor.classList.remove('contacto-liberado');
        contenedor.innerHTML = `
            <i class="fas fa-lock"></i>
            <div>
                <h4>Contacto protegido</h4>
                <p>El contacto del resguardante solo se mostrara si el dueño confirma que la evidencia corresponde a su perrito.</p>
            </div>
        `;
        return;
    }

    contenedor.classList.add('contacto-liberado');
    contenedor.innerHTML = `
        <i class="fas fa-location-dot"></i>
        <div>
            <span class="contacto-liberado-etiqueta">Datos liberados</span>
            <h4>Contacto del resguardante liberado</h4>
            <p class="contacto-liberado-ayuda">Estos son los datos del ciudadano que reporto el resguardo. Verifica la identidad del perrito antes de coordinar cualquier entrega.</p>
            <p><strong>Nombre:</strong> ${escapar(aviso.nombreResguardante || 'Sin dato')}</p>
            <p><strong>Correo:</strong> ${escapar(aviso.correoResguardante || 'Sin dato')}</p>
            <p><strong>Telefono:</strong> ${escapar(aviso.telefonoResguardante || 'Sin dato')}</p>
        </div>
    `;
}

function validarAviso(folio) {
    mostrarAvisoAvistamientos(`Abre el detalle del aviso para revisar ${folio}.`, 'warn');
}

function descartarAviso() {
    mostrarAvisoAvistamientos('El descarte se conectara al modulo de seguimiento y recuperacion.', 'warn');
}


function resolverImagenAviso(ruta) {
    if (!ruta) return 'assets/img/logo/logo-rupe.png';
    if (ruta.startsWith('http')) return ruta;
    if (ruta.startsWith('/uploads')) return `${RUPE_AVIS_API_BASE.replace('/api', '')}${ruta}`;
    return ruta;
}

function enfocarAvistamientoDesdeUrl() {
    const parametros = new URLSearchParams(window.location.search);
    const idReporteUrl = parametros.get('idReporte') || '';
    const folioUrl = normalizarTextoAvistamiento(parametros.get('folio') || '');
    const idAvistamientoUrl = parametros.get('idAvistamiento') || '';
    const vieneDeNotificacion = Boolean(parametros.get('idNotificacion'));

    if (!vieneDeNotificacion && !idAvistamientoUrl) return;

    const candidatos = Array.from(document.querySelectorAll('.avistamiento-item')).filter((item) => {
        const coincideId = !idAvistamientoUrl || String(item.dataset.id || '') === String(idAvistamientoUrl);
        const coincideReporte = !idReporteUrl || String(item.dataset.idReporte || '') === String(idReporteUrl);
        const coincideFolio = !folioUrl || String(item.dataset.folio || '').includes(folioUrl);
        return item.style.display !== 'none' && coincideId && coincideReporte && coincideFolio;
    });

    const objetivo = candidatos[0];
    if (!objetivo) return;

    document.querySelectorAll('.avistamiento-item.aviso-enfocado')
        .forEach((item) => item.classList.remove('aviso-enfocado'));
    objetivo.classList.add('aviso-enfocado');

    const id = Number(objetivo.dataset.id);
    if (id) {
        window.setTimeout(() => {
            objetivo.scrollIntoView({ behavior: 'smooth', block: 'center' });
            abrirPanelValidacion(id);
        }, 450);
    }
}

function asignarImagenAviso(id, ruta) {
    const imagen = document.getElementById(id);
    if (!imagen) return;
    imagen.onerror = () => {
        imagen.onerror = null;
        imagen.src = 'assets/img/logo/logo-rupe.png';
    };
    imagen.src = resolverImagenAviso(ruta);
}

function actualizarSinResultados(mostrar) {
    if (sinResultados) sinResultados.style.display = mostrar ? 'block' : 'none';
}

function normalizarEstatus(estatus, validadoDueno) {
    if (validadoDueno) return 'validado';
    const texto = (estatus || '').toLowerCase();
    if (texto.includes('descart')) return 'descartado';
    if (texto.includes('valid')) return 'validado';
    return 'pendiente';
}

function textoEstatus(estatus) {
    if (estatus === 'validado') return 'Validado';
    if (estatus === 'descartado') return 'Descartado';
    return 'Pendiente';
}

function zonaAviso(aviso) {
    return [aviso.colonia, aviso.municipio, aviso.estado].filter(Boolean).join(', ') || 'Sin ubicacion';
}

function formatearFecha(fecha) {
    if (!fecha) return 'Sin dato';
    const fechaObjeto = new Date(`${fecha}T00:00:00`);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin dato';
    return fechaObjeto.toLocaleDateString('es-MX');
}

function escapar(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function leerRespuestaAvistamientos(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return { mensaje: texto };
    }
}

function mostrarAvisoAvistamientos(mensaje, tipo) {
    mostrarVentanaDuenoAvistamientos({
        titulo: tipo === 'success' ? 'Operacion realizada' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaDuenoAvistamientos({ titulo, mensaje, tipo, textoBoton }) {
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
        <div class="dueno-modal" role="dialog" aria-modal="true" aria-labelledby="duenoAvisoTitulo">
            <i class="fas ${tipo === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i>
            <h2 id="duenoAvisoTitulo">${escaparHtmlAvistamientos(titulo || 'Aviso RUPE')}</h2>
            <p>${escaparHtmlAvistamientos(mensaje || '')}</p>
            <button type="button">${escaparHtmlAvistamientos(textoBoton || 'Aceptar')}</button>
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

    const boton = overlay.querySelector('button');
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
    }
    boton?.focus();
    boton?.addEventListener('click', () => overlay.remove());
}

function escaparHtmlAvistamientos(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function prepararReclamoDesdeUrl() {
    const idAvistamiento = new URLSearchParams(window.location.search).get('reclamarAvistamiento');
    if (!idAvistamiento) return;

    try {
        const respuesta = await fetch(`${RUPE_AVIS_API_BASE}/reportes/mis`, { credentials: 'include' });
        const reportes = await leerRespuestaAvistamientos(respuesta);
        if (!respuesta.ok) throw new Error(reportes.mensaje || reportes.error || 'No se pudieron cargar tus reportes.');

        const activos = (Array.isArray(reportes) ? reportes : []).filter((reporte) =>
            String(reporte.estatus || '').toLowerCase().includes('activo')
        );
        if (!activos.length) {
            mostrarDialogoAvistamientos({
                titulo: 'Reporte activo requerido',
                mensaje: 'Para solicitar la validacion de este avistamiento primero necesitas tener un reporte de extravio o robo activo. Esto protege al perrito resguardado y evita que una persona sin evidencia reclame una mascota que no le pertenece.',
                detalle: 'Registra o reactiva un reporte desde Mis Reportes y vuelve a intentar la solicitud.'
            });
            return;
        }

        mostrarPanelReclamo(Number(idAvistamiento), activos);
    } catch (error) {
        mostrarAvisoAvistamientos(error.message || 'No se pudo preparar el reclamo del avistamiento.', 'error');
    }
}

function mostrarPanelReclamo(idAvistamiento, reportes) {
    const tarjeta = document.querySelector('.avistamientos-card');
    if (!tarjeta) return;

    let panel = document.getElementById('panelReclamoAvistamiento');
    if (!panel) {
        panel = document.createElement('section');
        panel.id = 'panelReclamoAvistamiento';
        panel.className = 'panel-validacion activo';
        tarjeta.prepend(panel);
    }

    panel.innerHTML = `
        <div class="panel-header">
            <div>
                <span class="panel-badge">Reclamo de avistamiento</span>
                <h3>¿Reconoces a tu mascota?</h3>
                <p>Selecciona tu reporte activo. Esta acción solo inicia la validación; no libera datos personales ni autoriza la entrega del perrito.</p>
            </div>
        </div>
        <div class="form-group">
            <label for="reclamoReporte">Reporte de extravio activo</label>
            <select id="reclamoReporte">
                <option value="">Selecciona un reporte</option>
                ${reportes.map((reporte) => `
                    <option value="${reporte.idReporte}">
                        ${escapar(reporte.folio || 'Sin folio')} - ${escapar(reporte.nombreMascota || 'Perrito')}
                    </option>
                `).join('')}
            </select>
        </div>
        <div class="contacto-protegido">
            <i class="fas fa-shield-halved"></i>
            <div>
                <h4>Evidencia recomendada</h4>
                <p>Antes de coordinar una entrega, prepara fotos anteriores, cartilla o comprobante veterinario, señas particulares, collar, fecha y lugar de extravio. RUPE registra esta asociacion en bitacora para trazabilidad.</p>
            </div>
        </div>
        <div class="contacto-protegido">
            <i class="fas fa-lock"></i>
            <div>
                <h4>Validacion segura</h4>
                <p>Al asociarlo, el aviso entrara a tu historial de avistamientos. El contacto solo se libera despues de confirmar la coincidencia.</p>
            </div>
        </div>
        <div class="privacy-check">
            <input type="checkbox" id="confirmarPropiedadReclamo">
            <label for="confirmarPropiedadReclamo">
                Confirmo que este reclamo se realiza porque considero que el avistamiento puede corresponder a mi mascota y acepto presentar evidencia si se requiere.
            </label>
        </div>
        <div class="panel-acciones">
            <a class="btn-secundario-panel" href="perritos-extraviados.html">Volver a perritos extraviados</a>
            <button type="button" class="btn-validar" onclick="confirmarReclamoAvistamiento(${idAvistamiento})">
                Iniciar validacion
            </button>
        </div>
    `;
    panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function mostrarDialogoAvistamientos({ titulo, mensaje, detalle }) {
    // Modal de lectura para avisos importantes: no se cierra solo, el usuario debe aceptar.
    document.getElementById('dialogoAvistamientos')?.remove();
    const dialogo = document.createElement('div');
    dialogo.id = 'dialogoAvistamientos';
    dialogo.className = 'rupe-dialog-backdrop';
    dialogo.innerHTML = `
        <div class="rupe-dialog" role="dialog" aria-modal="true" aria-labelledby="dialogoAvistamientosTitulo">
            <div class="rupe-dialog-icon">
                <i class="fas fa-shield-halved"></i>
            </div>
            <h3 id="dialogoAvistamientosTitulo">${escapar(titulo || 'Aviso importante')}</h3>
            <p>${escapar(mensaje || '')}</p>
            ${detalle ? `<small>${escapar(detalle)}</small>` : ''}
            <button type="button" class="btn-validar" id="btnAceptarDialogoAvistamientos">Aceptar</button>
        </div>
    `;
    document.body.appendChild(dialogo);
    const boton = document.getElementById('btnAceptarDialogoAvistamientos');
    boton?.focus();
    boton?.addEventListener('click', () => dialogo.remove());
    dialogo.addEventListener('click', (evento) => {
        if (evento.target === dialogo) dialogo.remove();
    });
}

async function confirmarReclamoAvistamiento(idAvistamiento) {
    const idReporte = document.getElementById('reclamoReporte')?.value;
    if (!idReporte) {
        mostrarAvisoAvistamientos('Selecciona el reporte activo que corresponde a tu mascota.', 'warn');
        return;
    }
    if (!document.getElementById('confirmarPropiedadReclamo')?.checked) {
        mostrarAvisoAvistamientos('Confirma que puedes acreditar la propiedad o coincidencia del perrito antes de iniciar la validacion.', 'warn');
        document.getElementById('confirmarPropiedadReclamo')?.focus();
        return;
    }

    try {
        const respuesta = await fetch(`${RUPE_AVIS_API_BASE}/avistamientos/${encodeURIComponent(idAvistamiento)}/reclamar`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ idReporte: Number(idReporte) })
        });
        const contenido = await leerRespuestaAvistamientos(respuesta);
        if (!respuesta.ok) throw new Error(contenido.mensaje || contenido.error || 'No se pudo asociar el avistamiento.');

        mostrarAvisoAvistamientos('Solicitud de validacion iniciada. Revisa el aviso en tu listado y confirma la coincidencia con evidencia.', 'success');
        window.history.replaceState({}, '', 'mis-avistamientos.html');
        document.getElementById('panelReclamoAvistamiento')?.remove();
        await cargarMisAvistamientos();
    } catch (error) {
        mostrarAvisoAvistamientos(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function aplicarFiltrosDesdeUrl() {
    const parametros = new URLSearchParams(window.location.search);
    const folio = parametros.get('folio');
    if (folio && buscarFolio) buscarFolio.value = folio;
}

function normalizarTextoAvistamiento(valor) {
    return String(valor || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
}

function normalizarTipoReporteAviso(tipoReporte) {
    const valor = String(tipoReporte || '').toUpperCase();
    if (valor === 'ROBO') return 'robo';
    if (valor === 'EXTRAVIO') return 'avistamiento';
    return '';
}

function etiquetaTipoAviso(tipo, resguardado) {
    if (tipo === 'robo') return 'Robo';
    if (resguardado || tipo === 'resguardo') return 'Posible resguardo';
    return 'Avistamiento ciudadano';
}

function obtenerIdNotificacionUrl() {
    return new URLSearchParams(window.location.search).get('idNotificacion') || '';
}

async function marcarNotificacionDesdeUrl(silencioso = true) {
    const idNotificacion = obtenerIdNotificacionUrl();
    if (!idNotificacion) return;
    try {
        await fetch(`${RUPE_AVIS_API_BASE}/notificaciones/${encodeURIComponent(idNotificacion)}/leida`, {
            method: 'PUT',
            credentials: 'include'
        });
        if (!silencioso) mostrarAvisoAvistamientos('Notificacion marcada como leida.', 'success');
    } catch (error) {
        // La lectura de la notificacion no bloquea la validacion del avistamiento.
    }
}
