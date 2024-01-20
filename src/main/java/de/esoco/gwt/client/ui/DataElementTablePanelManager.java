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
import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.StyleData;
import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.UserInterfaceProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.esoco.lib.property.LayoutProperties.COLUMN_SPAN;
import static de.esoco.lib.property.LayoutProperties.HTML_HEIGHT;
import static de.esoco.lib.property.LayoutProperties.HTML_WIDTH;
import static de.esoco.lib.property.LayoutProperties.ROW_SPAN;
import static de.esoco.lib.property.LayoutProperties.SAME_ROW;
import static de.esoco.lib.property.StyleProperties.HEADER_LABEL;
import static de.esoco.lib.property.StyleProperties.HIDE_LABEL;
import static de.esoco.lib.property.StyleProperties.STYLE;

/**
 * A panel manager that organizes data elements in a table layout.
 *
 * @author eso
 */
public class DataElementTablePanelManager extends DataElementPanelManager {

	private boolean hasOptions;

	private boolean hasLabels;

	private int elementColumns;

	/**
	 * Creates a new instance from the elements in a {@link DataElementList}.
	 *
	 * @param parent          The parent panel manager
	 * @param dataElementList name A name for this instance that will be set as
	 *                        an additional GWT style name
	 */
	public DataElementTablePanelManager(PanelManager<?, ?> parent,
		DataElementList dataElementList) {
		super(parent, dataElementList);
	}

	@Override
	public void rebuild() {
		getDataElementsLayout().setGridCount(
			calcLayoutColumns(getDataElementList().getElements()));
		super.rebuild();
	}

	@Override
	public void setElementVisibility(DataElementUI<?> elementUI,
		boolean visible) {
		getDataElementsLayout().changeCellStyle(getContainer(),
			elementUI.getElementComponent(), CSS.gfEmptyCell(), !visible);
	}

	/**
	 * Adds the user interfaces for the data elements in this panel.
	 */
	@Override
	protected void buildElementUIs() {
		List<DataElement<?>> dataElements = getDataElementList().getElements();
		UserInterfaceContext context = getContext();
		List<Label> headers = null;

		int column = elementColumns;
		int headerColumn = 0;
		int elementCount = dataElements.size();
		boolean focusSet = false;

		for (int elementIndex = 0;
		     elementIndex < elementCount; elementIndex++) {
			DataElement<?> dataElement = dataElements.get(elementIndex);

			String width = dataElement.getProperty(HTML_WIDTH, null);
			String height = dataElement.getProperty(HTML_HEIGHT, null);

			boolean newRow = !dataElement.hasFlag(SAME_ROW);
			boolean immutable = dataElement.isImmutable();
			boolean hideLabel = dataElement.hasFlag(HIDE_LABEL) ||
				dataElement.hasFlag(HEADER_LABEL);

			boolean isChildViewElement = dataElement.hasProperty(
				UserInterfaceProperties.VIEW_DISPLAY_TYPE);

			int extraColumns = newRow && hideLabel && hasLabels ? 1 : 0;

			DataElementUI<?> elementUI =
				DataElementUIFactory.create(this, dataElement);

			String style = elementUI.getElementStyleName();

			if (newRow && !isChildViewElement) {
				int rowElement = elementIndex;
				int col = 0;

				headers = null;
				headerColumn = 0;

				while (col < elementColumns && rowElement < elementCount) {
					DataElement<?> element = dataElements.get(rowElement++);

					if (headers == null && element.hasFlag(HEADER_LABEL)) {
						headers = new ArrayList<>();
					}

					col += element.getIntProperty(COLUMN_SPAN, 1);
				}

				if (headers != null) {
					addElementRow(elementUI, "", column);
					column = 0;

					for (int i = elementIndex; i < rowElement; i++) {
						DataElement<?> element = dataElements.get(i);

						String headerStyle = element.getResourceId();

						Label header =
							addLabel(addStyles(HEADER_LABEL_STYLE,
									headerStyle),
								"", null);

						headers.add(header);
						addCellStyles(CSS.gfDataElementHeader(),
							headerStyle + "Header");
						column++;
					}
				}

				addElementRow(elementUI, style + "Label", column);
				column = 0;
			}

			if (immutable) {
				style = CSS.readonly() + " " + style;
			}

			StyleData elementStyle = addStyles(ELEMENT_STYLE, style);

			// element UI must be registered before building to allow
			// cross-panel references of selection dependencies
			getDataElementUIs().put(dataElement.getName(), elementUI);
			elementUI.buildUserInterface(this, elementStyle);

			if ("100%".equals(width) && "100%".equals(height)) {
				elementUI
					.getElementComponent()
					.getWidget()
					.setSize("100%", "100%");
			}

			if (!newRow || hideLabel) {
				if (headers != null && dataElement.hasFlag(HEADER_LABEL)) {
					Label headerLabel = headers.get(headerColumn);

					headerLabel.setText(elementUI.getElementLabelText(context));
				} else {
					elementUI.setHiddenLabelHint(context);
				}
			}

			if (!isChildViewElement) {
				headerColumn++;
				column +=
					checkLayoutProperties(dataElement, style, width, height,
						extraColumns);
			}

			if (getParent() == null && !(focusSet || immutable)) {
				focusSet = elementUI.requestFocus();
			}
		}
	}

