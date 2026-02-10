/* IMPORTANTE: 
   La variable 'BODEGA_ID' viene del HTML (Thymeleaf).
*/

let ticket = []; 

// --- 1. BUSCADOR DE PRODUCTOS (POS) ---
function filtrarPos() {
    const txt = document.getElementById('posSearch').value.toLowerCase();
    document.querySelectorAll('.item-pos').forEach(el => {
        const nombre = el.getAttribute('data-nombre');
        if(nombre) {
            el.style.display = nombre.includes(txt) ? 'block' : 'none';
        }
    });
}

// --- 2. AGREGAR AL TICKET ---
function agregarAlTicket(id, nombre, precio, stockMax) {
    const prodId = parseInt(id);
    const prodPrecio = parseFloat(precio);
    const prodStock = parseInt(stockMax);

    const existe = ticket.find(i => i.id === prodId);
    
    if (existe) {
        if(existe.cantidad >= prodStock) {
            return Swal.fire({
                toast: true, icon: 'warning', title: 'Stock máximo alcanzado', 
                position: 'top-end', showConfirmButton: false, timer: 1000
            });
        }
        existe.cantidad++;
    } else {
        if(prodStock < 1) {
            return Swal.fire({
                toast: true, icon: 'error', title: 'Producto sin stock', 
                position: 'top-end', showConfirmButton: false, timer: 1000
            });
        }
        ticket.push({ id: prodId, nombre: nombre, precio: prodPrecio, cantidad: 1 });
    }
    renderTicket();
}

// --- 3. DIBUJAR TICKET ---
function renderTicket() {
    const container = document.getElementById('ticketList');
    const totalEl = document.getElementById('ticketTotal');
    
    if (ticket.length === 0) {
        container.innerHTML = '<div class="text-center text-muted mt-5 opacity-50"><small>Agrega productos</small></div>';
        totalEl.innerText = '0.00';
        return;
    }

    let html = '';
    let total = 0;

    ticket.forEach((item, idx) => {
        const sub = item.cantidad * item.precio;
        total += sub;
        html += `
            <div class="d-flex justify-content-between align-items-center mb-2 p-2 bg-light rounded border border-light">
                <div style="overflow:hidden; flex:1;">
                    <div class="fw-bold small text-truncate">${item.nombre}</div>
                    <div class="text-muted" style="font-size:0.75rem;">${item.cantidad} x S/ ${item.precio.toFixed(2)}</div>
                </div>
                <div class="text-end ms-2">
                    <div class="fw-bold small">S/ ${sub.toFixed(2)}</div>
                    <i class="bi bi-trash text-danger" onclick="borrarItem(${idx})" style="cursor:pointer; font-size:0.8rem;"></i>
                </div>
            </div>`;
    });

    container.innerHTML = html;
    totalEl.innerText = total.toFixed(2);
}

function borrarItem(idx) {
    ticket.splice(idx, 1);
    renderTicket();
}

function limpiarTicket() {
    ticket = [];
    renderTicket();
}

// --- 4. COBRAR ---
function cobrar() {
    if (typeof BODEGA_ID === 'undefined') return Swal.fire("Error", "Recarga la página (ID Bodega no encontrado)", "error");
    if (ticket.length === 0) return Swal.fire("Ticket Vacío", "Agrega productos.", "warning");
    
    const metodoId = document.getElementById('posMetodoPago').value;
    if(!metodoId) return Swal.fire("Error", "Selecciona un método de pago.", "error");

    const clienteNombre = document.getElementById('posCliente').value;

    const request = {
        bodegaId: BODEGA_ID,
        productos: ticket.map(i => ({ productoBodegaId: i.id, cantidad: i.cantidad })),
        bodegaMetodoPagoId: parseInt(metodoId),
        tipoEntrega: 'RECOJO_EN_BODEGA',
        clienteNombre: clienteNombre || null
    };

    Swal.fire({title:'Procesando...', allowOutsideClick:false, didOpen:()=>{Swal.showLoading()}});

    fetch('/api/ventas', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(request)
    }).then(async res => {
        if(res.ok) {
            Swal.fire({icon:'success', title:'¡Venta Registrada!', showConfirmButton:false, timer:1500}).then(() => location.reload());
        } else {
            const err = await res.json();
            Swal.fire("Error", err.message || "Error al procesar", "error");
        }
    }).catch(e => Swal.fire("Error", "Fallo de conexión", "error"));
}

