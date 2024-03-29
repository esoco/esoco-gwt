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

	private static final Map<String, Command<?, ?>> commandRegistry =
		new HashMap<>();

	private String name;

	/**
	 * Default constructor for GWT serialization.
	 */
	Command() {
	}

	/**
	 * Creates a new instance. Private, only used internally by the factory
	 * method {@link #newInstance(String)}.
	 *
	 * @param name The name of the instance
	 */
	private Command(String name) {
		this.name = name;
	}

	/**
	 * Factory method that creates a new command instance.
	 *
	 * @param name The name of the new instance
	 * @return A new command instance
	 * @throws IllegalArgumentException If the given name is invalid or if a
	 *                                  command with the given name exists
	 *                                  already
	 */
	public static <T extends DataElement<?>, R extends DataElement<?>> Command<T, R> newInstance(
		String name) {
		if (name == null || name.length() == 0) {
			throw new NullPointerException("Name must not be NULL");
		}

		Command<T, R> command;

		if (commandRegistry.containsKey(name)) {
			throw new IllegalArgumentException(
				"A command type with name " + name + " exists already");
		} else {
			command = new Command<T, R>(name);
			commandRegistry.put(name, command);
		}

		return command;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		return name.equals(((Command<?, ?>) object).name);
	}

	/**
	 * Returns the name of this command.
	 *
	 * @return The command name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode() * 17;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "Command[" + name + "]";
	}

	/**
	 * Returns the singleton command instance for the given name.
	 *
	 * @return The command instance for the given name
	 * @throws IllegalStateException If no matching command could be found
	 */
	Object readResolve() {
		Command<?, ?> command = commandRegistry.get(name);

		if (command == null) {
			throw new IllegalStateException("Undefined command: " + name);
		}

		return command;
	}
}
