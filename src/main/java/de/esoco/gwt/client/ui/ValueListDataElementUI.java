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
package de.esoco.gwt.client.ui;

import de.esoco.data.element.DataElement;
import de.esoco.data.element.ListDataElement;
import de.esoco.data.element.SelectionDataElement;
import de.esoco.ewt.EWT;
import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.ComboBox;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.ListControl;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.layout.GenericLayout;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;
import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.lib.property.ContentType;
import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.ListStyle;
import de.esoco.lib.property.PropertyName;
import de.esoco.lib.property.Selectable;
import de.esoco.lib.property.StyleProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static de.esoco.data.element.DataElement.ALLOWED_VALUES_CHANGED;
import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;
import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.ContentProperties.NULL_VALUE;
import static de.esoco.lib.property.LayoutProperties.BUTTON_SIZE;
import static de.esoco.lib.property.LayoutProperties.COLUMNS;
import static de.esoco.lib.property.LayoutProperties.FLOAT;
import static de.esoco.lib.property.LayoutProperties.HORIZONTAL_ALIGN;
import static de.esoco.lib.property.LayoutProperties.ICON_ALIGN;
import static de.esoco.lib.property.LayoutProperties.ICON_SIZE;
import static de.esoco.lib.property.LayoutProperties.LAYOUT;
import static de.esoco.lib.property.LayoutProperties.LAYOUT_VISIBILITY;
import static de.esoco.lib.property.LayoutProperties.ROWS;
import static de.esoco.lib.property.LayoutProperties.VERTICAL_ALIGN;
import static de.esoco.lib.property.StateProperties.NO_EVENT_PROPAGATION;
import static de.esoco.lib.property.StyleProperties.BUTTON_STYLE;
import static de.esoco.lib.property.StyleProperties.CHECK_BOX_STYLE;
import static de.esoco.lib.property.StyleProperties.DISABLED_ELEMENTS;
import static de.esoco.lib.property.StyleProperties.ICON_COLOR;
import static de.esoco.lib.property.StyleProperties.LIST_STYLE;
import static de.esoco.lib.property.StyleProperties.SORT;

/**
 * A data element UI implementation for data elements that are constrained to a
 * list of allowed values.
 *
 * @author eso
 */
