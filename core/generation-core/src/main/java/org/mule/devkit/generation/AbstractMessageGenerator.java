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

package org.mule.devkit.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.Flow;
import org.mule.devkit.generation.callback.DefaultHttpCallbackGenerator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.code.Block;
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
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.session.DefaultMuleSession;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

import javax.lang.model.type.TypeMirror;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMessageGenerator extends AbstractModuleGenerator {

    protected FieldVariable generateFieldForPatternInfo(DefinedClass messageProcessorClass) {
        FieldVariable patternInfo = messageProcessorClass.field(Modifier.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");
        patternInfo.javadoc().add("Mule Pattern Info");
        return patternInfo;
    }

    protected FieldVariable generateFieldForExpressionManager(DefinedClass messageProcessorClass) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(ExpressionManager.class), "expressionManager");
        expressionManager.javadoc().add("Mule Expression Manager");
        return expressionManager;
    }

    protected FieldVariable generateFieldForMessageProcessor(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), name);
        expressionManager.javadoc().add("Message Processor");
        return expressionManager;
    }

    protected FieldVariable generateFieldForBoolean(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ctx().getCodeModel().BOOLEAN, name);
        return expressionManager;
    }

    protected FieldVariable generateFieldForString(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(String.class), name);
        return expressionManager;
    }

    protected FieldVariable generateFieldForModuleObject(DefinedClass messageProcessorClass, Type type) {
        FieldVariable field = messageProcessorClass.field(Modifier.PRIVATE, ref(Object.class), "moduleObject");
        field.javadoc().add("Module object");

        return field;
    }

    protected FieldVariable generateFieldForMessageProcessorListener(DefinedClass messageSourceClass) {
        FieldVariable messageProcessor = messageSourceClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), "messageProcessor");
        messageProcessor.javadoc().add("Message processor that will get called for processing incoming events");
        return messageProcessor;
    }

    protected DefinedClass getBeanDefinitionParserClass(Method executableElement) {
        Package pkg = ctx().getCodeModel()._package(executableElement.parent().getPackageName() + NamingConstants.CONFIG_NAMESPACE);
        DefinedClass abstractBeanDefinitionParser = ctx().getCodeModel()._class(DefinedClassRoles.ABSTRACT_BEAN_DEFINITION_PARSER);
        DefinedClass clazz = pkg._class(executableElement.getCapitalizedName() + NamingConstants.DEFINITION_PARSER_CLASS_NAME_SUFFIX, abstractBeanDefinitionParser);

        return clazz;
    }

    protected DefinedClass getConfigBeanDefinitionParserClass(Type type) {
        DefinedClass abstractBeanDefinitionParser = ctx().getCodeModel()._class(DefinedClassRoles.ABSTRACT_BEAN_DEFINITION_PARSER);
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.CONFIG_NAMESPACE);
        DefinedClass clazz = pkg._class(type.getClassName() + NamingConstants.CONFIG_DEFINITION_PARSER_CLASS_NAME_SUFFIX, abstractBeanDefinitionParser);

        clazz.role(DefinedClassRoles.CONFIG_BEAN_DEFINITION_PARSER, ref(type));

        return clazz;
    }

    protected Map<String, FieldVariableElement> generateProcessorFieldForEachParameter(DefinedClass messageProcessorClass, Method processorMethod) {
        return generateProcessorFieldForEachParameter(messageProcessorClass, processorMethod, null);
    }

    protected Map<String, FieldVariableElement> generateProcessorFieldForEachParameter(DefinedClass messageProcessorClass, Method processorMethod, Class annotatedWith) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (Parameter variable : processorMethod.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                continue;
            }

            if (annotatedWith != null && variable.getAnnotation(annotatedWith) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            FieldVariable field;
            FieldVariable fieldType;
            if (variable.isNestedProcessor()) {
                field = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(Object.class).
                        name(fieldName).
                        build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(ref(variable.asType())).
                        name("_" + fieldName + "Type").
                        build();
            } else if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                // for each parameter of type HttpCallback we need two fields: one that will hold a reference to the flow
                // that is going to be executed upon the callback and the other one to hold the HttpCallback object itself
                field = new FieldBuilder(messageProcessorClass).
                        type(Flow.class).
                        name(fieldName + "CallbackFlow").
                        javadoc("The flow to be invoked when the http callback is received").build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        type(HttpCallback.class).
                        name(fieldName).
                        javadoc("An HttpCallback instance responsible for linking the APIs http callback with the flow {@link " + messageProcessorClass.fullName() + "#" + fieldName + "CallbackFlow").build();
            } else {
                field = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(Object.class).
                        name(fieldName).
                        build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(ref(variable.asType())).
                        name("_" + fieldName + "Type").
                        build();
            }
            fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, fieldType, variable));
        }
        return fields;
    }

    protected Map<String, FieldVariableElement> generateStandardFieldForEachParameter(DefinedClass messageProcessorClass, Method processorMethod) {
        return generateStandardFieldForEachParameter(messageProcessorClass, processorMethod, null);
    }

    protected Map<String, FieldVariableElement> generateStandardFieldForEachParameter(DefinedClass messageProcessorClass, Method processorMethod, Class annotatedWith) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (Parameter variable : processorMethod.getParameters()) {
            if (annotatedWith != null && variable.getAnnotation(annotatedWith) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            FieldVariable field = null;
            FieldVariable fieldType = null;
            field = new FieldBuilder(messageProcessorClass).
                    privateVisibility().
                    type(ref(variable.asType())).
                    name(fieldName).
                    build();
            field.javadoc().add(variable.getJavaDocParameterSummary(variable.getSimpleName().toString()));
            fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, fieldType, variable));
        }
        return fields;
    }

    protected org.mule.devkit.model.code.Method generateInitialiseMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Type type, FieldVariable muleContext, FieldVariable object, FieldVariable retryCount, boolean shouldAutoCreate) {
        DefinedClass pojoClass = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));

        org.mule.devkit.model.code.Method initialise = messageProcessorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "initialise");
        initialise.javadoc().add("Obtains the expression manager from the Mule context and initialises the connector. If a target object ");
        initialise.javadoc().add(" has not been set already it will search the Mule registry for a default one.");
        initialise.javadoc().addThrows(ref(InitialisationException.class));
        initialise._throws(InitialisationException.class);

        if (retryCount != null) {
            initialise.body().assign(retryCount, ExpressionFactory._new(ref(AtomicInteger.class)));
        }

        if (object != null) {
            Conditional ifNoObject = initialise.body()._if(Op.eq(object, ExpressionFactory._null()));
            TryStatement tryLookUp = ifNoObject._then()._try();
            tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.dotclass(pojoClass)));
            Conditional ifObjectNoFound = tryLookUp.body()._if(Op.eq(object, ExpressionFactory._null()));
            if (shouldAutoCreate) {
                ifObjectNoFound._then().assign(object, ExpressionFactory._new(pojoClass));
                ifObjectNoFound._then().add(muleContext.invoke("getRegistry").invoke("registerObject").arg(pojoClass.dotclass().invoke("getName")).arg(object));
            } else {
                ifObjectNoFound._then()._throw(ExpressionFactory._new(ref(InitialisationException.class)).
                        arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").
                                arg("Cannot find object")).arg(ExpressionFactory._this()));
            }
            CatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class));
            org.mule.devkit.model.code.Variable exception = catchBlock.param("e");
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
            failedToInvoke.arg(pojoClass.fullName());
            Invocation messageException = ExpressionFactory._new(ref(InitialisationException.class));
            messageException.arg(failedToInvoke);
            messageException.arg(exception);
            messageException.arg(ExpressionFactory._this());
            catchBlock.body()._throw(messageException);
        }

        Conditional ifObjectIsString = initialise.body()._if(Op._instanceof(object, ref(String.class)));
        ifObjectIsString._then().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.cast(ref(String.class), object)));
        ifObjectIsString._then()._if(Op.eq(object, ExpressionFactory._null()))._then().
                _throw(ExpressionFactory._new(ref(InitialisationException.class)).
                        arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").
                                arg("Cannot find object by config name")).arg(ExpressionFactory._this()));

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifInitialisable = initialise.body()._if(Op._instanceof(variableElement.getField(), ref(Initialisable.class)));
                        ifInitialisable._then().add(
                                ExpressionFactory.cast(ref(Initialisable.class), variableElement.getField()).invoke("initialise")
                        );
                    } else {
                        Conditional ifIsList = initialise.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifInitialisable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Initialisable.class)));
                        ifInitialisable._then().add(
                                ExpressionFactory.cast(ref(Initialisable.class), forEachProcessor.var()).invoke("initialise")
                        );
                    }
                } else if (variableElement.getVariable().asType().toString().startsWith(HttpCallback.class.getName())) {
                    FieldVariable callbackFlowName = fields.get(fieldName).getField();
                    Block ifCallbackFlowNameIsNull = initialise.body()._if(Op.ne(callbackFlowName, ExpressionFactory._null()))._then();
                    org.mule.devkit.model.code.Variable castedModuleObject = ifCallbackFlowNameIsNull.decl(pojoClass, "castedModuleObject", ExpressionFactory.cast(pojoClass, object));
                    Invocation domain = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.DOMAIN_FIELD_NAME));
                    Invocation localPort = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.LOCAL_PORT_FIELD_NAME));
                    Invocation remotePort = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.REMOTE_PORT_FIELD_NAME));
                    Invocation async = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.ASYNC_FIELD_NAME));
                    ifCallbackFlowNameIsNull.assign(variableElement.getFieldType(), ExpressionFactory._new(ctx().getCodeModel()._class(DefinedClassRoles.DEFAULT_HTTP_CALLBACK)).
                            arg(callbackFlowName).arg(muleContext).arg(domain).arg(localPort).arg(remotePort).arg(async));
                }
            }
        }

        return initialise;
    }

    protected org.mule.devkit.model.code.Method generateSetMuleContextMethod(DefinedClass clazz, FieldVariable muleContext) {
        return generateSetMuleContextMethod(clazz, muleContext, null);
    }

    protected org.mule.devkit.model.code.Method generateSetMuleContextMethod(DefinedClass clazz, FieldVariable muleContext, Map<String, FieldVariableElement> fields) {
        org.mule.devkit.model.code.Method setMuleContext = clazz.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setMuleContext");
        setMuleContext.javadoc().add("Set the Mule context");
        setMuleContext.javadoc().addParam("context Mule context to set");
        org.mule.devkit.model.code.Variable muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(ExpressionFactory._this().ref(muleContext), muleContextParam);

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifMuleContextAware = setMuleContext.body()._if(Op._instanceof(variableElement.getField(), ref(MuleContextAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(MuleContextAware.class), variableElement.getField()).invoke("setMuleContext").arg(muleContextParam)
                        );
                    } else {
                        Conditional ifIsList = setMuleContext.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifMuleContextAware = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(MuleContextAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(MuleContextAware.class), forEachProcessor.var()).invoke("setMuleContext").arg(muleContextParam)
                        );
                    }
                }
            }
        }

        return setMuleContext;
    }

    protected org.mule.devkit.model.code.Method generateSetFlowConstructMethod(DefinedClass messageSourceClass, FieldVariable flowConstruct) {
        return generateSetFlowConstructMethod(messageSourceClass, flowConstruct, null);
    }

    protected org.mule.devkit.model.code.Method generateSetFlowConstructMethod(DefinedClass messageSourceClass, FieldVariable flowConstruct, Map<String, FieldVariableElement> fields) {
        org.mule.devkit.model.code.Method setFlowConstruct = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setFlowConstruct");
        setFlowConstruct.javadoc().add("Sets flow construct");
        setFlowConstruct.javadoc().addParam("flowConstruct Flow construct to set");
        org.mule.devkit.model.code.Variable newFlowConstruct = setFlowConstruct.param(ref(FlowConstruct.class), "flowConstruct");
        setFlowConstruct.body().assign(ExpressionFactory._this().ref(flowConstruct), newFlowConstruct);

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifMuleContextAware = setFlowConstruct.body()._if(Op._instanceof(variableElement.getField(), ref(FlowConstructAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(FlowConstructAware.class), variableElement.getField()).invoke("setFlowConstruct").arg(newFlowConstruct)
                        );
                    } else {
                        Conditional ifIsList = setFlowConstruct.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifMuleContextAware = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(FlowConstructAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(FlowConstructAware.class), forEachProcessor.var()).invoke("setFlowConstruct").arg(newFlowConstruct)
                        );

                    }
                }
            }
        }

        return setFlowConstruct;
    }


    protected void findConfig(Block block, FieldVariable muleContext, FieldVariable object, String methodName, org.mule.devkit.model.code.Variable event, DefinedClass moduleObjectClass, org.mule.devkit.model.code.Variable moduleObject) {
        Conditional ifObjectIsString = block._if(Op._instanceof(object, ref(String.class)));
        ifObjectIsString._else().assign(moduleObject, ExpressionFactory.cast(moduleObjectClass, object));
        ifObjectIsString._then().assign(moduleObject, ExpressionFactory.cast(moduleObjectClass, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.cast(ref(String.class), object))));

        TypeReference coreMessages = ref(CoreMessages.class);
        Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
        if (methodName != null) {
            failedToInvoke.arg(ExpressionFactory.lit(methodName));
        }
        Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        } else {
            messageException.arg(ExpressionFactory.cast(ref(MuleEvent.class), ExpressionFactory._null()));
        }
        messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("Cannot find the configuration specified by the config-ref attribute."));

        ifObjectIsString._then()._if(Op.eq(moduleObject, ExpressionFactory._null()))._then()._throw(messageException);
    }

    protected FieldVariable generateFieldForFlowConstruct(DefinedClass messageSourceClass) {
        FieldVariable flowConstruct = messageSourceClass.field(Modifier.PRIVATE, ref(FlowConstruct.class), "flowConstruct");
        flowConstruct.javadoc().add("Flow construct");
        return flowConstruct;
    }

    protected FieldVariable generateRetryCountField(DefinedClass messageSourceClass) {
        FieldVariable retryCount = messageSourceClass.field(Modifier.PRIVATE, ref(AtomicInteger.class), "retryCount");
        retryCount.javadoc().add("Variable used to track how many retries we have attempted on this message processor");
        return retryCount;
    }

    protected FieldVariable generateRetryMaxField(DefinedClass messageSourceClass) {
        FieldVariable retryMax = messageSourceClass.field(Modifier.PRIVATE, ctx().getCodeModel().INT, "retryMax");
        retryMax.javadoc().add("Maximum number of retries that can be attempted.");
        return retryMax;
    }


    protected org.mule.devkit.model.code.Method generateSetModuleObjectMethod(DefinedClass messageProcessorClass, FieldVariable object) {
        org.mule.devkit.model.code.Method setObject = messageProcessorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setModuleObject");
        setObject.javadoc().add("Sets the instance of the object under which the processor will execute");
        setObject.javadoc().addParam("moduleObject Instace of the module");
        org.mule.devkit.model.code.Variable objectParam = setObject.param(object.type(), "moduleObject");
        setObject.body().assign(ExpressionFactory._this().ref(object), objectParam);

        return setObject;
    }

    protected void generateTransform(Block block, org.mule.devkit.model.code.Variable transformedField, org.mule.devkit.model.code.Variable evaluatedField, TypeMirror expectedType, FieldVariable muleContext) {
        Invocation isAssignableFrom = ExpressionFactory.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        Conditional ifNotIsAssignableFrom = block._if(Op.not(isAssignableFrom));
        Block isNotAssignable = ifNotIsAssignableFrom._then();
        org.mule.devkit.model.code.Variable dataTypeSource = isNotAssignable.decl(ref(DataType.class), "source");
        org.mule.devkit.model.code.Variable dataTypeTarget = isNotAssignable.decl(ref(DataType.class), "target");

        isNotAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isNotAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).staticInvoke("create").arg(ExpressionFactory.dotclass(ref(expectedType).boxify())));

        org.mule.devkit.model.code.Variable transformer = isNotAssignable.decl(ref(Transformer.class), "t");
        Invocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isNotAssignable.assign(transformer, lookupTransformer);
        isNotAssignable.assign(transformedField, ExpressionFactory.cast(ref(expectedType).boxify(), transformer.invoke("transform").arg(evaluatedField)));

        Block notAssignable = ifNotIsAssignableFrom._else();
        notAssignable.assign(transformedField, ExpressionFactory.cast(ref(expectedType).boxify(), evaluatedField));
    }


    protected org.mule.devkit.model.code.Method generateSetListenerMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor) {
        org.mule.devkit.model.code.Method setListener = messageSourceClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setListener");
        setListener.javadoc().add("Sets the message processor that will \"listen\" the events generated by this message source");
        setListener.javadoc().addParam("listener Message processor");
        org.mule.devkit.model.code.Variable listener = setListener.param(ref(MessageProcessor.class), "listener");
        setListener.body().assign(ExpressionFactory._this().ref(messageProcessor), listener);

        return setListener;
    }

    protected void generateThrow(String bundle, Class<?> clazz, CatchBlock callProcessorCatch, Expression event, String methodName) {
        org.mule.devkit.model.code.Variable exception = callProcessorCatch.param("e");
        TypeReference coreMessages = ref(CoreMessages.class);
        Invocation failedToInvoke = coreMessages.staticInvoke(bundle);
        if (methodName != null) {
            failedToInvoke.arg(ExpressionFactory.lit(methodName));
        }
        Invocation messageException = ExpressionFactory._new(ref(clazz));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        }
        messageException.arg(exception);
        callProcessorCatch.body()._throw(messageException);
    }

    protected class FieldVariableElement {
        private final FieldVariable field;
        private final FieldVariable fieldType;
        private final Variable variable;

        public FieldVariableElement(FieldVariable field, FieldVariable fieldType, Variable variable) {
            this.field = field;
            this.fieldType = fieldType;
            this.variable = variable;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((field == null) ? 0 : field.hashCode());
            result = prime * result + ((fieldType == null) ? 0 : fieldType.hashCode());
            result = prime * result + ((variable == null) ? 0 : variable.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FieldVariableElement other = (FieldVariableElement) obj;
            if (field == null) {
                if (other.field != null) {
                    return false;
                }
            } else if (!field.equals(other.field)) {
                return false;
            }
            if (fieldType == null) {
                if (other.fieldType != null) {
                    return false;
                }
            } else if (!fieldType.equals(other.fieldType)) {
                return false;
            }
            if (variable == null) {
                if (other.variable != null) {
                    return false;
                }
            } else if (!variable.equals(other.variable)) {
                return false;
            }
            return true;
        }

        public FieldVariable getField() {
            return field;
        }

        public FieldVariable getFieldType() {
            return fieldType;
        }

        public Variable getVariable() {
            return variable;
        }
    }

    protected void generateSourceCallbackProcessMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        org.mule.devkit.model.code.Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process(org.mule.api.MuleEvent)}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));
        org.mule.devkit.model.code.Variable message = process.param(ref(Object.class), "message");

        org.mule.devkit.model.code.Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(message);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        org.mule.devkit.model.code.Variable muleSession = process.body().decl(ref(MuleSession.class), "muleSession");
        Invocation newMuleSession = ExpressionFactory._new(ref(DefaultMuleSession.class));
        newMuleSession.arg(flowConstruct);
        newMuleSession.arg(muleContext);
        process.body().assign(muleSession, newMuleSession);

        org.mule.devkit.model.code.Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));
        newMuleEvent.arg(muleSession);
        process.body().assign(muleEvent, newMuleEvent);

        TryStatement tryBlock = process.body()._try();
        org.mule.devkit.model.code.Variable responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        Invocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(muleEvent);
        tryBlock.body().assign(responseEvent, messageProcess);
        Conditional ifResponse = tryBlock.body()._if(
                Op.cand(Op.ne(responseEvent, ExpressionFactory._null()),
                        Op.ne(responseEvent.invoke("getMessage"), ExpressionFactory._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        org.mule.devkit.model.code.Variable exception = catchException.param("e");
        catchException.body()._throw(exception);

        process.body()._return(ExpressionFactory._null());
    }

    protected void generateSourceCallbackProcessMethodWithNoPayload(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        org.mule.devkit.model.code.Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process()}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));

        TryStatement tryBlock = process.body()._try();
        org.mule.devkit.model.code.Variable responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        Invocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(ref(RequestContext.class).staticInvoke("getEvent"));
        tryBlock.body().assign(responseEvent, messageProcess);
        Conditional ifResponse = tryBlock.body()._if(
                Op.cand(Op.ne(responseEvent, ExpressionFactory._null()),
                        Op.ne(responseEvent.invoke("getMessage"), ExpressionFactory._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        org.mule.devkit.model.code.Variable exception = catchException.param("e");
        catchException.body()._throw(exception);

        process.body()._return(ExpressionFactory._null());
    }


    protected void generateSourceCallbackProcessWithPropertiesMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        org.mule.devkit.model.code.Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process(org.mule.api.MuleEvent)}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));
        org.mule.devkit.model.code.Variable message = process.param(ref(Object.class), "message");
        org.mule.devkit.model.code.Variable properties = process.param(ref(Map.class).narrow(String.class).narrow(Object.class), "properties");

        org.mule.devkit.model.code.Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(message);
        newMuleMessage.arg(properties);
        newMuleMessage.arg(ExpressionFactory._null());
        newMuleMessage.arg(ExpressionFactory._null());
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        org.mule.devkit.model.code.Variable muleSession = process.body().decl(ref(MuleSession.class), "muleSession");
        Invocation newMuleSession = ExpressionFactory._new(ref(DefaultMuleSession.class));
        newMuleSession.arg(flowConstruct);
        newMuleSession.arg(muleContext);
        process.body().assign(muleSession, newMuleSession);

        org.mule.devkit.model.code.Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));
        newMuleEvent.arg(muleSession);
        process.body().assign(muleEvent, newMuleEvent);

        TryStatement tryBlock = process.body()._try();
        org.mule.devkit.model.code.Variable responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        Invocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(muleEvent);
        tryBlock.body().assign(responseEvent, messageProcess);
        Conditional ifResponse = tryBlock.body()._if(
                Op.cand(Op.ne(responseEvent, ExpressionFactory._null()),
                        Op.ne(responseEvent.invoke("getMessage"), ExpressionFactory._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        org.mule.devkit.model.code.Variable exception = catchException.param("e");
        catchException.body()._throw(exception);

        process.body()._return(ExpressionFactory._null());
    }


    protected void generateStartMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        org.mule.devkit.model.code.Method startMethod = messageProcessorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "start");
        startMethod._throws(ref(MuleException.class));

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifStartable = startMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Startable.class)));
                        ifStartable._then().add(
                                ExpressionFactory.cast(ref(Startable.class), variableElement.getField()).invoke("start")
                        );
                    } else {
                        Conditional ifIsList = startMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifStartable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Startable.class)));
                        ifStartable._then().add(
                                ExpressionFactory.cast(ref(Startable.class), forEachProcessor.var()).invoke("start")
                        );
                    }
                } else if (variableElement.getVariable().asType().toString().startsWith(HttpCallback.class.getName())) {
                    startMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "start");
                }
            }
        }
    }

    protected void generateStopMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        org.mule.devkit.model.code.Method stopMethod = messageProcessorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "stop");
        stopMethod._throws(ref(MuleException.class));

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifStoppable = stopMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Stoppable.class)));
                        ifStoppable._then().add(
                                ExpressionFactory.cast(ref(Stoppable.class), variableElement.getField()).invoke("stop")
                        );
                    } else {
                        Conditional ifIsList = stopMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor",
                                ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifStoppable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Stoppable.class)));
                        ifStoppable._then().add(
                                ExpressionFactory.cast(ref(Stoppable.class), forEachProcessor.var()).invoke("stop")
                        );
                    }
                } else if (variableElement.getVariable().asType().toString().startsWith(HttpCallback.class.getName())) {
                    stopMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "stop");
                }
            }
        }
    }

    protected void generateDiposeMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        org.mule.devkit.model.code.Method diposeMethod = messageProcessorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "dispose");

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (variableElement.getVariable().isNestedProcessor()) {
                    boolean isList = variableElement.getVariable().isArrayOrList();

                    if (!isList) {
                        Conditional ifDisposable = diposeMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Disposable.class)));
                        ifDisposable._then().add(
                                ExpressionFactory.cast(ref(Disposable.class), variableElement.getField()).invoke("dispose")
                        );
                    } else {
                        Conditional ifIsList = diposeMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifDisposable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Disposable.class)));
                        ifDisposable._then().add(
                                ExpressionFactory.cast(ref(Disposable.class), forEachProcessor.var()).invoke("dispose")
                        );
                    }
                }
            }


        }
    }
}
