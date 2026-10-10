<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#f5f7fb"><title>Profile  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin><link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261010-polish2"></head>
<body class="app-page"><div class="app-shell"><%@ include file="fragments/sidebar.jspf" %>
<main class="main-content"><header class="topbar"><button class="mobile-menu" type="button" data-menu-toggle aria-label="Toggle navigation"><span class="menu-glyph" aria-hidden="true"></span></button><div class="breadcrumbs"><span>Workspace</span><span class="crumb-divider">/</span><strong>Profile</strong></div><div class="topbar-actions"><span class="environment-pill"><span></span> LOCAL WORKSPACE</span><button class="icon-button notification-trigger" type="button" data-notification-toggle aria-expanded="false" aria-controls="notification-panel" aria-label="Notifications" title="Notifications"><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M15.5 8a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7.5h15C17.5 14 15.5 14 15.5 8ZM8 18h4"/></svg><span class="notification-badge" data-notification-badge hidden></span></button><button type="button" class="top-avatar account-menu-trigger" data-account-menu-toggle aria-expanded="false" aria-controls="account-menu-panel" aria-label="Open account menu" title="Account menu"><span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden></button></div></header>
<div class="page-wrap"><section class="welcome-row"><div><p class="eyebrow">ACCOUNT SETTINGS</p><h1>Your profile</h1><p class="page-intro">Manage your account details, security, and workspace preferences.</p></div></section>
    <section class="panel profile-photo-panel" id="profile-photo" aria-labelledby="profile-photo-title">
        <div class="profile-photo-control" data-photo-control>
            <button class="profile-photo-preview" type="button" data-photo-actions-toggle aria-expanded="false" aria-controls="profile-photo-menu" aria-label="Profile photo options">
                <span data-profile-initials><c:out value="${initials}"/></span><img class="profile-photo-image" data-profile-photo alt="" hidden>
                <span class="profile-photo-camera" aria-hidden="true"><svg viewBox="0 0 20 20" focusable="false"><path d="M3 6.5h3l1.2-2h5.6l1.2 2h3v9H3z"/><circle cx="10" cy="11" r="2.8"/></svg></span>
            </button>
            <div class="profile-photo-menu" id="profile-photo-menu" data-photo-menu role="group" aria-label="Profile photo options" hidden>
                <p>Profile photo</p>
                <button type="button" data-photo-upload><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M10 13V3m0 0L6.5 6.5M10 3l3.5 3.5"/><path d="M4 11v5h12v-5"/></svg><span>Upload photo</span></button>
                <button type="button" data-photo-remove aria-describedby="profile-photo-remove-help" disabled><svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M4 6h12M8 6V4h4v2m-7 0 1 10h8l1-10M8.5 9v4m3-4v4"/></svg><span>Remove photo</span></button>
                <small id="profile-photo-remove-help" data-photo-remove-help>Upload a photo before you can remove one.</small>
            </div>
            <input id="profile-photo-file" class="visually-hidden" type="file" accept="image/png,image/jpeg" data-profile-photo-file>
        </div>
        <div class="profile-photo-content"><p class="eyebrow">PERSONALIZE YOUR ACCOUNT</p><h2 id="profile-photo-title">Profile picture</h2><p>Hover over your photo or tap it to upload or remove your picture. PNG and JPG files up to 5 MB are supported.</p><span class="profile-photo-status" data-profile-photo-status role="status" aria-live="polite"></span></div>
    </section>
    <c:if test="${param.notice eq 'saved'}"><div class="alert alert-success" role="status">Profile changes saved.</div></c:if>
    <c:if test="${param.error eq 'validation'}"><div class="alert alert-error" role="alert">Check your name, email, and password confirmation. Passwords must be at least 10 characters.</div></c:if>
    <c:if test="${param.error eq 'password'}"><div class="alert alert-error" role="alert">The current password did not match. Your profile was not changed.</div></c:if>
    <c:if test="${param.error eq 'email'}"><div class="alert alert-error" role="alert">That email address is already used by another account.</div></c:if>
    <section class="profile-layout"><article class="panel profile-form-panel" id="profile-details"><div class="panel-heading"><div><p class="eyebrow">PERSONAL INFORMATION</p><h2>Account details</h2></div><span class="role-pill"><c:out value="${roleLabel}"/></span></div>
        <form method="post" action="${pageContext.request.contextPath}/profile" class="profile-form"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
            <label for="profile-name">Full name</label><input id="profile-name" name="fullName" value="<c:out value='${currentUser.fullName}'/>" required minlength="2" maxlength="120" autocomplete="name">
            <label for="profile-email">Email address</label><input id="profile-email" name="email" value="<c:out value='${currentUser.email}'/>" type="email" required maxlength="190" autocomplete="email">
            <div class="password-section" id="account-settings"><p class="eyebrow">PASSWORD</p><h3>Change password <span>Optional</span></h3><p>Leave these fields blank to keep your current password.</p>
                <label for="current-password">Current password</label><input id="current-password" name="currentPassword" type="password" autocomplete="current-password">
                <div class="password-field-grid"><div><label for="new-password">New password</label><input id="new-password" name="newPassword" type="password" minlength="10" maxlength="200" autocomplete="new-password"></div><div><label for="confirm-password">Confirm new password</label><input id="confirm-password" name="confirmPassword" type="password" minlength="10" maxlength="200" autocomplete="new-password"></div></div>
            </div>
            <div class="form-actions"><button class="button button-primary" type="submit">Save profile <span>-&gt;</span></button></div>
        </form>
    </article><aside class="panel profile-security-panel"><span class="security-icon">AI</span><p class="eyebrow">ACCOUNT SECURITY</p><h2>Your workspace identity</h2><p>This account uses <strong><c:out value="${roleLabel}"/></strong> access. Your project membership determines which datasets and experiments you can view.</p><a href="${pageContext.request.contextPath}/collaboration" class="text-action">Review project teams -&gt;</a></aside></section>
    <section class="panel preferences-panel" id="workspace-preferences" aria-labelledby="preferences-title">
        <div class="panel-heading"><div><p class="eyebrow">WORKSPACE PREFERENCES</p><h2 id="preferences-title">Notifications</h2></div></div>
        <div class="preference-row"><div class="preference-copy"><strong>Show unread activity count</strong><p>Display a small badge on the notification bell when there is unread project activity. You can still open notifications when this is off.</p><span class="preference-save-status" data-notification-preference-status role="status" aria-live="polite"></span></div><label class="preference-switch"><span class="visually-hidden">Show unread activity count</span><input type="checkbox" role="switch" data-notification-preference data-storage-key="datahive.preferences.notifications.<c:out value='${currentUser.id}'/>" checked><span aria-hidden="true"></span></label></div>
    </section>
    <footer class="page-footer"><span>DataHive Research Platform</span><span>Passwords are stored as salted PBKDF2 hashes.</span></footer>
</div></main></div><script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script></body></html>
