document.addEventListener('DOMContentLoaded', () => {
    document.body.classList.add('page-ready');

    document.querySelectorAll('.panel-card, .metric-card, .landing-hero-copy, .landing-hero-side, .landing-section-dark, .auth-clean-card, .auth-clean-visual, .executive-hero, .section-ribbon').forEach((el, index) => {
        el.style.animationDelay = `${index * 35}ms`;
        el.classList.add('fade-up-in');
    });

    document.querySelectorAll('a[href^="#"]').forEach(link => {
        link.addEventListener('click', event => {
            const id = link.getAttribute('href');
            const target = document.querySelector(id);
            if (target) {
                event.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

    const accountMenu = document.querySelector('[data-account-menu]');
    const accountToggle = document.querySelector('[data-account-toggle]');

    if (accountMenu && accountToggle) {
        const closeMenu = () => accountMenu.classList.remove('open');
        const toggleMenu = event => {
            event.stopPropagation();
            accountMenu.classList.toggle('open');
        };

        accountToggle.addEventListener('click', toggleMenu);
        document.addEventListener('click', event => {
            if (!accountMenu.contains(event.target)) {
                closeMenu();
            }
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                closeMenu();
            }
        });
    }

    const moduleSearch = document.querySelector('[data-module-search]');
    const moduleItems = Array.from(document.querySelectorAll('[data-module-item]'));

    if (moduleSearch && moduleItems.length) {
        moduleSearch.addEventListener('input', () => {
            const term = moduleSearch.value.trim().toLowerCase();
            moduleItems.forEach(item => {
                const text = item.textContent.toLowerCase();
                item.style.display = text.includes(term) ? '' : 'none';
            });
        });
    }
});
