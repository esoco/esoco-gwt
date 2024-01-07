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
package de.esoco.gwt.shared;

import de.esoco.data.element.QueryResultElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.element.StringMapDataElement;

import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.FilterableDataModel;
import de.esoco.lib.property.ContentProperties;
import de.esoco.lib.property.SortDirection;
import de.esoco.lib.property.StorageProperties;

/**
 * Base interface for services that are based on persistent objects from a
 * storage and/or for messages on a mail server.
 *
 * @author eso
 */
public interface StorageService extends AuthenticatedService {

	/**
	 * An error token for exceptions to indicate a locked entity.
	 */
	public static final String ERROR_ENTITY_LOCKED = "EntityLocked";

	/**
	 * An error token for exceptions to indicate the ID a locked entity.
	 */
	public static final String ERROR_LOCKED_ENTITY_ID = "LockedEntityId";

	//- Commands
	// ---------------------------------------------------------------

	/**
	 * A command that executes a query on a database storage or on a mail
	 * server. If the query yields less elements than defined by the given
	 * limit
	 * the resulting array will contain fewer elements than requested and may
	 * even be empty. The argument to the query command is a data element named
	 * with the query key and containing the following properties from
	 * {@link StorageProperties} (elements with a default value are optional):
	 *
	 * <ul>
	 *   <li>{@link StorageProperties#QUERY_START}: The index of the first
	 *     element to return (zero-based)</li>
	 *   <li>{@link StorageProperties#QUERY_LIMIT}: The maximum number of
	 *     elements to return.</li>
	 *   <li>{@link StorageProperties#QUERY_DEPTH}: The maximum depth up to
	 *     which child entities shall be read (Default: 0).</li>
	 *   <li>{@link StorageProperties#QUERY_SEARCH}: Must be a {@link
	 *     StringMapDataElement} that contains a mapping from column IDs to
	 *     search constraints as defined for the method {@link
	 *     FilterableDataModel#getFilter(String)} (Default: null, i.e. no
	 *     search constraints).</li>
	 *   <li>{@link StorageProperties#QUERY_SORT}: A mapping from column IDs to
	 *     a {@link SortDirection} (Default: null, i.e. no specific
	 *     sorting).</li>
	 * </ul>
	 *
	 * <p>This command returns a {@link QueryResultElement} that contains
	 * string
	 * {@link DataModel DataModels} that represent the queried rows. The string
	 * values in the row models contain the queried column values.</p>
	 */
	public static final Command<StringDataElement,
		QueryResultElement<DataModel<String>>>
		QUERY = Command.newInstance("QUERY");

	/**
	 * A command to prepare the download of data for a certain storage query.
	 * The input argument is the exactly the same data element as that of the
	 * {@link #QUERY} command. An additional property with the name
	 * {@link ContentProperties#FILE_NAME} can be set to pre-set a file name of
	 * the prepared download.
	 *
	 * <p>The command return value is a string data element containing the
	 * relative download URL for the generated data.</p>
	 */
	public static final Command<StringDataElement, StringDataElement>
		PREPARE_DOWNLOAD = Command.newInstance("PREPARE_DOWNLOAD");
}
