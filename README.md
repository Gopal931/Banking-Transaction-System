# Banking Transaction Processing System

A full-stack Banking Transaction Processing System built using Java, Spring Boot, PostgreSQL, React, and Material UI.

## Technology Stack

### Backend
- Java 17+
- Spring Boot
- Maven
- JDBC / JdbcTemplate
- PostgreSQL

### Frontend
- React
- Vite
- Material UI
- React Router
- Axios

## Project Structure

- `backend/` – Spring Boot REST API
- `frontend/` – React customer and admin interfaces
- `database/` – SQL scripts and database setup
- `docs/` – Architecture and technical documentation
- `performance/` – Performance testing and analysis

## Local Development

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL: http://localhost:8080

Health API: http://localhost:8080/api/health

### Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: http://localhost:5173

## Project Status

Phase 1 – Project setup and foundation.

Banking operations, authentication, database integration, and transaction processing will be implemented in subsequent phases.

## Important

This is an educational banking simulation project. It does not connect to real banking networks or process real money.

Docker and cloud deployment are not part of the local development setup.
