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
		Arrays.<PropertyName<?>>asList(BUTTON_STYLE, BUTTON_SIZE,
			CHECK_BOX_STYLE, ICON_SIZE, ICON_COLOR, HORIZONTAL_ALIGN,
			VERTICAL_ALIGN, ICON_ALIGN, FLOAT, NO_EVENT_PROPAGATION,
			LAYOUT_VISIBILITY);

	private List<Button> aListButtons;

	/**
	 * Handles the action event for list buttons.
	 *
	 * @see EwtEventHandler#handleEvent(EwtEvent)
	 */
	@Override
	public void handleEvent(EwtEvent rEvent) {
		setButtonSelection(getDataElement(), aListButtons,
			(Button) rEvent.getSource());
	}

	@Override
	protected Component createInputUI(ContainerBuilder<?> rBuilder,
		StyleData rInputStyle, DataElement<?> rDataElement) {
		return createListComponent(rBuilder, rInputStyle, rDataElement);
	}

	@Override
	protected void transferInputToDataElement(Component rComponent,
		DataElement<?> rDataElement) {
		if (rComponent instanceof ComboBox &&
			rDataElement instanceof ListDataElement) {
			Set<String> rValues = ((ComboBox) rComponent).getValues();

			@SuppressWarnings("unchecked")
			ListDataElement<String> rListDataElement =
				(ListDataElement<String>) rDataElement;

			// update validator to allow new values entered into the combo box
			rListDataElement.addAllowedValues(rValues);
			rListDataElement.clear();
			rListDataElement.addAll(rValues);
		} else if (rComponent instanceof ListControl) {
			setListSelection(rComponent, rDataElement);
		} else {
			super.transferInputToDataElement(rComponent, rDataElement);
		}
	}

	/**
	 * Updates the value of the element component if the data element value has
	 * changed.
	 */
	@Override
	protected void updateValue() {
		Component rComponent = getElementComponent();
		DataElement<?> rDataElement = getDataElement();
		List<String> rValues =
			getListValues(rComponent.getContext(), rDataElement);

		boolean bAllowedValuesChanged =
			rDataElement.hasFlag(ALLOWED_VALUES_CHANGED);

		if (rComponent instanceof ListControl) {
			updateList(rValues, bAllowedValuesChanged);
		} else if (rComponent instanceof ComboBox) {
			updateComboBox((ComboBox) rComponent, rValues,
				bAllowedValuesChanged);
		} else if (rComponent instanceof Container) {
			updateButtons(rValues, bAllowedValuesChanged);
		}

		super.updateValue();
	}

	/**
	 * Applies the indices of currently selected values to a list of
	 * components.
	 * Only components that implement the {@link Selectable} interface will be
	 * considered, any other components will be ignored.
	 *
	 * @param rSelection  The indices of the currently selected values
	 * @param rComponents A list of components
	 */
	private void applyCurrentSelection(int[] rSelection,
		List<? extends Component> rComponents) {
		int nComponent = 0;
		int nSelectionIndex = 0;

		for (Component rComponent : rComponents) {
			if (rComponent instanceof Selectable ||
				rComponent instanceof Button) {
				boolean bSelected = nSelectionIndex < rSelection.length &&
					nComponent == rSelection[nSelectionIndex];

				if (rComponent instanceof Selectable) {
					((Selectable) rComponent).setSelected(bSelected);
				} else {
					if (bSelected) {
						rComponent.addStyleName(CSS.gfActive());
					} else {
						rComponent.removeStyleName(CSS.gfActive());
					}
				}

				if (bSelected) {
					++nSelectionIndex;
				}

				nComponent++;
			}
		}
	}

	/**
	 * Creates the style data for buttons.
	 *
	 * @param rDataElement The data element to determine the buttons style for
	 * @return The button style
	 */
	private StyleData createButtonStyle(DataElement<?> rDataElement) {
		StyleData rButtonStyle = StyleData.DEFAULT.withProperties(rDataElement,
			BUTTON_STYLE_PROPERTIES);

		if (rDataElement.getProperty(CONTENT_TYPE, null) ==
			ContentType.HYPERLINK) {
			rButtonStyle = rButtonStyle.setFlags(StyleFlag.HYPERLINK);
		}

		return rButtonStyle;
	}

	/**
	 * Creates and initializes a {@link de.esoco.ewt.component.List} component.
	 *
	 * @param rBuilder     The builder to add the list with
	 * @param rStyle       The style for the list
	 * @param rDataElement The data element to create the list for
	 * @return The list component
	 */
	private de.esoco.ewt.component.List createList(ContainerBuilder<?> rBuilder,
		StyleData rStyle, DataElement<?> rDataElement) {
		de.esoco.ewt.component.List aList = rBuilder.addList(rStyle);
		int nRows = rDataElement.getIntProperty(ROWS, -1);

		if (nRows > 1) {
			aList.setVisibleItems(nRows);
		}

		return aList;
	}

	/**
	 * Creates a panel that contains buttons for a list of labels.
	 *
	 * @param rBuilder          The builder to create the button panel with
	 * @param rStyle            The style for the button panel
	 * @param rDataElement      The data element to create the panel for
	 * @param rButtonLabels     The labels of the buttons to create
	 * @param eListStyle        The list style
	 * @param rCurrentSelection The indices of the currently selected Values
	 * @return The container containing the buttons
	 */
	private Component createListButtonPanel(ContainerBuilder<?> rBuilder,
		StyleData rStyle, DataElement<?> rDataElement,
		List<String> rButtonLabels, ListStyle eListStyle,
		int[] rCurrentSelection) {
		int nColumns = rDataElement.getIntProperty(COLUMNS, 1);

		LayoutType eLayout =
			rDataElement.getProperty(LAYOUT, getButtonPanelDefaultLayout());

		// inline inserts buttons directly into enclosing panels
		// TODO: return not the parent container from this method as this
		// causes problems in some configurations as button style updates
		// will then modify the container
		if (eLayout != LayoutType.INLINE) {
			String sButtonPanelStyle =
				EsocoGwtResources.INSTANCE.css().gfButtonPanel();
			GenericLayout aPanelLayout;

			rStyle = rStyle.append(WEB_ADDITIONAL_STYLES, sButtonPanelStyle);

			setBaseStyle(getBaseStyle().append(WEB_ADDITIONAL_STYLES,
				sButtonPanelStyle));

			if (eLayout == LayoutType.TABLE) {
				aPanelLayout = new TableGridLayout(nColumns);
			} else {
				aPanelLayout = EWT
					.getLayoutFactory()
					.createLayout(rBuilder.getContainer(), rStyle, eLayout);
			}

			rBuilder = rBuilder.addPanel(rStyle, aPanelLayout);
		}

		aListButtons = createListButtons(rBuilder, rButtonLabels, eListStyle);
		applyCurrentSelection(rCurrentSelection, aListButtons);

		return rBuilder.getContainer();
	}

	/**
	 * Creates buttons for a list of labels.
	 *
	 * @param rBuilder      The builder to create the buttons with
	 * @param rButtonLabels The labels of the buttons to create
	 * @param eListStyle    The list style of the buttons
	 * @return A list containing the buttons that have been created
	 */
	private List<Button> createListButtons(ContainerBuilder<?> rBuilder,
		List<String> rButtonLabels, ListStyle eListStyle) {
		DataElement<?> rDataElement = getDataElement();
		StyleData rButtonStyle = createButtonStyle(rDataElement);
		boolean bMultiselect = rDataElement instanceof ListDataElement;
		String sDisabled = rDataElement.getProperty(DISABLED_ELEMENTS, "");

		List<Button> aButtons = new ArrayList<>(rButtonLabels.size());

		int nValueIndex = 0;

		for (String sValue : rButtonLabels) {
			String sText = sValue;
			Button aButton;

			if (eListStyle == ListStyle.IMMEDIATE) {
				if (bMultiselect) {
					aButton =
						rBuilder.addToggleButton(rButtonStyle, sText, null);
				} else {
					aButton = rBuilder.addButton(rButtonStyle, sText, null);
				}
			} else {
				if (bMultiselect) {
					aButton = rBuilder.addCheckBox(rButtonStyle, sText, null);
				} else {
					aButton =
						rBuilder.addRadioButton(rButtonStyle, sText, null);
				}
			}

			if (sDisabled.contains("(" + nValueIndex++ + ")")) {
				aButton.setEnabled(false);
			}

			aButton.addEventListener(EventType.ACTION, this);
			aButtons.add(aButton);
		}

		return aButtons;
	}

	/**
	 * Creates a {@link ComboBox} component for a list of values.
	 *
	 * @param rBuilder     The builder to create the component with
	 * @param rStyle       The default style for the component
	 * @param rDataElement The data element to create the list for
	 * @param rValues      The list of values to display
	 * @return The new component
	 */
	private ComboBox createListComboBox(ContainerBuilder<?> rBuilder,
		StyleData rStyle, DataElement<?> rDataElement, List<String> rValues) {
		ComboBox aComboBox = rBuilder.addComboBox(rStyle, null);

		updateComboBox(aComboBox, rValues, true);

		return aComboBox;
	}

	/**
	 * Creates a component to select the data element's value from a list of
	 * values that are defined in a list validator. Depending on the data
	 * element type and the component style different types of components will
	 * be created.
	 *
	 * @param rBuilder     The builder to add the list with
	 * @param rStyle       The style data for the list
	 * @param rDataElement The data element to create the component for
	 * @return A new list component
	 */
	private Component createListComponent(ContainerBuilder<?> rBuilder,
		StyleData rStyle, DataElement<?> rDataElement) {
		UserInterfaceContext rContext = rBuilder.getContext();
		List<String> rValues = getListValues(rContext, rDataElement);

		int[] rCurrentSelection =
			getCurrentSelection(rContext, rDataElement, rValues);

		ListStyle eListStyle = getListStyle(rDataElement, rValues);

		Component aComponent = null;

		if (rDataElement instanceof ListDataElement) {
			rStyle = rStyle.setFlags(StyleFlag.MULTISELECT);

			setBaseStyle(getBaseStyle().setFlags(StyleFlag.MULTISELECT));
		}

		switch (eListStyle) {
			case LIST:
			case DROP_DOWN:

				ListControl aList = eListStyle == ListStyle.LIST ?
				                    createList(rBuilder, rStyle,
					                    rDataElement) :
				                    rBuilder.addListBox(rStyle);

				setListControlValues(aList, rValues, rCurrentSelection);
				aComponent = aList;
				break;

			case EDITABLE:
				aComponent =
					createListComboBox(rBuilder, rStyle, rDataElement,
						rValues);
				break;

			case DISCRETE:
			case IMMEDIATE:
				aComponent =
					createListButtonPanel(rBuilder, rStyle, rDataElement,
						rValues, eListStyle, rCurrentSelection);
				break;
		}

		return aComponent;
	}

	/**
	 * Returns an array with the indices of the currently selected values. The
	 * returned array may be empty but will never be NULL. The index values
	 * will
	 * be sorted in ascending order.
	 *
	 * @param rContext     The user interface context
	 * @param rDataElement The data element to read the current values from
	 * @param rAllValues   The list of all values to calculate the selection
	 *                     indexes from
	 * @return The indices of the currently selected values (may be empty but
	 * will never be NULL)
	 */
	private int[] getCurrentSelection(UserInterfaceContext rContext,
		DataElement<?> rDataElement, List<String> rAllValues) {
		List<?> rCurrentValues;
		boolean bNullAllowed = false;
		int i = 0;

		if (rDataElement instanceof ListDataElement) {
			rCurrentValues = ((ListDataElement<?>) rDataElement).getElements();
		} else {
			Object rValue = rDataElement.getValue();

			if (rValue == null) {
				bNullAllowed =
					rDataElement.getProperty(NULL_VALUE, null) != null;
			}

			rCurrentValues = rValue != null ?
			                 Arrays.asList(rValue) :
			                 Collections.emptyList();
		}

		int[] aCurrentValueIndexes = new int[rCurrentValues.size()];

		for (Object rValue : rCurrentValues) {
			if (rValue != null) {
				String sValue = convertValueToString(rDataElement, rValue);
				int nIndex =
					rAllValues.indexOf(rContext.expandResource(sValue));

				if (nIndex >= 0) {
					aCurrentValueIndexes[i++] =
						bNullAllowed ? nIndex + 1 : nIndex;
				}
			} else if (bNullAllowed) {
				aCurrentValueIndexes[i++] = 0;
			}
		}

		Arrays.sort(aCurrentValueIndexes);

		return aCurrentValueIndexes;
	}

	/**
	 * Returns the list style for a certain data element. If no explicit style
	 * is set a default will be determined from the value list size.
	 *
	 * @param rDataElement The data element
	 * @param rValues      The value list
	 * @return The list style
	 */
	private ListStyle getListStyle(DataElement<?> rDataElement,
		List<String> rValues) {
		ListStyle eListStyle =
			rValues.size() > 6 ? ListStyle.LIST : ListStyle.DROP_DOWN;

		eListStyle = rDataElement.getProperty(LIST_STYLE, eListStyle);

		return eListStyle;
	}

	/**
	 * Returns the display values for a list from a value list. If the list
	 * values are resource IDs they will be converted accordingly. If the data
	 * element has the flag {@link StyleProperties#SORT} set and the values are
	 * of the type {@link Comparable} the returned list will be sorted by their
	 * natural order.
	 *
	 * @param rContext The user interface context for resource expansion
	 * @param rElement The data element
	 * @return The resulting list of display values
	 */
	private List<String> getListValues(UserInterfaceContext rContext,
		DataElement<?> rElement) {
		Collection<?> rRawValues = rElement.getAllowedValues();
		List<String> aListValues = new ArrayList<String>();

		for (Object rValue : rRawValues) {
			String sValue = convertValueToString(rElement, rValue);

			aListValues.add(rContext.expandResource(sValue));
		}

		if (rElement.hasFlag(SORT) && aListValues.size() > 1) {
			Collections.sort(aListValues);
		}

		String sNullValue = rElement.getProperty(NULL_VALUE, null);

		if (sNullValue != null) {
			aListValues.add(0, sNullValue);
		}

		return aListValues;
	}

	/**
	 * Sets the value of the data element from the selection of a button in a
	 * discrete style list component.
	 *
	 * @param rDataElement The data element to read the current state from
	 * @param rAllButtons  The list of all buttons
	 * @param rButton      The selected button
	 */
	private void setButtonSelection(DataElement<?> rDataElement,
		List<Button> rAllButtons, Button rButton) {
		boolean bSelected = rButton instanceof Selectable ?
		                    ((Selectable) rButton).isSelected() :
		                    false;

		List<?> rValues = rDataElement.getAllowedValues();
		Object rButtonValue = rValues.get(rAllButtons.indexOf(rButton));

		setDataElementValueFromList(rDataElement, rButtonValue, bSelected);
	}

	/**
	 * Sets a value from a list control to the data element of this instance.
	 *
	 * @param rDataElement The data element to set the value of
	 * @param rValue       The value to set
	 * @param bAdd         TRUE to add the value in a {@link ListDataElement},
	 *                     FALSE to remove
	 */
	@SuppressWarnings("unchecked")
	private void setDataElementValueFromList(DataElement<?> rDataElement,
		Object rValue, boolean bAdd) {
		if (rDataElement instanceof ListDataElement) {
			ListDataElement<Object> rListElement =
				(ListDataElement<Object>) getDataElement();

			if (bAdd) {
				rListElement.addElement(rValue);
			} else {
				rListElement.removeElement(rValue);
			}
		} else {
			((DataElement<Object>) rDataElement).setValue(rValue);
		}
	}

	/**
	 * Sets the values and the selection of a {@link ListControl} component.
	 *
	 * @param rListControl      The component
	 * @param rValues           The values
	 * @param rCurrentSelection The current selection
	 */
	private void setListControlValues(ListControl rListControl,
		List<String> rValues, int[] rCurrentSelection) {
		for (String sValue : rValues) {
			rListControl.add(sValue);
		}

		if (rCurrentSelection != null) {
			rListControl.setSelection(rCurrentSelection);
		}
	}

	/**
	 * Sets the selection for data elements that are based on a list validator.
	 *
	 * @param rComponent   The component to read the input from
	 * @param rDataElement The data element to set the value of
	 */
	private void setListSelection(Component rComponent,
		DataElement<?> rDataElement) {
		ListControl rList = (ListControl) rComponent;

		if (rDataElement instanceof SelectionDataElement) {
			String sSelection = Integer.toString(rList.getSelectionIndex());

			((SelectionDataElement) rDataElement).setValue(sSelection);
		} else if (rDataElement instanceof ListDataElement) {
			int[] rSelection = rList.getSelectionIndices();

			((ListDataElement<?>) rDataElement).clear();

			for (int nIndex : rSelection) {
				setListSelection(rDataElement, rList.getItem(nIndex), true);
			}
		} else {
			setListSelection(rDataElement, rList.getSelectedItem(), true);
		}
	}

	/**
	 * Sets the selection for data elements that are based on a list validator.
	 *
	 * @param rDataElement The data element to set the value of
	 * @param sSelection   The string value of the selection to set
	 * @param bAdd         TRUE to add a value to a list data element, FALSE to
	 *                     remove
	 */
	@SuppressWarnings("unchecked")
	private void setListSelection(DataElement<?> rDataElement,
		String sSelection, boolean bAdd) {
		UserInterfaceContext rContext = getElementComponent().getContext();

		List<?> rValues = rDataElement.getAllowedValues();
		String sNullValue = rDataElement.getProperty(NULL_VALUE, null);

		if (sNullValue != null) {
			sNullValue = rContext.expandResource(sNullValue);
		}

		if (sSelection == null || sSelection.equals(sNullValue)) {
			if (!(rDataElement instanceof ListDataElement)) {
				((DataElement<Object>) rDataElement).setValue(null);
			}
		} else {
			for (Object rValue : rValues) {
				String sValue = rContext.expandResource(
					convertValueToString(rDataElement, rValue));

				if (sSelection.equals(sValue)) {
					setDataElementValueFromList(rDataElement, rValue, bAdd);

					break;
				}
			}
		}
	}

	/**
	 * Updates the buttons in a list button panel.
	 *
	 * @param rButtonLabels   The button labels
	 * @param bButtonsChanged TRUE if button text has changed
	 */
	private void updateButtons(List<String> rButtonLabels,
		boolean bButtonsChanged) {
		Container rContainer = (Container) getElementComponent();

		if (bButtonsChanged) {
			if (aListButtons.size() == rButtonLabels.size()) {
				int nIndex = 0;

				for (Button rButton : aListButtons) {
					rButton.setProperties(rButtonLabels.get(nIndex++));
				}
			} else {
				ContainerBuilder<?> aBuilder =
					new ContainerBuilder<>(rContainer);

				DataElement<?> rDataElement = getDataElement();

				ListStyle eListStyle =
					getListStyle(rDataElement, rButtonLabels);

				for (Button rButton : aListButtons) {
					rContainer.removeComponent(rButton);
				}

				aListButtons =
					createListButtons(aBuilder, rButtonLabels, eListStyle);

				setupInteractionHandling(rContainer, true);
			}
		}

		int[] rCurrentSelection =
			getCurrentSelection(rContainer.getContext(), getDataElement(),
				rButtonLabels);

		applyCurrentSelection(rCurrentSelection, aListButtons);
	}

	/**
	 * Updates a combo box component with new values.
	 *
	 * @param rComboBox       The combo box component
	 * @param rValues         The value list object
	 * @param bChoicesChanged TRUE if the combo box choices have changed
	 */
	private void updateComboBox(ComboBox rComboBox, List<String> rValues,
		boolean bChoicesChanged) {
		DataElement<?> rDataElement = getDataElement();

		if (bChoicesChanged) {
			rComboBox.clearChoices();

			for (String sValue : rValues) {
				rComboBox.addChoice(sValue);
			}
		}

		if (rDataElement instanceof ListDataElement) {
			rComboBox.clearValues();

			for (Object rItem : (ListDataElement<?>) rDataElement) {
				rComboBox.addValue(convertValueToString(rDataElement, rItem));
			}
		} else {
			rComboBox.setText(
				convertValueToString(rDataElement, rDataElement.getValue()));
		}
	}

	/**
	 * Updates a list control component with new values.
	 *
	 * @param rValues            The list values
	 * @param bListValuesChanged TRUE if the list values have changed, not only
	 *                           the current selection
	 */
	private void updateList(List<String> rValues, boolean bListValuesChanged) {
		ListControl rListControl = (ListControl) getElementComponent();

		if (bListValuesChanged) {
			rListControl.removeAll();

			for (String sValue : rValues) {
				rListControl.add(sValue);
			}
		}

		rListControl.setSelection(
			getCurrentSelection(rListControl.getContext(), getDataElement(),
				rValues));
	}
}
