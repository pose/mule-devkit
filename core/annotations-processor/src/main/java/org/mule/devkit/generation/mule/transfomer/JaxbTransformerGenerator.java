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
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.type.DeclaredType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class JaxbTransformerGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) {
        for (Method executableElement : type.getMethodsAnnotatedWith(Processor.class)) {
            for (Parameter variable : executableElement.getParameters()) {
                if (variable.isXmlType() && !ctx().isJaxbElementRegistered(variable.asType())) {
                    // get class
                    DefinedClass jaxbTransformerClass = getJaxbTransformerClass(executableElement, variable);

                    // declare weight
                    FieldVariable weighting = jaxbTransformerClass.field(Modifier.PRIVATE, ctx().getCodeModel().INT, "weighting", Op.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), ExpressionFactory.lit(1)));

                    // load JAXB context
                    org.mule.devkit.model.code.Method loadJaxbContext = generateLoadJaxbContext(jaxbTransformerClass);

                    // declare JAXB context
                    FieldVariable jaxbContext = jaxbTransformerClass.field(Modifier.PRIVATE | Modifier.STATIC, JAXBContext.class, "JAXB_CONTEXT", ExpressionFactory.invoke(loadJaxbContext).arg(ref(variable.asType()).boxify().dotclass()));

                    //generate constructor
                    generateConstructor(jaxbTransformerClass, variable);

                    // doTransform
                    generateDoTransform(jaxbTransformerClass, jaxbContext, variable);

                    // set and get weight
                    generateGetPriorityWeighting(jaxbTransformerClass, weighting);
                    generateSetPriorityWeighting(jaxbTransformerClass, weighting);

                    ctx().registerAtBoot(jaxbTransformerClass);
                    ctx().registerJaxbElement(variable.asType());
                }
            }
        }

    }

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        org.mule.devkit.model.code.Method setPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setPriorityWeighting");
        org.mule.devkit.model.code.Variable localWeighting = setPriorityWeighting.param(ctx().getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(ExpressionFactory._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        org.mule.devkit.model.code.Method getPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, ctx().getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass jaxbTransformerClass, FieldVariable jaxbContext, Variable variable) {
        org.mule.devkit.model.code.Method doTransform = jaxbTransformerClass.method(Modifier.PROTECTED, Object.class, "doTransform");
        doTransform._throws(TransformerException.class);
        org.mule.devkit.model.code.Variable src = doTransform.param(Object.class, "src");
        org.mule.devkit.model.code.Variable encoding = doTransform.param(String.class, "encoding");

        org.mule.devkit.model.code.Variable result = doTransform.body().decl(ref(variable.asType()).boxify(), "result", ExpressionFactory._null());

        TryStatement tryBlock = doTransform.body()._try();
        org.mule.devkit.model.code.Variable unmarshaller = tryBlock.body().decl(ref(Unmarshaller.class), "unmarshaller");
        tryBlock.body().assign(unmarshaller, jaxbContext.invoke("createUnmarshaller"));
        org.mule.devkit.model.code.Variable inputStream = tryBlock.body().decl(ref(InputStream.class), "is", ExpressionFactory._new(ref(ByteArrayInputStream.class)).arg(
                ExpressionFactory.invoke(ExpressionFactory.cast(ref(String.class), src), "getBytes").arg(encoding)
        ));

        org.mule.devkit.model.code.Variable streamSource = tryBlock.body().decl(ref(StreamSource.class), "ss", ExpressionFactory._new(ref(StreamSource.class)).arg(inputStream));
        Invocation unmarshal = unmarshaller.invoke("unmarshal");
        unmarshal.arg(streamSource);
        unmarshal.arg(ExpressionFactory.dotclass(ref(variable.asType()).boxify()));

        tryBlock.body().assign(result, unmarshal.invoke("getValue"));

        CatchBlock unsupportedEncodingCatch = tryBlock._catch(ref(UnsupportedEncodingException.class));
        org.mule.devkit.model.code.Variable unsupportedEncoding = unsupportedEncodingCatch.param("unsupportedEncoding");

        generateThrowTransformFailedException(unsupportedEncodingCatch, unsupportedEncoding, variable);

        CatchBlock jaxbExceptionCatch = tryBlock._catch(ref(JAXBException.class));
        org.mule.devkit.model.code.Variable jaxbException = jaxbExceptionCatch.param("jaxbException");

        generateThrowTransformFailedException(jaxbExceptionCatch, jaxbException, variable);

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(CatchBlock catchBlock, org.mule.devkit.model.code.Variable exception, Variable variable) {
        Invocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg("String");
        transformFailedInvoke.arg(ExpressionFactory.lit(ref(variable.asType()).boxify().fullName()));

        Invocation transformerException = ExpressionFactory._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(ExpressionFactory._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private org.mule.devkit.model.code.Method generateLoadJaxbContext(DefinedClass jaxbTransformerClass) {
        org.mule.devkit.model.code.Method loadJaxbContext = jaxbTransformerClass.method(Modifier.PRIVATE | Modifier.STATIC, ref(JAXBContext.class), "loadJaxbContext");
        org.mule.devkit.model.code.Variable clazz = loadJaxbContext.param(ref(Class.class), "clazz");
        org.mule.devkit.model.code.Variable innerJaxbContext = loadJaxbContext.body().decl(ref(JAXBContext.class), "context");

        TryStatement tryBlock = loadJaxbContext.body()._try();
        tryBlock.body().assign(innerJaxbContext, ref(JAXBContext.class).staticInvoke("newInstance").arg(clazz));
        CatchBlock catchBlock = tryBlock._catch(ref(JAXBException.class));
        org.mule.devkit.model.code.Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(e));

        loadJaxbContext.body()._return(innerJaxbContext);

        return loadJaxbContext;
    }

    private void generateConstructor(DefinedClass jaxbTransformerClass, Variable variable) {
        // generate constructor
        org.mule.devkit.model.code.Method constructor = jaxbTransformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceType(constructor);

        // register destination data type
        registerDestinationType(constructor, variable);

        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

        constructor.body().invoke("setName").arg(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer");
    }

    private void registerDestinationType(org.mule.devkit.model.code.Method constructor, Variable variable) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(ref(variable.asType()).boxify()));
    }

    private void registerSourceType(org.mule.devkit.model.code.Method constructor) {
        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticRef("STRING"));
    }

    private DefinedClass getJaxbTransformerClass(Method executableElement, Variable variable) {
        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);
        Package pkg = ctx().getCodeModel()._package(executableElement.parent().getPackageName() + NamingConstants.TRANSFORMERS_NAMESPACE);

        return pkg._class(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer", AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class});
    }
}