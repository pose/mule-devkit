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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;


public class MuleStudioSiteXmlGenerator extends AbstractMessageGenerator {

    protected static final String SEPARATOR = File.separator;
    public static final String SITE_XML = "site.xml";

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

            Element site = document.createElement("site");
            document.appendChild(site);

            Element feature = document.createElement("feature");
            feature.setAttribute("url", "features" + SEPARATOR + MuleStudioFeatureGenerator.STUDIO_PREFIX + typeElement.name() + "_%VERSION%" +".jar");
            feature.setAttribute("id", MuleStudioFeatureGenerator.STUDIO_PREFIX + typeElement.name() );
            feature.setAttribute("version", "%VERSION%");

            Element category = document.createElement("category");
            category.setAttribute("name", "%CATEGORY%");

            feature.appendChild(category);

            Element categoryDef = document.createElement("category-def");
            categoryDef.setAttribute("name", "%CATEGORY%");
            categoryDef.setAttribute("label", "%CATEGORY%");
            site.appendChild(feature);
            site.appendChild(categoryDef);


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(ctx().getCodeModel().getCodeWriter().openBinary(null, SITE_XML));
            transformer.transform(source, result);



        } catch (Exception e) {
            throw new GenerationException("Error generating Mule Studio plugin.xml", e);
        }
    }
}
