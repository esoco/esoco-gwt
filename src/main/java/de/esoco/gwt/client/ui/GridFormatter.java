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

import de.esoco.data.element.DataElementList;

import de.esoco.ewt.style.StyleData;

import java.util.Collection;

/**
 * An class that defines the formatting of grid data elements. Can be subclassed
 * by an UI extension to provide the formatting for it's grid layout mechanism.
 * The row style method will always be invoked first and then the column style
 * method for each column UI. Therefore implementations may cache aggregated
 * data from the row style invocation to refer to it during the column
 * formatting.
 *
 * @author eso
 */
public class GridFormatter {

	/**
	 * This method will be invoked to apply the style of a column in the grid
	 * layout to the given style data. The default implementation simply
	 * returns
	 * the original style object.
	 *
	 * @param rColumUI     The data element UI of the column
	 * @param rColumnStyle The original column style to apply the layout styles
	 *                     to
	 * @return The row style data (a new instance if modified as
	 * {@link StyleData} is immutable)
	 */
	public StyleData applyColumnStyle(DataElementUI<?> rColumUI,
		StyleData rColumnStyle) {
		return rColumnStyle;
	}

	/**
	 * This method will be invoked to apply the style of a row in the grid
	 * layout to the given style data. The default implementation simply
	 * returns
	 * the original style object.
	 *
	 * @param rRowUIs   The data element UIs for the data elements in the row
	 * @param rRowStyle The original row style to apply the layout styles to
	 * @return The row style data (a new instance if modified as
	 * {@link StyleData} is immutable)
	 */
	public StyleData applyRowStyle(Collection<DataElementUI<?>> rRowUIs,
		StyleData rRowStyle) {
		return rRowStyle;
	}

	/**
	 * An interface for factories that create instances of
	 * {@link GridFormatter}.
	 *
	 * @author eso
	 */
	public static interface GridFormatterFactory {

		/**
		 * Creates a new grid formatter instance.
		 *
		 * @param rGridElement The data element to create the formatter for
		 * @return The new grid formatter
		 */
		public GridFormatter createGridFormatter(DataElementList rGridElement);
	}
}
