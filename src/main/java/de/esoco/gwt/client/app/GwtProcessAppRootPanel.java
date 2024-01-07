//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.data.element.DataElementList;
import de.esoco.data.process.ProcessState;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.dialog.MessageBox;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.gwt.client.ui.PanelManager;

/**
 * A standard root panel manager for applications with a main process.
 *
 * @author eso
 */
public class GwtProcessAppRootPanel extends
	GwtApplicationPanelManager<Container, GwtApplicationPanelManager<?, ?>> {

	private DataElementList rUserData;

	private ProcessPanelManager aProcessPanel;

	/**
	 * Creates a new instance.
	 */
	public GwtProcessAppRootPanel() {
		super(null, EsocoGwtResources.INSTANCE.css().gaRootPanel());

		// only used for re-authentication, initial login is process-based
		setLoginMode(LoginMode.DIALOG);
	}

	@Override
	public void displayMessage(String sMessage, int nDisplayTime) {
		MessageBox.showNotification(getContainer().getView(),
			"$tiErrorMessage",
			sMessage, MessageBox.ICON_ERROR);
	}

	@Override
	public void dispose() {
		rUserData = null;

		removeApplicationPanel();

		super.dispose();
	}

	/**
	 * Returns the process panel manager that is used to display the root
	 * process.
	 *
	 * @return The process panel manager
	 */
	public ProcessPanelManager getProcessPanel() {
		return aProcessPanel;
	}

	@Override
	public void updateUI() {
		if (aProcessPanel != null) {
			aProcessPanel.updateUI();
		}
	}

	@Override
	protected void addComponents() {
		login(false);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ContainerBuilder<Container> createContainer(
		ContainerBuilder<?> rBuilder, StyleData rStyleData) {
		// as the root panel only displays a process and therefore has no
		// own UI just return parent builder to inline the process panel in
		// the main application view
		return (ContainerBuilder<Container>) rBuilder;
	}

	/**
	 * Creates the process panel that will be used to render the application
	 * process. Can be overridden to return a different instance than the
	 * default implementation {@link ProcessPanelManager}.
	 *
	 * @param rProcessState The current process state
	 * @return A new process panel manager instance
	 */
	protected ProcessPanelManager createProcessPanel(
		ProcessState rProcessState) {
		ProcessPanelManager aProcessPanelManager =
			new ProcessPanelManager(this, rProcessState.getName(), false,
				true);

		aProcessPanelManager.setDisableOnInteraction(true);

		return aProcessPanelManager;
	}

	@Override
	protected void displayProcess(ProcessState rProcessState) {
		if (rProcessState.isFinished()) {
			processFinished(null, rProcessState);
		} else {
			aProcessPanel = createProcessPanel(rProcessState);

			aProcessPanel.buildIn(this, AlignedPosition.CENTER);
			aProcessPanel.handleCommandResult(rProcessState);
		}
	}

	/**
	 * Overridden to check whether process panels are currently open.
	 *
	 * @see GwtApplicationPanelManager#getCloseWarning()
	 */
	@Override
	protected String getCloseWarning() {
		return aProcessPanel != null ? "$msgWindowCloseWarning" : null;
	}

	@Override
	protected Container getProcessContainer() {
		return getContainer();
	}

	@Override
	protected DataElementList getUserData() {
		return rUserData;
	}

	/**
	 * Overridden to perform the error handling for process executions.
	 *
	 * @see GwtApplicationPanelManager#handleError(Throwable)
	 */
	@Override
	protected void handleError(Throwable eCaught) {
		if (aProcessPanel != null) {
			aProcessPanel.handleError(eCaught);
		} else {
			displayMessage("$msgServiceCallFailed", MESSAGE_DISPLAY_TIME);
		}
	}

	@Override
	protected void logout() {
		dispose();
		checkAuthentication();
	}

	/**
	 * Overridden to execute the application process for login.
	 *
	 * @see GwtApplicationPanelManager#performLogin(boolean)
	 */
	@Override
	protected void performLogin(boolean bReauthenticate) {
		if (bReauthenticate) {
			super.performLogin(true);
		} else {
			executeMainApplicationProcess();
		}
	}

	@Override
	protected void processFinished(PanelManager<?, ?> rPanelManager,
		ProcessState rProcessState) {
		logout();
	}

	@Override
	protected void processUpdated(PanelManager<?, ?> rPanelManager,
		ProcessState rProcessState) {
		// not needed as there is only one application process
	}

	@Override
	protected void removeApplicationPanel() {
		if (aProcessPanel != null) {
			aProcessPanel.dispose();
			aProcessPanel = null;
		}

		removeAllComponents();
	}
}
