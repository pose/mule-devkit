package org.mule.devkit.generation.studio;

import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;
import org.mule.devkit.generation.studio.editor.MuleStudioEditorXmlGenerator;

import java.util.ArrayList;
import java.util.List;

public class MuleStudioPlugin implements Plugin {
    private List<AnnotationVerifier> annotationVerifiers;
    private List<Generator> generators;

    public MuleStudioPlugin() {
        generators = new ArrayList<Generator>();
        generators.add(new MuleStudioManifestGenerator());
        generators.add(new MuleStudioEditorXmlGenerator());
        generators.add(new MuleStudioPluginActivatorGenerator());
        generators.add(new MuleStudioPluginXmlGenerator());
        generators.add(new MuleStudioIconsGenerator());
        generators.add(new MuleStudioFeatureGenerator());
        generators.add(new MuleStudioSiteXmlGenerator());

        annotationVerifiers = new ArrayList<AnnotationVerifier>();
        annotationVerifiers.add(new MuleStudioAnnotationVerifier());
    }

    @Override
    public String getOptionName() {
        return "enabledStudioPluginPackage";
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
