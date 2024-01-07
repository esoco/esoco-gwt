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
package de.esoco.gwt.client.app;

import de.esoco.ewt.app.EWTEntryPoint;
import de.esoco.ewt.app.EWTModule;
import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.ViewStyle;

import de.esoco.gwt.client.ServiceRegistry;
import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.gwt.client.ui.AuthenticationPanelManager;
import de.esoco.gwt.shared.GwtApplicationServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;

/**
 * A base class for GWT applications.
 *
 * @author eso
 */
public abstract class GwtApplication extends EWTEntryPoint {

	private GwtProcessAppRootPanel rootPanel;

	/**
	 * Returns the root process panel of this application.
	 *
	 * @return The root panel
	 */
	public GwtProcessAppRootPanel getRootPanel() {
		return rootPanel;
	}

	/**
	 * Overridden as final to invoke the {@link #init()} and {@link #start()}
	 * methods before and after loading the EWT module.
	 *
	 * @see EWTEntryPoint#onModuleLoad()
	 */
	@Override
	public final void onModuleLoad() {
		AuthenticationPanelManager.setAuthenticationCookiePrefix(
			getCookiePrefix());
		ServiceRegistry.init(createApplicationService());
		injectApplicationCss();

		init();

		super.onModuleLoad();

		start();
	}

	/**
	 * Must be implemented by subclasses to create the asynchronous application
	 * service interface by invoking {@link GWT#create(Class)}. This invocation
	 * must occur in the subclass because the GWT compiler parses the class
	 * literal used in the invocation.
	 *
	 * @return The application service
	 */
	protected abstract GwtApplicationServiceAsync createApplicationService();

	/**
	 * Default implementation that creates a module that renders the
	 * application
	 * process in a {@link GwtProcessAppRootPanel}.
	 *
	 * @see de.esoco.ewt.app.EWTEntryPoint#getApplicationModule()
	 */
	@Override
	protected EWTModule getApplicationModule() {
		return new GwtApplicationModule(ViewStyle.DEFAULT) {
			@Override
			protected void createModulePanel(ContainerBuilder<?> builder) {
				rootPanel = new GwtProcessAppRootPanel();

				rootPanel.buildIn(builder, AlignedPosition.CENTER);
			}
		};
	}

	/**
	 * Returns a prefix string for application cookies that are set by the
	 * framework. It is recommended to use an uppercase string without any
	 * special characters. The default implementation returns the only the
	 * uppercase characters of the class name.
	 *
	 * @return The cookie prefix
	 */
	protected String getCookiePrefix() {
		return getClass().getSimpleName().replaceAll("\\p{Lower}}", "");
	}

	/**
	 * Can be implemented by subclasses to perform additional initializations
	 * before application is started. The most important ones will
	 * automatically
	 * be performed by invoking the methods
	 * {@link #createApplicationService()},
	 * {@link #getCookiePrefix()}, and {@link #injectApplicationCss()}.
	 */
	protected void init() {
	}

	/**
	 * Must be overridden to inject the CSS of the current application by
	 * invoking {@link CssResource#ensureInjected()} on it. The implementation
	 * must also invoke the superclass implementation to ensure that the full
	 * CSS chain is initialized
	 */
	protected void injectApplicationCss() {
		EsocoGwtResources.INSTANCE.css().ensureInjected();
	}

	/**
	 * Can be overridden by subclasses to perform actions after the application
	 * has been completely initialized and it's main view is displayed.
	 */
	protected void start() {
	}
}
