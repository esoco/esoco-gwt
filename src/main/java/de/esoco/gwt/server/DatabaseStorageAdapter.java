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

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;

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

import static de.esoco.storage.StoragePredicates.similarTo;
import static de.esoco.storage.StoragePredicates.like;
import static de.esoco.storage.StoragePredicates.sortBy;

/**
 * A storage adapter for accessing database storages.
 *
 * @author eso
 */
public class DatabaseStorageAdapter extends AbstractStorageAdapter {

	private static final long serialVersionUID = 1L;

	private static int nNextQueryId = 1;

	private final DataElementFactory rDataElementFactory;

	private final Lock aLock = new ReentrantLock();

	private QueryPredicate<Entity> qBaseQuery;

	private QueryPredicate<Entity> qCurrentQuery;

	private Predicate<? super Entity> pDefaultConstraints;

	private Predicate<? super Entity> pDefaultSortCriteria;

	private Function<Entity, List<String>> fGetAttributes;

	private List<ColumnDefinition> rColumns;

	private List<Entity> aLastQueryResult;

	/**
	 * Creates a new instance that is associated with a certain data element
	 * factory.
	 *
	 * @param rDataElementFactory The data element factory to create the result
	 *                            objects with
	 */
	public <E extends Entity> DatabaseStorageAdapter(
		DataElementFactory rDataElementFactory) {
		this.rDataElementFactory = rDataElementFactory;
	}

	/**
	 * @see AbstractStorageAdapter#getColumns()
	 */
	@Override
	public List<ColumnDefinition> getColumns() {
		return rColumns;
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
		return qCurrentQuery;
	}

	/**
	 * @see AbstractStorageAdapter#getStorageDescription()
	 */
	@Override
	public String getStorageDescription() {
		return String.format("%s, %s, %s", qBaseQuery, pDefaultConstraints,
			pDefaultSortCriteria);
	}

	/**
	 * Performs a query on a {@link Storage} and returns a data element that
	 * contains the result.
	 *
	 * @param rQueryParams A data element list containing the query parameters
	 * @return A data element containing the query result
	 * @throws StorageException If accessing the storage fails
	 */
	@Override
	public QueryResultElement<DataModel<String>> performQuery(
		StringDataElement rQueryParams) throws StorageException {
		aLock.lock();

		try {
			int nStart = rQueryParams.getIntProperty(QUERY_START, 0);
			int nLimit = rQueryParams.getIntProperty(QUERY_LIMIT, 0);
			int nQuerySize;

			List<DataModel<String>> aQueryRows =
				new ArrayList<DataModel<String>>();

			Map<String, String> rConstraints =
				rQueryParams.getProperty(QUERY_SEARCH, null);
			Map<String, SortDirection> rSortFields =
				rQueryParams.getProperty(QUERY_SORT, null);

			Storage rStorage =
				StorageManager.getStorage(qBaseQuery.getQueryType());

			try {
				qCurrentQuery =
					createFullQuery(qBaseQuery, rConstraints, rSortFields);

				nQuerySize =
					executeQuery(rStorage, qCurrentQuery, nStart, nLimit,
						aQueryRows, qBaseQuery.get(FLAG_ATTRIBUTE));

				return new QueryResultElement<DataModel<String>>(
					"DBQ" + nNextQueryId++, aQueryRows, nQuerySize);
			} finally {
				rStorage.release();
			}
		} finally {
			aLock.unlock();
		}
	}

