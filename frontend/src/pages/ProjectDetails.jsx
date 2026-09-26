import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchProjectById } from '../api/projects';
import { connectGitHub, fetchRepositoryTree, fetchFileContent } from '../api/github';
import FileTree from '../components/FileTree';
import FileViewer from '../components/FileViewer';
import AiAssistant from '../components/AiAssistant';

const ProjectDetails = () => {
    const { id } = useParams();
    const [project, setProject] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const [isConnecting, setIsConnecting] = useState(false);
    const [connectError, setConnectError] = useState(null);

    const [treeItems, setTreeItems] = useState([]);
    const [isTreeLoading, setIsTreeLoading] = useState(false);
    const [treeError, setTreeError] = useState(null);

    const [selectedFile, setSelectedFile] = useState(null);
    const [isFileLoading, setIsFileLoading] = useState(false);
    const [fileError, setFileError] = useState(null);

    const loadProject = async () => {
        try {
            const data = await fetchProjectById(id);
            setProject(data);
            setError(null);

            // If the project already has repo metadata, fetch the tree
            if (data.githubUrl && data.repoOwner && data.repoName) {
                loadTree(id);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadProject();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const loadTree = async (projectId) => {
        setIsTreeLoading(true);
        setTreeError(null);
        try {
            const data = await fetchRepositoryTree(projectId);
            setTreeItems(data.tree || []);
        } catch (err) {
            setTreeError(err.message);
        } finally {
            setIsTreeLoading(false);
        }
    };

    const handleConnect = async () => {
        setIsConnecting(true);
        setConnectError(null);
        try {
            await connectGitHub(id);
            // Refresh project to get repo details
            await loadProject();
        } catch (err) {
            setConnectError(err.message);
        } finally {
            setIsConnecting(false);
        }
    };

    const handleSelectFile = async (node) => {
        if (node.type === 'tree') return; // Should not happen with current FileTree logic

        setSelectedFile({ path: node.path, content: '' });
        setIsFileLoading(true);
        setFileError(null);

        try {
            const data = await fetchFileContent(id, node.path);
            setSelectedFile({ path: node.path, content: data.content });
        } catch (err) {
            setFileError(err.message);
        } finally {
            setIsFileLoading(false);
        }
    };

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

    const hasRepoDetails = project.repoOwner && project.repoName;

    return (
        <div>
            <div className="page-header">
                <h1 className="page-title">{project.name}</h1>
                <Link to="/" className="btn btn-secondary">Back to Dashboard</Link>
            </div>

            <div className="card">
                <h2>Project Information</h2>
                <div style={{ marginTop: '1.5rem' }}>
                    <h3>Description</h3>
                    <p>{project.description || 'No description provided.'}</p>
                </div>
            </div>

            {project.githubUrl && (
                <div className="card">
                    <h2>GitHub Repository</h2>

                    {connectError && <div className="error-message">{connectError}</div>}

                    <div style={{ marginTop: '1.5rem' }}>
                        <p><strong>GitHub URL:</strong> <a href={project.githubUrl} target="_blank" rel="noreferrer">{project.githubUrl}</a></p>

                        {hasRepoDetails ? (
                            <>
                                <p><strong>Owner:</strong> {project.repoOwner}</p>
                                <p><strong>Repository:</strong> {project.repoName}</p>
                                <p><strong>Branch:</strong> {project.defaultBranch}</p>
                            </>
                        ) : (
                            <div style={{ marginTop: '1rem' }}>
                                <p style={{ color: '#64748b', marginBottom: '1rem' }}>
                                    Repository information has not been loaded yet.
                                </p>
                                <button
                                    className="btn btn-primary"
                                    onClick={handleConnect}
                                    disabled={isConnecting}
                                >
                                    {isConnecting ? 'Connecting...' : 'Connect GitHub'}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {hasRepoDetails && (
                <>
                    <div className="card">
                        <h2>Repository Explorer</h2>

                        {treeError && <div className="error-message">{treeError}</div>}

                        <div style={{ marginTop: '1.5rem' }}>
                            {isTreeLoading ? (
                                <div className="loading">Loading repository tree...</div>
                            ) : (
                                <div className="repo-explorer">
                                    <div className="file-tree-container">
                                        <FileTree
                                            items={treeItems}
                                            onSelectFile={handleSelectFile}
                                            selectedPath={selectedFile?.path}
                                        />
                                    </div>
                                    <FileViewer
                                        file={selectedFile}
                                        isLoading={isFileLoading}
                                        error={fileError}
                                    />
                                </div>
                            )}
                        </div>
                    </div>

                    <AiAssistant projectId={project.id} selectedFile={selectedFile} />
                </>
            )}
        </div>
    );
};

export default ProjectDetails;
