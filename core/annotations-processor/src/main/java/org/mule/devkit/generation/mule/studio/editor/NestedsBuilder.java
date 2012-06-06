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
import org.mule.devkit.Context;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.NestedElementType;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;
import org.mule.devkit.utils.NameUtils;

import javax.lang.model.type.DeclaredType;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class NestedsBuilder extends BaseStudioXmlBuilder {

    public NestedsBuilder(Context context, Method executableElement, Type type) {
        super(context, executableElement, type);
    }

    public NestedsBuilder(Context context, Type type) {
        super(context, type);
    }

    public List<? extends JAXBElement<NestedElementType>> build() {
        List<JAXBElement<NestedElementType>> nesteds = new ArrayList<JAXBElement<NestedElementType>>();
        for (Variable variable : getVariableElements()) {
            if (needToCreateNestedElement(variable)) {

                String localId = helper.getLocalId(executableElement, variable);
                NestedElementReference childElement = createChildElement(variable, localId);
                childElement.setControlled("createAList");
                NestedElementType firstLevelNestedElement = createFirstLevelNestedElement(variable, localId);
                firstLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));
                firstLevelNestedElement.setSpecialValuePersistance(helper.getUrl(type) + helper.getLocalId(executableElement, variable));

                NestedElementType secondLevelNestedElement = null;
                NestedElementType thirdLevelNestedElement = null;
                if (isSimpleList(variable)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variable, childElement);
                    handleSimpleList(variable, localId, secondLevelNestedElement);
                } else if (isSimpleMap(variable) || isListOfMaps(variable)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variable, childElement);
                    handleSimpleMap(variable, secondLevelNestedElement);
                    if (isListOfMaps(variable)) {
                        childElement.setName(NameUtils.singularize(childElement.getName()));
                        thirdLevelNestedElement = new NestedElementType();
                        thirdLevelNestedElement.setCaption(helper.formatCaption(NameUtils.friendlyNameFromCamelCase(variable.getSimpleName().toString())));
                        thirdLevelNestedElement.setLocalId(NameUtils.singularize(localId));
                        thirdLevelNestedElement.setXmlname(NameUtils.uncamel(NameUtils.singularize(variable.getSimpleName().toString())));
                        thirdLevelNestedElement.setDescription(helper.formatDescription(NameUtils.friendlyNameFromCamelCase(variable.getSimpleName().toString())));
                        thirdLevelNestedElement.setIcon(getIcon());
                        thirdLevelNestedElement.setImage(getImage());
                        NestedElementReference childElement1 = createChildElement(variable, SchemaGenerator.INNER_PREFIX + NameUtils.singularize(localId));
                        childElement1.setCaption(NameUtils.singularize(childElement1.getCaption()));
                        childElement1.setDescription(NameUtils.singularize(childElement1.getDescription()));
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
        if (executableElement != null) {
            if (executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorImage(type);
            }
            if (executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointImage(type);
            }
            if (executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerImage(type);
            }
        }
        return helper.getConnectorImage(type);
    }

    private String getIcon() {
        if (executableElement != null) {
            if (executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorIcon(type);
            }
            if (executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointIcon(type);
            }
            if (executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerIcon(type);
            }
        }
        return helper.getConnectorIcon(type);
    }

    private List<? extends Variable> getVariableElements() {
        if (executableElement != null) {
            return executableElement.getParameters();
        } else {
            return type.getFieldsAnnotatedWith(Configurable.class);
        }
    }

    private void handleSimpleMap(Variable parameter, NestedElementType secondLevelNestedElement) {
        AttributeType attributeTypeForMapKey;
        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
            attributeTypeForMapKey = new StringAttributeType();
        } else {
            attributeTypeForMapKey = helper.createAttributeTypeIgnoreEnumsAndCollections(((Identifiable) parameter.getTypeArguments().get(0)));
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

    private void handleSimpleList(Variable parameter, String localId, NestedElementType secondLevelNestedElement) {
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

        attributeTypeForListValues.setName(NameUtils.singularize(localId));
        attributeTypeForListValues.setCaption(helper.formatCaption(NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        if (executableElement != null) {
            attributeTypeForListValues.setDescription(helper.formatDescription(executableElement.getJavaDocParameterSummary(parameter.getSimpleName().toString())));
        } else {
            attributeTypeForListValues.setDescription(helper.formatDescription(parameter.getJavaDocSummary()));
        }
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForListValues));
    }

    private NestedElementType createSecondLevelNestedElement(Variable parameter, NestedElementReference childElement) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setCaption(helper.formatCaption(NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        String localIdSuffix = childElement.getName().substring(childElement.getName().lastIndexOf('/') + 1);
        if (isListOfMaps(parameter)) {
            nestedElement.setLocalId(SchemaGenerator.INNER_PREFIX + NameUtils.singularize(localIdSuffix));
            nestedElement.setXmlname(SchemaGenerator.INNER_PREFIX + NameUtils.uncamel(NameUtils.singularize(parameter.getSimpleName().toString())));
        } else {
            nestedElement.setLocalId(localIdSuffix);
            nestedElement.setXmlname(NameUtils.uncamel(NameUtils.singularize(parameter.getSimpleName().toString())));
        }
        nestedElement.setDescription(helper.formatDescription(NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(getIcon());
        nestedElement.setImage(getImage());
        return nestedElement;
    }

    private NestedElementType createFirstLevelNestedElement(Variable parameter, String localId) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setLocalId(localId);
        nestedElement.setXmlname(NameUtils.uncamel(parameter.getSimpleName().toString()));
        nestedElement.setCaption(helper.formatCaption(NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setDescription(helper.formatDescription(NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
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

    private boolean isListOfMaps(Variable parameter) {
        return parameter.isArrayOrList() && parameter.hasTypeArguments() && ((Identifiable) parameter.getTypeArguments().get(0)).isMap();
    }

    private boolean isSimpleMap(Variable parameter) {
        return parameter.isMap() && (!parameter.hasTypeArguments() || !((Identifiable) parameter.getTypeArguments().get(1)).isCollection());
    }

    private boolean isSimpleList(Variable parameter) {
        return parameter.isArrayOrList() && (!parameter.hasTypeArguments() || !((Identifiable) parameter.getTypeArguments().get(0)).isCollection());
    }

    private NestedElementReference createChildElement(Variable parameter, String localId) {
        NestedElementReference childElement = new NestedElementReference();
        String parameterFriendlyName = NameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString());
        if (isListOfMaps(parameter)) {
            childElement.setName(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + localId);
            childElement.setDescription(helper.formatDescription(NameUtils.singularize(parameterFriendlyName)));
            childElement.setCaption(helper.formatCaption(NameUtils.singularize(parameterFriendlyName)));
        } else {
            String singularizedLocalId = NameUtils.singularize(localId);
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

    private boolean needToCreateNestedElement(Variable parameter) {
        boolean needTo = (parameter.isMap() ||
                parameter.isArrayOrList());

        if (parameter instanceof Parameter &&
                ((Parameter) parameter).shouldBeIgnored()) {
            needTo = false;
        }

        return needTo;
    }

}