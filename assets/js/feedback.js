function mostrarMensaje(mensaje, tipo){
    const existente = document.querySelector('.rupe-toast');
    if(existente){ existente.remove(); }
    const toast = document.createElement('div');
    toast.className = 'rupe-toast ' + (tipo || 'info');
    toast.textContent = mensaje;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('visible'), 10);
    setTimeout(() => {
        toast.classList.remove('visible');
        setTimeout(() => toast.remove(), 300);
    }, 3200);
}
