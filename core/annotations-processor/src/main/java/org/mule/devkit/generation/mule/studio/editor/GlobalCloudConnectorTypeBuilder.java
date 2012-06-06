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

import org.mule.devkit.Context;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Group;
import org.mule.util.StringUtils;

import java.util.List;
import java.util.Map;

public class GlobalCloudConnectorTypeBuilder extends GlobalTypeBuilder {

    public GlobalCloudConnectorTypeBuilder(Context context, Type type) {
        super(context, type);
    }

    protected List<AttributeCategory> getAttributeCategories() {
        Group group = new Group();
        group.setId(moduleName + "GenericProperties");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(createNameAttributeType()));
        group.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.GROUP_DEFAULT_CAPTION));
        return processConfigurableFields(group);
    }

    @Override
    protected void processConnectionAttributes(Map<String, Group> groupsByName, Map<String, AttributeCategory> attributeCategoriesByName) {
        if (type.usesConnectionManager()) {
            Group connectionAttributesGroup = new Group();
            connectionAttributesGroup.setCaption(helper.formatCaption(CONNECTION_GROUP_NAME));
            connectionAttributesGroup.setId(StringUtils.uncapitalize(CONNECTION_GROUP_NAME));

            groupsByName.put(CONNECTION_GROUP_NAME, connectionAttributesGroup);

            List<AttributeType> connectionAttributes = getConnectionAttributes(type);
            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().addAll(helper.createJAXBElements(connectionAttributes));

            AttributeCategory defaultAttributeCategory = attributeCategoriesByName.get(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION);
            defaultAttributeCategory.getGroup().add(connectionAttributesGroup);
        }
    }

    protected String getDescriptionBasedOnType() {
        return helper.formatDescription("Global " + helper.getFormattedCaption(type) + " configuration information");
    }

    protected String getExtendsBasedOnType() {
        return MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + helper.getGlobalRefId(type.getModuleName());
    }

    protected String getLocalIdBasedOnType() {
        return MuleStudioEditorXmlGenerator.GLOBAL_CLOUD_CONNECTOR_LOCAL_ID;
    }

    protected String getCaptionBasedOnType() {
        return helper.getFormattedCaption(type);
    }

    protected String getNameDescriptionBasedOnType() {
        return helper.formatDescription("Give a name to this configuration so it can be later referenced by config-ref.");
    }

    @Override
    protected String getImage() {
        return helper.getConnectorImage(type);
    }

    @Override
    protected String getIcon() {
        return helper.getConnectorIcon(type);
    }
}