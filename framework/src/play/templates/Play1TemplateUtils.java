package play.templates;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Codec;
import play.libs.I18N;
import play.libs.Time;
import play.utils.HTML;
import play.utils.Java;
import play.vfs.VirtualFile;

public class Play1TemplateUtils extends TemplateUtils {

    @Override
    public void logWarn(String message, Object... args) {
        Logger.warn(message, args);
    }

    @Override
    public void logWarn(Throwable e, String message, Object... args) {
        Logger.warn(e, message, args);
    }

    @Override
    public void logError(String message, Object... args) {
        Logger.error(message, args);
    }

    @Override
    public void logError(Throwable e, String message) {
        Logger.error(e, message);
    }

    @Override
    public void logTraceIfEnabled(String message, Object... args) {
        if(Logger.isTraceEnabled()) {
            Logger.trace(message, args);
        }
    }

    @Override
    public boolean isDevMode() {
        return Play.mode == Play.Mode.DEV;
    }

    @Override
    public boolean usePrecompiled() {
        return Play.usePrecompiled;
    }

    @Override
    public PlayVirtualFile findTemplateWithPath(String path) {
        for (VirtualFile vf : Play.templatesPath) {
            if (vf == null) {
                continue;
            }
            VirtualFile tf = vf.child(path);
            if (tf.exists()) {
                return tf;
            }
        }
        return null;
    }

    @Override
    public PlayVirtualFile findFileWithPath(String path) {
        return Play.getVirtualFile(path);
    }

    @Override
    public List<PlayVirtualFile> list(PlayVirtualFile parent) {
        List<VirtualFile> children = ((VirtualFile) parent).list();
        List<PlayVirtualFile> abstractChildren = new ArrayList<PlayVirtualFile>();
        abstractChildren.addAll(children);
        return abstractChildren;
    }

    @Override
    public String encodeBASE64(byte[] value) {
        return Codec.encodeBASE64(value);
    }

    @Override
    public byte[] decodeBASE64(String value) {
        return Codec.decodeBASE64(value);
    }

    @Override
    public String htmlEscape(String value) {
        return HTML.htmlEscape(value.toString());
    }

    @Override
    public Integer parseDuration(String duration) {
        return Time.parseDuration(duration);
    }

    @Override
    public String getLang() {
        return Lang.get();
    }

    @Override
    public String getMessage(Object key, Object... args) {
        return Messages.get(key, args);
    }

    @Override
    public String getDateFormat() {
        return I18N.getDateFormat();
    }

    @Override
    public String getCurrencySymbol(String currencySymbol) {
        return I18N.getCurrencySymbol(currencySymbol);
    }

    @Override
    public Object getMessages() {
        return new Messages();
    }

    @Override
    public Object getPlay() {
        return new Play();
    }

    public String getDefaultWebEncoding() {
        return Play.defaultWebEncoding;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Play.classloader;
    }

    @Override
    public List<Class> getAssignableClasses(Class clazz) {
        return Play.classloader.getAssignableClasses(clazz);
    }

    @Override
    public List<Class> getAllClasses() {
        return Play.classloader.getAllClasses();
    }

    @Override
    protected String getAbsoluteApplicationPath() {
        return Play.applicationPath.getAbsolutePath();
    }

    @Override
    public byte[] serialize(Object o) throws Exception {
        return Java.serialize(o);
    }

    @Override
    public Object deserialize(byte[] b) throws Exception {
        return Java.deserialize(b);
    }

    @Override
    public Object getCached(String key) {
        return Cache.get(key);
    }

    @Override
    public void setCached(String key, String data, Integer duration) {
        Cache.set(key, data, duration + "s");
    }
}
