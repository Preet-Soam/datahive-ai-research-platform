<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f5f7fb">
    <title>Projects  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261008-photo-menu4">
</head>
<body class="app-page">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="main-content">
        <header class="topbar">
            <button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button>
            <div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong>Projects</strong></div>
            <div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div>
        </header>

        <div class="page-wrap">
            <section class="welcome-row project-welcome">
                <div><p class="eyebrow">ORGANIZE YOUR RESEARCH</p><h1>Projects</h1><p class="page-intro">Keep datasets, experiments, and collaborators connected to a shared research goal.</p></div>
                <details class="create-project" <c:if test="${not empty editingProject or param.open eq '1'}">open</c:if>>
                    <summary class="button button-primary"><span>+</span> New project</summary>
                    <form class="project-form" method="post" action="${pageContext.request.contextPath}/projects">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                        <input type="hidden" name="action" value="<c:choose><c:when test='${not empty editingProject}'>update</c:when><c:otherwise>create</c:otherwise></c:choose>"/>
                        <c:if test="${not empty editingProject}"><input type="hidden" name="projectId" value="<c:out value='${editingProject.id}'/>"/></c:if>
                        <div class="form-heading"><p class="eyebrow"><c:choose><c:when test="${not empty editingProject}">EDIT WORKSPACE</c:when><c:otherwise>NEW WORKSPACE</c:otherwise></c:choose></p><h2><c:choose><c:when test="${not empty editingProject}">Project details</c:when><c:otherwise>Start a research project</c:otherwise></c:choose></h2></div>
                        <label for="project-title">Project name</label>
                        <input id="project-title" name="title" required minlength="3" maxlength="160" placeholder="e.g. Customer retention research"
                               value="<c:if test='${not empty editingProject}'><c:out value='${editingProject.title}'/></c:if>">
                        <label for="project-description">Description <span>Optional</span></label>
                        <textarea id="project-description" name="description" rows="3" maxlength="1200" placeholder="What question is this team exploring?"><c:if test="${not empty editingProject}"><c:out value="${editingProject.description}"/></c:if></textarea>
                        <c:if test="${isAdmin}">
                            <label for="project-owner">Project owner</label>
                            <select id="project-owner" name="ownerId" required>
                                <option value="">Choose a researcher</option>
                                <c:forEach var="researcher" items="${researchers}">
                                    <option value="<c:out value='${researcher.id}'/>" <c:if test="${not empty editingProject and editingProject.ownerId eq researcher.id}">selected</c:if>><c:out value="${researcher.fullName}"/>  |  <c:out value="${researcher.email}"/></option>
                                </c:forEach>
                            </select>
                        </c:if>
                        <div class="form-actions">
                            <a class="button button-secondary" href="${pageContext.request.contextPath}/projects">Cancel</a>
                            <button class="button button-primary" type="submit"><c:choose><c:when test="${not empty editingProject}">Save changes</c:when><c:otherwise>Create project</c:otherwise></c:choose></button>
                        </div>
                    </form>
                </details>
            </section>

            <c:if test="${param.notice eq 'created'}"><div class="alert alert-success" role="status">Project created and ready for research.</div></c:if>
            <c:if test="${param.notice eq 'updated'}"><div class="alert alert-success" role="status">Project details updated.</div></c:if>
            <c:if test="${param.notice eq 'archived'}"><div class="alert alert-success" role="status">Project archived. Its records remain available for review.</div></c:if>
            <c:if test="${param.notice eq 'deleted'}"><div class="alert alert-success" role="status">Project, its uploaded files, and its saved experiment history were deleted.</div></c:if>
            <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Check the project name and description, then try again.</div></c:if>
            <c:if test="${param.error eq 'owner'}"><div class="alert alert-error" role="alert">Choose an active researcher as the project owner.</div></c:if>
            <c:if test="${param.error eq 'projectBusy'}"><div class="alert alert-error" role="alert">This project still has a queued or running model job. Wait for it to finish before deleting the project.</div></c:if>

            <section class="project-summary-row" aria-label="Project totals">
                <div class="project-summary"><span class="summary-icon summary-icon-blue">PR</span><span><strong><c:out value="${projects.size()}"/></strong><small>Visible projects</small></span></div>
                <div class="project-summary"><span class="summary-icon summary-icon-green">AI</span><span><strong><c:out value="${activeProjectCount}"/></strong><small>Active workspaces</small></span></div>
                <div class="project-summary"><span class="summary-icon summary-icon-purple">CO</span><span><strong>Project scoped</strong><small>Data access</small></span></div>
            </section>

            <section class="panel project-list-panel">
                <div class="panel-heading project-list-heading">
                    <div><p class="eyebrow">RESEARCH WORKSPACES</p><h2>All projects</h2></div>
                    <span class="quiet-label"><c:out value="${projects.size()}"/> total</span>
                </div>
                <c:choose>
                    <c:when test="${empty projects}">
                        <div class="project-empty"><span class="empty-mark" aria-hidden="true">PR</span><h3>No projects yet</h3><p>Create a project to bring datasets, experiments, and collaborators together.</p></div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll">
                            <table class="data-table">
                                <thead><tr><th>PROJECT</th><th>OWNER</th><th>DATASETS</th><th>EXPERIMENTS</th><th>STATUS</th><th class="table-action-heading">ACTIONS</th></tr></thead>
                                <tbody>
                                <c:forEach var="project" items="${projects}">
                                    <tr>
                                        <td><div class="project-name-cell"><span class="project-avatar"><c:out value="${project.initial}"/></span><span><strong><c:out value="${project.title}"/></strong><small><c:out value="${project.description}"/></small></span></div></td>
                                        <td><span class="owner-name"><c:out value="${project.ownerName}"/></span></td>
                                        <td><span class="table-count"><c:out value="${project.datasetCount}"/></span></td>
                                        <td><span class="table-count"><c:out value="${project.experimentCount}"/></span></td>
                                        <td><span class="status-badge <c:choose><c:when test='${project.status eq "ACTIVE"}'>status-active</c:when><c:otherwise>status-archived</c:otherwise></c:choose>"><span></span><c:out value="${project.statusLabel}"/></span></td>
                                        <td class="table-actions">
                                            <c:if test="${project.status eq 'ACTIVE'}"><a class="text-action" href="${pageContext.request.contextPath}/collaboration?project=<c:out value='${project.id}'/>">Team</a></c:if>
                                            <c:if test="${isAdmin or project.ownerId eq currentUser.id}"><a class="text-action" href="${pageContext.request.contextPath}/projects?edit=<c:out value='${project.id}'/>">Edit</a></c:if>
                                            <c:if test="${project.status eq 'ACTIVE' and (isAdmin or project.ownerId eq currentUser.id)}">
                                                <form method="post" action="${pageContext.request.contextPath}/projects" data-confirm="Archive this project? Existing dataset and experiment records will remain stored.">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                                                    <input type="hidden" name="action" value="archive"/>
                                                    <input type="hidden" name="projectId" value="<c:out value='${project.id}'/>"/>
                                                    <button class="text-action text-action-muted" type="submit">Archive</button>
                                                </form>
                                            </c:if>
                                            <c:if test="${isAdmin or project.ownerId eq currentUser.id}">
                                                <form method="post" action="${pageContext.request.contextPath}/projects" data-confirm="Permanently delete this project, all attached datasets, uploaded CSV files, experiments, and run history? This cannot be undone.">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                                                    <input type="hidden" name="action" value="delete"/>
                                                    <input type="hidden" name="projectId" value="<c:out value='${project.id}'/>"/>
                                                    <button class="text-action text-action-muted" type="submit">Delete</button>
                                                </form>
                                            </c:if>
                                            <c:if test="${not isAdmin and project.ownerId ne currentUser.id}"><span class="member-tag">Member</span></c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Projects keep research work organized and accessible to the right people.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
