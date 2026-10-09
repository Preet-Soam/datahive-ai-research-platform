<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#f5f7fb">
    <title>Administration  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261009-admin1">
</head>
<body class="app-page">
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="main-content">
        <header class="topbar"><button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button><div class="breadcrumbs"><span>Platform</span><span class="crumb-divider">/</span><strong>Administration</strong></div><div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div></header>
        <div class="page-wrap">
            <section class="welcome-row admin-welcome"><div><p class="eyebrow"><span class="admin-live-dot"></span>PLATFORM CONTROL</p><h1>Administration</h1><p class="page-intro">One place to manage platform access, research resources, and oversight.</p></div><span class="admin-scope-pill"><span>DH</span> Administrator</span></section>

            <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Check the submitted fields. Names, email addresses, roles, and capacities must be valid.</div></c:if>
            <c:if test="${not empty param.notice}"><div class="alert alert-success" role="status">Your administration changes were saved.</div></c:if>

            <nav class="admin-tabs" aria-label="Administration sections">
                <a class="<c:if test='${activeTab eq "overview"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=overview">Overview</a>
                <a class="<c:if test='${activeTab eq "users"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=users">Users <span><c:out value="${users.size()}"/></span></a>
                <a class="<c:if test='${activeTab eq "resources"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=resources">Resources <span><c:out value="${resources.size()}"/></span></a>
                <a class="<c:if test='${activeTab eq "projects"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=projects">Projects <span><c:out value="${projects.size()}"/></span></a>
                <a class="<c:if test='${activeTab eq "usage"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=usage">Usage reports</a>
                <a class="<c:if test='${activeTab eq "activity"}'>is-current</c:if>" href="${pageContext.request.contextPath}/admin?tab=activity">Activity log</a>
            </nav>

            <c:choose>
                <c:when test="${activeTab eq 'activity'}">
                    <section class="admin-section-header"><div><p class="eyebrow">AUDIT TRAIL</p><h2>Platform activity</h2><p>Recent account, resource, project, dataset, collaboration, and model-training events, with the person who performed each action.</p></div><span class="quiet-label">Latest 100 events</span></section>
                    <section class="panel project-list-panel admin-table-panel"><div class="panel-heading project-list-heading"><div><p class="eyebrow">RECENT EVENTS</p><h2>Who did what</h2></div><span class="quiet-label"><c:out value="${adminActivities.size()}"/> events shown</span></div>
                        <div class="table-scroll"><table class="data-table admin-activity-table"><thead><tr><th>WHEN</th><th>ACTOR</th><th>ACTION</th><th>RECORD</th><th>DETAILS</th></tr></thead><tbody>
                            <c:forEach var="activity" items="${adminActivities}"><tr><td><time class="admin-activity-time"><c:out value="${activity.createdAt}"/></time></td><td><strong class="admin-actor-name"><c:out value="${activity.actorName}"/></strong></td><td><span class="admin-activity-action"><span></span><c:out value="${activity.actionLabel}"/></span></td><td><span class="admin-record-type"><c:out value="${activity.targetType}"/><c:if test="${not empty activity.targetId}"> #<c:out value="${activity.targetId}"/></c:if></span></td><td class="admin-activity-detail"><c:out value="${activity.summary}"/></td></tr></c:forEach>
                            <c:if test="${empty adminActivities}"><tr><td colspan="5"><div class="inline-empty">No activity has been recorded yet. Successful platform actions will appear here.</div></td></tr></c:if>
                        </tbody></table></div>
                    </section>
                </c:when>
                <c:when test="${activeTab eq 'users'}">
                    <section class="admin-section-header"><div><p class="eyebrow">ACCESS CONTROL</p><h2>User management</h2><p>Create accounts, set roles, and deactivate access when needed.</p></div></section>
                    <details class="admin-create-panel">
                        <summary><span class="create-summary-icon">+</span><span><strong>Create user</strong><small>Set the name, email, role, and initial password.</small></span></summary>
                        <form class="admin-form-grid" method="post" action="${pageContext.request.contextPath}/admin?tab=users">
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="createUser"/>
                            <label>Full name<input name="fullName" required minlength="2" maxlength="120" placeholder="Researcher name"></label>
                            <label>Email address<input name="email" type="email" required maxlength="190" placeholder="name@research.org"></label>
                            <label>Role<select name="role"><option value="RESEARCHER">Researcher</option><option value="ADMIN">Admin</option></select></label>
                            <label>Temporary password<input name="password" type="password" required minlength="10" maxlength="200" autocomplete="new-password" placeholder="At least 10 characters"></label>
                            <div class="admin-form-actions"><button class="button button-primary" type="submit">Create account</button></div>
                        </form>
                    </details>
                    <section class="panel project-list-panel admin-table-panel">
                        <div class="panel-heading project-list-heading"><div><p class="eyebrow">PLATFORM ACCOUNTS</p><h2>All users</h2></div><span class="quiet-label"><c:out value="${activeUserCount}"/> active</span></div>
                        <div class="table-scroll"><table class="data-table admin-user-table"><thead><tr><th>USER</th><th>EMAIL</th><th>ROLE</th><th>ACCESS</th><th>ACTION</th></tr></thead><tbody>
                            <c:forEach var="account" items="${users}">
                                <tr>
                                    <td><input form="user-form-${account.id}" class="table-input user-name-input" name="fullName" value="<c:out value='${account.fullName}'/>" required maxlength="120" aria-label="User name"></td>
                                    <td><input form="user-form-${account.id}" class="table-input user-email-input" name="email" type="email" value="<c:out value='${account.email}'/>" required maxlength="190" aria-label="User email"></td>
                                    <td><select form="user-form-${account.id}" class="table-select" name="role" aria-label="User role"><option value="RESEARCHER" <c:if test="${account.role eq 'RESEARCHER'}">selected</c:if>>Researcher</option><option value="ADMIN" <c:if test="${account.role eq 'ADMIN'}">selected</c:if>>Admin</option></select></td>
                                    <td><select form="user-form-${account.id}" class="table-select" name="active" aria-label="Account access"><option value="true" <c:if test="${account.active}">selected</c:if>>Active</option><option value="false" <c:if test="${not account.active}">selected</c:if>>Inactive</option></select></td>
                                    <td><form id="user-form-${account.id}" method="post" action="${pageContext.request.contextPath}/admin?tab=users" class="admin-inline-form"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="updateUser"/><input type="hidden" name="userId" value="<c:out value='${account.id}'/>"/><button class="text-action" type="submit">Save</button></form></td>
                                </tr>
                            </c:forEach>
                        </tbody></table></div>
                    </section>
                </c:when>
                <c:when test="${activeTab eq 'resources'}">
                    <section class="admin-section-header"><div><p class="eyebrow">COMPUTE &amp; STORAGE</p><h2>Resource management</h2><p>Track resources available to research projects. These records do not provision cloud services.</p></div></section>
                    <details class="admin-create-panel">
                        <summary><span class="create-summary-icon">+</span><span><strong>Add a resource</strong><small>Register a compute or storage resource with its capacity and state.</small></span></summary>
                        <form class="admin-form-grid resource-create-form" method="post" action="${pageContext.request.contextPath}/admin?tab=resources">
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="createResource"/>
                            <label>Resource name<input name="name" required minlength="2" maxlength="140" placeholder="Research CPU pool"></label>
                            <label>Type<select name="type"><option value="COMPUTE">Compute</option><option value="STORAGE">Storage</option></select></label>
                            <label>Capacity<input name="capacity" type="number" min="0" step="0.01" value="1" required></label>
                            <label>Unit<input name="unit" maxlength="24" value="vCPU" required placeholder="vCPU, GB, TB"></label>
                            <label>Status<select name="status"><option value="AVAILABLE">Available</option><option value="IN_USE">In use</option><option value="MAINTENANCE">Maintenance</option><option value="OFFLINE">Offline</option></select></label>
                            <label>Description<input name="description" maxlength="600" placeholder="Optional notes"></label>
                            <div class="admin-form-actions"><button class="button button-primary" type="submit">Add resource</button></div>
                        </form>
                    </details>
                    <section class="panel project-list-panel admin-table-panel">
                        <div class="panel-heading project-list-heading"><div><p class="eyebrow">REGISTERED RESOURCES</p><h2>Compute and storage</h2></div><span class="quiet-label"><c:out value="${resources.size()}"/> records</span></div>
                        <div class="table-scroll"><table class="data-table admin-resource-table"><thead><tr><th>RESOURCE</th><th>TYPE</th><th>CAPACITY</th><th>STATUS</th><th>DESCRIPTION</th><th>ACTIONS</th></tr></thead><tbody>
                            <c:forEach var="resource" items="${resources}">
                                <tr><td colspan="6" class="resource-row-cell">
                                    <div class="resource-inline-grid">
                                        <form method="post" action="${pageContext.request.contextPath}/admin?tab=resources" class="admin-resource-form">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="updateResource"/><input type="hidden" name="resourceId" value="<c:out value='${resource.id}'/>"/>
                                            <input class="table-input" name="name" value="<c:out value='${resource.name}'/>" required maxlength="140" aria-label="Resource name">
                                            <select class="table-select" name="type" aria-label="Resource type"><option value="COMPUTE" <c:if test="${resource.type eq 'COMPUTE'}">selected</c:if>>Compute</option><option value="STORAGE" <c:if test="${resource.type eq 'STORAGE'}">selected</c:if>>Storage</option></select>
                                            <input class="table-input capacity-input" name="capacity" type="number" min="0" step="0.01" value="<c:out value='${resource.capacity}'/>" required aria-label="Capacity">
                                            <input class="table-input unit-input" name="unit" value="<c:out value='${resource.unit}'/>" maxlength="24" required aria-label="Capacity unit">
                                            <select class="table-select" name="status" aria-label="Resource status"><option value="AVAILABLE" <c:if test="${resource.status eq 'AVAILABLE'}">selected</c:if>>Available</option><option value="IN_USE" <c:if test="${resource.status eq 'IN_USE'}">selected</c:if>>In use</option><option value="MAINTENANCE" <c:if test="${resource.status eq 'MAINTENANCE'}">selected</c:if>>Maintenance</option><option value="OFFLINE" <c:if test="${resource.status eq 'OFFLINE'}">selected</c:if>>Offline</option></select>
                                            <input class="table-input" name="description" value="<c:out value='${resource.description}'/>" maxlength="600" aria-label="Description">
                                            <button class="text-action" type="submit">Save</button>
                                        </form>
                                        <form method="post" action="${pageContext.request.contextPath}/admin?tab=resources" data-confirm="Delete this resource record? Usage history will remain saved.">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/><input type="hidden" name="action" value="deleteResource"/><input type="hidden" name="resourceId" value="<c:out value='${resource.id}'/>"/><button class="text-action text-action-muted" type="submit">Delete</button>
                                        </form>
                                    </div>
                                </td></tr>
                            </c:forEach>
                            <c:if test="${empty resources}"><tr><td colspan="6"><div class="inline-empty">No resource records yet. Add compute and storage resources above.</div></td></tr></c:if>
                        </tbody></table></div>
                    </section>
                </c:when>
                <c:when test="${activeTab eq 'projects'}">
                    <section class="admin-section-header"><div><p class="eyebrow">RESEARCH WORKSPACES</p><h2>Project management</h2><p>Review active and archived projects, their owners, datasets, and experiment counts.</p></div><a class="button button-primary" href="${pageContext.request.contextPath}/projects">Manage projects <span>-&gt;</span></a></section>
                    <section class="panel project-list-panel admin-table-panel"><div class="panel-heading project-list-heading"><div><p class="eyebrow">ALL WORKSPACES</p><h2>Project inventory</h2></div><span class="quiet-label"><c:out value="${projects.size()}"/> total</span></div>
                        <div class="table-scroll"><table class="data-table"><thead><tr><th>PROJECT</th><th>OWNER</th><th>DATASETS</th><th>EXPERIMENTS</th><th>STATUS</th></tr></thead><tbody><c:forEach var="project" items="${projects}"><tr><td><strong class="profile-column-name"><c:out value="${project.title}"/></strong></td><td><c:out value="${project.ownerName}"/></td><td><c:out value="${project.datasetCount}"/></td><td><c:out value="${project.experimentCount}"/></td><td><span class="status-badge <c:choose><c:when test='${project.status eq "ACTIVE"}'>status-active</c:when><c:otherwise>status-archived</c:otherwise></c:choose>"><span></span><c:out value="${project.statusLabel}"/></span></td></tr></c:forEach></tbody></table></div>
                    </section>
                </c:when>
                <c:when test="${activeTab eq 'usage'}">
                    <section class="admin-section-header"><div><p class="eyebrow">RESOURCE ANALYTICS</p><h2>Usage reports</h2><p>Usage is derived from saved dataset uploads and completed training runs.</p></div><form class="usage-filter" method="get" action="${pageContext.request.contextPath}/admin"><input type="hidden" name="tab" value="usage"/><label for="usage-window">Reporting window</label><select id="usage-window" name="months" onchange="this.form.submit()"><option value="3" <c:if test="${usageMonthWindow eq 3}">selected</c:if>>Last 3 months</option><option value="6" <c:if test="${usageMonthWindow eq 6}">selected</c:if>>Last 6 months</option><option value="12" <c:if test="${usageMonthWindow eq 12}">selected</c:if>>Last 12 months</option></select><noscript><button class="button button-secondary" type="submit">Update</button></noscript></form><a class="button button-secondary" href="${pageContext.request.contextPath}/admin?tab=usage&amp;months=${usageMonthWindow}&amp;export=csv">Download CSV</a></section>
                    <section class="metric-grid admin-usage-cards">
                        <article class="metric-card"><div class="metric-card-top"><span class="metric-label">Recorded compute</span><span class="metric-icon">C</span></div><div class="metric-value"><c:out value="${usageTotals.computeLabel}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Training run duration</span></div></article>
                        <article class="metric-card metric-accent-2"><div class="metric-card-top"><span class="metric-label">CSV upload volume</span><span class="metric-icon">S</span></div><div class="metric-value"><c:out value="${usageTotals.storageLabel}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Total bytes uploaded</span></div></article>
                        <article class="metric-card metric-accent-3"><div class="metric-card-top"><span class="metric-label">Active users</span><span class="metric-icon">U</span></div><div class="metric-value"><c:out value="${activeUserCount}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Enabled platform accounts</span></div></article>
                        <article class="metric-card metric-accent-4"><div class="metric-card-top"><span class="metric-label">Resources</span><span class="metric-icon">R</span></div><div class="metric-value"><c:out value="${activeResourceCount}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Not offline</span></div></article>
                    </section>
                    <section class="panel usage-chart-panel"><div class="panel-heading"><div><p class="eyebrow">LAST <c:out value="${usageMonthWindow}"/> MONTHS</p><h2>Recorded resource usage</h2></div><div class="chart-legend"><span><i class="legend-compute"></i>Compute seconds</span><span><i class="legend-storage"></i>Uploaded bytes</span></div></div>
                        <c:choose><c:when test="${empty usageMonths}"><div class="usage-empty"><span class="empty-mark">USG</span><h3>No usage recorded yet</h3><p>Storage usage is recorded when a dataset is uploaded. Compute usage will appear after a training run completes.</p></div></c:when><c:otherwise>
                            <div class="usage-bars-chart"><c:forEach var="month" items="${usageMonths}"><div class="usage-month"><div class="usage-bar-pair"><span class="usage-bar usage-compute" style="height:${month.computeBar}px" title="Compute: ${month.computeSeconds} seconds"></span><span class="usage-bar usage-storage" style="height:${month.storageBar}px" title="Storage: ${month.storageBytes} bytes"></span></div><span class="usage-month-label"><c:out value="${month.label}"/></span></div></c:forEach></div>
                        </c:otherwise></c:choose>
                    </section>
                    <section class="panel project-list-panel usage-breakdown-panel"><div class="panel-heading project-list-heading"><div><p class="eyebrow">MONTHLY DETAIL</p><h2>Usage breakdown</h2></div><span class="quiet-label">Recorded application activity</span></div><c:choose><c:when test="${empty usageMonths}"><div class="inline-empty">No records fall within this reporting window.</div></c:when><c:otherwise><div class="table-scroll"><table class="data-table"><thead><tr><th>MONTH</th><th>COMPUTE TIME</th><th>CSV UPLOAD VOLUME</th></tr></thead><tbody><c:forEach var="month" items="${usageMonths}"><tr><td><strong class="profile-column-name"><c:out value="${month.label}"/></strong></td><td><c:out value="${month.computeLabel}"/></td><td><c:out value="${month.storageLabel}"/></td></tr></c:forEach></tbody></table></div></c:otherwise></c:choose></section>
                </c:when>
                <c:otherwise>
                    <section class="admin-section-header"><div><p class="eyebrow">PLATFORM SNAPSHOT</p><h2>Overview</h2><p>Current accounts, projects, resources, and saved dataset usage.</p></div></section>
                    <section class="metric-grid">
                        <article class="metric-card"><div class="metric-card-top"><span class="metric-label">Active users</span><span class="metric-icon">U</span></div><div class="metric-value"><c:out value="${activeUserCount}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Accounts with access</span></div></article>
                        <article class="metric-card metric-accent-2"><div class="metric-card-top"><span class="metric-label">Projects</span><span class="metric-icon">P</span></div><div class="metric-value"><c:out value="${projects.size()}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Active and archived</span></div></article>
                        <article class="metric-card metric-accent-3"><div class="metric-card-top"><span class="metric-label">Resources</span><span class="metric-icon">R</span></div><div class="metric-value"><c:out value="${activeResourceCount}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Compute and storage records</span></div></article>
                        <article class="metric-card metric-accent-4"><div class="metric-card-top"><span class="metric-label">CSV upload volume</span><span class="metric-icon">D</span></div><div class="metric-value"><c:out value="${usageTotals.storageLabel}"/></div><div class="metric-foot"><span class="metric-foot-dot"></span><span>Cumulative uploaded bytes</span></div></article>
                    </section>
                    <section class="content-grid admin-overview-grid">
                        <article class="panel usage-chart-panel"><div class="panel-heading"><div><p class="eyebrow">RESOURCE ANALYTICS</p><h2>Usage over time</h2></div><a class="text-action" href="${pageContext.request.contextPath}/admin?tab=usage">View report -&gt;</a></div>
                            <c:choose><c:when test="${empty usageMonths}"><div class="usage-empty usage-empty-compact"><span class="empty-mark">USG</span><h3>Usage starts with research activity</h3><p>Uploads and completed training runs populate this chart.</p></div></c:when><c:otherwise><div class="usage-bars-chart"><c:forEach var="month" items="${usageMonths}"><div class="usage-month"><div class="usage-bar-pair"><span class="usage-bar usage-compute" style="height:${month.computeBar}px"></span><span class="usage-bar usage-storage" style="height:${month.storageBar}px"></span></div><span class="usage-month-label"><c:out value="${month.label}"/></span></div></c:forEach></div></c:otherwise></c:choose>
                        </article>
                        <article class="panel admin-shortcuts"><p class="eyebrow">QUICK MANAGEMENT</p><h2>Platform controls</h2><a href="${pageContext.request.contextPath}/admin?tab=users"><span class="shortcut-icon">US</span><span><strong>Manage users</strong><small><c:out value="${users.size()}"/> accounts</small></span><b>-&gt;</b></a><a href="${pageContext.request.contextPath}/admin?tab=resources"><span class="shortcut-icon shortcut-green">RS</span><span><strong>Manage resources</strong><small><c:out value="${resources.size()}"/> resource records</small></span><b>-&gt;</b></a><a href="${pageContext.request.contextPath}/admin?tab=projects"><span class="shortcut-icon shortcut-purple">PR</span><span><strong>Review projects</strong><small><c:out value="${projects.size()}"/> project workspaces</small></span><b>-&gt;</b></a></article>
                    </section>
                </c:otherwise>
            </c:choose>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Usage charts reflect records stored by this application.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