	/**
	 * Calculates the total number of columns for a panel layout containing the
	 * given data elements.
	 *
	 * @param dataElements The data elements to analyze
	 * @return The new layout
	 */
	protected int calcLayoutColumns(Collection<DataElement<?>> dataElements) {
		int extraColumns = 0;
		int rowElementColumns = 0;

		elementColumns = 0;
		hasOptions = false;
		hasLabels = false;

		for (DataElement<?> dataElement : dataElements) {
			boolean newRow = !dataElement.hasFlag(SAME_ROW);

			if (!hasOptions && dataElement.isOptional()) {
				hasOptions = true;
				extraColumns++;
			}

			if (!hasLabels && newRow && !(dataElement.hasFlag(HIDE_LABEL) ||
				dataElement.hasFlag(HEADER_LABEL))) {
				hasLabels = true;
				extraColumns++;
			}

			if (newRow) {
				elementColumns = Math.max(elementColumns, rowElementColumns);

				rowElementColumns = 0;
			}

			rowElementColumns += dataElement.getIntProperty(COLUMN_SPAN, 1);
		}

		// repeat comparison for the last row
		elementColumns = Math.max(elementColumns, rowElementColumns);

		// always return at least 1 column in the case of no data elements
		return Math.max(elementColumns + extraColumns, 1);
	}

	@Override
	protected ContainerBuilder<? extends Panel> createPanel(
		ContainerBuilder<?> builder, StyleData styleData, LayoutType layout) {
		List<DataElement<?>> dataElements = getDataElementList().getElements();

		ContainerBuilder<Panel> containerBuilder = builder.addPanel(styleData,
			new TableGridLayout(calcLayoutColumns(dataElements), true));

		return containerBuilder;
	}

	/**
	 * Adds certain style names to a cell in the grid layout of this instance.
	 *
	 * @param styles labelCellStyle
	 */
	private void addCellStyles(String... styles) {
		TableGridLayout layout = getDataElementsLayout();
		Container container = getContainer();

		for (String style : styles) {
			if (style.length() > 0) {
				layout.addCellStyle(container, style);
			}
		}
	}

	/**
	 * Adds a new row of data element UIs to the layout.
	 *
	 * @param elementUI             The first data element UI to add
	 * @param labelCellStyle        The style for the grid cell of the element
	 *                              label
	 * @param previousRowLastColumn The last column of the previous row
	 */
	private void addElementRow(DataElementUI<?> elementUI,
		String labelCellStyle, int previousRowLastColumn) {
		DataElement<?> dataElement = elementUI.getDataElement();

		while (previousRowLastColumn++ < elementColumns) {
			addEmptyCell();
		}

		if (elementUI != null && !dataElement.isImmutable() &&
			dataElement.isOptional()) {
			elementUI.addOptionSelector(this);
		} else if (hasOptions) {
			addEmptyCell();
		}

		if (elementUI != null && !(dataElement.hasFlag(HIDE_LABEL) ||
			dataElement.hasFlag(HEADER_LABEL))) {
			StyleData elementLabelStyle =
				addStyles(ELEMENT_LABEL_STYLE, labelCellStyle);

			elementUI.createElementLabel(this, elementLabelStyle,
				elementUI.createElementLabelString(getContext()));
			addCellStyles(CSS.gfDataElementLabel(), labelCellStyle);
		}
	}

	/**
	 * Adds a cell with no content to a container builder with a grid layout.
	 */
	private void addEmptyCell() {
		addLabel(StyleData.DEFAULT, "", null);
		getDataElementsLayout().addCellStyle(getContainer(),
			CSS.gfEmptyCell());
	}

	/**
	 * Checks if grid layout properties exist for a data element and applies
	 * them if necessary. Returns the column span so that it can be subtracted
	 * from the extra columns in the layout. Also applies a style to the
	 * current
	 * cell.
	 *
	 * @param dataElement  The data element
	 * @param style        The style name for the data element
	 * @param width        The HTML width or NULL for none
	 * @param height       The HTML height or NULL for none
	 * @param extraColumns The count of extra columns that the element should
	 *                     span (zero for none)
	 * @return The column span
	 */
	private int checkLayoutProperties(DataElement<?> dataElement, String style,
		String width, String height, int extraColumns) {
		Container container = getContainer();
		TableGridLayout layout = getDataElementsLayout();
		String addStyle = dataElement.getProperty(STYLE, null);
		int rowSpan = dataElement.getIntProperty(ROW_SPAN, 1);
		int colSpan = dataElement.getIntProperty(COLUMN_SPAN, 1);

		if (rowSpan > 1) {
			layout.joinRows(container, rowSpan);
		}

		if (colSpan + extraColumns > 1) {
			layout.joinColumns(container, colSpan + extraColumns);
		}

		if (width != null || height != null) {
			layout.setCellSize(container, width, height);
		}

		if (addStyle != null) {
			style += " " + addStyle;
		}

		style = style.trim();

		if (style.length() > 0) {
			layout.addCellStyle(container, style);
		}

		return colSpan;
	}

	/**
	 * Returns the grid layout for the data elements in this panel.
	 *
	 * @return The data elements layout
	 */
	private TableGridLayout getDataElementsLayout() {
		return (TableGridLayout) getContainer().getLayout();
	}
}
