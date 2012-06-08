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

package org.mule.devkit.generation.studio.editor;

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.devkit.generation.studio.AbstractMuleStudioGenerator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.EndpointType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.NamespaceType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;

import java.util.List;

public class MuleStudioEditorXmlGenerator extends AbstractMuleStudioGenerator {

    public static final String EDITOR_XML_FILE_NAME = "editors.xml";
    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_CAPTION = "General";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION = "General";
    public static final String CONNECTION_ATTRIBUTE_CATEGORY_CAPTION = "Connection";
    public static final String GROUP_DEFAULT_CAPTION = "Generic";
    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public boolean shouldGenerate(Type type) {
        return true;
    }

    @Override
    public void generate(Type type) {
        String moduleName = type.getModuleName();
        boolean isOAuth = type.hasAnnotation(OAuth.class) || type.hasAnnotation(OAuth2.class);

        NamespaceType namespace = new NamespaceType();
        namespace.setPrefix(moduleName);
        namespace.setUrl(URI_PREFIX + moduleName);

        PatternTypeOperationsBuilder operationsBuilder = isOAuth
        				? new OAuthPatternTypeOperationsBuilder(ctx(), type, PatternTypes.CLOUD_CONNECTOR)
        				: new PatternTypeOperationsBuilder(ctx(), type, PatternTypes.CLOUD_CONNECTOR);
        
        GlobalType globalCloudConnector = new GlobalCloudConnectorTypeBuilder(ctx(), type).build();
        namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector));
        namespace.getConnectorOrEndpointOrGlobal().add(operationsBuilder.build());
        namespace.getConnectorOrEndpointOrGlobal().add(new ConfigRefBuilder(ctx(), type).build());
        namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), type).build());

        processProcessorMethods(type, namespace, isOAuth);
        processTransformerMethods(type, namespace);
        processSourceMethods(type, namespace);

        ctx().getStudioModel().setNamespaceType(namespace);
        ctx().getStudioModel().setOutputFileName(EDITOR_XML_FILE_NAME);
    }

    private void processProcessorMethods(Type type, NamespaceType namespace, boolean isOAuth) {
        for (Method processorMethod : type.getMethodsAnnotatedWith(Processor.class)) {
            PatternType cloudConnector = new PatternTypeBuilder(ctx(), processorMethod, type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(cloudConnector));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), processorMethod, type).build());
        }
        
        if (isOAuth) {
        	PatternType authorize = new OAuthPatternTypeBuilder(ctx(), type).build();
        	namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(authorize));
        }
    }

    private void processTransformerMethods(Type type, NamespaceType namespace) {
        List<Method> transformerMethods = type.getMethodsAnnotatedWith(Transformer.class);
        if (!transformerMethods.isEmpty()) {
            namespace.getConnectorOrEndpointOrGlobal().add(new PatternTypeOperationsBuilder(ctx(), type, PatternTypes.TRANSFORMER).build());
            namespace.getConnectorOrEndpointOrGlobal().add(new AbstractTransformerBuilder(ctx(), type).build());
            GlobalType globalTransformer = new GlobalTransformerTypeOperationsBuilder(ctx(), type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
        }
        for (Method transformerMethod : transformerMethods) {
            PatternType transformer = new PatternTypeBuilder(ctx(), transformerMethod, type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeTransformer(transformer));
            GlobalType globalTransformer = new GlobalTransformerTypeBuilder(ctx(), transformerMethod, type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), transformerMethod, type).build());
        }
    }

    private void processSourceMethods(Type type, NamespaceType namespace) {
        List<Method> sourceMethods = type.getMethodsAnnotatedWith(Source.class);
        if (!sourceMethods.isEmpty()) {
            GlobalType abstractGlobalEndpoint = new GlobalEndpointTypeWithNameBuilder(ctx(), type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(abstractGlobalEndpoint));
            EndpointType endpointTypeListingOps = new EndpointTypeOperationsBuilder(ctx(), type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpointTypeListingOps));
            GlobalType globalEndpointListingOps = new GlobalEndpointTypeOperationsBuilder(ctx(), type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpointListingOps));
        }
        for (Method sourceMethod : sourceMethods) {
            EndpointType endpoint = new EndpointTypeBuilder(ctx(), sourceMethod, type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpoint));
            GlobalType globalEndpoint = new GlobalEndpointTypeBuilder(ctx(), sourceMethod, type).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpoint));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), sourceMethod, type).build());
        }
    }
}