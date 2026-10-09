package org.datahive.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datahive.config.Database;

import java.io.IOException;
import java.sql.Connection;

@WebServlet(name = "HealthServlet", urlPatterns = "/health")
public final class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (Connection ignored = Database.getConnection()) {
            response.getWriter().write("{\"status\":\"ok\",\"database\":\"connected\"}");
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"unavailable\",\"database\":\"disconnected\"}");
        }
    }
}
