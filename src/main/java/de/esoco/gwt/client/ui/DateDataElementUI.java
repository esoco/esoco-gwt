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
import de.esoco.data.element.DateDataElement;
import de.esoco.data.element.DateDataElement.DateInputType;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Calendar;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.DateField;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;

import de.esoco.lib.property.ContentType;
import de.esoco.lib.property.DateAttribute;
import de.esoco.lib.property.InteractionEventType;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static de.esoco.data.element.DateDataElement.DATE_INPUT_TYPE;

import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.LayoutProperties.COLUMNS;

/**
 * The user interface implementation for date data elements.
 *
 * @author eso
 */
public class DateDataElementUI extends DataElementUI<DateDataElement> {

	private Map<Date, String> calendarEvents = Collections.emptyMap();

	/**
	 * @see DataElementUI#convertValueToString(DataElement, Object)
	 */
	@Override
	protected String convertValueToString(DataElement<?> dataElement,
		Object value) {
		return value != null ?
		       super.convertValueToString(dataElement, value) :
		       " ";
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData inputStyle, DateDataElement dataElement) {
		Date date = dataElement.getValue();
		Component component;

		DateInputType dateInputType =
			dataElement.getProperty(DATE_INPUT_TYPE, null);

		if (dataElement.getProperty(CONTENT_TYPE, null) ==
			ContentType.DATE_TIME) {
			inputStyle = inputStyle.setFlags(StyleFlag.DATE_TIME);
		}

		if (dateInputType == DateInputType.INPUT_FIELD) {
			DateField dateField = builder.addDateField(inputStyle, date);

			dateField.setColumns(dataElement.getIntProperty(COLUMNS, 10));
			component = dateField;
		} else {
			Calendar calendar = builder.addCalendar(inputStyle, date);

			updateCalendar(calendar, dataElement);
			component = calendar;
		}

		return component;
	}

	@Override
	protected DataElementInteractionHandler<DateDataElement> createInteractionHandler(
		DataElementPanelManager panelManager, DateDataElement dataElement) {
		return new DateInteractionHandler(panelManager, dataElement);
	}

	@Override
	protected void transferDataElementValueToComponent(DateDataElement element,
		Component component) {
		if (component instanceof DateAttribute) {
			((DateAttribute) component).setDate(element.getValue());

			if (component instanceof Calendar) {
				updateCalendar((Calendar) component, element);
			}
		} else {
			super.transferDataElementValueToComponent(element, component);
		}
	}

	@Override
	protected void transferInputToDataElement(Component component,
		DateDataElement element) {
		if (component instanceof DateAttribute) {
			element.setValue(((DateAttribute) component).getDate());
		} else {
			super.transferInputToDataElement(component, element);
		}
	}

	/**
	 * Sets the highlights on a calendar component from the properties of a
	 * date
	 * data element.
	 *
	 * @param calendar    The calendar component
	 * @param dataElement The date data element
	 */
	private void updateCalendar(Calendar calendar,
		DateDataElement dataElement) {
		for (Entry<Date, String> entry : calendarEvents.entrySet()) {
			Date date = entry.getKey();
			String style = entry.getValue();

			calendar.removeDateStyle(date, style);
		}

		calendarEvents =
			dataElement.getProperty(DateDataElement.DATE_HIGHLIGHTS,
				Collections.<Date, String>emptyMap());

		for (Entry<Date, String> entry : calendarEvents.entrySet()) {
			Date date = entry.getKey();
			String style = entry.getValue();

			calendar.addDateStyle(date, style);
		}
	}

	/**
	 * A date-specific interaction handler subclass.
	 *
	 * @author eso
	 */
	static class DateInteractionHandler
		extends DataElementInteractionHandler<DateDataElement> {

		/**
		 * @see DataElementInteractionHandler#DataElementInteractionHandler(DataElementPanelManager,
		 * DataElement)
		 */
		public DateInteractionHandler(DataElementPanelManager panelManager,
			DateDataElement dataElement) {
			super(panelManager, dataElement);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Set<EventType> getInteractionEventTypes(Component component,
			Set<InteractionEventType> interactionEventTypes) {
			return EnumSet.of(EventType.VALUE_CHANGED);
		}
	}
}
