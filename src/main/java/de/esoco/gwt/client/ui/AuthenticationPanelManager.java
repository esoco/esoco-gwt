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

	private static String cookiePrefix = "";

	private Command<?, ?> prevCommand;

	private DataElement<?> prevCommandData;

	private CommandResultHandler<?> prevCommandHandler;

	private DialogView loginDialog;

	private LoginPanelManager loginPanel;

	private LoginMode loginMode = LoginMode.DIALOG;

	private CommandResultHandler<DataElementList> getUserDataResultHandler =
		new DefaultCommandResultHandler<DataElementList>(this) {
			@Override
			public void handleCommandResult(DataElementList userData) {
				userAuthenticated(userData);
			}
		};

	/**
	 * @see PanelManager#PanelManager(PanelManager, String)
	 */
	public AuthenticationPanelManager(P parent, String panelStyle) {
		super(parent, panelStyle);
	}

	/**
	 * Creates an information string for the client browser.
	 *
	 * @return The client info string
	 */
	protected static String createClientInfo() {
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
	 * Creates a new data element containing the login data. The default
	 * implementation creates a string data element with the user name as it's
	 * name and the password as it's value. It also adds the user info created
	 * by {@link #createClientInfo()} as a property with the property name
	 * {@link AuthenticatedService#LOGIN_USER_INFO} and an existing session ID
	 * (from the session cookie) with the property
	 * {@link AuthenticatedService#SESSION_ID}.
	 *
	 * @param userName The login user name
	 * @param password The login password
	 * @return The login data object
	 */
	protected static StringDataElement createLoginData(String userName,
		String password) {
		String sessionId = Cookies.getCookie(getAuthenticationCookiePrefix());
		StringDataElement loginData = new StringDataElement(userName,
			password);

		loginData.setProperty(AuthenticatedService.LOGIN_USER_INFO,
			createClientInfo());

		if (sessionId != null) {
			loginData.setProperty(AuthenticatedService.SESSION_ID, sessionId);
		}

		return loginData;
	}

	/**
	 * Returns the authentication cookie prefix for the current application.
	 *
	 * @return The cookie prefix
	 */
	public static String getAuthenticationCookiePrefix() {
		return cookiePrefix;
	}

	/**
	 * Sets the authentication cookie prefix for the current application.
	 *
	 * @param prefix The cookie prefix
	 */
	public static void setAuthenticationCookiePrefix(String prefix) {
		cookiePrefix = prefix;
	}

	/**
	 * @see PanelManager#handleCommandFailure(Command, Throwable)
	 */
	@Override
	public void handleCommandFailure(Command<?, ?> command, Throwable caught) {
		if (caught instanceof AuthenticationException) {
			login(command != AuthenticatedService.GET_USER_DATA &&
				((AuthenticationException) caught).isRecoverable());
		} else {
			super.handleCommandFailure(command, caught);
		}
	}

	/**
	 * @see LoginHandler#loginFailed(Exception)
	 */
	@Override
	public void loginFailed(Exception error) {
		handleError(error);
	}

	/**
	 * @see LoginHandler#loginSuccessful(DataElementList)
	 */
	@Override
	public void loginSuccessful(DataElementList userData) {
		P parent = getParent();

		if (parent != null) {
			// delegate the call to the parent so that the topmost panel
			// manager
			// handles the re-execution of the last command
			parent.loginSuccessful(userData);
		} else {
			hideLoginPanel();
			executePreviousCommand(userData);
		}
	}

	/**
	 * Creates the login panel in the given container builder.
	 *
	 * @param builder        The builder to create the login panel with
	 * @param reauthenticate TRUE if the invocation is only for a
	 *                       re-authentication of the current user
	 * @return The {@link LoginPanelManager} instance
	 */
	protected LoginPanelManager buildLoginPanel(ContainerBuilder<?> builder,
		boolean reauthenticate) {
		final LoginPanelManager loginPanelManager =
			new LoginPanelManager(this, this, getAuthenticationCookiePrefix(),
				reauthenticate);

		loginPanelManager.buildIn(builder, AlignedPosition.CENTER);

		return loginPanelManager;
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
			getUserDataResultHandler);
	}

	/**
	 * Creates a login panel and displays it in a dialog.
	 *
	 * @param context        The user interface context to display the dialog
	 *                       in
	 * @param reauthenticate TRUE for a re-authentication of the current user
	 */
	protected void displayLoginDialog(UserInterfaceContext context,
		boolean reauthenticate) {
		loginDialog =
			context.createDialog(getContainer().getView(), ViewStyle.MODAL);

		ContainerBuilder<View> dialogBuilder =
			new ContainerBuilder<View>(loginDialog);

		loginDialog.getWidget().addStyleName(CSS.gfLoginDialog());
		loginDialog.setTitle(reauthenticate ? "$tiRepeatLogin" : "$tiLogin");

		loginPanel = buildLoginPanel(dialogBuilder, reauthenticate);

		loginDialog.pack();

		Rectangle screen = context.getDefaultScreen().getClientArea();

		context.displayView(loginDialog, screen.getX() + screen.getWidth() / 2,
			screen.getY() + screen.getHeight() / 3, AlignedPosition.CENTER,
			true);
	}

	/**
	 * Executes a certain command on the server.
	 *
	 * @param command       The command to execute
	 * @param data          The data to be processed by the command
	 * @param resultHandler The result handler to process the command result in
	 *                      case of a successful command execution
	 * @see PanelManager#executeCommand(Command, DataElement,
	 * CommandResultHandler)
	 */
	@Override
	protected <T extends DataElement<?>, R extends DataElement<?>> void executeCommand(
		Command<T, R> command, T data, CommandResultHandler<R> resultHandler) {
		P parent = getParent();

		if (parent != null) {
			// delegate the call to the parent so that the topmost panel
			// manager
			// handles the command execution and the storing of the last
			// command
			parent.executeCommand(command, data, resultHandler);
		} else {
			this.prevCommand = command;
			this.prevCommandData = data;
			this.prevCommandHandler = resultHandler;

			super.executeCommand(command, data, resultHandler);
		}
	}

	/**
	 * Hides the login panel. The default implementation hides the login
	 * dialog.
	 * Subclasses can override this panel to modify the login panel handling.
	 */
	protected void hideLoginPanel() {
		if (loginPanel != null) {
			if (loginMode == LoginMode.DIALOG) {
				loginDialog.setVisible(false);
				loginDialog = null;
			} else {
				removeComponent(loginPanel.getContainer());
				loginPanel = null;
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
	 * @param reauthenticate TRUE if this is a re-authentication because of an
	 *                       expired session
	 */
	protected final void login(boolean reauthenticate) {
		P parent = getParent();

		if (parent != null) {
			parent.login(reauthenticate);
		} else {
			performLogin(reauthenticate);
		}
	}

	/**
	 * Performs a login of a user by displaying a login form that is typically
	 * based on {@link LoginPanelManager} which will then execute the
	 * server-side login.
	 *
	 * @param reauthenticate TRUE if this is a re-authentication because of an
	 *                       expired session
	 */
	protected void performLogin(boolean reauthenticate) {
		if (!reauthenticate) {
			// if no re-auth possible let the app start over by processing the
			// initial get user data command
			prevCommand = AuthenticatedService.GET_USER_DATA;
			prevCommandData = null;
			prevCommandHandler = getUserDataResultHandler;
		}

		if (loginMode == LoginMode.DIALOG) {
			UserInterfaceContext context = getContext();

			displayLoginDialog(context, reauthenticate);

			context.runLater(new Runnable() {
				@Override
				public void run() {
					loginPanel.requestFocus();
				}
			});
		} else {
			removeApplicationPanel();

			loginPanel = buildLoginPanel(this, reauthenticate);
			loginPanel
				.getContainer()
				.getElement()
				.getStyle()
				.setPosition(Position.RELATIVE);
			loginPanel.requestFocus();
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
	 * @param mode The login mode
	 */
	protected void setLoginMode(LoginMode mode) {
		loginMode = mode;
	}

	/**
	 * Will be invoked to initialize this instance from the user data obtained
	 * through {@link #checkAuthentication()}. Subclasses can override this
	 * method to perform user-specific initializations. The default
	 * implementation does nothing.
	 *
	 * @param userData The user data received from the server
	 */
	protected void userAuthenticated(DataElementList userData) {
	}

	/**
	 * Executes the previous command that had failed to execute because of an
	 * authentication error.
	 *
	 * @param userData The user data received from the server after
	 *                 authentication
	 */
	@SuppressWarnings("unchecked")
	private void executePreviousCommand(DataElementList userData) {
		if (prevCommand == AuthenticatedService.GET_USER_DATA) {
			((CommandResultHandler<DataElementList>) prevCommandHandler).handleCommandResult(
				userData);
		} else {
			executeCommand(
				(Command<DataElement<?>, DataElement<?>>) prevCommand,
				prevCommandData,
				(CommandResultHandler<DataElement<?>>) prevCommandHandler);
		}
	}
}
