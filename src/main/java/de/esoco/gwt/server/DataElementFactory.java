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

import de.esoco.data.element.BigDecimalDataElement;
import de.esoco.data.element.BooleanDataElement;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElement.Flag;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.DataSetDataElement;
import de.esoco.data.element.DateDataElement;
import de.esoco.data.element.EntityDataElement;
import de.esoco.data.element.HierarchicalDataObject;
import de.esoco.data.element.IntegerDataElement;
import de.esoco.data.element.PeriodDataElement;
import de.esoco.data.element.SelectionDataElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.element.StringListDataElement;
import de.esoco.data.storage.StorageAdapter;
import de.esoco.data.storage.StorageAdapterId;
import de.esoco.data.storage.StorageAdapterRegistry;
import de.esoco.data.validate.DateValidator;
import de.esoco.data.validate.IntegerRangeValidator;
import de.esoco.data.validate.QueryValidator;
import de.esoco.data.validate.SelectionValidator;
import de.esoco.data.validate.StringListValidator;
import de.esoco.data.validate.Validator;
import de.esoco.entity.Entity;
import de.esoco.entity.EntityDefinition;
import de.esoco.entity.EntityDefinition.DisplayMode;
import de.esoco.entity.EntityFunctions;
import de.esoco.entity.EntityFunctions.GetExtraAttribute;
import de.esoco.entity.EntityManager;
import de.esoco.entity.EntityRelationTypes.HierarchicalQueryMode;
import de.esoco.gwt.client.data.QueryDataModel;
import de.esoco.gwt.shared.AuthenticationException;
import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.datatype.Period;
import de.esoco.lib.datatype.Period.Unit;
import de.esoco.lib.expression.BinaryFunction;
import de.esoco.lib.expression.CollectionFunctions;
import de.esoco.lib.expression.ElementAccess;
import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.Functions;
import de.esoco.lib.expression.Predicate;
import de.esoco.lib.expression.function.FunctionChain;
import de.esoco.lib.expression.predicate.FunctionPredicate;
import de.esoco.lib.json.JsonObject;
import de.esoco.lib.model.ColumnDefinition;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.DataSet;
import de.esoco.lib.model.ListDataModel;
import de.esoco.lib.model.SimpleColumnDefinition;
import de.esoco.lib.property.ContentType;
import de.esoco.lib.property.HasProperties;
import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.MutableProperties;
import de.esoco.lib.property.PropertyName;
import de.esoco.lib.property.SortDirection;
import de.esoco.lib.property.StringProperties;
import de.esoco.lib.property.UserInterfaceProperties;
import de.esoco.lib.reflect.ReflectUtil;
import de.esoco.lib.text.TextConvert;
import de.esoco.process.Process;
import de.esoco.process.ProcessRelationTypes;
import de.esoco.process.ProcessStep;
import de.esoco.storage.QueryList;
import de.esoco.storage.QueryPredicate;
import de.esoco.storage.StorageException;
import de.esoco.storage.StoragePredicates.SortPredicate;
import org.obrel.core.Relatable;
import org.obrel.core.Relation;
import org.obrel.core.RelationType;
import org.obrel.type.MetaTypes;
import org.obrel.type.StandardTypes;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static de.esoco.data.DataRelationTypes.CHILD_STORAGE_ADAPTER_ID;
import static de.esoco.data.DataRelationTypes.STORAGE_ADAPTER_ID;
import static de.esoco.data.DataRelationTypes.STORAGE_ADAPTER_IDS;
import static de.esoco.entity.EntityPredicates.forEntity;
import static de.esoco.entity.EntityPredicates.ifAttribute;
import static de.esoco.entity.EntityRelationTypes.DISPLAY_ENTITY_IDS;
import static de.esoco.entity.EntityRelationTypes.DISPLAY_PROPERTIES;
import static de.esoco.entity.EntityRelationTypes.ENTITY_ATTRIBUTES;
import static de.esoco.entity.EntityRelationTypes.ENTITY_DISPLAY_MODE;
import static de.esoco.entity.EntityRelationTypes.ENTITY_QUERY_PREDICATE;
import static de.esoco.entity.EntityRelationTypes.ENTITY_SORT_PREDICATE;
import static de.esoco.entity.EntityRelationTypes.HIERARCHICAL_QUERY_MODE;
import static de.esoco.entity.EntityRelationTypes.HIERARCHY_CHILD_PREDICATE;
import static de.esoco.lib.expression.Functions.asString;
import static de.esoco.lib.expression.Predicates.equalTo;
import static de.esoco.lib.expression.StringFunctions.capitalizedIdentifier;
import static de.esoco.lib.expression.StringFunctions.format;
import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.ContentProperties.RESOURCE_ID;
import static de.esoco.lib.property.ContentProperties.VALUE_RESOURCE_PREFIX;
import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.StateProperties.CURRENT_SELECTION;
import static de.esoco.lib.property.StateProperties.SORT_DIRECTION;
import static de.esoco.lib.property.StyleProperties.HIERARCHICAL;
import static de.esoco.process.ProcessRelationTypes.ALLOWED_VALUES;
import static de.esoco.process.ProcessRelationTypes.DATA_ELEMENT;
import static de.esoco.process.ProcessRelationTypes.INPUT_PARAMS;
import static de.esoco.process.ProcessRelationTypes.PROCESS;
import static org.obrel.type.MetaTypes.AUTOGENERATED;
import static org.obrel.type.MetaTypes.ELEMENT_DATATYPE;
import static org.obrel.type.MetaTypes.OPTIONAL;
import static org.obrel.type.StandardTypes.MAXIMUM;
import static org.obrel.type.StandardTypes.MINIMUM;

/**
 * A factory class that provides methods to create and manipulate instances of
 * {@link DataElement}.
 *
 * @author eso
 */
public class DataElementFactory {

	// ---------------------------------------------

	private static final Map<Class<? extends Enum<?>>, Validator<?>>
		aEnumValidatorRegistry =
		new HashMap<Class<? extends Enum<?>>, Validator<?>>();

	private static final Function<Date, Long> GET_DATE_LONG_VALUE =
		d -> d != null ? Long.valueOf(d.getTime()) : null;

	// --------------------------------------------------------

	private final StorageAdapterRegistry rStorageAdapterRegistry;

	// -----------------------------------------------------------

	/**
	 * Creates a new instance.
	 *
	 * @param rStorageAdapterRegistry The storage adapter registry to register
	 *                                storage adapter instances with
	 */
	public DataElementFactory(StorageAdapterRegistry rStorageAdapterRegistry) {
		this.rStorageAdapterRegistry = rStorageAdapterRegistry;
	}

	// ---------------------------------------------------------

	/**
	 * Creates an identifying name from an attribute function. If the given
	 * function is an instance of {@link FunctionChain} the identifier will be
	 * generated by recursive invocations of this method.
	 *
	 * @param rFunction The attribute function to create the identifier from
	 * @param sPrefix   An optional prefix for the generated name (empty string
	 *                  for none)
	 * @return The resulting column identifier
	 */
	public static String createAttributeName(Function<?, ?> rFunction,
		String sPrefix) {
		String sName = "";

		if (rFunction instanceof FunctionChain<?, ?, ?>) {
			FunctionChain<?, ?, ?> rChain = (FunctionChain<?, ?, ?>) rFunction;

			sName = createAttributeName(rChain.getInner(), "") +
				createAttributeName(rChain.getOuter(), "");
		} else if (rFunction instanceof FunctionPredicate<?, ?>) {
			sName = createAttributeName(
				((FunctionPredicate<?, ?>) rFunction).getFunction(), "");
		} else {
			Object rAttribute = getAttributeDescriptor(rFunction);

			if (rAttribute instanceof RelationType<?>) {
				sName =
					TextConvert.capitalizedLastElementOf(rAttribute.toString());
			}
		}

		return sPrefix + sName;
	}

