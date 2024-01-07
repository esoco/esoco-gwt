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

import de.esoco.data.element.DataElementList;
import de.esoco.data.element.EntityDataElement;

import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.component.Tree;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.layout.EdgeLayout;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.data.DataElementListModel;

import de.esoco.lib.model.ListDataModel;

/**
 * The user interface implementation for entity data elements.
 *
 * @author eso
 */
public class EntityDataElementUI extends DataElementListUI {

	/**
	 * Overridden to not generate a label.
	 *
	 * @see DataElementUI#getElementLabelText(UserInterfaceContext)
	 */
	@Override
	public String getElementLabelText(UserInterfaceContext rContext) {
		return "";
	}

	/**
	 * Creates a panel to display the hierarchy of an entity in a tree view.
	 *
	 * @param rBuilder The builder to add the panel with
	 * @param rElement The entity data element to create the tree for
	 * @return The new tree component
	 */
	Tree createEntityTreePanel(ContainerBuilder<?> rBuilder,
		final EntityDataElement rElement) {
		final ContainerBuilder<Panel> aBuilder =
			rBuilder.addPanel(StyleData.DEFAULT, new EdgeLayout(10));

		final Tree aTree = aBuilder.addTree(AlignedPosition.LEFT);

		DataElementListModel rEntityModel =
			new DataElementListModel(rBuilder.getContext(), rElement, null, "",
				true);

		aTree.setData(
			new ListDataModel<DataElementListModel>("<ROOT>", rEntityModel));

		ContainerBuilder<Panel> aDetailBuilder =
			aBuilder.addPanel(AlignedPosition.CENTER, new FlowLayout(false));

		aTree.addEventListener(EventType.SELECTION,
			new TreeDetailEventHandler(getParent(), aDetailBuilder,
				StyleData.DEFAULT));

		return aTree;
	}

	/**
	 * An event handler that updates the display of detail data when a
	 * selection
	 * in an entity tree occurs.
	 *
	 * @author eso
	 */
	static class TreeDetailEventHandler implements EwtEventHandler {

		private final PanelManager<Container, PanelManager<?, ?>>
			rParentPanelManager;

		private final ContainerBuilder<? extends Panel> rPanelBuilder;

		private final StyleData rDetailStyle;

		private PanelManager<Container, PanelManager<?, ?>> aDetailPanelManager;

		/**
		 * Creates a new instance.
		 *
		 * @param rParentPanelManager The manager of the parent panel
		 * @param rPanelBuilder       The builder to create the detail view
		 *                            with
		 * @param rDetailStyle        A style data that defines the
		 *                                  placement of
		 *                            the tree panel
		 */
		public TreeDetailEventHandler(
			PanelManager<Container, PanelManager<?, ?>> rParentPanelManager,
			ContainerBuilder<? extends Panel> rPanelBuilder,
			StyleData rDetailStyle) {
			this.rParentPanelManager = rParentPanelManager;
			this.rPanelBuilder = rPanelBuilder;
			this.rDetailStyle = rDetailStyle;
		}

		/**
		 * Handles the selection of an element in an entity tree component. The
		 * selected sub-entity element will then be displayed in a detail data
		 * element panel.
		 *
		 * @param rEvent rTree The tree component to handle the selection of
		 */
		@Override
		public void handleEvent(EwtEvent rEvent) {
			Object[] rSelection = ((Tree) rEvent.getSource()).getSelection();

			if (aDetailPanelManager != null) {
				rPanelBuilder.removeComponent(aDetailPanelManager.getPanel());
			}

			if (rSelection.length > 0) {
				DataElementListModel rModel =
					(DataElementListModel) rSelection[0];

				DataElementList rElement = rModel.getModelElement();

				if (rElement instanceof EntityDataElement) {
					aDetailPanelManager =
						new DataElementTablePanelManager(rParentPanelManager,
							rElement);

					aDetailPanelManager.buildIn(rPanelBuilder, rDetailStyle);
				}
			}
		}
	}
}
