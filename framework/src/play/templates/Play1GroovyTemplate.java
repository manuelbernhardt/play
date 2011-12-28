package play.templates;

import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;

public class Play1GroovyTemplate extends GroovyTemplate {

    public Play1GroovyTemplate(String name, String source) {
        super(name, source);
    }

    public Play1GroovyTemplate(String source) {
        super(source);
    }

    @Override
    void throwException(Throwable e) {
        
        try {
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                if (stackTraceElement.getClassName().equals(compiledTemplateName) || stackTraceElement.getClassName().startsWith(compiledTemplateName + "$_run_closure")) {
                    if (doBodyLines.contains(stackTraceElement.getLineNumber())) {
                        throw new TemplateExecutionException.DoBodyException(e);
                    } else if (e instanceof TagInternalException) {
                        throw (TagInternalException) cleanStackTrace(e);
                    } else if (e instanceof NoRouteFoundException) {
                        NoRouteFoundException ex = (NoRouteFoundException) cleanStackTrace(e);
                        if (ex.getFile() != null) {
                            throw new NoRouteFoundException(ex.getFile(), this, this.linesMatrix.get(stackTraceElement.getLineNumber()));
                        }
                        throw new NoRouteFoundException(ex.getAction(), ex.getArgs(), this, this.linesMatrix.get(stackTraceElement.getLineNumber()));
                    } else if (e instanceof TemplateExecutionException) {
                        TemplateExecutionException ex = ((TemplateExecutionException) e);
                        play.exceptions.TemplateExecutionException pe = new play.exceptions.TemplateExecutionException(ex.getTemplate(), ex.getLineNumber(), ex.getMessage(), ex.getCause());
                        throw (play.exceptions.TemplateExecutionException) cleanStackTrace(pe);
                    } else {
                        throw new TemplateExecutionException(this, this.linesMatrix.get(stackTraceElement.getLineNumber()), e.getMessage(), cleanStackTrace(e));
                    }
                }
                if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                    throw new JavaExecutionException(Play.classes.getApplicationClass(stackTraceElement.getClassName()), stackTraceElement.getLineNumber(), cleanStackTrace(e));
                }
            }
            throw new RuntimeException(e);
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
        }

    }

    @Override
    void handleException(TemplateEngineException e) {
        switch (e.getExceptionType()) {
            case NO_ROUTE_FOUND:
                if (((NoRouteFoundException) e.getCause()).isSourceAvailable()) {
                    throw e;
                }
                throwException(e.getCause());
                break;
            case PLAY:
                throw (PlayException) cleanStackTrace(e.getCause());
        }
    }
}
