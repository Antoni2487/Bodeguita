document.addEventListener('DOMContentLoaded', async function() {
    cargarCarrito();
});

let carrito = [];
let bodegaIdActual = null;
let mapa = null;
let marker = null;
let posicionCliente = { lat: -12.0464, lng: -77.0428 }; // Default
let checkoutData = null; // Aquí guardaremos la respuesta del servidor (totales, métodos pago)
let metodoPagoSeleccionadoData = null;

// 1. INICIALIZACIÓN
async function cargarCarrito() {
    carrito = JSON.parse(localStorage.getItem('carrito')) || [];
    
    if (carrito.length === 0) {
        document.getElementById('empty-cart').classList.remove('d-none');
        document.getElementById('cart-content').classList.add('d-none');
        return;
    }

    document.getElementById('cart-content').classList.remove('d-none');
    
    // Asumimos que todos los items son de la misma bodega (validación al agregar)
    bodegaIdActual = carrito[0].bodegaId; 
    document.getElementById('bodega-nombre').innerText = "Bodega: " + (carrito[0].bodegaNombre || "Seleccionada");

    renderizarItems();
    initMap(); // El mapa disparará la primera validación de checkout
}

// 2. RENDERIZADO DE ITEMS (TABLA)
function renderizarItems() {
    const container = document.getElementById('cart-items-container');
    container.innerHTML = '';

    carrito.forEach((item, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="width: 80px;">
                <img src="${item.imagen || '/images/placeholder.png'}" class="cart-item-img" style="width:60px; height:60px; object-fit:cover; border-radius:8px;">
            </td>
            <td>
                <h6 class="mb-0 small fw-bold">${item.nombre}</h6>
                <small class="text-muted">S/ ${item.precio.toFixed(2)}</small>
            </td>
            <td>
                <div class="input-group input-group-sm" style="width: 100px;">
                    <button class="btn btn-outline-secondary" onclick="cambiarCantidad(${index}, -1)">-</button>
                    <input type="text" class="form-control text-center p-0" value="${item.cantidad}" readonly>
                    <button class="btn btn-outline-secondary" onclick="cambiarCantidad(${index}, 1)">+</button>
                </div>
            </td>
            <td class="text-end fw-bold small">
                S/ ${(item.precio * item.cantidad).toFixed(2)}
            </td>
            <td class="text-end">
                <button class="btn btn-sm text-danger" onclick="eliminarItem(${index})"><i class="bi bi-trash"></i></button>
            </td>
        `;
        container.appendChild(tr);
    });
}

// 3. MAPA Y GEOLOCALIZACIÓN
function initMap() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (pos) => { setupMap(pos.coords.latitude, pos.coords.longitude); },
            () => { setupMap(posicionCliente.lat, posicionCliente.lng); }
        );
    } else {
        setupMap(posicionCliente.lat, posicionCliente.lng);
    }
}

function setupMap(lat, lng) {
    posicionCliente = { lat, lng };
    
    if(!mapa) {
        mapa = L.map('mapaEntrega').setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(mapa);
        
        marker = L.marker([lat, lng], {draggable: true}).addTo(mapa);
        
        // Al soltar el pin, consultamos al backend
        marker.on('dragend', function(e) {
            const pos = e.target.getLatLng();
            posicionCliente = { lat: pos.lat, lng: pos.lng };
            consultarPreCheckout(); // 🔥 LLAMADA AL SERVIDOR
        });
    } else {
        mapa.setView([lat, lng], 15);
        marker.setLatLng([lat, lng]);
    }
    
    // Primera consulta automática
    consultarPreCheckout();
}

// 4. CONSULTA AL BACKEND (/pre-checkout)
// Esta es la función CLAVE que reemplaza tus cálculos locales
async function consultarPreCheckout() {
    const btnCheckout = document.getElementById('btn-checkout');
    const msgDiv = document.getElementById('checkout-msg');
    const loadingMethods = document.getElementById('payment-methods');
    
    // Estado de carga
    document.getElementById('summary-delivery').innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    btnCheckout.disabled = true;
    msgDiv.innerText = '';

    // Armamos el DTO Request
    const requestDTO = {
        bodegaId: bodegaIdActual,
        latitud: posicionCliente.lat,
        longitud: posicionCliente.lng,
        productos: carrito.map(i => ({
            productoBodegaId: i.id, // Asegúrate que 'id' sea el ID de ProductoBodega
            cantidad: i.cantidad
        }))
    };

    try {
        const res = await fetch('/api/cliente/pedidos/pre-checkout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
                // 'X-CSRF-TOKEN': ... si usas CSRF
            },
            body: JSON.stringify(requestDTO)
        });

        if (!res.ok) throw new Error("Error de conexión");

        const data = await res.json();
        checkoutData = data; // Guardamos respuesta globalmente

        const subtotal = data.subtotal || 0;
        const delivery = data.costoDelivery || 0;
        const total = data.total || 0;

        // A) Actualizar Totales
        document.getElementById('summary-subtotal').innerText = data.subtotal.toFixed(2);
        document.getElementById('summary-delivery').innerText = data.costoDelivery.toFixed(2);
        document.getElementById('summary-total').innerText = data.total.toFixed(2);

        // B) Validar si es posible el delivery
        if (data.posible) {
            btnCheckout.disabled = false;
            msgDiv.className = "mt-2 text-center text-success small";
            msgDiv.innerText = `✅ ${data.mensaje} (Distancia: ${data.distanciaKm} km)`;
            
            // C) Renderizar Métodos de Pago dinámicamente
            renderizarMetodosPago(data.metodosPago);
        } else {
            btnCheckout.disabled = true;
            msgDiv.className = "mt-2 text-center text-danger small fw-bold";
            msgDiv.innerText = `❌ ${data.mensaje}`;
            loadingMethods.innerHTML = '<div class="alert alert-warning small">No disponible para esta ubicación.</div>';
        }

    } catch (error) {
        console.error(error);
        msgDiv.innerText = "Error calculando delivery. Intente nuevamente.";
    }
}

// 5. RENDERIZAR MÉTODOS DE PAGO
function renderizarMetodosPago(metodos) {
    const container = document.getElementById('payment-methods');
    container.innerHTML = '';

    if(!metodos || metodos.length === 0) {
        container.innerHTML = '<div class="alert alert-info small">Sin métodos de pago configurados.</div>';
        return;
    }

    metodos.forEach(m => {
        // Icono según el nombre (lógica visual simple)
        let icono = 'bi-cash-coin';
        let color = 'text-success';
        const nombre = (m.tipoMetodoPagoNombre || "").toUpperCase();
        
        if(nombre.includes('YAPE') || nombre.includes('PLIN')) {
            icono = 'bi-qr-code';
            color = 'text-primary';
        } else if (nombre.includes('TARJETA')) {
            icono = 'bi-credit-card';
            color = 'text-warning';
        }

        const div = document.createElement('div');
        div.className = `border rounded p-3 mb-2 payment-option cursor-pointer`;
        div.style.cursor = 'pointer';
        div.onclick = () => seleccionarPago(div, m.id); // Guardamos el ID del metodo de pago
        div.innerHTML = `
            <div class="d-flex align-items-center">
                <i class="bi ${icono} fs-4 me-3 ${color}"></i>
                <div>
                    <strong>${m.tipoMetodoPagoNombre}</strong><br>
                    <small class="text-muted">${m.nombreTitular || ''} ${m.numeroTelefono || ''}</small>
                </div>
            </div>
        `;
        container.appendChild(div);
    });
}

let metodoPagoSeleccionadoId = null;

function seleccionarPago(element, id) {
    document.querySelectorAll('.payment-option').forEach(el => {
        el.classList.remove('border-primary', 'bg-light');
    });
    element.classList.add('border-primary', 'bg-light');
    metodoPagoSeleccionadoId = id;
}

// 6. PROCESAR PEDIDO FINAL
async function procesarPedido() {
    const direccion = document.getElementById('direccionEntrega').value;
    const telefono = document.getElementById('telefonoContacto').value;

    if (!direccion || !telefono) {
        alert("Por favor completa la dirección exacta y un teléfono de contacto.");
        return;
    }

    if (!metodoPagoSeleccionadoId) {
        alert("Selecciona un método de pago.");
        return;
    }

    // Armar PedidoDTO final
    const pedidoDTO = {
        bodega: bodegaIdActual,
        direccionEntrega: direccion,
        telefonoContacto: telefono,
        // Datos importantes para el backend
        latitudEntrega: posicionCliente.lat, // Nuevo campo en DTO
        longitudEntrega: posicionCliente.lng, // Nuevo campo en DTO
        bodegaMetodoPagoId: metodoPagoSeleccionadoId, // Nuevo campo en DTO
        detalles: carrito.map(item => ({
            productoBodegaId: item.id,
            cantidad: item.cantidad
        }))
    };

    const btn = document.getElementById('btn-checkout');
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Procesando...';
    btn.disabled = true;

    try {
        const res = await fetch('/api/cliente/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pedidoDTO)
        });

        if (res.ok) {
            localStorage.removeItem('carrito');
            // Redirección exitosa
            window.location.href = '/cliente/mis-pedidos?exito=true'; 
        } else {
            // Manejo de errores 403, 400, etc.
            if(res.status === 403) alert("Tu sesión ha expirado. Inicia sesión nuevamente.");
            else alert("Error al procesar el pedido.");
            
            btn.disabled = false;
            btn.innerHTML = 'REALIZAR PEDIDO';
        }
    } catch (e) {
        console.error(e);
        alert("Error de conexión");
        btn.disabled = false;
        btn.innerHTML = 'REALIZAR PEDIDO';
    }
}

// Funciones auxiliares de items
function cambiarCantidad(index, delta) {
    const item = carrito[index];
    const nueva = item.cantidad + delta;
    if(nueva > 0) { // Falta validar stock máximo (idealmente viene del backend)
        item.cantidad = nueva;
        localStorage.setItem('carrito', JSON.stringify(carrito));
        renderizarItems();
        consultarPreCheckout(); // Recalcular todo
    }
}

function eliminarItem(index) {
    carrito.splice(index, 1);
    localStorage.setItem('carrito', JSON.stringify(carrito));
    if(carrito.length === 0) location.reload();
    else {
        renderizarItems();
        consultarPreCheckout();
    }
}
function seleccionarPago(element, id) {
    document.querySelectorAll('.payment-option').forEach(el => {
        el.classList.remove('border-primary', 'bg-light');
    });
    element.classList.add('border-primary', 'bg-light');
    
    // Buscamos el objeto completo en la data que trajo el checkout
    // checkoutData es variable global que llenamos en consultarPreCheckout()
    if(checkoutData && checkoutData.metodosPago) {
        metodoPagoSeleccionadoData = checkoutData.metodosPago.find(m => m.id === id);
    }
}

// 2. NUEVA FUNCIÓN: Abrir Modal en lugar de enviar directo
function procesarPedido() {
    const direccion = document.getElementById('direccionEntrega').value;
    const telefono = document.getElementById('telefonoContacto').value;

    if (!direccion || !telefono) {
        alert("Por favor completa la dirección y teléfono.");
        return;
    }
    if (!metodoPagoSeleccionadoData) {
        alert("Selecciona un método de pago.");
        return;
    }

    // Configurar Modal según el método
    const nombreMetodo = (metodoPagoSeleccionadoData.tipoMetodoPagoNombre || "").toUpperCase();
    const total = document.getElementById('summary-total').innerText;
    document.getElementById('modal-total-pagar').innerText = total;

    const zonaQr = document.getElementById('zona-pago-qr');
    const zonaEfectivo = document.getElementById('zona-pago-efectivo');

    if (nombreMetodo.includes('EFECTIVO')) {
        zonaQr.classList.add('d-none');
        zonaEfectivo.classList.remove('d-none');
    } else {
        // Es Yape/Plin/Digital
        zonaEfectivo.classList.add('d-none');
        zonaQr.classList.remove('d-none');
        
        document.getElementById('nombre-app-pago').innerText = metodoPagoSeleccionadoData.tipoMetodoPagoNombre;
        document.getElementById('img-qr-pago').src = metodoPagoSeleccionadoData.imagenQrUrl || '/images/qr-placeholder.png'; // Fallback imagen
        document.getElementById('datos-titular').innerText = metodoPagoSeleccionadoData.nombreTitular || "";
        document.getElementById('datos-telefono').innerText = metodoPagoSeleccionadoData.numeroTelefono || "";
    }

    // Abrir Modal (Bootstrap 5)
    const modal = new bootstrap.Modal(document.getElementById('modalPago'));
    modal.show();
}

// 3. NUEVA FUNCIÓN: Envío real al backend (lo que antes era procesarPedido)
async function confirmarYEnviarPedido() {
    const btn = document.querySelector('#modalPago .btn-success'); // Botón del modal
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Enviando...';

    // Recuperamos datos del form principal
    const direccion = document.getElementById('direccionEntrega').value;
    const telefono = document.getElementById('telefonoContacto').value;

    const pedidoDTO = {
        bodega: bodegaIdActual,
        direccionEntrega: direccion,
        telefonoContacto: telefono,
        latitudEntrega: posicionCliente.lat,
        longitudEntrega: posicionCliente.lng,
        bodegaMetodoPagoId: metodoPagoSeleccionadoData.id,
        detalles: carrito.map(item => ({
            productoBodegaId: item.id,
            cantidad: item.cantidad
        }))
    };

    try {
        const res = await fetch('/api/cliente/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pedidoDTO)
        });

        // Intentar leer respuesta (sea éxito o error)
        let data;
        const text = await res.text();
        try { data = JSON.parse(text); } catch(e) { data = { message: text }; }

        if (res.ok) {
            localStorage.removeItem('carrito');
            // Cerrar modal
            const modalEl = document.getElementById('modalPago');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            modalInstance.hide();
            
            window.location.href = '/cliente/mis-pedidos?exito=true'; 
        } else {
            alert("Error: " + (data.message || data || "Error desconocido"));
        }
    } catch (e) {
        console.error(e);
        alert("Error de conexión con el servidor.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}