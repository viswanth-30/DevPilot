import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProject } from '../api/projects';

const CreateProject = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        githubUrl: ''
    });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [validationErrors, setValidationErrors] = useState({});

    const validateForm = () => {
        const errors = {};
        if (!formData.name || formData.name.trim() === '') {
            errors.name = 'Project name is required';
        } else if (formData.name.length > 100) {
            errors.name = 'Project name must be 100 characters or less';
        }

        if (formData.description && formData.description.length > 500) {
            errors.description = 'Description is too long';
        }

        setValidationErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error when user starts typing
        if (validationErrors[name]) {
            setValidationErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setIsSubmitting(true);
        setError(null);

        try {
            const newProject = await createProject(formData);
            navigate(`/projects/${newProject.id}`);
        } catch (err) {
            setError(err.message);
            setIsSubmitting(false);
        }
    };

    return (
        <div>
            <div className="page-header">
                <h1 className="page-title">Create New Project</h1>
            </div>

            <div className="card" style={{ maxWidth: '600px' }}>
                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="name">Project Name *</label>
                        <input
                            type="text"
                            id="name"
                            name="name"
                            className="form-control"
                            value={formData.name}
                            onChange={handleChange}
                            maxLength={100}
                        />
                        {validationErrors.name && <div className="form-error">{validationErrors.name}</div>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="githubUrl">GitHub URL (Optional)</label>
                        <input
                            type="url"
                            id="githubUrl"
                            name="githubUrl"
                            className="form-control"
                            placeholder="https://github.com/username/repo"
                            value={formData.githubUrl}
                            onChange={handleChange}
                        />
                        {validationErrors.githubUrl && <div className="form-error">{validationErrors.githubUrl}</div>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="description">Description (Optional)</label>
                        <textarea
                            id="description"
                            name="description"
                            className="form-control"
                            value={formData.description}
                            onChange={handleChange}
                        />
                        {validationErrors.description && <div className="form-error">{validationErrors.description}</div>}
                    </div>

                    <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Creating...' : 'Create Project'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => navigate('/')}
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateProject;
