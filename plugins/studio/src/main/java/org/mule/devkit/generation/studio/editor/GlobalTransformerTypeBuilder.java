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
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;

import java.util.ArrayList;
import java.util.List;

public class GlobalTransformerTypeBuilder extends GlobalTypeBuilder {

    public GlobalTransformerTypeBuilder(Context context, Method executableElement, Type type) {
        super(context, executableElement, type);
    }

    @Override
    public GlobalType build() {
        GlobalType globalTransformer = super.build();
        globalTransformer.setAbstract(true);
        return globalTransformer;
    }

    protected List<AttributeCategory> getAttributeCategories() {
        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

        Group group = new Group();
        group.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.GROUP_DEFAULT_CAPTION));
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(createNameAttributeType()));

        attributeCategory.getGroup().add(group);

        List<AttributeCategory> attributeCategories = new ArrayList<AttributeCategory>();
        attributeCategories.add(attributeCategory);
        return attributeCategories;
    }

    protected String getDescriptionBasedOnType() {
        return helper.formatDescription(executableElement.getJavaDocSummary());
    }

    protected String getExtendsBasedOnType() {
        return MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + getLocalIdBasedOnType();
    }

    protected String getLocalIdBasedOnType() {
        return NameUtils.uncamel(executableElement.getSimpleName().toString());
    }

    protected String getCaptionBasedOnType() {
        return helper.getFormattedCaption(executableElement);
    }

    protected String getNameDescriptionBasedOnType() {
        return "Identifies the transformer so that other elements can reference it. Required if the transformer is defined at the global level.";
    }

    @Override
    protected String getImage() {
        return helper.getTransformerImage(type);
    }

    @Override
    protected String getIcon() {
        return helper.getTransformerIcon(type);
    }
}