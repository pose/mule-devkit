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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class VariableElementImpl implements VariableElement {

    private VariableElement variableElement;

    public VariableElementImpl(VariableElement variableElement) {
        this.variableElement = variableElement;
    }

    @Override
    public Object getConstantValue() {
        return variableElement.getConstantValue();
    }

    @Override
    public TypeMirror asType() {
        return variableElement.asType();
    }

    @Override
    public ElementKind getKind() {
        return variableElement.getKind();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return variableElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return variableElement.getAnnotation(aClass);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return variableElement.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return variableElement.getSimpleName();
    }

    @Override
    public Element getEnclosingElement() {
        return variableElement.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return variableElement.getEnclosedElements();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> rpElementVisitor, P p) {
        return variableElement.accept(rpElementVisitor, p);
    }
}
