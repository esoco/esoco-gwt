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

import de.esoco.lib.expression.monad.Try;
import de.esoco.lib.logging.Log;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import static java.util.stream.Collectors.toList;

/**
 * A server-side WebSocket to send client notifications over.
 *
 * @author eso
 */
public class ClientNotificationService {

	private static final CloseReason CLOSE_REASON_SHUTDOWN =
		new CloseReason(CloseCodes.GOING_AWAY, "Shutting down");

	private final String sWebSocketPath;

	private final List<Session> aSessions = new ArrayList<>();

	/**
	 * Creates a new instance.
	 *
	 * @param sPath The service-relative path to the web socket of this service
	 */
	public ClientNotificationService(String sPath) {
		this.sWebSocketPath = sPath.startsWith("/") ? sPath : "/" + sPath;
	}

	/**
	 * Notifies all registered clients of a message.
	 *
	 * @param sMessage The message string
	 */
	public void notifyClients(String sMessage) {
		aSessions.forEach(rSession -> Try
			.run(() -> rSession.getBasicRemote().sendText(sMessage))
			.orElse(e -> Log.errorf(e, "Notification of client %s failed",
				rSession.getId())));
	}

	/**
	 * Registers a {@link Endpoint WebSocket Endpoint} class for deployment
	 * at a
	 * certain path relative to the servlet context.
	 *
	 * @param rContext rWebSocketClass The class of the endpoint
	 * @throws ServletException If the endpoint registration failed
	 */
	public void start(ServletContext rContext) throws ServletException {
		ServerContainer rServerContainer =
			(ServerContainer) rContext.getAttribute(
				"javax.websocket.server.ServerContainer");

		if (rServerContainer == null) {
			throw new ServletException(
				"No server container for WebSocket deployment found");
		}

		ClientNotificationWebSocket.setService(this);

		ServerEndpointConfig aConfig = ServerEndpointConfig.Builder
			.create(ClientNotificationWebSocket.class,
				rContext.getContextPath() + sWebSocketPath)
			.build();

		try {
			rServerContainer.addEndpoint(aConfig);
		} catch (DeploymentException e) {
			throw new ServletException(e);
		}

		Log.infof("Client notification WebSocket deployed at %s\n",
			aConfig.getPath());
	}

	/**
	 * Stops this service by closing all open connections.
	 */
	public void stop() {
		Try
			.ofAll(aSessions
				.stream()
				.map(s -> Try.run(() -> s.close(CLOSE_REASON_SHUTDOWN)))
				.collect(toList()))
			.orElse(e -> Log.error("Error when closing sessions", e));

		aSessions.clear();
	}

	/**
	 * Returns the current sessions.
	 *
	 * @return The sessions
	 */
	List<Session> getSessions() {
		return aSessions;
	}
}
