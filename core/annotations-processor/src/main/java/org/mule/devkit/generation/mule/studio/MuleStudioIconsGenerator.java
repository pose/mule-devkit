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

package org.mule.devkit.generation.mule.studio;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.display.Icons;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.Type;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MuleStudioIconsGenerator extends AbstractMessageGenerator {

    public static final String ICONS_FOLDER = "icons/";

    @Override
    public boolean shouldGenerate(Type type) {
        return !ctx().hasOption("skipStudioPluginPackage");
    }

    @Override
    public void generate(Type type) throws GenerationException {
        Icons icons = type.getAnnotation(Icons.class);
        if (icons != null) {
            copyFile(icons.connectorSmall(), "icons/small", type);
            copyFile(icons.connectorLarge(), "icons/large", type);
            if(type.hasMethodsAnnotatedWith(Transformer.class)) {
                copyFile(icons.transformerSmall(), "icons/small", type);
                copyFile(icons.transformerLarge(), "icons/large", type);
            }
            if(type.hasMethodsAnnotatedWith(Source.class)) {
                copyFile(icons.endpointSmall(), "icons/small", type);
                copyFile(icons.endpointLarge(), "icons/large", type);
            }
        } else {
            copyFile(String.format(Icons.GENERIC_CLOUD_CONNECTOR_SMALL, type.name()), "icons/small", type);
            copyFile(String.format(Icons.GENERIC_CLOUD_CONNECTOR_LARGE, type.name()), "icons/large", type);
            if(type.hasMethodsAnnotatedWith(Transformer.class)) {
                copyFile(String.format(Icons.GENERIC_TRANSFORMER_SMALL, type.name()), "icons/small", type);
                copyFile(String.format(Icons.GENERIC_TRANSFORMER_LARGE, type.name()), "icons/large", type);
            }
            if(type.hasMethodsAnnotatedWith(Source.class)) {
                copyFile(String.format(Icons.GENERIC_ENDPOINT_SMALL, type.name()), "icons/small", type);
                copyFile(String.format(Icons.GENERIC_ENDPOINT_LARGE, type.name()), "icons/large", type);
            }
        }
    }

    private void copyFile(String fileName, String folder, Type type) throws GenerationException {
        String sourcePath = type.getPathToSourceFile();
        int packageCount = StringUtils.countMatches(type.getQualifiedName().toString(), ".") + 1;
        while (packageCount > 0) {
            sourcePath = sourcePath.substring(0, sourcePath.lastIndexOf("/"));
            packageCount--;
        }
        OutputStream outputStream = null;
        try {
            outputStream = createFile(folder, fileName);
            File fileToCopy = new File(sourcePath, fileName);
            if(!fileToCopy.exists()) {
                throw new GenerationException("The following icon file does not exist: " + fileToCopy.getAbsolutePath());
            }
            IOUtils.copy(new FileInputStream(fileToCopy), outputStream);
        } catch (IOException e) {
            throw new GenerationException("Error copying icons to output folder: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private OutputStream createFile(String folder, String fileName) throws GenerationException {
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        try {
            return ctx().getCodeModel().getCodeWriter().openBinary(null, folder + '/' + fileName);
        } catch (IOException e) {
            throw new GenerationException("Could not create file or folder " + fileName + ": " + e.getMessage(), e);
        }
    }
}