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
import de.esoco.data.element.DataElement.CopyMode;
import de.esoco.data.element.ListDataElement;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.validate.ListValidator;
import de.esoco.data.validate.StringListValidator;
import de.esoco.data.validate.Validator;

import de.esoco.ewt.EWT;
import de.esoco.ewt.UserInterfaceContext;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.CheckBox;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Control;
import de.esoco.ewt.component.FileChooser;
import de.esoco.ewt.component.Label;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.component.SelectableButton;
import de.esoco.ewt.component.TextArea;
import de.esoco.ewt.component.TextControl;
import de.esoco.ewt.component.TextField;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.KeyCode;
import de.esoco.ewt.event.ModifierKeys;
import de.esoco.ewt.graphics.Image;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.property.ImageAttribute;
import de.esoco.ewt.style.StyleData;
import de.esoco.ewt.style.StyleFlag;

import de.esoco.gwt.client.res.EsocoGwtCss;
import de.esoco.gwt.client.res.EsocoGwtResources;

import de.esoco.lib.property.ButtonStyle;
import de.esoco.lib.property.ContentProperties;
import de.esoco.lib.property.ContentType;
import de.esoco.lib.property.LabelStyle;
import de.esoco.lib.property.LayoutType;
import de.esoco.lib.property.PropertyName;
import de.esoco.lib.property.StateProperties;
import de.esoco.lib.property.TextAttribute;
import de.esoco.lib.text.TextConvert;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;

import static de.esoco.data.element.DataElement.ALLOWED_VALUES_CHANGED;
import static de.esoco.data.element.DataElement.HIDDEN_URL;
import static de.esoco.data.element.DataElement.INTERACTION_URL;
import static de.esoco.data.element.DataElement.ITEM_RESOURCE_PREFIX;

import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;

import static de.esoco.lib.property.ContentProperties.CONTENT_TYPE;
import static de.esoco.lib.property.ContentProperties.FORMAT;
import static de.esoco.lib.property.ContentProperties.FORMAT_ARGUMENTS;
import static de.esoco.lib.property.ContentProperties.IMAGE;
import static de.esoco.lib.property.ContentProperties.INPUT_CONSTRAINT;
import static de.esoco.lib.property.ContentProperties.LABEL;
import static de.esoco.lib.property.ContentProperties.NO_RESOURCE_PREFIX;
import static de.esoco.lib.property.ContentProperties.PLACEHOLDER;
import static de.esoco.lib.property.ContentProperties.RESOURCE;
import static de.esoco.lib.property.ContentProperties.TOOLTIP;
import static de.esoco.lib.property.ContentProperties.URL;
import static de.esoco.lib.property.ContentProperties.VALUE_RESOURCE_PREFIX;
import static de.esoco.lib.property.LayoutProperties.COLUMNS;
import static de.esoco.lib.property.LayoutProperties.HEIGHT;
import static de.esoco.lib.property.LayoutProperties.ROWS;
import static de.esoco.lib.property.LayoutProperties.WIDTH;
import static de.esoco.lib.property.StateProperties.CARET_POSITION;
import static de.esoco.lib.property.StateProperties.DISABLED;
import static de.esoco.lib.property.StateProperties.FOCUSED;
import static de.esoco.lib.property.StateProperties.HIDDEN;
import static de.esoco.lib.property.StateProperties.INVISIBLE;
import static de.esoco.lib.property.StateProperties.NO_INTERACTION_LOCK;
import static de.esoco.lib.property.StateProperties.VALUE_CHANGED;
import static de.esoco.lib.property.StyleProperties.BUTTON_STYLE;
import static de.esoco.lib.property.StyleProperties.DISABLED_ELEMENTS;
import static de.esoco.lib.property.StyleProperties.EDITABLE;
import static de.esoco.lib.property.StyleProperties.HAS_IMAGES;
import static de.esoco.lib.property.StyleProperties.LABEL_STYLE;
import static de.esoco.lib.property.StyleProperties.NO_WRAP;
import static de.esoco.lib.property.StyleProperties.STYLE;
import static de.esoco.lib.property.StyleProperties.WRAP;

/**
 * An base class for the implementation of user interfaces for single data
 * element objects. All methods have standard implementations that are
 * sufficient for data elements with simple datatypes which implement the method
 * {@link DataElement#setStringValue(String)}.
 *
 * @author eso
 */
public class DataElementUI<D extends DataElement<?>> {

	/**
	 * The default prefix for label resource IDs.
	 */
	protected static final String LABEL_RESOURCE_PREFIX = "$lbl";

	/**
	 * The default gap between components.
	 */
	protected static final int DEFAULT_COMPONENT_GAP = 5;

	static final EsocoGwtCss CSS = EsocoGwtResources.INSTANCE.css();

	// these properties are mapped to StyleData fields
	private static final List<PropertyName<?>> MAPPED_PROPERTIES =
		Arrays.asList(STYLE, WIDTH, HEIGHT, WRAP, NO_WRAP, RESOURCE);

	private static final int[] PHONE_NUMBER_FIELD_SIZES =
		new int[] { 3, 5, 8, 4 };

	private static final String[] PHONE_NUMBER_FIELD_TOOLTIPS =
		new String[] { "$ttPhoneCountryCode", "$ttPhoneAreaCode",
			"$ttPhoneNumber", "$ttPhoneExtension" };

	private static LayoutType eButtonPanelDefaultLayout = LayoutType.FLOW;

	/**
	 * The default suffix for label strings.
	 */
	private static String sLabelSuffix = ":";

	private DataElementPanelManager rPanelManager;

	private D rDataElement;

	private StyleData rBaseStyle;

	private Label aElementLabel;

	private Component aElementComponent;

	private CheckBox rOptionalCheckBox = null;

	private String sToolTip = null;

	private String sHiddenLabelHint = null;

	private String sTextClipboard = null;

	private boolean bHasError = false;

	private boolean bInteractionEnabled = true;

	private boolean bUIEnabled = true;

	private DataElementInteractionHandler<D> aInteractionHandler;

	/**
	 * Creates a new instance.
	 */
	public DataElementUI() {
	}

