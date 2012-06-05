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

package org.mule.devkit.generation.mule.studio.editor;

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.studio.EndpointType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.NamespaceType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;

import java.util.List;

public class MuleStudioEditorXmlGenerator extends AbstractMessageGenerator {

    public static final String EDITOR_XML_FILE_NAME = "editors.xml";
    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_CAPTION = "General";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION = "General";
    public static final String CONNECTION_ATTRIBUTE_CATEGORY_CAPTION = "Connection";
    public static final String GROUP_DEFAULT_CAPTION = "Generic";
    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !ctx().hasOption("skipStudioPluginPackage");
    }

    @Override
    public void generate(DevKitTypeElement typeElement) {
        String moduleName = typeElement.name();
        boolean isOAuth = typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class);

        NamespaceType namespace = new NamespaceType();
        namespace.setPrefix(moduleName);
        namespace.setUrl(URI_PREFIX + moduleName);

        PatternTypeOperationsBuilder operationsBuilder = isOAuth
        				? new OAuthPatternTypeOperationsBuilder(ctx(), typeElement, PatternTypes.CLOUD_CONNECTOR)
        				: new PatternTypeOperationsBuilder(ctx(), typeElement, PatternTypes.CLOUD_CONNECTOR);
        
        GlobalType globalCloudConnector = new GlobalCloudConnectorTypeBuilder(ctx(), typeElement).build();
        namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector));
        namespace.getConnectorOrEndpointOrGlobal().add(operationsBuilder.build());
        namespace.getConnectorOrEndpointOrGlobal().add(new ConfigRefBuilder(ctx(), typeElement).build());
        namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), typeElement).build());

        processProcessorMethods(typeElement, namespace, isOAuth);
        processTransformerMethods(typeElement, namespace);
        processSourceMethods(typeElement, namespace);

        ctx().getStudioModel().setNamespaceType(namespace);
        ctx().getStudioModel().setOutputFileName(EDITOR_XML_FILE_NAME);
    }

    private void processProcessorMethods(DevKitTypeElement typeElement, NamespaceType namespace, boolean isOAuth) {
        for (DevKitExecutableElement processorMethod : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            PatternType cloudConnector = new PatternTypeBuilder(ctx(), processorMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(cloudConnector));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), processorMethod, typeElement).build());
        }
        
        if (isOAuth) {
        	PatternType authorize = new OAuthPatternTypeBuilder(ctx(), typeElement).build();
        	namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(authorize));
        }
    }

    private void processTransformerMethods(DevKitTypeElement typeElement, NamespaceType namespace) {
        List<DevKitExecutableElement> transformerMethods = typeElement.getMethodsAnnotatedWith(Transformer.class);
        if (!transformerMethods.isEmpty()) {
            namespace.getConnectorOrEndpointOrGlobal().add(new PatternTypeOperationsBuilder(ctx(), typeElement, PatternTypes.TRANSFORMER).build());
            namespace.getConnectorOrEndpointOrGlobal().add(new AbstractTransformerBuilder(ctx(), typeElement).build());
            GlobalType globalTransformer = new GlobalTransformerTypeOperationsBuilder(ctx(), typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
        }
        for (DevKitExecutableElement transformerMethod : transformerMethods) {
            PatternType transformer = new PatternTypeBuilder(ctx(), transformerMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeTransformer(transformer));
            GlobalType globalTransformer = new GlobalTransformerTypeBuilder(ctx(), transformerMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), transformerMethod, typeElement).build());
        }
    }

    private void processSourceMethods(DevKitTypeElement typeElement, NamespaceType namespace) {
        List<DevKitExecutableElement> sourceMethods = typeElement.getMethodsAnnotatedWith(Source.class);
        if (!sourceMethods.isEmpty()) {
            GlobalType abstractGlobalEndpoint = new GlobalEndpointTypeWithNameBuilder(ctx(), typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(abstractGlobalEndpoint));
            EndpointType endpointTypeListingOps = new EndpointTypeOperationsBuilder(ctx(), typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpointTypeListingOps));
            GlobalType globalEndpointListingOps = new GlobalEndpointTypeOperationsBuilder(ctx(), typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpointListingOps));
        }
        for (DevKitExecutableElement sourceMethod : sourceMethods) {
            EndpointType endpoint = new EndpointTypeBuilder(ctx(), sourceMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpoint));
            GlobalType globalEndpoint = new GlobalEndpointTypeBuilder(ctx(), sourceMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpoint));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(ctx(), sourceMethod, typeElement).build());
        }
    }
}