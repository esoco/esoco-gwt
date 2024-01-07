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

import de.esoco.gwt.shared.Command;

/**
 * A default command result handler implementation that forwards failures to the
 * method {@link PanelManager#handleCommandFailure(Command, Throwable)}.
 *
 * @author eso
 */
public abstract class DefaultCommandResultHandler<T extends DataElement<?>>
	implements CommandResultHandler<T> {

	private PanelManager<?, ?> panelManager;

	/**
	 * Creates a new instance.
	 *
	 * @param panelManager The panel manager this instance belongs to
	 */
	public DefaultCommandResultHandler(PanelManager<?, ?> panelManager) {
		this.panelManager = panelManager;
	}

	@Override
	public void handleCommandFailure(Command<?, ?> command, Throwable caught) {
		panelManager.handleCommandFailure(command, caught);
	}
}
