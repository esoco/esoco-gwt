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

	private static Set<ViewStyle.Flag> defaultViewFlags =
		EnumSet.noneOf(ViewStyle.Flag.class);

	private DataElementListUI viewUI;

	private View view;

	/**
	 * Creates a new instance.
	 *
	 * @param parent      The parent data element panel manager
	 * @param viewElement The data element list to be displayed in a view
	 */
	public DataElementListView(DataElementPanelManager parent,
		DataElementList viewElement) {
		viewUI = (DataElementListUI) DataElementUIFactory.create(parent,
			viewElement);
	}

	/**
	 * Sets the default view style flags for new views.
	 *
	 * @param defaultViewFlags The default view style flags
	 */
	public static final void setDefaultViewFlags(
		Set<ViewStyle.Flag> defaultViewFlags) {
		defaultViewFlags = EnumSet.copyOf(defaultViewFlags);
	}

	/**
	 * Collects the modified data element UIs that received user input.
	 *
	 * @param modifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> modifiedElements) {
		viewUI.collectInput(modifiedElements);
	}

	/**
	 * Invokes {@link DataElementListUI#enableInteraction(boolean)}.
	 *
	 * @param enable TRUE to enable interactions, FALSE to disable
	 */
	public void enableInteraction(boolean enable) {
		viewUI.enableInteraction(enable);
	}

	/**
	 * Returns the data element UI of this view.
	 *
	 * @return The view's data elementUI
	 */
	public final DataElementListUI getViewUI() {
		return viewUI;
	}

	/**
	 * Hides this view.
	 */
	public void hide() {
		view.setVisible(false);
	}

	/**
	 * Returns the visibility of this view.
	 *
	 * @return TRUE if the view is visible, FALSE if it hidden
	 */
	public boolean isVisible() {
		return view.isVisible();
	}

	/**
	 * Shows this view.
	 */
	public void show() {
		ViewDisplayType viewType = viewUI
			.getDataElement()
			.getProperty(VIEW_DISPLAY_TYPE, ViewDisplayType.MODAL_DIALOG);

		ContainerBuilder<View> builder =
			createView(viewUI.getParent().getContainer().getView(), viewType);

		StyleData style = PanelManager.addStyles(StyleData.DEFAULT,
			viewUI.getElementStyleName());

		viewUI.buildUserInterface(builder, style);

		DataElementPanelManager viewManager = viewUI.getPanelManager();
		Collection<DataElement<?>> elements =
			Arrays.asList(viewManager.getDataElementList());

		viewManager.checkSelectionDependencies(viewManager, elements);

		view = builder.getContainer();

		view.pack();
		view.getContext().displayViewCentered(view);
	}

	/**
	 * Updates the data element of this view.
	 *
	 * @see DataElementUI#updateDataElement(DataElement, boolean)
	 */
	public void updateDataElement(DataElementList newElement,
		boolean updateUI) {
		viewUI.clearError();
		viewUI.updateDataElement(newElement, updateUI);
	}

	/**
	 * Creates a view of a certain type to display the list element UIs.
	 *
	 * @param parentView The parent view
	 * @param viewType   The view type
	 */
	private ContainerBuilder<View> createView(View parentView,
		ViewDisplayType viewType) {
		DataElementList dataElementList = viewUI.getDataElement();
		UserInterfaceContext context = parentView.getContext();
		ViewStyle viewStyle = ViewStyle.DEFAULT;
		ContainerBuilder<View> viewBuilder = null;
		View panelView = null;

		Set<ViewStyle.Flag> viewFlags = EnumSet.copyOf(defaultViewFlags);

		String dialogStyle =
			EsocoGwtResources.INSTANCE.css().gfDataElementListDialog();

		if (viewType == ViewDisplayType.MODAL_VIEW ||
			viewType == ViewDisplayType.MODAL_DIALOG) {
			viewStyle = ViewStyle.MODAL;
		}

		if (dataElementList.hasProperty(VERTICAL_ALIGN)) {
			Alignment alignment =
				dataElementList.getProperty(VERTICAL_ALIGN, null);

			if (alignment == Alignment.END) {
				viewFlags.add(ViewStyle.Flag.BOTTOM);
			} else {
				viewFlags.remove(ViewStyle.Flag.BOTTOM);
			}

			if (alignment == Alignment.FILL) {
				viewFlags.add(ViewStyle.Flag.FULL_SIZE);
			} else {
				viewFlags.remove(ViewStyle.Flag.FULL_SIZE);
			}
		}

		if (dataElementList.hasFlag(AUTO_HIDE)) {
			viewFlags.add(ViewStyle.Flag.AUTO_HIDE);
		} else {
			viewFlags.remove(ViewStyle.Flag.AUTO_HIDE);
		}

		if (!viewFlags.isEmpty()) {
			viewStyle = viewStyle.withFlags(viewFlags);
		}

		switch (viewType) {
			case DIALOG:
			case MODAL_DIALOG:
				panelView = context.createDialog(parentView, viewStyle);

				break;

			case VIEW:
			case MODAL_VIEW:
				panelView = context.createChildView(parentView, viewStyle);
				break;
		}

		String viewTitle = "$ti" + dataElementList.getResourceId();

		viewTitle =
			dataElementList.getProperty(StandardProperties.TITLE, viewTitle);

		panelView.addEventListener(EventType.VIEW_CLOSING,
			this::handleViewClosing);

		viewBuilder = new ContainerBuilder<View>(panelView);

		panelView.setTitle(viewTitle);
		panelView.applyStyle(
			StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES,
				dialogStyle));

		return viewBuilder;
	}

	/**
	 * Handles the view closing event.
	 *
	 * @param event The event
	 */
	private void handleViewClosing(EwtEvent event) {
		viewUI
			.getPanelManager()
			.handleInteractiveInput(viewUI.getDataElement(),
				InteractionEventType.UPDATE);
	}
}
