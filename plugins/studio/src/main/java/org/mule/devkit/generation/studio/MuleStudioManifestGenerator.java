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

package org.mule.devkit.generation.studio;

import org.mule.devkit.generation.api.GenerationException;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.devkit.model.Type;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class MuleStudioManifestGenerator extends AbstractMuleStudioGenerator {

    public static final String MANIFEST_FILE_NAME = "META-INF/MANIFEST.MF";

    @Override
    public boolean shouldGenerate(Type type) {
        return true;
    }

    @Override
    public void generate(Type type) throws GenerationException {
        PrintStream printStream = null;
        try {
            OutputStream outputStream = ctx().getCodeModel().getCodeWriter().openBinary(null, MANIFEST_FILE_NAME);
            printStream = new PrintStream(outputStream);
            printStream.append(getManifestContents(type));
            printStream.flush();
        } catch (IOException e) {
            throw new GenerationException("Could not create MANIFEST for Studio plugin: " + e.getMessage(), e);
        } finally {

            IOUtils.closeQuietly(printStream);
        }
    }

    private String getManifestContents(Type type) {
        StringBuilder manfiestContentBuilder = new StringBuilder(100);
        manfiestContentBuilder.append("Manifest-Version: 1.0\n");
        manfiestContentBuilder.append("Bundle-ManifestVersion: 2\n");
        manfiestContentBuilder.append("Bundle-Name: ").append(NameUtils.friendlyNameFromCamelCase(type.getModuleName())).append("\n");
        manfiestContentBuilder.append("Bundle-SymbolicName: " + MuleStudioFeatureGenerator.STUDIO_PREFIX).append(type.getModuleName()).append(";singleton:=true\n");
        manfiestContentBuilder.append("Bundle-Version: %VERSION%\n");
        manfiestContentBuilder.append("Bundle-Activator: org.mule.tooling.ui.contribution.Activator\n");
        manfiestContentBuilder.append("Bundle-Vendor: ").append(type.getJavaDocTagContent("author")).append("\n");
        manfiestContentBuilder.append("Require-Bundle: org.eclipse.ui,\n");
        manfiestContentBuilder.append(" org.eclipse.core.runtime,\n");
        manfiestContentBuilder.append(" org.mule.tooling.core;bundle-version=\"1.0.0\"\n");
        manfiestContentBuilder.append("Bundle-RequiredExecutionEnvironment: JavaSE-1.6\n");
        manfiestContentBuilder.append("Bundle-ActivationPolicy: lazy\n");
        manfiestContentBuilder.append("Eclipse-BundleShape: dir\n");
        return manfiestContentBuilder.toString();
    }
}