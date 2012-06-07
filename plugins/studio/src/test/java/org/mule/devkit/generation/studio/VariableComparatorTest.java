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
package org.mule.devkit.generation.studio;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.devkit.generation.api.Context;
import org.mule.devkit.generation.studio.editor.VariableComparator;
import org.mule.devkit.model.Variable;

import javax.lang.model.element.Name;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableComparatorTest {

    @Mock
    private Variable stringVariable;
    @Mock
    private Variable intVariable;
    @Mock
    private Variable enumVariable;
    @Mock
    private Variable mapVariable;
    @Mock
    private Variable booleanVariable;
    @Mock
    private Variable unknownTypeVariable;
    @Mock
    private Context context;

    @Before
    public void setUpTests() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(stringVariable.isString()).thenReturn(true);
        when(intVariable.isInteger()).thenReturn(true);
        when(enumVariable.isEnum()).thenReturn(true);
        when(mapVariable.isCollection()).thenReturn(true);
        when(booleanVariable.isBoolean()).thenReturn(true);
    }

    @Test
    public void compareByType() throws Exception {
        List<Variable> variables = new ArrayList<Variable>();
        variables.add(mapVariable);
        variables.add(enumVariable);
        variables.add(booleanVariable);
        variables.add(intVariable);
        variables.add(stringVariable);

        setName(mapVariable, "a");
        setName(enumVariable, "b");
        setName(booleanVariable, "c");
        setName(intVariable, "d");
        setName(stringVariable, "e");

        Collections.sort(variables, new VariableComparator());

        assertEquals(intVariable, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
        assertEquals(enumVariable, variables.get(2));
        assertEquals(booleanVariable, variables.get(3));
        assertEquals(mapVariable, variables.get(4));
    }

    @Test
    public void testCompareUnknownType() throws Exception {
        setName(unknownTypeVariable, "a");
        setName(booleanVariable, "b");

        List<Variable> variables = new ArrayList<Variable>();
        variables.add(unknownTypeVariable);
        variables.add(booleanVariable);

        Collections.sort(variables, new VariableComparator());

        assertEquals(unknownTypeVariable, variables.get(0));
        assertEquals(booleanVariable, variables.get(1));
    }

    private void setName(Variable Variable, String name) {
        Name simpleName = mock(Name.class);
        when(Variable.getSimpleName()).thenReturn(simpleName);
        when(simpleName.toString()).thenReturn(name);
    }

    @Test
    public void testCompareByNameAndType() throws Exception {
        Name a = mockName("a");
        Name b = mockName("b");

        when(stringVariable.getSimpleName()).thenReturn(b);
        when(intVariable.getSimpleName()).thenReturn(a);

        List<Variable> variables = new ArrayList<Variable>();
        variables.add(stringVariable);
        variables.add(intVariable);

        Collections.sort(variables, new VariableComparator());

        assertEquals(stringVariable, variables.get(0));
        assertEquals(intVariable, variables.get(1));
    }

    @Test
    public void testCompareByOrder() throws Exception {
        Placement mapVariablePlacement = mock(Placement.class);
        Placement intVariablePlacement = mock(Placement.class);

        when(mapVariable.getAnnotation(Placement.class)).thenReturn(mapVariablePlacement);
        when(intVariable.getAnnotation(Placement.class)).thenReturn(intVariablePlacement);

        when(mapVariablePlacement.order()).thenReturn(4);
        when(intVariablePlacement.order()).thenReturn(5);

        List<Variable> variables = new ArrayList<Variable>();
        variables.add(intVariable);
        variables.add(mapVariable);

        Collections.sort(variables, new VariableComparator());

        assertEquals(mapVariable, variables.get(0));
        assertEquals(intVariable, variables.get(1));
    }

    @Test
    public void testCompareBothWithFriendlyNames() throws Exception {
        Variable stringVariable2 = mock(Variable.class);
        when(stringVariable2.isString()).thenReturn(true);

        FriendlyName friendlyName1 = mock(FriendlyName.class);
        FriendlyName friendlyName2 = mock(FriendlyName.class);

        when(stringVariable.getAnnotation(FriendlyName.class)).thenReturn(friendlyName1);
        when(stringVariable2.getAnnotation(FriendlyName.class)).thenReturn(friendlyName2);

        when(friendlyName1.value()).thenReturn("b");
        when(friendlyName2.value()).thenReturn("a");

        List<Variable> variables = new ArrayList<Variable>();
        variables.add(stringVariable);
        variables.add(stringVariable2);

        Collections.sort(variables, new VariableComparator());

        assertEquals(stringVariable2, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
    }

    @Test
    public void testCompareFriendlyName() throws Exception {
        Variable stringVariable2 = mock(Variable.class);
        when(stringVariable2.isString()).thenReturn(true);

        FriendlyName friendlyName1 = mock(FriendlyName.class);
        when(friendlyName1.value()).thenReturn("b");

        Name a = mockName("a");

        when(stringVariable.getAnnotation(FriendlyName.class)).thenReturn(friendlyName1);
        when(stringVariable2.getSimpleName()).thenReturn(a);


        List<Variable> variables = new ArrayList<Variable>();
        variables.add(stringVariable);
        variables.add(stringVariable2);

        Collections.sort(variables, new VariableComparator());

        assertEquals(stringVariable2, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
    }

    private Name mockName(String a) {
        Name name = mock(Name.class);
        when(name.toString()).thenReturn(a);
        return name;
    }
}