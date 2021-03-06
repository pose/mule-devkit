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
import org.mule.devkit.model.Type;
import org.mule.devkit.model.studio.PatternType;

/**
 * This pattern type builder generates the metadata for the authorize
 * method
 * @author mariano.gonzalez@mulesoft.com
 *
 */
public class OAuthPatternTypeBuilder extends PatternTypeBuilder {

	public OAuthPatternTypeBuilder(Context context, Type type) {
        super(context, null, type);
	}
	
	@Override
	public PatternType build() {
		return this.createPatternType();
	}
	
	@Override
	protected PatternType createPatternType() {
		PatternType patternType = new PatternType();
        patternType.setLocalId("authorize");
        patternType.setCaption("Authorize");
        patternType.setAbstract(true);
        patternType.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + type.getModuleName() + '/' + helper.getGlobalRefId(type.getModuleName()));
        patternType.setIcon(getIcon());
        patternType.setImage(getImage());
        return patternType;
	}
	
	@Override
    protected String getImage() {
        return helper.getConnectorImage(type);
    }

    @Override
	protected String getIcon() {
        return helper.getConnectorIcon(type);
    }
	
}
