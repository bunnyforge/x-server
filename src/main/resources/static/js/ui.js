/**
 * UI Utilities - Custom dialogs and notifications
 */

// Toast Container - create on first use or when DOM ready
function ensureToastContainer() {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        container.id = 'toastContainer';
        document.body.appendChild(container);
    }
    return container;
}

/**
 * Show a toast notification
 * @param {string} type - 'success', 'error', 'warning', 'info'
 * @param {string} title - Toast title
 * @param {string} message - Toast message
 * @param {number} duration - Duration in ms (default 3000)
 */
function showToast(type, title, message, duration = 3000) {
    const container = ensureToastContainer();
    const icons = {
        success: '✅',
        error: '❌',
        warning: '⚠️',
        info: 'ℹ️'
    };

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || icons.info}</span>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
    `;

    container.appendChild(toast);

    // Auto remove
    setTimeout(() => {
        toast.classList.add('toast-out');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

/**
 * Show a custom confirm dialog
 * @param {Object} options - Dialog options
 * @param {string} options.type - 'danger', 'warning', 'info'
 * @param {string} options.title - Dialog title
 * @param {string} options.message - Dialog message
 * @param {string} options.confirmText - Confirm button text
 * @param {string} options.cancelText - Cancel button text
 * @returns {Promise<boolean>} - Resolves to true if confirmed, false if cancelled
 */
function showConfirm(options) {
    return new Promise((resolve) => {
        const {
            type = 'warning',
            title = 'Confirm',
            message = 'Are you sure?',
            confirmText = 'Confirm',
            cancelText = 'Cancel'
        } = options;

        const icons = {
            danger: '🗑️',
            warning: '⚠️',
            info: 'ℹ️'
        };

        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
            <div class="confirm-dialog">
                <div class="confirm-icon ${type}">${icons[type] || icons.warning}</div>
                <div class="confirm-title">${title}</div>
                <div class="confirm-message">${message}</div>
                <div class="confirm-actions">
                    <button class="btn btn-secondary" id="confirmCancel">${cancelText}</button>
                    <button class="btn ${type === 'danger' ? 'btn-danger' : 'btn-primary'}" id="confirmOk">${confirmText}</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const cleanup = (result) => {
            overlay.remove();
            resolve(result);
        };

        overlay.querySelector('#confirmOk').onclick = () => cleanup(true);
        overlay.querySelector('#confirmCancel').onclick = () => cleanup(false);
        overlay.onclick = (e) => {
            if (e.target === overlay) cleanup(false);
        };

        // Focus confirm button
        overlay.querySelector('#confirmOk').focus();

        // ESC to cancel
        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                document.removeEventListener('keydown', handleEsc);
                cleanup(false);
            }
        };
        document.addEventListener('keydown', handleEsc);
    });
}

/**
 * Show an alert dialog (single button)
 * @param {Object} options - Dialog options
 * @param {string} options.type - 'danger', 'warning', 'info', 'success'
 * @param {string} options.title - Dialog title
 * @param {string} options.message - Dialog message
 * @param {string} options.buttonText - Button text
 * @returns {Promise<void>}
 */
function showAlert(options) {
    return new Promise((resolve) => {
        const {
            type = 'info',
            title = 'Alert',
            message = '',
            buttonText = 'OK'
        } = options;

        const icons = {
            danger: '❌',
            warning: '⚠️',
            info: 'ℹ️',
            success: '✅'
        };

        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
            <div class="confirm-dialog">
                <div class="confirm-icon ${type}">${icons[type] || icons.info}</div>
                <div class="confirm-title">${title}</div>
                <div class="confirm-message">${message}</div>
                <div class="confirm-actions">
                    <button class="btn btn-primary" id="alertOk">${buttonText}</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const cleanup = () => {
            overlay.remove();
            resolve();
        };

        overlay.querySelector('#alertOk').onclick = cleanup;
        overlay.onclick = (e) => {
            if (e.target === overlay) cleanup();
        };

        overlay.querySelector('#alertOk').focus();

        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                document.removeEventListener('keydown', handleEsc);
                cleanup();
            }
        };
        document.addEventListener('keydown', handleEsc);
    });
}

/**
 * Setup custom form validation for a form
 * @param {HTMLFormElement} form - The form element to setup validation for
 */
function setupFormValidation(form) {
    const inputs = form.querySelectorAll('input[required], textarea[required], select[required]');
    
    inputs.forEach(input => {
        // Remove default browser validation UI
        input.addEventListener('invalid', (e) => {
            e.preventDefault();
            showValidationError(input);
        });
        
        // Clear error on input
        input.addEventListener('input', () => {
            clearValidationError(input);
        });
        
        // Clear error on focus
        input.addEventListener('focus', () => {
            clearValidationError(input);
        });
    });
}

/**
 * Show validation error tooltip
 * @param {HTMLElement} input - The input element
 */
function showValidationError(input) {
    clearValidationError(input);
    
    const message = input.validationMessage || 'This field is required';
    const tooltip = document.createElement('div');
    tooltip.className = 'validation-tooltip';
    tooltip.innerHTML = `
        <span class="validation-tooltip-icon">⚠️</span>
        <span>${message}</span>
    `;
    
    // Position tooltip
    const rect = input.getBoundingClientRect();
    const parent = input.closest('.form-group') || input.parentElement;
    parent.style.position = 'relative';
    parent.appendChild(tooltip);
    
    // Add error style to input
    input.style.borderColor = '#ef4444';
    
    // Auto hide after 3 seconds
    setTimeout(() => {
        clearValidationError(input);
    }, 3000);
}

/**
 * Clear validation error tooltip
 * @param {HTMLElement} input - The input element
 */
function clearValidationError(input) {
    const parent = input.closest('.form-group') || input.parentElement;
    const tooltip = parent.querySelector('.validation-tooltip');
    if (tooltip) {
        tooltip.remove();
    }
    input.style.borderColor = '';
}
