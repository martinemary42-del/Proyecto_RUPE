/* =====================================================
   DETALLE REPORTE - RUPE
   Consulta real del reporte por ID.
===================================================== */

const RUPE_DETALLE_REPORTE_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const RUPE_DETALLE_REPORTE_ORIGIN = window.RUPE_CONFIG?.BACKEND_ORIGIN || 'http://localhost:8080';

let reporteDetalleActual = null;

document.addEventListener('DOMContentLoaded', cargarDetalleReporte);

async function cargarDetalleReporte() {
    const parametros = new URLSearchParams(window.location.search);
    const idReporte = parametros.get('id');
    if (!idReporte) {
        mostrarAvisoReporte('No se recibio el identificador del reporte.', 'error');
        return;
    }

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_REPORTE_API}/reportes/${idReporte}`, {
            credentials: 'include'
        });
        const reporte = await leerRespuestaReporte(respuesta);
        if (!respuesta.ok) {
            throw new Error(reporte.mensaje || reporte.error || 'No se pudo consultar el reporte.');
        }
        reporteDetalleActual = reporte;
        pintarReporte(reporte);
    } catch (error) {
        mostrarAvisoReporte(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function pintarReporte(reporte) {
    const tipoReporte = etiquetaTipoReporte(reporte.tipoReporte);
    texto('tituloReporte', `${tipoReporte}: ${reporte.folio || 'RUPE'}`);
    texto('subtituloReporte', `Reporte ${textoEstatus(reporte.estatus)} en seguimiento.`);
    texto('folioReporte', reporte.folio);
    texto('nombrePerrito', reporte.nombreMascota);
    texto('razaPerrito', reporte.razaMascota);
    texto('colorPerrito', reporte.colorMascota);
    texto('tamanoPerrito', 'No especificado');
    texto('fechaExtravio', `${tipoReporte} - ${formatearFecha(reporte.fechaExtravio)}`);
    texto('estatusReporte', textoEstatus(reporte.estatus));
    texto('fechaRecuperacion', reporte.fechaRecuperacion ? formatearFechaHora(reporte.fechaRecuperacion) : 'Sin cerrar');
    texto('estadoReporte', reporte.estado);
    texto('municipioReporte', reporte.municipio);
    texto('coloniaReporte', reporte.colonia);
    texto('cpReporte', reporte.codigoPostal);
    texto('referenciasReporte', reporte.referencias);
    texto('descripcionReporte', reporte.descripcionHechos || reporte.senasMascota);

    const foto = document.getElementById('fotoPerrito');
    if (foto) {
        foto.src = urlFotoReporte(reporte.fotoPrincipalUrl);
        foto.alt = `Fotografia del perrito ${reporte.nombreMascota || 'reportado'}`;
        foto.onerror = () => {
            foto.onerror = null;
            foto.src = 'assets/img/logo/logo-rupe.png';
        };
    }

    cargarAvistamientosDelReporte(reporte);

    cargarQrReporte(reporte);

    const linkAvistamientos = document.querySelector('.detalle-acciones .btn-principal');
    if (linkAvistamientos) {
        linkAvistamientos.href = `mis-avistamientos.html?idReporte=${encodeURIComponent(reporte.idReporte)}`;
    }

    configurarAccionesReporte(reporte);
}

async function cargarAvistamientosDelReporte(reporte) {
    const contenedor = document.querySelector('.avistamiento-lista');
    if (!contenedor) return;

    contenedor.innerHTML = `
        <div class="avistamiento-item">
            <i class="fas fa-spinner fa-spin"></i>
            <div>
                <strong>Cargando avistamientos</strong>
                <p>Consultando avisos relacionados con el folio ${escapar(reporte.folio || '')}.</p>
            </div>
        </div>
    `;

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_REPORTE_API}/avistamientos/mis`, {
            credentials: 'include'
        });
        const avistamientos = await leerRespuestaReporte(respuesta);
        if (!respuesta.ok) {
            throw new Error(avistamientos.mensaje || 'No se pudieron consultar los avistamientos.');
        }

        const relacionados = Array.isArray(avistamientos)
            ? avistamientos.filter((aviso) => String(aviso.idReporte) === String(reporte.idReporte))
            : [];

        if (!relacionados.length) {
            contenedor.innerHTML = `
                <div class="avistamiento-item">
                    <i class="fas fa-circle-info"></i>
                    <div>
                        <strong>Sin avistamientos registrados</strong>
                        <p>Aun no existen avisos ciudadanos para el folio ${escapar(reporte.folio || '')}.</p>
                    </div>
                </div>
            `;
            return;
        }

        contenedor.innerHTML = '';
        relacionados.forEach((aviso) => {
            const validado = normalizarEstatusAviso(aviso);
            contenedor.insertAdjacentHTML('beforeend', `
                <div class="avistamiento-item">
                    <i class="fas ${aviso.resguardado ? 'fa-house-circle-check' : 'fa-location-dot'}"></i>
                    <div>
                        <strong>${aviso.resguardado ? 'Posible resguardo' : 'Avistamiento ciudadano'} · ${textoEstatusAviso(validado)}</strong>
                        <p>${formatearFecha(aviso.fechaAvistamiento)} · ${escapar(zonaAviso(aviso))}</p>
                    </div>
                </div>
            `);
        });
    } catch (error) {
        contenedor.innerHTML = `
            <div class="avistamiento-item">
                <i class="fas fa-circle-exclamation"></i>
                <div>
                    <strong>No se pudieron cargar los avistamientos</strong>
                    <p>${escapar(error.message || 'Intenta nuevamente mas tarde.')}</p>
                </div>
            </div>
        `;
    }
}

