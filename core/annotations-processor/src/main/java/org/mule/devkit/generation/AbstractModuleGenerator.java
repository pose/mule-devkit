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

import org.apache.commons.lang.StringUtils;
import org.mule.api.Capability;
import org.mule.api.MuleContext;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldRef;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;

public abstract class AbstractModuleGenerator extends AbstractGenerator {

    protected static final String MULE_CONTEXT_FIELD_NAME = "muleContext";

    public org.mule.devkit.model.code.Type ref(Type type) {
        return ctx().getCodeModel().ref(type.asType());
    }

    public org.mule.devkit.model.code.Type ref(TypeMirror typeMirror) {
        return ctx().getCodeModel().ref(typeMirror);
    }

    public TypeReference ref(Class<?> clazz) {
        return ctx().getCodeModel().ref(clazz);
    }

    public org.mule.devkit.model.code.Type ref(String fullyQualifiedClassName) {
        return ctx().getCodeModel().ref(fullyQualifiedClassName);
    }

    protected FieldVariable generateLoggerField(DefinedClass clazz) {
        return clazz.field(Modifier.PRIVATE | Modifier.STATIC, ref(Logger.class), "logger",
                ref(LoggerFactory.class).staticInvoke("getLogger").arg(clazz.dotclass()));
    }

    protected org.mule.devkit.model.code.Method generateSetter(DefinedClass clazz, FieldVariable field) {
        org.mule.devkit.model.code.Method setter = clazz.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Sets " + field.name());
        setter.javadoc().addParam("value Value to set");
        Variable value = setter.param(field.type(), "value");
        setter.body().assign(ExpressionFactory._this().ref(field), value);

        return setter;
    }

    protected org.mule.devkit.model.code.Method generateGetter(DefinedClass clazz, FieldVariable field) {
        org.mule.devkit.model.code.Method setter = clazz.method(Modifier.PUBLIC, field.type(), "get" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Retrieves " + field.name());
        setter.body()._return(ExpressionFactory._this().ref(field));

        return setter;
    }

    protected FieldVariable generateFieldForMuleContext(DefinedClass messageProcessorClass) {
        FieldVariable muleContext = messageProcessorClass.field(Modifier.PRIVATE, ref(MuleContext.class), MULE_CONTEXT_FIELD_NAME);
        muleContext.javadoc().add("Mule Context");

        return muleContext;
    }

    protected Expression isNull(Expression expression) {
        return Op.eq(expression, ExpressionFactory._null());
    }

    protected String getterMethodForFieldAnnotatedWith(Type type, Class<? extends Annotation> annotation) {
        return methodForFieldAnnotatedWith(type, annotation, "get");
    }

    private String methodForFieldAnnotatedWith(Type type, Class<? extends Annotation> annotation, String prefix) {
        List<Field> fields = type.getFields();
        for (Field field : fields) {
            if (field.getAnnotation(annotation) != null) {
                return prefix + StringUtils.capitalize(field.getSimpleName().toString());
            }
        }
        return null;
    }

    protected Method connectMethodForClass(Type type) {
        List<Method> connectMethods = type.getMethodsAnnotatedWith(Connect.class);
        return !connectMethods.isEmpty() ? connectMethods.get(0) : null;
    }

    protected Method validateConnectionMethodForClass(Type type) {
        List<Method> connectMethods = type.getMethodsAnnotatedWith(ValidateConnection.class);
        return !connectMethods.isEmpty() ? connectMethods.get(0) : null;
    }

    protected Method disconnectMethodForClass(Type type) {
        List<Method> disconnectMethods = type.getMethodsAnnotatedWith(Disconnect.class);
        return !disconnectMethods.isEmpty() ? disconnectMethods.get(0) : null;
    }

    protected Method connectionIdentifierMethodForClass(Type type) {
        List<Method> connectionIdentifierMethods = type.getMethodsAnnotatedWith(ConnectionIdentifier.class);
        return !connectionIdentifierMethods.isEmpty() ? connectionIdentifierMethods.get(0) : null;
    }

    protected Method connectForMethod(Method executableElement) {
        return connectMethodForClass(executableElement.parent());
    }

    protected Method connectionIdentifierForMethod(Method executableElement) {
        return connectionIdentifierMethodForClass(executableElement.parent());
    }

    protected void generateIsCapableOf(Type type, DefinedClass capabilitiesAdapter) {
        org.mule.devkit.model.code.Method isCapableOf = capabilitiesAdapter.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "isCapableOf");
        Variable capability = isCapableOf.param(ref(Capability.class), "capability");
        isCapableOf.javadoc().add("Returns true if this module implements such capability");

        addCapability(isCapableOf, capability, ref(Capability.class).staticRef("LIFECYCLE_CAPABLE"));

        if (type.hasAnnotation(OAuth2.class)) {
            addCapability(isCapableOf, capability, ref(Capability.class).staticRef("OAUTH2_CAPABLE"));
        }

        if (type.hasAnnotation(OAuth.class)) {
            addCapability(isCapableOf, capability, ref(Capability.class).staticRef("OAUTH1_CAPABLE"));
        }

        if (type.isPoolable()) {
            addCapability(isCapableOf, capability, ref(Capability.class).staticRef("POOLING_CAPABLE"));
        }

        Method connectMethod = connectMethodForClass(type);
        Method disconnectMethod = disconnectMethodForClass(type);

        if (connectMethod != null && disconnectMethod != null) {
            addCapability(isCapableOf, capability, ref(Capability.class).staticRef("CONNECTION_MANAGEMENT_CAPABLE"));
        }

        isCapableOf.body()._return(ExpressionFactory.FALSE);
    }

    private void addCapability(org.mule.devkit.model.code.Method capableOf, Variable capability, FieldRef capabilityToCheckFor) {
        Conditional isCapable = capableOf.body()._if(Op.eq(capability, capabilityToCheckFor));
        isCapable._then()._return(ExpressionFactory.TRUE);
    }
}