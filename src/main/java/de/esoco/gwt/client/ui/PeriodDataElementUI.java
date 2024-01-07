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

import de.esoco.data.element.PeriodDataElement;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.ListBox;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.component.Spinner;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.res.EsocoGwtCss;
import de.esoco.gwt.client.res.EsocoGwtResources;

import java.util.List;

import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;

/**
 * The user interface implementation for date data elements.
 *
 * @author eso
 */
public class PeriodDataElementUI extends DataElementUI<PeriodDataElement> {

	/**
	 * Shortcut constant to access the framework CSS
	 */
	static final EsocoGwtCss CSS = EsocoGwtResources.INSTANCE.css();

	/**
	 * Style constant for the period count input field.
	 */
	private static final StyleData PERIOD_COUNT_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gfPeriodCount());

	/**
	 * Style constant for the period unit input field.
	 */
	private static final StyleData PERIOD_UNIT_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gfPeriodUnit());

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData style, PeriodDataElement dataElement) {
		builder = builder.addPanel(style, new FlowLayout(true));

		Container panel = builder.getContainer();

		builder.addSpinner(PERIOD_COUNT_STYLE, 1, 1000, 1);
		builder.addListBox(PERIOD_UNIT_STYLE);
		transferDataElementValueToComponent(dataElement, panel);

		return panel;
	}

	@Override
	protected void transferDataElementValueToComponent(
		PeriodDataElement element, Component component) {
		List<Component> components = ((Panel) component).getComponents();
		Spinner spinner = (Spinner) components.get(0);
		ListBox comboBox = (ListBox) components.get(1);

		@SuppressWarnings("unchecked")
		List<String> units = (List<String>) element.getAllowedValues();

		for (String unit : units) {
			comboBox.add(unit);
		}

		spinner.setValue(element.getPeriodCount());
		comboBox.setSelection(units.indexOf(element.getPeriodUnit()));
	}

	@Override
	protected void transferInputToDataElement(Component component,
		PeriodDataElement element) {
		List<Component> components = ((Panel) component).getComponents();

		int count = ((Spinner) components.get(0)).getValue();
		String unit = ((ListBox) components.get(1)).getSelectedItem();

		element.setPeriodCount(count);
		element.setPeriodUnit(unit);
	}
}
