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

package org.mule.devkit.generation.studio.editor;

import org.mule.devkit.generation.api.Context;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.EndpointType;

import java.util.Collection;

public class EndpointTypeBuilder extends BaseStudioXmlBuilder {

    public EndpointTypeBuilder(Context context, Method executableElement, Type type) {
        super(context, executableElement, type);
    }

    public EndpointType build() {
        EndpointType endpointType = new EndpointType();
        endpointType.setLocalId(getLocalId());
        endpointType.setCaption(getCaption());
        endpointType.setIcon(helper.getEndpointIcon(type));
        endpointType.setImage(helper.getEndpointImage(type));
        endpointType.setDescription(getDescription());
        endpointType.setSupportsInbound(true);
        endpointType.setSupportsOutbound(false);
        endpointType.setInboundLocalName(getLocalId());
        endpointType.setAbstract(true);
        endpointType.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + helper.getGlobalRefId(type.getModuleName()));

        processMethodParameters(endpointType);

        return endpointType;
    }

    protected void processMethodParameters(EndpointType endpoint) {
        Collection<AttributeCategory> attributeCategories = processMethodParameters();
        endpoint.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);
    }

    protected String getDescription() {
        return helper.formatDescription(executableElement.getJavaDocSummary());
    }

    protected String getCaption() {
        return helper.getFormattedCaption(executableElement);
    }

    protected String getLocalId() {
        return NameUtils.uncamel(executableElement.getSimpleName().toString());
    }
}