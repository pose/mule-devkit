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
package org.mule.devkit.generation.api;

import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.code.CodeModel;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.schema.SchemaModel;
import org.mule.devkit.model.studio.StudioModel;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

public interface Context {
    CodeModel getCodeModel();

    List<DefinedClass> getRegisterAtBoot();

    void registerAtBoot(DefinedClass clazz);

    SchemaModel getSchemaModel();

    Types getTypeUtils();

    StudioModel getStudioModel();

    boolean hasOption(String option);

    void registerEnum(TypeMirror enumToRegister);

    boolean isEnumRegistered(TypeMirror enumToCheck);

    boolean isJaxbElementRegistered(TypeMirror jaxbElement);

    void registerJaxbElement(TypeMirror jaxbElement);

    boolean isEnvOptionSet(String envOption);

    void note(String msg);

    void warn(String msg);

    void error(String msg);

    void error(String msg, Identifiable element);
}
