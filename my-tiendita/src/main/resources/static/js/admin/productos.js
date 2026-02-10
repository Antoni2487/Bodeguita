// ==================== CONFIGURACIÓN ====================
const API_URL = '/api/productos';
const CATEGORIAS_URL = '/api/productos/form-data/categorias';
const SUBCATEGORIAS_URL = '/api/productos/form-data/subcategorias/categoria';

let productos = [];
let categorias = {};
let subcategorias = {};
let bodegas = {};
let modalInstance = null;
let detalleModalInstance = null;
let modoEdicion = false;

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Inicializando módulo de productos...');
    
    // Configurar tipo de entidad para el upload
    setTipoEntidad('productos');
    
    // Inicializar modales de Bootstrap
    const modalElement = document.getElementById('productoModal');
    const detalleModalElement = document.getElementById('detalleModal');
    
    if (modalElement) {
        modalInstance = new bootstrap.Modal(modalElement);
    }
    
    if (detalleModalElement) {
        detalleModalInstance = new bootstrap.Modal(detalleModalElement);
    }
    
    // Cargar datos iniciales
    cargarCategorias();
    cargarProductos();
    
    // Event listeners
    setupEventListeners();
    
    console.log('✅ Módulo de productos inicializado');
});

function setupEventListeners() {
    // Formulario de producto
    const form = document.getElementById('productoForm');
    if (form) {
        form.addEventListener('submit', handleSubmit);
    }
    
    // Filtros
    const filtroNombre = document.getElementById('filtroNombre');
    const filtroCategoria = document.getElementById('filtroCategoria');
    const filtroSubcategoria = document.getElementById('filtroSubcategoria');
    const filtroActivo = document.getElementById('filtroActivo');
    
    if (filtroNombre) {
        filtroNombre.addEventListener('input', debounce(aplicarFiltros, 300));
    }
    
    if (filtroCategoria) {
        filtroCategoria.addEventListener('change', function() {
            cargarSubcategoriasPorCategoria(this.value);
            aplicarFiltros();
        });
    }
    
    if (filtroSubcategoria) {
        filtroSubcategoria.addEventListener('change', aplicarFiltros);
    }
    
    if (filtroActivo) {
        filtroActivo.addEventListener('change', aplicarFiltros);
    }
    
    // Categoría en formulario (para habilitar subcategoría)
    const categoriaSelect = document.getElementById('categoria');
    if (categoriaSelect) {
        categoriaSelect.addEventListener('change', function() {
            const subcategoriaSelect = document.getElementById('subcategoria');
            const categoriaId = this.value;
            
            if (categoriaId) {
                cargarSubcategoriasFormulario(categoriaId);
                subcategoriaSelect.disabled = false;
            } else {
                subcategoriaSelect.disabled = true;
                subcategoriaSelect.innerHTML = '<option value="">Seleccione una subcategoría</option>';
            }
        });
    }
    
    // Reset de modal al cerrar
    const modalElement = document.getElementById('productoModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', function() {
            resetForm();
        });
    }
}

// ==================== CARGA DE DATOS ====================

async function cargarProductos() {
    try {
        showLoading();
        
        const response = await fetch(API_URL);
        
        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }
        
        productos = await response.json();
        
        console.log('✅ Productos cargados:', productos.length);
        
        renderizarTabla(productos);
        
    } catch (error) {
        console.error('❌ Error al cargar productos:', error);
        showError('Error al cargar los productos');
        renderizarTablaVacia('Error al cargar productos');
    }
}

async function cargarCategorias() {
    try {
        const response = await fetch(CATEGORIAS_URL);
        
        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }
        
        categorias = await response.json();
        
        console.log('✅ Categorías cargadas:', Object.keys(categorias).length);
        
        // Llenar select de filtro
        const filtroCategoria = document.getElementById('filtroCategoria');
        if (filtroCategoria) {
            filtroCategoria.innerHTML = '<option value="">Todas las categorías</option>';
            Object.entries(categorias).forEach(([id, nombre]) => {
                filtroCategoria.innerHTML += `<option value="${id}">${nombre}</option>`;
            });
        }
        
        // Llenar select de formulario
        const categoriaSelect = document.getElementById('categoria');
        if (categoriaSelect) {
            categoriaSelect.innerHTML = '<option value="">Seleccione una categoría</option>';
            Object.entries(categorias).forEach(([id, nombre]) => {
                categoriaSelect.innerHTML += `<option value="${id}">${nombre}</option>`;
            });
        }
        
    } catch (error) {
        console.error('❌ Error al cargar categorías:', error);
    }
}

