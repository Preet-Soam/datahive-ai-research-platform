# DataHive

DataHive is a Java web application for AI research teams. It brings projects, datasets, model-training runs, experiment tracking, and collaboration into one workspace. Administrators manage users, projects, compute/storage resources, and usage reports; researchers manage datasets, training, experiments, project collaboration, and profiles.

## What is implemented

DataHive now has two role-based workspaces and the main research workflow:

- Admins can create/edit/deactivate accounts, maintain compute/storage resource records, review projects, and see usage reports.
- Researchers can create and manage project workspaces, upload and profile CSV files, manage project teams, and update their profile.
- A researcher can start a real binary logistic-regression baseline on a project CSV. Runs are queued in a bounded local worker, use a deterministic stratified 80/20 holdout, and persist progress, logs, accuracy, precision, recall, F1, and elapsed compute time.
- Uploads and training durations feed the admin usage charts. Resource entries describe the local workspace; DataHive does not provision cloud compute or storage.
- Views use responsive JSP/HTML/CSS with small vanilla JavaScript interactions.

The detailed scope, architecture, schema, feature crosswalk, and remaining presentation/submission checklist are in [ROADMAP.md](ROADMAP.md). The application is running locally on Apache Tomcat 10.1.60 and has been opened in a browser; automated tests have not been run.

## Technology

- Java 21 source target
- Jakarta Servlet 6.0 / JSP 3.1
- Maven WAR packaging
- JDBC with H2 file database for local development
- HTML, CSS, and small vanilla JavaScript interactions

Deploy the WAR to Apache Tomcat 10.1 or another Servlet 6.0 compatible container. Tomcat 10.1 requires Java 11 or later.

## Prerequisites

- JDK 21 or newer
- Maven 3.9+
- Apache Tomcat 10.1+

## Run locally

1. From the repository root, build the WAR:

   ```powershell
   mvn clean package
   ```

2. Copy `target/datahive.war` into Tomcat 10.1's `webapps` folder and start Tomcat.
3. Open `http://localhost:8080/datahive/`.
4. Sign in with one of the local demonstration accounts:

   | Role | Email | Password |
   |---|---|---|
   | Admin | `admin@datahive.local` | `AdminPass123!` |
   | Researcher | `researcher@datahive.local` | `ResearcherPass123!` |

The demo accounts are inserted only when the database is empty. A clearly labeled, deterministic synthetic churn dataset is also created for the demo researcher on first initialization, so the training flow can be shown immediately. The accounts and data are for local review only; change/remove them before any public deployment. The file database is written under `data/`, and uploaded CSVs are written under `uploads/`; both locations are ignored by Git.

## Demo walkthrough

1. Sign in as the Researcher and open **Datasets**. Open **Synthetic churn baseline** to review its 160-row profile and column types.
2. Open **Experiments**, use `churned` as the binary target header, name the experiment, and start training. Other numeric columns become model features automatically.
3. Open the run detail to see queue/run progress, timestamped logs, and held-out accuracy, precision, recall, and F1. The experiment list shows saved results.
4. Open **Collaboration** to review or manage a project team. The demo project begins with its researcher owner; an Administrator can create additional real accounts.
5. Sign in as Admin to manage accounts and resource records, review the project inventory, and change the usage report window.

## Configuration

By default, the application uses a local H2 file database. Optional environment variables:

- `DATAHIVE_JDBC_URL`
- `DATAHIVE_DB_USER`
- `DATAHIVE_DB_PASSWORD`
- `DATAHIVE_DATA_DIR`
- `DATAHIVE_UPLOAD_DIR`

`DATAHIVE_JDBC_URL` may point to a supported H2 JDBC URL. By default, DataHive stores its H2 database and uploads under the current user's `.datahive` directory, independent of Tomcat's working directory. Set `DATAHIVE_DATA_DIR` or `DATAHIVE_UPLOAD_DIR` to choose other writable folders. Use the local demo accounts only for a local review; the demo setup is not production account provisioning.

Do not commit real credentials or uploaded datasets. For public deployment, use HTTPS, set secure session-cookie options, and configure production credentials outside the repository.

## Project layout

```text
src/main/java/org/datahive/     Java models, JDBC DAOs, services, filters, and servlets
src/main/resources/db/          Relational schema and indexes
src/main/webapp/WEB-INF/views/  JSP pages and shared sidebar
src/main/webapp/assets/         Responsive CSS and browser interactions
data/                           Local H2 files (ignored by Git)
uploads/                        Server-named local CSVs (ignored by Git)
```

## Training baseline

The first training workflow is intentionally bounded and explainable. It accepts CSVs up to 10 MiB, 50 columns, and 50,000 data rows. Choose a binary target header; all other columns inferred as integers or decimals are used as features. Missing feature values are imputed from the training split, features are standardized, and a fixed-seed logistic-regression model runs for 60, 120, or 180 epochs at a selected learning rate. Evaluation metrics are calculated on a stratified holdout set. Each experiment stores its configuration, queue/run state, timestamped log, and metrics. The local worker runs at most two jobs at once and queues up to twelve more. Admin reporting offers a 3, 6, or 12-month window for recorded run time and uploaded CSV bytes.

This is a demo baseline, not an AI platform execution service: there is no GPU/cloud provisioning, artifact registry, arbitrary Python model execution, or production-grade multi-tenant file storage.

## Presentation and submission

An editable eight-slide presentation is available at [`presentation/DataHive-AI-Research-Platform-v2.pptx`](presentation/DataHive-AI-Research-Platform-v2.pptx). It covers the research workflow, roles, application architecture, relational data model, training baseline, Java Web rubric mapping, and demo sequence. The deck has been rendered and visually reviewed; structural and slide-layout validation found no issues.

The live WAR/Tomcat launch has not yet been verified, so the deck does not pretend to show application screenshots. Capture real browser screenshots after a successful local launch, then:

- Make the GitHub repository public or grant reviewer access.
- Confirm the repository URL and presentation open from the submission form.
- The brief shows a deadline of 10 October 2026 at 11:59 PM; confirm the submission form's timezone.
