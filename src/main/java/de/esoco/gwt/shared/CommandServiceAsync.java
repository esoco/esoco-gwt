//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.gwt.shared;

import de.esoco.data.element.DataElement;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous variant of the {@link CommandService} interface.
 *
 * @author eso
 */
public interface CommandServiceAsync {

	/**
	 * @see CommandService#executeCommand(Command, DataElement)
	 */
	public <T extends DataElement<?>, R extends DataElement<?>> void executeCommand(
		Command<T, R> rCommand, T rData, AsyncCallback<R> rCallback);
}
