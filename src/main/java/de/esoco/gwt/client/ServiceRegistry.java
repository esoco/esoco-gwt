//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.gwt.client;

import de.esoco.gwt.shared.AuthenticatedServiceAsync;
import de.esoco.gwt.shared.CommandService;
import de.esoco.gwt.shared.CommandServiceAsync;
import de.esoco.gwt.shared.StorageServiceAsync;

/**
 * A registry for standard services. An application-specific service can
 * subclass standard services like {@link CommandService}. On creation of the
 * service the application should then register the asynchronous interface of
 * it's service through the method {@link #registerStandardServices(Object)}.
 * This will make the implemented standard services available to generic code.
 *
 * @author eso
 */
public class ServiceRegistry {

	private static CommandServiceAsync commandService;

	private static AuthenticatedServiceAsync authenticatedService;

	private static StorageServiceAsync storageService;

	/**
	 * Private, only static use.
	 */
	private ServiceRegistry() {
	}

	/**
	 * Returns the current command service. If no standard services have been
	 * registered by calling {@link #registerStandardServices(Object)} this
	 * method will return NULL.
	 *
	 * @return The asynchronous interface of the current command service or
	 * NULL
	 * for none
	 */
	public static AuthenticatedServiceAsync getAuthenticatedService() {
		return authenticatedService;
	}

	/**
	 * Returns the current authenticated service. If no standard services have
	 * been registered by calling {@link #registerStandardServices(Object)}
	 * this
	 * method will return NULL.
	 *
	 * @return The asynchronous interface of the current authenticated service
	 * or NULL for none
	 */
	public static CommandServiceAsync getCommandService() {
		return commandService;
	}

	/**
	 * Returns the current storage service. If no standard services have been
	 * registered by calling {@link #registerStandardServices(Object)} this
	 * method will return NULL.
	 *
	 * @return The asynchronous interface of the current storage service or
	 * NULL
	 * for none
	 */
	public static StorageServiceAsync getStorageService() {
		return storageService;
	}

	/**
	 * Initializes the service registry from a certain service class. The given
	 * class must be the asynchronous variant of a sub-interface of one of the
	 * GWT framework service classes that are based on {@link CommandService}.
	 * This method must be invoked by framework clients before the standard
	 * services are accessed.
	 *
	 * @param serviceAsync The service class to initialize the registry from
	 */
	public static void init(CommandServiceAsync serviceAsync) {
		registerStandardServices(serviceAsync);
	}

	/**
	 * This method can be invoked to register the standard asynchronous service
	 * interfaces that are implemented by a certain application-specific
	 * service
	 * for generic use. The standard services can then be queried through the
	 * other static methods in this class.
	 *
	 * @param serviceAsync The application-specific asynchronous service
	 *                     interface that extends one or more standard service
	 *                     interfaces
	 */
	public static void registerStandardServices(Object serviceAsync) {
		if (serviceAsync instanceof CommandServiceAsync) {
			commandService = (CommandServiceAsync) serviceAsync;
		}

		if (serviceAsync instanceof AuthenticatedServiceAsync) {
			authenticatedService = (AuthenticatedServiceAsync) serviceAsync;
		}

		if (serviceAsync instanceof StorageServiceAsync) {
			storageService = (StorageServiceAsync) serviceAsync;
		}
	}
}
