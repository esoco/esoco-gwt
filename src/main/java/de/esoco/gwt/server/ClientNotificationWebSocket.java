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

	private static ClientNotificationService rNotificationService;

	/**
	 * Sets the service this web socket belongs to.
	 *
	 * @param rService The service
	 */
	static void setService(ClientNotificationService rService) {
		rNotificationService = rService;
	}

	/**
	 * Invoked when the socket is closed.
	 *
	 * @param rSession The closed session
	 * @param rReason  The close reason
	 */
	@Override
	public void onClose(Session rSession, CloseReason rReason) {
		rNotificationService.getSessions().remove(rSession);

		Log.infof("%s[%s] closed", getClass().getSimpleName(),
			rSession.getId());
	}

	/**
	 * Invoked on socket errors.
	 *
	 * @param rSession The session
	 * @param eError   The error that occurred
	 */
	@Override
	public void onError(Session rSession, Throwable eError) {
		rNotificationService.getSessions().remove(rSession);

		Log.errorf(eError, "%s[%s] error", getClass().getSimpleName(),
			rSession.getId());
	}

	/**
	 * Invoked when the socket connection has been established.
	 *
	 * @param rSession The client session
	 * @param rConfig  The endpoint configuration
	 */
	@Override
	public void onOpen(Session rSession, EndpointConfig rConfig) {
		rNotificationService.getSessions().add(rSession);
		rSession.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String sMessage) {
				ClientNotificationWebSocket.this.onMessage(rSession, sMessage);
			}
		});

		Log.infof("%s[%s] opened", getClass().getSimpleName(),
			rSession.getId());
	}

	/**
	 * Receives client messages.
	 *
	 * @param rSession The client session
	 * @param sMessage The message
	 */
	void onMessage(Session rSession, String sMessage) {
		Log.warn("Client message ignored");
	}
}
