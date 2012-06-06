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

import org.apache.commons.lang.WordUtils;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Icons;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.generation.spring.SchemaTypeConversion;
import org.mule.devkit.model.DevKitElement;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.DevKitVariableElement;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.EncodingType;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.FlowRefType;
import org.mule.devkit.model.studio.IntegerType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PasswordType;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;
import org.mule.devkit.model.studio.UrlType;
import org.mule.devkit.utils.NameUtils;
import org.mule.util.StringUtils;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class MuleStudioUtils {

    private static final String IMAGE_PREFIX = "icons/large/";
    private static final String ICON_PREFIX = "icons/small/";

    public String formatCaption(String caption) {
        return WordUtils.capitalizeFully(caption);
    }

    public String formatDescription(String description) {
        if (Character.isLowerCase(description.charAt(0))) {
            description = StringUtils.capitalize(description);
        }
        if (!description.endsWith(".")) {
            description += '.';
        }
        return description.replaceAll("\\<.*?\\>", "");
    }

    public String getConnectorImage(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String image;
        if(icons != null) {
            image = icons.connectorLarge();
        } else {
            image = String.format(Icons.GENERIC_CLOUD_CONNECTOR_LARGE, typeElement.name());
        }
        if(image.contains("/")) {
            image = image.substring(image.lastIndexOf("/") +1);
        }
        return IMAGE_PREFIX + image;
    }

    public String getConnectorIcon(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String icon;
        if(icons != null) {
            icon = icons.connectorSmall();
        } else {
            icon = String.format(Icons.GENERIC_CLOUD_CONNECTOR_SMALL, typeElement.name());
        }
        if(icon.contains("/")) {
            icon = icon.substring(icon.lastIndexOf("/") +1);
        }
        return ICON_PREFIX + icon;
    }

    public String getEndpointImage(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String image;
        if(icons != null) {
            image = icons.endpointLarge();
        } else {
            image = String.format(Icons.GENERIC_ENDPOINT_LARGE, typeElement.name());
        }
        if(image.contains("/")) {
            image = image.substring(image.lastIndexOf("/") +1);
        }
        return IMAGE_PREFIX + image;
    }

    public String getEndpointIcon(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String icon;
        if(icons != null) {
            icon = icons.endpointSmall();
        } else {
            icon = String.format(Icons.GENERIC_ENDPOINT_SMALL, typeElement.name());
        }
        if(icon.contains("/")) {
            icon = icon.substring(icon.lastIndexOf("/") +1);
        }
        return ICON_PREFIX + icon;
    }

    public String getTransformerImage(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String image;
        if(icons != null) {
            image = icons.transformerLarge();
        } else {
            image = String.format(Icons.GENERIC_TRANSFORMER_LARGE, typeElement.name());
        }
        if(image.contains("/")) {
            image = image.substring(image.lastIndexOf("/") +1);
        }
        return IMAGE_PREFIX + image;
    }

    public String getTransformerIcon(DevKitTypeElement typeElement) {
        Icons icons = typeElement.getAnnotation(Icons.class);
        String icon;
        if(icons != null) {
            icon = icons.transformerSmall();
        } else {
            icon = String.format(Icons.GENERIC_TRANSFORMER_SMALL, typeElement.name());
        }
        if(icon.contains("/")) {
            icon = icon.substring(icon.lastIndexOf("/") +1);
        }
        return ICON_PREFIX + icon;
    }

    public String getGlobalRefId(String moduleName) {
        return "abstract" + StringUtils.capitalize(moduleName) + "ConnectorGeneric";
    }

    public List<JAXBElement<? extends AttributeType>> createJAXBElements(List<AttributeType> attributeTypes) {
        List<JAXBElement<? extends AttributeType>> jaxbElements = new ArrayList<JAXBElement<? extends AttributeType>>();
        for (AttributeType attributeType : attributeTypes) {
            JAXBElement<? extends AttributeType> jaxbElement = createJAXBElement(attributeType);
            if (jaxbElement != null) {
                jaxbElements.add(jaxbElement);
            }
        }
        return jaxbElements;
    }

    public JAXBElement<? extends AttributeType> createJAXBElement(AttributeType attributeType) {
        ObjectFactory objectFactory = new ObjectFactory();
        if (attributeType instanceof PasswordType) {
            return objectFactory.createGroupPassword((PasswordType) attributeType);
        }
        if (attributeType instanceof UrlType) {
            return objectFactory.createGroupUrl((UrlType) attributeType);
        }
        if (attributeType instanceof StringAttributeType) {
            return objectFactory.createGroupString((StringAttributeType) attributeType);
        }
        if (attributeType instanceof IntegerType) { // TODO: Studio has a problem with LongType, until that's resolved map longs to integer
            return objectFactory.createGroupInteger((IntegerType) attributeType);
        }
        if (attributeType instanceof EnumType) {
            return objectFactory.createGroupEnum((EnumType) attributeType);
        }
        if (attributeType instanceof Booleantype) {
            return objectFactory.createGroupBoolean((Booleantype) attributeType);
        }
        if (attributeType instanceof TextType) {
            return objectFactory.createGroupText((TextType) attributeType);
        }
        if (attributeType instanceof FlowRefType) {
            return objectFactory.createGroupFlowRef((FlowRefType) attributeType);
        }
        if (attributeType instanceof EncodingType) {
            return objectFactory.createGroupEncoding((EncodingType) attributeType);
        }
        if (attributeType instanceof NestedElementReference) {
            return objectFactory.createNestedElementTypeChildElement((NestedElementReference) attributeType);
        }
        return null;
    }

    public AttributeType createAttributeTypeIgnoreEnumsAndCollections(DevKitElement element) {
        if (skipAttributeTypeGeneration(element)) {
            return null;
        } else if (SchemaTypeConversion.isSupported(element.asType().toString())) {
            return createAttributeTypeOfSupportedType(element);
        } else if (element.isHttpCallback()) {
            FlowRefType flowRefType = new FlowRefType();
            flowRefType.setSupportFlow(true);
            flowRefType.setSupportSubflow(true);
            return flowRefType;
        } else {
            return new StringAttributeType();
        }
    }

    private boolean skipAttributeTypeGeneration(DevKitElement element) {
        return element.isCollection() || element.isEnum() || ((element instanceof DevKitParameterElement) && ((DevKitParameterElement) element).shouldBeIgnored());
    }

    private AttributeType createAttributeTypeOfSupportedType(DevKitElement element) {
        if (element.getAnnotation(Password.class) != null) {
            return new PasswordType();
        }
        if (element.isString() || element.isDate() || element.isChar() ||
                element.isFloat() || element.isDouble()) {
            return new StringAttributeType();
        } else if (element.isBoolean()) {
            Booleantype booleantype = new Booleantype();
            booleantype.setSupportsExpressions(true);
            return booleantype;
        } else if (element.isInteger() || element.isLong() || element.isBigDecimal() || element.isBigInteger()) {
            IntegerType integerType = new IntegerType();
            integerType.setMin(0);
            integerType.setStep(1);
            return integerType;
        } else if (element.isURL()) {
            return new UrlType();
        } else {
            throw new RuntimeException("Failed to create Studio XML, type not recognized: type=" + element.asType().toString() + " name=" + element.getSimpleName().toString());
        }
    }

    public void setAttributeTypeInfo(DevKitVariableElement variableElement, AttributeType attributeType) {
        String parameterName = variableElement.getSimpleName().toString();
        attributeType.setCaption(getFormattedCaption(variableElement));
        attributeType.setDescription(getFormattedDescription(variableElement));
        if (attributeType instanceof StringAttributeType && !SchemaTypeConversion.isSupported(variableElement.asType().toString())) {
            attributeType.setName(parameterName + SchemaGenerator.REF_SUFFIX);
        } else if (attributeType instanceof FlowRefType) {
            attributeType.setName(NameUtils.uncamel(parameterName) + SchemaGenerator.FLOW_REF_SUFFIX);
        } else {
            attributeType.setName(parameterName);
        }
        attributeType.setRequired(variableElement.getAnnotation(Optional.class) == null);
        attributeType.setJavaType(variableElement.asType().toString());
        setDefaultValueIfAvailable(variableElement, attributeType);
    }

    public void setDefaultValueIfAvailable(DevKitVariableElement variableElement, AttributeType parameter) {
        Default annotation = variableElement.getAnnotation(Default.class);
        if (annotation != null) {
            if (parameter instanceof Booleantype) {
                ((Booleantype) parameter).setDefaultValue(Boolean.valueOf(annotation.value()));
            } else if (parameter instanceof IntegerType) {
                ((IntegerType) parameter).setDefaultValue(Integer.valueOf(annotation.value()));
            } else if (parameter instanceof StringAttributeType) {
                ((StringAttributeType) parameter).setDefaultValue(annotation.value());
            } else if (parameter instanceof EnumType) {
                ((EnumType) parameter).setDefaultValue(annotation.value());
            }
        }
    }

    public String getLocalId(DevKitExecutableElement executableElement, DevKitVariableElement variableElement) {
        if (executableElement != null) {
            return NameUtils.uncamel(executableElement.getSimpleName().toString()) + '-' + NameUtils.uncamel(variableElement.getSimpleName().toString());
        } else {
            return "configurable-" + NameUtils.uncamel(variableElement.getSimpleName().toString());
        }
    }

    public String getLocalId(DevKitExecutableElement executableElement) {
        String localId;
        Processor processor = executableElement.getAnnotation(Processor.class);
        if (processor != null && StringUtils.isNotBlank(processor.name())) {
            localId = processor.name();
        } else {
            localId = executableElement.getSimpleName().toString();
        }
        return NameUtils.uncamel(localId);
    }

    public String getFormattedDescription(DevKitVariableElement element) {
        Summary description = element.getAnnotation(Summary.class);
        if (description != null && StringUtils.isNotBlank(description.value())) {
            return formatDescription(description.value());
        }
        if (element instanceof DevKitParameterElement) {
            return formatDescription(element.parent().getJavaDocParameterSummary(element.getSimpleName().toString()));
        }
        return formatDescription(element.getJavaDocSummary());
    }

    public String getFormattedDescription(DevKitTypeElement typeElement) {
        if(StringUtils.isNotBlank(typeElement.description())) {
            return typeElement.description();
        }
        return formatDescription(typeElement.getJavaDocSummary());
    }

    public String getFormattedCaption(DevKitTypeElement typeElement) {
        if(StringUtils.isNotBlank(typeElement.friendlyName())) {
            return typeElement.friendlyName();
        }
        return formatCaption(typeElement.name().replaceAll("-", " "));
    }

    public String getFormattedCaption(DevKitExecutableElement element) {
        return formatCaption(getFriendlyName(element));
    }

    public String getFormattedCaption(DevKitVariableElement element) {
        FriendlyName caption = element.getAnnotation(FriendlyName.class);
        if (caption != null && StringUtils.isNotBlank(caption.value())) {
            return caption.value();
        }
        String friendlyName = NameUtils.friendlyNameFromCamelCase(element.getSimpleName().toString());
        if (element.isHttpCallback()) {
            return formatCaption(friendlyName + " Flow");
        }
        if (!isKnownType(element)) {
            return formatCaption(friendlyName + " Reference");
        }
        return formatCaption(friendlyName);
    }

    public String getFriendlyName(DevKitExecutableElement element) {
        Processor processor = element.getAnnotation(Processor.class);
        if(processor != null && StringUtils.isNotBlank(processor.friendlyName())) {
            return processor.friendlyName();
        }
        Source source = element.getAnnotation(Source.class);
        if(source != null && StringUtils.isNotBlank(source.friendlyName())) {
            return source.friendlyName();
        }
        return NameUtils.friendlyNameFromCamelCase(element.getSimpleName().toString());
    }

    public boolean isKnownType(DevKitVariableElement variable) {
        return variable.isString() ||
                variable.isChar() ||
                variable.isDate() ||
                variable.isDouble() ||
                variable.isFloat() ||
                variable.isLong() ||
                variable.isHttpCallback() ||
                variable.isInteger() ||
                variable.isBigDecimal() ||
                variable.isBigInteger() ||
                variable.isBoolean() ||
                variable.isEnum() ||
                variable.isCollection() ||
                variable.isURL();
    }

    public String getUrl(DevKitTypeElement typeElement) {
        return MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/';
    }
}