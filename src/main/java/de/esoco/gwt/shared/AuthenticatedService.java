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
package de.esoco.gwt.shared;

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.element.StringListDataElement;

import de.esoco.lib.property.PropertyName;

/**
 * A standard interface for services that require a user to authenticate before
 * service methods can be executed.
 *
 * @author eso
 */
public interface AuthenticatedService extends CommandService {

	/**
	 * A property name for the session ID.
	 */
	public static final PropertyName<String> SESSION_ID =
		PropertyName.newStringName("SessionID");

	/**
	 * A string property containing informations about a user that tries to
	 * login.
	 */
	public static final PropertyName<String> LOGIN_USER_INFO =
		PropertyName.newStringName("LoginUserInfo");

	/**
	 * The name of the data element in user data that contains the user's full
	 * name.
	 */
	public static final String USER_NAME = "UserName";

	/**
	 * The name of the data element in user data that contains the user's login
	 * name.
	 */
	public static final String LOGIN_NAME = "LoginName";

	/**
	 * Name of the data element in the user data that contains the current role
	 * of the user. Type: {@link StringDataElement}.
	 */
	public static final String CURRENT_ROLE = "CurrentRole";

	/**
	 * Name of the data element in the user data that contains a list of the
	 * users roles. Type: {@link StringListDataElement}.
	 */
	public static final String USER_ROLES = "Roles";

	//- Commands
	// ---------------------------------------------------------------

	/**
	 * This command performs a login. The argument is a string data element
	 * with
	 * the login name as it's name and the password as the value. If the
	 * call is
	 * an attempt to re-login an expired session the data element may contain a
	 * property with the name {@link #SESSION_ID} that contains the previous
	 * session ID (typically stored by the client in a cookie) to re-use the
	 * data from the expired session. In that case the returned data will
	 * contain a new session ID which must again be stored by the client.
	 *
	 * <p>On successful authentication the result is a {@link DataElementList}
	 * containing user-specific data. The base implementation will add the user
	 * name string in an element with the name {@link #LOGIN_NAME}. Subclasses
	 * may add additional user data elements.</p>
	 */
	public static final Command<StringDataElement, DataElementList> LOGIN =
		Command.newInstance("LOGIN");

	/**
	 * This command returns the data of the user that is currently logged in as
	 * described for the {@link #LOGIN} command. If the user is not logged
	 * in an
	 * {@link AuthenticationException} will be thrown. No input value is
	 * required and it should be NULL.
	 */
	public static final Command<DataElement<?>, DataElementList> GET_USER_DATA =
		Command.newInstance("GET_USER_DATA");

	/**
	 * This command performs a log out of the current user. If no user is
	 * logged
	 * in it will have no effect. No input value is required and it should be
	 * NULL. This is a VOID call, i.e. the returned value is always NULL.
	 */
	public static final Command<DataElement<?>, DataElement<?>> LOGOUT =
		Command.newInstance("LOGOUT");

	/**
	 * A command to change the password of the currently authenticated user.
	 * The
	 * input data element must contain the old password as it's name and the
	 * new
	 * password as it's value.
	 */
	public static final Command<StringDataElement, DataElement<?>>
		CHANGE_PASSWORD = Command.newInstance("CHANGE_PASSWORD");
}
