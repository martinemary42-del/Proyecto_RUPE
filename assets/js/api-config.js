// Configuracion central del frontend RUPE.
// En local usa el mismo host de la pagina y el puerto 8080 de Spring Boot.
// En alojamiento solo cambia API_BASE_URL y BACKEND_ORIGIN, sin editar cada modulo JS.
window.RUPE_CONFIG = window.RUPE_CONFIG || (() => {
    const host = window.location.hostname || 'localhost';
    const protocol = window.location.protocol === 'https:' ? 'https' : 'http';
    const backendOrigin = `${protocol}://${host}:8080`;

    return {
        API_BASE_URL: `${backendOrigin}/api`,
        BACKEND_ORIGIN: backendOrigin,
        FRONTEND_ORIGIN: window.location.origin
    };
})();