public class ValueListDataElementUI extends DataElementUI<DataElement<?>>
	implements EwtEventHandler {

	private static final Collection<PropertyName<?>> BUTTON_STYLE_PROPERTIES =
		Arrays.asList(BUTTON_STYLE, BUTTON_SIZE, CHECK_BOX_STYLE, ICON_SIZE,
			ICON_COLOR, HORIZONTAL_ALIGN, VERTICAL_ALIGN, ICON_ALIGN, FLOAT,
			NO_EVENT_PROPAGATION, LAYOUT_VISIBILITY);

	private List<Button> listButtons;

	/**
	 * Handles the action event for list buttons.
	 *
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent event) {
		setButtonSelection(getDataElement(), listButtons,
			(Button) event.getSource());
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData inputStyle, DataElement<?> dataElement) {
		return createListComponent(builder, inputStyle, dataElement);
	}

	@Override
	protected void transferInputToDataElement(Component component,
		DataElement<?> dataElement) {
		if (component instanceof ComboBox &&
			dataElement instanceof ListDataElement) {
			Set<String> values = ((ComboBox) component).getValues();

			@SuppressWarnings("unchecked")
			ListDataElement<String> listDataElement =
				(ListDataElement<String>) dataElement;

			// update validator to allow new values entered into the combo box
			listDataElement.addAllowedValues(values);
			listDataElement.clear();
			listDataElement.addAll(values);
		} else if (component instanceof ListControl) {
			setListSelection(component, dataElement);
		} else {
			super.transferInputToDataElement(component, dataElement);
		}
	}

	/**
	 * Updates the value of the element component if the data element value has
	 * changed.
	 */
	@Override
	protected void updateValue() {
		Component component = getElementComponent();
		DataElement<?> dataElement = getDataElement();
		List<String> values =
			getListValues(component.getContext(), dataElement);

		boolean allowedValuesChanged =
			dataElement.hasFlag(ALLOWED_VALUES_CHANGED);

		if (component instanceof ListControl) {
			updateList(values, allowedValuesChanged);
		} else if (component instanceof ComboBox) {
			updateComboBox((ComboBox) component, values, allowedValuesChanged);
		} else if (component instanceof Container) {
			updateButtons(values, allowedValuesChanged);
		}

		super.updateValue();
	}

	/**
	 * Applies the indices of currently selected values to a list of
	 * components.
	 * Only components that implement the {@link Selectable} interface will be
	 * considered, any other components will be ignored.
	 *
	 * @param selection  The indices of the currently selected values
	 * @param components A list of components
	 */
	private void applyCurrentSelection(int[] selection,
		List<? extends Component> components) {
		int componentIndex = 0;
		int selectionIndex = 0;

		for (Component component : components) {
			if (component instanceof Selectable ||
				component instanceof Button) {
				boolean selected = selectionIndex < selection.length &&
					componentIndex == selection[selectionIndex];

				if (component instanceof Selectable) {
					((Selectable) component).setSelected(selected);
				} else {
					if (selected) {
						component.addStyleName(CSS.gfActive());
					} else {
						component.removeStyleName(CSS.gfActive());
					}
				}

				if (selected) {
					++selectionIndex;
				}

				componentIndex++;
			}
		}
	}

	/**
	 * Creates the style data for buttons.
	 *
	 * @param dataElement The data element to determine the buttons style for
	 * @return The button style
	 */
	private StyleData createButtonStyle(DataElement<?> dataElement) {
		StyleData buttonStyle = StyleData.DEFAULT.withProperties(dataElement,
			BUTTON_STYLE_PROPERTIES);

		if (dataElement.getProperty(CONTENT_TYPE, null) ==
			ContentType.HYPERLINK) {
			buttonStyle = buttonStyle.setFlags(StyleFlag.HYPERLINK);
		}

		return buttonStyle;
	}

	/**
	 * Creates and initializes a {@link de.esoco.ewt.component.List} component.
	 *
	 * @param builder     The builder to add the list with
	 * @param style       The style for the list
	 * @param dataElement The data element to create the list for
	 * @return The list component
	 */
	private de.esoco.ewt.component.List createList(ContainerBuilder<?> builder,
		StyleData style, DataElement<?> dataElement) {
		de.esoco.ewt.component.List list = builder.addList(style);
		int rows = dataElement.getIntProperty(ROWS, -1);

		if (rows > 1) {
			list.setVisibleItems(rows);
		}

		return list;
	}

	/**
	 * Creates a panel that contains buttons for a list of labels.
	 *
	 * @param builder          The builder to create the button panel with
	 * @param style            The style for the button panel
	 * @param dataElement      The data element to create the panel for
	 * @param buttonLabels     The labels of the buttons to create
	 * @param listStyle        The list style
	 * @param currentSelection The indices of the currently selected Values
	 * @return The container containing the buttons
	 */
	private Component createListButtonPanel(ContainerBuilder<?> builder,
		StyleData style, DataElement<?> dataElement, List<String> buttonLabels,
		ListStyle listStyle, int[] currentSelection) {
		int columns = dataElement.getIntProperty(COLUMNS, 1);

		LayoutType layout =
			dataElement.getProperty(LAYOUT, getButtonPanelDefaultLayout());

		// inline inserts buttons directly into enclosing panels
		// TODO: return not the parent container from this method as this
		// causes problems in some configurations as button style updates
		// will then modify the container
		if (layout != LayoutType.INLINE) {
			String buttonPanelStyle =
				EsocoGwtResources.INSTANCE.css().gfButtonPanel();
			GenericLayout panelLayout;

			style = style.append(WEB_ADDITIONAL_STYLES, buttonPanelStyle);

			setBaseStyle(
				getBaseStyle().append(WEB_ADDITIONAL_STYLES,
					buttonPanelStyle));

			if (layout == LayoutType.TABLE) {
				panelLayout = new TableGridLayout(columns);
			} else {
				panelLayout = EWT
					.getLayoutFactory()
					.createLayout(builder.getContainer(), style, layout);
			}

			builder = builder.addPanel(style, panelLayout);
		}

		listButtons = createListButtons(builder, buttonLabels, listStyle);
		applyCurrentSelection(currentSelection, listButtons);

		return builder.getContainer();
	}

	/**
	 * Creates buttons for a list of labels.
	 *
	 * @param builder      The builder to create the buttons with
	 * @param buttonLabels The labels of the buttons to create
	 * @param listStyle    The list style of the buttons
	 * @return A list containing the buttons that have been created
	 */
	private List<Button> createListButtons(ContainerBuilder<?> builder,
		List<String> buttonLabels, ListStyle listStyle) {
		DataElement<?> dataElement = getDataElement();
		StyleData buttonStyle = createButtonStyle(dataElement);
		boolean multiselect = dataElement instanceof ListDataElement;
		String disabled = dataElement.getProperty(DISABLED_ELEMENTS, "");

		List<Button> buttons = new ArrayList<>(buttonLabels.size());

		int valueIndex = 0;

		for (String value : buttonLabels) {
			String text = value;
			Button button;

			if (listStyle == ListStyle.IMMEDIATE) {
				if (multiselect) {
					button = builder.addToggleButton(buttonStyle, text, null);
				} else {
					button = builder.addButton(buttonStyle, text, null);
				}
			} else {
				if (multiselect) {
					button = builder.addCheckBox(buttonStyle, text, null);
				} else {
					button = builder.addRadioButton(buttonStyle, text, null);
				}
			}

			if (disabled.contains("(" + valueIndex++ + ")")) {
				button.setEnabled(false);
			}

			button.addEventListener(EventType.ACTION, this);
			buttons.add(button);
		}

		return buttons;
	}

	/**
	 * Creates a {@link ComboBox} component for a list of values.
	 *
	 * @param builder     The builder to create the component with
	 * @param style       The default style for the component
	 * @param dataElement The data element to create the list for
	 * @param values      The list of values to display
	 * @return The new component
	 */
	private ComboBox createListComboBox(ContainerBuilder<?> builder,
		StyleData style, DataElement<?> dataElement, List<String> values) {
		ComboBox comboBox = builder.addComboBox(style, null);

		updateComboBox(comboBox, values, true);

		return comboBox;
	}

	/**
	 * Creates a component to select the data element's value from a list of
	 * values that are defined in a list validator. Depending on the data
	 * element type and the component style different types of components will
	 * be created.
	 *
	 * @param builder     The builder to add the list with
	 * @param style       The style data for the list
	 * @param dataElement The data element to create the component for
	 * @return A new list component
	 */
	private Component createListComponent(ContainerBuilder<?> builder,
		StyleData style, DataElement<?> dataElement) {
		UserInterfaceContext context = builder.getContext();
		List<String> values = getListValues(context, dataElement);

		int[] currentSelection =
			getCurrentSelection(context, dataElement, values);

		ListStyle listStyle = getListStyle(dataElement, values);

		Component component = null;

		if (dataElement instanceof ListDataElement) {
			style = style.setFlags(StyleFlag.MULTISELECT);

			setBaseStyle(getBaseStyle().setFlags(StyleFlag.MULTISELECT));
		}

		switch (listStyle) {
			case LIST:
			case DROP_DOWN:

				ListControl list = listStyle == ListStyle.LIST ?
				                   createList(builder, style, dataElement) :
				                   builder.addListBox(style);

				setListControlValues(list, values, currentSelection);
				component = list;
				break;

			case EDITABLE:
				component =
					createListComboBox(builder, style, dataElement, values);
				break;

			case DISCRETE:
			case IMMEDIATE:
				component =
					createListButtonPanel(builder, style, dataElement, values,
						listStyle, currentSelection);
				break;
		}

		return component;
	}

	/**
	 * Returns an array with the indices of the currently selected values. The
	 * returned array may be empty but will never be NULL. The index values
	 * will
	 * be sorted in ascending order.
	 *
	 * @param context     The user interface context
	 * @param dataElement The data element to read the current values from
	 * @param allValues   The list of all values to calculate the selection
	 *                    indexes from
	 * @return The indices of the currently selected values (may be empty but
	 * will never be NULL)
	 */
	private int[] getCurrentSelection(UserInterfaceContext context,
		DataElement<?> dataElement, List<String> allValues) {
		List<?> currentValues;
		boolean nullAllowed = false;
		int i = 0;

		if (dataElement instanceof ListDataElement) {
			currentValues = ((ListDataElement<?>) dataElement).getElements();
		} else {
			Object value = dataElement.getValue();

			if (value == null) {
				nullAllowed =
					dataElement.getProperty(NULL_VALUE, null) != null;
			}

			currentValues = value != null ?
			                Collections.singletonList(value) :
			                Collections.emptyList();
		}

		int[] currentValueIndexes = new int[currentValues.size()];

		for (Object value : currentValues) {
			if (value != null) {
				String text = convertValueToString(dataElement, value);
				int index = allValues.indexOf(context.expandResource(text));

				if (index >= 0) {
					currentValueIndexes[i++] = nullAllowed ? index + 1 : index;
				}
			} else if (nullAllowed) {
				currentValueIndexes[i++] = 0;
			}
		}

		Arrays.sort(currentValueIndexes);

		return currentValueIndexes;
	}

	/**
	 * Returns the list style for a certain data element. If no explicit style
	 * is set a default will be determined from the value list size.
	 *
	 * @param dataElement The data element
	 * @param values      The value list
	 * @return The list style
	 */
	private ListStyle getListStyle(DataElement<?> dataElement,
		List<String> values) {
		ListStyle listStyle =
			values.size() > 6 ? ListStyle.LIST : ListStyle.DROP_DOWN;

		listStyle = dataElement.getProperty(LIST_STYLE, listStyle);

		return listStyle;
	}

	/**
	 * Returns the display values for a list from a value list. If the list
	 * values are resource IDs they will be converted accordingly. If the data
	 * element has the flag {@link StyleProperties#SORT} set and the values are
	 * of the type {@link Comparable} the returned list will be sorted by their
	 * natural order.
	 *
	 * @param context The user interface context for resource expansion
	 * @param element The data element
	 * @return The resulting list of display values
	 */
	private List<String> getListValues(UserInterfaceContext context,
		DataElement<?> element) {
		Collection<?> rawValues = element.getAllowedValues();
		List<String> listValues = new ArrayList<String>();

		for (Object value : rawValues) {
			String text = convertValueToString(element, value);

			listValues.add(context.expandResource(text));
		}

		if (element.hasFlag(SORT) && listValues.size() > 1) {
			Collections.sort(listValues);
		}

		String nullValue = element.getProperty(NULL_VALUE, null);

		if (nullValue != null) {
			listValues.add(0, nullValue);
		}

		return listValues;
	}

	/**
	 * Sets the value of the data element from the selection of a button in a
	 * discrete style list component.
	 *
	 * @param dataElement The data element to read the current state from
	 * @param allButtons  The list of all buttons
	 * @param button      The selected button
	 */
	private void setButtonSelection(DataElement<?> dataElement,
		List<Button> allButtons, Button button) {
		boolean selected =
			button instanceof Selectable && ((Selectable) button).isSelected();

		List<?> values = dataElement.getAllowedValues();
		Object buttonValue = values.get(allButtons.indexOf(button));

		setDataElementValueFromList(dataElement, buttonValue, selected);
	}

	/**
	 * Sets a value from a list control to the data element of this instance.
	 *
	 * @param dataElement The data element to set the value of
	 * @param value       The value to set
	 * @param add         TRUE to add the value in a {@link ListDataElement},
	 *                    FALSE to remove
	 */
	@SuppressWarnings("unchecked")
	private void setDataElementValueFromList(DataElement<?> dataElement,
		Object value, boolean add) {
		if (dataElement instanceof ListDataElement) {
			ListDataElement<Object> listElement =
				(ListDataElement<Object>) getDataElement();

			if (add) {
				listElement.addElement(value);
			} else {
				listElement.removeElement(value);
			}
		} else {
			((DataElement<Object>) dataElement).setValue(value);
		}
	}

	/**
	 * Sets the values and the selection of a {@link ListControl} component.
	 *
	 * @param listControl      The component
	 * @param values           The values
	 * @param currentSelection The current selection
	 */
	private void setListControlValues(ListControl listControl,
		List<String> values, int[] currentSelection) {
		for (String value : values) {
			listControl.add(value);
		}

		if (currentSelection != null) {
			listControl.setSelection(currentSelection);
		}
	}

	/**
	 * Sets the selection for data elements that are based on a list validator.
	 *
	 * @param component   The component to read the input from
	 * @param dataElement The data element to set the value of
	 */
	private void setListSelection(Component component,
		DataElement<?> dataElement) {
		ListControl list = (ListControl) component;

		if (dataElement instanceof SelectionDataElement) {
			String selection = Integer.toString(list.getSelectionIndex());

			((SelectionDataElement) dataElement).setValue(selection);
		} else if (dataElement instanceof ListDataElement) {
			int[] selection = list.getSelectionIndices();

			((ListDataElement<?>) dataElement).clear();

			for (int index : selection) {
				setListSelection(dataElement, list.getItem(index), true);
			}
		} else {
			setListSelection(dataElement, list.getSelectedItem(), true);
		}
	}

	/**
	 * Sets the selection for data elements that are based on a list validator.
	 *
	 * @param dataElement The data element to set the value of
	 * @param selection   The string value of the selection to set
	 * @param add         TRUE to add a value to a list data element, FALSE to
	 *                    remove
	 */
	@SuppressWarnings("unchecked")
	private void setListSelection(DataElement<?> dataElement, String selection,
		boolean add) {
		UserInterfaceContext context = getElementComponent().getContext();

		List<?> values = dataElement.getAllowedValues();
		String nullValue = dataElement.getProperty(NULL_VALUE, null);

		if (nullValue != null) {
			nullValue = context.expandResource(nullValue);
		}

		if (selection == null || selection.equals(nullValue)) {
			if (!(dataElement instanceof ListDataElement)) {
				dataElement.setValue(null);
			}
		} else {
			for (Object value : values) {
				String text = context.expandResource(
					convertValueToString(dataElement, value));

				if (selection.equals(text)) {
					setDataElementValueFromList(dataElement, value, add);

					break;
				}
			}
		}
	}

	/**
	 * Updates the buttons in a list button panel.
	 *
	 * @param buttonLabels   The button labels
	 * @param buttonsChanged TRUE if button text has changed
	 */
	private void updateButtons(List<String> buttonLabels,
		boolean buttonsChanged) {
		Container container = (Container) getElementComponent();

		if (buttonsChanged) {
			if (listButtons.size() == buttonLabels.size()) {
				int index = 0;

				for (Button button : listButtons) {
					button.setProperties(buttonLabels.get(index++));
				}
			} else {
				ContainerBuilder<?> builder =
					new ContainerBuilder<>(container);

				DataElement<?> dataElement = getDataElement();

				ListStyle listStyle = getListStyle(dataElement, buttonLabels);

				for (Button button : listButtons) {
					container.removeComponent(button);
				}

				listButtons =
					createListButtons(builder, buttonLabels, listStyle);

				setupInteractionHandling(container, true);
			}
		}

		int[] currentSelection =
			getCurrentSelection(container.getContext(), getDataElement(),
				buttonLabels);

		applyCurrentSelection(currentSelection, listButtons);
	}

	/**
	 * Updates a combo box component with new values.
	 *
	 * @param comboBox       The combo box component
	 * @param values         The value list object
	 * @param choicesChanged TRUE if the combo box choices have changed
	 */
	private void updateComboBox(ComboBox comboBox, List<String> values,
		boolean choicesChanged) {
		DataElement<?> dataElement = getDataElement();

		if (choicesChanged) {
			comboBox.clearChoices();

			for (String value : values) {
				comboBox.addChoice(value);
			}
		}

		if (dataElement instanceof ListDataElement) {
			comboBox.clearValues();

			for (Object item : (ListDataElement<?>) dataElement) {
				comboBox.addValue(convertValueToString(dataElement, item));
			}
		} else {
			comboBox.setText(
				convertValueToString(dataElement, dataElement.getValue()));
		}
	}

	/**
	 * Updates a list control component with new values.
	 *
	 * @param values            The list values
	 * @param listValuesChanged TRUE if the list values have changed, not only
	 *                          the current selection
	 */
	private void updateList(List<String> values, boolean listValuesChanged) {
		ListControl listControl = (ListControl) getElementComponent();

		if (listValuesChanged) {
			listControl.removeAll();

			for (String value : values) {
				listControl.add(value);
			}
		}

		listControl.setSelection(
			getCurrentSelection(listControl.getContext(), getDataElement(),
				values));
	}
}
