package util;

import model.User;

import javax.servlet.http.HttpSession;

/**
 * Aide a manipuler l'utilisateur authentifie dans la session.
 */
public class AuthContext {

    public static final String SESSION_KEY = "__AUTH_USER__";

    public static void login(HttpSession session, User user) {
        session.setAttribute(SESSION_KEY, user);
        session.setMaxInactiveInterval(30 * 60); // 30 minutes
    }

    public static void logout(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    public static User getCurrentUser(HttpSession session) {
        if (session == null) return null;
        return (User) session.getAttribute(SESSION_KEY);
    }

    public static boolean isAuthenticated(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    public static boolean hasRole(HttpSession session, String role) {
        User u = getCurrentUser(session);
        return u != null && u.hasRole(role);
    }
}