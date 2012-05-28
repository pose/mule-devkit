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

package org.mule.devkit.maven.studio;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.mule.devkit.generation.mule.studio.MuleStudioSiteXmlGenerator;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class StudioSiteXmlBuilder extends UpdateSiteElementsBuilder
{
    private String category;

    private TokensReplacer tokensReplacer;

    StudioSiteXmlBuilder(String pluginName, String pluginVersion, String muleAppName, String outputDirectory, File classesDirectory, String category) {
        super(pluginName, pluginVersion, muleAppName, outputDirectory, classesDirectory);
        this.category = StringUtils.isEmpty(category) ? "Connectors" : category;
        this.tokensReplacer = new TokensReplacer(buildTokens());

    }


    private Map<String, String> buildTokens() {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("%CATEGORY%", category);
        tokens.put("%VERSION%", pluginVersion);

        return tokens;
    }
    @Override
    public File build() throws MojoExecutionException {
        File siteXmlFile = new File(updateSitePath, MuleStudioSiteXmlGenerator.SITE_XML);
        File file = new File(classesDirectory, MuleStudioSiteXmlGenerator.SITE_XML);


        if (!file.exists()) {
            throw new MojoExecutionException("Error while packaging Mule Studio Site: " + file.getName() + " does not exist");
        }
        tokensReplacer.replaceTokensOn(file);

        try {
            FileUtils.copyFile(file, siteXmlFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while packaging Mule Studio Site: " + file.getName() + " does not exist");
        }

        return siteXmlFile;

    }
}
