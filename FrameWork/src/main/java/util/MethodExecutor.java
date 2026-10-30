package util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import annotation.RequestBody;
import annotation.RequestParam;
import annotation.PathVariable;
import annotation.RequestPart;
import annotation.ResponseBody;
import annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

public class MethodExecutor {

    public static Object execute(Method method, HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (method == null) {
            throw new IllegalArgumentException("Methode ne peut pas etre null");
        }

        Class<?> classController = method.getDeclaringClass();
        Object instance = classController.getDeclaredConstructor().newInstance();

        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        String contentType = req.getContentType();
        if (contentType == null) contentType = "";

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();

            // Objets injectes automatiquement
            if (type == HttpServletRequest.class) {
                args[i] = req;

            } else if (type == HttpServletResponse.class) {
                args[i] = res;

            } else if (type == HttpSession.class) {
                // Injecte la session (creee si absente)
                args[i] = req.getSession();

            } else if (type == ServletContext.class) {
                // Injecte le contexte applicatif
                args[i] = req.getServletContext();

            } else if (p.isAnnotationPresent(RequestPart.class)) {
                String name = p.getAnnotation(RequestPart.class).value();
                Part part = req.getPart(name);
                args[i] = part;

            } else if (p.isAnnotationPresent(PathVariable.class)) {
                String name = p.getAnnotation(PathVariable.class).value();
                String value = (String) req.getAttribute("__path_vars_" + name);
                args[i] = convert(value, type);

            } else if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = p.getAnnotation(RequestParam.class);
                String value = req.getParameter(rp.value());
                if (value == null && rp.required()) {
                    throw new IllegalArgumentException("Parametre requis manquant : " + rp.value());
                }
                args[i] = convert(value, type);

            } else if (p.isAnnotationPresent(RequestBody.class)) {
                if (contentType.contains("multipart/form-data")
                        || contentType.contains("application/x-www-form-urlencoded")) {
                    args[i] = FormBinder.bind(req, type);
                } else {
                    String body = new String(req.getInputStream().readAllBytes());
                    args[i] = JsonSerializer.fromJson(body, type);
                }

            } else {
                args[i] = null;
            }
        }

        return method.invoke(instance, args);
    }

    private static Object convert(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }

    public static boolean isJsonResponse(Method method) {
        if (method.getReturnType() == String.class) {
            return false;
        }
        return method.isAnnotationPresent(ResponseBody.class)
            || method.getDeclaringClass().isAnnotationPresent(RestController.class);
    }
}