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

package org.mule.devkit.generation.mule.transfomer;

import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Transformer;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.api.GenerationException;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;

public class TransformerGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) throws GenerationException {
        for (Method executableElement : type.getMethodsAnnotatedWith(Transformer.class)) {

            // get class
            DefinedClass transformerClass = getTransformerClass(executableElement);

            // declare weight
            Transformer transformer = executableElement.getAnnotation(Transformer.class);
            FieldVariable weighting = transformerClass.field(Modifier.PRIVATE, ctx().getCodeModel().INT, "weighting", Op.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), ExpressionFactory.lit(transformer.priorityWeighting())));

            //generate constructor
            generateConstructor(transformerClass, type, executableElement);

            // doTransform
            generateDoTransform(transformerClass, executableElement);

            // set and get weight
            generateGetPriorityWeighting(transformerClass, weighting);
            generateSetPriorityWeighting(transformerClass, weighting);

            ctx().registerAtBoot(transformerClass);
        }

    }

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        org.mule.devkit.model.code.Method setPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setPriorityWeighting");
        Variable localWeighting = setPriorityWeighting.param(ctx().getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(ExpressionFactory._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        org.mule.devkit.model.code.Method getPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, ctx().getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass transformerClass, Method executableElement) {
        org.mule.devkit.model.code.Method doTransform = transformerClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        doTransform.param(ref(String.class), "encoding");

        Variable result = doTransform.body().decl(ref(executableElement.getReturnType()).boxify(), "result", ExpressionFactory._null());

        TryStatement tryBlock = doTransform.body()._try();

        // do something
        Invocation invoke = ref(executableElement.parent().asType()).boxify().staticInvoke(executableElement.getSimpleName().toString());

        TypeMirror expectedType = executableElement.getParameters().get(0).asType();

        invoke.arg(ExpressionFactory.cast(ref(expectedType), src));
        tryBlock.body().assign(result, invoke);

        CatchBlock exceptionCatch = tryBlock._catch(ref(Exception.class));
        Variable exception = exceptionCatch.param("exception");

        generateThrowTransformFailedException(exceptionCatch, exception, src, ref(executableElement.getReturnType()).boxify());

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(CatchBlock catchBlock, Variable exception, Variable src, TypeReference target) {
        Invocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg(src.invoke("getClass").invoke("getName"));
        transformFailedInvoke.arg(ExpressionFactory.lit(target.fullName()));

        Invocation transformerException = ExpressionFactory._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(ExpressionFactory._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private void generateConstructor(DefinedClass transformerClass, Type moduleClass, Method executableElement) {
        // generate constructor
        org.mule.devkit.model.code.Method constructor = transformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceTypes(constructor, executableElement);

        // register destination data type
        registerDestinationType(constructor, moduleClass, executableElement);

        constructor.body().invoke("setName").arg(executableElement.getCapitalizedName() + "Transformer");
    }

    private void registerDestinationType(org.mule.devkit.model.code.Method constructor, Type moduleClass, Method executableElement) {
        TryStatement tryToFindMethod = constructor.body()._try();
        Invocation getMethod = ref(moduleClass.asType()).boxify().dotclass().invoke("getMethod").arg(executableElement.getSimpleName().toString());
        for(Parameter parameter : executableElement.getParameters() ) {
            getMethod.arg(ref(parameter.asType()).boxify().dotclass());
        }
        Variable method = tryToFindMethod.body().decl(ref(java.lang.reflect.Method.class), "method", getMethod);
        Variable dataType = tryToFindMethod.body().decl(ref(DataType.class), "dataType", ref(DataTypeFactory.class).staticInvoke("createFromReturnType").arg(method));
        tryToFindMethod.body().invoke("setReturnDataType").arg(dataType);
        CatchBlock catchNoSuchMethodException = tryToFindMethod._catch(ref(NoSuchMethodException.class));
        catchNoSuchMethodException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Unable to find method " + executableElement.getSimpleName().toString()));
    }

    private void registerSourceTypes(org.mule.devkit.model.code.Method constructor, Method executableElement) {
        final String transformerAnnotationName = Transformer.class.getName();
        List<? extends AnnotationValue> sourceTypes = null;
        List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (transformerAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    if ("sourceTypes".equals(entry.getKey().getSimpleName().toString())) {
                        sourceTypes = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        break;
                    }
                }
            }
        }

        if (sourceTypes != null) {
            for (AnnotationValue sourceType : sourceTypes) {
                Invocation registerSourceType = constructor.body().invoke("registerSourceType");
                registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref((TypeMirror) sourceType.getValue()).boxify().dotclass()));
            }
        }
    }

    public DefinedClass getTransformerClass(Method executableElement) {
        Package pkg = ctx().getCodeModel()._package(executableElement.parent().getPackageName() + NamingConstants.TRANSFORMERS_NAMESPACE);
        DefinedClass transformer = pkg._class(executableElement.getCapitalizedName() + NamingConstants.TRANSFORMER_CLASS_NAME_SUFFIX, AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class});

        return transformer;
    }
}