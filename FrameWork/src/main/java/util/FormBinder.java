package util;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

/**
 * Transforme les paramètres d'un formulaire HTML
 * (application/x-www-form-urlencoded) en objet Java.
 *
 * Convention : le nom du champ HTML = le nom du champ Java.
 * Ex : <input name="email">   →   user.email
 */
public class FormBinder {

    public static <T> T bind(HttpServletRequest req, Class<T> clazz) throws Exception {
        T obj = clazz.getDeclaredConstructor().newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            String value = req.getParameter(field.getName());
            if (value == null) {
                continue;   // champ absent → valeur par défaut (null, 0, false...)
            }

            field.setAccessible(true);
            Object converted = convertType(value, field.getType());
            field.set(obj, converted);
        }
        return obj;
    }

    private static Object convertType(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        // Pour les autres types, on tente de caster en String
        return value;
    }
}