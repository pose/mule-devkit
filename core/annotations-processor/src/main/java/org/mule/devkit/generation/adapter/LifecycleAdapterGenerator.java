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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.MuleManifest;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.ForLoop;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class LifecycleAdapterGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class) || type.hasAnnotation(ExpressionLanguage.class);
    }

    @Override
    public void generate(Type type) {
        DefinedClass lifecycleAdapter = getLifecycleAdapterClass(type);
        lifecycleAdapter.javadoc().add("A <code>" + lifecycleAdapter.name() + "</code> is a wrapper around ");
        lifecycleAdapter.javadoc().add(ref(type.asType()));
        lifecycleAdapter.javadoc().add(" that adds lifecycle methods to the pojo.");

        Method startElement = getStartElement(type);
        lifecycleAdapter._implements(Startable.class);
        org.mule.devkit.model.code.Method start = generateLifecycleInvocation(lifecycleAdapter, type, startElement, "start", DefaultMuleException.class, false);
        start._throws(ref(MuleException.class));

        Method stopElement = getStopElement(type);
        lifecycleAdapter._implements(Stoppable.class);
        org.mule.devkit.model.code.Method stop = generateLifecycleInvocation(lifecycleAdapter, type, stopElement, "stop", DefaultMuleException.class, false);
        stop._throws(ref(MuleException.class));

        Method postConstructElement = getPostConstructElement(type);
        lifecycleAdapter._implements(Initialisable.class);
        generateLifecycleInvocation(lifecycleAdapter, type, postConstructElement, "initialise", InitialisationException.class, true);

        Method preDestroyElement = getPreDestroyElement(type);
        lifecycleAdapter._implements(Disposable.class);
        generateLifecycleInvocation(lifecycleAdapter, type, preDestroyElement, "dispose", null, false);
    }

    private DefinedClass getLifecycleAdapterClass(Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.ADAPTERS_NAMESPACE);

        TypeReference previous = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));
        if (previous == null) {
            previous = (TypeReference) ref(type.asType());
        }

        int modifiers = Modifier.PUBLIC;
        if( type.isAbstract() ) {
            modifiers |= Modifier.ABSTRACT;
        }

        DefinedClass clazz = pkg._class(modifiers, type.getClassName() + NamingConstants.LIFECYCLE_ADAPTER_CLASS_NAME_SUFFIX, previous);

        clazz.role(DefinedClassRoles.MODULE_OBJECT, ref(type));

        return clazz;
    }

    private org.mule.devkit.model.code.Method generateLifecycleInvocation(DefinedClass lifecycleWrapper, Type type, Method superExecutableElement, String name, Class<?> catchException, boolean addThis) {
        org.mule.devkit.model.code.Method lifecycleMethod = lifecycleWrapper.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, name);

        if (name.equals("initialise")) {
            Variable log = lifecycleMethod.body().decl(ref(Logger.class), "log", ref(LoggerFactory.class).staticInvoke("getLogger").arg(lifecycleWrapper.dotclass()));
            Variable runtimeVersion = lifecycleMethod.body().decl(ref(String.class), "runtimeVersion", ref(MuleManifest.class).staticInvoke("getProductVersion"));
            Conditional ifUnkownVersion = lifecycleMethod.body()._if(runtimeVersion.invoke("equals").arg("Unknown"));
            ifUnkownVersion._then().add(log.invoke("warn").arg(ExpressionFactory.lit("Unknown Mule runtime version. This module may not work properly!")));
            Block ifKnownVersion = ifUnkownVersion._else();

            Variable expectedMinVersion = ifKnownVersion.decl(ref(String[].class), "expectedMinVersion", ExpressionFactory.lit(type.minMuleVersion()).invoke("split").arg("\\."));

            Block ifKnownVersionContainsDash = ifKnownVersion._if(ExpressionFactory.invoke(runtimeVersion, "contains").arg("-"))._then();
            ifKnownVersionContainsDash.assign(runtimeVersion, runtimeVersion.invoke("split").arg("-").component(ExpressionFactory.lit(0)));

            Variable currentRuntimeVersion = ifKnownVersion.decl(ref(String[].class), "currentRuntimeVersion", runtimeVersion.invoke("split").arg("\\."));

            ForLoop forEachVersionComponent = ifKnownVersion._for();
            Variable i = forEachVersionComponent.init(ctx().getCodeModel().INT, "i", ExpressionFactory.lit(0));
            forEachVersionComponent.test(Op.lt(i, expectedMinVersion.ref("length")));
            forEachVersionComponent.update(Op.incr(i));

            TryStatement tryToParseMuleVersion = forEachVersionComponent.body()._try();
            tryToParseMuleVersion.body()._if(Op.lt(
                    ref(Integer.class).staticInvoke("parseInt").arg(currentRuntimeVersion.component(i)),
                    ref(Integer.class).staticInvoke("parseInt").arg(expectedMinVersion.component(i))))._then()
                    ._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("This module is only valid for Mule " + type.minMuleVersion()));
            CatchBlock catchBlock = tryToParseMuleVersion._catch(ref(NumberFormatException.class));
            catchBlock.param("nfe");
            catchBlock.body().invoke(log, "warn").arg("Error parsing Mule version, cannot validate current Mule version");
        }

        if (catchException != null &&
                superExecutableElement != null &&
                superExecutableElement.getThrownTypes() != null &&
                superExecutableElement.getThrownTypes().size() > 0) {
            lifecycleMethod._throws(ref(catchException));
        }

        if (superExecutableElement != null) {

            Invocation startInvocation = ExpressionFactory._super().invoke(superExecutableElement.getSimpleName().toString());

            if (superExecutableElement.getThrownTypes().size() > 0) {
                TryStatement tryBlock = lifecycleMethod.body()._try();
                tryBlock.body().add(startInvocation);

                int i = 0;
                for (TypeMirror exception : superExecutableElement.getThrownTypes()) {
                    CatchBlock catchBlock = tryBlock._catch(ref(exception).boxify());
                    Variable catchedException = catchBlock.param("e" + i);

                    Invocation newMuleException = ExpressionFactory._new(ref(catchException));
                    newMuleException.arg(catchedException);

                    if (addThis) {
                        newMuleException.arg(ExpressionFactory._this());
                    }

                    catchBlock.body().add(newMuleException);
                    i++;
                }
            } else {
                lifecycleMethod.body().add(startInvocation);
            }
        }
        return lifecycleMethod;
    }

    private Method getStartElement(Type type) {
        List<Method> startMethods = type.getMethodsAnnotatedWith(Start.class);
        return !startMethods.isEmpty() ? startMethods.get(0) : null;
    }

    private Method getStopElement(Type type) {
        List<Method> stopMethods = type.getMethodsAnnotatedWith(Stop.class);
        return !stopMethods.isEmpty() ? stopMethods.get(0) : null;
    }

    private Method getPostConstructElement(Type type) {
        List<Method> postConstructMethods = type.getMethodsAnnotatedWith(PostConstruct.class);
        return !postConstructMethods.isEmpty() ? postConstructMethods.get(0) : null;
    }

    private Method getPreDestroyElement(Type type) {
        List<Method> preDestroyMethods = type.getMethodsAnnotatedWith(PreDestroy.class);
        return !preDestroyMethods.isEmpty() ? preDestroyMethods.get(0) : null;
    }
}