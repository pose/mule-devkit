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

package org.mule.devkit.generation.adapter;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;

public class LifecycleAdapterFactoryGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return (type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class)) && type.isPoolable();
    }

    @Override
    public void generate(Type type) {
        DefinedClass lifecycleAdapterFactory = getLifecycleAdapterFactoryClass(type);
        lifecycleAdapterFactory.javadoc().add("A <code>" + lifecycleAdapterFactory.name() + "</code> is an implementation  ");
        lifecycleAdapterFactory.javadoc().add(" of {@link ObjectFactory} interface for ");
        lifecycleAdapterFactory.javadoc().add(ref(type.asType()));

        DefinedClass poolObjectClass = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));
        poolObjectClass.role(DefinedClassRoles.POOL_OBJECT, ref(type));

        generateFields(type, lifecycleAdapterFactory);

        generateInitialiseMethod(lifecycleAdapterFactory);
        generateDisposeMethod(lifecycleAdapterFactory);
        generateGetInstanceMethod(type, lifecycleAdapterFactory, poolObjectClass);
        generateGetObjectClassMethod(lifecycleAdapterFactory, poolObjectClass);
        generateAddObjectInitialisationCallback(lifecycleAdapterFactory);
        generateIsSingleton(lifecycleAdapterFactory);
        generateIsAutoWireObject(lifecycleAdapterFactory);
        generateIsExternallyManagedLifecycle(lifecycleAdapterFactory);
    }

    private void generateIsExternallyManagedLifecycle(DefinedClass lifecycleAdapterFactory) {
        Method isExternallyManagedLifecycle = lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "isExternallyManagedLifecycle");
        isExternallyManagedLifecycle.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsAutoWireObject(DefinedClass lifecycleAdapterFactory) {
        Method isAutoWireObject = lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "isAutoWireObject");
        isAutoWireObject.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsSingleton(DefinedClass lifecycleAdapterFactory) {
        Method isSingleton = lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "isSingleton");
        isSingleton.body()._return(ExpressionFactory.FALSE);
    }

    private void generateAddObjectInitialisationCallback(DefinedClass lifecycleAdapterFactory) {
        Method addObjectInitialisationCallback = lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "addObjectInitialisationCallback");
        addObjectInitialisationCallback.param(ref(InitialisationCallback.class), "callback");
        addObjectInitialisationCallback.body()._throw(ExpressionFactory._new(ref(UnsupportedOperationException.class)));
    }

    private void generateGetObjectClassMethod(DefinedClass lifecycleAdapterFactory, DefinedClass poolObjectClass) {
        Method getObjectClass = lifecycleAdapterFactory.method(Modifier.PUBLIC, ref(Class.class), "getObjectClass");
        getObjectClass.body()._return(poolObjectClass.dotclass());
    }

    private void generateGetInstanceMethod(Type type, DefinedClass lifecycleAdapterFactory, DefinedClass poolObjectClass) {
        Method getInstance = lifecycleAdapterFactory.method(Modifier.PUBLIC, ref(Object.class), "getInstance");
        getInstance.param(ref(MuleContext.class), "muleContext");

        Variable object = getInstance.body().decl(poolObjectClass, "object", ExpressionFactory._new(poolObjectClass));
        for (Field variable : type.getFieldsAnnotatedWith(Configurable.class)) {
            getInstance.body().add(object.invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(ExpressionFactory._this().ref(variable.getSimpleName().toString())));
        }

        getInstance.body()._return(object);
    }

    private void generateDisposeMethod(DefinedClass lifecycleAdapterFactory) {
        lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "dispose");
    }

    private void generateInitialiseMethod(DefinedClass lifecycleAdapterFactory) {
        Method initialise = lifecycleAdapterFactory.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "initialise");
        initialise._throws(ref(InitialisationException.class));
    }

    private void generateFields(Type type, DefinedClass lifecycleAdapterFactory) {
        for (Field variable : type.getFieldsAnnotatedWith(Configurable.class)) {
            FieldVariable configField = lifecycleAdapterFactory.field(Modifier.PRIVATE, ref(variable.asType()), variable.getSimpleName().toString());
            generateSetter(lifecycleAdapterFactory, configField);
        }
    }

    private DefinedClass getLifecycleAdapterFactoryClass(Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.ADAPTERS_NAMESPACE);
        DefinedClass clazz = pkg._class(type.getClassName() + NamingConstants.LIFECYCLE_ADAPTER_FACTORY_CLASS_NAME_SUFFIX);
        clazz._implements(ref(ObjectFactory.class));

        clazz.role(DefinedClassRoles.POJO_FACTORY, ref(type));

        return clazz;
    }
}