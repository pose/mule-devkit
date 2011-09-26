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

package org.mule.devkit.generation.spring;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Session;
import org.mule.api.callback.HttpCallback;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.adapter.HttpCallbackAdapterGenerator;
import org.mule.devkit.model.schema.Annotation;
import org.mule.devkit.model.schema.Any;
import org.mule.devkit.model.schema.Attribute;
import org.mule.devkit.model.schema.ComplexContent;
import org.mule.devkit.model.schema.ComplexType;
import org.mule.devkit.model.schema.Documentation;
import org.mule.devkit.model.schema.Element;
import org.mule.devkit.model.schema.ExplicitGroup;
import org.mule.devkit.model.schema.ExtensionType;
import org.mule.devkit.model.schema.FormChoice;
import org.mule.devkit.model.schema.GroupRef;
import org.mule.devkit.model.schema.Import;
import org.mule.devkit.model.schema.LocalComplexType;
import org.mule.devkit.model.schema.LocalSimpleType;
import org.mule.devkit.model.schema.NoFixedFacet;
import org.mule.devkit.model.schema.NumFacet;
import org.mule.devkit.model.schema.ObjectFactory;
import org.mule.devkit.model.schema.Pattern;
import org.mule.devkit.model.schema.Restriction;
import org.mule.devkit.model.schema.Schema;
import org.mule.devkit.model.schema.SchemaLocation;
import org.mule.devkit.model.schema.SimpleContent;
import org.mule.devkit.model.schema.SimpleExtensionType;
import org.mule.devkit.model.schema.SimpleType;
import org.mule.devkit.model.schema.TopLevelComplexType;
import org.mule.devkit.model.schema.TopLevelElement;
import org.mule.devkit.model.schema.TopLevelSimpleType;
import org.mule.devkit.model.schema.Union;
import org.mule.util.StringUtils;

