/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.doclet;

import com.google.clearsilver.jsilver.data.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamTagInfo extends ParsedTagInfo {
    static final Pattern PATTERN = Pattern.compile("([^ \t\r\n]+)[ \t\r\n]+(.*)", Pattern.DOTALL);

    private boolean mIsTypeParameter;
    private String mParameterComment;
    private String mParameterName;
    private boolean mIsOptional;
    private String mAttributeName;
    private String mDefaultValue;
    private boolean mIsNestedProcessor;
    private boolean mIsCollection;

    ParamTagInfo(String name, String kind, String text, String attributeName, boolean isOptional, String defaultValue,
                 boolean isNestedProcessor, boolean isCollection, ContainerInfo base, SourcePositionInfo sp) {
        super(name, kind, text, base, sp);

        Matcher m = PATTERN.matcher(text);
        if (m.matches()) {
            mParameterName = m.group(1);
            mParameterComment = m.group(2);
            int len = mParameterName.length();
            mIsTypeParameter =
                    len > 2 && mParameterName.charAt(0) == '<' && mParameterName.charAt(len - 1) == '>';
        } else {
            mParameterName = text.trim();
            mParameterComment = "";
            mIsTypeParameter = false;
        }
        mIsOptional = isOptional;
        mAttributeName = attributeName;
        mDefaultValue = defaultValue;
        mIsNestedProcessor = isNestedProcessor;
        mIsCollection = isCollection;
        setCommentText(mParameterComment);
    }

    ParamTagInfo(String name, String kind, String text, ContainerInfo base, SourcePositionInfo sp) {
        super(name, kind, text, base, sp);

        Matcher m = PATTERN.matcher(text);
        if (m.matches()) {
            mParameterName = m.group(1);
            mParameterComment = m.group(2);
            int len = mParameterName.length();
            mIsTypeParameter =
                    len > 2 && mParameterName.charAt(0) == '<' && mParameterName.charAt(len - 1) == '>';
        } else {
            mParameterName = text.trim();
            mParameterComment = "";
            mIsTypeParameter = false;
        }
        setCommentText(mParameterComment);
    }

    ParamTagInfo(String name, String kind, String text, boolean isTypeParameter,
                 String parameterComment, String parameterName, ContainerInfo base, SourcePositionInfo sp) {
        super(name, kind, text, base, sp);
        mIsTypeParameter = isTypeParameter;
        mParameterComment = parameterComment;
        mParameterName = parameterName;
    }

    public boolean isTypeParameter() {
        return mIsTypeParameter;
    }

    public String parameterComment() {
        return mParameterComment;
    }

    public String parameterName() {
        return mParameterName;
    }

    public String attributeName() {
        return mAttributeName;
    }

    public String defaultValue() {
        return mDefaultValue;
    }

    public boolean optional() {
        return mIsOptional;
    }

    public boolean isNestedProcessor() {
        return mIsNestedProcessor;
    }

    public boolean isCollection() {
        return mIsCollection;
    }

    @Override
    public void makeHDF(Data data, String base) {
        data.setValue(base + ".name", parameterName());
        data.setValue(base + ".attributeName", attributeName());
        data.setValue(base + ".optional", Boolean.toString(optional()));
        data.setValue(base + ".defaultValue", defaultValue());
        data.setValue(base + ".isTypeParameter", isTypeParameter() ? "1" : "0");
        data.setValue(base + ".isNestedProcessor", isNestedProcessor() ? "1" : "0");
        data.setValue(base + ".isCollection", isCollection() ? "1" : "0");
        TagInfo.makeHDF(data, base + ".comment", commentTags());
    }

    public static void makeHDF(Data data, String base, ParamTagInfo[] tags) {
        for (int i = 0; i < tags.length; i++) {
            // don't output if the comment is ""
            //if (!"".equals(tags[i].parameterComment())) {
            tags[i].makeHDF(data, base + "." + i);
            //}
        }
    }
}
