package org.datahive.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.model.User;
import org.datahive.model.Role;

import java.io.IOException;
import java.util.UUID;

@WebFilter(urlPatterns = {"/app", "/logout", "/projects", "/datasets", "/admin", "/profile", "/experiments", "/collaboration"})
public final class AuthFilter implements Filter {
    public static final String USER_SESSION_KEY = "datahiveUser";
    public static final String CSRF_SESSION_KEY = "csrfToken";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(USER_SESSION_KEY);

        if (user == null || !user.isActive()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (session.getAttribute(CSRF_SESSION_KEY) == null) {
            session.setAttribute(CSRF_SESSION_KEY, UUID.randomUUID().toString());
        }
        request.setAttribute("csrfToken", session.getAttribute(CSRF_SESSION_KEY));
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdmin", user.getRole() == Role.ADMIN);
        request.setAttribute("roleLabel", user.getRole().name().toLowerCase(java.util.Locale.ROOT));
        chain.doFilter(request, response);
    }
}
