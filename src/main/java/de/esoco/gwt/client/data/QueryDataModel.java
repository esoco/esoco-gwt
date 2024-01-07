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

import com.google.gwt.user.client.rpc.AsyncCallback;
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

	private String queryId;

	private int querySize;

	private transient int windowSize;

	private transient int windowStart;

	private transient List<DataModel<String>> currentData;

	private transient Map<String, String> filters = new HashMap<>();

	private transient Map<String, SortDirection> sortFields = new HashMap<>();

	/**
	 * Creates a new instance for a certain query. If the query size is not
	 * known it should be set to zero.
	 *
	 * @param queryId   The query ID
	 * @param querySize The (initial) query size (zero if unknown)
	 */
	public QueryDataModel(String queryId, int querySize) {
		this.queryId = queryId;
		this.querySize = querySize;
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
		return currentData.size();
	}

	/**
	 * @see DataModel#getElement(int)
	 */
	@Override
	public DataModel<String> getElement(int index) {
		return currentData.get(index - windowStart);
	}

	/**
	 * @see DataModel#getElementCount()
	 */
	@Override
	public int getElementCount() {
		return querySize;
	}

	/**
	 * @see FilterableDataModel#getFilter(String)
	 */
	@Override
	public String getFilter(String fieldId) {
		return filters.get(fieldId);
	}

	/**
	 * @see FilterableDataModel#getFilters()
	 */
	@Override
	public Map<String, String> getFilters() {
		return filters;
	}

	/**
	 * Returns the query ID of this model.
	 *
	 * @return The query ID
	 */
	public final String getQueryId() {
		return queryId;
	}

	/**
	 * @see SortableDataModel#getSortDirection(String)
	 */
	@Override
	public SortDirection getSortDirection(String fieldId) {
		return sortFields.get(fieldId);
	}

	/**
	 * @see RemoteDataModel#getWindowSize()
	 */
	@Override
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * @see RemoteDataModel#getWindowStart()
	 */
	@Override
	public int getWindowStart() {
		return windowStart;
	}

	/**
	 * Returns an iterator over the current data set that has been retrieved by
	 * the last call to {@link #setWindow(int, int, Callback)}.
	 *
	 * @see DataModel#iterator()
	 */
	@Override
	public Iterator<DataModel<String>> iterator() {
		return currentData.iterator();
	}

	/**
	 * The integer limit parameter defines the maximum number of rows to
	 * download.
	 *
	 * @see Downloadable#prepareDownload(String, int, Callback)
	 */
	@Override
	public void prepareDownload(String fileName, int maxRows,
		final Callback<String> callback) {
		if (currentData != null) {
			StringDataElement queryData = createQueryData(0, maxRows);

			queryData.setProperty(FILE_NAME, fileName);

			ServiceRegistry
				.getStorageService()
				.executeCommand(StorageService.PREPARE_DOWNLOAD, queryData,
					new AsyncCallback<StringDataElement>() {
						@Override
						public void onFailure(Throwable e) {
							callback.onError(e);
						}

						@Override
						public void onSuccess(StringDataElement downloadUrl) {
							callback.onSuccess(downloadUrl.getValue());
						}
					});
		}
	}

	/**
	 * @see FilterableDataModel#removeAllFilters()
	 */
	@Override
	public void removeAllFilters() {
		filters.clear();
	}

	/**
	 * @see SortableDataModel#removeSorting()
	 */
	@Override
	public void removeSorting() {
		sortFields.clear();
	}

	/**
	 * Resets the query size to force a re-initialization.
	 */
	public void resetQuerySize() {
		querySize = 0;
	}

	/**
	 * @see FilterableDataModel#setFilter(String, String)
	 */
	@Override
	public void setFilter(String fieldId, String filter) {
		if (filter != null && !filter.isEmpty()) {
			filters.put(fieldId, filter);
		} else {
			filters.remove(fieldId);
		}
	}

	/**
	 * @see FilterableDataModel#setFilters(Map)
	 */
	@Override
	public void setFilters(Map<String, String> filters) {
		filters.clear();
		filters.putAll(filters);
	}

	/**
	 * @see SortableDataModel#setSortDirection(String, SortDirection)
	 */
	@Override
	public void setSortDirection(String fieldId, SortDirection direction) {
		if (direction != null) {
			sortFields.put(fieldId, direction);
		} else {
			sortFields.remove(fieldId);
		}
	}

	/**
	 * @see RemoteDataModel#setWindow(int, int, Callback)
	 */
	@Override
	public void setWindow(int queryStart, int queryLimit,
		final Callback<RemoteDataModel<DataModel<String>>> callback) {
		if (currentData == null) {
			currentData = new ArrayList<DataModel<String>>(queryLimit);
		} else if (queryLimit != windowSize) {
			currentData.clear();
		}

		windowSize = queryLimit;

		int windowEnd = windowStart + currentData.size();
		int end = queryStart + windowSize;

		if (queryStart > windowStart && queryStart < windowEnd) {
			queryLimit -= windowEnd - queryStart;
			queryStart = windowEnd;
		} else if (end >= windowStart && end < windowEnd) {
			queryLimit -= end - windowStart;
		}

		StringDataElement queryData = createQueryData(queryStart, queryLimit);

		executeQuery(queryData, queryStart, queryLimit, callback);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + queryId + "]";
	}

	/**
	 * Lets this model use the filters and sorting of another query model
	 * instance.
	 *
	 * @param otherModel The other model
	 */
	public void useConstraints(QueryDataModel otherModel) {
		filters = otherModel.filters;
		sortFields = otherModel.sortFields;
	}

	/**
	 * Create a data element list that contains the query data for this model's
	 * filter criteria.
	 *
	 * @param queryStart The index of the first object to query
	 * @param queryLimit The maximum number of objects to query
	 */
	private StringDataElement createQueryData(int queryStart, int queryLimit) {
		StringDataElement queryData = new StringDataElement(queryId, null);

		queryData.setProperty(QUERY_START, queryStart);
		queryData.setProperty(QUERY_LIMIT, queryLimit);

		if (!sortFields.isEmpty()) {
			queryData.setProperty(QUERY_SORT, sortFields);
		}

		if (!filters.isEmpty()) {
			queryData.setProperty(QUERY_SEARCH, filters);
		}

		return queryData;
	}

	/**
	 * Executes a query from {@link #setWindow(int, int, Callback)}.
	 *
	 * @param queryData The query data
	 * @param start     The start index for the query
	 * @param count     The number of data model elements to query
	 * @param callback  The callback to be invoked when the query is finished
	 */
	private void executeQuery(StringDataElement queryData, final int start,
		final int count,
		final Callback<RemoteDataModel<DataModel<String>>> callback) {
		ServiceRegistry
			.getStorageService()
			.executeCommand(StorageService.QUERY, queryData,
				new AsyncCallback<QueryResultElement<DataModel<String>>>() {
					@Override
					public void onFailure(Throwable e) {
						callback.onError(e);
					}

					@Override
					public void onSuccess(
						QueryResultElement<DataModel<String>> result) {
						setCurrentData(result, start, count);

						callback.onSuccess(QueryDataModel.this);
					}
				});
	}

	/**
	 * Sets the current data of this model by converting query data elements
	 * into data models.
	 *
	 * @param queryResult The data elements to convert
	 * @param start       The starting index of the new elements
	 */
	private void setCurrentData(
		QueryResultElement<DataModel<String>> queryResult, int start,
		int count) {
		querySize = queryResult.getQuerySize();

		if (count == windowSize || count == querySize ||
			count != queryResult.getElementCount()) {
			currentData.clear();
			windowStart = start;

			for (DataModel<String> row : queryResult) {
				currentData.add(row);
			}
		} else if (start < windowStart) {
			int last = windowSize - 1;
			int insert = 0;

			windowStart = start;

			for (DataModel<String> row : queryResult) {
				currentData.remove(last);
				currentData.add(insert++, row);
			}
		} else if (start > windowStart) {
			windowStart += count;

			for (DataModel<String> row : queryResult) {
				currentData.remove(0);
				currentData.add(row);
			}
		}
	}
}
