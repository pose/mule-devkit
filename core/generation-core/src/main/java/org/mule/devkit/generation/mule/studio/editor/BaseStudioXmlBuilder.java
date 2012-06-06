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

package org.mule.devkit.generation.mule.studio.editor;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.display.Placement;
import org.mule.devkit.generation.api.Context;
import org.mule.devkit.model.Field;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.Variable;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.EnumElement;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.devkit.model.studio.ModeType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.util.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.Types;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseStudioXmlBuilder {

    public static final String GENERAL_GROUP_NAME = "General";
    public static final String CONNECTION_GROUP_NAME = "Connection";
    public static final String CONNECTION_GROUP_LABEL = "Use these fields to override the credentials defined in the %s connector.";
    protected ObjectFactory objectFactory;
    protected MuleStudioUtils helper;
    protected Type type;
    protected Method executableElement;
    protected Types typeUtils;
    protected String moduleName;
    protected Context context;


    protected BaseStudioXmlBuilder(Context context) {
        this.context = context;
        typeUtils = context.getTypeUtils();
        helper = new MuleStudioUtils();
        objectFactory = new ObjectFactory();
    }

    protected BaseStudioXmlBuilder(Context context, Type type) {
        this(context);
        this.type = type;
        moduleName = type.getModuleName();
    }

    protected Group createGroupWithModeSwitch(List<Method> methods) {
       
        ModeType modeSwitch = new ModeType();
        modeSwitch.getMode().addAll(this.getModes(methods));
        modeSwitch.setCaption(helper.formatCaption("Operation"));
        modeSwitch.setName(StringUtils.capitalize(moduleName) + " operations to execute");
        modeSwitch.setDescription(helper.formatDescription("Operation"));

        Group group = new Group();
        group.setId(type.getModuleName() + "ConnectorGeneric");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupModeSwitch(modeSwitch));
        group.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.GROUP_DEFAULT_CAPTION));
        return group;
    }
    
    protected List<ModeElementType> getModes(List<Method> methods) {
		 List<ModeElementType> modes = new ArrayList<ModeElementType>();
	     for (Method method : methods) {
	         ModeElementType mode = new ModeElementType();
	         mode.setModeId(MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + helper.getLocalId(method));
	         mode.setModeLabel(StringUtils.capitalize(helper.getFriendlyName(method)));
	         modes.add(mode);
	     }
	     
	     return modes;
    }

    protected BaseStudioXmlBuilder(Context context, Method executableElement, Type type) {
        this(context, type);
        this.executableElement = executableElement;
    }

    protected List<AttributeCategory> processMethodParameters() {
        return processVariableElements(getParametersSorted());
    }

    protected List<AttributeCategory> processConfigurableFields(Group defaultGroup) {
        List<AttributeCategory> attributeCategories = processVariableElements(getConfigurableFieldsSorted());
        for (AttributeCategory attributeCategory : attributeCategories) {
            if (attributeCategory.getCaption().equals(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION)) {
                attributeCategory.setDescription(helper.formatDescription(type.getModuleName() + " configuration properties"));
                List<Group> groups = attributeCategory.getGroup();
                if (groups.isEmpty()) {
                    groups.add(defaultGroup);
                } else {
                    groups.add(0, defaultGroup);
                }
            }
        }
        if (attributeCategories.isEmpty()) {
            AttributeCategory attributeCategory = new AttributeCategory();
            attributeCategory.setCaption(helper.getFormattedCaption(type));
            attributeCategory.setDescription(helper.formatDescription(type.getModuleName() + " configuration properties"));
            attributeCategory.getGroup().add(defaultGroup);
            attributeCategories.add(attributeCategory);
        }
        return attributeCategories;
    }

    private List<AttributeCategory> processVariableElements(List<? extends Variable> variableElements) {

        Map<String, Group> groupsByName = new LinkedHashMap<String, Group>();
        Map<String, AttributeCategory> attributeCategoriesByName = new LinkedHashMap<String, AttributeCategory>();
        getOrCreateDefaultAttributeCategory(attributeCategoriesByName);

        processConnectionAttributes(groupsByName, attributeCategoriesByName);

        for (Variable parameter : variableElements) {
            JAXBElement<? extends AttributeType> jaxbElement = createJaxbElement(parameter);
            AttributeCategory attributeCategory = getOrCreateAttributeCategory(attributeCategoriesByName, parameter.getAnnotation(Placement.class));
            Group group = getOrCreateGroup(groupsByName, parameter);
            group.getRegexpOrEncodingOrModeSwitch().add(jaxbElement);

            if (!attributeCategory.getGroup().contains(group)) {
                attributeCategory.getGroup().add(group);
            }
        }

        return new ArrayList<AttributeCategory>(attributeCategoriesByName.values());
    }

    protected void processConnectionAttributes(Map<String, Group> groupsByName, Map<String, AttributeCategory> attributeCategoriesByName) {
        // override if necessary
    }

    private AttributeCategory getOrCreateDefaultAttributeCategory(Map<String, AttributeCategory> attributeCategoriesByName) {
        return getOrCreateAttributeCategory(attributeCategoriesByName, null);

    }

    private AttributeCategory getOrCreateAttributeCategory(Map<String, AttributeCategory> attributeCategoriesByName, Placement placement) {
        if (placement == null || StringUtils.isBlank(placement.tab())) {
            if (!attributeCategoriesByName.containsKey(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION)) {
                AttributeCategory attributeCategoryGeneral = new AttributeCategory();
                attributeCategoryGeneral.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
                attributeCategoryGeneral.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));
                attributeCategoriesByName.put(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION, attributeCategoryGeneral);
            }
            return attributeCategoriesByName.get(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION);
        } else {
            String attributeCategoryName;
            if (StringUtils.isNotBlank(placement.tab())) {
                attributeCategoryName = placement.tab();
            } else {
                attributeCategoryName = MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION;
            }
            if (!attributeCategoriesByName.containsKey(attributeCategoryName)) {
                AttributeCategory attributeCategory = new AttributeCategory();
                attributeCategory.setCaption(helper.formatCaption(attributeCategoryName));
                attributeCategory.setDescription(helper.formatDescription(attributeCategoryName));
                attributeCategoriesByName.put(attributeCategoryName, attributeCategory);
            }
            return attributeCategoriesByName.get(attributeCategoryName);
        }
    }

    private Group getOrCreateGroup(Map<String, Group> groupsByName, Variable parameter) {
        Placement placement = parameter.getAnnotation(Placement.class);
        if (placement == null || StringUtils.isBlank(placement.group())) {
            if (!groupsByName.containsKey(GENERAL_GROUP_NAME)) {
                Group groupGeneral = new Group();
                groupGeneral.setCaption(helper.formatCaption(GENERAL_GROUP_NAME));
                groupGeneral.setId(StringUtils.uncapitalize(GENERAL_GROUP_NAME));
                groupsByName.put(GENERAL_GROUP_NAME, groupGeneral);
            }
            return groupsByName.get(GENERAL_GROUP_NAME);
        } else {
            String groupName = placement.group();
            if (!groupsByName.containsKey(groupName)) {
                Group group = new Group();
                group.setCaption(groupName);
                group.setId(StringUtils.uncapitalize(groupName));
                groupsByName.put(groupName, group);
            }
            return groupsByName.get(groupName);
        }
    }

    private JAXBElement<? extends AttributeType> createJaxbElement(Variable parameter) {
        JAXBElement<? extends AttributeType> jaxbElement;
        if (parameter.isEnum()) {
            EnumType enumType = createEnumType(parameter);
            jaxbElement = helper.createJAXBElement(enumType);
        } else if (parameter.isCollection()) {
            NestedElementReference childElement = createNestedElementReference(executableElement, parameter);
            jaxbElement = objectFactory.createGroupChildElement(childElement);
        } else {
            AttributeType attributeType = createAttributeType(parameter);
            jaxbElement = helper.createJAXBElement(attributeType);
        }
        return jaxbElement;
    }

    private List<Parameter> getParametersSorted() {
        List<Parameter> parameters = new ArrayList<Parameter>(executableElement.getParameters());
        Iterator<Parameter> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().shouldBeIgnored()) {
                iterator.remove();
            }
        }
        Collections.sort(parameters, new VariableComparator());
        return parameters;
    }

    private AttributeType createAttributeType(Variable parameter) {
        AttributeType attributeType = helper.createAttributeTypeIgnoreEnumsAndCollections(parameter);
        if (attributeType != null) {
            helper.setAttributeTypeInfo(parameter, attributeType);
        }
        return attributeType;
    }

    protected List<AttributeType> getConnectionAttributes(Type type) {
        List<AttributeType> parameters = new ArrayList<AttributeType>();
        Method connectMethod = type.getMethodsAnnotatedWith(Connect.class).get(0);
        for (Parameter connectAttributeType : connectMethod.getParameters()) {
            AttributeType parameter = helper.createAttributeTypeIgnoreEnumsAndCollections(connectAttributeType);
            helper.setAttributeTypeInfo(connectAttributeType, parameter);
            parameter.setRequired(false);
            parameters.add(parameter);
        }
        return parameters;
    }

    private NestedElementReference createNestedElementReference(Method executableElement, Variable parameter) {
        NestedElementReference childElement = new NestedElementReference();
        String prefix;
        if (executableElement != null) {
            prefix = NameUtils.uncamel(executableElement.getSimpleName().toString());
            childElement.setDescription(helper.formatDescription(executableElement.getJavaDocParameterSummary(parameter.getSimpleName().toString())));
        } else {
            prefix = "configurable";
            childElement.setDescription(helper.formatDescription(parameter.getJavaDocSummary()));
        }
        childElement.setName(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + prefix + '-' + NameUtils.uncamel(parameter.getSimpleName().toString()));
        childElement.setAllowMultiple(false);
        childElement.setCaption(helper.getFormattedCaption(parameter));
        childElement.setInplace(true);
        childElement.setJavaType(parameter.asType().toString());
        return childElement;
    }

    private EnumType createEnumType(Variable parameter) {
        EnumType enumType = new EnumType();
        enumType.setSupportsExpressions(true);
        enumType.setAllowsCustom(true);
        helper.setAttributeTypeInfo(parameter, enumType);
        for (Element enumMember : typeUtils.asElement(parameter.asType()).getEnclosedElements()) {
            if (enumMember.getKind() == ElementKind.ENUM_CONSTANT) {
                String enumConstant = enumMember.getSimpleName().toString();
                EnumElement enumElement = new EnumElement();
                enumElement.setValue(enumConstant);
                enumType.getOption().add(enumElement);
            }
        }
        Collections.sort(enumType.getOption(), new EnumElementComparator());
        return enumType;
    }

    private List<Field> getConfigurableFieldsSorted() {
        List<Field> configurableFields = type.getFieldsAnnotatedWith(Configurable.class);
        Collections.sort(configurableFields, new VariableComparator());
        return configurableFields;
    }
}