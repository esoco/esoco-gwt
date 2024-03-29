//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.data.element.BigDecimalDataElement;
import de.esoco.data.element.BooleanDataElement;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.DateDataElement;
import de.esoco.data.element.IntegerDataElement;
import de.esoco.data.element.PeriodDataElement;
import de.esoco.data.element.SelectionDataElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.element.StringListDataElement;

import de.esoco.ewt.EWT;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The central factory for new instances of {@link DataElementUI}.
 *
 * @author eso
 */
public class DataElementUIFactory {

	private static Map<String, Function<DataElement<?>, DataElementUI<?>>>
		dataElementRegistry = new HashMap<>();

	static {
		registerDataElementUI(DataElement.class,
			DataElementUIFactory::createDefaultUI);
		registerDataElementUI(StringDataElement.class,
			DataElementUIFactory::createDefaultUI);
		registerDataElementUI(StringListDataElement.class,
			DataElementUIFactory::createDefaultUI);
		registerDataElementUI(DataElementList.class,
			e -> new DataElementListUI());
		registerDataElementUI(BooleanDataElement.class,
			e -> new BooleanDataElementUI());
		registerDataElementUI(IntegerDataElement.class,
			e -> new IntegerDataElementUI());
		registerDataElementUI(BigDecimalDataElement.class,
			e -> new BigDecimalDataElementUI());
		registerDataElementUI(DateDataElement.class,
			e -> new DateDataElementUI());
		registerDataElementUI(PeriodDataElement.class,
			e -> new PeriodDataElementUI());
		registerDataElementUI(SelectionDataElement.class,
			e -> new SelectionDataElementUI());
	}

	/**
	 * Private, only static use.
	 */
	private DataElementUIFactory() {
	}

	/**
	 * A factory method that creates a new data element user interface object
	 * for a certain data element.
	 *
	 * @param panelManager The parent panel manager of the new element UI
	 * @param element      The data element to create the user interface for
	 * @return An instance of a subclass for the given data element
	 * @throws IllegalArgumentException If no matching UI could be created
	 */
	@SuppressWarnings("unchecked")
	public static <T, D extends DataElement<T>> DataElementUI<?> create(
		DataElementPanelManager panelManager, D element) {
		DataElementUI<?> uI = null;

		Function<DataElement<?>, DataElementUI<?>> uiFactory =
			dataElementRegistry.get(element.getClass().getName());

		if (uiFactory == null) {
			EWT.log("No UI factory for " + element.getClass());
			uiFactory = dataElementRegistry.get(DataElement.class.getName());
		}

		if (uiFactory != null) {
			uI = uiFactory.apply(element);
		}

		if (uI == null) {
			throw new IllegalArgumentException(
				"No UI for data element " + element);
		}

		((DataElementUI<D>) uI).init(panelManager, element);

		return uI;
	}

	/**
	 * Internal generically typed method to create the default UI for a data
	 * element that has no specific factory.
	 *
	 * @param element The element to create the UI for
	 * @return The new default UI
	 */
	@SuppressWarnings("unchecked")
	static <D extends DataElement<?>> DataElementUI<D> createDefaultUI(
		D element) {
		DataElementUI<D> uI;

		if (element.getAllowedValues() != null) {
			uI = (DataElementUI<D>) new ValueListDataElementUI();
		} else {
			uI = (DataElementUI<D>) new DataElementUI<DataElement<?>>();
		}

		return uI;
	}

	/**
	 * Returns the registered data element UI factory for a certain data
	 * element
	 * type. The returned value can be used to create UI factories with a
	 * fallback on previously registered UIs.
	 *
	 * @param dataElementClass The data element type class
	 * @return The factory function for the type or NULL if none has been
	 * registered so far
	 */
	@SuppressWarnings("unchecked")
	public static <D extends DataElement<?>, U extends DataElementUI<D>> Function<D, U> getRegisteredUI(
		Class<D> dataElementClass) {
		return (Function<D, U>) dataElementRegistry.get(
			dataElementClass.getName());
	}

	/**
	 * Registers a data element UI factory for a certain data element type.
	 * This
	 * will replace any previously registered factory if such exists. If an
	 * existing factory should be extended instead the original can be queried
	 * with {@link #getRegisteredUI(Class)}.
	 *
	 * @param dataElementClass The data element type class
	 * @param factory          The factory function for the type
	 */
	@SuppressWarnings("unchecked")
	public static <D extends DataElement<?>, U extends DataElementUI<D>> void registerDataElementUI(
		Class<D> dataElementClass, Function<D, U> factory) {
		dataElementRegistry.put(dataElementClass.getName(),
			(Function<DataElement<?>, DataElementUI<?>>) factory);
	}
}
