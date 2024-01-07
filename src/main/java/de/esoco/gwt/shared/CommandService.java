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

import de.esoco.gwt.server.CommandServiceImpl;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * The interface for an abstract service class that can execute the commands
 * that are defined by derived interfaces. Sub-interfaces define commands as
 * singleton constants of the type {@link Command}. Each command should have a
 * detailed javadoc comment that documents the command purpose and it's input
 * and output. The implementation of a sub-interface must then derive from
 * {@link CommandServiceImpl}.
 *
 * @author eso
 */
public interface CommandService extends RemoteService {

	/**
	 * Executes a command in the service.
	 *
	 * @param command The command to execute
	 * @param data    The data to be processed by the command
	 * @return The resulting data element (will be NULL for commands that do
	 * not
	 * return a result)
	 */
	public <T extends DataElement<?>, R extends DataElement<?>> R executeCommand(
		Command<T, R> command, T data) throws ServiceException;
}
