//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.event.EWTEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.UserInterfaceProperties;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/********************************************************************
 * A data element panel manager implementation that wraps a single data element
 * and it's UI.
 *
 * @author eso
 */
public class SingleDataElementPanelManager extends DataElementPanelManager
{
	//~ Instance fields --------------------------------------------------------

	private DataElement<?>   rDataElement;
	private StyleData		 rElementStyle;
	private DataElementUI<?> aElementUI;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	public SingleDataElementPanelManager(
		PanelManager<?, ?> rParent,
		DataElement<?>	   rDataElement)
	{
		super(rParent, rDataElement.getResourceId());

		this.rDataElement = rDataElement;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void addElementEventListener(
		EventType		rEventType,
		EWTEventHandler rListener)
	{
		aElementUI.addEventListener(rEventType, rListener);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void addElementEventListener(DataElement<?>  rDataElement,
										EventType		rEventType,
										EWTEventHandler rListener)
	{
		if (rDataElement == this.rDataElement)
		{
			aElementUI.addEventListener(rEventType, rListener);
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void collectInput()
	{
		aElementUI.collectInput();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void enableInteraction(boolean bEnable)
	{
		aElementUI.enableInteraction(bEnable);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public DataElement<?> findDataElement(String sName)
	{
		return rDataElement.getName().equals(sName) ? rDataElement : null;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Component getContentComponent()
	{
		return aElementUI.getElementComponent();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Collection<DataElement<?>> getDataElements()
	{
		return Arrays.<DataElement<?>>asList(rDataElement);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public DataElementUI<?> getDataElementUI(DataElement<?> rDataElement)
	{
		return aElementUI.getDataElement() == rDataElement ? aElementUI : null;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void updateDataElements(List<DataElement<?>> rNewDataElements,
								   Map<String, String>  rErrorMessages,
								   boolean				bUpdateUI)
	{
		assert rNewDataElements.size() == 1;

		String		   sElementName    = rDataElement.getName();
		DataElement<?> rNewDataElement = rNewDataElements.get(0);

		if (rNewDataElement.getName().equals(sElementName))
		{
			rDataElement = rNewDataElement;
			aElementUI.updateDataElement(rDataElement,
										 rErrorMessages,
										 bUpdateUI);
		}
		else
		{
			assert false : "Replacing data element is not supported [" +
				   sElementName + "->" + rNewDataElement.getName() + "]";
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void updatePanel()
	{
		aElementUI.update();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void addComponents()
	{
		aElementUI = DataElementUIFactory.create(this, rDataElement);

		String    sStyle = aElementUI.getElementStyleName();
		StyleData aStyle =
			addStyles(rElementStyle,
					  CSS.gfDataElement(),
					  CSS.gfSingleDataElement());

		if (rDataElement.isImmutable())
		{
			sStyle = CSS.readonly() + " " + sStyle;
		}

		aStyle = addStyles(aStyle, sStyle);

		aElementUI.buildUserInterface(this, aStyle);
		aElementUI.setHiddenLabelHint(getContext());
		checkSelectionDependency(getRootDataElementPanelManager(),
								 rDataElement);

		if (rDataElement.hasFlag(UserInterfaceProperties.INITIAL_FOCUS))
		{
			aElementUI.requestFocus();
		}
	}

	/***************************************
	 * @see DataElementPanelManager#createContainer(ContainerBuilder, StyleData)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected ContainerBuilder<Panel> createContainer(
		ContainerBuilder<?> rBuilder,
		StyleData			rStyleData)
	{
		rElementStyle = rStyleData;

		return (ContainerBuilder<Panel>) rBuilder;
	}
}
