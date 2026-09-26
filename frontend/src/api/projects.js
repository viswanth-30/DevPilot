/**
 * API service for managing Projects
 */

import { API_BASE_URL } from './config';

export const fetchProjects = async () => {
    const response = await fetch(`${API_BASE_URL}/api/projects`);
    if (!response.ok) {
        throw new Error('Failed to fetch projects');
    }
    return await response.json();
};

export const fetchProjectById = async (id) => {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`);
    if (!response.ok) {
        throw new Error(`Failed to fetch project with id: ${id}`);
    }
    return await response.json();
};

export const createProject = async (projectData) => {
    const response = await fetch(`${API_BASE_URL}/api/projects`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(projectData),
    });

    if (!response.ok) {
        let errorMessage = 'Failed to create project';
        try {
            const errorBody = await response.json();
            if (errorBody && errorBody.error) {
                errorMessage = errorBody.error;
            }
        } catch (e) {
            // Ignore JSON parse error if response is not JSON
        }
        throw new Error(errorMessage);
    }
    return await response.json();
};