	/**
	 * Processes a list of entity attribute access functions and to create a
	 * list of column definitions from it.
	 *
	 * @param rEntityDefinition The entity definition to create the columns for
	 * @param rAttributes       The list of attribute functions
	 * @param sPrefix           The prefix string for the column titles
	 * @param rSortAttribute    An optional sort attribute or NULL for none
	 * @param eSortDirection    The sort direction for a sort attribute
	 * @return A list containing the corresponding column data element
	 */
	@SuppressWarnings("unchecked")
	private static List<ColumnDefinition> createColumnDefinitions(
		EntityDefinition<?> rEntityDefinition,
		Collection<Function<? super Entity, ?>> rAttributes, String sPrefix,
		RelationType<?> rSortAttribute, SortDirection eSortDirection) {
		List<ColumnDefinition> aColumns =
			new ArrayList<ColumnDefinition>(rAttributes.size());

		for (Function<? super Entity, ?> fGetAttr : rAttributes) {
			RelationType<?> rDisplayAttr = findDisplayAttribute(fGetAttr);

			String sName = createAttributeName(fGetAttr, sPrefix);
			String sTitle = ColumnDefinition.STD_COLUMN_PREFIX + sName;
			String sDatatype = null;
			String sId;

			boolean bSortable = !(fGetAttr instanceof GetExtraAttribute);
			boolean bSearchable = true;
			boolean bEditable = false;

			MutableProperties aDisplayProperties =
				rEntityDefinition.getDisplayProperties(rDisplayAttr);

			if (rDisplayAttr != null) {
				Class<?> rDatatype = rDisplayAttr.getTargetType();

				sId = rDisplayAttr.getName();
				sDatatype = rDatatype.getSimpleName();

				if (fGetAttr instanceof FunctionChain) {
					RelationType<?> rRefAttr =
						(RelationType<?>) Functions.firstInChain(fGetAttr);

					Class<?> rRefType = rRefAttr.getTargetType();

					if (Entity.class.isAssignableFrom(rRefType)) {
						// if the attribute function is for an attribute of a
						// referenced entity then add the display properties
						// from that entity and disable sorting and searching
						EntityDefinition<?> rRefDef =
							EntityManager.getEntityDefinition(
								(Class<? extends Entity>) rRefType);

						HasProperties aBaseProperties =
							rEntityDefinition.getDisplayProperties(rRefAttr);

						aDisplayProperties =
							rRefDef.getDisplayProperties(rDisplayAttr);

						// base properties always override reference properties
						aDisplayProperties.setProperties(aBaseProperties,
							true);

						sId = rRefAttr.getName();
						bSearchable = false;
					}
				}

				if (rDisplayAttr == fGetAttr || rDatatype.isEnum() ||
					rDatatype.isAssignableFrom(Date.class)) {
					if (rDatatype.isEnum()) {
						setEnumColumnProperties(
							(Class<? extends Enum<?>>) rDatatype,
							aDisplayProperties);
						sDatatype = Enum.class.getSimpleName();
					}
				} else {
					bSearchable = false;
				}
			} else {
				sId = sName;
				bSearchable = false;
			}

			SimpleColumnDefinition aColumn =
				new SimpleColumnDefinition(sId, sTitle, sDatatype, bSortable,
					bSearchable, bEditable);

			if (aDisplayProperties != null &&
				aDisplayProperties.getPropertyCount() > 0) {
				aColumn.setProperties(aDisplayProperties, true);
			}

			if (rDisplayAttr != null && rDisplayAttr == rSortAttribute) {
				aColumn.setProperty(SORT_DIRECTION, eSortDirection);
			}

			aColumns.add(aColumn);
		}

		return aColumns;
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection from a list
	 * of data objects.
	 *
	 * @param sName             The name of the element
	 * @param sCurrentValue     The currently selected value
	 * @param sPrefix           The prefix to be prepended to generated names
	 * @param rEntityDefinition The entity definition for the objects or NULL
	 *                          for none
	 * @param aDataObjects      rEntities The entities to select from
	 * @param rAttributes       The entity attributes to display
	 * @param bDisplayEntityIds TRUE to display entity IDs instead of string
	 *                          descriptions for entity attributes
	 * @return A new data element for the selection of an entity
	 */
	public static SelectionDataElement createSelectionDataElement(String sName,
		String sCurrentValue, String sPrefix,
		EntityDefinition<?> rEntityDefinition,
		List<HierarchicalDataObject> aDataObjects,
		Collection<Function<? super Entity, ?>> rAttributes,
		boolean bDisplayEntityIds) {
		rAttributes = processAttributeFunctions(rEntityDefinition, rAttributes,
			bDisplayEntityIds);

		List<ColumnDefinition> aColumns =
			createColumnDefinitions(rEntityDefinition, rAttributes, sPrefix,
				null, null);

		Validator<String> rValidator =
			new SelectionValidator(aDataObjects, aColumns);

		return new SelectionDataElement(sName, sCurrentValue, rValidator,
			null);
	}

	/**
	 * Creates a new string data element with a certain value. The meta data
	 * argument will be queried for a relation of the type
	 * {@link ProcessRelationTypes#ALLOWED_VALUES} that will constrain the
	 * possible values of the data element.
	 *
	 * @param sName          The name of the data element
	 * @param rValue         The initial value of the element
	 * @param rAllowedValues The allowed values to create a validator for or
	 *                       NULL for none
	 * @param rFlags         The optional data element flags
	 * @return The new data element
	 */
	public static StringDataElement createStringDataElement(String sName,
		Object rValue, Collection<?> rAllowedValues, Set<Flag> rFlags) {
		String sValue = rValue != null ? rValue.toString() : null;

		StringListValidator rValidator = null;

		if (rAllowedValues != null) {
			rValidator = createStringListValidator(rAllowedValues, false);
		}

		return new StringDataElement(sName, sValue, rValidator, rFlags);
	}

	/**
	 * Creates a new string list validator instance. The given list of values
	 * will be converted by invoking their {@link Object#toString() toString()}
	 * method.
	 *
	 * @param rValues      The values to be validated against
	 * @param bResourceIds Corresponds to same the flag of
	 *                     {@link StringListValidator#StringListValidator(List,
	 *                     boolean)}
	 * @return The new validator instance
	 */
	static StringListValidator createStringListValidator(Collection<?> rValues,
		boolean bResourceIds) {
		StringListValidator rValidator;
		List<String> aValues = new ArrayList<String>(rValues.size());

		for (Object rAllowedValue : rValues) {
			aValues.add(rAllowedValue.toString());
		}

		rValidator = new StringListValidator(aValues, bResourceIds);

		return rValidator;
	}

	/**
	 * Returns the relation type of an attribute access function. If the
	 * function consists of one or more function chains the returned relation
	 * type will be that of the innermost relation type of the chain if such
	 * exists. It will be determined by parsing the branch of inner functions
	 * recursively.
	 *
	 * @param rAccessFunction The attribute access function to parse
	 * @return The innermost attribute relation type or NULL for none
	 */
	private static RelationType<?> findDisplayAttribute(
		Function<?, ?> rAccessFunction) {
		RelationType<?> rResult = null;

		if (rAccessFunction instanceof RelationType<?>) {
			rResult = (RelationType<?>) rAccessFunction;
		} else if (rAccessFunction instanceof FunctionChain<?, ?, ?>) {
			FunctionChain<?, ?, ?> rChain =
				(FunctionChain<?, ?, ?>) rAccessFunction;

			rResult = findDisplayAttribute(rChain.getOuter());

			if (rResult == null) {
				rResult = findDisplayAttribute(rChain.getInner());
			}
		} else if (rAccessFunction instanceof FunctionPredicate<?, ?>) {
			rResult = findDisplayAttribute(
				((FunctionPredicate<?, ?>) rAccessFunction).getFunction());
		} else {
			Object rAttribute = getAttributeDescriptor(rAccessFunction);

			if (rAttribute instanceof RelationType<?>) {
				rResult = (RelationType<?>) rAttribute;
			}
		}

		return rResult;
	}

	/**
	 * Returns the attribute descriptor from a function.
	 *
	 * @param rFunction The function
	 * @return The attribute descriptor or NULL if the function doesn't contain
	 * one
	 */
	private static Object getAttributeDescriptor(Function<?, ?> rFunction) {
		Object rAttribute = null;

		if (rFunction instanceof BinaryFunction<?, ?, ?>) {
			rAttribute = ((BinaryFunction<?, ?, ?>) rFunction).getRightValue();
		} else if (rFunction instanceof ElementAccess<?>) {
			rAttribute = ((ElementAccess<?>) rFunction).getElementDescriptor();
		}

		return rAttribute;
	}

	/**
	 * Returns a validator for a certain enum class. To reduce serialization
	 * sizes validator instances are cached internally so that the same
	 * validator will be returned on subsequent invocations with the same enum
	 * class. The enum values will be stored in the validator as the enum names
	 * converted to camel case.
	 *
	 * @param rEnumClass     The enum class to return the validator for
	 * @param rAllowedValues The allowed values or NULL for all enum values
	 * @return The validator for the given enum class
	 */
	static StringListValidator getEnumValidator(
		Class<? extends Enum<?>> rEnumClass, Collection<?> rAllowedValues) {
		StringListValidator rValidator;

		if (rAllowedValues == null) {
			rValidator =
				(StringListValidator) aEnumValidatorRegistry.get(rEnumClass);

			if (rValidator == null) {
				Enum<?>[] rEnums = rEnumClass.getEnumConstants();

				rValidator =
					createStringListValidator(Arrays.asList(rEnums), true);

				aEnumValidatorRegistry.put(rEnumClass, rValidator);
			}
		} else {
			rValidator = createStringListValidator(rAllowedValues, true);
		}

		return rValidator;
	}

	/**
	 * Checks whether a certain relation type is a process input parameter.
	 *
	 * @param rObject The object to check for the input parameter information
	 * @param rType   The relation type
	 * @return TRUE if the given relation type is for input
	 */
	private static boolean isInputType(Relatable rObject,
		RelationType<?> rType) {
		return rObject.get(INPUT_PARAMS).contains(rType);
	}

	/**
	 * Converts an attribute access function (typically a relation type) into a
	 * function that provides a translated attribute value if necessary. This
	 * method will convert attributes that reference certain datatypes like
	 * entities, enums, or dates into special access functions.
	 *
	 * @param rEntityDefinition The entity definition of the attribute
	 * @param fGetAttr          The attribute access functions
	 * @param bDisplayEntityIds TRUE to display entity IDs instead of string
	 *                          descriptions for entity attributes
	 * @return Either the original function or a function that performs the
	 * necessary transformation
	 */
	@SuppressWarnings("unchecked")
	public static Function<? super Entity, ?> processAttributeFunction(
		EntityDefinition<?> rEntityDefinition,
		Function<? super Entity, ?> fGetAttr, boolean bDisplayEntityIds) {
		Class<?> rDatatype = String.class;
		RelationType<?> rAttribute = null;

		if (fGetAttr instanceof RelationType<?>) {
			rAttribute = (RelationType<?>) fGetAttr;
			rDatatype = rAttribute.getTargetType();

			if (Entity.class.isAssignableFrom(rDatatype)) {
				if (!bDisplayEntityIds) {
					fGetAttr = EntityFunctions
						.formatEntity("")
						.from((RelationType<Entity>) fGetAttr);
				} else if (rDatatype == Entity.class) {
					fGetAttr = EntityFunctions
						.getGlobalEntityId()
						.from((RelationType<Entity>) fGetAttr);
				} else {
					fGetAttr = EntityFunctions
						.getEntityId()
						.from((RelationType<Entity>) fGetAttr);
				}
			}
		} else if (fGetAttr instanceof FunctionChain) {
			rAttribute = findDisplayAttribute(fGetAttr);
			rDatatype = rAttribute.getTargetType();
		}

		if (rDatatype.isEnum()) {
			String sEnumPrefix = rDatatype.getSimpleName();
			StringBuilder aEnumItem =
				new StringBuilder(DataElement.ITEM_RESOURCE_PREFIX);

			HasProperties rDisplayProperties =
				rEntityDefinition.getDisplayProperties(rAttribute);

			if (rDisplayProperties != null) {
				sEnumPrefix =
					rDisplayProperties.getProperty(RESOURCE_ID, sEnumPrefix);
			}

			aEnumItem.append(sEnumPrefix);
			aEnumItem.append("%s");

			// add formatting function for enum values that creates a
			// resource identifier
			fGetAttr = format(aEnumItem.toString()).from(
				capitalizedIdentifier().from(asString().from(fGetAttr)));
		} else if (Date.class.isAssignableFrom(rDatatype)) {
			// convert dates into their long values
			fGetAttr = GET_DATE_LONG_VALUE.from(
				(Function<? super Entity, ? extends Date>) fGetAttr);
		} else if (rDatatype == Boolean.class) {
			String sAttr =
				TextConvert.capitalizedIdentifier(rAttribute.getSimpleName());

			sAttr = DataElement.ITEM_RESOURCE_PREFIX + sAttr + "%s";

			fGetAttr = format(sAttr).from(
				capitalizedIdentifier().from(asString().from(fGetAttr)));
		}

		return fGetAttr;
	}

	/**
	 * Converts a list of attribute access functions (typically relation types)
	 * into functions that provide a translated attribute value if necessary.
	 * This method will convert attributes that reference certain datatypes
	 * like
	 * entities, enums, or dates into special access functions.
	 *
	 * @param rEntityDefinition The entity definition of the attributes
	 * @param rAttributes       The list of attribute access functions
	 * @param bDisplayEntityIds TRUE to display entity IDs instead of string
	 *                          descriptions for entity attributes
	 * @return A list containing either the original functions or functions
	 * that
	 * perform the necessary transformations
	 */
	public static List<Function<? super Entity, ?>> processAttributeFunctions(
		EntityDefinition<?> rEntityDefinition,
		Collection<Function<? super Entity, ?>> rAttributes,
		boolean bDisplayEntityIds) {
		int nCount = rAttributes.size();

		List<Function<? super Entity, ?>> aResult =
			new ArrayList<Function<? super Entity, ?>>(nCount);

		for (Function<? super Entity, ?> fGetAttr : rAttributes) {
			aResult.add(processAttributeFunction(rEntityDefinition, fGetAttr,
				bDisplayEntityIds));
		}

		return aResult;
	}

	/**
	 * Set the display properties for an enum table column.
	 *
	 * @param rDatatype   The enum datatype class of the column
	 * @param rProperties The display properties to modify
	 */
	public static void setEnumColumnProperties(
		Class<? extends Enum<?>> rDatatype, MutableProperties rProperties) {
		String sAllowedValues = null;

		if (rProperties.hasProperty(UserInterfaceProperties.ALLOWED_VALUES)) {
			sAllowedValues =
				rProperties.getProperty(UserInterfaceProperties.ALLOWED_VALUES,
					"");
		}

		if (sAllowedValues == null || sAllowedValues.isEmpty()) {
			Object[] rEnumValues = rDatatype.getEnumConstants();

			sAllowedValues = CollectionUtil.toString(rEnumValues, ",");
		}

		rProperties.setProperty(VALUE_RESOURCE_PREFIX,
			DataElement.ITEM_RESOURCE_PREFIX + rDatatype.getSimpleName());
		rProperties.setProperty(UserInterfaceProperties.ALLOWED_VALUES,
			sAllowedValues);
	}

	// ----------------------------------------------------------------

	/**
	 * Applies a single data element to a relatable object by mapping the
	 * element back to the corresponding relation from which it had been
	 * created.
	 *
	 * @param rElement The data element to apply
	 * @param rTarget  The relatable object to apply the data element to
	 * @param rType    The type of the relation to apply
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	@SuppressWarnings("unchecked")
	public void applyDataElement(DataElement<?> rElement, Relatable rTarget,
		RelationType<?> rType)
		throws AuthenticationException, StorageException {
		Class<?> rTargetDatatype = rType.getTargetType();

		if (DataElement.class.isAssignableFrom(rTargetDatatype)) {
			// if the relation directly stores a data element no mapping is
			// necessary
			RelationType<DataElement<?>> rDataElementType =
				(RelationType<DataElement<?>>) rType;

			rTarget.set(rDataElementType, rElement);
		} else if (rElement instanceof EntityDataElement) {
			// apply attribute data elements recursively to target entity
			EntityDataElement rEntityDataElement =
				(EntityDataElement) rElement;

			applyEntityDataElement(rEntityDataElement, rTarget, rType);
		} else if (rElement instanceof SelectionDataElement) {
			applyEntitySelection((SelectionDataElement) rElement, rTarget,
				rType);
		} else if (rElement instanceof StringListDataElement &&
			Collection.class.isAssignableFrom(rTargetDatatype)) {
			Collection<?> rTargetCollection =
				(Collection<?>) rTarget.get(rType);

			applyStringList(((StringListDataElement) rElement).getList(),
				rType.get(ELEMENT_DATATYPE), rTargetCollection);
		} else if (!(rElement instanceof DataElementList)) {
			rTarget.set((RelationType<Object>) rType,
				convertValue(rTargetDatatype, rElement.getValue()));
		}
	}

	/**
	 * Applies a set of data elements to a relatable object by mapping the
	 * element back to the corresponding relation from which it had been
	 * created. Immutable elements will be ignored.
	 *
	 * @param rSourceElements The list of data elements to apply
	 * @param rTarget         The relatable object to apply the data elements
	 *                        to
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	public void applyDataElements(
		List<? extends DataElement<?>> rSourceElements, Relatable rTarget)
		throws AuthenticationException, StorageException {
		for (DataElement<?> rElement : rSourceElements) {
			RelationType<?> rType = RelationType.valueOf(rElement.getName());

			if (rType != null) {
				if (!rElement.isImmutable()) {
					if (!rElement.isOptional() || rElement.isSelected()) {
						applyDataElement(rElement, rTarget, rType);
					}
				}

				checkApplyProperties(rElement, rTarget, rType);

				if (rElement instanceof DataElementList) {
					Process rSubProcess =
						rTarget.getRelation(rType).getAnnotation(PROCESS);

					applyDataElements(
						((DataElementList) rElement).getElements(),
						rSubProcess != null ? rSubProcess : rTarget);
				}
			}
		}
	}

	/**
	 * Creates a list data element that contains data elements that are created
	 * from a collection of values. The collection can either contain relation
	 * types that will be converted to data elements recursively or other
	 * values
	 * which will be converted to strings. If the value collection argument is
	 * NULL the returned list data element will be empty.
	 *
	 * @param rObject The target object of the collection relation
	 * @param rType   The collection relation type
	 * @param rValues A collection containing the values to be converted into
	 *                data elements (may be NULL)
	 * @param rFlags  The optional data element flags
	 * @return The new list data element
	 * @throws StorageException If creating data elements recursively fails
	 */
	public DataElementList createDataElementList(Relatable rObject,
		RelationType<?> rType, Collection<?> rValues, Set<Flag> rFlags)
		throws StorageException {
		Class<?> rElementDatatype = rType.get(ELEMENT_DATATYPE);

		boolean bRecursive = (rElementDatatype != null &&
			RelationType.class.isAssignableFrom(rElementDatatype));

		List<DataElement<?>> aDataElements = null;

		if (rValues != null) {
			aDataElements = new ArrayList<DataElement<?>>(rValues.size());

			for (Object rValue : rValues) {
				DataElement<?> aDataElement;

				if (bRecursive) {
					aDataElement =
						getDataElement(rObject, (RelationType<?>) rValue);
				} else {
					aDataElement =
						new StringDataElement(rValue.getClass().getSimpleName(),
							rValue.toString());
				}

				if (aDataElement != null) {
					aDataElements.add(aDataElement);
				}
			}
		}

		return new DataElementList(rType.getName(), null, aDataElements,
			rFlags);
	}

	/**
	 * Creates a data object for a certain entity in a storage query.
	 *
	 * @param rEntity        The entity to create the data element from
	 * @param nIndex         The index of the data object
	 * @param pChildCriteria A predicate that constrains the child entities to
	 *                       be included in a hierarchical object or NULL for
	 *                       none
	 * @param pSortCriteria  The sort criteria for child queries (NULL for
	 *                       none)
	 * @param fGetColumnData The function to extract the entity's column data
	 *                       into a list of strings
	 * @param rFlags         The flags for the entity object
	 * @param bHierarchical  TRUE, to include children of the same type as the
	 *                       entity
	 * @return The resulting data element
	 * @throws StorageException If creating the data object fails
	 */
	public HierarchicalDataObject createEntityDataObject(Entity rEntity,
		int nIndex, Predicate<? super Entity> pChildCriteria,
		Predicate<? super Entity> pSortCriteria,
		Function<Entity, List<String>> fGetColumnData,
		Collection<String> rFlags, boolean bHierarchical)
		throws StorageException {
		List<String> aValues = fGetColumnData.evaluate(rEntity);

		DataModel<DataModel<String>> aChildren = null;

		if (bHierarchical &&
			rEntity.getDefinition().getHierarchyChildAttribute() != null) {
			aChildren =
				createChildDataModels(rEntity, pChildCriteria, pSortCriteria,
					fGetColumnData);
		}

		return new HierarchicalDataObject(Long.toString(rEntity.getId()),
			nIndex, aValues, true, rFlags, aChildren);
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection of a entity
	 * data element from a list of entities.
	 *
	 * @param sName            The name of the element
	 * @param rMetaData        A relatable object containing the meta data for
	 *                         the element to create
	 * @param rCurrentEntityId sCurrentValue The current selection or NULL for
	 *                         none
	 * @param rEntities        The entities to select from
	 * @param rAttributes      The entity attributes to display
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If initializing the query fails
	 */
	public SelectionDataElement createEntitySelectionElement(String sName,
		Relatable rMetaData, Number rCurrentEntityId, List<Entity> rEntities,
		List<Function<? super Entity, ?>> rAttributes) throws StorageException {
		EntityDefinition<?> rDef = rEntities.get(0).getDefinition();
		String sPrefix = rDef.getEntityName();

		List<HierarchicalDataObject> aEntityObjects =
			createEntityDataObjects(rEntities, rAttributes,
				rMetaData.hasFlag(MetaTypes.HIERARCHICAL));

		String sCurrentValue =
			rCurrentEntityId != null ? rCurrentEntityId.toString() : "-1";

		SelectionDataElement aResult =
			createSelectionDataElement(sName, sCurrentValue, sPrefix, rDef,
				aEntityObjects, rAttributes,
				rMetaData.hasFlag(DISPLAY_ENTITY_IDS));

		return aResult;
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection of a entity
	 * data element from a list of entities that is defined by a storage.
	 *
	 * @param sName                The name of the element
	 * @param rMetaData            A relatable object containing the meta data
	 *                             for the element to create
	 * @param rCurrentEntityId     rCurrentValue The current selection or NULL
	 *                             for none
	 * @param nCurrentSelection    The index of the current selection or -1
	 * @param pQuery               The query for the entities to select from
	 * @param pDefaultCriteria     An optional predicate containing default
	 *                             criteria to be used if no specific query
	 *                             constraints are provided
	 * @param pDefaultSortCriteria An optional predicate containing default
	 *                                sort
	 *                             criteria to be used if no specific sort
	 *                             fields are provided
	 * @param rAttributes          The entity attributes to query and display
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If registering the query storage adapter fails
	 */
	public <E extends Entity> SelectionDataElement createEntitySelectionElement(
		String sName, Relatable rMetaData, Number rCurrentEntityId,
		int nCurrentSelection, QueryPredicate<E> pQuery,
		Predicate<? super E> pDefaultCriteria,
		Predicate<? super E> pDefaultSortCriteria,
		List<Function<? super Entity, ?>> rAttributes) throws StorageException {
		Class<E> rQueryType = pQuery.getQueryType();
		boolean bDisplayEntityIds = rMetaData.hasFlag(DISPLAY_ENTITY_IDS);

		EntityDefinition<?> rDef =
			EntityManager.getEntityDefinition(rQueryType);

		String sPrefix = rDef.getEntityName();

		rAttributes =
			processAttributeFunctions(rDef, rAttributes, bDisplayEntityIds);

		RelationType<?> rSortAttribute = null;
		SortDirection eSortDirection = null;

		if (pDefaultSortCriteria instanceof SortPredicate) {
			SortPredicate<?> pSort = (SortPredicate<?>) pDefaultSortCriteria;
			Object rElementDescriptor = pSort.getElementDescriptor();

			if (rElementDescriptor instanceof RelationType) {
				rSortAttribute = (RelationType<?>) rElementDescriptor;
				eSortDirection = pSort.get(MetaTypes.SORT_DIRECTION);
			}
		}

		List<ColumnDefinition> aColumns =
			createColumnDefinitions(rDef, rAttributes, sPrefix, rSortAttribute,
				eSortDirection);

		Function<Entity, List<String>> fGetAttributes =
			CollectionFunctions.createStringList(false, rAttributes);

		StorageAdapterId rStorageAdapterId =
			getDatabaseStorageAdapter(rMetaData, STORAGE_ADAPTER_ID, pQuery,
				fGetAttributes, pDefaultCriteria, pDefaultSortCriteria,
				aColumns);

		Validator<String> rValidator =
			new QueryValidator(rStorageAdapterId.toString(), aColumns);

		String sCurrentValue =
			rCurrentEntityId != null ? rCurrentEntityId.toString() : "-1";

		SelectionDataElement aResult =
			new SelectionDataElement(sName, sCurrentValue, rValidator, null);

		boolean bHierarchical =
			pQuery.get(HIERARCHICAL_QUERY_MODE) != HierarchicalQueryMode.NEVER;

		aResult.setProperty(HIERARCHICAL, bHierarchical);

		if (nCurrentSelection == -1 && rCurrentEntityId != null) {
			nCurrentSelection =
				((DatabaseStorageAdapter) rStorageAdapterRegistry.getStorageAdapter(
					rStorageAdapterId)).positionOf(rCurrentEntityId);
		}

		if (nCurrentSelection >= 0) {
			aResult.setProperty(CURRENT_SELECTION, nCurrentSelection);
		}

		return aResult;
	}

	/**
	 * Returns a data element for a certain relation of a relatable object. If
	 * the given object is an instance of {@link ProcessStep} this method
	 * invokes {@link ProcessStep#getParameterRelation(RelationType)} to query
	 * the relation to also take into account parameters that are stored in the
	 * step's process.
	 *
	 * @param rObject The related object to query the relation from
	 * @param rType   The type of the relation to convert into a data element
	 * @return The data element or NULL if it could not be mapped
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	public DataElement<?> getDataElement(Relatable rObject,
		RelationType<?> rType) throws StorageException {
		Relation<?> rRelation;
		Object rValue;
		boolean bModified = false;

		if (rObject instanceof ProcessStep) {
			// handle process step differently because getParameter reads
			// values
			// from both the process and the step
			ProcessStep rProcessStep = (ProcessStep) rObject;

			rRelation = rProcessStep.getParameterRelation(rType);
			rValue = rProcessStep.getParameter(rType);
			bModified = rProcessStep.isParameterModified(rType);
		} else {
			rRelation = rObject.getRelation(rType);
			rValue = rObject.get(rType);
		}

		DataElement<?> aDataElement = null;

		if (rRelation != null) {
			Process rProcess = rRelation.get(PROCESS);

			if (rProcess != null) {
				// redirect to parameters of a sub-process if present
				rObject = rProcess.getInteractionStep();
			}

			aDataElement = rRelation.get(DATA_ELEMENT);
		}

		if (aDataElement instanceof SelectionDataElement && !bModified) {
			// keep existing selection elements to prevent that storage
			// adapters
			// become invalid because the previous data element is garbage
			// collected and the UI doesn't update to the new element
			// because of
			// the unchanged flag. Set all properties to keep value-independent
			// flags like DISABLED
			HasProperties rProperties =
				rRelation != null ? rRelation.get(DISPLAY_PROPERTIES) : null;

			if (rProperties != null) {
				aDataElement.setProperties(rProperties, true);
			}
		} else {
			aDataElement = createDataElement(rObject, rType, rRelation,
				rValue);
		}

		return aDataElement;
	}

	/**
	 * Creates the data elements for certain relations of a relatable object.
	 * For each relation a single data element will be created by invoking the
	 * method {@link #getDataElement(Relatable, RelationType)}. If that method
	 * returns NULL no data element will be added to the result for the
	 * respective relation.
	 *
	 * @param rObject The related object to query the relations from
	 * @param rTypes  The relation types to create data elements for
	 * @param rFlags  The optional flags for each data element
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	public List<DataElement<?>> getDataElements(Relatable rObject,
		Collection<? extends RelationType<?>> rTypes, Set<Flag> rFlags)
		throws StorageException {
		List<DataElement<?>> aResult =
			new ArrayList<DataElement<?>>(rTypes.size());

		for (RelationType<?> rType : rTypes) {
			DataElement<?> aElement = getDataElement(rObject, rType);

			if (aElement != null) {
				aResult.add(aElement);
			}
		}

		return aResult;
	}

	/**
	 * Applies a list of string values by converting the values according to
	 * the
	 * given datatype and storing them in a collection.
	 *
	 * @param rValues           The values to apply
	 * @param rDatatype         The target datatype
	 * @param rTargetCollection The collection to store the converted values in
	 */
	protected void applyStringList(List<String> rValues, Class<?> rDatatype,
		Collection<?> rTargetCollection) {
		@SuppressWarnings("unchecked")
		Collection<Object> rCollection =
			(Collection<Object>) rTargetCollection;

		rTargetCollection.clear();

		for (String sValue : rValues) {
			rCollection.add(convertValue(rDatatype, sValue));
		}
	}

	/**
	 * Creates a new data element for an enum value. The returned element will
	 * be constrained to the list of possible values for the given enum value.
	 * If the current value is a single enum value the returned data element
	 * will be a {@link StringDataElement}. If the value is a collection, the
	 * returned value will be a {@link StringListDataElement} that allows the
	 * selection of multiple values.
	 *
	 * @param sName          The name of the data element
	 * @param rEnumType      The enum type
	 * @param rCurrentValue  The enum value of the element
	 * @param rAllowedValues The allow values or NULL for all enum values
	 * @param rFlags         The optional data element flags
	 * @return A new string data element for the given enum
	 * @throws IllegalArgumentException If the enum value is NULL
	 */
	DataElement<?> createEnumDataElement(String sName,
		Class<? extends Enum<?>> rEnumType, Object rCurrentValue,
		Collection<?> rAllowedValues, Set<Flag> rFlags) {
		DataElement<?> aResult = null;

		StringListValidator rValidator =
			getEnumValidator(rEnumType, rAllowedValues);

		if (rCurrentValue instanceof Collection) {
			Collection<?> aValues = (Collection<?>) rCurrentValue;
			List<String> aStringValues = new ArrayList<String>(aValues.size());

			for (Object rValue : aValues) {
				aStringValues.add(rValue.toString());
			}

			aResult =
				new StringListDataElement(sName, aStringValues, rValidator,
					rFlags);
		} else {
			String sValue =
				rCurrentValue != null ? rCurrentValue.toString() : null;

			aResult = new StringDataElement(sName, sValue, rValidator, rFlags);
		}

		return aResult;
	}

	/**
	 * Applies the data from an entity data element to the corresponding
	 * entity.
	 */
	private void applyEntityDataElement(EntityDataElement rEntityDataElement,
		Relatable rTarget, RelationType<?> rType)
		throws AuthenticationException, StorageException {
		applyDataElements(rEntityDataElement.getDataElements(),
			(Entity) rTarget.get(rType));
	}

	/**
	 * Applies the selection value of a {@link SelectionDataElement} to a
	 * certain relation of a relatable target object.
	 *
	 * @param rDataElement The source data element
	 * @param rTarget      The relatable target object
	 * @param rType        The target relation type to apply the data element
	 *                     to
	 * @throws StorageException If accessing the storage to retrieve the
	 *                          selected entity fails
	 */
	@SuppressWarnings({ "boxing", "unchecked" })
	private void applyEntitySelection(SelectionDataElement rDataElement,
		Relatable rTarget, RelationType<?> rType) throws StorageException {
		Validator<?> rValidator = rDataElement.getValidator();
		Relation<?> rRelation = rTarget.getRelation(rType);
		int nSelectedId = Integer.valueOf(rDataElement.getValue());
		Entity rEntity;

		if (rValidator instanceof QueryValidator) {
			QueryValidator rQueryValidator = (QueryValidator) rValidator;
			String sQueryId = rQueryValidator.getQueryId();

			StorageAdapter rAdapter =
				rStorageAdapterRegistry.getStorageAdapter(sQueryId);

			QueryPredicate<Entity> pQuery;

			if (rAdapter instanceof DatabaseStorageAdapter) {
				DatabaseStorageAdapter rDatabaseAdapter =
					(DatabaseStorageAdapter) rAdapter;

				pQuery = rDatabaseAdapter.getQueryPredicate();
			} else {
				throw new IllegalArgumentException(
					"Not a database storage adapter ID: " + sQueryId);
			}

			Class<Entity> rEntityType = pQuery.getQueryType();

			if (nSelectedId > 0) {
				EntityDefinition<Entity> rDef =
					EntityManager.getEntityDefinition(rEntityType);

				// ignore duplicates even if querying by ID to support views
				// which may contain duplicate generated IDs
				rEntity = EntityManager.queryEntity(rEntityType,
					rDef.getIdAttribute(), nSelectedId, false);

				if (rEntity == null) {
					throw new IllegalArgumentException(String.format(
						"Could not find entity " + "%s with ID %d",
						rEntityType,
						nSelectedId));
				}
			} else {
				rEntity = null;
			}
		} else if (rValidator instanceof SelectionValidator) {
			rEntity =
				(Entity) CollectionUtil.get(rRelation.get(ALLOWED_VALUES),
				nSelectedId);
		} else {
			throw new UnsupportedOperationException();
		}

		rTarget.set((RelationType<Entity>) rType, rEntity);
		rRelation.annotate(DATA_ELEMENT, rDataElement);
	}

	/**
	 * Checks whether certain properties that have been received from the
	 * client
	 * need to be applied to a parameter.
	 *
	 * @param rElement The data element from the client
	 * @param rTarget  The relatable target to apply properties to
	 * @param rType    The relation type to set the properties on
	 */
	private void checkApplyProperties(DataElement<?> rElement,
		Relatable rTarget, RelationType<?> rType) {
		Relation<?> rRelation = rTarget.getRelation(rType);

		if (rRelation == null) {
			rRelation = rTarget.set(rType, null);
		}

		copyDisplayProperties(rElement, rRelation,
			DataElement.SERVER_PROPERTIES);
	}

	/**
	 * This method performs the value conversion that is necessary to set a
	 * value with a certain relation type.
	 *
	 * @param rDatatype rType The relation type
	 * @param rValue    rElement rValue The raw value
	 * @return The converted value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertValue(Class<?> rDatatype, Object rValue) {
		if (rValue instanceof String) {
			String sValue = (String) rValue;

			if (rDatatype.isEnum()) {
				rValue = Enum.valueOf((Class<Enum>) rDatatype, sValue);
			} else if (rDatatype == Period.class) {
				rValue = Period.valueOf(sValue);
			} else {
				rValue = sValue.trim();
			}
		}

		return rValue;
	}

	/**
	 * Copies display properties from a data element to a relation if they
	 * exist.
	 *
	 * @param rElement    The data element to read the property value from
	 * @param rRelation   The relation on which to set the property
	 * @param rProperties The names of the properties to copy
	 */
	@SuppressWarnings("unchecked")
	private void copyDisplayProperties(DataElement<?> rElement,
		Relation<?> rRelation, PropertyName<?>... rProperties) {
		MutableProperties rDisplayProperties =
			rRelation.get(DISPLAY_PROPERTIES);

		for (PropertyName<?> rProperty : rProperties) {
			Object rValue = rElement.getProperty(rProperty, null);

			if (rDisplayProperties == null && rValue != null) {
				rDisplayProperties = new StringProperties();
				rRelation.annotate(DISPLAY_PROPERTIES, rDisplayProperties);
			}

			if (rDisplayProperties != null) {
				rDisplayProperties.setProperty((PropertyName<Object>) rProperty,
					rValue);
			}
		}
	}

	/**
	 * Creates the data models for the hierarchical children of an entity, i.e.
	 * the child entities that have the same type as the parent. This method
	 * works recursively, i.e. the result will contain the complete child
	 * hierarchy of the entity.
	 *
	 * @param rParent        The parent entity
	 * @param pChildCriteria A predicate that constrains the child entities or
	 *                       NULL for none
	 * @param pSortCriteria  The sort order criteria or NULL for none
	 * @param fGetColumnData The function to extract the entity's column data
	 *                       into a list of strings
	 * @return A list containing the data elements for the children
	 * @throws StorageException If registering the child query storage adapter
	 *                          fails
	 */
	@SuppressWarnings("boxing")
	private DataModel<DataModel<String>> createChildDataModels(Entity rParent,
		Predicate<? super Entity> pChildCriteria,
		Predicate<? super Entity> pSortCriteria,
		Function<Entity, List<String>> fGetColumnData) throws StorageException {
		RelationType<List<Entity>> rChildAttribute =
			rParent.getDefinition().getHierarchyChildAttribute();

		DataModel<DataModel<String>> aChildModel = null;

		if (rChildAttribute != null && rParent.get(rChildAttribute) != null) {
			List<Entity> rChildList = rParent.get(rChildAttribute);
			int nChildCount = rChildList.size();

			if (rChildList instanceof QueryList) {
				QueryPredicate<Entity> qChildren =
					((QueryList<Entity>) rChildList).getQueryPredicate();

				DatabaseStorageAdapter aAdapter =
					new DatabaseStorageAdapter(this);

				aAdapter.setQueryParameters(qChildren, fGetColumnData,
					pChildCriteria, pSortCriteria, null);

				StorageAdapterId rAdapterId =
					rStorageAdapterRegistry.registerStorageAdapter(aAdapter);

				if (pChildCriteria != null) {
					qChildren.set(HIERARCHY_CHILD_PREDICATE, pChildCriteria);
					nChildCount = aAdapter.querySize();
				}

				aChildModel =
					new QueryDataModel(rAdapterId.toString(), nChildCount);

				// keep ID to prevent the adapter from being garbage collected
				rParent.set(CHILD_STORAGE_ADAPTER_ID, rAdapterId);
			} else if (nChildCount > 0) {
				String sName = rChildAttribute.getName();

				List<DataModel<String>> aChildObjects =
					new ArrayList<DataModel<String>>(nChildCount);

				int nIndex = 0;

				for (Entity rChild : rChildList) {
					if (pChildCriteria == null ||
						pChildCriteria.evaluate(rChild)) {
						aChildObjects.add(
							createEntityDataObject(rChild, nIndex++,
								pChildCriteria, pSortCriteria, fGetColumnData,
								null, true));
					}
				}

				aChildModel =
					new ListDataModel<DataModel<String>>(sName, aChildObjects);
			}
		}

		return aChildModel;
	}

	/**
	 * Creates a data element for a collection datatype.
	 *
	 * @param rObject        The related object to create the element for
	 * @param rType          The type of the relation to convert into a data
	 *                       element
	 * @param rValue         The relation value
	 * @param rAllowedValues The allowed values or NULL for none
	 * @param rFlags         The data element flags
	 * @return The new data element
	 * @throws StorageException If initializing an entity query for a
	 *                          subordinate data element fails
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createCollectionDataElement(Relatable rObject,
		RelationType<?> rType, Object rValue, Collection<?> rAllowedValues,
		Set<Flag> rFlags) throws StorageException {
		Class<?> rDatatype = rType.getTargetType();
		Class<?> rElementDatatype = rType.get(ELEMENT_DATATYPE);
		String sName = rType.getName();
		DataElement<?> aDataElement;

		if (rElementDatatype != null && rElementDatatype.isEnum()) {
			if (rValue == null) {
				rValue = ReflectUtil.newInstance(
					ReflectUtil.getImplementationClass(rDatatype));
			}

			aDataElement = createEnumDataElement(sName,
				(Class<? extends Enum<?>>) rElementDatatype, rValue,
				rAllowedValues, rFlags);
		} else if (rAllowedValues != null) {
			aDataElement = createListDataElement(sName, (Collection<?>) rValue,
				rAllowedValues, rFlags);
		} else {
			aDataElement =
				createDataElementList(rObject, rType, (Collection<?>) rValue,
					rFlags);
		}

		return aDataElement;
	}

	/**
	 * Creates a new data element with a certain name and datatype. The type of
	 * the returned data element will match the given datatype as close as
	 * possible. If available the relation parameter allows this method to
	 * query
	 * additional configuration parameters (i.e. meta-relations of the
	 * relation)
	 * to further constrain the type and value of the data element.
	 *
	 * @param rObject   The related object to query the relation from
	 * @param rType     The type of the relation to convert into a data element
	 * @param rRelation The relation for additional meta data (may be NULL)
	 * @param rValue    The relation value (may be a default value even if the
	 *                  relation is NULL)
	 * @return A new instance of a data element subclass that is the best match
	 * for the value datatype
	 * @throws IllegalArgumentException If either the type or the value
	 * argument
	 *                                  is NULL
	 * @throws StorageException         If registering the child query storage
	 *                                  adapter fails
	 */
	private DataElement<?> createDataElement(Relatable rObject,
		RelationType<?> rType, Relation<?> rRelation, Object rValue)
		throws StorageException {
		assert rType != null;

		Class<?> rDatatype = rType.getTargetType();
		String sName = rType.getName();
		DataElement<?> aDataElement;

		Collection<?> rAllowedValues =
			rRelation != null ? rRelation.get(ALLOWED_VALUES) : null;

		Set<Flag> rFlags = isInputType(rObject, rType) ?
		                   DataElement.INPUT_FLAGS :
		                   DataElement.DISPLAY_FLAGS;

		// create a mutable copy of the flag list
		rFlags = EnumSet.copyOf(rFlags);

		if (rType.hasFlag(AUTOGENERATED)) {
			rFlags.add(Flag.IMMUTABLE);
		}

		if (rType.hasFlag(OPTIONAL) ||
			(rRelation != null && rRelation.hasAnnotation(OPTIONAL))) {
			rFlags.add(Flag.OPTIONAL);
		}

		if (DataElement.class.isAssignableFrom(rDatatype)) {
			aDataElement = (DataElement<?>) rValue;
		} else if (Entity.class.isAssignableFrom(rDatatype)) {
			aDataElement = createEntityDataElement(sName, rValue, rRelation,
				rAllowedValues, rFlags);
		} else if (JsonObject.class.isAssignableFrom(rDatatype)) {
			aDataElement =
				createJsonDataElement(sName, rValue, rRelation, rFlags);
		} else if (Collection.class.isAssignableFrom(rDatatype)) {
			aDataElement = createCollectionDataElement(rObject, rType, rValue,
				rAllowedValues, rFlags);
		} else {
			aDataElement = createSimpleDataElement(rDatatype, sName, rValue,
				rAllowedValues, rRelation, rFlags);
		}

		if (aDataElement != null && rRelation != null) {
			HasProperties rDisplayProperties =
				rRelation.get(DISPLAY_PROPERTIES);

			if (rDisplayProperties != null) {
				aDataElement.setProperties(rDisplayProperties, true);
			}
		}

		return aDataElement;
	}

	/**
	 * Creates an entity data element for a particular entity.
	 *
	 * @param sName   The name of the data element
	 * @param rEntity The entity to create the data element from
	 * @param rFlags  The optional data element flags
	 * @return The new entity data element
	 * @throws StorageException If a storage-specific data element
	 *                          initialization fails
	 */
	@SuppressWarnings("unused")
	private EntityDataElement createEntityDataElement(String sName,
		Entity rEntity, Set<Flag> rFlags) throws StorageException {
		@SuppressWarnings("unchecked")
		EntityDefinition<Entity> rDef =
			(EntityDefinition<Entity>) rEntity.getDefinition();

		Collection<RelationType<?>> aAttributes =
			getEntityDataElementAttributes(rDef);

		List<DataElement<?>> aAttrElements =
			getDataElements(rEntity, aAttributes, rFlags);

		List<DataElement<?>> aChildElements = new ArrayList<DataElement<?>>();

		for (EntityDefinition<?> rChildDef : rDef.getChildMappings()) {
			Class<? extends Entity> rChildType = rChildDef.getMappedType();

			String sChildren = TextConvert.toPlural(rChildDef.getEntityName());

			RelationType<? extends Entity> rParentAttribute =
				rChildDef.getParentAttribute();

			if (rParentAttribute == null) {
				rParentAttribute = rChildDef.getMasterAttribute();
			}

			Predicate<Entity> pParent =
				ifAttribute(rParentAttribute, equalTo(rEntity));

			List<Function<? super Entity, ?>> rChildAttributes =
				new ArrayList<Function<? super Entity, ?>>(
					rChildDef.getDisplayAttributes(DisplayMode.COMPACT));

			SelectionDataElement aChildElement =
				createEntitySelectionElement(sChildren, rEntity, null, -1,
					forEntity(rChildType, pParent), null, null,
					rChildAttributes);

			aChildElements.add(aChildElement);
		}

		if (aChildElements.size() > 0) {
			DataElementList aChildList =
				new DataElementList(EntityDataElement.CHILDREN_ELEMENT, null,
					aChildElements, null);

			if (aChildElements.size() > 1) {
				aChildList.setProperty(LAYOUT, LayoutType.TABS);
			}

			aAttrElements.add(aChildList);
		}

		if (sName == null) {
			sName = rDef.getEntityName();
		}

		String sAttr = rEntity.attributeString(DisplayMode.MINIMAL, ", ");
		String sPrefix = rEntity.getClass().getSimpleName();

		EntityDataElement aElement =
			new EntityDataElement(sName, sAttr, sPrefix, aAttrElements,
				rFlags);

		return aElement;
	}

	/**
	 * Creates a data element for an entity data type.
	 *
	 * @param sName          The element name
	 * @param rValue         The element value
	 * @param rRelation      The relation to create the element for
	 * @param rAllowedValues The allowed values or NULL for none
	 * @param rFlags         The element flags
	 * @return The new data element
	 * @throws StorageException If initializing an entity query fails
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createEntityDataElement(String sName, Object rValue,
		Relation<?> rRelation, Collection<?> rAllowedValues, Set<Flag> rFlags)
		throws StorageException {
		DataElement<?> aDataElement;

		if (rRelation != null &&
			(rRelation.hasAnnotation(ENTITY_QUERY_PREDICATE) ||
				rAllowedValues != null)) {
			aDataElement = createEntitySelectionElement(sName,
				(Relation<? extends Entity>) rRelation, rAllowedValues);

			rRelation.annotate(DATA_ELEMENT, aDataElement);
		} else {
			String sValue =
				rValue != null ? ((Entity) rValue).getGlobalId() : "";

			aDataElement =
				createSimpleDataElement(rRelation.getType().getTargetType(),
					sName, sValue, rAllowedValues, rRelation, rFlags);
		}

		return aDataElement;
	}

	/**
	 * Creates a list of {@link HierarchicalDataObject} instances which contain
	 * the given attributes from the argument entities. Invokes the method
	 * {@link #createEntityDataObject(Entity, Function, boolean)} for each
	 * entity in the list.
	 *
	 * @param rEntities     The list of entities to convert into data elements
	 * @param rAttributes   The access functions for the attributes to include
	 * @param bHierarchical TRUE to add children of the same entity type
	 *                      recursively
	 * @return A new list of string list data elements
	 * @throws StorageException If creating a data object fails
	 */
	private List<HierarchicalDataObject> createEntityDataObjects(
		Collection<Entity> rEntities,
		List<Function<? super Entity, ?>> rAttributes, boolean bHierarchical)
		throws StorageException {
		List<HierarchicalDataObject> aEntityObjects =
			new ArrayList<HierarchicalDataObject>(rEntities.size());
		Function<Entity, List<String>> fCollectValues =
			CollectionFunctions.createStringList(false, rAttributes);

		int nIndex = 0;

		for (Entity rEntity : rEntities) {
			aEntityObjects.add(
				createEntityDataObject(rEntity, nIndex++, null, null,
					fCollectValues, null, bHierarchical));
		}

		return aEntityObjects;
	}

	/**
	 * Creates a new data element for the selection of an entity from a list of
	 * entities that is defined by either a storage query or a list of allowed
	 * values.
	 *
	 * @param sName     The name of the element
	 * @param rRelation The relation to query for the selection meta data
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If initializing the query fails
	 */
	@SuppressWarnings("unchecked")
	private SelectionDataElement createEntitySelectionElement(String sName,
		Relation<? extends Entity> rRelation, Collection<?> rAllowedValues)
		throws StorageException {
		QueryPredicate<? extends Entity> pQuery =
			rRelation.get(ENTITY_QUERY_PREDICATE);

		List<Function<? super Entity, ?>> rAttributes =
			rRelation.get(ENTITY_ATTRIBUTES);

		List<Entity> rAllowedEntities = (List<Entity>) rAllowedValues;
		EntityDefinition<?> rDef;

		if (pQuery != null) {
			rDef = EntityManager.getEntityDefinition(pQuery.getQueryType());
		} else {
			rDef = rAllowedEntities.get(0).getDefinition();
		}

		Entity rEntity = rRelation.getTarget();
		Number rEntityId = null;

		if (rEntity != null) {
			rEntityId = rEntity.get(rDef.getIdAttribute());
		}

		if (rAttributes == null) {
			DisplayMode rMode = rRelation.get(ENTITY_DISPLAY_MODE);

			rAttributes = new ArrayList<Function<? super Entity, ?>>(
				rDef.getDisplayAttributes(rMode));
		}

		int nCurrentSelection = -1;

		if (rRelation.hasAnnotation(DISPLAY_PROPERTIES)) {
			nCurrentSelection = rRelation
				.get(DISPLAY_PROPERTIES)
				.getIntProperty(CURRENT_SELECTION, -1);
		}

		if (pQuery != null) {
			Predicate<? super Entity> pSortOrder =
				rRelation.get(ENTITY_SORT_PREDICATE);

			return createEntitySelectionElement(sName, rRelation, rEntityId,
				nCurrentSelection, pQuery, null, pSortOrder, rAttributes);
		} else {
			return createEntitySelectionElement(sName, rRelation, rEntityId,
				rAllowedEntities, rAttributes);
		}
	}

	/**
	 * Creates an integer data element.
	 *
	 * @param sName     The name of the data element
	 * @param rValue    The enum value of the element
	 * @param rMetaData A relatable object containing optional meta data
	 *                  relations or NULL for none
	 * @param rFlags    The optional data element flags
	 * @return A new string data element for the given enum
	 * @throws IllegalArgumentException If the enum value is NULL
	 */
	@SuppressWarnings("boxing")
	private IntegerDataElement createIntegerDataElement(String sName,
		Integer rValue, Relatable rMetaData, Set<Flag> rFlags) {
		Validator<? super Integer> aValidator = null;
		IntegerDataElement aResult;

		if (rMetaData != null && rMetaData.hasRelation(MINIMUM) &&
			rMetaData.hasRelation(MAXIMUM)) {
			aValidator = new IntegerRangeValidator(rMetaData.get(MINIMUM),
				rMetaData.get(MAXIMUM));
		}

		aResult = new IntegerDataElement(sName, rValue, aValidator, rFlags);

		return aResult;
	}

	/**
	 * Creates a data element that references a list of JSON data from some
	 * endpoint.
	 *
	 * @param sName     The element name
	 * @param rValue    The element value
	 * @param rRelation The relation to initialize
	 * @param rFlags    The element flags
	 */
	private SelectionDataElement createJsonDataElement(String sName,
		Object rValue, Relation<?> rRelation, Set<Flag> rFlags) {
		URL rDataUrl = rRelation.get(StandardTypes.URL);

		Objects.requireNonNull(rDataUrl, "JSON data URL is required");

		new QueryValidator(sName, null);

		return new SelectionDataElement(sName,
			SelectionDataElement.NO_SELECTION, null, rFlags);
	}

	/**
	 * Creates a list data element for a collection value and a collection of
	 * allowed values. Such elements will typically be mapped to multiselection
	 * user interface components.
	 *
	 * @param sName          The name of the data element
	 * @param rValues        The current values of the data element
	 * @param rAllowedValues The allowed values
	 * @param rFlags         The optional data element flags
	 * @return The new data element
	 */
	private DataElement<?> createListDataElement(String sName,
		Collection<?> rValues, Collection<?> rAllowedValues,
		Set<Flag> rFlags) {
		List<String> aStringValues = new ArrayList<String>(rValues.size());

		Validator<? super String> rValidator =
			createStringListValidator(rAllowedValues, false);

		for (Object rValue : rValues) {
			aStringValues.add(rValue.toString());
		}

		return new StringListDataElement(sName, aStringValues, rValidator,
			rFlags);
	}

	/**
	 * Creates a data element for a simple (non-complex) datatype. If no
	 * specific data element exist for the given datatype a new instance of
	 * {@link StringDataElement} will be returned.
	 *
	 * @param rDatatype      The datatype to create the data element for
	 * @param sName          The name of the data element
	 * @param rValue         The value of the data element
	 * @param rAllowedValues The allowed values or NULL for no constraint
	 * @param rMetaData      A relatable object containing optional meta data
	 *                       relations or NULL for none
	 * @param rFlags         The optional data element flags
	 * @return The new data element
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createSimpleDataElement(Class<?> rDatatype,
		String sName, Object rValue, Collection<?> rAllowedValues,
		Relatable rMetaData, Set<Flag> rFlags) {
		DataElement<?> rResult;

		if (rDatatype == Boolean.class) {
			rResult = new BooleanDataElement(sName, (Boolean) rValue, rFlags);
		} else if (rDatatype == Integer.class) {
			rResult =
				createIntegerDataElement(sName, (Integer) rValue, rMetaData,
					rFlags);
		} else if (rDatatype == BigDecimal.class) {
			rResult =
				new BigDecimalDataElement(sName, (BigDecimal) rValue, null,
					rFlags);
		} else if (rDatatype == Date.class) {
			rResult = new DateDataElement(sName, (Date) rValue,
				new DateValidator(null, null), rFlags);
		} else if (rDatatype == Period.class) {
			Period rPeriod = rValue != null ? (Period) rValue : Period.NONE;

			rResult = new PeriodDataElement(sName, rPeriod.getCount(),
				rPeriod.getUnit().name(),
				getEnumValidator(Unit.class, rAllowedValues), rFlags);
		} else if (DataSet.class.isAssignableFrom(rDatatype)) {
			rResult =
				new DataSetDataElement(sName, (DataSet<?>) rValue, rFlags);
		} else if (rDatatype.isEnum()) {
			rResult = createEnumDataElement(sName,
				(Class<? extends Enum<?>>) rDatatype, rValue, rAllowedValues,
				rFlags);
		} else {
			rResult =
				createStringDataElement(sName, rValue, rAllowedValues, rFlags);

			if (rDatatype == URL.class) {
				rResult.setProperty(CONTENT_TYPE, ContentType.WEBSITE);
			}
		}

		return rResult;
	}

	/**
	 * Checks a target object for a database storage adapter. If no storage
	 * adapter exist a a new storage adapter instance will be created and
	 * registered. The return value is the registered storage adapter ID.
	 *
	 * @param rTarget          The target relatable to create the adapter for
	 * @param rAdapterId       The target's attribute to check for an existing
	 *                         storage adapter ID
	 * @param pQuery           The storage query to perform
	 * @param fGetColumnData   The column data function
	 * @param pDefaultCriteria The default criteria or NULL for none
	 * @param pSortCriteria    The sort criteria or NULL for none
	 * @param rColumns         The column definitions
	 * @return The storage adapter ID
	 * @throws StorageException If registering the storage adapter fails
	 */
	@SuppressWarnings("boxing")
	private <E extends Entity> StorageAdapterId getDatabaseStorageAdapter(
		Relatable rTarget, RelationType<StorageAdapterId> rAdapterId,
		QueryPredicate<E> pQuery,
		Function<Entity, List<String>> fGetColumnData,
		Predicate<? super E> pDefaultCriteria,
		Predicate<? super E> pSortCriteria, List<ColumnDefinition> rColumns)
		throws StorageException {
		DatabaseStorageAdapter rStorageAdapter = null;
		StorageAdapterId rStorageAdapterId = rTarget.get(rAdapterId);

		if (rStorageAdapterId != null) {
			rStorageAdapter =
				(DatabaseStorageAdapter) rStorageAdapterRegistry.getStorageAdapter(
					rStorageAdapterId);
		}

		if (rStorageAdapter == null) {
			rStorageAdapter = new DatabaseStorageAdapter(this);
			rStorageAdapterId =
				rStorageAdapterRegistry.registerStorageAdapter(rStorageAdapter);

			// keep ID to prevent the adapter from being garbage collected
			// because IDs may be stored only as weak keys in a weak hash map
			if (rTarget instanceof Relation) {
				rTarget.set(rAdapterId, rStorageAdapterId);
			} else {
				rTarget.get(STORAGE_ADAPTER_IDS).add(rStorageAdapterId);
			}
		}

		rStorageAdapter.setQueryParameters(pQuery, fGetColumnData,
			pDefaultCriteria, pSortCriteria, rColumns);

		return rStorageAdapterId;
	}

	/**
	 * Returns the attributes of an entity that are to be transferred into a
	 * entity data element.
	 *
	 * @param rDefinition The entity definition to get the attributes from
	 * @return The data element attributes
	 */
	private List<RelationType<?>> getEntityDataElementAttributes(
		EntityDefinition<?> rDefinition) {
		List<RelationType<?>> aAttributes =
			new ArrayList<RelationType<?>>(rDefinition.getAttributes());

		Iterator<RelationType<?>> aAttrIterator = aAttributes.iterator();

		// remove hierarchy parent attributes to prevent endless recursion
		while (aAttrIterator.hasNext()) {
			RelationType<?> rAttr = aAttrIterator.next();

			if (rDefinition.isHierarchyAttribute(rAttr)) {
				aAttrIterator.remove();
			}
		}

		return aAttributes;
	}
}
