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

	private final transient UserInterfaceContext context;

	private final String resourcePrefix;

	private final DataElementList modelElement;

	private DataElementListModel parent = null;

	private String displayString = null;

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
	 * @param context        The user interface context for resource lookups
	 *                       (may be NULL if no resource access is needed)
	 * @param modelElement   The list of root elements to create this model
	 *                       from
	 * @param elementNames   A list of the names of elements in the list to be
	 *                       included in this model or NULL for all
	 * @param resourcePrefix The prefix to be prepended to all model strings
	 *                       that are used for resource lookups
	 * @param listsOnly      TRUE if only element lists shall be added to the
	 *                       model, but no other child elements; FALSE to
	 *                       include the full element hierarchy
	 */
	public DataElementListModel(UserInterfaceContext context,
		DataElementList modelElement, List<String> elementNames,
		String resourcePrefix, boolean listsOnly) {
		super(modelElement.getName());

		this.context = context;
		this.modelElement = modelElement;
		this.resourcePrefix = resourcePrefix;

		setData(createModelData(modelElement, elementNames, listsOnly));
	}

	/**
	 * Returns the user interface context that is used for resource lookups.
	 *
	 * @return The user interface context or NULL if not set
	 */
	public final UserInterfaceContext getContext() {
		return context;
	}

	/**
	 * Returns the model element.
	 *
	 * @return The model element
	 */
	public DataElementList getModelElement() {
		return modelElement;
	}

	/**
	 * Returns the parent data model of this instance. The parent will be NULL
	 * if this model is the root of a hierarchy or if this is only a single,
	 * flat data model.
	 *
	 * @return The parent data model or NULL for none
	 */
	public final DataElementListModel getParent() {
		return parent;
	}

	/**
	 * Returns the string representation of the wrapped data element list.
	 *
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		if (displayString == null) {
			DataElement<?> modelElement = getModelElement();

			displayString = modelElement.getResourceId();

			if (context != null) {
				displayString =
					context.expandResource(resourcePrefix + displayString);
			}
		}

		return displayString;
	}

	/**
	 * Creates a list containing the model data from a list of data elements
	 *
	 * @param elements  The list of data elements
	 * @param listsOnly TRUE if only data element lists shall be added
	 * @return A new list containing the model data
	 */
	private List<Object> createModelData(DataElementList elements,
		boolean listsOnly) {
		ArrayList<Object> data =
			new ArrayList<Object>(elements.getElementCount());

		for (DataElement<?> element : elements) {
			Object value = createModelValue(element, listsOnly);

			if (value != null) {
				data.add(value);
			}
		}

		return data;
	}

	/**
	 * Creates a list containing the model data from a list of data elements,
	 * limited to a certain set of element names.
	 *
	 * @param elements  The list of data elements
	 * @param names     A list of the element names to be included in the model
	 *                  data
	 * @param listsOnly TRUE if only data element lists shall be added
	 * @return A new list containing the model data
	 */
	private List<Object> createModelData(DataElementList elements,
		List<String> names, boolean listsOnly) {
		List<Object> dataList;

		if (names == null) {
			dataList = createModelData(elements, listsOnly);
		} else {
			int count = names.size();

			dataList = new ArrayList<Object>(count);

			for (int i = 0; i < count; i++) {
				DataElement<?> element = elements.getElementAt(names.get(i));

				if (element != null) {
					Object value = createModelValue(element, listsOnly);

					if (value != null) {
						dataList.add(value);
					}
				} else {
					throw new IllegalArgumentException(
						"Unknown attribute: " + names.get(i));
				}
			}
		}

		return dataList;
	}

	/**
	 * Helper method to create a model data value from a data element.
	 *
	 * @param element   The data element to convert
	 * @param listsOnly TRUE if only data element lists shall be converted
	 * @return The resulting value or NULL for none
	 */
	private Object createModelValue(DataElement<?> element,
		boolean listsOnly) {
		Object value = null;

		if (element instanceof DataElementList) {
			DataElementListModel childModel =
				new DataElementListModel(context, (DataElementList) element,
					null, resourcePrefix, listsOnly);

			childModel.parent = this;
			value = childModel;
		} else if (!listsOnly) {
			Object elementValue = element.getValue();

			if (elementValue != null) {
				value = elementValue.toString();
			}
		}

		return value;
	}
}
