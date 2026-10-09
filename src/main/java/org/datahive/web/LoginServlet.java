package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.model.User;
import org.datahive.service.AuthService;
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public final class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(AuthFilter.USER_SESSION_KEY) instanceof User) {
            response.sendRedirect(request.getContextPath() + "/app");
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthFilter.CSRF_SESSION_KEY, UUID.randomUUID().toString());
        request.setAttribute("csrfToken", session.getAttribute(AuthFilter.CSRF_SESSION_KEY));
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        String expectedToken = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        String submittedToken = request.getParameter("csrfToken");
        if (expectedToken == null || submittedToken == null || !expectedToken.equals(submittedToken)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The sign-in form expired. Reload and try again.");
            return;
        }

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        try {
            Optional<User> authenticated = authService.authenticate(email, password);
            if (authenticated.isEmpty()) {
                request.setAttribute("email", email == null ? "" : email.trim());
                request.setAttribute("loginError", "We could not sign you in with those details.");
                request.setAttribute("csrfToken", expectedToken);
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }

            if (session != null) {
                session.invalidate();
            }
            HttpSession authenticatedSession = request.getSession(true);
            request.changeSessionId();
            authenticatedSession.setAttribute(AuthFilter.USER_SESSION_KEY, authenticated.get());
            authenticatedSession.setAttribute(AuthFilter.CSRF_SESSION_KEY, UUID.randomUUID().toString());
            ActivityLogger.record(authenticated.get(), "USER_SIGNED_IN", "Session", null,
                    "Signed in to the DataHive research platform.");
            response.sendRedirect(request.getContextPath() + "/app");
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Sign-in could not reach the database", exception);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Sign-in is temporarily unavailable. Please try again shortly.");
        }
    }
}
