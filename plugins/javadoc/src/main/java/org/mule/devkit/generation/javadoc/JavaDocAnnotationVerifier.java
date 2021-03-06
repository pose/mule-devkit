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

package org.mule.devkit.generation.javadoc;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.generation.api.AnnotationVerificationException;
import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.generation.api.Context;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Identifiable;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JavaDocAnnotationVerifier implements AnnotationVerifier {

    @Override
    public boolean shouldVerify(Type type) {
        return type.isModuleOrConnector();
    }

    @Override
    public void verify(Type type) throws AnnotationVerificationException {

        if (!hasComment(type)) {
            throw new AnnotationVerificationException(type, "Class " + type.getQualifiedName().toString() + " is not properly documented. A summary is missing.");
        }

        if (!type.hasJavaDocTag("author")) {
            throw new AnnotationVerificationException(type, "Class " + type.getQualifiedName().toString() + " needs to have an @author tag.");
        }

        for (Field variable : type.getFieldsAnnotatedWith(Configurable.class)) {
            if (!hasComment(variable)) {
                throw new AnnotationVerificationException(variable, "Field " + variable.getSimpleName().toString() + " is not properly documented. The description is missing.");
            }
        }

        for (Method method : type.getMethodsAnnotatedWith(Processor.class)) {
            validateMethod(type, method);
        }

        for (Method method : type.getMethodsAnnotatedWith(Source.class)) {
            validateMethod(type, method);
        }

        for (Method method : type.getMethodsAnnotatedWith(Transformer.class)) {
            validateMethod(type, method);
        }

        for (Method method : type.getMethodsAnnotatedWith(Connect.class)) {
            validateAllParameters(method);
        }

        for (Method method : type.getMethodsAnnotatedWith(Disconnect.class)) {
            validateAllParameters(method);
        }
    }

    private void validateAllParameters(Method method) throws AnnotationVerificationException {
        for (Parameter variable : method.getParameters()) {
            if (!hasParameterComment(variable.getSimpleName().toString(), variable.parent())) {
                throw new AnnotationVerificationException(variable, "Parameter " + variable.getSimpleName().toString() + " of method " + method.getSimpleName().toString() + " is not properly documented. A matching @param in the method documentation was not found. ");
            }
        }
    }

    private void validateMethod(Type type, Method method) throws AnnotationVerificationException {
        if (!hasComment(method)) {
            throw new AnnotationVerificationException(method, "Method " + method.getSimpleName().toString() + " is not properly documented. A description of what it can do is missing.");
        }

        if (!method.getReturnType().toString().equals("void") &&
                !method.getReturnType().toString().contains("StopSourceCallback")) {
            if (!method.hasJavaDocTag("return")) {
                throw new AnnotationVerificationException(type, "The return type of a non-void method must be documented. Method " + method.getSimpleName().toString() + " is at fault. Missing @return.");
            }
        }

        if (exampleDoesNotExist(method)) {
            throw new AnnotationVerificationException(type, "Method " + method.getSimpleName().toString() + " does not have the example pointed by the {@sample.xml} tag");
        }

        validateAllParameters(method);
    }

    private boolean hasComment(Identifiable element) {
        String comment = element.getJavaDocSummary();
        return StringUtils.isNotBlank(comment);

    }

    private boolean hasParameterComment(String paramName, Identifiable element) {
        String comment = element.getJavaDocParameterSummary(paramName);
        return StringUtils.isNotBlank(comment);
    }

    protected boolean exampleDoesNotExist(Method method) throws AnnotationVerificationException {

        if (!method.hasJavaDocTag("sample.xml")) {
            throw new AnnotationVerificationException(method, "Method " + method.getSimpleName().toString() + " does not contain an example using {@sample.xml} tag.");
        }

        boolean found = false;
        String sample = method.getJavaDocTagContent("sample.xml");
        String[] split = sample.split(" ");

        if (split.length != 2) {
            throw new AnnotationVerificationException(method, "Check @sample.xml javadoc tag because is not well formed for method: " + method.getSimpleName());
        }

        String pathToExamplesFile = split[0];
        String exampleName = split[1];

        String sourcePath = method.parent().getPathToSourceFile();
        int packageCount = StringUtils.countMatches(method.parent().getQualifiedName().toString(), ".") + 1;
        while (packageCount > 0) {
            sourcePath = sourcePath.substring(0, sourcePath.lastIndexOf("/"));
            packageCount--;
        }

        try {
            File docFile = new File(sourcePath, pathToExamplesFile);
            String examplesFileContent = IOUtils.toString(new FileInputStream(docFile));
            if (examplesFileContent.contains("BEGIN_INCLUDE(" + exampleName + ")")) {
                found = true;
            }
        } catch (IOException e) {
            // do nothing
        }

        return !found;
    }
}