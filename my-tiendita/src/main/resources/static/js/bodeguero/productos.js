/**
 * Gestión de Productos - Bodeguero
 * Maneja el CRUD completo de productos en la bodega
 */

// ==================== VARIABLES GLOBALES ====================
let productosActuales = [];
let catalogoCompleto = [];
let categorias = [];

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    // Siempre cargar categorías (para los filtros)
    cargarCategorias();
    
    // Solo cargar productos si hay bodega seleccionada
    if (BODEGA_ID) {
        cargarProductosBodega();
    }
});

// ==================== CARGAR DATOS ====================

/**
 * Cargar categorías para los filtros
 */
async function cargarCategorias() {
    try {
        const response = await fetch('/api/productos/form-data/categorias');
        if (response.ok) {
            const data = await response.json();
            categorias = Object.entries(data).map(([id, nombre]) => ({ id, nombre }));
            
            // Llenar select de filtro principal (solo si existe en el DOM)
            const selectFiltro = document.getElementById('filtroCategoriaId');
            if (selectFiltro) {
                categorias.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.id;
                    option.textContent = cat.nombre;
                    selectFiltro.appendChild(option);
                });
            }

            // Llenar select de filtro en catálogo (solo si existe en el DOM)
            const selectCatalogo = document.getElementById('filtroCatalogoCategoriaId');
            if (selectCatalogo) {
                categorias.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.id;
                    option.textContent = cat.nombre;
                    selectCatalogo.appendChild(option);
                });
            }
        }
    } catch (error) {
        console.error('Error al cargar categorías:', error);
    }
}

/**
 * Cargar productos de la bodega
 */
