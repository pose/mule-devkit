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

package org.mule.devkit.generation.mule;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;

import java.util.Map;

public class NestedProcessorChainGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasProcessorMethodWithParameter(NestedProcessor.class) ||
               typeElement.hasProcessorMethodWithParameterListOf(NestedProcessor.class);
    }

    @Override
    public void generate(DevKitTypeElement typeElement) {
        DefinedClass callbackClass = getNestedProcessorChainClass(typeElement);
        callbackClass._implements(ref(NestedProcessor.class));

        FieldVariable muleContext = callbackClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
        muleContext.javadoc().add("Mule Context");

        generateSetter(callbackClass, muleContext);

        FieldVariable chain = callbackClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), "chain");
        chain.javadoc().add("Chain that will be executed upon calling process");

        generateSetter(callbackClass, chain);

        FieldVariable event = callbackClass.field(Modifier.PRIVATE, ref(MuleEvent.class), "event");
        event.javadoc().add("Event that will be cloned for dispatching");

        generateSetter(callbackClass, event);

        generateCallbackConstructor(callbackClass, chain, event, muleContext);

        generateCallbackProcess(callbackClass, chain, event);

        generateCallbackProcessWithPayload(callbackClass, chain, event, muleContext);

        generateCallbackProcessWithProperties(callbackClass, chain, event);

        generateCallbackProcessWithPayloadAndProperties(callbackClass, chain, event, muleContext);
    }

    private void generateCallbackProcessWithPayload(DefinedClass callbackClass, FieldVariable chain, FieldVariable event, FieldVariable muleContext) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));
        Variable payload = process.param(ref(Object.class), "payload");

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(payload);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(event);
        process.body().assign(muleEvent, newMuleEvent);

        process.body()._return(chain.invoke("process").arg(muleEvent).invoke("getMessage").invoke("getPayload"));
    }

    private void generateCallbackProcessWithPayloadAndProperties(DefinedClass callbackClass, FieldVariable chain, FieldVariable event, FieldVariable muleContext) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));
        Variable payload = process.param(ref(Object.class), "payload");
        Variable properties = process.param(ref(Map.class).narrow(ref(String.class)).narrow(ref(Object.class)), "properties");

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(payload);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        ForEach forEachProperty = process.body().forEach(ref(String.class), "property", properties.invoke("keySet"));
        forEachProperty.body().add(muleMessage.invoke("setInvocationProperty")
                .arg(forEachProperty.var())
                .arg(properties.invoke("get").arg(forEachProperty.var())));

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(event);
        process.body().assign(muleEvent, newMuleEvent);

        process.body()._return(chain.invoke("process").arg(muleEvent).invoke("getMessage").invoke("getPayload"));
    }

    private void generateCallbackProcessWithProperties(DefinedClass callbackClass, FieldVariable chain, FieldVariable event) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "processWithExtraProperties");
        process._throws(ref(Exception.class));
        Variable properties = process.param(ref(Map.class).narrow(ref(String.class)).narrow(ref(Object.class)), "properties");

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        //Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        //newMuleMessage.arg(event.invoke("getMessage").invoke("getPayload"));
        //newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, event.invoke("getMessage"));

        ForEach forEachProperty = process.body().forEach(ref(String.class), "property", properties.invoke("keySet"));
        forEachProperty.body().add(muleMessage.invoke("setInvocationProperty")
                .arg(forEachProperty.var())
                .arg(properties.invoke("get").arg(forEachProperty.var())));

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(event);
        process.body().assign(muleEvent, newMuleEvent);

        process.body()._return(chain.invoke("process").arg(muleEvent).invoke("getMessage").invoke("getPayload"));
    }

    private void generateCallbackProcess(DefinedClass callbackClass, FieldVariable chain, FieldVariable event) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(event.invoke("getMessage"));
        newMuleEvent.arg(event);
        process.body().assign(muleEvent, newMuleEvent);

        process.body()._return(chain.invoke("process").arg(muleEvent).invoke("getMessage").invoke("getPayload"));
    }

    private void generateCallbackConstructor(DefinedClass callbackClass, FieldVariable chain, FieldVariable event, FieldVariable muleContext) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable event2 = constructor.param(ref(MuleEvent.class), "event");
        Variable muleContext2 = constructor.param(ref(MuleContext.class), "muleContext");
        Variable chain2 = constructor.param(ref(MessageProcessor.class), "chain");
        constructor.body().assign(ExpressionFactory._this().ref(event), event2);
        constructor.body().assign(ExpressionFactory._this().ref(chain), chain2);
        constructor.body().assign(ExpressionFactory._this().ref(muleContext), muleContext2);
    }

    private DefinedClass getNestedProcessorChainClass(DevKitTypeElement typeElement) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(typeElement.getPackageName() + NamingContants.CONFIG_NAMESPACE);
        DefinedClass clazz = pkg._class(NamingContants.NESTED_PROCESSOR_CHAIN_CLASS_NAME, new Class[]{MuleContextAware.class});
        clazz.role(DefinedClassRoles.NESTED_PROCESSOR_CHAIN);

        return clazz;
    }
}