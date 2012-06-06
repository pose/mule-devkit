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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.rest.RestCall;
import org.mule.api.annotations.rest.RestExceptionOn;
import org.mule.api.annotations.rest.RestHeaderParam;
import org.mule.api.annotations.rest.RestHttpClient;
import org.mule.api.annotations.rest.RestQueryParam;
import org.mule.api.annotations.rest.RestUriParam;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

public class RestValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, GeneratorContext context) {
        return type.isModuleOrConnector() && type.hasMethodsAnnotatedWith(RestCall.class);
    }

    @Override
    public void validate(Type type, GeneratorContext context) throws ValidationException {

        for (Method method : type.getMethodsAnnotatedWith(RestCall.class)) {

            if (!method.isAbstract()) {
                throw new ValidationException(method, "@RestCall can only be applied to abstract methods");
            }

            if (method.getThrownTypes().size() != 1) {
                throw new ValidationException(method, "@RestCall abstract method must throw IOException");
            }

            RestExceptionOn restExceptionOn = method.getAnnotation(RestExceptionOn.class);
            if (restExceptionOn != null) {
                if (restExceptionOn.statusCodeIs().length != 0 &&
                        restExceptionOn.statusCodeIsNot().length != 0) {
                    throw new ValidationException(method, "@RestExceptionOn can only be used with statusCodeIs or statusCodeIsNot. Not both.");
                }
                if (restExceptionOn.statusCodeIs().length == 0 &&
                        restExceptionOn.statusCodeIsNot().length == 0) {
                    throw new ValidationException(method, "@RestExceptionOn must have either statusCodeIs or statusCodeIsNot.");
                }
            }

            int nonAnnotatedParameterCount = 0;
            for (Parameter parameter : method.getParameters()) {
                if (parameter.getAnnotation(RestUriParam.class) == null &&
                        parameter.getAnnotation(RestHeaderParam.class) == null &&
                        parameter.getAnnotation(RestQueryParam.class) == null) {
                    nonAnnotatedParameterCount++;
                }
            }

            if (nonAnnotatedParameterCount > 1) {
                throw new ValidationException(method, "Only one parameter can be used as payload, everything else must be annotated with @RestUriParam, @RestQueryParam or @RestHeaderParam.");
            }
        }

        for (Field field : type.getFieldsAnnotatedWith(RestUriParam.class)) {
            boolean getterFound = false;
            for (Method method : type.getMethods()) {
                if (method.getSimpleName().toString().equals("get" + StringUtils.capitalize(field.getSimpleName().toString()))) {
                    getterFound = true;
                    break;
                }
            }
            if (!getterFound) {
                throw new ValidationException(field, "Cannot find a getter method for " + field.getSimpleName().toString() + " but its being marked as URI parameter of a REST call.");
            }
        }

        if (type.getFieldsAnnotatedWith(RestHttpClient.class).size() > 1) {
            throw new ValidationException(type, "There can only be one field annotated with @RestHttpClient.");
        }

        if( type.getFieldsAnnotatedWith(RestHttpClient.class).size() > 0 ) {
            if (!type.getFieldsAnnotatedWith(RestHttpClient.class).get(0).asType().toString().equals(HttpClient.class.getName())) {
                throw new ValidationException(type.getFieldsAnnotatedWith(RestHttpClient.class).get(0), "A field annotated with @RestHttpClient must be of type " + HttpClient.class.getName());
            }
        }

        for (Field field : type.getFieldsAnnotatedWith(RestHttpClient.class)) {
            boolean getterFound = false;
            for (Method method : type.getMethods()) {
                if (method.getSimpleName().toString().equals("get" + StringUtils.capitalize(field.getSimpleName().toString()))) {
                    getterFound = true;
                    break;
                }
            }
            if (!getterFound) {
                throw new ValidationException(field, "Cannot find a getter method for " + field.getSimpleName().toString() + " but its being marked with @RestHttpClient.");
            }
        }


    }
}