<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f5f7fb">
    <title>Datasets  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261009-research-ui1">
</head>
<body class="app-page">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="main-content">
        <header class="topbar">
            <button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button>
            <div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong>Datasets</strong></div>
            <div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div>
        </header>
        <div class="page-wrap">
            <section class="welcome-row project-welcome">
                <div><p class="eyebrow">YOUR RESEARCH INPUTS</p><h1>Datasets</h1><p class="page-intro">Upload a CSV, check its quality profile, and keep it attached to the right project.</p></div>
                <details class="create-project" <c:if test="${empty selectedDataset and not empty projects}">open</c:if>>
                    <summary class="button button-primary"><span>+</span> Upload CSV</summary>
                    <form class="project-form dataset-form" method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/datasets">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                        <input type="hidden" name="action" value="upload"/>
                        <div class="form-heading"><p class="eyebrow">ADD A DATASET</p><h2>Bring in your data</h2></div>
                        <label for="dataset-file">CSV file <span>10 MB max</span></label>
                        <input id="dataset-file" name="datasetFile" type="file" accept=".csv,text/csv" required>
                        <label for="dataset-name">Dataset name</label>
                        <input id="dataset-name" name="name" type="text" maxlength="160" placeholder="Defaults to the file name">
                        <label for="dataset-project">Project</label>
                        <select id="dataset-project" name="projectId" required>
                            <option value="">Choose an active project</option>
                            <c:forEach var="project" items="${projects}">
                                <option value="<c:out value='${project.id}'/>"><c:out value="${project.title}"/></option>
                            </c:forEach>
                        </select>
                        <label for="dataset-description">Description <span>Optional</span></label>
                        <textarea id="dataset-description" name="description" rows="2" maxlength="1000" placeholder="Source, purpose, or version notes"></textarea>
                        <div class="upload-note"><span aria-hidden="true">AI</span><p>CSV only  |  Up to 50 columns and 50,000 rows  |  A profile is generated when the file is saved.</p></div>
                        <div class="form-actions"><button class="button button-primary" type="submit">Upload and profile</button></div>
                    </form>
                </details>
            </section>

            <c:if test="${param.notice eq 'uploaded'}"><div class="alert alert-success" role="status">Dataset uploaded and profiled successfully.</div></c:if>
            <c:if test="${param.notice eq 'updated'}"><div class="alert alert-success" role="status">Dataset details updated.</div></c:if>
            <c:if test="${param.notice eq 'deleted'}"><div class="alert alert-success" role="status">Dataset removed from this workspace.</div></c:if>
            <c:if test="${param.error eq 'size'}"><div class="alert alert-error" role="alert">Choose a non-empty CSV file smaller than 10 MB.</div></c:if>
            <c:if test="${param.error eq 'format'}"><div class="alert alert-error" role="alert">This upload accepts CSV files only.</div></c:if>
            <c:if test="${param.error eq 'csv'}"><div class="alert alert-error" role="alert">The CSV upload could not be read. Check the file and try again.</div></c:if>
            <c:if test="${param.error eq 'csv-empty'}"><div class="alert alert-error" role="alert">The CSV is empty. Add a header row and data, then upload it again.</div></c:if>
            <c:if test="${param.error eq 'csv-header'}"><div class="alert alert-error" role="alert">Check the header row: every column needs a name, and header names must be unique.</div></c:if>
            <c:if test="${param.error eq 'csv-rows'}"><div class="alert alert-error" role="alert">Each data row must have the same number of values as the header.</div></c:if>
            <c:if test="${param.error eq 'csv-quotes'}"><div class="alert alert-error" role="alert">Check the CSV quotation marks. A quoted value may be missing a closing quote.</div></c:if>
            <c:if test="${param.error eq 'csv-limit'}"><div class="alert alert-error" role="alert">The CSV exceeds the supported row or column limit.</div></c:if>
            <c:if test="${param.error eq 'csv-invalid'}"><div class="alert alert-error" role="alert">We couldn’t read this CSV. Check its formatting and try again.</div></c:if>
            <c:if test="${param.error eq 'project'}"><div class="alert alert-error" role="alert">Select an active project you can access.</div></c:if>
            <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Check the dataset name and description, then try again.</div></c:if>
            <c:if test="${param.error eq 'metadata'}"><div class="alert alert-error" role="alert">Use a dataset name of 2–160 characters and a description no longer than 1,000 characters.</div></c:if>

            <section class="project-summary-row" aria-label="Dataset totals">
                <div class="project-summary"><span class="summary-icon summary-icon-blue">DS</span><span><strong><c:out value="${datasets.size()}"/></strong><small>Visible datasets</small></span></div>
                <div class="project-summary"><span class="summary-icon summary-icon-green">PR</span><span><strong><c:out value="${activeProjectCount}"/></strong><small>Active projects</small></span></div>
                <div class="project-summary"><span class="summary-icon summary-icon-purple">AI</span><span><strong>CSV profile</strong><small>Types, missing and distinct values</small></span></div>
            </section>

            <c:if test="${not empty selectedDataset}">
                <section class="panel dataset-detail-panel">
                    <div class="detail-header">
                        <a class="back-link" href="${pageContext.request.contextPath}/datasets">&lt; All datasets</a>
                        <div class="detail-title-row"><span class="dataset-file-icon">CSV</span><div><p class="eyebrow">DATASET PROFILE</p><h2><c:out value="${selectedDataset.name}"/></h2><p><c:out value="${selectedDataset.projectTitle}"/>  |  <c:out value="${selectedDataset.originalFilename}"/></p></div></div>
                    </div>
                    <div class="profile-stats">
                        <div><strong><c:out value="${selectedDataset.rowCount}"/></strong><span>data rows</span></div>
                        <div><strong><c:out value="${selectedDataset.columnCount}"/></strong><span>columns</span></div>
                        <div><strong><c:out value="${selectedDataset.formattedSize}"/></strong><span>file size</span></div>
                        <div><strong><c:out value="${selectedDataset.uploadedByName}"/></strong><span>uploaded by</span></div>
                    </div>
                    <c:if test="${isAdmin or selectedDataset.uploadedById eq currentUser.id}">
                        <details class="dataset-edit-panel"><summary><span class="dataset-edit-mark">ED</span><span><strong>Edit dataset details</strong><small>Update its display name or research notes.</small></span></summary>
                            <form class="dataset-edit-form" method="post" action="${pageContext.request.contextPath}/datasets"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="update"/><input type="hidden" name="datasetId" value="<c:out value='${selectedDataset.id}'/>"/><label>Dataset name<input name="name" value="<c:out value='${selectedDataset.name}'/>" required minlength="2" maxlength="160"/></label><label>Research notes<textarea name="description" rows="2" maxlength="1000"><c:out value="${selectedDataset.description}"/></textarea></label><button class="button button-secondary" type="submit">Save details</button></form>
                        </details>
                    </c:if>
                    <c:if test="${not empty selectedDataset.description}"><p class="dataset-description"><c:out value="${selectedDataset.description}"/></p></c:if>
                    <div class="panel-heading profile-heading"><div><p class="eyebrow">COLUMN-LEVEL INSPECTION</p><h2>Data profile</h2></div><span class="quiet-label">Inferred from uploaded CSV</span></div>
                    <div class="table-scroll">
                        <table class="data-table profile-table">
                            <thead><tr><th>FIELD</th><th>TYPE</th><th>MISSING</th><th>DISTINCT</th><th>SAMPLE VALUES</th></tr></thead>
                            <tbody><c:forEach var="column" items="${selectedDataset.columns}"><tr><td><strong class="profile-column-name"><c:out value="${column.name}"/></strong></td><td><span class="type-pill"><c:out value="${column.inferredType}"/></span></td><td><c:out value="${column.missingCount}"/></td><td><c:out value="${column.distinctCount}"/></td><td class="sample-values"><c:out value="${column.sampleValues}"/></td></tr></c:forEach></tbody>
                        </table>
                    </div>
                </section>
            </c:if>

            <section class="panel project-list-panel dataset-list-panel">
                <div class="panel-heading project-list-heading">
                    <div><p class="eyebrow">DATA CATALOG</p><h2><c:choose><c:when test="${not empty selectedDataset}">Other datasets</c:when><c:otherwise>Uploaded datasets</c:otherwise></c:choose></h2></div>
                    <span class="quiet-label"><c:out value="${datasets.size()}"/> total</span>
                </div>
                <c:choose>
                    <c:when test="${empty datasets}">
                        <div class="project-empty"><span class="empty-mark" aria-hidden="true">DS</span><h3>No datasets yet</h3><p>Upload a CSV to create a data profile and make it available to your project members.</p></div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll">
                            <table class="data-table">
                                <thead><tr><th>DATASET</th><th>PROJECT</th><th>ROWS / FIELDS</th><th>SIZE</th><th>UPLOADED BY</th><th>ACTIONS</th></tr></thead>
                                <tbody><c:forEach var="dataset" items="${datasets}">
                                    <tr>
                                        <td><div class="project-name-cell"><span class="dataset-file-icon dataset-file-icon-small">CSV</span><span><strong><c:out value="${dataset.name}"/></strong><small><c:out value="${dataset.originalFilename}"/></small></span></div></td>
                                        <td><span class="owner-name"><c:out value="${dataset.projectTitle}"/></span></td>
                                        <td><span class="table-count"><c:out value="${dataset.rowCount}"/> rows</span> <span class="table-count"><c:out value="${dataset.columnCount}"/> cols</span></td>
                                        <td><c:out value="${dataset.formattedSize}"/></td>
                                        <td><span class="owner-name"><c:out value="${dataset.uploadedByName}"/></span></td>
                                        <td class="table-actions"><a class="text-action" href="${pageContext.request.contextPath}/datasets?view=${dataset.id}">View profile</a>
                                            <c:if test="${isAdmin or dataset.uploadedById eq currentUser.id}">
                                                <form method="post" action="${pageContext.request.contextPath}/datasets" data-confirm="Delete this dataset and its saved profile?">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="delete"/><input type="hidden" name="datasetId" value="<c:out value='${dataset.id}'/>"/>
                                                    <button class="text-action text-action-muted" type="submit">Delete</button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach></tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Uploads are stored locally and excluded from the source repository.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
