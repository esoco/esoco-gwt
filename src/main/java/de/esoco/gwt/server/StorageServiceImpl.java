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
import de.esoco.data.FileType;
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

import java.math.BigDecimal;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

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

	private static long nNextStorageAdapterId = 1;

	static {
		RelationTypes.init(StorageServiceImpl.class);
	}

	private Set<String> aInvalidStorageAdapters;

	/**
	 * Creates a new instance.
	 */
	public StorageServiceImpl() {
		StorageManager.setStorageMetaData(DataRelationTypes.SESSION_MANAGER,
			this);
	}

	@Override
	public final StorageAdapter getStorageAdapter(StorageAdapterId rId)
		throws StorageException {
		return getStorageAdapterMap().get(rId);
	}

	/**
	 * Handles the {@link StorageService#PREPARE_DOWNLOAD} command.
	 *
	 * @param rQueryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws Exception If preparing the download data fails
	 */
	public StringDataElement handlePrepareDownload(
		StringDataElement rQueryParams) throws Exception {
		String sAdapterId = rQueryParams.getName();
		String sFileName = rQueryParams.getProperty(FILE_NAME, null);
		StorageAdapter rAdapter = checkStorageAdapter(sAdapterId);

		QueryResultElement<DataModel<String>> rQueryData =
			rAdapter.performQuery(rQueryParams);

		TabularDocumentWriter<byte[]> aDocumentWriter =
			createTableDownloadDocumentWriter();

		List<ColumnDefinition> rColumns = rAdapter.getColumns();

		for (ColumnDefinition rColumn : rColumns) {
			String sColumnTitle = rColumn.getTitle();

			if (sColumnTitle.startsWith("$")) {
				sColumnTitle =
					getResourceString(sColumnTitle.substring(1), null);
			}

			aDocumentWriter.addValue(sColumnTitle);
		}

		for (DataModel<String> rRow : rQueryData) {
			int nColumn = 0;

			aDocumentWriter.newRow();

			for (String sValue : rRow) {
				ColumnDefinition rColumn = rColumns.get(nColumn++);
				Object rValue = null;

				if (sValue != null) {
					if (sValue.startsWith("$")) {
						rValue = getResourceString(sValue.substring(1), null);
					} else if (rColumn.getDatatype().endsWith("Date")) {
						rValue = new Date(Long.parseLong(sValue));
					} else if (rColumn.getDatatype().endsWith("BigDecimal")) {
						rValue = new BigDecimal(sValue);
					} else {
						rValue = sValue;
					}
				}

				aDocumentWriter.addValue(rValue);
			}
		}

		byte[] aDocumentData = aDocumentWriter.createDocument();

		DownloadData aDownloadData =
			new DownloadData(sFileName != null ? sFileName : "download.xls",
				aDocumentWriter.getFileType(),
				Functions.<FileType, byte[]>value(aDocumentData), true);

		return new StringDataElement("DownloadUrl",
			prepareDownload(aDownloadData));
	}

	/**
	 * Handles the {@link StorageService#QUERY} command.
	 *
	 * @param rQueryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws Exception If performing the query fails
	 */
	public QueryResultElement<DataModel<String>> handleQuery(
		StringDataElement rQueryParams) throws Exception {
		String sAdapterId = rQueryParams.getName();
		StorageAdapter rAdapter = checkStorageAdapter(sAdapterId);

		return rAdapter.performQuery(rQueryParams);
	}

	@Override
	public StorageAdapterId registerStorageAdapter(StorageAdapter rAdapter)
		throws StorageException {
		StorageAdapterId aId = new StorageAdapterId(nNextStorageAdapterId++);

		Map<StorageAdapterId, StorageAdapter> rAdapterMap =
			getStorageAdapterMap();

		rAdapterMap.put(aId, rAdapter);

		return aId;
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
	 * @param sId The storage adapter ID
	 * @return The storage adapter if the ID is valid
	 * @throws ServiceException If the given storage adapter ID is invalid
	 * @throws StorageException If retrieving the storage adapter fails
	 */
	private StorageAdapter checkStorageAdapter(String sId)
		throws ServiceException, StorageException {
		StorageAdapter rStorageAdapter = getStorageAdapter(sId);

		if (rStorageAdapter == null) {
			if (aInvalidStorageAdapters == null) {
				aInvalidStorageAdapters = new HashSet<String>();
			}

			String sMessage =
				String.format("Unknown storage adapter for ID %s", sId);

			if (aInvalidStorageAdapters.contains(sId)) {
				Map<String, String> rDummyParams = Collections.emptyMap();

				throw new ServiceException(sMessage, rDummyParams, null);
			} else {
				aInvalidStorageAdapters.add(sId);
				throw new ServiceException(sMessage);
			}
		}

		return rStorageAdapter;
	}

	/**
	 * Returns the storage adapter map for the current session.
	 *
	 * @return The storage adapter map
	 * @throws StorageException If the client is not authenticated
	 */
	private Map<StorageAdapterId, StorageAdapter> getStorageAdapterMap()
		throws StorageException {
		SessionData rSessionData;

		try {
			rSessionData = getSessionData();
		} catch (AuthenticationException e) {
			throw new StorageException(e);
		}

		Map<StorageAdapterId, StorageAdapter> rAdapterMap =
			rSessionData.get(STORAGE_ADAPTER_MAP);

		if (rAdapterMap == null) {
			rAdapterMap = new WeakHashMap<StorageAdapterId, StorageAdapter>();
			rSessionData.set(STORAGE_ADAPTER_MAP, rAdapterMap);
		}

		return rAdapterMap;
	}
}
