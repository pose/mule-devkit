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

import org.apache.commons.lang.UnhandledException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.config.PoolingProfile;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.adapter.HttpCallbackAdapterGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.model.DevKitElement;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitFieldElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class BeanDefinitionParserGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(DevKitTypeElement typeElement) {
        generateConfigBeanDefinitionParserFor(typeElement);

        for (DevKitExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            generateBeanDefinitionParserForProcessor(executableElement);
        }

        for (DevKitExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Source.class)) {
            generateBeanDefinitionParserForSource(executableElement);
        }
    }

    private void generateConfigBeanDefinitionParserFor(DevKitTypeElement typeElement) {
        DefinedClass beanDefinitionparser = getConfigBeanDefinitionParserClass(typeElement);
        DefinedClass pojo = ctx().getClassForRole(ctx().getNameUtils().generateModuleObjectRoleKey(typeElement));

        ctx().note("Generating config element definition parser as " + beanDefinitionparser.fullName() + " for class " + typeElement.getSimpleName().toString());

        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContext");

        parse.body().invoke("parseConfigName").arg(element);

        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(pojo.dotclass().invoke("getName")));

        parse.body().invoke("setInitMethodIfNeeded").arg(builder).arg(pojo.dotclass());
        parse.body().invoke("setDestroyMethodIfNeeded").arg(builder).arg(pojo.dotclass());

        for (DevKitFieldElement variable : typeElement.getFieldsAnnotatedWith(Configurable.class)) {

            String fieldName = variable.getSimpleName().toString();

            if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseProperty(parse.body(), element, builder, fieldName);
            } else if (variable.isXmlType()) {
                generateParseXmlType(parse.body(), element, builder, fieldName);
            } else if (variable.isArrayOrList()) {
                generateParseList(parse, element, builder, variable, fieldName);
            } else if (variable.isMap()) {
                generateParseMap(parse, element, builder, variable, fieldName);
            } else if (variable.isEnum()) {
                generateParseProperty(parse.body(), element, builder, fieldName);
            } else {
                // not supported use the -ref approach
                Conditional ifNotNull = parse.body()._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg(fieldName + "-ref"));
                ifNotNull._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(element.invoke("getAttribute").arg(fieldName + "-ref"))
                ));
            }
        }

        for (DevKitFieldElement variable : typeElement.getFieldsAnnotatedWith(Inject.class)) {
            if (variable.asType().toString().equals("org.mule.api.store.ObjectStore")) {
                Conditional ifNotNull = parse.body()._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg("objectStore-ref"));
                ifNotNull._then().add(builder.invoke("addPropertyValue").arg(variable.getSimpleName().toString()).arg(
                        ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(element.invoke("getAttribute").arg("objectStore-ref"))
                ));
            }
        }

        DevKitExecutableElement connect = connectMethodForClass(typeElement);
        if (connect != null) {
            for (DevKitParameterElement variable : connect.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                    generateParseProperty(parse.body(), element, builder, fieldName);
                } else if (variable.isArrayOrList()) {
                    generateParseList(parse, element, builder, variable, fieldName);
                } else if (variable.isMap()) {
                    generateParseMap(parse, element, builder, variable, fieldName);
                } else if (variable.isEnum()) {
                    generateParseProperty(parse.body(), element, builder, fieldName);
                }
            }
        }

        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            generateParseHttpCallback(SchemaGenerator.OAUTH_CALLBACK_CONFIG_ELEMENT_NAME, parse, element, builder);

            DefinedClass saveAccessTokenCallbackFactory = ctx().getClassForRole(DefaultSaveAccessTokenCallbackFactoryGenerator.ROLE);
            DefinedClass restoreAccessTokenCallbackFactory = ctx().getClassForRole(DefaultRestoreAccessTokenCallbackFactoryGenerator.ROLE);
            generateParseNestedProcessor(parse.body(), element, parserContext, builder, "oauthSaveAccessToken", false, false, false, saveAccessTokenCallbackFactory);
            generateParseNestedProcessor(parse.body(), element, parserContext, builder, "oauthRestoreAccessToken", false, false, false, restoreAccessTokenCallbackFactory);
        }
        if (typeElement.hasProcessorMethodWithParameter(HttpCallback.class)) {
            generateParseHttpCallback(SchemaGenerator.HTTP_CALLBACK_CONFIG_ELEMENT_NAME, parse, element, builder);
        }

        if (connect != null) {
            generateParsePoolingProfile("connection-pooling-profile", "connectionPoolingProfile", parse, element, builder);
        }

        if (typeElement.isPoolable()) {
            generateParsePoolingProfile("pooling-profile", "poolingProfile", parse, element, builder);
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().invoke("setNoRecurseOnDefinition").arg(definition);

        parse.body()._return(definition);

    }

    private void generateParseHttpCallback(String elementName, Method parse, Variable element, Variable builder) {
        Variable httpCallbackConfigElement = parse.body().decl(ref(org.w3c.dom.Element.class), "httpCallbackConfigElement", ref(DomUtils.class).staticInvoke("getChildElementByTagName").
                arg(element).arg(elementName));
        Block ifHttpCallbackConfigPresent = parse.body()._if(Op.ne(httpCallbackConfigElement, ExpressionFactory._null()))._then();
        generateParseProperty(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.DOMAIN_FIELD_NAME);
        generateParseProperty(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.LOCAL_PORT_FIELD_NAME);
        generateParseProperty(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.REMOTE_PORT_FIELD_NAME);
        generateParseProperty(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.ASYNC_FIELD_NAME);

        Conditional ifNotNull = ifHttpCallbackConfigPresent._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME + "-ref"));
        ifNotNull._then().add(builder.invoke("addPropertyValue").arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(httpCallbackConfigElement.invoke("getAttribute").arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME + "-ref"))
        ));
    }

    private void generateParsePoolingProfile(String elementName, String propertyName, Method parse, Variable element, Variable builder) {
        Variable poolingProfileBuilder = parse.body().decl(ref(BeanDefinitionBuilder.class), propertyName + "Builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(ref(PoolingProfile.class).dotclass().invoke("getName")));

        Variable poolingProfileElement = parse.body().decl(ref(org.w3c.dom.Element.class), propertyName + "Element",
                ref(DomUtils.class).staticInvoke("getChildElementByTagName").arg(element).arg(elementName));

        Conditional ifElementNotNull = parse.body()._if(Op.ne(poolingProfileElement, ExpressionFactory._null()));

        generateParseProperty(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxActive");
        generateParseProperty(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxIdle");
        generateParseProperty(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxWait");

        Conditional ifNotNull = ifElementNotNull._then()._if(ExpressionFactory.invoke("hasAttribute").arg(poolingProfileElement).arg("exhaustedAction"));
        ifNotNull._then().add(poolingProfileBuilder.invoke("addPropertyValue").arg("exhaustedAction").arg(
                ref(PoolingProfile.class).staticRef("POOL_EXHAUSTED_ACTIONS").invoke("get").arg(poolingProfileElement.invoke("getAttribute").arg("exhaustedAction"))
        ));

        ifNotNull = ifElementNotNull._then()._if(ExpressionFactory.invoke("hasAttribute").arg(poolingProfileElement).arg("initialisationPolicy"));
        ifNotNull._then().add(poolingProfileBuilder.invoke("addPropertyValue").arg("initialisationPolicy").arg(
                ref(PoolingProfile.class).staticRef("POOL_INITIALISATION_POLICIES").invoke("get").arg(poolingProfileElement.invoke("getAttribute").arg("initialisationPolicy"))
        ));

        ifElementNotNull._then().add(builder.invoke("addPropertyValue").arg(propertyName).arg(
                poolingProfileBuilder.invoke("getBeanDefinition")
        ));
    }

    private void generateBeanDefinitionParserForSource(DevKitExecutableElement executableElement) {
        // get class
        Source sourceAnnotation = executableElement.getAnnotation(Source.class);
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement, sourceAnnotation.threadingModel() == SourceThreadingModel.SINGLE_THREAD);

        ctx().note("Generating bean definition parser as " + beanDefinitionparser.fullName() + " for message source " + messageSourceClass.fullName());

        generateSourceParseMethod(beanDefinitionparser, messageSourceClass, executableElement);
    }

    private void generateBeanDefinitionParserForProcessor(DevKitExecutableElement executableElement) {
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageProcessorClass;

        if (executableElement.getAnnotation(Processor.class).intercepting()) {
            messageProcessorClass = getInterceptingMessageProcessorClass(executableElement);
        } else {
            messageProcessorClass = getMessageProcessorClass(executableElement);
        }

        ctx().note("Generating bean definition parser as " + beanDefinitionparser.fullName() + " for message processor " + messageProcessorClass.fullName());

        generateProcessorParseMethod(beanDefinitionparser, messageProcessorClass, executableElement);
    }

    private void generateProcessorParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, DevKitExecutableElement executableElement) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContext");

        Variable definition = generateParseCommon(beanDefinitionparser, messageProcessorClass, executableElement, parse, element, parserContext);

        parse.body().invoke("attachProcessorDefinition").arg(parserContext).arg(definition);

        parse.body()._return(definition);
    }

    private void generateSourceParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, DevKitExecutableElement executableElement) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContext");

        Variable definition = generateParseCommon(beanDefinitionparser, messageProcessorClass, executableElement, parse, element, parserContext);

        parse.body().invoke("attachSourceDefinition").arg(parserContext).arg(definition);

        parse.body()._return(definition);
    }

    private Variable generateParseCommon(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, DevKitExecutableElement executableElement, Method parse, Variable element, Variable parserContext) {
        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(messageProcessorClass.dotclass().invoke("getName")));

        parse.body().invoke("parseConfigRef").arg(element).arg(builder);


        for (DevKitParameterElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            if (variable.isNestedProcessor()) {
                boolean isList = variable.isArrayOrList();
                if (executableElement.hasOnlyOneChildElement()) {
                    generateParseNestedProcessor(parse.body(), element, parserContext, builder, fieldName, true, isList, true, ref(MessageProcessorChainFactoryBean.class));
                } else {
                    generateParseNestedProcessor(parse.body(), element, parserContext, builder, fieldName, false, isList, true, ref(MessageProcessorChainFactoryBean.class));
                }
            } else if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseProperty(parse.body(), element, builder, fieldName);
            } else if (variable.isXmlType()) {
                generateParseXmlType(parse.body(), element, builder, fieldName);
            } else if (variable.isArrayOrList()) {
                generateParseList(parse, element, builder, variable, fieldName);
            } else if (variable.isMap()) {
                generateParseMap(parse, element, builder, variable, fieldName);
            } else if (variable.isEnum()) {
                generateParseProperty(parse.body(), element, builder, fieldName);
            } else if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                Variable callbackFlowName = parse.body().decl(ref(String.class), fieldName + "CallbackFlowName", ExpressionFactory.invoke("getAttributeValue").arg(element).arg(ctx().getNameUtils().uncamel(fieldName) + "-flow-ref"));
                Block block = parse.body()._if(Op.ne(callbackFlowName, ExpressionFactory._null()))._then();
                block.invoke(builder, "addPropertyValue").arg(fieldName + "CallbackFlow").arg(ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(callbackFlowName));
            } else {
                // not supported use the -ref approach
                Conditional ifNotNull = parse.body()._if(ExpressionFactory.invoke("hasAttribute").arg(element).arg(fieldName + "-ref"));
                Conditional ifNotExpression = ifNotNull._then()._if(element.invoke("getAttribute").arg(fieldName + "-ref").invoke("startsWith").arg("#"));

                ifNotExpression._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        Op.plus(Op.plus(ExpressionFactory.lit("#[registry:"),
                                element.invoke("getAttribute").arg(fieldName + "-ref")),
                                ExpressionFactory.lit("]"))
                ));

                ifNotExpression._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        element.invoke("getAttribute").arg(fieldName + "-ref")
                ));
            }
        }

        DevKitExecutableElement connectMethod = connectForMethod(executableElement);
        if (connectMethod != null) {
            generateParseProperty(parse.body(), element, builder, "retryMax");

            for (DevKitParameterElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                    generateParseProperty(parse.body(), element, builder, fieldName);
                } else if (variable.isArrayOrList()) {
                    generateParseList(parse, element, builder, variable, fieldName);
                } else if (variable.isMap()) {
                    generateParseMap(parse, element, builder, variable, fieldName);
                } else if (variable.isEnum()) {
                    generateParseProperty(parse.body(), element, builder, fieldName);
                }
            }
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().invoke("setNoRecurseOnDefinition").arg(definition);

        return definition;
    }

    private void generateParseList(Method parse, Variable element, Variable builder, DevKitElement variable, String fieldName) {
        Invocation parseListAndSetProperty = parse.body().invoke("parseListAndSetProperty")
                .arg(element)
                .arg(builder)
                .arg(fieldName)
                .arg(ctx().getNameUtils().uncamel(fieldName))
                .arg(ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName)));

        if (variable.hasTypeArguments()) {
            DevKitElement typeArgument = (DevKitElement) variable.getTypeArguments().get(0);

            if (typeArgument.isArrayOrList()) {
                String innerChildElementName = "inner-" + ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName));
                parseListAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForList(innerChildElementName)));
            } else if (typeArgument.isMap()) {
                String innerChildElementName = "inner-" + ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName));
                parseListAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForMap(innerChildElementName)));
            } else {
                parseListAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForTextContent()));
            }
        } else {
            parseListAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForTextContent()));
        }
    }

    private void generateParseMap(Method parse, Variable element, Variable builder, DevKitElement variable, String fieldName) {
        Invocation parseMapAndSetProperty = parse.body().invoke("parseMapAndSetProperty")
                .arg(element)
                .arg(builder)
                .arg(fieldName)
                .arg(ctx().getNameUtils().uncamel(fieldName))
                .arg(ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName)));

        if (variable.hasTypeArguments()) {
            DevKitElement typeArgument = (DevKitElement) variable.getTypeArguments().get(0);

            if (typeArgument.isArrayOrList()) {
                String innerChildElementName = "inner-" + ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName));
                parseMapAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForList(innerChildElementName)));
            } else if (typeArgument.isMap()) {
                String innerChildElementName = "inner-" + ctx().getNameUtils().uncamel(ctx().getNameUtils().singularize(fieldName));
                parseMapAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForMap(innerChildElementName)));
            } else {
                parseMapAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForTextContent()));
            }
        } else {
            parseMapAndSetProperty.arg(ExpressionFactory._new(generateParserDelegateForTextContent()));
        }
    }

    private void generateParseNestedProcessor(Block block, Variable element, Variable parserContext, Variable builder, String fieldName, boolean skipElement, boolean isList, boolean allowTextAttribute, TypeReference factoryBean) {
        if (skipElement) {
            block.invoke(isList ? "parseNestedProcessorAsListAndSetProperty" : "parseNestedProcessorAndSetProperty")
                    .arg(element)
                    .arg(parserContext)
                    .arg(factoryBean.dotclass())
                    .arg(builder)
                    .arg(fieldName);
        } else {
            block.invoke(isList ? "parseNestedProcessorAsListAndSetProperty" : "parseNestedProcessorAndSetProperty")
                    .arg(element)
                    .arg(ctx().getNameUtils().uncamel(fieldName))
                    .arg(parserContext)
                    .arg(factoryBean.dotclass())
                    .arg(builder)
                    .arg(fieldName);
        }
    }

    private void generateParseXmlType(Block block, Variable element, Variable builder, String fieldName) {
        Variable xmlElement = block.decl(ref(org.w3c.dom.Element.class),
                fieldName + "xmlElement",
                ExpressionFactory._null());

        block.assign(xmlElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                .arg(element)
                .arg(ctx().getNameUtils().uncamel(fieldName)));

        Conditional xmlElementNotNull = block._if(Op.ne(xmlElement, ExpressionFactory._null()));

        TryStatement tryBlock = xmlElementNotNull._then()._try();

        Variable xmlElementChilds = tryBlock.body().decl(ref(List.class).narrow(org.w3c.dom.Element.class), "xmlElementChilds",
                ref(DomUtils.class).staticInvoke("getChildElements").arg(xmlElement));

        Conditional xmlElementChildsNotEmpty = tryBlock.body()._if(Op.gt(xmlElementChilds.invoke("size"), ExpressionFactory.lit(0)));

        Variable domSource = xmlElementChildsNotEmpty._then().decl(ref(DOMSource.class), "domSource", ExpressionFactory._new(ref(DOMSource.class)).arg(
                xmlElementChilds.invoke("get").arg(ExpressionFactory.lit(0))));
        Variable stringWriter = xmlElementChildsNotEmpty._then().decl(ref(StringWriter.class), "stringWriter", ExpressionFactory._new(ref(StringWriter.class)));
        Variable streamResult = xmlElementChildsNotEmpty._then().decl(ref(StreamResult.class), "result", ExpressionFactory._new(ref(StreamResult.class)).arg(stringWriter));
        Variable tf = xmlElementChildsNotEmpty._then().decl(ref(TransformerFactory.class), "tf", ref(TransformerFactory.class).staticInvoke("newInstance"));
        Variable transformer = xmlElementChildsNotEmpty._then().decl(ref(Transformer.class), "transformer", tf.invoke("newTransformer"));
        Invocation transform = transformer.invoke("transform");
        transform.arg(domSource);
        transform.arg(streamResult);
        xmlElementChildsNotEmpty._then().add(transform);
        xmlElementChildsNotEmpty._then().add(stringWriter.invoke("flush"));

        xmlElementChildsNotEmpty._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                stringWriter.invoke("toString")));

        generateReThrow(tryBlock, TransformerConfigurationException.class);
        generateReThrow(tryBlock, TransformerException.class);
        generateReThrow(tryBlock, TransformerFactoryConfigurationError.class);
    }

    private void generateReThrow(TryStatement tryBlock, Class<?> clazz) {
        CatchBlock catchBlock = tryBlock._catch(ref(clazz).boxify());
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(UnhandledException.class)).arg(e));
    }

    private void generateParseProperty(Block block, Variable element, Variable builder, String fieldName) {
        block.invoke("parseProperty").arg(builder).arg(element).arg(fieldName);
    }

    private DefinedClass generateParserDelegateForTextContent() {
        DefinedClass parserDelegateInterface = ctx().getClassForRole(AbstractBeanDefinitionParserGenerator.DELEGATE_ROLE);
        DefinedClass anonymousClass = ctx().getCodeModel().anonymousClass(parserDelegateInterface.narrow(ref(String.class)));
        Method parseMethod = anonymousClass.method(Modifier.PUBLIC, ref(String.class), "parse");
        Variable element = parseMethod.param(ref(Element.class), "element");

        parseMethod.body()._return(element.invoke("getTextContent"));

        return anonymousClass;
    }

    private DefinedClass generateParserDelegateForList(String childElementName) {
        DefinedClass parserDelegateInterface = ctx().getClassForRole(AbstractBeanDefinitionParserGenerator.DELEGATE_ROLE);
        DefinedClass anonymousClass = ctx().getCodeModel().anonymousClass(parserDelegateInterface.narrow(ref(List.class)));
        Method parseMethod = anonymousClass.method(Modifier.PUBLIC, ref(List.class), "parse");
        Variable element = parseMethod.param(ref(Element.class), "element");

        parseMethod.body()._return(ExpressionFactory.invoke("parseList")
                .arg(element).arg(childElementName).arg(ExpressionFactory._new(generateParserDelegateForTextContent())
                ));

        return anonymousClass;
    }

    private DefinedClass generateParserDelegateForMap(String childElementName) {
        DefinedClass parserDelegateInterface = ctx().getClassForRole(AbstractBeanDefinitionParserGenerator.DELEGATE_ROLE);
        DefinedClass anonymousClass = ctx().getCodeModel().anonymousClass(parserDelegateInterface.narrow(ref(Map.class)));
        Method parseMethod = anonymousClass.method(Modifier.PUBLIC, ref(Map.class), "parse");
        Variable element = parseMethod.param(ref(Element.class), "element");

        parseMethod.body()._return(ExpressionFactory.invoke("parseMap")
                .arg(element).arg(childElementName).arg(ExpressionFactory._new(generateParserDelegateForTextContent())
                ));

        return anonymousClass;
    }
}
