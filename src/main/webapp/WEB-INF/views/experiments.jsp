<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#f5f7fb">
    <title>Experiments  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261008-photo-menu4">
</head>
<body class="app-page" data-run-live="<c:out value='${not empty selectedExperiment and selectedExperiment.live}'/>">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="main-content">
        <header class="topbar"><button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button><div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong>Experiments</strong></div><div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div></header>
        <div class="page-wrap">
            <section class="welcome-row"><div><p class="eyebrow">TRAIN  |  MEASURE  |  REPEAT</p><h1>Experiments</h1><p class="page-intro">Train a reproducible baseline on a project dataset, then review its held-out metrics and run log.</p></div><span class="model-chip">CPU  |  BINARY CLASSIFICATION</span></section>

            <c:if test="${param.notice eq 'queued'}"><div class="alert alert-success" role="status">The training run was added to the local queue. Its status and results will update here.</div></c:if>
            <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Enter a valid experiment name, choose a dataset, and provide the exact CSV target header.</div></c:if>
            <c:if test="${param.error eq 'features'}"><div class="alert alert-error" role="alert">That target header was not found, or the dataset has no other numeric feature columns.</div></c:if>
            <c:if test="${param.error eq 'project'}"><div class="alert alert-error" role="alert">Training requires a dataset attached to an active project you can access.</div></c:if>
            <c:if test="${param.error eq 'queue'}"><div class="alert alert-error" role="alert">The training queue is busy. Try again after a run finishes.</div></c:if>

            <section class="experiment-layout <c:if test='${empty selectedExperiment}'>experiment-layout-single</c:if>">
                <article class="panel experiment-launch-panel">
                    <div class="panel-heading"><div><p class="eyebrow">NEW RUN</p><h2>Binary logistic regression</h2></div><span class="experiment-model-icon">ML(x)</span></div>
                    <p class="experiment-lead">A real, CPU-based baseline with fixed seed, stratified holdout, standardized numeric features, and missing-value imputation.</p>
                    <form method="post" action="${pageContext.request.contextPath}/experiments" class="experiment-form">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                        <label for="experiment-name">Experiment name</label><input id="experiment-name" name="name" required minlength="3" maxlength="160" placeholder="e.g. Churn baseline  |  v1">
                        <label for="experiment-dataset">Project dataset</label>
                        <select id="experiment-dataset" name="datasetId" required>
                            <option value="">Choose a CSV dataset</option>
                            <c:forEach var="dataset" items="${datasets}"><option value="<c:out value='${dataset.id}'/>"><c:out value="${dataset.name}"/>  |  <c:out value="${dataset.projectTitle}"/></option></c:forEach>
                        </select>
                        <label for="target-column">Binary target header</label><input id="target-column" name="targetColumn" required maxlength="190" placeholder="Exact column name, e.g. churned">
                        <p class="field-hint">The target must have exactly two classes. Every other numeric column becomes a feature; open a dataset profile to check exact headers.</p>
                        <div class="training-parameter-grid"><div><label for="training-epochs">Training epochs</label><select id="training-epochs" name="epochs"><option value="60">60  |  quick</option><option value="120" selected>120  |  balanced</option><option value="180">180  |  extended</option></select></div><div><label for="learning-rate">Learning rate</label><select id="learning-rate" name="learningRate"><option value="0.05">0.05  |  cautious</option><option value="0.12" selected>0.12  |  balanced</option><option value="0.2">0.20  |  faster</option></select></div></div>
                        <div class="experiment-method-note"><span>01</span><p><strong>80/20 stratified split</strong><small>Each class is represented in train and holdout data.</small></p></div>
                        <div class="experiment-method-note"><span>02</span><p><strong>120 training epochs</strong><small>Seed 42 makes the split and training reproducible.</small></p></div>
                        <div class="form-actions"><a class="button button-secondary" href="${pageContext.request.contextPath}/datasets">Inspect datasets</a><button class="button button-primary" type="submit" <c:if test="${empty datasets}">disabled</c:if>>Start training <span>-&gt;</span></button></div>
                    </form>
                    <c:if test="${empty datasets}"><p class="experiment-empty-note">Upload a CSV dataset first to enable training.</p></c:if>
                </article>

                <c:if test="${not empty selectedExperiment}">
                    <article class="panel experiment-detail-panel">
                        <div class="panel-heading"><div><p class="eyebrow">RUN DETAIL  |  #<c:out value="${selectedExperiment.runId}"/></p><h2><c:out value="${selectedExperiment.name}"/></h2></div><a class="back-link" href="${pageContext.request.contextPath}/experiments">Close detail</a></div>
                        <div class="experiment-meta"><span><c:out value="${selectedExperiment.projectTitle}"/></span><span><c:out value="${selectedExperiment.datasetName}"/></span><span>Target: <strong><c:out value="${selectedExperiment.targetColumn}"/></strong></span><span><c:out value="${selectedExperiment.epochs}"/> epochs</span><span>Learning rate <c:out value="${selectedExperiment.learningRateLabel}"/></span></div>
                        <div class="run-progress-row"><span class="status-badge status-<c:out value='${selectedExperiment.statusLabel}'/>"><span></span><c:out value="${selectedExperiment.statusLabel}"/></span><strong><c:out value="${selectedExperiment.progress}"/>%</strong></div>
                        <div class="progress-track"><span style="width:<c:out value='${selectedExperiment.progress}'/>%"></span></div>
                        <c:if test="${selectedExperiment.status eq 'COMPLETED'}"><div class="run-metric-grid"><div><strong><c:out value="${selectedExperiment.accuracyPercent}"/></strong><span>Accuracy</span></div><div><strong><c:out value="${selectedExperiment.f1Percent}"/></strong><span>F1 score</span></div><div><strong><c:out value="${selectedExperiment.precisionPercent}"/></strong><span>Precision</span></div><div><strong><c:out value="${selectedExperiment.recallPercent}"/></strong><span>Recall</span></div></div></c:if>
                        <div class="run-log-heading"><h3>Run log</h3><span>Recorded status and progress</span></div>
                        <ol class="run-log-list"><c:forEach var="log" items="${selectedExperiment.logs}"><li class="log-${log.levelLabel}"><time><c:out value="${log.createdAt}"/></time><span><c:out value="${log.message}"/></span></li></c:forEach></ol>
                    </article>
                </c:if>
            </section>

            <section class="panel project-list-panel experiment-list-panel">
                <div class="panel-heading project-list-heading"><div><p class="eyebrow">EXPERIMENT TRACKING</p><h2>Training runs</h2></div><span class="quiet-label"><c:out value="${experiments.size()}"/> saved</span></div>
                <c:choose><c:when test="${empty experiments}"><div class="project-empty"><span class="empty-mark">ML</span><h3>No experiments yet</h3><p>Start a baseline run to create a persistent experiment, status history, metrics, and run log.</p></div></c:when><c:otherwise>
                    <div class="table-scroll"><table class="data-table"><thead><tr><th>EXPERIMENT</th><th>PROJECT / DATASET</th><th>MODEL / TARGET</th><th>STATUS</th><th>ACCURACY</th><th>F1</th><th>DETAIL</th></tr></thead><tbody>
                        <c:forEach var="experiment" items="${experiments}"><tr>
                            <td><div class="project-name-cell"><span class="experiment-table-icon">ML</span><span><strong><c:out value="${experiment.name}"/></strong><small>Run #<c:out value="${experiment.runId}"/>  |  <c:out value="${experiment.createdBy}"/></small></span></div></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.projectTitle}"/></strong><br><span class="quiet-label"><c:out value="${experiment.datasetName}"/></span></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.modelName}"/></strong><br><span class="quiet-label">Target: <c:out value="${experiment.targetColumn}"/></span></td>
                            <td><span class="status-badge status-<c:out value='${experiment.statusLabel}'/>"><span></span><c:out value="${experiment.statusLabel}"/></span><c:if test="${experiment.status eq 'RUNNING' or experiment.status eq 'QUEUED'}"><div class="mini-progress"><span style="width:<c:out value='${experiment.progress}'/>%"></span></div></c:if></td>
                            <td><c:out value="${experiment.accuracyPercent}"/></td><td><c:out value="${experiment.f1Percent}"/></td>
                            <td><a class="text-action" href="${pageContext.request.contextPath}/experiments?view=<c:out value='${experiment.id}'/>">Run log</a></td>
                        </tr></c:forEach>
                    </tbody></table></div>
                </c:otherwise></c:choose>
            </section>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Metrics come from a deterministic held-out split of the selected CSV.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