async function cargarSubcategoriasPorCategoria(categoriaId) {
    const filtroSubcategoria = document.getElementById('filtroSubcategoria');
    
    if (!categoriaId) {
        filtroSubcategoria.disabled = true;
        filtroSubcategoria.innerHTML = '<option value="">Todas las subcategorías</option>';
        return;
    }
    
    try {
        const response = await fetch(`${SUBCATEGORIAS_URL}/${categoriaId}`);
        
        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }
        
        subcategorias = await response.json();
        
        filtroSubcategoria.disabled = false;
        filtroSubcategoria.innerHTML = '<option value="">Todas las subcategorías</option>';
        
        Object.entries(subcategorias).forEach(([id, nombre]) => {
            filtroSubcategoria.innerHTML += `<option value="${id}">${nombre}</option>`;
        });
        
    } catch (error) {
        console.error('❌ Error al cargar subcategorías:', error);
        filtroSubcategoria.disabled = true;
    }
}

async function cargarSubcategoriasFormulario(categoriaId) {
    const subcategoriaSelect = document.getElementById('subcategoria');
    
    if (!categoriaId) {
        subcategoriaSelect.innerHTML = '<option value="">Seleccione una subcategoría</option>';
        return;
    }
    
    try {
        const response = await fetch(`${SUBCATEGORIAS_URL}/${categoriaId}`);
        
        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }
        
        const subs = await response.json();
        
        subcategoriaSelect.innerHTML = '<option value="">Seleccione una subcategoría</option>';
        
        Object.entries(subs).forEach(([id, nombre]) => {
            subcategoriaSelect.innerHTML += `<option value="${id}">${nombre}</option>`;
        });
        
    } catch (error) {
        console.error('❌ Error al cargar subcategorías:', error);
    }
}

// ==================== RENDERIZADO ====================

