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

import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.FilterableDataModel;
import de.esoco.lib.model.ListDataModel;
import de.esoco.lib.model.SortableDataModel;
import de.esoco.lib.property.SortDirection;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

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

	HashMap<String, SortDirection> aColumnSorting =
		new LinkedHashMap<String, SortDirection>();

	HashMap<String, String> aFilters = new LinkedHashMap<String, String>();

	private List<T> rData;

	private List<T> aDataCopy;

	private List<ColumnDefinition> rColumns;

	private boolean bNewFilters;

	private List<String> aFieldIds = new ArrayList<String>();

	private RegExp rConstraintPattern = RegExp.compile(
		"([" + FilterableDataModel.CONSTRAINT_OR_PREFIX +
			FilterableDataModel.CONSTRAINT_AND_PREFIX + "])([" +
			FilterableDataModel.CONSTRAINT_COMPARISON_CHARS + "])(.*)");

	/**
	 * Creates a new instance.
	 *
	 * @param sName    The name of this model.
	 * @param rData    The model's data
	 * @param rColumns The columns definitions
	 */
	public FilterableListDataModel(String sName, List<T> rData,
		List<ColumnDefinition> rColumns) {
		super(sName, rData);

		this.rData = rData;
		this.aDataCopy = new ArrayList<T>(rData);
		this.rColumns = rColumns;

		for (ColumnDefinition rColumnDefinition : rColumns) {
			aFieldIds.add(rColumnDefinition.getId());
		}
	}

	@Override
	public T getElement(int nIndex) {
		applyConstraints();

		return super.getElement(nIndex);
	}

	@Override
	public int getElementCount() {
		applyConstraints();

		return super.getElementCount();
	}

	@Override
	public String getFilter(String sFieldId) {
		return aFilters.get(sFieldId);
	}

	@Override
	public Map<String, String> getFilters() {
		return aFilters;
	}

	@Override
	public SortDirection getSortDirection(String sFieldId) {
		SortDirection eSortDirection = aColumnSorting.get(sFieldId);

		return eSortDirection != null ? eSortDirection : null;
	}

	@Override
	public void removeAllFilters() {
		aFilters.clear();
		bNewFilters = true;
	}

	@Override
	public void removeSorting() {
		aColumnSorting.clear();
		bNewFilters = true;
	}

	@Override
	public void setFilter(String sFieldId, String sFilter) {
		if (sFilter != null && !sFilter.isEmpty()) {
			aFilters.put(sFieldId, sFilter);
		} else {
			aFilters.remove(sFieldId);
		}

		bNewFilters = true;
	}

	@Override
	public void setFilters(Map<String, String> rFilters) {
		aFilters.clear();
		aFilters.putAll(rFilters);
		bNewFilters = true;
	}

	@Override
	public void setSortDirection(String sFieldId, SortDirection rMode) {
		if (rMode != null) {
			aColumnSorting.put(sFieldId, rMode);
		} else {
			aColumnSorting.remove(sFieldId);
		}

		bNewFilters = true;
	}

	/**
	 * Applies the defined constraints for sorting and filtering. This
	 * effectively sets the elements and their order in the data model.
	 */
	private void applyConstraints() {
		if (bNewFilters) {
			performFiltering();
			performSorting();
			bNewFilters = false;
		}
	}

	/**
	 * Applies all the filtering constraints to the data.
	 */
	private void performFiltering() {
		rData.clear();

		List<T> aAllData = new ArrayList<T>(aDataCopy);

		for (T rT : aAllData) {
			boolean bSatisfies = aFilters.isEmpty();
			Set<String> sFieldIds = aFilters.keySet();

			int nIndex = 0;

			for (String sFieldId : sFieldIds) {
				String sValue = rT.getElement(aFieldIds.indexOf(sFieldId));

				String sConstraints = aFilters.get(sFieldId);

				boolean bAttrOr = sConstraints.charAt(0) ==
					FilterableDataModel.CONSTRAINT_OR_PREFIX;

				boolean bSatisfiesConstraints =
					satisfiesConstraints(sConstraints, sValue);

				if (nIndex == 0) {
					bSatisfies = bSatisfiesConstraints;
				} else {
					if (bAttrOr) {
						bSatisfies = bSatisfiesConstraints || bSatisfies;
					} else {
						bSatisfies = bSatisfiesConstraints && bSatisfies;
					}
				}

				nIndex++;
			}

			if (bSatisfies) {
				rData.add(rT);
			}
		}
	}

	/**
	 * Performs the sorting according to the sorting preferences.
	 */

	private void performSorting() {
		if (aColumnSorting.size() > 0) {
			Set<String> sFieldIds = aColumnSorting.keySet();

			for (final String sFielId : sFieldIds) {
				final SortDirection eSortDirection =
					aColumnSorting.get(sFielId);

				final int nFieldIndex = aFieldIds.indexOf(sFielId);

				final ColumnDefinition rColumnDefinition =
					rColumns.get(nFieldIndex);

				Collections.sort(rData, new Comparator<DataModel<String>>() {
					@Override
					public int compare(DataModel<String> rDataModel,
						DataModel<String> rDataModelCmp) {
						String sValue = rDataModel.getElement(nFieldIndex);

						String sCompareValue =
							rDataModelCmp.getElement(nFieldIndex);

						int nResult = 0;

						if (eSortDirection == SortDirection.DESCENDING) {
							nResult = compareFieldValues(sCompareValue,
								sValue);
						} else {
							nResult = compareFieldValues(sValue,
								sCompareValue);
						}

						return nResult;
					}

					private int compareFieldValues(String sValue,
						String sCompareValue) {
						int nResult = 0;

						if (rColumnDefinition
							.getDatatype()
							.equals(Integer.class.getName())) {
							nResult = Integer
								.valueOf(sValue)
								.compareTo(Integer.valueOf(sCompareValue));
						} else {
							nResult = sValue.compareTo(sCompareValue);
						}

						return nResult;
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
	 * constraint value. The regular expression {@link #rConstraintPattern} is
	 * used to retrieve the three parts from the string elements.
	 *
	 * @param sConstraints The search constraints in string representation
	 * @param sValue       a value to be checked.
	 * @return TRUE if a given value satisfies the given search constraints
	 * FALSE otherwise.
	 */
	private boolean satisfiesConstraints(String sConstraints, String sValue) {
		boolean bSatisfies = true;

		String[] rContraints =
			sConstraints.split(FilterableDataModel.CONSTRAINT_SEPARATOR);

		for (int i = 0; i < rContraints.length; i++) {
			String sConstraint = rContraints[i];

			MatchResult rConstraintMatcher =
				rConstraintPattern.exec(sConstraint);

			if (rConstraintMatcher != null) {
				String sPrefix = rConstraintMatcher.getGroup(1);
				String sComparison = rConstraintMatcher.getGroup(2);
				String sConstraintValue = rConstraintMatcher.getGroup(3);

				boolean bOr = sPrefix.equals(
					String.valueOf(FilterableDataModel.CONSTRAINT_OR_PREFIX));

				boolean bSatisfiesConstraint = false;

				if (sConstraintValue != null && sValue != null) {
					switch (sComparison) {
						case "=":
							bSatisfiesConstraint =
								satisfiesEquals(sConstraintValue, sValue);

							break;

						case "\u2260": // !=
							bSatisfiesConstraint =
								!sConstraintValue.equals(sValue);
							break;

						case "~":

							String[] rConstraintItems =
								sConstraintValue.split(",");

							for (String sConstraintItem : rConstraintItems) {
								bSatisfiesConstraint |=
									satisfiesEquals(sConstraintItem, sValue);
							}

							break;

						case "<":
							bSatisfiesConstraint =
								sConstraintValue.compareTo(sValue) > 0;
							break;

						case ">":
							bSatisfiesConstraint =
								sConstraintValue.compareTo(sValue) < 0;
							break;

						case "\u2264": // <=
							bSatisfiesConstraint =
								sConstraintValue.compareTo(sValue) >= 0;
							break;

						case "\u2265": // >=
							bSatisfiesConstraint =
								sConstraintValue.compareTo(sValue) <= 0;
							break;
					}
				}

				if (i == 0) {
					bSatisfies = bSatisfies && bSatisfiesConstraint;
				} else {
					if (bOr) {
						bSatisfies = bSatisfies || bSatisfiesConstraint;
					} else {
						bSatisfies = bSatisfies && bSatisfiesConstraint;
					}
				}
			}
		}

		return bSatisfies;
	}

	/**
	 * Checks whether the given value satisfies the given constraint value when
	 * the equals "=" operator is applied. This check also respects wildcards
	 * (*) in the constraint value.
	 *
	 * @param sConstraintValue The search constraint value
	 * @param sValue           The value to check for a match.
	 * @return Whether the given value satisfies the given constraint value
	 * when
	 * the equals "=" operator is applied.
	 */
	private boolean satisfiesEquals(String sConstraintValue, String sValue) {
		boolean bSatisfiesConstraint = false;

		if (sConstraintValue.contains("*")) {
			String sFlags = "";

			if (RegExp
				.compile("[A-Z]+")
				.exec(sConstraintValue.replaceAll("\\*", "")) == null) {
				sFlags += "i";
			}

			RegExp rCompile =
				RegExp.compile("^" + sConstraintValue.replaceAll("\\*", ".*"),
					sFlags);
			MatchResult rExec = rCompile.exec(sValue);

			bSatisfiesConstraint = rExec != null;
		} else {
			bSatisfiesConstraint = sConstraintValue.equals(sValue);
		}

		return bSatisfiesConstraint;
	}
}
