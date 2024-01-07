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

import com.google.gwt.event.shared.HandlerRegistration;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.process.ProcessState;
import de.esoco.data.process.ProcessState.ProcessExecutionMode;
import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.dialog.MessageBox;
import de.esoco.ewt.dialog.MessageBox.ResultHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.graphics.Image;
import de.esoco.ewt.layout.DockLayout;
import de.esoco.ewt.layout.FillLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;
import de.esoco.gwt.client.res.EsocoGwtCss;
import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.gwt.client.ui.CommandResultHandler;
import de.esoco.gwt.client.ui.DataElementListUI;
import de.esoco.gwt.client.ui.DataElementListView;
import de.esoco.gwt.client.ui.DataElementPanelManager;
import de.esoco.gwt.client.ui.DataElementPanelManager.InteractiveInputHandler;
import de.esoco.gwt.client.ui.DataElementTablePanelManager;
import de.esoco.gwt.client.ui.DataElementUI;
import de.esoco.gwt.client.ui.PanelManager;
import de.esoco.gwt.shared.GwtApplicationService;
import de.esoco.gwt.shared.ServiceException;
import de.esoco.lib.property.ContentType;
import de.esoco.lib.property.InteractionEventType;
import de.esoco.lib.property.LayoutType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;
import static de.esoco.gwt.shared.StorageService.ERROR_ENTITY_LOCKED;
import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.ContentProperties.RESOURCE_ID;
import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.StateProperties.STRUCTURE_CHANGED;
import static de.esoco.lib.property.StyleProperties.STYLE;

/**
 * A panel manager subclass that handles the interaction with a process that
 * runs in the service.
 *
 * @author eso
 */
