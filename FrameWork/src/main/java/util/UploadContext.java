package util;

/**
 * Stocke le chemin du dossier d'upload, initialisé par ProcessRequest.
 * Accessible depuis n'importe quel contrôleur.
 */
public class UploadContext {
    private static String uploadDir;

    public static void setUploadDir(String dir) {
        uploadDir = dir;
    }

    public static String getUploadDir() {
        return uploadDir;
    }
}