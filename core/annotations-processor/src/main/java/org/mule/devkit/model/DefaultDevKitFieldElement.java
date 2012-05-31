package org.mule.devkit.model;

import javax.lang.model.element.VariableElement;

public class DefaultDevKitFieldElement extends DefaultDevKitVariableElement implements DevKitFieldElement {
    public DefaultDevKitFieldElement(VariableElement variableElement) {
        super(variableElement);
    }
}
