# Rain Cheque — Deployment Readiness & Go-Live Guide

This document assesses how close Rain Cheque is to a production deployment on a `.co.za` domain and lays out the remaining steps. It reflects the current state of the repository (prod Spring profile, Dockerfiles, and Kubernetes manifests).

## Summary

The deployment scaffolding is in good shape, but nothing is provisioned yet and several production-blocking gaps remain. You are further along on artifacts than on an actual running deployment.

- Artifacts present: ~60–70%
- Provisioned/running in production: none yet
- Realistic effort once a host is chosen: a few focused days

## What Is Already in Place

- **Production Spring profile** (`application-prod.yml`): PostgreSQL via environment variables, `ddl-auto: validate`, `open-in-view: false`, hidden error messages, H2 console disabled, tuned HikariCP pool.
- **Containerization**: multi-stage Dockerfiles for the backend (non-root user, JRE-alpine runtime) and the frontend (Vite build served by nginx).
- **Kubernetes manifests** under `k8s/`: namespace, configmap, secrets, backend deployment, frontend deployment, ingress.
- **Health endpoint** suitable for liveness/readiness probes.
- **Payments**: Paystack integration (Paystack supports South Africa) with verify-on-return working locally.

## Production-Blocking Gaps

### 1. Host and domain not yet chosen or provisioned
This is the biggest open decision and it drives everything else. Options:
- Single VM running `docker compose` (simplest, cheapest).
- Managed Kubernetes (the existing manifests target this).
- A PaaS such as Render, Railway, or Fly (least ops; would diverge from the k8s manifests).

For the domain: register the `.co.za` name through a ZACR-accredited registrar, then point DNS at the chosen host.

### 2. TLS and real hostname
The ingress is hardcoded to `host: raincheque.local` with no TLS section. Production needs the real domain and HTTPS (cert-manager + Let's Encrypt on Kubernetes, or the provider's certificate mechanism).

### 3. CORS is localhost-only
`app.cors.allowed-origins` is currently `http://localhost:5173,http://localhost:3000`. The production `.co.za` origin must be added (ideally made environment-driven), otherwise the browser will block the frontend from calling the API.

### 4. Secrets hygiene
The repository `.env` contains real-looking JWT, QR, database, and Paystack values.
- Confirm `.env` is gitignored.
- Rotate any secret that has been committed.
- Populate production secrets via `k8s/secrets.yml` (currently placeholders) or the host's secret store — never in the repository.

### 5. Production database and schema
`ddl-auto: validate` means Hibernate checks the schema but does not create it. Production therefore needs:
- A managed PostgreSQL instance.
- The schema applied from `database/init/01-schema.sql` (Flyway is disabled).
- The test seed `database/init/02-seed-data.sql` kept out of production.

### 6. Real-money payments
- Switch to Paystack **live** API keys.
- Fix `PAYSTACK_WEBHOOK_SECRET` — Paystack signs webhooks with your secret key, not a `pk_` value.
- Implement webhook-based fulfillment as the source of truth (verify-on-return alone misses users who close the tab before redirect).
- Point the callback and webhook URLs at the `.co.za` domain in the Paystack dashboard.

### 7. Email delivery
Ticket emails currently target `localhost:25`. Production needs a real SMTP provider.

### 8. Frontend build configuration
The production frontend image must be built with `VITE_API_BASE_URL=https://<your-domain>.co.za/api/v1`.

## Non-Blocking Follow-Ups

- `findConfirmedBookingsByEventId` does not actually filter by status (Spring Data ignores the "Confirmed" subject), so batch generation would process all bookings for an event.
- The `user@example.com` seed row has a malformed bcrypt hash (trailing `.`), so that account cannot log in.
- Consider a CI/CD pipeline to build images, run tests, and deploy on merge.

## Go-Live Checklists by Hosting Choice

### Option A — Single VM + docker-compose
1. Provision a VM; install Docker and Docker Compose.
2. Register the `.co.za` domain; point an A record at the VM.
3. Add a TLS-terminating reverse proxy (Caddy or Traefik, or nginx + certbot).
4. Create a production `.env` on the host (not in git) with real secrets and live Paystack keys.
5. Set the production CORS origin and `VITE_API_BASE_URL`.
6. Provision PostgreSQL; apply `01-schema.sql`; exclude test seed data.
7. Configure SMTP for ticket email.
8. `docker compose up -d --build`; verify `/actuator/health`.
9. Configure Paystack webhook/callback URLs; test a live transaction.

### Option B — Managed Kubernetes
1. Provision a cluster and a managed PostgreSQL instance.
2. Register the `.co.za` domain; point DNS at the ingress load balancer.
3. Update `k8s/ingress.yml` with the real host and a TLS section; install cert-manager for Let's Encrypt.
4. Fill `k8s/secrets.yml` with real secrets and live Paystack keys.
5. Set the production CORS origin; build the frontend image with the correct `VITE_API_BASE_URL`.
6. Apply `01-schema.sql` to the managed database; exclude test seed data.
7. Configure SMTP for ticket email.
8. Add liveness/readiness probes to the deployments if not already present.
9. `kubectl apply` the manifests; verify health and a live Paystack transaction.

### Option C — PaaS
1. Create backend and frontend services on the platform; attach a managed PostgreSQL add-on.
2. Register the `.co.za` domain; configure it on the platform (TLS usually automatic).
3. Set environment variables/secrets in the platform dashboard (JWT, QR, DB, live Paystack keys, CORS origin).
4. Build the frontend with the correct `VITE_API_BASE_URL`.
5. Apply `01-schema.sql`; exclude test seed data.
6. Configure SMTP for ticket email.
7. Deploy; verify health and a live Paystack transaction.

## Open Questions to Resolve First

1. Which hosting target (VM, Kubernetes, or PaaS)?
2. Is the `.co.za` domain already registered?
3. Is there a Paystack live (production) account ready?

Answering these determines the exact ordered steps and which configuration changes come next.