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

- Windows 10 or 11, PowerShell, and an internet connection for first-time setup.
- No separate Java, Maven, or Tomcat installation is required on Windows. The launcher downloads Java 21, Apache Maven, and Apache Tomcat into the ignored `.tools/` folder and verifies the downloaded archives before installing them.

## Run locally

### Windows one-click setup

Double-click `manage-datahive.bat` and choose **Set up, build, and run**. On first run it downloads Java 21, Apache Maven 3.10.0, and Apache Tomcat 10.1.60, verifies the archives, builds the WAR, deploys it to the bundled Tomcat, and starts the app. The first setup needs an internet connection and may take several minutes. Later runs reuse the downloaded tools. Downloads are stored in `.tools/` and are not added to Git.

Open `http://localhost:8080/datahive/` when the launcher reports that Tomcat is starting. Use **Stop DataHive** in the same menu to stop it. Port 8080 must be free; if another Tomcat or app is using it, stop that server before running DataHive.

### Manual setup on other platforms

Install JDK 21 or newer, Maven 3.9+, and Apache Tomcat 10.1+, then build the WAR from the project folder:

```sh
mvn clean package
```

Copy `target/datahive.war` into Tomcat's `webapps` folder, start Tomcat, and open `http://localhost:8080/datahive/`.

For a fresh local database, sign in with one of the demonstration accounts:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@datahive.local` | `AdminPass123!` |
| Researcher | `researcher@datahive.local` | `ResearcherPass123!` |

The demo accounts are inserted only when the database is empty. A clearly labeled, deterministic synthetic churn dataset is also created for the demo researcher on first initialization, so the training flow can be shown immediately. These known credentials are for local review only: do not expose them on a public deployment, and replace or remove them before deployment. The launcher downloads tools under `.tools/`; the file database is written under `data/`, and uploaded CSVs are written under `uploads/`. All three locations are ignored by Git.

## Demo walkthrough

1. Sign in as the Researcher and open **Datasets**. Open **Synthetic churn baseline** to review its 160-row profile and column types.
2. Open **Experiments**, choose `churned` as the binary target, select the numeric input columns, name the experiment, and start training.
3. Open the run detail to see queue/run progress, timestamped logs, and held-out accuracy, precision, recall, and F1. The experiment list shows saved results.
4. Open **Collaboration** to review or manage a project team. The demo project begins with its researcher owner; an Administrator can create additional real accounts.
5. Select two to four completed runs to compare their held-out metrics. Sign in as Admin to manage accounts and resource records, review the project inventory, and change the usage report window.

## Configuration

By default, the application uses a local H2 file database. Optional environment variables:

- `DATAHIVE_JDBC_URL`
- `DATAHIVE_DB_USER`
- `DATAHIVE_DB_PASSWORD`
- `DATAHIVE_DATA_DIR`
- `DATAHIVE_UPLOAD_DIR`
- `DATAHIVE_DEMO_MODE` (`true` by default; set to `false` to skip seeded demo accounts and data)
- `DATAHIVE_ADMIN_INVITE_CODE` (optional; required for public self-registration of Admin accounts)
- `DATAHIVE_GOOGLE_CLIENT_ID` (optional; enables Google Identity Services on sign-in and sign-up)

`DATAHIVE_JDBC_URL` may point to a supported H2 JDBC URL. By default, DataHive stores its H2 database and uploads under the current user's `.datahive` directory, independent of Tomcat's working directory. Set `DATAHIVE_DATA_DIR` or `DATAHIVE_UPLOAD_DIR` to choose other writable folders. Use the local demo accounts only for a local review; the demo setup is not production account provisioning.

Public account creation is available at `/register`. New Researcher accounts receive a personal research workspace automatically. Admin self-registration is denied unless `DATAHIVE_ADMIN_INVITE_CODE` is set to a private invite value. Google sign-in is optional: create a Google OAuth client of type **Web application**, add the app's origin (for local Tomcat, `http://localhost:8080`) as an authorized JavaScript origin, set `DATAHIVE_GOOGLE_CLIENT_ID` in the Tomcat environment, then restart Tomcat. Google ID tokens are verified on the server against Google's published signing keys, audience, issuer, expiry, verified-email claim, and a per-session nonce. No Google client secret is needed for this GIS credential flow. Existing accounts are matched by verified email; the Google role selector never changes an existing account's role.

### Configure Admin self-registration on Windows

Admin registration is intentionally protected by an invite code. Choose a private value and save it in your Windows user environment from PowerShell:

```powershell
setx DATAHIVE_ADMIN_INVITE_CODE "replace-with-your-private-code"
```

Replace the example value with your own code. Close and reopen the terminal, then restart Tomcat (or run `manage-datahive.bat start`) so the server receives the new setting. Select **Admin** on `/register` and enter the same code. Share it only with people who should be allowed to create administrator accounts. The invite code is not included in this README; never commit it to the repository. Researcher registration does not require an invite code.

Do not commit real credentials or uploaded datasets. For public deployment, use HTTPS, set secure session-cookie options, and configure production credentials outside the repository.

## Deploy to Render

The repository includes a Dockerfile for a Render **Web Service**. In Render, choose **New > Web Service**, connect the GitHub repository, select **Docker** as the runtime, and deploy from the repository root. Render builds the WAR and starts Tomcat on the port supplied by Render. The Docker image sets `DATAHIVE_DEMO_MODE=false`, so it does not create the public demo Admin account with its documented password.

In the service's **Environment** settings, add `DATAHIVE_ADMIN_INVITE_CODE` with a long, private value. After the first deploy, open your service's `/register` page and create your Admin account using that invite code. You may also add `DATAHIVE_GOOGLE_CLIENT_ID`; if you enable Google sign-in, add your Render service origin (for example, `https://your-service.onrender.com`) to the OAuth client's authorized JavaScript origins.

The default Render filesystem is ephemeral. For a short demo, the H2 database and uploads will work but can be lost when the service restarts or redeploys. To keep them, attach a **paid persistent disk** at `/var/data` and set `DATAHIVE_DATA_DIR=/var/data/db` and `DATAHIVE_UPLOAD_DIR=/var/data/uploads` in the service environment. Free Render web services cannot attach persistent disks and may spin down when idle. See [Render Docker deployment](https://render.com/docs/docker), [persistent disks](https://render.com/docs/disks), and [free service limitations](https://render.com/docs/free).

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
