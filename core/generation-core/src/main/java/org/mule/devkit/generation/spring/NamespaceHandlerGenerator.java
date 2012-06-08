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

import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.schema.SchemaConstants;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class NamespaceHandlerGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) {
        DefinedClass namespaceHandlerClass = getNamespaceHandlerClass(type);

        org.mule.devkit.model.code.Method init = namespaceHandlerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "init");
        init.javadoc().add("Invoked by the {@link DefaultBeanDefinitionDocumentReader} after construction but before any custom elements are parsed. \n@see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)");

        registerConfig(init, type);
        registerBeanDefinitionParserForEachProcessor(type, init);
        registerBeanDefinitionParserForEachSource(type, init);
        registerBeanDefinitionParserForEachTransformer(type, init);
    }

    private DefinedClass getNamespaceHandlerClass(Type type) {
        Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.CONFIG_NAMESPACE);
        DefinedClass clazz = pkg._class(type.getClassName() + NamingConstants.NAMESPACE_HANDLER_CLASS_NAME_SUFFIX, NamespaceHandlerSupport.class);

        String targetNamespace = type.getXmlNamespace();
        if (targetNamespace == null || targetNamespace.length() == 0) {
            targetNamespace = SchemaConstants.BASE_NAMESPACE + type.getModuleName();
        }
        clazz.javadoc().add("Registers bean definitions parsers for handling elements in <code>" + targetNamespace + "</code>.");

        clazz.role(DefinedClassRoles.NAMESPACE_HANDLER, ref(type));

        return clazz;
    }

    private void registerConfig(org.mule.devkit.model.code.Method init, Type pojo) {
        DefinedClass configBeanDefinitionParser = ctx().getCodeModel()._class(DefinedClassRoles.CONFIG_BEAN_DEFINITION_PARSER, ref(pojo));
        init.body().invoke("registerBeanDefinitionParser").arg("config").arg(ExpressionFactory._new(configBeanDefinitionParser));
    }

    private void registerBeanDefinitionParserForEachProcessor(Type type, org.mule.devkit.model.code.Method init) {
        if (type.hasAnnotation(OAuth.class) || type.hasAnnotation(OAuth2.class)) {
            DefinedClass authorizeMessageProcessorClass = ctx().getCodeModel()._class(DefinedClassRoles.AUTHORIZE_BEAN_DEFINITION_PARSER);
            init.body().invoke("registerBeanDefinitionParser").arg(ExpressionFactory.lit("authorize")).arg(ExpressionFactory._new(authorizeMessageProcessorClass));
        }
        for (Method executableElement : type.getMethodsAnnotatedWith(Processor.class)) {
            registerBeanDefinitionParserForProcessor(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachSource(Type type, org.mule.devkit.model.code.Method init) {
        for (Method executableElement : type.getMethodsAnnotatedWith(Source.class)) {
            registerBeanDefinitionParserForSource(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachTransformer(Type type, org.mule.devkit.model.code.Method init) {
        for (Method executableElement : type.getMethodsAnnotatedWith(Transformer.class)) {
            Invocation registerMuleBeanDefinitionParser = init.body().invoke("registerBeanDefinitionParser");
            registerMuleBeanDefinitionParser.arg(ExpressionFactory.lit(NameUtils.uncamel(executableElement.getSimpleName().toString())));
            String transformerClassName = type.getPackageName() + NamingConstants.TRANSFORMERS_NAMESPACE + "." + executableElement.getCapitalizedName() + NamingConstants.TRANSFORMER_CLASS_NAME_SUFFIX;
            registerMuleBeanDefinitionParser.arg(ExpressionFactory._new(ref(MessageProcessorDefinitionParser.class)).arg(ref(transformerClassName).boxify().dotclass()));
        }
    }

    private void registerBeanDefinitionParserForProcessor(org.mule.devkit.model.code.Method init, Method executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);

        Processor processor = executableElement.getAnnotation(Processor.class);
        String elementName = executableElement.getSimpleName().toString();
        if (processor.name().length() != 0) {
            elementName = processor.name();
        }

        init.body().invoke("registerBeanDefinitionParser").arg(ExpressionFactory.lit(NameUtils.uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));
    }

    private void registerBeanDefinitionParserForSource(org.mule.devkit.model.code.Method init, Method executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);

        Source source = executableElement.getAnnotation(Source.class);
        String elementName = executableElement.getSimpleName().toString();
        if (source.name().length() != 0) {
            elementName = source.name();
        }

        init.body().invoke("registerBeanDefinitionParser").arg(ExpressionFactory.lit(NameUtils.uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));
    }
}