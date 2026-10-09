# DataHive: Build Roadmap

## 1. Product goal

Build a responsive Java web application for AI research teams to organize projects, datasets, model-training runs, experiments, and collaboration. Administrators manage platform users, research projects, compute/storage resources, and usage reports. Researchers manage datasets, training, experiments, collaboration, and their profiles.

The primary demo should show one complete research journey: a researcher opens a project, uploads a CSV, reviews its profile, starts a real baseline training run, and inspects the saved status, logs, and metrics. The admin can then see the project and resource-usage summary.

**Build status (8 October 2026):** Java/JSP application foundation, authentication, projects, CSV upload/profiling, admin user/resource/project management, usage recording, baseline training, experiment logs/metrics, project collaboration, and profile updates are present in the source tree. The eight-slide editable presentation is at `presentation/DataHive-AI-Research-Platform-v2.pptx` and has been rendered, visually reviewed, and structurally/layout validated. Maven 3.9.16 packaged the Java 21 WAR successfully; tests were skipped. Tomcat runtime review, live UI screenshots, and repository-access/submission checks remain to do. Treat the application as unverified until it is launched.

## 2. Scope and implementation assumptions

- One person is implementing the application. Keep the build modular, but do not plan work as if there are multiple coders.
- Target the **Java Web** rubric in the brief: solution design (8), Core Java (10), JDBC/database integration (8), and Servlets/web integration (7), for 33 marks total. The separate Java GUI rubric is for a different project type.
- Use a Maven Java web application packaged as a WAR, Jakarta Servlets/JSP, JDBC, a relational database, and responsive HTML/CSS/JavaScript. Select versions compatible with the installed Java runtime and servlet container, and record them in the project configuration and README.
- For the first release, support CSV datasets and one CPU-based baseline training algorithm. Calculate metrics from actual model output; do not show invented training results.
- Resource management in this release records compute/storage resources and usage in DataHive. It does not provision cloud infrastructure.
- The screenshots show a deadline of **10 October 2026 at 11:59 PM**. They do not state a timezone; confirm the submission form's timezone.

## 3. Required user roles and features

### Administrator

1. **User management:** list users; create, edit, deactivate/delete accounts; assign Admin or Researcher role; show a clear success/error result.
2. **Resource management:** list and create/edit/delete compute or storage resource records, including type, capacity, and status.
3. **Project management:** list projects; create, edit, archive/delete projects; assign a researcher or project members.
4. **Usage monitoring:** show usage totals and trends with readable charts and a report view. Derive storage usage from uploaded file sizes and compute usage from recorded training-run duration. Label units and calculations.
5. **Admin dashboard:** user table, resource table, project table, and graphical resource-usage summary.

### Researcher

1. **Dataset management:** upload, list, inspect, and delete CSV datasets. Show row/column counts, column types, missing-value counts, and upload details.
2. **Model training:** select a project dataset and binary target; start the logistic-regression baseline; display queued/running/completed/failed status, progress, and evaluation metrics.
3. **Experiment tracking:** save experiment name, model settings, dataset, run status, logs, and metrics; view the experiment history. Run comparison is a stretch goal.
4. **Collaboration:** add existing users to a project and list project members. Enforce membership when viewing shared datasets and experiments.
5. **Profile management:** view/update name and email; allow password changes with secure password handling.
6. **Researcher dashboard:** dataset table, training jobs and statuses, experiments/logs, collaborative projects and members, and profile entry point.

## 4. Application structure

Keep request handling, business rules, persistence, and presentation separate.

```text
src/main/java/org/datahive/
  config/       database initialization and connection settings
  security/     password hashing
  model/        User, Project, Dataset, Experiment, Resource, and usage models
  dao/          JDBC queries, transactions, and row mapping
  service/      CSV profiling, authentication, dashboards, and training worker
  web/          filters and HTTP Servlet routes
src/main/webapp/
  WEB-INF/views/ JSP pages and shared layouts
  assets/css/   responsive styles and design tokens
  assets/js/    charts and small interactions
src/main/resources/db/
  schema.sql    tables, keys, indexes, constraints, and migration-safe additions
uploads/        local dataset files; excluded from Git
```

Use DAO classes for database operations, service classes for business logic, and Servlets as web controllers. JSPs render data passed by controllers. Use prepared statements, try-with-resources, transactions for multi-step changes, and a consistent error-handling path. Keep credentials outside source control.

## 5. Database design

Create the schema before building feature pages. Use primary keys, foreign keys, unique email constraints, timestamps, and indexes for common project/user lookups.

| Table | Purpose and key relationships |
|---|---|
| `users` | Account, name, unique email, password hash, role, active state, timestamps |
| `projects` | Title, description, owner, lifecycle status, timestamps |
| `project_members` | Project/user membership and project-level role; unique project/user pair |
| `resources` | Admin-managed compute/storage resource records, capacity, type, status |
| `datasets` | Project, uploader, metadata, stored file path, size, row/column counts |
| `dataset_columns` | Per-dataset column name/type and profile statistics |
| `experiments` | Project, dataset, name, model, configuration, creator |
| `training_runs` | Experiment run status, progress, start/end time, seed, failure message |
| `run_metrics` | Metric name/value for a training run |
| `run_logs` | Timestamped training messages and errors |
| `usage_records` | Resource, project/user, usage amount/unit, and timestamp for reports |

Each submitted experiment has a persisted training run with metrics and logs. A follow-up improvement is allowing multiple runs under one experiment for direct comparison. Project membership is the source of truth for shared access.

