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

	private SwitchPanel aSwitchPanel;

	private String sLabelPrefix;

	/**
	 * @see DataElementPanelManager#DataElementPanelManager(PanelManager,
	 * DataElementList)
	 */
	public DataElementSwitchPanelManager(PanelManager<?, ?> rParent,
		DataElementList rDataElementList) {
		super(rParent, rDataElementList);
	}

	/**
	 * Adds an event listener for page selection events. The listener will be
	 * notified of an {@link EventType#SELECTION} event if a new layout page is
	 * selected. The source of the event will be the panel used by this panel
	 * manager, not the manager itself. Not all switching panels may support
	 * listener registrations.
	 *
	 * @param rListener The listener to notify if a new page is selected
	 */
	public void addPageSelectionListener(EwtEventHandler rListener) {
		aSwitchPanel.addEventListener(EventType.SELECTION, rListener);
	}

	/**
	 * Invokes {@link DataElementPanelManager#collectInput(List)} on the
	 * currently selected page's panel manager.
	 *
	 * @see DataElementPanelManager#collectInput(List)
	 */
	@Override
	public void collectInput(List<DataElement<?>> rModifiedElements) {
		DataElementList rDataElementList = getDataElementList();
		int nSelection = getSelectedElement();

		if (nSelection <= 0) {
			rDataElementList.removeProperty(CURRENT_SELECTION);
		} else {
			rDataElementList.setProperty(CURRENT_SELECTION, nSelection);
		}

		checkIfDataElementListModified(rModifiedElements);

		if (nSelection >= 0) {
			DataElementUI<?> rDataElementUI = getDataElementUI(nSelection);

			rDataElementUI.collectInput(rModifiedElements);
		}
	}

	@Override
	public void dispose() {
		// remove event listener to avoid event handling for non-existing UIs
		aSwitchPanel.removeEventListener(EventType.SELECTION, this);

		super.dispose();
	}

	/**
	 * Returns the index of the currently selected page element.
	 *
	 * @return The selection index
	 */
	public int getSelectedElement() {
		return aSwitchPanel.getSelectionIndex();
	}

	/**
	 * Handles the selection of a page.
	 *
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent rEvent) {
		int nSelection = getSelectedElement();

		if (nSelection >= 0) {
			getDataElementUI(nSelection).update();
		}
	}

	/**
	 * Sets the currently selected page.
	 *
	 * @param nElement The index of the page to select
	 */
	public void setSelectedElement(int nElement) {
		aSwitchPanel.setSelection(nElement);
	}

	@Override
	public void updateFromChildChanges() {
		super.updateFromChildChanges();

		updatePageTitles();
	}

	@Override
	public void updateUI() {
		@SuppressWarnings("boxing")
		int nSelection = getDataElementList().getProperty(CURRENT_SELECTION,
			0);

		setSelectedElement(nSelection);

		if (nSelection >= 0) {
			getDataElementUI(nSelection).update();
		}

		updatePageTitles();
	}

	@Override
	protected void buildDataElementUI(DataElementUI<?> rDataElementUI,
		StyleData rStyle) {
		super.buildDataElementUI(rDataElementUI, rStyle);

		Component rElementComponent = rDataElementUI.getElementComponent();

		aSwitchPanel.addPage(rElementComponent,
			getPageTitle(rDataElementUI.getDataElement()), false);
	}

	/**
	 * Initializes the event handling for this instance.
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void buildElementUIs() {
		super.buildElementUIs();

		if (!aSwitchPanel.getComponents().isEmpty()) {
			setSelectedElement(
				getDataElementList().getProperty(CURRENT_SELECTION, 0));
		}

		aSwitchPanel.addEventListener(EventType.SELECTION, this);
	}

	@Override
	protected ContainerBuilder<? extends Panel> createPanel(
		ContainerBuilder<?> rBuilder, StyleData rStyleData,
		LayoutType eDisplayMode) {
		ContainerBuilder<? extends SwitchPanel> aPanelBuilder;

		switch (eDisplayMode) {
			case TABS:
				sLabelPrefix = "$tab";
				aPanelBuilder = rBuilder.addTabPanel(rStyleData);
				break;

			case STACK:
				sLabelPrefix = "$grp";
				aPanelBuilder = rBuilder.addStackPanel(rStyleData);
				break;

			case DECK:
				sLabelPrefix = null;
				aPanelBuilder = rBuilder.addDeckPanel(rStyleData);
				break;

			default:
				throw new IllegalStateException(
					"Unsupported DataElementList mode " + eDisplayMode);
		}

		aSwitchPanel = aPanelBuilder.getContainer();

		return aPanelBuilder;
	}

	/**
	 * Returns a value from a collection at a certain position, relative to the
	 * iteration order. The first position is zero.
	 *
	 * @param nIndex The position index
	 * @return The corresponding value
	 * @throws IndexOutOfBoundsException If the index is invalid for the
	 *                                   collection
	 */
	private DataElementUI<?> getDataElementUI(int nIndex) {
		assert nIndex >= 0 && nIndex < getDataElementUIs().size();

		Iterator<DataElementUI<?>> rUIs =
			getDataElementUIs().values().iterator();

		DataElementUI<?> rResult = null;

		while (nIndex-- >= 0 && rUIs.hasNext()) {
			rResult = rUIs.next();
		}

		return rResult;
	}

	/**
	 * Returns the switch panel page title for a certain data element.
	 *
	 * @param rDataElement The data element to create the title for
	 * @return The page title string
	 */
	private String getPageTitle(DataElement<?> rDataElement) {
		return sLabelPrefix != null ?
		       DataElementUI.getLabelText(getContext(), rDataElement,
			       sLabelPrefix) :
		       "";
	}

	/**
	 * Updates the page titles from the current state of the data elements.
	 */
	private void updatePageTitles() {
		int nPage = 0;

		for (DataElementUI<?> rElementUI : getDataElementUIs().values()) {
			aSwitchPanel.setPageTitle(nPage++,
				getPageTitle(rElementUI.getDataElement()));
		}
	}
}
