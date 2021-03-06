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

import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.devkit.generation.api.AnnotationVerificationException;
import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.lang.model.type.TypeKind;
import java.lang.annotation.Annotation;
import java.util.List;

public class LifecycleAnnotationVerifier implements AnnotationVerifier {

    @Override
    public boolean shouldVerify(Type type) {
        return true;
    }

    @Override
    public void verify(Type type) throws AnnotationVerificationException {
        check(type, PostConstruct.class);
        check(type, Start.class);
        check(type, Stop.class);
        check(type, PreDestroy.class);
    }

    private void check(Type type, Class<? extends Annotation> annotation) throws AnnotationVerificationException {
        List<Method> methods = type.getMethodsAnnotatedWith(annotation);
        if (methods.isEmpty()) {
            return;
        }
        if (methods.size() > 1) {
            throw new AnnotationVerificationException(type, "Cannot have more than method annotated with " + annotation.getSimpleName());
        }
        Method method = methods.get(0);
        if (!method.getParameters().isEmpty()) {
            throw new AnnotationVerificationException(type, "A method annotated with " + annotation.getSimpleName() + " cannot receive any paramters");
        }
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            throw new AnnotationVerificationException(type, "A method annotated with " + annotation.getSimpleName() + " can only return void");
        }
        if (!method.isStatic()) {
            throw new AnnotationVerificationException(method, "A method annotated with " + annotation.getSimpleName() + " cannot be static");
        }
        if (!method.isPublic()) {
            throw new AnnotationVerificationException(method, "A method annotated with " + annotation.getSimpleName() + " can only be public");
        }
    }
}