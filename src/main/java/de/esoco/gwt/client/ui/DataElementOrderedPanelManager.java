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
	public DataElementOrderedPanelManager(PanelManager<?, ?> rParent,
		DataElementList rDataElementList) {
		super(rParent, rDataElementList);
	}

	@Override
	protected ContainerBuilder<? extends Panel> createPanel(
		ContainerBuilder<?> rBuilder, StyleData rStyleData,
		LayoutType eLayout) {
		ContainerBuilder<? extends Panel> aPanelBuilder;

		switch (eLayout) {
			case DOCK:
				assert getDataElementList().getElementCount() <= 3 :
					"Element count for DOCK layout mode must be <= 3";
				aPanelBuilder =
					rBuilder.addPanel(rStyleData, new DockLayout(true, false));
				break;

			case SPLIT:
				aPanelBuilder = rBuilder.addSplitPanel(rStyleData);
				break;

			default:
				throw new IllegalStateException(
					"Unsupported layout " + eLayout);
		}

		return aPanelBuilder;
	}

	@Override
	protected Map<DataElement<?>, StyleData> prepareChildDataElements(
		DataElementList rDataElementList) {
		Map<DataElement<?>, StyleData> rElementStyles = new LinkedHashMap<>();

		Orientation eOrientation =
			rDataElementList.getProperty(ORIENTATION, Orientation.HORIZONTAL);

		int nElementCount = rDataElementList.getElementCount();

		// reorder elements because the center element must be added last
		AlignedPosition rCenter = AlignedPosition.CENTER;
		AlignedPosition rFirst = eOrientation == Orientation.VERTICAL ?
		                         AlignedPosition.TOP :
		                         AlignedPosition.LEFT;
		AlignedPosition rLast = eOrientation == Orientation.VERTICAL ?
		                        AlignedPosition.BOTTOM :
		                        AlignedPosition.RIGHT;

		if (nElementCount == 3) {
			rElementStyles.put(rDataElementList.getElement(0), rFirst);
			rElementStyles.put(rDataElementList.getElement(2), rLast);
			rElementStyles.put(rDataElementList.getElement(1), rCenter);
		} else if (nElementCount == 2) {
			if (rDataElementList
				.getElement(1)
				.hasProperty(
					eOrientation == Orientation.VERTICAL ? HEIGHT : WIDTH)) {
				rElementStyles.put(rDataElementList.getElement(1), rLast);
				rElementStyles.put(rDataElementList.getElement(0), rCenter);
			} else {
				rElementStyles.put(rDataElementList.getElement(0), rFirst);
				rElementStyles.put(rDataElementList.getElement(1), rCenter);
			}
		} else {
			rElementStyles.put(rDataElementList.getElement(0), rCenter);
		}

		return rElementStyles;
	}
}
