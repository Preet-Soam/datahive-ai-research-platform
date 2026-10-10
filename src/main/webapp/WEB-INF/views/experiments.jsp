<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#f5f7fb">
    <title>Experiments  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261010-polish2">
</head>
<body class="app-page" data-run-live="<c:out value='${not empty selectedExperiment and selectedExperiment.live}'/>">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="main-content">
        <header class="topbar"><button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button><div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong><c:choose><c:when test="${isAdmin}">Model oversight</c:when><c:otherwise>Experiments</c:otherwise></c:choose></strong></div><div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div></header>
        <div class="page-wrap">
            <section class="welcome-row"><div><p class="eyebrow"><c:choose><c:when test="${isAdmin}">PLATFORM MONITORING</c:when><c:otherwise>TRAIN  |  MEASURE  |  REPEAT</c:otherwise></c:choose></p><h1><c:choose><c:when test="${isAdmin}">Model oversight</c:when><c:otherwise>Experiments</c:otherwise></c:choose></h1><p class="page-intro"><c:choose><c:when test="${isAdmin}">Review platform-wide research runs, results, and logs. Researchers submit and own model training runs.</c:when><c:otherwise>Train reproducible supervised classification models on project datasets, review held-out results, and compare experiments.</c:otherwise></c:choose></p></div><span class="model-chip"><c:choose><c:when test="${isAdmin}">READ-ONLY OVERSIGHT</c:when><c:otherwise>LOCAL CPU  |  CLASSIFICATION</c:otherwise></c:choose></span></section>

            <c:if test="${param.notice eq 'queued' and param.backend eq 'remote'}"><div class="alert alert-success" role="status">The private dataset upload and Hugging Face Job were queued. This page will show the Job link, status, and results as they arrive.</div></c:if>
            <c:if test="${param.notice eq 'queued' and param.backend ne 'remote'}"><div class="alert alert-success" role="status">The training run was added to the local queue. Its status and results will update here.</div></c:if>
            <c:if test="${param.notice eq 'deleted'}"><div class="alert alert-success" role="status">The finished experiment and its saved run history were deleted.</div></c:if>
            <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Enter a valid experiment name, choose a dataset, and provide the exact CSV target header.</div></c:if>
            <c:if test="${param.error eq 'features'}"><div class="alert alert-error" role="alert">Choose a valid target column and at least one numeric input feature. The target must contain exactly two classes.</div></c:if>
            <c:if test="${param.error eq 'project'}"><div class="alert alert-error" role="alert">Training requires a dataset attached to an active project you can access.</div></c:if>
            <c:if test="${param.error eq 'queue'}"><div class="alert alert-error" role="alert">The training queue is busy. Try again after a run finishes.</div></c:if>
            <c:if test="${param.error eq 'trainingData' and not empty trainingPreflightError}"><div class="alert alert-error" role="alert"><strong>This dataset is not ready for training:</strong> <c:out value="${trainingPreflightError}"/>. Update the CSV or adjust the target and features, then try again.</div></c:if>
            <c:if test="${param.error eq 'remoteConfiguration' and not empty trainingPreflightError}"><div class="alert alert-error" role="alert"><strong>Hugging Face Jobs is not ready:</strong> <c:out value="${trainingPreflightError}"/></div></c:if>

            <section class="experiment-layout <c:if test='${empty selectedExperiment or isAdmin}'>experiment-layout-single</c:if>">
                <c:if test="${not isAdmin}">
                <article class="panel experiment-launch-panel">
                    <div class="panel-heading"><div><p class="eyebrow">NEW RUN</p><h2>Configure a model experiment</h2></div><span class="experiment-model-icon">ML(x)</span></div>
                    <p class="experiment-lead">Choose a local CPU baseline or prepare a private Hugging Face Jobs run. DataHive validates the dataset, target, and numeric features before any training starts and records reproducible run details.</p>
                    <form method="post" action="${pageContext.request.contextPath}/experiments" class="experiment-form">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                        <label for="experiment-name">Experiment name</label><input id="experiment-name" name="name" required minlength="3" maxlength="160" placeholder="e.g. Churn baseline  |  v1">
                        <label for="execution-backend">Training location</label><select id="execution-backend" name="executionBackend" required><option value="LOCAL_CPU">Local CPU · run now in this workspace</option><option value="HUGGINGFACE_JOBS">Hugging Face Jobs · private remote training</option></select>
                        <div class="remote-training-note" data-remote-training-note>
                            <c:choose><c:when test="${huggingFaceStatus.ready}"><strong>Hugging Face is configured</strong><span>Starting a remote run uploads this CSV to a private dataset repository and uses Hugging Face compute. The repository remains in your Hub account until you delete it.</span></c:when><c:otherwise><strong>Hugging Face needs deployment setup</strong><span><c:out value="${huggingFaceStatus.message}"/></span></c:otherwise></c:choose>
                        </div>
                        <label for="model-type">Classification model</label><select id="model-type" name="modelType" required><option value="LOGISTIC_REGRESSION">Logistic regression · two classes</option><option value="DECISION_TREE">Decision tree · two or more classes</option></select>
                        <p class="field-hint" data-model-hint>Logistic regression learns feature weights with gradient descent. Use it when your target has exactly two classes.</p>
                        <label for="experiment-dataset">Project dataset</label>
                        <select id="experiment-dataset" name="datasetId" required>
                            <option value="">Choose a CSV dataset</option>
                            <c:forEach var="dataset" items="${datasets}"><option value="<c:out value='${dataset.id}'/>"><c:out value="${dataset.name}"/>  |  <c:out value="${dataset.projectTitle}"/></option></c:forEach>
                        </select>
                        <label for="target-column">What should the model predict?</label><select id="target-column" name="targetColumn" required disabled><option value="">Choose a dataset first</option></select>
                        <p class="field-hint" data-target-hint>Choose the label column. Logistic regression requires exactly two classes; a decision tree can classify two or more. Numeric fields can be selected below as model inputs.</p>
                        <fieldset class="feature-picker" aria-describedby="feature-picker-hint"><legend>Model input features</legend><p class="feature-picker-hint" id="feature-picker-hint" data-feature-hint>Select a dataset to see its numeric columns. All eligible columns start selected.</p><div class="feature-picker-list" data-feature-list></div></fieldset>
                        <div class="training-parameter-grid" data-logistic-parameters><div><label for="training-epochs">Training epochs</label><select id="training-epochs" name="epochs"><option value="60">60  |  quick</option><option value="120" selected>120  |  balanced</option><option value="180">180  |  extended</option></select></div><div><label for="learning-rate">Learning rate</label><select id="learning-rate" name="learningRate"><option value="0.05">0.05  |  cautious</option><option value="0.12" selected>0.12  |  balanced</option><option value="0.2">0.20  |  faster</option></select></div></div>
                        <div class="experiment-method-note"><span>01</span><p><strong>80/20 stratified split</strong><small>Each class is represented in train and holdout data.</small></p></div>
                        <div class="experiment-method-note"><span>02</span><p data-reproducibility-note><strong>Seed 42</strong><small>Repeating the same setup keeps the split deterministic.</small></p></div>
                        <div class="form-actions"><a class="button button-secondary" href="${pageContext.request.contextPath}/datasets">Inspect datasets</a><button class="button button-primary" type="submit" <c:if test="${empty datasets}">disabled</c:if>>Start training <span>-&gt;</span></button></div>
                    </form>
                    <c:if test="${empty datasets}"><p class="experiment-empty-note">Upload a CSV dataset first to enable training.</p></c:if>
                </article>
                </c:if>

                <c:if test="${not empty selectedExperiment}">
                    <article class="panel experiment-detail-panel">
                        <div class="panel-heading"><div><p class="eyebrow">RUN DETAIL  |  #<c:out value="${selectedExperiment.runId}"/></p><h2><c:out value="${selectedExperiment.name}"/></h2></div><div class="table-actions"><a class="back-link" href="${pageContext.request.contextPath}/experiments">Close detail</a><c:if test="${not selectedExperiment.live and (isAdmin or selectedExperiment.createdById eq currentUser.id)}"><form method="post" action="${pageContext.request.contextPath}/experiments" data-confirm="Permanently delete this finished experiment, its metrics, and run logs?"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="delete"/><input type="hidden" name="experimentId" value="<c:out value='${selectedExperiment.id}'/>"/><button class="text-action text-action-muted" type="submit">Delete run</button></form></c:if></div></div>
                        <div class="experiment-meta"><span><c:out value="${selectedExperiment.projectTitle}"/></span><span><c:out value="${selectedExperiment.datasetName}"/></span><span>Target: <strong><c:out value="${selectedExperiment.targetColumn}"/></strong></span><c:choose><c:when test="${selectedExperiment.modelName eq 'Binary logistic regression'}"><span><c:out value="${selectedExperiment.epochs}"/> epochs</span><span>Learning rate <c:out value="${selectedExperiment.learningRateLabel}"/></span></c:when><c:otherwise><span>Maximum tree depth: 6</span><span>Macro-averaged metrics</span></c:otherwise></c:choose></div>
                        <div class="run-progress-row"><span class="status-badge status-<c:out value='${selectedExperiment.statusLabel}'/>"><span></span><c:out value="${selectedExperiment.statusLabel}"/></span><strong><c:out value="${selectedExperiment.progress}"/>%</strong></div>
                        <div class="progress-track"><span style="width:<c:out value='${selectedExperiment.progress}'/>%"></span></div>
                        <c:if test="${not empty selectedExperiment.remoteJobUrl}"><p class="remote-job-link"><span>REMOTE COMPUTE</span><a href="<c:out value='${selectedExperiment.remoteJobUrl}'/>" target="_blank" rel="noopener noreferrer">Open Hugging Face Job <span aria-hidden="true">↗</span></a></p></c:if>
                        <c:if test="${selectedExperiment.status eq 'FAILED'}">
                            <div class="alert alert-error" role="alert">
                                <strong>Training failed.</strong>
                                <c:forEach var="log" items="${selectedExperiment.logs}">
                                    <c:if test="${log.levelLabel eq 'error'}">
                                        <p><c:out value="${log.message}"/></p>
                                    </c:if>
                                </c:forEach>
                                <p>Correct the issue and start a new run.</p>
                            </div>
                        </c:if>
                        <c:if test="${selectedExperiment.status eq 'COMPLETED'}"><div class="run-metric-grid"><div><strong><c:out value="${selectedExperiment.accuracyPercent}"/></strong><span>Accuracy</span></div><div><strong><c:out value="${selectedExperiment.f1Percent}"/></strong><span><c:choose><c:when test="${not empty selectedExperiment.evaluation}">Macro F1</c:when><c:otherwise>F1 score</c:otherwise></c:choose></span></div><div><strong><c:out value="${selectedExperiment.precisionPercent}"/></strong><span><c:choose><c:when test="${not empty selectedExperiment.evaluation}">Macro precision</c:when><c:otherwise>Precision</c:otherwise></c:choose></span></div><div><strong><c:out value="${selectedExperiment.recallPercent}"/></strong><span><c:choose><c:when test="${not empty selectedExperiment.evaluation}">Macro recall</c:when><c:otherwise>Recall</c:otherwise></c:choose></span></div></div>
                            <c:choose><c:when test="${not empty selectedExperiment.evaluation}">
                                <section class="evaluation-panel" aria-labelledby="evaluation-title">
                                    <div class="evaluation-heading"><div><p class="eyebrow">HELD-OUT EVALUATION</p><h3 id="evaluation-title">How the model performed</h3></div><span class="evaluation-split"><c:out value="${selectedExperiment.evaluation.trainRows}"/> train · <c:out value="${selectedExperiment.evaluation.testRows}"/> test rows</span></div>
                                    <p class="evaluation-context">The model is evaluated on rows excluded from fitting. Macro scores weight each class equally; the baseline always predicts the most common class in the training split.</p>
                                    <div class="evaluation-baseline"><span>Majority-class baseline</span><strong><c:out value="${selectedExperiment.evaluation.baselineAccuracyPercent}"/></strong><span>Model accuracy</span><strong><c:out value="${selectedExperiment.evaluation.accuracyPercent}"/></strong></div>
                                    <div class="evaluation-columns">
                                        <div class="evaluation-matrix-wrap"><h4>Confusion matrix</h4><p>Rows are actual labels; columns are predicted labels.</p><div class="table-scroll"><table class="evaluation-matrix"><thead><tr><th>Actual ↓ / Predicted →</th><c:forEach var="label" items="${selectedExperiment.evaluation.labels}"><th><c:out value="${label}"/></th></c:forEach></tr></thead><tbody><c:forEach var="row" items="${selectedExperiment.evaluation.matrixRows}"><tr><th><c:out value="${row.actualLabel}"/></th><c:forEach var="cell" items="${row.cells}"><td class="<c:if test='${cell.correct}'>is-correct</c:if>"><c:out value="${cell.count}"/></td></c:forEach></tr></c:forEach></tbody></table></div></div>
                                        <div class="evaluation-class-wrap"><h4>Per-class performance</h4><div class="table-scroll"><table class="evaluation-class-table"><thead><tr><th>CLASS</th><th>SUPPORT</th><th>PRECISION</th><th>RECALL</th><th>F1</th></tr></thead><tbody><c:forEach var="metric" items="${selectedExperiment.evaluation.classes}"><tr><th><c:out value="${metric.label}"/></th><td><c:out value="${metric.support}"/></td><td><c:out value="${metric.precisionPercent}"/></td><td><c:out value="${metric.recallPercent}"/></td><td><strong><c:out value="${metric.f1Percent}"/></strong></td></tr></c:forEach></tbody></table></div></div>
                                    </div>
                                </section>
                            </c:when><c:otherwise><div class="evaluation-unavailable">Detailed holdout evidence is recorded for new runs. This earlier run only has its summary metrics.</div></c:otherwise></c:choose>
                            <p class="form-actions"><a class="button button-secondary" href="${pageContext.request.contextPath}/experiments?download=report&amp;view=<c:out value='${selectedExperiment.id}'/>">Download run report</a></p></c:if>
                        <div class="run-log-heading"><h3>Run log</h3><span>Recorded status and progress</span></div>
                        <ol class="run-log-list"><c:forEach var="log" items="${selectedExperiment.logs}"><li class="log-${log.levelLabel}"><time><c:out value="${log.createdAt}"/></time><span><c:out value="${log.message}"/></span></li></c:forEach></ol>
                    </article>
                </c:if>
            </section>

            <c:if test="${not isAdmin}"><div class="training-dataset-catalog" data-dataset-catalog hidden><c:forEach var="dataset" items="${datasets}"><div data-training-dataset="<c:out value='${dataset.id}'/>"><c:forEach var="column" items="${dataset.columns}"><span data-column-name="<c:out value='${column.name}'/>" data-column-type="<c:out value='${column.inferredType}'/>"></span></c:forEach></div></c:forEach></div></c:if>

            <c:if test="${not empty comparisonError}"><div class="alert alert-error comparison-alert" role="alert"><c:out value="${comparisonError}"/></div></c:if>
            <c:if test="${not empty comparisonExperiments}">
                <section class="panel comparison-panel" aria-labelledby="comparison-title">
                    <div class="panel-heading project-list-heading"><div><p class="eyebrow">RUN ANALYSIS</p><h2 id="comparison-title">Compare completed runs</h2></div><a class="back-link" href="${pageContext.request.contextPath}/experiments">Close comparison</a></div>
                    <p class="comparison-caption">Metrics come from each run's held-out split. Compare matching datasets and targets; older logistic-regression runs may use positive-class scores, while new runs report macro-averaged class scores.</p>
                    <div class="table-scroll"><table class="data-table comparison-table"><thead><tr><th>MEASURE</th><c:forEach var="run" items="${comparisonExperiments}"><th><c:out value="${run.name}"/><small>Run #<c:out value="${run.runId}"/></small></th></c:forEach></tr></thead><tbody>
                        <tr><th>Dataset</th><c:forEach var="run" items="${comparisonExperiments}"><td><c:out value="${run.datasetName}"/></td></c:forEach></tr>
                        <tr><th>Target</th><c:forEach var="run" items="${comparisonExperiments}"><td><c:out value="${run.targetColumn}"/></td></c:forEach></tr>
                        <tr><th>Training setup</th><c:forEach var="run" items="${comparisonExperiments}"><td><c:choose><c:when test="${run.modelName eq 'Binary logistic regression'}"><c:out value="${run.epochs}"/> epochs<br><span class="quiet-label">LR <c:out value="${run.learningRateLabel}"/></span></c:when><c:otherwise>Depth-limited CART<br><span class="quiet-label">Macro metrics</span></c:otherwise></c:choose></td></c:forEach></tr>
                        <tr><th>Accuracy</th><c:forEach var="run" items="${comparisonExperiments}"><td><strong><c:out value="${run.accuracyPercent}"/></strong></td></c:forEach></tr>
                        <tr><th>Precision</th><c:forEach var="run" items="${comparisonExperiments}"><td><strong><c:out value="${run.precisionPercent}"/></strong></td></c:forEach></tr>
                        <tr><th>Recall</th><c:forEach var="run" items="${comparisonExperiments}"><td><strong><c:out value="${run.recallPercent}"/></strong></td></c:forEach></tr>
                        <tr><th>F1 score</th><c:forEach var="run" items="${comparisonExperiments}"><td><strong><c:out value="${run.f1Percent}"/></strong></td></c:forEach></tr>
                    </tbody></table></div>
                </section>
            </c:if>

            <section class="panel project-list-panel experiment-list-panel">
                <div class="panel-heading project-list-heading"><div><p class="eyebrow"><c:choose><c:when test="${isAdmin}">PLATFORM RESEARCH ACTIVITY</c:when><c:otherwise>EXPERIMENT TRACKING</c:otherwise></c:choose></p><h2><c:choose><c:when test="${isAdmin}">Research runs</c:when><c:otherwise>Training runs</c:otherwise></c:choose></h2></div><span class="quiet-label"><c:out value="${experiments.size()}"/> saved</span></div>
                <c:choose><c:when test="${empty experiments}"><div class="project-empty"><span class="empty-mark">ML</span><h3>No experiments yet</h3><p><c:choose><c:when test="${isAdmin}">Researcher training runs will appear here for platform oversight.</c:when><c:otherwise>Start a baseline run to create a persistent experiment, status history, metrics, and run log.</c:otherwise></c:choose></p></div></c:when><c:otherwise>
                    <form id="experiment-comparison-form" method="get" action="${pageContext.request.contextPath}/experiments"><input type="hidden" name="compare" value="selected"></form><div class="comparison-toolbar"><span>Select 2–4 completed runs to compare their evaluation metrics.</span><button class="button button-secondary" type="submit" form="experiment-comparison-form">Compare selected</button></div>
                    <div class="table-scroll"><table class="data-table"><thead><tr><th><span class="visually-hidden">SELECT</span></th><th>EXPERIMENT</th><th>PROJECT / DATASET</th><th>MODEL / TARGET</th><th>STATUS</th><th>ACCURACY</th><th>F1</th><th>DETAIL</th><th>ACTION</th></tr></thead><tbody>
                        <c:forEach var="experiment" items="${experiments}"><tr>
                            <td><c:if test="${experiment.status eq 'COMPLETED'}"><label class="comparison-select"><input form="experiment-comparison-form" type="checkbox" name="compareId" value="<c:out value='${experiment.id}'/>" aria-label="Select <c:out value='${experiment.name}'/> for comparison"><span class="visually-hidden">Select for comparison</span></label></c:if></td>
                            <td><div class="project-name-cell"><span class="experiment-table-icon">ML</span><span><strong><c:out value="${experiment.name}"/></strong><small>Run #<c:out value="${experiment.runId}"/>  |  <c:out value="${experiment.createdBy}"/></small></span></div></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.projectTitle}"/></strong><br><span class="quiet-label"><c:out value="${experiment.datasetName}"/></span></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.modelName}"/></strong><br><span class="quiet-label">Target: <c:out value="${experiment.targetColumn}"/></span></td>
                            <td><span class="status-badge status-<c:out value='${experiment.statusLabel}'/>"><span></span><c:out value="${experiment.statusLabel}"/></span><c:if test="${experiment.status eq 'RUNNING' or experiment.status eq 'QUEUED'}"><div class="mini-progress"><span style="width:<c:out value='${experiment.progress}'/>%"></span></div></c:if></td>
                            <td><c:out value="${experiment.accuracyPercent}"/></td><td><c:out value="${experiment.f1Percent}"/></td>
                            <td><a class="text-action" href="${pageContext.request.contextPath}/experiments?view=<c:out value='${experiment.id}'/>">Run log</a></td>
                            <td><c:if test="${not experiment.live and (isAdmin or experiment.createdById eq currentUser.id)}"><form method="post" action="${pageContext.request.contextPath}/experiments" data-confirm="Permanently delete this finished experiment, its metrics, and run logs?"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="delete"/><input type="hidden" name="experimentId" value="<c:out value='${experiment.id}'/>"/><button class="text-action text-action-muted" type="submit">Delete</button></form></c:if></td>
                        </tr></c:forEach>
                    </tbody></table></div>
                </c:otherwise></c:choose>
            </section>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Metrics come from a deterministic held-out split of the selected CSV.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
<script src="${pageContext.request.contextPath}/assets/js/training-config.js?v=20261009-model-selector2" defer></script>
</body>
</html>
