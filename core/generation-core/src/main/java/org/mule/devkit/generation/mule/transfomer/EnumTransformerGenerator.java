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

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class EnumTransformerGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) {

        for (Field field : type.getFields()) {
            if (field.isEnum()) {
                if (!ctx().isEnumRegistered(field.asType())) {
                    registerEnumTransformer(field);
                    ctx().registerEnum(field.asType());
                }
            }
        }

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {
            for (Parameter variable : method.getParameters()) {
                if (variable.isEnum() && !ctx().isEnumRegistered(variable.asType())) {
                    registerEnumTransformer(variable);
                    ctx().registerEnum(variable.asType());
                } else if (variable.isCollection()) {
                    for (Identifiable variableTypeParameter : variable.getTypeArguments()) {
                        if (variableTypeParameter.isEnum() && !ctx().isEnumRegistered(variableTypeParameter.asType())) {
                            registerEnumTransformer(variableTypeParameter);
                            ctx().registerEnum(variableTypeParameter.asType());
                        }
                    }
                }
            }
        }

        for (Method method : type.getMethodsAnnotatedWith(Source.class)) {
            for (Parameter variable : method.getParameters()) {
                if (!variable.isEnum()) {
                    continue;
                }

                if (!ctx().isEnumRegistered(variable.asType())) {
                    registerEnumTransformer(variable);
                    ctx().registerEnum(variable.asType());
                }
            }
        }
    }

    private void registerEnumTransformer(Identifiable variableElement) {
        // get class
        DefinedClass transformerClass = getEnumTransformerClass(variableElement);

        // declare object
        FieldVariable muleContext = generateFieldForMuleContext(transformerClass);

        // declare weight
        FieldVariable weighting = transformerClass.field(Modifier.PRIVATE, ctx().getCodeModel().INT, "weighting", ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"));

        //generate constructor
        generateConstructor(transformerClass, variableElement);

        // add setmulecontext
        generateSetMuleContextMethod(transformerClass, muleContext);

        // doTransform
        generateDoTransform(transformerClass, variableElement);

        // set and get weight
        generateGetPriorityWeighting(transformerClass, weighting);
        generateSetPriorityWeighting(transformerClass, weighting);

        ctx().registerAtBoot(transformerClass);
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

    private void generateDoTransform(DefinedClass jaxbTransformerClass, Identifiable variableElement) {
        org.mule.devkit.model.code.Method doTransform = jaxbTransformerClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        doTransform.param(ref(String.class), "encoding");

        Variable result = doTransform.body().decl(ref(variableElement.asType()).boxify(), "result", ExpressionFactory._null());

        Invocation valueOf = ref(Enum.class).staticInvoke("valueOf");
        valueOf.arg(ref(variableElement.asType()).boxify().dotclass());
        valueOf.arg(ExpressionFactory.cast(ref(String.class), src));

        doTransform.body().assign(result, valueOf);

        doTransform.body()._return(result);
    }

    private void generateConstructor(DefinedClass transformerClass, Identifiable variableElement) {
        // generate constructor
        org.mule.devkit.model.code.Method constructor = transformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceTypes(constructor);

        // register destination data type
        registerDestinationType(constructor, ref(variableElement.asType()).boxify());

        constructor.body().invoke("setName").arg(transformerClass.name());
    }

    private void registerDestinationType(org.mule.devkit.model.code.Method constructor, TypeReference clazz) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(clazz));
    }

    private void registerSourceTypes(org.mule.devkit.model.code.Method constructor) {
        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref(String.class).boxify().dotclass()));
    }

    private DefinedClass getEnumTransformerClass(Identifiable variableElement) {
        javax.lang.model.element.Element enumElement = ctx().getTypeUtils().asElement(variableElement.asType());

        String packageName = "";
        if (variableElement instanceof Type) {
            packageName = ((Type)variableElement).getPackageName();
        } else if (variableElement instanceof Parameter) {
            packageName = (((Parameter) variableElement).parent().parent()).getPackageName();
        } else if (variableElement.parent() instanceof Type) {
            packageName = ((Type)variableElement.parent()).getPackageName();
        }

        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(packageName + NamingConstants.TRANSFORMERS_NAMESPACE);
        DefinedClass transformer = pkg._class(StringUtils.capitalise(enumElement.getSimpleName().toString()) + NamingConstants.ENUM_TRANSFORMER_CLASS_NAME_SUFFIX, AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class, MuleContextAware.class});

        return transformer;
    }
}

