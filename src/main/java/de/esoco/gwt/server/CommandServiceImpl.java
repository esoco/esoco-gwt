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

import de.esoco.data.SessionManager;
import de.esoco.data.element.DataElement;

import de.esoco.gwt.shared.Command;
import de.esoco.gwt.shared.CommandService;
import de.esoco.gwt.shared.ServiceException;

import de.esoco.lib.logging.Log;
import de.esoco.lib.reflect.ReflectUtil;
import de.esoco.lib.text.TextConvert;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The base implementation of the {@link CommandService} interface. The
 * interface method {@link #executeCommand(Command, DataElement)} has a default
 * implementation that dispatches command executions to subclass methods named
 * {@code handle <CommandName>} by means of reflection. Therefore a subclass
 * only needs to implement a corresponding handler method for each command it
 * defines in it's public service interface.
 *
 * @author eso
 */
public abstract class CommandServiceImpl extends RemoteServiceServlet
	implements CommandService {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_RESOURCE_KEY = "DEFAULT";

	private String applicationName = null;

	private Map<String, ResourceBundle> localeResources = new HashMap<>();

	/**
	 * @see CommandService#executeCommand(Command, DataElement)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends DataElement<?>, R extends DataElement<?>> R executeCommand(
		Command<T, R> command, T data) throws ServiceException {
		checkCommandExecution(command, data);

		String method =
			"handle" + TextConvert.capitalizedIdentifier(command.getName());

		try {
			Method handler =
				ReflectUtil.findAnyPublicMethod(getClass(), method);

			if (handler == null) {
				throw new ServiceException(
					"Missing command handling method " + method);
			}

			return (R) handler.invoke(this, data);
		} catch (Throwable e) {
			throw handleException(e);
		}
	}

	/**
	 * @see SessionManager#getAbsoluteFileName(String)
	 */
	public String getAbsoluteFileName(String fileName) {
		return getServletContext().getRealPath(fileName);
	}

	/**
	 * @see RemoteServiceServlet#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(),
			getServletContext().getServerInfo());
	}

	/**
	 * Performs a checks whether the execution of a command is possible. This
	 * method can be overridden by subclasses to implement authentication or
	 * command (parameter) validations. To deny the command execution the
	 * implementation must throw an exception.
	 *
	 * @param command The command that is about to be executed
	 * @param data    The command argument
	 * @throws ServiceException If the command is not allowed to be executed
	 */
	protected <T extends DataElement<?>> void checkCommandExecution(
		Command<T, ?> command, T data) throws ServiceException {
	}

	/**
	 * Returns the name of the application this service belongs to. The default
	 * implementation returns the service name (without a trailing
	 * "ServiceImpl"
	 * if present). Subclasses may override this method to return a different
	 * name.
	 *
	 * @return The application name
	 */
	protected String getApplicationName() {
		if (applicationName == null) {
			applicationName = getClass().getSimpleName();

			int index = applicationName.indexOf("ServiceImpl");

			if (index > 0) {
				applicationName = applicationName.substring(0, index);
			}
		}

		return applicationName;
	}

	/**
	 * Returns the app resource for a certain locale.
	 *
	 * @param locale The locale name or NULL for the default resource
	 * @return The resource bundle (will NULL if not even a default resource is
	 * found)
	 */
	protected ResourceBundle getResource(String locale) {
		String key = locale != null ? locale : DEFAULT_RESOURCE_KEY;

		ResourceBundle resource = localeResources.get(key);

		if (resource == null) {
			if (locale != null) {
				resource = readResourceFile(
					String.format("%s/%s_%sStrings.properties",
						getResourcePath(), getResourceBaseName(), locale));
			} else {
				resource = readResourceFile(
					String.format("%s/%sStrings.properties", getResourcePath(),
						getResourceBaseName()));
			}

			if (resource == null && locale != null) {
				int localeSeparator = locale.indexOf('_');
				String nextLocale = null;

				if (localeSeparator > 0) {
					nextLocale = locale.substring(0, localeSeparator);
				}

				resource = getResource(nextLocale);
			}

			// not in else branch to also store the default resource under the
			// locale key if no locale-specific file exists
			if (resource != null) {
				localeResources.put(key, resource);
			}
		}

		return resource;
	}

	/**
	 * Returns the base name for the lookup of resource properties files. The
	 * default implementation returns {@link #getApplicationName()}.
	 *
	 * @return The resource base name
	 */
	protected String getResourceBaseName() {
		return getApplicationName();
	}

	/**
	 * Returns the server-side path to the resource files of this application.
	 * The default value is 'data/res' relative to the web application base
	 * path. If overridden the returned path must not end with a directory
	 * separator.
	 *
	 * @return The resource path
	 */
	protected String getResourcePath() {
		return "data/res";
	}

	/**
	 * Returns a string from the application resources if such exist. If no
	 * resource for a given locale is found the corresponding string from the
	 * default resource is returned instead (if available).
	 *
	 * @param key    The key identifying the resource
	 * @param locale The locale to return the resource string for or NULL for
	 *               the default resource
	 * @return The resource string or NULL if not found
	 */
	protected String getResourceString(String key, String locale) {
		ResourceBundle resource = getResource(locale);
		String resourceString = null;

		if (resource != null) {
			try {
				resourceString = resource.getString(key);
			} catch (MissingResourceException e) {
				// just return NULL
			}
		}

		return resourceString;
	}

	/**
	 * Logs, processes, and if necessary converts an exception. If the argument
	 * exception is a service exception it will be returned directly. All other
	 * exceptions will be converted into a service exception.
	 *
	 * @param e The exception to handle
	 * @return Always returns a service exception
	 */
	protected ServiceException handleException(Throwable e) {
		if (e instanceof InvocationTargetException) {
			Throwable t = e.getCause();

			if (t instanceof Exception) {
				e = t;
			}
		}

		if (!(e instanceof ServiceException &&
			((ServiceException) e).isRecoverable())) {
			Log.error(e.getMessage(), e);
		}

		if (e instanceof ServiceException) {
			return (ServiceException) e;
		} else {
			return new ServiceException(e);
		}
	}

	/**
	 * Tries to read a resource file with a certain name.
	 *
	 * @param fileName The resource file name
	 * @return A new resource bundle instance from the given file or NULL if no
	 * matching file could be found
	 */
	protected ResourceBundle readResourceFile(String fileName) {
		ResourceBundle resource;

		try {
			fileName = getAbsoluteFileName(fileName);

			InputStreamReader reader =
				new InputStreamReader(new FileInputStream(fileName), "UTF-8");

			resource = new PropertyResourceBundle(reader);
		} catch (IOException e) {
			resource = null;
		}

		return resource;
	}
}
