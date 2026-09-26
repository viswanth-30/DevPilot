import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchProjects } from '../api/projects';

const Dashboard = () => {
    const [projects, setProjects] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadProjects = async () => {
            try {
                const data = await fetchProjects();
                setProjects(data);
                setError(null);
            } catch (err) {
                setError(err.message);
            } finally {
                setIsLoading(false);
            }
        };

        loadProjects();
    }, []);

    return (
        <div>
            <div className="page-header">
                <h1 className="page-title">Dashboard</h1>
                <Link to="/projects/new" className="btn btn-primary">New Project</Link>
            </div>

            {error && <div className="error-message">{error}</div>}

            {isLoading ? (
                <div className="loading">Loading projects...</div>
            ) : projects.length === 0 ? (
                <div className="empty-state">
                    <h3>No projects found</h3>
                    <p>Get started by creating your first project.</p>
                    <Link to="/projects/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>Create Project</Link>
                </div>
            ) : (
                <div className="projects-grid">
                    {projects.map((project) => (
                        <div key={project.id} className="card project-card">
                            <h3>{project.name}</h3>
                            {project.githubUrl && (
                                <div className="project-meta">
                                    Repository: {project.repoOwner}/{project.repoName}
                                </div>
                            )}
                            <p>{project.description || 'No description provided.'}</p>
                            <Link to={`/projects/${project.id}`} className="btn btn-secondary">
                                View Details
                            </Link>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default Dashboard;