function normalizarEstatusAviso(aviso) {
    if (aviso.validadoDueno) return 'validado';
    const texto = String(aviso.estatus || '').toLowerCase();
    if (texto.includes('descart')) return 'descartado';
    if (texto.includes('valid')) return 'validado';
    return 'pendiente';
}

function textoEstatusAviso(estatus) {
    if (estatus === 'validado') return 'Validado';
    if (estatus === 'descartado') return 'Descartado';
    return 'Pendiente';
}

function zonaAviso(aviso) {
    return [aviso.colonia, aviso.municipio, aviso.estado].filter(Boolean).join(', ') || 'Sin ubicacion';
}

function configurarAccionesReporte(reporte) {
    const botonRecuperado = document.getElementById('btnMarcarRecuperado');
    if (!botonRecuperado) return;
    const estatus = textoEstatus(reporte.estatus);
    botonRecuperado.style.display = estatus === 'activo' ? 'flex' : 'none';
    document.querySelectorAll('button[onclick="descargarCartel()"]').forEach((botonCartel) => {
        const activo = estatus === 'activo';
        botonCartel.disabled = !activo;
        botonCartel.style.opacity = activo ? '1' : '.65';
        botonCartel.style.cursor = activo ? 'pointer' : 'not-allowed';
        botonCartel.title = activo ? '' : 'El cartel solo se descarga mientras el reporte esta activo.';
        if (!activo) botonCartel.innerHTML = '<i class="fas fa-lock"></i> Cartel cerrado';
    });
    botonRecuperado.onclick = () => marcarReporteRecuperado(botonRecuperado);
}

