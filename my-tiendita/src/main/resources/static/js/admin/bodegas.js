"use strict";
/**
 * ADMIN - GESTIÓN DE BODEGAS
 * BodeguitaSmart - Flujo completo con bodeguero
 */

(() => {
  let modal, detalleModal, dataTable, isEdit = false;
  let map, marker;

  /**
   * 🔐 Headers de autenticación
   */
  function getAuthHeaders() {
    return {
      Accept: "application/json",
      "Content-Type": "application/json",
    };
  }

  /**
   * Mostrar alertas
   */
  function alertBox(type, message) {
    const el = document.createElement("div");
    el.className = `alert alert-${type} alert-dismissible fade show`;
    el.role = "alert";
    el.innerHTML = `
      <div><i class="bi bi-${
        type === "success"
          ? "check-circle"
          : type === "danger"
          ? "x-circle"
          : "info-circle"
      } me-2"></i>${message}</div>
      <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    const placeholder = document.getElementById("alertPlaceholder");
    placeholder.appendChild(el);
    setTimeout(() => {
      el.classList.remove("show");
      setTimeout(() => el.remove(), 150);
    }, 5000);
  }

  /**
   * Badge para estado
   */
  function getEstadoBadge(activo) {
    return activo
      ? '<span class="badge badge-activo"><i class="bi bi-check-circle me-1"></i>Activo</span>'
      : '<span class="badge badge-inactivo"><i class="bi bi-x-circle me-1"></i>Inactivo</span>';
  }

  /**
   * ========== CONSULTAR API DNI/RUC (miapi.cloud) ==========
   */
  async function consultarDocumento() {
    const numero = document.getElementById("numeroDocumento").value.trim();

    if (!numero) {
      alertBox("warning", "Ingrese un número de documento");
      return;
    }

    if (!/^[0-9]{8}$|^[0-9]{11}$/.test(numero)) {
      alertBox(
        "warning",
        "El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC)"
      );
      return;
    }

    try {
      alertBox("info", "Consultando API externa...");

      const res = await fetch(`/api/consultar-documento/${numero}`, {
        headers: getAuthHeaders(),
      });

      const data = await res.json();

      if (data.success) {
        // ✅ Auto-rellenar nombre
        if (data.nombre) {
          const nombreInput = document.getElementById("nombreBodeguero");
          if (nombreInput) {
            nombreInput.value = data.nombre;
          }
        }

        // ✅ Si el backend envía direccion / distrito (desde RENIEC), los usamos como sugerencia
        if (data.direccion && document.getElementById("direccion")) {
          document.getElementById("direccion").value = data.direccion;
        }
        if (data.distrito && document.getElementById("distrito")) {
          document.getElementById("distrito").value = data.distrito;
        }

        alertBox("success", `${data.tipo} encontrado: ${data.nombre || ""}`);
      } else {
        alertBox("warning", data.message || "No se encontró el documento");
        // Permitir ingreso manual
        document.getElementById("nombreBodeguero").focus();
      }
    } catch (e) {
      alertBox("danger", "Error al consultar la API: " + e.message);
    }
  }

  /**
   * ========== AUXILIAR: FETCH LISTA DE BODEGUEROS ==========
   */
  async function fetchBodeguerosData() {
      const res = await fetch("/api/usuarios/bodegueros", {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "same-origin"
      });
      if (!res.ok) throw new Error("No se pudieron cargar los bodegueros");
      return await res.json();
  }

  /**
   * ========== CARGAR BODEGUEROS (MODO CREACIÓN) ==========
   */
  async function cargarBodegueros() {
    const select = document.getElementById("bodegueroExistenteId");
    if (!select) return;

    try {
      const bodegueros = await fetchBodeguerosData();
      
      select.innerHTML = '<option value="">Seleccione un bodeguero...</option>';
      if (!bodegueros || bodegueros.length === 0) {
        select.innerHTML += '<option value="">No hay bodegueros disponibles</option>';
        return;
      }

      bodegueros.forEach((b) => {
        const option = document.createElement("option");
        option.value = b.id;
        option.textContent = `${b.nombre} - ${b.email}${b.numeroDocumento ? " (" + b.numeroDocumento + ")" : ""}`;
        select.appendChild(option);
      });
    } catch (e) {
      console.error(e);
      select.innerHTML = '<option value="">Error al cargar</option>';
    }
  }

  /**
   * ========== CARGAR BODEGUEROS (MODO EDICIÓN) ==========
   * ✅ NUEVA FUNCIÓN: Llena el select de edición y pre-selecciona el dueño actual
   */
 async function cargarBodeguerosParaEdicion(seleccionadoId = null) {
      const select = document.getElementById("bodegueroEditorSelect");
      if (!select) return;

      select.innerHTML = '<option value="">Cargando...</option>';
      
      try {
          // 1. Cargar lista general de bodegueros
          const bodegueros = await fetchBodeguerosData();
          let encontrado = false;
          
          select.innerHTML = '<option value="">Sin asignar / No cambiar</option>';
          
          bodegueros.forEach(b => {
              const option = document.createElement("option");
              option.value = b.id;
              option.textContent = `${b.nombre} (${b.email})`;
              
              // Verificar si este es el seleccionado
              if (seleccionadoId && b.id === Number(seleccionadoId)) {
                  option.selected = true;
                  encontrado = true;
              }
              select.appendChild(option);
          });

          // 2. 🚨 FIX: Si hay un ID seleccionado pero no estaba en la lista (ej: es Admin),
          // lo buscamos individualmente y lo agregamos.
          if (seleccionadoId && !encontrado) {
              try {
                  const resUser = await fetch(`/api/usuarios/${seleccionadoId}`, {
                      headers: getAuthHeaders(),
                      credentials: "same-origin"
                  });
                  if (resUser.ok) {
                      const user = await resUser.json();
                      const option = document.createElement("option");
                      option.value = user.id;
                      option.textContent = `${user.nombre} (${user.email}) - [Actual]`;
                      option.selected = true;
                      option.style.fontWeight = "bold";
                      // Lo insertamos al principio (después del placeholder)
                      select.insertBefore(option, select.children[1]);
                  }
              } catch (err) {
                  console.warn("No se pudo cargar el usuario dueño original", err);
              }
          }

      } catch (e) {
          console.error(e);
          select.innerHTML = '<option value="">Error al cargar lista</option>';
      }
  }

  /**
   * ========== CARGAR BODEGAS CON DATATABLES ==========
   */
  async function loadBodegas() {
    const tbody = document.querySelector("#tablaBodegas tbody");
    if (!tbody) return;

    const tablaExistente = dataTable !== null;

    try {
      tbody.innerHTML = `
        <tr>
          <td colspan="8" class="text-center text-muted py-4">
            <i class="bi bi-arrow-clockwise me-2"></i>Cargando bodegas...
          </td>
        </tr>`;

      const res = await fetch("/api/bodegas", {
        headers: getAuthHeaders(),
        credentials: "same-origin"
      });

      if (!res.ok) throw new Error("No se pudo cargar la lista de bodegas");

      const data = await res.json();
      const bodegas = data.data || data;

      if (dataTable) {
        dataTable.destroy();
        dataTable = null;
      }

      tbody.innerHTML = "";

      if (!bodegas || bodegas.length === 0) {
        tbody.innerHTML = `
          <tr>
            <td colspan="8" class="text-center text-muted py-4">
              <i class="bi bi-inbox fs-1 d-block mb-2"></i>
              No hay bodegas registradas
            </td>
          </tr>`;
        return;
      }

      bodegas.forEach((b) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${b.id ?? ""}</td>
          <td><strong>${b.nombre ?? ""}</strong></td>
          <td><small>${b.direccion ?? "-"}</small></td>
          <td>${b.distrito ?? "-"}</td>
          <td>${b.telefono ?? "-"}</td>
          <td><span class="badge bg-secondary">${b.bodeguerosAsignados ?? 0}</span></td>
          <td>${getEstadoBadge(b.activo)}</td>
          <td class="table-actions">
            <button class="btn btn-sm btn-outline-info btn-action" onclick="verDetalle(${b.id})" title="Ver detalle">
              <i class="bi bi-eye"></i>
            </button>
            <button class="btn btn-sm btn-outline-primary btn-action" onclick="openEdit(${b.id})" title="Editar">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button class="btn btn-sm btn-outline-danger btn-action" onclick="deleteBodega(${b.id})" title="Eliminar">
              <i class="bi bi-trash"></i>
            </button>
          </td>`;
        tbody.appendChild(tr);
      });

      await new Promise((resolve) => setTimeout(resolve, 100));

      dataTable = new DataTable("#tablaBodegas", {
        language: { url: "https://cdn.datatables.net/plug-ins/1.13.7/i18n/es-ES.json" },
        pageLength: 10,
        order: [[0, "desc"]],
        columnDefs: [{ orderable: false, targets: 7 }],
        responsive: true,
        retrieve: true,
      });

    } catch (e) {
      console.error("Error al cargar bodegas:", e);
      if (tablaExistente && dataTable) {
        alertBox("danger", `Error al recargar: ${e.message}`);
        return;
      }
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${e.message}</td></tr>`;
    }
  }

  /**
   * ========== VER DETALLE ==========
   */
  async function verDetalle(id) {
    try {
      const contenido = document.getElementById("detalleContenido");
      if (!contenido) return;

      contenido.innerHTML = `
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2 text-muted">Cargando información...</p>
        </div>`;

      if (detalleModal) detalleModal.show();

      const res = await fetch(`/api/bodegas/${id}/detalle`, {
        headers: getAuthHeaders(),
        credentials: "same-origin"
      });

      if (!res.ok) throw new Error("No se pudo obtener el detalle");

      const response = await res.json();
      const { bodega, bodegueros, estadisticas } = response.data;

      contenido.innerHTML = `
        <div class="row g-4">
          <div class="col-md-6">
            <h6 class="fw-bold text-primary mb-3"><i class="bi bi-shop me-2"></i> Información</h6>
            <table class="table table-sm table-bordered">
              <tr><th width="40%">Nombre:</th><td>${bodega.nombre || "-"}</td></tr>
              <tr><th>Dirección:</th><td>${bodega.direccion || "-"}</td></tr>
              <tr><th>Distrito:</th><td>${bodega.distrito || "-"}</td></tr>
              <tr><th>Teléfono:</th><td>${bodega.telefono || "-"}</td></tr>
              <tr><th>Horario:</th><td>${bodega.horario || "-"}</td></tr>
              <tr><th>Estado:</th><td>${getEstadoBadge(bodega.activo)}</td></tr>
            </table>
          </div>
          <div class="col-md-6">
            <h6 class="fw-bold text-primary mb-3"><i class="bi bi-people me-2"></i> Bodegueros</h6>
            ${bodegueros && bodegueros.length > 0 ? 
              `<ul class="list-group">${bodegueros.map(b => 
                `<li class="list-group-item"><strong>${b.nombre}</strong><br><small class="text-muted">${b.email}</small></li>`
              ).join("")}</ul>` : 
              '<p class="text-muted">Sin bodegueros asignados</p>'}
          </div>
          <div class="col-12">
             <h6 class="fw-bold text-primary mb-3"><i class="bi bi-graph-up me-2"></i> Estadísticas</h6>
             <div class="row text-center g-3">
               <div class="col-md-6"><div class="card bg-light border-0"><div class="card-body"><h3 class="mb-0 text-primary">${estadisticas?.totalProductos || 0}</h3><small>Productos</small></div></div></div>
               <div class="col-md-6"><div class="card bg-light border-0"><div class="card-body"><h3 class="mb-0 text-success">${estadisticas?.totalPedidos || 0}</h3><small>Pedidos</small></div></div></div>
             </div>
          </div>
        </div>`;

    } catch (e) {
      const contenido = document.getElementById("detalleContenido");
      if(contenido) contenido.innerHTML = `<div class="text-center text-danger py-5">${e.message}</div>`;
    }
  }

  /**
   * ========== BUSCAR DIRECCIÓN (Google Maps) ==========
   */
  async function buscarDireccionEnMapa() {
    const direccionInput = document.getElementById("direccion");
    if (!direccionInput) return;
    let txt = direccionInput.value.trim();
    if (!txt) { alertBox("warning", "Ingresa una dirección"); return; }

    try {
      alertBox("info", "Buscando ubicación...");
      const res = await fetch(`/api/google-maps/geocode?direccion=${encodeURIComponent(txt)}`);
      const data = await res.json();

      if (!data.success) { alertBox("warning", "No se encontró la dirección."); return; }

      const lat = parseFloat(data.lat);
      const lon = parseFloat(data.lon);

      document.getElementById("latitud").value = lat.toFixed(6);
      document.getElementById("longitud").value = lon.toFixed(6);

      if (map && marker) {
        const newPos = { lat: lat, lng: lon };
        map.setCenter(newPos);
        map.setZoom(17);
        marker.setPosition(newPos);
      }
      if (data.distrito) document.getElementById("distrito").value = data.distrito;
      alertBox("success", "Ubicación encontrada.");
    } catch (e) {
      alertBox("danger", "Error al buscar: " + e.message);
    }
  }

  function irADistrito() {
    const select = document.getElementById("distritoMapa");
    if (!select || !select.value) return;
    const [lat, lon] = select.value.split(",").map(parseFloat);

    if (map && marker) {
      const newPos = { lat: lat, lng: lon };
      map.setCenter(newPos);
      map.setZoom(15);
      marker.setPosition(newPos);
      document.getElementById("latitud").value = lat.toFixed(6);
      document.getElementById("longitud").value = lon.toFixed(6);
    }
  }

  async function geocodificarInverso(lat, lon) {
    try {
      const res = await fetch(`/api/google-maps/reverse?lat=${lat}&lon=${lon}`);
      const data = await res.json();
      if (data.success && data.display_name) {
        const direccionInput = document.getElementById("direccion");
        if (direccionInput) direccionInput.value = data.display_name;
        if (data.distrito) document.getElementById("distrito").value = data.distrito;
      }
    } catch (e) {
      console.warn("No se pudo obtener dirección automática:", e);
    }
  }

  /**
   * ========== INICIALIZAR MAPA ==========
   */
  function initMap(lat = -6.7714, lng = -79.8409) {
    const mapContainer = document.getElementById("map");
    if (!mapContainer) return;

    if (map) {
      const newPos = { lat: lat, lng: lng };
      map.setCenter(newPos);
      if (marker) marker.setPosition(newPos);
      return;
    }

    // @ts-ignore
    map = new google.maps.Map(mapContainer, {
      center: { lat: lat, lng: lng },
      zoom: 14,
      mapTypeControl: false,
      streetViewControl: false,
    });

    // @ts-ignore
    marker = new google.maps.Marker({
      position: { lat: lat, lng: lng },
      map: map,
      draggable: true,
      title: "Ubicación"
    });

    map.addListener("click", function (e) {
      marker.setPosition(e.latLng);
      document.getElementById("latitud").value = e.latLng.lat().toFixed(6);
      document.getElementById("longitud").value = e.latLng.lng().toFixed(6);
      geocodificarInverso(e.latLng.lat(), e.latLng.lng());
    });

    marker.addListener("dragend", function (e) {
      document.getElementById("latitud").value = e.latLng.lat().toFixed(6);
      document.getElementById("longitud").value = e.latLng.lng().toFixed(6);
      geocodificarInverso(e.latLng.lat(), e.latLng.lng());
    });

    document.getElementById("latitud").value = lat.toFixed(6);
    document.getElementById("longitud").value = lng.toFixed(6);
  }

  /**
   * ========== ABRIR MODAL CREAR (ACTUALIZADO) ==========
   */
  function openCreate() {
    isEdit = false;
    document.getElementById("bodegaModalTitle").innerHTML = '<i class="bi bi-shop-window me-2"></i> Nueva Bodega';
    
    const form = document.querySelector("#bodegaForm");
    if (form) form.reset();
    
    document.getElementById("isEdit").value = "false";
    document.getElementById("activo").checked = true;

    // ✅ GESTIÓN DE VISIBILIDAD: MODO CREAR
    // Mostrar sección de radio buttons y creación de usuario
    document.getElementById("seccionAsignarBodeguero").style.display = "block";
    
    // Ocultar sección de edición de dueño
    const divEditOwner = document.getElementById("seccionEditarOwner");
    if (divEditOwner) divEditOwner.style.display = "none";

    // Resets internos de radio buttons
    document.getElementById("radioExistente").checked = true;
    document.getElementById("seccionBodegueroExistente").style.display = "block";
    document.getElementById("seccionBodegueroNuevo").style.display = "none";

    cargarBodegueros(); // Carga select de creación
    if (modal) modal.show();
    setTimeout(() => initMap(-6.7714, -79.8409), 300);
  }

  /**
   * ========== ABRIR MODAL EDITAR (ACTUALIZADO) ==========
   */
  async function openEdit(id) {
    try {
      isEdit = true;
      const res = await fetch(`/api/bodegas/${id}`, {
        headers: getAuthHeaders(),
        credentials: "same-origin"
      });

      if (!res.ok) throw new Error("No se pudo obtener la bodega");

      const data = await res.json();
      const b = data.data || data;

      document.getElementById("bodegaModalTitle").innerHTML = `<i class="bi bi-pencil-square me-2"></i> Editar Bodega #${id}`;
      document.getElementById("id").value = b.id ?? "";
      document.getElementById("isEdit").value = "true";
      document.getElementById("nombre").value = b.nombre ?? "";
      document.getElementById("telefonoBodega").value = b.telefono ?? "";
      document.getElementById("direccion").value = b.direccion ?? "";
      document.getElementById("distrito").value = b.distrito ?? "";
      document.getElementById("horario").value = b.horario ?? "";
      document.getElementById("activo").checked = !!b.activo;

      // ✅ GESTIÓN DE VISIBILIDAD: MODO EDITAR
      // Ocultar toda la sección de creación de usuarios/radio buttons
      const seccionAsignar = document.getElementById("seccionAsignarBodeguero");
      if (seccionAsignar) seccionAsignar.style.display = "none";

      // Mostrar la sección simple de cambio de dueño
      const divEditOwner = document.getElementById("seccionEditarOwner");
      if (divEditOwner) {
          divEditOwner.style.display = "block";
          // Cargar usuarios en el select de edición, preseleccionando al actual
          await cargarBodeguerosParaEdicion(b.usuarioId);
      }

      modal.show();
      setTimeout(() => initMap(b.latitud ?? -12.0464, b.longitud ?? -77.0428), 300);
    } catch (e) {
      alertBox("danger", e.message);
    }
  }

  async function submitForm(e) {
    e.preventDefault();
    const esEdicion = document.getElementById("isEdit").value === "true";
    if (esEdicion) await submitEdicion();
    else await submitCreacion();
  }

  async function submitCreacion() {
    const radioChecked = document.querySelector('input[name="tipoBodeguero"]:checked');
    if (!radioChecked) { alertBox("warning", "Selecciona un tipo de bodeguero"); return; }
    
    const esNuevo = radioChecked.value === "nuevo";
    const payload = {
      esNuevoBodeguero: esNuevo,
      bodegueroNuevo: null,
      bodegueroExistenteId: null,
      bodega: {
        nombre: document.getElementById("nombre").value.trim(),
        direccion: document.getElementById("direccion").value.trim(),
        latitud: parseFloat(document.getElementById("latitud").value || -6.7714),
        longitud: parseFloat(document.getElementById("longitud").value || -79.8409),
        telefono: document.getElementById("telefonoBodega").value.trim(),
        distrito: document.getElementById("distrito").value.trim(),
        horario: document.getElementById("horario").value.trim(),
        activo: document.getElementById("activo").checked,
      },
    };

    if (!payload.bodega.nombre || !payload.bodega.direccion) {
      alertBox("warning", "Nombre y dirección son obligatorios"); return;
    }

    if (esNuevo) {
      // (Lógica de validación de nuevo bodeguero se mantiene igual...)
      const nombreBodeguero = document.getElementById("nombreBodeguero");
      // ... capturar resto de inputs ...
      payload.bodegueroNuevo = {
          nombre: nombreBodeguero ? nombreBodeguero.value.trim() : "",
          email: document.getElementById("emailBodeguero").value.trim(),
          password: document.getElementById("passwordBodeguero").value,
          telefono: document.getElementById("telefonoBodeguero").value.trim(),
          numeroDocumento: document.getElementById("numeroDocumento").value.trim(),
          rol: "BODEGUERO",
          activo: true
      };
      if (!payload.bodegueroNuevo.nombre || !payload.bodegueroNuevo.email || !payload.bodegueroNuevo.password) {
          alertBox("warning", "Datos del bodeguero incompletos"); return;
      }
    } else {
      const sel = document.getElementById("bodegueroExistenteId");
      payload.bodegueroExistenteId = sel ? parseInt(sel.value) : null;
      if (!payload.bodegueroExistenteId) { alertBox("warning", "Seleccione un bodeguero"); return; }
    }

    try {
      const res = await fetch("/api/bodegas/crear-con-bodeguero", {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "same-origin",
        body: JSON.stringify(payload),
      });
      const body = await res.json();
      if (!res.ok || body.success === false) throw new Error(body.message);
      
      alertBox("success", "Bodega creada correctamente");
      modal.hide();
      setTimeout(() => loadBodegas(), 300);
    } catch (err) {
      alertBox("danger", err.message);
    }
  }

  /**
   * ========== SUBMIT EDICIÓN (ACTUALIZADO) ==========
   */
  async function submitEdicion() {
    const id = document.getElementById("id").value;
    
    // ✅ Capturar el nuevo dueño desde el select de edición
    const bodegueroSelect = document.getElementById("bodegueroEditorSelect");
    const usuarioId = bodegueroSelect && bodegueroSelect.value ? parseInt(bodegueroSelect.value) : null;

    const payload = {
      nombre: document.getElementById("nombre").value.trim(),
      direccion: document.getElementById("direccion").value.trim(),
      latitud: parseFloat(document.getElementById("latitud").value || -6.7714),
      longitud: parseFloat(document.getElementById("longitud").value || -79.8409),
      telefono: document.getElementById("telefonoBodega").value.trim(),
      distrito: document.getElementById("distrito").value.trim(),
      horario: document.getElementById("horario").value.trim(),
      activo: document.getElementById("activo").checked,
      // ✅ ENVIAR USUARIO ID AL BACKEND
      usuarioId: usuarioId
    };

    if (!payload.nombre || !payload.direccion) {
      alertBox("warning", "Nombre y dirección son obligatorios"); return;
    }

    try {
      const res = await fetch(`/api/bodegas/${id}`, {
        method: "PUT",
        headers: getAuthHeaders(),
        credentials: "same-origin",
        body: JSON.stringify(payload),
      });

      const body = await res.json();
      if (!res.ok || body.success === false) throw new Error(body.message);

      alertBox("success", "Bodega actualizada correctamente");
      modal.hide();
      setTimeout(() => loadBodegas(), 300);
    } catch (err) {
      alertBox("danger", err.message);
    }
  }

  async function deleteBodega(id) {
    if (!confirm(`¿Eliminar bodega #${id}?`)) return;
    try {
      const res = await fetch(`/api/bodegas/${id}`, {
        method: "DELETE",
        headers: getAuthHeaders(),
        credentials: "same-origin"
      });
      const body = await res.json();
      if (!res.ok || body.success === false) throw new Error(body.message);
      alertBox("success", "Bodega eliminada");
      loadBodegas();
    } catch (e) {
      alertBox("danger", e.message);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    const modalEl = document.getElementById("bodegaModal");
    if (modalEl && window.bootstrap) modal = new bootstrap.Modal(modalEl);

    const detalleModalEl = document.getElementById("detalleModal");
    if (detalleModalEl && window.bootstrap) detalleModal = new bootstrap.Modal(detalleModalEl);

    const form = document.getElementById("bodegaForm");
    if (form) form.addEventListener("submit", submitForm);

    document.querySelectorAll('input[name="tipoBodeguero"]').forEach((radio) => {
      radio.addEventListener("change", function () {
        if (this.value === "nuevo") {
          document.getElementById("seccionBodegueroExistente").style.display = "none";
          document.getElementById("seccionBodegueroNuevo").style.display = "block";
        } else {
          document.getElementById("seccionBodegueroExistente").style.display = "block";
          document.getElementById("seccionBodegueroNuevo").style.display = "none";
        }
      });
    });

    loadBodegas();
    console.log("✅ Bodegas JS cargado");
  });

  window.openCreate = openCreate;
  window.openEdit = openEdit;
  window.deleteBodega = deleteBodega;
  window.consultarDocumento = consultarDocumento;
  window.buscarDireccionEnMapa = buscarDireccionEnMapa;
  window.irADistrito = irADistrito;
  window.verDetalle = verDetalle;
})();