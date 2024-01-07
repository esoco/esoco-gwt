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

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.StringDataElement;

import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.DialogView;
import de.esoco.ewt.component.View;
import de.esoco.ewt.geometry.Rectangle;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.ViewStyle;

import de.esoco.gwt.shared.AuthenticatedService;
import de.esoco.gwt.shared.AuthenticationException;
import de.esoco.gwt.shared.Command;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;

/**
 * A panel manager subclass that performs the authentication of users. It also
 * implements the automatic re-execution of commands that have failed because a
 * missing or expired authentication.
 *
 * @author eso
 */
public abstract class AuthenticationPanelManager<C extends Container,
	P extends AuthenticationPanelManager<?, ?>>
	extends PanelManager<C, P> implements LoginHandler {

	/**
	 * Enumeration of the possible login modes.
	 */
	public enum LoginMode {DIALOG, PAGE}

	private static String sCookiePrefix = "";

	private Command<?, ?> rPrevCommand;

	private DataElement<?> rPrevCommandData;

	private CommandResultHandler<?> rPrevCommandHandler;

	private DialogView aLoginDialog;

	private LoginPanelManager aLoginPanel;

	private LoginMode eLoginMode = LoginMode.DIALOG;

	private CommandResultHandler<DataElementList> aGetUserDataResultHandler =
		new DefaultCommandResultHandler<DataElementList>(this) {
			@Override
			public void handleCommandResult(DataElementList rUserData) {
				userAuthenticated(rUserData);
			}
		};

	/**
	 * @see PanelManager#PanelManager(PanelManager, String)
	 */
	public AuthenticationPanelManager(P rParent, String sPanelStyle) {
		super(rParent, sPanelStyle);
	}

	/**
	 * Creates an information string for the client browser.
	 *
	 * @return The client info string
	 */
	protected static String createClientInfo() {
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
	 * Creates a new data element containing the login data. The default
	 * implementation creates a string data element with the user name as it's
	 * name and the password as it's value. It also adds the user info created
	 * by {@link #createClientInfo()} as a property with the property name
	 * {@link AuthenticatedService#LOGIN_USER_INFO} and an existing session ID
	 * (from the session cookie) with the property
	 * {@link AuthenticatedService#SESSION_ID}.
	 *
	 * @param sUserName The login user name
	 * @param sPassword The login password
	 * @return The login data object
	 */
	protected static StringDataElement createLoginData(String sUserName,
		String sPassword) {
		String sSessionId = Cookies.getCookie(getAuthenticationCookiePrefix());
		StringDataElement aLoginData =
			new StringDataElement(sUserName, sPassword);

		aLoginData.setProperty(AuthenticatedService.LOGIN_USER_INFO,
			createClientInfo());

		if (sSessionId != null) {
			aLoginData.setProperty(AuthenticatedService.SESSION_ID,
				sSessionId);
		}

		return aLoginData;
	}

	/**
	 * Returns the authentication cookie prefix for the current application.
	 *
	 * @return The cookie prefix
	 */
	public static String getAuthenticationCookiePrefix() {
		return sCookiePrefix;
	}

	/**
	 * Sets the authentication cookie prefix for the current application.
	 *
	 * @param sPrefix The cookie prefix
	 */
	public static void setAuthenticationCookiePrefix(String sPrefix) {
		sCookiePrefix = sPrefix;
	}

	/**
	 * @see PanelManager#handleCommandFailure(Command, Throwable)
	 */
	@Override
	public void handleCommandFailure(Command<?, ?> rCommand,
		Throwable rCaught) {
		if (rCaught instanceof AuthenticationException) {
			login(rCommand != AuthenticatedService.GET_USER_DATA &&
				((AuthenticationException) rCaught).isRecoverable());
		} else {
			super.handleCommandFailure(rCommand, rCaught);
		}
	}

	/**
	 * @see LoginHandler#loginFailed(Exception)
	 */
	@Override
	public void loginFailed(Exception rError) {
		handleError(rError);
	}

	/**
	 * @see LoginHandler#loginSuccessful(DataElementList)
	 */
	@Override
	public void loginSuccessful(DataElementList rUserData) {
		P rParent = getParent();

		if (rParent != null) {
			// delegate the call to the parent so that the topmost panel
			// manager
			// handles the re-execution of the last command
			rParent.loginSuccessful(rUserData);
		} else {
			hideLoginPanel();
			executePreviousCommand(rUserData);
		}
	}

	/**
	 * Creates the login panel in the given container builder.
	 *
	 * @param rBuilder        The builder to create the login panel with
	 * @param bReauthenticate TRUE if the invocation is only for a
	 *                        re-authentication of the current user
	 * @return The {@link LoginPanelManager} instance
	 */
	protected LoginPanelManager buildLoginPanel(ContainerBuilder<?> rBuilder,
		boolean bReauthenticate) {
		final LoginPanelManager aLoginPanelManager =
			new LoginPanelManager(this, this, getAuthenticationCookiePrefix(),
				bReauthenticate);

		aLoginPanelManager.buildIn(rBuilder, AlignedPosition.CENTER);

		return aLoginPanelManager;
	}

	/**
	 * Checks whether the current user is authenticated. If not the login
	 * procedure will be initiated causing the login dialog to be displayed. On
	 * successful authentication the
	 * {@link #userAuthenticated(DataElementList)}
	 * method will be invoked. Subclasses should invoked this method before
	 * performing an action that requires an authenticated user.
	 */
	protected void checkAuthentication() {
		executeCommand(AuthenticatedService.GET_USER_DATA, null,
			aGetUserDataResultHandler);
	}

	/**
	 * Creates a login panel and displays it in a dialog.
	 *
	 * @param rContext        The user interface context to display the dialog
	 *                        in
	 * @param bReauthenticate TRUE for a re-authentication of the current user
	 */
	protected void displayLoginDialog(UserInterfaceContext rContext,
		boolean bReauthenticate) {
		aLoginDialog =
			rContext.createDialog(getContainer().getView(), ViewStyle.MODAL);

		ContainerBuilder<View> aDialogBuilder =
			new ContainerBuilder<View>(aLoginDialog);

		aLoginDialog.getWidget().addStyleName(CSS.gfLoginDialog());
		aLoginDialog.setTitle(bReauthenticate ? "$tiRepeatLogin" : "$tiLogin");

		aLoginPanel = buildLoginPanel(aDialogBuilder, bReauthenticate);

		aLoginDialog.pack();

		Rectangle rScreen = rContext.getDefaultScreen().getClientArea();

		rContext.displayView(aLoginDialog,
			rScreen.getX() + rScreen.getWidth() / 2,
			rScreen.getY() + rScreen.getHeight() / 3, AlignedPosition.CENTER,
			true);
	}

	/**
	 * Executes a certain command on the server.
	 *
	 * @param rCommand       The command to execute
	 * @param rData          The data to be processed by the command
	 * @param rResultHandler The result handler to process the command
	 *                          result in
	 *                       case of a successful command execution
	 * @see PanelManager#executeCommand(Command, DataElement,
	 * CommandResultHandler)
	 */
	@Override
	protected <T extends DataElement<?>, R extends DataElement<?>> void executeCommand(
		Command<T, R> rCommand, T rData,
		CommandResultHandler<R> rResultHandler) {
		P rParent = getParent();

		if (rParent != null) {
			// delegate the call to the parent so that the topmost panel
			// manager
			// handles the command execution and the storing of the last
			// command
			rParent.executeCommand(rCommand, rData, rResultHandler);
		} else {
			this.rPrevCommand = rCommand;
			this.rPrevCommandData = rData;
			this.rPrevCommandHandler = rResultHandler;

			super.executeCommand(rCommand, rData, rResultHandler);
		}
	}

	/**
	 * Hides the login panel. The default implementation hides the login
	 * dialog.
	 * Subclasses can override this panel to modify the login panel handling.
	 */
	protected void hideLoginPanel() {
		if (aLoginPanel != null) {
			if (eLoginMode == LoginMode.DIALOG) {
				aLoginDialog.setVisible(false);
				aLoginDialog = null;
			} else {
				removeComponent(aLoginPanel.getContainer());
				aLoginPanel = null;
			}
		}
	}

	/**
	 * Will be invoked to perform a login if no user is authenticated. The
	 * default implementation will delegate the call to the parent or, if this
	 * instance is the root panel (i.e. the parent is NULL) it will invoke
	 * {@link #performLogin(boolean)}. Subclasses that want to modify the
	 * actual
	 * login should therefore override the latter method.
	 *
	 * @param bReauthenticate TRUE if this is a re-authentication because of an
	 *                        expired session
	 */
	protected final void login(boolean bReauthenticate) {
		P rParent = getParent();

		if (rParent != null) {
			rParent.login(bReauthenticate);
		} else {
			performLogin(bReauthenticate);
		}
	}

	/**
	 * Performs a login of a user by displaying a login form that is typically
	 * based on {@link LoginPanelManager} which will then execute the
	 * server-side login.
	 *
	 * @param bReauthenticate TRUE if this is a re-authentication because of an
	 *                        expired session
	 */
	protected void performLogin(boolean bReauthenticate) {
		if (!bReauthenticate) {
			// if no re-auth possible let the app start over by processing the
			// initial get user data command
			rPrevCommand = AuthenticatedService.GET_USER_DATA;
			rPrevCommandData = null;
			rPrevCommandHandler = aGetUserDataResultHandler;
		}

		if (eLoginMode == LoginMode.DIALOG) {
			UserInterfaceContext rContext = getContext();

			displayLoginDialog(rContext, bReauthenticate);

			rContext.runLater(new Runnable() {
				@Override
				public void run() {
					aLoginPanel.requestFocus();
				}
			});
		} else {
			removeApplicationPanel();

			aLoginPanel = buildLoginPanel(this, bReauthenticate);
			aLoginPanel
				.getContainer()
				.getElement()
				.getStyle()
				.setPosition(Position.RELATIVE);
			aLoginPanel.requestFocus();
		}
	}

	/**
	 * Sets the login mode of this panel. If set to dialog the login panel will
	 * appear in a dialog view above the (initial) application view. Otherwise
	 * the login panel will replace the application view and the subclass must
	 * implement the method {@link #removeApplicationPanel()}.
	 *
	 * <p>To change the login mode (the default is dialog mode) this method
	 * must be invoked before the login takes place. To change the login mode
	 * while the application runs it must be done while no login is in
	 * progress,
	 * e.g. after invocation of {@link #hideLoginPanel()}.</p>
	 *
	 * @param eMode The login mode
	 */
	protected void setLoginMode(LoginMode eMode) {
		eLoginMode = eMode;
	}

	/**
	 * Will be invoked to initialize this instance from the user data obtained
	 * through {@link #checkAuthentication()}. Subclasses can override this
	 * method to perform user-specific initializations. The default
	 * implementation does nothing.
	 *
	 * @param rUserData The user data received from the server
	 */
	protected void userAuthenticated(DataElementList rUserData) {
	}

	/**
	 * Executes the previous command that had failed to execute because of an
	 * authentication error.
	 *
	 * @param rUserData The user data received from the server after
	 *                  authentication
	 */
	@SuppressWarnings("unchecked")
	private void executePreviousCommand(DataElementList rUserData) {
		if (rPrevCommand == AuthenticatedService.GET_USER_DATA) {
			((CommandResultHandler<DataElementList>) rPrevCommandHandler).handleCommandResult(
				rUserData);
		} else {
			executeCommand(
				(Command<DataElement<?>, DataElement<?>>) rPrevCommand,
				rPrevCommandData,
				(CommandResultHandler<DataElement<?>>) rPrevCommandHandler);
		}
	}
}
