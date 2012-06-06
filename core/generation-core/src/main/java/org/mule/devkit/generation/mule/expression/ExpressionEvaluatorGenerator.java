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

package org.mule.devkit.generation.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.ExpressionEvaluator;
import org.mule.api.annotations.ExpressionLanguage;
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
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.expression.ExpressionUtils;

import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluatorGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(ExpressionLanguage.class) && type.getMethodsAnnotatedWith(ExpressionEvaluator.class).size() > 0;
    }

    @Override
    public void generate(Type type) {
        String name = type.getAnnotation(ExpressionLanguage.class).name();

        Method executableElement = type.getMethodsAnnotatedWith(ExpressionEvaluator.class).get(0);
        TypeReference moduleObject = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));
        DefinedClass evaluatorClass = getEvaluatorClass(name, type);

        ctx().note("Generating expression evaluator " + evaluatorClass.fullName() + " for language at class " + type.getSimpleName().toString());

        FieldVariable module = generateModuleField(moduleObject, evaluatorClass);

        org.mule.devkit.model.code.Method setMuleContext = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setMuleContext");
        Variable muleContext = setMuleContext.param(ref(MuleContext.class), "muleContext");
        Conditional ifModuleIsContextAware = setMuleContext.body()._if(Op._instanceof(module, ref(MuleContextAware.class)));
        ifModuleIsContextAware._then().add(ExpressionFactory.cast(ref(MuleContextAware.class), module).invoke("setMuleContext").arg(muleContext));

        org.mule.devkit.model.code.Method start = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "start");
        start._throws(ref(MuleException.class));
        Conditional ifModuleIsStartable = start.body()._if(Op._instanceof(module, ref(Startable.class)));
        ifModuleIsStartable._then().add(ExpressionFactory.cast(ref(Startable.class), module).invoke("start"));

        org.mule.devkit.model.code.Method stop = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "stop");
        stop._throws(ref(MuleException.class));
        Conditional ifModuleIsStoppable = stop.body()._if(Op._instanceof(module, ref(Stoppable.class)));
        ifModuleIsStoppable._then().add(ExpressionFactory.cast(ref(Stoppable.class), module).invoke("stop"));

        org.mule.devkit.model.code.Method init = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "initialise");
        init._throws(ref(InitialisationException.class));
        Conditional ifModuleIsInitialisable = init.body()._if(Op._instanceof(module, ref(Initialisable.class)));
        ifModuleIsInitialisable._then().add(ExpressionFactory.cast(ref(Initialisable.class), module).invoke("initialise"));

        org.mule.devkit.model.code.Method dispose = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "dispose");
        Conditional ifModuleIsDisposable = dispose.body()._if(Op._instanceof(module, ref(Disposable.class)));
        ifModuleIsDisposable._then().add(ExpressionFactory.cast(ref(Disposable.class), module).invoke("dispose"));

        generateConstructor(moduleObject, evaluatorClass, module);

        generateGetName(name, evaluatorClass);

        generateSetName(evaluatorClass);

        generateComputeClassHierarchyMethod(evaluatorClass);
        generateIsListClassMethod(evaluatorClass);
        generateIsMapClassMethod(evaluatorClass);
        generateIsListMethod(evaluatorClass);
        generateIsMapMethod(evaluatorClass);
        generateIsAssignableFrom(evaluatorClass);
        generateTransformMethod(evaluatorClass);

        org.mule.devkit.model.code.Method evaluate = evaluatorClass.method(Modifier.PUBLIC, ref(Object.class), "evaluate");
        Variable expression = evaluate.param(ref(String.class), "expression");
        Variable message = evaluate.param(ref(MuleMessage.class), "message");

        TryStatement tryStatement = evaluate.body()._try();

        Invocation newArray = ExpressionFactory._new(ref(Class.class).array());
        for (Parameter parameter : executableElement.getParameters()) {
            //tryStatement.body().assign(parameterClasses.component(ExpressionFactory.lit(argCount)), ref(parameter.asType()).boxify().dotclass());
            if (parameter.asType().getKind() == TypeKind.BOOLEAN ||
                    parameter.asType().getKind() == TypeKind.BYTE ||
                    parameter.asType().getKind() == TypeKind.SHORT ||
                    parameter.asType().getKind() == TypeKind.CHAR ||
                    parameter.asType().getKind() == TypeKind.INT ||
                    parameter.asType().getKind() == TypeKind.FLOAT ||
                    parameter.asType().getKind() == TypeKind.LONG ||
                    parameter.asType().getKind() == TypeKind.DOUBLE) {
                newArray.arg(ref(parameter.asType()).boxify().staticRef("TYPE"));
            } else {
                newArray.arg(ref(parameter.asType()).boxify().dotclass());
            }
            //argCount++;
        }
        Variable parameterClasses = tryStatement.body().decl(ref(Class.class).array(), "parameterClasses", newArray);
        int argCount = 0;
        Invocation getMethod = module.invoke("getClass").invoke("getMethod").arg(executableElement.getSimpleName().toString()).arg(parameterClasses);
        Variable moduleEvaluate = tryStatement.body().decl(ref(java.lang.reflect.Method.class), "evaluateMethod", getMethod);
        List<Variable> types = new ArrayList<Variable>();
        for (Parameter parameter : executableElement.getParameters()) {
            Variable var = tryStatement.body().decl(ref(java.lang.reflect.Type.class), parameter.getSimpleName().toString() + "Type", moduleEvaluate.invoke("getGenericParameterTypes").component(ExpressionFactory.lit(types.size())));
            types.add(var);
        }

        argCount = 0;
        Invocation evaluateInvoke = module.invoke(executableElement.getSimpleName().toString());
        for (Parameter parameter : executableElement.getParameters()) {
            if (parameter.getAnnotation(Payload.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getPayload"))));
            } else if (parameter.getAnnotation(ExceptionPayload.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getExceptionPayload"))));
            } else if (parameter.getAnnotation(CorrelationId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationId"))));
            } else if (parameter.getAnnotation(CorrelationSequence.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationSequence"))));
            } else if (parameter.getAnnotation(CorrelationGroupSize.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationGroupSize"))));
            } else if (parameter.getAnnotation(MessageUniqueId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getUniqueId"))));
            } else if (parameter.getAnnotation(MessageRootId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getMessageRootId"))));
            } else if (parameter.asType().toString().startsWith(MuleMessage.class.getName())) {
                evaluateInvoke.arg(message);
            } else if (parameter.getAnnotation(InboundHeaders.class) != null) {
                InboundHeaders inboundHeaders = parameter.getAnnotation(InboundHeaders.class);
                if (parameter.isArrayOrList()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            )));
                } else if (parameter.isMap()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            )));
                } else {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message)
                            )));
                }
            } else if (parameter.getAnnotation(SessionHeaders.class) != null) {
                SessionHeaders sessionHeaders = parameter.getAnnotation(SessionHeaders.class);
                if (parameter.isArrayOrList()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            )));
                } else if (parameter.isMap()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            )));
                } else {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message)
                            )));
                }
            } else if (parameter.getAnnotation(OutboundHeaders.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                        ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("OUTBOUND:*").arg(message).arg(ref(Map.class).dotclass())
                        )));
            } else if (parameter.getAnnotation(InvocationHeaders.class) != null) {
                InvocationHeaders invocationHeaders = parameter.getAnnotation(InvocationHeaders.class);
                if (parameter.isArrayOrList()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            )));
                } else if (parameter.isMap()) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            )));
                } else {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message)
                            )));
                }
            } else {
                if (parameter.asType().toString().contains("String")) {
                    evaluateInvoke.arg(expression);
                }
            }
            argCount++;
        }

        tryStatement.body()._return(evaluateInvoke);

        catchAndRethrowAsRuntimeException(tryStatement, NoSuchMethodException.class);
        catchAndRethrowAsRuntimeException(tryStatement, TransformerException.class);

        ctx().registerAtBoot(evaluatorClass);
    }

    private void catchAndRethrowAsRuntimeException(TryStatement tryStatement, Class clazz) {
        CatchBlock catchBlock = tryStatement._catch(ref(clazz));
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(e));
    }

    private FieldVariable generateModuleField(TypeReference typeElement, DefinedClass evaluatorClass) {
        return evaluatorClass.field(Modifier.PRIVATE, typeElement, "module", ExpressionFactory._null());
    }

    private void generateConstructor(TypeReference typeElement, DefinedClass evaluatorClass, FieldVariable module) {
        org.mule.devkit.model.code.Method constructor = evaluatorClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(module, ExpressionFactory._new(typeElement));
    }

    private void generateGetName(String name, DefinedClass evaluatorClass) {
        org.mule.devkit.model.code.Method getName = evaluatorClass.method(Modifier.PUBLIC, ref(String.class), "getName");
        getName.body()._return(ExpressionFactory.lit(name));
    }

    private void generateSetName(DefinedClass evaluatorClass) {
        org.mule.devkit.model.code.Method setName = evaluatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setName");
        setName.param(ref(String.class), "name");
        setName.body()._throw(ExpressionFactory._new(ref(UnsupportedOperationException.class)));
    }

    private DefinedClass getEvaluatorClass(String name, Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.EXPRESSIONS_NAMESPACE);
        DefinedClass evaluator = pkg._class(NameUtils.camel(name) + NamingConstants.EXPRESSION_EVALUATOR_CLASS_NAME_SUFFIX, new Class<?>[]{org.mule.api.expression.ExpressionEvaluator.class});
        evaluator._implements(ref(MuleContextAware.class));
        evaluator._implements(ref(Startable.class));
        evaluator._implements(ref(Stoppable.class));
        evaluator._implements(ref(Initialisable.class));
        evaluator._implements(ref(Disposable.class));
        //evaluator._implements(ref(FlowConstructAware.class));

        return evaluator;
    }

}