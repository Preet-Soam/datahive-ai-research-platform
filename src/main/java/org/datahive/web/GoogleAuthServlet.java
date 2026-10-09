package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.dao.UserDao;
import org.datahive.model.Role;
import org.datahive.model.User;
import org.datahive.security.GoogleIdTokenVerifier;
import org.datahive.service.ActivityLogger;
import org.datahive.security.PasswordHasher;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

@WebServlet(name = "GoogleAuthServlet", urlPatterns = "/google-auth")
public final class GoogleAuthServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String mode = request.getParameter("mode");
        String destination = "register".equals(mode) ? "/register" : "/login";
        HttpSession session = request.getSession(false);
        String csrf = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        String nonce = session == null ? null : (String) session.getAttribute("googleNonce");
        if (csrf == null || !csrf.equals(request.getParameter("csrfToken")) || nonce == null ||
                !("login".equals(mode) || "register".equals(mode))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The Google sign-in form expired. Reload and try again.");
            return;
        }
        session.removeAttribute("googleNonce");

        String clientId = System.getenv("DATAHIVE_GOOGLE_CLIENT_ID");
        GoogleIdTokenVerifier.GoogleIdentity identity = GoogleIdTokenVerifier.verify(
                request.getParameter("credential"), clientId, nonce);
        if (identity == null) {
            response.sendRedirect(request.getContextPath() + destination + "?error=google");
            return;
        }

        try {
            var existing = userDao.findForLogin(identity.email());
            if ("login".equals(mode)) {
                if (existing.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/login?error=googleAccount");
                    return;
                }
                User user = existing.get().user();
                if (!user.isActive()) {
                    response.sendRedirect(request.getContextPath() + "/login?error=disabled");
                    return;
                }
                RegisterServlet.establishSession(request, user);
                ActivityLogger.record(user, "USER_SIGNED_IN", "Session", null,
                        "Signed in to the DataHive research platform with Google.");
                response.sendRedirect(request.getContextPath() + "/app");
                return;
            }

            Role role = RegisterServlet.parseRole(request.getParameter("role"));
            if (role == null || (role == Role.ADMIN && !RegisterServlet.validAdminCode(request.getParameter("adminInviteCode")))) {
                response.sendRedirect(request.getContextPath() + "/register?error=role");
                return;
            }
            if (existing.isPresent()) {
                response.sendRedirect(request.getContextPath() + "/register?error=googleExists");
                return;
            }
            String name = identity.name().trim();
            if (name.length() < 2 || name.length() > 120) {
                response.sendRedirect(request.getContextPath() + "/register?error=googleName");
                return;
            }
            User user = userDao.createAccount(name, identity.email(), PasswordHasher.hash(UUID.randomUUID().toString()), role);
            ActivityLogger.record(user, "USER_REGISTERED", "Account", user.getId(),
                    "Created a DataHive account with Google as " + role.name().toLowerCase(Locale.ROOT) + ".");
            establishGoogleSession(request, user);
            response.sendRedirect(request.getContextPath() + "/app?welcome=1");
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                response.sendRedirect(request.getContextPath() + "/register?error=googleExists");
                return;
            }
            throw new ServletException("Google sign-in could not reach the account database", exception);
        }
    }

    private static void establishGoogleSession(HttpServletRequest request, User user) {
        RegisterServlet.establishSession(request, user);
    }
}
