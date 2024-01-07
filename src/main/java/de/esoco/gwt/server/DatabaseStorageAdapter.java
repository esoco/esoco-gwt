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
package de.esoco.gwt.server;

import de.esoco.data.element.HierarchicalDataObject;
import de.esoco.data.element.QueryResultElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.element.StringMapDataElement;
import de.esoco.data.storage.AbstractStorageAdapter;
import de.esoco.data.storage.StorageAdapterId;
import de.esoco.entity.Entity;
import de.esoco.entity.EntityDefinition;
import de.esoco.entity.EntityManager;
import de.esoco.entity.EntityRelationTypes.HierarchicalQueryMode;
import de.esoco.gwt.shared.ServiceException;
import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.Predicate;
import de.esoco.lib.expression.Predicates;
import de.esoco.lib.expression.StringFunctions;
import de.esoco.lib.expression.function.CalendarFunctions;
import de.esoco.lib.expression.predicate.FunctionPredicate;
import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.property.SortDirection;
import de.esoco.lib.text.TextUtil;
import de.esoco.storage.Query;
import de.esoco.storage.QueryPredicate;
import de.esoco.storage.QueryResult;
import de.esoco.storage.Storage;
import de.esoco.storage.StorageException;
import de.esoco.storage.StorageManager;
import de.esoco.storage.StorageRelationTypes;
import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static de.esoco.data.DataRelationTypes.CHILD_STORAGE_ADAPTER_ID;
import static de.esoco.data.DataRelationTypes.FLAG_ATTRIBUTE;
import static de.esoco.data.DataRelationTypes.STORAGE_ADAPTER_IDS;
import static de.esoco.entity.EntityRelationTypes.HIERARCHICAL_QUERY_MODE;
import static de.esoco.entity.EntityRelationTypes.HIERARCHY_CHILD_PREDICATE;
import static de.esoco.entity.EntityRelationTypes.HIERARCHY_ROOT_PREDICATE;
import static de.esoco.lib.expression.CollectionPredicates.elementOf;
import static de.esoco.lib.expression.Predicates.equalTo;
import static de.esoco.lib.expression.Predicates.greaterOrEqual;
import static de.esoco.lib.expression.Predicates.greaterThan;
import static de.esoco.lib.expression.Predicates.isNull;
import static de.esoco.lib.expression.Predicates.lessOrEqual;
import static de.esoco.lib.expression.Predicates.lessThan;
import static de.esoco.lib.model.FilterableDataModel.CONSTRAINT_COMPARISON_CHARS;
import static de.esoco.lib.model.FilterableDataModel.CONSTRAINT_OR_PREFIX;
import static de.esoco.lib.model.FilterableDataModel.CONSTRAINT_SEPARATOR;
import static de.esoco.lib.model.FilterableDataModel.CONSTRAINT_SEPARATOR_ESCAPE;
import static de.esoco.lib.model.FilterableDataModel.NULL_CONSTRAINT_VALUE;
import static de.esoco.lib.property.StorageProperties.QUERY_LIMIT;
import static de.esoco.lib.property.StorageProperties.QUERY_SEARCH;
import static de.esoco.lib.property.StorageProperties.QUERY_SORT;
import static de.esoco.lib.property.StorageProperties.QUERY_START;
import static de.esoco.storage.StoragePredicates.like;
import static de.esoco.storage.StoragePredicates.similarTo;
import static de.esoco.storage.StoragePredicates.sortBy;

/**
 * A storage adapter for accessing database storages.
 *
 * @author eso
 */
public class DatabaseStorageAdapter extends AbstractStorageAdapter {

	private static final long serialVersionUID = 1L;

	private static int nextQueryId = 1;

	private final DataElementFactory dataElementFactory;

	private final Lock lock = new ReentrantLock();

	private QueryPredicate<Entity> baseQuery;

	private QueryPredicate<Entity> currentQuery;

	private Predicate<? super Entity> defaultConstraints;

	private Predicate<? super Entity> defaultSortCriteria;

