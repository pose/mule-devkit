package org.mule.devkit.generation.studio;

import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;
import org.mule.devkit.generation.api.Validator;
import org.mule.devkit.generation.studio.editor.MuleStudioEditorXmlGenerator;

import java.util.ArrayList;
import java.util.List;

public class MuleStudioPlugin implements Plugin {
    private List<Validator> validators;
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

        validators = new ArrayList<Validator>();
        validators.add(new MuleStudioValidator());
    }

    @Override
    public String getOptionName() {
        return "skipStudioPluginPackage";
    }

    @Override
    public List<Validator> getValidators() {
        return validators;
    }

    @Override
    public List<Generator> getGenerators() {
        return generators;
    }
}
