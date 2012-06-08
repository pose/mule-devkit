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
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.SourceMethod;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.*;
import org.mule.devkit.model.schema.SchemaTypeConversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSourceGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.isModuleOrConnector() &&
               type.hasMethodsAnnotatedWith(Source.class);
    }

    @Override
    public void generate(Type type) {
        for (SourceMethod sourceMethod : type.getSourceMethods()) {
            generateMessageSource(type, sourceMethod);
        }
    }

    private void generateMessageSource(Type type, SourceMethod sourceMethod) {
        // get class
        DefinedClass messageSourceClass = getMessageSourceClass(sourceMethod);

        messageSourceClass.javadoc().add(messageSourceClass.name() + " wraps ");
        messageSourceClass.javadoc().add("{@link " + (sourceMethod.parent()).getQualifiedName().toString() + "#");
        messageSourceClass.javadoc().add(sourceMethod.getSimpleName().toString() + "(");
        boolean first = true;
        for (Parameter variable : sourceMethod.getParameters()) {
            if (!first) {
                messageSourceClass.javadoc().add(", ");
            }
            messageSourceClass.javadoc().add(variable.asType().toString().replaceAll("<[a-zA-Z\\-\\.\\<\\>\\s\\,]*>", ""));
            first = false;
        }
        messageSourceClass.javadoc().add(")} method in ");
        messageSourceClass.javadoc().add(ref(sourceMethod.parent().asType()));
        messageSourceClass.javadoc().add(" as a message source capable of generating Mule events. ");
        messageSourceClass.javadoc().add(" The POJO's method is invoked in its own thread.");

        // add a field for each argument of the method
        Map<String, FieldVariableElement> fields = generateProcessorFieldForEachParameter(messageSourceClass, sourceMethod);

        // add fields for connectivity if required
        Method connectMethod = connectMethodForClass(type);
        Map<String, AbstractMessageGenerator.FieldVariableElement> connectFields = null;
        if (connectMethod != null) {
            connectFields = generateProcessorFieldForEachParameter(messageSourceClass, connectMethod);
        }

        // add standard fields
        FieldVariable object = generateFieldForModuleObject(messageSourceClass, type);
        FieldVariable muleContext = generateFieldForMuleContext(messageSourceClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageSourceClass);
        FieldVariable messageProcessor = generateFieldForMessageProcessorListener(messageSourceClass);

        FieldVariable stopSourceCallback = null;
        if (sourceMethod.getReturnType().toString().contains("StopSourceCallback")) {
            stopSourceCallback = messageSourceClass.field(Modifier.PRIVATE, ref(StopSourceCallback.class), "stopSourceCallback");
        }

        FieldVariable thread = null;
        if (sourceMethod.getThreadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            thread = messageSourceClass.field(Modifier.PRIVATE, ref(Thread.class), "thread");
            thread.javadoc().add("Thread under which this message source will execute");
        }

        // add initialise
        generateInitialiseMethod(messageSourceClass, fields, type, muleContext, null, null, object, null, !type.needsConfig());

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

        if (sourceMethod.getThreadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            // add start method
            generateSingleThreadStartMethod(messageSourceClass, thread);
            // add stop method
            generateSingleThreadStopMethod(messageSourceClass, thread);
        } else {
            // get pool object if poolable
            if (type.isPoolable()) {
                DefinedClass poolObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.POOL_OBJECT, ref(type));

                // add start method method
                generateNoThreadStartMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, stopSourceCallback);
            } else {
                // add start method method
                generateNoThreadStartMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, flowConstruct, stopSourceCallback);
            }
            // add stop method
            generateNoThreadStopMethod(messageSourceClass, stopSourceCallback, sourceMethod);
        }

        if (sourceMethod.getThreadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            // get pool object if poolable
            if (type.isPoolable()) {
                DefinedClass poolObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.POOL_OBJECT, ref(type));

                // add run method
                generateRunMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, stopSourceCallback);
            } else {
                // add run method
                generateRunMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, flowConstruct, stopSourceCallback);
            }
        }
    }

    private void generateRunMethod(DefinedClass messageSourceClass, SourceMethod sourceMethod, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        generateRunMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, null, flowConstruct, stopSourceCallback);
    }


    private void generateRunMethod(DefinedClass messageSourceClass, SourceMethod sourceMethod, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        String methodName = sourceMethod.getSimpleName().toString();
        org.mule.devkit.model.code.Method run = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "run");
        run.javadoc().add("Implementation {@link Runnable#run()} that will invoke the method on the pojo that this message source wraps.");

        generateSourceExecution(run.body(), sourceMethod, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, methodName, stopSourceCallback);
    }

    private void generateSourceExecution(Block body, SourceMethod sourceMethod, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, String methodName, FieldVariable stopSourceCallback) {
        DefinedClass moduleObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(sourceMethod.parent()));
        Variable moduleObject = body.decl(moduleObjectClass, "castedModuleObject", ExpressionFactory._null());

        Variable poolObject = null;
        if (poolObjectClass != null) {
            poolObject = body.decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }

        // add connection field declarations
        Map<String, Expression> connectionParameters = new HashMap<String, Expression>();
        Method connectMethod = connectForMethod(sourceMethod);
        Variable connection = null;
        if (connectMethod != null) {
            DefinedClass connectionClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connectMethod.parent()));
            connection = body.decl(connectionClass, "connection", ExpressionFactory._null());

            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                org.mule.devkit.model.code.Type type = ref(connectFields.get(fieldName).getVariable().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);

                Variable transformed = body.decl(type, name, ExpressionFactory._null());
                connectionParameters.put(fieldName, transformed);
            }
        }

        TryStatement callSource = body._try();

        findConfig(callSource.body(), muleContext, object, methodName, null, moduleObjectClass, moduleObject);

        if (connectMethod != null) {
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Conditional ifNotNull = callSource.body()._if(Op.ne(connectFields.get(fieldName).getField(),
                        ExpressionFactory._null()));

                org.mule.devkit.model.code.Type type = ref(connectFields.get(fieldName).getVariable().asType()).boxify();

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
        for (Parameter variable : sourceMethod.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                parameters.add(ExpressionFactory._this());
            } else {
                String fieldName = variable.getSimpleName().toString();
                if (SchemaTypeConversion.isSupported(fields.get(fieldName).getVariable().asType().toString()) ||
                        fields.get(fieldName).getVariable().isXmlType() ||
                        fields.get(fieldName).getVariable().isCollection() ||
                        fields.get(fieldName).getVariable().isEnum()) {
                    Variable transformed = callSource.body().decl(ref(fields.get(fieldName).getVariable().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), ExpressionFactory._null());
                    Conditional notNull = callSource.body()._if(Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()));
                    generateTransform(notNull._then(), transformed, fields.get(fieldName).getField(), fields.get(fieldName).getVariable().asType(), muleContext);
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
            DefinedClass connectionKey = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTION_PARAMETERS, ref(sourceMethod.parent()));

            Invocation newKey = ExpressionFactory._new(connectionKey);
            Invocation createConnection = moduleObject.invoke("acquireConnection");
            for (Parameter variable : connectMethod.getParameters()) {
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

        if (sourceMethod.getReturnType().toString().contains("StopSourceCallback")) {
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

            DefinedClass connectionKey = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTION_PARAMETERS, ref(sourceMethod.parent()));
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
        org.mule.devkit.model.code.Method start = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "start");
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
        org.mule.devkit.model.code.Method stop = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "stop");
        stop.javadoc().add("Method to be called when Mule instance gets stopped.");
        stop._throws(ref(MuleException.class));

        stop.body().add(thread.invoke("interrupt"));
    }

    private void generateNoThreadStartMethod(DefinedClass messageSourceClass, SourceMethod sourceMethod, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        generateNoThreadStartMethod(messageSourceClass, sourceMethod, fields, connectFields, object, muleContext, null, flowConstruct, stopSourceCallback);
    }

    private void generateNoThreadStartMethod(DefinedClass messageSourceClass, SourceMethod sourceMethod, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectFields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass, FieldVariable flowConstruct, FieldVariable stopSourceCallback) {
        String methodName = sourceMethod.getSimpleName().toString();
        org.mule.devkit.model.code.Method start = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "start");
        start.javadoc().add("Method to be called when Mule instance gets started.");
        start._throws(ref(MuleException.class));

        generateSourceExecution(start.body(), sourceMethod, fields, connectFields, object, muleContext, poolObjectClass, flowConstruct, methodName, stopSourceCallback);
    }


    private void generateNoThreadStopMethod(DefinedClass messageSourceClass, FieldVariable stopSourceCallback, SourceMethod sourceMethod) {
        String methodName = sourceMethod.getSimpleName().toString();
        org.mule.devkit.model.code.Method stop = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "stop");
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

    private DefinedClass getMessageSourceClass(SourceMethod sourceMethod) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(sourceMethod.parent().getPackageName() + NamingConstants.MESSAGE_SOURCE_NAMESPACE);
        ArrayList<Class> inherits = new ArrayList<Class>();
        inherits.add(MuleContextAware.class);
        inherits.add(Startable.class);
        inherits.add(Stoppable.class);
        inherits.add(Initialisable.class);
        inherits.add(SourceCallback.class);
        inherits.add(FlowConstructAware.class);

        if (sourceMethod.getAnnotation(Source.class).threadingModel() == SourceThreadingModel.SINGLE_THREAD) {
            inherits.add(Runnable.class);
        }

        if( sourceMethod.getAnnotation(Source.class).primaryNodeOnly() ) {
            inherits.add(ClusterizableMessageSource.class);
        } else {
            inherits.add(MessageSource.class);
        }

        DefinedClass clazz = pkg._class(sourceMethod.getCapitalizedName() + NamingConstants.MESSAGE_SOURCE_CLASS_NAME_SUFFIX, inherits.toArray( new Class<?>[] {} ));
        clazz.role(DefinedClassRoles.MESSAGE_SOURCE, sourceMethod);

        return clazz;
    }
}