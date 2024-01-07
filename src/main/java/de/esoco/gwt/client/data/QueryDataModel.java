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
package de.esoco.gwt.client.data;

import de.esoco.data.element.QueryResultElement;
import de.esoco.data.element.StringDataElement;

import de.esoco.gwt.client.ServiceRegistry;
import de.esoco.gwt.shared.StorageService;

import de.esoco.lib.model.Callback;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.Downloadable;
import de.esoco.lib.model.FilterableDataModel;
import de.esoco.lib.model.RemoteDataModel;
import de.esoco.lib.model.SortableDataModel;
import de.esoco.lib.property.SortDirection;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import static de.esoco.lib.property.ContentProperties.FILE_NAME;
import static de.esoco.lib.property.StorageProperties.QUERY_LIMIT;
import static de.esoco.lib.property.StorageProperties.QUERY_SEARCH;
import static de.esoco.lib.property.StorageProperties.QUERY_SORT;
import static de.esoco.lib.property.StorageProperties.QUERY_START;

/**
 * A remote data model implementation that performs entity queries through the
 * {@link StorageService} interface.
 *
 * @author eso
 */
public class QueryDataModel implements RemoteDataModel<DataModel<String>>,
	SortableDataModel<DataModel<String>>,
	FilterableDataModel<DataModel<String>>, Downloadable, Serializable {

	private static final long serialVersionUID = 1L;

	private String sQueryId;

	private int nQuerySize;

	private transient int nWindowSize;

	private transient int nWindowStart;

	private transient List<DataModel<String>> aCurrentData;

	private transient Map<String, String> aFilters = new HashMap<>();

	private transient Map<String, SortDirection> aSortFields = new HashMap<>();

	/**
	 * Creates a new instance for a certain query. If the query size is not
	 * known it should be set to zero.
	 *
	 * @param sQueryId   The query ID
	 * @param nQuerySize The (initial) query size (zero if unknown)
	 */
	public QueryDataModel(String sQueryId, int nQuerySize) {
		this.sQueryId = sQueryId;
		this.nQuerySize = nQuerySize;
	}

	/**
	 * Default constructor for GWT serialization.
	 */
	QueryDataModel() {
	}

	/**
	 * @see RemoteDataModel#getAvailableElementCount()
	 */
	@Override
	public int getAvailableElementCount() {
		return aCurrentData.size();
	}

	/**
	 * @see DataModel#getElement(int)
	 */
	@Override
	public DataModel<String> getElement(int nIndex) {
		return aCurrentData.get(nIndex - nWindowStart);
	}

	/**
	 * @see DataModel#getElementCount()
	 */
	@Override
	public int getElementCount() {
		return nQuerySize;
	}

	/**
	 * @see FilterableDataModel#getFilter(String)
	 */
	@Override
	public String getFilter(String sFieldId) {
		return aFilters.get(sFieldId);
	}

	/**
	 * @see FilterableDataModel#getFilters()
	 */
	@Override
	public Map<String, String> getFilters() {
		return aFilters;
	}

	/**
	 * Returns the query ID of this model.
	 *
	 * @return The query ID
	 */
	public final String getQueryId() {
		return sQueryId;
	}

	/**
	 * @see SortableDataModel#getSortDirection(String)
	 */
	@Override
	public SortDirection getSortDirection(String sFieldId) {
		return aSortFields.get(sFieldId);
	}

	/**
	 * @see RemoteDataModel#getWindowSize()
	 */
	@Override
	public int getWindowSize() {
		return nWindowSize;
	}

	/**
	 * @see RemoteDataModel#getWindowStart()
	 */
	@Override
	public int getWindowStart() {
		return nWindowStart;
	}

	/**
	 * Returns an iterator over the current data set that has been retrieved by
	 * the last call to {@link #setWindow(int, int, Callback)}.
	 *
	 * @see DataModel#iterator()
	 */
	@Override
	public Iterator<DataModel<String>> iterator() {
		return aCurrentData.iterator();
	}

	/**
	 * The integer limit parameter defines the maximum number of rows to
	 * download.
	 *
	 * @see Downloadable#prepareDownload(String, int, Callback)
	 */
	@Override
	public void prepareDownload(String sFileName, int nMaxRows,
		final Callback<String> rCallback) {
		if (aCurrentData != null) {
			StringDataElement aQueryData = createQueryData(0, nMaxRows);

			aQueryData.setProperty(FILE_NAME, sFileName);

			ServiceRegistry
				.getStorageService()
				.executeCommand(StorageService.PREPARE_DOWNLOAD, aQueryData,
					new AsyncCallback<StringDataElement>() {
						@Override
						public void onFailure(Throwable e) {
							rCallback.onError(e);
						}

						@Override
						public void onSuccess(StringDataElement rDownloadUrl) {
							rCallback.onSuccess(rDownloadUrl.getValue());
						}
					});
		}
	}

	/**
	 * @see FilterableDataModel#removeAllFilters()
	 */
	@Override
	public void removeAllFilters() {
		aFilters.clear();
	}

	/**
	 * @see SortableDataModel#removeSorting()
	 */
	@Override
	public void removeSorting() {
		aSortFields.clear();
	}

	/**
	 * Resets the query size to force a re-initialization.
	 */
	public void resetQuerySize() {
		nQuerySize = 0;
	}

	/**
	 * @see FilterableDataModel#setFilter(String, String)
	 */
	@Override
	public void setFilter(String sFieldId, String sFilter) {
		if (sFilter != null && !sFilter.isEmpty()) {
			aFilters.put(sFieldId, sFilter);
		} else {
			aFilters.remove(sFieldId);
		}
	}

	/**
	 * @see FilterableDataModel#setFilters(Map)
	 */
	@Override
	public void setFilters(Map<String, String> rFilters) {
		aFilters.clear();
		aFilters.putAll(rFilters);
	}

	/**
	 * @see SortableDataModel#setSortDirection(String, SortDirection)
	 */
	@Override
	public void setSortDirection(String sFieldId, SortDirection eDirection) {
		if (eDirection != null) {
			aSortFields.put(sFieldId, eDirection);
		} else {
			aSortFields.remove(sFieldId);
		}
	}

	/**
	 * @see RemoteDataModel#setWindow(int, int, Callback)
	 */
	@Override
	public void setWindow(int nQueryStart, int nQueryLimit,
		final Callback<RemoteDataModel<DataModel<String>>> rCallback) {
		if (aCurrentData == null) {
			aCurrentData = new ArrayList<DataModel<String>>(nQueryLimit);
		} else if (nQueryLimit != nWindowSize) {
			aCurrentData.clear();
		}

		nWindowSize = nQueryLimit;

		int nWindowEnd = nWindowStart + aCurrentData.size();
		int nEnd = nQueryStart + nWindowSize;

		if (nQueryStart > nWindowStart && nQueryStart < nWindowEnd) {
			nQueryLimit -= nWindowEnd - nQueryStart;
			nQueryStart = nWindowEnd;
		} else if (nEnd >= nWindowStart && nEnd < nWindowEnd) {
			nQueryLimit -= nEnd - nWindowStart;
		}

		StringDataElement aQueryData =
			createQueryData(nQueryStart, nQueryLimit);

		executeQuery(aQueryData, nQueryStart, nQueryLimit, rCallback);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + sQueryId + "]";
	}

	/**
	 * Lets this model use the filters and sorting of another query model
	 * instance.
	 *
	 * @param rOtherModel The other model
	 */
	public void useConstraints(QueryDataModel rOtherModel) {
		aFilters = rOtherModel.aFilters;
		aSortFields = rOtherModel.aSortFields;
	}

	/**
	 * Create a data element list that contains the query data for this model's
	 * filter criteria.
	 *
	 * @param nQueryStart The index of the first object to query
	 * @param nQueryLimit The maximum number of objects to query
	 */
	private StringDataElement createQueryData(int nQueryStart,
		int nQueryLimit) {
		StringDataElement aQueryData = new StringDataElement(sQueryId, null);

		aQueryData.setProperty(QUERY_START, nQueryStart);
		aQueryData.setProperty(QUERY_LIMIT, nQueryLimit);

		if (!aSortFields.isEmpty()) {
			aQueryData.setProperty(QUERY_SORT, aSortFields);
		}

		if (!aFilters.isEmpty()) {
			aQueryData.setProperty(QUERY_SEARCH, aFilters);
		}

		return aQueryData;
	}

	/**
	 * Executes a query from {@link #prepareWindow(int, Callback)}.
	 *
	 * @param aQueryData The query data
	 * @param nStart     The start index for the query
	 * @param nCount     The number of data model elements to query
	 * @param rCallback  The callback to be invoked when the query is finished
	 */
	private void executeQuery(StringDataElement aQueryData, final int nStart,
		final int nCount,
		final Callback<RemoteDataModel<DataModel<String>>> rCallback) {
		ServiceRegistry
			.getStorageService()
			.executeCommand(StorageService.QUERY, aQueryData,
				new AsyncCallback<QueryResultElement<DataModel<String>>>() {
					@Override
					public void onFailure(Throwable e) {
						rCallback.onError(e);
					}

					@Override
					public void onSuccess(
						QueryResultElement<DataModel<String>> rResult) {
						setCurrentData(rResult, nStart, nCount);

						rCallback.onSuccess(QueryDataModel.this);
					}
				});
	}

	/**
	 * Sets the current data of this model by converting query data elements
	 * into data models.
	 *
	 * @param rQueryResult The data elements to convert
	 * @param nStart       The starting index of the new elements
	 */
	private void setCurrentData(
		QueryResultElement<DataModel<String>> rQueryResult, int nStart,
		int nCount) {
		nQuerySize = rQueryResult.getQuerySize();

		if (nCount == nWindowSize || nCount == nQuerySize ||
			nCount != rQueryResult.getElementCount()) {
			aCurrentData.clear();
			nWindowStart = nStart;

			for (DataModel<String> rRow : rQueryResult) {
				aCurrentData.add(rRow);
			}
		} else if (nStart < nWindowStart) {
			int nLast = nWindowSize - 1;
			int nInsert = 0;

			nWindowStart = nStart;

			for (DataModel<String> rRow : rQueryResult) {
				aCurrentData.remove(nLast);
				aCurrentData.add(nInsert++, rRow);
			}
		} else if (nStart > nWindowStart) {
			nWindowStart += nCount;

			for (DataModel<String> rRow : rQueryResult) {
				aCurrentData.remove(0);
				aCurrentData.add(rRow);
			}
		}
	}
}
