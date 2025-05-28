document.addEventListener('DOMContentLoaded', function() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    
    console.log('CSRF Protection loaded:', { token: token ? 'Present' : 'Missing', header });

    const forms = document.querySelectorAll('form');
    console.log('Found forms:', forms.length);
    
    forms.forEach(form => {
        if (token && !form.querySelector('input[name="_csrf"]')) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = token;
            form.appendChild(input);
            console.log('Added CSRF token to form:', form);
        }
    });

    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
        if (token && header) {
            options.headers = options.headers || {};
            options.headers[header] = token;
            console.log('Added CSRF header to fetch:', url);
        }
        return originalFetch(url, options);
    };

    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            mutation.addedNodes.forEach(function(node) {
                if (node.nodeType === 1) {
                    if (node.tagName === 'FORM') {
                        addCsrfToForm(node);
                    }
                    const forms = node.querySelectorAll ? node.querySelectorAll('form') : [];
                    forms.forEach(addCsrfToForm);
                }
            });
        });
    });
    
    function addCsrfToForm(form) {
        if (token && !form.querySelector('input[name="_csrf"]')) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = token;
            form.appendChild(input);
            console.log('Added CSRF token to dynamic form:', form);
        }
    }

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
});
