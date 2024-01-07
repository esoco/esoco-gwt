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
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.LayoutType;

import java.util.List;
import java.util.Objects;

import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.StateProperties.STRUCTURE_CHANGED;
import static de.esoco.lib.property.StyleProperties.STYLE;

/**
 * A data element user interface that manages a {@link DataElementList} data
 * element in a child panel manager.
 *
 * @author eso
 */
public class DataElementListUI extends DataElementUI<DataElementList> {

	private DataElementPanelManager listPanelManager;

	@Override
	public void clearError() {
		super.clearError();

		listPanelManager.clearErrors();
	}

	@Override
	public void collectInput(List<DataElement<?>> modifiedElements) {
		listPanelManager.collectInput(modifiedElements);
	}

	/**
	 * Overridden to return the base type from the panel manager instead.
	 *
	 * @see DataElementUI#getBaseStyle()
	 */
	@Override
	public StyleData getBaseStyle() {
		return listPanelManager.getBaseStyle();
	}

	/**
	 * Returns the {@link DataElementTablePanelManager} that is used for the
	 * display of the {@link DataElementList} of this instance.
	 *
	 * @return The panel manager or NULL for none
	 */
	public final DataElementPanelManager getPanelManager() {
		return listPanelManager;
	}

	/**
	 * Updates the child panel manager with the current style and properties
	 * and
	 * the data element UIs of all children.
	 *
	 * @see DataElementUI#update()
	 */
	@Override
	public void update() {
		updateStyle();
		listPanelManager.updateUI();
	}

	@Override
	public void updateDataElement(DataElement<?> newElement,
		boolean updateUI) {
		if (newElement.hasFlag(STRUCTURE_CHANGED)) {
			// always use FALSE to not update UI before data element is
			// updated;
			// UI update will be done in the update() method
			super.updateDataElement(newElement, false);

			if (listPanelManager != null) {
				listPanelManager.update((DataElementList) newElement, false);
			}

			if (updateUI) {
				update();
			}
		} else {
			DataElementList dataElementList = getDataElement();
			String oldStyle = dataElementList.getProperty(STYLE, null);

			dataElementList.clearProperties();
			dataElementList.setProperties(newElement, true);

			boolean styleChanged = !Objects.equals(oldStyle,
				dataElementList.getProperty(STYLE, null));

			listPanelManager.updateFromProperties(styleChanged);

			updateStyle();
		}
	}

	/**
	 * Overridden to create a child {@link DataElementTablePanelManager} for
	 * the
	 * data element list.
	 *
	 * @see DataElementUI#buildDataElementUI(ContainerBuilder, StyleData)
	 */
	@Override
	protected Component buildDataElementUI(ContainerBuilder<?> builder,
		StyleData style) {
		DataElementList dataElementList = getDataElement();
		Container listPanel = null;

		LayoutType displayMode =
			dataElementList.getProperty(LAYOUT, LayoutType.TABLE);

		listPanelManager =
			DataElementPanelManager.newInstance(getParent(), dataElementList);

		listPanelManager.buildIn(builder, style);
		listPanel = listPanelManager.getPanel();

		if (displayMode == LayoutType.TABLE) {
			// DataElementPanelManager performs event handling for other cases
			setupInteractionHandling(listPanel, false);
		}

		return listPanel;
	}

	/**
	 * @see DataElementUI#enableInteraction(boolean)
	 */
	@Override
	protected void enableInteraction(boolean enable) {
		listPanelManager.enableInteraction(enable);
	}

	/**
	 * Implemented to close an open child view.
	 *
	 * @see DataElementUI#dispose()
	 */
	@Override
	void dispose() {
		listPanelManager.dispose();

		super.dispose();
	}

	/**
	 * Updates the style of the panel.
	 */
	private void updateStyle() {
		DataElementList dataElementList = getDataElement();
		String addStyle = listPanelManager.getStyleName();

		StyleData newStyle = applyElementStyle(dataElementList,
			PanelManager.addStyles(getBaseStyle(), addStyle));

		applyStyle();
		listPanelManager.getPanel().applyStyle(newStyle);
	}
}
