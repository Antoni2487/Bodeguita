document.addEventListener('DOMContentLoaded', function() {
    
    // =========================================================
    // 1. MAPA Y GEOLOCALIZACIÓN (LEAFLET)
    // =========================================================
    
    // Leemos los valores iniciales de los inputs ocultos
    const latInput = document.getElementById('inputLat');
    const lngInput = document.getElementById('inputLng');
    
    // Valores por defecto (Chiclayo) si no hay datos guardados
    let latInicial = parseFloat(latInput.value) || -6.77137;
    let lngInicial = parseFloat(lngInput.value) || -79.84088;

    // Inicializar Mapa
    const map = L.map('mapaUbicacion').setView([latInicial, lngInicial], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    // Marcador arrastrable
    const marker = L.marker([latInicial, lngInicial], {
        draggable: true,
        title: "Arrastra para ubicar tu bodega"
    }).addTo(map);

    // --- Funciones de Actualización ---
    function updateCoordinates(lat, lng) {
        // Actualizar inputs ocultos (para el backend)
        if(latInput) latInput.value = lat;
        if(lngInput) lngInput.value = lng;
        
        // Actualizar etiqueta visual
        const lblLat = document.getElementById('lblLat');
        const lblLng = document.getElementById('lblLng');
        if(lblLat) lblLat.innerText = lat.toFixed(6);
        if(lblLng) lblLng.innerText = lng.toFixed(6);
    }

    // Inicializar etiquetas
    updateCoordinates(latInicial, lngInicial);

    // --- Eventos del Mapa ---
    
    // Al soltar el marcador (dragend)
    marker.on('dragend', function(event) {
        const position = marker.getLatLng();
        updateCoordinates(position.lat, position.lng);
    });

    // Al hacer click en cualquier parte del mapa
    map.on('click', function(e) {
        marker.setLatLng(e.latlng);
        updateCoordinates(e.latlng.lat, e.latlng.lng);
    });

    // --- Fix para Pestañas (Leaflet se rompe en tabs ocultos) ---
    const tabDeliveryButton = document.getElementById('delivery-tab');
    
    if (tabDeliveryButton) {
        tabDeliveryButton.addEventListener('shown.bs.tab', function () {
            setTimeout(() => {
                map.invalidateSize(); // Obliga al mapa a recalcular su tamaño
                map.panTo(marker.getLatLng()); // Recentrar
            }, 100);
        });
    }

    // =========================================================
    // 2. SWITCH DELIVERY (HABILITAR/DESHABILITAR INPUTS)
    // =========================================================
    const switchDelivery = document.getElementById('switchDelivery');
    const inputsContainer = document.getElementById('reglasDeliveryInputs');
    
    function toggleDeliveryInputs() {
        if (!inputsContainer || !switchDelivery) return;

        const inputs = inputsContainer.querySelectorAll('input');
        const isEnabled = switchDelivery.checked;

        inputs.forEach(input => {
            input.disabled = !isEnabled;
        });

        // Efecto visual
        if (!isEnabled) {
            inputsContainer.classList.add('disabled-section');
        } else {
            inputsContainer.classList.remove('disabled-section');
        }
    }

    if (switchDelivery) {
        switchDelivery.addEventListener('change', toggleDeliveryInputs);
        // Ejecutar al inicio para establecer estado correcto
        toggleDeliveryInputs();
    }
});

// =========================================================
// 3. LOGICA DE PAGOS Y QR (AJAX) - GLOBAL
// =========================================================

/**
 * Previsualiza la imagen seleccionada antes de subirla
 */
window.previsualizarQR = function(input, id) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('preview-' + id);
            const icon = document.getElementById('icon-' + id);
            
            // Si ya existía la imagen, actualizamos src
            if (preview) {
                preview.src = e.target.result;
                preview.style.display = 'block';
                if(icon) icon.style.display = 'none';
            } else {
                // Si no existía (era null), ocultamos el icono y mostramos el preview
                if(icon) {
                    icon.style.display = 'none';
                    // Buscamos la imagen oculta o creamos una lógica si el HTML no la tenía
                    const hiddenPreview = document.getElementById('preview-' + id); 
                    if(hiddenPreview) {
                        hiddenPreview.src = e.target.result;
                        hiddenPreview.style.display = 'block';
                    }
                }
            }
        }
        reader.readAsDataURL(input.files[0]);
    }
};

/**
 * Envía el formulario de pago individual vía AJAX
 */
window.guardarMetodoPago = function(id) {
    const form = document.getElementById('form-pago-' + id);
    const switchActivo = document.getElementById('switch-' + id);
    
    if(!form) return;

    // Crear FormData con los datos del formulario
    const formData = new FormData(form);
    formData.append('id', id);
    formData.append('activo', switchActivo.checked);

    // UI Loading
    const btn = event.currentTarget;
    const originalContent = btn.innerHTML;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Guardando...';
    btn.disabled = true;

    // Fetch al endpoint API creado en el Controller
    fetch('/api/bodeguero/pagos/actualizar', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Usar un toast o alert bonito si tienes librerías, sino alert nativo
            alert("✅ " + data.message);
        } else {
            alert("❌ Error: " + data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert("❌ Error de conexión al guardar el método de pago.");
    })
    .finally(() => {
        // Restaurar botón
        btn.innerHTML = originalContent;
        btn.disabled = false;
    });
};