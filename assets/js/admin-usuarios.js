const RUPE_ADMIN_USERS_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';
const USUARIOS_POR_PAGINA = 10;

let usuariosAdmin = [];
let usuariosFiltradosAdmin = [];
let paginaUsuarios = 1;

document.addEventListener('DOMContentLoaded', () => {
    configurarFiltrosUsuarios();
    configurarNuevoAdministrador();
    cargarUsuariosAdmin();
});

async function cargarUsuariosAdmin() {
    const tbody = document.getElementById('tablaUsuariosAdmin');
    if (!tbody) return;

    try {
        const respuesta = await fetch(`${RUPE_ADMIN_USERS_API}/admin/usuarios`, { credentials: 'include' });
        const datos = await leerRespuestaUsuarios(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudieron cargar los usuarios.');

        usuariosAdmin = ordenarUsuariosAdmin(Array.isArray(datos) ? datos : []);
        paginaUsuarios = 1;
        aplicarFiltroDesdeBitacora();
        aplicarFiltrosUsuarios();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7">${escaparUsuarioAdmin(error.message || 'No se pudo conectar con el backend.')}</td></tr>`;
        actualizarContadorUsuarios(0, 0);
        mostrarUsuarioAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function aplicarFiltroDesdeBitacora() {
    const parametros = new URLSearchParams(window.location.search);
    const idUsuario = parametros.get('usuario');
    const buscador = document.getElementById('buscarUsuarioAdmin');
    if (!idUsuario || !buscador) return;
    // La bitacora manda solo el ID interno; asi se revisa la cuenta sin exponer correo completo.
    buscador.value = idUsuario;
    buscador.focus();
    mostrarUsuarioAdmin('Filtro aplicado desde bitácora. Revisa el usuario antes de cambiar su estatus.', 'warn');
}

function configurarFiltrosUsuarios() {
    const buscador = document.getElementById('buscarUsuarioAdmin');
    const rol = document.getElementById('filtroRolAdmin');
    const estado = document.getElementById('filtroEstadoAdmin');
    const limpiar = document.getElementById('limpiarFiltrosUsuarios');
    const anterior = document.getElementById('usuariosAnterior');
    const siguiente = document.getElementById('usuariosSiguiente');
    const exportarExcel = document.getElementById('btnExportarUsuariosExcel');

    [buscador, rol, estado].forEach((control) => {
        if (!control) return;
        control.addEventListener('input', () => {
            paginaUsuarios = 1;
            aplicarFiltrosUsuarios();
        });
    });

    if (limpiar) {
        limpiar.addEventListener('click', () => {
            if (buscador) buscador.value = '';
            if (rol) rol.value = '';
            if (estado) estado.value = '';
            paginaUsuarios = 1;
            aplicarFiltrosUsuarios();
        });
    }

    if (anterior) {
        anterior.addEventListener('click', () => {
            paginaUsuarios = Math.max(1, paginaUsuarios - 1);
            aplicarFiltrosUsuarios();
        });
    }

    if (siguiente) {
        siguiente.addEventListener('click', () => {
            paginaUsuarios += 1;
            aplicarFiltrosUsuarios();
        });
    }

    if (exportarExcel) exportarExcel.addEventListener('click', exportarUsuariosExcel);
}

function configurarNuevoAdministrador() {
    document.getElementById('btnNuevoAdministrador')?.addEventListener('click', () => {
        document.getElementById('modalAdminUsuario').hidden = false;
    });
    document.getElementById('cerrarModalAdminUsuario')?.addEventListener('click', cerrarModalNuevoAdmin);
    document.getElementById('formNuevoAdministrador')?.addEventListener('submit', crearNuevoAdministrador);
    document.getElementById('adminTelefono')?.addEventListener('input', (event) => {
        event.target.value = event.target.value.replace(/\D/g, '').slice(0, 10);
    });
}

function cerrarModalNuevoAdmin() {
    document.getElementById('modalAdminUsuario').hidden = true;
}

async function crearNuevoAdministrador(event) {
    event.preventDefault();
    const payload = {
        nombreCompleto: document.getElementById('adminNombre').value.trim(),
        correo: document.getElementById('adminCorreo').value.trim(),
        telefono: document.getElementById('adminTelefono').value.trim(),
        password: document.getElementById('adminPassword').value
    };
    if (!/^[0-9]{10}$/.test(payload.telefono)) {
        mostrarUsuarioAdmin('El teléfono debe tener 10 dígitos.', 'warn');
        return;
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,40}$/.test(payload.password)) {
        mostrarUsuarioAdmin('La contraseña debe incluir mayúscula, minúscula, número y carácter especial.', 'warn');
        return;
    }
    try {
        const respuesta = await fetch(`${RUPE_ADMIN_USERS_API}/admin/usuarios/admin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const datos = await leerRespuestaUsuarios(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo crear el administrador.');
        mostrarUsuarioAdmin('Administrador creado correctamente.', 'success');
        event.target.reset();
        cerrarModalNuevoAdmin();
        await cargarUsuariosAdmin();
    } catch (error) {
        mostrarUsuarioAdmin(error.message || 'No se pudo conectar con el backend.', 'error');
    }
}

function aplicarFiltrosUsuarios() {
    const texto = normalizarUsuarioAdmin(document.getElementById('buscarUsuarioAdmin')?.value || '');
    const rol = document.getElementById('filtroRolAdmin')?.value || '';
    const estado = document.getElementById('filtroEstadoAdmin')?.value || '';

    usuariosFiltradosAdmin = usuariosAdmin.filter((usuario) => {
        const estadoUsuario = obtenerEstadoUsuario(usuario).toUpperCase();
        const coincideTexto = !texto || normalizarUsuarioAdmin([
            usuario.idUsuario,
            usuario.nombreVisible,
            usuario.correoEnmascarado,
            usuario.rol,
            estadoUsuario
        ].join(' ')).includes(texto);
        const coincideRol = !rol || usuario.rol === rol;
        const coincideEstado = !estado || estadoUsuario === estado;
        return coincideTexto && coincideRol && coincideEstado;
    });

    const totalPaginas = Math.max(1, Math.ceil(usuariosFiltradosAdmin.length / USUARIOS_POR_PAGINA));
    paginaUsuarios = Math.min(paginaUsuarios, totalPaginas);
    const inicio = (paginaUsuarios - 1) * USUARIOS_POR_PAGINA;
    pintarUsuariosAdmin(usuariosFiltradosAdmin.slice(inicio, inicio + USUARIOS_POR_PAGINA), usuariosFiltradosAdmin.length);
    actualizarPaginacionUsuarios(totalPaginas, usuariosFiltradosAdmin.length);
}

function ordenarUsuariosAdmin(usuarios) {
    return [...usuarios].sort((a, b) => {
        const rolA = a.rol === 'ADMINISTRADOR' ? 0 : 1;
        const rolB = b.rol === 'ADMINISTRADOR' ? 0 : 1;
        if (rolA !== rolB) return rolA - rolB;
        return Number(a.idUsuario || 0) - Number(b.idUsuario || 0);
    });
}

function pintarUsuariosAdmin(usuarios, totalFiltrado) {
    const tbody = document.getElementById('tablaUsuariosAdmin');
    if (!usuarios.length) {
        tbody.innerHTML = '<tr><td colspan="7">No hay usuarios con esos filtros.</td></tr>';
        actualizarContadorUsuarios(totalFiltrado, usuariosAdmin.length);
        return;
    }

    tbody.innerHTML = usuarios.map((usuario) => {
        const estado = obtenerEstadoUsuario(usuario);
        const claseEstado = usuario.bloqueado ? 'danger' : (usuario.activo ? '' : 'warn');
        const accion = usuario.activo ? 'Desactivar' : 'Reactivar';
        const deshabilitarPrincipal = usuario.administradorPrincipal && usuario.activo;
        const proximoEstado = usuario.activo ? 'false' : 'true';

        return `
            <tr>
                <td>${usuario.idUsuario}</td>
                <td>${escaparUsuarioAdmin(usuario.nombreVisible)}</td>
                <td>${escaparUsuarioAdmin(usuario.correoEnmascarado)}</td>
                <td>${escaparUsuarioAdmin(usuario.rol)}</td>
                <td><span class="status-pill ${claseEstado}">${estado}</span></td>
                <td>${formatearFechaUsuarioAdmin(usuario.ultimoAcceso)}</td>
                <td>
                    <button class="module-btn secondary admin-action-btn"
                        data-user-id="${usuario.idUsuario}"
                        data-activo="${proximoEstado}"
                        ${deshabilitarPrincipal ? 'disabled title="Cuenta principal protegida"' : ''}>
                        ${deshabilitarPrincipal ? 'Principal' : accion}
                    </button>
                </td>
            </tr>`;
    }).join('');

    tbody.querySelectorAll('[data-user-id]').forEach((boton) => {
        boton.addEventListener('click', cambiarEstatusUsuarioAdmin);
    });
    actualizarContadorUsuarios(totalFiltrado, usuariosAdmin.length);
}

async function cambiarEstatusUsuarioAdmin(event) {
    const boton = event.currentTarget;
    const idUsuario = boton.dataset.userId;
    const activo = boton.dataset.activo === 'true';
    const accion = activo ? 'reactivar' : 'desactivar';

    const motivo = solicitarMotivoCambioEstatus(accion);
    if (!motivo) return;

    boton.disabled = true;
    boton.textContent = 'Guardando...';

    try {
        const respuesta = await fetch(`${RUPE_ADMIN_USERS_API}/admin/usuarios/${idUsuario}/estatus`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ activo, motivo })
        });
        const datos = await leerRespuestaUsuarios(respuesta);
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'No se pudo actualizar el usuario.');
        mostrarUsuarioAdmin('Estatus de usuario actualizado correctamente.', 'success');
        await cargarUsuariosAdmin();
    } catch (error) {
        mostrarUsuarioAdmin(error.message || 'No se pudo actualizar el usuario.', 'error');
        await cargarUsuariosAdmin();
    }
}

function solicitarMotivoCambioEstatus(accion) {
    const motivo = prompt(`Escribe el motivo para ${accion} esta cuenta. Este texto quedará en bitácora.`);
    if (motivo === null) return '';
    const limpio = motivo.trim().replace(/\s+/g, ' ');
    // El motivo evita cambios administrativos sin justificacion y apoya la trazabilidad.
    if (limpio.length < 10 || limpio.length > 300) {
        mostrarUsuarioAdmin('El motivo debe tener entre 10 y 300 caracteres.', 'warn');
        return '';
    }
    return limpio;
}

function exportarUsuariosExcel() {
    // Exporta solo datos administrativos mínimos; no incluye teléfono, correo completo ni contraseñas.
    const encabezados = ['ID', 'Usuario', 'Correo enmascarado', 'Rol', 'Estado', 'Ultimo acceso'];
    const filas = usuariosFiltradosAdmin.map((usuario) => [
        usuario.idUsuario,
        usuario.nombreVisible,
        usuario.correoEnmascarado,
        usuario.rol,
        obtenerEstadoUsuario(usuario),
        formatearFechaUsuarioAdmin(usuario.ultimoAcceso)
    ]);
    const excel = generarExcelXmlUsuarios('Usuarios RUPE', [encabezados, ...filas]);
    const blob = new Blob([excel], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'usuarios-rupe-admin.xls';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
    mostrarUsuarioAdmin('Excel de usuarios generado con datos mínimos.', 'success');
}

function generarExcelXmlUsuarios(nombreHoja, filas) {
    const filasXml = filas.map((fila) => `<Row>${fila.map((celda) => `<Cell><Data ss:Type="String">${escaparXmlUsuario(celda)}</Data></Cell>`).join('')}</Row>`).join('');
    return `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Worksheet ss:Name="${escaparXmlUsuario(nombreHoja)}">
  <Table>${filasXml}</Table>
 </Worksheet>
</Workbook>`;
}

function escaparXmlUsuario(valor) {
    return String(valor ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&apos;');
}

function obtenerEstadoUsuario(usuario) {
    if (usuario.bloqueado) return 'Bloqueado';
    return usuario.activo ? 'Activo' : 'Desactivado';
}

function actualizarContadorUsuarios(totalFiltrado, totalGeneral) {
    const contador = document.getElementById('contadorUsuariosAdmin');
    if (!contador) return;
    contador.textContent = `Mostrando ${totalFiltrado} de ${totalGeneral} usuarios registrados.`;
}

function actualizarPaginacionUsuarios(totalPaginas, totalFiltrado) {
    const contenedor = document.getElementById('paginacionUsuariosAdmin');
    const textoPagina = document.getElementById('paginaUsuariosAdmin');
    const anterior = document.getElementById('usuariosAnterior');
    const siguiente = document.getElementById('usuariosSiguiente');
    if (!contenedor || !textoPagina || !anterior || !siguiente) return;

    contenedor.hidden = totalFiltrado <= USUARIOS_POR_PAGINA;
    textoPagina.textContent = `Página ${paginaUsuarios} de ${totalPaginas}`;
    anterior.disabled = paginaUsuarios <= 1;
    siguiente.disabled = paginaUsuarios >= totalPaginas;
}

async function leerRespuestaUsuarios(respuesta) {
    const texto = await respuesta.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return { mensaje: texto }; }
}

function formatearFechaUsuarioAdmin(valor) {
    if (!valor) return 'Sin acceso';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return 'Sin acceso';
    return fecha.toLocaleDateString('es-MX');
}

function mostrarUsuarioAdmin(mensaje, tipo) {
    if (typeof mostrarMensaje === 'function') mostrarMensaje(mensaje, tipo);
    else alert(mensaje);
}

function normalizarUsuarioAdmin(valor) {
    return String(valor || '')
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim();
}

function escaparUsuarioAdmin(valor) {
    return String(valor || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
