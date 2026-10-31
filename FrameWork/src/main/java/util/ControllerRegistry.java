package util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre singleton des instances de controleurs.
 * Une seule instance par classe de controleur, reutilisee pour toutes les requetes.
 *
 * ATTENTION : les controleurs doivent etre SANS ETAT (stateless).
 * Ne stockez pas de donnees dans les champs, utilisez la session ou la BDD.
 */
public class ControllerRegistry {

    private static ControllerRegistry instance;

    // ConcurrentHashMap : thread-safe pour Tomcat multi-thread
    private final Map<Class<?>, Object> controllers = new ConcurrentHashMap<>();

    private ControllerRegistry() {}

    public static ControllerRegistry getInstance() {
        if (instance == null) {
            synchronized (ControllerRegistry.class) {
                if (instance == null) {
                    instance = new ControllerRegistry();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) throws Exception {
        Object existing = controllers.get(clazz);
        if (existing != null) {
            return (T) existing;
        }

        T newInstance = clazz.getDeclaredConstructor().newInstance();
        controllers.put(clazz, newInstance);
        System.out.println("[MonFramework] Controleur instancie (singleton) : " + clazz.getName());
        return newInstance;
    }

    public int size() {
        return controllers.size();
    }
}