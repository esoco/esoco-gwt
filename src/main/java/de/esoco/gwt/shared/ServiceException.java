//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.gwt.shared;

import de.esoco.data.process.ProcessState;

import java.util.Map;

/**
 * The base class for all exceptions that can be thrown by services.
 *
 * @author eso
 */
public class ServiceException extends Exception {

	private static final long serialVersionUID = 1L;

	private String sCauseMessage;

	private Map<String, String> rErrorParameters;

	private boolean bRecoverable;

	private ProcessState rProcessState = null;

	/**
	 * @see Exception#Exception()
	 */
	public ServiceException() {
	}

	/**
	 * @see Exception#Exception(String)
	 */
	public ServiceException(String sMessage) {
		super(sMessage);
	}

	/**
	 * @see Exception#Exception(Throwable)
	 */
	public ServiceException(Throwable eCause) {
		this(null, eCause);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public ServiceException(String sMessage, Throwable eCause) {
		super(sMessage, eCause);

		do {
			sCauseMessage = eCause.getMessage();
			eCause = eCause.getCause();
		} while (eCause != null);
	}

	/**
	 * Creates a new instance of a recoverable service exception. A recoverable
	 * exception will return TRUE from the {@link #isRecoverable()} method. It
	 * provides additional information about the causing problem by returning a
	 * map containing the causing parameters and a description of the error
	 * from
	 * the method {@link #getErrorParameters()} and optionally an updated
	 * process state from {@link #getProcessState()}.
	 *
	 * @param sMessage         The error message
	 * @param rErrorParameters The list of error
	 * @param rProcessState    An updated process state that reflects parameter
	 *                         updates for the signaled error
	 */
	public ServiceException(String sMessage,
		Map<String, String> rErrorParameters, ProcessState rProcessState) {
		super(sMessage);

		this.rErrorParameters = rErrorParameters;
		this.bRecoverable = true;
		this.rProcessState = rProcessState;
	}

	/**
	 * A constructor for subclasses that need to indicate a recoverable state.
	 *
	 * @see #ServiceException(String)
	 */
	protected ServiceException(String sMessage, boolean bRecoverable) {
		this(sMessage);

		this.bRecoverable = bRecoverable;
	}

	/**
	 * Returns the message of the root cause exception of this instance.
	 * This is
	 * needed for clients because the chain of cause messages is lost on GWT
	 * serialization.
	 *
	 * @return The causing exception's message or NULL for none
	 */
	public final String getCauseMessage() {
		return sCauseMessage;
	}

	/**
	 * Returns the optional parameters of a recoverable exception. The
	 * result is
	 * a map of strings that contains the erroneous parameters as the keys and
	 * the associated error message as the values.
	 *
	 * @return A map from erroneous parameters to error messages (NULL for
	 * none)
	 */
	public Map<String, String> getErrorParameters() {
		return rErrorParameters;
	}

	/**
	 * Returns an optional process state that is associated with this
	 * exception.
	 *
	 * @return The process state or NULL for none
	 */
	public ProcessState getProcessState() {
		return rProcessState;
	}

	/**
	 * Indicates whether this exception is recoverable.
	 *
	 * @return TRUE if this
	 */
	public boolean isRecoverable() {
		return bRecoverable;
	}
}
