package play.templates;

import java.util.List;

/**
 * Load templates
 */
public class TemplateLoader {

    public static String getUniqueNumberForTemplateFile(String path) {
        return GenericTemplateLoader.getUniqueNumberForTemplateFile(path);
    }
    
    /**
     * Load a template from a virtual file
     * @param file A VirtualFile
     * @return The executable template
     */
    public static Template load(PlayVirtualFile file) {
        try {
            // Try with plugin
            Template fromPlugin = Play1TemplateEngine.loadFromPlugin(file);
            if(fromPlugin != null) {
                return fromPlugin;
            }

            return GenericTemplateLoader.load(file);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

    /**
     * Load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate load(String key, String source) {
        try {
            return GenericTemplateLoader.load(key, source);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

    /**
     * Clean the cache for that key
     * Then load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate load(String key, String source, boolean reload) {
        try {
            return GenericTemplateLoader.load(key, source, reload);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

    /**
     * Load template from a String, but don't cache it
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate loadString(String source) {
        try {
            return GenericTemplateLoader.loadString(source);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        GenericTemplateLoader.cleanCompiledCache();
    }

    /**
     * Cleans the specified key from the cache
     * @param key The template key
     */
    public static void cleanCompiledCache(String key) {
        GenericTemplateLoader.cleanCompiledCache(key);
    }

    /**
     * Load a template
     * @param path The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        try {
            return GenericTemplateLoader.load(path);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

    /**
     * List all found templates
     * @return A list of executable templates
     */
    public static List<Template> getAllTemplate() {
        try {
            return GenericTemplateLoader.getAllTemplate();
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
            return null;
        }
    }

}
