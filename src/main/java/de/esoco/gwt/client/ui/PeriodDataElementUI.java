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
	protected Component createInputUI(ContainerBuilder<?> rBuilder,
		StyleData rStyle, PeriodDataElement rDataElement) {
		rBuilder = rBuilder.addPanel(rStyle, new FlowLayout(true));

		Container aPanel = rBuilder.getContainer();

		rBuilder.addSpinner(PERIOD_COUNT_STYLE, 1, 1000, 1);
		rBuilder.addListBox(PERIOD_UNIT_STYLE);
		transferDataElementValueToComponent(rDataElement, aPanel);

		return aPanel;
	}

	@Override
	protected void transferDataElementValueToComponent(
		PeriodDataElement rElement, Component rComponent) {
		List<Component> rComponents = ((Panel) rComponent).getComponents();
		Spinner aSpinner = (Spinner) rComponents.get(0);
		ListBox rComboBox = (ListBox) rComponents.get(1);

		@SuppressWarnings("unchecked")
		List<String> rUnits = (List<String>) rElement.getAllowedValues();

		for (String sUnit : rUnits) {
			rComboBox.add(sUnit);
		}

		aSpinner.setValue(rElement.getPeriodCount());
		rComboBox.setSelection(rUnits.indexOf(rElement.getPeriodUnit()));
	}

	@Override
	protected void transferInputToDataElement(Component rComponent,
		PeriodDataElement rElement) {
		List<Component> rComponents = ((Panel) rComponent).getComponents();

		int nCount = ((Spinner) rComponents.get(0)).getValue();
		String sUnit = ((ListBox) rComponents.get(1)).getSelectedItem();

		rElement.setPeriodCount(nCount);
		rElement.setPeriodUnit(sUnit);
	}
}
