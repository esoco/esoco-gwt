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

	private int nGridColumns;

	private String sSmallPrefix;

	private String sMediumPrefix;

	private String sLargePrefix;

	private int nCurrentColumn;

	private int[] aColumnWidths;

	/**
	 * Creates a new instance.
	 *
	 * @param nGridColumns  The number of grid columns
	 * @param sSmallPrefix  The prefix for small display column styles
	 * @param sMediumPrefix The prefix for medium display column styles
	 * @param sLargePrefix  The prefix for large display column styles
	 */
	public ColumnCountGridFormatter(int nGridColumns, String sSmallPrefix,
		String sMediumPrefix, String sLargePrefix) {
		this.nGridColumns = nGridColumns;
		this.sSmallPrefix = sSmallPrefix;
		this.sMediumPrefix = sMediumPrefix;
		this.sLargePrefix = sLargePrefix;
	}

	@Override
	public StyleData applyColumnStyle(DataElementUI<?> rColumUI,
		StyleData rColumnStyle) {
		DataElement<?> rDataElement = rColumUI.getDataElement();
		StringBuilder aColumnStyle = new StringBuilder();
		int nColumnWidth = aColumnWidths[nCurrentColumn++];

		int nSmallWidth = rDataElement.getIntProperty(SMALL_COLUMN_SPAN,
			Math.min(nColumnWidth * 4, nGridColumns));
		int nMediumWidth = rDataElement.getIntProperty(MEDIUM_COLUMN_SPAN,
			Math.min(nColumnWidth * 2, nGridColumns));

		aColumnStyle.append(sSmallPrefix).append(nSmallWidth).append(' ');
		aColumnStyle.append(sMediumPrefix).append(nMediumWidth).append(' ');
		aColumnStyle.append(sLargePrefix).append(nColumnWidth);

		return DataElementGridPanelManager.addStyles(rColumnStyle,
			aColumnStyle.toString());
	}

	@Override
	public StyleData applyRowStyle(Collection<DataElementUI<?>> rRowUIs,
		StyleData rRowStyle) {
		nCurrentColumn = 0;
		aColumnWidths = new int[rRowUIs.size()];

		int nRemainingWidth = nGridColumns;
		int nUnsetColumns = 0;
		int nColumn = 0;

		for (DataElementUI<?> rColumnUI : rRowUIs) {
			DataElement<?> rDataElement = rColumnUI.getDataElement();

			@SuppressWarnings("boxing")
			int nElementWidth = rDataElement.getProperty(COLUMN_SPAN, -1);

			if (nElementWidth == -1) {
				RelativeSize eRelativeWidth =
					rDataElement.getProperty(RELATIVE_WIDTH, null);

				if (eRelativeWidth != null) {
					nElementWidth = eRelativeWidth.calcSize(nGridColumns);
				} else {
					nUnsetColumns++;
				}
			}

			aColumnWidths[nColumn++] = nElementWidth;

			if (nElementWidth > 0) {
				nRemainingWidth -= nElementWidth;
			}
		}

		for (nColumn = 0; nColumn < aColumnWidths.length; nColumn++) {
			int nUnsetWidth =
				nRemainingWidth / (nUnsetColumns > 0 ? nUnsetColumns : 1);

			int nWidth = aColumnWidths[nColumn];

			if (nWidth < 0) {
				if (--nUnsetColumns == 0) {
					nWidth = nRemainingWidth;
				} else {
					nWidth = nUnsetWidth;
				}

				nRemainingWidth -= nUnsetWidth;
			}

			aColumnWidths[nColumn] = nWidth;
		}

		return super.applyRowStyle(rRowUIs, rRowStyle);
	}
}
