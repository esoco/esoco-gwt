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

	private TableControl aTable = null;

	private DataModel<? extends DataModel<?>> aDataModel;

	private ListDataModel<ColumnDefinition> aColumnModel;

	/**
	 * Returns the currently selected data model.
	 *
	 * @return The selected data model or NULL for no selection
	 */
	public DataModel<?> getSelection() {
		return aTable != null ? aTable.getSelection() : null;
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> rBuilder,
		StyleData rDisplayStyle, SelectionDataElement rDataElement) {
		Validator<?> rValidator = rDataElement.getValidator();
		Component aComponent;

		if (rValidator instanceof TabularDataValidator) {
			aComponent =
				createTableComponent(rBuilder, rDisplayStyle, rDataElement,
					(TabularDataValidator) rValidator);
		} else {
			aComponent =
				super.createDisplayUI(rBuilder, rDisplayStyle, rDataElement);
		}

		return aComponent;
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> rBuilder,
		StyleData rInputStyle, SelectionDataElement rDataElement) {
		Validator<?> rValidator = rDataElement.getValidator();
		Component aComponent;

		if (rValidator instanceof TabularDataValidator) {
			aComponent =
				createTableComponent(rBuilder, rInputStyle, rDataElement,
					(TabularDataValidator) rValidator);
		} else {
			aComponent =
				super.createInputUI(rBuilder, rInputStyle, rDataElement);
		}

		return aComponent;
	}

	@Override
	protected void transferDataElementValueToComponent(
		SelectionDataElement rDataElement, Component rComponent) {
		if (aTable != null) {
			initTable(rDataElement);
		} else {
			super.transferDataElementValueToComponent(rDataElement,
				rComponent);

			aTable.setSelection(
				rDataElement.getIntProperty(CURRENT_SELECTION, -1));
		}
	}

	@Override
	protected void transferInputToDataElement(Component rComponent,
		SelectionDataElement rDataElement) {
		if (aTable != null) {
			DataModel<?> rSelectedRow = aTable.getSelection();

			rDataElement.setProperty(CURRENT_SELECTION,
				aTable.getSelectionIndex());

			if (rSelectedRow instanceof HierarchicalDataObject) {
				rDataElement.setValue(
					((HierarchicalDataObject) rSelectedRow).getId());
			} else {
				rDataElement.setValue(SelectionDataElement.NO_SELECTION);
			}

			Map<String, String> rTableConstraints =
				((FilterableDataModel<?>) aDataModel).getFilters();

			if (rTableConstraints.isEmpty()) {
				rTableConstraints = null;
			}

			rDataElement.setProperty(FILTER_CRITERIA, rTableConstraints);
		} else {
			super.transferInputToDataElement(rComponent, rDataElement);
		}
	}

	/**
	 * Crates the data model for a table.
	 *
	 * @param rValidator The validator to create the model from
	 * @return The data model
	 */
	private DataModel<? extends DataModel<?>> checkTableDataModel(
		TabularDataValidator rValidator) {
		DataModel<? extends DataModel<?>> aModel;

		if (rValidator instanceof QueryValidator) {
			QueryValidator rQueryValidator = (QueryValidator) rValidator;
			QueryDataModel rCurrentModel = (QueryDataModel) aDataModel;
			String sQueryId = rQueryValidator.getQueryId();

			aModel = rCurrentModel;

			if (rCurrentModel == null ||
				!rCurrentModel.getQueryId().equals(sQueryId)) {
				aModel = new QueryDataModel(sQueryId, 0);

				if (aDataModel != null) {
					((QueryDataModel) aModel).useConstraints(rCurrentModel);
				}
			} else {
				rCurrentModel.resetQuerySize();
			}
		} else if (rValidator instanceof SelectionValidator) {
			SelectionValidator rSelectionValidator =
				(SelectionValidator) rValidator;

			aModel = new FilterableListDataModel<HierarchicalDataObject>(
				"DATA",
				rSelectionValidator.getValues(), rValidator.getColumns());
		} else {
			throw new IllegalArgumentException(
				"Invalid table validator: " + rValidator);
		}

		return aModel;
	}

	/**
	 * Creates a component that allows to select an element from the results of
	 * a remote query.
	 *
	 * @param rBuilder     The builder to create the component with
	 * @param rInputStyle  The default style data
	 * @param rDataElement The data element
	 * @param rValidator   The tabular data validator from the data element
	 * @return The new component
	 */
	@SuppressWarnings("boxing")
	private Component createTableComponent(ContainerBuilder<?> rBuilder,
		StyleData rInputStyle, SelectionDataElement rDataElement,
		TabularDataValidator rValidator) {
		List<ColumnDefinition> rColumns = rValidator.getColumns();

		if (rColumns != null) {
			int nRows = rDataElement.getIntProperty(TABLE_ROWS, -1);

			if (nRows > 0) {
				rInputStyle = rInputStyle.set(TABLE_ROWS, nRows);
			}

			if (rDataElement.hasFlag(UserInterfaceProperties.HIERARCHICAL)) {
				aTable = rBuilder.addTreeTable(rInputStyle);
			} else {
				aTable = rBuilder.addTable(rInputStyle);
			}

			initTable(rDataElement);
		} else {
			throw new IllegalArgumentException(
				"Missing table columns for " + rDataElement);
		}

		return aTable;
	}

	/**
	 * Initializes the table from the data element.
	 *
	 * @param rDataElement The selection data element
	 */
	private void initTable(final SelectionDataElement rDataElement) {
		TabularDataValidator rValidator =
			(TabularDataValidator) rDataElement.getValidator();

		aDataModel = checkTableDataModel(rValidator);

		Map<String, String> rConstraints =
			rDataElement.getProperty(FILTER_CRITERIA, Collections.emptyMap());

		((FilterableDataModel<?>) aDataModel).setFilters(rConstraints);

		aColumnModel = new ListDataModel<ColumnDefinition>("COLUMNS",
			rValidator.getColumns());

		int nSelection = rDataElement.getIntProperty(CURRENT_SELECTION, -1);

		aTable.setColumns(aColumnModel);
		aTable.setData(aDataModel);
		aTable.setSelection(nSelection, false);
		aTable.getContext().runLater(new Runnable() {
			@Override
			public void run() {
				aTable.repaint();
			}
		});
	}
}
