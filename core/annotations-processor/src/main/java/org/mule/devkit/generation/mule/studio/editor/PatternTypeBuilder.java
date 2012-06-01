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

package org.mule.devkit.generation.mule.studio.editor;

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.PatternType;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PatternTypeBuilder extends BaseStudioXmlBuilder {

    public PatternTypeBuilder(GeneratorContext context, DevKitExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public PatternType build() {
        PatternType patternType = createPatternType();
        if (executableElement.getAnnotation(Processor.class) != null) {
            Collection<AttributeCategory> attributeCategories = processMethodParameters();
            patternType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);
        }
        return patternType;
    }

    protected PatternType createPatternType() {
        PatternType patternType = new PatternType();
        patternType.setLocalId(helper.getLocalId(executableElement));
        patternType.setCaption(helper.getFormattedCaption(executableElement));
        patternType.setAbstract(true);

        if (executableElement.getAnnotation(Processor.class) != null) {
            patternType.setExtends(helper.getUrl(typeElement) + helper.getGlobalRefId(typeElement.name()));
            patternType.setReturnType(typeMirrorUtils.getJavaType(executableElement));
        } else if (executableElement.getAnnotation(Transformer.class) != null) {
            patternType.setExtends(helper.getUrl(typeElement) + AbstractTransformerBuilder.ABSTRACT_TRANSFORMER_LOCAL_ID);
            patternType.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));
        }

        patternType.setIcon(getIcon());
        patternType.setImage(getImage());
        return patternType;
    }

    protected String getImage() {
        if (executableElement.getAnnotation(Transformer.class) != null) {
            return helper.getTransformerImage(typeElement);
        } else {
            return helper.getConnectorImage(typeElement);
        }
    }

    protected String getIcon() {
        if (executableElement.getAnnotation(Transformer.class) != null) {
            return helper.getTransformerIcon(typeElement);
        } else {
            return helper.getConnectorIcon(typeElement);
        }
    }

    @Override
    protected void processConnectionAttributes(Map<String, Group> groupsByName, Map<String, AttributeCategory> attributeCategoriesByName) {
        if (typeElement.usesConnectionManager()) {
            Group connectionAttributesGroup = new Group();
            connectionAttributesGroup.setCaption(helper.formatCaption(CONNECTION_GROUP_NAME));
            connectionAttributesGroup.setId(StringUtils.uncapitalize(CONNECTION_GROUP_NAME));

            AttributeType label = new AttributeType();
            label.setCaption(String.format(CONNECTION_GROUP_LABEL, helper.getFormattedCaption(typeElement)));

            AttributeType newLine = new AttributeType();
            newLine.setCaption("");

            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupLabel(label));
            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupLabel(newLine));

            groupsByName.put(CONNECTION_GROUP_NAME, connectionAttributesGroup);

            List<AttributeType> connectionAttributes = getConnectionAttributes(typeElement);
            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().addAll(helper.createJAXBElements(connectionAttributes));

            AttributeCategory connectionAttributeCategory = new AttributeCategory();
            connectionAttributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION));
            connectionAttributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION));
            attributeCategoriesByName.put(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION, connectionAttributeCategory);
            connectionAttributeCategory.getGroup().add(connectionAttributesGroup);
        }
    }
}