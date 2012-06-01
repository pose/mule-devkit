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

package org.mule.devkit.validation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitFieldElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.util.IOUtils;

import javax.lang.model.element.Element;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JavaDocValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector() && !context.isEnvOptionSet("skipJavaDocValidation");
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        if (!hasComment(typeElement.unwrap(), context)) {
            throw new ValidationException(typeElement, "Class " + typeElement.getQualifiedName().toString() + " is not properly documented. A summary is missing.");
        }

        if (!context.getJavaDocUtils().hasTag("author", typeElement.unwrap())) {
            throw new ValidationException(typeElement, "Class " + typeElement.getQualifiedName().toString() + " needs to have an @author tag.");
        }

        for (DevKitFieldElement variable : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            if (!hasComment(variable, context)) {
                throw new ValidationException(variable, "Field " + variable.getSimpleName().toString() + " is not properly documented. The description is missing.");
            }
        }

        for (DevKitExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            validateMethod(typeElement, context, method);
        }

        for (DevKitExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {
            validateMethod(typeElement, context, method);
        }

        for (DevKitExecutableElement method : typeElement.getMethodsAnnotatedWith(Transformer.class)) {
            validateMethod(typeElement, context, method);
        }

        for (DevKitExecutableElement method : typeElement.getMethodsAnnotatedWith(Connect.class)) {
            validateAllParameters(context, method);
        }

        for (DevKitExecutableElement method : typeElement.getMethodsAnnotatedWith(Disconnect.class)) {
            validateAllParameters(context, method);
        }
    }

    private void validateAllParameters(GeneratorContext context, DevKitExecutableElement method) throws ValidationException {
        for (DevKitParameterElement variable : method.getParameters()) {
            if (!hasParameterComment(variable.getSimpleName().toString(), variable.getEnclosingElement(), context)) {
                throw new ValidationException(variable, "Parameter " + variable.getSimpleName().toString() + " of method " + method.getSimpleName().toString() + " is not properly documented. A matching @param in the method documentation was not found. ");
            }
        }
    }

    private void validateMethod(DevKitTypeElement typeElement, GeneratorContext context, DevKitExecutableElement method) throws ValidationException {
        if (!hasComment(method, context)) {
            throw new ValidationException(method, "Method " + method.getSimpleName().toString() + " is not properly documented. A description of what it can do is missing.");
        }

        if (!method.getReturnType().toString().equals("void") &&
                !method.getReturnType().toString().contains("StopSourceCallback")) {
            if (!context.getJavaDocUtils().hasTag("return", method.unwrap())) {
                throw new ValidationException(typeElement, "The return type of a non-void method must be documented. Method " + method.getSimpleName().toString() + " is at fault. Missing @return.");
            }
        }

        if (exampleDoesNotExist(context, method)) {
            throw new ValidationException(typeElement, "Method " + method.getSimpleName().toString() + " does not have the example pointed by the {@sample.xml} tag");
        }

        validateAllParameters(context, method);
    }

    private boolean hasComment(Element element, GeneratorContext context) {
        String comment = context.getJavaDocUtils().getSummary(element);
        return StringUtils.isNotBlank(comment);

    }

    private boolean hasParameterComment(String paramName, Element element, GeneratorContext context) {
        String comment = context.getJavaDocUtils().getParameterSummary(paramName, element);
        return StringUtils.isNotBlank(comment);
    }

    protected boolean exampleDoesNotExist(GeneratorContext context, DevKitExecutableElement method) throws ValidationException {

        if (!context.getJavaDocUtils().hasTag("sample.xml", method.unwrap())) {
            throw new ValidationException(method, "Method " + method.getSimpleName().toString() + " does not contain an example using {@sample.xml} tag.");
        }

        boolean found = false;
        String sample = context.getJavaDocUtils().getTagContent("sample.xml", method);
        String[] split = sample.split(" ");

        if (split.length != 2) {
            throw new ValidationException(method, "Check @sample.xml javadoc tag because is not well formed for method: " + method.getSimpleName());
        }

        String pathToExamplesFile = split[0];
        String exampleName = split[1];

        String sourcePath = context.getSourceUtils().getPath(method.parent().unwrap());
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