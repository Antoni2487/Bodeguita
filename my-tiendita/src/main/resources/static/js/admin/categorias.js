
const API_URL = '/api';
let categorias = [];

// Cargar categorías al iniciar
document.addEventListener('DOMContentLoaded', function() {
    cargarCategorias();
});

// Cargar todas las categorías
async function cargarCategorias() {
    mostrarLoading(true);
    try {
        const response = await fetch(`${API_URL}/categorias`);
        if (!response.ok) throw new Error('Error al cargar categorías');
        
        categorias = await response.json();
        await renderizarCategorias();
    } catch (error) {
        mostrarAlerta('Error al cargar categorías: ' + error.message, 'danger');
    } finally {
        mostrarLoading(false);
    }
}

// Renderizar accordion de categorías
async function renderizarCategorias() {
    const accordion = document.getElementById('categoriasAccordion');
    const emptyState = document.getElementById('emptyState');

    if (categorias.length === 0) {
        accordion.classList.add('d-none');
        emptyState.classList.remove('d-none');
        return;
    }

    accordion.classList.remove('d-none');
    emptyState.classList.add('d-none');

    let html = '';
    for (const categoria of categorias) {
        const subcategorias = await cargarSubcategorias(categoria.id);
        html += generarAccordionItem(categoria, subcategorias);
    }

    accordion.innerHTML = html;
}

// Cargar subcategorías de una categoría
async function cargarSubcategorias(categoriaId) {
    try {
        const response = await fetch(`${API_URL}/categorias/${categoriaId}/subcategorias`);
        if (!response.ok) return [];
        return await response.json();
    } catch (error) {
        console.error('Error al cargar subcategorías:', error);
        return [];
    }
}

// Generar HTML del accordion item
function generarAccordionItem(categoria, subcategorias) {
    const subcategoriasHTML = subcategorias.map(sub => `
        <div class="subcategoria-item d-flex justify-content-between align-items-center">
            <div>
                <i class="bi bi-tag-fill text-primary"></i>
                <strong>${escapeHtml(sub.nombre)}</strong>
                ${sub.descripcion ? `<br><small class="text-muted">${escapeHtml(sub.descripcion)}</small>` : ''}
            </div>
            <div>
                <button class="btn btn-sm btn-outline-primary btn-action" onclick="editarSubcategoria(${sub.id}, ${categoria.id}, '${escapeHtml(categoria.nombre)}')">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger btn-action" onclick="confirmarEliminarSubcategoria(${sub.id}, '${escapeHtml(sub.nombre)}')">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `).join('');

    return `
        <div class="accordion-item">
            <h2 class="accordion-header">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse${categoria.id}">
                    <div class="categoria-header">
                        <span>
                            <i class="bi bi-folder-fill"></i> 
                            <strong>${escapeHtml(categoria.nombre)}</strong>
                            <span class="badge bg-secondary ms-2">${subcategorias.length}</span>
                        </span>
                        <div onclick="event.stopPropagation();">
                            <button class="btn btn-sm btn-outline-primary btn-action me-2" onclick="editarCategoria(${categoria.id})">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger btn-action" onclick="confirmarEliminarCategoria(${categoria.id}, '${escapeHtml(categoria.nombre)}', ${subcategorias.length})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                </button>
            </h2>
            <div id="collapse${categoria.id}" class="accordion-collapse collapse">
                <div class="accordion-body">
                    ${categoria.descripcion ? `<p class="text-muted"><em>${escapeHtml(categoria.descripcion)}</em></p>` : ''}
                    
                    ${subcategorias.length > 0 ? subcategoriasHTML : '<p class="text-muted">No hay subcategorías</p>'}
                    
                    <button class="btn btn-sm btn-success mt-3" onclick="nuevaSubcategoria(${categoria.id}, '${escapeHtml(categoria.nombre)}')">
                        <i class="bi bi-plus-circle"></i> Agregar Subcategoría
                    </button>
                </div>
            </div>
        </div>
    `;
}

// Abrir modal para nueva categoría
function nuevaCategoria() {
    document.getElementById('modalCategoriaTitle').textContent = 'Nueva Categoría';
    document.getElementById('formCategoria').reset();
    document.getElementById('categoriaId').value = '';
    new bootstrap.Modal(document.getElementById('modalCategoria')).show();
}

// Editar categoría
async function editarCategoria(id) {
    const categoria = categorias.find(c => c.id === id);
    if (!categoria) return;

    document.getElementById('modalCategoriaTitle').textContent = 'Editar Categoría';
    document.getElementById('categoriaId').value = categoria.id;
    document.getElementById('categoriaNombre').value = categoria.nombre;
    document.getElementById('categoriaDescripcion').value = categoria.descripcion || '';
    
    new bootstrap.Modal(document.getElementById('modalCategoria')).show();
}

// Guardar categoría
async function guardarCategoria() {
    const id = document.getElementById('categoriaId').value;
    const data = {
        nombre: document.getElementById('categoriaNombre').value.trim(),
        descripcion: document.getElementById('categoriaDescripcion').value.trim() || null
    };

    if (!data.nombre) {
        mostrarAlerta('El nombre es obligatorio', 'warning');
        return;
    }

    try {
        const url = id ? `${API_URL}/categorias/${id}` : `${API_URL}/categorias`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Error al guardar categoría');

        bootstrap.Modal.getInstance(document.getElementById('modalCategoria')).hide();
        mostrarAlerta(id ? 'Categoría actualizada exitosamente' : 'Categoría creada exitosamente', 'success');
        await cargarCategorias();
    } catch (error) {
        mostrarAlerta('Error: ' + error.message, 'danger');
    }
}

