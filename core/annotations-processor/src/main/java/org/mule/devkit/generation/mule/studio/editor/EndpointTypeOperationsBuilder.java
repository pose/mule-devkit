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

import org.mule.api.annotations.Source;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.EndpointType;

import java.util.Collections;
import java.util.List;

public class EndpointTypeOperationsBuilder extends EndpointTypeBuilder {

    public EndpointTypeOperationsBuilder(GeneratorContext context, Type type) {
        super(context, null, type);
    }

    public EndpointType build() {
        EndpointType endpoinTypeOperations = super.build();
        endpoinTypeOperations.setAbstract(false);
        endpoinTypeOperations.setInboundLocalName(null);

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

        attributeCategory.getGroup().add(createGroupWithModeSwitch(getSourceMethodsSorted()));

        endpoinTypeOperations.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        endpoinTypeOperations.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + type.name() + '/' + helper.getGlobalRefId(type.name()));

        return endpoinTypeOperations;
    }

    @Override
    protected void processMethodParameters(EndpointType endpoint) {
        // ignore
    }

    @Override
    protected String getDescription() {
        return helper.getFormattedDescription(type);
    }

    @Override
    protected String getCaption() {
        return helper.getFormattedCaption(type) + " (Streaming)";
    }

    @Override
    protected String getLocalId() {
        return "endpoint";
    }

    private List<Method> getSourceMethodsSorted() {
        List<Method> methods = type.getMethodsAnnotatedWith(Source.class);
        Collections.sort(methods, new MethodComparator());
        return methods;
    }
}