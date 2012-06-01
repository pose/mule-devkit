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

import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.callback.HttpCallback;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.transport.Connector;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.util.NumberUtils;

public class HttpCallbackAdapterGenerator extends AbstractModuleGenerator {

    public static final String LOCAL_PORT_FIELD_NAME = "localPort";
    public static final String REMOTE_PORT_FIELD_NAME = "remotePort";
    public static final String DOMAIN_FIELD_NAME = "domain";
    public static final String ASYNC_FIELD_NAME = "async";
    public static final String CONNECTOR_FIELD_NAME = "connector";
    private static final int DEFAULT_LOCAL_PORT = 8080;
    private static final int DEFAULT_REMOTE_PORT = 80;

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth.class) ||
                typeElement.hasAnnotation(OAuth2.class) ||
                typeElement.hasProcessorMethodWithParameter(HttpCallback.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass httpCallbackAdapter = getHttpCallbackAdapterClass(typeElement);
        FieldVariable localPort = localPortFieldWithGetterAndSetter(httpCallbackAdapter);
        FieldVariable remotePort = remotePortFieldWithGetterAndSetter(httpCallbackAdapter);
        FieldVariable domain = domainFieldWithGetterAndSetter(httpCallbackAdapter);
        connectorFieldWithGetterAndSetter(httpCallbackAdapter);
        FieldVariable logger = FieldBuilder.newLoggerField(httpCallbackAdapter);
        asyncFieldWithGetterAndSetter(httpCallbackAdapter);
        generateInitialiseMethod(httpCallbackAdapter, localPort, remotePort, domain, logger);
    }

    private void generateInitialiseMethod(DefinedClass httpCallbackAdapter, FieldVariable localPort, FieldVariable remotePort, FieldVariable domain, FieldVariable logger) {
        Method initialise = httpCallbackAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "initialise");
        if (ref(Initialisable.class).isAssignableFrom(httpCallbackAdapter._extends())) {
            initialise.body().invoke(ExpressionFactory._super(), "initialise");
        }

        Block ifLocalPortIsNull = initialise.body()._if(Op.eq(localPort, ExpressionFactory._null()))._then();
        initialiseLocalPort(localPort, logger, ifLocalPortIsNull);

        Block ifRemotePortIsNull = initialise.body()._if(Op.eq(remotePort, ExpressionFactory._null()))._then();
        ifRemotePortIsNull.invoke(logger, "info").arg(ExpressionFactory.lit("Using default remotePort: 80"));
        ifRemotePortIsNull.assign(remotePort, ExpressionFactory.lit(DEFAULT_REMOTE_PORT));

        Block ifDomainIsNull = initialise.body()._if(Op.eq(domain, ExpressionFactory._null()))._then();
        assignDomainSystemVariable(domain, logger, ifDomainIsNull);
    }

    private void initialiseLocalPort(FieldVariable localPort, FieldVariable logger, Block ifPortIsNull) {
        Variable portSystemVar = ifPortIsNull.decl(ref(String.class), "portSystemVar", ref(System.class).staticInvoke("getProperty").arg("http.port"));
        Conditional conditional = ifPortIsNull._if(ref(NumberUtils.class).staticInvoke("isDigits").arg(portSystemVar));
        conditional._then().block().assign(localPort, ref(Integer.class).staticInvoke("parseInt").arg(portSystemVar));
        Block thenBlock = conditional._else().block();
        thenBlock.invoke(logger, "warn").arg(ExpressionFactory.lit("Environment variable 'http.port' not found, using default localPort: 8080"));
        thenBlock.assign(localPort, ExpressionFactory.lit(DEFAULT_LOCAL_PORT));
    }

    private void assignDomainSystemVariable(FieldVariable domain, FieldVariable logger, Block ifDomainIsNull) {
        Variable domainSystemVar = ifDomainIsNull.decl(ref(String.class), "domainSystemVar", ref(System.class).staticInvoke("getProperty").arg("fullDomain"));
        Conditional conditional = ifDomainIsNull._if(Op.ne(domainSystemVar, ExpressionFactory._null()));
        conditional._then().block().assign(domain, domainSystemVar);
        Block thenBlock = conditional._else().block();
        thenBlock.invoke(logger, "warn").arg("Environment variable 'fullDomain' not found, using default: localhost");
        thenBlock.assign(domain, ExpressionFactory.lit("localhost"));
    }

    private DefinedClass getHttpCallbackAdapterClass(DevKitTypeElement typeElement) {
        String httpCallbackAdapterClassName = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.HTTP_CALLBACK_ADAPTER_CLASS_NAME_SUFFIX);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackAdapterClassName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass oauthAdapter = pkg._class(context.getNameUtils().getClassName(httpCallbackAdapterClassName), classToExtend);
        oauthAdapter._implements(ref(Initialisable.class));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), oauthAdapter);

        return oauthAdapter;
    }

    private FieldVariable localPortFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(Integer.class).name(LOCAL_PORT_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable remotePortFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(Integer.class).name(REMOTE_PORT_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable domainFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(DOMAIN_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable connectorFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(Connector.class).name(CONNECTOR_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable asyncFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(Boolean.class).name(ASYNC_FIELD_NAME).initialValue(ExpressionFactory.lit(false)).getterAndSetter().build();
    }
}