import javax.inject.Named;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class SchemaGenerator extends AbstractModuleGenerator {

    public static final String DOMAIN_ATTRIBUTE_NAME = HttpCallbackAdapterGenerator.DOMAIN_FIELD_NAME;
    public static final String LOCAL_PORT_ATTRIBUTE_NAME = HttpCallbackAdapterGenerator.LOCAL_PORT_FIELD_NAME;
    public static final String REMOTE_PORT_ATTRIBUTE_NAME = HttpCallbackAdapterGenerator.REMOTE_PORT_FIELD_NAME;
    public static final String ASYNC_ATTRIBUTE_NAME = HttpCallbackAdapterGenerator.ASYNC_FIELD_NAME;
    public static final String HTTP_CALLBACK_CONFIG_ELEMENT_NAME = "http-callback-config";
    private static final String ATTRIBUTE_NAME_KEY = "key";
    private static final String ATTRIBUTE_NAME_REF = "ref";
    private static final String ATTRIBUTE_NAME_VALUE_REF = "value-ref";
    private static final String ATTRIBUTE_NAME_KEY_REF = "key-ref";
    private static final String XSD_EXTENSION = ".xsd";
    private static final String NAMESPACE_HANDLER_SUFFIX = "NamespaceHandler";
    private static final String ENUM_TYPE_SUFFIX = "EnumType";
    private static final String TYPE_SUFFIX = "Type";
    private static final String XML_TYPE_SUFFIX = "XmlType";
    private static final String ATTRIBUTE_NAME_CONFIG_REF = "config-ref";
    private static final String UNBOUNDED = "unbounded";
    private static final String LAX = "lax";
    private static final String ELEMENT_NAME_CONFIG = "config";
    private static final String ATTRIBUTE_NAME_NAME = "name";
    private static final String REF_SUFFIX = "-ref";
    private static final String TRANSFER_OBJECT_TYPE_SUFFIX = "TransferObjectType";
    private static final String DOMAIN_DEFAULT_VALUE = "${fullDomain}";
    private static final String PORT_DEFAULT_VALUE = "${http.port}";
    private static final String ASYNC_DEFAULT_VALUE = "true";
    private Schema schema;
    private ObjectFactory objectFactory;

    public SchemaGenerator() {
        schema = new Schema();
        objectFactory = new ObjectFactory();
    }

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {
        Module module = typeElement.getAnnotation(Module.class);
        String targetNamespace = module.namespace();
        if (targetNamespace == null || targetNamespace.length() == 0) {
            targetNamespace = SchemaConstants.BASE_NAMESPACE + module.name();
        }

        schema.setTargetNamespace(targetNamespace);
        schema.setElementFormDefault(FormChoice.QUALIFIED);
        schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);

        importXmlNamespace();
        importSpringFrameworkNamespace();
        importMuleNamespace();

        registerTypes();
        registerConfigElement(targetNamespace, typeElement);
        registerProcessorsAndSources(targetNamespace, typeElement);
        registerTransformers(typeElement);
        registerEnums(typeElement);

        String fileName = "META-INF/mule-" + module.name() + XSD_EXTENSION;

        String location = module.schemaLocation();
        if (location == null || location.length() == 0) {
            location = schema.getTargetNamespace() + "/" + module.version() + "/mule-" + module.name() + XSD_EXTENSION;
        }

        // TODO: replace with a class role
        String namespaceHandlerName = context.getNameUtils().generateClassName(typeElement, ".config.spring", NAMESPACE_HANDLER_SUFFIX);

        SchemaLocation schemaLocation = new SchemaLocation(schema, fileName, location, namespaceHandlerName);

        context.getSchemaModel().addSchemaLocation(schemaLocation);
    }

    private void registerEnums(DevkitTypeElement typeElement) {
        Set<TypeMirror> registeredEnums = new HashSet<TypeMirror>();

        for (VariableElement field : typeElement.getFields()) {
            if (context.getTypeMirrorUtils().isEnum(field.asType())) {
                if (!registeredEnums.contains(field.asType())) {
                    registerEnum(field.asType());
                    registeredEnums.add(field.asType());
                }
            }

        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            for (VariableElement variable : method.getParameters()) {
                if (context.getTypeMirrorUtils().isEnum(variable.asType()) && !registeredEnums.contains(variable.asType())) {
                    registerEnum(variable.asType());
                    registeredEnums.add(variable.asType());
                } else if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                    DeclaredType variableType = (DeclaredType) variable.asType();
                    for (TypeMirror variableTypeParameter : variableType.getTypeArguments()) {
                        if (context.getTypeMirrorUtils().isEnum(variableTypeParameter) && !registeredEnums.contains(variableTypeParameter)) {
                            registerEnum(variableTypeParameter);
                            registeredEnums.add(variableTypeParameter);
                        }
                    }
                }
            }
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {
            for (VariableElement variable : method.getParameters()) {
                if (!context.getTypeMirrorUtils().isEnum(variable.asType()))
                    continue;

                if (!registeredEnums.contains(variable.asType())) {
                    registerEnum(variable.asType());
                    registeredEnums.add(variable.asType());
                }
            }
        }
    }

    private void registerEnum(TypeMirror enumType) {
        javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(enumType);

        TopLevelSimpleType enumSimpleType = new TopLevelSimpleType();
        enumSimpleType.setName(enumElement.getSimpleName() + ENUM_TYPE_SUFFIX);

        Union union = new Union();
        union.getSimpleType().add(createEnumSimpleType(enumElement));
        union.getSimpleType().add(createExpressionSimpleType());
        enumSimpleType.setUnion(union);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(enumSimpleType);
    }

    private LocalSimpleType createEnumSimpleType(javax.lang.model.element.Element enumElement) {
        LocalSimpleType enumValues = new LocalSimpleType();
        Restriction restriction = new Restriction();
        enumValues.setRestriction(restriction);
        restriction.setBase(SchemaConstants.STRING);

        for (javax.lang.model.element.Element enclosed : enumElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                NoFixedFacet noFixedFacet = objectFactory.createNoFixedFacet();
                noFixedFacet.setValue(enclosed.getSimpleName().toString());

                JAXBElement<NoFixedFacet> enumeration = objectFactory.createEnumeration(noFixedFacet);
                enumValues.getRestriction().getFacets().add(enumeration);
            }
        }
        return enumValues;
    }

    private void registerTransformers(DevkitTypeElement typeElement) {
        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Transformer.class)) {
            Element transformerElement = registerTransformer(context.getNameUtils().uncamel(method.getSimpleName().toString()));
            schema.getSimpleTypeOrComplexTypeOrGroup().add(transformerElement);
        }
    }

    private void registerProcessorsAndSources(String targetNamespace, DevkitTypeElement typeElement) {
        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            String name = method.getSimpleName().toString();
            Processor processor = method.getAnnotation(Processor.class);
            if (processor.name().length() > 0)
                name = processor.name();
            String typeName = StringUtils.capitalize(name) + TYPE_SUFFIX;

            registerProcessorElement(processor.intercepting(), targetNamespace, name, typeName);

            registerProcessorType(processor.intercepting(), targetNamespace, typeName, method);
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {
            String name = method.getSimpleName().toString();
            Source source = method.getAnnotation(Source.class);
            if (source.name().length() > 0)
                name = source.name();
            String typeName = StringUtils.capitalize(name) + TYPE_SUFFIX;

            registerSourceElement(targetNamespace, name, typeName);

            registerSourceType(targetNamespace, typeName, method);
        }

    }

    private void registerProcessorElement(boolean intercepting, String targetNamespace, String name, String typeName) {
        Element element = new TopLevelElement();
        element.setName(context.getNameUtils().uncamel(name));
        if (intercepting) {
            element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR);
        } else {
            element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR);
        }
        element.setType(new QName(targetNamespace, typeName));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerSourceElement(String targetNamespace, String name, String typeName) {
        Element element = new TopLevelElement();
        element.setName(context.getNameUtils().uncamel(name));
        element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_INBOUND_ENDPOINT);
        element.setType(new QName(targetNamespace, typeName));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerProcessorType(boolean intercepting, String targetNamespace, String name, ExecutableElement element) {
        if (intercepting) {
            registerExtendedType(SchemaConstants.MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR_TYPE, targetNamespace, name, element);
        } else {
            registerExtendedType(SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE, targetNamespace, name, element);
        }
    }

    private void registerSourceType(String targetNamespace, String name, ExecutableElement element) {
        registerExtendedType(SchemaConstants.MULE_ABSTRACT_INBOUND_ENDPOINT_TYPE, targetNamespace, name, element);
    }

    private void registerExtendedType(QName base, String targetNamespace, String name, ExecutableElement element) {
        TopLevelComplexType complexType = new TopLevelComplexType();
        complexType.setName(name);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(base);
        complexContent.setExtension(complexContentExtension);

        Attribute configRefAttr = createAttribute(ATTRIBUTE_NAME_CONFIG_REF, true, SchemaConstants.STRING, "Specify which configuration to use for this invocation.");
        complexContentExtension.getAttributeOrAttributeGroup().add(configRefAttr);

        ExplicitGroup all = new ExplicitGroup();
        complexContentExtension.setSequence(all);

        if (element.getKind() == ElementKind.METHOD) {
            int requiredChildElements = 0;
            for (VariableElement variable : element.getParameters()) {
                if (context.getTypeMirrorUtils().ignoreParameter(variable)) {
                    continue;
                }
                if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                    requiredChildElements++;
                } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                    requiredChildElements++;
                } else if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                    requiredChildElements++;
                }
            }
            for (VariableElement variable : element.getParameters()) {
                if (context.getTypeMirrorUtils().ignoreParameter(variable)) {
                    continue;
                }
                if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                    if( requiredChildElements == 1 ) {
                        Optional optional = variable.getAnnotation(Optional.class);
                        GroupRef groupRef = generateNestedProcessorGroup(optional);
                        complexContentExtension.setGroup(groupRef);
                        complexContentExtension.setAll(null);

                        Attribute attribute = new Attribute();
                        attribute.setUse(SchemaConstants.USE_OPTIONAL);
                        attribute.setName("text");
                        attribute.setType(SchemaConstants.STRING);

                        complexContentExtension.getAttributeOrAttributeGroup().add(attribute);
                    } else {
                        generateNestedProcessorElement(all, variable);
                    }
                } else if (variable.getAnnotation(Session.class) != null) {
                    // get the executable element for create session
                    ExecutableElement sessionCreate = createSessionForMethod(element);
                    // add a configurable argument for each session variable
                    for (VariableElement sessionVariable : sessionCreate.getParameters()) {
                        if (context.getTypeMirrorUtils().isCollection(sessionVariable.asType())) {
                            generateCollectionElement(targetNamespace, all, sessionVariable, true);
                        } else {
                            complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(sessionVariable, true));
                        }
                    }
                } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                    all.getParticle().add(objectFactory.createElement(generateXmlElement(variable.getSimpleName().toString(), targetNamespace)));
                } else if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                    generateCollectionElement(targetNamespace, all, variable);
                } else {
                    complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(variable));
                }
            }
        }

        if( all.getParticle().size() == 0 ) {
            complexContentExtension.setSequence(null);
            //complexContent.setMixed(false);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

    }

    private void generateNestedProcessorElement(ExplicitGroup all, VariableElement variable) {
        Optional optional = variable.getAnnotation(Optional.class);

        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(context.getNameUtils().uncamel(variable.getSimpleName().toString()));

        if (optional != null) {
            collectionElement.setMinOccurs(BigInteger.valueOf(0L));
       } else {
            collectionElement.setMinOccurs(BigInteger.valueOf(1L));
        }
        collectionElement.setMaxOccurs("1");

        LocalComplexType collectionComplexType = new LocalComplexType();
        GroupRef group = generateNestedProcessorGroup(optional);

        collectionComplexType.setGroup(group);
        collectionElement.setComplexType(collectionComplexType);

        Attribute attribute = new Attribute();
        attribute.setUse(SchemaConstants.USE_OPTIONAL);
        attribute.setName("text");
        attribute.setType(SchemaConstants.STRING);

        collectionComplexType.getAttributeOrAttributeGroup().add(attribute);

    }

    private GroupRef generateNestedProcessorGroup(Optional optional) {
        GroupRef group = new GroupRef();
        group.generateNestedProcessorGroup(SchemaConstants.MULE_MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT_TYPE);
        //if (optional != null) {
            group.setMinOccurs(BigInteger.valueOf(0L));
        //} else {
        //    group.setMinOccurs(BigInteger.valueOf(1L));
        //}
        group.setMaxOccurs("unbounded");
        return group;
    }

    private void generateCollectionElement(String targetNamespace, ExplicitGroup all, VariableElement variable) {
        generateCollectionElement(targetNamespace, all, variable, false);
    }

    private void generateCollectionElement(String targetNamespace, ExplicitGroup all, VariableElement variable, boolean forceOptional) {
        Optional optional = variable.getAnnotation(Optional.class);

        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(context.getNameUtils().uncamel(variable.getSimpleName().toString()));

        if (!forceOptional) {
            if (optional != null) {
                collectionElement.setMinOccurs(BigInteger.valueOf(0L));
            } else {
                collectionElement.setMinOccurs(BigInteger.valueOf(1L));
            }
        } else {
            collectionElement.setMinOccurs(BigInteger.valueOf(0L));
        }
        collectionElement.setMaxOccurs("1");

        String collectionName = context.getNameUtils().uncamel(context.getNameUtils().singularize(collectionElement.getName()));
        LocalComplexType collectionComplexType = generateCollectionComplexType(targetNamespace, collectionName, variable.asType());
        collectionElement.setComplexType(collectionComplexType);
    }

    private LocalComplexType generateCollectionComplexType(String targetNamespace, String name, TypeMirror type) {
        LocalComplexType collectionComplexType = new LocalComplexType();
        ExplicitGroup sequence = new ExplicitGroup();
        ExplicitGroup choice = new ExplicitGroup();

        if (context.getTypeMirrorUtils().isMap(type)) {
            collectionComplexType.setChoice(choice);
            choice.getParticle().add(objectFactory.createSequence(sequence));

            Any any = new Any();
            any.setProcessContents(LAX);
            any.setMinOccurs(new BigInteger("0"));
            any.setMaxOccurs(UNBOUNDED);

            ExplicitGroup anySequence = new ExplicitGroup();
            anySequence.getParticle().add(any);
            choice.getParticle().add(objectFactory.createSequence(anySequence));
        } else if (context.getTypeMirrorUtils().isArrayOrList(type)) {
            collectionComplexType.setSequence(sequence);
        }

        TopLevelElement collectionItemElement = new TopLevelElement();
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

        if (name != null) {
            collectionItemElement.setName(name);
        }

        collectionItemElement.setMinOccurs(BigInteger.valueOf(0L));
        collectionItemElement.setMaxOccurs(UNBOUNDED);

        collectionItemElement.setComplexType(generateComplexType(targetNamespace, name, type));

        Attribute ref = createAttribute(ATTRIBUTE_NAME_REF, true, SchemaConstants.STRING, "The reference object for this parameter");
        collectionComplexType.getAttributeOrAttributeGroup().add(ref);

        return collectionComplexType;
    }

    private LocalComplexType generateComplexType(String targetNamespace, String name, TypeMirror typeMirror) {
        if (context.getTypeMirrorUtils().isArrayOrList(typeMirror)) {
            DeclaredType variableType = (DeclaredType) typeMirror;
            java.util.List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();
            if (variableTypeParameters.size() != 0) {
                TypeMirror genericType = variableTypeParameters.get(0);

                if (isTypeSupported(genericType)) {
                    return generateComplexTypeWithRef(genericType);
                } else if (context.getTypeMirrorUtils().isArrayOrList(genericType) ||
                        context.getTypeMirrorUtils().isMap(genericType)) {
                    return generateCollectionComplexType(targetNamespace, "inner-" + name, genericType);
                } else if (context.getTypeMirrorUtils().isTransferObject(genericType)) {
                    return generatePojoComplexTypeWithRef(targetNamespace, genericType);
                } else if (context.getTypeMirrorUtils().isEnum(genericType)) {
                    return genereateEnumComplexType(genericType, targetNamespace);
                } else {
                    return generateRefComplexType(ATTRIBUTE_NAME_VALUE_REF);
                }
            } else {
                return generateRefComplexType(ATTRIBUTE_NAME_VALUE_REF);
            }
        } else if (context.getTypeMirrorUtils().isMap(typeMirror)) {
            DeclaredType variableType = (DeclaredType) typeMirror;
            java.util.List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();

            LocalComplexType mapComplexType = new LocalComplexType();
            Attribute keyAttribute = new Attribute();
            if (variableTypeParameters.size() > 0 && isTypeSupported(variableTypeParameters.get(0))) {
                keyAttribute.setName(ATTRIBUTE_NAME_KEY);
                keyAttribute.setType(SchemaTypeConversion.convertType(variableTypeParameters.get(0).toString(), schema.getTargetNamespace()));
            } else if (variableTypeParameters.size() > 0 && context.getTypeMirrorUtils().isEnum(variableTypeParameters.get(0))) {
                keyAttribute.setName(ATTRIBUTE_NAME_KEY);
                javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(variableTypeParameters.get(0));
                keyAttribute.setType(new QName(schema.getTargetNamespace(), enumElement.getSimpleName() + ENUM_TYPE_SUFFIX));
            } else {
                keyAttribute.setUse(SchemaConstants.USE_REQUIRED);
                keyAttribute.setName(ATTRIBUTE_NAME_KEY_REF);
                keyAttribute.setType(SchemaConstants.STRING);
            }

            if (variableTypeParameters.size() > 1 && isTypeSupported(variableTypeParameters.get(1))) {
                SimpleContent simpleContent = new SimpleContent();
                mapComplexType.setSimpleContent(simpleContent);
                SimpleExtensionType complexContentExtension = new SimpleExtensionType();
                complexContentExtension.setBase(SchemaTypeConversion.convertType(variableTypeParameters.get(1).toString(), schema.getTargetNamespace()));
                simpleContent.setExtension(complexContentExtension);

                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
                refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                complexContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
                complexContentExtension.getAttributeOrAttributeGroup().add(keyAttribute);
            } else if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isTransferObject(variableTypeParameters.get(1))) {
                ComplexContent simpleContent = new ComplexContent();
                mapComplexType.setComplexContent(simpleContent);
                ExtensionType complexContentExtension = new ExtensionType();
                QName qname = new QName(targetNamespace, ref(variableTypeParameters.get(1)).name() + TRANSFER_OBJECT_TYPE_SUFFIX);
                complexContentExtension.setBase(qname);
                simpleContent.setExtension(complexContentExtension);

                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
                refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                complexContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
                complexContentExtension.getAttributeOrAttributeGroup().add(keyAttribute);

            } else {
                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_REQUIRED);
                refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                mapComplexType.getAttributeOrAttributeGroup().add(refAttribute);
                mapComplexType.getAttributeOrAttributeGroup().add(keyAttribute);
            }

            return mapComplexType;
        }

        return null;
    }

    private LocalComplexType genereateEnumComplexType(TypeMirror genericType, String targetNamespace) {
        LocalComplexType complexType = new LocalComplexType();
        SimpleContent simpleContent = new SimpleContent();
        complexType.setSimpleContent(simpleContent);
        SimpleExtensionType simpleContentExtension = new SimpleExtensionType();
        javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(genericType);
        simpleContentExtension.setBase(new QName(targetNamespace, enumElement.getSimpleName() + ENUM_TYPE_SUFFIX));
        simpleContent.setExtension(simpleContentExtension);
        return complexType;
    }

    private LocalComplexType generatePojoComplexTypeWithRef(String targetNamespace, TypeMirror genericType) {
        LocalComplexType complexType = new LocalComplexType();
        ComplexContent simpleContent = new ComplexContent();
        complexType.setComplexContent(simpleContent);
        ExtensionType simpleContentExtension = new ExtensionType();
        QName qname = new QName(targetNamespace, ref(genericType).name() + TRANSFER_OBJECT_TYPE_SUFFIX);
        simpleContentExtension.setBase(qname);
        simpleContent.setExtension(simpleContentExtension);

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
        refAttribute.setType(SchemaConstants.STRING);

        simpleContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
        return complexType;
    }

    private LocalComplexType generateComplexTypeWithRef(TypeMirror genericType) {
        LocalComplexType complexType = new LocalComplexType();
        SimpleContent simpleContent = new SimpleContent();
        complexType.setSimpleContent(simpleContent);
        SimpleExtensionType simpleContentExtension = new SimpleExtensionType();
        simpleContentExtension.setBase(SchemaTypeConversion.convertType(genericType.toString(), schema.getTargetNamespace()));
        simpleContent.setExtension(simpleContentExtension);

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
        refAttribute.setType(SchemaConstants.STRING);

        simpleContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
        return complexType;
    }

    private LocalComplexType generateRefComplexType(String name) {
        LocalComplexType itemComplexType = new LocalComplexType();

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(SchemaConstants.USE_REQUIRED);
        refAttribute.setName(name);
        refAttribute.setType(SchemaConstants.STRING);

        itemComplexType.getAttributeOrAttributeGroup().add(refAttribute);
        return itemComplexType;
    }

    private TopLevelElement generateXmlElement(String elementName, String targetNamespace) {
        TopLevelElement xmlElement = new TopLevelElement();
        xmlElement.setName(elementName);
        xmlElement.setType(new QName(targetNamespace, XML_TYPE_SUFFIX));
        return xmlElement;
    }

    private ComplexType createAnyXmlType() {
        ComplexType xmlComplexType = new TopLevelComplexType();
        xmlComplexType.setName(XML_TYPE_SUFFIX);
        Any any = new Any();
        any.setProcessContents(LAX);
        any.setMinOccurs(new BigInteger("0"));
        any.setMaxOccurs(UNBOUNDED);
        ExplicitGroup all = new ExplicitGroup();
        all.getParticle().add(any);
        xmlComplexType.setSequence(all);

        Attribute ref = createAttribute(ATTRIBUTE_NAME_REF, true, SchemaConstants.STRING, "The reference object for this parameter");
        xmlComplexType.getAttributeOrAttributeGroup().add(ref);

        return xmlComplexType;
    }

    private void registerConfigElement(String targetNamespace, DevkitTypeElement typeElement) {

        ExtensionType config = registerExtension(ELEMENT_NAME_CONFIG);
        Attribute nameAttribute = createAttribute(ATTRIBUTE_NAME_NAME, true, SchemaConstants.STRING, "Give a name to this configuration so it can be later referenced by config-ref.");
        config.getAttributeOrAttributeGroup().add(nameAttribute);

        ExplicitGroup all = new ExplicitGroup();
        config.setSequence(all);

        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                generateCollectionElement(targetNamespace, all, variable);
            } else {
                config.getAttributeOrAttributeGroup().add(createAttribute(variable));
            }
        }

        // get the executable typeElement for create session
        ExecutableElement sessionCreate = createSessionForClass(typeElement);

        if (sessionCreate != null) {
            // add a configurable argument for each session variable
            for (VariableElement sessionVariable : sessionCreate.getParameters()) {
                if (context.getTypeMirrorUtils().isCollection(sessionVariable.asType())) {
                    generateCollectionElement(targetNamespace, all, sessionVariable, true);
                } else {
                    config.getAttributeOrAttributeGroup().add(createParameterAttribute(sessionVariable, true));
                }
            }

            TopLevelElement poolingProfile = new TopLevelElement();
            poolingProfile.setName("session-pooling-profile");
            poolingProfile.setType(SchemaConstants.MULE_POOLING_PROFILE_TYPE);
            poolingProfile.setMinOccurs(BigInteger.valueOf(0L));

            Annotation annotation = new Annotation();
            Documentation doc = new Documentation();
            doc.setSource("Characteristics of the session pool.");
            annotation.getAppinfoOrDocumentation().add(doc);

            poolingProfile.setAnnotation(annotation);

            all.getParticle().add(objectFactory.createElement(poolingProfile));
        }

        // add oauth callback configuration
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class) || typeElement.hasProcessorMethodWithParameter(HttpCallback.class)) {

            Attribute domainAttribute = new Attribute();
            domainAttribute.setUse(SchemaConstants.USE_OPTIONAL);
            domainAttribute.setName(DOMAIN_ATTRIBUTE_NAME);
            domainAttribute.setType(SchemaConstants.STRING);
            domainAttribute.setDefault(DOMAIN_DEFAULT_VALUE);

            Attribute localPortAttribute = new Attribute();
            localPortAttribute.setUse(SchemaConstants.USE_OPTIONAL);
            localPortAttribute.setName(LOCAL_PORT_ATTRIBUTE_NAME);
            localPortAttribute.setType(SchemaConstants.STRING);
            localPortAttribute.setDefault(PORT_DEFAULT_VALUE);

            Attribute remotePortAttribute = new Attribute();
            remotePortAttribute.setUse(SchemaConstants.USE_OPTIONAL);
            remotePortAttribute.setName(REMOTE_PORT_ATTRIBUTE_NAME);
            remotePortAttribute.setType(SchemaConstants.STRING);
            remotePortAttribute.setDefault(PORT_DEFAULT_VALUE);

            Attribute asyncAttribute = new Attribute();
            asyncAttribute.setUse(SchemaConstants.USE_OPTIONAL);
            asyncAttribute.setName(ASYNC_ATTRIBUTE_NAME);
            asyncAttribute.setType(SchemaConstants.BOOLEAN);
            asyncAttribute.setDefault(ASYNC_DEFAULT_VALUE);

            TopLevelElement httpCallbackConfig = new TopLevelElement();
            httpCallbackConfig.setName(HTTP_CALLBACK_CONFIG_ELEMENT_NAME);
            httpCallbackConfig.setMinOccurs(BigInteger.ZERO);
            httpCallbackConfig.setMaxOccurs("1");

            Annotation annotation = new Annotation();
            Documentation doc = new Documentation();
            doc.setSource("Config for http callbacks.");
            annotation.getAppinfoOrDocumentation().add(doc);
            httpCallbackConfig.setAnnotation(annotation);

            ExtensionType extensionType = new ExtensionType();
            extensionType.setBase(SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE);
            extensionType.getAttributeOrAttributeGroup().add(localPortAttribute);
            extensionType.getAttributeOrAttributeGroup().add(remotePortAttribute);
            extensionType.getAttributeOrAttributeGroup().add(domainAttribute);
            extensionType.getAttributeOrAttributeGroup().add(asyncAttribute);

            ComplexContent complextContent = new ComplexContent();
            complextContent.setExtension(extensionType);

            LocalComplexType localComplexType = new LocalComplexType();
            localComplexType.setComplexContent(complextContent);

            httpCallbackConfig.setComplexType(localComplexType);
            all.getParticle().add(objectFactory.createElement(httpCallbackConfig));
        }

        if (typeElement.getAnnotation(Module.class).poolable()) {
            //<xsd:element name="abstract-pooling-profile" abstract="true" type="abstractPoolingProfileType"/>

            TopLevelElement poolingProfile = new TopLevelElement();
            poolingProfile.setName("pooling-profile");
            poolingProfile.setType(SchemaConstants.MULE_POOLING_PROFILE_TYPE);
            poolingProfile.setMinOccurs(BigInteger.valueOf(0L));

            Annotation annotation = new Annotation();
            Documentation doc = new Documentation();
            doc.setSource("Characteristics of the object pool.");
            annotation.getAppinfoOrDocumentation().add(doc);

            poolingProfile.setAnnotation(annotation);

            all.getParticle().add(objectFactory.createElement(poolingProfile));
        }

        if( all.getParticle().size() == 0 ) {
            config.setSequence(null);
        }
    }

    private Attribute createAttribute(VariableElement variable) {
        Named named = variable.getAnnotation(Named.class);
        Optional optional = variable.getAnnotation(Optional.class);
        Default def = variable.getAnnotation(Default.class);

        String name = variable.getSimpleName().toString();
        if (named != null && named.value().length() > 0)
            name = named.value();

        Attribute attribute = new Attribute();

        // set whenever or not is optional
        attribute.setUse(optional != null ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);

        if (isTypeSupported(variable.asType())) {
            attribute.setName(name);
            attribute.setType(SchemaTypeConversion.convertType(variable.asType().toString(), schema.getTargetNamespace()));
        } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
            attribute.setName(name);
            javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(variable.asType());
            attribute.setType(new QName(schema.getTargetNamespace(), enumElement.getSimpleName() + ENUM_TYPE_SUFFIX));
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + REF_SUFFIX);
            attribute.setType(SchemaConstants.STRING);
        }

        // add default value
        if (def != null && def.value().length() > 0) {
            attribute.setDefault(def.value());
        }
        return attribute;
    }

    private Attribute createParameterAttribute(VariableElement variable) {
        return createParameterAttribute(variable, false);
    }

    private Attribute createParameterAttribute(VariableElement variable, boolean forceOptional) {
        Named named = variable.getAnnotation(Named.class);
        Optional optional = variable.getAnnotation(Optional.class);
        Default def = variable.getAnnotation(Default.class);

        String name = variable.getSimpleName().toString();
        if (named != null && named.value().length() > 0)
            name = named.value();

        Attribute attribute = new Attribute();

        // set whenever or not is optional
        if (!forceOptional) {
            attribute.setUse(optional != null ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);
        } else {
            attribute.setUse(SchemaConstants.USE_OPTIONAL);
        }


        if (isTypeSupported(variable.asType())) {
            attribute.setName(name);
            attribute.setType(SchemaTypeConversion.convertType(variable.asType().toString(), schema.getTargetNamespace()));
        } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
            attribute.setName(name);
            javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(variable.asType());
            attribute.setType(new QName(schema.getTargetNamespace(), enumElement.getSimpleName() + ENUM_TYPE_SUFFIX));
        } else if (variable.asType().toString().contains(HttpCallback.class.getName())) {
            attribute.setName(context.getNameUtils().uncamel(name) + "-flow-ref");
            attribute.setType(SchemaConstants.STRING);
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + REF_SUFFIX);
            attribute.setType(SchemaConstants.STRING);
        }

        // add default value
        if (def != null && def.value().length() > 0) {
            attribute.setDefault(def.value());
        }
        return attribute;
    }

    private boolean isTypeSupported(TypeMirror typeMirror) {
        return SchemaTypeConversion.isSupported(typeMirror.toString());
    }

    private void importMuleNamespace() {
        Import muleSchemaImport = new Import();
        muleSchemaImport.setNamespace(SchemaConstants.MULE_NAMESPACE);
        muleSchemaImport.setSchemaLocation(SchemaConstants.MULE_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    }

    private void importSpringFrameworkNamespace() {
        Import springFrameworkImport = new Import();
        springFrameworkImport.setNamespace(SchemaConstants.SPRING_FRAMEWORK_NAMESPACE);
        springFrameworkImport.setSchemaLocation(SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(springFrameworkImport);
    }

    private void importXmlNamespace() {
        Import xmlImport = new Import();
        xmlImport.setNamespace(SchemaConstants.XML_NAMESPACE);
        schema.getIncludeOrImportOrRedefine().add(xmlImport);
    }

    private Attribute createAttribute(String name, boolean optional, QName type, String description) {
        Attribute attr = new Attribute();
        attr.setName(name);
        attr.setUse(optional ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);
        attr.setType(type);
        Annotation nameAnnotation = new Annotation();
        attr.setAnnotation(nameAnnotation);
        Documentation nameDocumentation = new Documentation();
        nameDocumentation.getContent().add(description);
        nameAnnotation.getAppinfoOrDocumentation().add(nameDocumentation);

        return attr;
    }

    private Element registerTransformer(String name) {
        Element transformer = new TopLevelElement();
        transformer.setName(name);
        transformer.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_TRANSFORMER);
        transformer.setType(SchemaConstants.MULE_ABSTRACT_TRANSFORMER_TYPE);

        return transformer;
    }

    private ExtensionType registerExtension(String name) {
        LocalComplexType complexType = new LocalComplexType();

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_EXTENSION);
        extension.setComplexType(complexType);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);

        return complexContentExtension;
    }

    private void registerTypes() {
        registerType("integerType", SchemaConstants.INTEGER);
        registerType("decimalType", SchemaConstants.DECIMAL);
        registerType("floatType", SchemaConstants.FLOAT);
        registerType("doubleType", SchemaConstants.DOUBLE);
        registerType("dateTimeType", SchemaConstants.DATETIME);
        registerType("longType", SchemaConstants.LONG);
        registerType("byteType", SchemaConstants.BYTE);
        registerType("booleanType", SchemaConstants.BOOLEAN);
        registerType("anyUriType", SchemaConstants.ANYURI);
        registerType("charType", SchemaConstants.STRING, 1, 1);

        registerAnyXmlType();
    }

    private void registerAnyXmlType() {
        ComplexType xmlComplexType = createAnyXmlType();
        schema.getSimpleTypeOrComplexTypeOrGroup().add(xmlComplexType);
    }

    private void registerType(String name, QName base) {
        registerType(name, base, -1, -1);
    }

    private void registerType(String name, QName base, int minlen, int maxlen) {
        SimpleType simpleType = new TopLevelSimpleType();
        simpleType.setName(name);
        Union union = new Union();
        simpleType.setUnion(union);

        union.getSimpleType().add(createSimpleType(base, minlen, maxlen));
        union.getSimpleType().add(createExpressionSimpleType());

        schema.getSimpleTypeOrComplexTypeOrGroup().add(simpleType);
    }

    private LocalSimpleType createSimpleType(QName base, int minlen, int maxlen) {
        LocalSimpleType simpleType = new LocalSimpleType();
        Restriction restriction = new Restriction();
        restriction.setBase(base);

        if (minlen != -1) {
            NumFacet minLenFacet = new NumFacet();
            minLenFacet.setValue(Integer.toString(minlen));
            JAXBElement<NumFacet> element = objectFactory.createMinLength(minLenFacet);
            restriction.getFacets().add(element);
        }

        if (maxlen != -1) {
            NumFacet maxLenFacet = new NumFacet();
            maxLenFacet.setValue(Integer.toString(maxlen));
            JAXBElement<NumFacet> element = objectFactory.createMaxLength(maxLenFacet);
            restriction.getFacets().add(element);
        }

        simpleType.setRestriction(restriction);

        return simpleType;
    }

    private LocalSimpleType createExpressionSimpleType() {
        LocalSimpleType expression = new LocalSimpleType();
        Restriction restriction = new Restriction();
        expression.setRestriction(restriction);
        restriction.setBase(SchemaConstants.STRING);
        Pattern pattern = new Pattern();
        pattern.setValue("\\#\\[[^\\]]+\\]");
        restriction.getFacets().add(pattern);

        return expression;
    }
}