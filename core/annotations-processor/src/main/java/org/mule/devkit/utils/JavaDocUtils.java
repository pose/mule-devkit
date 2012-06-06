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
package org.mule.devkit.utils;

import org.apache.commons.lang.StringUtils;
import org.mule.devkit.model.DevKitElement;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.util.StringTokenizer;

public class JavaDocUtils {
    private Elements elements;

    public JavaDocUtils(Elements elements) {
        this.elements = elements;
    }




    public String getParameterSummary(String paramName, DevKitElement element) {
        String comment = elements.getDocComment(element.unwrap());
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
