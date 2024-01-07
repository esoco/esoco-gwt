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

	private final LoginHandler rLoginHandler;

	private final boolean bReauthenticate;

	private final String sUserCookie;

	private final String sSessionCookie;

	private TextField aUserField;

	private TextField aPasswordField;

	private Button aLoginButton;

	private Label aFailureMessage;

	/**
	 * @see PanelManager#PanelManager(PanelManager, String)
	 */
	public LoginPanelManager(PanelManager<?, ?> rParent,
		LoginHandler rLoginHandler, String sCookiePrefix,
		boolean bReauthenticate) {
		super(rParent, EsocoGwtResources.INSTANCE.css().gfLoginPanel());

		this.rLoginHandler = rLoginHandler;
		this.bReauthenticate = bReauthenticate;

		sUserCookie = sCookiePrefix + USER_NAME_COOKIE;
		sSessionCookie = sCookiePrefix + SESSION_ID_COOKIE;
	}

	/**
	 * Handles events in the login components.
	 *
	 * @param rEvent The event that occurred
	 */
	@Override
	public void handleEvent(EwtEvent rEvent) {
		if (rEvent.getSource() == aUserField) {
			aPasswordField.requestFocus();
		} else // return in password field or login button
		{
			login();
		}
	}

	/**
	 * Sets the focus to the first input field.
	 */
	public void requestFocus() {
		if (aUserField != null && aUserField.getText().length() == 0) {
			aUserField.requestFocus();
		} else {
			aPasswordField.requestFocus();
		}
	}

	@Override
	protected void addComponents() {
		ContainerBuilder<Panel> aBuilder =
			createLoginComponentsPanel(AlignedPosition.CENTER);

		String sUserName = Cookies.getCookie(sUserCookie);

		addLoginPanelHeader(aBuilder, AlignedPosition.TOP);

		aUserField = addUserComponents(aBuilder, sUserName, bReauthenticate);
		aPasswordField = addPasswortComponents(aBuilder);
		aFailureMessage = addFailureMessageComponents(aBuilder);
		aLoginButton = addSubmitLoginComponents(aBuilder);

		if (aUserField != null) {
			aUserField.addEventListener(EventType.ACTION, this);
		}

		aPasswordField.addEventListener(EventType.ACTION, this);
		aLoginButton.addEventListener(EventType.ACTION, this);
		aFailureMessage.setVisible(false);
	}

	/**
	 * Adds the components to displays a login failure message.
	 *
	 * @param rBuilder The container build to create the components with
	 * @return The failure message label
	 */
	protected Label addFailureMessageComponents(ContainerBuilder<?> rBuilder) {
		String sError = EsocoGwtResources.INSTANCE.css().error();

		StyleData rErrorStyle =
			StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES, sError);

		rBuilder.addLabel(StyleData.DEFAULT, "", null);

		return rBuilder.addLabel(rErrorStyle, "$lblLoginFailed", null);
	}

	/**
	 * Adds the header of the login panel.
	 *
	 * @param rBuilder     The container builder to add the header with
	 * @param rHeaderStyle The style for the panel header
	 */
	protected void addLoginPanelHeader(ContainerBuilder<?> rBuilder,
		StyleData rHeaderStyle) {
		rBuilder.addLabel(rHeaderStyle, null, "#$imLogin");
		rBuilder.addLabel(rHeaderStyle, "$lblLogin", null);
	}

	/**
	 * Adds the components for the password input.
	 *
	 * @param rBuilder The builder to create the components with
	 * @return The password input field
	 */
	protected TextField addPasswortComponents(ContainerBuilder<?> rBuilder) {
		rBuilder.addLabel(
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_RIGHT),
			"$lblPassword", null);

		return rBuilder.addTextField(
			StyleData.DEFAULT.set(TEXT_FIELD_STYLE, TextFieldStyle.PASSWORD),
			"");
	}

	/**
	 * Adds the components for the submission of the login data.
	 *
	 * @param rBuilder The builder to create the components with
	 * @return The login button
	 */
	protected Button addSubmitLoginComponents(ContainerBuilder<?> rBuilder) {
		StyleData rButtonStyle =
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_CENTER);

		rBuilder.addLabel(StyleData.DEFAULT, "", null);

		return rBuilder.addButton(rButtonStyle, "$btnLogin", null);
	}

	/**
	 * Adds the components for the user input.
	 *
	 * @param rBuilder        The builder to create the components with
	 * @param sUserName       The user name preset
	 * @param bReauthenticate TRUE for a re-authentication of the current user
	 * @return The user input field or NULL if no user input is needed (in the
	 * case of re-authentication)
	 */
	protected TextField addUserComponents(ContainerBuilder<?> rBuilder,
		String sUserName, boolean bReauthenticate) {
		TextField aUserInputField = null;

		rBuilder.addLabel(
			StyleData.DEFAULT.setFlags(StyleFlag.HORIZONTAL_ALIGN_RIGHT),
			"$lblLoginName", null);

		if (bReauthenticate) {
			rBuilder.addLabel(StyleData.DEFAULT, sUserName, null);
		} else {
			aUserInputField = rBuilder.addTextField(StyleData.DEFAULT, "");
			aUserInputField.setText(sUserName);
		}

		return aUserInputField;
	}

	@Override
	protected ContainerBuilder<Panel> createContainer(
		ContainerBuilder<?> rBuilder, StyleData rStyleData) {
		return rBuilder.addPanel(rStyleData);
	}

	/**
	 * Creates the panel for the login components.
	 *
	 * @param rPanelStyle The style to be used for the panel
	 * @return The container builder for the new panel
	 */
	protected ContainerBuilder<Panel> createLoginComponentsPanel(
		StyleData rPanelStyle) {
		return addPanel(rPanelStyle, new TableGridLayout(2, true, 3));
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
	 * @param sUserName The login user name
	 * @param sPassword The login password
	 * @return The login data object
	 */
	protected StringDataElement createLoginData(String sUserName,
		String sPassword) {
		String sSessionId = Cookies.getCookie(sSessionCookie);
		StringDataElement aLoginData =
			new StringDataElement(sUserName, sPassword);

		aLoginData.setProperty(AuthenticatedService.LOGIN_USER_INFO,
			createLoginUserInfo());

		if (sSessionId != null) {
			aLoginData.setProperty(AuthenticatedService.SESSION_ID,
				sSessionId);
		}

		return aLoginData;
	}

	/**
	 * Creates an information string for the user that is currently logging in.
	 *
	 * @return The user info string
	 */
	protected String createLoginUserInfo() {
		StringBuilder aLoginUserInfo = new StringBuilder();

		aLoginUserInfo.append("UserAgent: ");
		aLoginUserInfo.append(Window.Navigator.getUserAgent());
		aLoginUserInfo.append("\nApp: ");
		aLoginUserInfo.append(Window.Navigator.getAppName());
		aLoginUserInfo.append(" (");
		aLoginUserInfo.append(Window.Navigator.getAppCodeName());
		aLoginUserInfo.append(")\nVersion: ");
		aLoginUserInfo.append(Window.Navigator.getAppVersion());
		aLoginUserInfo.append("\nPlatform: ");
		aLoginUserInfo.append(Window.Navigator.getPlatform());

		return aLoginUserInfo.toString();
	}

	/**
	 * Handles login failures. There are two ways this method can be invoked.
	 * This class first tries to connect to the server with user name and
	 * password set to NULL to check for an existing authentication. If that
	 * call fails the internal container builder reference will not be NULL and
	 * will be used to create the login components to query for the user login.
	 * The builder reference will then be set to NULL.
	 *
	 * @param rCaught The exception that occurred
	 */
	protected void handleLoginFailure(Throwable rCaught) {
		aLoginButton.setEnabled(true);
		aFailureMessage.setVisible(true);
		rLoginHandler.loginFailed((Exception) rCaught);
	}

	/**
	 * Handles a successful authentication by invoking the login method
	 * {@link LoginHandler#loginSuccessful(DataElementList)}.
	 *
	 * @param rUserData The user data instance returned by the service
	 */
	protected void handleLoginSuccess(DataElementList rUserData) {
		String sSessionID =
			rUserData.getProperty(AuthenticatedService.SESSION_ID, null);

		if (sSessionID == null) {
			throw new IllegalArgumentException("No Session ID in user data");
		}

		if (aPasswordField != null) {
			aPasswordField.setText("");
			aFailureMessage.setVisible(false);

			aUserField = null;
			aPasswordField = null;
		}

		Cookies.setCookie(sSessionCookie, sSessionID);
		rLoginHandler.loginSuccessful(rUserData);
	}

	/**
	 * Performs the login with the data from the input fields.
	 */
	protected void login() {
		aLoginButton.setEnabled(false);

		String sUserName = bReauthenticate ?
		                   Cookies.getCookie(sUserCookie) :
		                   aUserField.getText();

		String sPassword = aPasswordField.getText();

		setUserNameCookie(sUserName);

		ServiceRegistry
			.getCommandService()
			.executeCommand(AuthenticatedService.LOGIN,
				createLoginData(sUserName, sPassword),
				new AsyncCallback<DataElementList>() {
					@Override
					public void onFailure(Throwable rCaught) {
						handleLoginFailure(rCaught);
					}

					@Override
					public void onSuccess(DataElementList rResult) {
						handleLoginSuccess(rResult);
					}
				});
	}

	/**
	 * Sets a cookie with the user name for re-use on subsequent logins. The
	 * expiration period of this cookie is 3 months.
	 *
	 * @param sUserName The user name to set
	 */
	protected void setUserNameCookie(String sUserName) {
		Date aExpiryDate = new Date();

		CalendarUtil.addMonthsToDate(aExpiryDate, 3);
		Cookies.setCookie(sUserCookie, sUserName, aExpiryDate);
	}
}
