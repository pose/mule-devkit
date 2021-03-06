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
import org.mule.devkit.generation.studio.editor.MuleStudioEditorXmlGenerator;
import org.mule.devkit.model.Type;

import java.util.Arrays;
import java.util.List;

/**
 * Acts as a composite of generators that contribute to the structure of the Mule Studio plugin
 */
public class MuleStudioPluginGenerator extends AbstractMuleStudioGenerator {

    public static final String[] GENERATED_FILES = new String[]{
            MuleStudioManifestGenerator.MANIFEST_FILE_NAME,
            MuleStudioEditorXmlGenerator.EDITOR_XML_FILE_NAME,
            MuleStudioPluginActivatorGenerator.ACTIVATOR_PATH,
            MuleStudioPluginXmlGenerator.PLUGIN_XML_FILE_NAME};

    @Override
    public boolean shouldGenerate(Type type) {
        return true;
    }

    @Override
    public void generate(Type type) throws GenerationException {
        List<? extends AbstractMuleStudioGenerator> muleStudioGenerators = (List<? extends AbstractMuleStudioGenerator>) Arrays.asList(
                new MuleStudioManifestGenerator(),
                new MuleStudioEditorXmlGenerator(),
                new MuleStudioPluginActivatorGenerator(),
                new MuleStudioPluginXmlGenerator(),
                new MuleStudioIconsGenerator());
        for (AbstractMuleStudioGenerator muleStudioGenerator : muleStudioGenerators) {
            muleStudioGenerator.setCtx(ctx());
            muleStudioGenerator.generate(type);
        }
    }
}