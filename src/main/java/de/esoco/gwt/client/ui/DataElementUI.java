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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
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

	private static LayoutType buttonPanelDefaultLayout = LayoutType.FLOW;

	/**
	 * The default suffix for label strings.
	 */
	private static String labelSuffix = ":";

	private DataElementPanelManager panelManager;

	private D dataElement;

	private StyleData baseStyle;

	private Label elementLabel;

	private Component elementComponent;

	private CheckBox optionalCheckBox = null;

	private String toolTip = null;

	private String hiddenLabelHint = null;

	private String textClipboard = null;

	private boolean hasError = false;

	private boolean interactionEnabled = true;

	private boolean iEnabled = true;

	private DataElementInteractionHandler<D> interactionHandler;

	/**
	 * Creates a new instance.
	 */
	public DataElementUI() {
	}

	/**
	 * Applies the style properties of a data element to a style data object.
	 *
	 * @param dataElement The data element
	 * @param style       The style data to apply the element styles to
	 * @return A new style data object
	 */
	public static StyleData applyElementStyle(DataElement<?> dataElement,
		StyleData style) {
		String styleName = dataElement.getProperty(STYLE, null);

		if (styleName != null) {
			style = style.append(WEB_ADDITIONAL_STYLES, styleName);
		}

		if (dataElement.hasProperty(WIDTH)) {
			style = style.w(dataElement.getIntProperty(WIDTH, 0));
		}

		if (dataElement.hasProperty(HEIGHT)) {
			style = style.h(dataElement.getIntProperty(HEIGHT, 0));
		}

		if (dataElement.hasFlag(WRAP)) {
			style = style.setFlags(StyleFlag.WRAP);
		}

		if (dataElement.hasFlag(NO_WRAP)) {
			style = style.setFlags(StyleFlag.NO_WRAP);
		}

		if (dataElement.hasFlag(RESOURCE)) {
			style = style.setFlags(StyleFlag.RESOURCE);
		}

		Collection<PropertyName<?>> copyProperties =
			new HashSet<>(dataElement.getPropertyNames());

		copyProperties.removeAll(MAPPED_PROPERTIES);

		return style.withProperties(dataElement, copyProperties);
	}

	/**
	 * Returns the default layout mode used for button panels.
	 *
	 * @return The default layout mode
	 */
	public static LayoutType getButtonPanelDefaultLayout() {
		return buttonPanelDefaultLayout;
	}

	/**
	 * Returns the style name for this a data element.
	 *
	 * @param dataElement The data element
	 * @return The style name for this element (empty if no style should be
	 * used)
	 */
	public static String getElementStyleName(DataElement<?> dataElement) {
		return dataElement.getResourceId();
	}

	/**
	 * Returns the UI label text for a certain data element.
	 *
	 * @param context        The user interface context to expand resources
	 * @param dataElement    The data element
	 * @param resourcePrefix The prefix for resource IDs (should typically
	 *                          start
	 *                       with a '$' character)
	 * @return The expanded label text (can be empty but will never be null)
	 */
	static String getLabelText(UserInterfaceContext context,
		DataElement<?> dataElement, String resourcePrefix) {
		String label = dataElement.getProperty(LABEL, null);

		if (label == null) {
			label = getElementStyleName(dataElement);

			if (label == null) {
				label = dataElement.getResourceId();
			}

			if (label.length() > 0) {
				label = resourcePrefix + label;
			}
		}

		if (label != null && label.length() > 0) {
			label = context.expandResource(label);
		}

		return label;
	}

	/**
	 * Returns the resource ID prefix for a value item of a certain data
	 * element.
	 *
	 * @param dataElement The data element
	 * @return The prefix for a value item
	 */
	public static String getValueItemPrefix(DataElement<?> dataElement) {
		String itemPrefix = ITEM_RESOURCE_PREFIX;

		if (!dataElement.hasFlag(NO_RESOURCE_PREFIX)) {
			String valuePrefix =
				dataElement.getProperty(VALUE_RESOURCE_PREFIX, null);

			itemPrefix +=
				valuePrefix != null ? valuePrefix :
				dataElement.getResourceId();
		}

		return itemPrefix;
	}

	/**
	 * Returns a value item resource string for a certain data element value .
	 * If the value has already been converted to a item resource it will be
	 * returned unchanged.
	 *
	 * @param dataElement The data element
	 * @param value       The value to convert
	 * @return The value item string
	 */
	public static String getValueItemString(DataElement<?> dataElement,
		String value) {
		if (value.length() > 0 && value.charAt(0) != '$') {
			value = getValueItemPrefix(dataElement) +
				TextConvert.capitalizedIdentifier(value);
		}

		return value;
	}

	/**
	 * Sets the default layout mode to be used for button panels.
	 *
	 * @param layoutMode The new button panel default layout mode
	 */
	public static void setButtonPanelDefaultLayout(LayoutType layoutMode) {
		buttonPanelDefaultLayout = layoutMode;
	}

	/**
	 * Configuration method to sets the suffix to be added to UI labels
	 * (default: ':').
	 *
	 * @param suffix The new label suffix (NULL or empty to disable)
	 */
	public static final void setLabelSuffix(String suffix) {
		labelSuffix = suffix;
	}

	/**
	 * Adds an event handler to the element component. If the element component
	 * is a container the event handler will be added to the container children
	 * (in the case of button panels). The source of events received from this
	 * registration will be the element component, not this instance.
	 *
	 * @param eventType The event type to add the handler for
	 * @param handler   The event handler
	 */
	public void addEventListener(EventType eventType,
		EwtEventHandler handler) {
		Component component = getElementComponent();

		if (component instanceof Container) {
			List<Component> components =
				((Container) component).getComponents();

			for (Component child : components) {
				child.addEventListener(eventType, handler);
			}
		} else {
			component.addEventListener(eventType, handler);
		}
	}

	/**
	 * Applies the current UI styles from the element style and other
	 * properties.
	 */
	public void applyStyle() {
		elementComponent.applyStyle(
			applyElementStyle(dataElement, getBaseStyle()));
		applyElementProperties();
		enableComponent(iEnabled);
	}

	/**
	 * Builds the user interface for the data element. This method Invokes
	 * {@link #buildDataElementUI(ContainerBuilder, StyleData)} which can be
	 * overridden by subclasses to modify the default building if necessary.
	 *
	 * @param builder The container builder to create the components with
	 * @param style   The style data for display components
	 */
	public final void buildUserInterface(ContainerBuilder<?> builder,
		StyleData style) {
		baseStyle = style;
		elementComponent = buildDataElementUI(builder, style);

		applyElementProperties();
		dataElement.setModified(false);
	}

	/**
	 * Clears an error message if such has been set previously.
	 */
	public void clearError() {
		if (hasError) {
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
	 * @param modifiedElements A list to add modified data elements to
	 */
	public void collectInput(List<DataElement<?>> modifiedElements) {
		if (!dataElement.isImmutable()) {
			if (optionalCheckBox != null) {
				dataElement.setSelected(optionalCheckBox.isSelected());
			}

			transferInputToDataElement(elementComponent, dataElement);

			if (dataElement.isModified()) {
				modifiedElements.add(dataElement.copy(CopyMode.FLAT,
					DataElement.SERVER_PROPERTIES));
				dataElement.setModified(false);
			}
		}
	}

	/**
	 * Creates the label string for the data element of this instance.
	 *
	 * @param context The user interface context for resource expansion
	 * @return The Label string (may be emtpy but will never be null
	 */
	public String createElementLabelString(UserInterfaceContext context) {
		return appendLabelSuffix(getElementLabelText(context));
	}

	/**
	 * Returns the base style data object for this instance. This is the style
	 * before applying any styles from the data element properties.
	 *
	 * @return The base style data
	 */
	public StyleData getBaseStyle() {
		return baseStyle;
	}

	/**
	 * Returns the data element of this instance.
	 *
	 * @return The data element
	 */
	public final D getDataElement() {
		return dataElement;
	}

	/**
	 * Returns the user interface component that is associated with the data
	 * element value.
	 *
	 * @return The user interface component for the data element
	 */
	public final Component getElementComponent() {
		return elementComponent;
	}

	/**
	 * Returns the text for a label that describes the data element.
	 *
	 * @param context builder
	 * @return The element label (can be empty but will never be null)
	 */
	public String getElementLabelText(UserInterfaceContext context) {
		return getLabelText(context, dataElement, LABEL_RESOURCE_PREFIX);
	}

	/**
	 * Returns the style name for this UI's data element.
	 *
	 * @return The style name for this element (empty if no style should be
	 * used)
	 */
	public String getElementStyleName() {
		return getElementStyleName(dataElement);
	}

	/**
	 * Returns the parent date element panel manager of this instance.
	 *
	 * @return The parent panel manager
	 */
	public final DataElementPanelManager getParent() {
		return panelManager;
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
		boolean isControl = (elementComponent instanceof Control);

		if (isControl) {
			((Control) elementComponent).requestFocus();
		}

		return isControl;
	}

	/**
	 * Sets the component size as HTML string values.
	 *
	 * @param width  The component width
	 * @param height The component height
	 */
	public void setComponentSize(String width, String height) {
		elementComponent.getWidget().setSize("100%", "100%");
	}

	/**
	 * Shows or hides an error message for an error of the data element value.
	 * The default implementation sets the message as the element component's
	 * tooltip (and on the label too if such exists).
	 *
	 * @param message The error message or NULL to clear
	 */
	public void setErrorMessage(String message) {
		hasError = (message != null);

		if (message != null && !message.startsWith("$")) {
			message = "$msg" + message;
		}

		elementComponent.setError(message);

		if (elementLabel != null) {
			elementLabel.setError(message);
		}
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return TextConvert.format("%s[%s: %s]",
			TextConvert.lastElementOf(getClass().getName()),
			TextConvert.lastElementOf(dataElement.getName()),
			getElementComponent().getClass().getSimpleName());
	}

	/**
	 * Updates the element component display from the data element value. Uses
	 * {@link #updateValue()} to display the new value.
	 */
	public void update() {
		if (elementComponent != null) {
			if (dataElement.hasFlag(VALUE_CHANGED)) {
				updateValue();
			}

			applyStyle();
			elementComponent.repaint();
			checkRequestFocus();
		}
	}

	/**
	 * Package-internal method to update the data element of this instance.
	 *
	 * @param newElement The new data element
	 * @param updateUI   TRUE to also update the UI, FALSE to only update data
	 *                   element references
	 */
	@SuppressWarnings("unchecked")
	public void updateDataElement(DataElement<?> newElement,
		boolean updateUI) {
		dataElement = (D) newElement;
		panelManager.getDataElementList().replaceElement(newElement);

		if (interactionHandler != null) {
			interactionHandler.updateDataElement(dataElement);
		}

		if (updateUI) {
			update();
		}
	}

	/**
	 * Creates a checkbox to select an optional component.
	 *
	 * @param builder The builder to add the component with
	 */
	protected void addOptionSelector(ContainerBuilder<?> builder) {
		optionalCheckBox = builder.addCheckBox(StyleData.DEFAULT, "", null);

		optionalCheckBox.setSelected(false);
		optionalCheckBox.addEventListener(EventType.ACTION,
			new EwtEventHandler() {
				@Override
				public void handleEvent(EwtEvent event) {
					setEnabled(optionalCheckBox.isSelected());
				}
			});
	}

	/**
	 * Applies certain element properties to the UI component(s) which are not
	 * reflected in the {@link StyleData} object for the component.
	 */
	protected void applyElementProperties() {
		boolean visible = !dataElement.hasFlag(HIDDEN);

		if (elementComponent != null) {
			toolTip = dataElement.getProperty(TOOLTIP, hiddenLabelHint);

			elementComponent.setVisible(visible);
			panelManager.setElementVisibility(this, visible);

			if (dataElement.hasProperty(INVISIBLE)) {
				elementComponent.setVisibility(!dataElement.hasFlag(INVISIBLE));
			}

			if (!hasError && toolTip != null && toolTip.length() > 0) {
				elementComponent.setToolTip(toolTip);
			}

			String image = dataElement.getProperty(IMAGE, null);

			if (image != null && elementComponent instanceof ImageAttribute) {
				((ImageAttribute) elementComponent).setImage(
					elementComponent.getContext().createImage(image));
			}
		}

		if (elementLabel != null) {
			String label = appendLabelSuffix(
				getElementLabelText(elementLabel.getContext()));

			elementLabel.setProperties(label);
			elementLabel.setVisible(visible);

			if (dataElement.hasProperty(INVISIBLE)) {
				elementComponent.setVisibility(!dataElement.hasFlag(INVISIBLE));
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
	 * @param builder The container builder to create the components with
	 * @param style   The style data for display components
	 * @return The UI component
	 */
	protected Component buildDataElementUI(ContainerBuilder<?> builder,
		StyleData style) {
		style = applyElementStyle(dataElement, style);

		if (dataElement.isImmutable()) {
			elementComponent = createDisplayUI(builder, style, dataElement);
		} else {
			elementComponent = createInputUI(builder, style, dataElement);

			if (elementComponent != null) {
				setupInteractionHandling(elementComponent, true);
			}

			setEnabled(optionalCheckBox == null);
			checkRequestFocus();
		}

		return elementComponent;
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
	 * @param dataElement The data element to check for format arguments
	 * @param context     The user interface context for resource expansion
	 * @param text        The text string to format
	 * @return The formatted string
	 */
	protected String checkApplyFormatting(D dataElement,
		UserInterfaceContext context, String text) {
		if (dataElement.hasProperty(FORMAT_ARGUMENTS)) {
			List<String> formatArgs = dataElement.getProperty(FORMAT_ARGUMENTS,
				Collections.emptyList());

			Object[] args = new String[formatArgs.size()];

			for (int i = args.length - 1; i >= 0; i--) {
				args[i] = context.expandResource(formatArgs.get(i));
			}

			text = TextConvert.format(context.expandResource(text), args);
		}

		return text;
	}

	/**
	 * Checks whether the data element has the {@link StateProperties#FOCUSED}
	 * flag and if so sets the input focus on the element component.
	 */
	protected void checkRequestFocus() {
		if (dataElement.hasFlag(FOCUSED)) {
			requestFocus();
			dataElement.clearFlag(FOCUSED);
		}
	}

	/**
	 * A helper method that checks whether a certain validator contains a list
	 * of resource IDs.
	 *
	 * @param validator The validator to check
	 * @return TRUE if the validator contains resource IDs
	 */
	protected boolean containsResourceIds(Validator<?> validator) {
		return validator instanceof StringListValidator &&
			((StringListValidator) validator).isResourceIds();
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
	 * @param dataElement The data element to convert the value for
	 * @param value       The value to convert
	 * @return The element display string
	 */
	protected String convertValueToString(DataElement<?> dataElement,
		Object value) {
		String text = "";
		boolean imageValue = false;

		if (value instanceof Date) {
			text = formatDate(dataElement, (Date) value);
		} else if (value instanceof BigDecimal) {
			String format = dataElement.getProperty(FORMAT,
				BigDecimalDataElementUI.DEFAULT_FORMAT);

			text = NumberFormat.getFormat(format).format((BigDecimal) value);
		} else if (value instanceof ListDataElement) {
			text = ((ListDataElement<?>) value).getElements().toString();
		} else if (value instanceof DataElement) {
			text = convertValueToString(dataElement,
				((DataElement<?>) value).getValue());
		} else if (value instanceof Enum) {
			text = ((Enum<?>) value).name();
			text = getValueItemString(dataElement, text);
			imageValue = dataElement.hasFlag(HAS_IMAGES);
		} else if (value != null) {
			text = value.toString();
			imageValue = dataElement.hasFlag(HAS_IMAGES);
		}

		if (containsResourceIds(dataElement.getElementValidator())) {
			text = getValueItemString(dataElement, text);
		}

		if (!text.isEmpty() && imageValue) {
			if (text.charAt(1) == Image.IMAGE_PREFIX_SEPARATOR &&
				text.charAt(0) == Image.IMAGE_DATA_PREFIX) {
				text = "#" + text;
			} else if (
				Component.COMPOUND_PROPERTY_CHARS.indexOf(text.charAt(0)) < 0) {
				text = "%" + text;
			}
		}

		return text;
	}

	/**
	 * Creates a label component.
	 *
	 * @param builder     The builder
	 * @param style       The style
	 * @param dataElement The data element to create the label for
	 * @return The new component
	 */
	protected Component createButton(ContainerBuilder<?> builder,
		StyleData style, D dataElement) {
		ButtonStyle buttonStyle =
			dataElement.getProperty(BUTTON_STYLE, ButtonStyle.DEFAULT);

		return builder.addButton(style.set(BUTTON_STYLE, buttonStyle),
			convertValueToString(dataElement, dataElement), null);
	}

	/**
	 * This method can be overridden by subclasses to create the user interface
	 * for a data element that is either immutable or for display only. The
	 * default implementation creates a label with the string-converted
	 * value of
	 * the data element as returned by
	 * {@link #convertValueToString(DataElement, Object)}.
	 *
	 * @param builder     The container builder to create the components with
	 * @param style       The default style data for the display components
	 * @param dataElement The data element to create the UI for
	 * @return The display user interface component
	 */
	protected Component createDisplayUI(ContainerBuilder<?> builder,
		StyleData style, D dataElement) {
		int rows = dataElement.getIntProperty(ROWS, 1);
		ContentType contentType = dataElement.getProperty(CONTENT_TYPE, null);
		Object value = dataElement.getValue();
		Component component;

		if (dataElement instanceof ListDataElement<?>) {
			de.esoco.ewt.component.List list = builder.addList(style);

			for (Object item : (ListDataElement<?>) dataElement) {
				list.add(convertValueToString(dataElement, item));
			}

			component = list;
		} else if (contentType == ContentType.WEBSITE) {
			component = builder.addWebsite(style, value.toString());
		} else if (contentType == ContentType.HYPERLINK) {
			component =
				createHyperlinkDisplayComponent(builder, style, dataElement);
		} else if (contentType == ContentType.ABSOLUTE_URL ||
			contentType == ContentType.RELATIVE_URL) {
			component = createUrlComponent(builder, style, contentType);
		} else if (rows != 1) {
			component = createTextArea(builder, style, dataElement, rows);
		} else {
			component = createLabel(builder, style, dataElement);
		}

		return component;
	}

	/**
	 * Creates a label with the given container builder.
	 *
	 * @param builder The container builder to add the label with
	 * @param style   The default label style
	 * @param label   The label string
	 */
	protected void createElementLabel(ContainerBuilder<?> builder,
		StyleData style, String label) {
		elementLabel = builder.addLabel(style, label, null);
	}

	/**
	 * Creates a display component to render hyperlinks.
	 *
	 * @param builder     The container builder
	 * @param style       The style data
	 * @param dataElement The data element
	 * @return The new hyperlink component
	 */
	protected Component createHyperlinkDisplayComponent(
		ContainerBuilder<?> builder, StyleData style, D dataElement) {
		final String rL = dataElement.getValue().toString();
		final String title =
			builder.getContext().expandResource(dataElement.getResourceId());

		style = style.setFlags(StyleFlag.HYPERLINK);

		Component component = builder.addLabel(style, rL, null);

		component.addEventListener(EventType.ACTION, new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent event) {
				Window.open(rL, title, "");
			}
		});

		return component;
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
	 * @param builder     The container builder to create the components with
	 * @param style       The default style data for the input components
	 * @param dataElement The data element to create the UI for
	 * @return The input user interface component or NULL if it shall not be
	 * handled by this instance
	 */
	protected Component createInputUI(ContainerBuilder<?> builder,
		StyleData style, D dataElement) {
		String value = convertValueToString(dataElement, dataElement);

		ContentType contentType = dataElement.getProperty(CONTENT_TYPE, null);
		Component component;

		if (contentType == ContentType.PHONE_NUMBER) {
			component = createPhoneNumberInputComponent(builder, style, value);
		} else if (contentType == ContentType.FILE_UPLOAD) {
			component = builder.addFileChooser(style,
				dataElement.getProperty(URL, null),
				"$btn" + dataElement.getResourceId());
		} else if (contentType == ContentType.WEBSITE) {
			component =
				builder.addWebsite(style, dataElement.getValue().toString());
		} else if (contentType == ContentType.HYPERLINK) {
			component = builder.addLabel(style.setFlags(StyleFlag.HYPERLINK),
				dataElement.getValue().toString(), null);
		} else if (dataElement.getProperty(LABEL_STYLE, null) != null) {
			component = createLabel(builder, style, dataElement);
		} else if (dataElement.getProperty(BUTTON_STYLE, null) != null) {
			component = createButton(builder, style, dataElement);
		} else {
			component =
				createTextInputComponent(builder, style, dataElement, value,
					contentType);
		}

		return component;
	}

	/**
	 * Creates the interaction handler for this instances. Subclasses may
	 * override this method to return their own type of interaction handler.
	 *
	 * @return The data element interaction handler
	 */
	protected DataElementInteractionHandler<D> createInteractionHandler(
		DataElementPanelManager panelManager, D dataElement) {
		return new DataElementInteractionHandler<D>(panelManager, dataElement);
	}

	/**
	 * Creates a label component.
	 *
	 * @param builder     The builder
	 * @param style       The style
	 * @param dataElement The data element to create the label for
	 * @return The new component
	 */
	protected Component createLabel(ContainerBuilder<?> builder,
		StyleData style, D dataElement) {
		LabelStyle labelStyle = dataElement.getProperty(LABEL_STYLE, null);

		if (labelStyle != null) {
			style = style.set(LABEL_STYLE, labelStyle);
		}

		String label = checkApplyFormatting(dataElement, builder.getContext(),
			convertValueToString(dataElement, dataElement));

		return builder.addLabel(style, label, null);
	}

	/**
	 * Creates a component for the input of a phone number.
	 *
	 * @param builder The builder to add the input component with
	 * @param style   The style data for the component
	 * @param value   The initial value
	 * @return The new component
	 */
	protected Component createPhoneNumberInputComponent(
		ContainerBuilder<?> builder, StyleData style, String value) {
		final List<TextField> numberFields = new ArrayList<TextField>(4);
		String partSeparator = "+";

		EwtEventHandler eventHandler = new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent event) {
				handlePhoneNumberEvent(event, numberFields);
			}
		};

		builder =
			builder.addPanel(style.setFlags(StyleFlag.HORIZONTAL_ALIGN_CENTER),
				new FlowLayout(true));

		for (int i = 0; i < 4; i++) {
			if (i == 1) {
				partSeparator += " 0";
			}

			builder.addLabel(StyleData.DEFAULT, partSeparator, null);

			TextField field = builder.addTextField(StyleData.DEFAULT, "");

			field.addEventListener(EventType.KEY_PRESSED, eventHandler);
			field.addEventListener(EventType.KEY_TYPED, eventHandler);
			field.addEventListener(EventType.KEY_RELEASED, eventHandler);
			field.setColumns(PHONE_NUMBER_FIELD_SIZES[i]);
			field.setToolTip(PHONE_NUMBER_FIELD_TOOLTIPS[i]);

			numberFields.add(field);
			partSeparator = "-";
		}

		setPhoneNumber(numberFields, value);

		return builder.getContainer();
	}

	/**
	 * Creates a text area component.
	 *
	 * @param builder     The builder
	 * @param style       The style
	 * @param dataElement The data element to create the text area for
	 * @param rows        The number of text rows to display
	 * @return The new component
	 */
	protected Component createTextArea(ContainerBuilder<?> builder,
		StyleData style, D dataElement, int rows) {
		Component component;
		int cols = dataElement.getIntProperty(COLUMNS, -1);

		String text = checkApplyFormatting(dataElement, builder.getContext(),
			convertValueToString(dataElement, dataElement));

		TextArea textArea = builder.addTextArea(style, text);

		component = textArea;

		textArea.setEditable(false);

		if (rows > 0) {
			textArea.setRows(rows);
		}

		if (cols != -1) {
			textArea.setColumns(cols);
		}

		return component;
	}

	/**
	 * Creates a component for the input of a string value.
	 *
	 * @param builder     The builder to add the input component with
	 * @param style       The style data for the component
	 * @param dataElement The data element to create the component for
	 * @param text        The initial value
	 * @return The new component
	 */
	protected TextControl createTextInputComponent(ContainerBuilder<?> builder,
		StyleData style, D dataElement, String text, ContentType contentType) {
		int rows = dataElement.getIntProperty(ROWS, 1);

		TextControl textComponent;

		text = checkApplyFormatting(dataElement, builder.getContext(), text);

		if (rows > 1 || rows == -1) {
			textComponent = builder.addTextArea(style, text);

			if (rows > 1) {
				((TextArea) textComponent).setRows(rows);
			}
		} else {
			textComponent = builder.addTextField(style, text);
		}

		updateTextComponent(textComponent);

		return textComponent;
	}

	/**
	 * Adds the component for the display of data elements with a URL content
	 * type.
	 *
	 * @param builder      The container builder to create the components with
	 * @param displayStyle The default style data for the display components
	 * @param contentType  TRUE for a relative and FALSE for an absolute URL
	 * @return The URL component
	 */
	protected Component createUrlComponent(ContainerBuilder<?> builder,
		StyleData displayStyle, ContentType contentType) {
		String text = "$btn" + dataElement.getResourceId();

		if (dataElement.hasFlag(HAS_IMAGES)) {
			text = "+" + text;
		}

		Button button = builder.addButton(displayStyle, text, null);

		button.addEventListener(EventType.ACTION, new EwtEventHandler() {
			@Override
			public void handleEvent(EwtEvent event) {
				openUrl(dataElement.getValue().toString(),
					dataElement.getProperty(CONTENT_TYPE, null) ==
						ContentType.RELATIVE_URL);
			}
		});

		return button;
	}

	/**
	 * Enables or disables the user interface component(s) of this instance. If
	 * the component is an instance of {@link Container} the enabled state of
	 * it's child elements will be changed instead.
	 *
	 * @param enable TRUE to enable the user interface
	 */
	protected void enableComponent(boolean enable) {
		enableComponent(elementComponent,
			enable && !dataElement.hasFlag(DISABLED));
	}

	/**
	 * Enables or disables a certain component. If the component is an instance
	 * of {@link Container} the enabled state of it's child elements will be
	 * changed instead.
	 *
	 * @param component The component to enable or disable
	 * @param enabled   TRUE to enable the user interface
	 */
	protected void enableComponent(Component component, boolean enabled) {
		if (component instanceof Container) {
			String elements = dataElement.getProperty(DISABLED_ELEMENTS, null);
			int index = 0;

			for (Component child : ((Container) component).getComponents()) {
				if (elements != null) {
					child.setEnabled(
						enabled && !elements.contains("(" + index++ + ")"));
				} else {
					child.setEnabled(enabled);
				}
			}
		} else if (component != null) {
			component.setEnabled(enabled);
		}
	}

	/**
	 * Enables or disables interactions through this panel manager's user
	 * interface. This default implementation stores the current enabled state
	 * and then disables or restores the enabled state of the element component
	 * through the method {@link #enableComponent(boolean)}.
	 *
	 * @param enabled TRUE to enable interaction, FALSE to disable
	 */
	protected void enableInteraction(boolean enabled) {
		enabled = enabled || dataElement.hasFlag(NO_INTERACTION_LOCK);

		interactionEnabled = enabled;

		if (enabled) {
			enabled = iEnabled;
		}

		enableComponent(enabled);
	}

	/**
	 * Formats a date value from a data element. If the data element has the
	 * property {@link ContentProperties#FORMAT} this format string will be
	 * used
	 * to format the date value. Else the property
	 * {@link ContentProperties#CONTENT_TYPE} is queried for the date and/or
	 * time content type.
	 *
	 * @param dataElement The data element the value has been read from
	 * @param date        The date value of the data element
	 * @return A string containing the formatted date value
	 */
	protected String formatDate(DataElement<?> dataElement, Date date) {
		String formatString = dataElement.getProperty(FORMAT, null);
		DateTimeFormat dateFormat;

		if (formatString != null) {
			dateFormat = DateTimeFormat.getFormat(formatString);
		} else {
			ContentType contentType =
				dataElement.getProperty(CONTENT_TYPE, null);

			dateFormat = DateTimeFormat.getFormat(
				contentType == ContentType.DATE_TIME ?
				PredefinedFormat.DATE_TIME_MEDIUM :
				PredefinedFormat.DATE_MEDIUM);
		}

		return dateFormat.format(date);
	}

	/**
	 * Returns the enabled state of this UIs component(s).
	 *
	 * @return The current enabled state
	 */
	protected boolean isEnabled() {
		boolean enabled = false;

		if (elementComponent instanceof Container) {
			List<Component> components =
				((Container) elementComponent).getComponents();

			if (components.size() > 0) {
				enabled = components.get(0).isEnabled();
			}
		} else if (elementComponent != null) {
			enabled = elementComponent.isEnabled();
		}

		return enabled;
	}

	/**
	 * Opens a URL in a page or a hidden frame.
	 *
	 * @param url    The URL to open
	 * @param hidden TRUE to open the URL in a hidden frame, FALSE to open
	 *                  it in
	 *               a new browser page
	 */
	protected void openUrl(String url, boolean hidden) {
		if (hidden) {
			EWT.openHiddenUrl(url);
		} else {
			EWT.openUrl(url, null, null);
		}
	}

	/**
	 * Sets the enabled state of this UI's component(s) and stores the enabled
	 * state internally.
	 *
	 * @param enabled The new enabled state
	 */
	protected void setEnabled(boolean enabled) {
		iEnabled = enabled;

		if (interactionEnabled) {
			enableComponent(enabled);
		}
	}

	/**
	 * Sets a hint for the component if the element label is not visible. The
	 * default implementation sets the label text as the component tooltip but
	 * subclasses can override this method.
	 *
	 * @param context The builder the element UI has been built with
	 */
	protected void setHiddenLabelHint(UserInterfaceContext context) {
		hiddenLabelHint = getElementLabelText(context);

		if (toolTip == null && hiddenLabelHint != null &&
			hiddenLabelHint.length() > 0) {
			elementComponent.setToolTip(hiddenLabelHint);
		}
	}

	/**
	 * Initializes the handling of interaction events for a certain
	 * component if
	 * necessary.
	 *
	 * @param component           The component to setup the input handling for
	 * @param onContainerChildren TRUE to setup the input handling for the
	 *                            children if the component is a container
	 */
	protected void setupInteractionHandling(Component component,
		boolean onContainerChildren) {
		DataElementInteractionHandler<D> eventHandler =
			createInteractionHandler(panelManager, dataElement);

		if (eventHandler.setupEventHandling(component, onContainerChildren)) {
			interactionHandler = eventHandler;
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
	 * @param dataElement The data element to transfer the value of
	 * @param component   The component to set the value of
	 */
	protected void transferDataElementValueToComponent(D dataElement,
		Component component) {
		ContentType contentType = dataElement.getProperty(CONTENT_TYPE, null);

		if (component instanceof TextAttribute) {
			if (contentType != ContentType.ABSOLUTE_URL &&
				contentType != ContentType.RELATIVE_URL &&
				contentType != ContentType.FILE_UPLOAD &&
				!(component instanceof SelectableButton ||
					dataElement instanceof ListDataElement)) {
				String value =
					checkApplyFormatting(dataElement, component.getContext(),
						convertValueToString(dataElement, dataElement));

				component.setProperties(value);
			}
		} else if (contentType == ContentType.PHONE_NUMBER) {
			Object value = dataElement.getValue();
			String phoneNumber = value != null ? value.toString() : null;

			List<Component> components = ((Panel) component).getComponents();
			List<TextField> numberFields = new ArrayList<TextField>(4);

			for (int i = 1; i < 8; i += 2) {
				numberFields.add((TextField) components.get(i));
			}

			setPhoneNumber(numberFields, phoneNumber);
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
	 * @param component   The component to read the input from
	 * @param dataElement The data element to set the value of
	 */
	protected void transferInputToDataElement(Component component,
		D dataElement) {
		if (component instanceof FileChooser) {
			dataElement.setStringValue(((FileChooser) component).getFilename());
		} else if (component instanceof TextAttribute) {
			if (!dataElement.isImmutable() && !(component instanceof Button)) {
				transferTextInput((TextAttribute) component, dataElement);
			}
		} else if (component instanceof Panel) {
			if (dataElement.getProperty(CONTENT_TYPE, null) ==
				ContentType.PHONE_NUMBER) {
				dataElement.setStringValue(getPhoneNumber(component));
			}
		} else {
			throw new UnsupportedOperationException(
				"Cannot transfer input to " + dataElement);
		}
	}

	/**
	 * Transfers the input from a component with a text attribute to a data
	 * element.
	 *
	 * @param component   The source text attribute component
	 * @param dataElement The target data element
	 */
	protected void transferTextInput(TextAttribute component, D dataElement) {
		String text = component.getText();

		try {
			dataElement.setStringValue(text);
		} catch (Exception e) {
			// ignore parsing errors TODO: check if obsolete
		}

		if (component instanceof TextControl &&
			dataElement.hasProperty(CARET_POSITION)) {
			dataElement.setProperty(CARET_POSITION,
				((TextControl) component).getCaretPosition());
		}
	}

	/**
	 * Updates the state of a text component from the properties of the data
	 * element.
	 *
	 * @param textComponent The component to update
	 */
	protected void updateTextComponent(TextControl textComponent) {
		String constraint = dataElement.getProperty(INPUT_CONSTRAINT, null);
		String placeholder = dataElement.getProperty(PLACEHOLDER, null);
		int columns = dataElement.getIntProperty(COLUMNS, -1);
		int caretPos = dataElement.getIntProperty(CARET_POSITION, -1);

		if (columns > 0) {
			textComponent.setColumns(columns);
		}

		if (caretPos >= 0) {
			textComponent.setCaretPosition(caretPos);
		}

		if (constraint != null) {
			textComponent.setInputConstraint(constraint);
		}

		if (placeholder != null) {
			textComponent.setPlaceholder(placeholder);
		}

		if (dataElement.hasProperty(EDITABLE)) {
			textComponent.setEditable(dataElement.hasFlag(EDITABLE));
		}
	}

	/**
	 * Updates the value of the element component if the data element value has
	 * changed.
	 */
	protected void updateValue() {
		if (elementComponent instanceof TextControl) {
			updateTextComponent((TextControl) elementComponent);
		}

		transferDataElementValueToComponent(dataElement, elementComponent);

		dataElement.clearFlag(VALUE_CHANGED);
		dataElement.clearFlag(ALLOWED_VALUES_CHANGED);

		String interactionUrl = dataElement.getProperty(INTERACTION_URL, null);

		if (interactionUrl != null) {
			dataElement.removeProperty(INTERACTION_URL);
			openUrl(interactionUrl, dataElement.hasFlag(HIDDEN_URL));
		}

		// reset any modifications so that only changes from subsequent user
		// interactions are recorded as modifications
		dataElement.setModified(false);
	}

	/**
	 * Will be invoked by the panel manager if a UI is removed.
	 */
	void dispose() {
	}

	/**
	 * Handles an event in an input f4ield of a composite phone number UI.
	 *
	 * @param event        The event that occurred
	 * @param numberFields The list of the input fields for the phone number
	 *                     parts
	 */
	void handlePhoneNumberEvent(EwtEvent event, List<TextField> numberFields) {
		TextField field = (TextField) event.getSource();
		String text = field.getText();
		String selectedText = field.getSelectedText();
		KeyCode keyCode = event.getKeyCode();
		ModifierKeys modifiers = event.getModifiers();

		boolean noSelection =
			(selectedText == null || selectedText.length() == 0);

		if (event.getType() == EventType.KEY_PRESSED) {
			textClipboard = null;

			if (modifiers == ModifierKeys.CTRL) {
				if (keyCode == KeyCode.C || keyCode == KeyCode.X) {
					if (noSelection) {
						// set field content to full text for cur or copy on
						// release
						String phoneNumber = getPhoneNumber(field.getParent());

						textClipboard = text;
						field.setText(phoneNumber);
						field.setSelection(0, phoneNumber.length());
					}
				}
			}
		} else if (event.getType() == EventType.KEY_RELEASED) {
			if (modifiers == ModifierKeys.CTRL) {
				switch (keyCode) {
					case X:
						if (textClipboard != null) {
							setPhoneNumber(numberFields, "");
						}

						break;

					case C:
						if (textClipboard != null) {
							// restore original text after copy
							field.setText(textClipboard);
						}

						break;

					case V:
						setPhoneNumber(numberFields, text);
						break;

					default:
						// ignore other
				}
			}
		} else if (event.getType() == EventType.KEY_TYPED) {
			char c = event.getKeyChar();

			if (c != 0) {
				if (Character.isDigit(c)) {
					if (noSelection && numberFields.indexOf(field) == 0 &&
						text.length() == 3) {
						event.cancel();
					}
				} else if (modifiers == ModifierKeys.NONE ||
					modifiers == ModifierKeys.SHIFT) {
					event.cancel();
				}
			}
		}
	}

	/**
	 * Initializes this instance for a certain parent and data element.
	 *
	 * @param parent  The parent panel manager
	 * @param element The data element to be displayed
	 */
	void init(DataElementPanelManager parent, D element) {
		this.panelManager = parent;
		this.dataElement = element;
	}

	/**
	 * Updates the base style of this instance. Can be used by subclasses to
	 * add
	 * implementation-specific styles.
	 *
	 * @param baseStyle The new base style
	 */
	final void setBaseStyle(StyleData baseStyle) {
		this.baseStyle = baseStyle;
	}

	/**
	 * Checks whether the {@link #labelSuffix} needs to be appended to the
	 * given
	 * label.
	 *
	 * @param label The label text
	 * @return The label with the appended suffix if necessary
	 */
	private String appendLabelSuffix(String label) {
		if (label != null && labelSuffix != null && label.length() > 0 &&
			"#%+".indexOf(label.charAt(0)) == -1) {
			label += labelSuffix;
		}

		return label;
	}

	/**
	 * Collects the phone number string from a composite phone number field.
	 *
	 * @param component The parent component of the phone number input fields
	 * @return The phone number string
	 */
	private String getPhoneNumber(Component component) {
		StringBuilder numberBuilder = new StringBuilder();
		List<Component> components = ((Panel) component).getComponents();
		char separator = '+';

		for (int i = 1; i < 8; i += 2) {
			TextField numberField = (TextField) components.get(i);
			String part = numberField.getText();

			if (i < 7 || part.length() > 0) {
				numberBuilder.append(separator);
			}

			numberBuilder.append(part);

			separator = i == 1 ? '.' : '-';
		}

		String phoneNumber = numberBuilder.toString();

		if (phoneNumber.equals("+.-")) {
			phoneNumber = "";
		}

		return phoneNumber;
	}

	/**
	 * Parses a phone number value and sets it into a set of input fields for
	 * the distinct number parts.
	 *
	 * @param numberFields The input fields for the number parts
	 * @param number       The phone number value
	 */
	private void setPhoneNumber(final List<TextField> numberFields,
		String number) {
		List<String> numberParts =
			StringDataElement.getPhoneNumberParts(number);

		for (int i = 0; i < 4; i++) {
			numberFields.get(i).setText(numberParts.get(i));
		}
	}
}
