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
import de.esoco.data.element.DataElement.CopyMode;
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.EWT;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.SelectableButton;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.layout.GenericLayout;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.res.EsocoGwtCss;
import de.esoco.gwt.client.res.EsocoGwtResources;

import de.esoco.lib.property.InteractionEventType;
import de.esoco.lib.property.LabelStyle;
import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.SingleSelection;
import de.esoco.lib.property.StateProperties;
import de.esoco.lib.text.TextConvert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import static de.esoco.lib.property.LayoutProperties.HTML_HEIGHT;
import static de.esoco.lib.property.LayoutProperties.HTML_WIDTH;
import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.StateProperties.CURRENT_SELECTION;
import static de.esoco.lib.property.StateProperties.SELECTION_DEPENDENCY;
import static de.esoco.lib.property.StateProperties.SELECTION_DEPENDENCY_REVERSE_PREFIX;
import static de.esoco.lib.property.StateProperties.STRUCTURE_CHANGED;
import static de.esoco.lib.property.StyleProperties.LABEL_STYLE;
import static de.esoco.lib.property.StyleProperties.SHOW_LABEL;
import static de.esoco.lib.property.StyleProperties.STYLE;

/**
 * The base class for panel managers that display and handle data elements.
 *
 * @author eso
 */
public abstract class DataElementPanelManager
	extends PanelManager<Container, PanelManager<?, ?>> {

	static final EsocoGwtCss CSS = EsocoGwtResources.INSTANCE.css();

	static final StyleData ELEMENT_STYLE =
		addStyles(StyleData.DEFAULT, CSS.gfDataElement());

	static final StyleData ELEMENT_LABEL_STYLE =
		addStyles(StyleData.DEFAULT, CSS.gfDataElementLabel());

	static final StyleData FORM_LABEL_STYLE =
		ELEMENT_LABEL_STYLE.set(LABEL_STYLE, LabelStyle.FORM);

	static final StyleData HEADER_LABEL_STYLE =
		addStyles(StyleData.DEFAULT, CSS.gfDataElementHeader());

	private static final Set<LayoutType> ORDERED_LAYOUTS =
		EnumSet.of(LayoutType.DOCK, LayoutType.SPLIT);

	private static final Set<LayoutType> SWITCH_LAYOUTS =
		EnumSet.of(LayoutType.TABS, LayoutType.STACK, LayoutType.DECK);

	private static final Set<LayoutType> GRID_LAYOUTS =
		EnumSet.of(LayoutType.GRID, LayoutType.FORM, LayoutType.GROUP);

	private DataElementList rDataElementList;

	private LayoutType eLayout;

	private Map<String, DataElementUI<?>> aDataElementUIs;

	private InteractiveInputHandler rInteractiveInputHandler = null;

	private boolean bHandlingSelectionEvent = false;

	private DataElementInteractionHandler<DataElementList> aInteractionHandler;

	private String sChildIndent;

	/**
	 * Creates a new instance.
	 *
	 * @param rParent          The parent panel manager
	 * @param rDataElementList The data elements to display grouped
	 */
	protected DataElementPanelManager(PanelManager<?, ?> rParent,
		DataElementList rDataElementList) {
		super(rParent, createPanelStyle(rDataElementList));

		this.rDataElementList = rDataElementList;
	}

	/**
	 * Checks whether two lists of data elements contains the same data
	 * elements. The elements must be the same regargind their name and order.
	 *
	 * @param rList1 The first list of data elements to compare
	 * @param rList2 The second list of data elements to compare
	 * @return TRUE if the argument lists differ at least in one data element
	 */
	public static boolean containsSameElements(List<DataElement<?>> rList1,
		List<DataElement<?>> rList2) {
		int nCount = rList1.size();
		boolean bHasSameElements = (rList2.size() == nCount);

		if (bHasSameElements) {
			for (int i = 0; i < nCount; i++) {
				bHasSameElements =
					rList1.get(i).getName().equals(rList2.get(i).getName());

				if (!bHasSameElements) {
					break;
				}
			}
		}

		return bHasSameElements;
	}

	/**
	 * Static helper method to create the panel manager's style name.
	 *
	 * @param rDataElementList The data element list to create the style name
	 *                         for
	 * @return The panel style
	 */
	protected static String createPanelStyle(DataElementList rDataElementList) {
		StringBuilder aStyle = new StringBuilder(CSS.gfDataElementPanel());

		{
			LayoutType eDisplayMode =
				rDataElementList.getProperty(LAYOUT, LayoutType.TABLE);

			aStyle.append(" gf-DataElement");
			aStyle.append(
				TextConvert.capitalizedIdentifier(eDisplayMode.name()));
			aStyle.append("Panel");
		}

		aStyle.append(' ');
		aStyle.append(rDataElementList.getResourceId());

		return aStyle.toString();
	}

	/**
	 * Factory method that creates a new subclass instance based on the
	 * {@link LayoutType} of the data element list argument.
	 *
	 * @param rParent          The parent panel manager
	 * @param rDataElementList The list of data elements to be handled by the
	 *                         new panel manager
	 * @return A new data element panel manager instance
	 */
	public static DataElementPanelManager newInstance(
		PanelManager<?, ?> rParent, DataElementList rDataElementList) {
		DataElementPanelManager aPanelManager = null;

		LayoutType eLayout =
			rDataElementList.getProperty(LAYOUT, LayoutType.TABLE);

		if (eLayout == LayoutType.TABLE) {
			aPanelManager =
				new DataElementTablePanelManager(rParent, rDataElementList);
		} else if (eLayout == LayoutType.INLINE) {
			aPanelManager =
				new DataElementInlinePanelManager(rParent, rDataElementList);
		} else if (SWITCH_LAYOUTS.contains(eLayout)) {
			aPanelManager =
				new DataElementSwitchPanelManager(rParent, rDataElementList);
		} else if (ORDERED_LAYOUTS.contains(eLayout)) {
			aPanelManager =
				new DataElementOrderedPanelManager(rParent, rDataElementList);
		} else if (GRID_LAYOUTS.contains(eLayout)) {
			aPanelManager =
				new DataElementGridPanelManager(rParent, rDataElementList);
		} else {
			aPanelManager =
				new DataElementLayoutPanelManager(rParent, rDataElementList);
		}

		return aPanelManager;
	}

	/**
	 * Registers an event listener for events on all data element components
	 * that are displayed in this panel. The listener will be added by means of
	 * {@link DataElementUI#addEventListener(EventType, EwtEventHandler)}.
	 *
	 * @param rEventType The event type the listener shall be registered for
	 * @param rListener  The event listener to be notified of events
	 */
	public void addElementEventListener(EventType rEventType,
		EwtEventHandler rListener) {
		for (DataElementUI<?> rDataElementUI : aDataElementUIs.values()) {
			rDataElementUI.addEventListener(rEventType, rListener);
		}
	}

	/**
	 * Registers an event listener for events of a certain data element
	 * component that is displayed in this panel. The listener will be added
	 * with {@link DataElementUI#addEventListener(EventType, EwtEventHandler)}.
	 *
	 * @param rDataElement The data element to register the event listener for
	 * @param rEventType   The event type the listener shall be registered for
	 * @param rListener    The event listener to be notified of events
	 */
	public void addElementEventListener(DataElement<?> rDataElement,
		EventType rEventType, EwtEventHandler rListener) {
		DataElementUI<?> rDataElementUI =
			aDataElementUIs.get(rDataElement.getName());

		if (rDataElementUI != null) {
			rDataElementUI.addEventListener(rEventType, rListener);
		} else {
			throw new IllegalArgumentException(
				"Unknown data element: " + rDataElement);
		}
	}

	/**
	 * Hierarchically checks all data element UIs for dependencies in the root
	 * data element panel manager.
	 *
	 * @see PanelManager#buildIn(ContainerBuilder, StyleData)
	 */
	@Override
	public void buildIn(ContainerBuilder<?> rBuilder, StyleData rStyle) {
		super.buildIn(rBuilder, rStyle);

		// only check selection dependencies from the root after all child data
		// element UIs have been initialized
		if (!(getParent() instanceof DataElementPanelManager)) {
			checkSelectionDependencies(this, Arrays.asList(rDataElementList));
		}
	}

	/**
	 * Clears all error indicators in the contained data element UIs.
	 */
	public void clearErrors() {
		for (DataElementUI<?> rUI : aDataElementUIs.values()) {
			rUI.clearError();
		}
	}

	/**
	 * Collects the values from the input components into the corresponding
	 * data
	 * elements.
	 *
	 * @param rModifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> rModifiedElements) {
		checkIfDataElementListModified(rModifiedElements);

		for (DataElementUI<?> rUI : aDataElementUIs.values()) {
			if (rUI != null) {
				rUI.collectInput(rModifiedElements);
			}
		}
	}

	/**
	 * Overridden to dispose the existing data element UIs.
	 *
	 * @see PanelManager#dispose()
	 */
	@Override
	public void dispose() {
		for (DataElementUI<?> rUI : aDataElementUIs.values()) {
			rUI.dispose();
		}

		aDataElementUIs.clear();
	}

	/**
	 * Enables or disables interactions through this panel manager's user
	 * interface.
	 *
	 * @param bEnable TRUE to enable interactions, FALSE to disable them
	 */
	public void enableInteraction(boolean bEnable) {
		for (DataElementUI<?> rUI : aDataElementUIs.values()) {
			rUI.enableInteraction(bEnable);
		}
	}

	/**
	 * Searches for a data element with a certain name in this manager's
	 * hierarchy.
	 *
	 * @param sName The name of the data element to search
	 * @return The matching data element or NULL if no such element exists
	 */
	public DataElement<?> findDataElement(String sName) {
		return rDataElementList.findChild(sName);
	}

	/**
	 * Searches for the UI of a data element with a certain name in this
	 * manager's hierarchy.
	 *
	 * @param sElementName The name of the data element to search the UI for
	 * @return The matching data element UI or NULL if no such element exists
	 */
	public DataElementUI<?> findDataElementUI(String sElementName) {
		DataElementUI<?> rElementUI = aDataElementUIs.get(sElementName);

		if (rElementUI == null) {
			for (DataElementUI<?> rUI : aDataElementUIs.values()) {
				if (rUI instanceof DataElementListUI) {
					DataElementPanelManager rPanelManager =
						((DataElementListUI) rUI).getPanelManager();

					rElementUI = rPanelManager.findDataElementUI(sElementName);

					if (rElementUI != null) {
						break;
					}
				}
			}
		}

		return rElementUI;
	}

	/**
	 * Returns a certain element in this manager's list of data elements.
	 *
	 * @param nIndex The index of the element to return
	 * @return The data element
	 */
	public final DataElement<?> getDataElement(int nIndex) {
		return rDataElementList.getElement(nIndex);
	}

	/**
	 * Returns the dataElementList value.
	 *
	 * @return The dataElementList value
	 */
	public final DataElementList getDataElementList() {
		return rDataElementList;
	}

	/**
	 * Returns the UI for a certain data element from the hierarchy of this
	 * instance. Implementations must search child panel manager too if they
	 * don't contain the data element themselves.
	 *
	 * @param rDataElement The data element to return the UI for
	 * @return The data element UI for the given element or NULL if not found
	 */
	public final DataElementUI<?> getDataElementUI(
		DataElement<?> rDataElement) {
		DataElementUI<?> rDataElementUI =
			aDataElementUIs.get(rDataElement.getName());

		if (rDataElementUI == null) {
			for (DataElementUI<?> rUI : aDataElementUIs.values()) {
				if (rUI instanceof DataElementListUI) {
					rDataElementUI = ((DataElementListUI) rUI)
						.getPanelManager()
						.getDataElementUI(rDataElement);
				}

				if (rDataElementUI != null) {
					break;
				}
			}
		}

		return rDataElementUI;
	}

	/**
	 * Returns the root panel manager of this instance's hierarchy. If this
	 * instance is already the root of the hierarchy (i.e. it has no parent) it
	 * will be returned directly.
	 *
	 * @return The root panel manager
	 */
	public DataElementPanelManager getRoot() {
		PanelManager<?, ?> rParent = getParent();

		return rParent instanceof DataElementPanelManager ?
		       ((DataElementPanelManager) rParent).getRoot() :
		       this;
	}

	/**
	 * Sets the visibility of a data element. The default implementation does
	 * nothing but subclasses can override this method if the need to modify
	 * their state on visibility changes.
	 *
	 * @param rElementUI The UI of the data element
	 * @param bVisible   The visibility of the data element
	 */
	public void setElementVisibility(DataElementUI<?> rElementUI,
		boolean bVisible) {
	}

	/**
	 * Sets the handler of interactive input events for data elements.
	 *
	 * @param rHandler The interactive input handler
	 */
	public final void setInteractiveInputHandler(
		InteractiveInputHandler rHandler) {
		rInteractiveInputHandler = rHandler;
	}

	/**
	 * Updates this instance from a new data element list.
	 *
	 * @param rNewDataElementList The list containing the new data elements
	 * @param bUpdateUI           TRUE to also update the UI, FALSE to only
	 *                            update the data element references
	 */
	public void update(DataElementList rNewDataElementList,
		boolean bUpdateUI) {
		boolean bIsUpdate = !rNewDataElementList.hasFlag(STRUCTURE_CHANGED) &&
			rNewDataElementList.getName().equals(rDataElementList.getName()) &&
			containsSameElements(rNewDataElementList.getElements(),
				rDataElementList.getElements());

		boolean bStyleChanged =
			!Objects.equals(rDataElementList.getProperty(STYLE, null),
				rNewDataElementList.getProperty(STYLE, null));

		rDataElementList = rNewDataElementList;

		if (bIsUpdate) {
			updateElementUIs(bUpdateUI);
		} else {
			dispose();
			rebuild();
		}

		updateFromProperties(bStyleChanged);
	}

	/**
	 * Updates this list from properties of updated child data elements. This
	 * will be invoked after all children have been update and therefore
	 * replaced in the hierarchy. Can be overridden by subclasses that need to
	 * react to child updates if no re-build has been performed.
	 */
	public void updateFromChildChanges() {
	}

	@Override
	public void updateUI() {
		for (DataElementUI<?> rElementUI : aDataElementUIs.values()) {
			rElementUI.update();
		}
	}

	@Override
	protected void addComponents() {
		buildElementUIs();
	}

	/**
	 * Applies the UI properties of a data element to the UI component.
	 *
	 * @param rElementUI The data element UI to apply the properties to
	 */
	protected void applyElementProperties(DataElementUI<?> rElementUI) {
		Component rComponent = rElementUI.getElementComponent();

		if (rComponent != null) {
			DataElement<?> rElement = rElementUI.getDataElement();
			String sWidth = rElement.getProperty(HTML_WIDTH, null);
			String sHeight = rElement.getProperty(HTML_HEIGHT, null);

			if (sWidth != null) {
				rComponent.setWidth(sWidth);
			}

			if (sHeight != null) {
				rComponent.setHeight(sHeight);
			}
		}
	}

	/**
	 * Applies the current selection value in the data element of this instance
	 * to it's container if it implements the {@link SingleSelection}
	 * interface.
	 */
	protected void applyElementSelection() {
		GenericLayout rLayout = getContainer().getLayout();

		if (rLayout instanceof SingleSelection) {
			SingleSelection rSelectable = (SingleSelection) rLayout;

			if (rDataElementList.hasProperty(CURRENT_SELECTION)) {
				rSelectable.setSelection(
					rDataElementList.getIntProperty(CURRENT_SELECTION, 0));
			}
		}
	}

	/**
	 * Builds the user interface for a data element in this container.
	 *
	 * @param rDataElementUI The element UI to build
	 * @param rStyle         The style for the data element UI
	 */
	protected void buildDataElementUI(DataElementUI<?> rDataElementUI,
		StyleData rStyle) {
		ContainerBuilder<?> rUiBuilder = this;

		if (rDataElementUI.getDataElement().hasFlag(SHOW_LABEL)) {
			rUiBuilder = addPanel(StyleData.DEFAULT, LayoutType.FLOW);

			String sLabel =
				rDataElementUI.createElementLabelString(getContext());

			if (sLabel.length() > 0) {
				rDataElementUI.createElementLabel(rUiBuilder, FORM_LABEL_STYLE,
					sLabel);
			}
		}

		rDataElementUI.buildUserInterface(rUiBuilder, rStyle);
		applyElementProperties(rDataElementUI);
	}

	/**
	 * Builds and initializes the UIs for the data elements in this panel.
	 * Invoked by {@link #addComponents()}.
	 */
	protected void buildElementUIs() {
		Map<DataElement<?>, StyleData> rDataElementStyles =
			prepareChildDataElements(rDataElementList);

		for (Entry<DataElement<?>, StyleData> rElementStyle :
			rDataElementStyles.entrySet()) {
			DataElement<?> rDataElement = rElementStyle.getKey();
			StyleData rStyle = rElementStyle.getValue();

			DataElementUI<?> aDataElementUI =
				DataElementUIFactory.create(this, rDataElement);

			if (!(rDataElement instanceof DataElementList)) {
				String sElementStyle = aDataElementUI.getElementStyleName();

				if (rDataElement.isImmutable()) {
					sElementStyle = CSS.readonly() + " " + sElementStyle;
				}

				rStyle = addStyles(rStyle, CSS.gfDataElement(), sElementStyle);
			}

			buildDataElementUI(aDataElementUI, rStyle);
			aDataElementUIs.put(rDataElement.getName(), aDataElementUI);
		}

		setupEventHandling();
	}

	/**
	 * When a {@link StateProperties#SELECTION_DEPENDENCY} property exists in
	 * the given data element the corresponding event handling is initialized
	 * for all concerned data elements.
	 *
	 * @param rRootManager The root manager to search for dependent elements
	 * @param rDataElement The data element to check for dependencies
	 */
	protected void checkSelectionDependency(
		DataElementPanelManager rRootManager, DataElement<?> rDataElement) {
		String sDependendElements =
			rDataElement.getProperty(SELECTION_DEPENDENCY, null);

		if (sDependendElements != null) {
			String[] sElementNames = sDependendElements.split(",");

			DataElementUI<?> rMainUI = getDataElementUI(rDataElement);

			SelectionDependencyHandler aSelectionHandler =
				new SelectionDependencyHandler(rMainUI);

			for (String sElement : sElementNames) {
				boolean bReverseState =
					sElement.startsWith(SELECTION_DEPENDENCY_REVERSE_PREFIX);

				if (bReverseState) {
					sElement = sElement.substring(1);
				}

				DataElement<?> rElement =
					rRootManager.findDataElement(sElement);

				if (rElement != null) {
					DataElementUI<?> rUI =
						rRootManager.getDataElementUI(rElement);

					if (rUI != null) {
						aSelectionHandler.addDependency(rUI, bReverseState);
					} else {
						assert false : "No UI for " + sElement;
					}
				} else {
					EWT.log(
						"Warning: No data element %s for selection dependency",
						sElement);
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ContainerBuilder<Container> createContainer(
		ContainerBuilder<?> rBuilder, StyleData rStyle) {
		ContainerBuilder<? extends Container> aPanelBuilder = null;

		aDataElementUIs =
			new LinkedHashMap<>(rDataElementList.getElementCount());

		rStyle = DataElementUI.applyElementStyle(rDataElementList, rStyle);

		eLayout = rDataElementList.getProperty(LAYOUT, LayoutType.TABS);

		aPanelBuilder = createPanel(rBuilder, rStyle, eLayout);

		return (ContainerBuilder<Container>) aPanelBuilder;
	}

	/**
	 * Must be implemented by subclasses to create the panel in which the data
	 * element user interfaces are placed.
	 *
	 * @param rBuilder   The builder to create the panel with
	 * @param rStyleData The style to create the panel with
	 * @param eLayout    The layout of the data element list
	 * @return A container builder instance for the new panel
	 */
	protected abstract ContainerBuilder<?> createPanel(
		ContainerBuilder<?> rBuilder, StyleData rStyleData,
		LayoutType eLayout);

	/**
	 * Returns the {@link DataElementUI} instances of this instance. The order
	 * in the returned map corresponds to the order in which the UIs are
	 * displayed.
	 *
	 * @return A ordered mapping from data element names to data element UIs
	 */
	protected final Map<String, DataElementUI<?>> getDataElementUIs() {
		return aDataElementUIs;
	}

	/**
	 * Returns the layout of this panel.
	 *
	 * @return The layout
	 */
	protected final LayoutType getLayout() {
		return eLayout;
	}

	/**
	 * Handles the occurrence of an interactive input event for a data element
	 * that is a child of this manager. Will be invoked by the event handler of
	 * the child's data element UI.
	 *
	 * @param rDataElement The data element in which the event occurred
	 * @param eEventType   bActionEvent TRUE for an action event, FALSE for a
	 *                     continuous (selection) event
	 */
	protected void handleInteractiveInput(DataElement<?> rDataElement,
		InteractionEventType eEventType) {
		if (!bHandlingSelectionEvent) {
			if (rInteractiveInputHandler != null) {
				rInteractiveInputHandler.handleInteractiveInput(rDataElement,
					eEventType);
			} else {
				PanelManager<?, ?> rParent = getParent();

				if (rParent instanceof DataElementPanelManager) {
					((DataElementPanelManager) rParent).handleInteractiveInput(
						rDataElement, eEventType);
				}
			}
		}
	}

	/**
	 * Prepares the child data elements that need to be displayed in this
	 * instance.
	 *
	 * @param rDataElementList The list of child data elements
	 * @return A mapping from child data elements to the corresponding styles
	 */
	protected Map<DataElement<?>, StyleData> prepareChildDataElements(
		DataElementList rDataElementList) {
		Map<DataElement<?>, StyleData> rDataElementStyles =
			new LinkedHashMap<>();

		for (DataElement<?> rDataElement : rDataElementList) {
			rDataElementStyles.put(rDataElement, StyleData.DEFAULT);
		}

		return rDataElementStyles;
	}

	/**
	 * Initializes the event handling for this instance.
	 */
	protected void setupEventHandling() {
		DataElementInteractionHandler<DataElementList> aEventHandler =
			new DataElementInteractionHandler<>(this, rDataElementList);

		if (aEventHandler.setupEventHandling(getContainer(), false)) {
			aInteractionHandler = aEventHandler;
		}
	}

	/**
	 * Updates the data element UIs of this instance.
	 *
	 * @param bUpdateUI TRUE to also update the UI, FALSE to only update the
	 *                  data element references
	 */
	protected void updateElementUIs(boolean bUpdateUI) {
		if (aInteractionHandler != null) {
			aInteractionHandler.updateDataElement(rDataElementList);
		}

		if (bUpdateUI) {
			getContainer().applyStyle(
				DataElementUI.applyElementStyle(rDataElementList,
					getBaseStyle()));
		}

		List<DataElement<?>> rOrderedElements = new ArrayList<>(
			prepareChildDataElements(rDataElementList).keySet());

		int nIndex = 0;

		for (DataElementUI<?> rUI : aDataElementUIs.values()) {
			DataElement<?> rNewElement = rOrderedElements.get(nIndex++);

			rUI.updateDataElement(rNewElement, bUpdateUI);
		}
	}

	/**
	 * Updates the style and selection from the data element list.
	 *
	 * @param bStyleChanged TRUE if the container style has changed
	 */
	protected void updateFromProperties(boolean bStyleChanged) {
		if (bStyleChanged) {
			updateContainerStyle();
		}

		applyElementSelection();
	}

	/**
	 * Checks whether the data element list of this instance has been modified
	 * and adds it to the given list if necessary.
	 *
	 * @param rModifiedElements The list of modified elements
	 */
	void checkIfDataElementListModified(
		List<DataElement<?>> rModifiedElements) {
		if (rDataElementList.isModified()) {
			rModifiedElements.add(rDataElementList.copy(CopyMode.PROPERTIES,
				DataElement.SERVER_PROPERTIES));
		}
	}

	/**
	 * Checks the given data elements for selection dependencies with other
	 * data
	 * elements and initializes the element UIs accordingly.
	 *
	 * @param rRootManager The root manager to search for dependent elements
	 * @param rElements    The data elements to check
	 */
	void checkSelectionDependencies(DataElementPanelManager rRootManager,
		Collection<DataElement<?>> rElements) {
		for (DataElement<?> rDataElement : rElements) {
			checkSelectionDependency(rRootManager, rDataElement);

			if (rDataElement instanceof DataElementList) {
				List<DataElement<?>> rChildElements =
					((DataElementList) rDataElement).getElements();

				checkSelectionDependencies(rRootManager, rChildElements);
			}
		}
	}

	/**
	 * Returns the indentation for the hierarchy level of this panel manager's
	 * data element.
	 *
	 * @return The hierarchy indent
	 */
	String getHierarchyChildIndent() {
		if (sChildIndent == null) {
			DataElementList rParent = rDataElementList.getParent();
			StringBuilder aIndent = new StringBuilder();

			while (rParent != null) {
				aIndent.append("| ");
				rParent = rParent.getParent();
			}

			aIndent.setLength(aIndent.length() - 1);
			sChildIndent = aIndent.toString();
		}

		return sChildIndent;
	}

	/**
	 * Returns the indentation for the hierarchy level of this panel manager's
	 * data element.
	 *
	 * @return The hierarchy indent
	 */
	String getHierarchyIndent() {
		PanelManager<?, ?> rParent = getParent();

		return rParent instanceof DataElementPanelManager ?
		       ((DataElementPanelManager) rParent).getHierarchyIndent() :
		       "";
	}

	/**
	 * Check if the container style needs to be updated for a new data element.
	 */
	private void updateContainerStyle() {
		String sElementStyle = rDataElementList.getProperty(STYLE, null);
		String sStyleName = getStyleName();

		if (sElementStyle != null && sStyleName.indexOf(sElementStyle) < 0) {
			sStyleName = sStyleName + " " + sElementStyle;
		}

		rDataElementList.setProperty(STYLE, sStyleName);

		StyleData rNewStyle =
			DataElementUI.applyElementStyle(rDataElementList, getBaseStyle());

		getContainer().applyStyle(rNewStyle);
	}

	/**
	 * An interface to handle interactive input events for data elements.
	 *
	 * @author eso
	 */
	public static interface InteractiveInputHandler {

		/**
		 * Handles the occurrence of an interactive input event for a certain
		 * data element.
		 *
		 * @param rDataElement The data element that caused the event
		 * @param eEventType   The interaction event that occurred
		 */
		void handleInteractiveInput(DataElement<?> rDataElement,
			InteractionEventType eEventType);
	}

	/**
	 * An inner class that handles selection events for components that are
	 * referenced by a {@link DataElement#SELECTION_DEPENDENCY} property. The
	 * dependency can either be the mutual exclusion of components that
	 * implement the {@link SingleSelection} interface of the enabling and
	 * disabling of components that are referenced by a button or another
	 * selectable component.
	 *
	 * @author eso
	 */
	private class SelectionDependencyHandler implements EwtEventHandler {

		private final DataElementUI<?> rMainUI;

		private Map<DataElementUI<?>, Boolean> rUIs =
			new LinkedHashMap<DataElementUI<?>, Boolean>(2);

		/**
		 * Creates a new instance.
		 *
		 * @param rMainUI The main data element user interface
		 */
		@SuppressWarnings("boxing")
		public SelectionDependencyHandler(DataElementUI<?> rMainUI) {
			this.rMainUI = rMainUI;

			Component rComponent = rMainUI.getElementComponent();

			rUIs.put(rMainUI, false);

			if (rComponent instanceof SingleSelection) {
				rComponent.addEventListener(EventType.SELECTION, this);
			} else {
				rComponent.addEventListener(EventType.ACTION, this);
			}
		}

		/**
		 * Adds a component to be handled by this instance.
		 *
		 * @param rUI           The dependent UI
		 * @param bReverseState TRUE to reverse the state of the dependent UI
		 */
		@SuppressWarnings("boxing")
		public void addDependency(DataElementUI<?> rUI,
			boolean bReverseState) {
			Component rMain = rMainUI.getElementComponent();
			Component rDependent = rUI.getElementComponent();

			boolean bIsSingleSelection = rDependent instanceof SingleSelection;
			boolean bIsMutualSelection = rMain instanceof SingleSelection;

			rUIs.put(rUI, bReverseState);

			if (bIsSingleSelection && bIsMutualSelection) {
				rDependent.addEventListener(EventType.SELECTION, this);
			} else {
				boolean bEnabled = false;

				if (rMain instanceof SelectableButton) {
					bEnabled = ((SelectableButton) rMain).isSelected();
				} else if (rMain instanceof SingleSelection) {
					bEnabled =
						((SingleSelection) rMain).getSelectionIndex() >= 0;
				}

				rUI.setEnabled(bReverseState ? !bEnabled : bEnabled);
			}
		}

		/**
		 * @see EwtEventHandler#handleEvent(EwtEvent)
		 */
		@Override
		@SuppressWarnings("boxing")
		public void handleEvent(EwtEvent rEvent) {
			// prevent re-invocation due to selection change in dependent UIs
			if (!bHandlingSelectionEvent) {
				bHandlingSelectionEvent = true;

				for (Entry<DataElementUI<?>, Boolean> rEntry :
					rUIs.entrySet()) {
					DataElementUI<?> rTargetUI = rEntry.getKey();
					boolean bReverseState = rEntry.getValue();

					Object rSourceComponent = rEvent.getSource();

					if (rTargetUI.getElementComponent() != rSourceComponent) {
						if (rEvent.getType() == EventType.ACTION) {
							handleActionEvent(rSourceComponent, rTargetUI,
								bReverseState);
						} else {
							handleSelectionEvent(rSourceComponent, rTargetUI,
								bReverseState);
						}
					}
				}

				bHandlingSelectionEvent = false;
			}
		}

		/**
		 * Performs the dependency changes caused by an action event.
		 *
		 * @param rSourceComponent The component that has been selected
		 * @param rTargetUI        The target data element UI
		 * @param bReverseState    TRUE to reverse the state of the source
		 *                         component in the target component
		 */
		private void handleActionEvent(Object rSourceComponent,
			DataElementUI<?> rTargetUI, boolean bReverseState) {
			if (rSourceComponent instanceof SelectableButton) {
				setEnabled(rTargetUI,
					((SelectableButton) rSourceComponent).isSelected(),
					bReverseState);
			} else {
				setEnabled(rTargetUI,
					!rTargetUI.getElementComponent().isEnabled(),
					bReverseState);
			}
		}

		/**
		 * Performs the dependency changes caused by a selection event.
		 *
		 * @param rSourceComponent The component that has been selected
		 * @param rTargetUI        The target data element UI
		 * @param bReverseState    TRUE to reverse the state of the source
		 *                         component in the target component
		 */
		private void handleSelectionEvent(Object rSourceComponent,
			DataElementUI<?> rTargetUI, boolean bReverseState) {
			Component rTargetComponent = rTargetUI.getElementComponent();

			if (rTargetComponent instanceof SingleSelection) {
				((SingleSelection) rTargetComponent).setSelection(-1);
			} else {
				int nSelection =
					((SingleSelection) rSourceComponent).getSelectionIndex();

				boolean bEnabled = nSelection >= 0;

				setEnabled(rTargetUI, bEnabled, bReverseState);
			}
		}

		/**
		 * Sets the enabled state of a dependent component.
		 *
		 * @param rDependentUI  The dependent component
		 * @param bEnabled      bState rSelectedComponent The selected
		 *                      component
		 * @param bReverseState TRUE to reverse the enabled state
		 */
		private void setEnabled(DataElementUI<?> rDependentUI,
			boolean bEnabled,
			boolean bReverseState) {
			rDependentUI.setEnabled(bReverseState ? !bEnabled : bEnabled);
		}
	}
}