function renderizarTabla(productosData) {
    const tbody = document.querySelector('#tablaProductos tbody');
    
    if (!tbody) {
        console.error('No se encontró el tbody de la tabla');
        return;
    }
    
    if (!productosData || productosData.length === 0) {
        renderizarTablaVacia('No hay productos para mostrar');
        return;
    }
    
    tbody.innerHTML = productosData.map(producto => {
        const estadoBadge = producto.activo 
            ? '<span class="badge badge-activo">Activo</span>'
            : '<span class="badge badge-inactivo">Inactivo</span>';
        
        const imagen = producto.imagen 
            ? `<img src="${producto.imagen}" alt="${producto.nombre}" class="producto-imagen" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
               <div class="no-image-placeholder" style="display:none;"><i class="bi bi-image"></i></div>`
            : `<div class="no-image-placeholder"><i class="bi bi-image"></i></div>`;
        
        const categoriaNombre = categorias[producto.categoria] || 'Sin categoría';
        
        return `
            <tr>
                <td>${producto.id}</td>
                <td>${imagen}</td>
                <td>
                    <strong>${producto.nombre}</strong>
                    ${producto.descripcion ? `<br><small class="text-muted">${truncateText(producto.descripcion, 50)}</small>` : ''}
                </td>
                <td>${categoriaNombre}</td>
                <td><span class="precio-badge">S/ ${parseFloat(producto.precioSugerido).toFixed(2)}</span></td>
                <td>${estadoBadge}</td>
                <td class="table-actions">
                    <button class="btn btn-sm btn-info text-white" onclick="openDetail(${producto.id})" title="Ver detalles">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-warning text-white" onclick="openEdit(${producto.id})" title="Editar">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="confirmDelete(${producto.id}, '${producto.nombre}')" title="Eliminar">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderizarTablaVacia(mensaje) {
    const tbody = document.querySelector('#tablaProductos tbody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-4 text-muted">
                    <i class="bi bi-inbox" style="font-size: 2rem;"></i>
                    <p class="mt-2 mb-0">${mensaje}</p>
                </td>
            </tr>
        `;
    }
}

function showLoading() {
    const tbody = document.querySelector('#tablaProductos tbody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="table-loading">
                    <i class="bi bi-arrow-clockwise"></i>
                    <p class="mt-2 mb-0">Cargando productos...</p>
                </td>
            </tr>
        `;
    }
}

// ==================== FILTROS ====================

function aplicarFiltros() {
    const filtroNombre = document.getElementById('filtroNombre')?.value.toLowerCase() || '';
    const filtroCategoria = document.getElementById('filtroCategoria')?.value || '';
    const filtroSubcategoria = document.getElementById('filtroSubcategoria')?.value || '';
    const filtroActivo = document.getElementById('filtroActivo')?.value || '';
    
    const productosFiltrados = productos.filter(producto => {
        const matchNombre = !filtroNombre || producto.nombre.toLowerCase().includes(filtroNombre);
        const matchCategoria = !filtroCategoria || producto.categoria == filtroCategoria;
        const matchSubcategoria = !filtroSubcategoria || producto.subcategoria == filtroSubcategoria;
        const matchActivo = !filtroActivo || producto.activo.toString() === filtroActivo;
        
        return matchNombre && matchCategoria && matchSubcategoria && matchActivo;
    });
    
    renderizarTabla(productosFiltrados);
}

// ==================== CRUD OPERATIONS ====================

function openCreate() {
    modoEdicion = false;
    resetForm();
    
    document.getElementById('productoModalTitle').innerHTML = '<i class="bi bi-box-seam me-2"></i> Nuevo Producto';
    
    if (modalInstance) {
        modalInstance.show();
    }
}

async function openEdit(id) {
    modoEdicion = true;
    
    const producto = productos.find(p => p.id === id);
    
    if (!producto) {
        showError('Producto no encontrado');
        return;
    }
    
    document.getElementById('productoModalTitle').innerHTML = '<i class="bi bi-pencil me-2"></i> Editar Producto';
    
    // Llenar formulario
    document.getElementById('id').value = producto.id;
    document.getElementById('nombre').value = producto.nombre;
    document.getElementById('descripcion').value = producto.descripcion;
    document.getElementById('precioSugerido').value = producto.precioSugerido;
    document.getElementById('activo').checked = producto.activo;
    
    // Categoría
    document.getElementById('categoria').value = producto.categoria || '';
    
    // Cargar subcategorías si hay categoría
    if (producto.categoria) {
        await cargarSubcategoriasFormulario(producto.categoria);
        const subcategoriaSelect = document.getElementById('subcategoria');
        subcategoriaSelect.disabled = false;
        subcategoriaSelect.value = producto.subcategoria || '';
    }
    
    // Bodega
    if (producto.bodega) {
        document.getElementById('bodega').value = producto.bodega;
    }
    
    // Mostrar imagen existente
    if (producto.imagen) {
        setExistingImage(producto.imagen);
    }
    
    // Cambiar texto del password helper
    const passwordInput = document.getElementById('password');
    const passwordRequired = document.getElementById('passwordRequired');
    const passwordHelp = document.getElementById('passwordHelp');
    
    if (passwordInput) passwordInput.required = false;
    if (passwordRequired) passwordRequired.style.display = 'none';
    if (passwordHelp) passwordHelp.style.display = 'block';
    
    if (modalInstance) {
        modalInstance.show();
    }
}

async function openDetail(id) {
    const producto = productos.find(p => p.id === id);
    
    if (!producto) {
        showError('Producto no encontrado');
        return;
    }
    
    const detalleContent = document.getElementById('detalleContent');
    
    if (!detalleContent) return;
    
    const estadoBadge = producto.activo 
        ? '<span class="badge badge-activo">Activo</span>'
        : '<span class="badge badge-inactivo">Inactivo</span>';
    
    const categoriaNombre = categorias[producto.categoria] || 'Sin categoría';
    const subcategoriaNombre = producto.subcategoria ? (subcategorias[producto.subcategoria] || 'N/A') : 'N/A';
    const bodegaNombre = producto.bodega ? (bodegas[producto.bodega] || 'Sin bodega') : 'Sin bodega';
    
    const imagenHtml = producto.imagen 
        ? `<img src="${producto.imagen}" alt="${producto.nombre}" class="detalle-imagen-grande" onerror="this.src='/images/no-image.png';">`
        : `<div class="text-center text-muted py-4">
               <i class="bi bi-image" style="font-size: 3rem;"></i>
               <p class="mt-2">Sin imagen</p>
           </div>`;
    
    detalleContent.innerHTML = `
        <div class="row g-4">
            <div class="col-md-5">
                ${imagenHtml}
            </div>
            <div class="col-md-7">
                <div class="detalle-row">
                    <div class="detalle-label">ID</div>
                    <div class="detalle-value">#${producto.id}</div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Nombre</div>
                    <div class="detalle-value"><strong>${producto.nombre}</strong></div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Descripción</div>
                    <div class="detalle-value">${producto.descripcion}</div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Precio Sugerido</div>
                    <div class="detalle-value"><span class="precio-badge">S/ ${parseFloat(producto.precioSugerido).toFixed(2)}</span></div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Categoría</div>
                    <div class="detalle-value">${categoriaNombre}</div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Subcategoría</div>
                    <div class="detalle-value">${subcategoriaNombre}</div>
                </div>
                <div class="detalle-row">
                    <div class="detalle-label">Estado</div>
                    <div class="detalle-value">${estadoBadge}</div>
                </div>
            </div>
        </div>
    `;
    
    if (detalleModalInstance) {
        detalleModalInstance.show();
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    
    const id = document.getElementById('id').value;
    const isEdit = id && id !== '';
    
    try {
        // Mostrar loading
        Swal.fire({
            title: isEdit ? 'Actualizando producto...' : 'Creando producto...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
        
        // Preparar datos básicos del producto
        const productoData = {
            nombre: document.getElementById('nombre').value.trim(),
            descripcion: document.getElementById('descripcion').value.trim(),
            precioSugerido: parseFloat(document.getElementById('precioSugerido').value),
            activo: document.getElementById('activo').checked,
            categoria: parseInt(document.getElementById('categoria').value) || null,
            subcategoria: document.getElementById('subcategoria').value ? parseInt(document.getElementById('subcategoria').value) : null,
            imagen: null
        };
        
        // Manejar la imagen
        if (hasNewFile()) {
            // Hay un archivo nuevo, subirlo primero
            const archivo = getSelectedImage();
            const urlImagen = await uploadImageToServer(archivo, 'productos');
            productoData.imagen = urlImagen;
        } else if (hasImage()) {
            // Mantener la imagen existente
            productoData.imagen = getSelectedImage();
        }
        
        // Enviar datos al servidor
        const url = isEdit ? `${API_URL}/${id}` : API_URL;
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productoData)
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error HTTP ${response.status}`);
        }
        
        Swal.close();
        
        showSuccess(isEdit ? 'Producto actualizado correctamente' : 'Producto creado correctamente');
        
        // Cerrar modal
        if (modalInstance) {
            modalInstance.hide();
        }
        
        // Recargar productos
        await cargarProductos();
        
    } catch (error) {
        console.error('❌ Error al guardar producto:', error);
        Swal.close();
        showError('Error al guardar el producto: ' + error.message);
    }
}

