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

package org.mule.devkit.generation.mule.studio;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.api.GenerationException;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.osgi.framework.BundleContext;

public class MuleStudioPluginActivatorGenerator extends AbstractMessageGenerator {

    public static final String ACTIVATOR_PATH = "org/mule/tooling/ui/contribution/Activator.class";
    private static final String ACTIVATOR_PACKAGE = "org.mule.tooling.ui.contribution";
    private static final String ACTIVATOR_CLASS_NAME = "Activator";

    @Override
    public boolean shouldGenerate(Type type) {
        return !ctx().hasOption("skipStudioPluginPackage");
    }

    @Override
    public void generate(Type type) throws GenerationException {
        DefinedClass activatorClass = getActivatorClass();
        activatorClass.javadoc().add("The activator class controls the plug-in life cycle");
        new FieldBuilder(activatorClass).
                publicVisibility().
                staticField().
                finalField().
                type(String.class).
                name("PLUGIN_ID").
                initialValue("org.mule.tooling.ui.contribution." + type.getModuleName()).build();
        TypeReference activatorTypeRef = ctx().getCodeModel().directClass(ACTIVATOR_PACKAGE + "." + ACTIVATOR_CLASS_NAME);
        FieldVariable plugin = new FieldBuilder(activatorClass).staticField().type(activatorTypeRef).name("plugin").build();

        generateStartMethod(activatorClass, plugin);

        generateStopMethod(activatorClass, plugin);

        Method getDefault = activatorClass.method(Modifier.STATIC | Modifier.PUBLIC, activatorTypeRef, "getDefault");
        getDefault.body()._return(plugin);
    }

    private DefinedClass getActivatorClass() {
        Package pkg = ctx().getCodeModel()._package(ACTIVATOR_PACKAGE);
        DefinedClass clazz;
        clazz = pkg._class(ACTIVATOR_CLASS_NAME);
        clazz._extends(AbstractUIPlugin.class);
        return clazz;
    }

    private void generateStartMethod(DefinedClass activatorClass, FieldVariable plugin) {
        Method start = activatorClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "start");
        start._throws(Exception.class);
        Variable context = start.param(BundleContext.class, "context");
        start.body().invoke(ExpressionFactory._super(), "start").arg(context);
        start.body().assign(plugin, ExpressionFactory._this());
    }

    private void generateStopMethod(DefinedClass activatorClass, FieldVariable plugin) {
        Method stop = activatorClass.method(Modifier.PUBLIC, super.ctx().getCodeModel().VOID, "stop");
        stop._throws(Exception.class);
        Variable context = stop.param(BundleContext.class, "context");
        stop.body().assign(plugin, ExpressionFactory._null());
        stop.body().invoke(ExpressionFactory._super(), "stop").arg(context);
    }
}