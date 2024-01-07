//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.gwt.client.res;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

/**
 * The resource bundle for the GWT framework.
 *
 * @author eso
 */
public interface EsocoGwtResources extends ClientBundle {

	/**
	 * The singleton instance of this class.
	 */
	public static final EsocoGwtResources INSTANCE =
		GWT.create(EsocoGwtResources.class);

	/**
	 * CSS
	 *
	 * @return CSS
	 */
	@Source("esoco-gwt.css")
	EsocoGwtCss css();
}
