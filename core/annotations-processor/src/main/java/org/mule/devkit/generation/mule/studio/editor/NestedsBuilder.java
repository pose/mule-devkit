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

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.model.DefaultDevKitElement;
import org.mule.devkit.model.DevKitElement;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.DevKitVariableElement;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.NestedElementType;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class NestedsBuilder extends BaseStudioXmlBuilder {

    public NestedsBuilder(GeneratorContext context, DevKitExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public NestedsBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public List<? extends JAXBElement<NestedElementType>> build() {
        List<JAXBElement<NestedElementType>> nesteds = new ArrayList<JAXBElement<NestedElementType>>();
        for (DevKitVariableElement variableElement : getVariableElements()) {
            if (needToCreateNestedElement(variableElement)) {

                String localId = helper.getLocalId(executableElement, variableElement);
                NestedElementReference childElement = createChildElement(variableElement, localId);
                childElement.setControlled("createAList");
                NestedElementType firstLevelNestedElement = createFirstLevelNestedElement(variableElement, localId);
                firstLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));
                firstLevelNestedElement.setSpecialValuePersistance(helper.getUrl(typeElement) + helper.getLocalId(executableElement, variableElement));

                NestedElementType secondLevelNestedElement = null;
                NestedElementType thirdLevelNestedElement = null;
                if (isSimpleList(variableElement)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variableElement, childElement);
                    handleSimpleList(variableElement, localId, secondLevelNestedElement);
                } else if (isSimpleMap(variableElement) || isListOfMaps(variableElement)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variableElement, childElement);
                    handleSimpleMap(variableElement, secondLevelNestedElement);
                    if (isListOfMaps(variableElement)) {
                        childElement.setName(nameUtils.singularize(childElement.getName()));
                        thirdLevelNestedElement = new NestedElementType();
                        thirdLevelNestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setLocalId(nameUtils.singularize(localId));
                        thirdLevelNestedElement.setXmlname(nameUtils.uncamel(nameUtils.singularize(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setIcon(getIcon());
                        thirdLevelNestedElement.setImage(getImage());
                        NestedElementReference childElement1 = createChildElement(variableElement, SchemaGenerator.INNER_PREFIX + nameUtils.singularize(localId));
                        childElement1.setCaption(nameUtils.singularize(childElement1.getCaption()));
                        childElement1.setDescription(nameUtils.singularize(childElement1.getDescription()));
                        thirdLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement1));
                    }
                }

                nesteds.add(objectFactory.createNested(firstLevelNestedElement));
                nesteds.add(objectFactory.createNested(secondLevelNestedElement));
                if (thirdLevelNestedElement != null) {
                    nesteds.add(objectFactory.createNested(thirdLevelNestedElement));
                }
            }
        }
        return nesteds;
    }

    private String getImage() {
        if(executableElement != null) {
            if(executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorImage(typeElement);
            }
            if(executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointImage(typeElement);
            }
            if(executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerImage(typeElement);
            }
        }
        return helper.getConnectorImage(typeElement);
    }

    private String getIcon() {
        if(executableElement != null) {
            if(executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorIcon(typeElement);
            }
            if(executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointIcon(typeElement);
            }
            if(executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerIcon(typeElement);
            }
        }
        return helper.getConnectorIcon(typeElement);
    }

    private List<? extends DevKitVariableElement> getVariableElements() {
        if (executableElement != null) {
            return executableElement.getParameters();
        } else {
            return typeElement.getFieldsAnnotatedWith(Configurable.class);
        }
    }

    private void handleSimpleMap(DevKitVariableElement parameter, NestedElementType secondLevelNestedElement) {
        AttributeType attributeTypeForMapKey;
        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
            attributeTypeForMapKey = new StringAttributeType();
        } else {
            TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
            attributeTypeForMapKey = helper.createAttributeTypeIgnoreEnumsAndCollections(new DefaultDevKitElement(typeUtils.asElement(typeMirror), null, null));
            if (attributeTypeForMapKey == null) { // nested
                attributeTypeForMapKey = new StringAttributeType();
            }
        }

        attributeTypeForMapKey.setName(SchemaGenerator.ATTRIBUTE_NAME_KEY);
        attributeTypeForMapKey.setDescription(helper.formatDescription("Key."));
        attributeTypeForMapKey.setCaption(helper.formatCaption("Key"));
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForMapKey));

        TextType attributeTypeForMapValues = new TextType();
        attributeTypeForMapValues.setName("value");
        attributeTypeForMapValues.setDescription(helper.formatDescription("Value."));
        attributeTypeForMapValues.setCaption(helper.formatCaption("Value"));
        attributeTypeForMapValues.setIsToElement(true);
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForMapValues));
    }

    private void handleSimpleList(DevKitVariableElement parameter, String localId, NestedElementType secondLevelNestedElement) {
        AttributeType attributeTypeForListValues;
        // TODO: temporarily commented out and added follwing 3 lines until Studio supports the isToElement attribute for other attributes types different than text
//        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty() || typeMirrorUtils.isString(typeUtils.asElement(((DeclaredType) parameter.asType()).getTypeArguments().get(0)))) {
//            TextType textType = new TextType();
//            textType.setIsToElement(true);
//            attributeTypeForListValues = textType;
//        } else {
//            TypeMirror typeParameter = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
//            attributeTypeForListValues = helper.createAttributeTypeIgnoreEnumsAndCollections(typeUtils.asElement(typeParameter));
//        }
        TextType textType = new TextType();
        textType.setIsToElement(true);
        attributeTypeForListValues = textType;

        attributeTypeForListValues.setName(nameUtils.singularize(localId));
        attributeTypeForListValues.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        if (executableElement != null) {
            attributeTypeForListValues.setDescription(helper.formatDescription(javaDocUtils.getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
        } else {
            attributeTypeForListValues.setDescription(helper.formatDescription(javaDocUtils.getSummary(parameter)));
        }
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForListValues));
    }

    private NestedElementType createSecondLevelNestedElement(DevKitVariableElement parameter, NestedElementReference childElement) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        String localIdSuffix = childElement.getName().substring(childElement.getName().lastIndexOf('/') + 1);
        if (isListOfMaps(parameter)) {
            nestedElement.setLocalId(SchemaGenerator.INNER_PREFIX + nameUtils.singularize(localIdSuffix));
            nestedElement.setXmlname(SchemaGenerator.INNER_PREFIX + nameUtils.uncamel(nameUtils.singularize(parameter.getSimpleName().toString())));
        } else {
            nestedElement.setLocalId(localIdSuffix);
            nestedElement.setXmlname(nameUtils.uncamel(nameUtils.singularize(parameter.getSimpleName().toString())));
        }
        nestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(getIcon());
        nestedElement.setImage(getImage());
        return nestedElement;
    }

    private NestedElementType createFirstLevelNestedElement(DevKitVariableElement parameter, String localId) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setLocalId(localId);
        nestedElement.setXmlname(nameUtils.uncamel(parameter.getSimpleName().toString()));
        nestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(getIcon());
        nestedElement.setImage(getImage());

        Booleantype radioBoolean1 = new Booleantype();
        radioBoolean1.setLabelledWith(helper.getFormattedCaption(parameter));
        radioBoolean1.setCaption(helper.formatCaption("From Message"));
        radioBoolean1.setName("fromMessage");
        radioBoolean1.setTransientt(false);
        radioBoolean1.setHiddenForUser(true);

        StringAttributeType stringAttribute = new StringAttributeType();
        stringAttribute.setName("ref");
        stringAttribute.setControlled("fromMessage");
        stringAttribute.setSupportsExpressions(false);
        stringAttribute.setSingeLineForExpressions(true);

        Booleantype radioBoolean2 = new Booleantype();
        radioBoolean2.setCaption(helper.formatCaption("Create a List"));
        radioBoolean2.setName("createAList");
        radioBoolean2.setTransientt(false);
        radioBoolean2.setHiddenForUser(true);

        nestedElement.getRegexpOrEncodingOrString().add(objectFactory.createGroupRadioBoolean(radioBoolean1));
        nestedElement.getRegexpOrEncodingOrString().add(objectFactory.createGroupString(stringAttribute));
        nestedElement.getRegexpOrEncodingOrString().add(objectFactory.createGroupRadioBoolean(radioBoolean2));
        return nestedElement;
    }

    private boolean isListOfMaps(DevKitVariableElement parameter) {
        return parameter.isArrayOrList() && parameter.hasTypeArguments() && ((DevKitElement)parameter.getTypeArguments().get(0)).isMap();
    }

    private boolean isSimpleMap(DevKitVariableElement parameter) {
        return parameter.isMap() && (!parameter.hasTypeArguments() || !((DevKitTypeElement)parameter.getTypeArguments().get(1)).isCollection());
    }

    private boolean isSimpleList(DevKitVariableElement parameter) {
        return parameter.isArrayOrList() && (!parameter.hasTypeArguments() || !((DevKitTypeElement)parameter.getTypeArguments().get(0)).isCollection());
    }

    private NestedElementReference createChildElement(DevKitVariableElement parameter, String localId) {
        NestedElementReference childElement = new NestedElementReference();
        String parameterFriendlyName = nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString());
        if (isListOfMaps(parameter)) {
            childElement.setName(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + localId);
            childElement.setDescription(helper.formatDescription(nameUtils.singularize(parameterFriendlyName)));
            childElement.setCaption(helper.formatCaption(nameUtils.singularize(parameterFriendlyName)));
        } else {
            String singularizedLocalId = nameUtils.singularize(localId);
            if (localId.equals(singularizedLocalId)) {
                singularizedLocalId += "-each";
            }
            childElement.setName(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + singularizedLocalId);
            childElement.setDescription(helper.formatDescription(parameterFriendlyName));
            childElement.setCaption(helper.formatCaption(parameterFriendlyName));
        }
        childElement.setAllowMultiple(true);
        return childElement;
    }

    private boolean needToCreateNestedElement(DevKitVariableElement parameter) {
        return (parameter.isMap() ||
                parameter.isArrayOrList()) && !typeMirrorUtils.ignoreParameter(parameter);
    }

}