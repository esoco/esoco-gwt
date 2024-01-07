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
package de.esoco.gwt.shared;

import de.esoco.data.element.DataElement;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines a command for a {@link CommandService}. The type parameter
 * T defines the type of the input value for the command and the parameter R
 * stands for the datatype of the command result. New instances are created with
 * the factory method {@link #newInstance(String)} and are must to be defined as
 * static singleton constants which should have the same name as the instance.
 * The factory method enforces this by checking that not two commands have the
 * same name.
 *
 * @author eso
 */
public class Command<T extends DataElement<?>, R extends DataElement<?>>
	implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Map<String, Command<?, ?>> aCommandRegistry =
		new HashMap<>();

	private String sName;

	/**
	 * Default constructor for GWT serialization.
	 */
	Command() {
	}

	/**
	 * Creates a new instance. Private, only used internally by the factory
	 * method {@link #newInstance(String)}.
	 *
	 * @param sName The name of the instance
	 */
	private Command(String sName) {
		this.sName = sName;
	}

	/**
	 * Factory method that creates a new command instance.
	 *
	 * @param sName The name of the new instance
	 * @return A new command instance
	 * @throws IllegalArgumentException If the given name is invalid or if a
	 *                                  command with the given name exists
	 *                                  already
	 */
	public static <T extends DataElement<?>, R extends DataElement<?>> Command<T, R> newInstance(
		String sName) {
		if (sName == null || sName.length() == 0) {
			throw new NullPointerException("Name must not be NULL");
		}

		Command<T, R> aCommand;

		if (aCommandRegistry.containsKey(sName)) {
			throw new IllegalArgumentException(
				"A command type with name " + sName + " exists already");
		} else {
			aCommand = new Command<T, R>(sName);
			aCommandRegistry.put(sName, aCommand);
		}

		return aCommand;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object rObject) {
		if (this == rObject) {
			return true;
		}

		if (rObject == null || getClass() != rObject.getClass()) {
			return false;
		}

		return sName.equals(((Command<?, ?>) rObject).sName);
	}

	/**
	 * Returns the name of this command.
	 *
	 * @return The command name
	 */
	public final String getName() {
		return sName;
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return sName.hashCode() * 17;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "Command[" + sName + "]";
	}

	/**
	 * Returns the singleton command instance for the given name.
	 *
	 * @return The command instance for the given name
	 * @throws IllegalStateException If no matching command could be found
	 */
	Object readResolve() {
		Command<?, ?> rCommand = aCommandRegistry.get(sName);

		if (rCommand == null) {
			throw new IllegalStateException("Undefined command: " + sName);
		}

		return rCommand;
	}
}
