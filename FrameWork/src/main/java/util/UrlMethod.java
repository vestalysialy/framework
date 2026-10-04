package util;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

import annotation.GetMapping;
import annotation.PostMapping;
import annotation.UrlMapping;

public class UrlMethod {

    private final String url;
    private final String method;
    private Pattern pattern;  // pour les URLs avec {id}

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = (method == null || method.isBlank()) ? "GET" : method.trim().toUpperCase();
        this.pattern = buildPattern(url);
    }

    public UrlMethod(UrlMapping annotation) {
        this(annotation.url(), annotation.method());
    }

    public UrlMethod(GetMapping annotation) {
        this(annotation.url(), "GET");
    }

    public UrlMethod(PostMapping annotation) {
        this(annotation.url(), "POST");
    }

    // Compile "/users/{id}" en regex "^/users/([^/]+)$"
    private Pattern buildPattern(String url) {
        String regex = url.replaceAll("\\{[^/]+\\}", "([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    // Extrait les valeurs {id}, {name}, etc.
    public Map<String, String> extractPathVariables(String actualUrl) {
        Map<String, String> vars = new HashMap<>();
        Matcher m = pattern.matcher(actualUrl);
        if (!m.matches()) return vars;

        // Récupère les noms des variables dans l'ordre
        Matcher names = Pattern.compile("\\{([^/]+)\\}").matcher(url);
        int i = 1;
        while (names.find()) {
            vars.put(names.group(1), m.group(i++));
        }
        return vars;
    }

    public boolean matches(String actualUrl, String httpMethod) {
        return this.method.equals(httpMethod) && pattern.matcher(actualUrl).matches();
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlMethod that = (UrlMethod) o;
        return Objects.equals(url, that.url) && Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

    @Override
    public String toString() {
        return method + " " + url;
    }
}