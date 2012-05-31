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

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class DefaultDevKitTypeElement extends TypeElementImpl implements DevKitTypeElement {

    public DefaultDevKitTypeElement(TypeElement typeElement) {
        super(typeElement);
    }

    @Override
    public boolean needsConfig() {
        boolean needsConfig = false;

        for (VariableElement variable : getFieldsAnnotatedWith(Configurable.class)) {
            //if (variable.getAnnotation(Optional.class) == null) {
            needsConfig = true;
            //}
        }

        if (typeElement.getAnnotation(OAuth.class) != null ||
                typeElement.getAnnotation(OAuth2.class) != null) {
            needsConfig = true;
        }

        return needsConfig;
    }

    @Override
    public boolean hasProcessorMethodWithParameter(Class<?> parameterType) {
        for (ExecutableElement method : getMethodsAnnotatedWith(Processor.class)) {
            for (VariableElement parameter : method.getParameters()) {
                if (parameter.asType().toString().startsWith(parameterType.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasConfigurableWithType(Class<?> parameterType) {
        for (VariableElement field : getFieldsAnnotatedWith(Configurable.class)) {
            if (field.asType().toString().startsWith(parameterType.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasProcessorMethodWithParameterListOf(Class<?> listGenericType) {
        for (ExecutableElement method : getMethodsAnnotatedWith(Processor.class)) {
            for (VariableElement parameter : method.getParameters()) {
                if (parameter.asType().toString().startsWith(List.class.getName())) {
                    List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
                    if (!typeArguments.isEmpty() && typeArguments.get(0).toString().equals(listGenericType.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return typeElement.getAnnotation(annotation) != null;
    }

    @Override
    public List<DevKitExecutableElement> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        List<DevKitExecutableElement> result = new ArrayList<DevKitExecutableElement>();
        for (DevKitExecutableElement method : getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                result.add(method);
            }
        }
        return result;
    }

    @Override
    public List<DevKitExecutableElement> getMethodsWhoseParametersAreAnnotatedWith(Class<? extends Annotation> annotation) {
        List<DevKitExecutableElement> result = new ArrayList<DevKitExecutableElement>();
        for (DevKitExecutableElement method : getMethods()) {
            for (VariableElement parameter : method.getParameters()) {
                if (parameter.getAnnotation(annotation) != null) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    @Override
    public List<DevKitFieldElement> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
        List<DevKitFieldElement> result = new ArrayList<DevKitFieldElement>();
        for (DevKitFieldElement field : getFields()) {
            if (field.getAnnotation(annotation) != null) {
                result.add(field);
            }
        }
        return result;
    }

    @Override
    public boolean hasMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        for (ExecutableElement method : getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasFieldAnnotatedWith(Class<? extends Annotation> annotation) {
        for (VariableElement field : getFields()) {
            if (field.getAnnotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<DevKitFieldElement> getFields() {
        List<DevKitFieldElement> fields = new ArrayList<DevKitFieldElement>();
        for(VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements()) ) {
            fields.add(new DefaultDevKitFieldElement(variableElement));
        }

        return fields;
    }

    public TypeElement unWrap() {
        return typeElement;
    }

    @Override
    public List<DevKitExecutableElement> getMethods() {
        List<DevKitExecutableElement> methods = new ArrayList<DevKitExecutableElement>();
        for(ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements()) ) {
            methods.add(new DefaultDevKitExecutableElement(executableElement));
        }

        return methods;
    }

    @Override
    public boolean isInterface() {
        return typeElement.getKind() == ElementKind.INTERFACE;
    }

    @Override
    public boolean isParametrized() {
        return !typeElement.getTypeParameters().isEmpty();
    }

    @Override
    public boolean isPublic() {
        return typeElement.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public TypeElement getInnerTypeElement() {
        return typeElement;
    }

    @Override
    public boolean isModuleOrConnector() {
        return hasAnnotation(Module.class) || hasAnnotation(Connector.class);
    }

    @Override
    public boolean isPoolable() {
        if (hasAnnotation(Module.class) &&
                getAnnotation(Module.class).poolable()) {
            return true;
        }

        return false;
    }

    @Override
    public String minMuleVersion() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).minMuleVersion();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).minMuleVersion();
        }
        if (hasAnnotation(ExpressionLanguage.class)) {
            return getAnnotation(ExpressionLanguage.class).minMuleVersion();
        }

        return null;
    }

    @Override
    public String namespace() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).namespace();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).namespace();
        }

        return null;
    }

    @Override
    public String name() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).name();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).name();
        }

        return null;
    }

    @Override
    public String schemaLocation() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).schemaLocation();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).schemaLocation();
        }

        return null;
    }

    @Override
    public String schemaVersion() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).schemaVersion();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).schemaVersion();
        }

        return null;
    }

    @Override
    public boolean usesConnectionManager() {
        return hasMethodsAnnotatedWith(Connect.class) && hasMethodsAnnotatedWith(Disconnect.class);
    }

    @Override
    public String friendlyName() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).friendlyName();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).friendlyName();
        }
        return null;
    }

    @Override
    public String description() {
        if (hasAnnotation(Module.class)) {
            return getAnnotation(Module.class).description();
        }
        if (hasAnnotation(Connector.class)) {
            return getAnnotation(Connector.class).description();
        }
        return null;
    }
}