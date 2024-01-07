//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.data.element.BooleanDataElement;

import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.CheckBox;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.UserInterfaceProperties;

import static de.esoco.lib.property.ContentProperties.LABEL;

/**
 * The user interface implementation for boolean data elements.
 *
 * @author eso
 */
public class BooleanDataElementUI extends DataElementUI<BooleanDataElement> {

	/**
	 * Overridden to create an empty label because the label is displayed as
	 * the
	 * checkbox text. Only if a label has explicitly been set with the user
	 * interface property {@link UserInterfaceProperties#LABEL} an additional
	 * label will be displayed with the label text.
	 *
	 * @see DataElementUI#getElementLabelText(UserInterfaceContext)
	 */
	@Override
	public String getElementLabelText(UserInterfaceContext rContext) {
		return getDataElement().getProperty(LABEL, "");
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> rBuilder,
		StyleData rDisplayStyle, BooleanDataElement rDataElement) {
		CheckBox rCheckBox =
			createCheckBox(rBuilder, rDisplayStyle, rDataElement);

		// both methods must be invoked because DataElementUI.setEnabled()
		// doesn't know the check box component yet
		rCheckBox.setEnabled(false);
		setEnabled(false);

		return rCheckBox;
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> rBuilder,
		StyleData rInputStyle, BooleanDataElement rDataElement) {
		return createCheckBox(rBuilder, rInputStyle, rDataElement);
	}

	/**
	 * Overridden to do nothing because the label text is displayed as the
	 * checkbox text.
	 *
	 * @see DataElementUI#setHiddenLabelHint(UserInterfaceContext)
	 */
	@Override
	protected void setHiddenLabelHint(UserInterfaceContext rContext) {
	}

	@Override
	protected void transferDataElementValueToComponent(
		BooleanDataElement rDataElement, Component rComponent) {
		((CheckBox) rComponent).setSelected(
			Boolean.TRUE.equals(rDataElement.getValue()));
	}

	@Override
	protected void transferInputToDataElement(Component rComponent,
		BooleanDataElement rElement) {
		rElement.setValue(
			Boolean.valueOf(((CheckBox) rComponent).isSelected()));
	}

	/**
	 * Creates check box to represent the data element's state.
	 *
	 * @param rBuilder     The builder to create the check box with
	 * @param rStyle       The style data
	 * @param rDataElement The data element
	 * @return The new check box
	 */
	private CheckBox createCheckBox(ContainerBuilder<?> rBuilder,
		StyleData rStyle, BooleanDataElement rDataElement) {
		String sLabel = getLabelText(rBuilder.getContext(), rDataElement,
			LABEL_RESOURCE_PREFIX);

		CheckBox aCheckBox = rBuilder.addCheckBox(rStyle, sLabel, null);
		Boolean rValue = rDataElement.getValue();

		if (rValue != null) {
			aCheckBox.setSelected(rValue.booleanValue());
		}

		return aCheckBox;
	}
}
