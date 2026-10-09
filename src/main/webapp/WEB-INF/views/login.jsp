<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#102142">
    <title>Sign in  |  DataHive</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=20261008-photo-menu4">
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-story" aria-labelledby="story-heading">
        <a class="brand brand-light" href="${pageContext.request.contextPath}/login" aria-label="DataHive home">
            <span class="brand-mark" aria-hidden="true"><span></span><span></span><span></span></span>
            <span class="brand-name">data<span>hive</span></span>
        </a>
        <div class="story-copy">
            <p class="eyebrow eyebrow-light"><span class="eyebrow-dot"></span> AI research workspace</p>
            <h1 id="story-heading">Make every<br>experiment count.</h1>
            <p class="story-description">Bring your datasets, training runs, and research team into one clear, collaborative workspace.</p>
        </div>
        <div class="story-footnote"><span class="sparkle" aria-hidden="true">AI</span> Research with clarity, from first dataset to final result.</div>
        <div class="story-orbit story-orbit-one" aria-hidden="true"></div>
        <div class="story-orbit story-orbit-two" aria-hidden="true"></div>
    </section>

    <section class="login-panel" aria-labelledby="login-heading">
        <div class="login-card">
            <div class="mobile-brand brand">
                <span class="brand-mark" aria-hidden="true"><span></span><span></span><span></span></span>
                <span class="brand-name">data<span>hive</span></span>
            </div>
            <p class="eyebrow">WELCOME BACK</p>
            <h2 id="login-heading">Sign in to DataHive</h2>
            <p class="login-intro">Pick up where your research left off.</p>

            <c:if test="${not empty loginError}">
                <div class="alert alert-error" role="alert"><c:out value="${loginError}"/></div>
            </c:if>
            <c:if test="${param.loggedOut eq '1'}">
                <div class="alert alert-success" role="status">You have been signed out.</div>
            </c:if>

            <form class="login-form" method="post" action="${pageContext.request.contextPath}/login">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                <label for="email">Email address</label>
                <div class="input-wrap">
                    <span class="input-icon" aria-hidden="true">@</span>
                    <input id="email" name="email" type="email" autocomplete="username" required maxlength="190"
                           placeholder="you@research.org" value="<c:out value='${email}'/>" autofocus>
                </div>

                <div class="label-row">
                    <label for="password">Password</label>
                    <span class="label-note">Use your DataHive account</span>
                </div>
                <div class="input-wrap">
                    <span class="input-icon" aria-hidden="true">PW</span>
                    <input id="password" name="password" type="password" autocomplete="current-password" required maxlength="200"
                           placeholder="Enter your password">
                    <button class="password-toggle" type="button" data-toggle-password aria-label="Show password">Show</button>
                </div>

                <button class="button button-primary button-full" type="submit">Sign in <span aria-hidden="true">-&gt;</span></button>
            </form>

            <div class="demo-access">
                <div class="demo-access-heading"><span class="demo-shield" aria-hidden="true">OK</span><span>Demo access</span></div>
                <p>Use the Admin or Researcher demo account listed in the project README.</p>
            </div>
            <p class="login-privacy">Your research workspace is private to your account and project members.</p>
        </div>
        <footer class="login-footer">DataHive <span> | </span> AI Research &amp; Development</footer>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