async function marcarReporteRecuperado(boton) {
    if (!reporteDetalleActual?.idReporte) return;
    const confirmar = await mostrarConfirmacionReporte({
        titulo: 'Confirmar recuperacion',
        mensaje: 'Confirma que el perrito fue recuperado. El reporte pasara a recuperado, se cerrara el seguimiento publico y ya no podra descargarse nuevamente el cartel.',
        textoConfirmar: 'Marcar recuperado',
        textoCancelar: 'Cancelar'
    });
    if (!confirmar || boton.disabled) return;

    const textoOriginal = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cerrando...';

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_REPORTE_API}/reportes/${reporteDetalleActual.idReporte}/recuperar`, {
            method: 'PUT',
            credentials: 'include'
        });
        const contenido = await leerRespuestaReporte(respuesta);
        if (!respuesta.ok) {
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo cerrar el reporte.');
        }
        reporteDetalleActual = contenido;
        pintarReporte(contenido);
        mostrarAvisoReporte(contenido.mensaje || 'Reporte marcado como recuperado.', 'success');
    } catch (error) {
        boton.disabled = false;
        boton.innerHTML = textoOriginal;
        mostrarAvisoReporte(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

async function descargarCartel() {
    if (!reporteDetalleActual?.idReporte) {
        mostrarAvisoReporte('No se ha cargado el reporte.', 'error');
        return;
    }
    if (textoEstatus(reporteDetalleActual.estatus) !== 'activo') {
        mostrarAvisoReporte('El cartel solo puede descargarse mientras el reporte esta activo. Este reporte ya fue cerrado o recuperado.', 'error');
        return;
    }
    try {
        const respuesta = await fetch(`${RUPE_DETALLE_REPORTE_API}/reportes/${reporteDetalleActual.idReporte}/cartel`, {
            credentials: 'include'
        });
        if (!respuesta.ok) {
            const contenido = await leerRespuestaReporte(respuesta);
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo descargar el cartel.');
        }
        const blob = await respuesta.blob();
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `cartel-${reporteDetalleActual.folio || 'rupe'}.pdf`;
        enlace.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        mostrarAvisoReporte(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function texto(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.textContent = valor || 'Sin dato';
}

function textoEstatus(estatus) {
    const valor = (estatus || 'Activo').toLowerCase();
    if (valor.includes('recuper')) return 'recuperado';
    if (valor.includes('cerr')) return 'cerrado';
    return 'activo';
}

async function cargarQrReporte(reporte) {
    const qrBox = document.querySelector('.qr-demo');
    if (!qrBox || !reporte?.idReporte) return;

    qrBox.innerHTML = '<i class="fas fa-spinner fa-spin"></i><span>Generando QR...</span>';

    try {
        const respuesta = await fetch(`${RUPE_DETALLE_REPORTE_API}/reportes/${encodeURIComponent(reporte.idReporte)}/qr`, {
            credentials: 'include'
        });
        if (!respuesta.ok) {
            const contenido = await leerRespuestaReporte(respuesta);
            throw new Error(contenido.mensaje || contenido.error || 'No se pudo generar el QR.');
        }

        const blob = await respuesta.blob();
        const url = URL.createObjectURL(blob);
        qrBox.innerHTML = `<img src="${url}" alt="Codigo QR del reporte">`;
    } catch (error) {
        qrBox.innerHTML = `
            <i class="fas fa-qrcode"></i>
            <span>QR no disponible</span>
        `;
        mostrarAvisoReporte(error.message || 'No se pudo generar el QR del reporte.', 'error');
    }
}

function etiquetaTipoReporte(tipoReporte) {
    return String(tipoReporte || '').toUpperCase() === 'ROBO' ? 'Robo de mascota' : 'Extravio o perdida';
}

function formatearFecha(fecha) {
    if (!fecha) return 'Sin dato';
    const fechaObjeto = new Date(`${fecha}T00:00:00`);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin dato';
    return fechaObjeto.toLocaleDateString('es-MX');
}

function formatearFechaHora(fecha) {
    if (!fecha) return 'Sin dato';
    const fechaObjeto = new Date(fecha);
    if (Number.isNaN(fechaObjeto.getTime())) return 'Sin dato';
    return fechaObjeto.toLocaleString('es-MX');
}

function urlFotoReporte(url) {
    if (!url) return 'assets/img/logo/logo-rupe.png';
    if (url.startsWith('http')) return url;
    return `${RUPE_DETALLE_REPORTE_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
}

function escapar(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function mostrarConfirmacionReporte({ titulo, mensaje, textoConfirmar, textoCancelar }) {
    return new Promise((resolve) => {
        mostrarVentanaDuenoReporte({
            titulo,
            mensaje,
            tipo: 'confirmacion',
            textoBoton: textoConfirmar || 'Aceptar',
            textoCancelar: textoCancelar || 'Cancelar',
            resolver: resolve
        });
    });
}

function mostrarAvisoReporte(mensaje, tipo) {
    mostrarVentanaDuenoReporte({
        titulo: tipo === 'success' ? 'Operacion realizada' : 'Aviso RUPE',
        mensaje,
        tipo,
        textoBoton: 'Aceptar'
    });
}

function mostrarVentanaDuenoReporte({ titulo, mensaje, tipo, textoBoton, textoCancelar, resolver }) {
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
        <div class="dueno-modal" role="dialog" aria-modal="true" aria-labelledby="duenoModalTitulo">
            <i class="fas ${tipo === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i>
            <h2 id="duenoModalTitulo">${escapar(titulo || 'Aviso RUPE')}</h2>
            <p>${escapar(mensaje || '')}</p>
            ${textoCancelar ? `<button type="button" class="btn-cancelar-modal">${escapar(textoCancelar)}</button>` : ''}
            <button type="button" class="btn-aceptar-modal">${escapar(textoBoton || 'Aceptar')}</button>
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
    boton?.addEventListener('click', () => { overlay.remove(); resolver?.(true); });
}

async function leerRespuestaReporte(respuesta) {
    const textoRespuesta = await respuesta.text();
    if (!textoRespuesta) return {};
    try {
        return JSON.parse(textoRespuesta);
    } catch {
        return { mensaje: textoRespuesta };
    }
}


