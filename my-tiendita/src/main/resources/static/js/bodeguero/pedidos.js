/**
 * LÓGICA DE PEDIDOS:
 * 1. Confirmación de atención (FIFO)
 * 2. Carrito de Pedido Manual
 */

document.addEventListener("DOMContentLoaded", () => {
    // Detectar mensajes Flash
    const successMsg = document.getElementById('flashSuccess')?.value;
    const errorMsg = document.getElementById('flashError')?.value;

    if (successMsg) {
        Swal.fire({
            icon: 'success',
            title: '¡Listo!',
            text: successMsg,
            timer: 2000,
            showConfirmButton: false
        });
    }

    if (errorMsg) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: errorMsg
        });
    }
});

/**
 * Confirmar atención del pedido siguiente (FIFO)
 */
function confirmarAtencion(event) {
    event.preventDefault(); 
    const form = event.target.closest('form');
    
    const idPedido = document.getElementById('lblPedidoId')?.innerText || '';
    const total = document.getElementById('lblPedidoTotal')?.innerText || '';

    Swal.fire({
        title: '¿Atender el Pedido #' + idPedido + '?',
        html: `
            <div class="text-start">
                <p class="mb-1">Esto realizará las siguientes acciones:</p>
                <ul class="text-muted small mb-0" style="list-style-position: inside;">
                    <li>Descontará el stock del inventario.</li>
                    <li>Registrará la venta en caja por <b>S/ ${total}</b>.</li>
                    <li>Marcará el pedido como "EN PREPARACIÓN".</li>
                </ul>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#10B981',
        cancelButtonColor: '#64748b',
        confirmButtonText: '<i class="bi bi-check-lg"></i> Sí, Atender',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            Swal.fire({
                title: 'Procesando...',
                text: 'Generando venta y actualizando stock',
                allowOutsideClick: false,
                didOpen: () => Swal.showLoading()
            });
            form.submit();
        }
    });
}

// ==========================================
// LÓGICA PEDIDO MANUAL (CARRITO)
// ==========================================
let itemsManual = [];

function agregarItemManual() {
    const select = document.getElementById('manualProducto');
    const option = select.options[select.selectedIndex];
    const cantidadInput = document.getElementById('manualCantidad');
    const cantidad = parseInt(cantidadInput.value);
    
    if(!select.value) {
        Swal.fire({toast: true, position: 'top-end', icon: 'warning', title: 'Selecciona un producto', showConfirmButton: false, timer: 1500});
        return;
    }

    const id = select.value;
    const nombre = option.getAttribute('data-nombre');
    const precio = parseFloat(option.getAttribute('data-precio'));

    // Verificar si ya existe
    const existe = itemsManual.find(i => i.id === id);
    if(existe) {
        existe.cantidad += cantidad;
    } else {
        itemsManual.push({ id, nombre, precio, cantidad });
    }
    
    // Reset inputs
    select.value = "";
    cantidadInput.value = 1;
    
    renderTablaManual();
}

function renderTablaManual() {
    const tbody = document.getElementById('tablaManualItems');
    
    if (itemsManual.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3 small">Agrega productos a la lista</td></tr>';
        document.getElementById('manualTotal').innerText = '0.00';
        return;
    }

    let html = '';
    let total = 0;
    
    itemsManual.forEach((item, idx) => {
        const sub = item.cantidad * item.precio;
        total += sub;
        html += `
            <tr>
                <td>${item.nombre}</td>
                <td>${item.cantidad}</td>
                <td class="text-end">S/ ${sub.toFixed(2)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-link text-danger p-0" onclick="borrarItemManual(${idx})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
    document.getElementById('manualTotal').innerText = total.toFixed(2);
}

function borrarItemManual(idx) {
    itemsManual.splice(idx, 1);
    renderTablaManual();
}

function guardarPedidoManual() {
    const clienteId = document.getElementById('manualCliente').value;
    if(!clienteId || itemsManual.length === 0) {
        return Swal.fire("Faltan datos", "Selecciona un cliente y agrega al menos un producto.", "warning");
    }

    const payload = {
        bodegaId: BODEGA_ID, // Variable global del HTML
        usuarioId: parseInt(clienteId),
        direccionEntrega: document.getElementById('manualDireccion').value,
        productos: itemsManual.map(i => ({ productoBodegaId: i.id, cantidad: i.cantidad }))
    };

    // Loading
    Swal.fire({title: 'Guardando...', didOpen: () => Swal.showLoading()});

    fetch('/bodeguero/pedidos/manual', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    }).then(async res => {
        if(res.ok) {
            Swal.fire("¡Éxito!", "Pedido agregado a la cola correctamente.", "success")
                .then(() => location.reload());
        } else {
            const err = await res.json();
            Swal.fire("Error", err.error || "No se pudo guardar el pedido.", "error");
        }
    }).catch(e => {
        console.error(e);
        Swal.fire("Error", "Fallo de conexión", "error");
    });
}