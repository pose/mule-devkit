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

package org.mule.devkit.utils;

import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
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
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.devkit.model.DevKitElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TypeMirrorUtils {
    private static final List<Class<?>> PARAMETER_TYPES_TO_IGNORE = Arrays.asList(SourceCallback.class, MuleMessage.class);
    private static final List<Class<? extends Annotation>> PARAMETERS_ANNOTATIONS_TO_IGNORE =
            Arrays.asList(InboundHeaders.class, InvocationHeaders.class, OutboundHeaders.class,
                    SessionHeaders.class, Payload.class, OAuthAccessToken.class,
                    OAuthAccessTokenSecret.class, ExceptionPayload.class, CorrelationId.class,
                    CorrelationSequence.class, CorrelationGroupSize.class, MessageUniqueId.class,
                    MessageRootId.class);

    private Types types;

    public TypeMirrorUtils(Types types) {
        this.types = types;
    }

    public boolean ignoreParameter(DevKitElement element) {
        String variableType = element.asType().toString();
        for (Class<?> typeToIgnore : PARAMETER_TYPES_TO_IGNORE) {
            if (variableType.contains(typeToIgnore.getName())) {
                return true;
            }
        }
        for (Class<? extends Annotation> annotationToIgnore : PARAMETERS_ANNOTATIONS_TO_IGNORE) {
            if (element.getAnnotation(annotationToIgnore) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isString(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(String.class.getName());
    }

    public boolean isBoolean(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Boolean.class.getName()) || className.startsWith("boolean");
    }

    public boolean isInteger(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Integer.class.getName()) || className.startsWith("int");
    }

    public boolean isLong(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Long.class.getName()) || className.startsWith("long");
    }

    public boolean isFloat(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Float.class.getName()) || className.startsWith("float");
    }

    public boolean isDouble(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Double.class.getName()) || className.startsWith("double");
    }

    public boolean isChar(DevKitElement element) {
        String className = element.asType().toString();
        return className.startsWith(Character.class.getName()) || className.startsWith("char");
    }

    public boolean isHttpCallback(DevKitElement element) {
        return element.asType().toString().startsWith(HttpCallback.class.getName());
    }

    public boolean isURL(DevKitElement element) {
        return element.asType().toString().startsWith(URL.class.getName());
    }

    public boolean isDate(DevKitElement element) {
        return element.asType().toString().startsWith(Date.class.getName());
    }

    public boolean isBigDecimal(DevKitElement element) {
        return element.asType().toString().startsWith(BigDecimal.class.getName());
    }

    public boolean isBigInteger(DevKitElement element) {
        return element.asType().toString().startsWith(BigInteger.class.getName());
    }

    public String getJavaType(DevKitElement element) {
        return element.asType().toString();
    }

    public String getJavaType(ExecutableElement element) {
        return element.getReturnType().toString();
    }
}