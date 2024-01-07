//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.obrel.core.RelatedObject;

/**
 * A base class that provides a context for GWT web applications. It implements
 * the ServletContextListener interface to initialize and shutdown global data
 * structures and executes schedule processes if setup by subclasses. It also
 * listens via HttpSessionListener for sessions and reports changes to an
 * {@link AuthenticatedServiceImpl} instance if one has been registered with.
 *
 * <p>To enable a service context the following code must be added to the
 * application's web.xml file:</p>
 *
 * <pre>
 * &lt;listener&gt;
 * &lt;listener-class&gt;
 * [full name of the ServiceContext subclass]
 * &lt;listener-class&gt;
 * &lt;listener&gt;
 * </pre>
 *
 * @author eso
 */
public abstract class ServiceContext extends RelatedObject
	implements ServletContextListener, HttpSessionListener {

	private static ServiceContext serviceContextInstance = null;

	private AuthenticatedServiceImpl<?> service;

	private ServletContext servletContext;

	/**
	 * Returns the instance.
	 *
	 * @return The instance
	 */
	public static ServiceContext getInstance() {
		return serviceContextInstance;
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		destroy(servletContext);

		servletContext = null;
		serviceContextInstance = null;

		Log.info("Service context shutdown complete");
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		if (serviceContextInstance != null) {
			throw new IllegalStateException("Multiple service contexts");
		}

		servletContext = event.getServletContext();

		init(servletContext);

		serviceContextInstance = this;
	}

	/**
	 * Returns the service of this instance.
	 *
	 * @return The service or NULL if not set
	 */
	public final AuthenticatedServiceImpl<?> getService() {
		return service;
	}

	/**
	 * Returns the servlet context of this instance.
	 *
	 * @return The servlet context
	 */
	public final ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		if (service != null) {
			service.removeSession(event.getSession());
		}
	}

	/**
	 * Sets the service of this context.
	 *
	 * @param service The service
	 */
	public final void setService(AuthenticatedServiceImpl<?> service) {
		this.service = service;
	}

	/**
	 * This method can be overridden by subclasses to cleanup internal data
	 * structures. The default implementation does nothing.
	 *
	 * @param servletContext The servlet context
	 */
	protected void destroy(ServletContext servletContext) {
	}

	/**
	 * Returns the application name.
	 *
	 * @return The application name
	 */
	protected String getApplicationName() {
		String name;

		if (service != null) {
			name = service.getApplicationName();
		} else {
			name = getClass().getSimpleName();

			int index = name.indexOf(ServiceContext.class.getSimpleName());

			if (index > 0) {
				name = name.substring(0, index);
			}
		}

		return name;
	}

	/**
	 * This method can be overridden by subclasses to initialize internal data
	 * structures. The default implementation does nothing.
	 *
	 * @param servletContext The servlet context
	 */
	protected void init(ServletContext servletContext) {
	}
}
