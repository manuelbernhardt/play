package play.templates;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import play.data.validation.*;
import play.exceptions.TagInternalException;
import play.templates.exceptions.TemplateExecutionException;
import play.mvc.Router;
import play.mvc.Scope;

@FastTags.Namespace("_play")
public class Play1FastTags extends FastTags {

    /**
     * The field tag is a helper, based on the spirit of Don't Repeat Yourself.
     * @param args tag attributes
     * @param body tag inner body
     * @param out the output writer
     * @param template enclosing template
     * @param fromLine template line number where the tag is defined
     */
    public static void _field(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        String _arg = args.get("arg").toString();
        field.put("name", _arg);
        field.put("id", _arg.replace('.','_'));
        field.put("flash", Scope.Flash.current().get(_arg));
        field.put("flashArray", field.get("flash") != null && !StringUtils.isEmpty(field.get("flash").toString()) ? field.get("flash").toString().split(",") : new String[0]);
        field.put("error", Validation.error(_arg));
        field.put("errorClass", field.get("error") != null ? "hasError" : "");
        String[] pieces = _arg.split("\\.");
        Object obj = body.getProperty(pieces[0]);
        if(obj != null){
            if(pieces.length > 1){
                for(int i = 1; i < pieces.length; i++){
                    try{
                        Field f = obj.getClass().getField(pieces[i]);
                        if(i == (pieces.length-1)){
                            try{
                                Method getter = obj.getClass().getMethod("get"+JavaExtensions.capFirst(f.getName()));
                                field.put("value", getter.invoke(obj, new Object[0]));
                            }catch(NoSuchMethodException e){
                                field.put("value",f.get(obj).toString());
                            }
                        }else{
                            obj = f.get(obj);
                        }
                    }catch(Exception e){
                        // if there is a problem reading the field we dont set any value
                    }
                }
            }else{
                field.put("value", obj);
            }
        }
        body.setProperty("field", field);
        body.call();
    }

    public static void _ifErrors(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        if (Validation.hasErrors()) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }

    public static void _ifError(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        if (Validation.hasError(args.get("arg").toString())) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }

    public static void _errorClass(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        if (Validation.hasError(args.get("arg").toString())) {
            out.print("hasError");
        }
    }

    public static void _error(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null && args.get("key") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        String key = args.get("arg") == null ? args.get("key") + "" : args.get("arg") + "";
        play.data.validation.Error error = Validation.error(key);
        if (error != null) {
            if (args.get("field") == null) {
                out.print(error.message());
            } else {
                out.print(error.message(args.get("field") + ""));
            }
        }
    }

    public static void _jsRoute(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        final Object arg = args.get("arg");
        if (!(arg instanceof Router.ActionDefinition)) {
            throw new TemplateExecutionException(template.template, fromLine, "Wrong parameter type, try #{jsRoute @Application.index() /}", new TagInternalException("Wrong parameter type"));
        }
        final Router.ActionDefinition action = (Router.ActionDefinition)arg;
        out.print("{");
        if (action.args.isEmpty()) {
            out.print("url: function() { return '" + action.url.replace("&amp;", "&") + "'; },");
        } else {
            out.print("url: function(args) { var pattern = '" + action.url.replace("&amp;", "&") + "'; for (var key in args) { pattern = pattern.replace(':'+key, args[key]); } return pattern; },");
        }
        out.print("method: '" + action.method + "'");
        out.print("}");
    }

    /**
     * Generates a html form element linked to a controller action
     * @param args tag attributes
     * @param body tag inner body
     * @param out the output writer
     * @param template enclosing template
     * @param fromLine template line number where the tag is defined
     */
    public static void _form(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        Router.ActionDefinition actionDef = (Router.ActionDefinition) args.get("arg");
        if (actionDef == null) {
            actionDef = (Router.ActionDefinition) args.get("action");
        }
        String enctype = (String) args.get("enctype");
        if (enctype == null) {
            enctype = "application/x-www-form-urlencoded";
        }
        if (actionDef.star) {
            actionDef.method = "POST"; // prefer POST for form ....
        }
        if (args.containsKey("method")) {
            actionDef.method = args.get("method").toString();
        }
        if (!("GET".equals(actionDef.method) || "POST".equals(actionDef.method))) {
            String separator = actionDef.url.indexOf('?') != -1 ? "&" : "?";
            actionDef.url += separator + "x-http-method-override=" + actionDef.method.toUpperCase();
            actionDef.method = "POST";
        }
        String encoding = TemplateEngine.engine.getCurrentResponseEncoding();
        out.print("<form action=\"" + actionDef.url + "\" method=\"" + actionDef.method.toLowerCase() + "\" accept-charset=\""+encoding+"\" enctype=\"" + enctype + "\" " + serialize(args, "action", "method", "accept-charset", "enctype") + ">");
        if (!("GET".equals(actionDef.method))) {
            _authenticityToken(args, body, out, template, fromLine);
        }
        out.println(JavaExtensions.toString(body));
        out.print("</form>");
    }

    /**
     * Generates a html link to a controller action
     * @param args tag attributes
     * @param body tag inner body
     * @param out the output writer
     * @param template enclosing template
     * @param fromLine template line number where the tag is defined
     */
    public static void _a(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        Router.ActionDefinition actionDef = (Router.ActionDefinition) args.get("arg");
        if (actionDef == null) {
            actionDef = (Router.ActionDefinition) args.get("action");
        }
        if (!("GET".equals(actionDef.method))) {
            if (!("POST".equals(actionDef.method))) {
                String separator = actionDef.url.indexOf('?') != -1 ? "&" : "?";
                actionDef.url += separator + "x-http-method-override=" + actionDef.method;
                actionDef.method = "POST";
            }
            String id = TemplateEngine.utils.UUID();
            out.print("<form method=\"POST\" id=\"" + id + "\" " +(args.containsKey("target") ? "target=\"" + args.get("target") + "\"" : "")+ " style=\"display:none\" action=\"" + actionDef.url + "\">");
            _authenticityToken(args, body, out, template, fromLine);
            out.print("</form>");
            out.print("<a href=\"javascript:document.getElementById('" + id + "').submit();\" " + serialize(args, "href") + ">");
            out.print(JavaExtensions.toString(body));
            out.print("</a>");
        } else {
            out.print("<a href=\"" + actionDef.url + "\" " + serialize(args, "href") + ">");
            out.print(JavaExtensions.toString(body));
            out.print("</a>");
        }
    }


}
