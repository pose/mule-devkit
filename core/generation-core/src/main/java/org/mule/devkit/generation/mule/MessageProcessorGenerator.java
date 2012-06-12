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
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.InvalidateConnectionOn;
import org.mule.api.annotations.Mime;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.param.CorrelationGroupSize;
import org.mule.api.annotations.param.CorrelationId;
import org.mule.api.annotations.param.CorrelationSequence;
import org.mule.api.annotations.param.ExceptionPayload;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.MessageRootId;
import org.mule.api.annotations.param.MessageUniqueId;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.annotations.param.SessionHeaders;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.expression.MessageHeaderExpressionEvaluator;
import org.mule.expression.MessageHeadersExpressionEvaluator;
import org.mule.expression.MessageHeadersListExpressionEvaluator;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {


    @Override
    public boolean shouldGenerate(Type type) {
        return (type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class)) &&
                type.hasMethodsAnnotatedWith(Processor.class);
    }

    @Override
    public void generate(Type type) {
        for (Method executableElement : type.getMethodsAnnotatedWith(Processor.class)) {
            generateMessageProcessor(type, executableElement);
        }
    }

    private void generateMessageProcessor(Type type, Method processorMethod) {
        // get class
        DefinedClass messageProcessorClass;

        boolean intercepting = processorMethod.getAnnotation(Processor.class).intercepting();
        if (intercepting) {
            messageProcessorClass = getInterceptingMessageProcessorClass(processorMethod);
        } else {
            messageProcessorClass = getMessageProcessorClass(processorMethod);
        }

        ctx().note("Generating message processor as " + messageProcessorClass.fullName() + " for method " + processorMethod.getSimpleName().toString() + " in " + type.getSimpleName().toString());

        // add javadoc
        generateMessageProcessorClassDoc(processorMethod, messageProcessorClass);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateProcessorFieldForEachParameter(messageProcessorClass, processorMethod);

        // add fields for connectivity if required
        Method connectMethod = connectMethodForClass(type);
        Map<String, AbstractMessageGenerator.FieldVariableElement> connectFields = null;
        if (connectMethod != null) {
            connectFields = generateProcessorFieldForEachParameter(messageProcessorClass, connectMethod);
        }

        // add standard fields
        FieldVariable logger = generateLoggerField(messageProcessorClass);
        FieldVariable object = generateFieldForModuleObject(messageProcessorClass, type);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageProcessorClass);
        FieldVariable retryCount = generateRetryCountField(messageProcessorClass);
        FieldVariable retryMax = generateRetryMaxField(messageProcessorClass);

        FieldVariable messageProcessorListener = null;
        if (intercepting) {
            messageProcessorListener = generateFieldForMessageProcessorListener(messageProcessorClass);
        }

        // add initialise
        generateInitialiseMethod(messageProcessorClass, fields, type, muleContext, object, retryCount, !type.needsConfig());

        // add start
        generateStartMethod(messageProcessorClass, fields);

        // add stop
        generateStopMethod(messageProcessorClass, fields);

        // add dispose
        generateDiposeMethod(messageProcessorClass, fields);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext, fields);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageProcessorClass, flowConstruct, fields);

        if (intercepting) {
            // add setlistener
            generateSetListenerMethod(messageProcessorClass, messageProcessorListener);

            // add process method
            generateSourceCallbackProcessMethod(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
            generateSourceCallbackProcessWithPropertiesMethod(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
            generateSourceCallbackProcessMethodWithNoPayload(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
        }

        // add setobject
        generateSetModuleObjectMethod(messageProcessorClass, object);

        // add setRetryMax
        generateSetter(messageProcessorClass, retryMax);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // generate setters for connectivity fields
        if (connectFields != null) {
            for (String fieldName : connectFields.keySet()) {
                generateSetter(messageProcessorClass, connectFields.get(fieldName).getField());
            }
        }

        // get pool object if poolable
        if (type.isPoolable()) {
            DefinedClass poolObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.POOL_OBJECT, ref(type));

            // add process method
            generateProcessMethod(processorMethod, messageProcessorClass, fields, connectFields, messageProcessorListener, muleContext, object, poolObjectClass, logger, retryCount, retryMax);
        } else {
            // add process method
            generateProcessMethod(processorMethod, messageProcessorClass, fields, connectFields, messageProcessorListener, muleContext, object, logger, retryCount, retryMax);
        }
    }

    private void generateMessageProcessorClassDoc(Method executableElement, DefinedClass messageProcessorClass) {
        messageProcessorClass.javadoc().add(messageProcessorClass.name() + " invokes the ");
        messageProcessorClass.javadoc().add("{@link " + (executableElement.parent()).getQualifiedName().toString() + "#");
        messageProcessorClass.javadoc().add(executableElement.getSimpleName().toString() + "(");
        boolean first = true;
        for (Parameter variable : executableElement.getParameters()) {
            if (!first) {
                messageProcessorClass.javadoc().add(", ");
            }
            messageProcessorClass.javadoc().add(variable.asType().toString().replaceAll("<[a-zA-Z\\-\\.\\<\\>\\s\\,]*>", ""));
            first = false;
        }
        messageProcessorClass.javadoc().add(")} method in ");
        messageProcessorClass.javadoc().add(ref(executableElement.parent().asType()));
        messageProcessorClass.javadoc().add(". For each argument there is a field in this processor to match it. ");
        messageProcessorClass.javadoc().add(" Before invoking the actual method the processor will evaluate and transform");
        messageProcessorClass.javadoc().add(" where possible to the expected argument type.");
    }

    private void generateProcessMethod(Method executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable logger, FieldVariable retryCount, FieldVariable retryMax) {
        generateProcessMethod(executableElement, messageProcessorClass, fields, connectionFields, messageProcessorListener, muleContext, object, null, logger, retryCount, retryMax);
    }

    private void generateProcessMethod(Method executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, DefinedClass poolObjectClass, FieldVariable logger, FieldVariable retryCount, FieldVariable retryMax) {
        String methodName = executableElement.getSimpleName().toString();
        org.mule.devkit.model.code.Type muleEvent = ref(MuleEvent.class);

        org.mule.devkit.model.code.Method process = messageProcessorClass.method(Modifier.PUBLIC, muleEvent, "process");
        process.javadoc().add("Invokes the MessageProcessor.");
        process.javadoc().addParam("event MuleEvent to be processed");
        process.javadoc().addThrows(ref(MuleException.class));

        process._throws(MuleException.class);
        org.mule.devkit.model.code.Variable event = process.param(muleEvent, "event");
        org.mule.devkit.model.code.Variable muleMessage = process.body().decl(ref(MuleMessage.class), "_muleMessage", event.invoke("getMessage"));

        DefinedClass moduleObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(executableElement.parent()));
        org.mule.devkit.model.code.Variable moduleObject = process.body().decl(moduleObjectClass, "_castedModuleObject", ExpressionFactory._null());
        findConfig(process.body(), muleContext, object, methodName, event, moduleObjectClass, moduleObject);

        org.mule.devkit.model.code.Variable poolObject = declarePoolObjectIfClassNotNull(poolObjectClass, process);

        Map<String, Expression> connectionParameters = declareConnectionParametersVariables(executableElement, connectionFields, process);
        org.mule.devkit.model.code.Variable connection = addConnectionVariableIfNeeded(executableElement, process);

        Method connectMethod = connectForMethod(executableElement);
        Method connectionIdentifierMethod = connectionIdentifierForMethod(executableElement);
        TryStatement callProcessor = process.body()._try();

        if (connectMethod != null) {
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Conditional ifNotNull = callProcessor.body()._if(Op.ne(connectionFields.get(fieldName).getField(),
                        ExpressionFactory._null()));

                org.mule.devkit.model.code.Type type = ref(connectionFields.get(fieldName).getVariable().asType()).boxify();

                org.mule.devkit.model.code.Variable transformed = (org.mule.devkit.model.code.Variable) connectionParameters.get(fieldName);

                Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                        ExpressionFactory.lit(connectionFields.get(fieldName).getFieldType().name())
                ).invoke("getGenericType");
                Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(getGenericType).arg(ExpressionFactory._null());

                evaluateAndTransform.arg(connectionFields.get(fieldName).getField());

                Cast cast = ExpressionFactory.cast(type, evaluateAndTransform);

                ifNotNull._then().assign(transformed, cast);

                Invocation evaluateAndTransformLocal = ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(getGenericType).arg(ExpressionFactory._null());

                evaluateAndTransformLocal.arg(moduleObject.invoke("get" + StringUtils.capitalize(fieldName)));

                Cast castLocal = ExpressionFactory.cast(type, evaluateAndTransformLocal);

                Conditional ifConfigAlsoNull = ifNotNull._else()._if(Op.eq(moduleObject.invoke("get" + StringUtils.capitalize(fieldName)), ExpressionFactory._null()));
                TypeReference coreMessages = ref(CoreMessages.class);
                Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
                if (methodName != null) {
                    failedToInvoke.arg(ExpressionFactory.lit(methodName));
                }
                Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
                messageException.arg(failedToInvoke);
                if (event != null) {
                    messageException.arg(event);
                }
                messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("You must provide a " + fieldName + " at the config or the message processor level."));
                ifConfigAlsoNull._then()._throw(messageException);

                ifNotNull._else().assign(transformed, castLocal);

            }
        }

        List<Expression> parameters = new ArrayList<Expression>();
        org.mule.devkit.model.code.Variable interceptCallback = null;
        org.mule.devkit.model.code.Variable outboundHeadersMap = null;
        for (Parameter variable : executableElement.getParameters()) {
            String fieldName = variable.getSimpleName().toString();

            if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                parameters.add(fields.get(fieldName).getFieldType());
            } else if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                parameters.add(ExpressionFactory._this());
            } else if (variable.getAnnotation(OAuthAccessToken.class) != null) {
                continue;
            } else if (variable.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                continue;
            } else if (variable.isNestedProcessor()) {
                declareNestedProcessorParameter(fields, muleContext, event, callProcessor, parameters, variable, fieldName);
            } else if (variable.asType().toString().startsWith(MuleMessage.class.getName())) {
                parameters.add(muleMessage);
            } else {
                outboundHeadersMap = declareStandardParameter(messageProcessorClass, fields, muleMessage, callProcessor, parameters, outboundHeadersMap, variable, fieldName, muleContext);
            }
        }

        if (connectMethod != null) {
            DefinedClass connectionKey = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTION_PARAMETERS, ref(executableElement.parent()));

            Conditional ifDebugEnabled = callProcessor.body()._if(logger.invoke("isDebugEnabled"));
            org.mule.devkit.model.code.Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "_messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Attempting to acquire a connection using "));
            for (String field : connectionParameters.keySet()) {
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[" + field + " = ")));
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(connectionParameters.get(field)));
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
            }
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            Invocation newKey = ExpressionFactory._new(connectionKey);
            Invocation createConnection = moduleObject.invoke("acquireConnection");
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }
            createConnection.arg(newKey);
            callProcessor.body().assign(connection, createConnection);

            Conditional ifConnectionIsNull = callProcessor.body()._if(Op.eq(connection, ExpressionFactory._null()));
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
            if (methodName != null) {
                failedToInvoke.arg(ExpressionFactory.lit(methodName));
            }
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            if (event != null) {
                messageException.arg(event);
            }
            messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("Cannot create connection"));
            ifConnectionIsNull._then()._throw(messageException);

            ifDebugEnabled = ifConnectionIsNull._else()._if(logger.invoke("isDebugEnabled"));
            messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "_messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Connection has been acquired with "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[id = ")));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));
        }

        org.mule.devkit.model.code.Type returnType = ref(executableElement.getReturnType());

        callProcessor.body().add(retryCount.invoke("getAndIncrement"));

        if (connectMethod != null) {
            generateMethodCall(callProcessor.body(), connection, methodName, parameters, event, returnType, poolObject, interceptCallback, messageProcessorListener);
        } else {
            generateMethodCall(callProcessor.body(), moduleObject, methodName, parameters, event, returnType, poolObject, interceptCallback, messageProcessorListener);
        }

        callProcessor.body().add(retryCount.invoke("set").arg(ExpressionFactory.lit(0)));

        for (Parameter variable : executableElement.getParameters()) {
            OutboundHeaders outboundHeaders = variable.getAnnotation(OutboundHeaders.class);
            if (outboundHeaders != null) {
                Conditional ifNotEmpty = callProcessor.body()._if(Op.cand(Op.ne(outboundHeadersMap, ExpressionFactory._null()),
                        Op.not(outboundHeadersMap.invoke("isEmpty"))));
                ifNotEmpty._then().add(event.invoke("getMessage").invoke("addProperties").arg(outboundHeadersMap)
                        .arg(ref(PropertyScope.class).staticRef("OUTBOUND")));
            }
        }
        
        if( executableElement.getAnnotation(Mime.class) != null ) {
            Cast defaultMuleMessage = ExpressionFactory.cast(ref(DefaultMuleMessage.class), event.invoke("getMessage"));
            Invocation setMimeType = defaultMuleMessage.invoke("setMimeType").arg(
                    ExpressionFactory.lit(executableElement.getAnnotation(Mime.class).value())
            );
            callProcessor.body().add(setMimeType);
        }

        callProcessor.body()._return(event);

        InvalidateConnectionOn invalidateConnectionOn = executableElement.getAnnotation(InvalidateConnectionOn.class);
        if (connectMethod != null &&
                invalidateConnectionOn != null) {

            final String transformerAnnotationName = InvalidateConnectionOn.class.getName();
            DeclaredType exception = null;
            List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
            for (AnnotationMirror annotationMirror : annotationMirrors) {
                if (transformerAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                        if ("exception".equals(
                                entry.getKey().getSimpleName().toString())) {
                            exception = (DeclaredType) entry.getValue().getValue();
                            break;
                        }
                    }
                }
            }

            CatchBlock catchBlock = callProcessor._catch(ref(exception).boxify());

            Conditional ifDebugEnabled = catchBlock.body()._if(logger.invoke("isDebugEnabled"));
            org.mule.devkit.model.code.Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "_messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("An exception ("));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ref(exception).boxify().fullName()));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(") has been thrown while executing "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit(methodName)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(". Destroying the connection with [id = "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            TryStatement innerTry = catchBlock.body()._try();

            DefinedClass connectionKey = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTION_PARAMETERS, ref(executableElement.parent()));
            Invocation newKey = ExpressionFactory._new(connectionKey);
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }

            Invocation destroySession = moduleObject.invoke("destroyConnection");
            destroySession.arg(newKey);
            destroySession.arg(connection);

            innerTry.body().add(destroySession);
            innerTry.body().assign(connection, ExpressionFactory._null());

            CatchBlock logException = innerTry._catch(ref(Exception.class));
            org.mule.devkit.model.code.Variable destroyException = logException.param("e");
            logException.body().add(logger.invoke("error").arg(destroyException.invoke("getMessage")).arg(destroyException));

            Conditional ifRetryMaxNotReached = catchBlock.body()._if(Op.lte(retryCount.invoke("get"), retryMax));
            ifDebugEnabled = ifRetryMaxNotReached._then()._if(logger.invoke("isDebugEnabled"));
            messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "_messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Forcing a retry [time="));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(retryCount));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(" out of  "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(retryMax));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            ifRetryMaxNotReached._then()._return(ExpressionFactory.invoke("process").arg(event));

            org.mule.devkit.model.code.Variable invalidConnection = catchBlock.param("invalidConnection");
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToInvoke");
            if (methodName != null) {
                failedToInvoke.arg(ExpressionFactory.lit(methodName));
            }
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            if (event != null) {
                messageException.arg(event);
            }
            messageException.arg(invalidConnection);
            catchBlock.body()._throw(messageException);
        }

        generateThrow("failedToInvoke", MessagingException.class,
                callProcessor._catch(ref(Exception.class)), event, methodName);

        if (poolObjectClass != null) {
            Block fin = callProcessor._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(moduleObject.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }

        if (connectMethod != null) {
            Block fin = callProcessor._finally();

            TryStatement tryToReleaseConnection = fin._try();

            Conditional ifConnectionNotNull = tryToReleaseConnection.body()._if(Op.ne(connection, ExpressionFactory._null()));


            Conditional ifDebugEnabled = ifConnectionNotNull._then()._if(logger.invoke("isDebugEnabled"));
            org.mule.devkit.model.code.Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "_messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Releasing the connection back into the pool [id="));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));


            DefinedClass connectionKey = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTION_PARAMETERS, ref(executableElement.parent()));
            Invocation newKey = ExpressionFactory._new(connectionKey);
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }

            Invocation returnConnection = moduleObject.invoke("releaseConnection");
            returnConnection.arg(newKey);
            returnConnection.arg(connection);

            ifConnectionNotNull._then().add(returnConnection);

            generateThrow("failedToInvoke", MessagingException.class,
                    tryToReleaseConnection._catch(ref(Exception.class)), event, methodName);
        }

    }

    private org.mule.devkit.model.code.Variable declareStandardParameter(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, org.mule.devkit.model.code.Variable muleMessage, TryStatement callProcessor, List<Expression> parameters, org.mule.devkit.model.code.Variable outboundHeadersMap, Variable variable, String fieldName, FieldVariable muleContext) {
        InboundHeaders inboundHeaders = variable.getAnnotation(InboundHeaders.class);
        OutboundHeaders outboundHeaders = variable.getAnnotation(OutboundHeaders.class);
        InvocationHeaders invocationHeaders = variable.getAnnotation(InvocationHeaders.class);
        SessionHeaders sessionHeaders = variable.getAnnotation(SessionHeaders.class);
        Payload payload = variable.getAnnotation(Payload.class);
        ExceptionPayload exceptionPayload = variable.getAnnotation(ExceptionPayload.class);
        CorrelationId correlationId = variable.getAnnotation(CorrelationId.class);
        CorrelationSequence correlationSequence = variable.getAnnotation(CorrelationSequence.class);
        CorrelationGroupSize correlationGroupSize = variable.getAnnotation(CorrelationGroupSize.class);
        MessageRootId messageRootId = variable.getAnnotation(MessageRootId.class);
        MessageUniqueId messageUniqueId = variable.getAnnotation(MessageUniqueId.class);

        if (outboundHeaders == null) {
            org.mule.devkit.model.code.Type type = ref(fields.get(fieldName).getVariable().asType()).boxify();
            String name = "_transformed" + StringUtils.capitalize(fieldName);
            Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                    ExpressionFactory.lit(fields.get(fieldName).getFieldType().name())
            ).invoke("getGenericType");
            Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(getGenericType);
            
            Mime mime = fields.get(fieldName).getVariable().getAnnotation(Mime.class);
            if( mime != null ) {
                evaluateAndTransform.arg(ExpressionFactory.lit(mime.value()));
            } else {
                evaluateAndTransform.arg(ExpressionFactory._null());
            }

            if (inboundHeaders != null) {
                if (fields.get(fieldName).getVariable().isArrayOrList()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersListExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                } else if (fields.get(fieldName).getVariable().isMap()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                } else {
                    evaluateAndTransform.arg("#[" + MessageHeaderExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                }
            } else if (invocationHeaders != null) {
                if (fields.get(fieldName).getVariable().isArrayOrList()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersListExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                } else if (fields.get(fieldName).getVariable().isMap()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                } else {
                    evaluateAndTransform.arg("#[" + MessageHeaderExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                }
            } else if (sessionHeaders != null) {
                if (fields.get(fieldName).getVariable().isArrayOrList()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersListExpressionEvaluator.NAME + ":SESSION:" + sessionHeaders.value() + "]");
                } else if (fields.get(fieldName).getVariable().isMap()) {
                    evaluateAndTransform.arg("#[" + MessageHeadersExpressionEvaluator.NAME + ":SESSION:" + sessionHeaders.value() + "]");
                } else {
                    evaluateAndTransform.arg("#[" + MessageHeaderExpressionEvaluator.NAME + ":SESSION:" + sessionHeaders.value() + "]");
                }
            } else if (payload != null) {
                evaluateAndTransform.arg("#[payload]");
            } else if (exceptionPayload != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getExceptionPayload"));
            } else if (correlationId != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getCorrelationId"));
            } else if (correlationSequence != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getCorrelationSequence"));
            } else if (correlationGroupSize != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getCorrelationGroupSize"));
            } else if (messageRootId != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getMessageRootId"));
            } else if (messageUniqueId != null) {
                evaluateAndTransform.arg(muleMessage.invoke("getUniqueId"));
            } else {
                evaluateAndTransform.arg(fields.get(fieldName).getField());
            }

            Cast cast = ExpressionFactory.cast(type, evaluateAndTransform);

            org.mule.devkit.model.code.Variable transformed = callProcessor.body().decl(type, name, cast);
            parameters.add(transformed);
        } else {
            org.mule.devkit.model.code.Type type = ref(HashMap.class).narrow(ref(String.class), ref(Object.class));
            String name = "_transformed" + StringUtils.capitalize(fieldName);

            outboundHeadersMap = callProcessor.body().decl(type, name, ExpressionFactory._new(type));
            parameters.add(outboundHeadersMap);
        }
        return outboundHeadersMap;
    }

    private void declareNestedProcessorParameter(Map<String, FieldVariableElement> fields, FieldVariable muleContext, org.mule.devkit.model.code.Variable event, TryStatement callProcessor, List<Expression> parameters, Variable variable, String fieldName) {
        DefinedClass callbackClass = ctx().getCodeModel()._class(DefinedClassRoles.NESTED_PROCESSOR_CHAIN);
        DefinedClass stringCallbackClass = ctx().getCodeModel()._class(DefinedClassRoles.NESTED_PROCESSOR_STRING);

        boolean isList = variable.isArrayOrList();

        if (!isList) {
            org.mule.devkit.model.code.Variable transformed = callProcessor.body().decl(ref(NestedProcessor.class), "_transformed" + StringUtils.capitalize(fieldName),
                    ExpressionFactory._null());

            Conditional ifMessageProcessor = callProcessor.body()._if(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(MessageProcessor.class))));

            ifMessageProcessor._then()
                    .assign(transformed,
                            ExpressionFactory._new(callbackClass).arg(event).arg(muleContext).arg(
                                    ExpressionFactory.cast(ref(MessageProcessor.class), fields.get(fieldName).getField())));

            Conditional ifString = ifMessageProcessor._elseif(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(String.class))));

            ifString._then()
                    .assign(transformed,
                            ExpressionFactory._new(stringCallbackClass).arg(
                                    ExpressionFactory.cast(ref(String.class), fields.get(fieldName).getField())
                            ));

            parameters.add(transformed);
        } else {
            org.mule.devkit.model.code.Variable transformed = callProcessor.body().decl(ref(List.class).narrow(NestedProcessor.class), "_transformed" + StringUtils.capitalize(fieldName),
                    ExpressionFactory._new(ref(ArrayList.class).narrow(NestedProcessor.class)));

            Conditional ifMessageProcessor = callProcessor.body()._if(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(List.class))));

            ForEach forEachProcessor = ifMessageProcessor._then().forEach(ref(MessageProcessor.class),
                    "messageProcessor",
                    ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class),
                            fields.get(fieldName).getField()));
            forEachProcessor.body().add(transformed.invoke("add").arg(
                    ExpressionFactory._new(callbackClass).arg(event).arg(muleContext).arg(
                            forEachProcessor.var())
            ));

            Conditional ifString = ifMessageProcessor._elseif(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(String.class))));

            ifString._then()
                    .add(transformed.invoke("add").arg(
                            ExpressionFactory._new(stringCallbackClass).arg(
                                    ExpressionFactory.cast(ref(String.class), fields.get(fieldName).getField())
                            )));

            parameters.add(transformed);
        }
    }

    private Map<String, Expression> declareConnectionParametersVariables(Method executableElement, Map<String, FieldVariableElement> connectionFields, org.mule.devkit.model.code.Method process) {
        Map<String, Expression> connectionParameters = new HashMap<String, Expression>();
        Method connectMethod = connectForMethod(executableElement);
        if (connectMethod != null) {
            for (Parameter variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                org.mule.devkit.model.code.Type type = ref(connectionFields.get(fieldName).getVariable().asType()).boxify();
                String name = "_transformed" + StringUtils.capitalize(fieldName);

                org.mule.devkit.model.code.Variable transformed = process.body().decl(type, name, ExpressionFactory._null());
                connectionParameters.put(fieldName, transformed);
            }
        }
        return connectionParameters;
    }

    private org.mule.devkit.model.code.Variable addConnectionVariableIfNeeded(Method executableElement, org.mule.devkit.model.code.Method process) {
        Method connectMethod = connectForMethod(executableElement);
        if (connectForMethod(executableElement) != null) {
            DefinedClass connectionClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connectMethod.parent()));
            return process.body().decl(connectionClass, "connection", ExpressionFactory._null());
        }
        return null;
    }

    private org.mule.devkit.model.code.Variable declarePoolObjectIfClassNotNull(DefinedClass poolObjectClass, org.mule.devkit.model.code.Method process) {
        if (poolObjectClass != null) {
            return process.body().decl(poolObjectClass, "_poolObject", ExpressionFactory._null());
        }
        return null;
    }

    private org.mule.devkit.model.code.Variable generateMethodCall(Block body, org.mule.devkit.model.code.Variable object, String methodName, List<Expression> parameters, org.mule.devkit.model.code.Variable event, org.mule.devkit.model.code.Type returnType, org.mule.devkit.model.code.Variable poolObject, org.mule.devkit.model.code.Variable interceptCallback, FieldVariable messageProcessorListener) {
        org.mule.devkit.model.code.Variable resultPayload = null;
        if (returnType != ctx().getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }

        Invocation methodCall;
        if (poolObject != null) {
            body.assign(poolObject, ExpressionFactory.cast(poolObject.type(), object.invoke("getLifecyleEnabledObjectPool").invoke("borrowObject")));
            methodCall = poolObject.invoke(methodName);
        } else {
            methodCall = object.invoke(methodName);
        }

        for (Expression parameter : parameters) {
            methodCall.arg(parameter);
        }

        if (returnType != ctx().getCodeModel().VOID) {
            body.assign(resultPayload, methodCall);
        } else {
            body.add(methodCall);
        }

        Block scope = body;
        if (interceptCallback != null) {
            Conditional shallContinue = body._if(Op.cand(interceptCallback.invoke("getShallContinue"),
                    Op.ne(messageProcessorListener, ExpressionFactory._null())));

            shallContinue._then().assign(event, messageProcessorListener.invoke("process").arg(event));

            scope = shallContinue._else();
        }

        if (returnType != ctx().getCodeModel().VOID) {
            generatePayloadOverwrite(scope, event, resultPayload);
        }

        return resultPayload;
    }

    private void generatePayloadOverwrite(Block block, org.mule.devkit.model.code.Variable event, org.mule.devkit.model.code.Variable resultPayload) {
        Invocation applyTransformers = event.invoke("getMessage").invoke("applyTransformers");
        applyTransformers.arg(event);
        Invocation newTransformerTemplate = ExpressionFactory._new(ref(TransformerTemplate.class));

        org.mule.devkit.model.code.Variable overwritePayloadCallback = block.decl(ref(TransformerTemplate.OverwitePayloadCallback.class), "overwritePayloadCallback", ExpressionFactory._null());

        Conditional ifPayloadIsNull = block._if(resultPayload.eq(ExpressionFactory._null()));

        Invocation newOverwritePayloadCallback = ExpressionFactory._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallback.arg(resultPayload);
        Invocation newOverwritePayloadCallbackWithNull = ExpressionFactory._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallbackWithNull.arg(ref(NullPayload.class).staticInvoke("getInstance"));
        ifPayloadIsNull._else().assign(overwritePayloadCallback, newOverwritePayloadCallback);
        ifPayloadIsNull._then().assign(overwritePayloadCallback, newOverwritePayloadCallbackWithNull);

        newTransformerTemplate.arg(overwritePayloadCallback);

        org.mule.devkit.model.code.Variable transformerList = block.decl(ref(List.class).narrow(Transformer.class), "transformerList");
        block.assign(transformerList, ExpressionFactory._new(ref(ArrayList.class).narrow(Transformer.class)));
        block.add(transformerList.invoke("add").arg(newTransformerTemplate));

        applyTransformers.arg(transformerList);
        block.add(applyTransformers);
    }

    private DefinedClass getMessageProcessorClass(Method processorMethod) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(processorMethod.parent().getPackageName() + NamingConstants.MESSAGE_PROCESSOR_NAMESPACE);
        DefinedClass abstractExpressionEvaluator = ctx().getCodeModel()._class(DefinedClassRoles.ABSTRACT_EXPRESSION_EVALUATOR);
        DefinedClass clazz = pkg._class(processorMethod.getCapitalizedName() + NamingConstants.MESSAGE_PROCESSOR_CLASS_NAME_SUFFIX, abstractExpressionEvaluator, new Class[]{
                Initialisable.class,
                Startable.class,
                Disposable.class,
                Stoppable.class,
                MessageProcessor.class,
                MuleContextAware.class,
                FlowConstructAware.class});

        clazz.role(DefinedClassRoles.MESSAGE_PROCESSOR, ref(processorMethod.parent()), processorMethod.getSimpleName().toString());

        return clazz;
    }

    private DefinedClass getInterceptingMessageProcessorClass(Method processorMethod) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(processorMethod.parent().getPackageName() + NamingConstants.MESSAGE_PROCESSOR_NAMESPACE);
        DefinedClass abstractExpressionEvaluator = ctx().getCodeModel()._class(DefinedClassRoles.ABSTRACT_EXPRESSION_EVALUATOR);
        DefinedClass clazz = pkg._class(processorMethod.getCapitalizedName() + NamingConstants.MESSAGE_PROCESSOR_CLASS_NAME_SUFFIX, abstractExpressionEvaluator, new Class[]{
                Initialisable.class,
                Startable.class,
                Disposable.class,
                Stoppable.class,
                InterceptingMessageProcessor.class,
                MuleContextAware.class,
                FlowConstructAware.class,
                SourceCallback.class});

        clazz.role(DefinedClassRoles.MESSAGE_PROCESSOR, ref(processorMethod.parent()), processorMethod.getSimpleName().toString());

        return clazz;
    }

}