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

	private DataElementPanelManager rPanelManager;

	private D rDataElement;

	private int nEventHandlingDelay = 0;

	private Timer aInputEventTimer;

	private Set<InteractionEventType> rEventTypes;

	/**
	 * Creates a new instance.
	 *
	 * @param rPanelManager The panel manager the data element belongs to
	 * @param rDataElement  The data element to handle events for
	 */
	public DataElementInteractionHandler(DataElementPanelManager rPanelManager,
		D rDataElement) {
		this.rPanelManager = rPanelManager;
		this.rDataElement = rDataElement;
	}

	/**
	 * Returns the data element.
	 *
	 * @return The data element
	 */
	public final D getDataElement() {
		return rDataElement;
	}

	/**
	 * Returns the panel manager.
	 *
	 * @return The panel manager
	 */
	public final DataElementPanelManager getPanelManager() {
		return rPanelManager;
	}

	@Override
	public void handleEvent(final EwtEvent rEvent) {
		boolean bDeferredEventHandling = nEventHandlingDelay > 0 ||
			(rEventTypes.contains(InteractionEventType.UPDATE) &&
				rEvent.getType() == EventType.KEY_RELEASED);

		cancelInputEventTimer();

		if (bDeferredEventHandling) {
			aInputEventTimer = new Timer() {
				@Override
				public void run() {
					processEvent(rEvent);
				}
			};
			aInputEventTimer.schedule(nEventHandlingDelay > 0 ?
			                          nEventHandlingDelay :
			                          DEFAULT_EVENT_HANDLING_DELAY);
		} else {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					processEvent(rEvent);
				}
			});
		}
	}

	/**
	 * Initializes the handling of interactive input events for a certain
	 * component if necessary.
	 *
	 * @param rComponent           The component to setup the input handling
	 *                             for
	 * @param bOnContainerChildren TRUE to setup the input handling for the
	 *                             children if the component is a container
	 * @return TRUE if the event handling has been initialized, FALSE if no
	 * event types have been registered and no event handling is necessary
	 */
	public boolean setupEventHandling(Component rComponent,
		boolean bOnContainerChildren) {
		rEventTypes = rDataElement.getProperty(INTERACTION_EVENT_TYPES,
			Collections.<InteractionEventType>emptySet());

		boolean bHasEventHandling = !rEventTypes.isEmpty();
		Widget rWidget = rComponent.getWidget();

		if (rWidget instanceof HasEventHandlingDelay) {
			nEventHandlingDelay =
				((HasEventHandlingDelay) rWidget).getEventHandlingDelay();
		}

		nEventHandlingDelay = rDataElement.getIntProperty(EVENT_HANDLING_DELAY,
			nEventHandlingDelay);

		if (bHasEventHandling) {
			if (bOnContainerChildren && rComponent instanceof Container) {
				List<Component> rComponents =
					((Container) rComponent).getComponents();

				for (Component rChild : rComponents) {
					registerEventHandler(rChild, rEventTypes);
				}
			} else {
				registerEventHandler(rComponent, rEventTypes);
			}
		}

		return bHasEventHandling;
	}

	/**
	 * Maps the interaction event types to the corresponding GEWT event types
	 * for a certain component.
	 *
	 * @param rComponent             The component
	 * @param rInteractionEventTypes The interaction event types to map
	 * @return The mapped GEWT event types
	 */
	protected Set<EventType> getInteractionEventTypes(Component rComponent,
		Set<InteractionEventType> rInteractionEventTypes) {
		Set<EventType> rEventTypes = EnumSet.noneOf(EventType.class);

		if (rComponent instanceof TextControl) {
			if (rInteractionEventTypes.contains(InteractionEventType.UPDATE)) {
				rEventTypes.add(EventType.KEY_RELEASED);
				rEventTypes.add(EventType.VALUE_CHANGED);
			}

			if (rInteractionEventTypes.contains(InteractionEventType.ACTION)) {
				rEventTypes.add(EventType.ACTION);
			}

			if (rInteractionEventTypes.contains(
				InteractionEventType.FOCUS_LOST)) {
				rEventTypes.add(EventType.FOCUS_LOST);
			}
		} else {
			if (rInteractionEventTypes.contains(InteractionEventType.UPDATE)) {
				rEventTypes.add(EventType.SELECTION);
			}

			if (rInteractionEventTypes.contains(InteractionEventType.ACTION)) {
				rEventTypes.add(EventType.ACTION);
			}
		}

		return rEventTypes;
	}

	/**
	 * Checks whether the value of an event source component has changed
	 * compared to the data element. This applies only to {@link TextControl}
	 * components to prevent unnecessary events on cursor navigation and
	 * similar, where the actual content doesn't change.
	 *
	 * @param rEventSource The event source to check
	 * @return TRUE if the value has changed
	 */
	protected boolean hasValueChanged(Object rEventSource) {
		return !(rEventSource instanceof TextControl) ||
			rEventSource instanceof ComboBox || !((TextControl) rEventSource)
			.getText()
			.equals(rDataElement.getValue());
	}

	/**
	 * Maps a GWT event type to the corresponding interaction event type.
	 *
	 * @param eEventType The event type to map
	 * @return The matching interaction event type
	 */
	protected InteractionEventType mapToInteractionEventType(
		EventType eEventType) {
		InteractionEventType eInteractionEventType;

		if (eEventType == EventType.ACTION) {
			eInteractionEventType = InteractionEventType.ACTION;
		} else if (eEventType == EventType.FOCUS_LOST) {
			eInteractionEventType = InteractionEventType.FOCUS_LOST;
		} else {
			eInteractionEventType = InteractionEventType.UPDATE;
		}

		return eInteractionEventType;
	}

	/**
	 * Processes a certain event and forwards it to the panel manager for
	 * interaction handling.
	 *
	 * @param rEvent The GEWT event that occurred
	 */
	protected void processEvent(EwtEvent rEvent) {
		cancelInputEventTimer();

		EventType eEventType = rEvent.getType();
		Object rEventData = rEvent.getElement();
		Object rSource = rEvent.getSource();

		InteractionEventType eInteractionEventType =
			mapToInteractionEventType(eEventType);

		if (rEventData != null) {
			rDataElement.setProperty(INTERACTION_EVENT_DATA,
				rEventData.toString());
		}

		// VALUE_CHANGED can occur if a text field looses focus
		if ((eEventType != EventType.VALUE_CHANGED &&
			eEventType != EventType.FOCUS_LOST) ||
			!(rSource instanceof TextControl)) {
			// this is needed to re-establish the input focus in certain
			// browsers (Webkit, IE)
			rDataElement.setFlag(FOCUSED);
		}

		if ((eEventType != EventType.KEY_RELEASED &&
			eEventType != EventType.VALUE_CHANGED) ||
			hasValueChanged(rSource)) {
			if (rPanelManager
				.getDataElementList()
				.hasFlag(DISABLE_ON_INTERACTION)) {
				rPanelManager.getContainer().setChildrenEnabled(false);
			} else if (rDataElement.hasFlag(DISABLE_ON_INTERACTION)) {
				((Component) rSource).setEnabled(false);
			}

			rPanelManager.handleInteractiveInput(rDataElement,
				eInteractionEventType);
		}
	}

	/**
	 * Registers an event listener for the handling of interactive input events
	 * with the given component.
	 *
	 * @param aComponent  The component
	 * @param rEventTypes The event types to register the event handlers for
	 */
	protected void registerEventHandler(Component aComponent,
		Set<InteractionEventType> rEventTypes) {
		for (EventType eEventType : getInteractionEventTypes(aComponent,
			rEventTypes)) {
			aComponent.addEventListener(eEventType, this);
		}
	}

	/**
	 * Updates the data element to a new instance.
	 *
	 * @param rNewDataElement The new data element
	 */
	void updateDataElement(D rNewDataElement) {
		rDataElement = rNewDataElement;
	}

	/**
	 * Cancels the input event timer if it is currently running.
	 */
	private void cancelInputEventTimer() {
		if (aInputEventTimer != null) {
			aInputEventTimer.cancel();
			aInputEventTimer = null;
		}
	}
}
