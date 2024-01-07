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
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
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
import java.util.Collection;
import java.util.Collections;
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

	private DataElementList dataElementList;

	private LayoutType layout;

	private Map<String, DataElementUI<?>> dataElementUIs;

	private InteractiveInputHandler interactiveInputHandler = null;

	private boolean handlingSelectionEvent = false;

	private DataElementInteractionHandler<DataElementList> interactionHandler;

	private String childIndent;

	/**
	 * Creates a new instance.
	 *
	 * @param parent          The parent panel manager
	 * @param dataElementList The data elements to display grouped
	 */
	protected DataElementPanelManager(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		super(parent, createPanelStyle(dataElementList));

		this.dataElementList = dataElementList;
	}

	/**
	 * Checks whether two lists of data elements contains the same data
	 * elements. The elements must be the same regargind their name and order.
	 *
	 * @param list1 The first list of data elements to compare
	 * @param list2 The second list of data elements to compare
	 * @return TRUE if the argument lists differ at least in one data element
	 */
	public static boolean containsSameElements(List<DataElement<?>> list1,
		List<DataElement<?>> list2) {
		int count = list1.size();
		boolean hasSameElements = (list2.size() == count);

		if (hasSameElements) {
			for (int i = 0; i < count; i++) {
				hasSameElements =
					list1.get(i).getName().equals(list2.get(i).getName());

				if (!hasSameElements) {
					break;
				}
			}
		}

		return hasSameElements;
	}

	/**
	 * Static helper method to create the panel manager's style name.
	 *
	 * @param dataElementList The data element list to create the style name
	 *                        for
	 * @return The panel style
	 */
	protected static String createPanelStyle(DataElementList dataElementList) {
		StringBuilder style = new StringBuilder(CSS.gfDataElementPanel());

		{
			LayoutType displayMode =
				dataElementList.getProperty(LAYOUT, LayoutType.TABLE);

			style.append(" gf-DataElement");
			style.append(TextConvert.capitalizedIdentifier(displayMode.name()));
			style.append("Panel");
		}

		style.append(' ');
		style.append(dataElementList.getResourceId());

		return style.toString();
	}

	/**
	 * Factory method that creates a new subclass instance based on the
	 * {@link LayoutType} of the data element list argument.
	 *
	 * @param parent          The parent panel manager
	 * @param dataElementList The list of data elements to be handled by the
	 *                          new
	 *                        panel manager
	 * @return A new data element panel manager instance
	 */
	public static DataElementPanelManager newInstance(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		DataElementPanelManager panelManager = null;

		LayoutType layout =
			dataElementList.getProperty(LAYOUT, LayoutType.TABLE);

		if (layout == LayoutType.TABLE) {
			panelManager =
				new DataElementTablePanelManager(parent, dataElementList);
		} else if (layout == LayoutType.INLINE) {
			panelManager =
				new DataElementInlinePanelManager(parent, dataElementList);
		} else if (SWITCH_LAYOUTS.contains(layout)) {
			panelManager =
				new DataElementSwitchPanelManager(parent, dataElementList);
		} else if (ORDERED_LAYOUTS.contains(layout)) {
			panelManager =
				new DataElementOrderedPanelManager(parent, dataElementList);
		} else if (GRID_LAYOUTS.contains(layout)) {
			panelManager =
				new DataElementGridPanelManager(parent, dataElementList);
		} else {
			panelManager =
				new DataElementLayoutPanelManager(parent, dataElementList);
		}

		return panelManager;
	}

	/**
	 * Registers an event listener for events on all data element components
	 * that are displayed in this panel. The listener will be added by means of
	 * {@link DataElementUI#addEventListener(EventType, EwtEventHandler)}.
	 *
	 * @param eventType The event type the listener shall be registered for
	 * @param listener  The event listener to be notified of events
	 */
	public void addElementEventListener(EventType eventType,
		EwtEventHandler listener) {
		for (DataElementUI<?> dataElementUI : dataElementUIs.values()) {
			dataElementUI.addEventListener(eventType, listener);
		}
	}

	/**
	 * Registers an event listener for events of a certain data element
	 * component that is displayed in this panel. The listener will be added
	 * with {@link DataElementUI#addEventListener(EventType, EwtEventHandler)}.
	 *
	 * @param dataElement The data element to register the event listener for
	 * @param eventType   The event type the listener shall be registered for
	 * @param listener    The event listener to be notified of events
	 */
	public void addElementEventListener(DataElement<?> dataElement,
		EventType eventType, EwtEventHandler listener) {
		DataElementUI<?> dataElementUI =
			dataElementUIs.get(dataElement.getName());

		if (dataElementUI != null) {
			dataElementUI.addEventListener(eventType, listener);
		} else {
			throw new IllegalArgumentException(
				"Unknown data element: " + dataElement);
		}
	}

	/**
	 * Hierarchically checks all data element UIs for dependencies in the root
	 * data element panel manager.
	 *
	 * @see PanelManager#buildIn(ContainerBuilder, StyleData)
	 */
	@Override
	public void buildIn(ContainerBuilder<?> builder, StyleData style) {
		super.buildIn(builder, style);

		// only check selection dependencies from the root after all child data
		// element UIs have been initialized
		if (!(getParent() instanceof DataElementPanelManager)) {
			checkSelectionDependencies(this,
				Collections.singletonList(dataElementList));
		}
	}

	/**
	 * Clears all error indicators in the contained data element UIs.
	 */
	public void clearErrors() {
		for (DataElementUI<?> uI : dataElementUIs.values()) {
			uI.clearError();
		}
	}

	/**
	 * Collects the values from the input components into the corresponding
	 * data
	 * elements.
	 *
	 * @param modifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> modifiedElements) {
		checkIfDataElementListModified(modifiedElements);

		for (DataElementUI<?> uI : dataElementUIs.values()) {
			if (uI != null) {
				uI.collectInput(modifiedElements);
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
		for (DataElementUI<?> uI : dataElementUIs.values()) {
			uI.dispose();
		}

		dataElementUIs.clear();
	}

	/**
	 * Enables or disables interactions through this panel manager's user
	 * interface.
	 *
	 * @param enable TRUE to enable interactions, FALSE to disable them
	 */
	public void enableInteraction(boolean enable) {
		for (DataElementUI<?> uI : dataElementUIs.values()) {
			uI.enableInteraction(enable);
		}
	}

	/**
	 * Searches for a data element with a certain name in this manager's
	 * hierarchy.
	 *
	 * @param name The name of the data element to search
	 * @return The matching data element or NULL if no such element exists
	 */
	public DataElement<?> findDataElement(String name) {
		return dataElementList.findChild(name);
	}

	/**
	 * Searches for the UI of a data element with a certain name in this
	 * manager's hierarchy.
	 *
	 * @param elementName The name of the data element to search the UI for
	 * @return The matching data element UI or NULL if no such element exists
	 */
	public DataElementUI<?> findDataElementUI(String elementName) {
		DataElementUI<?> elementUI = dataElementUIs.get(elementName);

		if (elementUI == null) {
			for (DataElementUI<?> uI : dataElementUIs.values()) {
				if (uI instanceof DataElementListUI) {
					DataElementPanelManager panelManager =
						((DataElementListUI) uI).getPanelManager();

					elementUI = panelManager.findDataElementUI(elementName);

					if (elementUI != null) {
						break;
					}
				}
			}
		}

		return elementUI;
	}

	/**
	 * Returns a certain element in this manager's list of data elements.
	 *
	 * @param index The index of the element to return
	 * @return The data element
	 */
	public final DataElement<?> getDataElement(int index) {
		return dataElementList.getElement(index);
	}

	/**
	 * Returns the dataElementList value.
	 *
	 * @return The dataElementList value
	 */
	public final DataElementList getDataElementList() {
		return dataElementList;
	}

	/**
	 * Returns the UI for a certain data element from the hierarchy of this
	 * instance. Implementations must search child panel manager too if they
	 * don't contain the data element themselves.
	 *
	 * @param dataElement The data element to return the UI for
	 * @return The data element UI for the given element or NULL if not found
	 */
	public final DataElementUI<?> getDataElementUI(DataElement<?> dataElement) {
		DataElementUI<?> dataElementUI =
			dataElementUIs.get(dataElement.getName());

		if (dataElementUI == null) {
			for (DataElementUI<?> uI : dataElementUIs.values()) {
				if (uI instanceof DataElementListUI) {
					dataElementUI = ((DataElementListUI) uI)
						.getPanelManager()
						.getDataElementUI(dataElement);
				}

				if (dataElementUI != null) {
					break;
				}
			}
		}

		return dataElementUI;
	}

	/**
	 * Returns the root panel manager of this instance's hierarchy. If this
	 * instance is already the root of the hierarchy (i.e. it has no parent) it
	 * will be returned directly.
	 *
	 * @return The root panel manager
	 */
	public DataElementPanelManager getRoot() {
		PanelManager<?, ?> parent = getParent();

		return parent instanceof DataElementPanelManager ?
		       ((DataElementPanelManager) parent).getRoot() :
		       this;
	}

	/**
	 * Sets the visibility of a data element. The default implementation does
	 * nothing but subclasses can override this method if the need to modify
	 * their state on visibility changes.
	 *
	 * @param elementUI The UI of the data element
	 * @param visible   The visibility of the data element
	 */
	public void setElementVisibility(DataElementUI<?> elementUI,
		boolean visible) {
	}

	/**
	 * Sets the handler of interactive input events for data elements.
	 *
	 * @param handler The interactive input handler
	 */
	public final void setInteractiveInputHandler(
		InteractiveInputHandler handler) {
		interactiveInputHandler = handler;
	}

	/**
	 * Updates this instance from a new data element list.
	 *
	 * @param newDataElementList The list containing the new data elements
	 * @param updateUI           TRUE to also update the UI, FALSE to only
	 *                           update the data element references
	 */
	public void update(DataElementList newDataElementList, boolean updateUI) {
		boolean isUpdate = !newDataElementList.hasFlag(STRUCTURE_CHANGED) &&
			newDataElementList.getName().equals(dataElementList.getName()) &&
			containsSameElements(newDataElementList.getElements(),
				dataElementList.getElements());

		boolean styleChanged =
			!Objects.equals(dataElementList.getProperty(STYLE, null),
				newDataElementList.getProperty(STYLE, null));

		dataElementList = newDataElementList;

		if (isUpdate) {
			updateElementUIs(updateUI);
		} else {
			dispose();
			rebuild();
		}

		updateFromProperties(styleChanged);
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
		for (DataElementUI<?> elementUI : dataElementUIs.values()) {
			elementUI.update();
		}
	}

	@Override
	protected void addComponents() {
		buildElementUIs();
	}

	/**
	 * Applies the UI properties of a data element to the UI component.
	 *
	 * @param elementUI The data element UI to apply the properties to
	 */
	protected void applyElementProperties(DataElementUI<?> elementUI) {
		Component component = elementUI.getElementComponent();

		if (component != null) {
			DataElement<?> element = elementUI.getDataElement();
			String width = element.getProperty(HTML_WIDTH, null);
			String height = element.getProperty(HTML_HEIGHT, null);

			if (width != null) {
				component.setWidth(width);
			}

			if (height != null) {
				component.setHeight(height);
			}
		}
	}

	/**
	 * Applies the current selection value in the data element of this instance
	 * to it's container if it implements the {@link SingleSelection}
	 * interface.
	 */
	protected void applyElementSelection() {
		GenericLayout layout = getContainer().getLayout();

		if (layout instanceof SingleSelection) {
			SingleSelection selectable = (SingleSelection) layout;

			if (dataElementList.hasProperty(CURRENT_SELECTION)) {
				selectable.setSelection(
					dataElementList.getIntProperty(CURRENT_SELECTION, 0));
			}
		}
	}

	/**
	 * Builds the user interface for a data element in this container.
	 *
	 * @param dataElementUI The element UI to build
	 * @param style         The style for the data element UI
	 */
	protected void buildDataElementUI(DataElementUI<?> dataElementUI,
		StyleData style) {
		ContainerBuilder<?> uiBuilder = this;

		if (dataElementUI.getDataElement().hasFlag(SHOW_LABEL)) {
			uiBuilder = addPanel(StyleData.DEFAULT, LayoutType.FLOW);

			String label =
				dataElementUI.createElementLabelString(getContext());

			if (label.length() > 0) {
				dataElementUI.createElementLabel(uiBuilder, FORM_LABEL_STYLE,
					label);
			}
		}

		dataElementUI.buildUserInterface(uiBuilder, style);
		applyElementProperties(dataElementUI);
	}

	/**
	 * Builds and initializes the UIs for the data elements in this panel.
	 * Invoked by {@link #addComponents()}.
	 */
	protected void buildElementUIs() {
		Map<DataElement<?>, StyleData> dataElementStyles =
			prepareChildDataElements(dataElementList);

		for (Entry<DataElement<?>, StyleData> elementStyle :
			dataElementStyles.entrySet()) {
			DataElement<?> dataElement = elementStyle.getKey();
			StyleData style = elementStyle.getValue();

			DataElementUI<?> dataElementUI =
				DataElementUIFactory.create(this, dataElement);

			if (!(dataElement instanceof DataElementList)) {
				String elementStyleName = dataElementUI.getElementStyleName();

				if (dataElement.isImmutable()) {
					elementStyleName = CSS.readonly() + " " + elementStyleName;
				}

				style = addStyles(style, CSS.gfDataElement(),
					elementStyleName);
			}

			buildDataElementUI(dataElementUI, style);
			dataElementUIs.put(dataElement.getName(), dataElementUI);
		}

		setupEventHandling();
	}

	/**
	 * When a {@link StateProperties#SELECTION_DEPENDENCY} property exists in
	 * the given data element the corresponding event handling is initialized
	 * for all concerned data elements.
	 *
	 * @param rootManager The root manager to search for dependent elements
	 * @param dataElement The data element to check for dependencies
	 */
	protected void checkSelectionDependency(DataElementPanelManager rootManager,
		DataElement<?> dataElement) {
		String dependendElements =
			dataElement.getProperty(SELECTION_DEPENDENCY, null);

		if (dependendElements != null) {
			String[] elementNames = dependendElements.split(",");

			DataElementUI<?> mainUI = getDataElementUI(dataElement);

			SelectionDependencyHandler selectionHandler =
				new SelectionDependencyHandler(mainUI);

			for (String elementName : elementNames) {
				boolean reverseState =
					elementName.startsWith(SELECTION_DEPENDENCY_REVERSE_PREFIX);

				if (reverseState) {
					elementName = elementName.substring(1);
				}

				DataElement<?> element =
					rootManager.findDataElement(elementName);

				if (element != null) {
					DataElementUI<?> uI =
						rootManager.getDataElementUI(element);

					if (uI != null) {
						selectionHandler.addDependency(uI, reverseState);
					} else {
						assert false : "No UI for " + element;
					}
				} else {
					EWT.log(
						"Warning: No data element %s for selection dependency",
						element);
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ContainerBuilder<Container> createContainer(
		ContainerBuilder<?> builder, StyleData style) {
		ContainerBuilder<? extends Container> panelBuilder = null;

		dataElementUIs =
			new LinkedHashMap<>(dataElementList.getElementCount());

		style = DataElementUI.applyElementStyle(dataElementList, style);

		layout = dataElementList.getProperty(LAYOUT, LayoutType.TABS);

		panelBuilder = createPanel(builder, style, layout);

		return (ContainerBuilder<Container>) panelBuilder;
	}

	/**
	 * Must be implemented by subclasses to create the panel in which the data
	 * element user interfaces are placed.
	 *
	 * @param builder   The builder to create the panel with
	 * @param styleData The style to create the panel with
	 * @param layout    The layout of the data element list
	 * @return A container builder instance for the new panel
	 */
	protected abstract ContainerBuilder<?> createPanel(
		ContainerBuilder<?> builder, StyleData styleData, LayoutType layout);

	/**
	 * Returns the {@link DataElementUI} instances of this instance. The order
	 * in the returned map corresponds to the order in which the UIs are
	 * displayed.
	 *
	 * @return A ordered mapping from data element names to data element UIs
	 */
	protected final Map<String, DataElementUI<?>> getDataElementUIs() {
		return dataElementUIs;
	}

	/**
	 * Returns the layout of this panel.
	 *
	 * @return The layout
	 */
	protected final LayoutType getLayout() {
		return layout;
	}

	/**
	 * Handles the occurrence of an interactive input event for a data element
	 * that is a child of this manager. Will be invoked by the event handler of
	 * the child's data element UI.
	 *
	 * @param dataElement The data element in which the event occurred
	 * @param eventType   actionEvent TRUE for an action event, FALSE for a
	 *                    continuous (selection) event
	 */
	protected void handleInteractiveInput(DataElement<?> dataElement,
		InteractionEventType eventType) {
		if (!handlingSelectionEvent) {
			if (interactiveInputHandler != null) {
				interactiveInputHandler.handleInteractiveInput(dataElement,
					eventType);
			} else {
				PanelManager<?, ?> parent = getParent();

				if (parent instanceof DataElementPanelManager) {
					((DataElementPanelManager) parent).handleInteractiveInput(
						dataElement, eventType);
				}
			}
		}
	}

	/**
	 * Prepares the child data elements that need to be displayed in this
	 * instance.
	 *
	 * @param dataElementList The list of child data elements
	 * @return A mapping from child data elements to the corresponding styles
	 */
	protected Map<DataElement<?>, StyleData> prepareChildDataElements(
		DataElementList dataElementList) {
		Map<DataElement<?>, StyleData> dataElementStyles =
			new LinkedHashMap<>();

		for (DataElement<?> dataElement : dataElementList) {
			dataElementStyles.put(dataElement, StyleData.DEFAULT);
		}

		return dataElementStyles;
	}

	/**
	 * Initializes the event handling for this instance.
	 */
	protected void setupEventHandling() {
		DataElementInteractionHandler<DataElementList> eventHandler =
			new DataElementInteractionHandler<>(this, dataElementList);

		if (eventHandler.setupEventHandling(getContainer(), false)) {
			interactionHandler = eventHandler;
		}
	}

	/**
	 * Updates the data element UIs of this instance.
	 *
	 * @param updateUI TRUE to also update the UI, FALSE to only update the
	 *                    data
	 *                 element references
	 */
	protected void updateElementUIs(boolean updateUI) {
		if (interactionHandler != null) {
			interactionHandler.updateDataElement(dataElementList);
		}

		if (updateUI) {
			getContainer().applyStyle(
				DataElementUI.applyElementStyle(dataElementList,
					getBaseStyle()));
		}

		List<DataElement<?>> orderedElements =
			new ArrayList<>(prepareChildDataElements(dataElementList).keySet());

		int index = 0;

		for (DataElementUI<?> uI : dataElementUIs.values()) {
			DataElement<?> newElement = orderedElements.get(index++);

			uI.updateDataElement(newElement, updateUI);
		}
	}

	/**
	 * Updates the style and selection from the data element list.
	 *
	 * @param styleChanged TRUE if the container style has changed
	 */
	protected void updateFromProperties(boolean styleChanged) {
		if (styleChanged) {
			updateContainerStyle();
		}

		applyElementSelection();
	}

	/**
	 * Checks whether the data element list of this instance has been modified
	 * and adds it to the given list if necessary.
	 *
	 * @param modifiedElements The list of modified elements
	 */
	void checkIfDataElementListModified(List<DataElement<?>> modifiedElements) {
		if (dataElementList.isModified()) {
			modifiedElements.add(dataElementList.copy(CopyMode.PROPERTIES,
				DataElement.SERVER_PROPERTIES));
		}
	}

	/**
	 * Checks the given data elements for selection dependencies with other
	 * data
	 * elements and initializes the element UIs accordingly.
	 *
	 * @param rootManager The root manager to search for dependent elements
	 * @param elements    The data elements to check
	 */
	void checkSelectionDependencies(DataElementPanelManager rootManager,
		Collection<DataElement<?>> elements) {
		for (DataElement<?> dataElement : elements) {
			checkSelectionDependency(rootManager, dataElement);

			if (dataElement instanceof DataElementList) {
				List<DataElement<?>> childElements =
					((DataElementList) dataElement).getElements();

				checkSelectionDependencies(rootManager, childElements);
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
		if (childIndent == null) {
			DataElementList parent = dataElementList.getParent();
			StringBuilder indent = new StringBuilder();

			while (parent != null) {
				indent.append("| ");
				parent = parent.getParent();
			}

			indent.setLength(indent.length() - 1);
			childIndent = indent.toString();
		}

		return childIndent;
	}

	/**
	 * Returns the indentation for the hierarchy level of this panel manager's
	 * data element.
	 *
	 * @return The hierarchy indent
	 */
	String getHierarchyIndent() {
		PanelManager<?, ?> parent = getParent();

		return parent instanceof DataElementPanelManager ?
		       ((DataElementPanelManager) parent).getHierarchyIndent() :
		       "";
	}

	/**
	 * Check if the container style needs to be updated for a new data element.
	 */
	private void updateContainerStyle() {
		String elementStyle = dataElementList.getProperty(STYLE, null);
		String styleName = getStyleName();

		if (elementStyle != null && styleName.indexOf(elementStyle) < 0) {
			styleName = styleName + " " + elementStyle;
		}

		dataElementList.setProperty(STYLE, styleName);

		StyleData newStyle =
			DataElementUI.applyElementStyle(dataElementList, getBaseStyle());

		getContainer().applyStyle(newStyle);
	}

	/**
	 * An interface to handle interactive input events for data elements.
	 *
	 * @author eso
	 */
	public interface InteractiveInputHandler {

		/**
		 * Handles the occurrence of an interactive input event for a certain
		 * data element.
		 *
		 * @param dataElement The data element that caused the event
		 * @param eventType   The interaction event that occurred
		 */
		void handleInteractiveInput(DataElement<?> dataElement,
			InteractionEventType eventType);
	}

	/**
	 * An inner class that handles selection events for components that are
	 * referenced by a {@link StateProperties#SELECTION_DEPENDENCY} property.
	 * The dependency can either be the mutual exclusion of components that
	 * implement the {@link SingleSelection} interface of the enabling and
	 * disabling of components that are referenced by a button or another
	 * selectable component.
	 *
	 * @author eso
	 */
	private class SelectionDependencyHandler implements EwtEventHandler {

		private final DataElementUI<?> mainUI;

		private final Map<DataElementUI<?>, Boolean> uIs =
			new LinkedHashMap<DataElementUI<?>, Boolean>(2);

		/**
		 * Creates a new instance.
		 *
		 * @param mainUI The main data element user interface
		 */
		@SuppressWarnings("boxing")
		public SelectionDependencyHandler(DataElementUI<?> mainUI) {
			this.mainUI = mainUI;

			Component component = mainUI.getElementComponent();

			uIs.put(mainUI, false);

			if (component instanceof SingleSelection) {
				component.addEventListener(EventType.SELECTION, this);
			} else {
				component.addEventListener(EventType.ACTION, this);
			}
		}

		/**
		 * Adds a component to be handled by this instance.
		 *
		 * @param uI           The dependent UI
		 * @param reverseState TRUE to reverse the state of the dependent UI
		 */
		@SuppressWarnings("boxing")
		public void addDependency(DataElementUI<?> uI, boolean reverseState) {
			Component main = mainUI.getElementComponent();
			Component dependent = uI.getElementComponent();

			boolean isSingleSelection = dependent instanceof SingleSelection;
			boolean isMutualSelection = main instanceof SingleSelection;

			uIs.put(uI, reverseState);

			if (isSingleSelection && isMutualSelection) {
				dependent.addEventListener(EventType.SELECTION, this);
			} else {
				boolean enabled = false;

				if (main instanceof SelectableButton) {
					enabled = ((SelectableButton) main).isSelected();
				} else if (main instanceof SingleSelection) {
					enabled =
						((SingleSelection) main).getSelectionIndex() >= 0;
				}

				uI.setEnabled(reverseState != enabled);
			}
		}

		/**
		 * @see EwtEventHandler#handleEvent(EwtEvent)
		 */
		@Override
		@SuppressWarnings("boxing")
		public void handleEvent(EwtEvent event) {
			// prevent re-invocation due to selection change in dependent UIs
			if (!handlingSelectionEvent) {
				handlingSelectionEvent = true;

				for (Entry<DataElementUI<?>, Boolean> entry : uIs.entrySet()) {
					DataElementUI<?> targetUI = entry.getKey();
					boolean reverseState = entry.getValue();

					Object sourceComponent = event.getSource();

					if (targetUI.getElementComponent() != sourceComponent) {
						if (event.getType() == EventType.ACTION) {
							handleActionEvent(sourceComponent, targetUI,
								reverseState);
						} else {
							handleSelectionEvent(sourceComponent, targetUI,
								reverseState);
						}
					}
				}

				handlingSelectionEvent = false;
			}
		}

		/**
		 * Performs the dependency changes caused by an action event.
		 *
		 * @param sourceComponent The component that has been selected
		 * @param targetUI        The target data element UI
		 * @param reverseState    TRUE to reverse the state of the source
		 *                        component in the target component
		 */
		private void handleActionEvent(Object sourceComponent,
			DataElementUI<?> targetUI, boolean reverseState) {
			if (sourceComponent instanceof SelectableButton) {
				setEnabled(targetUI,
					((SelectableButton) sourceComponent).isSelected(),
					reverseState);
			} else {
				setEnabled(targetUI,
					!targetUI.getElementComponent().isEnabled(), reverseState);
			}
		}

		/**
		 * Performs the dependency changes caused by a selection event.
		 *
		 * @param sourceComponent The component that has been selected
		 * @param targetUI        The target data element UI
		 * @param reverseState    TRUE to reverse the state of the source
		 *                        component in the target component
		 */
		private void handleSelectionEvent(Object sourceComponent,
			DataElementUI<?> targetUI, boolean reverseState) {
			Component targetComponent = targetUI.getElementComponent();

			if (targetComponent instanceof SingleSelection) {
				((SingleSelection) targetComponent).setSelection(-1);
			} else {
				int selection =
					((SingleSelection) sourceComponent).getSelectionIndex();

				boolean enabled = selection >= 0;

				setEnabled(targetUI, enabled, reverseState);
			}
		}

		/**
		 * Sets the enabled state of a dependent component.
		 *
		 * @param dependentUI  The dependent component
		 * @param enabled      state rSelectedComponent The selected component
		 * @param reverseState TRUE to reverse the enabled state
		 */
		private void setEnabled(DataElementUI<?> dependentUI, boolean enabled,
			boolean reverseState) {
			dependentUI.setEnabled(reverseState != enabled);
		}
	}
}