	/**
	 * Applies the style properties of a data element to a style data object.
	 *
	 * @param rDataElement The data element
	 * @param rStyle       The style data to apply the element styles to
	 * @return A new style data object
	 */
	public static StyleData applyElementStyle(DataElement<?> rDataElement,
		StyleData rStyle) {
		String sStyle = rDataElement.getProperty(STYLE, null);

		if (sStyle != null) {
			rStyle = rStyle.append(WEB_ADDITIONAL_STYLES, sStyle);
		}

		if (rDataElement.hasProperty(WIDTH)) {
			rStyle = rStyle.w(rDataElement.getIntProperty(WIDTH, 0));
		}

		if (rDataElement.hasProperty(HEIGHT)) {
			rStyle = rStyle.h(rDataElement.getIntProperty(HEIGHT, 0));
		}

		if (rDataElement.hasFlag(WRAP)) {
			rStyle = rStyle.setFlags(StyleFlag.WRAP);
		}

		if (rDataElement.hasFlag(NO_WRAP)) {
			rStyle = rStyle.setFlags(StyleFlag.NO_WRAP);
		}

		if (rDataElement.hasFlag(RESOURCE)) {
			rStyle = rStyle.setFlags(StyleFlag.RESOURCE);
		}

		Collection<PropertyName<?>> aCopyProperties =
			new HashSet<>(rDataElement.getPropertyNames());

		aCopyProperties.removeAll(MAPPED_PROPERTIES);

		return rStyle.withProperties(rDataElement, aCopyProperties);
	}

	/**
	 * Returns the default layout mode used for button panels.
	 *
	 * @return The default layout mode
	 */
	public static LayoutType getButtonPanelDefaultLayout() {
		return eButtonPanelDefaultLayout;
	}

	/**
	 * Returns the style name for this a data element.
	 *
	 * @param rDataElement The data element
	 * @return The style name for this element (empty if no style should be
	 * used)
	 */
	public static String getElementStyleName(DataElement<?> rDataElement) {
		return rDataElement.getResourceId();
	}

	/**
	 * Returns the UI label text for a certain data element.
	 *
	 * @param rContext        The user interface context to expand resources
	 * @param rDataElement    The data element
	 * @param sResourcePrefix The prefix for resource IDs (should typically
	 *                        start with a '$' character)
	 * @return The expanded label text (can be empty but will never be null)
	 */
	static String getLabelText(UserInterfaceContext rContext,
		DataElement<?> rDataElement, String sResourcePrefix) {
		String sLabel = rDataElement.getProperty(LABEL, null);

		if (sLabel == null) {
			sLabel = getElementStyleName(rDataElement);

			if (sLabel == null) {
				sLabel = rDataElement.getResourceId();
			}

			if (sLabel.length() > 0) {
				sLabel = sResourcePrefix + sLabel;
			}
		}

		if (sLabel != null && sLabel.length() > 0) {
			sLabel = rContext.expandResource(sLabel);
		}

		return sLabel;
	}

	/**
	 * Returns the resource ID prefix for a value item of a certain data
	 * element.
	 *
	 * @param rDataElement The data element
	 * @return The prefix for a value item
	 */
	public static String getValueItemPrefix(DataElement<?> rDataElement) {
		String sItemPrefix = ITEM_RESOURCE_PREFIX;

		if (!rDataElement.hasFlag(NO_RESOURCE_PREFIX)) {
			String sValuePrefix =
				rDataElement.getProperty(VALUE_RESOURCE_PREFIX, null);

			sItemPrefix += sValuePrefix != null ?
			               sValuePrefix :
			               rDataElement.getResourceId();
		}

		return sItemPrefix;
	}

	/**
	 * Returns a value item resource string for a certain data element value .
	 * If the value has already been converted to a item resource it will be
	 * returned unchanged.
	 *
	 * @param rDataElement The data element
	 * @param sValue       The value to convert
	 * @return The value item string
	 */
	public static String getValueItemString(DataElement<?> rDataElement,
		String sValue) {
		if (sValue.length() > 0 && sValue.charAt(0) != '$') {
			sValue = getValueItemPrefix(rDataElement) +
				TextConvert.capitalizedIdentifier(sValue);
		}

		return sValue;
	}

	/**
	 * Sets the default layout mode to be used for button panels.
	 *
	 * @param eLayoutMode The new button panel default layout mode
	 */
	public static void setButtonPanelDefaultLayout(LayoutType eLayoutMode) {
		eButtonPanelDefaultLayout = eLayoutMode;
	}

	/**
	 * Configuration method to sets the suffix to be added to UI labels
	 * (default: ':').
	 *
	 * @param sSuffix The new label suffix (NULL or empty to disable)
	 */
	public static final void setLabelSuffix(String sSuffix) {
		sLabelSuffix = sSuffix;
	}

	/**
	 * Adds an event handler to the element component. If the element component
	 * is a container the event handler will be added to the container children
	 * (in the case of button panels). The source of events received from this
	 * registration will be the element component, not this instance.
	 *
	 * @param eEventType The event type to add the handler for
	 * @param rHandler   The event handler
	 */
	public void addEventListener(EventType eEventType,
		EwtEventHandler rHandler) {
		Component rComponent = getElementComponent();

		if (rComponent instanceof Container) {
			List<Component> rComponents =
				((Container) rComponent).getComponents();

			for (Component rChild : rComponents) {
				rChild.addEventListener(eEventType, rHandler);
			}
		} else {
			rComponent.addEventListener(eEventType, rHandler);
		}
	}

	/**
	 * Applies the current UI styles from the element style and other
	 * properties.
	 */
	public void applyStyle() {
		aElementComponent.applyStyle(
			applyElementStyle(rDataElement, getBaseStyle()));
		applyElementProperties();
		enableComponent(bUIEnabled);
	}

	/**
	 * Builds the user interface for the data element. This method Invokes
	 * {@link #buildDataElementUI(ContainerBuilder, StyleData)} which can be
	 * overridden by subclasses to modify the default building if necessary.
	 *
	 * @param rBuilder The container builder to create the components with
	 * @param rStyle   The style data for display components
	 */
	public final void buildUserInterface(ContainerBuilder<?> rBuilder,
		StyleData rStyle) {
		rBaseStyle = rStyle;
		aElementComponent = buildDataElementUI(rBuilder, rStyle);

		applyElementProperties();
		rDataElement.setModified(false);
	}

	/**
	 * Clears an error message if such has been set previously.
	 */
	public void clearError() {
		if (bHasError) {
			setErrorMessage(null);
		}
	}

