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
	public String getElementLabelText(UserInterfaceContext context) {
		return getDataElement().getProperty(LABEL, "");
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> builder,
		StyleData displayStyle, BooleanDataElement dataElement) {
		CheckBox checkBox = createCheckBox(builder, displayStyle, dataElement);

		// both methods must be invoked because DataElementUI.setEnabled()
		// doesn't know the check box component yet
		checkBox.setEnabled(false);
		setEnabled(false);

		return checkBox;
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData inputStyle, BooleanDataElement dataElement) {
		return createCheckBox(builder, inputStyle, dataElement);
	}

	/**
	 * Overridden to do nothing because the label text is displayed as the
	 * checkbox text.
	 *
	 * @see DataElementUI#setHiddenLabelHint(UserInterfaceContext)
	 */
	@Override
	protected void setHiddenLabelHint(UserInterfaceContext context) {
	}

	@Override
	protected void transferDataElementValueToComponent(
		BooleanDataElement dataElement, Component component) {
		((CheckBox) component).setSelected(
			Boolean.TRUE.equals(dataElement.getValue()));
	}

	@Override
	protected void transferInputToDataElement(Component component,
		BooleanDataElement element) {
		element.setValue(Boolean.valueOf(((CheckBox) component).isSelected()));
	}

	/**
	 * Creates check box to represent the data element's state.
	 *
	 * @param builder     The builder to create the check box with
	 * @param style       The style data
	 * @param dataElement The data element
	 * @return The new check box
	 */
	private CheckBox createCheckBox(ContainerBuilder<?> builder,
		StyleData style, BooleanDataElement dataElement) {
		String label = getLabelText(builder.getContext(), dataElement,
			LABEL_RESOURCE_PREFIX);

		CheckBox checkBox = builder.addCheckBox(style, label, null);
		Boolean value = dataElement.getValue();

		if (value != null) {
			checkBox.setSelected(value.booleanValue());
		}

		return checkBox;
	}
}
