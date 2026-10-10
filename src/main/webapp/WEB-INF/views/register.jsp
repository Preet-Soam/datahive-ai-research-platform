<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#102142">
    <title>Create account | DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261010-polish2">
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-story" aria-labelledby="story-heading">
        <a class="brand brand-light" href="${pageContext.request.contextPath}/login" aria-label="DataHive home"><span class="brand-mark" aria-hidden="true"><span></span><span></span><span></span></span><span class="brand-name">data<span>hive</span></span></a>
        <div class="story-copy"><p class="eyebrow eyebrow-light"><span class="eyebrow-dot"></span> AI research workspace</p><h1 id="story-heading">Build your next<br>research story.</h1><p class="story-description">Keep datasets, model runs, and collaborators in one workspace designed for clear, reproducible research.</p></div>
        <div class="story-footnote"><span class="sparkle" aria-hidden="true">AI</span> Your work starts with a better research workflow.</div><div class="story-orbit story-orbit-one" aria-hidden="true"></div><div class="story-orbit story-orbit-two" aria-hidden="true"></div>
    </section>
    <section class="login-panel" aria-labelledby="register-heading">
        <div class="login-card register-card">
            <div class="mobile-brand brand"><span class="brand-mark" aria-hidden="true"><span></span><span></span><span></span></span><span class="brand-name">data<span>hive</span></span></div>
            <p class="eyebrow"><c:choose><c:when test="${googleSignup}">GOOGLE ACCOUNT VERIFIED</c:when><c:otherwise>GET STARTED</c:otherwise></c:choose></p><h2 id="register-heading"><c:choose><c:when test="${googleSignup}">Choose your workspace</c:when><c:otherwise>Create your account</c:otherwise></c:choose></h2><p class="login-intro"><c:choose><c:when test="${googleSignup}">You’re signed in with Google. Choose Researcher or Admin to finish creating your DataHive account.</c:when><c:otherwise>Choose your workspace role and set up your research profile.</c:otherwise></c:choose></p>
            <c:if test="${not empty registerError}"><div class="alert alert-error" role="alert"><c:out value="${registerError}"/></div></c:if>
            <c:if test="${param.error eq 'google'}"><div class="alert alert-error" role="alert">Google could not verify the sign-up. Please try again.</div></c:if>
            <c:if test="${param.error eq 'googleExists'}"><div class="alert alert-error" role="alert">That Google address already has a DataHive account. Sign in instead.</div></c:if>
            <c:if test="${param.error eq 'role'}"><div class="alert alert-error" role="alert">Choose a role and provide a valid Admin invite code if you selected Admin.</div></c:if>
            <c:if test="${param.error eq 'googleName'}"><div class="alert alert-error" role="alert">Your Google profile needs a name before DataHive can create your account.</div></c:if>
            <form class="login-form register-form" method="post" action="${pageContext.request.contextPath}/register" data-selected-role="<c:out value='${selectedRole}'/>">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                <c:if test="${googleSignup}"><div class="google-verified-identity"><span class="google-verified-mark" aria-hidden="true">G</span><span><c:choose><c:when test="${empty fullName}"><strong>Google account connected</strong><small><c:out value="${email}"/></small></c:when><c:otherwise><strong><c:out value="${fullName}"/></strong><small><c:out value="${email}"/></small></c:otherwise></c:choose></span><span class="google-verified-check" aria-label="Verified">✓</span></div><c:if test="${empty fullName}"><label for="full-name">Your name</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">ID</span><input id="full-name" name="fullName" type="text" autocomplete="name" required minlength="2" maxlength="120" placeholder="Your name"></div></c:if></c:if>
                <c:if test="${not googleSignup}">
                    <label for="full-name">Your name</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">ID</span><input id="full-name" name="fullName" type="text" autocomplete="name" required minlength="2" maxlength="120" placeholder="e.g. Preet Soam" value="<c:out value='${fullName}'/>"/></div>
                    <label for="register-email">Email address</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">@</span><input id="register-email" name="email" type="email" autocomplete="email" required maxlength="190" placeholder="you@research.org" value="<c:out value='${email}'/>"/></div>
                </c:if>
                <fieldset class="role-choice-group"><legend>How will you use DataHive?</legend><div class="role-choice-grid">
                    <label class="role-choice"><input type="radio" name="role" value="RESEARCHER" <c:if test="${selectedRole eq 'RESEARCHER' or empty selectedRole}">checked</c:if>><span class="role-choice-icon" aria-hidden="true">R</span><span><strong>Researcher</strong><small>Manage datasets, train models, and collaborate.</small></span><span class="role-choice-check" aria-hidden="true"></span></label>
                    <label class="role-choice"><input type="radio" name="role" value="ADMIN" <c:if test="${selectedRole eq 'ADMIN'}">checked</c:if>><span class="role-choice-icon role-choice-admin" aria-hidden="true">A</span><span><strong>Admin</strong><small>Manage platform users, projects, and resources.</small></span><span class="role-choice-check" aria-hidden="true"></span></label>
                </div></fieldset>
                <div class="admin-invite-field" data-admin-invite hidden><label for="admin-invite">Administrator invite code</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">KEY</span><input id="admin-invite" name="adminInviteCode" type="password" autocomplete="off" maxlength="200" placeholder="Enter your invite code" <c:if test="${hasAdminInvite}">required</c:if>></div><small><c:choose><c:when test="${hasAdminInvite}">Admin access requires a code provided by your platform owner.</c:when><c:otherwise>Admin self-registration is currently disabled. Ask the platform owner to configure an invite code.</c:otherwise></c:choose></small></div>
                <c:if test="${not googleSignup}"><div class="register-password-grid"><div><label for="new-password">Password</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">PW</span><input id="new-password" name="password" type="password" autocomplete="new-password" required minlength="10" maxlength="200" placeholder="At least 10 characters"></div></div><div><label for="confirm-password">Confirm password</label><div class="input-wrap"><span class="input-icon" aria-hidden="true">PW</span><input id="confirm-password" name="confirmPassword" type="password" autocomplete="new-password" required minlength="10" maxlength="200" placeholder="Enter it again"></div></div></div></c:if>
                <button class="button button-primary button-full" type="submit"><c:choose><c:when test="${googleSignup}">Finish creating account</c:when><c:otherwise>Create account</c:otherwise></c:choose><span aria-hidden="true">-&gt;</span></button>
            </form>
            <c:if test="${not googleSignup}"><div class="auth-divider"><span>or sign up with</span></div>
            <c:choose><c:when test="${not empty googleClientId}">
                <form class="google-auth-form" method="post" action="${pageContext.request.contextPath}/google-auth" data-google-auth data-google-mode="register" data-google-client-id="<c:out value='${googleClientId}'/>" data-google-nonce="<c:out value='${googleNonce}'/>" data-role-source=".register-form">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="mode" value="register"><input type="hidden" name="role" value="RESEARCHER"><input type="hidden" name="adminInviteCode" value=""><input type="hidden" name="credential" value="">
                    <div class="google-button-slot" data-google-button aria-label="Sign up with Google"></div>
                </form>
            </c:when><c:otherwise><div class="google-auth-unavailable"><button class="google-disabled-button" type="button" disabled><span aria-hidden="true">G</span>Continue with Google</button><small>Connect a Google OAuth client ID to enable Google sign-in.</small></div></c:otherwise></c:choose></c:if>
            <p class="auth-switch">Already have an account? <a href="${pageContext.request.contextPath}/login">Sign in</a></p>
            <p class="login-privacy">Your role determines which research tools and administration areas are available.</p>
        </div>
        <footer class="login-footer">DataHive <span> | </span> AI Research &amp; Development</footer>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/register.js?v=20261009-auth1" defer></script>
<c:if test="${not empty googleClientId}"><script src="https://accounts.google.com/gsi/client" async defer></script><script src="${pageContext.request.contextPath}/assets/js/google-auth.js?v=20261010-unified-auth1" defer></script></c:if>
</body>
</html>
