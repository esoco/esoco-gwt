//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.ewt.component.ComboBox;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.TextControl;
import de.esoco.ewt.event.EwtEvent;
import de.esoco.ewt.event.EwtEventHandler;
import de.esoco.ewt.event.EventType;
import de.esoco.ewt.impl.gwt.HasEventHandlingDelay;

import de.esoco.lib.property.InteractionEventType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import static de.esoco.lib.property.StateProperties.DISABLE_ON_INTERACTION;
import static de.esoco.lib.property.StateProperties.EVENT_HANDLING_DELAY;
import static de.esoco.lib.property.StateProperties.FOCUSED;
import static de.esoco.lib.property.StateProperties.INTERACTION_EVENT_DATA;
import static de.esoco.lib.property.StateProperties.INTERACTION_EVENT_TYPES;

/**
 * A class that handles the interaction events for a certain
 * {@link DataElement}.
 *
 * @author eso
 */
public class DataElementInteractionHandler<D extends DataElement<?>>
	implements EwtEventHandler {

	private static final int DEFAULT_EVENT_HANDLING_DELAY = 750;

	private DataElementPanelManager panelManager;

	private D dataElement;

	private int eventHandlingDelay = 0;

	private Timer inputEventTimer;

	private Set<InteractionEventType> eventTypes;

	/**
	 * Creates a new instance.
	 *
	 * @param panelManager The panel manager the data element belongs to
	 * @param dataElement  The data element to handle events for
	 */
	public DataElementInteractionHandler(DataElementPanelManager panelManager,
		D dataElement) {
		this.panelManager = panelManager;
		this.dataElement = dataElement;
	}

	/**
	 * Returns the data element.
	 *
	 * @return The data element
	 */
	public final D getDataElement() {
		return dataElement;
	}

	/**
	 * Returns the panel manager.
	 *
	 * @return The panel manager
	 */
	public final DataElementPanelManager getPanelManager() {
		return panelManager;
	}

	@Override
	public void handleEvent(final EwtEvent event) {
		boolean deferredEventHandling = eventHandlingDelay > 0 ||
			(eventTypes.contains(InteractionEventType.UPDATE) &&
				event.getType() == EventType.KEY_RELEASED);

		cancelInputEventTimer();

		if (deferredEventHandling) {
			inputEventTimer = new Timer() {
				@Override
				public void run() {
					processEvent(event);
				}
			};
			inputEventTimer.schedule(eventHandlingDelay > 0 ?
			                         eventHandlingDelay :
			                         DEFAULT_EVENT_HANDLING_DELAY);
		} else {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					processEvent(event);
				}
			});
		}
	}

	/**
	 * Initializes the handling of interactive input events for a certain
	 * component if necessary.
	 *
	 * @param component           The component to setup the input handling for
	 * @param onContainerChildren TRUE to setup the input handling for the
	 *                            children if the component is a container
	 * @return TRUE if the event handling has been initialized, FALSE if no
	 * event types have been registered and no event handling is necessary
	 */
	public boolean setupEventHandling(Component component,
		boolean onContainerChildren) {
		eventTypes = dataElement.getProperty(INTERACTION_EVENT_TYPES,
			Collections.<InteractionEventType>emptySet());

		boolean hasEventHandling = !eventTypes.isEmpty();
		Widget widget = component.getWidget();

		if (widget instanceof HasEventHandlingDelay) {
			eventHandlingDelay =
				((HasEventHandlingDelay) widget).getEventHandlingDelay();
		}

		eventHandlingDelay = dataElement.getIntProperty(EVENT_HANDLING_DELAY,
			eventHandlingDelay);

		if (hasEventHandling) {
			if (onContainerChildren && component instanceof Container) {
				List<Component> components =
					((Container) component).getComponents();

				for (Component child : components) {
					registerEventHandler(child, eventTypes);
				}
			} else {
				registerEventHandler(component, eventTypes);
			}
		}

		return hasEventHandling;
	}

	/**
	 * Maps the interaction event types to the corresponding GEWT event types
	 * for a certain component.
	 *
	 * @param component             The component
	 * @param interactionEventTypes The interaction event types to map
	 * @return The mapped GEWT event types
	 */
	protected Set<EventType> getInteractionEventTypes(Component component,
		Set<InteractionEventType> interactionEventTypes) {
		Set<EventType> eventTypes = EnumSet.noneOf(EventType.class);

		if (component instanceof TextControl) {
			if (interactionEventTypes.contains(InteractionEventType.UPDATE)) {
				eventTypes.add(EventType.KEY_RELEASED);
				eventTypes.add(EventType.VALUE_CHANGED);
			}

			if (interactionEventTypes.contains(InteractionEventType.ACTION)) {
				eventTypes.add(EventType.ACTION);
			}

			if (interactionEventTypes.contains(
				InteractionEventType.FOCUS_LOST)) {
				eventTypes.add(EventType.FOCUS_LOST);
			}
		} else {
			if (interactionEventTypes.contains(InteractionEventType.UPDATE)) {
				eventTypes.add(EventType.SELECTION);
			}

			if (interactionEventTypes.contains(InteractionEventType.ACTION)) {
				eventTypes.add(EventType.ACTION);
			}
		}

		return eventTypes;
	}

	/**
	 * Checks whether the value of an event source component has changed
	 * compared to the data element. This applies only to {@link TextControl}
	 * components to prevent unnecessary events on cursor navigation and
	 * similar, where the actual content doesn't change.
	 *
	 * @param eventSource The event source to check
	 * @return TRUE if the value has changed
	 */
	protected boolean hasValueChanged(Object eventSource) {
		return !(eventSource instanceof TextControl) ||
			eventSource instanceof ComboBox || !((TextControl) eventSource)
			.getText()
			.equals(dataElement.getValue());
	}

	/**
	 * Maps a GWT event type to the corresponding interaction event type.
	 *
	 * @param eventType The event type to map
	 * @return The matching interaction event type
	 */
	protected InteractionEventType mapToInteractionEventType(
		EventType eventType) {
		InteractionEventType interactionEventType;

		if (eventType == EventType.ACTION) {
			interactionEventType = InteractionEventType.ACTION;
		} else if (eventType == EventType.FOCUS_LOST) {
			interactionEventType = InteractionEventType.FOCUS_LOST;
		} else {
			interactionEventType = InteractionEventType.UPDATE;
		}

		return interactionEventType;
	}

	/**
	 * Processes a certain event and forwards it to the panel manager for
	 * interaction handling.
	 *
	 * @param event The GEWT event that occurred
	 */
	protected void processEvent(EwtEvent event) {
		cancelInputEventTimer();

		EventType eventType = event.getType();
		Object eventData = event.getElement();
		Object source = event.getSource();

		InteractionEventType interactionEventType =
			mapToInteractionEventType(eventType);

		if (eventData != null) {
			dataElement.setProperty(INTERACTION_EVENT_DATA,
				eventData.toString());
		}

		// VALUE_CHANGED can occur if a text field looses focus
		if ((eventType != EventType.VALUE_CHANGED &&
			eventType != EventType.FOCUS_LOST) ||
			!(source instanceof TextControl)) {
			// this is needed to re-establish the input focus in certain
			// browsers (Webkit, IE)
			dataElement.setFlag(FOCUSED);
		}

		if ((eventType != EventType.KEY_RELEASED &&
			eventType != EventType.VALUE_CHANGED) || hasValueChanged(source)) {
			if (panelManager
				.getDataElementList()
				.hasFlag(DISABLE_ON_INTERACTION)) {
				panelManager.getContainer().setChildrenEnabled(false);
			} else if (dataElement.hasFlag(DISABLE_ON_INTERACTION)) {
				((Component) source).setEnabled(false);
			}

			panelManager.handleInteractiveInput(dataElement,
				interactionEventType);
		}
	}

	/**
	 * Registers an event listener for the handling of interactive input events
	 * with the given component.
	 *
	 * @param component  The component
	 * @param eventTypes The event types to register the event handlers for
	 */
	protected void registerEventHandler(Component component,
		Set<InteractionEventType> eventTypes) {
		for (EventType eventType : getInteractionEventTypes(component,
			eventTypes)) {
			component.addEventListener(eventType, this);
		}
	}

	/**
	 * Updates the data element to a new instance.
	 *
	 * @param newDataElement The new data element
	 */
	void updateDataElement(D newDataElement) {
		dataElement = newDataElement;
	}

	/**
	 * Cancels the input event timer if it is currently running.
	 */
	private void cancelInputEventTimer() {
		if (inputEventTimer != null) {
			inputEventTimer.cancel();
			inputEventTimer = null;
		}
	}
}
