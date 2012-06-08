package org.mule.devkit.apt;

import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;
import org.mule.devkit.generation.api.PluginScanner;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SupportedAnnotationTypes(value = {"org.mule.api.annotations.Connector",
        "org.mule.api.annotations.ExpressionLanguage",
        "org.mule.api.annotations.Module"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class AnnotationProcessor extends AbstractAnnotationProcessor {

    @Override
    public List<AnnotationVerifier> getAnnotationVerifiers() {
        List<AnnotationVerifier> annotationVerifiers = new ArrayList<AnnotationVerifier>();

        try {
            List<Plugin> plugins = PluginScanner.getInstance().getAllPlugins(getUserClassLoader(getClass().getClassLoader()));
            for (Plugin p : plugins) {
                if (p.getOptionName() == null) {
                    annotationVerifiers.addAll(p.getAnnotationVerifiers());
                } else if (processingEnv.getOptions().get(p.getOptionName()) != null &&
                        processingEnv.getOptions().get(p.getOptionName()).equals("true")) {
                    annotationVerifiers.addAll(p.getAnnotationVerifiers());
                }
            }
        } catch (MalformedURLException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getLocalizedMessage());
        }

        return annotationVerifiers;
    }

    @Override
    public List<Generator> getGenerators() {
        List<Generator> generators = new ArrayList<Generator>();

        try {
            List<Plugin> plugins = PluginScanner.getInstance().getAllPlugins(getUserClassLoader(getClass().getClassLoader()));
            for (Plugin p : plugins) {
                if (p.getOptionName() == null) {
                    generators.addAll(p.getGenerators());
                } else if (processingEnv.getOptions().get(p.getOptionName()) != null &&
                        processingEnv.getOptions().get(p.getOptionName()).equals("true")) {
                    generators.addAll(p.getGenerators());
                }
            }
        } catch (MalformedURLException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getLocalizedMessage());
        }

        return generators;
    }


    /**
     * Gets a classLoader that can load classes specified via the
     * -classpath option.
     */
    public URLClassLoader getUserClassLoader(ClassLoader parent) throws MalformedURLException {
        List<URL> classpaths = new ArrayList<URL>();

        for (Map.Entry<String, String> me : processingEnv.getOptions().entrySet()) {
            String key = me.getKey();

            if (key.equals("-classpath") || key.equals("-cp")) {
                for (String p : me.getValue().split(File.pathSeparator)) {
                    File file = new File(p);
                    classpaths.add(file.toURL());
                }
            }
        }

        return new URLClassLoader(
                classpaths.toArray(new URL[classpaths.size()]), parent);
    }
}
