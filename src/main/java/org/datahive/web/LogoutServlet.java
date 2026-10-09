package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.model.User;
import org.datahive.service.ActivityLogger;

import java.io.IOException;

@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public final class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        String submitted = request.getParameter("csrfToken");
        if (expected == null || submitted == null || !expected.equals(submitted)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request could not be verified.");
            return;
        }
        Object sessionUser = session.getAttribute(AuthFilter.USER_SESSION_KEY);
        if (sessionUser instanceof User user) {
            ActivityLogger.record(user, "USER_SIGNED_OUT", "Session", null,
                    "Signed out of the DataHive research platform.");
        }
        session.invalidate();
        response.sendRedirect(request.getContextPath() + "/login?loggedOut=1");
    }
}
