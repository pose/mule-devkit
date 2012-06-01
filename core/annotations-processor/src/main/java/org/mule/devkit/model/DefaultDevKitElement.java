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

import org.mule.api.NestedProcessor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultDevKitElement<T extends Element, P extends DevKitElement> implements DevKitElement<T, P> {
    protected T innerElement;
    protected P parent;
    protected Types types;

    public DefaultDevKitElement(T element, P parent, Types types) {
        this.innerElement = element;
        this.parent = parent;
        this.types = types;
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
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return innerElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return (A) innerElement.getAnnotation(aClass);
    }

    @Override
    public Name getSimpleName() {
        return innerElement.getSimpleName();
    }


    @Override
    public boolean isXmlType() {
        if (asType().getKind() == TypeKind.DECLARED) {

            DeclaredType declaredType = (DeclaredType) asType();
            XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

            if (xmlType != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPublic() {
        return innerElement.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isPrivate() {
        return innerElement.getModifiers().contains(Modifier.PRIVATE);
    }

    @Override
    public boolean isProtected() {
        return innerElement.getModifiers().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isAbstract() {
        return innerElement.getModifiers().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isFinal() {
        return innerElement.getModifiers().contains(Modifier.FINAL);
    }

    @Override
    public boolean isStatic() {
        return innerElement.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public boolean isCollection() {
        return isArrayOrList(asType()) || isMap(asType());
    }

    @Override
    public boolean isNestedProcessor() {
        if (asType().toString().startsWith(NestedProcessor.class.getName())) {
            return true;
        }

        if (asType().toString().startsWith(List.class.getName())) {
            DeclaredType variableType = (DeclaredType) asType();
            List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();
            if (variableTypeParameters.isEmpty()) {
                return false;
            }

            if (variableTypeParameters.get(0).toString().startsWith(NestedProcessor.class.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isArrayOrList() {
        return isArrayOrList(asType());
    }

    private boolean isArrayOrList(TypeMirror type) {
        if (type.toString().equals("byte[]")) {
            return false;
        }

        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }

        if (type.toString().startsWith(List.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isArrayOrList(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMap() {
        return isMap(asType());
    }

    private boolean isMap(TypeMirror type) {
        if (type.toString().startsWith(Map.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isMap(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEnum() {
        return isEnum(asType());
    }

    private boolean isEnum(TypeMirror type) {
        if (type.toString().startsWith(Enum.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isEnum(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<DevKitElement> getTypeArguments() {
        List<DevKitElement> typeArguments = new ArrayList<DevKitElement>();
        DeclaredType declaredType = (DeclaredType)asType();
        for( TypeMirror typeMirror : declaredType.getTypeArguments() ) {
            if( typeMirror instanceof WildcardType) {
                continue;
            }
            typeArguments.add(new DefaultDevKitElement(types.asElement(typeMirror), this, types));
        }

        return typeArguments;
    }

    @Override
    public boolean hasTypeArguments() {
        return ((DeclaredType)asType()).getTypeArguments().size() > 0;
    }
}
