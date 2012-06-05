package org.mule.devkit.it;

public class Parameter {
    /**
     * Name of the parameter
     */
    private String name;

    /**
     * Description of the parameter
     */
    private String description;

    /**
     * Data type of the parameter
     */
    private Class<?> dataType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public void setDataType(Class<?> dataType) {
        this.dataType = dataType;
    }
}
