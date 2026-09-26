import React from 'react';
import { NavLink } from 'react-router-dom';

const Sidebar = () => {
    return (
        <div className="sidebar">
            <div className="sidebar-header">
                <h2>DevPilot</h2>
            </div>
            <nav className="sidebar-nav">
                <NavLink
                    to="/"
                    className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}
                    end
                >
                    Dashboard
                </NavLink>
                <NavLink
                    to="/projects/new"
                    className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}
                >
                    Create Project
                </NavLink>
            </nav>
        </div>
    );
};

export default Sidebar;
