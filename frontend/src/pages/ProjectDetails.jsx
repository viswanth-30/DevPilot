import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchProjectById } from '../api/projects';

const ProjectDetails = () => {
    const { id } = useParams();
    const [project, setProject] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadProject = async () => {
            try {
                const data = await fetchProjectById(id);
                setProject(data);
                setError(null);
            } catch (err) {
                setError(err.message);
            } finally {
                setIsLoading(false);
            }
        };

        loadProject();
    }, [id]);

    if (isLoading) {
        return <div className="loading">Loading project details...</div>;
    }

    if (error) {
        return (
            <div>
                <div className="error-message">{error}</div>
                <Link to="/" className="btn btn-secondary">Back to Dashboard</Link>
            </div>
        );
    }

    if (!project) {
        return <div className="empty-state">Project not found</div>;
    }

    return (
        <div>
            <div className="page-header">
                <h1 className="page-title">{project.name}</h1>
                <Link to="/" className="btn btn-secondary">Back to Dashboard</Link>
            </div>

            <div className="card">
                <h2>Project Information</h2>
                <div style={{ marginTop: '1.5rem' }}>
                    <p><strong>Status:</strong> {project.githubUrl ? 'Connected to GitHub' : 'Local Project'}</p>

                    {project.githubUrl && (
                        <>
                            <p><strong>GitHub URL:</strong> <a href={project.githubUrl} target="_blank" rel="noreferrer">{project.githubUrl}</a></p>
                            <p><strong>Repository:</strong> {project.repoOwner}/{project.repoName}</p>
                            <p><strong>Default Branch:</strong> {project.defaultBranch || 'Not analyzed yet'}</p>
                        </>
                    )}

                    <div style={{ marginTop: '1.5rem' }}>
                        <h3>Description</h3>
                        <p>{project.description || 'No description provided.'}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ProjectDetails;
