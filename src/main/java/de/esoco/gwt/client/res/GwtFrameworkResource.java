//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import com.google.gwt.resources.client.ImageResource;


/********************************************************************
 * The resource bundle for the GWT framework.
 *
 * @author eso
 */
public interface GwtFrameworkResource extends ClientBundle
{
	//~ Static fields/initializers ---------------------------------------------

	/** The singleton instance of this class. */
	public static final GwtFrameworkResource INSTANCE =
		GWT.create(GwtFrameworkResource.class);

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * CSS
	 *
	 * @return CSS
	 */
	@Source("gwt-framework.css")
	GwtFrameworkCss css();

	/***************************************
	 * Image
	 *
	 * @return The image
	 */
	@Source("img/loading-animation-small.gif")
	ImageResource imLoadingSmall();

	/***************************************
	 * Image
	 *
	 * @return Image
	 */
	@Source("img/login.png")
	ImageResource imLogin();
}
