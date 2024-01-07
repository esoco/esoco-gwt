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

import de.esoco.lib.logging.Log;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * The WebSocket endpoint for the {@link ClientNotificationService}.
 *
 * @author eso
 */
public class ClientNotificationWebSocket extends Endpoint {

	private static ClientNotificationService notificationService;

	/**
	 * Sets the service this web socket belongs to.
	 *
	 * @param service The service
	 */
	static void setService(ClientNotificationService service) {
		notificationService = service;
	}

	/**
	 * Invoked when the socket is closed.
	 *
	 * @param session The closed session
	 * @param reason  The close reason
	 */
	@Override
	public void onClose(Session session, CloseReason reason) {
		notificationService.getSessions().remove(session);

		Log.infof("%s[%s] closed", getClass().getSimpleName(),
			session.getId());
	}

	/**
	 * Invoked on socket errors.
	 *
	 * @param session The session
	 * @param error   The error that occurred
	 */
	@Override
	public void onError(Session session, Throwable error) {
		notificationService.getSessions().remove(session);

		Log.errorf(error, "%s[%s] error", getClass().getSimpleName(),
			session.getId());
	}

	/**
	 * Invoked when the socket connection has been established.
	 *
	 * @param session The client session
	 * @param config  The endpoint configuration
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		notificationService.getSessions().add(session);
		session.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				ClientNotificationWebSocket.this.onMessage(session, message);
			}
		});

		Log.infof("%s[%s] opened", getClass().getSimpleName(),
			session.getId());
	}

	/**
	 * Receives client messages.
	 *
	 * @param session The client session
	 * @param message The message
	 */
	void onMessage(Session session, String message) {
		Log.warn("Client message ignored");
	}
}
