//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.gwt.client.app;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.dialog.MessageBox;
import de.esoco.ewt.dialog.MessageBox.ResultHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;
import de.esoco.gwt.client.ui.DefaultCommandResultHandler;
import de.esoco.gwt.shared.AuthenticatedService;

import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;
import static de.esoco.ewt.style.StyleFlag.HYPERLINK;
import static de.esoco.ewt.style.StyleFlag.VERTICAL_ALIGN_CENTER;

/**
 * Panel manager that creates a panel for the top view of the main window.
 *
 * @author eso
 */
public abstract class GwtApplicationTopPanelManager<P extends GwtApplicationPanelManager<?, ?>>
	extends GwtApplicationPanelManager<Panel, P>
	implements EwtEventHandler, ClosingHandler, CloseHandler<Window> {

	private final StyleData logoStyle;

	private final StyleData userInfoStyle;

	private final StyleData logoutLinkStyle;

	private final StyleData messageStyle;

	private Label logoutLink;

	private Label messageLabel;

	private Timer clearMessageTimer;

	private final HandlerRegistration windowClosingHandler;

	private final HandlerRegistration closeHandler;

	/**
	 * @see GwtApplicationPanelManager#GwtApplicationPanelManager(GwtApplicationPanelManager,
	 * String)
	 */
	protected GwtApplicationTopPanelManager(P parent, String panelStyle,
		String logoStyle, String userInfoStyle, String logoutLinkStyle,
		String messageStyle) {
		super(parent, panelStyle);

		this.logoStyle =
			StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, logoStyle);
		this.userInfoStyle =
			AlignedPosition.LEFT.set(WEB_ADDITIONAL_STYLES, userInfoStyle);
		this.logoutLinkStyle = StyleData.DEFAULT
			.set(WEB_ADDITIONAL_STYLES, logoutLinkStyle)
			.setFlags(HYPERLINK, VERTICAL_ALIGN_CENTER);
		this.messageStyle =
			AlignedPosition.CENTER.set(WEB_ADDITIONAL_STYLES, messageStyle);

		windowClosingHandler = Window.addWindowClosingHandler(this);
		closeHandler = Window.addCloseHandler(this);
	}

	/**
	 * @see GwtApplicationPanelManager#displayMessage(String, int)
	 */
	@Override
	public void displayMessage(String message, int displayTime) {
		if (clearMessageTimer != null) {
			clearMessageTimer.cancel();
			clearMessageTimer = null;
		}

		messageLabel.setText(message != null ? message : "");

		if (displayTime > 0) {
			clearMessageTimer = new Timer() {
				@Override
				public void run() {
					messageLabel.setText("");
				}
			};

			clearMessageTimer.schedule(MESSAGE_DISPLAY_TIME);
		}
	}

	@Override
	public void dispose() {
		windowClosingHandler.removeHandler();
		closeHandler.removeHandler();

		super.dispose();
	}

	/**
	 * Returns the component that is used to display messages.
	 *
	 * @return The message component
	 */
	public final Component getMessageComponent() {
		return messageLabel;
	}

	/**
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent event) {
		Object source = event.getSource();

		if (source == logoutLink) {
			checkLogout("$tiConfirmLogout", "$msgConfirmLogout");
		}
	}

	/**
	 * Handles the closing or reloading of the browser window.
	 *
	 * @see CloseHandler#onClose(CloseEvent)
	 */
	@Override
	public void onClose(CloseEvent<Window> event) {
		// logout is not possible from this method, service calls won't be
		// executed if performed here
		dispose();
	}

	@Override
	public void onWindowClosing(ClosingEvent event) {
		UserInterfaceContext context = getContext();
		String closeWarning = getCloseWarning();

		if (context != null && closeWarning != null) {
			event.setMessage(context.expandResource(closeWarning));
		}
	}

	/**
	 * Sets the user info display.
	 *
	 * @param userData The user data to create the info display from
	 */
	public void setUserInfo(DataElementList userData) {
		if (userData != null) {
			String user = findElement(userData,
				AuthenticatedService.USER_NAME);

			logoutLink.setText(user.replaceAll(" ", "&nbsp;"));
			logoutLink.setToolTip("$ttLogout");
			logoutLink.setVisible(true);
			messageLabel.setText("");
		} else {
			logoutLink.setVisible(false);
			messageLabel.setText("$lblDoLogin");
		}
	}

	@Override
	protected void addComponents() {
		ContainerBuilder<Panel> builder = createUserInfoPanel(userInfoStyle);

		builder.addLabel(logoStyle, null, "#$imLogo");
		addUserComponents(builder);

		builder = addPanel(
			AlignedPosition.CENTER.setFlags(StyleFlag.VERTICAL_ALIGN_CENTER),
			new FlowLayout(true));

		messageLabel = builder.addLabel(messageStyle, "$lblDoLogin", null);
	}

	/**
	 * Adds the user information and logout components.
	 *
	 * @param builder The builder to add the components with
	 */
	protected void addUserComponents(ContainerBuilder<?> builder) {
		logoutLink = builder.addLabel(logoutLinkStyle, "", "#$imLogout");
		logoutLink.setVisible(false);
		logoutLink.addEventListener(EventType.ACTION, this);
	}

	/**
	 * Displays a confirmation message box and performs a logout if the user
	 * accepts.
	 *
	 * @param title   The message box title
	 * @param message The message text
	 */
	protected void checkLogout(String title, String message) {
		if (getCloseWarning() != null) {
			MessageBox.showQuestion(getPanel().getView(), title, message,
				MessageBox.ICON_QUESTION, new ResultHandler() {
					@Override
					public void handleResult(int button) {
						if (button == 1) {
							performLogout();
						}
					}
				});
		} else {
			performLogout();
		}
	}

	@Override
	protected ContainerBuilder<Panel> createContainer(
		ContainerBuilder<?> builder, StyleData styleData) {
		return builder.addPanel(styleData);
	}

	/**
	 * Creates the user info panel.
	 *
	 * @param style The style data for the panel
	 * @return The user info panel
	 */
	protected ContainerBuilder<Panel> createUserInfoPanel(StyleData style) {
		return addPanel(userInfoStyle, new TableGridLayout(2));
	}

	/**
	 * Executes the logout command and invokes the {@link #logout()} method of
	 * the parent class on success.
	 */
	void performLogout() {
		executeCommand(AuthenticatedService.LOGOUT, null,
			new DefaultCommandResultHandler<DataElement<?>>(this) {
				@Override
				public void handleCommandResult(DataElement<?> result) {
					logout();
				}
			});
	}
}
