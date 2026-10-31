package util;

import annotation.PreAuthorize;
import annotation.Public;
import model.User;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;

/**
 * Verifie si l'utilisateur a le droit d'executer une methode de controleur.
 */
public class AuthChecker {

    /**
     * Verifie la methode (ou sa classe).
     * @return null si OK, sinon un message d'erreur.
     */
    public static String check(Method method, HttpSession session) {
        // Une methode @Public est toujours accessible
        if (method.isAnnotationPresent(Public.class)) {
            return null;
        }

        // Cherche @PreAuthorize sur la methode d'abord, puis sur la classe
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        if (preAuth == null) {
            preAuth = method.getDeclaringClass().getAnnotation(PreAuthorize.class);
        }

        // Pas d'annotation : acces libre (opt-in)
        if (preAuth == null) {
            return null;
        }

        // Il faut etre authentifie
        User user = AuthContext.getCurrentUser(session);
        if (user == null) {
            return "UNAUTHORIZED";  // 401
        }

        // Il faut avoir le role
        if (!user.hasRole(preAuth.value())) {
            return "FORBIDDEN";     // 403
        }

        return null;
    }
}