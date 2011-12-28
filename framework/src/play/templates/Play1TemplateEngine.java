package play.templates;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.classloading.BytecodeCache;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.data.binding.Unbinder;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.templates.exceptions.TemplateException;
import play.templates.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope;
import play.vfs.VirtualFile;

public class Play1TemplateEngine extends TemplateEngine {


    @Override
    protected TemplateEngine initEngineImplementation() {
        return new Play1TemplateEngine();
    }

    @Override
    protected TemplateUtils initUtilsImplementation() {
        return new Play1TemplateUtils();
    }

    @Override
    public void handleException(Throwable t) {
        if(t instanceof TemplateEngineException) {
            handleException(t.getCause());
        } else if (t.getCause() instanceof WrappedTemplateException) {
            throw (WrappedTemplateException) t.getCause();
        } else if(t.getCause() instanceof WrappedTemplateNotFoundException) {
            throw (WrappedTemplateNotFoundException) t.getCause();
        } else if(t instanceof TemplateException) {
            throw new WrappedTemplateException((TemplateException)t);
        } else if(t instanceof TemplateNotFoundException) {
            throw new WrappedTemplateNotFoundException((TemplateNotFoundException)t);
        } else if(t instanceof NoRouteFoundException) {
            throw (NoRouteFoundException)t;
        } else if(t instanceof PlayException) {
            throw (PlayException)t;
        } else {
            throw new UnexpectedException(t);
        }
    }

    public static Template loadFromPlugin(PlayVirtualFile f) {
        Template pluginProvided = Play.pluginCollection.loadTemplate(f);
        if (pluginProvided != null) {
            return pluginProvided;
        }
        return null;
    }

    @Override
    public void compileGroovyRoutes() {
        for (VirtualFile root : Play.roots) {
            VirtualFile vf = root.child("conf/routes");
            if (vf != null && vf.exists()) {
                Template template = GenericTemplateLoader.load(vf);
                if (template != null) {
                    template.compile();
                }
            }
        }
    }

    @Override
    public BaseTemplate createTemplate(String source) {
        return new Play1GroovyTemplate(source);
    }

    @Override
    public BaseTemplate createTemplate(String key, String source) {
        return new Play1GroovyTemplate(key, source);
    }

    @Override
    public List<PlayVirtualFile> getTemplatePaths() {
        List<PlayVirtualFile> templatePath = new ArrayList<PlayVirtualFile>();
        templatePath.addAll(Play.templatesPath);
        return templatePath;
    }

    @Override
    public File getPrecompiledTemplate(String name) {
        return Play.getFile("precompiled/templates/" + name);
    }

    @Override
    public String reverseWithCheck(String action, boolean absolute) {
        return Router.reverseWithCheck(action, Play.getVirtualFile(action), absolute);
    }

