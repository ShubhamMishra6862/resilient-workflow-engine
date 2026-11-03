# FlowLab: Crash-Proof Resilient Workflow Engine

> **A durable workflow engine designed to execute long-running multi-step processes that survive server crashes (`kill -9`), eliminate duplicate side effects through idempotency, automatically backoff & retry, and compensate transactions using the SAGA pattern.**

![FlowLab Banner](https://img.shields.io/badge/FlowLab-Crash--Proof%20Engine-blue?style=for-the-badge)
![Tech Stack](https://img.shields.io/badge/Spring%20Boot%203-Java%2021-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)
![React](https://img.shields.io/badge/React%2018-TypeScript-indigo?style=flat-square)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-Dark%20Dashboard-38bdf8?style=flat-square)

---

## 1. What This Project Is

In distributed systems, critical business processes (e.g., e-commerce orders, refund pipelines, money transfers) span multiple independent systems and take time. If the server dies halfway through—say after charging payment but before updating inventory—a standard application loses memory and fails catastrophically:
- The customer is double charged when retried from scratch.
- Or the workflow is orphaned and forgotten.

**FlowLab** guarantees **durable execution**:
1. **Durable State**: State is committed to PostgreSQL before and after every single step transition.
2. **Crash-Proof Resume**: If a worker or server dies mid-step (`kill -9`), the engine automatically detects expired worker leases and resumes from the exact pending step without restarting from scratch.
3. **Strict Idempotency**: Side-effect operations (e.g. credit card payment) write an `idempotency_record`. Re-running after a crash returns the stored payment reference without re-charging.
4. **Non-Blocking Retries with Backoff**: Failed steps calculate an exponential backoff with jitter and persist `next_retry_at` in the database. No worker threads ever block on `Thread.sleep()`.
5. **SAGA Backward Compensation**: If a step fails permanently after retries, previous successful steps are rolled back in reverse order (e.g., automated payment refund).

---

## 2. Dashboard UI (FlowLab)

The user interface matches the dark-theme design:

```
+---------------------------------------------------------------------------------------------------------+
| [FlowLab]  Workflows > Order Processing                                                                 |
|                                                                                                         |
| Order Processing [Active]                                                      [+ Start Workflow]       |
| Process customer orders with payment, inventory and notification steps.                                 |
|                                                                                                         |
| +-----------------+     +-----------------+     +-----------------+     +-----------------+             |
| | [Cart]          |     | [Credit Card]   |     | [Cube]          |     | [Mail]          |             |
| | Validate Order  | --> | Process Payment | --> | Update Inventory| --> | Send Notif.     |             |
| | Completed       |     | Completed       |     | Running         |     | Pending         |             |
| +-----------------+     +-----------------+     +-----------------+     +-----------------+             |
|                                                                                                         |
| Execution History                                                                     [View Logs]       |
| Step               Status       Started At            Duration        Attempts                          |
| Validate Order     ● Success    Oct 10, 2024 10:24:11 2.3s            1                                 |
| Process Payment    ● Success    Oct 10, 2024 10:24:14 4.1s            1                                 |
| Update Inventory   ● Running    Oct 10, 2024 10:24:18 12.4s           2                                 |
| Send Notification  ● Pending    -                     -               -                                 |
+---------------------------------------------------------------------------------------------------------+
```

---

## 3. High-Level Architecture

```
                    React UI (FlowLab Dashboard)
                               │
                       REST API (/api/v1)
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
       PostgreSQL Database              Step Worker Pool
       ───────────────────              ────────────────
       - workflow_definition            - ValidateOrderStep
       - workflow_execution             - ProcessPaymentStep (Idempotent)
       - step_execution (Leases)        - UpdateInventoryStep (Simulated Faults)
       - workflow_event (Audit)         - SendNotificationStep
       - idempotency_record             - SAGA Compensation Rollback
       - outbox (Transactional)
               ▲
               │ Scans expired leases (lease_expires_at < now)
               │ Scans due retries (next_retry_at <= now)
        Recovery Scheduler (@Scheduled)
```

---

## 4. Quick Start Guide

### Option A: Run via Docker Compose (Recommended)
Spins up PostgreSQL, Redis, Backend (Spring Boot 3), and Frontend (Nginx SPA):
```bash
docker compose up --build
```
- **FlowLab Dashboard**: `http://localhost:3000`
- **Backend REST API**: `http://localhost:8080/api/v1`

---

### Option B: Run Locally (Zero external dependencies needed!)
The backend is configured with an in-memory PostgreSQL-compatible mode so it runs immediately out-of-the-box without requiring a running Docker daemon.

#### 1. Start the Backend (Spring Boot 3):
```bash
cd backend
./mvnw spring-boot:run
```
Backend will start on port `8080`.

#### 2. Start the Frontend (Vite + React):
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## 5. Live Interview Demonstrations

### Demo 1: The "Crash-Proof" `kill -9` Resumption
1. In the UI, click **Start Workflow**.
2. Uncheck "Simulate Transient Failure" or leave it default.
3. While `UpdateInventory` is in `Running` status, click the **"Simulate Crash (kill -9)"** button in the top navigation.
4. **What Happens**:
   - The worker lease is artificially expired (`lease_expires_at < now()`).
   - Within 3 seconds, the background `RecoveryScheduler` detects the dead worker lease.
   - It re-claims the lease, logs a `STEP_RECOVERED` event in the audit trail, and resumes execution from that exact step!
   - The workflow finishes cleanly without restarting from `ValidateOrder`.

### Demo 2: Idempotency Under Retries (No Double-Charging)
1. Run an order workflow that retries `ProcessPayment`.
2. Notice in the logs:
   - On attempt 1, `ProcessPayment` executes the payment and saves `${executionId}:ProcessPayment` to `idempotency_record`.
   - On subsequent executions/retries, `ProcessPayment` detects the record and logs:
     `[ProcessPayment] Idempotency record FOUND! Skipping payment gateway call`.
   - The same payment ID (`PAY-XXXX`) is reused.

### Demo 3: SAGA Backward Compensation
1. Click **Start Workflow**.
2. Check **"Trigger SAGA backward compensation"**.
3. Watch the pipeline:
   - `ValidateOrder` succeeds.
   - `ProcessPayment` succeeds ($149.99 charged).
   - `UpdateInventory` permanently fails (out of stock).
   - SAGA triggers: previous steps are compensated in reverse order (`ProcessPayment` is refunded, `ValidateOrder` is cancelled).
   - Execution state reaches `COMPENSATED`.

---

## 6. System Design Interview Cheatsheet

### Q1: How does the engine survive a server crash (`kill -9`)?
> **Answer**: State is persisted before and after every step transition in PostgreSQL. Workers acquire a time-bounded lease in the database (`lease_expires_at = now() + 30s`). If a worker dies, its lease expires. The leader `RecoveryScheduler` periodically queries `WHERE status = 'RUNNING' AND lease_expires_at < now()`, marks the step for recovery, and dispatches it to another worker.

### Q2: What if the worker crashes right after charging the credit card but before writing SUCCESS to the DB?
> **Answer**: This is the classic dual-write distributed dilemma. When the worker re-runs after crash recovery, it generates the idempotency key (`executionId + ':' + stepName`). Before calling Stripe/Payment Gateway, it checks `idempotency_record`. Because the gateway call and idempotency write are linked, the gateway call returns the existing payment token or the local idempotency table returns the cached result, preventing double-charging.

### Q3: Why use the Transactional Outbox pattern?
> **Answer**: Writing to PostgreSQL and publishing to Kafka directly leads to race conditions and dual-write inconsistencies if the broker is unreachable. With the transactional outbox pattern, the state change and the event row are committed in **one single database transaction**. An outbox poller then reliably publishes events to Kafka with at-least-once delivery guarantees.

### Q4: Why store `next_retry_at` in the database instead of `Thread.sleep()`?
> **Answer**: `Thread.sleep()` ties up worker threads, exhausting thread pools and degrading throughput. More critically, if the server restarts during sleep, the retry is permanently lost. Storing `next_retry_at` in the database allows the worker thread to be freed immediately, makes the retry immune to server restarts, and lets a background scheduler dispatch it precisely when due.

### Q5: How does this custom engine compare with Temporal?
> **Answer**: Temporal uses event-sourcing and deterministic code replay: activities run at-least-once, while workflow code is replayed to reconstruct state. FlowLab uses explicit state machine tables in PostgreSQL. While Temporal is great for production, building a custom engine reveals the foundational mechanics: distributed leases, optimistic locking (`@Version`), idempotency keys, and recovery schedulers.

---

## 7. REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/workflows` | Lists all workflow definitions |
| `GET` | `/api/v1/workflows/{name}` | Retrieves single workflow definition |
| `POST` | `/api/v1/workflows/{name}/executions` | Starts a new workflow execution (Returns `202 Accepted` + `Location`) |
| `GET` | `/api/v1/executions` | Lists executions with pagination & status filters |
| `GET` | `/api/v1/executions/{id}` | Retrieves execution details with all step states |
| `GET` | `/api/v1/executions/{id}/events` | Returns chronological append-only audit event log |
| `POST` | `/api/v1/executions/{id}/cancel` | Cancels an active execution |
| `POST` | `/api/v1/executions/{id}/steps/{step}/retry` | Manually retries a failed step |
| `POST` | `/api/v1/executions/{id}/simulate-crash` | Simulates an abrupt worker crash (`kill -9`) |

---

## 8. Database Schema Overview

```sql
workflow_definition (id, name, version, description, steps_json, active, created_at)
workflow_execution  (id, workflow_name, definition_id, status, current_step_index, context_json, version, created_at)
step_execution      (id, execution_id, step_name, step_index, status, attempt_count, max_attempts, next_retry_at, lease_expires_at, duration_ms, error)
workflow_event      (id, execution_id, event_type, step_name, detail_json, created_at)
idempotency_record  (idempotency_key, aggregate_id, step_name, result_json, created_at)
outbox              (id, aggregate_id, topic, payload_json, status, retry_count, created_at)
```

---

## License
MIT License. Created for System Design & Engineering Excellence.
