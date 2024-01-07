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

	private DataElementPanelManager aListPanelManager;

	@Override
	public void clearError() {
		super.clearError();

		aListPanelManager.clearErrors();
	}

	@Override
	public void collectInput(List<DataElement<?>> rModifiedElements) {
		aListPanelManager.collectInput(rModifiedElements);
	}

	/**
	 * Overridden to return the base type from the panel manager instead.
	 *
	 * @see DataElementUI#getBaseStyle()
	 */
	@Override
	public StyleData getBaseStyle() {
		return aListPanelManager.getBaseStyle();
	}

	/**
	 * Returns the {@link DataElementTablePanelManager} that is used for the
	 * display of the {@link DataElementList} of this instance.
	 *
	 * @return The panel manager or NULL for none
	 */
	public final DataElementPanelManager getPanelManager() {
		return aListPanelManager;
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
		aListPanelManager.updateUI();
	}

	@Override
	public void updateDataElement(DataElement<?> rNewElement,
		boolean bUpdateUI) {
		if (rNewElement.hasFlag(STRUCTURE_CHANGED)) {
			// always use FALSE to not update UI before data element is
			// updated;
			// UI update will be done in the update() method
			super.updateDataElement(rNewElement, false);

			if (aListPanelManager != null) {
				aListPanelManager.update((DataElementList) rNewElement, false);
			}

			if (bUpdateUI) {
				update();
			}
		} else {
			DataElementList rDataElementList = getDataElement();
			String sOldStyle = rDataElementList.getProperty(STYLE, null);

			rDataElementList.clearProperties();
			rDataElementList.setProperties(rNewElement, true);

			boolean bStyleChanged = !Objects.equals(sOldStyle,
				rDataElementList.getProperty(STYLE, null));

			aListPanelManager.updateFromProperties(bStyleChanged);

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
	protected Component buildDataElementUI(ContainerBuilder<?> rBuilder,
		StyleData rStyle) {
		DataElementList rDataElementList = getDataElement();
		Container rListPanel = null;

		LayoutType eDisplayMode =
			rDataElementList.getProperty(LAYOUT, LayoutType.TABLE);

		aListPanelManager =
			DataElementPanelManager.newInstance(getParent(), rDataElementList);

		aListPanelManager.buildIn(rBuilder, rStyle);
		rListPanel = aListPanelManager.getPanel();

		if (eDisplayMode == LayoutType.TABLE) {
			// DataElementPanelManager performs event handling for other cases
			setupInteractionHandling(rListPanel, false);
		}

		return rListPanel;
	}

	/**
	 * @see DataElementUI#enableInteraction(boolean)
	 */
	@Override
	protected void enableInteraction(boolean bEnable) {
		aListPanelManager.enableInteraction(bEnable);
	}

	/**
	 * Implemented to close an open child view.
	 *
	 * @see DataElementUI#dispose()
	 */
	@Override
	void dispose() {
		aListPanelManager.dispose();

		super.dispose();
	}

	/**
	 * Updates the style of the panel.
	 */
	private void updateStyle() {
		DataElementList rDataElementList = getDataElement();
		String sAddStyle = aListPanelManager.getStyleName();

		StyleData rNewStyle = applyElementStyle(rDataElementList,
			PanelManager.addStyles(getBaseStyle(), sAddStyle));

		applyStyle();
		aListPanelManager.getPanel().applyStyle(rNewStyle);
	}
}
