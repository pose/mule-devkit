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
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.PatternType;

import javax.xml.bind.JAXBElement;
import java.util.Collections;
import java.util.List;

public class PatternTypeOperationsBuilder extends BaseStudioXmlBuilder {

    private static final String ALIAS_ID_PREFIX = "org.mule.tooling.ui.modules.core.pattern.";
    private static final MethodComparator METHOD_COMPARATOR = new MethodComparator();
    private PatternTypes patternTypeToUse;

    public PatternTypeOperationsBuilder(GeneratorContext context, Type type, PatternTypes patternTypeToUse) {
        super(context, type);
        if (!patternTypeToUse.equals(PatternTypes.CLOUD_CONNECTOR) && !patternTypeToUse.equals(PatternTypes.TRANSFORMER)) {
            throw new IllegalArgumentException("PatternType not supported: " + patternTypeToUse);
        }
        this.patternTypeToUse = patternTypeToUse;
    }

    public JAXBElement<PatternType> build() {

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));
        attributeCategory.getGroup().add(createGroupWithModeSwitch(getMethods()));

        PatternType patternType = new PatternType();
        patternType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        patternType.setCaption(helper.getFormattedCaption(type));

        if (patternTypeToUse.equals(PatternTypes.CLOUD_CONNECTOR)) {
            patternType.setLocalId(type.name() + "-connector");
            patternType.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + type.name() + '/' + helper.getGlobalRefId(type.name()));
        } else {
            patternType.setLocalId(type.name() + "-transformer");
        }
        patternType.setDescription(helper.getFormattedDescription(type));
        patternType.setAliasId(ALIAS_ID_PREFIX + type.name());
        patternType.setIcon(getIcon());
        patternType.setImage(getImage());

        if (patternTypeToUse.equals(PatternTypes.CLOUD_CONNECTOR)) {
            return objectFactory.createNamespaceTypeCloudConnector(patternType);
        } else {
            return objectFactory.createNamespaceTypeTransformer(patternType);
        }
    }

    private String getImage() {
        if (patternTypeToUse.equals(PatternTypes.TRANSFORMER)) {
            return helper.getTransformerImage(type);
        } else {
            return helper.getConnectorImage(type);
        }
    }

    private String getIcon() {
        if (patternTypeToUse.equals(PatternTypes.TRANSFORMER)) {
            return helper.getTransformerIcon(type);
        } else {
            return helper.getConnectorIcon(type);
        }
    }

    private List<Method> getMethods() {
        List<Method> methods;
        if (patternTypeToUse.equals(PatternTypes.CLOUD_CONNECTOR)) {
            methods = type.getMethodsAnnotatedWith(Processor.class);
        } else {
            methods = type.getMethodsAnnotatedWith(Transformer.class);
        }
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }
}