	/**
	 * This method must be invoked externally to collect the values from an
	 * input user interface into the associated data element. Only after this
	 * will the data element contain any new values that have been input by the
	 * user. If this instance is not for input the call will be ignored.
	 * Therefore it won't harm to invoke this method on display user interfaces
	 * too.
	 *
	 * @param rModifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> rModifiedElements) {
		if (!rDataElement.isImmutable()) {
			if (rOptionalCheckBox != null) {
				rDataElement.setSelected(rOptionalCheckBox.isSelected());
			}

			transferInputToDataElement(aElementComponent, rDataElement);

			if (rDataElement.isModified()) {
				rModifiedElements.add(rDataElement.copy(CopyMode.FLAT,
					DataElement.SERVER_PROPERTIES));
				rDataElement.setModified(false);
			}
		}
	}

	/**
	 * Creates the label string for the data element of this instance.
	 *
	 * @param rContext The user interface context for resource expansion
	 * @return The Label string (may be emtpy but will never be null
	 */
	public String createElementLabelString(UserInterfaceContext rContext) {
		return appendLabelSuffix(getElementLabelText(rContext));
	}

	/**
	 * Returns the base style data object for this instance. This is the style
	 * before applying any styles from the data element properties.
	 *
	 * @return The base style data
	 */
	public StyleData getBaseStyle() {
		return rBaseStyle;
	}

	/**
	 * Returns the data element of this instance.
	 *
	 * @return The data element
	 */
	public final D getDataElement() {
		return rDataElement;
	}

	/**
	 * Returns the user interface component that is associated with the data
	 * element value.
	 *
	 * @return The user interface component for the data element
	 */
	public final Component getElementComponent() {
		return aElementComponent;
	}

	/**
	 * Returns the text for a label that describes the data element.
	 *
	 * @param rContext rBuilder
	 * @return The element label (can be empty but will never be null)
	 */
	public String getElementLabelText(UserInterfaceContext rContext) {
		return getLabelText(rContext, rDataElement, LABEL_RESOURCE_PREFIX);
	}

	/**
	 * Returns the style name for this UI's data element.
	 *
	 * @return The style name for this element (empty if no style should be
	 * used)
	 */
	public String getElementStyleName() {
		return getElementStyleName(rDataElement);
	}

	/**
	 * Returns the parent date element panel manager of this instance.
	 *
	 * @return The parent panel manager
	 */
	public final DataElementPanelManager getParent() {
		return rPanelManager;
	}

	/**
	 * Sets the input focus on the input component of this instance if
	 * possible.
	 * This method may be overridden by complex user interface implementations
	 * to set the input focus to a specific component.
	 *
	 * @return TRUE if the input focus could be set
	 */
	public boolean requestFocus() {
		boolean bIsControl = (aElementComponent instanceof Control);

		if (bIsControl) {
			((Control) aElementComponent).requestFocus();
		}

		return bIsControl;
	}

	/**
	 * Sets the component size as HTML string values.
	 *
	 * @param sWidth  The component width
	 * @param sHeight The component height
	 */
	public void setComponentSize(String sWidth, String sHeight) {
		aElementComponent.getWidget().setSize("100%", "100%");
	}

	/**
	 * Shows or hides an error message for an error of the data element value.
	 * The default implementation sets the message as the element component's
	 * tooltip (and on the label too if such exists).
	 *
	 * @param sMessage The error message or NULL to clear
	 */
	public void setErrorMessage(String sMessage) {
		bHasError = (sMessage != null);

		if (sMessage != null && !sMessage.startsWith("$")) {
			sMessage = "$msg" + sMessage;
		}

		aElementComponent.setError(sMessage);

		if (aElementLabel != null) {
			aElementLabel.setError(sMessage);
		}
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return TextConvert.format("%s[%s: %s]",
			TextConvert.lastElementOf(getClass().getName()),
			TextConvert.lastElementOf(rDataElement.getName()),
			getElementComponent().getClass().getSimpleName());
	}

	/**
	 * Updates the element component display from the data element value. Uses
	 * {@link #updateValue()} to display the new value.
	 */
	public void update() {
		if (aElementComponent != null) {
			if (rDataElement.hasFlag(VALUE_CHANGED)) {
				updateValue();
			}

			applyStyle();
			aElementComponent.repaint();
			checkRequestFocus();
		}
	}

	/**
	 * Package-internal method to update the data element of this instance.
	 *
	 * @param rNewElement The new data element
	 * @param bUpdateUI   TRUE to also update the UI, FALSE to only update data
	 *                    element references
	 */
	@SuppressWarnings("unchecked")
	public void updateDataElement(DataElement<?> rNewElement,
		boolean bUpdateUI) {
		rDataElement = (D) rNewElement;
		rPanelManager.getDataElementList().replaceElement(rNewElement);

		if (aInteractionHandler != null) {
			aInteractionHandler.updateDataElement(rDataElement);
		}

		if (bUpdateUI) {
			update();
		}
	}

	/**
	 * Creates a checkbox to select an optional component.
	 *
	 * @param rBuilder The builder to add the component with
	 */
	protected void addOptionSelector(ContainerBuilder<?> rBuilder) {
		rOptionalCheckBox = rBuilder.addCheckBox(StyleData.DEFAULT, "", null);

		rOptionalCheckBox.setSelected(false);
		rOptionalCheckBox.addEventListener(EventType.ACTION,
			new EwtEventHandler() {
				@Override
				public void handleEvent(EwtEvent rEvent) {
					setEnabled(rOptionalCheckBox.isSelected());
				}
			});
	}

	/**
	 * Applies certain element properties to the UI component(s) which are not
	 * reflected in the {@link StyleData} object for the component.
	 */
	protected void applyElementProperties() {
		boolean bVisible = !rDataElement.hasFlag(HIDDEN);

		if (aElementComponent != null) {
			sToolTip = rDataElement.getProperty(TOOLTIP, sHiddenLabelHint);

			aElementComponent.setVisible(bVisible);
			rPanelManager.setElementVisibility(this, bVisible);

			if (rDataElement.hasProperty(INVISIBLE)) {
				aElementComponent.setVisibility(
					!rDataElement.hasFlag(INVISIBLE));
			}

			if (!bHasError && sToolTip != null && sToolTip.length() > 0) {
				aElementComponent.setToolTip(sToolTip);
			}

			String sImage = rDataElement.getProperty(IMAGE, null);

			if (sImage != null && aElementComponent instanceof ImageAttribute) {
				((ImageAttribute) aElementComponent).setImage(
					aElementComponent.getContext().createImage(sImage));
			}
		}

		if (aElementLabel != null) {
			String sLabel = appendLabelSuffix(
				getElementLabelText(aElementLabel.getContext()));

			aElementLabel.setProperties(sLabel);
			aElementLabel.setVisible(bVisible);

			if (rDataElement.hasProperty(INVISIBLE)) {
				aElementComponent.setVisibility(
					!rDataElement.hasFlag(INVISIBLE));
			}
		}
	}

