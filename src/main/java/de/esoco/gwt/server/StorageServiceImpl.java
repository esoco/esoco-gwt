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
package de.esoco.gwt.server;

import de.esoco.data.DataRelationTypes;
import de.esoco.data.DownloadData;
import de.esoco.data.SessionData;
import de.esoco.data.document.TabularDocumentWriter;
import de.esoco.data.element.QueryResultElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.storage.StorageAdapter;
import de.esoco.data.storage.StorageAdapterId;
import de.esoco.data.storage.StorageAdapterRegistry;
import de.esoco.entity.Entity;
import de.esoco.gwt.shared.AuthenticationException;
import de.esoco.gwt.shared.ServiceException;
import de.esoco.gwt.shared.StorageService;
import de.esoco.lib.expression.Functions;
import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.storage.StorageException;
import de.esoco.storage.StorageManager;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static de.esoco.lib.property.ContentProperties.FILE_NAME;
import static org.obrel.core.RelationTypes.newMapType;

/**
 * Implementation of the {@link StorageService} interface.
 *
 * @author eso
 */
public abstract class StorageServiceImpl<E extends Entity>
	extends AuthenticatedServiceImpl<E>
	implements StorageService, StorageAdapterRegistry {

	private static final long serialVersionUID = 1L;

	// relation type to store the storage adapters for a session
	private static final RelationType<Map<StorageAdapterId, StorageAdapter>>
		STORAGE_ADAPTER_MAP = newMapType(false);

	private static long nextStorageAdapterId = 1;

	static {
		RelationTypes.init(StorageServiceImpl.class);
	}

	private Set<String> invalidStorageAdapters;

	/**
	 * Creates a new instance.
	 */
	public StorageServiceImpl() {
		StorageManager.setStorageMetaData(DataRelationTypes.SESSION_MANAGER,
			this);
	}

	@Override
	public final StorageAdapter getStorageAdapter(StorageAdapterId id)
		throws StorageException {
		return getStorageAdapterMap().get(id);
	}

	/**
	 * Handles the {@link StorageService#PREPARE_DOWNLOAD} command.
	 *
	 * @param queryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws Exception If preparing the download data fails
	 */
	public StringDataElement handlePrepareDownload(
		StringDataElement queryParams) throws Exception {
		String adapterId = queryParams.getName();
		String fileName = queryParams.getProperty(FILE_NAME, null);
		StorageAdapter adapter = checkStorageAdapter(adapterId);

		QueryResultElement<DataModel<String>> queryData =
			adapter.performQuery(queryParams);

		TabularDocumentWriter<byte[]> documentWriter =
			createTableDownloadDocumentWriter();

		List<ColumnDefinition> columns = adapter.getColumns();

		for (ColumnDefinition column : columns) {
			String columnTitle = column.getTitle();

			if (columnTitle.startsWith("$")) {
				columnTitle = getResourceString(columnTitle.substring(1),
					null);
			}

			documentWriter.addValue(columnTitle);
		}

		for (DataModel<String> row : queryData) {
			int column = 0;

			documentWriter.newRow();

			for (String cellValue : row) {
				ColumnDefinition columnDef = columns.get(column++);
				Object value = null;

				if (cellValue != null) {
					if (cellValue.startsWith("$")) {
						value = getResourceString(cellValue.substring(1),
							null);
					} else if (columnDef.getDatatype().endsWith("Date")) {
						value = new Date(Long.parseLong(cellValue));
					} else if (columnDef.getDatatype().endsWith("BigDecimal")) {
						value = new BigDecimal(cellValue);
					} else {
						value = cellValue;
					}
				}

				documentWriter.addValue(value);
			}
		}

		byte[] documentData = documentWriter.createDocument();

		DownloadData downloadData =
			new DownloadData(fileName != null ? fileName : "download.xls",
				documentWriter.getFileType(), Functions.value(documentData),
				true);

		return new StringDataElement("DownloadUrl",
			prepareDownload(downloadData));
	}

	/**
	 * Handles the {@link StorageService#QUERY} command.
	 *
	 * @param queryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws Exception If performing the query fails
	 */
	public QueryResultElement<DataModel<String>> handleQuery(
		StringDataElement queryParams) throws Exception {
		String adapterId = queryParams.getName();
		StorageAdapter adapter = checkStorageAdapter(adapterId);

		return adapter.performQuery(queryParams);
	}

	@Override
	public StorageAdapterId registerStorageAdapter(StorageAdapter adapter)
		throws StorageException {
		StorageAdapterId id = new StorageAdapterId(nextStorageAdapterId++);

		Map<StorageAdapterId, StorageAdapter> adapterMap =
			getStorageAdapterMap();

		adapterMap.put(id, adapter);

		return id;
	}

	/**
	 * Subclasses that want to provide a download option from UI tables must
	 * implement this method to return an implementation of
	 * {@link TabularDocumentWriter} that processes table data for download.
	 * The
	 * default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @return An tabular document writer instance
	 */
	protected TabularDocumentWriter<byte[]> createTableDownloadDocumentWriter() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * Retrieves a storage adapter for a certain adapter ID and throws an
	 * exception if the ID is invalid.
	 *
	 * @param id The storage adapter ID
	 * @return The storage adapter if the ID is valid
	 * @throws ServiceException If the given storage adapter ID is invalid
	 * @throws StorageException If retrieving the storage adapter fails
	 */
	private StorageAdapter checkStorageAdapter(String id)
		throws ServiceException, StorageException {
		StorageAdapter storageAdapter = getStorageAdapter(id);

		if (storageAdapter == null) {
			if (invalidStorageAdapters == null) {
				invalidStorageAdapters = new HashSet<String>();
			}

			String message =
				String.format("Unknown storage adapter for ID %s", id);

			if (invalidStorageAdapters.contains(id)) {
				Map<String, String> dummyParams = Collections.emptyMap();

				throw new ServiceException(message, dummyParams, null);
			} else {
				invalidStorageAdapters.add(id);
				throw new ServiceException(message);
			}
		}

		return storageAdapter;
	}

	/**
	 * Returns the storage adapter map for the current session.
	 *
	 * @return The storage adapter map
	 * @throws StorageException If the client is not authenticated
	 */
	private Map<StorageAdapterId, StorageAdapter> getStorageAdapterMap()
		throws StorageException {
		SessionData sessionData;

		try {
			sessionData = getSessionData();
		} catch (AuthenticationException e) {
			throw new StorageException(e);
		}

		Map<StorageAdapterId, StorageAdapter> adapterMap =
			sessionData.get(STORAGE_ADAPTER_MAP);

		if (adapterMap == null) {
			adapterMap = new WeakHashMap<StorageAdapterId, StorageAdapter>();
			sessionData.set(STORAGE_ADAPTER_MAP, adapterMap);
		}

		return adapterMap;
	}
}
