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

import de.esoco.data.element.HierarchicalDataObject;
import de.esoco.data.element.SelectionDataElement;
import de.esoco.data.validate.QueryValidator;
import de.esoco.data.validate.SelectionValidator;
import de.esoco.data.validate.TabularDataValidator;
import de.esoco.data.validate.Validator;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.TableControl;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.data.QueryDataModel;
import de.esoco.gwt.client.data.FilterableListDataModel;

import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.ListDataModel;
import de.esoco.lib.model.FilterableDataModel;
import de.esoco.lib.property.UserInterfaceProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static de.esoco.lib.property.StateProperties.CURRENT_SELECTION;
import static de.esoco.lib.property.StateProperties.FILTER_CRITERIA;
import static de.esoco.lib.property.StyleProperties.TABLE_ROWS;

/**
 * The user interface implementation for {@link SelectionDataElement}
 * instances.
 *
 * @author eso
 */
public class SelectionDataElementUI
	extends DataElementUI<SelectionDataElement> {

	private TableControl table = null;

	private DataModel<? extends DataModel<?>> dataModel;

	private ListDataModel<ColumnDefinition> columnModel;

	/**
	 * Returns the currently selected data model.
	 *
	 * @return The selected data model or NULL for no selection
	 */
	public DataModel<?> getSelection() {
		return table != null ? table.getSelection() : null;
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> builder,
		StyleData displayStyle, SelectionDataElement dataElement) {
		Validator<?> validator = dataElement.getValidator();
		Component component;

		if (validator instanceof TabularDataValidator) {
			component = createTableComponent(builder, displayStyle,
				dataElement,
				(TabularDataValidator) validator);
		} else {
			component =
				super.createDisplayUI(builder, displayStyle, dataElement);
		}

		return component;
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData inputStyle, SelectionDataElement dataElement) {
		Validator<?> validator = dataElement.getValidator();
		Component component;

		if (validator instanceof TabularDataValidator) {
			component = createTableComponent(builder, inputStyle, dataElement,
				(TabularDataValidator) validator);
		} else {
			component = super.createInputUI(builder, inputStyle, dataElement);
		}

		return component;
	}

	@Override
	protected void transferDataElementValueToComponent(
		SelectionDataElement dataElement, Component component) {
		if (table != null) {
			initTable(dataElement);
		} else {
			super.transferDataElementValueToComponent(dataElement, component);

			table.setSelection(
				dataElement.getIntProperty(CURRENT_SELECTION, -1));
		}
	}

	@Override
	protected void transferInputToDataElement(Component component,
		SelectionDataElement dataElement) {
		if (table != null) {
			DataModel<?> selectedRow = table.getSelection();

			dataElement.setProperty(CURRENT_SELECTION,
				table.getSelectionIndex());

			if (selectedRow instanceof HierarchicalDataObject) {
				dataElement.setValue(
					((HierarchicalDataObject) selectedRow).getId());
			} else {
				dataElement.setValue(SelectionDataElement.NO_SELECTION);
			}

			Map<String, String> tableConstraints =
				((FilterableDataModel<?>) dataModel).getFilters();

			if (tableConstraints.isEmpty()) {
				tableConstraints = null;
			}

			dataElement.setProperty(FILTER_CRITERIA, tableConstraints);
		} else {
			super.transferInputToDataElement(component, dataElement);
		}
	}

	/**
	 * Crates the data model for a table.
	 *
	 * @param validator The validator to create the model from
	 * @return The data model
	 */
	private DataModel<? extends DataModel<?>> checkTableDataModel(
		TabularDataValidator validator) {
		DataModel<? extends DataModel<?>> model;

		if (validator instanceof QueryValidator) {
			QueryValidator queryValidator = (QueryValidator) validator;
			QueryDataModel currentModel = (QueryDataModel) dataModel;
			String queryId = queryValidator.getQueryId();

			model = currentModel;

			if (currentModel == null ||
				!currentModel.getQueryId().equals(queryId)) {
				model = new QueryDataModel(queryId, 0);

				if (dataModel != null) {
					((QueryDataModel) model).useConstraints(currentModel);
				}
			} else {
				currentModel.resetQuerySize();
			}
		} else if (validator instanceof SelectionValidator) {
			SelectionValidator selectionValidator =
				(SelectionValidator) validator;

			model = new FilterableListDataModel<HierarchicalDataObject>("DATA",
				selectionValidator.getValues(), validator.getColumns());
		} else {
			throw new IllegalArgumentException(
				"Invalid table validator: " + validator);
		}

		return model;
	}

	/**
	 * Creates a component that allows to select an element from the results of
	 * a remote query.
	 *
	 * @param builder     The builder to create the component with
	 * @param inputStyle  The default style data
	 * @param dataElement The data element
	 * @param validator   The tabular data validator from the data element
	 * @return The new component
	 */
	@SuppressWarnings("boxing")
	private Component createTableComponent(ContainerBuilder<?> builder,
		StyleData inputStyle, SelectionDataElement dataElement,
		TabularDataValidator validator) {
		List<ColumnDefinition> columns = validator.getColumns();

		if (columns != null) {
			int rows = dataElement.getIntProperty(TABLE_ROWS, -1);

			if (rows > 0) {
				inputStyle = inputStyle.set(TABLE_ROWS, rows);
			}

			if (dataElement.hasFlag(UserInterfaceProperties.HIERARCHICAL)) {
				table = builder.addTreeTable(inputStyle);
			} else {
				table = builder.addTable(inputStyle);
			}

			initTable(dataElement);
		} else {
			throw new IllegalArgumentException(
				"Missing table columns for " + dataElement);
		}

		return table;
	}

	/**
	 * Initializes the table from the data element.
	 *
	 * @param dataElement The selection data element
	 */
	private void initTable(final SelectionDataElement dataElement) {
		TabularDataValidator validator =
			(TabularDataValidator) dataElement.getValidator();

		dataModel = checkTableDataModel(validator);

		Map<String, String> constraints =
			dataElement.getProperty(FILTER_CRITERIA, Collections.emptyMap());

		((FilterableDataModel<?>) dataModel).setFilters(constraints);

		columnModel = new ListDataModel<ColumnDefinition>("COLUMNS",
			validator.getColumns());

		int selection = dataElement.getIntProperty(CURRENT_SELECTION, -1);

		table.setColumns(columnModel);
		table.setData(dataModel);
		table.setSelection(selection, false);
		table.getContext().runLater(new Runnable() {
			@Override
			public void run() {
				table.repaint();
			}
		});
	}
}
