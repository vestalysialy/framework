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
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.JsonSerializer;
import util.MethodExecutor;
import util.ModelAndView;
import util.UploadContext;
import util.UrlMethod;

/**
 * Servlet frontal du framework :
 *  - intercepte toutes les requêtes (url-pattern = /*)
 *  - sert les fichiers statiques (/uploads/*, *.html)
 *  - cherche la route correspondante dans urlMap
 *  - exécute la méthode du contrôleur via MethodExecutor
 *  - sérialise la réponse (JSON, texte, ModelAndView)
 */
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,       // 1 MB
    maxFileSize = 10 * 1024 * 1024,        // 10 MB
    maxRequestSize = 50 * 1024 * 1024      // 50 MB
)
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

        // Initialisation du dossier d'upload (utilisé par UploadController)
        String uploadsPath = context.getRealPath("/uploads");
        UploadContext.setUploadDir(uploadsPath);
        System.out.println("[MonFramework] Dossier uploads : " + uploadsPath);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = url.substring(contextPath.length());
        String httpMethod = req.getMethod();

        if (relativePath.startsWith("/uploads/")) {
            String physicalPath = getServletContext().getRealPath(relativePath);
            File file = new File(physicalPath);

            if (file.exists() && file.isFile()) {
                String mime = getServletContext().getMimeType(file.getName());
                if (mime == null) mime = "application/octet-stream";
                res.setContentType(mime);
                res.setContentLengthLong(file.length());
                Files.copy(file.toPath(), res.getOutputStream());
                return;
            } else {
                res.sendError(404, "Fichier introuvable : " + relativePath);
                return;
            }
        }

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
                res.sendError(404, "Fichier introuvable : " + output);
                return;
            }
        }

        Method method = null;
        UrlMethod matchedUrl = null;

        if (urlMap != null) {
            for (Map.Entry<UrlMethod, Method> entry : urlMap.entrySet()) {
                UrlMethod u = entry.getKey();
                if (u.matches(relativePath, httpMethod)) {
                    method = entry.getValue();
                    matchedUrl = u;
                    break;
                }
            }
        }

        if (method == null) {
            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("Aucune methode trouvee pour l'URL : " + url
                    + " et la Methode : " + httpMethod);
            out.println("");
            out.println("URLs disponibles :");

            if (urlMap != null) {
                for (UrlMethod u : urlMap.keySet()) {
                    Method m = urlMap.get(u);
                    out.println(u.getMethod() + " " + u.getUrl() + "    "
                            + m.getDeclaringClass().getName() + "." + m.getName() + "()");
                }
            }
            return;
        }

        try {
            // Extraire et stocker les path variables ({id}, ...)
            if (matchedUrl != null) {
                Map<String, String> pathVars = matchedUrl.extractPathVariables(relativePath);
                for (Map.Entry<String, String> e : pathVars.entrySet()) {
                    req.setAttribute("__path_vars_" + e.getKey(), e.getValue());
                }
            }

            // Invoquer la méthode du contrôleur
            Object obj = MethodExecutor.execute(method, req, res);

            // NOUVEAU : si le contrôleur a déjà écrit/redirigé, on ne fait rien
            if (res.isCommitted()) {
                return;
            }

            // Si la méthode retourne void, on ne fait rien non plus
            if (method.getReturnType() == void.class) {
                return;
            }
            // Détection JSON
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

            // ModelAndView → forward vers une vue JSP
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

            // Autre (String, objet...) → texte brut
            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            if (obj instanceof String) {
                // Cas d'un retour String explicite : afficher tel quel
                out.println(obj);
            } else {
                out.println("URL     : " + url);
                out.println("Methode : " + method.getName() + "()");
                out.println("Execution de :" + method);
                out.println("Resultat : " + obj);
            }

        } catch (util.UnauthorizedException e) {
            // 401 : non authentifie -> redirection vers la page de login
            res.sendRedirect(req.getContextPath() + "/login.html");
        } catch (util.ForbiddenException e) {
            // 403 : role insuffisant
            res.setContentType("application/json;charset=UTF-8");
            res.setStatus(403);
            PrintWriter out = res.getWriter();
            out.println("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }
}