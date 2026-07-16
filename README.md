# RUPE - Frontend preparado para backend local

Proyecto Terminal II. Sistema local para registro, reporte y seguimiento de perritos extraviados.

## Estado
- Navegación compacta para dueño y administrador.
- Formularios principales normalizados hacia el DER.
- Páginas legales y política de seguridad agregadas.
- Datos demo marcados para reemplazo por API/backend.

## Backend sugerido
- Java Spring Boot o Node/Express.
- MySQL o MariaDB local.
- Validación en frontend y backend.
- Contraseñas con hash seguro.
- Sesiones por rol.
- Bitácora para acciones importantes.
- Respaldos de base de datos y carpeta de fotografías.

## Referencias de seguridad
Se toman como guía ISO/IEC 27001, ISO/IEC 27002 e ISO/IEC 27701. No implica certificación ISO.
## Compresión de imágenes
- El frontend usa `assets/js/image-compressor.js`.
- Valida JPG, PNG y WebP.
- Máximo de entrada sugerido: 8 MB.
- Redimensiona a máximo 1200 x 1200 px.
- Comprime a JPG con calidad aproximada de 76%.
- El backend debe volver a validar tipo, tamaño y nombre del archivo.