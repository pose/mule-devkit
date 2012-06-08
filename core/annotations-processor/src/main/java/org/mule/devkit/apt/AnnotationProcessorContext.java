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

package org.mule.devkit.apt;

import org.mule.devkit.generation.api.Context;
import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.code.CodeModel;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.writer.FilerCodeWriter;
import org.mule.devkit.model.schema.SchemaModel;
import org.mule.devkit.model.studio.StudioModel;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnotationProcessorContext implements Context {

    private Messager messager;
    private CodeModel codeModel;
    private SchemaModel schemaModel;
    private StudioModel studioModel;
    private List<DefinedClass> registerAtBoot;
    private Types types;
    private Elements elements;
    private Set<TypeMirror> registeredEnums;
    private Set<TypeMirror> registeredJaxbElements;

    public AnnotationProcessorContext(ProcessingEnvironment env) {
        registerAtBoot = new ArrayList<DefinedClass>();
        codeModel = new CodeModel(new FilerCodeWriter(env.getFiler()));
        schemaModel = new SchemaModel(new FilerCodeWriter(env.getFiler()));
        elements = env.getElementUtils();
        types = env.getTypeUtils();
        studioModel = new StudioModel(new FilerCodeWriter(env.getFiler()));
        registeredEnums = new HashSet<TypeMirror>();
        registeredJaxbElements = new HashSet<TypeMirror>();
        messager = env.getMessager();
    }

    @Override
    public CodeModel getCodeModel() {
        return codeModel;
    }

    @Override
    public List<DefinedClass> getRegisterAtBoot() {
        return registerAtBoot;
    }

    @Override
    public void registerAtBoot(DefinedClass clazz) {
        registerAtBoot.add(clazz);
    }

    @Override
    public SchemaModel getSchemaModel() {
        return schemaModel;
    }

    @Override
    public Types getTypeUtils() {
        return types;
    }

    @Override
    public StudioModel getStudioModel() {
        return studioModel;
    }

    @Override
    public void registerEnum(TypeMirror enumToRegister) {
        registeredEnums.add(enumToRegister);
    }

    @Override
    public boolean isEnumRegistered(TypeMirror enumToCheck) {
        return registeredEnums.contains(enumToCheck);
    }

    @Override
    public boolean isJaxbElementRegistered(TypeMirror jaxbElement) {
        return registeredJaxbElements.contains(jaxbElement);
    }

    @Override
    public void registerJaxbElement(TypeMirror jaxbElement) {
        registeredJaxbElements.add(jaxbElement);
    }

    @Override
    public void note(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public void warn(String msg) {
        messager.printMessage(Diagnostic.Kind.WARNING, msg);
    }

    @Override
    public void error(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public void error(String msg, Identifiable element) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, element.unwrap());
    }
}