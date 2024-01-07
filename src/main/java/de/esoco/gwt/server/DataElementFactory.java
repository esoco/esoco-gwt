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
		enumValidatorRegistry =
		new HashMap<Class<? extends Enum<?>>, Validator<?>>();

	private static final Function<Date, Long> GET_DATE_LONG_VALUE =
		d -> d != null ? Long.valueOf(d.getTime()) : null;

	// --------------------------------------------------------

	private final StorageAdapterRegistry storageAdapterRegistry;

	// -----------------------------------------------------------

	/**
	 * Creates a new instance.
	 *
	 * @param storageAdapterRegistry The storage adapter registry to register
	 *                               storage adapter instances with
	 */
	public DataElementFactory(StorageAdapterRegistry storageAdapterRegistry) {
		this.storageAdapterRegistry = storageAdapterRegistry;
	}

	// ---------------------------------------------------------

	/**
	 * Creates an identifying name from an attribute function. If the given
	 * function is an instance of {@link FunctionChain} the identifier will be
	 * generated by recursive invocations of this method.
	 *
	 * @param function The attribute function to create the identifier from
	 * @param prefix   An optional prefix for the generated name (empty string
	 *                 for none)
	 * @return The resulting column identifier
	 */
	public static String createAttributeName(Function<?, ?> function,
		String prefix) {
		String name = "";

		if (function instanceof FunctionChain<?, ?, ?>) {
			FunctionChain<?, ?, ?> chain = (FunctionChain<?, ?, ?>) function;

			name = createAttributeName(chain.getInner(), "") +
				createAttributeName(chain.getOuter(), "");
		} else if (function instanceof FunctionPredicate<?, ?>) {
			name = createAttributeName(
				((FunctionPredicate<?, ?>) function).getFunction(), "");
		} else {
			Object attribute = getAttributeDescriptor(function);

			if (attribute instanceof RelationType<?>) {
				name =
					TextConvert.capitalizedLastElementOf(attribute.toString());
			}
		}

		return prefix + name;
	}

	/**
	 * Processes a list of entity attribute access functions and to create a
	 * list of column definitions from it.
	 *
	 * @param entityDefinition The entity definition to create the columns for
	 * @param attributes       The list of attribute functions
	 * @param prefix           The prefix string for the column titles
	 * @param sortAttribute    An optional sort attribute or NULL for none
	 * @param sortDirection    The sort direction for a sort attribute
	 * @return A list containing the corresponding column data element
	 */
	@SuppressWarnings("unchecked")
	private static List<ColumnDefinition> createColumnDefinitions(
		EntityDefinition<?> entityDefinition,
		Collection<Function<? super Entity, ?>> attributes, String prefix,
		RelationType<?> sortAttribute, SortDirection sortDirection) {
		List<ColumnDefinition> columns =
			new ArrayList<ColumnDefinition>(attributes.size());

		for (Function<? super Entity, ?> getAttr : attributes) {
			RelationType<?> displayAttr = findDisplayAttribute(getAttr);

			String name = createAttributeName(getAttr, prefix);
			String title = ColumnDefinition.STD_COLUMN_PREFIX + name;
			String datatypeName = null;
			String id;

			boolean sortable = !(getAttr instanceof GetExtraAttribute);
			boolean searchable = true;
			boolean editable = false;

			MutableProperties displayProperties =
				entityDefinition.getDisplayProperties(displayAttr);

			if (displayAttr != null) {
				Class<?> datatype = displayAttr.getTargetType();

				id = displayAttr.getName();
				datatypeName = datatype.getSimpleName();

				if (getAttr instanceof FunctionChain) {
					RelationType<?> refAttr =
						(RelationType<?>) Functions.firstInChain(getAttr);

					Class<?> refType = refAttr.getTargetType();

					if (Entity.class.isAssignableFrom(refType)) {
						// if the attribute function is for an attribute of a
						// referenced entity then add the display properties
						// from that entity and disable sorting and searching
						EntityDefinition<?> refDef =
							EntityManager.getEntityDefinition(
								(Class<? extends Entity>) refType);

						HasProperties baseProperties =
							entityDefinition.getDisplayProperties(refAttr);

						displayProperties =
							refDef.getDisplayProperties(displayAttr);

						// base properties always override reference properties
						displayProperties.setProperties(baseProperties, true);

						id = refAttr.getName();
						searchable = false;
					}
				}

				if (displayAttr == getAttr || datatype.isEnum() ||
					datatype.isAssignableFrom(Date.class)) {
					if (datatype.isEnum()) {
						setEnumColumnProperties(
							(Class<? extends Enum<?>>) datatype,
							displayProperties);
						datatypeName = Enum.class.getSimpleName();
					}
				} else {
					searchable = false;
				}
			} else {
				id = name;
				searchable = false;
			}

			SimpleColumnDefinition column =
				new SimpleColumnDefinition(id, title, datatypeName, sortable,
					searchable, editable);

			if (displayProperties != null &&
				displayProperties.getPropertyCount() > 0) {
				column.setProperties(displayProperties, true);
			}

			if (displayAttr != null && displayAttr == sortAttribute) {
				column.setProperty(SORT_DIRECTION, sortDirection);
			}

			columns.add(column);
		}

		return columns;
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection from a list
	 * of data objects.
	 *
	 * @param name             The name of the element
	 * @param currentValue     The currently selected value
	 * @param prefix           The prefix to be prepended to generated names
	 * @param entityDefinition The entity definition for the objects or NULL
	 *                           for
	 *                         none
	 * @param dataObjects      entities The entities to select from
	 * @param attributes       The entity attributes to display
	 * @param displayEntityIds TRUE to display entity IDs instead of string
	 *                         descriptions for entity attributes
	 * @return A new data element for the selection of an entity
	 */
	public static SelectionDataElement createSelectionDataElement(String name,
		String currentValue, String prefix,
		EntityDefinition<?> entityDefinition,
		List<HierarchicalDataObject> dataObjects,
		Collection<Function<? super Entity, ?>> attributes,
		boolean displayEntityIds) {
		attributes = processAttributeFunctions(entityDefinition, attributes,
			displayEntityIds);

		List<ColumnDefinition> columns =
			createColumnDefinitions(entityDefinition, attributes, prefix, null,
				null);

		Validator<String> validator =
			new SelectionValidator(dataObjects, columns);

		return new SelectionDataElement(name, currentValue, validator, null);
	}

	/**
	 * Creates a new string data element with a certain value. The meta data
	 * argument will be queried for a relation of the type
	 * {@link ProcessRelationTypes#ALLOWED_VALUES} that will constrain the
	 * possible values of the data element.
	 *
	 * @param name          The name of the data element
	 * @param value         The initial value of the element
	 * @param allowedValues The allowed values to create a validator for or
	 *                         NULL
	 *                      for none
	 * @param flags         The optional data element flags
	 * @return The new data element
	 */
	public static StringDataElement createStringDataElement(String name,
		Object value, Collection<?> allowedValues, Set<Flag> flags) {
		String text = value != null ? value.toString() : null;

		StringListValidator validator = null;

		if (allowedValues != null) {
			validator = createStringListValidator(allowedValues, false);
		}

		return new StringDataElement(name, text, validator, flags);
	}

	/**
	 * Creates a new string list validator instance. The given list of values
	 * will be converted by invoking their {@link Object#toString() toString()}
	 * method.
	 *
	 * @param values      The values to be validated against
	 * @param resourceIds Corresponds to same the flag of
	 *                    {@link StringListValidator#StringListValidator(List,
	 *                    boolean)}
	 * @return The new validator instance
	 */
	static StringListValidator createStringListValidator(Collection<?> values,
		boolean resourceIds) {
		StringListValidator validator;
		List<String> valueList = new ArrayList<>(values.size());

		for (Object allowedValue : values) {
			valueList.add(allowedValue.toString());
		}

		validator = new StringListValidator(valueList, resourceIds);

		return validator;
	}

	/**
	 * Returns the relation type of an attribute access function. If the
	 * function consists of one or more function chains the returned relation
	 * type will be that of the innermost relation type of the chain if such
	 * exists. It will be determined by parsing the branch of inner functions
	 * recursively.
	 *
	 * @param accessFunction The attribute access function to parse
	 * @return The innermost attribute relation type or NULL for none
	 */
	private static RelationType<?> findDisplayAttribute(
		Function<?, ?> accessFunction) {
		RelationType<?> result = null;

		if (accessFunction instanceof RelationType<?>) {
			result = (RelationType<?>) accessFunction;
		} else if (accessFunction instanceof FunctionChain<?, ?, ?>) {
			FunctionChain<?, ?, ?> chain =
				(FunctionChain<?, ?, ?>) accessFunction;

			result = findDisplayAttribute(chain.getOuter());

			if (result == null) {
				result = findDisplayAttribute(chain.getInner());
			}
		} else if (accessFunction instanceof FunctionPredicate<?, ?>) {
			result = findDisplayAttribute(
				((FunctionPredicate<?, ?>) accessFunction).getFunction());
		} else {
			Object attribute = getAttributeDescriptor(accessFunction);

			if (attribute instanceof RelationType<?>) {
				result = (RelationType<?>) attribute;
			}
		}

		return result;
	}

	/**
	 * Returns the attribute descriptor from a function.
	 *
	 * @param function The function
	 * @return The attribute descriptor or NULL if the function doesn't contain
	 * one
	 */
	private static Object getAttributeDescriptor(Function<?, ?> function) {
		Object attribute = null;

		if (function instanceof BinaryFunction<?, ?, ?>) {
			attribute = ((BinaryFunction<?, ?, ?>) function).getRightValue();
		} else if (function instanceof ElementAccess<?>) {
			attribute = ((ElementAccess<?>) function).getElementDescriptor();
		}

		return attribute;
	}

	/**
	 * Returns a validator for a certain enum class. To reduce serialization
	 * sizes validator instances are cached internally so that the same
	 * validator will be returned on subsequent invocations with the same enum
	 * class. The enum values will be stored in the validator as the enum names
	 * converted to camel case.
	 *
	 * @param enumClass     The enum class to return the validator for
	 * @param allowedValues The allowed values or NULL for all enum values
	 * @return The validator for the given enum class
	 */
	static StringListValidator getEnumValidator(
		Class<? extends Enum<?>> enumClass, Collection<?> allowedValues) {
		StringListValidator validator;

		if (allowedValues == null) {
			validator =
				(StringListValidator) enumValidatorRegistry.get(enumClass);

			if (validator == null) {
				Enum<?>[] enums = enumClass.getEnumConstants();

				validator =
					createStringListValidator(Arrays.asList(enums), true);

				enumValidatorRegistry.put(enumClass, validator);
			}
		} else {
			validator = createStringListValidator(allowedValues, true);
		}

		return validator;
	}

	/**
	 * Checks whether a certain relation type is a process input parameter.
	 *
	 * @param object The object to check for the input parameter information
	 * @param type   The relation type
	 * @return TRUE if the given relation type is for input
	 */
	private static boolean isInputType(Relatable object,
		RelationType<?> type) {
		return object.get(INPUT_PARAMS).contains(type);
	}

	/**
	 * Converts an attribute access function (typically a relation type) into a
	 * function that provides a translated attribute value if necessary. This
	 * method will convert attributes that reference certain datatypes like
	 * entities, enums, or dates into special access functions.
	 *
	 * @param entityDefinition The entity definition of the attribute
	 * @param getAttr          The attribute access functions
	 * @param displayEntityIds TRUE to display entity IDs instead of string
	 *                         descriptions for entity attributes
	 * @return Either the original function or a function that performs the
	 * necessary transformation
	 */
	@SuppressWarnings("unchecked")
	public static Function<? super Entity, ?> processAttributeFunction(
		EntityDefinition<?> entityDefinition,
		Function<? super Entity, ?> getAttr, boolean displayEntityIds) {
		Class<?> datatype = String.class;
		RelationType<?> attribute = null;

		if (getAttr instanceof RelationType<?>) {
			attribute = (RelationType<?>) getAttr;
			datatype = attribute.getTargetType();

			if (Entity.class.isAssignableFrom(datatype)) {
				if (!displayEntityIds) {
					getAttr = EntityFunctions
						.formatEntity("")
						.from((RelationType<Entity>) getAttr);
				} else if (datatype == Entity.class) {
					getAttr = EntityFunctions
						.getGlobalEntityId()
						.from((RelationType<Entity>) getAttr);
				} else {
					getAttr = EntityFunctions
						.getEntityId()
						.from((RelationType<Entity>) getAttr);
				}
			}
		} else if (getAttr instanceof FunctionChain) {
			attribute = findDisplayAttribute(getAttr);
			datatype = attribute.getTargetType();
		}

		if (datatype.isEnum()) {
			String enumPrefix = datatype.getSimpleName();
			StringBuilder enumItem =
				new StringBuilder(DataElement.ITEM_RESOURCE_PREFIX);

			HasProperties displayProperties =
				entityDefinition.getDisplayProperties(attribute);

			if (displayProperties != null) {
				enumPrefix =
					displayProperties.getProperty(RESOURCE_ID, enumPrefix);
			}

			enumItem.append(enumPrefix);
			enumItem.append("%s");

			// add formatting function for enum values that creates a
			// resource identifier
			getAttr = format(enumItem.toString()).from(
				capitalizedIdentifier().from(asString().from(getAttr)));
		} else if (Date.class.isAssignableFrom(datatype)) {
			// convert dates into their long values
			getAttr = GET_DATE_LONG_VALUE.from(
				(Function<? super Entity, ? extends Date>) getAttr);
		} else if (datatype == Boolean.class) {
			String attr =
				TextConvert.capitalizedIdentifier(attribute.getSimpleName());

			attr = DataElement.ITEM_RESOURCE_PREFIX + attr + "%s";

			getAttr = format(attr).from(
				capitalizedIdentifier().from(asString().from(getAttr)));
		}

		return getAttr;
	}

	/**
	 * Converts a list of attribute access functions (typically relation types)
	 * into functions that provide a translated attribute value if necessary.
	 * This method will convert attributes that reference certain datatypes
	 * like
	 * entities, enums, or dates into special access functions.
	 *
	 * @param entityDefinition The entity definition of the attributes
	 * @param attributes       The list of attribute access functions
	 * @param displayEntityIds TRUE to display entity IDs instead of string
	 *                         descriptions for entity attributes
	 * @return A list containing either the original functions or functions
	 * that
	 * perform the necessary transformations
	 */
	public static List<Function<? super Entity, ?>> processAttributeFunctions(
		EntityDefinition<?> entityDefinition,
		Collection<Function<? super Entity, ?>> attributes,
		boolean displayEntityIds) {
		int count = attributes.size();

		List<Function<? super Entity, ?>> result =
			new ArrayList<Function<? super Entity, ?>>(count);

		for (Function<? super Entity, ?> getAttr : attributes) {
			result.add(processAttributeFunction(entityDefinition, getAttr,
				displayEntityIds));
		}

		return result;
	}

	/**
	 * Set the display properties for an enum table column.
	 *
	 * @param datatype   The enum datatype class of the column
	 * @param properties The display properties to modify
	 */
	public static void setEnumColumnProperties(
		Class<? extends Enum<?>> datatype, MutableProperties properties) {
		String allowedValues = null;

		if (properties.hasProperty(UserInterfaceProperties.ALLOWED_VALUES)) {
			allowedValues =
				properties.getProperty(UserInterfaceProperties.ALLOWED_VALUES,
					"");
		}

		if (allowedValues == null || allowedValues.isEmpty()) {
			Object[] enumValues = datatype.getEnumConstants();

			allowedValues = CollectionUtil.toString(enumValues, ",");
		}

		properties.setProperty(VALUE_RESOURCE_PREFIX,
			DataElement.ITEM_RESOURCE_PREFIX + datatype.getSimpleName());
		properties.setProperty(UserInterfaceProperties.ALLOWED_VALUES,
			allowedValues);
	}

	// ----------------------------------------------------------------

	/**
	 * Applies a single data element to a relatable object by mapping the
	 * element back to the corresponding relation from which it had been
	 * created.
	 *
	 * @param element The data element to apply
	 * @param target  The relatable object to apply the data element to
	 * @param type    The type of the relation to apply
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	@SuppressWarnings("unchecked")
	public void applyDataElement(DataElement<?> element, Relatable target,
		RelationType<?> type) throws AuthenticationException,
		StorageException {
		Class<?> targetDatatype = type.getTargetType();

		if (DataElement.class.isAssignableFrom(targetDatatype)) {
			// if the relation directly stores a data element no mapping is
			// necessary
			RelationType<DataElement<?>> dataElementType =
				(RelationType<DataElement<?>>) type;

			target.set(dataElementType, element);
		} else if (element instanceof EntityDataElement) {
			// apply attribute data elements recursively to target entity
			EntityDataElement entityDataElement = (EntityDataElement) element;

			applyEntityDataElement(entityDataElement, target, type);
		} else if (element instanceof SelectionDataElement) {
			applyEntitySelection((SelectionDataElement) element, target, type);
		} else if (element instanceof StringListDataElement &&
			Collection.class.isAssignableFrom(targetDatatype)) {
			Collection<?> targetCollection = (Collection<?>) target.get(type);

			applyStringList(((StringListDataElement) element).getList(),
				type.get(ELEMENT_DATATYPE), targetCollection);
		} else if (!(element instanceof DataElementList)) {
			target.set((RelationType<Object>) type,
				convertValue(targetDatatype, element.getValue()));
		}
	}

	/**
	 * Applies a set of data elements to a relatable object by mapping the
	 * element back to the corresponding relation from which it had been
	 * created. Immutable elements will be ignored.
	 *
	 * @param sourceElements The list of data elements to apply
	 * @param target         The relatable object to apply the data elements to
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	public void applyDataElements(List<? extends DataElement<?>> sourceElements,
		Relatable target) throws AuthenticationException, StorageException {
		for (DataElement<?> element : sourceElements) {
			RelationType<?> type = RelationType.valueOf(element.getName());

			if (type != null) {
				if (!element.isImmutable()) {
					if (!element.isOptional() || element.isSelected()) {
						applyDataElement(element, target, type);
					}
				}

				checkApplyProperties(element, target, type);

				if (element instanceof DataElementList) {
					Process subProcess =
						target.getRelation(type).getAnnotation(PROCESS);

					applyDataElements(((DataElementList) element).getElements(),
						subProcess != null ? subProcess : target);
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
	 * @param object The target object of the collection relation
	 * @param type   The collection relation type
	 * @param values A collection containing the values to be converted into
	 *               data elements (may be NULL)
	 * @param flags  The optional data element flags
	 * @return The new list data element
	 * @throws StorageException If creating data elements recursively fails
	 */
	public DataElementList createDataElementList(Relatable object,
		RelationType<?> type, Collection<?> values, Set<Flag> flags)
		throws StorageException {
		Class<?> elementDatatype = type.get(ELEMENT_DATATYPE);

		boolean recursive = (elementDatatype != null &&
			RelationType.class.isAssignableFrom(elementDatatype));

		List<DataElement<?>> dataElements = null;

		if (values != null) {
			dataElements = new ArrayList<DataElement<?>>(values.size());

			for (Object value : values) {
				DataElement<?> dataElement;

				if (recursive) {
					dataElement =
						getDataElement(object, (RelationType<?>) value);
				} else {
					dataElement =
						new StringDataElement(value.getClass().getSimpleName(),
							value.toString());
				}

				if (dataElement != null) {
					dataElements.add(dataElement);
				}
			}
		}

		return new DataElementList(type.getName(), null, dataElements, flags);
	}

	/**
	 * Creates a data object for a certain entity in a storage query.
	 *
	 * @param entity        The entity to create the data element from
	 * @param index         The index of the data object
	 * @param childCriteria A predicate that constrains the child entities
	 *                         to be
	 *                      included in a hierarchical object or NULL for none
	 * @param sortCriteria  The sort criteria for child queries (NULL for none)
	 * @param getColumnData The function to extract the entity's column data
	 *                      into a list of strings
	 * @param flags         The flags for the entity object
	 * @param hierarchical  TRUE, to include children of the same type as the
	 *                      entity
	 * @return The resulting data element
	 * @throws StorageException If creating the data object fails
	 */
	public HierarchicalDataObject createEntityDataObject(Entity entity,
		int index, Predicate<? super Entity> childCriteria,
		Predicate<? super Entity> sortCriteria,
		Function<Entity, List<String>> getColumnData, Collection<String> flags,
		boolean hierarchical) throws StorageException {
		List<String> values = getColumnData.evaluate(entity);

		DataModel<DataModel<String>> children = null;

		if (hierarchical &&
			entity.getDefinition().getHierarchyChildAttribute() != null) {
			children =
				createChildDataModels(entity, childCriteria, sortCriteria,
					getColumnData);
		}

		return new HierarchicalDataObject(Long.toString(entity.getId()), index,
			values, true, flags, children);
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection of a entity
	 * data element from a list of entities.
	 *
	 * @param name            The name of the element
	 * @param metaData        A relatable object containing the meta data for
	 *                        the element to create
	 * @param currentEntityId sCurrentValue The current selection or NULL for
	 *                        none
	 * @param entities        The entities to select from
	 * @param attributes      The entity attributes to display
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If initializing the query fails
	 */
	public SelectionDataElement createEntitySelectionElement(String name,
		Relatable metaData, Number currentEntityId, List<Entity> entities,
		List<Function<? super Entity, ?>> attributes) throws StorageException {
		EntityDefinition<?> def = entities.get(0).getDefinition();
		String prefix = def.getEntityName();

		List<HierarchicalDataObject> entityObjects =
			createEntityDataObjects(entities, attributes,
				metaData.hasFlag(MetaTypes.HIERARCHICAL));

		String currentValue =
			currentEntityId != null ? currentEntityId.toString() : "-1";

		SelectionDataElement result =
			createSelectionDataElement(name, currentValue, prefix, def,
				entityObjects, attributes,
				metaData.hasFlag(DISPLAY_ENTITY_IDS));

		return result;
	}

	/**
	 * Creates a new {@link SelectionDataElement} for the selection of a entity
	 * data element from a list of entities that is defined by a storage.
	 *
	 * @param name                The name of the element
	 * @param metaData            A relatable object containing the meta data
	 *                            for the element to create
	 * @param currentEntityId     currentValue The current selection or NULL
	 *                              for
	 *                            none
	 * @param currentSelection    The index of the current selection or -1
	 * @param query               The query for the entities to select from
	 * @param defaultCriteria     An optional predicate containing default
	 *                            criteria to be used if no specific query
	 *                            constraints are provided
	 * @param defaultSortCriteria An optional predicate containing default sort
	 *                            criteria to be used if no specific sort
	 *                            fields
	 *                            are provided
	 * @param attributes          The entity attributes to query and display
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If registering the query storage adapter fails
	 */
	public <E extends Entity> SelectionDataElement createEntitySelectionElement(
		String name, Relatable metaData, Number currentEntityId,
		int currentSelection, QueryPredicate<E> query,
		Predicate<? super E> defaultCriteria,
		Predicate<? super E> defaultSortCriteria,
		List<Function<? super Entity, ?>> attributes) throws StorageException {
		Class<E> queryType = query.getQueryType();
		boolean displayEntityIds = metaData.hasFlag(DISPLAY_ENTITY_IDS);

		EntityDefinition<?> def = EntityManager.getEntityDefinition(queryType);

		String prefix = def.getEntityName();

		attributes =
			processAttributeFunctions(def, attributes, displayEntityIds);

		RelationType<?> sortAttribute = null;
		SortDirection sortDirection = null;

		if (defaultSortCriteria instanceof SortPredicate) {
			SortPredicate<?> sort = (SortPredicate<?>) defaultSortCriteria;
			Object elementDescriptor = sort.getElementDescriptor();

			if (elementDescriptor instanceof RelationType) {
				sortAttribute = (RelationType<?>) elementDescriptor;
				sortDirection = sort.get(MetaTypes.SORT_DIRECTION);
			}
		}

		List<ColumnDefinition> columns =
			createColumnDefinitions(def, attributes, prefix, sortAttribute,
				sortDirection);

		Function<Entity, List<String>> getAttributes =
			CollectionFunctions.createStringList(false, attributes);

		StorageAdapterId storageAdapterId =
			getDatabaseStorageAdapter(metaData, STORAGE_ADAPTER_ID, query,
				getAttributes, defaultCriteria, defaultSortCriteria, columns);

		Validator<String> validator =
			new QueryValidator(storageAdapterId.toString(), columns);

		String currentValue =
			currentEntityId != null ? currentEntityId.toString() : "-1";

		SelectionDataElement result =
			new SelectionDataElement(name, currentValue, validator, null);

		boolean hierarchical =
			query.get(HIERARCHICAL_QUERY_MODE) != HierarchicalQueryMode.NEVER;

		result.setProperty(HIERARCHICAL, hierarchical);

		if (currentSelection == -1 && currentEntityId != null) {
			currentSelection =
				((DatabaseStorageAdapter) storageAdapterRegistry.getStorageAdapter(
					storageAdapterId)).positionOf(currentEntityId);
		}

		if (currentSelection >= 0) {
			result.setProperty(CURRENT_SELECTION, currentSelection);
		}

		return result;
	}

	/**
	 * Returns a data element for a certain relation of a relatable object. If
	 * the given object is an instance of {@link ProcessStep} this method
	 * invokes {@link ProcessStep#getParameterRelation(RelationType)} to query
	 * the relation to also take into account parameters that are stored in the
	 * step's process.
	 *
	 * @param object The related object to query the relation from
	 * @param type   The type of the relation to convert into a data element
	 * @return The data element or NULL if it could not be mapped
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	public DataElement<?> getDataElement(Relatable object,
		RelationType<?> type)
		throws StorageException {
		Relation<?> relation;
		Object value;
		boolean modified = false;

		if (object instanceof ProcessStep) {
			// handle process step differently because getParameter reads
			// values
			// from both the process and the step
			ProcessStep processStep = (ProcessStep) object;

			relation = processStep.getParameterRelation(type);
			value = processStep.getParameter(type);
			modified = processStep.isParameterModified(type);
		} else {
			relation = object.getRelation(type);
			value = object.get(type);
		}

		DataElement<?> dataElement = null;

		if (relation != null) {
			Process process = relation.get(PROCESS);

			if (process != null) {
				// redirect to parameters of a sub-process if present
				object = process.getInteractionStep();
			}

			dataElement = relation.get(DATA_ELEMENT);
		}

		if (dataElement instanceof SelectionDataElement && !modified) {
			// keep existing selection elements to prevent that storage
			// adapters
			// become invalid because the previous data element is garbage
			// collected and the UI doesn't update to the new element
			// because of
			// the unchanged flag. Set all properties to keep value-independent
			// flags like DISABLED
			HasProperties properties =
				relation != null ? relation.get(DISPLAY_PROPERTIES) : null;

			if (properties != null) {
				dataElement.setProperties(properties, true);
			}
		} else {
			dataElement = createDataElement(object, type, relation, value);
		}

		return dataElement;
	}

	/**
	 * Creates the data elements for certain relations of a relatable object.
	 * For each relation a single data element will be created by invoking the
	 * method {@link #getDataElement(Relatable, RelationType)}. If that method
	 * returns NULL no data element will be added to the result for the
	 * respective relation.
	 *
	 * @param object The related object to query the relations from
	 * @param types  The relation types to create data elements for
	 * @param flags  The optional flags for each data element
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	public List<DataElement<?>> getDataElements(Relatable object,
		Collection<? extends RelationType<?>> types, Set<Flag> flags)
		throws StorageException {
		List<DataElement<?>> result =
			new ArrayList<DataElement<?>>(types.size());

		for (RelationType<?> type : types) {
			DataElement<?> element = getDataElement(object, type);

			if (element != null) {
				result.add(element);
			}
		}

		return result;
	}

	/**
	 * Applies a list of string values by converting the values according to
	 * the
	 * given datatype and storing them in a collection.
	 *
	 * @param values           The values to apply
	 * @param datatype         The target datatype
	 * @param targetCollection The collection to store the converted values in
	 */
	protected void applyStringList(List<String> values, Class<?> datatype,
		Collection<?> targetCollection) {
		@SuppressWarnings("unchecked")
		Collection<Object> collection = (Collection<Object>) targetCollection;

		targetCollection.clear();

		for (String value : values) {
			collection.add(convertValue(datatype, value));
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
	 * @param name          The name of the data element
	 * @param enumType      The enum type
	 * @param currentValue  The enum value of the element
	 * @param allowedValues The allow values or NULL for all enum values
	 * @param flags         The optional data element flags
	 * @return A new string data element for the given enum
	 * @throws IllegalArgumentException If the enum value is NULL
	 */
	DataElement<?> createEnumDataElement(String name,
		Class<? extends Enum<?>> enumType, Object currentValue,
		Collection<?> allowedValues, Set<Flag> flags) {
		DataElement<?> result = null;

		StringListValidator validator =
			getEnumValidator(enumType, allowedValues);

		if (currentValue instanceof Collection) {
			Collection<?> values = (Collection<?>) currentValue;
			List<String> stringValues = new ArrayList<String>(values.size());

			for (Object value : values) {
				stringValues.add(value.toString());
			}

			result =
				new StringListDataElement(name, stringValues, validator,
					flags);
		} else {
			String value =
				currentValue != null ? currentValue.toString() : null;

			result = new StringDataElement(name, value, validator, flags);
		}

		return result;
	}

	/**
	 * Applies the data from an entity data element to the corresponding
	 * entity.
	 */
	private void applyEntityDataElement(EntityDataElement entityDataElement,
		Relatable target, RelationType<?> type)
		throws AuthenticationException, StorageException {
		applyDataElements(entityDataElement.getDataElements(),
			(Entity) target.get(type));
	}

	/**
	 * Applies the selection value of a {@link SelectionDataElement} to a
	 * certain relation of a relatable target object.
	 *
	 * @param dataElement The source data element
	 * @param target      The relatable target object
	 * @param type        The target relation type to apply the data element to
	 * @throws StorageException If accessing the storage to retrieve the
	 *                          selected entity fails
	 */
	@SuppressWarnings({ "boxing", "unchecked" })
	private void applyEntitySelection(SelectionDataElement dataElement,
		Relatable target, RelationType<?> type) throws StorageException {
		Validator<?> validator = dataElement.getValidator();
		Relation<?> relation = target.getRelation(type);
		int selectedId = Integer.valueOf(dataElement.getValue());
		Entity entity;

		if (validator instanceof QueryValidator) {
			QueryValidator queryValidator = (QueryValidator) validator;
			String queryId = queryValidator.getQueryId();

			StorageAdapter adapter =
				storageAdapterRegistry.getStorageAdapter(queryId);

			QueryPredicate<Entity> query;

			if (adapter instanceof DatabaseStorageAdapter) {
				DatabaseStorageAdapter databaseAdapter =
					(DatabaseStorageAdapter) adapter;

				query = databaseAdapter.getQueryPredicate();
			} else {
				throw new IllegalArgumentException(
					"Not a database storage adapter ID: " + queryId);
			}

			Class<Entity> entityType = query.getQueryType();

			if (selectedId > 0) {
				EntityDefinition<Entity> def =
					EntityManager.getEntityDefinition(entityType);

				// ignore duplicates even if querying by ID to support views
				// which may contain duplicate generated IDs
				entity =
					EntityManager.queryEntity(entityType, def.getIdAttribute(),
						selectedId, false);

				if (entity == null) {
					throw new IllegalArgumentException(String.format(
						"Could not find entity " + "%s with ID %d", entityType,
						selectedId));
				}
			} else {
				entity = null;
			}
		} else if (validator instanceof SelectionValidator) {
			entity = (Entity) CollectionUtil.get(relation.get(ALLOWED_VALUES),
				selectedId);
		} else {
			throw new UnsupportedOperationException();
		}

		target.set((RelationType<Entity>) type, entity);
		relation.annotate(DATA_ELEMENT, dataElement);
	}

	/**
	 * Checks whether certain properties that have been received from the
	 * client
	 * need to be applied to a parameter.
	 *
	 * @param element The data element from the client
	 * @param target  The relatable target to apply properties to
	 * @param type    The relation type to set the properties on
	 */
	private void checkApplyProperties(DataElement<?> element, Relatable target,
		RelationType<?> type) {
		Relation<?> relation = target.getRelation(type);

		if (relation == null) {
			relation = target.set(type, null);
		}

		copyDisplayProperties(element, relation,
			DataElement.SERVER_PROPERTIES);
	}

	/**
	 * This method performs the value conversion that is necessary to set a
	 * value with a certain relation type.
	 *
	 * @param datatype rType The relation type
	 * @param value    element rValue The raw value
	 * @return The converted value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertValue(Class<?> datatype, Object value) {
		if (value instanceof String) {
			String text = value.toString();

			if (datatype.isEnum()) {
				value = Enum.valueOf((Class<Enum>) datatype, text);
			} else if (datatype == Period.class) {
				value = Period.valueOf(text);
			} else {
				value = text.trim();
			}
		}

		return value;
	}

	/**
	 * Copies display properties from a data element to a relation if they
	 * exist.
	 *
	 * @param element    The data element to read the property value from
	 * @param relation   The relation on which to set the property
	 * @param properties The names of the properties to copy
	 */
	@SuppressWarnings("unchecked")
	private void copyDisplayProperties(DataElement<?> element,
		Relation<?> relation, PropertyName<?>... properties) {
		MutableProperties displayProperties = relation.get(DISPLAY_PROPERTIES);

		for (PropertyName<?> property : properties) {
			Object value = element.getProperty(property, null);

			if (displayProperties == null && value != null) {
				displayProperties = new StringProperties();
				relation.annotate(DISPLAY_PROPERTIES, displayProperties);
			}

			if (displayProperties != null) {
				displayProperties.setProperty((PropertyName<Object>) property,
					value);
			}
		}
	}

	/**
	 * Creates the data models for the hierarchical children of an entity, i.e.
	 * the child entities that have the same type as the parent. This method
	 * works recursively, i.e. the result will contain the complete child
	 * hierarchy of the entity.
	 *
	 * @param parent        The parent entity
	 * @param childCriteria A predicate that constrains the child entities or
	 *                      NULL for none
	 * @param sortCriteria  The sort order criteria or NULL for none
	 * @param getColumnData The function to extract the entity's column data
	 *                      into a list of strings
	 * @return A list containing the data elements for the children
	 * @throws StorageException If registering the child query storage adapter
	 *                          fails
	 */
	@SuppressWarnings("boxing")
	private DataModel<DataModel<String>> createChildDataModels(Entity parent,
		Predicate<? super Entity> childCriteria,
		Predicate<? super Entity> sortCriteria,
		Function<Entity, List<String>> getColumnData) throws StorageException {
		RelationType<List<Entity>> childAttribute =
			parent.getDefinition().getHierarchyChildAttribute();

		DataModel<DataModel<String>> childModel = null;

		if (childAttribute != null && parent.get(childAttribute) != null) {
			List<Entity> childList = parent.get(childAttribute);
			int childCount = childList.size();

			if (childList instanceof QueryList) {
				QueryPredicate<Entity> children =
					((QueryList<Entity>) childList).getQueryPredicate();

				DatabaseStorageAdapter adapter =
					new DatabaseStorageAdapter(this);

				adapter.setQueryParameters(children, getColumnData,
					childCriteria, sortCriteria, null);

				StorageAdapterId adapterId =
					storageAdapterRegistry.registerStorageAdapter(adapter);

				if (childCriteria != null) {
					children.set(HIERARCHY_CHILD_PREDICATE, childCriteria);
					childCount = adapter.querySize();
				}

				childModel =
					new QueryDataModel(adapterId.toString(), childCount);

				// keep ID to prevent the adapter from being garbage collected
				parent.set(CHILD_STORAGE_ADAPTER_ID, adapterId);
			} else if (childCount > 0) {
				String name = childAttribute.getName();

				List<DataModel<String>> childObjects =
					new ArrayList<DataModel<String>>(childCount);

				int index = 0;

				for (Entity child : childList) {
					if (childCriteria == null ||
						childCriteria.evaluate(child)) {
						childObjects.add(createEntityDataObject(child, index++,
							childCriteria, sortCriteria, getColumnData, null,
							true));
					}
				}

				childModel =
					new ListDataModel<DataModel<String>>(name, childObjects);
			}
		}

		return childModel;
	}

	/**
	 * Creates a data element for a collection datatype.
	 *
	 * @param object        The related object to create the element for
	 * @param type          The type of the relation to convert into a data
	 *                      element
	 * @param value         The relation value
	 * @param allowedValues The allowed values or NULL for none
	 * @param flags         The data element flags
	 * @return The new data element
	 * @throws StorageException If initializing an entity query for a
	 *                          subordinate data element fails
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createCollectionDataElement(Relatable object,
		RelationType<?> type, Object value, Collection<?> allowedValues,
		Set<Flag> flags) throws StorageException {
		Class<?> datatype = type.getTargetType();
		Class<?> elementDatatype = type.get(ELEMENT_DATATYPE);
		String name = type.getName();
		DataElement<?> dataElement;

		if (elementDatatype != null && elementDatatype.isEnum()) {
			if (value == null) {
				value = ReflectUtil.newInstance(
					ReflectUtil.getImplementationClass(datatype));
			}

			dataElement = createEnumDataElement(name,
				(Class<? extends Enum<?>>) elementDatatype, value,
				allowedValues, flags);
		} else if (allowedValues != null) {
			dataElement = createListDataElement(name, (Collection<?>) value,
				allowedValues, flags);
		} else {
			dataElement =
				createDataElementList(object, type, (Collection<?>) value,
					flags);
		}

		return dataElement;
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
	 * @param object   The related object to query the relation from
	 * @param type     The type of the relation to convert into a data element
	 * @param relation The relation for additional meta data (may be NULL)
	 * @param value    The relation value (may be a default value even if the
	 *                 relation is NULL)
	 * @return A new instance of a data element subclass that is the best match
	 * for the value datatype
	 * @throws IllegalArgumentException If either the type or the value
	 * argument
	 *                                  is NULL
	 * @throws StorageException         If registering the child query storage
	 *                                  adapter fails
	 */
	private DataElement<?> createDataElement(Relatable object,
		RelationType<?> type, Relation<?> relation, Object value)
		throws StorageException {
		assert type != null;

		Class<?> datatype = type.getTargetType();
		String name = type.getName();
		DataElement<?> dataElement;

		Collection<?> allowedValues =
			relation != null ? relation.get(ALLOWED_VALUES) : null;

		Set<Flag> flags = isInputType(object, type) ?
		                  DataElement.INPUT_FLAGS :
		                  DataElement.DISPLAY_FLAGS;

		// create a mutable copy of the flag list
		flags = EnumSet.copyOf(flags);

		if (type.hasFlag(AUTOGENERATED)) {
			flags.add(Flag.IMMUTABLE);
		}

		if (type.hasFlag(OPTIONAL) ||
			(relation != null && relation.hasAnnotation(OPTIONAL))) {
			flags.add(Flag.OPTIONAL);
		}

		if (DataElement.class.isAssignableFrom(datatype)) {
			dataElement = (DataElement<?>) value;
		} else if (Entity.class.isAssignableFrom(datatype)) {
			dataElement =
				createEntityDataElement(name, value, relation, allowedValues,
					flags);
		} else if (JsonObject.class.isAssignableFrom(datatype)) {
			dataElement = createJsonDataElement(name, value, relation, flags);
		} else if (Collection.class.isAssignableFrom(datatype)) {
			dataElement =
				createCollectionDataElement(object, type, value, allowedValues,
					flags);
		} else {
			dataElement =
				createSimpleDataElement(datatype, name, value, allowedValues,
					relation, flags);
		}

		if (dataElement != null && relation != null) {
			HasProperties displayProperties = relation.get(DISPLAY_PROPERTIES);

			if (displayProperties != null) {
				dataElement.setProperties(displayProperties, true);
			}
		}

		return dataElement;
	}

	/**
	 * Creates an entity data element for a particular entity.
	 *
	 * @param name   The name of the data element
	 * @param entity The entity to create the data element from
	 * @param flags  The optional data element flags
	 * @return The new entity data element
	 * @throws StorageException If a storage-specific data element
	 *                          initialization fails
	 */
	@SuppressWarnings("unused")
	private EntityDataElement createEntityDataElement(String name,
		Entity entity, Set<Flag> flags) throws StorageException {
		@SuppressWarnings("unchecked")
		EntityDefinition<Entity> def =
			(EntityDefinition<Entity>) entity.getDefinition();

		Collection<RelationType<?>> attributes =
			getEntityDataElementAttributes(def);

		List<DataElement<?>> attrElements =
			getDataElements(entity, attributes, flags);

		List<DataElement<?>> childElements = new ArrayList<DataElement<?>>();

		for (EntityDefinition<?> childDef : def.getChildMappings()) {
			Class<? extends Entity> childType = childDef.getMappedType();

			String children = TextConvert.toPlural(childDef.getEntityName());

			RelationType<? extends Entity> parentAttribute =
				childDef.getParentAttribute();

			if (parentAttribute == null) {
				parentAttribute = childDef.getMasterAttribute();
			}

			Predicate<Entity> parent =
				ifAttribute(parentAttribute, equalTo(entity));

			List<Function<? super Entity, ?>> childAttributes =
				new ArrayList<Function<? super Entity, ?>>(
					childDef.getDisplayAttributes(DisplayMode.COMPACT));

			SelectionDataElement childElement =
				createEntitySelectionElement(children, entity, null, -1,
					forEntity(childType, parent), null, null, childAttributes);

			childElements.add(childElement);
		}

		if (childElements.size() > 0) {
			DataElementList childList =
				new DataElementList(EntityDataElement.CHILDREN_ELEMENT, null,
					childElements, null);

			if (childElements.size() > 1) {
				childList.setProperty(LAYOUT, LayoutType.TABS);
			}

			attrElements.add(childList);
		}

		if (name == null) {
			name = def.getEntityName();
		}

		String attr = entity.attributeString(DisplayMode.MINIMAL, ", ");
		String prefix = entity.getClass().getSimpleName();

		EntityDataElement element =
			new EntityDataElement(name, attr, prefix, attrElements, flags);

		return element;
	}

	/**
	 * Creates a data element for an entity data type.
	 *
	 * @param name          The element name
	 * @param value         The element value
	 * @param relation      The relation to create the element for
	 * @param allowedValues The allowed values or NULL for none
	 * @param flags         The element flags
	 * @return The new data element
	 * @throws StorageException If initializing an entity query fails
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createEntityDataElement(String name, Object value,
		Relation<?> relation, Collection<?> allowedValues, Set<Flag> flags)
		throws StorageException {
		DataElement<?> dataElement;

		if (relation.hasAnnotation(ENTITY_QUERY_PREDICATE) ||
			allowedValues != null) {
			dataElement = createEntitySelectionElement(name,
				(Relation<? extends Entity>) relation, allowedValues);

			relation.annotate(DATA_ELEMENT, dataElement);
		} else {
			String text = value != null ? ((Entity) value).getGlobalId() : "";

			dataElement =
				createSimpleDataElement(relation.getType().getTargetType(),
					name, text, null, relation, flags);
		}

		return dataElement;
	}

	/**
	 * Creates a list of {@link HierarchicalDataObject} instances which contain
	 * the given attributes from the argument entities. Invokes the method
	 * {@link #createEntityDataObject(Entity, int, Predicate, Predicate,
	 * Function, Collection, boolean)} for each entity in the list.
	 *
	 * @param entities     The list of entities to convert into data elements
	 * @param attributes   The access functions for the attributes to include
	 * @param hierarchical TRUE to add children of the same entity type
	 *                     recursively
	 * @return A new list of string list data elements
	 * @throws StorageException If creating a data object fails
	 */
	private List<HierarchicalDataObject> createEntityDataObjects(
		Collection<Entity> entities,
		List<Function<? super Entity, ?>> attributes, boolean hierarchical)
		throws StorageException {
		List<HierarchicalDataObject> entityObjects =
			new ArrayList<HierarchicalDataObject>(entities.size());
		Function<Entity, List<String>> collectValues =
			CollectionFunctions.createStringList(false, attributes);

		int index = 0;

		for (Entity entity : entities) {
			entityObjects.add(
				createEntityDataObject(entity, index++, null, null,
					collectValues, null, hierarchical));
		}

		return entityObjects;
	}

	/**
	 * Creates a new data element for the selection of an entity from a list of
	 * entities that is defined by either a storage query or a list of allowed
	 * values.
	 *
	 * @param name     The name of the element
	 * @param relation The relation to query for the selection meta data
	 * @return A new data element for the selection of an entity
	 * @throws StorageException If initializing the query fails
	 */
	@SuppressWarnings("unchecked")
	private SelectionDataElement createEntitySelectionElement(String name,
		Relation<? extends Entity> relation, Collection<?> allowedValues)
		throws StorageException {
		QueryPredicate<? extends Entity> query =
			relation.get(ENTITY_QUERY_PREDICATE);

		List<Function<? super Entity, ?>> attributes =
			relation.get(ENTITY_ATTRIBUTES);

		List<Entity> allowedEntities = (List<Entity>) allowedValues;
		EntityDefinition<?> def;

		if (query != null) {
			def = EntityManager.getEntityDefinition(query.getQueryType());
		} else {
			def = allowedEntities.get(0).getDefinition();
		}

		Entity entity = relation.getTarget();
		Number entityId = null;

		if (entity != null) {
			entityId = entity.get(def.getIdAttribute());
		}

		if (attributes == null) {
			DisplayMode mode = relation.get(ENTITY_DISPLAY_MODE);

			attributes = new ArrayList<Function<? super Entity, ?>>(
				def.getDisplayAttributes(mode));
		}

		int currentSelection = -1;

		if (relation.hasAnnotation(DISPLAY_PROPERTIES)) {
			currentSelection = relation
				.get(DISPLAY_PROPERTIES)
				.getIntProperty(CURRENT_SELECTION, -1);
		}

		if (query != null) {
			Predicate<? super Entity> sortOrder =
				relation.get(ENTITY_SORT_PREDICATE);

			return createEntitySelectionElement(name, relation, entityId,
				currentSelection, query, null, sortOrder, attributes);
		} else {
			return createEntitySelectionElement(name, relation, entityId,
				allowedEntities, attributes);
		}
	}

	/**
	 * Creates an integer data element.
	 *
	 * @param name     The name of the data element
	 * @param value    The enum value of the element
	 * @param metaData A relatable object containing optional meta data
	 *                 relations or NULL for none
	 * @param flags    The optional data element flags
	 * @return A new string data element for the given enum
	 * @throws IllegalArgumentException If the enum value is NULL
	 */
	@SuppressWarnings("boxing")
	private IntegerDataElement createIntegerDataElement(String name,
		Integer value, Relatable metaData, Set<Flag> flags) {
		Validator<? super Integer> validator = null;
		IntegerDataElement result;

		if (metaData != null && metaData.hasRelation(MINIMUM) &&
			metaData.hasRelation(MAXIMUM)) {
			validator = new IntegerRangeValidator(metaData.get(MINIMUM),
				metaData.get(MAXIMUM));
		}

		result = new IntegerDataElement(name, value, validator, flags);

		return result;
	}

	/**
	 * Creates a data element that references a list of JSON data from some
	 * endpoint.
	 *
	 * @param name     The element name
	 * @param value    The element value
	 * @param relation The relation to initialize
	 * @param flags    The element flags
	 */
	private SelectionDataElement createJsonDataElement(String name,
		Object value, Relation<?> relation, Set<Flag> flags) {
		URL dataUrl = relation.get(StandardTypes.URL);

		Objects.requireNonNull(dataUrl, "JSON data URL is required");

		new QueryValidator(name, null);

		return new SelectionDataElement(name,
			SelectionDataElement.NO_SELECTION,
			null, flags);
	}

	/**
	 * Creates a list data element for a collection value and a collection of
	 * allowed values. Such elements will typically be mapped to multiselection
	 * user interface components.
	 *
	 * @param name          The name of the data element
	 * @param values        The current values of the data element
	 * @param allowedValues The allowed values
	 * @param flags         The optional data element flags
	 * @return The new data element
	 */
	private DataElement<?> createListDataElement(String name,
		Collection<?> values, Collection<?> allowedValues, Set<Flag> flags) {
		List<String> stringValues = new ArrayList<String>(values.size());

		Validator<? super String> validator =
			createStringListValidator(allowedValues, false);

		for (Object value : values) {
			stringValues.add(value.toString());
		}

		return new StringListDataElement(name, stringValues, validator, flags);
	}

	/**
	 * Creates a data element for a simple (non-complex) datatype. If no
	 * specific data element exist for the given datatype a new instance of
	 * {@link StringDataElement} will be returned.
	 *
	 * @param datatype      The datatype to create the data element for
	 * @param name          The name of the data element
	 * @param value         The value of the data element
	 * @param allowedValues The allowed values or NULL for no constraint
	 * @param metaData      A relatable object containing optional meta data
	 *                      relations or NULL for none
	 * @param flags         The optional data element flags
	 * @return The new data element
	 */
	@SuppressWarnings("unchecked")
	private DataElement<?> createSimpleDataElement(Class<?> datatype,
		String name, Object value, Collection<?> allowedValues,
		Relatable metaData, Set<Flag> flags) {
		DataElement<?> result;

		if (datatype == Boolean.class) {
			result = new BooleanDataElement(name, (Boolean) value, flags);
		} else if (datatype == Integer.class) {
			result = createIntegerDataElement(name, (Integer) value, metaData,
				flags);
		} else if (datatype == BigDecimal.class) {
			result = new BigDecimalDataElement(name, (BigDecimal) value, null,
				flags);
		} else if (datatype == Date.class) {
			result = new DateDataElement(name, (Date) value,
				new DateValidator(null, null), flags);
		} else if (datatype == Period.class) {
			Period period = value != null ? (Period) value : Period.NONE;

			result = new PeriodDataElement(name, period.getCount(),
				period.getUnit().name(),
				getEnumValidator(Unit.class, allowedValues), flags);
		} else if (DataSet.class.isAssignableFrom(datatype)) {
			result = new DataSetDataElement(name, (DataSet<?>) value, flags);
		} else if (datatype.isEnum()) {
			result =
				createEnumDataElement(name,
					(Class<? extends Enum<?>>) datatype,
					value, allowedValues, flags);
		} else {
			result = createStringDataElement(name, value, allowedValues,
				flags);

			if (datatype == URL.class) {
				result.setProperty(CONTENT_TYPE, ContentType.WEBSITE);
			}
		}

		return result;
	}

	/**
	 * Checks a target object for a database storage adapter. If no storage
	 * adapter exist a a new storage adapter instance will be created and
	 * registered. The return value is the registered storage adapter ID.
	 *
	 * @param target          The target relatable to create the adapter for
	 * @param adapterId       The target's attribute to check for an existing
	 *                        storage adapter ID
	 * @param query           The storage query to perform
	 * @param getColumnData   The column data function
	 * @param defaultCriteria The default criteria or NULL for none
	 * @param sortCriteria    The sort criteria or NULL for none
	 * @param columns         The column definitions
	 * @return The storage adapter ID
	 * @throws StorageException If registering the storage adapter fails
	 */
	@SuppressWarnings("boxing")
	private <E extends Entity> StorageAdapterId getDatabaseStorageAdapter(
		Relatable target, RelationType<StorageAdapterId> adapterId,
		QueryPredicate<E> query, Function<Entity, List<String>> getColumnData,
		Predicate<? super E> defaultCriteria,
		Predicate<? super E> sortCriteria,
		List<ColumnDefinition> columns) throws StorageException {
		DatabaseStorageAdapter storageAdapter = null;
		StorageAdapterId storageAdapterId = target.get(adapterId);

		if (storageAdapterId != null) {
			storageAdapter =
				(DatabaseStorageAdapter) storageAdapterRegistry.getStorageAdapter(
					storageAdapterId);
		}

		if (storageAdapter == null) {
			storageAdapter = new DatabaseStorageAdapter(this);
			storageAdapterId =
				storageAdapterRegistry.registerStorageAdapter(storageAdapter);

			// keep ID to prevent the adapter from being garbage collected
			// because IDs may be stored only as weak keys in a weak hash map
			if (target instanceof Relation) {
				target.set(adapterId, storageAdapterId);
			} else {
				target.get(STORAGE_ADAPTER_IDS).add(storageAdapterId);
			}
		}

		storageAdapter.setQueryParameters(query, getColumnData,
			defaultCriteria,
			sortCriteria, columns);

		return storageAdapterId;
	}

	/**
	 * Returns the attributes of an entity that are to be transferred into a
	 * entity data element.
	 *
	 * @param definition The entity definition to get the attributes from
	 * @return The data element attributes
	 */
	private List<RelationType<?>> getEntityDataElementAttributes(
		EntityDefinition<?> definition) {
		List<RelationType<?>> attributes =
			new ArrayList<RelationType<?>>(definition.getAttributes());

		Iterator<RelationType<?>> attrIterator = attributes.iterator();

		// remove hierarchy parent attributes to prevent endless recursion
		while (attrIterator.hasNext()) {
			RelationType<?> attr = attrIterator.next();

			if (definition.isHierarchyAttribute(attr)) {
				attrIterator.remove();
			}
		}

		return attributes;
	}
}
