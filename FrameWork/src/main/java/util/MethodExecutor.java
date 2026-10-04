package util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import annotation.RequestBody;
import annotation.RequestParam;
import annotation.PathVariable;
import annotation.ResponseBody;
import annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MethodExecutor {

    public static Object execute(Method method, HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (method == null) {
            throw new IllegalArgumentException("Methode ne peut pas etre null");
        }

        Class<?> classController = method.getDeclaringClass();
        Object instance = classController.getDeclaredConstructor().newInstance();

        // Prépare les arguments
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();

            if (type == HttpServletRequest.class) {
                args[i] = req;
            } else if (type == HttpServletResponse.class) {
                args[i] = res;
            } else if (p.isAnnotationPresent(PathVariable.class)) {
                String name = p.getAnnotation(PathVariable.class).value();
                String value = (String) req.getAttribute("__path_vars_" + name);
                args[i] = convert(value, type);
            } else if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = p.getAnnotation(RequestParam.class);
                String value = req.getParameter(rp.value());
                if (value == null && rp.required()) {
                    throw new IllegalArgumentException("Paramètre requis manquant : " + rp.value());
                }
                args[i] = convert(value, type);
            } else if (p.isAnnotationPresent(RequestBody.class)) {
                String body = new String(req.getInputStream().readAllBytes());
                args[i] = JsonSerializer.fromJson(body, type);
            } else {
                args[i] = null; // non supporté pour l'instant
            }
        }

        return method.invoke(instance, args);
    }

    // Helper : conversion simple String → type primitif
    private static Object convert(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }

    // Utilitaire : est-ce que le retour doit être en JSON ?
    public static boolean isJsonResponse(Method method) {
        return method.isAnnotationPresent(ResponseBody.class)
            || method.getDeclaringClass().isAnnotationPresent(RestController.class);
    }
}