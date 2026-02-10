document.addEventListener('DOMContentLoaded', function() {
    cargarSolicitudes();
});

// Variable global para almacenar los datos
let todasLasSolicitudes = [];
let filtroActual = 'PENDIENTE';

// 1. Cargar datos desde el Backend
function cargarSolicitudes() {
    mostrarLoader(true);
    
    // Asumimos que tienes un RestController expuesto en /api/solicitudes
    // Si no, hay que crearlo.
    fetch('/api/solicitudes') 
        .then(response => {
            if (!response.ok) throw new Error("Error al cargar solicitudes");
            return response.json();
        })
        .then(data => {
            todasLasSolicitudes = data;
            actualizarContadores();
            renderizarSolicitudes(); // Renderiza según el filtro actual
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarAlerta('Error al cargar datos: ' + error.message, 'danger');
        })
        .finally(() => {
            mostrarLoader(false);
        });
}

// 2. Filtrar al cambiar de Tab
function filtrarSolicitudes(estado) {
    filtroActual = estado;
    renderizarSolicitudes();
}

// 3. Renderizar las Cards (Pilas)
function renderizarSolicitudes() {
    const container = document.getElementById('solicitudesContainer');
    const emptyState = document.getElementById('emptyState');
    
    // Filtrar datos
    const filtradas = todasLasSolicitudes.filter(s => s.estado === filtroActual);
    
    container.innerHTML = '';
    
    if (filtradas.length === 0) {
        container.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    container.style.display = 'flex'; // row es flex por defecto

    filtradas.forEach(solicitud => {
        const cardHTML = crearCardHTML(solicitud);
        container.insertAdjacentHTML('beforeend', cardHTML);
    });
}

// 4. Crear HTML de una Card individual
function crearCardHTML(s) {
    // Definir colores y botones según estado
    let borderClass = 'border-start-pending';
    let iconClass = 'bg-icon-pending';
    let icon = 'bi-shop';
    let botonesAccion = '';

    if (s.estado === 'PENDIENTE') {
        borderClass = 'border-start-pending';
        iconClass = 'bg-icon-pending';
        botonesAccion = `
            <div class="d-flex gap-2 mt-3 pt-3 border-top">
                <button class="btn btn-success flex-grow-1" onclick="procesarSolicitud(${s.id}, 'APROBAR')">
                    <i class="bi bi-check-lg"></i> Aprobar
                </button>
                <button class="btn btn-outline-danger flex-grow-1" onclick="procesarSolicitud(${s.id}, 'RECHAZAR')">
                    <i class="bi bi-x-lg"></i> Rechazar
                </button>
            </div>
        `;
    } else if (s.estado === 'APROBADA') {
        borderClass = 'border-start-approved';
        iconClass = 'bg-icon-approved';
        icon = 'bi-check-lg';
        botonesAccion = `<div class="mt-3 text-success fw-bold text-center"><i class="bi bi-check-circle-fill"></i> Solicitud Aprobada</div>`;
    } else {
        borderClass = 'border-start-rejected';
        iconClass = 'bg-icon-rejected';
        icon = 'bi-x-lg';
        botonesAccion = `<div class="mt-3 text-danger fw-bold text-center"><i class="bi bi-x-circle-fill"></i> Solicitud Rechazada</div>`;
    }

    return `
        <div class="col-md-6 col-lg-4">
            <div class="card solicitud-card shadow-sm h-100 ${borderClass}">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-3">
                        <div class="card-header-icon ${iconClass} me-3 shadow-sm">
                            <i class="bi ${icon}"></i>
                        </div>
                        <div>
                            <h5 class="card-title fw-bold mb-0 text-truncate" title="${s.nombreBodega}">
                                ${s.nombreBodega}
                            </h5>
                            <small class="text-muted">ID: #${s.id}</small>
                        </div>
                    </div>

                    <div class="row g-2">
                        <div class="col-12">
                            <div class="info-label">Solicitante</div>
                            <div class="info-value text-truncate">
                                <i class="bi bi-person me-1 text-primary"></i> ${s.nombreSolicitante}
                            </div>
                        </div>
                        <div class="col-12">
                            <div class="info-label">Contacto</div>
                            <div class="info-value text-truncate">
                                <i class="bi bi-envelope me-1 text-primary"></i> ${s.email}
                            </div>
                            <div class="info-value">
                                <i class="bi bi-telephone me-1 text-primary"></i> ${s.telefono}
                            </div>
                        </div>
                        <div class="col-12">
                            <div class="info-label">Dirección</div>
                            <div class="info-value small text-muted">
                                <i class="bi bi-geo-alt me-1 text-primary"></i> ${s.direccionBodega}
                            </div>
                        </div>
                        <div class="col-12" ${s.ruc ? '' : 'style="display:none"'}>
                            <div class="info-label">RUC</div>
                            <div class="info-value">${s.ruc || '-'}</div>
                        </div>
                    </div>

                    ${botonesAccion}
                </div>
                <div class="card-footer bg-light border-0 py-2">
                    <small class="text-muted">
                        <i class="bi bi-clock"></i> Recibido hace: ${calcularTiempo(s.dateCreated)}
                    </small>
                </div>
            </div>
        </div>
    `;
}

// 5. Lógica para Aprobar/Rechazar
function procesarSolicitud(id, accion) {
    if (!confirm(`¿Estás seguro de ${accion} esta solicitud?`)) return;

    const endpoint = accion === 'APROBAR' 
        ? `/api/solicitudes/${id}/aprobar` 
        : `/api/solicitudes/${id}/rechazar`;

    // Asumimos PUT o POST
    fetch(endpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // 'X-CSRF-TOKEN': ... (Si usas Spring Security CSRF, necesitas inyectar el token aquí)
        }
    })
    .then(response => {
        if (response.ok) {
            mostrarAlerta(`Solicitud ${accion === 'APROBAR' ? 'aprobada' : 'rechazada'} con éxito`, 'success');
            cargarSolicitudes(); // Recargar todo
        } else {
            return response.text().then(text => { throw new Error(text) });
        }
    })
    .catch(err => {
        console.error(err);
        mostrarAlerta('Error al procesar: ' + err.message, 'danger');
    });
}

// Utilitarios
function actualizarContadores() {
    const pendientes = todasLasSolicitudes.filter(s => s.estado === 'PENDIENTE').length;
    const badge = document.getElementById('badge-pendientes');
    if(badge) badge.textContent = pendientes;
}

function mostrarLoader(show) {
    document.getElementById('loader').style.display = show ? 'block' : 'none';
    if(show) {
        document.getElementById('solicitudesContainer').style.display = 'none';
        document.getElementById('emptyState').style.display = 'none';
    }
}

function mostrarAlerta(mensaje, tipo) {
    const placeholder = document.getElementById('alertPlaceholder');
    placeholder.innerHTML = `
        <div class="alert alert-${tipo} alert-dismissible fade show" role="alert">
            ${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    setTimeout(() => { placeholder.innerHTML = ''; }, 3000);
}

function calcularTiempo(dateString) {
    // Implementación simple de "hace X tiempo"
    if(!dateString) return '-';
    // Si usas moment.js es más fácil, si no, manual:
    return 'un momento'; // Placeholder
}