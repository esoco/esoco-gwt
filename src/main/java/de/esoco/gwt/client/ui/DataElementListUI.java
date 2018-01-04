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
import de.esoco.data.element.DataElementList;

import de.esoco.ewt.EWT;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.style.StyleData;

import de.esoco.lib.property.LayoutType;

import java.util.List;
import java.util.Map;

import static de.esoco.lib.property.LayoutProperties.LAYOUT;


/********************************************************************
 * A data element user interface that manages a {@link DataElementList} data
 * element in a child panel manager.
 *
 * @author eso
 */
public class DataElementListUI extends DataElementUI<DataElementList>
{
	//~ Static fields/initializers ---------------------------------------------

	private static final boolean PROFILING = false;

	//~ Instance fields --------------------------------------------------------

	private DataElementPanelManager aListPanelManager;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void collectInput(List<DataElement<?>> rModifiedElements)
	{
		aListPanelManager.collectInput(rModifiedElements);
	}

	/***************************************
	 * Returns the {@link DataElementTablePanelManager} that is used for the
	 * display of the {@link DataElementList} of this instance.
	 *
	 * @return The panel manager or NULL for none
	 */
	public final DataElementPanelManager getPanelManager()
	{
		return aListPanelManager;
	}

	/***************************************
	 * Updates the child panel manager with the current style and properties and
	 * the data element UIs of all children.
	 *
	 * @see DataElementUI#update()
	 */
	@Override
	public void update()
	{
		long t = System.currentTimeMillis();

		DataElementList rDataElement = getDataElement();
		String		    sAddStyle    = aListPanelManager.getStyleName();

		StyleData rNewStyle =
			applyElementStyle(rDataElement,
							  PanelManager.addStyles(getBaseStyle(),
													 sAddStyle));

		applyStyle();
		aListPanelManager.getPanel().applyStyle(rNewStyle);
		aListPanelManager.updateUI();

		if (PROFILING)
		{
			profile("DEL-UPDATE", t);
		}
	}

	/***************************************
	 * Overridden to create a child {@link DataElementTablePanelManager} for the
	 * data element list.
	 *
	 * @see DataElementUI#buildDataElementUI(ContainerBuilder, StyleData)
	 */
	@Override
	protected Component buildDataElementUI(
		ContainerBuilder<?> rBuilder,
		StyleData			rStyle)
	{
		DataElementList rDataElementList = getDataElement();
		Container	    rListPanel		 = null;

		LayoutType eDisplayMode =
			rDataElementList.getProperty(LAYOUT, LayoutType.TABLE);

		aListPanelManager =
			DataElementPanelManager.newInstance(getParent(), rDataElementList);

		aListPanelManager.buildIn(rBuilder, rStyle);
		rListPanel = aListPanelManager.getPanel();

		if (eDisplayMode == LayoutType.TABLE)
		{
			// DataElementPanelManager performs event handling for other cases
			setupInteractionHandling(rListPanel, false);
		}

		return rListPanel;
	}

	/***************************************
	 * @see DataElementUI#enableInteraction(boolean)
	 */
	@Override
	protected void enableInteraction(boolean bEnable)
	{
		aListPanelManager.enableInteraction(bEnable);
	}

	/***************************************
	 * Implemented to close an open child view.
	 *
	 * @see DataElementUI#dispose()
	 */
	@Override
	void dispose()
	{
		aListPanelManager.dispose();

		super.dispose();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	void profile(String sDescription, long nStartTime)
	{
		String sIndent    = getPanelManager().getHierarchyChildIndent();
		String sIndicator =
			System.currentTimeMillis() - nStartTime > 100 ? "!!" : "  ";

		sIndent = sIndent.substring(0, sIndent.length() - 1);

		EWT.logTime(sIndicator + sIndent + "o-" + sDescription,
					getDataElement().getSimpleName(),
					nStartTime);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	void updateDataElement(DataElement<?>	   rNewElement,
						   Map<String, String> rElementErrors,
						   boolean			   bUpdateUI)
	{
		// always use FALSE to not update UI before data element is updated;
		// UI update will be done in the update() method
		super.updateDataElement(rNewElement, rElementErrors, false);

		if (aListPanelManager != null)
		{
			aListPanelManager.update(getDataElement(), rElementErrors, false);
		}

		if (bUpdateUI)
		{
			update();
		}
	}
}