public class ProcessPanelManager extends
	GwtApplicationPanelManager<Container, GwtApplicationPanelManager<?, ?>>
	implements InteractiveInputHandler, CommandResultHandler<ProcessState>,
	EwtEventHandler {

	/**
	 * A prefix for the generation of labels from process names.
	 */
	public static final String PROCESS_LABEL_PREFIX = "$lblPrc";

	private static final EsocoGwtCss CSS = EsocoGwtResources.INSTANCE.css();

	private static final StyleData TOP_PANEL_STYLE = AlignedPosition.TOP
		.h(6)
		.set(WEB_ADDITIONAL_STYLES, CSS.gaProcessTopPanel());

	private static final StyleData TOOLBAR_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES,
			CSS.gaProcessButtonPanel());

	private static final StyleData PARAM_PANEL_STYLE =
		AlignedPosition.CENTER.set(WEB_ADDITIONAL_STYLES,
			CSS.gaProcessParamPanel());

	private static final StyleData SUMMARY_PANEL_STYLE =
		AlignedPosition.CENTER.set(WEB_ADDITIONAL_STYLES,
			CSS.gaProcessSummaryPanel());

	private static final StyleData TITLE_LABEL_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gaProcessTitle());

	private static final StyleData MESSAGE_LABEL_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gaErrorMessage());

	private static final StyleData SUMMARY_LABEL_STYLE =
		AlignedPosition.CENTER.set(CONTENT_TYPE, ContentType.HTML);

	private final String processName;

	private final boolean showNavigationBar;

	private final boolean renderInline;

	private final HandlerRegistration uiInspectorEventHandler = null;

	private DataElementPanelManager paramPanelManager;

	private Container paramPanel;

	private Button prevButton;

	private Button nextButton;

	private Button cancelButton;

	private Button reloadButton;

	private Label titleLabel;

	private Label messageLabel;

	private Image busyImage = null;

	private ProcessState processState = null;

	private String previousStep = null;

	private String previousStyle = "";

	private boolean disableOnInteraction = true;

	private boolean autoContinue = false;

	private boolean pauseAutoContinue = false;

	private boolean cancelProcess = false;

	private boolean cancelled = false;

	private Map<String, DataElementListView> processViews =
		Collections.emptyMap();

	private boolean locked = false;

	/**
	 * Creates a new instance for a certain process.
	 *
	 * @param parent      The parent panel manager
	 * @param processName The name of the process
	 */
	public ProcessPanelManager(GwtApplicationPanelManager<?, ?> parent,
		String processName) {
		this(parent, processName, true, false);
	}

	/**
	 * Creates a new instance for a certain process.
	 *
	 * @param parent            The parent panel manager
	 * @param processName       The name of the process
	 * @param showNavigationBar TRUE to show the process navigation bar at the
	 *                          top, FALSE to show only the process parameters
	 * @param renderInline      TRUE to render the process UI in the parent
	 *                          container, FALSE to create a separate panel
	 *                          (may
	 *                          not be compatible with a navigation bar)
	 */
	public ProcessPanelManager(GwtApplicationPanelManager<?, ?> parent,
		String processName, boolean showNavigationBar, boolean renderInline) {
		super(parent, CSS.gaProcessPanel());

		this.processName = processName;
		this.showNavigationBar = showNavigationBar;
		this.renderInline = renderInline;
	}

	/**
	 * Creates a label for the current state of a process. If the process state
	 * is NULL or the process is finished an empty string will be returned.
	 *
	 * @param processState The process sate to create the label for
	 * @return The resulting label string
	 */
	public static String createProcessLabel(ProcessState processState) {
		return processState != null && !processState.isFinished() ?
		       PROCESS_LABEL_PREFIX + processState.getCurrentStep() :
		       "";
	}

	/**
	 * Creates the panel that renders the process parameters.
	 *
	 * @return The panel builder
	 */
	public ContainerBuilder<? extends Container> createParameterPanel() {
		if (paramPanelManager != null) {
			paramPanelManager.dispose();
		}

		removeParameterPanel();

		ContainerBuilder<? extends Container> builder = renderInline ?
		                                                this :
		                                                addPanel(
			                                                PARAM_PANEL_STYLE,
			                                                new FillLayout(
				                                                true));

		paramPanel = builder.getContainer();

		return builder;
	}

	/**
	 * Sets the message label visible and displays a message in the panels
	 * message area.
	 *
	 * @see GwtApplicationPanelManager#displayMessage(String, int)
	 */
	@Override
	public void displayMessage(String message, int displayTime) {
		if (messageLabel != null) {
			messageLabel.setVisible(true);
			messageLabel.setText(message);
		}
	}

	@Override
	public void dispose() {
		for (DataElementListView view : processViews.values()) {
			view.hide();
		}

		super.dispose();
	}

	@Override
	public void handleCommandResult(ProcessState newState) {
		boolean finishProcess =
			processState != null && processState.isFinalStep();

		locked = false;
		processState = newState;
		autoContinue = processState.isAutoContinue();

		if (cancelProcess) {
			// cancel process if user confirmed the cancellation during an
			// automatically continued process step
			cancelProcess = false;
			cancelProcess();
		} else if (!cancelled) {
			for (ProcessState newProcess : processState.getSpawnProcesses()) {
				displayProcess(newProcess);
			}

			update(finishProcess);
		}
	}

	/**
	 * Handles the failure of an asynchronous call. The handling differs if the
	 * exception is recoverable or not. If not, a message will be displayed to
	 * the user. If the exception is a recoverable exception a rollback to the
	 * previous interaction will be done. The user can then input the data
	 * again.
	 *
	 * @param caught The exception that is caught
	 */
	@Override
	public void handleError(Throwable caught) {
		locked = false;
		autoContinue = false;

		if (caught instanceof ServiceException &&
			((ServiceException) caught).isRecoverable()) {
			ServiceException service = (ServiceException) caught;

			handleRecoverableError(service);
		} else {
			handleUnrecoverableError(caught);
		}

		setUserInterfaceState();
	}

	/**
	 * Handles the events of user interface elements in this panel.
	 *
	 * @param event The event
	 */
	@Override
	public void handleEvent(EwtEvent event) {
		if (event.getSource() instanceof Button) {
			Button source = (Button) event.getSource();

			if (source.isEnabled()) {
				lockUserInterface();

				if (source == nextButton) {
					handleNextProcessStepEvent();
				} else if (source == prevButton) {
					handlePreviousProcessStepEvent();
				} else if (source == cancelButton) {
					handleCancelProcessEvent();
				} else if (source == reloadButton) {
					reload();
				}
			}
		}
	}

	/**
	 * Executes the current process step to send the new value when an
	 * interactive input event occurs.
	 *
	 * @see InteractiveInputHandler#handleInteractiveInput(DataElement,
	 * InteractionEventType)
	 */
	@Override
	public void handleInteractiveInput(DataElement<?> interactionElement,
		InteractionEventType eventType) {
		if (!locked && !processState.isFinished()) {
			locked = true;
			lockUserInterface();

			ProcessState interactionState =
				createInteractionState(interactionElement, eventType);

			executeProcess(interactionState, ProcessExecutionMode.EXECUTE);
		}
	}

	/**
	 * Checks the disable on interaction option.
	 *
	 * @return TRUE if the panel elements are disabled on interactions
	 */
	public final boolean isDisableOnInteraction() {
		return disableOnInteraction;
	}

	/**
	 * Reloads the process data by re-executing the process.
	 */
	public void reload() {
		executeProcess(processState, ProcessExecutionMode.RELOAD);
	}

	/**
	 * Sets the disable on interaction option.
	 *
	 * @param disableOnInteraction TRUE to disable the panel elements on
	 *                             interactions
	 */
	public final void setDisableOnInteraction(boolean disableOnInteraction) {
		this.disableOnInteraction = disableOnInteraction;
	}

	@Override
	protected void addComponents() {
		if (showNavigationBar) {
			if (busyImage == null) {
				busyImage = getContext().createImage("$imBusy");
			}

			buildTopPanel();
		}

		updateParameterPanel();
		setUserInterfaceState();

		addUiInspectorEventHandler();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ContainerBuilder<Container> createContainer(
		ContainerBuilder<?> builder, StyleData styleData) {
		ContainerBuilder<? extends Container> panelBuilder;

		if (renderInline) {
			panelBuilder = builder;
		} else {
			panelBuilder = builder.addPanel(styleData, showNavigationBar ?
			                                           new DockLayout(false,
				                                           false) :
			                                           new FillLayout());
		}

		return (ContainerBuilder<Container>) panelBuilder;
	}

	/**
	 * Executes the process to receive the next process state.
	 *
	 * @param state The process state to transmit to the server
	 * @param mode  The execution mode
	 */
	protected void executeProcess(ProcessState state,
		ProcessExecutionMode mode) {
		previousStep = processState.getCurrentStep();

		state.setExecutionMode(mode);
		setClientSize(state);
		executeCommand(GwtApplicationService.EXECUTE_PROCESS, state, this);
	}

	/**
	 * Handles service exceptions for errors that can be recovered by
	 * displaying
	 * the error process state.
	 *
	 * @param service The recoverable service exception
	 */
	protected void handleRecoverableError(ServiceException service) {
		String message = service.getMessage();
		Map<String, String> errorParams = service.getErrorParameters();

		if (message.equals(ERROR_ENTITY_LOCKED)) {
			Object[] messageArgs = errorParams.values().toArray();

			message =
				getContext().getResourceString("msg" + message, messageArgs);
		} else {
			ProcessState newState = service.getProcessState();

			if (newState != null) {
				processState = newState;
				updateParameterPanel();
			}

			if (!message.startsWith("$")) {
				message = "$msg" + message;
			}

			if (errorParams != null) {
				for (Entry<String, String> error : errorParams.entrySet()) {
					String elementName = error.getKey();
					DataElementUI<?> errorUI =
						paramPanelManager.findDataElementUI(elementName);

					if (errorUI != null) {
						errorUI.setErrorMessage(error.getValue());
					} else {
						for (DataElementListView view :
							processViews.values()) {
							errorUI = view
								.getViewUI()
								.getPanelManager()
								.findDataElementUI(elementName);

							if (errorUI != null) {
								errorUI.setErrorMessage(error.getValue());

								break;
							}
						}
					}
				}
			}
		}

		displayMessage(message, 0);
	}

	/**
	 * Handles fatal exceptions that cannot be recovered by re-executing the
	 * current process.
	 *
	 * @param caught The exception that signaled the non-recoverable error
	 */
	protected void handleUnrecoverableError(Throwable caught) {
		cancelled = true;
		buildSummaryPanel(caught);
	}

	/**
	 * Overridden to remove the UI inspector event handler registration.
	 *
	 * @see GwtApplicationPanelManager#processFinished(PanelManager,
	 * ProcessState)
	 */
	@Override
	protected void processFinished(PanelManager<?, ?> processPanelManager,
		ProcessState processState) {
		if (uiInspectorEventHandler != null) {
			uiInspectorEventHandler.removeHandler();
		}

		super.processFinished(processPanelManager, processState);
	}

	/**
	 * Updates the UI from the process state after an interaction.
	 *
	 * @param finishProcess TRUE if the process needs to be finished
	 */
	protected void update(boolean finishProcess) {
		if (processState.isFinished()) {
			if (finishProcess) {
				processFinished(this, processState);
			} else {
				setTitle(null);
				buildSummaryPanel(null);
				setUserInterfaceState();
			}
		} else {
			processUpdated(ProcessPanelManager.this, processState);
			setTitle(processState.getName());
			updateParameterPanel();

			if (autoContinue && !pauseAutoContinue) {
				executeProcess(processState, ProcessExecutionMode.EXECUTE);
			}

			setUserInterfaceState();
		}
	}

	/**
	 * Lock the user interface against input events.
	 */
	void lockUserInterface() {
		if (showNavigationBar) {
			nextButton.setImage(busyImage);
			messageLabel.setVisible(false);

			prevButton.setEnabled(false);
			nextButton.setEnabled(false);
			cancelButton.setEnabled(false);
			reloadButton.setEnabled(false);
		}

		if (paramPanelManager != null && disableOnInteraction) {
			paramPanelManager.enableInteraction(false);
		}

		for (DataElementListView view : processViews.values()) {
			view.enableInteraction(false);
		}
	}

	/**
	 * Adds a data element panel for a list of process parameter data elements.
	 */
	private void addParameterDataElementPanel() {
		ContainerBuilder<?> builder = createParameterPanel();
		List<DataElement<?>> params = processState.getInteractionParams();
		String step = processState.getCurrentStep();

		DataElement<?> firstElement = params.get(0);

		if (params.size() == 1 && firstElement instanceof DataElementList &&
			firstElement.getProperty(LAYOUT, LayoutType.TABLE) !=
				LayoutType.TABLE) {
			paramPanelManager = DataElementPanelManager.newInstance(this,
				(DataElementList) firstElement);
		} else // legacy handling for root-level table layouts
		{
			String name = processName + " " + step;
			String style = processState.getProperty(STYLE, null);

			if (style != null && style.length() > 0) {
				name += " " + style;
			}

			DataElementList paramsList = new DataElementList(name, params);

			paramsList.setProperty(RESOURCE_ID, name);

			paramPanelManager =
				new DataElementTablePanelManager(this, paramsList);
		}

		StyleData panelStyle =
			StyleData.DEFAULT.setFlags(StyleFlag.VERTICAL_ALIGN_TOP);

		paramPanelManager.buildIn(builder, panelStyle);
		paramPanelManager.setInteractiveInputHandler(this);
	}

	/**
	 * Adds a global event handler that listens for the invocation of the UI
	 * inspector.
	 */
	private void addUiInspectorEventHandler() {
//		uiInspectorEventHandler =
//			Event.addNativePreviewHandler(new NativePreviewHandler()
//				{
//					@Override
//					public void onPreviewNativeEvent(NativePreviewEvent event)
//					{
//						NativeEvent nativeEvent = event.getNativeEvent();
//
//						if ((event.getTypeInt() & Event.ONKEYDOWN) != 0 &&
//							nativeEvent.getKeyCode() == 73 &&
//							nativeEvent.getAltKey() &&
//							nativeEvent.getCtrlKey())
//						{
//							toggleUiInspector();
//						}
//					}
//				});
	}

	/**
	 * Builds the summary panel.
	 *
	 * @param exception An error exception that occurred or NULL for success
	 */

	private void buildSummaryPanel(Throwable exception) {
		String message =
			(exception != null ? "$msgProcessError" : "$msgProcessSuccess");

		removeParameterPanel();

		if (showNavigationBar) {
			titleLabel.setVisible(false);
		}

		if (renderInline) {
			addLabel(StyleData.DEFAULT.set(CONTENT_TYPE, ContentType.HTML),
				message, null);
		} else {
			ContainerBuilder<Panel> builder =
				addPanel(SUMMARY_PANEL_STYLE, new FillLayout(true));

			builder.addLabel(SUMMARY_LABEL_STYLE, message, null);
		}
	}

	/**
	 * Builds the panel with the process control buttons.
	 */
	private void buildTopPanel() {
		ContainerBuilder<Panel> toolbar =
			addToolbar(this, TOP_PANEL_STYLE, TOOLBAR_STYLE, 0);

		prevButton =
			addToolbarButton(toolbar, "#imNavPrev", "$ttProcessPrevious");
		nextButton = addToolbarButton(toolbar, "#imNavNext", "$ttProcessNext");

		addToolbarSeparator(toolbar);

		cancelButton =
			addToolbarButton(toolbar, "#imCancel", "$ttProcessCancel");

		String title = createProcessLabel(processState);

		addToolbarSeparator(toolbar);
		titleLabel = toolbar.addLabel(TITLE_LABEL_STYLE, title, null);

		addToolbarSeparator(toolbar);
		reloadButton = addToolbarButton(toolbar, "#imReload", "$ttReload");
		addToolbarSeparator(toolbar);
		messageLabel = toolbar.addLabel(MESSAGE_LABEL_STYLE, "", "#imWarning");

		messageLabel.setVisible(false);

		nextButton.addEventListener(EventType.ACTION, this);
		prevButton.addEventListener(EventType.ACTION, this);
		cancelButton.addEventListener(EventType.ACTION, this);
		reloadButton.addEventListener(EventType.ACTION, this);
	}

	/**
	 * Cancels the currently running process and notifies the parent panel
	 * manager.
	 */
	private void cancelProcess() {
		if (isCommandExecuting()) {
			// if a command is currently executed delay the actual canceling
			// until handleCommandResult() is invoked
			cancelProcess = true;
		} else {
			cancelled = true;
			executeProcess(processState, ProcessExecutionMode.CANCEL);
			processFinished(this, processState);
		}
	}

	/**
	 * Creates a {@link ProcessState} instance for an interaction event.
	 *
	 * @param interactionElement The data element from which the event
	 *                           originated
	 * @param eventType          The event type
	 * @return The interaction process state
	 */
	private ProcessState createInteractionState(
		DataElement<?> interactionElement, InteractionEventType eventType) {
		List<DataElement<?>> modifiedElements = new ArrayList<>();

		paramPanelManager.collectInput(modifiedElements);

		for (DataElementListView view : processViews.values()) {
			view.collectInput(modifiedElements);
		}

		ProcessState interactionState =
			new ProcessState(processState, eventType, interactionElement,
				modifiedElements);

		// reset all modification flags for next interaction loop
		for (DataElement<?> element : modifiedElements) {
			element.setModified(false);
		}

		return interactionState;
	}

	/**
	 * Handles the button selection from the confirmation message box displayed
	 * by {@link #handleCancelProcessEvent()}.
	 *
	 * @param button The selected button
	 */
	private void handleCancelConfirmation(int button) {
		pauseAutoContinue = false;

		if (button == 1) {
			cancelProcess();
		} else if (autoContinue && !isCommandExecuting()) {
			// restart an automatically continuing process if it had been
			// stopped in the meantime with pauseAutoContinue
			executeProcess(processState, ProcessExecutionMode.EXECUTE);
		}

		setUserInterfaceState();
	}

	/**
	 * Handles the event of canceling the currently running process.
	 */
	private void handleCancelProcessEvent() {
		if (cancelled) {
			processFinished(this, processState);
		} else {
			if (processState.hasImmedidateInteraction()) {
				cancelProcess();
			} else {
				if (isCommandExecuting()) {
					// pause a running process while cancel dialog is displayed
					pauseAutoContinue = true;
				}

				MessageBox.showQuestion(getPanel().getView(),
					"$tiCancelProcess", "$msgCancelProcess",
					MessageBox.ICON_QUESTION, new ResultHandler() {
						@Override
						public void handleResult(int button) {
							handleCancelConfirmation(button);
						}
					});
			}
		}
	}

	/**
	 * Handles the event of executing the next process step after an
	 * interaction.
	 */
	private void handleNextProcessStepEvent() {
		ProcessState interactionState = processState;

		if (processState.isFinished()) {
			processFinished(this, processState);
		} else if (paramPanelManager != null) {
			interactionState = createInteractionState(null, null);
		}

		executeProcess(interactionState, ProcessExecutionMode.EXECUTE);
	}

	/**
	 * Handles a rollback to the previous step event.
	 */
	private void handlePreviousProcessStepEvent() {
		executeProcess(processState, ProcessExecutionMode.ROLLBACK);
	}

	/**
	 * Manages the views that are defined in the process state.
	 */
	private void manageProcessViews() {
		List<DataElementList> viewParams = processState.getViewParams();

		Map<String, DataElementListView> newViews =
			new HashMap<>(viewParams.size());

		for (DataElementList viewParam : viewParams) {
			String viewName = viewParam.getName();
			DataElementListView view = processViews.remove(viewName);

			if (view != null && view.isVisible()) {
				// always update full view structure
				viewParam.setFlag(STRUCTURE_CHANGED);
				view.updateDataElement(viewParam, true);
			} else {
				view = new DataElementListView(paramPanelManager, viewParam);
				view.show();
			}

			newViews.put(viewName, view);
		}

		// close views that are no longer listed
		for (DataElementListView oldView : processViews.values()) {
			oldView.hide();
		}

		processViews = newViews;
	}

	/**
	 * Removes the current center panel.
	 */
	private void removeParameterPanel() {
		if (renderInline) {
			getContainer().clear();
			paramPanelManager = null;
		} else if (paramPanel != null) {
			removeComponent(paramPanel);
			paramPanel = null;
		}
	}

	/**
	 * Changes the title in the navigation bar if it is visible.
	 *
	 * @param processStepName The name of the current process step or NULL to
	 *                        clear the title
	 */
	private void setTitle(String processStepName) {
		if (showNavigationBar) {
			String text = "";
			Image image = null;

			if (processStepName != null) {
				String imageRef = "$im" + processStepName;

				image = getContext().createImage(imageRef);
				text = createProcessLabel(processState);
			}
			titleLabel.setText(text);
			titleLabel.setImage(image);
		}
	}

	/**
	 * Sets the state of the user interface elements from the current process
	 * state.
	 */
	private void setUserInterfaceState() {
		boolean hasState = processState != null && !processState.isFinished();

		if (paramPanelManager != null) {
			paramPanelManager.enableInteraction(hasState);
		}

		for (DataElementListView view : processViews.values()) {
			view.enableInteraction(hasState);
		}

		if (showNavigationBar) {
			UserInterfaceContext context = getContext();

			String nextImage;
			String nextToolTip;

			if (processState != null &&
				(processState.isFinished() || processState.isFinalStep())) {
				nextImage = "$imFinish";
				nextToolTip = "$ttProcessFinish";
			} else {
				nextImage = "$imNavNext";
				nextToolTip = "$ttProcessNext";
			}

			nextButton.setImage(context.createImage(nextImage));
			nextButton.setToolTip(nextToolTip);

			// if auto-continue is active only enable cancel to allow the
			// interruption of the process
			cancelButton.setEnabled(hasState);
			reloadButton.setEnabled(!autoContinue && hasState);
			prevButton.setEnabled(!autoContinue && !cancelled && hasState &&
				processState.canRollback());
			nextButton.setEnabled(!autoContinue && !cancelled &&
				!(hasState && processState.hasImmedidateInteraction()));
		}
	}

	/**
	 * Updates the UIs for the data elements that have been modified during the
	 * last interaction.
	 *
	 * @return TRUE if all UIs could be updated, FALSE if a rebuild is
	 * necessary
	 */
	private boolean updateInteractionUIs() {
		List<DataElementListUI> listUIs = new ArrayList<>();

		for (DataElement<?> updateElement :
			processState.getInteractionParams()) {
			DataElementUI<?> updateUI =
				paramPanelManager.findDataElementUI(updateElement.getName());

			if (updateUI != null) {
				updateUI.updateDataElement(updateElement, true);

				if (updateUI instanceof DataElementListUI) {
					listUIs.add((DataElementListUI) updateUI);
				}
			} else {
				return false;
			}
		}

		// finally update list UIs after all children have been updated
		for (DataElementListUI listUI : listUIs) {
			listUI.getPanelManager().updateFromChildChanges();
		}

		manageProcessViews();

		return true;
	}

	/**
	 * Updates the panel that displays the process parameters of the current
	 * process interaction.
	 */
	private void updateParameterPanel() {
		if (processState != null && !processState.isFinished()) {
			String currentStep = processState.getCurrentStep();
			String stepStyle = processState.getProperty(STYLE, "");

			if (paramPanelManager != null && currentStep.equals(previousStep) &&
				stepStyle.equals(previousStyle)) {
				if (updateInteractionUIs()) {
					paramPanelManager.clearErrors();
				} else {
					// if a UI has not been found the structure has changed,
					// therefore rebuild the complete panel; addComponents()
					// will then invoke updateParameterPanel() again which then
					// falls into the else branch below
					paramPanelManager.dispose();
					paramPanelManager = null;
					rebuild();
				}
			} else if (!processState.getInteractionParams().isEmpty()) {
				previousStyle = stepStyle;
				addParameterDataElementPanel();
				manageProcessViews();
			}
		}
	}
}