	/**
	 * Builds the UI for a data element. Depending on the immutable state of
	 * the
	 * data element it invokes either
	 * {@link #createInputUI(ContainerBuilder, StyleData, DataElement)} or
	 * {@link #createDisplayUI(ContainerBuilder, StyleData, DataElement)}.
	 *
	 * @param rBuilder The container builder to create the components with
	 * @param rStyle   The style data for display components
	 * @return The UI component
	 */
	protected Component buildDataElementUI(ContainerBuilder<?> rBuilder,
		StyleData rStyle) {
		rStyle = applyElementStyle(rDataElement, rStyle);

		if (rDataElement.isImmutable()) {
			aElementComponent = createDisplayUI(rBuilder, rStyle,
				rDataElement);
		} else {
			aElementComponent = createInputUI(rBuilder, rStyle, rDataElement);

			if (aElementComponent != null) {
				setupInteractionHandling(aElementComponent, true);
			}

			setEnabled(rOptionalCheckBox == null);
			checkRequestFocus();
		}

		return aElementComponent;
	}

	/**
	 * Checks whether a text string needs to be expanded with format arguments
	 * stored in the {@link ContentProperties#FORMAT_ARGUMENTS} property. If
	 * the
	 * property exists it will be tried to replace all occurrences of
	 * placeholders (%s) analog to {@link String#format(String, Object...)},
	 * but
	 * only string values are supported. Placeholders may be indexed (e.g.
	 * %1$s)
	 * in which case they can also occur multiple times. If not enough
	 * placeholders occur in the text string any surplus values will be
	 * ignored.
	 *
	 * @param rDataElement The data element to check for format arguments
	 * @param rContext     The user interface context for resource expansion
	 * @param sText        The text string to format
	 * @return The formatted string
	 */
	protected String checkApplyFormatting(D rDataElement,
		UserInterfaceContext rContext, String sText) {
		if (rDataElement.hasProperty(FORMAT_ARGUMENTS)) {
			List<String> rFormatArgs =
				rDataElement.getProperty(FORMAT_ARGUMENTS,
					Collections.emptyList());

			Object[] aArgs = new String[rFormatArgs.size()];

			for (int i = aArgs.length - 1; i >= 0; i--) {
				aArgs[i] = rContext.expandResource(rFormatArgs.get(i));
			}

			sText = TextConvert.format(rContext.expandResource(sText), aArgs);
		}

		return sText;
	}

	/**
	 * Checks whether the data element has the {@link StateProperties#FOCUSED}
	 * flag and if so sets the input focus on the element component.
	 */
	protected void checkRequestFocus() {
		if (rDataElement.hasFlag(FOCUSED)) {
			requestFocus();
			rDataElement.clearFlag(FOCUSED);
		}
	}

	/**
	 * A helper method that checks whether a certain validator contains a list
	 * of resource IDs.
	 *
	 * @param rValidator The validator to check
	 * @return TRUE if the validator contains resource IDs
	 */
	protected boolean containsResourceIds(Validator<?> rValidator) {
		return rValidator instanceof StringListValidator &&
			((StringListValidator) rValidator).isResourceIds();
	}

	/**
	 * Returns a string representation of a value that is related to a certain
	 * data element. Related means that it is either the data element value
	 * itself or of the element's validator. This method can be overridden by
	 * subclasses to modify the standard display of data elements. It will be
	 * invoked by the default implementations of the component creation
	 * methods.
	 *
	 * <p>The default implementation returns the toString() result for the
	 * value or an empty string if the value is NULL.</p>
	 *
	 * @param rDataElement The data element to convert the value for
	 * @param rValue       The value to convert
	 * @return The element display string
	 */
	protected String convertValueToString(DataElement<?> rDataElement,
		Object rValue) {
		String sValue = "";
		boolean bImageValue = false;

		if (rValue instanceof Date) {
			sValue = formatDate(rDataElement, (Date) rValue);
		} else if (rValue instanceof BigDecimal) {
			String sFormat = rDataElement.getProperty(FORMAT,
				BigDecimalDataElementUI.DEFAULT_FORMAT);

			sValue =
				NumberFormat.getFormat(sFormat).format((BigDecimal) rValue);
		} else if (rValue instanceof ListDataElement) {
			sValue = ((ListDataElement<?>) rValue).getElements().toString();
		} else if (rValue instanceof DataElement) {
			sValue = convertValueToString(rDataElement,
				((DataElement<?>) rValue).getValue());
		} else if (rValue instanceof Enum) {
			sValue = ((Enum<?>) rValue).name();
			sValue = getValueItemString(rDataElement, sValue);
			bImageValue = rDataElement.hasFlag(HAS_IMAGES);
		} else if (rValue != null) {
			sValue = rValue.toString();
			bImageValue = rDataElement.hasFlag(HAS_IMAGES);
		}

		if (containsResourceIds(rDataElement.getElementValidator())) {
			sValue = getValueItemString(rDataElement, sValue);
		}

		if (sValue.length() > 0 && bImageValue) {
			if (sValue.charAt(1) == Image.IMAGE_PREFIX_SEPARATOR &&
				sValue.charAt(0) == Image.IMAGE_DATA_PREFIX) {
				sValue = "#" + sValue;
			} else if (
				Component.COMPOUND_PROPERTY_CHARS.indexOf(sValue.charAt(0)) <
					0) {
				sValue = "%" + sValue;
			}
		}

		return sValue;
	}

	/**
	 * Creates a label component.
	 *
	 * @param rBuilder     The builder
	 * @param rStyle       The style
	 * @param rDataElement The data element to create the label for
	 * @return The new component
	 */
	protected Component createButton(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement) {
		ButtonStyle eButtonStyle =
			rDataElement.getProperty(BUTTON_STYLE, ButtonStyle.DEFAULT);

		return rBuilder.addButton(rStyle.set(BUTTON_STYLE, eButtonStyle),
			convertValueToString(rDataElement, rDataElement), null);
	}

