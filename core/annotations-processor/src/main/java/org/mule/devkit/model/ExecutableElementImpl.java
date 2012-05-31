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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class ExecutableElementImpl implements ExecutableElement {
    private ExecutableElement executableElement;

    public ExecutableElementImpl(ExecutableElement executableElement) {
        this.executableElement = executableElement;
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return executableElement.getTypeParameters();
    }

    @Override
    public TypeMirror getReturnType() {
        return executableElement.getReturnType();
    }

    @Override
    public List<? extends VariableElement> getParameters() {
        return executableElement.getParameters();
    }

    @Override
    public boolean isVarArgs() {
        return executableElement.isVarArgs();
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        return executableElement.getThrownTypes();
    }

    @Override
    public AnnotationValue getDefaultValue() {
        return executableElement.getDefaultValue();
    }

    @Override
    public TypeMirror asType() {
        return executableElement.asType();
    }

    @Override
    public ElementKind getKind() {
        return executableElement.getKind();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return executableElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return executableElement.getAnnotation(aClass);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return executableElement.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return executableElement.getSimpleName();
    }

    @Override
    public Element getEnclosingElement() {
        return executableElement.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return executableElement.getEnclosedElements();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> rpElementVisitor, P p) {
        return executableElement.accept(rpElementVisitor, p );
    }
}
