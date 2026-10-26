package util;

import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Sauvegarde un fichier uploadé dans un dossier donné.
 * Le nom est préfixé par un timestamp pour éviter les collisions.
 * Les caractères dangereux sont remplacés.
 */
public class FileStorage {

    public static File save(Part part, String uploadDir) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalName = part.getSubmittedFileName();
        if (originalName == null || originalName.isEmpty()) {
            originalName = "fichier";
        }

        // Nettoyage : supprime tout sauf lettres, chiffres, point, tiret, underscore
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Préfixe timestamp pour éviter les collisions
        String finalName = System.currentTimeMillis() + "_" + safeName;

        File dest = new File(dir, finalName);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }
}