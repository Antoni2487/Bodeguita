

function iniciarPollingNotificaciones() {
    setInterval(actualizarBadge, 5000); // 5 segundos
    actualizarBadge(); // Ejecutar al cargar
}

function actualizarBadge() {
    fetch('/api/notificaciones/mis-alertas')
        .then(response => {
            if (!response.ok) throw new Error("Error red");
            return response.json();
        })
        .then(notificaciones => {
            // Buscamos los elementos en el DOM (Deben estar en tu navbar)
            const badge = document.getElementById('notifBadge'); 
            const lista = document.getElementById('notifList');
            
            // 1. Contar no leídas
            const noLeidas = notificaciones.filter(n => !n.leido).length;

            // 2. Actualizar Badge (Bolita Roja)
            if (badge) {
                badge.innerText = noLeidas > 99 ? '99+' : noLeidas;
                // Mostrar solo si hay > 0
                if (noLeidas > 0) {
                    badge.classList.remove('d-none');
                    badge.classList.add('animate__animated', 'animate__pulse'); // Animación opcional
                } else {
                    badge.classList.add('d-none');
                }
            }

            // 3. Lllenar Lista del Dropdown (PILA VISUAL)
            if (lista) {
                if (notificaciones.length === 0) {
                    lista.innerHTML = '<li><span class="dropdown-item text-muted text-center py-3">Sin notificaciones</span></li>';
                } else {
                    let html = '';
                    // El backend ya devuelve la lista ordenada (LIFO), así que solo iteramos
                    notificaciones.slice(0, 5).forEach(n => { // Mostramos solo las 5 últimas
                        const bgClass = n.leido ? '' : 'bg-light fw-bold';
                        const icono = n.tipo === 'PEDIDO' ? '<i class="bi bi-cart-plus text-primary"></i>' : '<i class="bi bi-info-circle"></i>';
                        
                        html += `
                            <li>
                                <a class="dropdown-item ${bgClass} border-bottom py-2" href="${n.urlDestino}">
                                    <div class="d-flex align-items-center gap-2">
                                        ${icono}
                                        <div>
                                            <div class="small text-truncate" style="max-width: 200px;">${n.mensaje}</div>
                                            <div class="text-muted" style="font-size: 0.7rem;">Hace un momento</div>
                                        </div>
                                    </div>
                                </a>
                            </li>
                        `;
                    });
                    html += '<li><a class="dropdown-item text-center small text-primary py-2" href="/notificaciones">Ver todas</a></li>';
                    lista.innerHTML = html;
                }
            }
        })
        .catch(e => console.error("Polling error:", e));
}

// Auto-iniciar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", iniciarPollingNotificaciones);