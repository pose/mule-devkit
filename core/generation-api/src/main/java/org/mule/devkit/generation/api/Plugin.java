package org.mule.devkit.generation.api;

import java.util.List;

/**
 * Plugin that works on the generated source code.
 *
 * This plugin will be called after the default code generation
 * has finished.
 */
public interface Plugin {
    /**
     * Gets the option name to turn on this plugin-in.
     *
     * For example, if "abc" is returned, "-abc" will
     * turn on this plugin. If null is returned then the
     * plugin is turn on implicitly.
     */
    String getOptionName();

    /**
     * Retrieve a list of validators for the specified object type
     *
     * @return A list of validators implementing Validator
     */
    public abstract List<Validator> getValidators();

    /**
     * Retrieve a list of generators for the specified object type
     *
     * @return A list of validators implementing Generator
     */
    public abstract List<Generator> getGenerators();
}
