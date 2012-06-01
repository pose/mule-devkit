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
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class DefaultDevKitElement<T extends Element, P extends DevKitElement> implements DevKitElement<T, P> {
    protected T innerElement;
    protected P parent;

    public DefaultDevKitElement(T element, P parent) {
        this.innerElement = element;
        this.parent = parent;
    }

    public P parent() {
        return this.parent;
    }

    public T unwrap() {
        return innerElement;
    }

    @Override
    public TypeMirror asType() {
        return innerElement.asType();
    }

    @Override
    public ElementKind getKind() {
        return innerElement.getKind();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return innerElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return innerElement.getAnnotation(aClass);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return innerElement.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return innerElement.getSimpleName();
    }

    @Override
    public Element getEnclosingElement() {
        return innerElement.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return innerElement.getEnclosedElements();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> rpElementVisitor, P p) {
        return innerElement.accept(rpElementVisitor, p);
    }
}
