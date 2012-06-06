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
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;

public class DefaultSaveAccessTokenCallbackFactoryGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    public void generate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass factory = getDefaultSaveAccessTokenCallbackFactoryClass(typeElement);

        DefinedClass callback = ctx().getCodeModel()._class(DefinedClassRoles.DEFAULT_SAVE_ACCESS_TOKEN_CALLBACK);
        Method getObjectType = factory.method(Modifier.PUBLIC, ref(Class.class), "getObjectType");
        getObjectType.body()._return(callback.dotclass());

        Method getObject = factory.method(Modifier.PUBLIC, ref(Object.class), "getObject");
        getObject._throws(ref(Exception.class));
        Variable callbackVariable = getObject.body().decl(callback, "callback", ExpressionFactory._new(callback));
        getObject.body().add(
                callbackVariable.invoke("setMessageProcessor").arg(
                        ExpressionFactory.cast(ref(MessageProcessor.class),
                                ExpressionFactory._super().invoke("getObject"))));

        getObject.body()._return(callbackVariable);
    }

    private DefinedClass getDefaultSaveAccessTokenCallbackFactoryClass(DevKitTypeElement type) {
        String callbackClassName = ctx().getNameUtils().generateClassNameInPackage(type, NamingContants.CONFIG_NAMESPACE, NamingContants.SAVE_ACCESS_TOKEN_CALLBACK_FACTORY_BEAN_CLASS_NAME);
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(ctx().getNameUtils().getPackageName(callbackClassName));
        DefinedClass clazz = pkg._class(ctx().getNameUtils().getClassName(callbackClassName));
        clazz._extends(ref(MessageProcessorChainFactoryBean.class));
        clazz.role(DefinedClassRoles.DEFAULT_SAVE_ACCESS_TOKEN_FACTORY);

        return clazz;
    }

}