	/**
	 * This method can be overridden by subclasses to create the user interface
	 * for a data element that is either immutable or for display only. The
	 * default implementation creates a label with the string-converted
	 * value of
	 * the data element as returned by
	 * {@link #convertValueToString(DataElement, Object)}.
	 *
	 * @param rBuilder     The container builder to create the components with
	 * @param rStyle       The default style data for the display components
	 * @param rDataElement The data element to create the UI for
	 * @return The display user interface component
	 */
	protected Component createDisplayUI(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement) {
		int nRows = rDataElement.getIntProperty(ROWS, 1);
		ContentType eContentType = rDataElement.getProperty(CONTENT_TYPE,
			null);
		Object rValue = rDataElement.getValue();
		Component aComponent;

		if (rDataElement instanceof ListDataElement<?>) {
			de.esoco.ewt.component.List aList = rBuilder.addList(rStyle);

			for (Object rItem : (ListDataElement<?>) rDataElement) {
				aList.add(convertValueToString(rDataElement, rItem));
			}

			aComponent = aList;
		} else if (eContentType == ContentType.WEBSITE) {
			aComponent = rBuilder.addWebsite(rStyle, rValue.toString());
		} else if (eContentType == ContentType.HYPERLINK) {
			aComponent =
				createHyperlinkDisplayComponent(rBuilder, rStyle,
					rDataElement);
		} else if (eContentType == ContentType.ABSOLUTE_URL ||
			eContentType == ContentType.RELATIVE_URL) {
			aComponent = createUrlComponent(rBuilder, rStyle, eContentType);
		} else if (nRows != 1) {
			aComponent = createTextArea(rBuilder, rStyle, rDataElement, nRows);
		} else {
			aComponent = createLabel(rBuilder, rStyle, rDataElement);
		}

		return aComponent;
	}

	/**
	 * Creates a label with the given container builder.
	 *
	 * @param rBuilder The container builder to add the label with
	 * @param rStyle   The default label style
	 * @param sLabel   The label string
	 */
	protected void createElementLabel(ContainerBuilder<?> rBuilder,
		StyleData rStyle, String sLabel) {
		aElementLabel = rBuilder.addLabel(rStyle, sLabel, null);
	}

