/**
 * ADMIN - GESTIÓN DE USUARIOS
 * BodeguitaSmart
 */

(() => {
    let modal, detalleModal;
    let isEdit = false;

    // Almacenar instancias de DataTable globalmente
    let dtAdmin, dtBodeguero, dtCliente;

    // --------------------------
    // UTILIDADES
    // --------------------------

    function alertBox(type, message) {
        const el = document.createElement("div");
        el.className = `alert alert-${type} alert-dismissible fade show`;
        el.innerHTML = `
        <div><i class="bi bi-${type === 'success' ? 'check-circle' :
            type === 'danger' ? 'x-circle' : 'info-circle'} me-2"></i>${message}</div>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        
        document.getElementById("alertPlaceholder").appendChild(el);

        setTimeout(() => {
            el.classList.remove("show");
            setTimeout(() => el.remove(), 200);
        }, 5000);
    }

    function getRolBadge(rol) {
        const map = {
            "ADMIN": "badge-admin",
            "BODEGUERO": "badge-bodeguero",
            "CLIENTE": "badge-cliente"
        };
        return `<span class="badge badge-rol ${map[rol] || 'bg-secondary'}">${rol}</span>`;
    }

    function getEstadoBadge(activo) {
        return activo
            ? `<span class="badge badge-activo"><i class="bi bi-check-circle me-1"></i>Activo</span>`
            : `<span class="badge badge-inactivo"><i class="bi bi-x-circle me-1"></i>Inactivo</span>`;
    }

    // --------------------------
    // CARGAR TODOS LOS USUARIOS
    // --------------------------

    async function loadUsuarios() {
        try {
            const res = await fetch("/api/usuarios", { headers: { "Accept": "application/json" } });
            if (!res.ok) throw new Error("No se pudo obtener la lista");

            const data = await res.json();

            // Separar por rol
            const admins = data.filter(u => u.rol === "ADMIN");
            const bodegueros = data.filter(u => u.rol === "BODEGUERO");
            const clientes = data.filter(u => u.rol === "CLIENTE");

            updateCounters(admins, bodegueros, clientes);
            fillTables(admins, bodegueros, clientes);

        } catch (e) {
            alertBox("danger", e.message);
        }
    }

    function updateCounters(admins, bodegueros, clientes) {
        document.getElementById("count-admin").innerText = admins.length;
        document.getElementById("count-bodeguero").innerText = bodegueros.length;
        document.getElementById("count-cliente").innerText = clientes.length;
    }

    // --------------------------
    // LLENAR TABLAS
    // --------------------------

    function fillTables(admins, bodegueros, clientes) {
        fillTable("tablaAdmin", admins, "ADMIN");
        fillTable("tablaBodeguero", bodegueros, "BODEGUERO");
        fillTable("tablaCliente", clientes, "CLIENTE");
    }

    function fillTable(tableId, data, rol) {
        const table = document.querySelector(`#${tableId} tbody`);
        
        // Destruir DataTable si existe
        destroyDataTable(tableId);
        
        table.innerHTML = "";

        if (data.length === 0) {
            const colspan = rol === "ADMIN" ? 7 : 8;
            table.innerHTML = `
            <tr>
                <td colspan="${colspan}" class="table-loading">
                    <i class="bi bi-inbox fs-1"></i>
                    <p class="mt-2 mb-0">No hay usuarios registrados</p>
                </td>
            </tr>`;
            return;
        }

        data.forEach(u => {
            const tr = document.createElement("tr");

            let columns = `
                <td>${u.id}</td>
                <td><strong>${u.nombre}</strong></td>
                <td>${u.email}</td>
                <td>${u.numeroDocumento || "-"}</td>
                <td>${u.telefono || "-"}</td>
            `;

            // Columnas específicas por rol
            if (rol === "BODEGUERO") {
                const numBodegas = u.bodegas && Array.isArray(u.bodegas) ? u.bodegas.length : 0;
                columns += `<td><span class="badge bg-warning">${numBodegas}</span></td>`;
            } else if (rol === "CLIENTE") {
                columns += `<td>${u.direccion || "-"}</td>`;
            }

            columns += `
                <td>${getEstadoBadge(u.activo)}</td>
                <td class="table-actions">
                    <button class="btn btn-sm btn-outline-info btn-action" onclick="openDetalle(${u.id})">
                        <i class="bi bi-info-circle"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-primary btn-action" onclick="openEdit(${u.id})">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger btn-action" onclick="deleteUsuario(${u.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            `;

            tr.innerHTML = columns;
            table.appendChild(tr);
        });

        // Inicializar DataTable solo si hay datos
        if (data.length > 0) {
            initDataTable(tableId);
        }
    }

    function destroyDataTable(tableId) {
        // Mapear tableId a variable global
        const dtMap = {
            'tablaAdmin': 'dtAdmin',
            'tablaBodeguero': 'dtBodeguero',
            'tablaCliente': 'dtCliente'
        };
        
        const dtVarName = dtMap[tableId];
        
        try {
            if (window[dtVarName] && typeof window[dtVarName].destroy === 'function') {
                window[dtVarName].destroy();
                window[dtVarName] = null;
            }
        } catch (e) {
            console.warn(`Error al destruir DataTable ${tableId}:`, e);
        }
    }

    function initDataTable(tableId) {
        const dtMap = {
            'tablaAdmin': 'dtAdmin',
            'tablaBodeguero': 'dtBodeguero',
            'tablaCliente': 'dtCliente'
        };
        
        const dtVarName = dtMap[tableId];

        try {
            const dt = new DataTable(`#${tableId}`, {
                language: { url: "https://cdn.datatables.net/plug-ins/1.13.7/i18n/es-ES.json" },
                pageLength: 10,
                responsive: true,
                ordering: true,
                order: [[0, 'desc']], // Ordenar por ID descendente
                destroy: true // Permitir recrear DataTable
            });

            window[dtVarName] = dt;
        } catch (e) {
            console.warn(`No se pudo inicializar DataTable para ${tableId}:`, e);
        }
    }

    // --------------------------
    // MODAL CREAR
    // --------------------------

    window.openCreate = function () {
        isEdit = false;

        document.getElementById("usuarioModalTitle").innerHTML =
            `<i class="bi bi-person-plus-fill me-2"></i>Nuevo Usuario`;

        resetForm(true);

        modal.show();
    };

    // --------------------------
    // MODAL EDITAR
    // --------------------------

    window.openEdit = async function (id) {
        isEdit = true;

        try {
            const res = await fetch(`/api/usuarios/${id}`);
            if (!res.ok) throw new Error("No se pudo cargar el usuario");

            const u = await res.json();

            document.getElementById("usuarioModalTitle").innerHTML =
                `<i class="bi bi-pencil-square me-2"></i>Editar Usuario #${id}`;

            resetForm(false);

            document.getElementById("id").value = u.id;
            document.getElementById("nombre").value = u.nombre;
            document.getElementById("email").value = u.email;
            document.getElementById("telefono").value = u.telefono || "";
            document.getElementById("direccion").value = u.direccion || "";
            document.getElementById("rol").value = u.rol;
            document.getElementById("activo").checked = u.activo;
            document.getElementById("numeroDocumento").value = u.numeroDocumento || "";

            modal.show();

        } catch (e) {
            alertBox("danger", e.message);
        }
    };

    // --------------------------
    // MODAL DETALLE
    // --------------------------

    window.openDetalle = async function (id) {
        try {
            const res = await fetch(`/api/usuarios/${id}`);
            if (!res.ok) throw new Error("No se pudo cargar detalle");

            const u = await res.json();

            let detalleHTML = `
                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-hash me-2"></i>ID</div>
                    <div class="detalle-value"><strong>#${u.id}</strong></div>
                </div>

                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-person me-2"></i>Nombre</div>
                    <div class="detalle-value">${u.nombre}</div>
                </div>

                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-envelope me-2"></i>Email</div>
                    <div class="detalle-value">${u.email}</div>
                </div>

                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-shield me-2"></i>Rol</div>
                    <div class="detalle-value">${getRolBadge(u.rol)}</div>
                </div>

                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-card-text me-2"></i>Número de Documento</div>
                    <div class="detalle-value">${u.numeroDocumento || "-"}</div>
                </div>

                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-telephone me-2"></i>Teléfono</div>
                    <div class="detalle-value">${u.telefono || "-"}</div>
                </div>
            `;

            // Campos específicos por rol
            if (u.rol === "ADMIN") {
                detalleHTML += `
                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-calendar me-2"></i>Fecha de Creación</div>
                        <div class="detalle-value">${u.dateCreated ? new Date(u.dateCreated).toLocaleString('es-PE') : "-"}</div>
                    </div>
                `;
            }

            if (u.rol === "BODEGUERO") {
                const numBodegas = u.bodegas && Array.isArray(u.bodegas) ? u.bodegas.length : 0;
                detalleHTML += `
                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-shop me-2"></i>Bodegas Asignadas</div>
                        <div class="detalle-value">
                            ${numBodegas > 0 
                                ? `<span class="badge bg-warning">${numBodegas} bodega(s)</span>` 
                                : "<span class='text-muted'>Sin bodegas asignadas</span>"}
                        </div>
                    </div>

                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-geo-alt me-2"></i>Ubicación</div>
                        <div class="detalle-value">
                            ${u.latitud && u.longitud 
                                ? `Lat: ${u.latitud}, Lng: ${u.longitud}` 
                                : "<span class='text-muted'>Sin ubicación registrada</span>"}
                        </div>
                    </div>

                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-calendar me-2"></i>Fecha de Creación</div>
                        <div class="detalle-value">${u.dateCreated ? new Date(u.dateCreated).toLocaleString('es-PE') : "-"}</div>
                    </div>
                `;
            }

            if (u.rol === "CLIENTE") {
                detalleHTML += `
                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-geo-alt me-2"></i>Dirección</div>
                        <div class="detalle-value">${u.direccion || "-"}</div>
                    </div>

                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-pin-map me-2"></i>Ubicación</div>
                        <div class="detalle-value">
                            ${u.latitud && u.longitud 
                                ? `Lat: ${u.latitud}, Lng: ${u.longitud}` 
                                : "<span class='text-muted'>Sin ubicación registrada</span>"}
                        </div>
                    </div>

                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-bag me-2"></i>Total de Pedidos</div>
                        <div class="detalle-value">
                            <span class="badge bg-success">${u.pedidos?.length || 0} pedido(s)</span>
                        </div>
                    </div>

                    <div class="detalle-row">
                        <div class="detalle-label"><i class="bi bi-calendar me-2"></i>Fecha de Registro</div>
                        <div class="detalle-value">${u.dateCreated ? new Date(u.dateCreated).toLocaleString('es-PE') : "-"}</div>
                    </div>
                `;
            }

            detalleHTML += `
                <div class="detalle-row">
                    <div class="detalle-label"><i class="bi bi-toggle-on me-2"></i>Estado</div>
                    <div class="detalle-value">${getEstadoBadge(u.activo)}</div>
                </div>
            `;

            document.getElementById("detalleContent").innerHTML = detalleHTML;

            detalleModal.show();

        } catch (e) {
            alertBox("danger", e.message);
        }
    };

    // --------------------------
    // MANEJO DE ERRORES EN CAMPOS
    // --------------------------

    function clearFieldErrors() {
        document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        document.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
    }

    function showFieldError(fieldId, message) {
        const field = document.getElementById(fieldId);
        if (!field) return;

        field.classList.add('is-invalid');
        
        // Eliminar error previo si existe
        const existingError = field.parentElement.querySelector('.invalid-feedback');
        if (existingError) existingError.remove();

        // Crear mensaje de error
        const errorDiv = document.createElement('div');
        errorDiv.className = 'invalid-feedback';
        errorDiv.style.display = 'block';
        errorDiv.innerHTML = `<i class="bi bi-exclamation-circle me-1"></i>${message}`;
        
        field.parentElement.appendChild(errorDiv);
        
        // Hacer scroll al primer error
        field.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // --------------------------
    // RESET FORM
    // --------------------------

    function resetForm(isNew) {
        document.getElementById("usuarioForm").reset();
        clearFieldErrors();

        document.getElementById("passwordRequired").style.display = isNew ? "inline" : "none";
        document.getElementById("passwordHelp").style.display = isNew ? "none" : "block";
        document.getElementById("password").required = isNew;
        
        // Limpiar ID oculto
        document.getElementById("id").value = "";
    }

    // --------------------------
    // SUBMIT FORM
    // --------------------------

    document.getElementById("usuarioForm").addEventListener("submit", async e => {
        e.preventDefault();

        const id = document.getElementById("id").value;

        const payload = {
            nombre: document.getElementById("nombre").value.trim(),
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            telefono: document.getElementById("telefono").value.trim(),
            direccion: document.getElementById("direccion").value.trim(),
            rol: document.getElementById("rol").value,
            activo: document.getElementById("activo").checked,
            numeroDocumento: document.getElementById("numeroDocumento").value.trim(),
            bodegas: []
        };

        // Validaciones
        if (!payload.nombre || !payload.email || !payload.rol) {
            alertBox("warning", "Nombre, email y rol son obligatorios");
            return;
        }

        // Validar documento si se ingresó
        if (payload.numeroDocumento && !payload.numeroDocumento.match(/^[0-9]{8}$|^[0-9]{11}$/)) {
            alertBox("warning", "El documento debe tener 8 dígitos (DNI) o 11 dígitos (RUC)");
            return;
        }

        try {
            const res = await fetch(isEdit ? `/api/usuarios/${id}` : "/api/usuarios", {
                method: isEdit ? "PUT" : "POST",
                headers: { "Content-Type": "application/json", "Accept": "application/json" },
                body: JSON.stringify(payload)
            });

            // Manejar respuestas de error
            if (!res.ok) {
                const errorData = await res.json().catch(() => ({}));
                
                // Errores de validación de campos (400 Bad Request)
                if (errorData.fieldErrors && errorData.fieldErrors.length > 0) {
                    const emailError = errorData.fieldErrors.find(err => err.field === "email");
                    if (emailError) {
                        alertBox("danger", `❌ ${emailError.message}`);
                        return;
                    }
                    
                    const docError = errorData.fieldErrors.find(err => err.field === "numeroDocumento");
                    if (docError) {
                        alertBox("danger", `❌ ${docError.message}`);
                        return;
                    }
                    
                    // Mostrar el primer error encontrado
                    alertBox("danger", `❌ ${errorData.fieldErrors[0].message}`);
                    return;
                }
                
                // Error 409 Conflict (DataIntegrityViolation o ReferencedException)
                if (res.status === 409) {
                    alertBox("danger", `❌ ${errorData.message || "Error de integridad de datos"}`);
                    return;
                }
                
                // Error 404 Not Found
                if (res.status === 404) {
                    alertBox("danger", "❌ Usuario no encontrado");
                    return;
                }
                
                // Otros errores con mensaje
                const errorMsg = errorData.message || "Error al guardar el usuario";
                alertBox("danger", `❌ ${errorMsg}`);
                return;
            }

            alertBox("success", isEdit ? "✅ Usuario actualizado correctamente" : "✅ Usuario creado correctamente");
            modal.hide();
            
            // Recargar usuarios después de guardar
            await loadUsuarios();

        } catch (e) {
            console.error("Error en submit:", e);
            alertBox("danger", "❌ Error de conexión. Por favor, intenta nuevamente.");
        }
    });

    // --------------------------
    // ELIMINAR
    // --------------------------

    window.deleteUsuario = async function (id) {
        if (!confirm(`¿Está seguro de eliminar el usuario #${id}?`)) return;

        try {
            const res = await fetch(`/api/usuarios/${id}`, {
                method: "DELETE",
                headers: { "Accept": "application/json" }
            });

            if (!res.ok) {
                throw new Error("No se pudo eliminar el usuario");
            }

            alertBox("success", "✅ Usuario eliminado correctamente");
            
            // Recargar usuarios después de eliminar
            await loadUsuarios();

        } catch (e) {
            alertBox("danger", e.message);
        }
    };

    // --------------------------
    // INIT
    // --------------------------

    document.addEventListener("DOMContentLoaded", () => {
        modal = new bootstrap.Modal(document.getElementById("usuarioModal"));
        detalleModal = new bootstrap.Modal(document.getElementById("detalleModal"));

        loadUsuarios();
        console.log("✅ Gestión de Usuarios cargado correctamente");
    });

})();