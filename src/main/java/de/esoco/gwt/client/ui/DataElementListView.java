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

import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.View;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.ViewStyle;

import de.esoco.gwt.client.res.EsocoGwtResources;

import de.esoco.lib.property.Alignment;
import de.esoco.lib.property.InteractionEventType;
import de.esoco.lib.property.StandardProperties;
import de.esoco.lib.property.ViewDisplayType;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static de.esoco.lib.property.LayoutProperties.VERTICAL_ALIGN;
import static de.esoco.lib.property.LayoutProperties.VIEW_DISPLAY_TYPE;
import static de.esoco.lib.property.StyleProperties.AUTO_HIDE;

/**
 * A class that handles the display of a data element list in a separate GEWT
 * view.
 *
 * @author eso
 */
public class DataElementListView {

	private static Set<ViewStyle.Flag> aDefaultViewFlags =
		EnumSet.noneOf(ViewStyle.Flag.class);

	private DataElementListUI aViewUI;

	private View aView;

	/**
	 * Creates a new instance.
	 *
	 * @param rParent      The parent data element panel manager
	 * @param rViewElement The data element list to be displayed in a view
	 */
	public DataElementListView(DataElementPanelManager rParent,
		DataElementList rViewElement) {
		aViewUI = (DataElementListUI) DataElementUIFactory.create(rParent,
			rViewElement);
	}

	/**
	 * Sets the default view style flags for new views.
	 *
	 * @param rDefaultViewFlags The default view style flags
	 */
	public static final void setDefaultViewFlags(
		Set<ViewStyle.Flag> rDefaultViewFlags) {
		aDefaultViewFlags = EnumSet.copyOf(rDefaultViewFlags);
	}

	/**
	 * Collects the modified data element UIs that received user input.
	 *
	 * @param rModifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> rModifiedElements) {
		aViewUI.collectInput(rModifiedElements);
	}

	/**
	 * Invokes {@link DataElementListUI#enableInteraction(boolean)}.
	 *
	 * @param bEnable TRUE to enable interactions, FALSE to disable
	 */
	public void enableInteraction(boolean bEnable) {
		aViewUI.enableInteraction(bEnable);
	}

	/**
	 * Returns the data element UI of this view.
	 *
	 * @return The view's data elementUI
	 */
	public final DataElementListUI getViewUI() {
		return aViewUI;
	}

	/**
	 * Hides this view.
	 */
	public void hide() {
		aView.setVisible(false);
	}

	/**
	 * Returns the visibility of this view.
	 *
	 * @return TRUE if the view is visible, FALSE if it hidden
	 */
	public boolean isVisible() {
		return aView.isVisible();
	}

	/**
	 * Shows this view.
	 */
	public void show() {
		ViewDisplayType eViewType = aViewUI
			.getDataElement()
			.getProperty(VIEW_DISPLAY_TYPE, ViewDisplayType.MODAL_DIALOG);

		ContainerBuilder<View> rBuilder =
			createView(aViewUI.getParent().getContainer().getView(),
				eViewType);

		StyleData rStyle = PanelManager.addStyles(StyleData.DEFAULT,
			aViewUI.getElementStyleName());

		aViewUI.buildUserInterface(rBuilder, rStyle);

		DataElementPanelManager rViewManager = aViewUI.getPanelManager();
		Collection<DataElement<?>> rElements =
			Arrays.asList(rViewManager.getDataElementList());

		rViewManager.checkSelectionDependencies(rViewManager, rElements);

		aView = rBuilder.getContainer();

		aView.pack();
		aView.getContext().displayViewCentered(aView);
	}

	/**
	 * Updates the data element of this view.
	 *
	 * @see DataElementUI#updateDataElement(DataElement, boolean)
	 */
	public void updateDataElement(DataElementList rNewElement,
		boolean bUpdateUI) {
		aViewUI.clearError();
		aViewUI.updateDataElement(rNewElement, bUpdateUI);
	}

	/**
	 * Creates a view of a certain type to display the list element UIs.
	 *
	 * @param rParentView The parent view
	 * @param eViewType   The view type
	 */
	private ContainerBuilder<View> createView(View rParentView,
		ViewDisplayType eViewType) {
		DataElementList rDataElementList = aViewUI.getDataElement();
		UserInterfaceContext rContext = rParentView.getContext();
		ViewStyle rViewStyle = ViewStyle.DEFAULT;
		ContainerBuilder<View> aViewBuilder = null;
		View aPanelView = null;

		Set<ViewStyle.Flag> aViewFlags = EnumSet.copyOf(aDefaultViewFlags);

		String sDialogStyle =
			EsocoGwtResources.INSTANCE.css().gfDataElementListDialog();

		if (eViewType == ViewDisplayType.MODAL_VIEW ||
			eViewType == ViewDisplayType.MODAL_DIALOG) {
			rViewStyle = ViewStyle.MODAL;
		}

		if (rDataElementList.hasProperty(VERTICAL_ALIGN)) {
			Alignment eAlignment =
				rDataElementList.getProperty(VERTICAL_ALIGN, null);

			if (eAlignment == Alignment.END) {
				aViewFlags.add(ViewStyle.Flag.BOTTOM);
			} else {
				aViewFlags.remove(ViewStyle.Flag.BOTTOM);
			}

			if (eAlignment == Alignment.FILL) {
				aViewFlags.add(ViewStyle.Flag.FULL_SIZE);
			} else {
				aViewFlags.remove(ViewStyle.Flag.FULL_SIZE);
			}
		}

		if (rDataElementList.hasFlag(AUTO_HIDE)) {
			aViewFlags.add(ViewStyle.Flag.AUTO_HIDE);
		} else {
			aViewFlags.remove(ViewStyle.Flag.AUTO_HIDE);
		}

		if (!aViewFlags.isEmpty()) {
			rViewStyle = rViewStyle.withFlags(aViewFlags);
		}

		switch (eViewType) {
			case DIALOG:
			case MODAL_DIALOG:
				aPanelView = rContext.createDialog(rParentView, rViewStyle);

				break;

			case VIEW:
			case MODAL_VIEW:
				aPanelView = rContext.createChildView(rParentView, rViewStyle);
				break;
		}

		String sViewTitle = "$ti" + rDataElementList.getResourceId();

		sViewTitle =
			rDataElementList.getProperty(StandardProperties.TITLE, sViewTitle);

		aPanelView.addEventListener(EventType.VIEW_CLOSING,
			this::handleViewClosing);

		aViewBuilder = new ContainerBuilder<View>(aPanelView);

		aPanelView.setTitle(sViewTitle);
		aPanelView.applyStyle(
			StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES,
				sDialogStyle));

		return aViewBuilder;
	}

	/**
	 * Handles the view closing event.
	 *
	 * @param rEvent The event
	 */
	private void handleViewClosing(EwtEvent rEvent) {
		aViewUI
			.getPanelManager()
			.handleInteractiveInput(aViewUI.getDataElement(),
				InteractionEventType.UPDATE);
	}
}
