import React, { useState, useEffect } from 'react';
import { explainCode, analyzeBugs, suggestImprovements, suggestTests } from '../api/ai';
import AiResult from './AiResult';

const AiAssistant = ({ projectId, selectedFile }) => {
    const [activeOp, setActiveOp] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);

    // Clear state when the selected file changes
    useEffect(() => {
        setActiveOp(null);
        setIsLoading(false);
        setResult(null);
        setError(null);
    }, [selectedFile]);

    if (!selectedFile) {
        return (
            <div className="card">
                <h2>AI Code Assistant</h2>
                <div className="empty-state">
                    Select a source file to use the AI Code Assistant.
                </div>
            </div>
        );
    }

    const handleAction = async (operationName, apiCall) => {
        setActiveOp(operationName);
        setIsLoading(true);
        setResult(null);
        setError(null);

        try {
            const data = await apiCall(projectId, selectedFile.path);
            setResult({ operation: operationName, data });
        } catch (err) {
            setError(err.message);
            setActiveOp(null);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="card">
            <h2>AI Code Assistant</h2>

            <div className="ai-file-banner">
                Selected: <strong>{selectedFile.path}</strong>
            </div>

            <div className="ai-actions">
                <button
                    className="btn btn-secondary"
                    onClick={() => handleAction('explain', explainCode)}
                    disabled={isLoading}
                >
                    Explain Code
                </button>
                <button
                    className="btn btn-secondary"
                    onClick={() => handleAction('bugs', analyzeBugs)}
                    disabled={isLoading}
                >
                    Find Bugs
                </button>
                <button
                    className="btn btn-secondary"
                    onClick={() => handleAction('improvements', suggestImprovements)}
                    disabled={isLoading}
                >
                    Suggest Improvements
                </button>
                <button
                    className="btn btn-secondary"
                    onClick={() => handleAction('tests', suggestTests)}
                    disabled={isLoading}
                >
                    Suggest Tests
                </button>
            </div>

            {isLoading && (
                <div className="loading">
                    Running AI {activeOp}... please wait.
                </div>
            )}

            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}

            {!isLoading && !error && result && (
                <AiResult result={result} />
            )}
        </div>
    );
};

export default AiAssistant;
