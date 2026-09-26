/**
 * API service for managing GitHub integration
 */

import { API_BASE_URL } from './config';

export const connectGitHub = async (projectId) => {
    const response = await fetch(`${API_BASE_URL}/api/github/connect/${projectId}`, {
        method: 'POST'
    });

    if (!response.ok) {
        let errorMessage = 'Failed to connect to GitHub';
        try {
            const errorBody = await response.json();
            if (errorBody && errorBody.message) {
                errorMessage = errorBody.message;
            } else if (errorBody && errorBody.error) {
                errorMessage = errorBody.error;
            }
        } catch (e) {
            // Ignore JSON parse error if response is not JSON
        }
        throw new Error(errorMessage);
    }

    return await response.json();
};

export const fetchRepositoryTree = async (projectId) => {
    const response = await fetch(`${API_BASE_URL}/api/github/tree/${projectId}`);

    if (!response.ok) {
        let errorMessage = 'Failed to load repository tree';
        try {
            const errorBody = await response.json();
            if (errorBody && errorBody.message) {
                errorMessage = errorBody.message;
            } else if (errorBody && errorBody.error) {
                errorMessage = errorBody.error;
            }
        } catch (e) {
            // Ignore
        }
        throw new Error(errorMessage);
    }

    return await response.json();
};

export const fetchFileContent = async (projectId, path) => {
    const response = await fetch(`${API_BASE_URL}/api/github/file/${projectId}?path=${encodeURIComponent(path)}`);

    if (!response.ok) {
        let errorMessage = 'Failed to load file content';
        try {
            const errorBody = await response.json();
            if (errorBody && errorBody.message) {
                errorMessage = errorBody.message;
            } else if (errorBody && errorBody.error) {
                errorMessage = errorBody.error;
            }
        } catch (e) {
            // Ignore
        }
        throw new Error(errorMessage);
    }

    return await response.json();
};
