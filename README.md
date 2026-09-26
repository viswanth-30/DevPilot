# DevPilot

DevPilot is an AI-powered software engineering assistant.

## Deployment Notes

### Local Development

**Prerequisites:**
- Java 21
- Node.js 18+
- PostgreSQL

**Backend Setup:**
1. Start PostgreSQL (e.g. on port 5432).
2. Set backend environment variables (refer to `backend/.env.example`):
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `GITHUB_TOKEN`
   - `AI_API_KEY`
3. Start the Spring Boot backend:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

**Frontend Setup:**
1. Ensure the backend is running.
2. Install dependencies and start the Vite development server:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
*(Note: In development, the Vite proxy forwards `/api` requests to `localhost:8080`, so no frontend environment variables are strictly necessary).*

### Production Deployment

**Backend:**
- Exposes the API on port 8080.
- Secrets (`DB_USERNAME`, `DB_PASSWORD`, `GITHUB_TOKEN`, `AI_API_KEY`) must be configured securely by the deployment platform via environment variables.
- You can override `DB_URL` (e.g., `jdbc:postgresql://db:5432/devpilot`).
- Set `CORS_ALLOWED_ORIGINS` to your frontend production URL (e.g., `https://devpilot.example.com`).
- A `Dockerfile` is provided for containerized deployment.

**Frontend:**
- Set `VITE_API_BASE_URL` during build/deployment to point to the production backend URL (e.g., `https://api.devpilot.example.com`).
- Build for production using `npm run build`. Serve the resulting `dist` folder.
