# NexusOps

**NexusOps is an in-progress server monitoring and incident intelligence platform.**
It provides a secure foundation for managing monitored hosts and collecting
system telemetry, with anomaly detection and AI-assisted incident analysis
planned as future capabilities.

## Overview

NexusOps combines a React dashboard, a Spring Boot modular-monolith backend, a
PostgreSQL data store, and a lightweight Java monitoring agent. The agent uses
OSHI to collect host metrics and sends authenticated heartbeats and metric
batches to the backend for persistence.

The project is being developed incrementally, with operational reliability and
clear module boundaries prioritized over premature service decomposition.

## Current capabilities

- User registration and login with BCrypt password hashing.
- Short-lived JWT access tokens and rotating, revocable refresh tokens.
- Role-based access for administrators, operators, viewers, and agents.
- Server registration, listing, updates, and decommissioning.
- Agent credential generation, rotation, and revocation; credentials are
  stored as hashes.
- Authenticated agent heartbeats and bounded metric batch ingestion.
- PostgreSQL persistence managed through Flyway migrations.
- OSHI collection of CPU, memory, disk, network, and process metrics.
- Docker Compose deployment for the frontend, backend, database, and optional
  monitoring agent.

The current dashboard supports authentication and server listing. Metric
visualization, alert management, and incident workflows are planned and are not
yet available.

## Architecture

```mermaid
flowchart LR
    UI[React dashboard] -->|REST API| API[Spring Boot modular monolith]
    Agent[Java OSHI agent] -->|Authenticated heartbeat and metric batches| API
    API --> DB[(PostgreSQL)]
    API -. future: live events .-> UI
    API -. future: anomaly analysis .-> ML[Python analysis service]
    ML -. future: incident context .-> LLM[LLM provider]
```

The backend groups authentication, server management, metrics, and shared
security/web infrastructure in one application. The monitoring agent is a
separate executable because it runs on monitored hosts. A Python analysis
service is a future extension, not part of the current deployment.

## Technology stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, JDBC |
| Frontend | React, TypeScript, Vite |
| Database | PostgreSQL, Flyway |
| Monitoring agent | Java 21, OSHI |
| API | REST, JSON, JWT |
| Packaging and local deployment | Docker, Docker Compose, Nginx |
| Planned analytics | Python, scikit-learn |
| Planned incident intelligence | LLM API integration |
| Planned live updates | WebSockets |

## Roadmap

### In progress: monitoring and telemetry

- Complete agent configuration and resilient delivery.
- Expand metric querying, aggregation, retention, and dashboard visualization.
- Improve agent and ingestion integration test coverage.

### Planned: alerting and incident response

- Configurable alert rules and threshold evaluation.
- Alert lifecycle and incident grouping.
- Incident timelines, assignment, acknowledgement, and resolution.
- Notifications through email or webhooks.

### Planned: real-time operations

- WebSocket updates for server health, metrics, alerts, and incidents.
- Live operational dashboards with reconnect handling.

### Planned: intelligence and analysis

- Python-based anomaly detection and model evaluation.
- Context-aware, structured LLM incident summaries and recommendations.
- Human-reviewed recommendations; no automatic remediation in the initial scope.

### Planned: production readiness

- Broader integration and load testing.
- Operational dashboards, backup/restore guidance, and deployment documentation.
- Continued security, dependency, and container-image reviews.

## Repository layout

```text
backend/    Spring Boot API, security, persistence, and database migrations
agent/      Standalone OSHI monitoring agent
frontend/   React and TypeScript application
```

## Project status

NexusOps is an active portfolio project. Authentication, server management,
metric ingestion, and the OSHI agent are implemented. Alerting, incident
management, metric visualization, WebSockets, anomaly detection, and LLM
analysis remain future work.
