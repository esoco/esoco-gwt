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

import de.esoco.data.element.IntegerDataElement;
import de.esoco.data.validate.IntegerRangeValidator;
import de.esoco.data.validate.Validator;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.ProgressBar;
import de.esoco.ewt.component.Spinner;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.ContentType;

import com.google.gwt.user.client.Timer;

import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.ContentProperties.INPUT_CONSTRAINT;
import static de.esoco.lib.property.StateProperties.AUTO_UPDATE_INCREMENT;
import static de.esoco.lib.property.StateProperties.AUTO_UPDATE_INTERVAL;

/**
 * The user interface implementation for integer data elements.
 *
 * @author eso
 */
public class IntegerDataElementUI extends DataElementUI<IntegerDataElement> {

	/**
	 * @see DataElementUI#DataElementUI()
	 */
	public IntegerDataElementUI() {
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> builder,
		StyleData style, IntegerDataElement dataElement) {
		Component component;

		if (dataElement.getProperty(CONTENT_TYPE, null) ==
			ContentType.PROGRESS) {
			ProgressBar progressBar = builder.addProgressBar(style);
			Validator<?> validator = dataElement.getValidator();

			if (validator instanceof IntegerRangeValidator) {
				IntegerRangeValidator range =
					(IntegerRangeValidator) validator;
				Integer value = dataElement.getValue();

				progressBar.setMinimum(range.getMinimum());
				progressBar.setMaximum(range.getMaximum());
				progressBar.setValue(value != null ? value.intValue() : 0);

				checkSetupAutoUpdate(progressBar, dataElement);
			}

			component = progressBar;
		} else {
			component = super.createDisplayUI(builder, style, dataElement);
		}

		return component;
	}

	@Override
	@SuppressWarnings("boxing")
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData style, IntegerDataElement dataElement) {
		Validator<?> validator = dataElement.getValidator();
		Component component;

		if (validator instanceof IntegerRangeValidator) {
			IntegerRangeValidator range = (IntegerRangeValidator) validator;

			Spinner spinner = builder.addSpinner(style, range.getMinimum(),
				range.getMaximum(), 1);

			if (dataElement.getValue() != null) {
				spinner.setValue(dataElement.getValue());
			}

			component = spinner;
		} else {
			dataElement.setProperty(INPUT_CONSTRAINT, "-?\\d*");
			component = super.createInputUI(builder, style, dataElement);
		}

		return component;
	}

	/**
	 * @see DataElementUI#transferDataElementValueToComponent(de.esoco.data.element.DataElement,
	 * Component)
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void transferDataElementValueToComponent(
		IntegerDataElement element, Component component) {
		Integer value = element.getValue();

		if (component instanceof Spinner) {
			((Spinner) component).setValue(value != null ? value : 0);
		} else if (component instanceof ProgressBar) {
			((ProgressBar) component).setValue(value != null ? value : 0);
		} else {
			super.transferDataElementValueToComponent(element, component);
		}
	}

	@Override
	@SuppressWarnings("boxing")
	protected void transferInputToDataElement(Component component,
		IntegerDataElement dataElement) {
		if (component instanceof Spinner) {
			dataElement.setValue(((Spinner) component).getValue());
		} else {
			super.transferInputToDataElement(component, dataElement);
		}
	}

	/**
	 * Checks whether the given progress bar data element should be updated
	 * automatically.
	 *
	 * @param progressBar The progress bar component
	 * @param dataElement The associated data element
	 */
	private void checkSetupAutoUpdate(ProgressBar progressBar,
		IntegerDataElement dataElement) {
		int increment = dataElement.getIntProperty(AUTO_UPDATE_INCREMENT, 0);
		int interval = dataElement.getIntProperty(AUTO_UPDATE_INTERVAL, -1);

		if (increment != 0 && interval > 0) {
			Timer timer = new Timer() {
				@Override
				public void run() {
					int current = progressBar.getValue();

					if ((increment > 0 && current < progressBar.getMaximum()) ||
						(increment < 0 && current > progressBar.getMinimum())) {
						progressBar.setValue(current + increment);
					} else {
						cancel();
					}
				}
			};
			timer.scheduleRepeating(interval);
		}
	}
}
