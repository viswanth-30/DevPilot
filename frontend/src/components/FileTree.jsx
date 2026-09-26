import React, { useMemo, useState } from 'react';

const buildTree = (flatList) => {
    const root = [];
    const map = {};

    flatList.forEach(node => {
        map[node.path] = { ...node, children: [] };
    });

    flatList.forEach(node => {
        const parts = node.path.split('/');
        if (parts.length === 1) {
            root.push(map[node.path]);
        } else {
            const parentPath = parts.slice(0, -1).join('/');
            if (map[parentPath]) {
                map[parentPath].children.push(map[node.path]);
            } else {
                root.push(map[node.path]);
            }
        }
    });

    const sortNodes = (nodes) => {
        nodes.sort((a, b) => {
            if (a.type === 'tree' && b.type !== 'tree') return -1;
            if (a.type !== 'tree' && b.type === 'tree') return 1;
            return a.path.split('/').pop().localeCompare(b.path.split('/').pop());
        });
        nodes.forEach(n => sortNodes(n.children));
    };
    sortNodes(root);

    return root;
};

const TreeNode = ({ node, level, onSelectFile, selectedPath }) => {
    const [isExpanded, setIsExpanded] = useState(false);
    const isFolder = node.type === 'tree';
    const name = node.path.split('/').pop();
    const isSelected = selectedPath === node.path;

    const handleClick = () => {
        if (isFolder) {
            setIsExpanded(!isExpanded);
        } else {
            onSelectFile(node);
        }
    };

    return (
        <div>
            <div
                className={`file-tree-item ${isFolder ? 'folder' : 'file'} ${isSelected ? 'selected' : ''}`}
                style={{ paddingLeft: `${level * 1.2 + 0.5}rem` }}
                onClick={handleClick}
            >
                <span className="icon">
                    {isFolder ? (isExpanded ? '📂' : '📁') : '📄'}
                </span>
                {name}
            </div>
            {isFolder && isExpanded && node.children.map(child => (
                <TreeNode
                    key={child.path}
                    node={child}
                    level={level + 1}
                    onSelectFile={onSelectFile}
                    selectedPath={selectedPath}
                />
            ))}
        </div>
    );
};

const FileTree = ({ items, onSelectFile, selectedPath }) => {
    const treeData = useMemo(() => buildTree(items || []), [items]);

    if (!items || items.length === 0) {
        return <div style={{ padding: '1rem', color: '#64748b' }}>No files found.</div>;
    }

    return (
        <div>
            {treeData.map(node => (
                <TreeNode
                    key={node.path}
                    node={node}
                    level={0}
                    onSelectFile={onSelectFile}
                    selectedPath={selectedPath}
                />
            ))}
        </div>
    );
};

export default FileTree;
