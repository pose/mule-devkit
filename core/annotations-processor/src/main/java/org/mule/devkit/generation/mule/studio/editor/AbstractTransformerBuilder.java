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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.EncodingType;
import org.mule.devkit.model.studio.EnumElement;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.PatternType;
import org.mule.devkit.model.studio.StringAttributeType;

import javax.xml.bind.JAXBElement;

public class AbstractTransformerBuilder extends BaseStudioXmlBuilder {

    public static final String ABSTRACT_TRANSFORMER_LOCAL_ID = "abstractTransformer";
    public static final String ABSTRACT_TRANSFORMER_ATTRIBUTE_CATEGORY_CAPTION = "Advanced";
    public static final String ABSTRACT_TRANSFORMER_ATTRIBUTE_CATEGORY_DESCRIPTION = "Advanced settings for transformer";

    public AbstractTransformerBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public JAXBElement<PatternType> build() {
        return objectFactory.createNamespaceTypeTransformer(createAbstractTransformer());
    }

    private PatternType createAbstractTransformer() {
        PatternType abstractTransformer = new PatternType();
        abstractTransformer.setLocalId(ABSTRACT_TRANSFORMER_LOCAL_ID);
        abstractTransformer.setCaption(helper.formatCaption("Base transformer"));
        abstractTransformer.setDescription(helper.formatDescription("Base transformer"));
        abstractTransformer.setAbstract(true);
        abstractTransformer.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + "/" + typeElement.name() + "-transformer");

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(ABSTRACT_TRANSFORMER_ATTRIBUTE_CATEGORY_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(ABSTRACT_TRANSFORMER_ATTRIBUTE_CATEGORY_DESCRIPTION));

        attributeCategory.getGroup().add(createTransformerSettingsGroup());
        attributeCategory.getGroup().add(createMimeAttributesGroup());

        abstractTransformer.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);

        return abstractTransformer;
    }

    private Group createMimeAttributesGroup() {
        Group mimeAttributesGroup = new Group();
        mimeAttributesGroup.setCaption(helper.formatCaption("Mime type attributes"));
        mimeAttributesGroup.setId("mimeTypeAttributes");

        EnumType mimeTypesEnum = new EnumType();
        mimeTypesEnum.setCaption(helper.formatCaption("Mime type"));
        mimeTypesEnum.setDescription(helper.formatDescription("The mime type of the transformer's output"));
        mimeTypesEnum.setName("mimeType");
        mimeTypesEnum.setXsdType("string");
        mimeTypesEnum.setAllowsCustom(true);

        for (MimeType mimeType : MimeType.values()) {
            EnumElement mimeTypeOption = new EnumElement();
            mimeTypeOption.setValue(mimeType.toString());
            mimeTypesEnum.getOption().add(mimeTypeOption);
        }

        mimeAttributesGroup.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(mimeTypesEnum));
        return mimeAttributesGroup;
    }

    private Group createTransformerSettingsGroup() {
        Group transformerSettingsGroup = new Group();
        transformerSettingsGroup.setCaption(helper.formatCaption("Transformer Settings"));
        transformerSettingsGroup.setId("abstractTransformerSettings");

        StringAttributeType returnClassAttribute = new StringAttributeType();
        returnClassAttribute.setName("returnClass");
        returnClassAttribute.setDescription(helper.formatDescription("The class of the message generated by the transformer. This is used if transformers are auto-selected and to validate that the transformer returns the correct type. Note that if you need to specify an array type you need postfix the class name with '[]'. For example, if you want return a an Orange[], you set the return class to 'org.mule.tck.testmodels.fruit.Orange[]'."));
        returnClassAttribute.setCaption(helper.formatCaption("Return Class"));

        Booleantype ignoreBadInputAttribute = new Booleantype();
        ignoreBadInputAttribute.setName("ignoreBadInput");
        ignoreBadInputAttribute.setDescription(helper.formatDescription("Many transformers only accept certain classes. Such transformers are never called with inappropriate input (whatever the value of this attribute). If a transformer forms part of a chain and cannot accept the current message class, this flag controls whether the remaining part of the chain is evaluated. If true, the next transformer is called. If false the chain ends, keeping the result generated up to that point."));
        ignoreBadInputAttribute.setCaption(helper.formatCaption("Ignore Bad Input"));
        ignoreBadInputAttribute.setXsdType("substitutableBoolean");

        EncodingType encodingAttribute = new EncodingType();
        encodingAttribute.setName("encoding");
        encodingAttribute.setDescription(helper.formatDescription("String encoding used for transformer output."));
        encodingAttribute.setCaption(helper.formatCaption("Encoding"));
        encodingAttribute.setXsdType("string");

        transformerSettingsGroup.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(returnClassAttribute));
        transformerSettingsGroup.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(ignoreBadInputAttribute));
        transformerSettingsGroup.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(encodingAttribute));
        return transformerSettingsGroup;
    }
}