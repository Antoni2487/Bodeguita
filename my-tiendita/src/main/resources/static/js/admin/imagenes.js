// ==================== CONFIGURACIÓN ====================
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

let tipoEntidad = 'productos'; // Tipo por defecto
let imagenActual = null; // URL de la imagen actual (para edición)
let archivoSeleccionado = null; // Archivo seleccionado para subir

// ==================== CONFIGURAR TIPO DE ENTIDAD ====================
/**
 * Configura el tipo de entidad para las rutas de upload
 * @param {string} tipo - productos, marcas, categorias, etc.
 */
function setTipoEntidad(tipo) {
    tipoEntidad = tipo;
    console.log(`📌 Tipo de entidad configurado: ${tipo}`);
}

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    initializeImageUpload();
});

function initializeImageUpload() {
    const fileInput = document.getElementById('imagenFile');
    const previewContainer = document.getElementById('imagePreviewContainer');

    if (fileInput) {
        fileInput.addEventListener('change', handleFileSelect);
    }

    if (previewContainer) {
        setupDragAndDrop(previewContainer, fileInput);
    }

    console.log('✅ imagenes.js inicializado correctamente');
}

// ==================== MANEJO DE SELECCIÓN DE ARCHIVO ====================
function handleFileSelect(event) {
    const file = event.target.files[0];
    
    if (!file) {
        clearImagePreview();
        return;
    }

    // Validar archivo
    const validationError = validateFile(file);
    if (validationError) {
        showError(validationError);
        event.target.value = ''; // Limpiar input
        clearImagePreview();
        return;
    }

    // Guardar archivo seleccionado
    archivoSeleccionado = file;

    // Mostrar preview
    showImagePreview(file);
}

// ==================== VALIDACIÓN DE ARCHIVO ====================
function validateFile(file) {
    if (!file) {
        return 'No se ha seleccionado ningún archivo';
    }

    // Validar tamaño
    if (file.size > MAX_FILE_SIZE) {
        return 'La imagen excede el tamaño máximo permitido (5MB)';
    }

    // Validar tipo MIME
    if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
        return 'Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG, GIF y WEBP';
    }

    // Validar extensión
    const extension = getFileExtension(file.name).toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
        return 'Extensión de archivo no permitida';
    }

    return null; // Archivo válido
}

function getFileExtension(filename) {
    if (!filename) return '';
    const lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
        return filename.substring(lastDotIndex + 1);
    }
    return '';
}

// ==================== PREVIEW DE IMAGEN ====================
function showImagePreview(file) {
    const container = document.getElementById('imagePreviewContainer');
    if (!container) return;

    const reader = new FileReader();
    
    reader.onload = function(e) {
        container.innerHTML = `
            <div class="image-preview-item">
                <img src="${e.target.result}" 
                     alt="Preview de ${file.name}" 
                     class="preview-image">
                <div class="preview-info">
                    <span class="preview-filename">${file.name}</span>
                    <span class="preview-size">${formatFileSize(file.size)}</span>
                </div>
                <button type="button" 
                        class="btn-remove-preview" 
                        onclick="removeImagePreview()"
                        title="Eliminar imagen">
                    <i class="bi bi-x-circle"></i>
                </button>
            </div>
        `;
    };

    reader.onerror = function() {
        showError('Error al leer el archivo');
        clearImagePreview();
    };

    reader.readAsDataURL(file);
}

function showExistingImage(imageUrl) {
    const container = document.getElementById('imagePreviewContainer');
    if (!container || !imageUrl) return;

    imagenActual = imageUrl;

    container.innerHTML = `
        <div class="image-preview-item existing">
            <img src="${imageUrl}" 
                 alt="Imagen actual" 
                 class="preview-image"
                 onerror="this.src='/images/no-image.png'">
            <div class="preview-info">
                <span class="preview-filename">Imagen actual</span>
            </div>
            <button type="button" 
                    class="btn-remove-preview" 
                    onclick="removeImagePreview()"
                    title="Eliminar imagen">
                <i class="bi bi-x-circle"></i>
            </button>
        </div>
    `;
}

function removeImagePreview() {
    const fileInput = document.getElementById('imagenFile');
    
    if (fileInput) {
        fileInput.value = '';
    }
    
    archivoSeleccionado = null;
    imagenActual = null;
    
    clearImagePreview();
}

