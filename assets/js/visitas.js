/* Conteo agregado de visitas publicas. No almacena datos personales. */
const RUPE_VISIT_API = window.RUPE_CONFIG?.API_BASE_URL || 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const pagina = window.location.pathname.split('/').pop() || 'index.html';
    fetch(`${RUPE_VISIT_API}/publico/visita`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pagina })
    }).catch(() => {});
});
