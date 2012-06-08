package org.mule.devkit.generation.javadoc;

import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;

import java.util.ArrayList;
import java.util.List;

public class JavaDocPlugin implements Plugin {
    private List<AnnotationVerifier> annotationVerifiers;
    private List<Generator> generators;

    public JavaDocPlugin() {
        generators = new ArrayList<Generator>();

        annotationVerifiers = new ArrayList<AnnotationVerifier>();
        annotationVerifiers.add(new JavaDocAnnotationVerifier());
    }

    @Override
    public String getOptionName() {
        return "enableJavaDocValidation";
    }

    @Override
    public List<AnnotationVerifier> getAnnotationVerifiers() {
        return annotationVerifiers;
    }

    @Override
    public List<Generator> getGenerators() {
        return generators;
    }
}
