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
 * This interface must be implement to handle the result of executing a
 * command.
 *
 * @author eso
 */
public interface CommandResultHandler<T extends DataElement<?>> {

	/**
	 * handles a command failure.
	 *
	 * @param command The failed command
	 * @param caught  The exception that occurred
	 */
	public void handleCommandFailure(Command<?, ?> command, Throwable caught);

	/**
	 * Handles the result of a successful command execution.
	 *
	 * @param result The data element that has been returned by the command
	 */
	public void handleCommandResult(T result);

}
