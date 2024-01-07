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
package de.esoco.gwt.client.data;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.FilterableDataModel;
import de.esoco.lib.model.ListDataModel;
import de.esoco.lib.model.SortableDataModel;
import de.esoco.lib.property.SortDirection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple data model implementation that is based on a list. A name can be
 * assigned to instances so that they can be rendered directly in certain user
 * interface elements. This data model is sortable and searchable.
 *
 * @author ueggers
 */
public class FilterableListDataModel<T extends DataModel<String>>
	extends ListDataModel<T>
	implements SortableDataModel<T>, FilterableDataModel<T>, Serializable {

	private static final long serialVersionUID = 1L;

	private final List<T> data;

	private final List<T> dataCopy;

	private final List<ColumnDefinition> columns;

	private final List<String> fieldIds = new ArrayList<String>();

	private final RegExp constraintPattern = RegExp.compile(
		"([" + FilterableDataModel.CONSTRAINT_OR_PREFIX +
			FilterableDataModel.CONSTRAINT_AND_PREFIX + "])([" +
			FilterableDataModel.CONSTRAINT_COMPARISON_CHARS + "])(.*)");

	HashMap<String, SortDirection> columnSorting = new LinkedHashMap<>();

	HashMap<String, String> filters = new LinkedHashMap<>();

	private boolean newFilters;

	/**
	 * Creates a new instance.
	 *
	 * @param name    The name of this model.
	 * @param data    The model's data
	 * @param columns The columns definitions
	 */
	public FilterableListDataModel(String name, List<T> data,
		List<ColumnDefinition> columns) {
		super(name, data);

		this.data = data;
		this.dataCopy = new ArrayList<T>(data);
		this.columns = columns;

		for (ColumnDefinition columnDefinition : columns) {
			fieldIds.add(columnDefinition.getId());
		}
	}

	@Override
	public T getElement(int index) {
		applyConstraints();

		return super.getElement(index);
	}

	@Override
	public int getElementCount() {
		applyConstraints();

		return super.getElementCount();
	}

	@Override
	public String getFilter(String fieldId) {
		return filters.get(fieldId);
	}

	@Override
	public Map<String, String> getFilters() {
		return filters;
	}

	@Override
	public SortDirection getSortDirection(String fieldId) {
		SortDirection sortDirection = columnSorting.get(fieldId);

		return sortDirection;
	}

	@Override
	public void removeAllFilters() {
		filters.clear();
		newFilters = true;
	}

	@Override
	public void removeSorting() {
		columnSorting.clear();
		newFilters = true;
	}

	@Override
	public void setFilter(String fieldId, String filter) {
		if (filter != null && !filter.isEmpty()) {
			filters.put(fieldId, filter);
		} else {
			filters.remove(fieldId);
		}

		newFilters = true;
	}

	@Override
	public void setFilters(Map<String, String> filters) {
		filters.clear();
		filters.putAll(filters);
		newFilters = true;
	}

	@Override
	public void setSortDirection(String fieldId, SortDirection mode) {
		if (mode != null) {
			columnSorting.put(fieldId, mode);
		} else {
			columnSorting.remove(fieldId);
		}

		newFilters = true;
	}

	/**
	 * Applies the defined constraints for sorting and filtering. This
	 * effectively sets the elements and their order in the data model.
	 */
	private void applyConstraints() {
		if (newFilters) {
			performFiltering();
			performSorting();
			newFilters = false;
		}
	}

	/**
	 * Applies all the filtering constraints to the data.
	 */
	private void performFiltering() {
		data.clear();

		List<T> allData = new ArrayList<T>(dataCopy);

		for (T dataModel : allData) {
			boolean filterMatched = filters.isEmpty();
			Set<String> filteredIds = filters.keySet();

			int index = 0;

			for (String filterId : filteredIds) {
				String value =
					dataModel.getElement(fieldIds.indexOf(filterId));

				String constraints = filters.get(filterId);

				boolean attrOr = constraints.charAt(0) ==
					FilterableDataModel.CONSTRAINT_OR_PREFIX;

				boolean satisfiesConstraints =
					satisfiesConstraints(constraints, value);

				if (index == 0) {
					filterMatched = satisfiesConstraints;
				} else {
					if (attrOr) {
						filterMatched = satisfiesConstraints || filterMatched;
					} else {
						filterMatched = satisfiesConstraints && filterMatched;
					}
				}

				index++;
			}

			if (filterMatched) {
				data.add(dataModel);
			}
		}
	}

	/**
	 * Performs the sorting according to the sorting preferences.
	 */

	private void performSorting() {
		if (columnSorting.size() > 0) {
			Set<String> sortIds = columnSorting.keySet();

			for (final String sortId : sortIds) {
				final SortDirection sortDirection = columnSorting.get(sortId);

				final int fieldIndex = fieldIds.indexOf(sortId);

				final ColumnDefinition columnDefinition =
					columns.get(fieldIndex);

				data.sort(new Comparator<DataModel<String>>() {
					@Override
					public int compare(DataModel<String> dataModel,
						DataModel<String> dataModelCmp) {
						String value = dataModel.getElement(fieldIndex);

						String compareValue =
							dataModelCmp.getElement(fieldIndex);

						int result = 0;

						if (sortDirection == SortDirection.DESCENDING) {
							result = compareFieldValues(compareValue, value);
						} else {
							result = compareFieldValues(value, compareValue);
						}

						return result;
					}

					private int compareFieldValues(String value,
						String compareValue) {
						int result = 0;

						if (columnDefinition
							.getDatatype()
							.equals(Integer.class.getName())) {
							result = Integer
								.valueOf(value)
								.compareTo(Integer.valueOf(compareValue));
						} else {
							result = value.compareTo(compareValue);
						}

						return result;
					}
				});
			}
		}
	}

	/**
	 * Checks whether a given value satisfies a given search constraints. The
	 * search constrains are parsed and split up using
	 * {@link FilterableDataModel#CONSTRAINT_SEPARATOR}. The resulting string
	 * elements contain a constraint prefix, a comparison operator and a
	 * constraint value. The regular expression {@link #constraintPattern} is
	 * used to retrieve the three parts from the string elements.
	 *
	 * @param constraints The search constraints in string representation
	 * @param value       a value to be checked.
	 * @return TRUE if a given value satisfies the given search constraints
	 * FALSE otherwise.
	 */
	private boolean satisfiesConstraints(String constraints, String value) {
		boolean satisfies = true;

		String[] contraints =
			constraints.split(FilterableDataModel.CONSTRAINT_SEPARATOR);

		for (int i = 0; i < contraints.length; i++) {
			String constraint = contraints[i];

			MatchResult constraintMatcher = constraintPattern.exec(constraint);

			if (constraintMatcher != null) {
				String prefix = constraintMatcher.getGroup(1);
				String comparison = constraintMatcher.getGroup(2);
				String constraintValue = constraintMatcher.getGroup(3);

				boolean or = prefix.equals(
					String.valueOf(FilterableDataModel.CONSTRAINT_OR_PREFIX));

				boolean satisfiesConstraint = false;

				if (constraintValue != null && value != null) {
					switch (comparison) {
						case "=":
							satisfiesConstraint =
								satisfiesEquals(constraintValue, value);

							break;

						case "\u2260": // !=
							satisfiesConstraint =
								!constraintValue.equals(value);
							break;

						case "~":

							String[] constraintItems =
								constraintValue.split(",");

							for (String constraintItem : constraintItems) {
								satisfiesConstraint |=
									satisfiesEquals(constraintItem, value);
							}

							break;

						case "<":
							satisfiesConstraint =
								constraintValue.compareTo(value) > 0;
							break;

						case ">":
							satisfiesConstraint =
								constraintValue.compareTo(value) < 0;
							break;

						case "\u2264": // <=
							satisfiesConstraint =
								constraintValue.compareTo(value) >= 0;
							break;

						case "\u2265": // >=
							satisfiesConstraint =
								constraintValue.compareTo(value) <= 0;
							break;
					}
				}

				if (i == 0) {
					satisfies = satisfies && satisfiesConstraint;
				} else {
					if (or) {
						satisfies = satisfies || satisfiesConstraint;
					} else {
						satisfies = satisfies && satisfiesConstraint;
					}
				}
			}
		}

		return satisfies;
	}

	/**
	 * Checks whether the given value satisfies the given constraint value when
	 * the equals "=" operator is applied. This check also respects wildcards
	 * (*) in the constraint value.
	 *
	 * @param constraintValue The search constraint value
	 * @param value           The value to check for a match.
	 * @return Whether the given value satisfies the given constraint value
	 * when
	 * the equals "=" operator is applied.
	 */
	private boolean satisfiesEquals(String constraintValue, String value) {
		boolean satisfiesConstraint = false;

		if (constraintValue.contains("*")) {
			String flags = "";

			if (RegExp
				.compile("[A-Z]+")
				.exec(constraintValue.replaceAll("\\*", "")) == null) {
				flags += "i";
			}

			RegExp compile =
				RegExp.compile("^" + constraintValue.replaceAll("\\*", ".*"),
					flags);
			MatchResult exec = compile.exec(value);

			satisfiesConstraint = exec != null;
		} else {
			satisfiesConstraint = constraintValue.equals(value);
		}

		return satisfiesConstraint;
	}
}
