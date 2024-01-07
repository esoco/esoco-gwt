//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 3.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-3.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.gwt.client.ui;

import de.esoco.data.element.DataElement;

import de.esoco.ewt.build.ContainerBuilder;
import de.esoco.ewt.build.ContainerManager;
import de.esoco.ewt.component.Button;
import de.esoco.ewt.component.Component;
import de.esoco.ewt.component.Container;
import de.esoco.ewt.component.Panel;
import de.esoco.ewt.layout.FlowLayout;
import de.esoco.ewt.layout.GenericLayout;
import de.esoco.ewt.layout.TableGridLayout;
import de.esoco.ewt.style.AlignedPosition;
import de.esoco.ewt.style.StyleData;

import de.esoco.gwt.client.ServiceRegistry;
import de.esoco.gwt.client.res.EsocoGwtCss;
import de.esoco.gwt.client.res.EsocoGwtResources;
import de.esoco.gwt.shared.Command;
import de.esoco.gwt.shared.CommandService;

import com.google.gwt.user.client.rpc.AsyncCallback;

import static de.esoco.ewt.style.StyleData.WEB_ADDITIONAL_STYLES;

/**
 * The base class for objects that create and manage a panel. A Subclass must
 * implement the method {@link #createContainer(ContainerBuilder, StyleData)} to
 * create and build the managed panel with the desired layout.
 *
 * <p>The generic type parameters allow subclasses to define the types of the
 * panel container (C) they build their contents in and of the parent panel
 * manager (P) they can be associated with. This allows subclasses to access
 * specific methods of these objects through the methods {@link #getPanel()} and
 * {@link #getParent()} methods without the need for type casting.</p>
 *
 * @author eso
 */
