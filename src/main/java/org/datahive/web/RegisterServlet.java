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
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public final class RegisterServlet extends HttpServlet {
    private static final Pattern EMAIL = Pattern.compile("(?i)^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$");
    static final String GOOGLE_PENDING_NAME = "datahiveGooglePendingName";
    static final String GOOGLE_PENDING_EMAIL = "datahiveGooglePendingEmail";
    private final UserDao userDao = new UserDao();

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
        session.setAttribute("googleNonce", UUID.randomUUID().toString());
        preparePage(request, session);
        String googleName = (String) session.getAttribute(GOOGLE_PENDING_NAME);
        String googleEmail = (String) session.getAttribute(GOOGLE_PENDING_EMAIL);
        if (googleEmail != null) {
            request.setAttribute("googleSignup", true);
            request.setAttribute("fullName", googleName);
            request.setAttribute("email", googleEmail);
        }
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        String submitted = request.getParameter("csrfToken");
        if (expected == null || submitted == null || !expected.equals(submitted)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The account form expired. Reload and try again.");
            return;
        }

        String googleName = (String) session.getAttribute(GOOGLE_PENDING_NAME);
        String googleEmail = (String) session.getAttribute(GOOGLE_PENDING_EMAIL);
        if (googleEmail != null) {
            completeGoogleSignup(request, response, session, googleName, googleEmail);
            return;
        }

        String name = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email")).toLowerCase(Locale.ROOT);
        String password = request.getParameter("password");
        String confirmation = request.getParameter("confirmPassword");
        name = clean(name);
        if (name.isBlank()) name = clean(request.getParameter("fullName"));
        Role role = parseRole(request.getParameter("role"));
        String issue = null;
        if (name.length() < 2 || name.length() > 120 || !EMAIL.matcher(email).matches() || email.length() > 190) {
            issue = "Enter a name and a valid email address.";
        } else if (password == null || password.length() < 10 || password.length() > 200) {
            issue = "Choose a password between 10 and 200 characters.";
        } else if (!password.equals(confirmation)) {
            issue = "The passwords do not match.";
        } else if (role == null) {
            issue = "Choose whether you are joining as a Researcher or an Admin.";
        } else if (role == Role.ADMIN && !validAdminCode(request.getParameter("adminInviteCode"))) {
            issue = "Admin accounts require a valid administrator invite code.";
        }

        if (issue != null) {
            request.setAttribute("registerError", issue);
            request.setAttribute("fullName", name);
            request.setAttribute("email", email);
            request.setAttribute("selectedRole", request.getParameter("role"));
            preparePage(request, session);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        try {
            User created = userDao.createAccount(name, email, password, role);
            ActivityLogger.record(created, "USER_REGISTERED", "Account", created.getId(),
                    "Created a DataHive account as " + role.name().toLowerCase(Locale.ROOT) + ".");
            establishSession(request, created);
            response.sendRedirect(request.getContextPath() + "/app?welcome=1");
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                request.setAttribute("registerError", "An account with this email already exists. Sign in instead.");
                request.setAttribute("fullName", name);
                request.setAttribute("email", email);
                request.setAttribute("selectedRole", request.getParameter("role"));
                preparePage(request, session);
                request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                return;
            }
            throw new ServletException("Could not create your DataHive account", exception);
        }
    }

    private void preparePage(HttpServletRequest request, HttpSession session) {
        request.setAttribute("csrfToken", session.getAttribute(AuthFilter.CSRF_SESSION_KEY));
        request.setAttribute("googleNonce", session.getAttribute("googleNonce"));
        request.setAttribute("googleClientId", System.getenv("DATAHIVE_GOOGLE_CLIENT_ID"));
        request.setAttribute("hasAdminInvite", nonBlank(System.getenv("DATAHIVE_ADMIN_INVITE_CODE")));
    }

    private void completeGoogleSignup(HttpServletRequest request, HttpServletResponse response,
                                      HttpSession session, String name, String email)
            throws ServletException, IOException {
        Role role = parseRole(request.getParameter("role"));
        if (name.length() < 2 || name.length() > 120 || !EMAIL.matcher(email).matches() || email.length() > 190) {
            session.removeAttribute(GOOGLE_PENDING_NAME);
            session.removeAttribute(GOOGLE_PENDING_EMAIL);
            response.sendRedirect(request.getContextPath() + "/login?error=google");
            return;
        }
        if (role == null || (role == Role.ADMIN && !validAdminCode(request.getParameter("adminInviteCode")))) {
            request.setAttribute("registerError", "Choose a workspace role. Admin accounts also need a valid invite code.");
            request.setAttribute("selectedRole", request.getParameter("role"));
            request.setAttribute("googleSignup", true);
            request.setAttribute("fullName", name);
            request.setAttribute("email", email);
            preparePage(request, session);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        try {
            User created = userDao.createAccount(name, email,
                    org.datahive.security.PasswordHasher.hash(UUID.randomUUID().toString()), role);
            ActivityLogger.record(created, "USER_REGISTERED", "Account", created.getId(),
                    "Created a DataHive account with Google as " + role.name().toLowerCase(Locale.ROOT) + ".");
            establishSession(request, created);
            response.sendRedirect(request.getContextPath() + "/app?welcome=1");
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                session.removeAttribute(GOOGLE_PENDING_NAME);
                session.removeAttribute(GOOGLE_PENDING_EMAIL);
                response.sendRedirect(request.getContextPath() + "/login?error=googleExists");
                return;
            }
            throw new ServletException("Could not create your Google-linked DataHive account", exception);
        }
    }

    static boolean validAdminCode(String submitted) {
        String configured = System.getenv("DATAHIVE_ADMIN_INVITE_CODE");
        if (configured == null || configured.isBlank() || submitted == null) return false;
        return MessageDigest.isEqual(configured.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8));
    }

    static Role parseRole(String value) {
        if ("RESEARCHER".equals(value)) return Role.RESEARCHER;
        if ("ADMIN".equals(value)) return Role.ADMIN;
        return null;
    }

    static void establishSession(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AuthFilter.USER_SESSION_KEY, user);
        session.setAttribute(AuthFilter.CSRF_SESSION_KEY, UUID.randomUUID().toString());
    }

    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
