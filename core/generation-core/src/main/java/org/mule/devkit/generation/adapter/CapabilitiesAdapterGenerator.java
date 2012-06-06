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

import org.mule.api.Capabilities;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TypeReference;

public class CapabilitiesAdapterGenerator extends AbstractModuleGenerator {

    @Override
    public boolean shouldGenerate(Type type) {
        return type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class);
    }

    @Override
    public void generate(Type type) {
        DefinedClass capabilitiesAdapter = getCapabilitiesAdapterClass(type);
        capabilitiesAdapter.javadoc().add("A <code>" + capabilitiesAdapter.name() + "</code> is a wrapper around ");
        capabilitiesAdapter.javadoc().add(ref(type.asType()));
        capabilitiesAdapter.javadoc().add(" that implements {@link org.mule.api.Capabilities} interface.");

        generateIsCapableOf(type, capabilitiesAdapter);

    }

    private DefinedClass getCapabilitiesAdapterClass(Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName() + NamingConstants.ADAPTERS_NAMESPACE);

        TypeReference previous = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(type));

        if( previous == null ) {
            previous = (TypeReference) ref(type.asType());
        }
        
        int modifiers = Modifier.PUBLIC;
        if( type.isAbstract() ) {
            modifiers |= Modifier.ABSTRACT;
        }

        DefinedClass clazz = pkg._class(modifiers, type.getClassName() + NamingConstants.CAPABILITIES_ADAPTER_CLASS_NAME_SUFFIX, previous);
        clazz._implements(Capabilities.class);

        clazz.role(DefinedClassRoles.MODULE_OBJECT, ref(type));

        return clazz;
    }
}