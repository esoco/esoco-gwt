//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.UserInterfaceContext;

import de.esoco.lib.model.ListDataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of an EWT data model based on a list of data elements.
 *
 * @author eso
 */
public class DataElementListModel extends ListDataModel<Object> {

	private static final long serialVersionUID = 1L;

	private final UserInterfaceContext rContext;

	private final String sResourcePrefix;

	private DataElementList rModelElement;

	private DataElementListModel rParent = null;

	private String sDisplayString = null;

	/**
	 * Recursively creates a new instance from a list of root data elements.
	 * The
	 * boolean parameter allows to control which elements from the list will be
	 * added to this model. If TRUE, only the data element lists will be added
	 * to this model, omitting any detail information from the other data
	 * elements in the hierarchy. If FALSE, all data elements will be available
	 * from this model instance.
	 *
	 * <p>The attributes parameter allows to add only certain attributes of the
	 * data elements to this model. The attributes list must contain valid
	 * attribute names for the given data elements.</p>
	 *
	 * @param rContext        The user interface context for resource lookups
	 *                        (may be NULL if no resource access is needed)
	 * @param rModelElement   The list of root elements to create this model
	 *                        from
	 * @param rElementNames   A list of the names of elements in the list to be
	 *                        included in this model or NULL for all
	 * @param sResourcePrefix The prefix to be prepended to all model strings
	 *                        that are used for resource lookups
	 * @param bListsOnly      TRUE if only element lists shall be added to the
	 *                        model, but no other child elements; FALSE to
	 *                        include the full element hierarchy
	 */
	public DataElementListModel(UserInterfaceContext rContext,
		DataElementList rModelElement, List<String> rElementNames,
		String sResourcePrefix, boolean bListsOnly) {
		super(rModelElement.getName());

		this.rContext = rContext;
		this.rModelElement = rModelElement;
		this.sResourcePrefix = sResourcePrefix;

		setData(createModelData(rModelElement, rElementNames, bListsOnly));
	}

	/**
	 * Returns the user interface context that is used for resource lookups.
	 *
	 * @return The user interface context or NULL if not set
	 */
	public final UserInterfaceContext getContext() {
		return rContext;
	}

	/**
	 * Returns the model element.
	 *
	 * @return The model element
	 */
	public DataElementList getModelElement() {
		return rModelElement;
	}

	/**
	 * Returns the parent data model of this instance. The parent will be NULL
	 * if this model is the root of a hierarchy or if this is only a single,
	 * flat data model.
	 *
	 * @return The parent data model or NULL for none
	 */
	public final DataElementListModel getParent() {
		return rParent;
	}

	/**
	 * Returns the string representation of the wrapped data element list.
	 *
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		if (sDisplayString == null) {
			DataElement<?> rModelElement = getModelElement();

			sDisplayString = rModelElement.getResourceId();

			if (rContext != null) {
				sDisplayString =
					rContext.expandResource(sResourcePrefix + sDisplayString);
			}
		}

		return sDisplayString;
	}

	/**
	 * Creates a list containing the model data from a list of data elements
	 *
	 * @param rElements  The list of data elements
	 * @param bListsOnly TRUE if only data element lists shall be added
	 * @return A new list containing the model data
	 */
	private List<Object> createModelData(DataElementList rElements,
		boolean bListsOnly) {
		ArrayList<Object> aData =
			new ArrayList<Object>(rElements.getElementCount());

		for (DataElement<?> rElement : rElements) {
			Object rValue = createModelValue(rElement, bListsOnly);

			if (rValue != null) {
				aData.add(rValue);
			}
		}

		return aData;
	}

	/**
	 * Creates a list containing the model data from a list of data elements,
	 * limited to a certain set of element names.
	 *
	 * @param rElements  The list of data elements
	 * @param rNames     A list of the element names to be included in the
	 *                      model
	 *                   data
	 * @param bListsOnly TRUE if only data element lists shall be added
	 * @return A new list containing the model data
	 */
	private List<Object> createModelData(DataElementList rElements,
		List<String> rNames, boolean bListsOnly) {
		List<Object> aDataList;

		if (rNames == null) {
			aDataList = createModelData(rElements, bListsOnly);
		} else {
			int nCount = rNames.size();

			aDataList = new ArrayList<Object>(nCount);

			for (int i = 0; i < nCount; i++) {
				DataElement<?> rElement =
					rElements.getElementAt(rNames.get(i));

				if (rElement != null) {
					Object rValue = createModelValue(rElement, bListsOnly);

					if (rValue != null) {
						aDataList.add(rValue);
					}
				} else {
					throw new IllegalArgumentException(
						"Unknown attribute: " + rNames.get(i));
				}
			}
		}

		return aDataList;
	}

	/**
	 * Helper method to create a model data value from a data element.
	 *
	 * @param rElement   The data element to convert
	 * @param bListsOnly TRUE if only data element lists shall be converted
	 * @return The resulting value or NULL for none
	 */
	private Object createModelValue(DataElement<?> rElement,
		boolean bListsOnly) {
		Object rValue = null;

		if (rElement instanceof DataElementList) {
			DataElementListModel rChildModel =
				new DataElementListModel(rContext, (DataElementList) rElement,
					null, sResourcePrefix, bListsOnly);

			rChildModel.rParent = this;
			rValue = rChildModel;
		} else if (!bListsOnly) {
			Object rElementValue = rElement.getValue();

			if (rElementValue != null) {
				rValue = rElementValue.toString();
			}
		}

		return rValue;
	}
}
