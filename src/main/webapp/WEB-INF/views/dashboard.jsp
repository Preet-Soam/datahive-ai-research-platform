<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f5f7fb">
    <title>Overview  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261009-research-dashboard2">
</head>
<body class="app-page">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>

    <main class="main-content">
        <header class="topbar">
            <button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button>
            <div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong>Overview</strong></div>
            <div class="topbar-actions">
                <span class="environment-pill"><span></span> LOCAL WORKSPACE</span>
                <button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button>
                <button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button>
            </div>
        </header>

        <div class="page-wrap">
            <section class="welcome-row">
                <div>
                    <p class="eyebrow">YOUR RESEARCH, IN ONE PLACE</p>
                    <h1>Good to see you, <c:out value="${firstName}"/> <span class="wave" aria-hidden="true">AI</span></h1>
                    <p class="page-intro">Here's what's happening across your <c:out value="${roleLabel}"/> workspace.</p>
                </div>
                <div class="date-chip"><span class="date-icon">NOW</span><span>Research workspace</span></div>
            </section>

            <section class="metric-grid" aria-label="Workspace summary">
                <c:forEach var="metric" items="${summary.metrics}" varStatus="loop">
                    <article class="metric-card metric-accent-${loop.index + 1}">
                        <div class="metric-card-top"><span class="metric-label"><c:out value="${metric.label}"/></span><span class="metric-icon"><c:out value="${metric.icon}"/></span></div>
                        <div class="metric-value"><c:out value="${metric.value}"/></div>
                        <div class="metric-foot"><span class="metric-foot-dot"></span><span><c:out value="${metric.hint}"/></span></div>
                    </article>
                </c:forEach>
            </section>

            <section class="content-grid">
                <article class="panel activity-panel">
                    <div class="panel-heading">
                        <div><p class="eyebrow">WORKSPACE PULSE</p><h2><c:choose><c:when test="${isAdmin}">Platform activity</c:when><c:otherwise>Research activity</c:otherwise></c:choose></h2></div>
                        <span class="panel-period">Latest five</span>
                    </div>
                    <div class="dashboard-activity-list">
                        <c:choose><c:when test="${empty summary.recentActivity}"><div class="activity-zero"><span class="empty-mark" aria-hidden="true">AI</span><h3>Your workspace is ready</h3><p>Projects, uploads, and training runs will appear here as they are created.</p><a class="text-action" href="${pageContext.request.contextPath}/projects">Open projects -&gt;</a></div></c:when><c:otherwise>
                            <c:forEach var="activity" items="${summary.recentActivity}"><article class="dashboard-event"><span class="dashboard-event-mark">></span><div><strong><c:out value="${activity.type}"/>  |  <c:out value="${activity.title}"/></strong><p><c:out value="${activity.detail}"/></p></div><time><c:out value="${activity.createdAt}"/></time></article></c:forEach>
                        </c:otherwise></c:choose>
                    </div>
                </article>

                <article class="panel side-panel">
                    <div class="panel-heading">
                        <div><p class="eyebrow">GETTING STARTED</p><h2>Research flow</h2></div>
                        <span class="flow-counter"><c:out value="${summary.projectCount}"/> <span>project<c:if test="${summary.projectCount ne 1}">s</c:if></span></span>
                    </div>
                    <ol class="flow-list">
                        <li class="flow-step"><span class="flow-step-number">01</span><span class="flow-step-copy"><strong>Choose a project</strong><small>Keep datasets and runs together</small></span><span class="flow-step-status flow-done" aria-label="Ready">OK</span></li>
                        <li class="flow-step"><span class="flow-step-number">02</span><span class="flow-step-copy"><strong>Add a dataset</strong><small>Upload and inspect your data</small></span><span class="flow-step-status">02</span></li>
                        <li class="flow-step"><span class="flow-step-number">03</span><span class="flow-step-copy"><strong>Run an experiment</strong><small>Track settings, progress, results</small></span><span class="flow-step-status">03</span></li>
                    </ol>
                    <div class="privacy-note"><span class="privacy-icon" aria-hidden="true">AI</span><p><strong>Project-scoped access</strong><br>Datasets and experiment records are shared with project members.</p></div>
                </article>
            </section>

            <section class="below-grid">
                <article class="panel recent-panel">
                    <div class="panel-heading">
                        <div><p class="eyebrow">WORKSPACE TOOLS</p><h2>Continue your research</h2></div>
                        <span class="quiet-label">Quick links</span>
                    </div>
                    <div class="dashboard-shortcuts"><a href="${pageContext.request.contextPath}/projects"><span class="shortcut-icon">PR</span><span><strong>Projects</strong><small>Create or open a workspace</small></span><b>-&gt;</b></a><a href="${pageContext.request.contextPath}/datasets"><span class="shortcut-icon shortcut-green">DS</span><span><strong>Datasets</strong><small>Upload or inspect a CSV</small></span><b>-&gt;</b></a><a href="${pageContext.request.contextPath}/experiments"><span class="shortcut-icon shortcut-purple">EX</span><span><strong>Experiments</strong><small>Train and review model runs</small></span><b>-&gt;</b></a><a href="${pageContext.request.contextPath}/collaboration"><span class="shortcut-icon shortcut-green">CO</span><span><strong>Collaboration</strong><small>Manage project teams</small></span><b>-&gt;</b></a></div>
                </article>
                <article class="panel role-card">
                    <div class="role-card-badge"><c:choose><c:when test="${isAdmin}">A</c:when><c:otherwise>R</c:otherwise></c:choose></div>
                    <div><p class="eyebrow">SIGNED IN AS</p><h2><c:out value="${roleLabel}"/></h2><p><c:choose><c:when test="${isAdmin}">Platform administration and research resources.</c:when><c:otherwise>Dataset management, model training, and collaboration.</c:otherwise></c:choose></p></div>
                </article>
            </section>

            <c:choose>
                <c:when test="${isAdmin}">
                    <section class="dashboard-feature-grid" aria-label="Administration dashboard lists">
                        <article class="panel dashboard-data-panel">
                            <div class="panel-heading"><div><p class="eyebrow">ACCESS CONTROL</p><h2>User management</h2></div><a class="text-action" href="${pageContext.request.contextPath}/admin?tab=users">Manage users -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>ACCOUNT</th><th>ROLE</th><th>ACCESS</th></tr></thead><tbody>
                                <c:forEach var="account" items="${adminUsers}"><tr><td><strong class="profile-column-name"><c:out value="${account.fullName}"/></strong><br><span class="quiet-label"><c:out value="${account.email}"/></span></td><td><c:out value="${account.role}"/></td><td><c:choose><c:when test="${account.active}">Active</c:when><c:otherwise>Inactive</c:otherwise></c:choose></td></tr></c:forEach>
                                <c:if test="${empty adminUsers}"><tr><td colspan="3"><div class="inline-empty">No accounts are registered.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                        <article class="panel dashboard-data-panel">
                            <div class="panel-heading"><div><p class="eyebrow">COMPUTE &amp; STORAGE</p><h2>Resource management</h2></div><a class="text-action" href="${pageContext.request.contextPath}/admin?tab=resources">Manage resources -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>RESOURCE</th><th>TYPE</th><th>CAPACITY</th><th>STATE</th></tr></thead><tbody>
                                <c:forEach var="resource" items="${adminResources}"><tr><td><strong class="profile-column-name"><c:out value="${resource.name}"/></strong></td><td><c:out value="${resource.type}"/></td><td><c:out value="${resource.capacityLabel}"/></td><td><c:out value="${resource.statusLabel}"/></td></tr></c:forEach>
                                <c:if test="${empty adminResources}"><tr><td colspan="4"><div class="inline-empty">No resource records are available.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                        <article class="panel dashboard-data-panel dashboard-data-wide">
                            <div class="panel-heading"><div><p class="eyebrow">RESEARCH WORKSPACES</p><h2>Project management</h2></div><a class="text-action" href="${pageContext.request.contextPath}/admin?tab=projects">Review projects -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>PROJECT</th><th>OWNER</th><th>TEAM</th><th>DATASETS</th><th>EXPERIMENTS</th><th>STATUS</th></tr></thead><tbody>
                                <c:forEach var="project" items="${dashboardProjects}"><tr><td><strong class="profile-column-name"><c:out value="${project.title}"/></strong></td><td><c:out value="${project.ownerName}"/></td><td><c:out value="${project.memberCount}"/></td><td><c:out value="${project.datasetCount}"/></td><td><c:out value="${project.experimentCount}"/></td><td><c:out value="${project.statusLabel}"/></td></tr></c:forEach>
                                <c:if test="${empty dashboardProjects}"><tr><td colspan="6"><div class="inline-empty">No research projects yet.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                    </section>
                </c:when>
                <c:otherwise>
                    <section class="dashboard-feature-grid" aria-label="Researcher dashboard lists">
                        <article class="panel dashboard-data-panel">
                            <div class="panel-heading"><div><p class="eyebrow">DATASET MANAGEMENT</p><h2>Uploaded datasets</h2></div><a class="text-action" href="${pageContext.request.contextPath}/datasets">Open datasets -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>DATASET</th><th>PROJECT</th><th>ROWS</th><th>COLUMNS</th></tr></thead><tbody>
                                <c:forEach var="dataset" items="${dashboardDatasets}"><tr><td><strong class="profile-column-name"><c:out value="${dataset.name}"/></strong><br><span class="quiet-label"><c:out value="${dataset.formattedSize}"/></span></td><td><c:out value="${dataset.projectTitle}"/></td><td><c:out value="${dataset.rowCount}"/></td><td><c:out value="${dataset.columnCount}"/></td></tr></c:forEach>
                                <c:if test="${empty dashboardDatasets}"><tr><td colspan="4"><div class="inline-empty">Upload a CSV dataset to begin a research run.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                        <article class="panel dashboard-data-panel">
                            <div class="panel-heading"><div><p class="eyebrow">MODEL TRAINING</p><h2>Recent training jobs</h2></div><a class="text-action" href="${pageContext.request.contextPath}/experiments">Open experiments -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>EXPERIMENT</th><th>MODEL</th><th>STATUS</th><th>ACCURACY</th></tr></thead><tbody>
                                <c:forEach var="run" items="${dashboardRuns}"><tr><td><a class="text-action" href="${pageContext.request.contextPath}/experiments?view=<c:out value='${run.id}'/>"><c:out value="${run.name}"/></a><br><span class="quiet-label"><c:out value="${run.datasetName}"/></span></td><td><c:out value="${run.modelName}"/></td><td><span class="status-badge status-<c:out value='${run.statusLabel}'/>"><span></span><c:out value="${run.statusLabel}"/></span><c:if test="${run.live}"><div class="mini-progress"><span style="width:<c:out value='${run.progress}'/>%"></span></div></c:if></td><td><c:out value="${run.accuracyPercent}"/></td></tr></c:forEach>
                                <c:if test="${empty dashboardRuns}"><tr><td colspan="4"><div class="inline-empty">Training jobs and saved metrics will appear here.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                        <article class="panel dashboard-data-panel">
                            <div class="panel-heading"><div><p class="eyebrow">COLLABORATION</p><h2>Project teams</h2></div><a class="text-action" href="${pageContext.request.contextPath}/collaboration">Manage teams -&gt;</a></div>
                            <div class="table-scroll"><table class="data-table"><thead><tr><th>PROJECT</th><th>OWNER</th><th>TEAM MEMBERS</th></tr></thead><tbody>
                                <c:forEach var="project" items="${dashboardProjects}"><tr><td><a class="text-action" href="${pageContext.request.contextPath}/collaboration?project=<c:out value='${project.id}'/>"><c:out value="${project.title}"/></a></td><td><c:out value="${project.ownerName}"/></td><td><c:out value="${project.memberCount}"/></td></tr></c:forEach>
                                <c:if test="${empty dashboardProjects}"><tr><td colspan="3"><div class="inline-empty">Create or join a project to collaborate.</div></td></tr></c:if>
                            </tbody></table></div>
                        </article>
                        <article class="panel dashboard-profile-card">
                            <div class="dashboard-profile-avatar"><c:out value="${initials}"/></div><div><p class="eyebrow">PROFILE INFORMATION</p><h2><c:out value="${accountName}"/></h2><p><c:out value="${accountEmail}"/></p><span class="member-role-pill role-researcher">Researcher</span></div><a class="button button-secondary" href="${pageContext.request.contextPath}/profile">Update profile</a>
                        </article>
                    </section>
                </c:otherwise>
            </c:choose>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Thoughtful research starts with a clear workspace.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
