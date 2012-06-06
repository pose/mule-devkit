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

import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.utils.NameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * <p>Generates the the feature.xml file for an update site</p>
 */
public class MuleStudioFeatureGenerator extends AbstractMessageGenerator {
    public static final String FEATURE_XML_FILENAME = "feature.xml";
    public static final String STUDIO_PREFIX = "org.mule.tooling.ui.extension.";
    public static final String LABEL_SUFFIX = " Mule Studio Extension";

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !ctx().hasOption("skipStudioPluginPackage");
    }

    @Override
    public void generate(DevKitTypeElement typeElement) throws GenerationException {
        try {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            Element feature = document.createElement("feature");
            feature.setAttribute("id", STUDIO_PREFIX + typeElement.name());
            feature.setAttribute("label", NameUtils.friendlyNameFromCamelCase(typeElement.name()) + LABEL_SUFFIX);
            feature.setAttribute("version", "%VERSION%");
            feature.setAttribute("provider-name", "Mulesoft, Inc.");
            document.appendChild(feature);

            Element license = document.createElement("license");
            license.setTextContent("%LICENSE%");
            license.setNodeValue("%LICENSE%");
            feature.appendChild(license);

            Element plugin = document.createElement("plugin");
            plugin.setAttribute("id", STUDIO_PREFIX + typeElement.name());
            plugin.setAttribute("download-size", "0");
            plugin.setAttribute("install-size", "0");
            plugin.setAttribute("version", "%VERSION%");
            plugin.setAttribute("unpack", "true");
            feature.appendChild(plugin);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(ctx().getCodeModel().getCodeWriter().openBinary(null, FEATURE_XML_FILENAME));
            transformer.transform(source, result);

        } catch (Exception e) {
            throw new GenerationException("Error generating Mule Studio plugin.xml", e);
        }
    }
}
