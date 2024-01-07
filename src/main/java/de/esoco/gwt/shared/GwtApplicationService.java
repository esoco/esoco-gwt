//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.data.element.DataElementList;

/**
 * The base interface for application GWT RPC services.
 *
 * @author eso
 */
public interface GwtApplicationService extends ProcessService {

	/**
	 * Name of the data element in the user data that contains the processes
	 * that can be executed by the user. Type: {@link DataElementList}.
	 */
	public static final String USER_PROCESSES = "UserProcesses";

	/**
	 * The name of the process parameter and data element used to transfer an
	 * entity ID to the process. This must be the same name as that of the
	 * entity relation type {@code ENTITY_ID}.
	 */
	public static final String ENTITY_ID_NAME = "de.esoco.entity.ENTITY_ID";
}
