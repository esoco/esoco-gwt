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

import de.esoco.data.element.BigDecimalDataElement;
import de.esoco.data.element.BigDecimalDataElement.DisplayStyle;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.TextControl;
import de.esoco.ewt.composite.Calculator;
import de.esoco.ewt.composite.MultiFormatDisplay;
import de.esoco.ewt.composite.MultiFormatDisplay.NumberDisplayFormat;
import de.esoco.ewt.style.StyleData;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.NumberConstants;

import static de.esoco.lib.property.ContentProperties.FORMAT;
import static de.esoco.lib.property.ContentProperties.INPUT_CONSTRAINT;

/**
 * The user interface implementation for {@link BigDecimal} data elements.
 *
 * @author eso
 */
public class BigDecimalDataElementUI
	extends DataElementUI<BigDecimalDataElement> {

	/**
	 * The default format for numbers if no explicit format is set.
	 */
	public static final String DEFAULT_FORMAT = "#0.00";

	/**
	 * @see DataElementUI#DataElementUI()
	 */
	public BigDecimalDataElementUI() {
	}

	@Override
	protected Component createDisplayUI(ContainerBuilder<?> builder,
		StyleData style, BigDecimalDataElement dataElement) {
		DisplayStyle displayStyle =
			style.getProperty(BigDecimalDataElement.DISPLAY_STYLE,
				DisplayStyle.DECIMAL);

		Component component;

		if (displayStyle == DisplayStyle.MULTI_FORMAT) {
			MultiFormatDisplay<BigDecimal, NumberDisplayFormat>
				multiFormatDisplay =
				new MultiFormatDisplay<>(NumberDisplayFormat.DECIMAL,
					NumberDisplayFormat.HEXADECIMAL,
					NumberDisplayFormat.BINARY);

			component = builder.addComposite(multiFormatDisplay, style);
		} else {
			component = super.createDisplayUI(builder, style, dataElement);
		}

		return component;
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData style, BigDecimalDataElement dataElement) {
		DisplayStyle displayStyle =
			style.getProperty(BigDecimalDataElement.DISPLAY_STYLE,
				DisplayStyle.DECIMAL);

		Component component;

		if (displayStyle == DisplayStyle.CALCULATOR) {
			Calculator calculator = new Calculator();

			component = builder.addComposite(calculator, style);
		} else {
			convertInputConstraintToLocale(dataElement);
			component = super.createInputUI(builder, style, dataElement);
		}

		return component;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void transferDataElementValueToComponent(
		BigDecimalDataElement dataElement, Component component) {
		if (component instanceof Calculator) {
			((Calculator) component).setValue(dataElement.getValue());
		}

		if (component instanceof MultiFormatDisplay) {
			((MultiFormatDisplay<BigDecimal, NumberDisplayFormat>) component).update(
				dataElement.getValue());
		} else {
			super.transferDataElementValueToComponent(dataElement, component);
		}
	}

	@Override
	protected void transferInputToDataElement(Component component,
		BigDecimalDataElement dataElement) {
		BigDecimal value;

		if (component instanceof Calculator) {
			value = ((Calculator) component).getValue();
		} else {
			String text = ((TextControl) component).getText();

			try {
				String format = dataElement.getProperty(FORMAT,
					DEFAULT_FORMAT);
				int scale = 0;

				int decimalPoint = format.indexOf('.');

				if (decimalPoint >= 0) {
					scale = format.length() - (decimalPoint + 1);
				}

				// TODO: check for actual decimal group characters
				text = text.replace(',', '.');
				value = new BigDecimal(text);

				value = value.setScale(scale, RoundingMode.HALF_UP);
			} catch (Exception e) {
				value = null;
			}
		}

		dataElement.setValue(value);
	}

	/**
	 * Overridden to first invoke the input constraint conversion.
	 *
	 * @see DataElementUI#updateTextComponent(TextControl)
	 */
	@Override
	protected void updateTextComponent(TextControl textComponent) {
		convertInputConstraintToLocale(getDataElement());
		super.updateTextComponent(textComponent);
	}

	/**
	 * Converts the input constraint of the data element to the current user's
	 * locale.
	 *
	 * @param dataElement The data element to convert the constraint of
	 */
	private void convertInputConstraintToLocale(
		BigDecimalDataElement dataElement) {
		String constraint = dataElement.getProperty(INPUT_CONSTRAINT,
			BigDecimalDataElement.DEFAULT_CONSTRAINT);

		NumberConstants numberConstants =
			LocaleInfo.getCurrentLocale().getNumberConstants();

		String groupingSeparator = numberConstants.groupingSeparator();
		String decimalSeparator = numberConstants.decimalSeparator();

		constraint =
			constraint.replaceAll(BigDecimalDataElement.DECIMAL_GROUP_CHAR,
				"\\\\" + groupingSeparator);
		constraint =
			constraint.replaceAll(BigDecimalDataElement.DECIMAL_SEPARATOR_CHAR,
				"\\\\" + decimalSeparator);

		dataElement.setProperty(INPUT_CONSTRAINT, constraint);
	}
}
