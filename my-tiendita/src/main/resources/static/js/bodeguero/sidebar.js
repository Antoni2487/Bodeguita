const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');
    const toggleBtn = document.getElementById('toggleSidebar');
    const overlay = document.getElementById('sidebarOverlay');

    toggleBtn.addEventListener('click', () => {
      if (window.innerWidth <= 991) {
        // Mobile: show/hide sidebar
        sidebar.classList.toggle('show');
        overlay.classList.toggle('show');
      } else {
        // Desktop: collapse/expand sidebar
        sidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('expanded');
      }
    });

    // Close sidebar when clicking overlay (mobile)
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('show');
      overlay.classList.remove('show');
    });

    // Close sidebar when clicking a link (mobile)
    document.querySelectorAll('.sidebar-menu a').forEach(link => {
      link.addEventListener('click', () => {
        if (window.innerWidth <= 991) {
          sidebar.classList.remove('show');
          overlay.classList.remove('show');
        }
      });

    });