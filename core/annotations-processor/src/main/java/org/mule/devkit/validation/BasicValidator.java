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

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Type;

import javax.lang.model.type.TypeKind;

public class BasicValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, GeneratorContext context) {
        return type.isModuleOrConnector();
    }

    @Override
    public void validate(Type type, GeneratorContext context) throws ValidationException {

        if (type.isInterface()) {
            throw new ValidationException(type, "@Module/@Connector cannot be applied to an interface");
        }

        if (type.isParametrized()) {
            throw new ValidationException(type, "@Module/@Connector type cannot have type parameters");
        }

        if (!type.isPublic()) {
            throw new ValidationException(type, "@Module/@Connector must be public");
        }

        for (Field variable : type.getFieldsAnnotatedWith(Configurable.class)) {

            if (variable.isFinal()) {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with final modifier");
            }

            if (variable.isStatic()) {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with static modifier");
            }

            if (variable.asType().getKind() == TypeKind.ARRAY) {
                throw new ValidationException(variable, "@Configurable cannot be applied to arrays");
            }

            Optional optional = variable.getAnnotation(Optional.class);
            Default def = variable.getAnnotation(Default.class);
            if (variable.asType().getKind().isPrimitive() && optional != null && (def == null || def.value().length() == 0)) {
                throw new ValidationException(variable, "@Optional @Configurable fields can only be applied to non-primitive types with a @Default value");
            }

            if (def != null && optional == null) {
                throw new ValidationException(variable, "@Default @Configurable fields must also include @Optional, otherwise the @Default will never take place.");
            }
        }
    }
}