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

import java.util.List;

import javax.lang.model.element.ExecutableElement;

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.util.StringUtils;

/**
 * Extension of {@link org.mule.devkit.generation.mule.studio.editor.PatternTypeOperationsBuilder}
 * to handle modules implementing OAuth. The purpose of this extension is to generate metadata for the 
 * implicit authorize method
 * @author mariano.gonzalez@mulesoft.com
 *
 */
public class OAuthPatternTypeOperationsBuilder extends PatternTypeOperationsBuilder {

	public OAuthPatternTypeOperationsBuilder(GeneratorContext context, DevKitTypeElement typeElement, PatternTypes patternTypeToUse) {
		super(context, typeElement, patternTypeToUse);
	}
	
	@Override
    protected List<ModeElementType> getModes(List<DevKitExecutableElement> methods) {
    	List<ModeElementType> modes = super.getModes(methods);
    	
    	ModeElementType mode = new ModeElementType();
        String methodName = "authorize";
        mode.setModeId(MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/' + nameUtils.uncamel(methodName));
        mode.setModeLabel(StringUtils.capitalize(methodName));
        modes.add(0, mode);

        return modes;
    }
	
}