function clearImagePreview() {
    const container = document.getElementById('imagePreviewContainer');
    if (!container) return;

    container.innerHTML = `
        <div class="image-preview-placeholder">
            <i class="bi bi-image"></i>
            <p>Arrastra una imagen aquí o haz clic para seleccionar</p>
            <small>Formatos: JPG, PNG, GIF, WEBP (máx. 5MB)</small>
        </div>
    `;
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

// ==================== DRAG & DROP ====================
function setupDragAndDrop(container, fileInput) {
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        container.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        container.addEventListener(eventName, () => {
            container.classList.add('dragover');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        container.addEventListener(eventName, () => {
            container.classList.remove('dragover');
        }, false);
    });

    container.addEventListener('drop', function(e) {
        const dt = e.dataTransfer;
        const files = dt.files;

        if (files.length > 0) {
            const file = files[0]; // Solo tomamos el primer archivo
            
            // Validar archivo
            const validationError = validateFile(file);
            if (validationError) {
                showError(validationError);
                return;
            }

            // Asignar al input y disparar evento change
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileInput.files = dataTransfer.files;

            const event = new Event('change', { bubbles: true });
            fileInput.dispatchEvent(event);
        }
    });

    // Click en el contenedor abre el selector de archivos
    container.addEventListener('click', function(e) {
        // No abrir si se hizo click en el botón de eliminar
        if (!e.target.closest('.btn-remove-preview')) {
            fileInput.click();
        }
    });
}

// ==================== SUBIR IMAGEN AL SERVIDOR ====================
/**
 * Sube una imagen al servidor
 * @param {File} file - Archivo a subir
 * @param {string} tipo - Tipo de entidad (opcional, usa tipoEntidad por defecto)
 * @returns {Promise<string>} URL de la imagen subida
 */
async function uploadImageToServer(file, tipo = null) {
    const tipoFinal = tipo || tipoEntidad;
    
    if (!file) {
        throw new Error('No hay archivo para subir');
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`/api/upload/${tipoFinal}/imagen`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error HTTP ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            console.log('✅ Imagen subida exitosamente:', data.url);
            return data.url;
        } else {
            throw new Error(data.message || 'Error al subir imagen');
        }
    } catch (error) {
        console.error('❌ Error al subir imagen:', error);
        throw error;
    }
}

// ==================== ELIMINAR IMAGEN DEL SERVIDOR ====================
/**
 * Elimina una imagen del servidor
 * @param {string} imageUrl - URL de la imagen (ej: /uploads/productos/uuid.jpg)
 * @param {string} tipo - Tipo de entidad (opcional)
 * @returns {Promise<boolean>} true si se eliminó correctamente
 */
async function deleteImageFromServer(imageUrl, tipo = null) {
    if (!imageUrl) return false;

    const tipoFinal = tipo || tipoEntidad;
    
    // Extraer el nombre del archivo de la URL
    const filename = imageUrl.split('/').pop();
    
    if (!filename) {
        console.warn('No se pudo extraer el nombre del archivo');
        return false;
    }

    try {
        const response = await fetch(`/api/upload/${tipoFinal}/imagen/${filename}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            console.warn(`No se pudo eliminar la imagen del servidor: ${response.status}`);
            return false;
        }

        const data = await response.json();
        
        if (data.success) {
            console.log('✅ Imagen eliminada del servidor:', filename);
            return true;
        } else {
            console.warn('La imagen no se eliminó:', data.message);
            return false;
        }
    } catch (error) {
        console.error('❌ Error al eliminar imagen:', error);
        return false;
    }
}

// ==================== OBTENER IMAGEN SELECCIONADA ====================
/**
 * Obtiene la imagen seleccionada (archivo o URL actual)
 * @returns {File|string|null} Archivo seleccionado o URL de imagen actual
 */
function getSelectedImage() {
    // Si hay un archivo nuevo seleccionado, retornar el archivo
    if (archivoSeleccionado) {
        return archivoSeleccionado;
    }
    
    // Si hay una imagen actual (edición), retornar la URL
    if (imagenActual) {
        return imagenActual;
    }
    
    return null;
}

/**
 * Verifica si hay un archivo nuevo seleccionado
 * @returns {boolean}
 */
function hasNewFile() {
    return archivoSeleccionado !== null;
}

/**
 * Verifica si hay una imagen (nueva o existente)
 * @returns {boolean}
 */
function hasImage() {
    return archivoSeleccionado !== null || imagenActual !== null;
}

// ==================== RESETEAR ESTADO ====================
/**
 * Limpia completamente el estado de imágenes
 * Usar al crear un nuevo registro
 */
function resetImageState() {
    const fileInput = document.getElementById('imagenFile');
    if (fileInput) {
        fileInput.value = '';
    }
    
    archivoSeleccionado = null;
    imagenActual = null;
    
    clearImagePreview();
    
    console.log('🔄 Estado de imagen reseteado');
}

/**
 * Configura el estado para edición
 * @param {string} imageUrl - URL de la imagen existente
 */
function setExistingImage(imageUrl) {
    resetImageState();
    
    if (imageUrl) {
        showExistingImage(imageUrl);
        console.log('✅ Imagen existente cargada:', imageUrl);
    }
}

// ==================== NOTIFICACIONES ====================
function showError(message) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: message,
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 4000,
            timerProgressBar: true
        });
    } else {
        alert('Error: ' + message);
    }
}

function showWarning(message) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            icon: 'warning',
            title: 'Advertencia',
            text: message,
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
    } else {
        alert('Advertencia: ' + message);
    }
}

function showSuccess(message) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            icon: 'success',
            title: 'Éxito',
            text: message,
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
    } else {
        alert(message);
    }
}

console.log('✅ imagenes.js cargado correctamente (versión simplificada)');