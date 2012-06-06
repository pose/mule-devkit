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

package org.mule.devkit.validation;

import org.mule.api.annotations.Processor;
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
import org.mule.devkit.Context;
import org.mule.devkit.ValidationException;
import org.mule.devkit.Validator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

import javax.lang.model.type.TypeKind;
import java.util.List;

public class ProcessorValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, Context context) {
        return type.isModuleOrConnector();
    }

    @Override
    public void validate(Type type, Context context) throws ValidationException {

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {

            if (method.isStatic()) {
                throw new ValidationException(method, "@Processor cannot be applied to a static method");
            }

            if (!method.getTypeParameters().isEmpty()) {
                throw new ValidationException(method, "@Processor cannot be applied to a generic method");
            }

            if (!method.isPublic()) {
                throw new ValidationException(method, "@Processor cannot be applied to a non-public method");
            }

            validateIntercepting(method);

            for (Parameter parameter : method.getParameters()) {
                int count = 0;
                if (parameter.getAnnotation(InboundHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(OutboundHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(InvocationHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(SessionHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(Payload.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(ExceptionPayload.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(CorrelationId.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(CorrelationSequence.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(CorrelationGroupSize.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(MessageUniqueId.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(MessageRootId.class) != null) {
                    count++;
                }

                if (count > 1) {
                    throw new ValidationException(parameter, "You cannot have more than one of InboundHeader, InvocationHeaders or Payload annotation");
                }

                if (parameter.getAnnotation(Payload.class) == null && parameter.asType().getKind() == TypeKind.ARRAY) {
                    throw new ValidationException(parameter, "@Processor parameter cannot be arrays, use List instead");
                }
            }
        }
    }

    private void validateIntercepting(Method method) throws ValidationException {
        if (method.getAnnotation(Processor.class).intercepting()) {
            boolean containsSourceCallback = false;
            List<Parameter> parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.asType().toString().startsWith(SourceCallback.class.getName())) {
                    containsSourceCallback = true;
                }
            }

            if (!containsSourceCallback) {
                throw new ValidationException(method, "An intercepting method method must contain a SourceCallback as one of its parameters");
            }
        }
    }
}