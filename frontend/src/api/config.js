/**
 * Base URL for the API.
 * In development, this relies on the Vite proxy (e.g., empty string -> /api).
 * In production, it can be configured via VITE_API_BASE_URL.
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
