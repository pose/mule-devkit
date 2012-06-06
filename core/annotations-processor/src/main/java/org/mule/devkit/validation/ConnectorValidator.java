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

import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.ValidateConnection;
import org.mule.devkit.Context;
import org.mule.devkit.ValidationException;
import org.mule.devkit.Validator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;

import java.util.List;

public class ConnectorValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, Context context) {
        return type.isModuleOrConnector();
    }

    @Override
    public void validate(Type type, Context context) throws ValidationException {

        List<Method> connectMethods = type.getMethodsAnnotatedWith(Connect.class);
        List<Method> validateConnectionMethods = type.getMethodsAnnotatedWith(ValidateConnection.class);
        List<Method> disconnectMethods = type.getMethodsAnnotatedWith(Disconnect.class);
        List<Method> connectionIdentifierMethods = type.getMethodsAnnotatedWith(ConnectionIdentifier.class);

        if (type.hasAnnotation(Module.class)) {
            if (!connectMethods.isEmpty()) {
                throw new ValidationException(type, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!validateConnectionMethods.isEmpty()) {
                throw new ValidationException(type, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!disconnectMethods.isEmpty()) {
                throw new ValidationException(type, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!connectionIdentifierMethods.isEmpty()) {
                throw new ValidationException(type, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            return;
        }

        checkConnectMethod(type, connectMethods);
        checkDisconnetcMethod(type, disconnectMethods);
        checkConnectionIdentifierMethod(type, connectionIdentifierMethods);
        checkValidateConnectionMethod(type, validateConnectionMethods);
    }

    private void checkConnectMethod(Type type, List<Method> connectMethods) throws ValidationException {
        if (connectMethods.size() != 1) {
            throw new ValidationException(type, "You must have exactly one method annotated with @Connect");
        }
        Method connectMethod = connectMethods.get(0);
        if (!connectMethod.isPublic()) {
            throw new ValidationException(type, "A @Connect method must be public.");
        }
        if (connectMethod.getThrownTypes().size() != 1) {
            throw new ValidationException(type, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
        }

        if (!connectMethod.getThrownTypes().get(0).toString().equals("org.mule.api.ConnectionException")) {
            throw new ValidationException(type, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
        }

        if (!connectMethod.getReturnType().toString().equals("void")) {
            throw new ValidationException(type, "A @Connect method cannot return anything.");
        }
    }

    private void checkDisconnetcMethod(Type type, List<Method> disconnectMethods) throws ValidationException {
        if (disconnectMethods.size() != 1) {
            throw new ValidationException(type, "You must have exactly one method annotated with @Disconnect");
        }
        Method disconnectMethod = disconnectMethods.get(0);
        if (!disconnectMethod.isPublic()) {
            throw new ValidationException(type, "A @Disconnect method must be public.");
        }
        if (!disconnectMethod.getParameters().isEmpty()) {
            throw new ValidationException(type, "The @Disconnect method cannot receive any arguments");
        }
        if (!disconnectMethod.getReturnType().toString().equals("void")) {
            throw new ValidationException(type, "A @Disconnect method cannot return anything.");
        }
    }

    private void checkValidateConnectionMethod(Type type, List<Method> validateConnectionMethods) throws ValidationException {
        if (validateConnectionMethods.size() != 1) {
            throw new ValidationException(type, "You must have exactly one method annotated with @ValidateConnection");
        }
        Method validateConnectionMethod = validateConnectionMethods.get(0);
        if (!validateConnectionMethod.isPublic()) {
            throw new ValidationException(type, "A @ValidateConnection method must be public.");
        }
        if (!validateConnectionMethod.getReturnType().toString().equals("boolean") &&
                !validateConnectionMethod.getReturnType().toString().equals("java.lang.Boolean")) {
            throw new ValidationException(type, "A @ValidateConnection method must return a boolean.");
        }
        if (!validateConnectionMethod.getParameters().isEmpty()) {
            throw new ValidationException(type, "The @ValidateConnection method cannot receive any arguments");
        }
    }

    private void checkConnectionIdentifierMethod(Type type, List<Method> connectionIdentifierMethods) throws ValidationException {
        if (connectionIdentifierMethods.size() != 1) {
            throw new ValidationException(type, "You must have exactly one method annotated with @ConnectionIdentifier");
        }
        Method connectionIdentifierMethod = connectionIdentifierMethods.get(0);
        if (!connectionIdentifierMethod.getReturnType().toString().equals("java.lang.String")) {
            throw new ValidationException(type, "A @ConnectionIdentifier must return java.lang.String.");
        }
        if (!connectionIdentifierMethod.isPublic()) {
            throw new ValidationException(type, "A @ConnectionIdentifier method must be public.");
        }
        if (connectionIdentifierMethod.isStatic()) {
            throw new ValidationException(type, "A @ConnectionIdentifier cannot be static.");
        }
        if (!connectionIdentifierMethod.getParameters().isEmpty()) {
            throw new ValidationException(type, "The @ConnectionIdentifier method cannot receive any arguments");
        }
    }
}