## 6. Build phases and acceptance criteria

### Phase 1 — Confirm scope and make the application boot

- Check the installed Java/runtime and database; select compatible pinned dependencies.
- Create the Maven web-app skeleton, configuration handling, shared page layout, and navigation.
- Add schema/seed scripts and a database health check.

**Done when:** the app starts locally, renders the shared layout, and can connect to an initialized database using local configuration.

### Phase 2 — Accounts, sessions, roles, and projects

- Implement login/logout and session handling.
- Add authentication and role filters; check authorization on every protected request.
- Add project create/list/detail flows and project membership.
- Add admin user and project management screens.

**Done when:** Admin and Researcher accounts see the correct navigation, and direct requests to unauthorized routes are rejected.

### Phase 3 — Dataset upload and profiling

- Accept validated CSV files with a documented maximum size.
- Generate safe server-side file names; keep upload paths out of user control.
- Parse the CSV, validate headers/rows, calculate profile statistics, and persist metadata.
- Add dataset list/detail/edit/delete pages with useful validation messages.

**Done when:** a researcher uploads a CSV into a project, sees its stored profile, and can manage it from the dashboard.

### Phase 4 — Experiments and actual model training

- Create an experiment with dataset, model, and parameters.
- Run one baseline model on a deterministic train/test split using actual dataset values.
- Execute longer runs through a bounded background worker; persist status/progress so the browser can refresh safely.
- Store run configuration, logs, completion/failure state, duration, and calculated metrics.
- Add run detail view; direct run comparison remains a stretch goal.

**Done when:** the user can start a run, see its lifecycle, and inspect metrics that came from the training calculation. Errors appear as a failed run with a useful message.

### Phase 5 — Collaboration, profile, resources, and usage

- Finish researcher profile update and project-member management.
- Add admin resource CRUD and status display.
- Record storage bytes and training duration as usage data; render dashboard totals and trends.
- Enforce project membership checks for shared records.

**Done when:** each screenshot-listed feature has a working page and persists or derives its data correctly.

### Phase 6 — Visual polish and submission

- Apply one visual system: consistent typography, spacing, colors, forms, tables, status badges, and chart labels.
- Make the key pages responsive; include loading, empty, success, and failure states.
- Seed realistic demo records without committing private data or uploaded files.
- Expand README with prerequisites, configuration, database initialization, start/run steps, demo credentials, and known limitations.
- **Completed:** prepare an editable eight-slide PPT at `presentation/DataHive-AI-Research-Platform-v2.pptx`, with workflow, architecture and schema diagrams, training baseline, rubric mapping, and demo order. Capture genuine application screenshots after successful Tomcat verification; the current deck explicitly marks those as pending.
- Ensure the GitHub repository is public or reviewer-accessible; submit its correct URL with the presentation before the deadline.

**Done when:** a reviewer can follow the README, launch the app, sign in with the demo roles, complete the core journey, and understand the architecture from the slides.

## 7. One-builder time plan

The screenshot deadline is close, so implement the phases in dependency order and preserve time for a working demo and submission.

| Date | Focus | Required outcome |
|---|---|---|
| **8 Oct** | Scope, project skeleton, schema, database connection, authentication/roles, shared layout | App boots; seeded Admin/Researcher login; role-protected pages |
| **9 Oct** | Projects, CSV upload/profile, experiment configuration, baseline training and saved metrics | Complete researcher journey works end to end |
| **10 Oct** | Admin resource/usage pages, collaboration/profile completion, responsive polish, README, slides, GitHub access, demo rehearsal | Required feature checklist and submission package complete before 11:59 PM |

If time slips, keep the required pages and flows minimal but functional. Defer optional prediction APIs, multiple model algorithms, email invitations, cloud provisioning, direct run comparison, and elaborate report exports until the required features, demo, README, and presentation are ready.

## 8. Interface and demo checklist

- Sign-in page and role-specific navigation.
- Admin overview with user/project/resource tables and usage charts.
- Researcher overview with datasets, training jobs, experiments, project members, and profile link.
- Dataset upload form and profile detail page.
- Experiment creation form, run status, logs, metrics, and comparison view.
- Collaboration/member list and profile form.
- Consistent responsive layout, keyboard-usable forms, clear field errors, and no dead-end buttons.

**Demo order:** sign in as Researcher → open/create project → upload CSV → inspect profile → configure/start training → inspect run logs and metrics → compare run → show shared project members → sign in as Admin → show users, resources, projects, and usage.

## 9. Rubric and deliverable crosswalk

| Screenshot requirement | Evidence in DataHive |
|---|---|
| Problem understanding and solution design | User-role needs, architecture diagram, schema diagram, complete demo workflow |
| Core Java | Models/services, collections for tabular/profile data, validation, exceptions, and bounded background training worker |
| Database integration (JDBC) | Connection/configuration layer, DAO classes, prepared statements, relational schema, persisted workflow data |
| Servlets and web integration | Servlet routes, authentication/role filters, JSP views, form validation, responsive browser UI |
| Admin/researcher functions and dashboards | Feature checklist in Section 3 |
| Presentation and GitHub submission | PDF/PPT visuals, accessible repository, README setup/run guide, correct submission link |
| Code quality | Modular packages, clear names/comments where useful, consistent error handling, configuration kept out of source control |

## 10. Scope boundaries and honest reporting

Do not claim cloud compute was provisioned when DataHive only records resource metadata. Label any seeded sample data as demo data. Do not present metrics as trained results unless the training code calculated them. Credit contributors according to the work they actually performed.
