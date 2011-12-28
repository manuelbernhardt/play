package play;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.templates.Play1TemplateEngine;
import play.vfs.VirtualFile;

/**
 * Builder-pattern-builder for Play-class..
 *
 * It's kind of odd since Play only uses statics,
 * But it basically inits the needed properties for Play-object to work in unittests
 */
public class PlayBuilder {

    public Properties configuration = new Properties();

    public PlayBuilder withConfiguration(Properties config){
        this.configuration = config;
        return this;
    }


    @SuppressWarnings({"deprecation"})
    public void build(){
        
        Play.configuration = configuration;
        Play.classes = new ApplicationClasses();
        Play.javaPath = new ArrayList<VirtualFile>();
        Play.applicationPath = new File(".");
        Play.classloader = new ApplicationClassloader();
        Play.plugins = Collections.unmodifiableList( new ArrayList<PlayPlugin>());

        Play.groovyTemplateEngine = new Play1TemplateEngine();
        Play.groovyTemplateEngine.startup();

    }
}
