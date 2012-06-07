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
package org.mule.devkit.generation.studio.editor;

import org.mule.devkit.generation.api.Context;
import org.mule.devkit.generation.utils.NameUtils;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.util.StringUtils;

import java.util.List;

/**
 * Extension of {@link org.mule.devkit.generation.studio.editor.PatternTypeOperationsBuilder}
 * to handle modules implementing OAuth. The purpose of this extension is to generate metadata for the 
 * implicit authorize method
 * @author mariano.gonzalez@mulesoft.com
 *
 */
public class OAuthPatternTypeOperationsBuilder extends PatternTypeOperationsBuilder {

	public OAuthPatternTypeOperationsBuilder(Context context, Type type, PatternTypes patternTypeToUse) {
		super(context, type, patternTypeToUse);
	}
	
	@Override
    protected List<ModeElementType> getModes(List<Method> methods) {
    	List<ModeElementType> modes = super.getModes(methods);
    	
    	ModeElementType mode = new ModeElementType();
        String methodName = "authorize";
        mode.setModeId(MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + NameUtils.uncamel(methodName));
        mode.setModeLabel(StringUtils.capitalize(methodName));
        modes.add(0, mode);

        return modes;
    }
	
}
