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

import de.esoco.lib.property.PropertyName;


/********************************************************************
 * The interface for services that can execute {@link Process Processes}.
 *
 * @author eso
 */
public interface ProcessService extends StorageService
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * A {@link ProcessState} property containing the list of the entities that
	 * are current locked by a process.
	 */
	public static final PropertyName<String> PROCESS_ENTITY_LOCKS =
		PropertyName.newStringName("ProcessEntityLocks");

	// - Commands --------------------------------------------------------------

	/**
	 * Executes a process for the current user. If the argument is an instance
	 * of {@link ProcessDescription} a new process will be created. If it is an
	 * instance of {@link ProcessState} the associated process that has been
	 * created by a previous call to this method will continue to execute. If
	 * the process needs user input the result will be a {@link ProcessState}
	 * object containing the current state of the process, including any
	 * interaction parameters. If the process has already terminated the result
	 * will be NULL.
	 *
	 * <p>The mode of the process execution must be set by invoking the method
	 * {@link ProcessDescription#setExecutionMode(ProcessExecutionMode)}.</p>
	 */
	public static final Command<ProcessDescription, ProcessState> EXECUTE_PROCESS =
		Command.newInstance("EXECUTE_PROCESS");
}
