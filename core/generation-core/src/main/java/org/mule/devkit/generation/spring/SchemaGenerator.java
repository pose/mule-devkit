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
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.HttpCallback;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.api.GenerationException;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.schema.Annotation;
import org.mule.devkit.model.schema.Any;
import org.mule.devkit.model.schema.Attribute;
import org.mule.devkit.model.schema.ComplexContent;
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
import org.mule.devkit.model.schema.SchemaConstants;
import org.mule.devkit.model.schema.SchemaLocation;
import org.mule.devkit.model.schema.SchemaTypeConversion;
import org.mule.devkit.model.schema.SimpleContent;
import org.mule.devkit.model.schema.SimpleExtensionType;
import org.mule.devkit.model.schema.TopLevelComplexType;
import org.mule.devkit.model.schema.TopLevelElement;
import org.mule.devkit.model.schema.TopLevelSimpleType;
import org.mule.devkit.model.schema.Union;
import org.mule.util.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchemaGenerator extends AbstractModuleGenerator {

    private ObjectFactory objectFactory;

    public SchemaGenerator() {
        objectFactory = new ObjectFactory();
    }

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) throws GenerationException {
        String targetNamespace = type.getXmlNamespace();

        Schema schema = new Schema();
        schema.setTargetNamespace(targetNamespace);
        schema.setElementFormDefault(FormChoice.QUALIFIED);
        schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);

        importXmlNamespace(schema);
        importSpringFrameworkNamespace(schema);
        importMuleNamespace(schema);
        importMuleDevKitNamespace(schema);

        registerTypes(schema);
        registerConfigElement(schema, targetNamespace, type);
        registerProcessorsAndSources(schema, targetNamespace, type);
        registerTransformers(schema, type);
        registerEnums(schema, type);
        registerComplexTypes(schema, type);

        String fileName = "META-INF/mule-" + type.getModuleName() + SchemaConstants.XSD_EXTENSION;

        String versionedLocation = type.getVersionedSchemaLocation();
        String currentLocation = type.getCurrentSchemaLocation();
        String namespaceHandlerName = ctx().getCodeModel()._class(DefinedClassRoles.NAMESPACE_HANDLER, ref(type)).boxify().fullName();
        String className = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type)).boxify().fullName();

        SchemaLocation versionedSchemaLocation = new SchemaLocation(schema, schema.getTargetNamespace(), fileName, versionedLocation, namespaceHandlerName, className);

        ctx().getSchemaModel().addSchemaLocation(versionedSchemaLocation);

        if (currentLocation != null) {
            SchemaLocation currentSchemaLocation = new SchemaLocation(null, schema.getTargetNamespace(), fileName, currentLocation, namespaceHandlerName, className);
            ctx().getSchemaModel().addSchemaLocation(currentSchemaLocation);
        }
    }

    private void registerComplexTypes(Schema schema, Type type) {
        Set<TypeMirror> registeredComplexTypes = new HashSet<TypeMirror>();

        for (Field field : type.getFields()) {

            if (!isTypeSupported(field.asType()) && !(field.isArrayOrList() || field.isMap()) &&
                    !field.isEnum() && !field.asType().toString().equals("java.lang.Object")) {
                if (!registeredComplexTypes.contains(field.asType())) {
                    registerComplexType(schema, field);
                    registeredComplexTypes.add(field.asType());
                }
            }

        }

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {
            for (Parameter variable : method.getParameters()) {
                if (!isTypeSupported(variable.asType()) && !(variable.isArrayOrList() || variable.isMap()) &&
                        !variable.isEnum() && !variable.asType().toString().equals("java.lang.Object") && !registeredComplexTypes.contains(variable.asType())) {
                    registerComplexType(schema, variable);
                    registeredComplexTypes.add(variable.asType());
                } else if (variable.isCollection()) {
                    for (Identifiable variableTypeParameter : variable.getTypeArguments()) {
                        if (!isTypeSupported(variableTypeParameter.asType()) && !(variableTypeParameter.isArrayOrList() || variableTypeParameter.isMap()) &&
                                !variableTypeParameter.isEnum() && !variableTypeParameter.asType().toString().equals("java.lang.Object")
                                && !registeredComplexTypes.contains(variableTypeParameter.asType())) {
                            registerComplexType(schema, variableTypeParameter);
                            registeredComplexTypes.add(variableTypeParameter.asType());
                        }
                    }
                }
            }
        }
    }

    private void registerComplexType(Schema schema, Identifiable element) {
        TopLevelComplexType complexType = new TopLevelComplexType();
        complexType.setName(element.getSimpleName() + SchemaConstants.OBJECT_TYPE_SUFFIX);

        ExplicitGroup all = new ExplicitGroup();
        complexType.setSequence(all);

        if( element instanceof Type) {
            Type type = (Type)element;
            for( Field field : type.getFields() ) {
                if( field.isCollection() ) {
                    generateCollectionElement(schema, schema.getTargetNamespace(), all, field);
                } else {
                    complexType.getAttributeOrAttributeGroup().add(createAttribute(schema, field));
                }
            }
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

    }

    private void registerEnums(Schema schema, Type type) {
        Set<TypeMirror> registeredEnums = new HashSet<TypeMirror>();

        for (Field field : type.getFields()) {
            if (field.isEnum()) {
                if (!registeredEnums.contains(field.asType())) {
                    registerEnum(schema, field.asType());
                    registeredEnums.add(field.asType());
                }
            }

        }

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {
            for (Parameter variable : method.getParameters()) {
                if (variable.isEnum() && !registeredEnums.contains(variable.asType())) {
                    registerEnum(schema, variable.asType());
                    registeredEnums.add(variable.asType());
                } else if (variable.isCollection()) {
                    for (Identifiable variableTypeParameter : variable.getTypeArguments()) {
                        if (variableTypeParameter.isEnum() && !registeredEnums.contains(variableTypeParameter.asType())) {
                            registerEnum(schema, variableTypeParameter.asType());
                            registeredEnums.add(variableTypeParameter.asType());
                        }
                    }
                }
            }
        }

        for (Method method : type.getMethodsAnnotatedWith(Source.class)) {
            for (Parameter variable : method.getParameters()) {
                if (!variable.isEnum()) {
                    continue;
                }

                if (!registeredEnums.contains(variable.asType())) {
                    registerEnum(schema, variable.asType());
                    registeredEnums.add(variable.asType());
                }
            }
        }
    }

    private void registerEnum(Schema schema, TypeMirror enumType) {
        javax.lang.model.element.Element enumElement = ctx().getTypeUtils().asElement(enumType);

        TopLevelSimpleType enumSimpleType = new TopLevelSimpleType();
        enumSimpleType.setName(enumElement.getSimpleName() + SchemaConstants.ENUM_TYPE_SUFFIX);

        Union union = new Union();
        union.getSimpleType().add(createEnumSimpleType(enumElement));
        union.getSimpleType().add(createExpressionAndPropertyPlaceHolderSimpleType());
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

    private void registerTransformers(Schema schema, Type type) {
        for (Method method : type.getMethodsAnnotatedWith(Transformer.class)) {
            Element transformerElement = registerTransformer(NameUtils.uncamel(method.getSimpleName().toString()));
            schema.getSimpleTypeOrComplexTypeOrGroup().add(transformerElement);
        }
    }

    private void registerProcessorsAndSources(Schema schema, String targetNamespace, Type type) {
        if (type.hasAnnotation(OAuth.class) || type.hasAnnotation(OAuth2.class)) {
            // generate an MP to start the OAuth process
            registerProcessorElement(schema, false, targetNamespace, "authorize", "AuthorizeType", "Starts OAuth authorization process. It must be called from a flow with an http:inbound-endpoint.");
            registerProcessorType(schema, false, targetNamespace, "AuthorizeType", null);
        }

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {
            String name = method.getSimpleName().toString();
            Processor processor = method.getAnnotation(Processor.class);
            if (processor.name().length() > 0) {
                name = processor.name();
            }
            String typeName = StringUtils.capitalize(name) + SchemaConstants.TYPE_SUFFIX;

            registerProcessorElement(schema, processor.intercepting(), targetNamespace, name, typeName, method.getJavaDocSummary());

            registerProcessorType(schema, processor.intercepting(), targetNamespace, typeName, method);
        }

        for (Method method : type.getMethodsAnnotatedWith(Source.class)) {
            String name = method.getSimpleName().toString();
            Source source = method.getAnnotation(Source.class);
            if (source.name().length() > 0) {
                name = source.name();
            }
            String typeName = StringUtils.capitalize(name) + SchemaConstants.TYPE_SUFFIX;

            registerSourceElement(schema, targetNamespace, name, typeName, method);

            registerSourceType(schema, targetNamespace, typeName, method);
        }

    }

    private void registerProcessorElement(Schema schema, boolean intercepting, String targetNamespace, String name, String typeName, String docText) {
        Element element = new TopLevelElement();
        element.setName(NameUtils.uncamel(name));
        if (intercepting) {
            element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR);
        } else {
            element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR);
        }
        element.setType(new QName(targetNamespace, typeName));

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(docText);
        annotation.getAppinfoOrDocumentation().add(doc);

        element.setAnnotation(annotation);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerSourceElement(Schema schema, String targetNamespace, String name, String typeName, Method executableElement) {
        Element element = new TopLevelElement();
        element.setName(NameUtils.uncamel(name));
        element.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_INBOUND_ENDPOINT);
        element.setType(new QName(targetNamespace, typeName));

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(executableElement.getJavaDocSummary());
        annotation.getAppinfoOrDocumentation().add(doc);

        element.setAnnotation(annotation);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerProcessorType(Schema schema, boolean intercepting, String targetNamespace, String name, Method element) {
        if (intercepting) {
            registerExtendedType(schema, SchemaConstants.MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR_TYPE, targetNamespace, name, element);
        } else {
            registerExtendedType(schema, SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE, targetNamespace, name, element);
        }
    }

    private void registerSourceType(Schema schema, String targetNamespace, String name, Method element) {
        registerExtendedType(schema, SchemaConstants.MULE_ABSTRACT_INBOUND_ENDPOINT_TYPE, targetNamespace, name, element);
    }

    private void registerExtendedType(Schema schema, QName base, String targetNamespace, String name, Method element) {
        TopLevelComplexType complexType = new TopLevelComplexType();
        complexType.setName(name);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(base);
        complexContent.setExtension(complexContentExtension);

        Attribute configRefAttr = createAttribute(SchemaConstants.ATTRIBUTE_NAME_CONFIG_REF, true, SchemaConstants.STRING, "Specify which configuration to use for this invocation.");
        complexContentExtension.getAttributeOrAttributeGroup().add(configRefAttr);

        ExplicitGroup all = new ExplicitGroup();
        complexContentExtension.setSequence(all);

        if (element != null) {
            int requiredChildElements = 0;
            for (Parameter variable : element.getParameters()) {
                if (variable.shouldBeIgnored()) {
                    continue;
                }
                if (variable.isNestedProcessor()) {
                    requiredChildElements++;
                } else if (variable.isXmlType()) {
                    requiredChildElements++;
                } else if (variable.isCollection()) {
                    requiredChildElements++;
                }
            }
            for (Parameter variable : element.getParameters()) {
                if (variable.shouldBeIgnored()) {
                    continue;
                }
                if (variable.isNestedProcessor()) {
                    if (requiredChildElements == 1) {
                        GroupRef groupRef = generateNestedProcessorGroup();
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
                } else if (variable.isXmlType()) {
                    all.getParticle().add(objectFactory.createElement(generateXmlElement(variable.getSimpleName().toString(), targetNamespace)));
                } else if (variable.isCollection()) {
                    generateCollectionElement(schema, targetNamespace, all, variable);
                } else {
                    complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(schema, variable));
                }
            }

            Method connectExecutableElement = connectForMethod(element);
            if (connectExecutableElement != null) {
                if (element.getAnnotation(Processor.class) != null) {
                    Attribute retryMaxAttr = createAttribute(SchemaConstants.ATTRIBUTE_RETRY_MAX, true, SchemaConstants.STRING, SchemaConstants.ATTRIBUTE_RETRY_MAX_DESCRIPTION);
                    retryMaxAttr.setDefault("1");
                    complexContentExtension.getAttributeOrAttributeGroup().add(retryMaxAttr);
                }

                for (Parameter connectVariable : connectExecutableElement.getParameters()) {
                    if (connectVariable.isCollection()) {
                        generateCollectionElement(schema, targetNamespace, all, connectVariable, true);
                    } else {
                        complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(schema, connectVariable, true));
                    }
                }
            }
        }

        if (all.getParticle().size() == 0) {
            complexContentExtension.setSequence(null);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

    }

    private void generateNestedProcessorElement(ExplicitGroup all, Variable variable) {
        Optional optional = variable.getAnnotation(Optional.class);

        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(NameUtils.uncamel(variable.getSimpleName().toString()));

        if (optional != null) {
            collectionElement.setMinOccurs(BigInteger.valueOf(0L));
        } else {
            collectionElement.setMinOccurs(BigInteger.valueOf(1L));
        }
        collectionElement.setMaxOccurs("1");

        LocalComplexType collectionComplexType = new LocalComplexType();
        GroupRef group = generateNestedProcessorGroup();

        collectionComplexType.setGroup(group);
        collectionElement.setComplexType(collectionComplexType);

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(variable.parent().getJavaDocParameterSummary(variable.getSimpleName().toString()));
        annotation.getAppinfoOrDocumentation().add(doc);

        collectionElement.setAnnotation(annotation);

        Attribute attribute = new Attribute();
        attribute.setUse(SchemaConstants.USE_OPTIONAL);
        attribute.setName("text");
        attribute.setType(SchemaConstants.STRING);

        collectionComplexType.getAttributeOrAttributeGroup().add(attribute);
    }

    private GroupRef generateNestedProcessorGroup() {
        GroupRef group = new GroupRef();
        group.generateNestedProcessorGroup(SchemaConstants.MULE_MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT_TYPE);
        group.setMinOccurs(BigInteger.valueOf(0L));
        group.setMaxOccurs("unbounded");
        return group;
    }

    private void generateCollectionElement(Schema schema, String targetNamespace, ExplicitGroup all, Variable variable) {
        generateCollectionElement(schema, targetNamespace, all, variable, false);
    }

    private void generateCollectionElement(Schema schema, String targetNamespace, ExplicitGroup all, Variable variable, boolean forceOptional) {
        Optional optional = variable.getAnnotation(Optional.class);

        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(NameUtils.uncamel(variable.getSimpleName().toString()));

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

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(variable.parent().getJavaDocParameterSummary(variable.getSimpleName().toString()));
        annotation.getAppinfoOrDocumentation().add(doc);

        collectionElement.setAnnotation(annotation);

        String collectionName = NameUtils.uncamel(NameUtils.singularize(collectionElement.getName()));
        LocalComplexType collectionComplexType = generateCollectionComplexType(schema, targetNamespace, collectionName, variable);
        collectionElement.setComplexType(collectionComplexType);
    }

    private LocalComplexType generateCollectionComplexType(Schema schema, String targetNamespace, String name, Identifiable type) {
        LocalComplexType collectionComplexType = new LocalComplexType();
        ExplicitGroup sequence = new ExplicitGroup();
        ExplicitGroup choice = new ExplicitGroup();

        if (type.isMap()) {
            collectionComplexType.setChoice(choice);
            choice.getParticle().add(objectFactory.createSequence(sequence));

            Any any = new Any();
            any.setProcessContents(SchemaConstants.LAX);
            any.setMinOccurs(new BigInteger("0"));
            any.setMaxOccurs(SchemaConstants.UNBOUNDED);

            ExplicitGroup anySequence = new ExplicitGroup();
            anySequence.getParticle().add(any);
            choice.getParticle().add(objectFactory.createSequence(anySequence));
        } else if (type.isArrayOrList()) {
            collectionComplexType.setSequence(sequence);
        }

        TopLevelElement collectionItemElement = new TopLevelElement();
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

        if (name != null) {
            collectionItemElement.setName(name);
        }

        collectionItemElement.setMinOccurs(BigInteger.valueOf(0L));
        collectionItemElement.setMaxOccurs(SchemaConstants.UNBOUNDED);

        collectionItemElement.setComplexType(generateComplexType(schema, targetNamespace, name, type));

        Attribute ref = createAttribute(SchemaConstants.ATTRIBUTE_NAME_REF, true, SchemaConstants.STRING, "The reference object for this parameter");
        collectionComplexType.getAttributeOrAttributeGroup().add(ref);

        return collectionComplexType;
    }

    private LocalComplexType generateComplexType(Schema schema, String targetNamespace, String name, Identifiable typeMirror) {
        if (typeMirror.isArrayOrList()) {
            java.util.List<Identifiable> variableTypeParameters = typeMirror.getTypeArguments();
            if (variableTypeParameters.size() != 0) {
                Identifiable genericType = variableTypeParameters.get(0);

                if (isTypeSupported(genericType.asType())) {
                    return generateComplexTypeWithRef(schema, genericType);
                } else if (genericType.isArrayOrList() ||
                        genericType.isMap()) {
                    return generateCollectionComplexType(schema, targetNamespace, SchemaConstants.INNER_PREFIX + name, genericType);
                } else if (genericType.isEnum()) {
                    return genereateEnumComplexType(genericType, targetNamespace);
                } else {
                    return generateExtendedRefComplexType(schema, genericType, SchemaConstants.ATTRIBUTE_NAME_VALUE_REF);
                }
            } else {
                return generateRefComplexType(SchemaConstants.ATTRIBUTE_NAME_VALUE_REF);
            }
        } else if (typeMirror.isMap()) {
            java.util.List<Identifiable> variableTypeParameters = typeMirror.getTypeArguments();

            LocalComplexType mapComplexType = new LocalComplexType();
            Attribute keyAttribute = new Attribute();
            if (variableTypeParameters.size() > 0 && isTypeSupported(variableTypeParameters.get(0).asType())) {
                keyAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_KEY);
                keyAttribute.setType(SchemaTypeConversion.convertType(variableTypeParameters.get(0).asType().toString(), schema.getTargetNamespace()));
            } else if (variableTypeParameters.size() > 0 && variableTypeParameters.get(0).isEnum()) {
                keyAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_KEY);
                keyAttribute.setType(new QName(schema.getTargetNamespace(), variableTypeParameters.get(0).getSimpleName() + SchemaConstants.ENUM_TYPE_SUFFIX));
            } else {
                keyAttribute.setUse(SchemaConstants.USE_REQUIRED);
                keyAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_KEY_REF);
                keyAttribute.setType(SchemaConstants.STRING);
            }

            if (variableTypeParameters.size() > 1 && isTypeSupported(variableTypeParameters.get(1).asType())) {
                SimpleContent simpleContent = new SimpleContent();
                mapComplexType.setSimpleContent(simpleContent);
                SimpleExtensionType complexContentExtension = new SimpleExtensionType();
                complexContentExtension.setBase(SchemaTypeConversion.convertType(variableTypeParameters.get(1).asType().toString(), schema.getTargetNamespace()));
                simpleContent.setExtension(complexContentExtension);

                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
                refAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                complexContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
                complexContentExtension.getAttributeOrAttributeGroup().add(keyAttribute);
            } else {
                SimpleContent simpleContent = new SimpleContent();
                mapComplexType.setSimpleContent(simpleContent);
                SimpleExtensionType complexContentExtension = new SimpleExtensionType();
                complexContentExtension.setBase(new QName(SchemaConstants.XSD_NAMESPACE, "string", "xs"));
                simpleContent.setExtension(complexContentExtension);

                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
                refAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                complexContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
                complexContentExtension.getAttributeOrAttributeGroup().add(keyAttribute);

                /*
                Attribute refAttribute = new Attribute();
                refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
                refAttribute.setName(ATTRIBUTE_NAME_VALUE_REF);
                refAttribute.setType(SchemaConstants.STRING);

                mapComplexType.getAttributeOrAttributeGroup().add(refAttribute);
                mapComplexType.getAttributeOrAttributeGroup().add(keyAttribute);
                */
            }

            return mapComplexType;
        }

        return null;
    }

    private LocalComplexType genereateEnumComplexType(Identifiable genericType, String targetNamespace) {
        LocalComplexType complexType = new LocalComplexType();
        SimpleContent simpleContent = new SimpleContent();
        complexType.setSimpleContent(simpleContent);
        SimpleExtensionType simpleContentExtension = new SimpleExtensionType();
        //javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(genericType);
        simpleContentExtension.setBase(new QName(targetNamespace, genericType.getSimpleName() + SchemaConstants.ENUM_TYPE_SUFFIX));
        simpleContent.setExtension(simpleContentExtension);
        return complexType;
    }

    private LocalComplexType generateComplexTypeWithRef(Schema schema, Identifiable genericType) {
        LocalComplexType complexType = new LocalComplexType();
        SimpleContent simpleContent = new SimpleContent();
        complexType.setSimpleContent(simpleContent);
        SimpleExtensionType simpleContentExtension = new SimpleExtensionType();
        QName extensionBase = SchemaTypeConversion.convertType(genericType.asType().toString(), schema.getTargetNamespace());
        simpleContentExtension.setBase(extensionBase);
        simpleContent.setExtension(simpleContentExtension);

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        refAttribute.setName(SchemaConstants.ATTRIBUTE_NAME_VALUE_REF);
        refAttribute.setType(SchemaConstants.STRING);

        simpleContentExtension.getAttributeOrAttributeGroup().add(refAttribute);
        return complexType;
    }

    private LocalComplexType generateExtendedRefComplexType(Schema schema, Identifiable element, String name) {
        LocalComplexType itemComplexType = new LocalComplexType();
        itemComplexType.setComplexContent(new ComplexContent());
        itemComplexType.getComplexContent().setExtension(new ExtensionType());
        itemComplexType.getComplexContent().getExtension().setBase(
                new QName(schema.getTargetNamespace(), element.getSimpleName() + SchemaConstants.OBJECT_TYPE_SUFFIX)
        ); // base to the element type

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        refAttribute.setName(name);
        refAttribute.setType(SchemaConstants.STRING);

        itemComplexType.getComplexContent().getExtension().getAttributeOrAttributeGroup().add(refAttribute);
        return itemComplexType;
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
        xmlElement.setType(new QName(targetNamespace, SchemaConstants.XML_TYPE_SUFFIX));
        return xmlElement;
    }

    private TopLevelComplexType createAnyXmlType() {
        TopLevelComplexType xmlComplexType = new TopLevelComplexType();
        xmlComplexType.setName(SchemaConstants.XML_TYPE_SUFFIX);
        Any any = new Any();
        any.setProcessContents(SchemaConstants.LAX);
        any.setMinOccurs(new BigInteger("0"));
        any.setMaxOccurs(SchemaConstants.UNBOUNDED);
        ExplicitGroup all = new ExplicitGroup();
        all.getParticle().add(any);
        xmlComplexType.setSequence(all);

        Attribute ref = createAttribute(SchemaConstants.ATTRIBUTE_NAME_REF, true, SchemaConstants.STRING, SchemaConstants.ATTRIBUTE_NAME_REF_DESCRIPTION);
        xmlComplexType.getAttributeOrAttributeGroup().add(ref);

        return xmlComplexType;
    }

    private void registerConfigElement(Schema schema, String targetNamespace, Type type) {

        DefinedClass moduleClass = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));
        Map<QName, String> otherAttributes = new HashMap<QName, String>();
        otherAttributes.put(SchemaConstants.MULE_DEVKIT_JAVA_CLASS_TYPE, moduleClass.fullName());
        ExtensionType config = registerExtension(schema, SchemaConstants.ELEMENT_NAME_CONFIG, otherAttributes);
        Attribute nameAttribute = createAttribute(SchemaConstants.ATTRIBUTE_NAME_NAME, true, SchemaConstants.STRING, SchemaConstants.ATTRIBUTE_NAME_NAME_DESCRIPTION);
        config.getAttributeOrAttributeGroup().add(nameAttribute);

        ExplicitGroup all = new ExplicitGroup();
        config.setSequence(all);

        for (Field variable : type.getFieldsAnnotatedWith(Configurable.class)) {
            if (variable.isCollection()) {
                generateCollectionElement(schema, targetNamespace, all, variable);
            } else {
                config.getAttributeOrAttributeGroup().add(createAttribute(schema, variable));
            }
        }

        for (Field variable : type.getFieldsAnnotatedWith(Inject.class)) {
            if (variable.asType().toString().equals("org.mule.api.store.ObjectStore")) {
                config.getAttributeOrAttributeGroup().add(createObjectStoreRefAttribute(variable));
            }
        }

        // get the executable type for create connectivity
        Method connectMethod = connectMethodForClass(type);

        if (connectMethod != null) {
            // add a configurable argument for each connectivity variable
            for (Parameter connectVariable : connectMethod.getParameters()) {
                if (connectVariable.isCollection()) {
                    generateCollectionElement(schema, targetNamespace, all, connectVariable, true);
                } else {
                    config.getAttributeOrAttributeGroup().add(createParameterAttribute(schema, connectVariable, true));
                }
            }

            TopLevelElement poolingProfile = new TopLevelElement();
            poolingProfile.setName(SchemaConstants.CONNECTION_POOLING_PROFILE);
            poolingProfile.setType(SchemaConstants.MULE_POOLING_PROFILE_TYPE);
            poolingProfile.setMinOccurs(BigInteger.valueOf(0L));

            Annotation annotation = new Annotation();
            Documentation doc = new Documentation();
            doc.getContent().add(SchemaConstants.CONNECTION_POOLING_PROFILE_ELEMENT_DESCRIPTION);
            annotation.getAppinfoOrDocumentation().add(doc);

            poolingProfile.setAnnotation(annotation);

            all.getParticle().add(objectFactory.createElement(poolingProfile));
        }

        // add oauth callback configuration
        if (type.hasAnnotation(OAuth.class) || type.hasAnnotation(OAuth2.class)) {
            generateHttpCallbackElement(SchemaConstants.OAUTH_CALLBACK_CONFIG_ELEMENT_NAME, all);

            generateOAuthSaveAccessTokenElement(all);
            generateOAuthRestoreAccessTokenElement(all);
        }
        if (type.hasProcessorMethodWithParameter(HttpCallback.class)) {
            generateHttpCallbackElement(SchemaConstants.HTTP_CALLBACK_CONFIG_ELEMENT_NAME, all);
        }

        if (type.isPoolable()) {
            //<xsd:element name="abstract-pooling-profile" abstract="true" type="abstractPoolingProfileType"/>

            TopLevelElement poolingProfile = new TopLevelElement();
            poolingProfile.setName(SchemaConstants.POOLING_PROFILE_ELEMENT);
            poolingProfile.setType(SchemaConstants.MULE_POOLING_PROFILE_TYPE);
            poolingProfile.setMinOccurs(BigInteger.valueOf(0L));

            Annotation annotation = new Annotation();
            Documentation doc = new Documentation();
            doc.getContent().add(SchemaConstants.POOLING_PROFILE_ELEMENT_DESCRIPTION);
            annotation.getAppinfoOrDocumentation().add(doc);

            poolingProfile.setAnnotation(annotation);

            all.getParticle().add(objectFactory.createElement(poolingProfile));
        }

        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(type.getJavaDocSummary());
        annotation.getAppinfoOrDocumentation().add(doc);
        config.setAnnotation(annotation);

        if (all.getParticle().size() == 0) {
            config.setSequence(null);
        }
    }

    private void generateOAuthSaveAccessTokenElement(ExplicitGroup all) {
        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(SchemaConstants.OAUTH_SAVE_ACCESS_TOKEN_ELEMENT);

        collectionElement.setMinOccurs(BigInteger.valueOf(0L));
        collectionElement.setMaxOccurs("1");

        LocalComplexType collectionComplexType = new LocalComplexType();
        GroupRef group = generateNestedProcessorGroup();

        collectionComplexType.setGroup(group);
        collectionElement.setComplexType(collectionComplexType);

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(SchemaConstants.OAUTH_SAVE_ACCESS_TOKEN_ELEMENT_DESCRIPTION);
        annotation.getAppinfoOrDocumentation().add(doc);

        collectionElement.setAnnotation(annotation);
    }

    private void generateOAuthRestoreAccessTokenElement(ExplicitGroup all) {
        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(SchemaConstants.OAUTH_RESTORE_ACCESS_TOKEN_ELEMENT);

        collectionElement.setMinOccurs(BigInteger.valueOf(0L));
        collectionElement.setMaxOccurs("1");

        LocalComplexType collectionComplexType = new LocalComplexType();
        GroupRef group = generateNestedProcessorGroup();

        collectionComplexType.setGroup(group);
        collectionElement.setComplexType(collectionComplexType);

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(SchemaConstants.OAUTH_RESTORE_ACCESS_TOKEN_ELEMENT_DESCRIPTION);
        annotation.getAppinfoOrDocumentation().add(doc);

        collectionElement.setAnnotation(annotation);
    }

    private void generateHttpCallbackElement(String elementName, ExplicitGroup all) {
        Attribute domainAttribute = new Attribute();
        domainAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        domainAttribute.setName(SchemaConstants.DOMAIN_ATTRIBUTE_NAME);
        domainAttribute.setType(SchemaConstants.STRING);
        domainAttribute.setDefault(SchemaConstants.DOMAIN_DEFAULT_VALUE);

        Attribute localPortAttribute = new Attribute();
        localPortAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        localPortAttribute.setName(SchemaConstants.LOCAL_PORT_ATTRIBUTE_NAME);
        localPortAttribute.setType(SchemaConstants.STRING);
        localPortAttribute.setDefault(SchemaConstants.PORT_DEFAULT_VALUE);

        Attribute remotePortAttribute = new Attribute();
        remotePortAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        remotePortAttribute.setName(SchemaConstants.REMOTE_PORT_ATTRIBUTE_NAME);
        remotePortAttribute.setType(SchemaConstants.STRING);
        remotePortAttribute.setDefault(SchemaConstants.PORT_DEFAULT_VALUE);

        Attribute asyncAttribute = new Attribute();
        asyncAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        asyncAttribute.setName(SchemaConstants.ASYNC_ATTRIBUTE_NAME);
        asyncAttribute.setType(SchemaConstants.BOOLEAN);
        asyncAttribute.setDefault(SchemaConstants.ASYNC_DEFAULT_VALUE);

        Attribute connectorRefAttribute = new Attribute();
        connectorRefAttribute.setUse(SchemaConstants.USE_OPTIONAL);
        connectorRefAttribute.setName("connector-ref");
        connectorRefAttribute.setType(SchemaConstants.STRING);

        TopLevelElement httpCallbackConfig = new TopLevelElement();
        httpCallbackConfig.setName(elementName);
        httpCallbackConfig.setMinOccurs(BigInteger.ZERO);
        httpCallbackConfig.setMaxOccurs("1");

        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add("Config for http callbacks.");
        annotation.getAppinfoOrDocumentation().add(doc);
        httpCallbackConfig.setAnnotation(annotation);

        ExtensionType extensionType = new ExtensionType();
        extensionType.setBase(SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE);
        extensionType.getAttributeOrAttributeGroup().add(localPortAttribute);
        extensionType.getAttributeOrAttributeGroup().add(remotePortAttribute);
        extensionType.getAttributeOrAttributeGroup().add(domainAttribute);
        extensionType.getAttributeOrAttributeGroup().add(asyncAttribute);
        extensionType.getAttributeOrAttributeGroup().add(connectorRefAttribute);

        ComplexContent complextContent = new ComplexContent();
        complextContent.setExtension(extensionType);

        LocalComplexType localComplexType = new LocalComplexType();
        localComplexType.setComplexContent(complextContent);

        httpCallbackConfig.setComplexType(localComplexType);
        all.getParticle().add(objectFactory.createElement(httpCallbackConfig));
    }

    private Attribute createObjectStoreRefAttribute(Variable variable) {
        Attribute attribute = new Attribute();

        // set whenever or not is optional
        attribute.setUse(SchemaConstants.USE_OPTIONAL);
        attribute.setName("objectStore-ref");
        attribute.setType(SchemaConstants.STRING);

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(variable.getJavaDocSummary());
        annotation.getAppinfoOrDocumentation().add(doc);

        attribute.setAnnotation(annotation);

        return attribute;
    }

    private Attribute createAttribute(Schema schema, Variable variable) {
        Named named = variable.getAnnotation(Named.class);
        Optional optional = variable.getAnnotation(Optional.class);
        Default def = variable.getAnnotation(Default.class);

        String name = variable.getSimpleName().toString();
        if (named != null && named.value().length() > 0) {
            name = named.value();
        }

        Attribute attribute = new Attribute();

        // set whenever or not is optional
        attribute.setUse(optional != null ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);

        if (isTypeSupported(variable.asType())) {
            attribute.setName(name);
            attribute.setType(SchemaTypeConversion.convertType(variable.asType().toString(), schema.getTargetNamespace()));
        } else if (variable.isEnum()) {
            attribute.setName(name);
            javax.lang.model.element.Element enumElement = ctx().getTypeUtils().asElement(variable.asType());
            attribute.setType(new QName(schema.getTargetNamespace(), enumElement.getSimpleName() + SchemaConstants.ENUM_TYPE_SUFFIX));
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + SchemaConstants.REF_SUFFIX);
            attribute.setType(SchemaConstants.STRING);
        }

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(variable.getJavaDocSummary());
        annotation.getAppinfoOrDocumentation().add(doc);

        attribute.setAnnotation(annotation);

        // add default value
        if (def != null && def.value().length() > 0) {
            attribute.setDefault(def.value());
        }
        return attribute;
    }

    private Attribute createParameterAttribute(Schema schema, Variable variable) {
        return createParameterAttribute(schema, variable, false);
    }

    private Attribute createParameterAttribute(Schema schema, Variable variable, boolean forceOptional) {
        Named named = variable.getAnnotation(Named.class);
        Optional optional = variable.getAnnotation(Optional.class);
        Default def = variable.getAnnotation(Default.class);

        String name = variable.getSimpleName().toString();
        if (named != null && named.value().length() > 0) {
            name = named.value();
        }

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
        } else if (variable.isEnum()) {
            attribute.setName(name);
            javax.lang.model.element.Element enumElement = ctx().getTypeUtils().asElement(variable.asType());
            attribute.setType(new QName(schema.getTargetNamespace(), enumElement.getSimpleName() + SchemaConstants.ENUM_TYPE_SUFFIX));
        } else if (variable.isHttpCallback()) {
            attribute.setName(NameUtils.uncamel(name) + SchemaConstants.FLOW_REF_SUFFIX);
            attribute.setType(SchemaConstants.STRING);
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + SchemaConstants.REF_SUFFIX);
            attribute.setType(SchemaConstants.STRING);
        }

        // add doc
        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(variable.parent().getJavaDocParameterSummary(variable.getSimpleName().toString()));
        annotation.getAppinfoOrDocumentation().add(doc);

        attribute.setAnnotation(annotation);

        // add default value
        if (def != null && def.value().length() > 0) {
            attribute.setDefault(def.value());
        }
        return attribute;
    }

    private boolean isTypeSupported(TypeMirror typeMirror) {
        return SchemaTypeConversion.isSupported(typeMirror.toString());
    }

    private void importMuleNamespace(Schema schema) {
        Import muleSchemaImport = new Import();
        muleSchemaImport.setNamespace(SchemaConstants.MULE_NAMESPACE);
        muleSchemaImport.setSchemaLocation(SchemaConstants.MULE_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    }

    private void importMuleDevKitNamespace(Schema schema) {
        Import muleSchemaImport = new Import();
        muleSchemaImport.setNamespace(SchemaConstants.MULE_DEVKIT_NAMESPACE);
        muleSchemaImport.setSchemaLocation(SchemaConstants.MULE_DEVKIT_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    }

    private void importSpringFrameworkNamespace(Schema schema) {
        Import springFrameworkImport = new Import();
        springFrameworkImport.setNamespace(SchemaConstants.SPRING_FRAMEWORK_NAMESPACE);
        springFrameworkImport.setSchemaLocation(SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(springFrameworkImport);
    }

    private void importXmlNamespace(Schema schema) {
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

    private ExtensionType registerExtension(Schema schema, String name, Map<QName, String> otherAttributes) {
        LocalComplexType complexType = new LocalComplexType();

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setSubstitutionGroup(SchemaConstants.MULE_ABSTRACT_EXTENSION);
        extension.setComplexType(complexType);

        extension.getOtherAttributes().putAll(otherAttributes);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);

        return complexContentExtension;
    }

    private void registerTypes(Schema schema) {
        registerType(schema, "integerType", SchemaConstants.INTEGER);
        registerType(schema, "decimalType", SchemaConstants.DECIMAL);
        registerType(schema, "floatType", SchemaConstants.FLOAT);
        registerType(schema, "doubleType", SchemaConstants.DOUBLE);
        registerType(schema, "dateTimeType", SchemaConstants.DATETIME);
        registerType(schema, "longType", SchemaConstants.LONG);
        registerType(schema, "byteType", SchemaConstants.BYTE);
        registerType(schema, "booleanType", SchemaConstants.BOOLEAN);
        registerType(schema, "anyUriType", SchemaConstants.ANYURI);
        registerType(schema, "charType", SchemaConstants.STRING, 1, 1);

        registerAnyXmlType(schema);
    }

    private void registerAnyXmlType(Schema schema) {
        TopLevelComplexType xmlComplexType = createAnyXmlType();
        schema.getSimpleTypeOrComplexTypeOrGroup().add(xmlComplexType);
    }

    private void registerType(Schema schema, String name, QName base) {
        registerType(schema, name, base, -1, -1);
    }

    private void registerType(Schema schema, String name, QName base, int minlen, int maxlen) {
        TopLevelSimpleType simpleType = new TopLevelSimpleType();
        simpleType.setName(name);
        Union union = new Union();
        simpleType.setUnion(union);

        union.getSimpleType().add(createSimpleType(base, minlen, maxlen));
        union.getSimpleType().add(createExpressionAndPropertyPlaceHolderSimpleType());

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

    private LocalSimpleType createExpressionAndPropertyPlaceHolderSimpleType() {
        LocalSimpleType expression = new LocalSimpleType();
        Restriction restriction = new Restriction();
        expression.setRestriction(restriction);
        restriction.setBase(SchemaConstants.STRING);
        Pattern pattern = new Pattern();
        pattern.setValue("(\\#\\[[^\\]]+\\]|\\$\\{[^\\}]+\\})");
        restriction.getFacets().add(pattern);

        return expression;
    }
}