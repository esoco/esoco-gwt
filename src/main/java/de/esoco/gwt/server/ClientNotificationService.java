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

	private final String webSocketPath;

	private final List<Session> sessions = new ArrayList<>();

	/**
	 * Creates a new instance.
	 *
	 * @param path The service-relative path to the web socket of this service
	 */
	public ClientNotificationService(String path) {
		this.webSocketPath = path.startsWith("/") ? path : "/" + path;
	}

	/**
	 * Notifies all registered clients of a message.
	 *
	 * @param message The message string
	 */
	public void notifyClients(String message) {
		sessions.forEach(session -> Try
			.run(() -> session.getBasicRemote().sendText(message))
			.orElse(e -> Log.errorf(e, "Notification of client %s failed",
				session.getId())));
	}

	/**
	 * Registers a {@link Endpoint WebSocket Endpoint} class for deployment
	 * at a
	 * certain path relative to the servlet context.
	 *
	 * @param context rWebSocketClass The class of the endpoint
	 * @throws ServletException If the endpoint registration failed
	 */
	public void start(ServletContext context) throws ServletException {
		ServerContainer serverContainer =
			(ServerContainer) context.getAttribute(
				"javax.websocket.server.ServerContainer");

		if (serverContainer == null) {
			throw new ServletException(
				"No server container for WebSocket deployment found");
		}

		ClientNotificationWebSocket.setService(this);

		ServerEndpointConfig config = ServerEndpointConfig.Builder
			.create(ClientNotificationWebSocket.class,
				context.getContextPath() + webSocketPath)
			.build();

		try {
			serverContainer.addEndpoint(config);
		} catch (DeploymentException e) {
			throw new ServletException(e);
		}

		Log.infof("Client notification WebSocket deployed at %s\n",
			config.getPath());
	}

	/**
	 * Stops this service by closing all open connections.
	 */
	public void stop() {
		Try
			.ofAll(sessions
				.stream()
				.map(s -> Try.run(() -> s.close(CLOSE_REASON_SHUTDOWN)))
				.collect(toList()))
			.orElse(e -> Log.error("Error when closing sessions", e));

		sessions.clear();
	}

	/**
	 * Returns the current sessions.
	 *
	 * @return The sessions
	 */
	List<Session> getSessions() {
		return sessions;
	}
}
