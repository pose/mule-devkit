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
package org.mule.devkit.generation.mule.oauth;

import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.generation.spring.AbstractBeanDefinitionParserGenerator;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class AuthorizeBeanDefinitionParserGenerator extends AbstractMessageGenerator {
    public static final String AUTHORIZE_DEFINITION_PARSER_ROLE = "AuthorizeDefinitionParser";

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    public void generate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass beanDefinitionParser = getAuthorizeBeanDefinitionParserClass(typeElement);
        DefinedClass messageProcessorClass = ctx().getCodeModel()._class(DefinedClassRoles.AUTHORIZE_MESSAGE_PROCESSOR);

        Method parse = beanDefinitionParser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(messageProcessorClass.dotclass().invoke("getName")));

        parse.body().invoke("parseConfigRef").arg(element).arg(builder);

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().invoke("setNoRecurseOnDefinition").arg(definition);

        parse.body().invoke("attachProcessorDefinition").arg(parserContext).arg(definition);

        parse.body()._return(definition);
    }

    private DefinedClass getAuthorizeBeanDefinitionParserClass(DevKitTypeElement type) {
        String authorizeBeanDefinitionParserClass = ctx().getNameUtils().generateClassNameInPackage(type, NamingContants.CONFIG_NAMESPACE, NamingContants.AUTHORIZE_DEFINITION_PARSER_CLASS_NAME);
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(ctx().getNameUtils().getPackageName(authorizeBeanDefinitionParserClass));
        DefinedClass abstractDefinitionParser = ctx().getCodeModel()._class(DefinedClassRoles.ABSTRACT_BEAN_DEFINITION_PARSER);
        DefinedClass clazz = pkg._class(ctx().getNameUtils().getClassName(authorizeBeanDefinitionParserClass), abstractDefinitionParser);
        clazz.role(DefinedClassRoles.AUTHORIZE_BEAN_DEFINITION_PARSER);

        return clazz;
    }

}