	/**
	 * Allows to query the position of an entity with a certain ID in the query
	 * result of this adapter.
	 *
	 * @param rId The ID of the entity to query the position of
	 * @return The entity position or -1 if undefined
	 * @throws StorageException If the database query fails
	 */
	public int positionOf(Object rId) throws StorageException {
		return queryPositionOrSize(rId);
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
	 * @param pBaseQuery           A query predicate containing the base query
	 * @param fGetAttributes       A function that retrieves the attribute
	 *                                value
	 *                             strings from an entity
	 * @param pDefaultCriteria     An optional predicate containing default
	 *                             criteria to be used if no specific
	 *                             constraints are given
	 * @param pDefaultSortCriteria An optional predicate containing default
	 *                                sort
	 *                             criteria to be used if no specific sort
	 *                             fields are given
	 * @param rColumns             The query columns
	 */
	@SuppressWarnings("unchecked")
	public <E extends Entity> void setQueryParameters(
		QueryPredicate<E> pBaseQuery,
		Function<Entity, List<String>> fGetAttributes,
		Predicate<? super E> pDefaultCriteria,
		Predicate<? super E> pDefaultSortCriteria,
		List<ColumnDefinition> rColumns) {
		aLock.lock();

		try {
			this.qBaseQuery = (QueryPredicate<Entity>) pBaseQuery;
			this.fGetAttributes = fGetAttributes;
			this.rColumns = rColumns;
			this.pDefaultConstraints =
				(Predicate<? super Entity>) pDefaultCriteria;
			this.pDefaultSortCriteria =
				(Predicate<? super Entity>) pDefaultSortCriteria;
		} finally {
			aLock.unlock();
		}
	}

	/**
	 * Returns the query predicate.
	 *
	 * @return The query predicate
	 */
	protected final QueryPredicate<Entity> getQueryPredicate() {
		return qBaseQuery;
	}

	/**
	 * Internal method to apply optional search constraints to a query
	 * predicate
	 * if they are available.
	 *
	 * @param pQuery       The query to apply the search constraints to
	 * @param rConstraints A {@link StringMapDataElement} containing the search
	 *                     constraints map or NULL for none
	 * @return A new query predicate if constraints are available or else the
	 * unchanged input predicate
	 */
	private QueryPredicate<Entity> applyQueryConstraints(
		QueryPredicate<Entity> pQuery, Map<String, String> rConstraints) {
		Predicate<? super Entity> pConstraints = null;

		if (rConstraints != null) {
			EntityDefinition<Entity> rDef =
				EntityManager.getEntityDefinition(pQuery.getQueryType());

			for (Entry<String, String> rConstraint : rConstraints.entrySet()) {
				String sAttr = rConstraint.getKey();
				String sAttrConstraint = rConstraint.getValue().trim();

				RelationType<?> rAttr = rDef.getAttribute(sAttr);

				if (rAttr == null) {
					throw new IllegalArgumentException(
						"Unknown search attribute: " + sAttr);
				}

				if (sAttrConstraint != null && sAttrConstraint.length() > 1) {
					boolean bAttrOr =
						sAttrConstraint.charAt(0) == CONSTRAINT_OR_PREFIX;

					Predicate<? super Entity> pAttrConstraints = null;

					for (String sConstraint : sAttrConstraint.split(
						CONSTRAINT_SEPARATOR)) {
						boolean bOr =
							sConstraint.charAt(0) == CONSTRAINT_OR_PREFIX;

						sConstraint = sConstraint.substring(1);

						Predicate<? super Entity> pAttrConstraint =
							createAttributeConstraint(rAttr, sConstraint);

						pAttrConstraints =
							combinePredicates(pAttrConstraints,
								pAttrConstraint,
								bOr);
					}

					pConstraints =
						combinePredicates(pConstraints, pAttrConstraints,
							bAttrOr);
				}
			}
		}

		return checkNewQuery(pQuery, pConstraints);
	}

	/**
	 * Internal method to apply optional sort fields to a query predicate if
	 * they are available.
	 *
	 * @param pQuery      The query predicate to apply the sort fields to
	 * @param rSortFields A {@link StringMapDataElement} containing the sort
	 *                    field map or NULL for none
	 * @return A new query predicate if sort fields are available or else the
	 * unchanged input predicate
	 */
	private QueryPredicate<Entity> applySortFields(
		QueryPredicate<Entity> pQuery,
		Map<String, SortDirection> rSortFields) {
		Predicate<? super Entity> pSortCriteria = null;

		if (rSortFields != null) {
			EntityDefinition<Entity> rDef =
				EntityManager.getEntityDefinition(pQuery.getQueryType());

			for (Entry<String, SortDirection> rAttrSort :
				rSortFields.entrySet()) {
				String sAttr = rAttrSort.getKey();
				RelationType<?> rAttr = rDef.getAttribute(sAttr);

				if (rAttr == null) {
					throw new IllegalArgumentException(
						"Unknown attribute: " + sAttr);
				}

				pSortCriteria = Predicates.and(pSortCriteria,
					sortBy(rAttr, rAttrSort.getValue()));
			}
		} else {
			pSortCriteria = pDefaultSortCriteria;
		}

		return checkNewQuery(pQuery, pSortCriteria);
	}

	/**
	 * Helper method to check whether a new new query needs to be created if
	 * the
	 * criteria have changed. If not the original query predicate will be
	 * returned.
	 *
	 * @param pQuery         The query predicate
	 * @param pExtraCriteria The new (or same
	 * @return Either the same or a new query predicate
	 */
	private QueryPredicate<Entity> checkNewQuery(QueryPredicate<Entity> pQuery,
		Predicate<? super Entity> pExtraCriteria) {
		Predicate<? super Entity> pQueryCriteria = pQuery.getCriteria();
		Predicate<? super Entity> pCriteria =
			Predicates.and(pQueryCriteria, pExtraCriteria);

		if (pCriteria != pQueryCriteria) {
			QueryPredicate<Entity> pNewQuery =
				new QueryPredicate<Entity>(pQuery.getQueryType(), pCriteria);

			ObjectRelations.copyRelations(pQuery, pNewQuery, false);
			pQuery = pNewQuery;
		}

		return pQuery;
	}

	/**
	 * Combines to predicates with either a logical OR or AND. Any of the
	 * predicate arguments can be NULL.
	 *
	 * @param pFirst  The first predicate
	 * @param pSecond The second predicate
	 * @param bOr     TRUE for OR, FALSE for AND
	 * @return The resulting predicate
	 */
	private Predicate<? super Entity> combinePredicates(
		Predicate<? super Entity> pFirst, Predicate<? super Entity> pSecond,
		boolean bOr) {
		if (bOr) {
			pFirst = Predicates.or(pFirst, pSecond);
		} else {
			pFirst = Predicates.and(pFirst, pSecond);
		}

		return pFirst;
	}

	/**
	 * Creates a constraint predicate for a certain attributes.
	 *
	 * @param rAttr       The attribute to create the constraint for
	 * @param sConstraint The constraint string
	 * @return The predicate containing the attribute constraint or NULL if the
	 * constraint is not valid
	 */
	@SuppressWarnings({ "unchecked" })
	private Predicate<Entity> createAttributeConstraint(RelationType<?> rAttr,
		String sConstraint) {
		Class<?> rDatatype = rAttr.getTargetType();
		Predicate<?> pAttribute = null;
		char cComparison = sConstraint.charAt(0);

		if (CONSTRAINT_COMPARISON_CHARS.indexOf(sConstraint.charAt(0)) >= 0) {
			sConstraint = sConstraint.substring(1);
		}

		sConstraint = sConstraint.replaceAll(CONSTRAINT_SEPARATOR_ESCAPE,
			CONSTRAINT_SEPARATOR);

		if (sConstraint.length() > 0) {
			if (cComparison == '#') {
				String[] aRawValues = sConstraint.split(",");

				List<Object> aValues =
					new ArrayList<Object>(aRawValues.length);

				for (String sValue : aRawValues) {
					aValues.add(parseConstraintValue(sValue.trim(),
						rDatatype));
				}

				pAttribute = rAttr.is(elementOf(aValues));
			} else {
				Object rValue = parseConstraintValue(sConstraint, rDatatype);

				if (rValue instanceof Comparable) {
					pAttribute =
						createComparableConstraint(rAttr, cComparison, rValue,
							sConstraint);
				}
			}
		}

		return (Predicate<Entity>) pAttribute;
	}

	/**
	 * Creates a query constraint predicate for a comparable value.
	 *
	 * @param rAttr       The attribute to create the predicate for
	 * @param cComparison The comparison to perform
	 * @param rValue      The comparable value
	 * @param sConstraint The constraint string
	 * @return A predicate containing the query constraint
	 */
	@SuppressWarnings({ "unchecked" })
	private <C extends Comparable<C>> Predicate<?> createComparableConstraint(
		RelationType<?> rAttr, char cComparison, Object rValue,
		String sConstraint) {
		Predicate<?> pAttribute;

		RelationType<C> rComparableAttr = (RelationType<C>) rAttr;
		C rCompareValue = (C) rValue;

		switch (cComparison) {
			case '<':
				pAttribute = rComparableAttr.is(lessThan(rCompareValue));
				break;

			case '>':
				pAttribute = rComparableAttr.is(greaterThan(rCompareValue));
				break;

			case '\u2264': // <=
				pAttribute = rComparableAttr.is(lessOrEqual(rCompareValue));
				break;

			case '\u2265': // >=
				pAttribute = rComparableAttr.is(greaterOrEqual(rCompareValue));
				break;

			case '~':
				pAttribute = rComparableAttr.is(similarTo(sConstraint));
				break;

			default:
				pAttribute = createValueConstraint(rComparableAttr,
					cComparison,
					rCompareValue, sConstraint);
		}

		return pAttribute;
	}

	/**
	 * Creates the final query predicate for this instance by applying
	 * constraints and sort fields (if available) to the base query predicate.
	 *
	 * @param qBaseQuery   The base query predicate
	 * @param rConstraints The additional query constraints (NULL for none)
	 * @param rSortFields  The optional sort fields (NULL for none)
	 * @return The total size of the query
	 * @throws StorageException If accessing the storage fails
	 * @throws ServiceException If creating a result data object fails
	 */
	private QueryPredicate<Entity> createFullQuery(
		QueryPredicate<Entity> qBaseQuery, Map<String, String> rConstraints,
		Map<String, SortDirection> rSortFields) {
		Class<Entity> rQueryType = qBaseQuery.getQueryType();
		Predicate<? super Entity> pCriteria = qBaseQuery.getCriteria();

		HierarchicalQueryMode eHierarchyMode =
			qBaseQuery.get(HIERARCHICAL_QUERY_MODE);

		boolean bNoConstraints =
			rConstraints == null || rConstraints.size() == 0;
		boolean bHierarchical =
			eHierarchyMode == HierarchicalQueryMode.ALWAYS ||
				eHierarchyMode == HierarchicalQueryMode.UNCONSTRAINED &&
					bNoConstraints;

		if (bHierarchical) {
			Predicate<? super Entity> pIsHierarchyRoot =
				qBaseQuery.get(HIERARCHY_ROOT_PREDICATE);

			if (pIsHierarchyRoot == null) {
				RelationType<? extends Entity> rParentAttribute = EntityManager
					.getEntityDefinition(rQueryType)
					.getParentAttribute();

				if (rParentAttribute != null) {
					pIsHierarchyRoot = rParentAttribute.is(isNull());
				}
			}

			if (pIsHierarchyRoot != null) {
				pCriteria = Predicates.and(pCriteria, pIsHierarchyRoot);
			}
		}

		if (bNoConstraints && pDefaultConstraints != null) {
			pCriteria = Predicates.and(pCriteria, pDefaultConstraints);
		}

		QueryPredicate<Entity> qFullQuery = qBaseQuery;

		if (pCriteria != qBaseQuery.getCriteria()) {
			qFullQuery = new QueryPredicate<Entity>(rQueryType, pCriteria);

			ObjectRelations.copyRelations(qBaseQuery, qFullQuery, false);
		}

		qFullQuery = applyQueryConstraints(qFullQuery, rConstraints);
		qFullQuery = applySortFields(qFullQuery, rSortFields);

		return qFullQuery;
	}

	/**
	 * Creates a comparison predicate for a single-day constraint.
	 *
	 * @param rAttribute    The attribute to create the comparison for
	 * @param rCompareValue The compare value
	 * @param bNegate       TRUE to negate the comparison
	 * @return The comparison predicate
	 */
	private Predicate<Entity> createSingleDayComparison(
		RelationType<Date> rAttribute, Date rCompareValue, boolean bNegate) {
		Calendar rCalendar = Calendar.getInstance();
		Predicate<Entity> pDayComparison;
		Predicate<Entity> pLastDate;

		rCalendar.setTime(rCompareValue);
		CalendarFunctions.clearTime(rCalendar);
		rCompareValue = rCalendar.getTime();

		if (bNegate) {
			pDayComparison = rAttribute.is(lessThan(rCompareValue));
		} else {
			pDayComparison = rAttribute.is(greaterOrEqual(rCompareValue));
		}

		rCalendar.add(Calendar.DAY_OF_MONTH, 1);
		rCompareValue = rCalendar.getTime();

		if (bNegate) {
			pLastDate = rAttribute.is(greaterOrEqual(rCompareValue));
		} else {
			pLastDate = rAttribute.is(lessThan(rCompareValue));
		}

		pDayComparison = pDayComparison.and(pLastDate);

		return pDayComparison;
	}

	/**
	 * Creates a single value comparison query constraint predicate.
	 *
	 * @param rAttr       The attribute to create the predicate for
	 * @param cComparison The comparison to perform
	 * @param rValue      The comparable value
	 * @param sConstraint The constraint string
	 * @return A predicate containing the query constraint
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Predicate<?> createValueConstraint(RelationType<?> rAttr,
		char cComparison, Comparable rValue, String sConstraint) {
		Predicate<?> pAttribute;
		boolean bNegate = (cComparison == '\u2260'); // !=

		sConstraint = StorageManager.convertToSqlConstraint(sConstraint);

		if (NULL_CONSTRAINT_VALUE.equals(rValue)) {
			rValue = null;
		}

		if (rValue instanceof Date) {
			pAttribute = createSingleDayComparison((RelationType<Date>) rAttr,
				(Date) rValue, bNegate);
		} else {
			Class<?> rDatatype = rAttr.getTargetType();
			Predicate<Object> pComparison;

			if (sConstraint.indexOf('%') >= 0 ||
				sConstraint.indexOf('_') >= 0) {
				pComparison = like(sConstraint);
			} else {
				pComparison = equalTo(rValue);
			}

			if (bNegate) {
				pComparison = Predicates.not(pComparison);
			}

			if ((rDatatype == String.class || rDatatype.isEnum()) &&
				!TextUtil.containsUpperCase(sConstraint)) {
				Function<Relatable, String> fAttr = StringFunctions
					.toLowerCase()
					.from((Function<Relatable, String>) rAttr);

				pAttribute =
					new FunctionPredicate<Entity, String>(fAttr, pComparison);
			} else {
				pAttribute = rAttr.is(pComparison);
			}
		}

		return pAttribute;
	}

	/**
	 * Executes a storage query with certain parameters. The query object will
	 * be closed after successful execution.
	 *
	 * @param qEntities      The predicate of the query to execute
	 * @param nStart         The starting index of the entities to query
	 * @param nLimit         The maximum number of entities to retrieve
	 * @param aResultRows    The list to store the queried data objects in
	 * @param rFlagAttribute The attribute that should be set as the data
	 *                       objects flag
	 * @return The total size of the query
	 * @throws StorageException If accessing the storage fails
	 * @throws ServiceException If creating a result data object fails
	 */
	private int executeQuery(Storage rStorage,
		QueryPredicate<Entity> qEntities,
		int nStart, int nLimit, List<DataModel<String>> aResultRows,
		RelationType<?> rFlagAttribute) throws StorageException {
		Predicate<? super Entity> pChildCriteria =
			qEntities.get(HIERARCHY_CHILD_PREDICATE);

		int nQuerySize;

		try (Query<Entity> aQuery = rStorage.query(qEntities)) {
			aQuery.set(StorageRelationTypes.QUERY_LIMIT, nLimit);
			aQuery.set(StorageRelationTypes.QUERY_OFFSET, nStart);

			QueryResult<Entity> aEntities = aQuery.execute();

			nQuerySize = aQuery.size();
			aLastQueryResult = new ArrayList<Entity>(Math.min(nLimit, 1000));

			while (nLimit-- > 0 && aEntities.hasNext()) {
				Entity rEntity = aEntities.next();
				Set<String> aFlags = null;

				if (rFlagAttribute != null) {
					Object rFlagValue = rEntity.get(rFlagAttribute);

					if (rFlagValue != null) {
						aFlags = Collections.singleton(rFlagValue.toString());
					}
				}

				HierarchicalDataObject aDataObject =
					rDataElementFactory.createEntityDataObject(rEntity,
						nStart++, pChildCriteria, pDefaultSortCriteria,
						fGetAttributes, aFlags, true);

				aResultRows.add(aDataObject);

				StorageAdapterId rChildAdapterId =
					rEntity.get(CHILD_STORAGE_ADAPTER_ID);

				if (rChildAdapterId != null) {
					get(STORAGE_ADAPTER_IDS).add(rChildAdapterId);
					rEntity.deleteRelation(CHILD_STORAGE_ADAPTER_ID);
				}

				// keep result entities to prevent garbage collection of child
				// list
				// storage adapters which are only stored in weak references
				aLastQueryResult.add(rEntity);
			}
		}

		return nQuerySize;
	}

	/**
	 * Allows to query the position of an entity with a certain ID in the query
	 * result of this adapter.
	 *
	 * @param rId The ID of the entity to query the position or NULL for the
	 *            query size
	 * @return The entity position or -1 if undefined
	 * @throws StorageException If the database query fails
	 */
	private int queryPositionOrSize(Object rId) throws StorageException {
		int nResult;

		aLock.lock();

		try {
			Storage rStorage =
				StorageManager.getStorage(qBaseQuery.getQueryType());

			try (Query<Entity> rQuery = rStorage.query(
				createFullQuery(qBaseQuery, null, null))) {
				nResult =
					(rId != null ? rQuery.positionOf(rId) : rQuery.size());
			} finally {
				rStorage.release();
			}
		} finally {
			aLock.unlock();
		}

		return nResult;
	}
}
