# Checklist de cumplimiento - modulo administrador RUPE

Fecha de revision: 2026-06-29

## Seguridad y control

- [x] Acceso restringido por rol administrador.
- [x] Filtro backend `SessionAuthorizationFilter.java` para bloquear APIs privadas sin sesion y rutas admin sin rol administrador.
- [x] Configuracion sensible preparada por variables de entorno: base de datos, CORS, URL publica, Turnstile y exposicion de token.
- [x] Configuracion frontend centralizada en `assets/js/api-config.js` para cambiar URL del API sin editar cada modulo.
- [x] Bitacora de seguridad para login, fallos, bloqueos, catalogos, soporte y exportaciones.
- [x] Bitacora extendida para reportes: creacion, renovacion, marcado como recuperado, generacion de QR y descarga de cartel.
- [x] Bitacora extendida para avistamientos y notificaciones: reclamo, validacion y marcado de notificacion como leida.
- [x] Bitacora con clasificacion visual de riesgo y medidas sugeridas para revision administrativa.
- [x] Bitacora permite revisar usuario relacionado por ID sin mostrar correo completo ni ejecutar sanciones desde auditoria.
- [x] Activacion/desactivacion de usuarios exige motivo obligatorio y registra la accion en bitacora.
- [x] Restriccion de intentos de inicio de sesion con bloqueo temporal.
- [x] CAPTCHA requerido despues de intentos fallidos.
- [x] CAPTCHA en formularios publicos de avistamiento y contacto.
- [x] Recuperacion y restablecimiento de contrasena protegidos con CAPTCHA, token temporal y politica robusta de contrasena.
- [x] Token de recuperacion ocultable en produccion mediante `RUPE_EXPOSE_RESET_TOKEN=false`.
- [x] Validacion de datos de entrada en contacto, catalogos, autenticacion, reportes y avistamientos.
- [x] Politica de contrasena robusta validada en frontend y backend: longitud, mayuscula, minuscula, numero y caracter especial.
- [x] Soporte administrativo exige respuesta o accion realizada antes de marcar un mensaje como atendido.
- [x] Mascotas, reportes y avistamientos se consultan por usuario en sesion, no por id de usuario enviado desde frontend.
- [x] Reportes y mascotas validan propiedad: un usuario no puede operar recursos de otro usuario desde el frontend.
- [x] Borrado logico en catalogos mediante campo activo.
- [x] Baja logica de mascotas mediante campo activo para conservar historial.
- [x] Exportaciones administrativas registradas en bitacora.
- [x] Estadisticas por tipo de reporte sin permitir edicion administrativa de reportes reales.
- [ ] HTTPS/SSL pendiente del alojamiento final.
- [ ] SMTP real pendiente de credenciales productivas.

## UX/UI

- [x] Menu administrador con Panel, Usuarios, Reportes, Catalogos, Estadisticas, Soporte y Bitacora.
- [x] Alertas administrativas al ingresar al panel.
- [x] Mensajes de retroalimentacion para acciones principales.
- [x] Formularios con validacion y ayuda contextual.
- [x] Tablas con busqueda, filtros y estados visuales.
- [x] Bitacora con panel de alertas de mal uso: riesgo alto, medio, fallos, bloqueos, exportaciones e IP repetidas.
- [x] Cambio de pestana en catalogos limpia la busqueda y enfoca el campo para una nueva consulta.
- [x] Alta de reporte muestra recomendaciones antifraude para el dueño/tutor.
- [x] Contacto publico ofrece retroalimentacion al enviar mensajes y contador de caracteres.
- [x] Soporte permite filtrar por texto, estatus y asunto, con paginacion administrativa.
- [x] Perritos extraviados separa reportes con folio RUPE y avistamientos ciudadanos sin folio.
- [x] Busqueda publica en tiempo real por folio/nombre, raza, senas/color/descripcion, sexo, zona y tipo.

## Evaluacion dinamica

- [x] Prueba de carga de contexto Spring Boot.
- [x] Pruebas de validacion para contacto.
- [x] Pruebas de validacion para dueño/tutor: mascota, reporte de extravio y avistamiento.
- [x] Pruebas de propiedad de mascotas: detalle, actualizacion, baja logica y consulta por usuario en sesion.
- [x] Pruebas de busqueda publica y regla de 45 dias para avistamientos sin resguardo.
- [x] Pruebas de cambio de estatus de usuario con motivo auditable.
- [x] Prueba de enmascaramiento de correo en bitacora.
- [x] Pruebas del filtro de sesion y rol administrador: sin sesion, usuario no admin, admin y ruta publica.
- [x] Pruebas de seguridad de reportes: no crear reporte con mascota ajena y auditoria al renovar.
- [x] Pruebas de seguimiento: reclamo de avistamiento, validacion administrativa y notificacion leida con bitacora.
- [x] Pruebas de recuperacion/restablecimiento de contrasena con CAPTCHA y politica robusta.
- [x] Pruebas de registro con politica robusta de contrasena.
- [x] Pruebas unitarias de login, CAPTCHA y bloqueo temporal.
- [x] Pruebas de privacidad publica: DTO sin correo, telefono ni datos del propietario.
- [x] Pruebas de sanitizacion basica para visitas publicas.
- [x] Suite actual validada con `mvn test`: 35 pruebas sin fallas.
- [ ] Agregar pruebas de endpoints admin con MockMvc y sesion de administrador.

## Visualizacion de datos

- [x] Dashboard administrador con indicadores agregados.
- [x] Estadisticas con graficas y exportacion Excel.
- [x] Estadisticas separan reportes por extravio/perdida y robo de mascota.
- [x] Bitacora con filtros, CSV y Excel.
- [x] CSV de bitacora incluye riesgo calculado y medida sugerida para facilitar auditoria.
- [x] Soporte con filtros, CSV y Excel.
- [x] Soporte exporta respuesta administrativa y conserva trazabilidad del cierre.
- [x] Catalogos con filtros y Excel.
- [x] Exportaciones sin contrasenas ni datos sensibles completos.

## Alcance recomendado del administrador

- El administrador debe monitorear, consultar, atender soporte, administrar catalogos y exportar informacion.
- El administrador no debe alterar reportes ni avistamientos reales para conservar trazabilidad.
- Los cambios de catalogos deben conservar historial mediante desactivacion, no borrado fisico.
- La categoria "robo de mascota" se usa para seguimiento, reportes y graficas; no reemplaza una denuncia formal.
- La bitacora detecta y documenta riesgos; la desactivacion de cuentas se realiza desde Usuarios con trazabilidad.
- Todo cambio de estatus de usuario debe incluir motivo administrativo para justificar la decision.
- Todo cierre de soporte debe incluir respuesta o accion realizada para evitar atenciones sin evidencia.

## Alcance publico y ciudadano

- Las paginas publicas deben mostrar reportes activos y avistamientos ciudadanos sin exponer datos personales del dueño.
- El formulario publico de avistamiento debe mantener CAPTCHA, fecha valida, validacion de contacto si existe resguardo y revision de imagen.
- El formulario publico de contacto debe mantener CAPTCHA para reducir mensajes automatizados.
- Los datos de resguardante deben usarse para seguimiento interno del dueño, no para publicacion abierta.
- El campo `avistamiento.descripcion` se usa como senas visibles del perrito para busqueda publica sin exponer datos personales.
- Los avistamientos sin folio y sin resguardo se muestran por 45 dias; si estan en resguardo permanecen visibles mientras sigan activos.
