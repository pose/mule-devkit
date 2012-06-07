package org.mule.devkit.generation.studio;

import org.mule.devkit.generation.api.Context;
import org.mule.devkit.generation.api.Generator;

public abstract class AbstractMuleStudioGenerator implements Generator {

    protected Context context;

    public Context ctx() {
        return context;
    }

    public void setCtx(Context generationContext) {
        this.context = generationContext;
    }
}