// Abrir modal para nueva subcategoría
function nuevaSubcategoria(categoriaId, categoriaNombre) {
    document.getElementById('modalSubcategoriaTitle').textContent = 'Nueva Subcategoría';
    document.getElementById('formSubcategoria').reset();
    document.getElementById('subcategoriaId').value = '';
    document.getElementById('subcategoriaCategoriaId').value = categoriaId;
    document.getElementById('subcategoriaCategoriaNombre').value = categoriaNombre;
    
    new bootstrap.Modal(document.getElementById('modalSubcategoria')).show();
}

// Editar subcategoría
async function editarSubcategoria(id, categoriaId, categoriaNombre) {
    try {
        const response = await fetch(`${API_URL}/subcategorias/${id}`);
        if (!response.ok) throw new Error('Error al cargar subcategoría');
        
        const subcategoria = await response.json();
        
        document.getElementById('modalSubcategoriaTitle').textContent = 'Editar Subcategoría';
        document.getElementById('subcategoriaId').value = subcategoria.id;
        document.getElementById('subcategoriaCategoriaId').value = categoriaId;
        document.getElementById('subcategoriaCategoriaNombre').value = categoriaNombre;
        document.getElementById('subcategoriaNombre').value = subcategoria.nombre;
        document.getElementById('subcategoriaDescripcion').value = subcategoria.descripcion || '';
        
        new bootstrap.Modal(document.getElementById('modalSubcategoria')).show();
    } catch (error) {
        mostrarAlerta('Error: ' + error.message, 'danger');
    }
}

// Guardar subcategoría
async function guardarSubcategoria() {
    const id = document.getElementById('subcategoriaId').value;
    const data = {
        nombre: document.getElementById('subcategoriaNombre').value.trim(),
        descripcion: document.getElementById('subcategoriaDescripcion').value.trim() || null,
        categoriaId: parseInt(document.getElementById('subcategoriaCategoriaId').value)
    };

    if (!data.nombre) {
        mostrarAlerta('El nombre es obligatorio', 'warning');
        return;
    }

    try {
        const url = id ? `${API_URL}/subcategorias/${id}` : `${API_URL}/subcategorias`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Error al guardar subcategoría');

        bootstrap.Modal.getInstance(document.getElementById('modalSubcategoria')).hide();
        mostrarAlerta(id ? 'Subcategoría actualizada exitosamente' : 'Subcategoría creada exitosamente', 'success');
        await cargarCategorias();
    } catch (error) {
        mostrarAlerta('Error: ' + error.message, 'danger');
    }
}

// Confirmar eliminar categoría
function confirmarEliminarCategoria(id, nombre, numSubcategorias) {
    const mensaje = numSubcategorias > 0 
        ? `¿Estás seguro de eliminar la categoría "${nombre}"? Se eliminarán también sus ${numSubcategorias} subcategoría(s).`
        : `¿Estás seguro de eliminar la categoría "${nombre}"?`;
    
    document.getElementById('mensajeEliminar').textContent = mensaje;
    document.getElementById('btnConfirmarEliminar').onclick = () => eliminarCategoria(id);
    
    new bootstrap.Modal(document.getElementById('modalEliminar')).show();
}

// Eliminar categoría
async function eliminarCategoria(id) {
    try {
        const response = await fetch(`${API_URL}/categorias/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Error al eliminar categoría');

        bootstrap.Modal.getInstance(document.getElementById('modalEliminar')).hide();
        mostrarAlerta('Categoría eliminada exitosamente', 'success');
        await cargarCategorias();
    } catch (error) {
        mostrarAlerta('Error: ' + error.message, 'danger');
    }
}

// Confirmar eliminar subcategoría
function confirmarEliminarSubcategoria(id, nombre) {
    document.getElementById('mensajeEliminar').textContent = 
        `¿Estás seguro de eliminar la subcategoría "${nombre}"?`;
    document.getElementById('btnConfirmarEliminar').onclick = () => eliminarSubcategoria(id);
    
    new bootstrap.Modal(document.getElementById('modalEliminar')).show();
}

// Eliminar subcategoría
async function eliminarSubcategoria(id) {
    try {
        const response = await fetch(`${API_URL}/subcategorias/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Error al eliminar subcategoría');

        bootstrap.Modal.getInstance(document.getElementById('modalEliminar')).hide();
        mostrarAlerta('Subcategoría eliminada exitosamente', 'success');
        await cargarCategorias();
    } catch (error) {
        mostrarAlerta('Error: ' + error.message, 'danger');
    }
}

// Mostrar alerta
function mostrarAlerta(mensaje, tipo) {
    const alertContainer = document.getElementById('alertContainer');
    const alert = `
        <div class="alert alert-${tipo} alert-dismissible fade show" role="alert">
            ${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    alertContainer.innerHTML = alert;
    
    setTimeout(() => {
        const alertElement = alertContainer.querySelector('.alert');
        if (alertElement) alertElement.remove();
    }, 5000);
}

// Mostrar/ocultar loading
function mostrarLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const accordion = document.getElementById('categoriasAccordion');
    
    if (show) {
        spinner.classList.remove('d-none');
        accordion.classList.add('d-none');
    } else {
        spinner.classList.add('d-none');
    }
}

// Escape HTML para prevenir XSS
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
        