// --- 5. VER DETALLE ---
function verDetalle(idVenta) {
    Swal.fire({title: 'Cargando...', didOpen: () => Swal.showLoading()});

    fetch(`/api/ventas/${idVenta}`)
        .then(res => {
            if (!res.ok) throw new Error("Error al obtener datos");
            return res.json();
        })
        .then(data => {
            Swal.close();

            document.getElementById('detId').innerText = 'Ticket #' + data.id;
            document.getElementById('detFecha').innerText = data.fecha ? new Date(data.fecha).toLocaleString() : '--';
            document.getElementById('detCliente').innerText = data.clienteNombre || 'Público General';
            
            // ✅ CORRECCIÓN: Usamos nombreMetodoPago (o fallback)
            document.getElementById('detPago').innerText = data.nombreMetodoPago || data.tipoMetodoPago || 'Efectivo';
            
            document.getElementById('detTotal').innerText = data.monto.toFixed(2);

            // Manejar estado anulado
            const tagAnulado = document.getElementById('detAnuladoTag');
            if(data.estado === 'ANULADO') {
                tagAnulado.classList.remove('d-none');
            } else {
                tagAnulado.classList.add('d-none');
            }

            // Llenar tabla de productos
            const tbody = document.getElementById('detListaProductos');
            tbody.innerHTML = '';

            if(data.detalles && data.detalles.length > 0) {
                data.detalles.forEach(d => {
                    const subtotal = d.subtotal || (d.precioUnitario * d.cantidad);
                    tbody.innerHTML += `
                        <tr>
                            <td>${d.cantidad}</td>
                            <td>${d.nombreProducto}</td>
                            <td class="text-end">S/ ${subtotal.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">Sin detalles</td></tr>';
            }

            const modalEl = document.getElementById('modalDetalleVenta');
            const modal = new bootstrap.Modal(modalEl);
            modal.show();
        })
        .catch(err => {
            console.error(err);
            Swal.fire('Error', 'No se pudo cargar el detalle', 'error');
        });
}

// --- 6. IMPRIMIR TICKET ---
function imprimirTicket() {
    const contenido = document.getElementById('ticketContent').innerHTML;
    const ventana = window.open('', '', 'height=600,width=400');
    
    ventana.document.write('<html><head><title>Imprimir Ticket</title>');
    ventana.document.write('<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">');
    ventana.document.write('</head><body>');
    ventana.document.write(contenido);
    ventana.document.write('</body></html>');
    
    ventana.document.close();
    ventana.focus();
    
    setTimeout(() => { 
        ventana.print(); 
        ventana.close(); 
    }, 500);
}

// --- 7. ANULAR VENTA (NUEVO) ---
function anularVenta(id) {
    Swal.fire({
        title: '¿Anular esta venta?',
        text: "El stock de los productos será devuelto al inventario automáticamente.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, anular venta',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            Swal.showLoading();
            
            fetch(`/api/ventas/${id}/anular`, { method: 'POST' })
            .then(res => {
                if (res.ok) {
                    Swal.fire(
                        '¡Anulada!',
                        'La venta ha sido anulada y el stock repuesto.',
                        'success'
                    ).then(() => location.reload());
                } else {
                    Swal.fire('Error', 'No se pudo anular la venta.', 'error');
                }
            })
            .catch(err => {
                console.error(err);
                Swal.fire('Error', 'Fallo de conexión.', 'error');
            });
        }
    });
}