public abstract class PanelManager<C extends Container,
	P extends PanelManager<?, ?>>
	extends ContainerManager<C> {

	// ~ Static fields/initializers
	// ---------------------------------------------

	/**
	 * Shortcut constant to access the framework CSS
	 */
	static final EsocoGwtCss CSS = EsocoGwtResources.INSTANCE.css();

	/**
	 * A default style constant for the background panel of toolbars.
	 */
	private static final StyleData TOOLBAR_HORIZONTAL_BACKGROUND_STYLE =
		AlignedPosition.TOP.set(WEB_ADDITIONAL_STYLES,
			CSS.gfToolbarBack() + " " + CSS.horizontal());

	/**
	 * A default style constant for the background panel of toolbars.
	 */
	private static final StyleData TOOLBAR_VERTICAL_BACKGROUND_STYLE =
		AlignedPosition.LEFT.set(WEB_ADDITIONAL_STYLES,
			CSS.gfToolbarBack() + " " + CSS.vertical());

	private static final StyleData HORIZONTAL_TOOLBAR_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES,
			CSS.gfToolbar() + " " + CSS.horizontal());

	private static final StyleData VERTICAL_TOOLBAR_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES,
			CSS.gfToolbar() + " " + CSS.vertical());

	private static final StyleData TOOLBAR_BUTTON_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gfToolButton());

	private static final StyleData TOOLBAR_SEPARATOR_STYLE =
		StyleData.DEFAULT.set(WEB_ADDITIONAL_STYLES, CSS.gfToolSeparator());

	static {
		CSS.ensureInjected();
	}

	// ~ Instance fields
	// --------------------------------------------------------
	private final P rParent;

	private final String sStyleName;

	private int nToolbarColumns;

	private int nNextToolbarColumn = 0;

	private boolean bIsToolbarSeparator = true;

	private boolean bCommandExecuting = false;
	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Creates a child panel manager with a certain parent panel manager.
	 *
	 * @param rParent     The parent panel manager or NULL for a root panel
	 * @param sPanelStyle The panel's style name
	 */
	public PanelManager(P rParent, String sPanelStyle) {
		this.rParent = rParent;
		this.sStyleName = sPanelStyle;
	}
	// ~ Static methods
	// ---------------------------------------------------------

	/**
	 * Helper method to add another additional style name to a style data. NULL
	 * values and empty strings will be ignored.
	 *
	 * @param rStyleData The style data to add the style to
	 * @param sStyles    The style to add (may be NULL or empty)
	 * @return The new style data
	 */
	public static StyleData addStyles(StyleData rStyleData,
		String... sStyles) {
		for (String sAddStyle : sStyles) {
			if (sAddStyle != null && sAddStyle.length() > 0) {
				rStyleData =
					rStyleData.append(WEB_ADDITIONAL_STYLES, sAddStyle);
			}
		}

		return rStyleData;
	}
	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * Builds a new panel in the container of the given container builder.
	 *
	 * @param rBuilder   The container builder to build the panel with
	 * @param rStyleData The style data for the new panel
	 */
	@Override
	public void buildIn(ContainerBuilder<?> rBuilder, StyleData rStyleData) {
		super.buildIn(rBuilder, addStyles(rStyleData, sStyleName));
	}

	/**
	 * Disposes this panel manager. Subclasses can override this method if
	 * necessary to cleanup resource they have acquired. The superclass method
	 * should always be invoked.
	 */
	public void dispose() {
	}

	/**
	 * Returns the panel of this manager.
	 *
	 * @return This manager's panel
	 */
	public C getPanel() {
		return getContainer();
	}

	/**
	 * Returns the parent container in a hierarchy of panels.
	 *
	 * @return The parent container or NULL if this instance has no parent
	 */
	@Override
	public P getParent() {
		return rParent;
	}

	/**
	 * Returns the root panel manager of this instance's hierarchy. If this
	 * instance is already the root of the hierarchy (i.e. it has no parent) it
	 * will be returned directly.
	 *
	 * @return The root panel manager
	 */
	public PanelManager<?, ?> getRootManager() {
		return rParent != null ? rParent.getRootManager() : this;
	}

	/**
	 * Returns the style name of the managed panel.
	 *
	 * @return The style name
	 */
	public final String getStyleName() {
		return sStyleName;
	}

	/**
	 * Returns TRUE if currently a command execution is in progress.
	 *
	 * @return TRUE if a command execution is in progress
	 */
	public final boolean isCommandExecuting() {
		return bCommandExecuting;
	}

	/**
	 * This method can be be overridden by subclasses to update the panel
	 * content from model data if it has changed. The default implementation
	 * invokes {@link Component#repaint()} on the manager's panel.
	 *
	 * <p><b>Attention</b>: Invoking this method will replace any values
	 * entered into input components with the model value stored in the data
	 * elements. Therefore it should only be invoked on display UIs or when the
	 * resetting of input fields is explicitly desired.</p>
	 */
	public void updateUI() {
		getPanel().repaint();
	}

	/**
	 * Create a standard button toolbar panel. The toolbar panel will have the
	 * style {@link EsocoGwtCss#gfToolbar()} and the enclosing (background)
	 * panel will have the argument style.
	 *
	 * @param rBuilder         The builder to build the toolbar with
	 * @param rBackgroundStyle The style of the background or NULL for the
	 *                         default
	 * @param rToolbarStyle    The style of the toolbar or NULL for the default
	 * @param nColumns         The number of columns for a vertical toolbar or
	 *                         zero for a horizontal toolbar
	 * @return The container builder for the toolbar panel
	 */
	protected ContainerBuilder<Panel> addToolbar(ContainerBuilder<?> rBuilder,
		StyleData rBackgroundStyle, StyleData rToolbarStyle, int nColumns) {
		boolean bHorizontal = (nColumns == 0);

		nToolbarColumns = nColumns;

		if (rBackgroundStyle == null) {
			if (bHorizontal) {
				rBackgroundStyle = TOOLBAR_HORIZONTAL_BACKGROUND_STYLE;
			} else {
				rBackgroundStyle = TOOLBAR_VERTICAL_BACKGROUND_STYLE;
			}
		}

		if (rToolbarStyle == null) {
			if (bHorizontal) {
				rToolbarStyle = HORIZONTAL_TOOLBAR_STYLE;
			} else {
				rToolbarStyle = VERTICAL_TOOLBAR_STYLE;
			}
		}

		rBuilder = rBuilder.addPanel(rBackgroundStyle, new FlowLayout(true));

		return rBuilder.addPanel(rToolbarStyle,
			bHorizontal ? new FlowLayout(true) :
			new TableGridLayout(nColumns));
	}

	/**
	 * Create a standard toolbar button. The builder should be one that has
	 * been
	 * returned by
	 * {@link #addToolbar(ContainerBuilder, StyleData, StyleData, int)}. The
	 * button will have the style {@link EsocoGwtCss#gfToolButton()}.
	 *
	 * @param rToolbarBuilder The toolbar panel builder to create the button
	 *                        with
	 * @param rImage          The button image
	 * @param sToolTip        The optional tooltip text or NULL for none
	 * @return The container builder for the toolbar panel
	 */
	protected Button addToolbarButton(ContainerBuilder<Panel> rToolbarBuilder,
		Object rImage, String sToolTip) {
		Button aButton =
			rToolbarBuilder.addButton(TOOLBAR_BUTTON_STYLE, null, rImage);

		aButton.setToolTip(sToolTip);
		bIsToolbarSeparator = false;
		nNextToolbarColumn++;

		if (nNextToolbarColumn >= nToolbarColumns) {
			nNextToolbarColumn = 0;
		}

		return aButton;
	}

	/**
	 * Adds a separator to a toolbar. The builder must be one that had been
	 * returned by
	 * {@link #addToolbar(ContainerBuilder, StyleData, StyleData, int)}. The
	 * button will have the style {@link EsocoGwtCss#gfToolSeparator()}.
	 *
	 * @param rBuilder The toolbar panel builder
	 */
	protected void addToolbarSeparator(ContainerBuilder<Panel> rBuilder) {
		if (!bIsToolbarSeparator || nToolbarColumns == 0) {
			Panel rContainer = rBuilder.getContainer();
			GenericLayout rLayout = rContainer.getLayout();
			int nSeparatorLimit = nToolbarColumns;

			if (nToolbarColumns == 0) {
				nNextToolbarColumn = -1;
			} else if (nNextToolbarColumn > 0) {
				// add a completely empty row
				nSeparatorLimit += nToolbarColumns;
			}

			while (nNextToolbarColumn++ < nSeparatorLimit) {
				rBuilder.addLabel(TOOLBAR_SEPARATOR_STYLE, "", null);

				if (rLayout instanceof TableGridLayout) {
					((TableGridLayout) rLayout).addCellStyle(rContainer,
						CSS.gfToolSeparator());
				}
			}

			nNextToolbarColumn = 0;
			bIsToolbarSeparator = true;
		}
	}

	/**
	 * Displays a message in the message area. Using NULL as the message
	 * argument will clear any previous message. This default implementation
	 * forwards the call to the parent panel manager if such exists. If not
	 * this
	 * method will have no effect. It can either be overridden by a certain
	 * subclass or by a parent panel manager which then handles the message
	 * display.
	 *
	 * @param sMessage     The message to display or NULL to clear the message
	 * @param nDisplayTime The time in milliseconds to display the message or
	 *                     zero for no timeout
	 */
	protected void displayMessage(String sMessage, int nDisplayTime) {
		if (rParent != null) {
			rParent.displayMessage(sMessage, nDisplayTime);
		}
	}

	/**
	 * Executes a command on a {@link CommandService}. The service is queried
	 * through the {@link ServiceRegistry}.
	 *
	 * @param rCommand       The command to execute
	 * @param rData          The data to be processed by the command
	 * @param rResultHandler The result handler to process the command
	 *                          result in
	 *                       case of a successful command execution
	 */
	protected <T extends DataElement<?>, R extends DataElement<?>> void executeCommand(
		final Command<T, R> rCommand, T rData,
		final CommandResultHandler<R> rResultHandler) {
		bCommandExecuting = true;
		ServiceRegistry
			.getCommandService()
			.executeCommand(rCommand, rData, new AsyncCallback<R>() {
				@Override
				public void onFailure(Throwable rCaught) {
					bCommandExecuting = false;
					rResultHandler.handleCommandFailure(rCommand, rCaught);
				}

				@Override
				public void onSuccess(R rResult) {
					bCommandExecuting = false;
					rResultHandler.handleCommandResult(rResult);
				}
			});
	}

	/**
	 * This method must be overridden by subclasses that want to display a
	 * warning if the user tries to close the browser window. The returned
	 * string will then be displayed in the browser's warning message to allow
	 * the user to cancel the window closing. The returned string can be a
	 * resource.
	 *
	 * <p>The default implementation will ask the parent for the close warning
	 * or return NULL if no parent exists. Therefore the typical place to
	 * implement this method is in a root panel manager.</p>
	 *
	 * @return The close warning or NULL if the window can be closed without a
	 * warning
	 */
	protected String getCloseWarning() {
		return rParent != null ? rParent.getCloseWarning() : null;
	}

	/**
	 * handles a command failure. Can be overridden by subclasses for more
	 * specific failure handling.
	 *
	 * @param rCommand The failed command
	 * @param rCaught  The exception that occurred
	 */
	protected void handleCommandFailure(Command<?, ?> rCommand,
		Throwable rCaught) {
		handleError(rCaught);
	}

	/**
	 * Performs the error handling for this manager. The default implementation
	 * forwards this call to the parent panel manager if such exists.
	 *
	 * @param rCaught The exception that caused the error
	 */
	protected void handleError(Throwable rCaught) {
		if (rParent != null) {
			rParent.handleError(rCaught);
		}
	}

	/**
	 * Must be implemented to remove the main panel of an application. This
	 * method can be invoked to reset the application's view. This happens
	 * typically if a user's authentication has expired.
	 *
	 * <p>The default implementation invokes the parent method if a parent
	 * panel manager is set. Therefore the implementation of this method should
	 * be done in the root panel manager of the application.</p>
	 */
	protected void removeApplicationPanel() {
		if (rParent != null) {
			rParent.removeApplicationPanel();
		}
	}
}