	/**
	 * Creates a display component to render hyperlinks.
	 *
	 * @param rBuilder     The container builder
	 * @param rStyle       The style data
	 * @param rDataElement The data element
	 * @return The new hyperlink component
	 */
	protected Component createHyperlinkDisplayComponent(
		ContainerBuilder<?> rBuilder, StyleData rStyle, D rDataElement) {
		final String sURL = rDataElement.getValue().toString();
		final String sTitle =
			rBuilder.getContext().expandResource(rDataElement.getResourceId());

		rStyle = rStyle.setFlags(StyleFlag.HYPERLINK);

		Component aComponent = rBuilder.addLabel(rStyle, sURL, null);

		aComponent.addEventListener(EventType.ACTION, new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent rEvent) {
				Window.open(sURL, sTitle, "");
			}
		});

		return aComponent;
	}

	/**
	 * Must be overridden by subclasses to modify the creation of the user
	 * interface components for the editing of the data element's value. The
	 * default implementation creates either a {@link TextField} with the value
	 * returned by {@link #convertValueToString(DataElement, Object)} or a list
	 * if the data element's value is constrained by a {@link ListValidator}.
	 * The list values will be read from the validator, converted with the
	 * above
	 * method, and finally expanded as resources.}.
	 *
	 * @param rBuilder     The container builder to create the components with
	 * @param rStyle       The default style data for the input components
	 * @param rDataElement The data element to create the UI for
	 * @return The input user interface component or NULL if it shall not be
	 * handled by this instance
	 */
	protected Component createInputUI(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement) {
		String sValue = convertValueToString(rDataElement, rDataElement);

		ContentType eContentType = rDataElement.getProperty(CONTENT_TYPE,
			null);
		Component aComponent;

		if (eContentType == ContentType.PHONE_NUMBER) {
			aComponent =
				createPhoneNumberInputComponent(rBuilder, rStyle, sValue);
		} else if (eContentType == ContentType.FILE_UPLOAD) {
			aComponent = rBuilder.addFileChooser(rStyle,
				rDataElement.getProperty(URL, null),
				"$btn" + rDataElement.getResourceId());
		} else if (eContentType == ContentType.WEBSITE) {
			aComponent =
				rBuilder.addWebsite(rStyle,
					rDataElement.getValue().toString());
		} else if (eContentType == ContentType.HYPERLINK) {
			aComponent =
				rBuilder.addLabel(rStyle.setFlags(StyleFlag.HYPERLINK),
				rDataElement.getValue().toString(), null);
		} else if (rDataElement.getProperty(LABEL_STYLE, null) != null) {
			aComponent = createLabel(rBuilder, rStyle, rDataElement);
		} else if (rDataElement.getProperty(BUTTON_STYLE, null) != null) {
			aComponent = createButton(rBuilder, rStyle, rDataElement);
		} else {
			aComponent =
				createTextInputComponent(rBuilder, rStyle, rDataElement,
					sValue,
					eContentType);
		}

		return aComponent;
	}

	/**
	 * Creates the interaction handler for this instances. Subclasses may
	 * override this method to return their own type of interaction handler.
	 *
	 * @return The data element interaction handler
	 */
	protected DataElementInteractionHandler<D> createInteractionHandler(
		DataElementPanelManager rPanelManager, D rDataElement) {
		return new DataElementInteractionHandler<D>(rPanelManager,
			rDataElement);
	}

	/**
	 * Creates a label component.
	 *
	 * @param rBuilder     The builder
	 * @param rStyle       The style
	 * @param rDataElement The data element to create the label for
	 * @return The new component
	 */
	protected Component createLabel(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement) {
		LabelStyle eLabelStyle = rDataElement.getProperty(LABEL_STYLE, null);

		if (eLabelStyle != null) {
			rStyle = rStyle.set(LABEL_STYLE, eLabelStyle);
		}

		String sLabel =
			checkApplyFormatting(rDataElement, rBuilder.getContext(),
				convertValueToString(rDataElement, rDataElement));

		return rBuilder.addLabel(rStyle, sLabel, null);
	}

	/**
	 * Creates a component for the input of a phone number.
	 *
	 * @param rBuilder The builder to add the input component with
	 * @param rStyle   The style data for the component
	 * @param sValue   The initial value
	 * @return The new component
	 */
	protected Component createPhoneNumberInputComponent(
		ContainerBuilder<?> rBuilder, StyleData rStyle, String sValue) {
		final List<TextField> aNumberFields = new ArrayList<TextField>(4);
		String sPartSeparator = "+";

		EwtEventHandler rEventHandler = new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent rEvent) {
				handlePhoneNumberEvent(rEvent, aNumberFields);
			}
		};

		rBuilder = rBuilder.addPanel(
			rStyle.setFlags(StyleFlag.HORIZONTAL_ALIGN_CENTER),
			new FlowLayout(true));

		for (int i = 0; i < 4; i++) {
			if (i == 1) {
				sPartSeparator += " 0";
			}

			rBuilder.addLabel(StyleData.DEFAULT, sPartSeparator, null);

			TextField aField = rBuilder.addTextField(StyleData.DEFAULT, "");

			aField.addEventListener(EventType.KEY_PRESSED, rEventHandler);
			aField.addEventListener(EventType.KEY_TYPED, rEventHandler);
			aField.addEventListener(EventType.KEY_RELEASED, rEventHandler);
			aField.setColumns(PHONE_NUMBER_FIELD_SIZES[i]);
			aField.setToolTip(PHONE_NUMBER_FIELD_TOOLTIPS[i]);

			aNumberFields.add(aField);
			sPartSeparator = "-";
		}

		setPhoneNumber(aNumberFields, sValue);

		return rBuilder.getContainer();
	}

	/**
	 * Creates a text area component.
	 *
	 * @param rBuilder     The builder
	 * @param rStyle       The style
	 * @param rDataElement The data element to create the text area for
	 * @param nRows        The number of text rows to display
	 * @return The new component
	 */
	protected Component createTextArea(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement, int nRows) {
		Component aComponent;
		int nCols = rDataElement.getIntProperty(COLUMNS, -1);

		String sText = checkApplyFormatting(rDataElement,
			rBuilder.getContext(),
			convertValueToString(rDataElement, rDataElement));

		TextArea aTextArea = rBuilder.addTextArea(rStyle, sText);

		aComponent = aTextArea;

		aTextArea.setEditable(false);

		if (nRows > 0) {
			aTextArea.setRows(nRows);
		}

		if (nCols != -1) {
			aTextArea.setColumns(nCols);
		}

		return aComponent;
	}

	/**
	 * Creates a component for the input of a string value.
	 *
	 * @param rBuilder     The builder to add the input component with
	 * @param rStyle       The style data for the component
	 * @param rDataElement The data element to create the component for
	 * @param sText        The initial value
	 * @return The new component
	 */
	protected TextControl createTextInputComponent(ContainerBuilder<?> rBuilder,
		StyleData rStyle, D rDataElement, String sText,
		ContentType eContentType) {
		int nRows = rDataElement.getIntProperty(ROWS, 1);

		TextControl aTextComponent;

		sText =
			checkApplyFormatting(rDataElement, rBuilder.getContext(), sText);

		if (nRows > 1 || nRows == -1) {
			aTextComponent = rBuilder.addTextArea(rStyle, sText);

			if (nRows > 1) {
				((TextArea) aTextComponent).setRows(nRows);
			}
		} else {
			aTextComponent = rBuilder.addTextField(rStyle, sText);
		}

		updateTextComponent(aTextComponent);

		return aTextComponent;
	}

	/**
	 * Adds the component for the display of data elements with a URL content
	 * type.
	 *
	 * @param rBuilder      The container builder to create the components with
	 * @param rDisplayStyle The default style data for the display components
	 * @param eContentType  TRUE for a relative and FALSE for an absolute URL
	 * @return The URL component
	 */
	protected Component createUrlComponent(ContainerBuilder<?> rBuilder,
		StyleData rDisplayStyle, ContentType eContentType) {
		String sText = "$btn" + rDataElement.getResourceId();

		if (rDataElement.hasFlag(HAS_IMAGES)) {
			sText = "+" + sText;
		}

		Button aButton = rBuilder.addButton(rDisplayStyle, sText, null);

		aButton.addEventListener(EventType.ACTION, new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent rEvent) {
				openUrl(rDataElement.getValue().toString(),
					rDataElement.getProperty(CONTENT_TYPE, null) ==
						ContentType.RELATIVE_URL);
			}
		});

		return aButton;
	}

	/**
	 * Enables or disables the user interface component(s) of this instance. If
	 * the component is an instance of {@link Container} the enabled state of
	 * it's child elements will be changed instead.
	 *
	 * @param bEnable TRUE to enable the user interface
	 */
	protected void enableComponent(boolean bEnable) {
		enableComponent(aElementComponent,
			bEnable && !rDataElement.hasFlag(DISABLED));
	}

	/**
	 * Enables or disables a certain component. If the component is an instance
	 * of {@link Container} the enabled state of it's child elements will be
	 * changed instead.
	 *
	 * @param rComponent The component to enable or disable
	 * @param bEnabled   TRUE to enable the user interface
	 */
	protected void enableComponent(Component rComponent, boolean bEnabled) {
		if (rComponent instanceof Container) {
			String sElements =
				rDataElement.getProperty(DISABLED_ELEMENTS, null);
			int nIndex = 0;

			for (Component rChild : ((Container) rComponent).getComponents()) {
				if (sElements != null) {
					rChild.setEnabled(
						bEnabled && !sElements.contains("(" + nIndex++ + ")"));
				} else {
					rChild.setEnabled(bEnabled);
				}
			}
		} else if (rComponent != null) {
			rComponent.setEnabled(bEnabled);
		}
	}

	/**
	 * Enables or disables interactions through this panel manager's user
	 * interface. This default implementation stores the current enabled state
	 * and then disables or restores the enabled state of the element component
	 * through the method {@link #enableComponent(boolean)}.
	 *
	 * @param bEnabled TRUE to enable interaction, FALSE to disable
	 */
	protected void enableInteraction(boolean bEnabled) {
		bEnabled = bEnabled || rDataElement.hasFlag(NO_INTERACTION_LOCK);

		bInteractionEnabled = bEnabled;

		if (bEnabled) {
			bEnabled = bUIEnabled;
		}

		enableComponent(bEnabled);
	}

	/**
	 * Formats a date value from a data element. If the data element has the
	 * property {@link ContentProperties#FORMAT} this format string will be
	 * used
	 * to format the date value. Else the property
	 * {@link ContentProperties#CONTENT_TYPE} is queried for the date and/or
	 * time content type.
	 *
	 * @param rDataElement The data element the value has been read from
	 * @param rDate        The date value of the data element
	 * @return A string containing the formatted date value
	 */
	protected String formatDate(DataElement<?> rDataElement, Date rDate) {
		String sFormat = rDataElement.getProperty(FORMAT, null);
		DateTimeFormat rFormat;

		if (sFormat != null) {
			rFormat = DateTimeFormat.getFormat(sFormat);
		} else {
			ContentType eContentType =
				rDataElement.getProperty(CONTENT_TYPE, null);

			rFormat = DateTimeFormat.getFormat(
				eContentType == ContentType.DATE_TIME ?
				PredefinedFormat.DATE_TIME_MEDIUM :
				PredefinedFormat.DATE_MEDIUM);
		}

		return rFormat.format(rDate);
	}

	/**
	 * Returns the enabled state of this UIs component(s).
	 *
	 * @return The current enabled state
	 */
	protected boolean isEnabled() {
		boolean bEnabled = false;

		if (aElementComponent instanceof Container) {
			List<Component> rComponents =
				((Container) aElementComponent).getComponents();

			if (rComponents.size() > 0) {
				bEnabled = rComponents.get(0).isEnabled();
			}
		} else if (aElementComponent != null) {
			bEnabled = aElementComponent.isEnabled();
		}

		return bEnabled;
	}

	/**
	 * Opens a URL in a page or a hidden frame.
	 *
	 * @param sUrl    The URL to open
	 * @param bHidden TRUE to open the URL in a hidden frame, FALSE to open it
	 *                in a new browser page
	 */
	protected void openUrl(String sUrl, boolean bHidden) {
		if (bHidden) {
			EWT.openHiddenUrl(sUrl);
		} else {
			EWT.openUrl(sUrl, null, null);
		}
	}

	/**
	 * Sets the enabled state of this UI's component(s) and stores the enabled
	 * state internally.
	 *
	 * @param bEnabled The new enabled state
	 */
	protected void setEnabled(boolean bEnabled) {
		bUIEnabled = bEnabled;

		if (bInteractionEnabled) {
			enableComponent(bEnabled);
		}
	}

	/**
	 * Sets a hint for the component if the element label is not visible. The
	 * default implementation sets the label text as the component tooltip but
	 * subclasses can override this method.
	 *
	 * @param rContext The builder the element UI has been built with
	 */
	protected void setHiddenLabelHint(UserInterfaceContext rContext) {
		sHiddenLabelHint = getElementLabelText(rContext);

		if (sToolTip == null && sHiddenLabelHint != null &&
			sHiddenLabelHint.length() > 0) {
			aElementComponent.setToolTip(sHiddenLabelHint);
		}
	}

	/**
	 * Initializes the handling of interaction events for a certain
	 * component if
	 * necessary.
	 *
	 * @param rComponent           The component to setup the input handling
	 *                             for
	 * @param bOnContainerChildren TRUE to setup the input handling for the
	 *                             children if the component is a container
	 */
	protected void setupInteractionHandling(Component rComponent,
		boolean bOnContainerChildren) {
		DataElementInteractionHandler<D> aEventHandler =
			createInteractionHandler(rPanelManager, rDataElement);

		if (aEventHandler.setupEventHandling(rComponent,
			bOnContainerChildren)) {
			aInteractionHandler = aEventHandler;
		}
	}

	/**
	 * Transfers the value of a data element into a component. Must be
	 * overridden by subclasses that use special components to render data
	 * elements. This default implementation handles the standard components
	 * that are created by this base class.
	 *
	 * <p>This method will be invoked from the {@link #updateValue()} method if
	 * the UI needs to be updated from the model data (i.e. from the data
	 * element).</p>
	 *
	 * @param rDataElement The data element to transfer the value of
	 * @param rComponent   The component to set the value of
	 */
	protected void transferDataElementValueToComponent(D rDataElement,
		Component rComponent) {
		ContentType eContentType = rDataElement.getProperty(CONTENT_TYPE,
			null);

		if (rComponent instanceof TextAttribute) {
			if (eContentType != ContentType.ABSOLUTE_URL &&
				eContentType != ContentType.RELATIVE_URL &&
				eContentType != ContentType.FILE_UPLOAD &&
				!(rComponent instanceof SelectableButton ||
					rDataElement instanceof ListDataElement)) {
				String sValue =
					checkApplyFormatting(rDataElement, rComponent.getContext(),
						convertValueToString(rDataElement, rDataElement));

				rComponent.setProperties(sValue);
			}
		} else if (eContentType == ContentType.PHONE_NUMBER) {
			Object rValue = rDataElement.getValue();
			String sPhoneNumber = rValue != null ? rValue.toString() : null;

			List<Component> rComponents = ((Panel) rComponent).getComponents();
			List<TextField> aNumberFields = new ArrayList<TextField>(4);

			for (int i = 1; i < 8; i += 2) {
				aNumberFields.add((TextField) rComponents.get(i));
			}

			setPhoneNumber(aNumberFields, sPhoneNumber);
		}
	}

	/**
	 * Must be overridden by subclasses to modify the default transfer of input
	 * values from the user interface components to the data element. This
	 * method will only be invoked if this instance is in input mode and the
	 * data element is not immutable.
	 *
	 * <p>The default implementation expects either a text field or a list as
	 * created by
	 * {@link #createInputUI(ContainerBuilder, StyleData, DataElement)}. In the
	 * first case it invokes the {@link DataElement#setStringValue(String)}
	 * method with the field value. For a list the selected value will be read
	 * from the element's list validator.</p>
	 *
	 * @param rComponent   The component to read the input from
	 * @param rDataElement The data element to set the value of
	 */
	protected void transferInputToDataElement(Component rComponent,
		D rDataElement) {
		if (rComponent instanceof FileChooser) {
			rDataElement.setStringValue(
				((FileChooser) rComponent).getFilename());
		} else if (rComponent instanceof TextAttribute) {
			if (!rDataElement.isImmutable() &&
				!(rComponent instanceof Button)) {
				transferTextInput((TextAttribute) rComponent, rDataElement);
			}
		} else if (rComponent instanceof Panel) {
			if (rDataElement.getProperty(CONTENT_TYPE, null) ==
				ContentType.PHONE_NUMBER) {
				rDataElement.setStringValue(getPhoneNumber(rComponent));
			}
		} else {
			throw new UnsupportedOperationException(
				"Cannot transfer input to " + rDataElement);
		}
	}

	/**
	 * Transfers the input from a component with a text attribute to a data
	 * element.
	 *
	 * @param rComponent   The source text attribute component
	 * @param rDataElement The target data element
	 */
	protected void transferTextInput(TextAttribute rComponent,
		D rDataElement) {
		String sText = rComponent.getText();

		try {
			rDataElement.setStringValue(sText);
		} catch (Exception e) {
			// ignore parsing errors TODO: check if obsolete
		}

		if (rComponent instanceof TextControl &&
			rDataElement.hasProperty(CARET_POSITION)) {
			rDataElement.setProperty(CARET_POSITION,
				((TextControl) rComponent).getCaretPosition());
		}
	}

	/**
	 * Updates the state of a text component from the properties of the data
	 * element.
	 *
	 * @param rTextComponent The component to update
	 */
	protected void updateTextComponent(TextControl rTextComponent) {
		String sConstraint = rDataElement.getProperty(INPUT_CONSTRAINT, null);
		String sPlaceholder = rDataElement.getProperty(PLACEHOLDER, null);
		int nColumns = rDataElement.getIntProperty(COLUMNS, -1);
		int nCaretPos = rDataElement.getIntProperty(CARET_POSITION, -1);

		if (nColumns > 0) {
			rTextComponent.setColumns(nColumns);
		}

		if (nCaretPos >= 0) {
			rTextComponent.setCaretPosition(nCaretPos);
		}

		if (sConstraint != null) {
			rTextComponent.setInputConstraint(sConstraint);
		}

		if (sPlaceholder != null) {
			rTextComponent.setPlaceholder(sPlaceholder);
		}

		if (rDataElement.hasProperty(EDITABLE)) {
			rTextComponent.setEditable(rDataElement.hasFlag(EDITABLE));
		}
	}

	/**
	 * Updates the value of the element component if the data element value has
	 * changed.
	 */
	protected void updateValue() {
		if (aElementComponent instanceof TextControl) {
			updateTextComponent((TextControl) aElementComponent);
		}

		transferDataElementValueToComponent(rDataElement, aElementComponent);

		rDataElement.clearFlag(VALUE_CHANGED);
		rDataElement.clearFlag(ALLOWED_VALUES_CHANGED);

		String sInteractionUrl =
			rDataElement.getProperty(INTERACTION_URL, null);

		if (sInteractionUrl != null) {
			rDataElement.removeProperty(INTERACTION_URL);
			openUrl(sInteractionUrl, rDataElement.hasFlag(HIDDEN_URL));
		}

		// reset any modifications so that only changes from subsequent user
		// interactions are recorded as modifications
		rDataElement.setModified(false);
	}

	/**
	 * Will be invoked by the panel manager if a UI is removed.
	 */
	void dispose() {
	}

	/**
	 * Handles an event in an input f4ield of a composite phone number UI.
	 *
	 * @param rEvent        The event that occurred
	 * @param rNumberFields The list of the input fields for the phone number
	 *                      parts
	 */
	void handlePhoneNumberEvent(EwtEvent rEvent,
		List<TextField> rNumberFields) {
		TextField rField = (TextField) rEvent.getSource();
		String sText = rField.getText();
		String sSelectedText = rField.getSelectedText();
		KeyCode eKeyCode = rEvent.getKeyCode();
		ModifierKeys rModifiers = rEvent.getModifiers();

		boolean bNoSelection =
			(sSelectedText == null || sSelectedText.length() == 0);

		if (rEvent.getType() == EventType.KEY_PRESSED) {
			sTextClipboard = null;

			if (rModifiers == ModifierKeys.CTRL) {
				if (eKeyCode == KeyCode.C || eKeyCode == KeyCode.X) {
					if (bNoSelection) {
						// set field content to full text for cur or copy on
						// release
						String sPhoneNumber =
							getPhoneNumber(rField.getParent());

						sTextClipboard = sText;
						rField.setText(sPhoneNumber);
						rField.setSelection(0, sPhoneNumber.length());
					}
				}
			}
		} else if (rEvent.getType() == EventType.KEY_RELEASED) {
			if (rModifiers == ModifierKeys.CTRL) {
				switch (eKeyCode) {
					case X:
						if (sTextClipboard != null) {
							setPhoneNumber(rNumberFields, "");
						}

						break;

					case C:
						if (sTextClipboard != null) {
							// restore original text after copy
							rField.setText(sTextClipboard);
						}

						break;

					case V:
						setPhoneNumber(rNumberFields, sText);
						break;

					default:
						// ignore other
				}
			}
		} else if (rEvent.getType() == EventType.KEY_TYPED) {
			char nChar = rEvent.getKeyChar();

			if (nChar != 0) {
				if (Character.isDigit(nChar)) {
					if (bNoSelection && rNumberFields.indexOf(rField) == 0 &&
						sText.length() == 3) {
						rEvent.cancel();
					}
				} else if (rModifiers == ModifierKeys.NONE ||
					rModifiers == ModifierKeys.SHIFT) {
					rEvent.cancel();
				}
			}
		}
	}

	/**
	 * Initializes this instance for a certain parent and data element.
	 *
	 * @param rParent  The parent panel manager
	 * @param rElement The data element to be displayed
	 */
	void init(DataElementPanelManager rParent, D rElement) {
		this.rPanelManager = rParent;
		this.rDataElement = rElement;
	}

	/**
	 * Updates the base style of this instance. Can be used by subclasses to
	 * add
	 * implementation-specific styles.
	 *
	 * @param rBaseStyle The new base style
	 */
	final void setBaseStyle(StyleData rBaseStyle) {
		this.rBaseStyle = rBaseStyle;
	}

	/**
	 * Checks whether the {@link #sLabelSuffix} needs to be appended to the
	 * given label.
	 *
	 * @param sLabel The label text
	 * @return The label with the appended suffix if necessary
	 */
	private String appendLabelSuffix(String sLabel) {
		if (sLabel != null && sLabelSuffix != null && sLabel.length() > 0 &&
			"#%+".indexOf(sLabel.charAt(0)) == -1) {
			sLabel += sLabelSuffix;
		}

		return sLabel;
	}

	/**
	 * Collects the phone number string from a composite phone number field.
	 *
	 * @param rComponent The parent component of the phone number input fields
	 * @return The phone number string
	 */
	private String getPhoneNumber(Component rComponent) {
		StringBuilder aPhoneNumber = new StringBuilder();
		List<Component> rComponents = ((Panel) rComponent).getComponents();
		char cSeparator = '+';

		for (int i = 1; i < 8; i += 2) {
			TextField rNumberField = (TextField) rComponents.get(i);
			String sPart = rNumberField.getText();

			if (i < 7 || sPart.length() > 0) {
				aPhoneNumber.append(cSeparator);
			}

			aPhoneNumber.append(sPart);

			cSeparator = i == 1 ? '.' : '-';
		}

		String sPhoneNumber = aPhoneNumber.toString();

		if (sPhoneNumber.equals("+.-")) {
			sPhoneNumber = "";
		}

		return sPhoneNumber;
	}

	/**
	 * Parses a phone number value and sets it into a set of input fields for
	 * the distinct number parts.
	 *
	 * @param aNumberFields The input fields for the number parts
	 * @param sNumber       The phone number value
	 */
	private void setPhoneNumber(final List<TextField> aNumberFields,
		String sNumber) {
		List<String> rNumberParts =
			StringDataElement.getPhoneNumberParts(sNumber);

		for (int i = 0; i < 4; i++) {
			aNumberFields.get(i).setText(rNumberParts.get(i));
		}
	}
}
