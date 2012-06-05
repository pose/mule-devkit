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

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TypeVariable;
import org.mule.devkit.model.code.Variable;
import org.mule.util.TemplateParser;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

public class AbstractBeanDefinitionParserGenerator extends AbstractMessageGenerator {
    public static final String ROLE = "AbstractBeanDefinitionParser";
    public static final String DELEGATE_ROLE = "ParserDelegateInterface";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass abstractBeanDefinitionParserClass = getAbstractBeanDefinitionParserClass(typeElement);

        FieldVariable patternInfo = generateFieldForPatternInfo(abstractBeanDefinitionParserClass);

        Method constructor = abstractBeanDefinitionParserClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));

        DefinedClass parseDelegateInterface = generateParseDelegateInterface(abstractBeanDefinitionParserClass);
        generateHasAttributeMethod(abstractBeanDefinitionParserClass);
        generateSetRefMethod(abstractBeanDefinitionParserClass);
        generateIsMuleExpressionMethod(abstractBeanDefinitionParserClass, patternInfo);
        generateParseListMethod(abstractBeanDefinitionParserClass, parseDelegateInterface);
        generateParseListAndSetPropertyMethod(abstractBeanDefinitionParserClass, parseDelegateInterface);
        generateParseMapMethod(abstractBeanDefinitionParserClass, parseDelegateInterface);
        generateParseMapAndSetPropertyMethod(abstractBeanDefinitionParserClass, parseDelegateInterface);
        generateParseConfigRefMethod(abstractBeanDefinitionParserClass);
        generateAttachProcessorDefinitionMethod(abstractBeanDefinitionParserClass);
        generateAttachSourceDefinitionMethod(abstractBeanDefinitionParserClass);
        generateGetAttributeValueMethod(abstractBeanDefinitionParserClass);
        generateParseConfigNameMethod(abstractBeanDefinitionParserClass);
        generateSetInitMethodIfNeededMethod(abstractBeanDefinitionParserClass);
        generateSetDestroyMethodIfNeededMethod(abstractBeanDefinitionParserClass);
    }

    private DefinedClass getAbstractBeanDefinitionParserClass(DevKitTypeElement typeElement) {
        String abstractBeanDefinitionParserClass = context.getNameUtils().generateClassNameInPackage(typeElement, NamingContants.CONFIG_NAMESPACE, NamingContants.ABSTRACT_DEFINITION_PARSER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(abstractBeanDefinitionParserClass));
        DefinedClass clazz = pkg._class(Modifier.ABSTRACT, context.getNameUtils().getClassName(abstractBeanDefinitionParserClass), new Class[]{BeanDefinitionParser.class});

        context.setClassRole(ROLE, clazz);

        return clazz;
    }

    private DefinedClass generateParseDelegateInterface(DefinedClass beanDefinitionParserClass) {
        try {
            DefinedClass parseDelegate = beanDefinitionParserClass._interface(Modifier.PUBLIC, "ParseDelegate");
            TypeVariable typeVariable = parseDelegate.generify("T");

            Method parse = parseDelegate.method(Modifier.PUBLIC, typeVariable, "parse");
            parse.param(ref(Element.class), "element");

            context.setClassRole(DELEGATE_ROLE, parseDelegate);

            return parseDelegate;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    private void generateParseMapMethod(DefinedClass beanDefinitionParserClass, DefinedClass parserInterface) {
        Method parseMap = beanDefinitionParserClass.method(Modifier.PROTECTED, ref(ManagedMap.class), "parseMap");
        Variable element = parseMap.param(ref(Element.class), "element");
        Variable childElementName = parseMap.param(ref(String.class), "childElementName");
        Variable delegate = parseMap.param(parserInterface, "parserDelegate");

        Variable managedMap = parseMap.body().decl(ref(ManagedMap.class), "managedMap", ExpressionFactory._new(ref(ManagedMap.class)));
        Variable childDomElements = parseMap.body().decl(ref(List.class).narrow(ref(Element.class)), "childDomElements", ref(DomUtils.class).staticInvoke("getChildElementsByTagName").arg(element).arg(childElementName));

        Conditional ifChildDomElementsIsEmpty = parseMap.body()._if(Op.eq(childDomElements.invoke("size"), ExpressionFactory.lit(0)));
        ifChildDomElementsIsEmpty._then().assign(childDomElements, ref(DomUtils.class).staticInvoke("getChildElements").arg(element));

        ForEach forEachChildDomElement = parseMap.body().forEach(ref(Element.class), "childDomElement", childDomElements);

        Variable key = forEachChildDomElement.body().decl(ref(Object.class), "key", ExpressionFactory._null());
        Conditional ifChildDomElementHasKeyRef = forEachChildDomElement.body()._if(ExpressionFactory.invoke("hasAttribute").arg(forEachChildDomElement.var()).arg("key-ref"));
        ifChildDomElementHasKeyRef._then().assign(key, ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(forEachChildDomElement.var().invoke("getAttribute").arg("key-ref")));
        Conditional ifChildDomElementHasKey = ifChildDomElementHasKeyRef._else()._if(ExpressionFactory.invoke("hasAttribute").arg(forEachChildDomElement.var()).arg("key"));
        ifChildDomElementHasKey._then().assign(key, forEachChildDomElement.var().invoke("getAttribute").arg("key"));
        ifChildDomElementHasKey._else().assign(key, forEachChildDomElement.var().invoke("getTagName"));

        Conditional ifChildDomElementHasRef = forEachChildDomElement.body()._if(ExpressionFactory.invoke("hasAttribute").arg(forEachChildDomElement.var()).arg("value-ref"));
        Conditional ifChildDomElementHasRefAndIsAMuleExpression = ifChildDomElementHasRef._then()._if(Op.not(ExpressionFactory.invoke("isMuleExpression").arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref"))));
        ifChildDomElementHasRefAndIsAMuleExpression._then().add(managedMap.invoke("put").arg(key).arg(ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref"))));
        ifChildDomElementHasRefAndIsAMuleExpression._else().add(managedMap.invoke("put").arg(key).arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref")));
        ifChildDomElementHasRef._else().add(managedMap.invoke("put").arg(key).arg(delegate.invoke("parse").arg(forEachChildDomElement.var())));

        parseMap.body()._return(managedMap);
    }

    private void generateParseMapAndSetPropertyMethod(DefinedClass beanDefinitionParserClass, DefinedClass parserInterface) {
        Method parseMap = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseMapAndSetProperty");
        Variable element = parseMap.param(ref(Element.class), "element");
        Variable builder = parseMap.param(ref(BeanDefinitionBuilder.class), "builder");
        Variable fieldName = parseMap.param(ref(String.class), "fieldName");
        Variable parentElementName = parseMap.param(ref(String.class), "parentElementName");
        Variable childElementName = parseMap.param(ref(String.class), "childElementName");
        Variable delegate = parseMap.param(parserInterface, "parserDelegate");

        Variable domElement = parseMap.body().decl(ref(Element.class), "domElement", ref(DomUtils.class).staticInvoke("getChildElementByTagName").arg(element).arg(parentElementName));

        Conditional ifDomElementNotNull = parseMap.body()._if(Op.ne(domElement, ExpressionFactory._null()));
        Conditional ifHasRef = ifDomElementNotNull._then()._if(ExpressionFactory.invoke("hasAttribute").arg(domElement).arg("ref"));
        ifHasRef._then().invoke("setRef").arg(builder).arg(fieldName).arg(domElement.invoke("getAttribute").arg("ref"));

        Variable managedMap = ifHasRef._else().decl(ref(ManagedMap.class), "managedMap", ExpressionFactory.invoke("parseMap").arg(domElement).arg(childElementName).arg(delegate));

        ifHasRef._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap));
    }

    private void generateParseListMethod(DefinedClass beanDefinitionParserClass, DefinedClass parserInterface) {
        Method parseList = beanDefinitionParserClass.method(Modifier.PROTECTED, ref(ManagedList.class), "parseList");
        Variable element = parseList.param(ref(Element.class), "element");
        Variable childElementName = parseList.param(ref(String.class), "childElementName");
        Variable delegate = parseList.param(parserInterface, "parserDelegate");

        Variable managedList = parseList.body().decl(ref(ManagedList.class), "managedList", ExpressionFactory._new(ref(ManagedList.class)));
        Variable childDomElements = parseList.body().decl(ref(List.class).narrow(ref(Element.class)), "childDomElements", ref(DomUtils.class).staticInvoke("getChildElementsByTagName").arg(element).arg(childElementName));

        ForEach forEachChildDomElement = parseList.body().forEach(ref(Element.class), "childDomElement", childDomElements);

        Conditional ifChildDomElementHasRef = forEachChildDomElement.body()._if(ExpressionFactory.invoke("hasAttribute").arg(forEachChildDomElement.var()).arg("value-ref"));
        Conditional ifChildDomElementHasRefAndIsAMuleExpression = ifChildDomElementHasRef._then()._if(Op.not(ExpressionFactory.invoke("isMuleExpression").arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref"))));
        ifChildDomElementHasRefAndIsAMuleExpression._then().add(managedList.invoke("add").arg(ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref"))));
        ifChildDomElementHasRefAndIsAMuleExpression._else().add(managedList.invoke("add").arg(forEachChildDomElement.var().invoke("getAttribute").arg("value-ref")));
        ifChildDomElementHasRef._else().add(managedList.invoke("add").arg(delegate.invoke("parse").arg(forEachChildDomElement.var())));

        parseList.body()._return(managedList);
    }

    private void generateParseListAndSetPropertyMethod(DefinedClass beanDefinitionParserClass, DefinedClass parserInterface) {
        Method parseList = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseListAndSetProperty");
        Variable element = parseList.param(ref(Element.class), "element");
        Variable builder = parseList.param(ref(BeanDefinitionBuilder.class), "builder");
        Variable fieldName = parseList.param(ref(String.class), "fieldName");
        Variable parentElementName = parseList.param(ref(String.class), "parentElementName");
        Variable childElementName = parseList.param(ref(String.class), "childElementName");
        Variable delegate = parseList.param(parserInterface, "parserDelegate");

        Variable domElement = parseList.body().decl(ref(Element.class), "domElement", ref(DomUtils.class).staticInvoke("getChildElementByTagName").arg(element).arg(parentElementName));

        Conditional ifDomElementNotNull = parseList.body()._if(Op.ne(domElement, ExpressionFactory._null()));
        Conditional ifHasRef = ifDomElementNotNull._then()._if(ExpressionFactory.invoke("hasAttribute").arg(domElement).arg("ref"));
        ifHasRef._then().invoke("setRef").arg(builder).arg(fieldName).arg(domElement.invoke("getAttribute").arg("ref"));

        Variable managedList = ifHasRef._else().decl(ref(ManagedList.class), "managedList", ExpressionFactory.invoke("parseList").arg(domElement).arg(childElementName).arg(delegate));

        ifHasRef._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList));
    }

    private void generateParseConfigRefMethod(DefinedClass beanDefinitionParserClass) {
        Method parseConfigRef = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseConfigRef");
        Variable element = parseConfigRef.param(ref(Element.class), "element");
        Variable builder = parseConfigRef.param(ref(BeanDefinitionBuilder.class), "builder");

        Conditional ifConfigRef = parseConfigRef.body()._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg("config-ref"));
        ifConfigRef._then().add(builder.invoke("addPropertyValue").arg("moduleObject").arg(
                element.invoke("getAttribute").arg("config-ref")));
    }

    private void generateAttachSourceDefinitionMethod(DefinedClass beanDefinitionParserClass) {
        Method attachDefinition = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "attachSourceDefinition");
        Variable parserContext = attachDefinition.param(ref(ParserContext.class), "parserContext");
        Variable definition = attachDefinition.param(ref(BeanDefinition.class), "definition");

        Variable propertyValues = attachDefinition.body().decl(ref(MutablePropertyValues.class), "propertyValues",
                parserContext.invoke("getContainingBeanDefinition").invoke("getPropertyValues"));

        attachDefinition.body().add(propertyValues.invoke("addPropertyValue").arg("messageSource").arg(
                definition
        ));
    }


    private void generateAttachProcessorDefinitionMethod(DefinedClass beanDefinitionParserClass) {
        Method attachDefinition = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "attachProcessorDefinition");
        Variable parserContext = attachDefinition.param(ref(ParserContext.class), "parserContext");
        Variable definition = attachDefinition.param(ref(BeanDefinition.class), "definition");

        Variable propertyValues = attachDefinition.body().decl(ref(MutablePropertyValues.class), "propertyValues",
                parserContext.invoke("getContainingBeanDefinition").invoke("getPropertyValues"));

        Conditional ifIsPoll = attachDefinition.body()._if(parserContext.invoke("getContainingBeanDefinition").invoke("getBeanClassName")
                .invoke("equals").arg("org.mule.config.spring.factories.PollingMessageSourceFactoryBean"));

        ifIsPoll._then().add(propertyValues.invoke("addPropertyValue").arg("messageProcessor").arg(definition));

        Conditional ifIsEnricher = ifIsPoll._else()._if(parserContext.invoke("getContainingBeanDefinition").invoke("getBeanClassName")
                .invoke("equals").arg("org.mule.enricher.MessageEnricher"));

        ifIsEnricher._then().add(propertyValues.invoke("addPropertyValue").arg("enrichmentMessageProcessor").arg(definition));

        Variable messageProcessors = ifIsEnricher._else().decl(ref(PropertyValue.class), "messageProcessors",
                propertyValues.invoke("getPropertyValue").arg("messageProcessors"));
        Conditional noList = ifIsEnricher._else()._if(Op.cor(Op.eq(messageProcessors, ExpressionFactory._null()), Op.eq(messageProcessors.invoke("getValue"),
                ExpressionFactory._null())));
        noList._then().add(propertyValues.invoke("addPropertyValue").arg("messageProcessors").arg(ExpressionFactory._new(ref(ManagedList.class))));
        Variable listMessageProcessors = ifIsEnricher._else().decl(ref(List.class), "listMessageProcessors",
                ExpressionFactory.cast(ref(List.class), propertyValues.invoke("getPropertyValue").arg("messageProcessors").invoke("getValue")));
        ifIsEnricher._else().add(listMessageProcessors.invoke("add").arg(
                definition
        ));
    }

    private void generateHasAttributeMethod(DefinedClass beanDefinitionParserClass) {
        Method hasAttribute = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().BOOLEAN, "hasAttribute");
        Variable element = hasAttribute.param(ref(Element.class), "element");
        Variable attributeName = hasAttribute.param(ref(String.class), "attributeName");
        Variable value = hasAttribute.body().decl(ref(String.class), "value", element.invoke("getAttribute").arg(attributeName));

        Conditional isNotNullAndNotEmpty = hasAttribute.body()._if(Op.cand(Op.ne(value, ExpressionFactory._null()), Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(value))));
        isNotNullAndNotEmpty._then()._return(ExpressionFactory.TRUE);

        hasAttribute.body()._return(ExpressionFactory.FALSE);
    }

    private void generateSetRefMethod(DefinedClass beanDefinitionParserClass) {
        Method setRef = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "setRef");
        Variable builder = setRef.param(ref(BeanDefinitionBuilder.class), "builder");
        Variable propertyName = setRef.param(ref(String.class), "propertyName");
        Variable ref = setRef.param(ref(String.class), "ref");

        Conditional ifRefNotExpresion = setRef.body()._if(Op.not(ExpressionFactory.invoke("isMuleExpression").arg(ref)));

        ifRefNotExpresion._then().add(builder.invoke("addPropertyValue").arg(propertyName).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(ref)
        ));

        ifRefNotExpresion._else().add(builder.invoke("addPropertyValue").arg(propertyName).arg(
                ref));
    }

    private void generateIsMuleExpressionMethod(DefinedClass beanDefinitionParserClass, FieldVariable patternInfo) {
        Method isMuleExpression = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().BOOLEAN, "isMuleExpression");
        Variable value = isMuleExpression.param(ref(String.class), "value");

        Conditional ifRefNotExpresion = isMuleExpression.body()._if(Op.cand(
                Op.not(value.invoke("startsWith").arg(patternInfo.invoke("getPrefix"))),
                Op.not(value.invoke("endsWith").arg(patternInfo.invoke("getSuffix")))
        ));

        ifRefNotExpresion._then()._return(ExpressionFactory.FALSE);

        ifRefNotExpresion._else()._return(ExpressionFactory.TRUE);
    }

    protected void generateGetAttributeValueMethod(DefinedClass beanDefinitionParserClass) {
        Method getAttributeValue = beanDefinitionParserClass.method(Modifier.PROTECTED, ref(String.class), "getAttributeValue");
        Variable element = getAttributeValue.param(ref(org.w3c.dom.Element.class), "element");
        Variable attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        Invocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        Invocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        Block ifIsEmpty = getAttributeValue.body()._if(isEmpty.not())._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(ExpressionFactory._null());
    }

    private void generateParseConfigNameMethod(DefinedClass beanDefinitionParserClass) {
        Method parseConfigName = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseConfigName");
        Variable element = parseConfigName.param(ref(org.w3c.dom.Element.class), "element");

        Conditional ifNotNamed = parseConfigName.body()._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg("name"));

        ifNotNamed._then().add(element.invoke("setAttribute")
                .arg("name")
                .arg(ref(AutoIdUtils.class).staticInvoke("getUniqueName").arg(element).arg("mule-bean")));
    }

    private void generateSetInitMethodIfNeededMethod(DefinedClass beanDefinitionParserClass) {
        Method setInitMethodIfNeeded = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "setInitMethodIfNeeded");
        Variable builder = setInitMethodIfNeeded.param(ref(BeanDefinitionBuilder.class), "builder");
        Variable clazz = setInitMethodIfNeeded.param(ref(Class.class), "clazz");

        Conditional isInitialisable = setInitMethodIfNeeded.body()._if(ref(Initialisable.class).dotclass()
                .invoke("isAssignableFrom").arg(clazz));
        isInitialisable._then().add(builder.invoke("setInitMethodName").arg(ref(Initialisable.class).staticRef("PHASE_NAME")));
    }

    private void generateSetDestroyMethodIfNeededMethod(DefinedClass beanDefinitionParserClass) {
        Method setDestroyMethodIfNeeded = beanDefinitionParserClass.method(Modifier.PROTECTED, context.getCodeModel().VOID, "setDestroyMethodIfNeeded");
        Variable builder = setDestroyMethodIfNeeded.param(ref(BeanDefinitionBuilder.class), "builder");
        Variable clazz = setDestroyMethodIfNeeded.param(ref(Class.class), "clazz");

        Conditional isDisposable = setDestroyMethodIfNeeded.body()._if(ref(Disposable.class).dotclass()
                .invoke("isAssignableFrom").arg(clazz));
        isDisposable._then().add(builder.invoke("setDestroyMethodName").arg(ref(Disposable.class).staticRef("PHASE_NAME")));
    }

}
