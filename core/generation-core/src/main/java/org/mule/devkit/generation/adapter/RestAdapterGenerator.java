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

package org.mule.devkit.generation.adapter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.rest.HttpMethod;
import org.mule.api.annotations.rest.RestCall;
import org.mule.api.annotations.rest.RestExceptionOn;
import org.mule.api.annotations.rest.RestHeaderParam;
import org.mule.api.annotations.rest.RestHttpClient;
import org.mule.api.annotations.rest.RestQueryParam;
import org.mule.api.annotations.rest.RestUriParam;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Field;
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
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.WhileLoop;
import org.mule.registry.TypeBasedTransformerResolver;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestAdapterGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.isModuleOrConnector() && type.hasMethodsAnnotatedWith(RestCall.class);
    }

    @Override
    public void generate(Type type) {
        DefinedClass restClientAdapterClass = getRestClientAdapterClass(type);

        // logger field
        //FieldVariable logger = generateLoggerField(restClientAdapterClass);

        FieldVariable responseTimeout = restClientAdapterClass.field(Modifier.PRIVATE, ctx().getCodeModel().INT, "responseTimeout");
        FieldVariable muleContext = restClientAdapterClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");

        Expression httpClient;
        if (!type.hasFieldAnnotatedWith(RestHttpClient.class)) {
            httpClient = restClientAdapterClass.field(Modifier.PRIVATE | Modifier.VOLATILE, ref(HttpClient.class), "httpClient");
        } else {
            httpClient = ExpressionFactory.invoke("get" + StringUtils.capitalize(type.getFieldsAnnotatedWith(RestHttpClient.class).get(0).getSimpleName().toString()));
        }

        generateSetMuleContext(restClientAdapterClass, muleContext);

        org.mule.devkit.model.code.Method initialise = restClientAdapterClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "initialise");
        initialise.annotate(ref(Override.class));
        initialise.body().add(ExpressionFactory._super().invoke("initialise"));
        if (!type.hasFieldAnnotatedWith(RestHttpClient.class)) {
            initialise.body().assign((FieldVariable) httpClient, ExpressionFactory._new(ref(HttpClient.class)));
        } else {
            initialise.body().invoke("set" + StringUtils.capitalize(type.getFieldsAnnotatedWith(RestHttpClient.class).get(0).getSimpleName().toString())).arg(ExpressionFactory._new(ref(HttpClient.class)));
        }
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.protocol.version").arg(ref(HttpVersion.class).staticRef("HTTP_1_1")));
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.socket.timeout").arg(responseTimeout));
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.protocol.content-charset").arg("UTF-8"));
        initialise.body().add(httpClient.invoke("getParams").invoke("setCookiePolicy").arg(ref(CookiePolicy.class).staticRef("BROWSER_COMPATIBILITY")));

        generateSetter(restClientAdapterClass, responseTimeout);

        generateRestCallImplementations(type, httpClient, muleContext, restClientAdapterClass);
    }

    private void generateSetMuleContext(DefinedClass restClientAdapterClass, FieldVariable muleContext) {
        org.mule.devkit.model.code.Method setMuleContext = restClientAdapterClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "setMuleContext");
        setMuleContext.annotate(Override.class);
        org.mule.devkit.model.code.Variable context = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(muleContext, context);
    }

    private void generateRestCallImplementations(Type type, Expression httpClient, org.mule.devkit.model.code.Variable muleContext, DefinedClass capabilitiesAdapter) {
        Map<String, org.mule.devkit.model.code.Variable> variables = new HashMap<String, org.mule.devkit.model.code.Variable>();
        for (Method executableElement : type.getMethodsAnnotatedWith(RestCall.class)) {
            org.mule.devkit.model.code.Method override = capabilitiesAdapter.method(Modifier.PUBLIC, ref(executableElement.getReturnType()), executableElement.getSimpleName().toString());
            override._throws(ref(IOException.class));
            RestCall restCall = executableElement.getAnnotation(RestCall.class);

            for (Parameter parameter : executableElement.getParameters()) {
                if (parameter.getAnnotation(OAuthAccessToken.class) != null ||
                        parameter.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                    continue;
                }

                variables.put(
                        parameter.getSimpleName().toString(),
                        override.param(ref(parameter.asType()), parameter.getSimpleName().toString())
                );
            }

            org.mule.devkit.model.code.Variable method = override.body().decl(ref(org.apache.commons.httpclient.HttpMethod.class), "method", ExpressionFactory._null());
            org.mule.devkit.model.code.Variable queryString = override.body().decl(ref(List.class).narrow(ref(NameValuePair.class)), "queryString", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(NameValuePair.class))));

            generateMethodAssignment(override, restCall, method);

            generateParametersCode(type, variables, executableElement, override, restCall, method, queryString);

            if (restCall.method() == HttpMethod.POST || restCall.method() == HttpMethod.PUT) {
                Parameter payloadParameter = null;
                for (Parameter parameter : executableElement.getParameters()) {
                    if (parameter.getAnnotation(RestUriParam.class) == null &&
                            parameter.getAnnotation(RestHeaderParam.class) == null &&
                            parameter.getAnnotation(RestQueryParam.class) == null) {
                        payloadParameter = parameter;
                        break;
                    }
                }
                if (payloadParameter != null) {
                    if (!restCall.contentType().equals(MimeTypes.ANY)) {
                        TryStatement tryToTransform = override.body()._try();
                        org.mule.devkit.model.code.Variable payloadInputDataType = tryToTransform.body().decl(ref(DataType.class), "payloadInputDataType", ref(DataTypeFactory.class).staticInvoke("createFromObject").arg(variables.get(payloadParameter.getSimpleName().toString())));
                        org.mule.devkit.model.code.Variable payloadOutputDataType = tryToTransform.body().decl(ref(DataType.class), "payloadOutputDataType", ref(DataTypeFactory.class).staticInvoke("create").arg(ref(String.class).dotclass()).arg(restCall.contentType()));
                        org.mule.devkit.model.code.Variable typeBasedResolver = tryToTransform.body().decl(ref(TransformerResolver.class), "typeBasedResolver", muleContext.invoke("getRegistry").invoke("lookupObject").arg(ref(TypeBasedTransformerResolver.class).dotclass()));
                        org.mule.devkit.model.code.Variable payloadTransformer = tryToTransform.body().decl(ref(Transformer.class), "payloadTransformer", typeBasedResolver.invoke("resolve").arg(payloadInputDataType).arg(payloadOutputDataType));
                        tryToTransform.body()._if(Op.eq(payloadTransformer, ExpressionFactory._null()))._then().assign(payloadTransformer, muleContext.invoke("getRegistry").invoke("lookupTransformer").arg(payloadInputDataType).arg(payloadOutputDataType));

                        org.mule.devkit.model.code.Variable payloadRequestEntity = tryToTransform.body().decl(ref(RequestEntity.class), "payloadRequestEntity", ExpressionFactory._new(ref(StringRequestEntity.class)).arg(ExpressionFactory.cast(ref(String.class), payloadTransformer.invoke("transform").arg(variables.get(payloadParameter.getSimpleName().toString())))).arg(restCall.contentType()).arg(ExpressionFactory.lit("UTF-8")));
                        tryToTransform.body().add(ExpressionFactory.cast(ref(PostMethod.class), method).invoke("setRequestEntity").arg(payloadRequestEntity));

                        CatchBlock catchResolverException = tryToTransform._catch(ref(ResolverException.class));
                        org.mule.devkit.model.code.Variable resolverException = catchResolverException.param("rese");

                        catchResolverException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(resolverException.invoke("getMessage")).arg(resolverException));

                        CatchBlock catchRegistrationException = tryToTransform._catch(ref(RegistrationException.class));
                        org.mule.devkit.model.code.Variable registrationException = catchRegistrationException.param("re");

                        catchRegistrationException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(registrationException.invoke("getMessage")).arg(registrationException));

                        CatchBlock catchTransformerException = tryToTransform._catch(ref(TransformerException.class));
                        org.mule.devkit.model.code.Variable transformerException = catchTransformerException.param("te");

                        catchTransformerException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(transformerException.invoke("getMessage")).arg(transformerException));
                    } else {
                        org.mule.devkit.model.code.Variable payloadRequestEntity = override.body().decl(ref(RequestEntity.class), "payloadRequestEntity", ExpressionFactory._new(ref(StringRequestEntity.class)).arg(variables.get(payloadParameter.getSimpleName().toString()).invoke("toString")));
                        override.body().add(ExpressionFactory.cast(ref(PostMethod.class), method).invoke("setRequestEntity").arg(payloadRequestEntity));
                    }
                }
            }

            org.mule.devkit.model.code.Variable statusCode = override.body().decl(ctx().getCodeModel().INT, "statusCode", httpClient.invoke("executeMethod").arg(method));

            generateParseResponseCode(type, executableElement, override, method, statusCode, muleContext);

            override.body()._return(ExpressionFactory._null());
        }
    }

    private void generateParametersCode(Type type, Map<String, org.mule.devkit.model.code.Variable> variables, Method executableElement, org.mule.devkit.model.code.Method override, RestCall restCall, org.mule.devkit.model.code.Variable method, org.mule.devkit.model.code.Variable queryString) {
        org.mule.devkit.model.code.Variable uri = override.body().decl(ref(String.class), "uri", ExpressionFactory.lit(restCall.uri()));
        for (Parameter parameter : executableElement.getParameters()) {
            RestUriParam restUriParam = parameter.getAnnotation(RestUriParam.class);
            if (restUriParam != null) {
                if (restCall.uri().contains("{" + restUriParam.value() + "}")) {
                    override.body().assign(uri, uri.invoke("replace").arg("{" + restUriParam.value() + "}").arg(variables.get(parameter.getSimpleName().toString())));
                }
            }
        }
        for (Field field : type.getFieldsAnnotatedWith(RestUriParam.class)) {
            RestUriParam restUriParam = field.getAnnotation(RestUriParam.class);
            if (restUriParam != null) {
                if (restCall.uri().contains("{" + restUriParam.value() + "}")) {
                    override.body().assign(uri, uri.invoke("replace").arg("{" + restUriParam.value() + "}").arg(ExpressionFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString()))));
                }
            }
        }

        override.body().add(method.invoke("setURI").arg(ExpressionFactory._new(ref(URI.class)).arg(uri).arg(ExpressionFactory.FALSE)));
        for (Parameter parameter : executableElement.getParameters()) {
            RestUriParam restUriParam = parameter.getAnnotation(RestUriParam.class);
            if (restUriParam != null) {
                if (restCall.uri().contains("{" + restUriParam.value() + "}")) {
                    continue;
                }
                addQueryParameter(override.body(), queryString, variables.get(parameter.getSimpleName().toString()), parameter);
            }
        }

        for (Field field : type.getFieldsAnnotatedWith(RestUriParam.class)) {
            RestUriParam restUriParam = field.getAnnotation(RestUriParam.class);
            if (restUriParam != null) {
                if (restCall.uri().contains("{" + restUriParam.value() + "}")) {
                    continue;
                }
                addQueryParameter(override.body(), queryString, ExpressionFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString())), field);
            }
        }

        for (Parameter parameter : executableElement.getParameters()) {
            RestHeaderParam restHeaderParam = parameter.getAnnotation(RestHeaderParam.class);
            if (restHeaderParam != null) {
                override.body().add(method.invoke("addRequestHeader").arg(restHeaderParam.value()).arg(variables.get(parameter.getSimpleName().toString())));
            }
        }

        for (Field field : type.getFieldsAnnotatedWith(RestHeaderParam.class)) {
            RestHeaderParam restHeaderParam = field.getAnnotation(RestHeaderParam.class);
            if (restHeaderParam != null) {
                override.body().add(method.invoke("addRequestHeader").arg(restHeaderParam.value()).arg(ExpressionFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString()))));
            }
        }


        if (restCall.method() == HttpMethod.GET) {
            override.body().add(method.invoke("setQueryString").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));
        } else if (restCall.method() == HttpMethod.PUT) {
            override.body().add(ExpressionFactory.cast(ref(PutMethod.class), method).invoke("addParameters").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));
        } else if (restCall.method() == HttpMethod.DELETE) {
            override.body().add(method.invoke("setQueryString").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));
        } else if (restCall.method() == HttpMethod.POST) {
            override.body().add(ExpressionFactory.cast(ref(PostMethod.class), method).invoke("addParameters").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));
        } else if (restCall.method() == HttpMethod.TRACE) {
            override.body().add(method.invoke("setQueryString").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));
        }
    }

    private void generateMethodAssignment(org.mule.devkit.model.code.Method override, RestCall restCall, org.mule.devkit.model.code.Variable method) {
        if (restCall.method() == HttpMethod.GET) {
            override.body().assign(method, ExpressionFactory._new(ref(GetMethod.class)));
        } else if (restCall.method() == HttpMethod.PUT) {
            override.body().assign(method, ExpressionFactory._new(ref(PutMethod.class)));
        } else if (restCall.method() == HttpMethod.DELETE) {
            override.body().assign(method, ExpressionFactory._new(ref(DeleteMethod.class)));
        } else if (restCall.method() == HttpMethod.POST) {
            override.body().assign(method, ExpressionFactory._new(ref(PostMethod.class)));
        } else if (restCall.method() == HttpMethod.TRACE) {
            override.body().assign(method, ExpressionFactory._new(ref(TraceMethod.class)));
        }
    }

    private void generateParseResponseCode(Type type, Method executableElement, org.mule.devkit.model.code.Method override, org.mule.devkit.model.code.Variable method, org.mule.devkit.model.code.Variable statusCode, org.mule.devkit.model.code.Variable muleContext) {
        Conditional ifMethodExecuted = override.body()._if(Op.cand(Op.ne(method, ExpressionFactory._null()), method.invoke("hasBeenUsed")));
        org.mule.devkit.model.code.Variable bufferedReader = ifMethodExecuted._then().decl(ref(BufferedReader.class), "bufferedReader", ExpressionFactory._null());
        org.mule.devkit.model.code.Variable stringWriter = ifMethodExecuted._then().decl(ref(StringWriter.class), "stringWriter", ExpressionFactory._new(ref(StringWriter.class)));
        org.mule.devkit.model.code.Variable bufferedWriter = ifMethodExecuted._then().decl(ref(BufferedWriter.class), "bufferedWriter", ExpressionFactory._new(ref(BufferedWriter.class)).arg(stringWriter).arg(ExpressionFactory.lit(8192)));

        TryStatement readStream = ifMethodExecuted._then()._try();
        org.mule.devkit.model.code.Variable line = readStream.body().decl(ref(String.class), "line", ExpressionFactory.lit(""));
        readStream.body().assign(bufferedReader, ExpressionFactory._new(ref(BufferedReader.class)).arg(ExpressionFactory._new(ref(InputStreamReader.class)).arg(method.invoke("getResponseBodyAsStream"))));
        WhileLoop whileLoop = readStream.body()._while(Op.ne(ExpressionFactory.assign(line, bufferedReader.invoke("readLine")), ExpressionFactory._null()));
        whileLoop.body().add(bufferedWriter.invoke("write").arg(line));
        whileLoop.body().add(bufferedWriter.invoke("newLine"));

        readStream._finally().add(bufferedWriter.invoke("flush"));
        readStream._finally().add(bufferedWriter.invoke("close"));
        readStream._finally()._if(Op.ne(bufferedReader, ExpressionFactory._null()))._then().add(bufferedReader.invoke("close"));

        org.mule.devkit.model.code.Variable output = ifMethodExecuted._then().decl(ref(String.class), "output", ref(StringEscapeUtils.class).staticInvoke("unescapeHtml").arg(stringWriter.invoke("toString")));

        generateExeptionOnBlock(executableElement, statusCode, ifMethodExecuted, output);

        generateTransformAndReturn(type, executableElement, muleContext, ifMethodExecuted, output);
    }

    private void generateTransformAndReturn(Type moduleClass, Method executableElement, org.mule.devkit.model.code.Variable muleContext, Conditional block, org.mule.devkit.model.code.Variable output) {
        Conditional shouldTransform = block._then()._if(Op.cand(
                Op.ne(output, ExpressionFactory._null()),
                Op.not(ref(executableElement.getReturnType()).boxify().dotclass().invoke("isAssignableFrom").arg(ref(String.class).dotclass()))
        ));

        org.mule.devkit.model.code.Variable outputDataType = shouldTransform._then().decl(ref(DataType.class), "outputDataType",
                ExpressionFactory._null());

        TryStatement tryToTransform = shouldTransform._then()._try();

        Invocation getMethod = ref(moduleClass.asType()).boxify().dotclass().invoke("getMethod").arg(executableElement.getSimpleName().toString());
        for (Parameter parameter : executableElement.getParameters()) {
            getMethod.arg(ref(parameter.asType()).boxify().dotclass());
        }

        org.mule.devkit.model.code.Variable method = tryToTransform.body().decl(ref(java.lang.reflect.Method.class), "reflectedMethod", getMethod);

        tryToTransform.body().assign(outputDataType,
                ref(DataTypeFactory.class).staticInvoke("createFromReturnType").arg(method));

        org.mule.devkit.model.code.Variable typeBasedResolver = tryToTransform.body().decl(ref(TransformerResolver.class), "typeBasedResolver", muleContext.invoke("getRegistry").invoke("lookupObject").arg(ref(TypeBasedTransformerResolver.class).dotclass()));

        org.mule.devkit.model.code.Variable transformer = tryToTransform.body().decl(ref(Transformer.class), "payloadTransformer", typeBasedResolver.invoke("resolve").arg(ref(DataType.class).staticRef("STRING_DATA_TYPE")).arg(outputDataType));
        tryToTransform.body()._if(Op.eq(transformer, ExpressionFactory._null()))._then().assign(transformer, muleContext.invoke("getRegistry").invoke("lookupTransformer").arg(ref(DataType.class).staticRef("STRING_DATA_TYPE")).arg(outputDataType));

        tryToTransform.body()._return(ExpressionFactory.cast(ref(executableElement.getReturnType()), transformer.invoke("transform").arg(output)));

        CatchBlock catchResolverException = tryToTransform._catch(ref(ResolverException.class));
        org.mule.devkit.model.code.Variable resolverException = catchResolverException.param("rese");

        catchResolverException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(resolverException.invoke("getMessage")).arg(resolverException));

        CatchBlock catchRegistrationException = tryToTransform._catch(ref(RegistrationException.class));
        org.mule.devkit.model.code.Variable registrationException = catchRegistrationException.param("re");

        catchRegistrationException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(registrationException.invoke("getMessage")).arg(registrationException));

        CatchBlock catchTransformerException = tryToTransform._catch(ref(TransformerException.class));
        org.mule.devkit.model.code.Variable transformerException = catchTransformerException.param("te");

        catchTransformerException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(Op.plus(ExpressionFactory.lit("Unable to transform output from String to "), outputDataType.invoke("toString"))).arg(transformerException));

        CatchBlock catchNoSuchMethodException = tryToTransform._catch(ref(NoSuchMethodException.class));
        org.mule.devkit.model.code.Variable noSuchMethodException = catchNoSuchMethodException.param("nsme");

        catchNoSuchMethodException.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(ExpressionFactory.lit("Unable to find method named " + executableElement.getSimpleName().toString())).arg(noSuchMethodException));

        shouldTransform._else()._return(ExpressionFactory.cast(ref(executableElement.getReturnType()), ExpressionFactory.cast(ref(Object.class), output)));
    }

    private void generateExeptionOnBlock(Method executableElement, org.mule.devkit.model.code.Variable statusCode, Conditional block, org.mule.devkit.model.code.Variable message) {
        RestExceptionOn restExceptionOn = executableElement.getAnnotation(RestExceptionOn.class);
        final String restExceptionOnAnnotationName = RestExceptionOn.class.getName();
        DeclaredType exception = null;
        List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (restExceptionOnAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    if ("exception".equals(
                            entry.getKey().getSimpleName().toString())) {
                        exception = (DeclaredType) entry.getValue().getValue();
                        break;
                    }
                }
            }
        }

        if (restExceptionOn != null && restExceptionOn.statusCodeIs().length > 0) {
            for (int expectedStatusCode : restExceptionOn.statusCodeIs()) {
                Conditional ifStatusCode = block._then()._if(Op.eq(statusCode, ExpressionFactory.lit(expectedStatusCode)));
                if (exception == null) {
                    ifStatusCode._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(message));
                } else {
                    ifStatusCode._then()._throw(ExpressionFactory._new(ref(exception)).arg(message));
                }
            }
        }
        if (restExceptionOn != null && restExceptionOn.statusCodeIsNot().length > 0) {
            Expression notEq = null;
            for (int expectedStatusCode : restExceptionOn.statusCodeIsNot()) {
                if (notEq == null) {
                    notEq = Op.ne(statusCode, ExpressionFactory.lit(expectedStatusCode));
                } else {
                    notEq = Op.cand(notEq, Op.ne(statusCode, ExpressionFactory.lit(expectedStatusCode)));
                }
            }
            Conditional ifStatusCode = block._then()._if(notEq);
            if (exception == null) {
                ifStatusCode._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(message));
            } else {
                ifStatusCode._then()._throw(ExpressionFactory._new(ref(exception)).arg(message));
            }
        }
    }

    private void addQueryParameter(Block body, org.mule.devkit.model.code.Variable queryString, Expression variable, Variable parameter) {
        RestUriParam restUriParam = parameter.getAnnotation(RestUriParam.class);
        Expression rvalue = variable.invoke("toString");
        if (restUriParam != null) {
            if (restUriParam.separatedBy() != null &&
                    !StringUtils.isEmpty(restUriParam.separatedBy())) {
                rvalue = ref(StringUtils.class).staticInvoke("join").arg(variable.invoke("toArray")).arg(restUriParam.separatedBy());
            }
            Conditional ifNotNull = body._if(Op.ne(variable, ExpressionFactory._null()));
            ifNotNull._then().add(queryString.invoke("add").arg(ExpressionFactory._new(ref(NameValuePair.class)).arg(restUriParam.value()).arg(rvalue)));
        }
    }

    private DefinedClass getRestClientAdapterClass(Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.ADAPTERS_NAMESPACE);
        TypeReference previous = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));

        if (previous == null) {
            previous = (TypeReference) ref(type.asType());
        }

        DefinedClass clazz = pkg._class(type.getClassName() + NamingConstants.REST_CLIENT_ADAPTER_CLASS_NAME_SUFFIX, previous);
        clazz._implements(ref(Initialisable.class));
        clazz._implements(ref(Disposable.class));
        clazz._implements(ref(MuleContextAware.class));

        clazz.role(DefinedClassRoles.MODULE_OBJECT, ref(type));

        return clazz;
    }
}