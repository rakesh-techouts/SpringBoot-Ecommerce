class ThemeManager {
    constructor() {
        this.storageKey = 'theme-preference';
        this.darkIcon = '🌙';
        this.lightIcon = '☀️';
        console.log('ThemeManager initialized');
        this.init();
    }

    init() {
        this.createToggleButton();
        this.loadTheme();
        this.setupEventListeners();
        console.log('Theme setup complete');
    }

    createToggleButton() {
        const button = document.createElement('button');
        button.className = 'theme-toggle';
        button.setAttribute('aria-label', 'Toggle theme');
        button.innerHTML = `<span class="theme-icon">${this.getIcon()}</span>`;
        document.body.appendChild(button);
        this.toggleButton = button;
        console.log('Theme toggle button created');
    }

    setupEventListeners() {
        this.toggleButton.addEventListener('click', () => this.toggleTheme());
        
        // Listen for system theme changes
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
                if (!localStorage.getItem(this.storageKey)) {
                    this.setTheme(e.matches ? 'dark' : 'light');
                }
            });
        }
    }

    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        this.setTheme(newTheme);
    }

    setTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(this.storageKey, theme);
        this.updateIcon(theme);
        
        // Add transition class for smooth theme switching
        document.body.classList.add('theme-transitioning');
        setTimeout(() => {
            document.body.classList.remove('theme-transitioning');
        }, 300);
    }

    loadTheme() {
        const savedTheme = localStorage.getItem(this.storageKey);
        const systemPrefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        const theme = savedTheme || (systemPrefersDark ? 'dark' : 'light');
        this.setTheme(theme);
    }

    getIcon() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        return currentTheme === 'dark' ? this.darkIcon : this.lightIcon;
    }

    updateIcon(theme) {
        if (this.toggleButton) {
            const icon = this.toggleButton.querySelector('.theme-icon');
            if (icon) {
                icon.textContent = theme === 'dark' ? this.darkIcon : this.lightIcon;
            }
        }
    }
}

// Initialize theme manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ThemeManager();
});

// Add smooth transitions for theme switching
const style = document.createElement('style');
style.textContent = `
    .theme-transitioning *,
    .theme-transitioning *::before,
    .theme-transitioning *::after {
        transition: all 0.3s ease !important;
    }
`;
document.head.appendChild(style);
