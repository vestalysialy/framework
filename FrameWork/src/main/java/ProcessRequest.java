import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.JsonSerializer;
import util.MethodExecutor;
import util.ModelAndView;
import util.UrlMethod;

public class ProcessRequest extends HttpServlet {

    private HashMap<UrlMethod, Method> urlMap;
    private String prefix;
    private String surfix;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.urlMap = (HashMap<UrlMethod, Method>) context.getAttribute("urlMap");
        this.prefix = context.getInitParameter("view-prefix");
        this.surfix = context.getInitParameter("view-suffix");
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String url = req.getRequestURI();
        String[] uri = url.split("/");
        String output = uri[uri.length - 1];

        if (output.endsWith(".html")) {
            String cheminPhysique = getServletContext().getRealPath("/" + output);
            File fichier = new File(cheminPhysique);

            if (fichier.exists()) {
                res.setContentType("text/html;charset=UTF-8");
                Files.copy(fichier.toPath(), res.getOutputStream());
                return;
            } else {
                res.sendError(404, "Fichier introuvable");
                return;
            }
        }

        String httpMethod = req.getMethod();
        // ⚠️ Récupérer TOUT le chemin après le context path
        String contextPath = req.getContextPath();
        String fullPath = url.substring(contextPath.length());   // "/api/users/42"

        // Chercher une route qui matche (avec ou sans {id})
        Method method = null;
        UrlMethod matchedUrl = null;

        if (urlMap != null) {
            for (Map.Entry<UrlMethod, Method> entry : urlMap.entrySet()) {
                UrlMethod u = entry.getKey();
                if (u.matches(fullPath, httpMethod)) {
                    method = entry.getValue();
                    matchedUrl = u;
                    break;
                }
            }
        }

        if (method == null) {
            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("Aucune methode trouvee pour l'URL : " + url + " et la Methode : " + httpMethod);
            out.println("");
            out.println("URLs disponibles :");

            if (urlMap != null) {
                for (UrlMethod u : urlMap.keySet()) {
                    Method m = urlMap.get(u);
                    out.println(u.getMethod() + " " + u.getUrl() + "    " + m.getDeclaringClass().getName() + "."
                            + m.getName() + "()");
                }
            }
            return;
        }

        try {
            // ✅ Extraire les path variables et les mettre dans la requête
            if (matchedUrl != null) {
                Map<String, String> pathVars = matchedUrl.extractPathVariables(fullPath);
                for (Map.Entry<String, String> e : pathVars.entrySet()) {
                    req.setAttribute("__path_vars_" + e.getKey(), e.getValue());
                }
            }

            // ✅ Appel avec req + res
            Object obj = MethodExecutor.execute(method, req, res);

            // ✅ Détection JSON (RestController ou @ResponseBody ou retour Map/List)
            boolean isJson = MethodExecutor.isJsonResponse(method)
                    || obj instanceof Map
                    || obj instanceof java.util.List
                    || (obj != null && obj.getClass().isArray());

            if (isJson) {
                res.setContentType("application/json;charset=UTF-8");
                res.setCharacterEncoding("UTF-8");
                PrintWriter out = res.getWriter();
                out.println(JsonSerializer.toJson(obj));
                return;
            }

            if (obj instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) obj;
                Map<String, Object> map = mv.getModel();

                for (Map.Entry<String, Object> mm : map.entrySet()) {
                    req.setAttribute(mm.getKey(), mm.getValue());
                }
                String path = this.prefix + mv.getView() + this.surfix;
                RequestDispatcher requestDispatcher = req.getRequestDispatcher(path);
                requestDispatcher.forward(req, res);
                return;
            }

            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("URL     : " + url);
            out.println("Methode : " + method.getName() + "()");
            out.println("Execution de :" + method);
            out.println("Resultat : " + obj);

        } catch (Exception e) {
            res.setContentType("application/json;charset=UTF-8");
            res.setStatus(500);
            PrintWriter out = res.getWriter();
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            out.println("{\"error\":\"" + msg.replace("\"", "'") + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }
}
