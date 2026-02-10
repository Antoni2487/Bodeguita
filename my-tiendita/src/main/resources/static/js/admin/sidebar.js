/**
 * ADMIN SIDEBAR - JavaScript
 * BodeguitaSmart - Panel de Administración
 */

document.addEventListener('DOMContentLoaded', function() {
    
    // Elementos del DOM
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');
    const toggleBtn = document.getElementById('toggleSidebar');
    const overlay = document.getElementById('sidebarOverlay');

    /**
     * Alternar visibilidad del sidebar
     */
    function toggleSidebar() {
        sidebar.classList.toggle('collapsed');
        sidebar.classList.toggle('active');
        mainContent.classList.toggle('expanded');
        overlay.classList.toggle('active');
    }

    // Event listeners
    if (toggleBtn) {
        toggleBtn.addEventListener('click', toggleSidebar);
    }

    if (overlay) {
        overlay.addEventListener('click', toggleSidebar);
    }

    /**
     * Cerrar sidebar en mobile al hacer click en un link
     */
    if (window.innerWidth <= 768) {
        const sidebarLinks = document.querySelectorAll('.sidebar-menu a');
        sidebarLinks.forEach(link => {
            link.addEventListener('click', function() {
                sidebar.classList.remove('active');
                overlay.classList.remove('active');
            });
        });
    }

    /**
     * Manejar resize de ventana
     */
    window.addEventListener('resize', function() {
        if (window.innerWidth > 768) {
            sidebar.classList.remove('active');
            overlay.classList.remove('active');
        }
    });

    /**
     * Prevenir scroll del body cuando el sidebar está abierto en mobile
     */
    overlay.addEventListener('click', function() {
        document.body.style.overflow = 'auto';
    });

    toggleBtn.addEventListener('click', function() {
        if (window.innerWidth <= 768) {
            if (sidebar.classList.contains('active')) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = 'auto';
            }
        }
    });

    console.log('✅ Admin Sidebar cargado correctamente');
});