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

import org.apache.commons.lang.StringUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.api.callback.SourceCallback;
import org.mule.api.callback.StopSourceCallback;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.spring.SchemaTypeConversion;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSourceGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return (typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class)) &&
                typeElement.hasMethodsAnnotatedWith(Source.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        for (DevKitExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Source.class)) {
            generateMessageSource(typeElement, executableElement);
        }
    }

    private void generateMessageSource(DevKitTypeElement typeElement, DevKitExecutableElement executableElement) {
        // get class
        Source sourceAnnotation = executableElement.getAnnotation(Source.class);
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement, sourceAnnotation.threadingModel() == SourceThreadingModel.SINGLE_THREAD);

        messageSourceClass.javadoc().add(messageSourceClass.name() + " wraps ");
        messageSourceClass.javadoc().add("{@link " + (executableElement.parent()).getQualifiedName().toString() + "#");
        messageSourceClass.javadoc().add(executableElement.getSimpleName().toString() + "(");
        boolean first = true;
        for (DevKitParameterElement variable : executableElement.getParameters()) {
            if (!first) {
                messageSourceClass.javadoc().add(", ");
            }
            messageSourceClass.javadoc().add(variable.asType().toString().replaceAll("<[a-zA-Z\\-\\.\\<\\>\\s\\,]*>", ""));
            first = false;
        }
        messageSourceClass.javadoc().add(")} method in ");
        messageSourceClass.javadoc().add(ref(executableElement.parent().asType()));
        messageSourceClass.javadoc().add(" as a message source capable of generating Mule events. ");
        messageSourceClass.javadoc().add(" The POJO's method is invoked in its own thread.");

        // add a field for each argument of the method
        Map<String, FieldVariableElement> fields = generateProcessorFieldForEachParameter(messageSourceClass, executableElement);

        // add fields for connectivity if required
        DevKitExecutableElement connectMethod = connectMethodForClass(typeElement);
        Map<String, AbstractMessageGenerator.FieldVariableElement> connectFields = null;
        if (connectMethod != null) {
            connectFields = generateProcessorFieldForEachParameter(messageSourceClass, connectMethod);
        }

        // add standard fields
        FieldVariable object = generateFieldForModuleObject(messageSourceClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageSourceClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageSourceClass);
        FieldVariable messageProcessor = generateFieldForMessageProcessorListener(messageSourceClass);

        FieldVariable stopSourceCallback = null;
        if (executableElement.getReturnType().toString().contains("StopSourceCallback")) {
            stopSourceCallback = messageSourceClass.field(Modifier.PRIVATE, ref(StopSourceCallback.class), "stopSourceCallback");
        }

        FieldVariable thread = null;
        if (sourceAnnotation.threadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            thread = messageSourceClass.field(Modifier.PRIVATE, ref(Thread.class), "thread");
            thread.javadoc().add("Thread under which this message source will execute");
        }

        // add initialise
        generateInitialiseMethod(messageSourceClass, fields, typeElement, muleContext, null, null, object, null, !typeElement.needsConfig());

        // add setmulecontext
        generateSetMuleContextMethod(messageSourceClass, muleContext);

        // add setobject
        generateSetModuleObjectMethod(messageSourceClass, object);

        // add setlistener
        generateSetListenerMethod(messageSourceClass, messageProcessor);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageSourceClass, flowConstruct);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageSourceClass, fields.get(fieldName).getField());
        }

        // generate setters for connectivity fields
        if (connectFields != null) {
            for (String fieldName : connectFields.keySet()) {
                generateSetter(messageSourceClass, connectFields.get(fieldName).getField());
            }
        }

        // add process method
        generateSourceCallbackProcessMethod(messageSourceClass, messageProcessor, muleContext, flowConstruct);
        generateSourceCallbackProcessWithPropertiesMethod(messageSourceClass, messageProcessor, muleContext, flowConstruct);
        generateSourceCallbackProcessMethodWithNoPayload(messageSourceClass, messageProcessor, muleContext, flowConstruct);

        if (sourceAnnotation.threadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            // add start method
            generateSingleThreadStartMethod(messageSourceClass, thread);
            // add stop method
            generateSingleThreadStopMethod(messageSourceClass, thread);
        } else {
            // get pool object if poolable
            if (typeElement.isPoolable()) {
                DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey(typeElement));

                // add start method method
                generateNoThreadStartMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, stopSourceCallback);
            } else {
                // add start method method
                generateNoThreadStartMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, flowConstruct, stopSourceCallback);
            }
            // add stop method
            generateNoThreadStopMethod(messageSourceClass, stopSourceCallback, executableElement);
        }

        if (sourceAnnotation.threadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            // get pool object if poolable
            if (typeElement.isPoolable()) {
                DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey(typeElement));

                // add run method
                generateRunMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, stopSourceCallback);
            } else {
                // add run method
                generateRunMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, flowConstruct, stopSourceCallback);
            }
        }
    }

    private void generateRunMethod(DefinedClass messageSourceClass, DevKitExecutableElement executableElement, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        generateRunMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, null, flowConstruct, stopSourceCallback);
    }


    private void generateRunMethod(DefinedClass messageSourceClass, DevKitExecutableElement executableElement, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        String methodName = executableElement.getSimpleName().toString();
        Method run = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "run");
        run.javadoc().add("Implementation {@link Runnable#run()} that will invoke the method on the pojo that this message source wraps.");

        generateSourceExecution(run.body(), executableElement, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, methodName, stopSourceCallback);
    }

    private void generateSourceExecution(Block body, DevKitExecutableElement executableElement, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, String methodName, FieldVariable stopSourceCallback) {
        DefinedClass moduleObjectClass = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(executableElement.parent()));
        Variable moduleObject = body.decl(moduleObjectClass, "castedModuleObject", ExpressionFactory._null());

        Variable poolObject = null;
        if (poolObjectClass != null) {
            poolObject = body.decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }

        // add connection field declarations
        Map<String, Expression> connectionParameters = new HashMap<String, Expression>();
        DevKitExecutableElement connectMethod = connectForMethod(executableElement);
        Variable connection = null;
        if (connectMethod != null) {
            DefinedClass connectionClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey(connectMethod.parent()));
            connection = body.decl(connectionClass, "connection", ExpressionFactory._null());

            for (DevKitParameterElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Type type = ref(connectFields.get(fieldName).getVariableElement().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);

                Variable transformed = body.decl(type, name, ExpressionFactory._null());
                connectionParameters.put(fieldName, transformed);
            }
        }

        TryStatement callSource = body._try();

        findConfig(callSource.body(), muleContext, object, methodName, null, moduleObjectClass, moduleObject);

        if (connectMethod != null) {
            for (DevKitParameterElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Conditional ifNotNull = callSource.body()._if(Op.ne(connectFields.get(fieldName).getField(),
                        ExpressionFactory._null()));

                Type type = ref(connectFields.get(fieldName).getVariableElement().asType()).boxify();

                Variable transformed = (Variable) connectionParameters.get(fieldName);

                Cast cast = ExpressionFactory.cast(type, connectFields.get(fieldName).getField());

                ifNotNull._then().assign(transformed, cast);

                Cast castLocal = ExpressionFactory.cast(type, moduleObject.invoke("get" + StringUtils.capitalize(fieldName)));

                Conditional ifConfigAlsoNull = ifNotNull._else()._if(Op.eq(moduleObject.invoke("get" + StringUtils.capitalize(fieldName)), ExpressionFactory._null()));
                TypeReference coreMessages = ref(CoreMessages.class);
                Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
                if (methodName != null) {
                    failedToInvoke.arg(ExpressionFactory.lit(methodName));
                }
                Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
                messageException.arg(failedToInvoke);
                messageException.arg(ExpressionFactory.cast(ref(MuleEvent.class), ExpressionFactory._null()));
                messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("You must provide a " + fieldName + " at the config or the message processor level."));
                ifConfigAlsoNull._then()._throw(messageException);

                ifNotNull._else().assign(transformed, castLocal);

            }
        }

        List<Expression> parameters = new ArrayList<Expression>();
        for (DevKitParameterElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                parameters.add(ExpressionFactory._this());
            } else {
                String fieldName = variable.getSimpleName().toString();
                if (SchemaTypeConversion.isSupported(fields.get(fieldName).getVariableElement().asType().toString()) ||
                        fields.get(fieldName).getVariableElement().isXmlType() ||
                        context.getTypeMirrorUtils().isCollection(fields.get(fieldName).getVariableElement().asType()) ||
                        context.getTypeMirrorUtils().isEnum(fields.get(fieldName).getVariableElement().asType())) {
                    Variable transformed = callSource.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), ExpressionFactory._null());
                    Conditional notNull = callSource.body()._if(Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()));
                    generateTransform(notNull._then(), transformed, fields.get(fieldName).getField(), fields.get(fieldName).getVariableElement().asType(), muleContext);
                    parameters.add(transformed);
                } else {
                    parameters.add(fields.get(fieldName).getField());
                }
            }
        }


        Invocation methodCall;
        if (poolObject != null) {
            callSource.body().assign(poolObject, ExpressionFactory.cast(poolObject.type(), moduleObject.invoke("getLifecyleEnabledObjectPool").invoke("borrowObject")));
            methodCall = poolObject.invoke(methodName);
        } else if (connectMethod != null) {
            DefinedClass connectionKey = context.getClassForRole(context.getNameUtils().generateConnectionParametersRoleKey(executableElement.parent()));

            Invocation newKey = ExpressionFactory._new(connectionKey);
            Invocation createConnection = moduleObject.invoke("acquireConnection");
            for (DevKitParameterElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }
            createConnection.arg(newKey);
            callSource.body().assign(connection, createConnection);

            Conditional ifConnectionIsNull = callSource.body()._if(Op.eq(connection, ExpressionFactory._null()));
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
            if (methodName != null) {
                failedToInvoke.arg(ExpressionFactory.lit(methodName));
            }
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            messageException.arg(ExpressionFactory.cast(ref(MuleEvent.class), ExpressionFactory._null()));
            messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("Cannot create connection"));
            ifConnectionIsNull._then()._throw(messageException);

            methodCall = connection.invoke(methodName);
        } else {
            methodCall = moduleObject.invoke(methodName);
        }

        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
        }

        if (executableElement.getReturnType().toString().contains("StopSourceCallback")) {
            callSource.body().assign(stopSourceCallback, methodCall);
        } else {
            callSource.body().add(methodCall);
        }

        CatchBlock catchMessagingException = callSource._catch(ref(MessagingException.class));
        Variable messagingException = catchMessagingException.param("e");
        catchMessagingException.body().add(flowConstruct.invoke("getExceptionListener").invoke("handleException").arg(messagingException).arg(messagingException.invoke("getEvent")));

        CatchBlock catchException = callSource._catch(ref(Exception.class));
        Variable exception = catchException.param("e");
        catchException.body().add(muleContext.invoke("getExceptionListener").invoke("handleException").arg(exception));

        if (poolObjectClass != null) {
            Block fin = callSource._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(moduleObject.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }

        if (connectMethod != null) {
            Block fin = callSource._finally();
            Block connectionNotNull = fin._if(Op.ne(connection, ExpressionFactory._null()))._then();

            TryStatement tryToReleaseSession = connectionNotNull._try();

            DefinedClass connectionKey = context.getClassForRole(context.getNameUtils().generateConnectionParametersRoleKey(executableElement.parent()));
            Invocation newKey = ExpressionFactory._new(connectionKey);
            for (String field : connectionParameters.keySet()) {
                newKey.arg(connectionParameters.get(field));
            }

            Invocation returnConnection = moduleObject.invoke("releaseConnection");
            returnConnection.arg(newKey);
            returnConnection.arg(connection);

            tryToReleaseSession.body().add(returnConnection);

            tryToReleaseSession._catch(ref(Exception.class));
        }
    }

    private void generateSingleThreadStartMethod(DefinedClass messageSourceClass, FieldVariable thread) {
        Method start = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        start.javadoc().add("Method to be called when Mule instance gets started.");
        start._throws(ref(MuleException.class));
        Conditional ifNoThread = start.body()._if(Op.eq(thread, ExpressionFactory._null()));
        Invocation newThread = ExpressionFactory._new(ref(Thread.class));
        newThread.arg(ExpressionFactory._this());
        newThread.arg("Receiving Thread");
        ifNoThread._then().assign(thread, newThread);

        start.body().add(thread.invoke("start"));
    }


    private void generateSingleThreadStopMethod(DefinedClass messageSourceClass, FieldVariable thread) {
        Method stop = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stop.javadoc().add("Method to be called when Mule instance gets stopped.");
        stop._throws(ref(MuleException.class));

        stop.body().add(thread.invoke("interrupt"));
    }

    private void generateNoThreadStartMethod(DefinedClass messageSourceClass, DevKitExecutableElement executableElement, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        generateNoThreadStartMethod(messageSourceClass, executableElement, fields, connectFields, object, muleContext, null, flowConstruct, stopSourceCallback);
    }

    private void generateNoThreadStartMethod(DefinedClass messageSourceClass, DevKitExecutableElement executableElement, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        String methodName = executableElement.getSimpleName().toString();
        Method start = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        start.javadoc().add("Method to be called when Mule instance gets started.");
        start._throws(ref(MuleException.class));

        generateSourceExecution(start.body(), executableElement, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, methodName, stopSourceCallback);
    }


    private void generateNoThreadStopMethod(DefinedClass messageSourceClass, FieldVariable stopSourceCallback, DevKitExecutableElement executableElement) {
        String methodName = executableElement.getSimpleName().toString();
        Method stop = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stop.javadoc().add("Method to be called when Mule instance gets stopped.");
        stop._throws(ref(MuleException.class));

        if (stopSourceCallback != null) {
            Conditional ifStopCallbackNotNull = stop.body()._if(Op.ne(stopSourceCallback, ExpressionFactory._null()));
            TryStatement tryToStop = ifStopCallbackNotNull._then()._try();
            tryToStop.body().add(stopSourceCallback.invoke("stop"));
            CatchBlock catchException = tryToStop._catch(ref(Exception.class));
            Variable e = catchException.param("e");

            Invocation messagingException = ExpressionFactory._new(ref(MessagingException.class));
            messagingException.arg(ref(CoreMessages.class).staticInvoke("failedToStop").arg(methodName));
            messagingException.arg(ExpressionFactory.cast(ref(MuleEvent.class), ExpressionFactory._null()));
            messagingException.arg(e);

            catchException.body()._throw(messagingException);
        }
    }

}