import React from 'react';

const ExplainResult = ({ data }) => {
    return (
        <div className="ai-result-section">
            <h3 className="ai-result-title">Code Explanation</h3>
            <div className="ai-explanation-text">
                {data.explanation.split('\n').map((line, idx) => (
                    <p key={idx}>{line}</p>
                ))}
            </div>
        </div>
    );
};

const BugResults = ({ data }) => {
    if (!data.bugs || data.bugs.length === 0) {
        return (
            <div className="ai-result-section">
                <h3 className="ai-result-title">Bug Analysis</h3>
                <p>{data.summary || 'No bugs were identified.'}</p>
                <div className="success-message" style={{ marginTop: '1rem' }}>
                    No bugs were identified.
                </div>
            </div>
        );
    }

    const getSeverityClass = (severity) => {
        const sev = (severity || '').toUpperCase();
        if (sev === 'CRITICAL' || sev === 'HIGH') return 'severity-high';
        if (sev === 'MEDIUM') return 'severity-medium';
        return 'severity-low';
    };

    return (
        <div className="ai-result-section">
            <h3 className="ai-result-title">Bug Analysis</h3>
            <p><strong>Summary:</strong> {data.summary}</p>
            <p>Found {data.bugs.length} potential issue(s):</p>

            <div className="ai-cards-list">
                {data.bugs.map((bug, idx) => (
                    <div key={idx} className="ai-item-card">
                        <div className="ai-item-header">
                            <span className={`ai-badge ${getSeverityClass(bug.severity)}`}>
                                {bug.severity}
                            </span>
                            <h4>{bug.title}</h4>
                        </div>
                        <p className="ai-item-desc">{bug.description}</p>
                        {bug.lineReference && (
                            <p className="ai-item-meta"><strong>Line(s):</strong> {bug.lineReference}</p>
                        )}
                        <div className="ai-item-recommendation">
                            <strong>Suggestion:</strong> {bug.suggestion}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

const ImprovementResults = ({ data }) => {
    if (!data.suggestions || data.suggestions.length === 0) {
        return (
            <div className="ai-result-section">
                <h3 className="ai-result-title">Code Improvements</h3>
                <p>{data.summary || 'No significant improvement opportunities were identified.'}</p>
                <div className="success-message" style={{ marginTop: '1rem' }}>
                    No significant improvement opportunities were identified.
                </div>
            </div>
        );
    }

    return (
        <div className="ai-result-section">
            <h3 className="ai-result-title">Code Improvements</h3>
            <p><strong>Summary:</strong> {data.summary}</p>

            <div className="ai-cards-list">
                {data.suggestions.map((sug, idx) => (
                    <div key={idx} className="ai-item-card">
                        <div className="ai-item-header">
                            <span className="ai-badge badge-category">{sug.category}</span>
                            <span className="ai-badge badge-priority">{sug.priority}</span>
                            <h4>{sug.title}</h4>
                        </div>
                        <p className="ai-item-desc">{sug.description}</p>
                        {sug.lineReference && (
                            <p className="ai-item-meta"><strong>Line(s):</strong> {sug.lineReference}</p>
                        )}
                        <div className="ai-item-recommendation">
                            <strong>Recommendation:</strong> {sug.recommendation}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

const TestResults = ({ data }) => {
    if (!data.tests || data.tests.length === 0) {
        return (
            <div className="ai-result-section">
                <h3 className="ai-result-title">Test Suggestions</h3>
                <p>{data.summary || 'No tests are required for this code.'}</p>
                <div className="success-message" style={{ marginTop: '1rem' }}>
                    No tests are required for this code.
                </div>
            </div>
        );
    }

    return (
        <div className="ai-result-section">
            <h3 className="ai-result-title">Test Suggestions</h3>
            <p><strong>Summary:</strong> {data.summary}</p>

            <div className="ai-cards-list">
                {data.tests.map((test, idx) => (
                    <div key={idx} className="ai-item-card">
                        <div className="ai-item-header">
                            <span className="ai-badge badge-category">{test.testType}</span>
                            <span className="ai-badge badge-priority">{test.priority}</span>
                            <h4>{test.title}</h4>
                        </div>
                        <p className="ai-item-desc">{test.description}</p>
                        {test.targetMethod && (
                            <p className="ai-item-meta"><strong>Target Method:</strong> {test.targetMethod}</p>
                        )}
                        {test.scenario && (
                            <p className="ai-item-meta"><strong>Scenario:</strong> {test.scenario}</p>
                        )}
                        <div className="ai-item-recommendation">
                            <strong>Expected Behavior:</strong> {test.expectedBehavior}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

const AiResult = ({ result }) => {
    if (!result || !result.data) return null;

    switch (result.operation) {
        case 'explain':
            return <ExplainResult data={result.data} />;
        case 'bugs':
            return <BugResults data={result.data} />;
        case 'improvements':
            return <ImprovementResults data={result.data} />;
        case 'tests':
            return <TestResults data={result.data} />;
        default:
            return null;
    }
};

export default AiResult;
