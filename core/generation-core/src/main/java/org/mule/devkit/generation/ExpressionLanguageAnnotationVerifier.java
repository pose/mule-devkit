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
package org.mule.devkit.generation;

import org.mule.api.annotations.ExpressionEnricher;
import org.mule.api.annotations.ExpressionEvaluator;
import org.mule.api.annotations.param.CorrelationId;
import org.mule.api.annotations.param.CorrelationSequence;
import org.mule.api.annotations.param.ExceptionPayload;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.annotations.param.SessionHeaders;
import org.mule.devkit.generation.api.AnnotationVerificationException;
import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

public class ExpressionLanguageAnnotationVerifier implements AnnotationVerifier {
    @Override
    public boolean shouldVerify(Type type) {
        return true;
    }

    @Override
    public void verify(Type type) throws AnnotationVerificationException {
        if (type.getMethodsAnnotatedWith(ExpressionEvaluator.class).size() > 1) {
            throw new AnnotationVerificationException(type, "An @ExpressionLanguage can only contain one @ExpressionEvaluator.");
        }
        if (type.getMethodsAnnotatedWith(ExpressionEnricher.class).size() > 1) {
            throw new AnnotationVerificationException(type, "An @ExpressionLanguage can only contain one @ExpressionEnricher.");
        }
        if (type.getMethodsAnnotatedWith(ExpressionEvaluator.class).size() == 0 &&
                type.getMethodsAnnotatedWith(ExpressionEnricher.class).size() == 0) {
            throw new AnnotationVerificationException(type, "An @ExpressionLanguage must contain one @ExpressionEnricher or one @ExpressionEvaluator or both.");
        }
        for (Method executableElement : type.getMethodsAnnotatedWith(ExpressionEvaluator.class)) {
            if (executableElement.getParameters().size() == 0) {
                throw new AnnotationVerificationException(executableElement, "An @ExpressionEvaluator must receive at least a String that represents the expression to evaluate.");
            }

            if (executableElement.getReturnType().toString().equals("void")) {
                throw new AnnotationVerificationException(executableElement, "@ExpressionEvaluator cannot be void");
            }

            boolean expressionStringFound = false;
            for (Parameter parameter : executableElement.getParameters()) {
                if (parameter.getAnnotation(Payload.class) == null &&
                        parameter.getAnnotation(OutboundHeaders.class) == null &&
                        parameter.getAnnotation(InboundHeaders.class) == null &&
                        parameter.getAnnotation(SessionHeaders.class) == null &&
                        parameter.getAnnotation(InvocationHeaders.class) == null &&
                        parameter.getAnnotation(ExceptionPayload.class) == null &&
                        parameter.getAnnotation(CorrelationId.class) == null &&
                        parameter.getAnnotation(CorrelationSequence.class) == null) {
                    if (parameter.asType().toString().contains("String")) {
                        if (expressionStringFound) {
                            throw new AnnotationVerificationException(executableElement, "An @ExpressionEvaluator can receive only one String and the rest of the arguments must be annotated with either @Payload, @InboundHeaders, @OutboundHeaders, @SessionHeader or @InvocationHeaders.");
                        }
                        expressionStringFound = true;
                    } else {
                        throw new AnnotationVerificationException(executableElement, "An @ExpressionEvaluator can receive only one String and the rest of the arguments must be annotated with either @Payload, @InboundHeaders, @OutboundHeaders, @SessionHeader or @InvocationHeaders.");
                    }
                }
            }
        }

        for (Method executableElement : type.getMethodsAnnotatedWith(ExpressionEnricher.class)) {
            if (executableElement.getParameters().size() == 0) {
                throw new AnnotationVerificationException(executableElement, "An @ExpressionEnricher must receive at least a String that represents the expression and Object that represents the object to be used for enrichment.");
            }

            if (!executableElement.getReturnType().toString().equals("void")) {
                throw new AnnotationVerificationException(executableElement, "@ExpressionEnricher must be void");
            }

            boolean expressionStringFound = false;
            boolean enrichObjectFound = false;
            for (Parameter parameter : executableElement.getParameters()) {
                if (parameter.getAnnotation(Payload.class) == null &&
                        parameter.getAnnotation(OutboundHeaders.class) == null &&
                        parameter.getAnnotation(InboundHeaders.class) == null &&
                        parameter.getAnnotation(SessionHeaders.class) == null &&
                        parameter.getAnnotation(InvocationHeaders.class) == null &&
                        parameter.getAnnotation(ExceptionPayload.class) == null &&
                        parameter.getAnnotation(CorrelationId.class) == null &&
                        parameter.getAnnotation(CorrelationSequence.class) == null) {
                    if (parameter.asType().toString().contains("String")) {
                        if (expressionStringFound) {
                            throw new AnnotationVerificationException(executableElement, "An @ExpressionEnricher can receive only one Object and one String and the rest of the arguments must be annotated with either @Payload, @InboundHeaders, @OutboundHeaders, @SessionHeader or @InvocationHeaders.");
                        }
                        expressionStringFound = true;
                    } else if (parameter.asType().toString().contains("Object")) {
                        if (enrichObjectFound) {
                            throw new AnnotationVerificationException(executableElement, "An @ExpressionEnricher can receive only one Object and one String and the rest of the arguments must be annotated with either @Payload, @InboundHeaders, @OutboundHeaders, @SessionHeader or @InvocationHeaders.");
                        }
                        enrichObjectFound = true;
                    } else {
                        throw new AnnotationVerificationException(executableElement, "An @ExpressionEnricher can receive only one Object and one String and the rest of the arguments must be annotated with either @Payload, @InboundHeaders, @OutboundHeaders, @SessionHeader or @InvocationHeaders.");
                    }
                }
            }

        }
    }
}
