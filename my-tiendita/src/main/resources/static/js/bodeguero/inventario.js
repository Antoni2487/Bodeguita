const modalMovimiento = new bootstrap.Modal(document.getElementById('modalMovimiento'));
const modalKardex = new bootstrap.Modal(document.getElementById('modalKardex'));
let tiposMovimientoCache = [];

document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/tipos-movimiento')
        .then(res => res.json())
        .then(data => {
            tiposMovimientoCache = data;
            const select = document.getElementById('movTipoId');
            data.forEach(tipo => {
                if (tipo.nombre !== 'VENTA') {
                    const option = document.createElement('option');
                    option.value = tipo.id;
                    option.text = tipo.nombre.replace(/_/g, ' '); 
                    option.dataset.naturaleza = tipo.naturaleza;
                    select.appendChild(option);
                }
            });
        });
});

function prepararMovimiento(btn) {
    const id = btn.getAttribute('data-id');
    const nombre = btn.getAttribute('data-nombre');

    document.getElementById('movProductoBodegaId').value = id;
    document.getElementById('movProductoNombre').textContent = nombre;
    
    // Limpiar campos
    document.getElementById('movCantidad').value = '';
    document.getElementById('movMotivo').value = '';
    document.getElementById('movTipoId').value = '';
    document.getElementById('movHint').textContent = '';
    
    modalMovimiento.show();
}

function actualizarHint() {
    const select = document.getElementById('movTipoId');
    const selectedOption = select.options[select.selectedIndex];
    const naturaleza = selectedOption.dataset.naturaleza;
    const hint = document.getElementById('movHint');
    
    if (naturaleza === 'ENTRADA') {
        hint.innerHTML = '<span class="text-success"><i class="bi bi-plus-circle"></i> Esto aumentará el stock</span>';
    } else {
        hint.innerHTML = '<span class="text-danger"><i class="bi bi-dash-circle"></i> Esto descontará del stock</span>';
    }
}

function guardarMovimiento(e) {
    e.preventDefault();
    
    const data = {
        productoBodegaId: document.getElementById('movProductoBodegaId').value,
        tipoMovimientoId: document.getElementById('movTipoId').value,
        cantidad: document.getElementById('movCantidad').value,
        motivo: document.getElementById('movMotivo').value
    };

    fetch('/api/inventario', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(async response => {
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Error al registrar');
        }
        return response.json();
    })
    .then(() => {
        modalMovimiento.hide();
        Swal.fire({
            icon: 'success',
            title: 'Movimiento registrado',
            showConfirmButton: false,
            timer: 1500
        }).then(() => location.reload());
    })
    .catch(err => {
        Swal.fire('Error', err.message, 'error');
    });
}

function prepararKardex(btn) {
    const id = btn.getAttribute('data-id');
    const nombre = btn.getAttribute('data-nombre');

    document.getElementById('kardexProductoNombre').textContent = nombre;
    const tbody = document.getElementById('kardexTableBody');
    tbody.innerHTML = '<tr><td colspan="5">Cargando...</td></tr>';
    
    modalKardex.show();

    fetch(`/api/inventario/${id}`)
        .then(res => res.json())
        .then(movimientos => {
            tbody.innerHTML = '';
            if (movimientos.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-muted">Sin movimientos registrados</td></tr>';
                return;
            }

            movimientos.forEach(mov => {
            
                const palabrasClaveEntrada = ['COMPRA', 'DEVOLUCION', 'ENTRADA', 'AJUSTE_POSITIVO', 'ANULACION'];
                
                const nombreMov = mov.tipoMovimientoNombre ? mov.tipoMovimientoNombre.toUpperCase() : '';
                const esEntrada = palabrasClaveEntrada.some(s => nombreMov.includes(s));

                const badgeClass = esEntrada ? 'badge-entrada' : 'badge-salida';
                const signo = esEntrada ? '+' : '-';
                const colorClass = esEntrada ? 'text-success' : 'text-danger';

                const tr = `
                    <tr>
                        <td class="small">${new Date(mov.fecha).toLocaleString()}</td>
                        <td><span class="badge-movimiento ${badgeClass}">${mov.tipoMovimientoNombre}</span></td>
                        <td class="small text-start">${mov.motivo || '-'}</td>
                        <td class="fw-bold ${colorClass}">${signo}${mov.cantidad}</td>
                        <td class="small text-muted">${mov.usuarioNombre}</td>
                    </tr>
                `;
                tbody.innerHTML += tr;
            });
        });
}