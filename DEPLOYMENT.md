# Monday Backend — Cloud Deployment Guide (Docker)

This guide details how to build, configure, and deploy the Monday backend container to any major cloud service (Google Cloud Run, AWS App Runner / ECS, Railway, Render, Fly.io, or any Docker VPS).

---

## 1. Cloud-Ready Architecture Highlights

The backend is built with cloud-native principles:
- **Multi-Stage Dockerfile**: Builds on `maven:3.9.6-eclipse-temurin-21-jammy` and runs on a slim `eclipse-temurin:21-jre-jammy` image.
- **Security & Least Privilege**: Runs as an unprivileged user (`monday:monday`) rather than `root`.
- **Dynamic Port Binding**: Automatically binds to `${PORT:-8080}` provided by cloud platforms (Google Cloud Run, Render, Railway, AWS App Runner).
- **Graceful Shutdown**: Enabled by default (`server.shutdown=graceful`) with a 20-second drain window for zero-downtime rolling deploys.
- **Forwarded Headers**: Automatically trusts proxy headers (`X-Forwarded-Proto`, `X-Forwarded-For`) so HTTPS sessions, cookies, and Swagger documentation work correctly behind Cloud Load Balancers and CDNs.
- **Observability & Probes**: `/actuator/health` (including `/actuator/health/liveness` and `/actuator/health/readiness`) are exposed for cloud health checks.
- **Container Memory Optimization**: Automated JVM cgroup integration (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`).

---

## 2. Environment Variables Reference

| Variable | Description | Default / Example |
|---|---|---|
| `PORT` | Listening HTTP port (set automatically by most cloud PaaS) | `8080` |
| `MONDAY_DB_URL` | PostgreSQL JDBC URL (or `SPRING_DATASOURCE_URL`) | `jdbc:postgresql://<db-host>:5432/<dbname>` |
| `MONDAY_DB_USERNAME` | PostgreSQL username (or `SPRING_DATASOURCE_USERNAME`) | `postgres` |
| `MONDAY_DB_PASSWORD` | PostgreSQL password (or `SPRING_DATASOURCE_PASSWORD`) | *(your-password)* |
| `OPENAI_API_KEY` | OpenAI API key for intelligence briefing | *(your-key)* |
| `MONDAY_SESSION_SECURE` | Set `true` in production behind HTTPS (defaults to `true`) | `true` |
| `MONDAY_SESSION_SAME_SITE`| Session cookie SameSite attribute (`none`, `lax`, `strict`) | `none` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `https://monday-ui-omega.vercel.app` |
| `SPRING_FLYWAY_ENABLED` | Set `true` to auto-run Flyway migrations on startup | `false` |
| `JAVA_OPTS` | Additional JVM options | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` |
| `LOGGING_LEVEL_APP` | Application logging level | `INFO` |

> **Note for Managed PostgreSQL (Neon, Supabase, AWS RDS, Cloud SQL)**:  
> When connecting over the public internet or managed services, ensure SSL is enabled in the JDBC URL:  
> `jdbc:postgresql://<db-host>:5432/monday?sslmode=require`

---

## 3. Database Initialization

Before starting the backend in production:
1. Create a PostgreSQL database (version 15, 16, or 17).
2. Execute the consolidated schema script:
   - File: [`backend/src/main/resources/db/master_schema.sql`](file:///backend/src/main/resources/db/master_schema.sql)
   - You can run this in your cloud provider SQL console (Supabase SQL Editor, Neon SQL Console, AWS RDS query tool, or via `psql`):
     ```bash
     psql -h <host> -U <user> -d <dbname> -f backend/src/main/resources/db/master_schema.sql
     ```
3. Alternatively, if you prefer migrations to run automatically on application startup, set:
   ```env
   SPRING_FLYWAY_ENABLED=true
   ```

---

## 4. Local Testing with Docker Compose

To test the container and database setup locally:

```bash
# Build and start both PostgreSQL and the backend
docker-compose up --build -d

# Check container health and logs
docker-compose ps
docker-compose logs -f backend

# Verify health endpoint
curl http://localhost:8080/actuator/health
```

---

## 5. Building & Pushing the Docker Image

```bash
# Set your container registry / repository name
REGISTRY="docker.io/your-username" # or ghcr.io, gcr.io, ECR URL
IMAGE_NAME="monday-backend"
TAG="latest"

# Build the image using the backend Dockerfile
docker build -t ${REGISTRY}/${IMAGE_NAME}:${TAG} -f backend/Dockerfile backend/

# Push to your cloud container registry
docker push ${REGISTRY}/${IMAGE_NAME}:${TAG}
```

---

## 6. Cloud Platform Deployment Guides

### A. Google Cloud Run

Google Cloud Run is an ideal serverless container platform for Monday:

1. Build and push image to Google Artifact Registry:
   ```bash
   gcloud builds submit backend/ --tag gcr.io/$PROJECT_ID/monday-backend:latest
   ```
2. Deploy the service:
   ```bash
   gcloud run deploy monday-backend \
     --image gcr.io/$PROJECT_ID/monday-backend:latest \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --port 8080 \
     --memory 1Gi \
     --cpu 1 \
     --set-env-vars "MONDAY_DB_URL=jdbc:postgresql://<HOST>:5432/monday?sslmode=require,MONDAY_DB_USERNAME=postgres,MONDAY_DB_PASSWORD=<PASS>,OPENAI_API_KEY=<KEY>,MONDAY_SESSION_SECURE=true"
   ```

### B. AWS App Runner / AWS ECS (Fargate)

1. Push your container image to **AWS Elastic Container Registry (ECR)**.
2. In AWS App Runner:
   - **Source**: Select your ECR image repository.
   - **Port**: `8080`.
   - **Health check path**: `/actuator/health`.
   - **Environment variables**: Add `MONDAY_DB_URL`, `MONDAY_DB_USERNAME`, `MONDAY_DB_PASSWORD`, `OPENAI_API_KEY`, and `MONDAY_SESSION_SECURE=true`.
   - **Compute**: 1 vCPU, 2 GB RAM.

### C. Render / Railway

1. Link your GitHub repository.
2. Set service type to **Docker**.
3. Point Dockerfile path to `backend/Dockerfile` and Docker build context to `backend`.
4. Under **Environment Variables**, configure:
   - `MONDAY_DB_URL`: `jdbc:postgresql://...`
   - `MONDAY_DB_USERNAME`: Database username
   - `MONDAY_DB_PASSWORD`: Database password
   - `OPENAI_API_KEY`: Your OpenAI API key
   - `MONDAY_SESSION_SECURE`: `true`
5. Set Health Check path to `/actuator/health`.

### D. Self-Hosted VPS (EC2 / DigitalOcean / Hetzner)

1. Clone repository to your server:
   ```bash
   git clone <repo-url> && cd monday
   ```
2. Copy and customize `.env`:
   ```bash
   cp backend/.env.example .env
   ```
3. Run with Docker Compose:
   ```bash
   docker-compose up -d --build
   ```
4. Put Nginx, Caddy, or Cloudflare Tunnel in front with SSL termination.

---

## 7. Health Checks & Verification

After deployment, test the following endpoints:

- **Liveness probe**: `GET /actuator/health/liveness` &rarr; `200 OK {"status":"UP"}`
- **Readiness probe**: `GET /actuator/health/readiness` &rarr; `200 OK {"status":"UP"}`
- **API Documentation**: `GET /swagger-ui/index.html` or `/swagger-ui.html`
- **Authentication**: `POST /api/v1/auth/login`