    @Override
    public Object handleActionInvocation(String controller, String name, Object param, boolean absolute, GroovyTemplate.ExecutableTemplate template) {
        try {
            if (controller == null) {
                controller = Http.Request.current().controller;
            }
            String action = controller + "." + name;
            if (action.endsWith(".call")) {
                action = action.substring(0, action.length() - 5);
            }
            try {
                Map<String, Object> r = new HashMap<String, Object>();
                Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
                String[] names = (String[]) actionMethod.getDeclaringClass().getDeclaredField("$" + actionMethod.getName() + LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes())).get(null);
                if (param instanceof Object[]) {
                    if(((Object[])param).length == 1 && ((Object[])param)[0] instanceof Map) {
                        r = (Map<String,Object>)((Object[])param)[0];
                    } else {
                        // too many parameters versus action, possibly a developer error. we must warn him.
                        if (names.length < ((Object[]) param).length) {
                            throw new TemplateEngineException(TemplateEngineException.ExceptionType.NO_ROUTE_FOUND, new NoRouteFoundException(action, null));
                        }
                        for (int i = 0; i < ((Object[]) param).length; i++) {
                            if (((Object[]) param)[i] instanceof Router.ActionDefinition && ((Object[]) param)[i] != null) {
                                Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", actionMethod.getAnnotations());
                            } else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
                                if (((Object[]) param)[i] != null) {
                                    Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", actionMethod.getAnnotations());
                                }
                            } else {
                                Unbinder.unBind(r, ((Object[]) param)[i], i < names.length ? names[i] : "", actionMethod.getAnnotations());
                            }
                        }
                    }
                }
                Router.ActionDefinition def = Router.reverse(action, r);
                if (absolute) {
                    def.absolute();
                }
                if (template.template.name.endsWith(".xml")) {
                    def.url = def.url.replace("&", "&amp;");
                }
                return def;
            } catch (ActionNotFoundException e) {
                throw new TemplateEngineException(TemplateEngineException.ExceptionType.NO_ROUTE_FOUND, new NoRouteFoundException(action, null));
            }
        } catch (Exception e) {
            if (e instanceof PlayException) {
                throw new TemplateEngineException(TemplateEngineException.ExceptionType.PLAY, (PlayException) e);
            }
            throw new TemplateEngineException(TemplateEngineException.ExceptionType.UNEXPECTED, new UnexpectedException(e));
        }
    }

    @Override
    public String getCurrentResponseEncoding() {
        Http.Response currentResponse = Http.Response.current();
        if (currentResponse != null) {
            return currentResponse.encoding;
        }
        return null;
    }

    @Override
    public String getAuthenticityToken() {
        return Scope.Session.current().getAuthenticityToken();
    }

    static boolean isSimpleParam(Class type) {
        return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
    }

    @Override
    public byte[] getCachedTemplate(String name, String source) {
        return BytecodeCache.getBytecode(name, source);
    }

    @Override
    public void cacheBytecode(byte[] byteCode, String name, String source) {
        BytecodeCache.cacheBytecode(byteCode, name, source);
    }

    @Override
    public void deleteBytecode(String name) {
        BytecodeCache.deleteBytecode(name);
    }

    @Override
    public byte[] loadPrecompiledTemplate(String name) {
        File file = Play.getFile("precompiled/templates/" + name);
        return IO.readContent(file);
    }

    @Override
    public List<String> addTemplateExtensions() {
        return Play.pluginCollection.addTemplateExtensions();
    }

    @Override
    public String overrideTemplateSource(BaseTemplate template, String source) {
        return Play.pluginCollection.overrideTemplateSource(template, source);
    }

    @Override
    public List<Class<? extends FastTags>> getFastTags() {
        ArrayList<Class<? extends FastTags>> ft = new ArrayList<Class<? extends FastTags>>();
        ft.add(Play1FastTags.class);
        return ft;
    }
    
    static class WrappedTemplateException extends play.exceptions.TemplateException {

        private final TemplateException e;

        public WrappedTemplateException(TemplateException e) {
            super(e.getTemplate(), e.getLineNumber(), e.getMessage());
            this.e = e;
        }

        @Override
        public List<String> getSource() {
            return e.getSource();
        }

        @Override
        public String getSourceFile() {
            return e.getSourceFile();
        }

        @Override
        public boolean isSourceAvailable() {
            return e.isSourceAvailable();
        }

        @Override
        public String getErrorTitle() {
            return e.getErrorTitle();
        }

        @Override
        public String getErrorDescription() {
            return e.getErrorDescription();
        }

    }

    static class WrappedTemplateNotFoundException extends play.exceptions.TemplateNotFoundException {

        private final TemplateNotFoundException e;

        public WrappedTemplateNotFoundException(TemplateNotFoundException e) {
            super(e.getPath());
            this.e = e;
        }

        @Override
        public String getPath() {
            return e.getPath();
        }

        @Override
        public String getErrorTitle() {
            return e.getErrorTitle();
        }

        @Override
        public String getErrorDescription() {
            return e.getErrorDescription();
        }

        @Override
        public boolean isSourceAvailable() {
            return e.isSourceAvailable();
        }

        @Override
        public String getSourceFile() {
            return e.getSourceFile();
        }

        @Override
        public List<String> getSource() {
            return e.getSource();
        }

        @Override
        public Integer getLineNumber() {
            return e.getLineNumber();
        }

        @Override
        public String getMessage() {
            return e.getMessage();
        }
    }

}
