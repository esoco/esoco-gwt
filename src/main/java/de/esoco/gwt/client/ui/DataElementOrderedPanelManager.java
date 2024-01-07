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
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.layout.DockLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.Orientation;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.esoco.lib.property.LayoutProperties.HEIGHT;
import static de.esoco.lib.property.LayoutProperties.WIDTH;
import static de.esoco.lib.property.StyleProperties.ORIENTATION;

/**
 * A panel manager for {@link DataElementList} instances that places the child
 * data elements in a panel that displays the elements in a certain order or
 * sequence.
 *
 * @author eso
 */
public class DataElementOrderedPanelManager extends DataElementPanelManager {

	/**
	 * @see DataElementPanelManager#DataElementPanelManager(PanelManager,
	 * DataElementList)
	 */
	public DataElementOrderedPanelManager(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		super(parent, dataElementList);
	}

	@Override
	protected ContainerBuilder<? extends Panel> createPanel(
		ContainerBuilder<?> builder, StyleData styleData, LayoutType layout) {
		ContainerBuilder<? extends Panel> panelBuilder;

		switch (layout) {
			case DOCK:
				assert getDataElementList().getElementCount() <= 3 :
					"Element count for DOCK layout mode must be <= 3";
				panelBuilder =
					builder.addPanel(styleData, new DockLayout(true, false));
				break;

			case SPLIT:
				panelBuilder = builder.addSplitPanel(styleData);
				break;

			default:
				throw new IllegalStateException("Unsupported layout " + layout);
		}

		return panelBuilder;
	}

	@Override
	protected Map<DataElement<?>, StyleData> prepareChildDataElements(
		DataElementList dataElementList) {
		Map<DataElement<?>, StyleData> elementStyles = new LinkedHashMap<>();

		Orientation orientation =
			dataElementList.getProperty(ORIENTATION, Orientation.HORIZONTAL);

		int elementCount = dataElementList.getElementCount();

		// reorder elements because the center element must be added last
		AlignedPosition center = AlignedPosition.CENTER;
		AlignedPosition first = orientation == Orientation.VERTICAL ?
		                        AlignedPosition.TOP :
		                        AlignedPosition.LEFT;
		AlignedPosition last = orientation == Orientation.VERTICAL ?
		                       AlignedPosition.BOTTOM :
		                       AlignedPosition.RIGHT;

		if (elementCount == 3) {
			elementStyles.put(dataElementList.getElement(0), first);
			elementStyles.put(dataElementList.getElement(2), last);
			elementStyles.put(dataElementList.getElement(1), center);
		} else if (elementCount == 2) {
			if (dataElementList
				.getElement(1)
				.hasProperty(
					orientation == Orientation.VERTICAL ? HEIGHT : WIDTH)) {
				elementStyles.put(dataElementList.getElement(1), last);
				elementStyles.put(dataElementList.getElement(0), center);
			} else {
				elementStyles.put(dataElementList.getElement(0), first);
				elementStyles.put(dataElementList.getElement(1), center);
			}
		} else {
			elementStyles.put(dataElementList.getElement(0), center);
		}

		return elementStyles;
	}
}