function confirmDelete(id, nombre) {
    Swal.fire({
        title: '¿Estás seguro?',
        html: `¿Deseas eliminar el producto <strong>${nombre}</strong>?<br><small class="text-muted">Esta acción no se puede deshacer</small>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#EF4444',
        cancelButtonColor: '#6B7280',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteProducto(id);
        }
    });
}

async function deleteProducto(id) {
    try {
        Swal.fire({
            title: 'Eliminando producto...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
        
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }
        
        Swal.close();
        
        showSuccess('Producto eliminado correctamente');
        
        await cargarProductos();
        
    } catch (error) {
        console.error('❌ Error al eliminar producto:', error);
        Swal.close();
        showError('Error al eliminar el producto');
    }
}

// ==================== UTILIDADES ====================

function resetForm() {
    const form = document.getElementById('productoForm');
    if (form) {
        form.reset();
    }
    
    document.getElementById('id').value = '';
    document.getElementById('activo').checked = true;
    
    // Resetear subcategoría
    const subcategoriaSelect = document.getElementById('subcategoria');
    if (subcategoriaSelect) {
        subcategoriaSelect.disabled = true;
        subcategoriaSelect.innerHTML = '<option value="">Seleccione una subcategoría</option>';
    }
    
    // Resetear imagen
    resetImageState();
    
    // Resetear password helpers
    const passwordInput = document.getElementById('password');
    const passwordRequired = document.getElementById('passwordRequired');
    const passwordHelp = document.getElementById('passwordHelp');
    
    if (passwordInput) passwordInput.required = true;
    if (passwordRequired) passwordRequired.style.display = 'inline';
    if (passwordHelp) passwordHelp.style.display = 'none';
}

function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// ==================== NOTIFICACIONES ====================

function showSuccess(message) {
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
}

function showError(message) {
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
}

console.log('✅ productos.js cargado correctamente');