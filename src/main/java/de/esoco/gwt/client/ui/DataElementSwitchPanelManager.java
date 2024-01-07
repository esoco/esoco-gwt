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
package de.esoco.gwt.client.ui;

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.component.SwitchPanel;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.LayoutType;

import java.util.Iterator;
import java.util.List;

import static de.esoco.lib.property.StateProperties.CURRENT_SELECTION;

/**
 * A panel manager for {@link DataElementList} instances that renders the child
 * elements of the list in distinct visual groups (e.g. Tabs) that can be
 * switched between.
 *
 * @author eso
 */
public class DataElementSwitchPanelManager extends DataElementPanelManager
	implements EwtEventHandler {

	private SwitchPanel switchPanel;

	private String labelPrefix;

	/**
	 * @see DataElementPanelManager#DataElementPanelManager(PanelManager,
	 * DataElementList)
	 */
	public DataElementSwitchPanelManager(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		super(parent, dataElementList);
	}

	/**
	 * Adds an event listener for page selection events. The listener will be
	 * notified of an {@link EventType#SELECTION} event if a new layout page is
	 * selected. The source of the event will be the panel used by this panel
	 * manager, not the manager itself. Not all switching panels may support
	 * listener registrations.
	 *
	 * @param listener The listener to notify if a new page is selected
	 */
	public void addPageSelectionListener(EwtEventHandler listener) {
		switchPanel.addEventListener(EventType.SELECTION, listener);
	}

	/**
	 * Invokes {@link DataElementPanelManager#collectInput(List)} on the
	 * currently selected page's panel manager.
	 *
	 * @see DataElementPanelManager#collectInput(List)
	 */
	@Override
	public void collectInput(List<DataElement<?>> modifiedElements) {
		DataElementList dataElementList = getDataElementList();
		int selection = getSelectedElement();

		if (selection <= 0) {
			dataElementList.removeProperty(CURRENT_SELECTION);
		} else {
			dataElementList.setProperty(CURRENT_SELECTION, selection);
		}

		checkIfDataElementListModified(modifiedElements);

		if (selection >= 0) {
			DataElementUI<?> dataElementUI = getDataElementUI(selection);

			dataElementUI.collectInput(modifiedElements);
		}
	}

	@Override
	public void dispose() {
		// remove event listener to avoid event handling for non-existing UIs
		switchPanel.removeEventListener(EventType.SELECTION, this);

		super.dispose();
	}

	/**
	 * Returns the index of the currently selected page element.
	 *
	 * @return The selection index
	 */
	public int getSelectedElement() {
		return switchPanel.getSelectionIndex();
	}

	/**
	 * Handles the selection of a page.
	 *
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent event) {
		int selection = getSelectedElement();

		if (selection >= 0) {
			getDataElementUI(selection).update();
		}
	}

	/**
	 * Sets the currently selected page.
	 *
	 * @param element The index of the page to select
	 */
	public void setSelectedElement(int element) {
		switchPanel.setSelection(element);
	}

	@Override
	public void updateFromChildChanges() {
		super.updateFromChildChanges();

		updatePageTitles();
	}

	@Override
	public void updateUI() {
		@SuppressWarnings("boxing")
		int selection = getDataElementList().getProperty(CURRENT_SELECTION, 0);

		setSelectedElement(selection);

		if (selection >= 0) {
			getDataElementUI(selection).update();
		}

		updatePageTitles();
	}

	@Override
	protected void buildDataElementUI(DataElementUI<?> dataElementUI,
		StyleData style) {
		super.buildDataElementUI(dataElementUI, style);

		Component elementComponent = dataElementUI.getElementComponent();

		switchPanel.addPage(elementComponent,
			getPageTitle(dataElementUI.getDataElement()), false);
	}

	/**
	 * Initializes the event handling for this instance.
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void buildElementUIs() {
		super.buildElementUIs();

		if (!switchPanel.getComponents().isEmpty()) {
			setSelectedElement(
				getDataElementList().getProperty(CURRENT_SELECTION, 0));
		}

		switchPanel.addEventListener(EventType.SELECTION, this);
	}

	@Override
	protected ContainerBuilder<? extends Panel> createPanel(
		ContainerBuilder<?> builder, StyleData styleData,
		LayoutType displayMode) {
		ContainerBuilder<? extends SwitchPanel> panelBuilder;

		switch (displayMode) {
			case TABS:
				labelPrefix = "$tab";
				panelBuilder = builder.addTabPanel(styleData);
				break;

			case STACK:
				labelPrefix = "$grp";
				panelBuilder = builder.addStackPanel(styleData);
				break;

			case DECK:
				labelPrefix = null;
				panelBuilder = builder.addDeckPanel(styleData);
				break;

			default:
				throw new IllegalStateException(
					"Unsupported DataElementList mode " + displayMode);
		}

		switchPanel = panelBuilder.getContainer();

		return panelBuilder;
	}

	/**
	 * Returns a value from a collection at a certain position, relative to the
	 * iteration order. The first position is zero.
	 *
	 * @param index The position index
	 * @return The corresponding value
	 * @throws IndexOutOfBoundsException If the index is invalid for the
	 *                                   collection
	 */
	private DataElementUI<?> getDataElementUI(int index) {
		assert index >= 0 && index < getDataElementUIs().size();

		Iterator<DataElementUI<?>> uIs =
			getDataElementUIs().values().iterator();

		DataElementUI<?> result = null;

		while (index-- >= 0 && uIs.hasNext()) {
			result = uIs.next();
		}

		return result;
	}

	/**
	 * Returns the switch panel page title for a certain data element.
	 *
	 * @param dataElement The data element to create the title for
	 * @return The page title string
	 */
	private String getPageTitle(DataElement<?> dataElement) {
		return labelPrefix != null ?
		       DataElementUI.getLabelText(getContext(), dataElement,
			       labelPrefix) :
		       "";
	}

	/**
	 * Updates the page titles from the current state of the data elements.
	 */
	private void updatePageTitles() {
		int page = 0;

		for (DataElementUI<?> elementUI : getDataElementUIs().values()) {
			switchPanel.setPageTitle(page++,
				getPageTitle(elementUI.getDataElement()));
		}
	}
}
