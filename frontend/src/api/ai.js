import { API_BASE_URL } from './config';
/**
 * API service for managing AI operations
 */

const handleResponse = async (response) => {
    if (!response.ok) {
        let errorMessage = 'Failed to execute AI operation';
        try {
            const errorBody = await response.json();
            if (errorBody && errorBody.message) {
                errorMessage = errorBody.message;
            } else if (errorBody && errorBody.error) {
                errorMessage = errorBody.error;
            }
        } catch (e) {
            // Ignore if not json
        }
        throw new Error(errorMessage);
    }
    return await response.json();
};

export const explainCode = async (projectId, path) => {
    const response = await fetch(`${API_BASE_URL}/api/ai/explain/${projectId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ path }),
    });
    return handleResponse(response);
};

export const analyzeBugs = async (projectId, path) => {
    const response = await fetch(`${API_BASE_URL}/api/ai/analyze/${projectId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ path }),
    });
    return handleResponse(response);
};

export const suggestImprovements = async (projectId, path) => {
    const response = await fetch(`${API_BASE_URL}/api/ai/improve/${projectId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ path }),
    });
    return handleResponse(response);
};

export const suggestTests = async (projectId, path) => {
    const response = await fetch(`${API_BASE_URL}/api/ai/tests/${projectId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ path }),
    });
    return handleResponse(response);
};
