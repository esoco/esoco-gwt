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
import de.esoco.ewt.style.StyleData;
import de.esoco.lib.property.RelativeSize;

import java.util.Collection;

import static de.esoco.lib.property.LayoutProperties.COLUMN_SPAN;
import static de.esoco.lib.property.LayoutProperties.MEDIUM_COLUMN_SPAN;
import static de.esoco.lib.property.LayoutProperties.RELATIVE_WIDTH;
import static de.esoco.lib.property.LayoutProperties.SMALL_COLUMN_SPAN;

/**
 * A {@link GridFormatter} implementation that uses a fixed column count for the
 * grid size calculation.
 *
 * @author eso
 */
public class ColumnCountGridFormatter extends GridFormatter {

	private final int gridColumns;

	private final String smallPrefix;

	private final String mediumPrefix;

	private final String largePrefix;

	private int currentColumn;

	private int[] columnWidths;

	/**
	 * Creates a new instance.
	 *
	 * @param gridColumns  The number of grid columns
	 * @param smallPrefix  The prefix for small display column styles
	 * @param mediumPrefix The prefix for medium display column styles
	 * @param largePrefix  The prefix for large display column styles
	 */
	public ColumnCountGridFormatter(int gridColumns, String smallPrefix,
		String mediumPrefix, String largePrefix) {
		this.gridColumns = gridColumns;
		this.smallPrefix = smallPrefix;
		this.mediumPrefix = mediumPrefix;
		this.largePrefix = largePrefix;
	}

	@Override
	public StyleData applyColumnStyle(DataElementUI<?> columUI,
		StyleData columnStyle) {
		DataElement<?> dataElement = columUI.getDataElement();
		StringBuilder style = new StringBuilder();
		int columnWidth = columnWidths[currentColumn++];

		int smallWidth = dataElement.getIntProperty(SMALL_COLUMN_SPAN,
			Math.min(columnWidth * 4, gridColumns));
		int mediumWidth = dataElement.getIntProperty(MEDIUM_COLUMN_SPAN,
			Math.min(columnWidth * 2, gridColumns));

		style.append(smallPrefix).append(smallWidth).append(' ');
		style.append(mediumPrefix).append(mediumWidth).append(' ');
		style.append(largePrefix).append(columnWidth);

		return DataElementGridPanelManager.addStyles(columnStyle,
			style.toString());
	}

	@Override
	public StyleData applyRowStyle(Collection<DataElementUI<?>> rowUIs,
		StyleData rowStyle) {
		currentColumn = 0;
		columnWidths = new int[rowUIs.size()];

		int remainingWidth = gridColumns;
		int unsetColumns = 0;
		int column = 0;

		for (DataElementUI<?> columnUI : rowUIs) {
			DataElement<?> dataElement = columnUI.getDataElement();

			@SuppressWarnings("boxing")
			int elementWidth = dataElement.getProperty(COLUMN_SPAN, -1);

			if (elementWidth == -1) {
				RelativeSize relativeWidth =
					dataElement.getProperty(RELATIVE_WIDTH, null);

				if (relativeWidth != null) {
					elementWidth = relativeWidth.calcSize(gridColumns);
				} else {
					unsetColumns++;
				}
			}

			columnWidths[column++] = elementWidth;

			if (elementWidth > 0) {
				remainingWidth -= elementWidth;
			}
		}

		for (column = 0; column < columnWidths.length; column++) {
			int unsetWidth =
				remainingWidth / (unsetColumns > 0 ? unsetColumns : 1);

			int width = columnWidths[column];

			if (width < 0) {
				if (--unsetColumns == 0) {
					width = remainingWidth;
				} else {
					width = unsetWidth;
				}

				remainingWidth -= unsetWidth;
			}

			columnWidths[column] = width;
		}

		return super.applyRowStyle(rowUIs, rowStyle);
	}
}
