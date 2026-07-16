/* =====================================================
   RUPE - Compresión de imágenes en frontend
   Reduce peso antes de enviar al backend.
===================================================== */

const RUPE_IMAGE_CONFIG = {
    maxInputMb: 25,
    maxOutputMb: 5,
    maxOutputWidth: 1200,
    maxOutputHeight: 1200,
    quality: 0.76,
    outputType: 'image/jpeg'
};

document.addEventListener('DOMContentLoaded', () => {
    const imageInputs = document.querySelectorAll('input[type="file"][accept*="image"], input[type="file"][accept*=".jpg"], input[type="file"][accept*=".png"]');

    imageInputs.forEach((input) => {
        input.dataset.originalName = input.name;
        input.accept = '.jpg,.jpeg,.png,.webp';
        input.addEventListener('change', () => optimizarImagen(input));
        crearPanelImagen(input);
    });
});

function crearPanelImagen(input){
    if(input.parentElement.querySelector('.image-optimizer')) return;

    const panel = document.createElement('div');
    panel.className = 'image-optimizer';
    panel.innerHTML = `
        <div class="image-preview" aria-live="polite">
            <span>Vista previa optimizada</span>
        </div>
        <p class="image-status">Selecciona una imagen JPG, PNG o WebP. Se reducirá automáticamente antes de enviarla.</p>
    `;

    input.insertAdjacentElement('afterend', panel);
}

async function optimizarImagen(input){
    const file = input.files && input.files[0];
    const panel = input.parentElement.querySelector('.image-optimizer');
    const status = panel ? panel.querySelector('.image-status') : null;
    const preview = panel ? panel.querySelector('.image-preview') : null;

    if(!file) return;

    if(!file.type.startsWith('image/')){
        input.value = '';
        mostrarEstadoImagen(status, 'El archivo debe ser una imagen válida.', 'error');
        return;
    }

    const originalMb = file.size / (1024 * 1024);
    if(originalMb > RUPE_IMAGE_CONFIG.maxInputMb){
        input.value = '';
        mostrarEstadoImagen(status, `La imagen pesa ${formatoMb(file.size)}. Para evitar bloqueos del navegador, usa una imagen menor a ${RUPE_IMAGE_CONFIG.maxInputMb} MB.`, 'error');
        return;
    }

    try{
        mostrarEstadoImagen(status, 'Optimizando imagen...', 'info');

        const bitmap = await cargarImagen(file);
        const size = calcularTamano(bitmap.width, bitmap.height);
        const blob = await dibujarComprimido(bitmap, size.width, size.height);
        const optimizedFile = new File(
            [blob],
            nombreOptimizado(file.name),
            { type: RUPE_IMAGE_CONFIG.outputType, lastModified: Date.now() }
        );

        if ((optimizedFile.size / (1024 * 1024)) > RUPE_IMAGE_CONFIG.maxOutputMb) {
            input.value = '';
            mostrarEstadoImagen(status, `La imagen optimizada pesa ${formatoMb(optimizedFile.size)}. Intenta con otra foto.`, 'error');
            return;
        }

        reemplazarArchivo(input, optimizedFile);
        mostrarPreview(preview, optimizedFile);
        mostrarEstadoImagen(
            status,
            `Imagen lista: ${formatoMb(file.size)} -> ${formatoMb(optimizedFile.size)} (${size.width}x${size.height}px).`,
            'ok'
        );
    }catch(error){
        input.value = '';
        mostrarEstadoImagen(status, 'No fue posible optimizar la imagen. Intenta con otra fotografía.', 'error');
    }
}

function cargarImagen(file){
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = reject;
        img.src = URL.createObjectURL(file);
    });
}

function calcularTamano(width, height){
    const maxW = RUPE_IMAGE_CONFIG.maxOutputWidth;
    const maxH = RUPE_IMAGE_CONFIG.maxOutputHeight;
    const scale = Math.min(maxW / width, maxH / height, 1);

    return {
        width: Math.round(width * scale),
        height: Math.round(height * scale)
    };
}

function dibujarComprimido(img, width, height){
    return new Promise((resolve) => {
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
            (blob) => resolve(blob),
            RUPE_IMAGE_CONFIG.outputType,
            RUPE_IMAGE_CONFIG.quality
        );
    });
}

function reemplazarArchivo(input, file){
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    input.files = dataTransfer.files;
}

function nombreOptimizado(name){
    const base = name.replace(/\.[^/.]+$/, '').replace(/\s+/g, '-').toLowerCase();
    return `${base}-optimizada.jpg`;
}

function mostrarPreview(container, file){
    if(!container) return;
    const url = URL.createObjectURL(file);
    container.innerHTML = `<img src="${url}" alt="Vista previa de imagen optimizada">`;
}

function mostrarEstadoImagen(element, message, type){
    if(!element) return;
    element.textContent = message;
    element.className = `image-status ${type || 'info'}`;
}

function formatoMb(bytes){
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}