async function cargarProductosBodega() {
    try {
        const response = await fetch(`/api/producto-bodega/bodega/${BODEGA_ID}`);
        
        if (response.ok) {
            productosActuales = await response.json();
            renderizarProductos(productosActuales);
        } else {
            mostrarError('Error al cargar productos');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión al cargar productos');
    }
}

/**
 * Renderizar grid de productos
 */
function renderizarProductos(productos) {
    const grid = document.getElementById('productosGrid');
    const emptyState = document.getElementById('emptyState');
    const contador = document.getElementById('contadorProductos');

    // Actualizar contador
    contador.textContent = productos.length;

    if (productos.length === 0) {
        grid.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    
    grid.innerHTML = productos.map(producto => `
        <div class="col-lg-3 col-md-4 col-sm-6">
            <div class="producto-card">
                <div class="producto-card-img">
                    <img src="${producto.productoImagen || '/images/no-image.png'}" 
                         alt="${producto.productoNombre}"
                         onerror="this.src='/images/no-image.png'">
                    ${producto.activo 
                        ? '<span class="badge-estado badge-activo"><i class="bi bi-check-circle me-1"></i>Activo</span>'
                        : '<span class="badge-estado badge-inactivo"><i class="bi bi-x-circle me-1"></i>Inactivo</span>'
                    }
                    ${producto.stock <= 5 
                        ? '<span class="badge-stock-bajo"><i class="bi bi-exclamation-triangle me-1"></i>Stock bajo</span>'
                        : ''
                    }
                </div>
                <div class="producto-card-body">
                    <span class="producto-categoria">
                        <i class="bi bi-tag-fill me-1"></i>
                        ${producto.categoriaNombre || 'Sin categoría'}
                    </span>
                    <h5 class="producto-nombre">${producto.productoNombre}</h5>
                    
                    <div class="producto-precios">
                        <div class="precio-item">
                            <span class="precio-label">Sugerido</span>
                            <span class="precio-valor precio-sugerido">S/ ${parseFloat(producto.precioSugerido).toFixed(2)}</span>
                        </div>
                        <div class="precio-item">
                            <span class="precio-label">Tu precio</span>
                            <span class="precio-valor precio-bodeguero">S/ ${parseFloat(producto.precioBodeguero).toFixed(2)}</span>
                        </div>
                    </div>

                    <div class="producto-stock ${producto.stock <= 5 ? 'stock-bajo' : ''}">
                        <span class="stock-label">
                            <i class="bi bi-box me-1"></i>Stock:
                        </span>
                        <span class="stock-valor">${producto.stock}</span>
                    </div>

                    <div class="producto-card-actions">
                        <button class="btn-card-action" 
                                onclick="abrirModalEditar(${producto.id})"
                                title="Editar">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="btn-card-action" 
                                onclick="abrirModalDetalle(${producto.id})"
                                title="Ver detalle">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn-card-action" 
                                onclick="${producto.activo ? `desactivarProducto(${producto.id})` : `activarProducto(${producto.id})`}"
                                title="${producto.activo ? 'Desactivar' : 'Activar'}">
                            <i class="bi bi-${producto.activo ? 'toggle-off' : 'toggle-on'}"></i>
                        </button>
                        <button class="btn-card-action text-danger" 
                                onclick="confirmarQuitarProducto(${producto.id}, '${producto.productoNombre}')"
                                title="Quitar de bodega">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

// ==================== FILTROS Y BÚSQUEDA ====================

/**
 * Aplicar filtros combinados
 */
function aplicarFiltros() {
    const categoriaId = document.getElementById('filtroCategoriaId').value;
    const estado = document.getElementById('filtroEstado').value;
    const stockBajo = document.getElementById('filtroStockBajo').checked;
    const textoBusqueda = document.getElementById('inputBuscar').value.toLowerCase();

    let productosFiltrados = [...productosActuales];

    // Filtrar por categoría
    if (categoriaId) {
        productosFiltrados = productosFiltrados.filter(p => 
            p.categoriaId == categoriaId
        );
    }

    // Filtrar por estado
    if (estado !== '') {
        productosFiltrados = productosFiltrados.filter(p => 
            p.activo.toString() === estado
        );
    }

    // Filtrar por stock bajo
    if (stockBajo) {
        productosFiltrados = productosFiltrados.filter(p => p.stock <= 5);
    }

    // Filtrar por texto
    if (textoBusqueda) {
        productosFiltrados = productosFiltrados.filter(p =>
            p.productoNombre.toLowerCase().includes(textoBusqueda)
        );
    }

    renderizarProductos(productosFiltrados);
}

/**
 * Buscar productos (con debounce)
 */
let buscarTimeout;
function buscarProductos() {
    clearTimeout(buscarTimeout);
    buscarTimeout = setTimeout(() => {
        aplicarFiltros();
    }, 300);
}

/**
 * Limpiar búsqueda
 */
function limpiarBusqueda() {
    document.getElementById('inputBuscar').value = '';
    aplicarFiltros();
}

// ==================== MODAL CATÁLOGO ====================

/**
 * Abrir modal de catálogo
 */
async function abrirModalCatalogo() {
    // Verificar que Bootstrap esté disponible
    if (typeof bootstrap === 'undefined') {
        console.error('Bootstrap no está cargado');
        alert('Error: Bootstrap no está cargado. Recarga la página.');
        return;
    }

    const modalElement = document.getElementById('modalCatalogo');
    if (!modalElement) {
        console.error('Modal no encontrado en el DOM');
        return;
    }

    const modal = new bootstrap.Modal(modalElement);
    modal.show();

    document.getElementById('catalogoLoading').style.display = 'block';
    document.getElementById('catalogoGrid').innerHTML = '';

    try {
        const response = await fetch('/api/productos');
        if (response.ok) {
            const productos = await response.json();
            
            // Filtrar solo productos activos
            catalogoCompleto = productos.filter(p => p.activo);
            
            document.getElementById('catalogoLoading').style.display = 'none';
            renderizarCatalogo(catalogoCompleto);
        }
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('catalogoLoading').style.display = 'none';
        mostrarError('Error al cargar el catálogo');
    }
}

/**
 * Renderizar catálogo
 */
function renderizarCatalogo(productos) {
    const grid = document.getElementById('catalogoGrid');
    
    if (productos.length === 0) {
        grid.innerHTML = '<div class="col-12 text-center py-5"><p class="text-muted">No se encontraron productos</p></div>';
        return;
    }

    grid.innerHTML = productos.map(producto => {
        // Verificar si ya está agregado a la bodega
        const yaAgregado = productosActuales.some(p => p.producto === producto.id);
        
        return `
            <div class="col-lg-3 col-md-4 col-sm-6">
                <div class="catalogo-card ${yaAgregado ? 'ya-agregado' : ''}" 
                     onclick="${!yaAgregado ? `seleccionarProductoCatalogo(${producto.id})` : ''}">
                    ${yaAgregado ? '<span class="badge-ya-agregado">Ya agregado</span>' : ''}
                    <img src="${producto.imagen || '/images/no-image.png'}" 
                         alt="${producto.nombre}"
                         class="catalogo-card-img"
                         onerror="this.src='/images/no-image.png'">
                    <div class="catalogo-card-nombre">${producto.nombre}</div>
                    <div class="catalogo-card-precio">S/ ${parseFloat(producto.precioSugerido).toFixed(2)}</div>
                    <small class="text-muted d-block">
                        <i class="bi bi-tag me-1"></i>${producto.categoriaNombre || 'Sin categoría'}
                    </small>
                    ${!yaAgregado ? '<button class="btn btn-sm btn-primary w-100 mt-2"><i class="bi bi-plus-circle me-1"></i>Agregar</button>' : ''}
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Filtrar catálogo
 */
function filtrarCatalogo() {
    const texto = document.getElementById('buscarCatalogo').value.toLowerCase();
    const categoriaId = document.getElementById('filtroCatalogoCategoriaId').value;

    let productosFiltrados = [...catalogoCompleto];

    if (texto) {
        productosFiltrados = productosFiltrados.filter(p =>
            p.nombre.toLowerCase().includes(texto)
        );
    }

    if (categoriaId) {
        productosFiltrados = productosFiltrados.filter(p =>
            p.categoria == categoriaId
        );
    }

    renderizarCatalogo(productosFiltrados);
}

/**
 * Seleccionar producto del catálogo
 */
function seleccionarProductoCatalogo(productoId) {
    const producto = catalogoCompleto.find(p => p.id === productoId);
    if (!producto) return;

    // Cerrar modal de catálogo
    bootstrap.Modal.getInstance(document.getElementById('modalCatalogo')).hide();

    // Abrir modal de agregar
    document.getElementById('agregarProductoId').value = producto.id;
    document.getElementById('agregarProductoNombre').textContent = producto.nombre;
    document.getElementById('agregarProductoCategoria').textContent = producto.categoriaNombre || 'Sin categoría';
    document.getElementById('agregarProductoImagen').src = producto.imagen || '/images/no-image.png';
    document.getElementById('agregarPrecioSugerido').textContent = parseFloat(producto.precioSugerido).toFixed(2);
    document.getElementById('agregarPrecioBodeguero').value = parseFloat(producto.precioSugerido).toFixed(2);
    document.getElementById('agregarStock').value = 0;
    document.getElementById('agregarActivo').checked = true;

    const modal = new bootstrap.Modal(document.getElementById('modalAgregar'));
    modal.show();
}

// ==================== CRUD PRODUCTOS ====================

/**
 * Guardar nuevo producto
 */
async function guardarProducto(event) {
    event.preventDefault();

    const datos = {
        producto: parseInt(document.getElementById('agregarProductoId').value),
        bodega: BODEGA_ID,
        precioBodeguero: parseFloat(document.getElementById('agregarPrecioBodeguero').value),
        stock: parseInt(document.getElementById('agregarStock').value),
        activo: document.getElementById('agregarActivo').checked
    };

    try {
        const response = await fetch('/api/producto-bodega', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalAgregar')).hide();
            mostrarExito('Producto agregado exitosamente');
            cargarProductosBodega();
        } else {
            const error = await response.text();
            mostrarError(error || 'Error al agregar producto');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión');
    }
}

/**
 * Abrir modal editar
 */
async function abrirModalEditar(productoBodegaId) {
    try {
        const response = await fetch(`/api/producto-bodega/${productoBodegaId}`);
        if (response.ok) {
            const producto = await response.json();

            document.getElementById('editarId').value = producto.id;
            document.getElementById('editarProductoNombre').textContent = producto.productoNombre;
            document.getElementById('editarProductoCategoria').textContent = producto.categoriaNombre || 'Sin categoría';
            document.getElementById('editarProductoImagen').src = producto.productoImagen || '/images/no-image.png';
            document.getElementById('editarPrecioSugerido').textContent = parseFloat(producto.precioSugerido).toFixed(2);
            document.getElementById('editarPrecioBodeguero').value = parseFloat(producto.precioBodeguero).toFixed(2);
            document.getElementById('editarStock').value = producto.stock;

            const modal = new bootstrap.Modal(document.getElementById('modalEditar'));
            modal.show();
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error al cargar producto');
    }
}

/**
 * Actualizar producto
 */
async function actualizarProducto(event) {
    event.preventDefault();

    const id = document.getElementById('editarId').value;
    const producto = productosActuales.find(p => p.id == id);

    const datos = {
        producto: producto.producto,
        bodega: BODEGA_ID,
        precioBodeguero: parseFloat(document.getElementById('editarPrecioBodeguero').value),
        stock: parseInt(document.getElementById('editarStock').value),
        activo: producto.activo
    };

    try {
        const response = await fetch(`/api/producto-bodega/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalEditar')).hide();
            mostrarExito('Producto actualizado exitosamente');
            cargarProductosBodega();
        } else {
            mostrarError('Error al actualizar producto');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión');
    }
}

/**
 * Abrir modal detalle
 */
async function abrirModalDetalle(productoBodegaId) {
    try {
        const response = await fetch(`/api/producto-bodega/${productoBodegaId}`);
        if (response.ok) {
            const producto = await response.json();

            document.getElementById('detalleImagen').src = producto.productoImagen || '/images/no-image.png';
            document.getElementById('detalleNombre').textContent = producto.productoNombre;
            document.getElementById('detalleCategoria').textContent = producto.categoriaNombre || 'Sin categoría';
            document.getElementById('detalleSubcategoria').textContent = producto.subcategoriaNombre || '';
            document.getElementById('detalleDescripcion').textContent = producto.descripcion || 'Sin descripción';
            document.getElementById('detallePrecioSugerido').textContent = parseFloat(producto.precioSugerido).toFixed(2);
            document.getElementById('detallePrecioBodeguero').textContent = parseFloat(producto.precioBodeguero).toFixed(2);
            document.getElementById('detalleStock').innerHTML = `<span class="${producto.stock <= 5 ? 'text-danger' : 'text-success'}">${producto.stock} unidades</span>`;
            document.getElementById('detalleActivo').innerHTML = producto.activo 
                ? '<span class="badge bg-success">Activo</span>' 
                : '<span class="badge bg-secondary">Inactivo</span>';

            // Ocultar subcategoría si no existe
            if (!producto.subcategoriaNombre) {
                document.getElementById('detalleSubcategoria').style.display = 'none';
            }

            const modal = new bootstrap.Modal(document.getElementById('modalDetalle'));
            modal.show();
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error al cargar detalle');
    }
}

/**
 * Desactivar producto
 */
async function desactivarProducto(productoBodegaId) {
    const producto = productosActuales.find(p => p.id === productoBodegaId);

    const datos = {
        producto: producto.producto,
        bodega: BODEGA_ID,
        precioBodeguero: producto.precioBodeguero,
        stock: producto.stock,
        activo: false
    };

    try {
        const response = await fetch(`/api/producto-bodega/${productoBodegaId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (response.ok) {
            mostrarExito('Producto desactivado');
            cargarProductosBodega();
        } else {
            mostrarError('Error al desactivar producto');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión');
    }
}

/**
 * Activar producto
 */
async function activarProducto(productoBodegaId) {
    const producto = productosActuales.find(p => p.id === productoBodegaId);

    const datos = {
        producto: producto.producto,
        bodega: BODEGA_ID,
        precioBodeguero: producto.precioBodeguero,
        stock: producto.stock,
        activo: true
    };

    try {
        const response = await fetch(`/api/producto-bodega/${productoBodegaId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (response.ok) {
            mostrarExito('Producto activado');
            cargarProductosBodega();
        } else {
            mostrarError('Error al activar producto');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión');
    }
}

/**
 * Confirmar quitar producto
 */
function confirmarQuitarProducto(productoBodegaId, nombreProducto) {
    document.getElementById('mensajeConfirmacion').innerHTML = `
        <strong>¿Estás seguro de quitar este producto de tu bodega?</strong><br>
        <p class="mt-2 mb-0">Producto: <strong>${nombreProducto}</strong></p>
        <p class="text-muted small mt-1">Esta acción no se puede deshacer.</p>
    `;

    const btnConfirmar = document.getElementById('btnConfirmarAccion');
    btnConfirmar.onclick = () => quitarProducto(productoBodegaId);

    const modal = new bootstrap.Modal(document.getElementById('modalConfirmar'));
    modal.show();
}

/**
 * Quitar producto de la bodega
 */
async function quitarProducto(productoBodegaId) {
    try {
        const response = await fetch(`/api/producto-bodega/${productoBodegaId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalConfirmar')).hide();
            mostrarExito('Producto quitado de tu bodega');
            cargarProductosBodega();
        } else {
            mostrarError('Error al quitar producto');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión');
    }
}

// ==================== SELECTOR DE BODEGA ====================

/**
 * Cambiar bodega seleccionada
 */
function seleccionarBodega() {
    const bodegaId = document.getElementById('selectorBodega').value;
    if (bodegaId) {
        window.location.href = `/bodeguero/mis_productos?bodegaId=${bodegaId}`;
    }
}

// ==================== NOTIFICACIONES ====================

/**
 * Mostrar mensaje de éxito
 */
function mostrarExito(mensaje) {
    const alerta = document.getElementById("alertPlaceholder");
    alerta.innerHTML = `
      <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle me-2"></i>${mensaje}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>`;
}

function mostrarError(mensaje) {
    const alerta = document.getElementById("alertPlaceholder");
    alerta.innerHTML = `
      <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle me-2"></i>${mensaje}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>`;
}
