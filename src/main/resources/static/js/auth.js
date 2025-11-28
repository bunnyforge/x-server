// Check for auth token
const authToken = localStorage.getItem('authToken');

if (!authToken) {
    window.location.href = '/login.html';
}

// Intercept fetch requests to add token
const originalFetch = window.fetch;
window.fetch = async function(url, options = {}) {
    if (!options.headers) {
        options.headers = {};
    }
    
    // Add auth token if not already present
    if (!options.headers['X-Auth-Token']) {
        options.headers['X-Auth-Token'] = authToken;
    }

    try {
        const response = await originalFetch(url, options);
        
        // Handle 401 Unauthorized
        if (response.status === 401) {
            localStorage.removeItem('authToken');
            window.location.href = '/login.html';
            return Promise.reject(new Error('Unauthorized'));
        }
        
        return response;
    } catch (error) {
        throw error;
    }
};
