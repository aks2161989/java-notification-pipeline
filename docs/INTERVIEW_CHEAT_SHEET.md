# Software Engineering (SWE / Backend) Interview Preparation & Talking Points Guide
**Role**: Software Engineer / Backend Engineer / Systems Engineer  
**Candidate**: Akshay Madan Chavan | **Project**: Automated Publication-Notification Pipeline (Humanitarians AI)  
**Repository**: [https://github.com/aks2161989/notification-pipeline](https://github.com/aks2161989/notification-pipeline)  
**Core Technologies**: Python 3.10+, PostgreSQL / SQLite, SQLAlchemy ORM, Hexagonal Architecture (Ports & Adapters), Pytest, REST APIs, CLI Tooling

---

## 1. Executive Summary & Opening Elevator Pitch

> *"At Humanitarians AI, I identified an operational bottleneck in our content publication workflow. Fellows submitted educational videos to Google Drive, staff reviewed and queued them on YouTube, and an operations coordinator spent 4 to 6 hours every week manually cross-referencing filenames against YouTube video titles to determine who submitted what and email them.
>
> Matching by string titles was inherently fragile—titles were reworded for YouTube SEO, files got renamed, and multiple fellows frequently submitted videos with identical default titles. As a result, publication notifications were either missed or sent multiple times, eroding stakeholder trust.
>
> I designed and engineered a production-grade, event-driven reconciliation and notification pipeline in Python and PostgreSQL. Rather than writing a quick script, I architected the system using **Hexagonal Architecture (Ports & Adapters)** with a deterministic relational data model linking Google Drive file IDs, Fellow identities, and YouTube video IDs.
>
> To ensure zero duplicate notifications under network timeouts or server crashes, I implemented a **Two-Phase Commit state machine** and cross-process execution locks with TTL. I also built a pluggable adapter layer with stateful mocks, a 4-stage deployment rollout engine (`DRY_RUN`, `REDIRECTED`, `COHORT`, `PROD`), an automated retry service, and a diagnostic health monitor (`doctor`).
>
> The project eliminated 100% of the manual reconciliation effort, reduced notification latency from days to under 5 minutes upon video release, achieved a 24-test hermetic test suite executing in ~2 seconds, and serves as an auditable single source of truth for the entire content lifecycle."*

---

## 2. Core SWE Competency Mapping: Architecture $\longleftrightarrow$ Implementation

| Software Engineering Competency | Where It Lives in the Codebase | Key Talking Point for Interviewers |
| :--- | :--- | :--- |
| **Clean Architecture & Design Patterns (Hexagonal / Ports & Adapters)** | `src/adapters/`<br>`src/pipeline/dispatcher.py`<br>`src/models/domain.py` | "I decoupled core domain and business logic from external I/O (Google Drive, YouTube API, SMTP). Domain models are pure dataclasses, repositories abstract persistence, and external services implement abstract interfaces, making the entire system modular, pluggable, and easily testable." |
| **Distributed Systems Reliability & Idempotency** | `src/pipeline/engine.py`<br>`src/pipeline/recovery.py` | "To guarantee at-most-once delivery semantics in an unreliable network environment, I implemented a Two-Phase Commit state machine (`pending` $\rightarrow$ `sending` $\rightarrow$ `sent`/`failed`). State transitions are written to persistent storage *before* calling external APIs, preventing duplicate sends even if the worker process crashes mid-dispatch." |
| **Concurrency Control & Deadlock Prevention** | `src/pipeline/lock.py`<br>`src/pipeline/engine.py` | "Engineered a cross-process `ExecutionLock` featuring atomic file acquisition, PID metadata tracking, and automatic stale-lock reclamation (TTL) to prevent overlapping cron runs and deadlock states after ungraceful termination." |
| **Relational Data Modeling & ACID Integrity** | `src/models/orm.py`<br>`src/storage/seeder.py` | "Designed a normalized 3NF relational schema across 6 tables (`projects`, `pms`, `fellows`, `videos`, `review_events`, `notifications`). Enforced foreign key constraints, composite indexing (`idx_notif_video_status`), and SQL-level `CheckConstraint`s to guarantee data integrity at the database layer." |
| **Fault Isolation, Retries & Exponential Backoff** | `src/pipeline/recovery.py`<br>`src/cli.py` (`retry`) | "Engineered fault-isolated batch processing where an API failure on video #5 does not abort remaining items. Implemented a dedicated `RetryService` managing transient errors with retry quotas and exhaustion transitions to prevent infinite error loops." |
| **Defensive Integration with 3rd-Party APIs** | `src/adapters/youtube_adapter.py`<br>`src/adapters/normalizer.py` | "Built canonical extractors using regex to sanitize inconsistent input URLs down to canonical 11-char IDs, and designed API adapters resilient to rate limits, network timeouts, and authentication anomalies." |
| **Automated Testing & Test-Driven Design (TDD)** | `tests/conftest.py`<br>`tests/test_*.py` | "Engineered a hermetic test suite using pytest and in-memory SQLite fixtures with session rollback. Built mock adapters simulating edge cases (API throttling, dropped connections, unreleased videos). 24 automated unit and integration tests execute in 2.2 seconds with zero external dependencies." |
| **Production Rollout & Observability** | `src/adapters/email_adapter.py`<br>`src/cli.py` (`doctor`, `status`) | "Designed a 4-tier rollout engine (`DRY_RUN`, `REDIRECTED`, `COHORT`, `PROD`) allowing safe canary verification before touching live recipient inboxes. Implemented an operational CLI suite with health diagnostics (`doctor`) that scans for stranded records." |

---

## 3. Comprehensive SWE Interview Questions & STAR Answers

### Category 1: System Architecture & End-to-End Design

#### Q1: "Walk me through the high-level architecture of this system and why you chose it."
> **STAR Answer**:
> * **Situation**: The organization had a critical business workflow fragmented across spreadsheets, Google Drive, and YouTube, resulting in dropped communications and high manual overhead.
> * **Task**: I needed to architect an automated, resilient, and maintainable backend service that ingests metadata, tracks state transitions, and reliably delivers notifications upon video publication.
> * **Action**: I chose **Hexagonal Architecture (Ports and Adapters)** over a monolithic script:
>   1. **Domain Layer (`domain.py`)**: Defines core business entities (`VideoRecord`, `Fellow`, `NotificationRecord`) as decoupled dataclasses with zero framework dependencies.
>   2. **Storage Layer (`orm.py`, `repository.py`)**: Implements the Repository Pattern using SQLAlchemy, supporting seamless switching between SQLite for local testing and PostgreSQL for production.
>   3. **Adapter Layer (`adapters/`)**: Abstract interfaces for external dependencies (`YouTubeAdapter`, `DriveAdapter`, `EmailAdapter`), backed by concrete implementations for production and in-memory mock adapters for testing.
>   4. **Application Core (`engine.py`, `dispatcher.py`)**: Orchestrates business workflows, managing state transitions, concurrency locks, and idempotency checks.
>   5. **Interface Layer (`cli.py`)**: Exposes operational commands (`reconcile`, `status`, `doctor`, `retry`) for scheduled cron jobs or manual administrative operations.
> * **Result**: High separation of concerns, 100% testability with zero network mocks, and complete isolation of core business logic from third-party vendor APIs.

#### Q2: "Why did you build a standalone Python service with PostgreSQL instead of using serverless functions (AWS Lambda) or no-code tools (Zapier/Make)?"
> **STAR Answer**:
> * **Situation**: Initial internal discussions suggested using Google Apps Script or Zapier because the source documents resided in Google Workspace.
> * **Task**: Evaluate architectural trade-offs between rapid prototyping tools and an enterprise-grade backend service.
> * **Action**: I conducted a trade-off analysis and advocated for a dedicated Python/PostgreSQL service based on four engineering criteria:
>   1. **Execution Bounds & Concurrency**: Apps Script imposes a strict 6-minute execution timeout and lacks robust distributed locking (`LockService` has a 30s timeout and fails silently). In Python, we have deterministic execution limits and cross-process locking with TTL.
>   2. **Hermetic Automated Testing & CI/CD**: Low-code platforms cannot run deterministic unit/integration test suites or mock external network dependencies in a CI pipeline. Our Python suite runs 24 unit and integration tests in 2.2 seconds.
>   3. **Data Integrity & Relational Guarantees**: Zapier stores intermediate state in proprietary key-value blobs. PostgreSQL provides 3NF normalization, foreign key constraints, and transactional consistency (ACID).
>   4. **Portability & Cost**: A containerized Python service can be deployed anywhere (Docker, Kubernetes, AWS ECS, or local cron) with zero vendor lock-in and negligible cost compared to per-task SaaS tiers.
> * **Result**: Delivered a production-ready service with zero vendor lock-in, auditable transaction logs, and a repeatable CI/CD testing path.

---

### Category 2: Distributed Systems, Concurrency & Reliability

#### Q3: "What was the most difficult technical challenge you faced, and how did you solve it?"
> **STAR Answer**:
> * **Situation**: The most critical risk in any automated messaging system is **preventing duplicate notifications during partial failure modes**. If an external SMTP or Gmail API request hangs, times out, or the worker process crashes mid-flight, a naive retry loop would re-send emails to recipients, causing spam and stakeholder confusion.
> * **Task**: Implement strict **idempotency and at-most-once delivery semantics** across overlapping cron triggers, worker crashes, and transient network timeouts.
> * **Action**: I engineered a multi-layered defensive strategy:
>   1. **Pre-Check Filter**: Before touching any network adapter, the engine executes an eager join: `repo.get_active_or_sent_notification(video_id)` checking for rows with `delivery_status IN ('sending', 'sent')`. If found, processing for that video halts immediately.
>   2. **Two-Phase Commit State Machine**: If clear, the engine inserts a row with `delivery_status = 'sending'` and flushes the transaction to disk *before* making the external network call.
>   3. **Fault-Isolated Post-Commit**: If the email call succeeds, the record transitions to `SENT` with a UTC timestamp. If the network call raises an exception, it transitions to `FAILED` with the full error trace recorded.
>   4. **Crash Recovery (`doctor` CLI)**: If the host machine suffers an abrupt power loss or OOM kill mid-send, the row remains safely pinned at `SENDING`. Subsequent runs will never blindly re-send. Instead, our operational `doctor` command scans for stranded `SENDING` rows older than a configurable threshold and flags them for controlled investigation.
> * **Result**: Zero duplicate emails across hundreds of test executions, including simulated server crashes, dropped TCP sockets, and overlapping triggers.

#### Q4: "How does your `ExecutionLock` prevent race conditions, and how does it handle stale locks if a worker crashes?"
> **STAR Answer**:
> * **Situation**: If a cron job triggers while a previous reconciliation run is still executing (e.g., due to slow API responses), two concurrent processes could attempt to reconcile the same video records simultaneously, leading to race conditions.
> * **Task**: Ensure mutual exclusion across processes while safeguarding against permanent deadlocks caused by ungraceful crashes (SIGKILL, power failure).
> * **Action**: I designed a file-based lock with metadata and TTL in `src/pipeline/lock.py`:
>   1. **Atomic Acquisition**: Opens the lock file using OS-level atomic flags (`os.O_CREAT | os.O_EXCL`) ensuring only one process can create the lock file.
>   2. **Process Metadata Tracking**: The acquiring process writes its `pid`, `hostname`, and `timestamp` into the lock file for operational observability.
>   3. **Stale Lock Reclamation (TTL)**: If the lock exists, the incoming process inspects the file timestamp. If `current_time - lock_time > TTL` (default 10 minutes), the lock is deemed stale (from a dead process), removed, and cleanly re-acquired.
>   4. **Context Manager Pattern**: Implemented Python's `__enter__` and `__exit__` protocol to guarantee automatic lock release upon normal completion or unhandled exceptions.
> * **Result**: Robust mutual exclusion across cron runs with automatic self-healing if a worker terminates abnormally.

#### Q5: "Why did you choose a file-based lock instead of Redis or Database Advisory Locks?"
> **STAR Answer**:
> * **Situation**: Choosing the right locking mechanism requires balancing architectural simplicity against scalability.
> * **Task**: Select a synchronization strategy appropriate for a single-node scheduled batch worker without adding unnecessary infrastructure complexity.
> * **Action**: Evaluated three alternatives:
>   * *Redis (Redlock)*: Introduces an external network dependency, connection pool management, and maintenance overhead for a single-worker service.
>   * *Database Advisory Locks (`pg_try_advisory_lock`)*: Highly effective in PostgreSQL, but our system was designed to run seamlessly on both SQLite (for lightweight local development/CI) and PostgreSQL (for production). SQLite does not support advisory locks.
>   * *File-Based Lock with TTL*: Zero infrastructure dependencies, works consistently across POSIX/Windows environments, and supports both SQLite and Postgres backends.
>   * *Extensibility*: Abstracted the lock behind an interface so that when moving to distributed multi-node workers, switching to Redis or Postgres advisory locks requires changing only the locking adapter, without altering business logic.
> * **Result**: Eliminated infrastructure overhead while maintaining a clear upgrade path for horizontal scaling.

---

### Category 3: Data Modeling, Storage & Integrity

#### Q6: "Walk me through your database schema design. How did you ensure data integrity?"
> **STAR Answer**:
> * **Situation**: The historical tracking system was an unnormalized spreadsheet where video titles, fellow names, and URLs were stored as free-form strings, resulting in inconsistent data and orphaned records.
> * **Task**: Design a normalized relational data model enforcing strict referential integrity and query efficiency.
> * **Action**: I designed a **Third Normal Form (3NF)** schema across 6 relational tables in `src/models/orm.py`:
>   1. **Entities**: `projects`, `pms` (Project Managers), `fellows`, `videos`, `review_events`, and `notifications`.
>   2. **Referential Integrity**: Enforced foreign keys across all parent-child relationships (e.g., `videos.fellow_id` $\rightarrow$ `fellows.fellow_id`).
>   3. **Surrogate Keys vs Natural Keys**: Generated deterministic surrogate keys while maintaining unique constraints on natural business keys (`drive_file_id`, `youtube_video_id`).
>   4. **SQL-Level Constraints**: Instead of relying solely on Python application logic, declared SQL `CheckConstraint`s on critical columns:
>      * `current_status IN ('submitted', 'in_review', 'needs_modification', 'queued', 'published')`
>      * `delivery_status IN ('pending', 'sending', 'sent', 'failed')`
>   5. **Composite Indexing**: Added `Index("idx_notif_video_status", "video_id", "delivery_status")` to optimize the frequent idempotency pre-check query from an $O(N)$ table scan to an $O(1)$ index lookup.
> * **Result**: 100% data integrity enforced at the storage engine level, preventing corrupt states from entering the database.

#### Q7: "How did you handle dirty, unstructured input data from external sources?"
> **STAR Answer**:
> * **Situation**: Input data from Google Sheets contained inconsistent formats: full URLs, short URLs (`youtu.be/`), embed links, trailing whitespace, varied casing, and composite names (e.g., `"Jane Doe - Fall Cohort"`).
> * **Task**: Build a resilient ingestion and normalization pipeline to standardize external inputs before database insertion.
> * **Action**: In `src/adapters/normalizer.py`, I built canonical extraction and sanitization routines:
>   1. **Regex URL Canonicalization**: Created parsers extracting standard 11-character YouTube video IDs and alphanumeric Google Drive file tokens from any arbitrary URL variant.
>   2. **Text Normalization**: Applied Unicode normalization (`unicodedata.normalize`), trailing whitespace trimming, and lowercase conversion for email addresses.
>   3. **Data Seeder (`seeder.py`)**: Built an idempotent ingestion seeder that reconciles raw spreadsheet rows into normalized relational records, using `ON CONFLICT DO UPDATE` semantics to avoid duplicate entries.
> * **Result**: Clean ingestion boundary that insulates the core domain model from third-party formatting anomalies.

---

### Category 4: Software Quality, Testing & CI/CD

#### Q8: "How did you approach testing for this system? Walk me through your test strategy."
> **STAR Answer**:
> * **Situation**: Since the pipeline orchestrates email delivery and interacts with external APIs, manual testing in production would risk emailing real users with invalid or duplicate messages.
> * **Task**: Implement a fast, hermetic, and comprehensive automated test suite covering all critical edge cases and failure modes.
> * **Action**: I implemented an automated testing pyramid using `pytest`:
>   1. **Hermetic In-Memory Fixtures (`conftest.py`)**: Tests execute against an in-memory SQLite database using SQLAlchemy session-scoped fixtures with automatic teardown, ensuring each test is completely isolated and repeatable.
>   2. **Stateful Mock Adapters**: Built `MockYouTubeAdapter` and `MockDriveAdapter` that simulate state transitions (e.g., video toggling from private to public, API network timeouts, rate limit throttling) without live credentials or external network calls.
>   3. **Edge-Case Test Matrix**:
>      * *Idempotency Tests*: Verified that re-running the engine against previously processed videos skips them with zero duplicate records created.
>      * *Concurrency Tests*: Verified lock mutual exclusion, PID contention, and stale-lock TTL reclamation.
>      * *Constraint Enforcement Tests*: Verified that attempting to insert an invalid status triggers a database-level `IntegrityError`.
>      * *Crash Recovery Tests*: Simulated stranded `SENDING` rows and verified that the `doctor` command correctly identifies and quarantines them.
> * **Result**: **24 automated unit and integration tests executing in ~2.2 seconds** with zero external dependencies, providing instant feedback for CI/CD pipelines.

#### Q9: "Why and how did you implement a 4-Tier Rollout Engine?"
> **STAR Answer**:
> * **Situation**: Deploying changes to communication systems carry significant blast radius risks if bugs exist in email templating or recipient resolution.
> * **Task**: Provide a safe, staged deployment mechanism allowing progressive verification in staging and production environments.
> * **Action**: I designed a 4-tier operational execution mode in `src/config.py` and `src/adapters/email_adapter.py`:
>   1. `DRY_RUN`: Complete end-to-end execution that queries the database, polls APIs, and renders HTML email previews to a local `outbox/` directory, performing zero external network dispatches.
>   2. `REDIRECTED`: Dispatches live emails, but intercepts and rewrites the `To:` address to an internal test inbox (`REDIRECT_TEST_EMAIL`) while injecting the recipient's real address into an `X-Original-Recipient` header. This validates end-to-end SMTP deliverability and mobile client rendering without emailing users.
>   3. `COHORT`: Restricts live dispatch to a specified whitelist of internal test IDs (`COHORT_FELLOW_IDS`), serving as a canary deployment.
>   4. `PROD`: Full live production dispatch.
> * **Result**: Complete operational safety, enabling verified canary testing before activating production notifications.

---

### Category 5: Scalability, Performance & Systems Evolution

#### Q10: "If the publication volume scaled by 100x or 1,000x, how would you evolve the architecture?"
> **STAR Answer**:
> * **Situation**: The current system is optimized for batch processing hundreds of videos per day on a single node.
> * **Task**: Architect the system to scale horizontally to handle tens of thousands of video events per hour.
> * **Action**: I would evolve the system across four key architectural dimensions:
>   1. **Decoupled Asynchronous Task Queues**:
>      * Transition from a single-process cron loop to a distributed message queue architecture using **Celery**, **RabbitMQ**, or **AWS SQS / Temporal**.
>      * Split the pipeline into two decoupled workers:
>        * `StatusDetectionWorker`: Detects publication changes.
>        * `NotificationDispatchWorker`: Renders and delivers notifications.
>   2. **Database Concurrency with `SELECT ... FOR UPDATE SKIP LOCKED`**:
>      * Multiple distributed workers can pull batches of queued records from PostgreSQL concurrently without contention or deadlocks using `SKIP LOCKED`.
>   3. **External API Batching**:
>      * The YouTube Data API v3 `videos.list` endpoint supports batching up to 50 video IDs per HTTP call (`id=id1,id2,id3...`). Implementing batching reduces API quota consumption by up to 98%.
>   4. **Event-Driven Webhooks Over Polling**:
>      * Replace polling with YouTube WebSub (PubSubHubbub) webhooks, enabling real-time push notifications upon publication and eliminating polling latency entirely.
> * **Result**: Seamless horizontal scaling with high throughput, low latency, and efficient resource utilization.

#### Q11: "What would you do differently if you were to redesign this system from scratch today?"
> **STAR Answer**:
> * **Situation**: Reflecting on architectural decisions is crucial for continuous engineering improvement.
> * **Action**:
>   1. **Event-Driven Architecture from Day One**: Rather than scheduled polling, implement a webhook ingestion endpoint backed by an event log (such as Kafka or AWS EventBridge) for instant event dispatch.
>   2. **OpenTelemetry & Distributed Tracing**: Add structured distributed tracing (tracing context across API calls and database transactions) and export Prometheus metrics (`pipeline_duration_seconds`, `notifications_sent_total`, `retry_exhausted_total`) to Grafana.
>   3. **Dynamic Template Management**: Decouple email HTML templates from the application repository using an external template service or S3 bucket, allowing marketing/editorial updates without code deployments.

---

### Category 6: Behavioral & Engineering Leadership Questions

#### Q12: "Tell me about a time you had to balance engineering perfection with practical delivery timelines."
> **STAR Answer**:
> * **Situation**: When designing the synchronization engine, I considered implementing a distributed consensus system using Redis and Celery. However, our actual workload was hundreds of videos per day, not millions per second.
> * **Task**: Deliver an ultra-reliable, bug-free solution within a two-week sprint without over-engineering infrastructure.
> * **Action**: I opted for an elegant, low-overhead approach:
>   * Implemented a robust file-based execution lock with TTL rather than standing up and maintaining a Redis cluster.
>   * Designed a Two-Phase Commit state machine in PostgreSQL rather than a complex distributed saga orchestrator.
>   * Abstracted all components behind clean interfaces (Ports & Adapters) so that scaling out to distributed queues in the future would require changing only adapters, without rewriting business logic.
> * **Result**: Delivered the system ahead of schedule with zero external infrastructure maintenance costs, while maintaining architectural extensibility.

#### Q13: "Tell me about a time you identified a flaw in a proposed approach and pushed for a better solution."
> **STAR Answer**:
> * **Situation**: The initial team proposal was to write a quick Google Apps Script embedded directly inside the tracking Google Sheet to send emails via Gmail triggers.
> * **Task**: Advocate for a robust engineering approach that protects data integrity and user trust.
> * **Action**:
>   * I highlighted the structural limitations of Apps Script: silent failure modes under execution timeouts (6-minute limit), lack of automated regression testing, inability to handle race conditions, and lack of audit logging.
>   * I created a rapid proof-of-concept demonstrating how a Python service with SQLite/Postgres provided deterministic idempotency, automated tests, and reproducible local development.
>   * I demonstrated how the 4-tier rollout mode (`REDIRECTED`, `DRY_RUN`) allowed internal stakeholders to review email rendering before live sends.
> * **Result**: The team enthusiastically adopted the standalone architecture, which prevented numerous potential duplicate email incidents and established an auditable single source of truth.

---

## 4. Technical Deep-Dive & Live Code Walkthrough Guide

If an interviewer asks you to share your screen, explain your code, or conduct a system walkthrough:

```
notification-pipeline/
├── src/
│   ├── adapters/          # External I/O (Ports & Adapters Pattern)
│   │   ├── drive_adapter.py    # Google Drive API interface & mocks
│   │   ├── email_adapter.py    # Multi-mode email dispatcher (Dry-run, Redirected, Prod)
│   │   ├── normalizer.py       # Regex sanitization for YouTube/Drive URLs
│   │   └── youtube_adapter.py  # YouTube Data API v3 client & mock simulator
│   ├── models/            # Domain & Storage Models
│   │   ├── domain.py           # Pure business dataclasses
│   │   └── orm.py              # SQLAlchemy 3NF models with SQL CheckConstraints
│   ├── pipeline/          # Core Business Logic Engine
│   │   ├── dispatcher.py       # Template rendering & email routing
│   │   ├── engine.py           # Reconciliation engine & Two-Phase State Machine
│   │   ├── lock.py             # Atomic cross-process file lock with TTL
│   │   └── recovery.py         # Retry service & stranded job recovery
│   ├── storage/           # Persistence & Ingestion
│   │   ├── repository.py       # Repository Pattern for database operations
│   │   └── seeder.py           # Idempotent spreadsheet/Excel ingestion
│   ├── cli.py             # Operational CLI (reconcile, status, doctor, retry)
│   └── config.py          # Environment settings & runtime mode validation
└── tests/                 # Automated Test Suite (24 tests, 2.2s execution)
```

### Key Modules to Showcase in a Technical Interview:

#### 1. Two-Phase Commit State Machine (`src/pipeline/engine.py` $\rightarrow$ `_process_single_video`)
* **Show the Idempotency Check**:
  ```python
  existing = self.repo.get_active_or_sent_notification(video.video_id)
  if existing:
      logger.info(f"Notification already active/sent for video {video.video_id}")
      return False
  ```
* **Show the Pre-Dispatch State Commit**:
  ```python
  # Phase 1: Pre-commit SENDING to persistent disk BEFORE touching network
  notif = self.repo.create_notification(
      video_id=video.video_id,
      delivery_status=DeliveryStatus.SENDING,
      recipient_email=fellow.email
  )
  self.repo.commit()
  ```
* **Show the Fault-Isolated Post-Commit**:
  ```python
  # Phase 2: Dispatch and commit terminal state
  try:
      self.dispatcher.send_publication_notification(...)
      self.repo.update_notification_status(notif.notification_id, DeliveryStatus.SENT)
      self.repo.update_video_status(video.video_id, VideoStatus.PUBLISHED)
      self.repo.commit()
  except Exception as e:
      self.repo.update_notification_status(notif.notification_id, DeliveryStatus.FAILED, error=str(e))
      self.repo.commit()
  ```

#### 2. Cross-Process `ExecutionLock` (`src/pipeline/lock.py`)
* Explain atomic file creation via `os.O_CREAT | os.O_EXCL`.
* Explain the TTL calculation preventing deadlock from terminated worker processes.

#### 3. Relational Schema & Integrity (`src/models/orm.py`)
* Point out foreign key integrity: `VideoRecordORM.fellow_id -> FellowORM.fellow_id`.
* Point out composite indexing: `Index("idx_notif_video_status", "video_id", "delivery_status")`.
* Show SQL `CheckConstraint` on domain status columns.

#### 4. Live Terminal Demonstration in PowerShell
Execute these commands live to showcase operational excellence and software quality:

```powershell
# 1. Display operational status and pipeline statistics
python -m src.cli status

# 2. Run diagnostic health check to verify zero stranded jobs
python -m src.cli doctor

# 3. Run the full automated pytest suite (24 passing tests in ~2.2s)
.venv\Scripts\pytest tests/ -v
```

> **Why this impresses interviewers**: Demonstrating a live CLI with automated health diagnostics (`doctor`) and a clean, 24-test passing test suite running in ~2 seconds immediately establishes senior-level engineering discipline, system design maturity, and software craftsmanship.
