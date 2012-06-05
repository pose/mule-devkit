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
package org.mule.devkit.model;

import org.mule.api.MuleMessage;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
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
import org.mule.api.callback.SourceCallback;

import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

public class DefaultDevKitParameterElement extends DefaultDevKitVariableElement<DevKitExecutableElement> implements DevKitParameterElement {
    private static final List<Class<?>> PARAMETER_TYPES_TO_IGNORE = Arrays.asList(SourceCallback.class, MuleMessage.class);
    private static final List<Class<? extends Annotation>> PARAMETERS_ANNOTATIONS_TO_IGNORE =
            Arrays.asList(InboundHeaders.class, InvocationHeaders.class, OutboundHeaders.class,
                    SessionHeaders.class, Payload.class, OAuthAccessToken.class,
                    OAuthAccessTokenSecret.class, ExceptionPayload.class, CorrelationId.class,
                    CorrelationSequence.class, CorrelationGroupSize.class, MessageUniqueId.class,
                    MessageRootId.class);

    public DefaultDevKitParameterElement(VariableElement element, DevKitExecutableElement parent, Types types) {
        super(element, parent, types);
    }

    @Override
    public boolean shouldBeIgnored() {
        String variableType = innerElement.asType().toString();
        for (Class<?> typeToIgnore : PARAMETER_TYPES_TO_IGNORE) {
            if (variableType.contains(typeToIgnore.getName())) {
                return true;
            }
        }
        for (Class<? extends Annotation> annotationToIgnore : PARAMETERS_ANNOTATIONS_TO_IGNORE) {
            if (innerElement.getAnnotation(annotationToIgnore) != null) {
                return true;
            }
        }
        return false;
    }
}
