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
package de.esoco.gwt.client.app;

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.IntegerDataElement;
import de.esoco.data.element.SelectionDataElement;
import de.esoco.data.process.ProcessDescription;
import de.esoco.data.process.ProcessState;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;

import de.esoco.gwt.client.ui.AuthenticationPanelManager;
import de.esoco.gwt.client.ui.DefaultCommandResultHandler;
import de.esoco.gwt.client.ui.PanelManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;

import static de.esoco.gwt.shared.GwtApplicationService.ENTITY_ID_NAME;
import static de.esoco.gwt.shared.GwtApplicationService.USER_PROCESSES;
import static de.esoco.gwt.shared.ProcessService.APPLICATION_MAIN_PROCESS;
import static de.esoco.gwt.shared.ProcessService.APPLICATION_PROCESS_PATH;
import static de.esoco.gwt.shared.ProcessService.EXECUTE_PROCESS;

/**
 * An abstract base class for the panel managers of GWT applications. The
 * default implementations of most methods always invoke the same method of the
 * parent panel recursively. Therefore there must always be a subclass that
 * serves as a root panel manager which overrides these methods and performs the
 * actual action defined by the respective method.
 *
 * @author eso
 */
public abstract class GwtApplicationPanelManager<C extends Container,
	P extends GwtApplicationPanelManager<?, ?>>
	extends AuthenticationPanelManager<C, P> {

	/**
	 * The default display time for messages.
	 */
	public static final int MESSAGE_DISPLAY_TIME = 10000;

	private Map<ProcessDescription, Button> aProcessButtons = null;

	/**
	 * @see AuthenticationPanelManager#AuthenticationPanelManager(AuthenticationPanelManager,
	 * String)
	 */
	public GwtApplicationPanelManager(P rParent, String sPanelStyle) {
		super(rParent, sPanelStyle);
	}

	/**
	 * Returns a list of the process toolbar buttons in the order in which they
	 * have been created.
	 *
	 * @return The list of process buttons
	 */
	public final List<Button> getProcessButtons() {
		return new ArrayList<Button>(aProcessButtons.values());
	}

	/**
	 * Adds buttons for process descriptions to a panel. The panel should have
	 * been created with
	 * {@link #addToolbar(ContainerBuilder, de.esoco.ewt.style.StyleData,
	 * de.esoco.ewt.style.StyleData, int)}.
	 *
	 * @param rBuilder   The container builder to add the buttons with
	 * @param rProcesses The process descriptions
	 */
	protected void addProcessButtons(ContainerBuilder<Panel> rBuilder,
		DataElementList rProcesses) {
		int nCount = rProcesses.getElementCount();

		if (aProcessButtons == null) {
			aProcessButtons =
				new LinkedHashMap<ProcessDescription, Button>(nCount);
		}

		for (int i = 0; i < nCount; i++) {
			DataElement<?> rDataElement = rProcesses.getElement(i);

			if (rDataElement instanceof ProcessDescription) {
				ProcessDescription rDesc = (ProcessDescription) rDataElement;
				final Button aButton;

				if (rDesc.isSeparator()) {
					aButton = null;
					addToolbarSeparator(rBuilder);
				} else {
					final String sName = rDesc.getName();

					aButton = addToolbarButton(rBuilder, "#$im" + sName,
						"$prc" + sName);

					aButton.addEventListener(EventType.ACTION,
						new EwtEventHandler() {
							@Override
							public void handleEvent(EwtEvent rEvent) {
								executeProcess(sName, getSelectedElement());
							}
						});
				}

				aProcessButtons.put(rDesc, aButton);
			}
		}

		setProcessButtonStates(false);
	}

	/**
	 * Appends a certain text to the window title.
	 */
	protected void appendToWindowTitle(String sText) {
		String sTitle = Window.getTitle();

		int nHyphen = sTitle.lastIndexOf('-');

		if (nHyphen > 0) {
			sTitle = sTitle.substring(0, nHyphen - 1);
		}

		Window.setTitle(sTitle + " - " + sText);
	}

	/**
	 * This method must be implemented by a panel manager in a panel manager
	 * hierarchy to display the result of a process execution. This method will
	 * be invoked by {@link #executeProcess(String, DataElement)}. The default
	 * implementation invokes the parent method if a parent exists.
	 *
	 * @param rProcessState The process state returned by the process execution
	 */
	protected void displayProcess(ProcessState rProcessState) {
		P rParent = getParent();

		if (rParent != null) {
			rParent.displayProcess(rProcessState);
		}
	}

	/**
	 * Executes the application process with a certain name.
	 *
	 * @param sProcessName rUserData The user data to read the process from
	 */
	protected void executeApplicationProcess(String sProcessName) {
		executeProcess(APPLICATION_PROCESS_PATH + "/" + sProcessName, null);
	}

	/**
	 * Executes the main application process.
	 */
	protected void executeMainApplicationProcess() {
		executeApplicationProcess(APPLICATION_MAIN_PROCESS);
	}

	/**
	 * Executes a certain process on the server.
	 *
	 * @param sProcessName  The name of the process to execute
	 * @param rProcessInput The ID of the currently selected entity or -1 for
	 *                      none
	 */
	protected void executeProcess(String sProcessName,
		DataElement<?> rProcessInput) {
		String sProcessGroup = getProcessGroup();

		if (sProcessGroup != null) {
			sProcessName = sProcessGroup + '/' + sProcessName;
		}

		P rParent = getParent();

		if (rParent != null) {
			rParent.executeProcess(sProcessName, rProcessInput);
		} else {
			ProcessDescription rProcessDescription;

			if (sProcessName.startsWith(APPLICATION_PROCESS_PATH)) {
				rProcessDescription =
					new ProcessDescription(sProcessName, null, 0, false);
			} else {
				sProcessName = USER_PROCESSES + "/" + sProcessName;

				rProcessDescription =
					(ProcessDescription) getUserData().getElementAt(
						sProcessName);

				if (rProcessDescription.isInputRequired()) {
					rProcessDescription.setProcessInput(rProcessInput);
				}
			}

			setClientSize(rProcessDescription);
			rProcessDescription.setClientLocale(
				LocaleInfo.getCurrentLocale().getLocaleName());

			executeCommand(EXECUTE_PROCESS, rProcessDescription,
				new DefaultCommandResultHandler<ProcessState>(this) {
					@Override
					public void handleCommandResult(
						ProcessState rProcessState) {
						rProcessState.setClientInfo(createClientInfo());
						displayProcess(rProcessState);
					}
				});
		}
	}

	/**
	 * Helper method to lookup a certain data element in a list of elements and
	 * to return it's value as a string.
	 *
	 * @param rList    The list of data elements
	 * @param sElement The name of the element to return the value of
	 * @return A string describing the element value
	 */
	protected String findElement(DataElementList rList, String sElement) {
		return "" + rList.getElementAt(sElement).getValue();
	}

	/**
	 * Can be overridden by subclasses to return the container in which the
	 * result of a process execution will be displayed. This will be used by
	 * the
	 * method {@link #setClientSize(ProcessDescription)} to calculate the
	 * correct size of the client area. If not overridden the container of this
	 * panel will be returned.
	 *
	 * @return The process container
	 */
	protected C getProcessContainer() {
		return getContainer();
	}

	/**
	 * Can be overridden by subclasses to return a specific process group that
	 * will be prepended to the names of processes to be executed. The default
	 * implementation returns NULL.
	 *
	 * @return The process group name
	 */
	protected String getProcessGroup() {
		return null;
	}

	/**
	 * This method can be overridden by subclasses that support the
	 * selection of
	 * elements. The implementation must return the a data element that
	 * describes the currently selected element or NULL if no selection is
	 * available. This method will be invoked from the event handler set by
	 * {@link #addProcessButtons(ContainerBuilder, DataElementList)}. The
	 * default implementation always returns NULL.
	 *
	 * @return A data element describing the currently selected element or NULL
	 * 1 for no selection
	 */
	protected DataElement<?> getSelectedElement() {
		return null;
	}

	/**
	 * Helper method that creates an integer data element containing the ID
	 * of a
	 * currently selected entity from a selection data element.
	 *
	 * @param rDataElement The data element to read the selection from
	 * @return A new integer data element for the current selection or NULL if
	 * no entity is selected
	 */
	protected IntegerDataElement getSelectedEntityId(
		DataElement<?> rDataElement) {
		String sSelection = rDataElement.getValue().toString();
		IntegerDataElement aSelectionId = null;

		if (!SelectionDataElement.NO_SELECTION.equals(sSelection)) {
			aSelectionId = new IntegerDataElement(ENTITY_ID_NAME,
				Integer.parseInt(sSelection));
		}

		return aSelectionId;
	}

	/**
	 * Returns the client-specific data for the currently authenticated user.
	 * This call is forwarded to the parent manager so that it must be
	 * implemented by the root panel manager.
	 *
	 * @return The user data or NULL if no user has been authenticated
	 */
	protected DataElementList getUserData() {
		return getParent().getUserData();
	}

	/**
	 * Performs a logout of the current user. This call is forwarded to the
	 * parent manager so that it must be implemented by the root panel manager.
	 */
	protected void logout() {
		getParent().logout();
	}

	/**
	 * Will be invoked by the {@link ProcessPanelManager} after a process has
	 * finished execution. The standard implementation simply invokes the
	 * parent
	 * panel manger's method if a parent is available. The handling is
	 * typically
	 * implemented in the root of the panel hierarchy.
	 *
	 * @param rProcessPanelManager The manager of the causing panel
	 * @param rProcessState        The last state of the finished process
	 */
	protected void processFinished(PanelManager<?, ?> rProcessPanelManager,
		ProcessState rProcessState) {
		P rParent = getParent();

		if (rParent != null) {
			rParent.processFinished(rProcessPanelManager, rProcessState);
		}
	}

	/**
	 * Will be invoked by the {@link ProcessPanelManager} after a process has
	 * updated it's state, typically when it stops for an interaction. The
	 * standard implementation simply invokes the parent panel manger's method
	 * if a parent is available. The handling is typically implemented in the
	 * root of the panel hierarchy.
	 *
	 * @param rPanelManager The manager of the causing panel
	 * @param rProcessState The current process state
	 */
	protected void processUpdated(PanelManager<?, ?> rPanelManager,
		ProcessState rProcessState) {
		P rParent = getParent();

		if (rParent != null) {
			getParent().processUpdated(rPanelManager, rProcessState);
		}
	}

	/**
	 * Sets the current size of the user's client (the web browser) into the
	 * given process description.
	 */
	protected void setClientSize(ProcessDescription rProcessDescription) {
		C rContainer = getProcessContainer();

		int w = rContainer.getWidth();
		int h = rContainer.getHeight();

		if (w == 0 || h == 0) {
			w = Window.getClientWidth();
			h = Window.getClientHeight();
		}

		rProcessDescription.setClientSize(w, h);
	}

	/**
	 * Sets the process button states depending on the process requirement for
	 * an input value and the current selection state of this panel.
	 *
	 * @param bHasSelection nEntityId The ID of the selected entity or -1 for
	 *                      none
	 */
	protected void setProcessButtonStates(boolean bHasSelection) {
		for (Entry<ProcessDescription, Button> rProcessButton :
			aProcessButtons.entrySet()) {
			Button rButton = rProcessButton.getValue();

			if (rButton != null) {
				rButton.setEnabled(bHasSelection ||
					!rProcessButton.getKey().isInputRequired());
			}
		}
	}
}
