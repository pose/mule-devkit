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

import org.mule.api.annotations.Connector;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.devkit.Context;
import org.mule.devkit.ValidationException;
import org.mule.devkit.Validator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

import java.lang.annotation.Annotation;

public class OAuthValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, Context context) {
        return type.isModuleOrConnector();
    }

    @Override
    public void validate(Type type, Context context) throws ValidationException {
        if (type.hasAnnotation(OAuth.class)) {
            validateOAuth1Class(type);
        } else if (type.hasAnnotation(OAuth2.class)) {
            validateOAuth2Class(type);
        } else {
            validateNonOAuthClass(type);
        }
    }

    private void validateOAuth1Class(Type type) throws ValidationException {
        if (type.hasAnnotation(Connector.class)) {
            throw new ValidationException(type, "It is not possible to use OAuth support in @Connector annotated classes, use @Module instead");
        }
        if (!type.hasFieldAnnotatedWith(OAuthConsumerKey.class)) {
            throw new ValidationException(type, "@OAuth class must contain a field annotated with @OAuthConsumerKey");
        }
        if (!type.hasFieldAnnotatedWith(OAuthConsumerSecret.class)) {
            throw new ValidationException(type, "@OAuth class must contain a field annotated with @OAuthConsumerSecret");
        }
        if (!classHasMethodWithParameterAnnotated(type, OAuthAccessToken.class)) {
            throw new ValidationException(type, "@OAuth class must have at least one method parameter annotated with @OAuthAccessToken");
        }
    }

    private void validateOAuth2Class(Type type) throws ValidationException {
        if (type.hasAnnotation(Connector.class)) {
            throw new ValidationException(type, "It is not possible to use OAuth support in @Connector annotated classes, use @Module instead");
        }
        if (!type.hasFieldAnnotatedWith(OAuthConsumerKey.class)) {
            throw new ValidationException(type, "@OAuth2 class must contain a field annotated with @OAuthConsumerKey");
        }
        if (!type.hasFieldAnnotatedWith(OAuthConsumerSecret.class)) {
            throw new ValidationException(type, "@OAuth2 class must contain a field annotated with @OAuthConsumerSecret");
        }
        if (!classHasMethodWithParameterAnnotated(type, OAuthAccessToken.class)) {
            throw new ValidationException(type, "@OAuth2 class must have at least one method parameter annotated with @OAuthAccessToken");
        }
        if (classHasMethodWithParameterAnnotated(type, OAuthAccessTokenSecret.class)) {
            throw new ValidationException(type, "@OAuth2 class cannot have method parameters annotated with @OAuthAccessTokenSecret");
        }
    }

    private void validateNonOAuthClass(Type type) throws ValidationException {
        if (classHasMethodWithParameterAnnotated(type, OAuthAccessToken.class)) {
            throw new ValidationException(type, "Cannot annotate parameter with @OAuthAccessToken without annotating the class with @OAuth or @OAuth2");
        }
        if (classHasMethodWithParameterAnnotated(type, OAuthAccessTokenSecret.class)) {
            throw new ValidationException(type, "Cannot annotate parameter with @OAuthAccessTokenSecret without annotating the class with @OAuth");
        }
    }

    private boolean classHasMethodWithParameterAnnotated(Type type, Class<? extends Annotation> annotation) {
        for (Method method : type.getMethods()) {
            for (Parameter parameter : method.getParameters()) {
                if (parameter.getAnnotation(annotation) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}