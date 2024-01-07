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
package de.esoco.gwt.client.ui;

import de.esoco.data.element.DataElementList;

/**
 * An interface that must be implemented to handle login callbacks.
 *
 * @author eso
 */
public interface LoginHandler {

	/**
	 * Will be invoked if the login failed.
	 *
	 * @param rError The exception that signaled the failure
	 */
	public void loginFailed(Exception rError);

	/**
	 * Will be invoked if the login was successful.
	 *
	 * @param rUserData A list of data elements containing information related
	 *                  to the login user
	 */
	public void loginSuccessful(DataElementList rUserData);
}
