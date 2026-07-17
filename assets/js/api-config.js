// Configuracion central del frontend RUPE.
// En local usa el mismo host de la pagina y el puerto 8080 de Spring Boot.
// En alojamiento solo cambia API_BASE_URL y BACKEND_ORIGIN, sin editar cada modulo JS.
const RUPE_BACKEND_PUBLICO = 'https://rupe-backend.onrender.com';

window.RUPE_CONFIG = {
    API_BASE_URL: `${RUPE_BACKEND_PUBLICO}/api`,
    BACKEND_ORIGIN: RUPE_BACKEND_PUBLICO,
    FRONTEND_ORIGIN: window.location.origin
};