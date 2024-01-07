//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import de.esoco.data.element.StringDataElement;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.component.TextField;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;

import de.esoco.gwt.client.ServiceRegistry;
import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.gwt.shared.AuthenticatedService;

import de.esoco.lib.property.TextFieldStyle;

import java.util.Date;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.datepicker.client.CalendarUtil;

import static de.esoco.lib.property.StyleProperties.TEXT_FIELD_STYLE;

/**
 * A panel manager implementation that displays and handles a login panel.
 *
 * @author eso
 */
public class LoginPanelManager extends PanelManager<Panel, PanelManager<?, ?>>
	implements EwtEventHandler {

	private static final String USER_NAME_COOKIE = "_USER";

	private static final String SESSION_ID_COOKIE = "_SID";

	private final LoginHandler loginHandler;

	private final boolean reauthenticate;

	private final String userCookie;

	private final String sessionCookie;

	private TextField userField;

	private TextField passwordField;

	private Button loginButton;

	private Label failureMessage;

	/**
	 * @see PanelManager#PanelManager(PanelManager, String)
	 */
	public LoginPanelManager(PanelManager<?, ?> parent,
		LoginHandler loginHandler, String cookiePrefix,
		boolean reauthenticate) {
		super(parent, EsocoGwtResources.INSTANCE.css().gfLoginPanel());

		this.loginHandler = loginHandler;
		this.reauthenticate = reauthenticate;

		userCookie = cookiePrefix + USER_NAME_COOKIE;
		sessionCookie = cookiePrefix + SESSION_ID_COOKIE;
	}

	/**
	 * Handles events in the login components.
	 *
	 * @param event The event that occurred
	 */
	@Override
	public void handleEvent(EwtEvent event) {
		if (event.getSource() == userField) {
			passwordField.requestFocus();
		} else // return in password field or login button
		{
			login();
		}
	}

	/**
	 * Sets the focus to the first input field.
	 */
	public void requestFocus() {
		if (userField != null && userField.getText().length() == 0) {
			userField.requestFocus();
		} else {
			passwordField.requestFocus();
		}
	}

	@Override
	protected void addComponents() {
		ContainerBuilder<Panel> builder =
			createLoginComponentsPanel(AlignedPosition.CENTER);

		String userName = Cookies.getCookie(userCookie);

		addLoginPanelHeader(builder, AlignedPosition.TOP);

		userField = addUserComponents(builder, userName, reauthenticate);
		passwordField = addPasswortComponents(builder);
		failureMessage = addFailureMessageComponents(builder);
		loginButton = addSubmitLoginComponents(builder);

		if (userField != null) {
			userField.addEventListener(EventType.ACTION, this);
		}

		passwordField.addEventListener(EventType.ACTION, this);
		loginButton.addEventListener(EventType.ACTION, this);
		failureMessage.setVisible(false);
	}

	/**
	 * Adds the components to displays a login failure message.
	 *
	 * @param builder The container build to create the components with
	 * @return The failure message label
	 */
	protected Label addFailureMessageComponents(ContainerBuilder<?> builder) {
		String error = EsocoGwtResources.INSTANCE.css().error();

		StyleData errorStyle =
			StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES, error);

		builder.addLabel(StyleData.DEFAULT, "", null);

		return builder.addLabel(errorStyle, "$lblLoginFailed", null);
	}

	/**
	 * Adds the header of the login panel.
	 *
	 * @param builder     The container builder to add the header with
	 * @param headerStyle The style for the panel header
	 */
	protected void addLoginPanelHeader(ContainerBuilder<?> builder,
		StyleData headerStyle) {
		builder.addLabel(headerStyle, null, "#$imLogin");
		builder.addLabel(headerStyle, "$lblLogin", null);
	}

	/**
	 * Adds the components for the password input.
	 *
	 * @param builder The builder to create the components with
	 * @return The password input field
	 */
	protected TextField addPasswortComponents(ContainerBuilder<?> builder) {
		builder.addLabel(
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_RIGHT),
			"$lblPassword", null);

		return builder.addTextField(
			StyleData.DEFAULT.set(TEXT_FIELD_STYLE, TextFieldStyle.PASSWORD),
			"");
	}

	/**
	 * Adds the components for the submission of the login data.
	 *
	 * @param builder The builder to create the components with
	 * @return The login button
	 */
	protected Button addSubmitLoginComponents(ContainerBuilder<?> builder) {
		StyleData buttonStyle =
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_CENTER);

		builder.addLabel(StyleData.DEFAULT, "", null);

		return builder.addButton(buttonStyle, "$btnLogin", null);
	}

	/**
	 * Adds the components for the user input.
	 *
	 * @param builder        The builder to create the components with
	 * @param userName       The user name preset
	 * @param reauthenticate TRUE for a re-authentication of the current user
	 * @return The user input field or NULL if no user input is needed (in the
	 * case of re-authentication)
	 */
	protected TextField addUserComponents(ContainerBuilder<?> builder,
		String userName, boolean reauthenticate) {
		TextField userInputField = null;

		builder.addLabel(
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_RIGHT),
			"$lblLoginName", null);

		if (reauthenticate) {
			builder.addLabel(StyleData.DEFAULT, userName, null);
		} else {
			userInputField = builder.addTextField(StyleData.DEFAULT, "");
			userInputField.setText(userName);
		}

		return userInputField;
	}

	@Override
	protected ContainerBuilder<Panel> createContainer(
		ContainerBuilder<?> builder, StyleData styleData) {
		return builder.addPanel(styleData);
	}

	/**
	 * Creates the panel for the login components.
	 *
	 * @param panelStyle The style to be used for the panel
	 * @return The container builder for the new panel
	 */
	protected ContainerBuilder<Panel> createLoginComponentsPanel(
		StyleData panelStyle) {
		return addPanel(panelStyle, new TableGridLayout(2, true, 3));
	}

	/**
	 * Creates a new data element containing the login data. The default
	 * implementation creates a string data element with the user name as it's
	 * name and the password as it's value. It also adds the user info created
	 * by {@link #createLoginUserInfo()} as a property with the property name
	 * {@link AuthenticatedService#LOGIN_USER_INFO} and an existing session ID
	 * (from the session cookie) with the property
	 * {@link AuthenticatedService#SESSION_ID}.
	 *
	 * @param userName The login user name
	 * @param password The login password
	 * @return The login data object
	 */
	protected StringDataElement createLoginData(String userName,
		String password) {
		String sessionId = Cookies.getCookie(sessionCookie);
		StringDataElement loginData = new StringDataElement(userName,
			password);

		loginData.setProperty(AuthenticatedService.LOGIN_USER_INFO,
			createLoginUserInfo());

		if (sessionId != null) {
			loginData.setProperty(AuthenticatedService.SESSION_ID, sessionId);
		}

		return loginData;
	}

	/**
	 * Creates an information string for the user that is currently logging in.
	 *
	 * @return The user info string
	 */
	protected String createLoginUserInfo() {
		StringBuilder loginUserInfo = new StringBuilder();

		loginUserInfo.append("UserAgent: ");
		loginUserInfo.append(Window.Navigator.getUserAgent());
		loginUserInfo.append("\nApp: ");
		loginUserInfo.append(Window.Navigator.getAppName());
		loginUserInfo.append(" (");
		loginUserInfo.append(Window.Navigator.getAppCodeName());
		loginUserInfo.append(")\nVersion: ");
		loginUserInfo.append(Window.Navigator.getAppVersion());
		loginUserInfo.append("\nPlatform: ");
		loginUserInfo.append(Window.Navigator.getPlatform());

		return loginUserInfo.toString();
	}

	/**
	 * Handles login failures. There are two ways this method can be invoked.
	 * This class first tries to connect to the server with user name and
	 * password set to NULL to check for an existing authentication. If that
	 * call fails the internal container builder reference will not be NULL and
	 * will be used to create the login components to query for the user login.
	 * The builder reference will then be set to NULL.
	 *
	 * @param caught The exception that occurred
	 */
	protected void handleLoginFailure(Throwable caught) {
		loginButton.setEnabled(true);
		failureMessage.setVisible(true);
		loginHandler.loginFailed((Exception) caught);
	}

	/**
	 * Handles a successful authentication by invoking the login method
	 * {@link LoginHandler#loginSuccessful(DataElementList)}.
	 *
	 * @param userData The user data instance returned by the service
	 */
	protected void handleLoginSuccess(DataElementList userData) {
		String sessionID =
			userData.getProperty(AuthenticatedService.SESSION_ID, null);

		if (sessionID == null) {
			throw new IllegalArgumentException("No Session ID in user data");
		}

		if (passwordField != null) {
			passwordField.setText("");
			failureMessage.setVisible(false);

			userField = null;
			passwordField = null;
		}

		Cookies.setCookie(sessionCookie, sessionID);
		loginHandler.loginSuccessful(userData);
	}

	/**
	 * Performs the login with the data from the input fields.
	 */
	protected void login() {
		loginButton.setEnabled(false);

		String userName = reauthenticate ?
		                  Cookies.getCookie(userCookie) :
		                  userField.getText();

		String password = passwordField.getText();

		setUserNameCookie(userName);

		ServiceRegistry
			.getCommandService()
			.executeCommand(AuthenticatedService.LOGIN,
				createLoginData(userName, password),
				new AsyncCallback<DataElementList>() {
					@Override
					public void onFailure(Throwable caught) {
						handleLoginFailure(caught);
					}

					@Override
					public void onSuccess(DataElementList result) {
						handleLoginSuccess(result);
					}
				});
	}

	/**
	 * Sets a cookie with the user name for re-use on subsequent logins. The
	 * expiration period of this cookie is 3 months.
	 *
	 * @param userName The user name to set
	 */
	protected void setUserNameCookie(String userName) {
		Date expiryDate = new Date();

		CalendarUtil.addMonthsToDate(expiryDate, 3);
		Cookies.setCookie(userCookie, userName, expiryDate);
	}
}
