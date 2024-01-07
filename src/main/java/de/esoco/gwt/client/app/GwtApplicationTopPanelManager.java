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

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.dialog.MessageBox;
import de.esoco.ewt.dialog.MessageBox.ResultHandler;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;

import de.esoco.gwt.client.ui.DefaultCommandResultHandler;
import de.esoco.gwt.shared.AuthenticatedService;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

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

	private final StyleData aLogoStyle;

	private final StyleData aUserInfoStyle;

	private final StyleData aLogoutLinkStyle;

	private final StyleData aMessageStyle;

	private Label aLogoutLink;

	private Label aMessageLabel;

	private Timer aClearMessageTimer;

	private HandlerRegistration rWindowClosingHandler;

	private HandlerRegistration rCloseHandler;

	/**
	 * @see GwtApplicationPanelManager#GwtApplicationPanelManager(GwtApplicationPanelManager,
	 * String)
	 */
	protected GwtApplicationTopPanelManager(P rParent, String sPanelStyle,
		String sLogoStyle, String sUserInfoStyle, String sLogoutLinkStyle,
		String sMessageStyle) {
		super(rParent, sPanelStyle);

		aLogoStyle = StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, sLogoStyle);
		aUserInfoStyle =
			AlignedPosition.LEFT.set(WEB_ADDITIONAL_STYLES, sUserInfoStyle);
		aLogoutLinkStyle = StyleData.DEFAULT
			.set(WEB_ADDITIONAL_STYLES, sLogoutLinkStyle)
			.setFlags(HYPERLINK, VERTICAL_ALIGN_CENTER);
		aMessageStyle =
			AlignedPosition.CENTER.set(WEB_ADDITIONAL_STYLES, sMessageStyle);

		rWindowClosingHandler = Window.addWindowClosingHandler(this);
		rCloseHandler = Window.addCloseHandler(this);
	}

	/**
	 * @see GwtApplicationPanelManager#displayMessage(String, int)
	 */
	@Override
	public void displayMessage(String sMessage, int nDisplayTime) {
		if (aClearMessageTimer != null) {
			aClearMessageTimer.cancel();
			aClearMessageTimer = null;
		}

		aMessageLabel.setText(sMessage != null ? sMessage : "");

		if (nDisplayTime > 0) {
			aClearMessageTimer = new Timer() {
				@Override
				public void run() {
					aMessageLabel.setText("");
				}
			};

			aClearMessageTimer.schedule(MESSAGE_DISPLAY_TIME);
		}
	}

	@Override
	public void dispose() {
		rWindowClosingHandler.removeHandler();
		rCloseHandler.removeHandler();

		super.dispose();
	}

	/**
	 * Returns the component that is used to display messages.
	 *
	 * @return The message component
	 */
	public final Component getMessageComponent() {
		return aMessageLabel;
	}

	/**
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent rEvent) {
		Object rSource = rEvent.getSource();

		if (rSource == aLogoutLink) {
			checkLogout("$tiConfirmLogout", "$msgConfirmLogout");
		}
	}

	/**
	 * Handles the closing or reloading of the browser window.
	 *
	 * @see CloseHandler#onClose(CloseEvent)
	 */
	@Override
	public void onClose(CloseEvent<Window> rEvent) {
		// logout is not possible from this method, service calls won't be
		// executed if performed here
		dispose();
	}

	@Override
	public void onWindowClosing(ClosingEvent rEvent) {
		UserInterfaceContext rContext = getContext();
		String sCloseWarning = getCloseWarning();

		if (rContext != null && sCloseWarning != null) {
			rEvent.setMessage(rContext.expandResource(sCloseWarning));
		}
	}

	/**
	 * Sets the user info display.
	 *
	 * @param rUserData The user data to create the info display from
	 */
	public void setUserInfo(DataElementList rUserData) {
		if (rUserData != null) {
			String sUser =
				findElement(rUserData, AuthenticatedService.USER_NAME);

			aLogoutLink.setText(sUser.replaceAll(" ", "&nbsp;"));
			aLogoutLink.setToolTip("$ttLogout");
			aLogoutLink.setVisible(true);
			aMessageLabel.setText("");
		} else {
			aLogoutLink.setVisible(false);
			aMessageLabel.setText("$lblDoLogin");
		}
	}

	@Override
	protected void addComponents() {
		ContainerBuilder<Panel> aBuilder = createUserInfoPanel(aUserInfoStyle);

		aBuilder.addLabel(aLogoStyle, null, "#$imLogo");
		addUserComponents(aBuilder);

		aBuilder = addPanel(
			AlignedPosition.CENTER.setFlags(StyleFlag.VERTICAL_ALIGN_CENTER),
			new FlowLayout(true));

		aMessageLabel = aBuilder.addLabel(aMessageStyle, "$lblDoLogin", null);
	}

	/**
	 * Adds the user information and logout components.
	 *
	 * @param rBuilder The builder to add the components with
	 */
	protected void addUserComponents(ContainerBuilder<?> rBuilder) {
		aLogoutLink = rBuilder.addLabel(aLogoutLinkStyle, "", "#$imLogout");
		aLogoutLink.setVisible(false);
		aLogoutLink.addEventListener(EventType.ACTION, this);
	}

	/**
	 * Displays a confirmation message box and performs a logout if the user
	 * accepts.
	 *
	 * @param sTitle   The message box title
	 * @param sMessage The message text
	 */
	protected void checkLogout(String sTitle, String sMessage) {
		if (getCloseWarning() != null) {
			MessageBox.showQuestion(getPanel().getView(), sTitle, sMessage,
				MessageBox.ICON_QUESTION, new ResultHandler() {
					@Override
					public void handleResult(int nButton) {
						if (nButton == 1) {
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
		ContainerBuilder<?> rBuilder, StyleData rStyleData) {
		return rBuilder.addPanel(rStyleData);
	}

	/**
	 * Creates the user info panel.
	 *
	 * @param rStyle The style data for the panel
	 * @return The user info panel
	 */
	protected ContainerBuilder<Panel> createUserInfoPanel(StyleData rStyle) {
		return addPanel(aUserInfoStyle, new TableGridLayout(2));
	}

	/**
	 * Executes the logout command and invokes the {@link #logout()} method of
	 * the parent class on success.
	 */
	void performLogout() {
		executeCommand(AuthenticatedService.LOGOUT, null,
			new DefaultCommandResultHandler<DataElement<?>>(this) {
				@Override
				public void handleCommandResult(DataElement<?> rResult) {
					logout();
				}
			});
	}
}