	private Function<Entity, List<String>> getAttributes;

	private List<ColumnDefinition> columns;

	private List<Entity> lastQueryResult;

	/**
	 * Creates a new instance that is associated with a certain data element
	 * factory.
	 *
	 * @param dataElementFactory The data element factory to create the result
	 *                           objects with
	 */
	public <E extends Entity> DatabaseStorageAdapter(
		DataElementFactory dataElementFactory) {
		this.dataElementFactory = dataElementFactory;
	}

	/**
	 * @see AbstractStorageAdapter#getColumns()
	 */
	@Override
	public List<ColumnDefinition> getColumns() {
		return columns;
	}

	/**
	 * Returns the current query of this instance. This will return the query
	 * predicate that had been created by the last execution of the method
	 * {@link #performQuery(StringDataElement)} .
	 *
	 * @return The current query predicate or NULL if no query has been
	 * executed
	 * yet
	 */
	@Override
	public QueryPredicate<Entity> getCurrentQueryCriteria() {
		return currentQuery;
	}

	/**
	 * @see AbstractStorageAdapter#getStorageDescription()
	 */
	@Override
	public String getStorageDescription() {
		return String.format("%s, %s, %s", baseQuery, defaultConstraints,
			defaultSortCriteria);
	}

	/**
	 * Performs a query on a {@link Storage} and returns a data element that
	 * contains the result.
	 *
	 * @param queryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws StorageException If accessing the storage fails
	 */
	@Override
	public QueryResultElement<DataModel<String>> performQuery(
		StringDataElement queryParams) throws StorageException {
		lock.lock();

		try {
			int start = queryParams.getIntProperty(QUERY_START, 0);
			int limit = queryParams.getIntProperty(QUERY_LIMIT, 0);
			int querySize;

			List<DataModel<String>> queryRows =
				new ArrayList<DataModel<String>>();

			Map<String, String> constraints =
				queryParams.getProperty(QUERY_SEARCH, null);
			Map<String, SortDirection> sortFields =
				queryParams.getProperty(QUERY_SORT, null);

			Storage storage =
				StorageManager.getStorage(baseQuery.getQueryType());

			try {
				currentQuery =
					createFullQuery(baseQuery, constraints, sortFields);

				querySize =
					executeQuery(storage, currentQuery, start, limit,
						queryRows,
						baseQuery.get(FLAG_ATTRIBUTE));

				return new QueryResultElement<DataModel<String>>(
					"DBQ" + nextQueryId++, queryRows, querySize);
			} finally {
				storage.release();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Allows to query the position of an entity with a certain ID in the query
	 * result of this adapter.
	 *
	 * @param id The ID of the entity to query the position of
	 * @return The entity position or -1 if undefined
	 * @throws StorageException If the database query fails
	 */
	public int positionOf(Object id) throws StorageException {
		return queryPositionOrSize(id);
	}

	/**
	 * Allows to query the position of an entity with a certain ID in the query
	 * result of this adapter.
	 *
	 * @return The entity position or -1 if undefined
	 * @throws StorageException If the database query fails
	 */
	public int querySize() throws StorageException {
		return queryPositionOrSize(null);
	}

	/**
	 * Sets the query parameters of this instance.
	 *
	 * @param baseQuery           A query predicate containing the base query
	 * @param getAttributes       A function that retrieves the attribute value
	 *                            strings from an entity
	 * @param defaultCriteria     An optional predicate containing default
	 *                            criteria to be used if no specific
	 *                            constraints
	 *                            are given
	 * @param defaultSortCriteria An optional predicate containing default sort
	 *                            criteria to be used if no specific sort
	 *                            fields
	 *                            are given
	 * @param columns             The query columns
	 */
	@SuppressWarnings("unchecked")
	public <E extends Entity> void setQueryParameters(
		QueryPredicate<E> baseQuery,
		Function<Entity, List<String>> getAttributes,
		Predicate<? super E> defaultCriteria,
		Predicate<? super E> defaultSortCriteria,
		List<ColumnDefinition> columns) {
		lock.lock();

		try {
			this.baseQuery = (QueryPredicate<Entity>) baseQuery;
			this.getAttributes = getAttributes;
			this.columns = columns;
			this.defaultConstraints =
				(Predicate<? super Entity>) defaultCriteria;
			this.defaultSortCriteria =
				(Predicate<? super Entity>) defaultSortCriteria;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the query predicate.
	 *
	 * @return The query predicate
	 */
	protected final QueryPredicate<Entity> getQueryPredicate() {
		return baseQuery;
	}

	/**
	 * Internal method to apply optional search constraints to a query
	 * predicate
	 * if they are available.
	 *
	 * @param query            The query to apply the search constraints to
	 * @param queryConstraints A {@link StringMapDataElement} containing the
	 *                         search constraints map or NULL for none
	 * @return A new query predicate if constraints are available or else the
	 * unchanged input predicate
	 */
	private QueryPredicate<Entity> applyQueryConstraints(
		QueryPredicate<Entity> query, Map<String, String> queryConstraints) {
		Predicate<? super Entity> constraints = null;

		if (queryConstraints != null) {
			EntityDefinition<Entity> def =
				EntityManager.getEntityDefinition(query.getQueryType());

			for (Entry<String, String> constraint :
				queryConstraints.entrySet()) {
				String attrName = constraint.getKey();
				String attrConstraint = constraint.getValue().trim();

				RelationType<?> attr = def.getAttribute(attrName);

				if (attr == null) {
					throw new IllegalArgumentException(
						"Unknown search attribute: " + attr);
				}

				if (attrConstraint.length() > 1) {
					boolean attrOr =
						attrConstraint.charAt(0) == CONSTRAINT_OR_PREFIX;

					Predicate<? super Entity> attrConstraints = null;

					for (String constraintElem : attrConstraint.split(
						CONSTRAINT_SEPARATOR)) {
						boolean or =
							constraintElem.charAt(0) == CONSTRAINT_OR_PREFIX;

						constraintElem = constraintElem.substring(1);

						Predicate<? super Entity> attrPredicate =
							createAttributeConstraint(attr, constraintElem);

						attrConstraints =
							combinePredicates(attrConstraints, attrPredicate,
								or);
					}

					constraints =
						combinePredicates(constraints, attrConstraints,
							attrOr);
				}
			}
		}

		return checkNewQuery(query, constraints);
	}

	/**
	 * Internal method to apply optional sort fields to a query predicate if
	 * they are available.
	 *
	 * @param query      The query predicate to apply the sort fields to
	 * @param sortFields A {@link StringMapDataElement} containing the sort
	 *                   field map or NULL for none
	 * @return A new query predicate if sort fields are available or else the
	 * unchanged input predicate
	 */
	private QueryPredicate<Entity> applySortFields(QueryPredicate<Entity> query,
		Map<String, SortDirection> sortFields) {
		Predicate<? super Entity> sortCriteria = null;

		if (sortFields != null) {
			EntityDefinition<Entity> def =
				EntityManager.getEntityDefinition(query.getQueryType());

			for (Entry<String, SortDirection> attrSort :
				sortFields.entrySet()) {
				String attrName = attrSort.getKey();
				RelationType<?> attr = def.getAttribute(attrName);

				if (attr == null) {
					throw new IllegalArgumentException(
						"Unknown attribute: " + attrName);
				}

				sortCriteria = Predicates.and(sortCriteria,
					sortBy(attr, attrSort.getValue()));
			}
		} else {
			sortCriteria = defaultSortCriteria;
		}

		return checkNewQuery(query, sortCriteria);
	}

	/**
	 * Helper method to check whether a new new query needs to be created if
	 * the
	 * criteria have changed. If not the original query predicate will be
	 * returned.
	 *
	 * @param query         The query predicate
	 * @param extraCriteria The new (or same
	 * @return Either the same or a new query predicate
	 */
	private QueryPredicate<Entity> checkNewQuery(QueryPredicate<Entity> query,
		Predicate<? super Entity> extraCriteria) {
		Predicate<? super Entity> queryCriteria = query.getCriteria();
		Predicate<? super Entity> criteria =
			Predicates.and(queryCriteria, extraCriteria);

		if (criteria != queryCriteria) {
			QueryPredicate<Entity> newQuery =
				new QueryPredicate<Entity>(query.getQueryType(), criteria);

			ObjectRelations.copyRelations(query, newQuery, false);
			query = newQuery;
		}

		return query;
	}

	/**
	 * Combines to predicates with either a logical OR or AND. Any of the
	 * predicate arguments can be NULL.
	 *
	 * @param first  The first predicate
	 * @param second The second predicate
	 * @param or     TRUE for OR, FALSE for AND
	 * @return The resulting predicate
	 */
	private Predicate<? super Entity> combinePredicates(
		Predicate<? super Entity> first, Predicate<? super Entity> second,
		boolean or) {
		if (or) {
			first = Predicates.or(first, second);
		} else {
			first = Predicates.and(first, second);
		}

		return first;
	}

	/**
	 * Creates a constraint predicate for a certain attributes.
	 *
	 * @param attr       The attribute to create the constraint for
	 * @param constraint The constraint string
	 * @return The predicate containing the attribute constraint or NULL if the
	 * constraint is not valid
	 */
	@SuppressWarnings({ "unchecked" })
	private Predicate<Entity> createAttributeConstraint(RelationType<?> attr,
		String constraint) {
		Class<?> datatype = attr.getTargetType();
		Predicate<?> attribute = null;
		char comparison = constraint.charAt(0);

		if (CONSTRAINT_COMPARISON_CHARS.indexOf(constraint.charAt(0)) >= 0) {
			constraint = constraint.substring(1);
		}

		constraint = constraint.replaceAll(CONSTRAINT_SEPARATOR_ESCAPE,
			CONSTRAINT_SEPARATOR);

		if (constraint.length() > 0) {
			if (comparison == '#') {
				String[] rawValues = constraint.split(",");

				List<Object> values = new ArrayList<Object>(rawValues.length);

				for (String value : rawValues) {
					values.add(parseConstraintValue(value.trim(), datatype));
				}

				attribute = attr.is(elementOf(values));
			} else {
				Object value = parseConstraintValue(constraint, datatype);

				if (value instanceof Comparable) {
					attribute =
						createComparableConstraint(attr, comparison, value,
							constraint);
				}
			}
		}

		return (Predicate<Entity>) attribute;
	}

	/**
	 * Creates a query constraint predicate for a comparable value.
	 *
	 * @param attr       The attribute to create the predicate for
	 * @param comparison The comparison to perform
	 * @param value      The comparable value
	 * @param constraint The constraint string
	 * @return A predicate containing the query constraint
	 */
	@SuppressWarnings({ "unchecked" })
	private <C extends Comparable<C>> Predicate<?> createComparableConstraint(
		RelationType<?> attr, char comparison, Object value,
		String constraint) {
		Predicate<?> attribute;

		RelationType<C> comparableAttr = (RelationType<C>) attr;
		C compareValue = (C) value;

		switch (comparison) {
			case '<':
				attribute = comparableAttr.is(lessThan(compareValue));
				break;

			case '>':
				attribute = comparableAttr.is(greaterThan(compareValue));
				break;

			case '\u2264': // <=
				attribute = comparableAttr.is(lessOrEqual(compareValue));
				break;

			case '\u2265': // >=
				attribute = comparableAttr.is(greaterOrEqual(compareValue));
				break;

			case '~':
				attribute = comparableAttr.is(similarTo(constraint));
				break;

			default:
				attribute = createValueConstraint(comparableAttr, comparison,
					compareValue, constraint);
		}

		return attribute;
	}

	/**
	 * Creates the final query predicate for this instance by applying
	 * constraints and sort fields (if available) to the base query predicate.
	 *
	 * @param baseQuery   The base query predicate
	 * @param constraints The additional query constraints (NULL for none)
	 * @param sortFields  The optional sort fields (NULL for none)
	 * @return The total size of the query
	 * @throws StorageException If accessing the storage fails
	 * @throws ServiceException If creating a result data object fails
	 */
	private QueryPredicate<Entity> createFullQuery(
		QueryPredicate<Entity> baseQuery, Map<String, String> constraints,
		Map<String, SortDirection> sortFields) {
		Class<Entity> queryType = baseQuery.getQueryType();
		Predicate<? super Entity> criteria = baseQuery.getCriteria();

		HierarchicalQueryMode hierarchyMode =
			baseQuery.get(HIERARCHICAL_QUERY_MODE);

		boolean noConstraints = constraints == null || constraints.size() == 0;
		boolean hierarchical = hierarchyMode == HierarchicalQueryMode.ALWAYS ||
			hierarchyMode == HierarchicalQueryMode.UNCONSTRAINED &&
				noConstraints;

		if (hierarchical) {
			Predicate<? super Entity> isHierarchyRoot =
				baseQuery.get(HIERARCHY_ROOT_PREDICATE);

			if (isHierarchyRoot == null) {
				RelationType<? extends Entity> parentAttribute = EntityManager
					.getEntityDefinition(queryType)
					.getParentAttribute();

				if (parentAttribute != null) {
					isHierarchyRoot = parentAttribute.is(isNull());
				}
			}

			if (isHierarchyRoot != null) {
				criteria = Predicates.and(criteria, isHierarchyRoot);
			}
		}

		if (noConstraints && defaultConstraints != null) {
			criteria = Predicates.and(criteria, defaultConstraints);
		}

		QueryPredicate<Entity> fullQuery = baseQuery;

		if (criteria != baseQuery.getCriteria()) {
			fullQuery = new QueryPredicate<Entity>(queryType, criteria);

			ObjectRelations.copyRelations(baseQuery, fullQuery, false);
		}

		fullQuery = applyQueryConstraints(fullQuery, constraints);
		fullQuery = applySortFields(fullQuery, sortFields);

		return fullQuery;
	}

	/**
	 * Creates a comparison predicate for a single-day constraint.
	 *
	 * @param attribute    The attribute to create the comparison for
	 * @param compareValue The compare value
	 * @param negate       TRUE to negate the comparison
	 * @return The comparison predicate
	 */
	private Predicate<Entity> createSingleDayComparison(
		RelationType<Date> attribute, Date compareValue, boolean negate) {
		Calendar calendar = Calendar.getInstance();
		Predicate<Entity> dayComparison;
		Predicate<Entity> lastDate;

		calendar.setTime(compareValue);
		CalendarFunctions.clearTime(calendar);
		compareValue = calendar.getTime();

		if (negate) {
			dayComparison = attribute.is(lessThan(compareValue));
		} else {
			dayComparison = attribute.is(greaterOrEqual(compareValue));
		}

		calendar.add(Calendar.DAY_OF_MONTH, 1);
		compareValue = calendar.getTime();

		if (negate) {
			lastDate = attribute.is(greaterOrEqual(compareValue));
		} else {
			lastDate = attribute.is(lessThan(compareValue));
		}

		dayComparison = dayComparison.and(lastDate);

		return dayComparison;
	}

	/**
	 * Creates a single value comparison query constraint predicate.
	 *
	 * @param attr           The attribute to create the predicate for
	 * @param comparisonChar The comparison to perform
	 * @param value          The comparable value
	 * @param constraint     The constraint string
	 * @return A predicate containing the query constraint
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Predicate<?> createValueConstraint(RelationType<?> attr,
		char comparisonChar, Comparable value, String constraint) {
		Predicate<?> attribute;
		boolean negate = (comparisonChar == '\u2260'); // !=

		constraint = StorageManager.convertToSqlConstraint(constraint);

		if (NULL_CONSTRAINT_VALUE.equals(value)) {
			value = null;
		}

		if (value instanceof Date) {
			attribute = createSingleDayComparison((RelationType<Date>) attr,
				(Date) value, negate);
		} else {
			Class<?> datatype = attr.getTargetType();
			Predicate<Object> comparison;

			if (constraint.indexOf('%') >= 0 || constraint.indexOf('_') >= 0) {
				comparison = like(constraint);
			} else {
				comparison = equalTo(value);
			}

			if (negate) {
				comparison = Predicates.not(comparison);
			}

			if ((datatype == String.class || datatype.isEnum()) &&
				!TextUtil.containsUpperCase(constraint)) {
				Function<Relatable, String> attrFunction = StringFunctions
					.toLowerCase()
					.from((Function<Relatable, String>) attr);

				attribute = new FunctionPredicate<Entity, String>(attrFunction,
					comparison);
			} else {
				attribute = attr.is(comparison);
			}
		}

		return attribute;
	}

	/**
	 * Executes a storage query with certain parameters. The query object will
	 * be closed after successful execution.
	 *
	 * @param entities      The predicate of the query to execute
	 * @param start         The starting index of the entities to query
	 * @param limit         The maximum number of entities to retrieve
	 * @param resultRows    The list to store the queried data objects in
	 * @param flagAttribute The attribute that should be set as the data
	 *                         objects
	 *                      flag
	 * @return The total size of the query
	 * @throws StorageException If accessing the storage fails
	 * @throws ServiceException If creating a result data object fails
	 */
	private int executeQuery(Storage storage,
		QueryPredicate<Entity> entityQuery, int start, int limit,
		List<DataModel<String>> resultRows, RelationType<?> flagAttribute)
		throws StorageException {
		Predicate<? super Entity> childCriteria =
			entityQuery.get(HIERARCHY_CHILD_PREDICATE);

		int querySize;

		try (Query<Entity> query = storage.query(entityQuery)) {
			query.set(StorageRelationTypes.QUERY_LIMIT, limit);
			query.set(StorageRelationTypes.QUERY_OFFSET, start);

			QueryResult<Entity> entities = query.execute();

			querySize = query.size();
			lastQueryResult = new ArrayList<Entity>(Math.min(limit, 1000));

			while (limit-- > 0 && entities.hasNext()) {
				Entity entity = entities.next();
				Set<String> flags = null;

				if (flagAttribute != null) {
					Object flagValue = entity.get(flagAttribute);

					if (flagValue != null) {
						flags = Collections.singleton(flagValue.toString());
					}
				}

				HierarchicalDataObject dataObject =
					dataElementFactory.createEntityDataObject(entity, start++,
						childCriteria, defaultSortCriteria, getAttributes,
						flags, true);

				resultRows.add(dataObject);

				StorageAdapterId childAdapterId =
					entity.get(CHILD_STORAGE_ADAPTER_ID);

				if (childAdapterId != null) {
					get(STORAGE_ADAPTER_IDS).add(childAdapterId);
					entity.deleteRelation(CHILD_STORAGE_ADAPTER_ID);
				}

				// keep result entities to prevent garbage collection of child
				// list
				// storage adapters which are only stored in weak references
				lastQueryResult.add(entity);
			}
		}

		return querySize;
	}

	/**
	 * Allows to query the position of an entity with a certain ID in the query
	 * result of this adapter.
	 *
	 * @param id The ID of the entity to query the position or NULL for the
	 *           query size
	 * @return The entity position or -1 if undefined
	 * @throws StorageException If the database query fails
	 */
	private int queryPositionOrSize(Object id) throws StorageException {
		int result;

		lock.lock();

		try {
			Storage storage =
				StorageManager.getStorage(baseQuery.getQueryType());

			try (Query<Entity> query = storage.query(
				createFullQuery(baseQuery, null, null))) {
				result = (id != null ? query.positionOf(id) : query.size());
			} finally {
				storage.release();
			}
		} finally {
			lock.unlock();
		}

		return result;
	}
}
