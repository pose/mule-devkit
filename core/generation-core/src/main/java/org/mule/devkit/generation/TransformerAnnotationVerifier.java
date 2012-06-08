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

import org.mule.api.annotations.Transformer;
import org.mule.devkit.generation.api.AnnotationVerificationException;
import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;

public class TransformerAnnotationVerifier implements AnnotationVerifier {

    @Override
    public boolean shouldVerify(Type type) {
        return type.isModuleOrConnector() && type.hasMethodsAnnotatedWith(Transformer.class);
    }

    @Override
    public void verify(Type type) throws AnnotationVerificationException {
        for (Method method : type.getMethodsAnnotatedWith(Transformer.class)) {

            if (!method.isStatic()) {
                throw new AnnotationVerificationException(method, "@Transformer must be a static method");
            }

            if (!method.isPublic()) {
                throw new AnnotationVerificationException(method, "@Transformer cannot be applied to a non-public method");
            }

            if (method.getReturnType().toString().equals("void")) {
                throw new AnnotationVerificationException(method, "@Transformer cannot be void");
            }

            if (method.getReturnType().toString().equals("java.lang.Object")) {
                throw new AnnotationVerificationException(method, "@Transformer cannot return java.lang.Object");
            }

            if (method.getParameters().size() != 1) {
                throw new AnnotationVerificationException(method, "@Transformer must receive exactly one argument.");
            }

            List<? extends AnnotationValue> sourceTypes = getSourceTypes(method);
            if (sourceTypes == null || sourceTypes.isEmpty()) {
                throw new AnnotationVerificationException(method, "@Transformer must have at declare at least one element in the sourceTypes attribute");
            }
        }
    }

    private List<? extends AnnotationValue> getSourceTypes(Method method) {
        String transformerAnnotationName = Transformer.class.getName();
        List<? extends AnnotationMirror> annotationMirrors = method.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (transformerAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    if ("sourceTypes".equals(entry.getKey().getSimpleName().toString())) {
                        return (List<? extends AnnotationValue>) entry.getValue().getValue();
                    }
                }
            }
        }
        return null;
    }
}