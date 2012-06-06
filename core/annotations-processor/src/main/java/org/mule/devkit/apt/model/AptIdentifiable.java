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
package org.mule.devkit.apt.model;

import com.sun.source.util.Trees;
import org.apache.commons.lang.StringUtils;
import org.mule.api.NestedProcessor;
import org.mule.api.callback.HttpCallback;
import org.mule.devkit.model.Identifiable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class AptIdentifiable<T extends Element, P extends Identifiable> implements Identifiable<T, P> {
    protected T innerElement;
    protected P parent;
    protected Types types;
    protected Elements elements;
    protected Trees trees;

    public AptIdentifiable(T element, P parent, Types types, Elements elements, Trees trees) {
        this.innerElement = element;
        this.parent = parent;
        this.types = types;
        this.elements = elements;
        this.trees = trees;
    }

    public P parent() {
        return this.parent;
    }

    public T unwrap() {
        return innerElement;
    }

    @Override
    public TypeMirror asType() {
        return innerElement.asType();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return innerElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return (A) innerElement.getAnnotation(aClass);
    }

    @Override
    public Name getSimpleName() {
        return innerElement.getSimpleName();
    }


    @Override
    public boolean isXmlType() {
        if (asType().getKind() == TypeKind.DECLARED) {

            DeclaredType declaredType = (DeclaredType) asType();
            XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

            if (xmlType != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPublic() {
        return innerElement.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isPrivate() {
        return innerElement.getModifiers().contains(Modifier.PRIVATE);
    }

    @Override
    public boolean isProtected() {
        return innerElement.getModifiers().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isAbstract() {
        return innerElement.getModifiers().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isFinal() {
        return innerElement.getModifiers().contains(Modifier.FINAL);
    }

    @Override
    public boolean isStatic() {
        return innerElement.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public boolean isCollection() {
        return isArrayOrList(asType()) || isMap(asType());
    }

    @Override
    public boolean isNestedProcessor() {
        if (asType().toString().startsWith(NestedProcessor.class.getName())) {
            return true;
        }

        if (asType().toString().startsWith(List.class.getName())) {
            DeclaredType variableType = (DeclaredType) asType();
            List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();
            if (variableTypeParameters.isEmpty()) {
                return false;
            }

            if (variableTypeParameters.get(0).toString().startsWith(NestedProcessor.class.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isArrayOrList() {
        return isArrayOrList(asType());
    }

    private boolean isArrayOrList(TypeMirror type) {
        if (type.toString().equals("byte[]")) {
            return false;
        }

        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }

        if (type.toString().startsWith(List.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isArrayOrList(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMap() {
        return isMap(asType());
    }

    private boolean isMap(TypeMirror type) {
        if (type.toString().startsWith(Map.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isMap(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEnum() {
        return isEnum(asType());
    }

    private boolean isEnum(TypeMirror type) {
        if (type.toString().startsWith(Enum.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isEnum(inherit)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Identifiable> getTypeArguments() {
        List<Identifiable> typeArguments = new ArrayList<Identifiable>();
        DeclaredType declaredType = (DeclaredType)asType();
        for( TypeMirror typeMirror : declaredType.getTypeArguments() ) {
            if( typeMirror instanceof WildcardType || typeMirror instanceof TypeVariable ) {
                continue;
            }
            Element element = types.asElement(typeMirror);
            if( element instanceof TypeElement ) {
                typeArguments.add(new AptType((TypeElement)element, types, elements, trees));
            } else {
                typeArguments.add(new AptIdentifiable(element, this, types, elements, trees));
            }
        }

        return typeArguments;
    }

    @Override
    public boolean hasTypeArguments() {
        return getTypeArguments().size() > 0;
    }

    @Override
    public boolean isString() {
        String className = innerElement.asType().toString();
        return className.startsWith(String.class.getName());
    }

    @Override
    public boolean isBoolean() {
        String className = innerElement.asType().toString();
        return className.startsWith(Boolean.class.getName()) || className.startsWith("boolean");
    }

    @Override
    public boolean isInteger() {
        String className = innerElement.asType().toString();
        return className.startsWith(Integer.class.getName()) || className.startsWith("int");
    }

    @Override
    public boolean isLong() {
        String className = innerElement.asType().toString();
        return className.startsWith(Long.class.getName()) || className.startsWith("long");
    }

    @Override
    public boolean isFloat() {
        String className = innerElement.asType().toString();
        return className.startsWith(Float.class.getName()) || className.startsWith("float");
    }

    @Override
    public boolean isDouble() {
        String className = innerElement.asType().toString();
        return className.startsWith(Double.class.getName()) || className.startsWith("double");
    }

    @Override
    public boolean isChar() {
        String className = innerElement.asType().toString();
        return className.startsWith(Character.class.getName()) || className.startsWith("char");
    }

    @Override
    public boolean isHttpCallback() {
        return innerElement.asType().toString().startsWith(HttpCallback.class.getName());
    }

    @Override
    public boolean isURL() {
        return innerElement.asType().toString().startsWith(URL.class.getName());
    }

    @Override
    public boolean isDate() {
        return innerElement.asType().toString().startsWith(Date.class.getName());
    }

    @Override
    public boolean isBigDecimal() {
        return innerElement.asType().toString().startsWith(BigDecimal.class.getName());
    }

    @Override
    public boolean isBigInteger() {
        return innerElement.asType().toString().startsWith(BigInteger.class.getName());
    }

    @Override
    public String getJavaDocSummary() {
        String comment = elements.getDocComment(innerElement);
        if (comment == null || StringUtils.isBlank(comment)) {
            return null;
        }

        comment = comment.trim();

        String parsedComment = "";
        boolean tagsBegan = false;
        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@")) {
                tagsBegan = true;
            }
            if (!tagsBegan) {
                parsedComment = parsedComment + nextToken + "\n";
            }
        }

        String strippedComments = "";
        boolean insideTag = false;
        for (int i = 0; i < parsedComment.length(); i++) {
            if (parsedComment.charAt(i) == '{' &&
                    parsedComment.charAt(i + 1) == '@') {
                insideTag = true;
            } else if (parsedComment.charAt(i) == '}') {
                insideTag = false;
            } else {
                if (!insideTag) {
                    strippedComments += parsedComment.charAt(i);
                }
            }
        }

        strippedComments = strippedComments.trim();
        while (strippedComments.length() > 0 &&
                strippedComments.charAt(strippedComments.length() - 1) == '\n') {
            strippedComments = StringUtils.chomp(strippedComments);
        }

        return strippedComments;
    }

    @Override
    public boolean hasJavaDocTag(String tagName) {
        String comment = elements.getDocComment(innerElement);
        if (StringUtils.isBlank(comment)) {
            return false;
        }

        StringTokenizer st = new StringTokenizer(comment.trim(), "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@" + tagName)) {
                String tagContent = StringUtils.difference("@" + tagName, nextToken);
                return !StringUtils.isBlank(tagContent);
            }
            if (nextToken.startsWith("{@" + tagName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getJavaDocTagContent(String tagName) {
        String comment = elements.getDocComment(innerElement);
        if (StringUtils.isBlank(comment)) {
            return "";
        }

        StringTokenizer st = new StringTokenizer(comment.trim(), "\n\r");
        boolean insideTag = false;
        StringBuilder tagContent = new StringBuilder();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@" + tagName)) {
                return StringUtils.difference("@" + tagName, nextToken).trim();
            }
            if (nextToken.startsWith("{@" + tagName)) {
                if (nextToken.endsWith("}")) {
                    return StringUtils.difference("{@" + tagName, nextToken).replaceAll("}", "").trim();
                } else {
                    tagContent.append(StringUtils.difference("{@" + tagName, nextToken).replaceAll("}", "").trim());
                    insideTag = true;
                }
            } else if (insideTag) {
                if (nextToken.endsWith("}")) {
                    tagContent.append(' ').append(nextToken.replaceAll("}", ""));
                    insideTag = false;
                } else {
                    tagContent.append(' ').append(nextToken);
                }
            }
        }

        return tagContent.toString();
    }

    @Override
    public String getJavaDocParameterSummary(String paramName) {
        String comment = elements.getDocComment(innerElement);
        if (StringUtils.isBlank(comment)) {
            return null;
        }

        comment = comment.trim();

        StringBuilder parameterCommentBuilder = new StringBuilder();
        boolean insideParameter = false;
        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@param " + paramName + " ") || nextToken.equals("@param " + paramName)) {
                insideParameter = true;
            } else if (nextToken.startsWith("@")) {
                insideParameter = false;
            }
            if (insideParameter) {
                parameterCommentBuilder.append(nextToken).append(" ");
            }
        }

        int startIndex = 7 + paramName.length() + 1;
        if (parameterCommentBuilder.length() < startIndex) {
            return null;
        }

        String parameterComment = parameterCommentBuilder.substring(startIndex);

        StringBuilder strippedCommentBuilder = new StringBuilder();
        boolean insideTag = false;
        for (int i = 0; i < parameterComment.length(); i++) {
            if (parameterComment.charAt(i) == '{' &&
                    parameterComment.charAt(i + 1) == '@') {
                insideTag = true;
            } else if (parameterComment.charAt(i) == '}') {
                insideTag = false;
            } else {
                if (!insideTag) {
                    strippedCommentBuilder.append(parameterComment.charAt(i));
                }
            }
        }

        String strippedComment = strippedCommentBuilder.toString().trim();
        while (strippedComment.length() > 0 && strippedComment.charAt(strippedComment.length() - 1) == '\n') {
            strippedComment = StringUtils.chomp(strippedComment);
        }

        return strippedComment;
    }
}
