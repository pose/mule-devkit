package org.mule.devkit.model;

import javax.lang.model.element.VariableElement;

public class DefaultDevKitVariableElement extends VariableElementImpl implements DevKitVariableElement {
    public DefaultDevKitVariableElement(VariableElement variableElement) {
        super(variableElement);
    }
}
