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

package org.mule.devkit.model.schema;

import javax.xml.namespace.QName;

public final class SchemaConstants {
    public static final String BASE_NAMESPACE = "http://www.mulesoft.org/schema/mule/";
    public static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    public static final String SPRING_FRAMEWORK_NAMESPACE = "http://www.springframework.org/schema/beans";
    public static final String SPRING_FRAMEWORK_SCHEMA_LOCATION = "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd";
    public static final String MULE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    public static final String MULE_DEVKIT_NAMESPACE = "http://www.mulesoft.org/schema/mule/devkit";
    public static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
    public static final String MULE_DEVKIT_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/devkit/current/mule-devkit.xsd";
    public static final QName MULE_ABSTRACT_EXTENSION = new QName(MULE_NAMESPACE, "abstract-extension", "mule");
    public static final QName MULE_ABSTRACT_EXTENSION_TYPE = new QName(MULE_NAMESPACE, "abstractExtensionType", "mule");
    public static final QName MULE_ANNOTATED_MIXED_CONTENT_TYPE = new QName(MULE_NAMESPACE, "annotatedMixedContentType", "mule");
    public static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR = new QName(MULE_NAMESPACE, "abstract-message-processor", "mule");
    public static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE = new QName(MULE_NAMESPACE, "abstractMessageProcessorType", "mule");
    public static final QName MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR = new QName(MULE_NAMESPACE, "abstract-intercepting-message-processor", "mule");
    public static final QName MULE_ABSTRACT_INTERCEPTING_MESSAGE_PROCESSOR_TYPE = new QName(MULE_NAMESPACE, "abstractInterceptingMessageProcessorType", "mule");
    public static final QName MULE_ABSTRACT_TRANSFORMER = new QName(MULE_NAMESPACE, "abstract-transformer", "mule");
    public static final QName MULE_ABSTRACT_TRANSFORMER_TYPE = new QName(MULE_NAMESPACE, "abstractTransformerType", "mule");
    public static final QName MULE_ABSTRACT_INBOUND_ENDPOINT = new QName(MULE_NAMESPACE, "abstract-inbound-endpoint", "mule");
    public static final QName MULE_ABSTRACT_INBOUND_ENDPOINT_TYPE = new QName(MULE_NAMESPACE, "abstractInboundEndpointType", "mule");
    public static final QName MULE_ABSTRACT_POOLING_PROFILE = new QName(MULE_NAMESPACE, "abstract-pooling-profile", "mule");
    public static final QName MULE_POOLING_PROFILE_TYPE = new QName(MULE_NAMESPACE, "poolingProfileType", "mule");
    public static final QName MULE_MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT_TYPE = new QName(MULE_NAMESPACE, "messageProcessorOrOutboundEndpoint", "mule");
    public static final QName MULE_DEVKIT_JAVA_CLASS_TYPE = new QName(MULE_DEVKIT_NAMESPACE, "javaClass", "devkit");
    public static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
    public static final QName DECIMAL = new QName(XSD_NAMESPACE, "decimal", "xs");
    public static final QName FLOAT = new QName(XSD_NAMESPACE, "float", "xs");
    public static final QName INTEGER = new QName(XSD_NAMESPACE, "integer", "xs");
    public static final QName DOUBLE = new QName(XSD_NAMESPACE, "double", "xs");
    public static final QName DATETIME = new QName(XSD_NAMESPACE, "dateTime", "xs");
    public static final QName LONG = new QName(XSD_NAMESPACE, "long", "xs");
    public static final QName BYTE = new QName(XSD_NAMESPACE, "byte", "xs");
    public static final QName BOOLEAN = new QName(XSD_NAMESPACE, "boolean", "xs");
    public static final QName ANYURI = new QName(XSD_NAMESPACE, "anyURI", "xs");
    public static final String USE_REQUIRED = "required";
    public static final String USE_OPTIONAL = "optional";
    public static final String ELEMENT_NAME_CONFIG = "config";
    public static final String DOMAIN_ATTRIBUTE_NAME = "domain";
    public static final String LOCAL_PORT_ATTRIBUTE_NAME = "localPort";
    public static final String REMOTE_PORT_ATTRIBUTE_NAME = "remotePort";
    public static final String ASYNC_ATTRIBUTE_NAME = "async";
    public static final String HTTP_CALLBACK_CONFIG_ELEMENT_NAME = "http-callback-config";
    public static final String OAUTH_CALLBACK_CONFIG_ELEMENT_NAME = "oauth-callback-config";
    public static final String REF_SUFFIX = "-ref";
    public static final String FLOW_REF_SUFFIX = "-flow-ref";
    public static final String INNER_PREFIX = "inner-";
    public static final String ATTRIBUTE_NAME_CONFIG_REF = "config-ref";
    public static final String ATTRIBUTE_NAME_KEY = "key";
    public static final String ATTRIBUTE_NAME_REF = "ref";
    public static final String ATTRIBUTE_NAME_VALUE_REF = "value-ref";
    public static final String ATTRIBUTE_NAME_KEY_REF = "key-ref";
    public static final String ATTRIBUTE_RETRY_MAX = "retryMax";
    public static final String XSD_EXTENSION = ".xsd";
    public static final String ENUM_TYPE_SUFFIX = "EnumType";
    public static final String OBJECT_TYPE_SUFFIX = "ObjectType";
    public static final String TYPE_SUFFIX = "Type";
    public static final String XML_TYPE_SUFFIX = "XmlType";
    public static final String UNBOUNDED = "unbounded";
    public static final String LAX = "lax";
    public static final String ATTRIBUTE_NAME_NAME = "name";
    public static final String DOMAIN_DEFAULT_VALUE = "${fullDomain}";
    public static final String PORT_DEFAULT_VALUE = "${http.port}";
    public static final String ASYNC_DEFAULT_VALUE = "true";
    public static final String ATTRIBUTE_RETRY_MAX_DESCRIPTION = "Specify how many times this operation can be retried automatically.";
    public static final String ATTRIBUTE_NAME_REF_DESCRIPTION = "The reference object for this parameter";
    public static final String ATTRIBUTE_NAME_NAME_DESCRIPTION = "Give a name to this configuration so it can be later referenced by config-ref.";
    public static final String CONNECTION_POOLING_PROFILE = "connection-pooling-profile";
    public static final String CONNECTION_POOLING_PROFILE_ELEMENT_DESCRIPTION = "Characteristics of the connection pool.";
    public static final String POOLING_PROFILE_ELEMENT = "pooling-profile";
    public static final String POOLING_PROFILE_ELEMENT_DESCRIPTION = "Characteristics of the object pool.";
    public static final String OAUTH_SAVE_ACCESS_TOKEN_ELEMENT = "oauth-save-access-token";
    public static final String OAUTH_RESTORE_ACCESS_TOKEN_ELEMENT = "oauth-restore-access-token";
    public static final String OAUTH_SAVE_ACCESS_TOKEN_ELEMENT_DESCRIPTION = "A chain of message processors processed synchronously that can be used to save OAuth state. They will be executed once the connector acquires an OAuth access token.";
    public static final String OAUTH_RESTORE_ACCESS_TOKEN_ELEMENT_DESCRIPTION = "A chain of message processors processed synchronously that can be used to restore OAuth state. They will be executed whenever access to a protected resource is requested and the connector is not authorized yet.";
}
