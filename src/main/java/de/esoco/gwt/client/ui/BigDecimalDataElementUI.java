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

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.TextControl;
import de.esoco.ewt.composite.Calculator;
import de.esoco.ewt.style.StyleData;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.NumberConstants;

import static de.esoco.lib.property.ContentProperties.FORMAT;
import static de.esoco.lib.property.ContentProperties.INPUT_CONSTRAINT;


/********************************************************************
 * The user interface implementation for {@link BigDecimal} data elements.
 *
 * @author eso
 */
public class BigDecimalDataElementUI
	extends DataElementUI<BigDecimalDataElement>
{
	//~ Static fields/initializers ---------------------------------------------

	/** The default format for numbers if no explicit format is set. */
	public static final String DEFAULT_FORMAT = "#0.00";

	//~ Instance fields --------------------------------------------------------

	private Calculator aCalculator;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * @see DataElementUI#DataElementUI()
	 */
	public BigDecimalDataElementUI()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected Component createInputUI(ContainerBuilder<?>   rBuilder,
									  StyleData				rStyle,
									  BigDecimalDataElement rDataElement)
	{
		Component rComponent;

		if (rStyle.hasFlag(BigDecimalDataElement.CALCULATOR))
		{
			aCalculator = new Calculator();
			rComponent  = aCalculator;

			rBuilder.addComposite(aCalculator, rStyle);
		}
		else
		{
			convertInputConstraintToLocale(rDataElement);
			rComponent = super.createInputUI(rBuilder, rStyle, rDataElement);
		}

		return rComponent;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void transferDataElementValueToComponent(
		BigDecimalDataElement rDataElement,
		Component			  rComponent)
	{
		if (aCalculator != null)
		{
			aCalculator.setValue(rDataElement.getValue());
		}
		else
		{
			super.transferDataElementValueToComponent(rDataElement, rComponent);
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void transferInputToDataElement(
		Component			  rComponent,
		BigDecimalDataElement rDataElement)
	{
		BigDecimal rValue;

		if (aCalculator != null)
		{
			rValue = aCalculator.getValue();
		}
		else
		{
			String sText = ((TextControl) rComponent).getText();

			try
			{
				String sFormat =
					rDataElement.getProperty(FORMAT, DEFAULT_FORMAT);
				int    nScale  = 0;

				int nDecimalPoint = sFormat.indexOf('.');

				if (nDecimalPoint >= 0)
				{
					nScale = sFormat.length() - (nDecimalPoint + 1);
				}

				// TODO: check for actual decimal group characters
				sText  = sText.replace(',', '.');
				rValue = new BigDecimal(sText);

				rValue = rValue.setScale(nScale, RoundingMode.HALF_UP);
			}
			catch (Exception e)
			{
				rValue = null;
			}
		}

		rDataElement.setValue(rValue);
	}

	/***************************************
	 * Overridden to first invoke the input constraint conversion.
	 *
	 * @see DataElementUI#updateTextComponent(TextControl)
	 */
	@Override
	protected void updateTextComponent(TextControl rTextComponent)
	{
		convertInputConstraintToLocale(getDataElement());
		super.updateTextComponent(rTextComponent);
	}

	/***************************************
	 * Converts the input constraint of the data element to the current user's
	 * locale.
	 *
	 * @param rDataElement The data element to convert the constraint of
	 */
	private void convertInputConstraintToLocale(
		BigDecimalDataElement rDataElement)
	{
		String sConstraint =
			rDataElement.getProperty(INPUT_CONSTRAINT,
									 BigDecimalDataElement.DEFAULT_CONSTRAINT);

		NumberConstants rNumberConstants =
			LocaleInfo.getCurrentLocale().getNumberConstants();

		String sGroupingSeparator = rNumberConstants.groupingSeparator();
		String sDecimalSeparator  = rNumberConstants.decimalSeparator();

		sConstraint =
			sConstraint.replaceAll(BigDecimalDataElement.DECIMAL_GROUP_CHAR,
								   "\\\\" + sGroupingSeparator);
		sConstraint =
			sConstraint.replaceAll(BigDecimalDataElement.DECIMAL_SEPARATOR_CHAR,
								   "\\\\" + sDecimalSeparator);

		rDataElement.setProperty(INPUT_CONSTRAINT, sConstraint);
	}
}
