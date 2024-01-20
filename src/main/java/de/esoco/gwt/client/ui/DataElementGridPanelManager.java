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
import de.esoco.ewt.style.StyleData;
import de.esoco.gwt.client.ui.GridFormatter.GridFormatterFactory;
import de.esoco.lib.property.Alignment;
import de.esoco.lib.property.LayoutType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static de.esoco.lib.property.LayoutProperties.HORIZONTAL_ALIGN;
import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.LayoutProperties.SAME_ROW;
import static de.esoco.lib.property.LayoutProperties.VERTICAL_ALIGN;
import static de.esoco.lib.property.StyleProperties.HIDE_LABEL;

/**
 * A layout panel manager subclass that arranges the data element UIs in a CSS
 * grid by setting the corresponding styles based on the data element
 * properties.
 *
 * @author eso
 */
public class DataElementGridPanelManager extends DataElementLayoutPanelManager {

	private static final StyleData ROW_VALIGN_CENTER_STYLE =
		StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES,
			CSS.valignCenter());

	private static final StyleData ROW_VALIGN_BOTTOM_STYLE =
		StyleData.DEFAULT.set(StyleData.WEB_ADDITIONAL_STYLES,
			CSS.valignBottom());

	private static GridFormatterFactory gridFormatterFactory =
		e -> new ColumnCountGridFormatter(12, "s", "m", "l");

	private final Map<DataElementUI<?>, StyleData> currentRow =
		new LinkedHashMap<>();

	private GridFormatter gridFormatter;

	private StyleData rowStyle = StyleData.DEFAULT;

	/**
	 * @see DataElementLayoutPanelManager#DataElementLayoutPanelManager(PanelManager,
	 * DataElementList)
	 */
	public DataElementGridPanelManager(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		super(parent, dataElementList);
	}

	/**
	 * A global configuration method to set the grid formatter for all
	 * grid-based panels.
	 *
	 * @param factory formatter The global grid formatter
	 */
	public static void setGridFormatterFactory(GridFormatterFactory factory) {
		gridFormatterFactory = factory;
	}

	/**
	 * Sets the style of a completed row of data elements.
	 */
	protected void buildCurrentRow() {
		ContainerBuilder<?> rowBuilder = this;
		boolean firstElement = true;

		for (Entry<DataElementUI<?>, StyleData> uiAndStyle :
			currentRow.entrySet()) {
			DataElementUI<?> uI = uiAndStyle.getKey();
			DataElement<?> dataElement = uI.getDataElement();
			StyleData style = uiAndStyle.getValue();
			int elementCount = currentRow.size();

			LayoutType elementLayout = dataElement.getProperty(LAYOUT, null);

			if (elementLayout == LayoutType.GRID_ROW && elementCount == 1) {
				style = gridFormatter.applyRowStyle(currentRow.keySet(),
					style);
			} else if (firstElement) {
				firstElement = false;

				rowBuilder = addPanel(
					gridFormatter.applyRowStyle(currentRow.keySet(),
						this.rowStyle), LayoutType.GRID_ROW);
			}

			ContainerBuilder<?> uiBuilder = rowBuilder;

			boolean addLabel = !dataElement.hasFlag(HIDE_LABEL);

			if (addLabel || elementLayout != LayoutType.GRID_COLUMN) {
				StyleData columnStyle = StyleData.DEFAULT;

				Alignment align =
					dataElement.getProperty(HORIZONTAL_ALIGN, null);

				if (align != null) {
					columnStyle = columnStyle.set(HORIZONTAL_ALIGN, align);
				}

				columnStyle = gridFormatter.applyColumnStyle(uI, columnStyle);

				uiBuilder =
					rowBuilder.addPanel(columnStyle, LayoutType.GRID_COLUMN);

				if (addLabel) {
					String label = uI.createElementLabelString(getContext());

					if (!label.isEmpty()) {
						uI.createElementLabel(uiBuilder, FORM_LABEL_STYLE,
							label);
					}
				}
			} else {
				// apply column count to elements with Layout GRID_COLUMN
				style = gridFormatter.applyColumnStyle(uI, style);
			}

			uI.buildUserInterface(uiBuilder, style);
			applyElementProperties(uI);
		}

		currentRow.clear();
	}

	@Override
	protected void buildDataElementUI(DataElementUI<?> dataElementUI,
		StyleData style) {
		if (!dataElementUI.getDataElement().hasFlag(SAME_ROW)) {
			buildCurrentRow();
		}

		currentRow.put(dataElementUI, style);
	}

	@Override
	protected void buildElementUIs() {
		gridFormatter =
			gridFormatterFactory.createGridFormatter(getDataElementList());

		super.buildElementUIs();

		// build last row
		buildCurrentRow();

		gridFormatter = null;
	}

	/**
	 * Overridden to check the container style for vertical alignment.
	 *
	 * @see DataElementLayoutPanelManager#createPanel(ContainerBuilder,
	 * StyleData, LayoutType)
	 */
	@Override
	protected ContainerBuilder<?> createPanel(ContainerBuilder<?> builder,
		StyleData style, LayoutType layout) {
		Alignment align = style.getProperty(VERTICAL_ALIGN, null);

		if (align == Alignment.CENTER) {
			rowStyle = ROW_VALIGN_CENTER_STYLE;
		} else if (align == Alignment.END) {
			rowStyle = ROW_VALIGN_BOTTOM_STYLE;
		}

		return super.createPanel(builder, style, layout);
	}
}
