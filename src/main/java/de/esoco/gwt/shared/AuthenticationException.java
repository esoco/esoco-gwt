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

/**
 * A service exception to signal authentication errors.
 *
 * @author eso
 */
public class AuthenticationException extends ServiceException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 *
	 * @param message The error message
	 */
	public AuthenticationException(String message) {
		super(message);
	}

	/**
	 * Creates a new instance that indicates whether a re-authentication of the
	 * user is possible to continue an existing current session. The state can
	 * be queried through the inherited {@link #isRecoverable()} method.
	 *
	 * @param message                  The error message
	 * @param reAuthenticationPossible TRUE if the current user can be
	 *                                 re-authenticated
	 */
	public AuthenticationException(String message,
		boolean reAuthenticationPossible) {
		super(message, reAuthenticationPossible);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param message The error message
	 * @param cause   The causing exception
	 */
	public AuthenticationException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Default constructor for serialization.
	 *
	 * @see ServiceException#ServiceException()
	 */
	AuthenticationException() {
	}
}
