package org.mule.devkit.generation.javadoc;

import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;
import org.mule.devkit.generation.api.Validator;

import java.util.ArrayList;
import java.util.List;

public class JavaDocPlugin implements Plugin {
    private List<Validator> validators;
    private List<Generator> generators;

    public JavaDocPlugin() {
        generators = new ArrayList<Generator>();

        validators = new ArrayList<Validator>();
        validators.add(new JavaDocValidator());
    }

    @Override
    public String getOptionName() {
        return "enableJavaDocValidation";
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
