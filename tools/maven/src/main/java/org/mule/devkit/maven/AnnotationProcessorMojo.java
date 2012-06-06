/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;
import java.util.List;
import java.util.Set;

@MojoGoal("generate-sources")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase("generate-sources")
public class AnnotationProcessorMojo extends AbstractAnnotationProcessorMojo {

    private static String[] processors = {"org.mule.devkit.apt.AnnotationProcessor"};

    /**
     * project classpath
     */
    @MojoParameter(expression = "${project.compileClasspathElements}", required = true, readonly = true)
    @SuppressWarnings("unchecked")
    private List classpathElements;

    @MojoParameter(expression = "${project.build.directory}/generated-sources/mule", required = true)
    private File defaultOutputDirectory;

    @MojoParameter(required = false, expression = "${project.build.outputDirectory}", description = "Set the destination directory for class files (same behaviour of -d option)")
    private File outputClassDirectory;

    @MojoParameter(required = false, expression = "${devkit.javadoc.check.skip}", description = "Skip JavaDoc validation", defaultValue = "false")
    private boolean skipJavaDocValidation;

    @Override
    protected File getOutputClassDirectory() {
        return outputClassDirectory;
    }

    @Override
    protected String[] getProcessors() {
        return processors;
    }

    @Override
    protected void addCompileSourceRoot(MavenProject project, String dir) {
        project.addCompileSourceRoot(dir);
    }

    @Override
    public File getDefaultOutputDirectory() {
        return defaultOutputDirectory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Set<String> getClasspathElements(Set<String> result) {
        List<Resource> resources = project.getResources();

        if (resources != null) {
            for (Resource r : resources) {
                result.add(r.getDirectory());
            }
        }

        result.addAll(classpathElements);

        return result;
    }

    @Override
    protected void addCompilerArguments(List<String> options) {
        if (skipJavaDocValidation) {
            options.add("-AskipJavaDocValidation=true");
        }
        if (skipStudioPluginPackage) {
            options.add("-AskipStudioPluginPackage=true");
        }

        super.addCompilerArguments(options);
    }
}