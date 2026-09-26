import React from 'react';

const FileViewer = ({ file, isLoading, error }) => {
    if (isLoading) {
        return (
            <div className="file-viewer-container">
                <div className="file-viewer-header">Loading file...</div>
                <div className="file-viewer-content" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748b' }}>
                    Fetching file contents...
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="file-viewer-container">
                <div className="file-viewer-header">Error</div>
                <div className="file-viewer-content" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--danger-color)' }}>
                    {error}
                </div>
            </div>
        );
    }

    if (!file) {
        return (
            <div className="file-viewer-container">
                <div className="file-viewer-header">No file selected</div>
                <div className="file-viewer-content" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748b' }}>
                    Select a file from the tree to view its contents
                </div>
            </div>
        );
    }

    return (
        <div className="file-viewer-container">
            <div className="file-viewer-header">
                {file.path}
            </div>
            <div className="file-viewer-content">
                {file.content || 'File is empty.'}
            </div>
        </div>
    );
};

export default FileViewer;
