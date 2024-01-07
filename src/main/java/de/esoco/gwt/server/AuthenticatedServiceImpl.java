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

import de.esoco.data.DataRelationTypes;
import de.esoco.data.DownloadData;
import de.esoco.data.SessionContext;
import de.esoco.data.SessionData;
import de.esoco.data.SessionManager;
import de.esoco.data.UploadHandler;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.StringDataElement;

import de.esoco.entity.Entity;
import de.esoco.entity.EntityFunctions;
import de.esoco.entity.EntityManager;
import de.esoco.entity.ExtraAttributes;

import de.esoco.gwt.shared.AuthenticatedService;
import de.esoco.gwt.shared.AuthenticationException;
import de.esoco.gwt.shared.Command;
import de.esoco.gwt.shared.ServiceException;

import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogAspect;
import de.esoco.lib.net.AuthorizationCallback;
import de.esoco.lib.net.ExternalService;
import de.esoco.lib.net.ExternalService.AccessType;
import de.esoco.lib.net.ExternalServiceAccess;
import de.esoco.lib.net.ExternalServiceDefinition;
import de.esoco.lib.net.ExternalServiceRequest;
import de.esoco.lib.property.HasProperties;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.obrel.core.ProvidesConfiguration;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static de.esoco.data.SessionData.SESSION_START_TIME;

import static org.obrel.core.RelationTypes.newMapType;
import static org.obrel.core.RelationTypes.newSetType;

/**
 * Implementation of {@link AuthenticatedService}. The generic parameter defines
 * the entity type that is used to perform the authentication on (typically some
 * type of person entity).
 *
 * @author eso
 */
public abstract class AuthenticatedServiceImpl<E extends Entity>
	extends CommandServiceImpl
	implements AuthenticatedService, SessionManager, ExternalServiceAccess {

	/**
	 * An extra attribute that defines the timeout for a user's authentication.
	 */
	public static final RelationType<Integer> AUTHENTICATION_TIMEOUT =
		ExtraAttributes.newExtraAttribute();

	static final RelationType<Map<String, UploadHandler>> SESSION_UPLOADS =
		newMapType(false);

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_UPLOAD_URL = "upload";

	private static final String DEFAULT_DOWNLOAD_URL = "srv/download/";

	private static final String DEFAULT_OAUTH_CALLBACK_URL = "/oauth";

	private static final String ATTR_SESSION_CONTEXT = "ATTR_SESSION_CONTEXT";

	private static final RelationType<AuthorizationCallback>
		AUTHORIZATION_CALLBACK = RelationTypes.newType();

	private static final RelationType<Map<String, DownloadData>>
		SESSION_DOWNLOADS = newMapType(false);

	private static final RelationType<Set<ExternalService>> EXTERNAL_SERVICES =
		newSetType(true);

	private static int nNextUploadId = 1;

	static {
		RelationTypes.init(AuthenticatedServiceImpl.class);
	}

	/**
	 * Returns the session data structures for all registered clients.
	 *
	 * @param rServletContext The servlet context to return the sessions for
	 * @return The client sessions
	 */
	protected static Collection<SessionData> getClientSessions(
		ServletContext rServletContext) {
		return getSessionContext(rServletContext)
			.get(SessionData.USER_SESSIONS)
			.values();
	}

	/**
	 * Returns the session context from a certain {@link ServletContext}. If no
	 * session context exists yet it will be created.
	 *
	 * @param rServletContext The servlet context
	 * @return The session context
	 */
	static SessionContext getSessionContext(ServletContext rServletContext) {
		SessionContext rSessionContext =
			(SessionContext) rServletContext.getAttribute(ATTR_SESSION_CONTEXT);

		if (rSessionContext == null) {
			rSessionContext = new SessionContext();
			rServletContext.setAttribute(ATTR_SESSION_CONTEXT,
				rSessionContext);
		}

		return rSessionContext;
	}

	/**
	 * Returns the session data for a request. This method first checks whether
	 * the session is properly authenticated and throws an exception otherwise.
	 *
	 * @param rRequest             The session to return the session data for
	 * @param bCheckAuthentication TRUE to throw an exception if no user is
	 *                             authenticated for the current session
	 * @return The session data object
	 * @throws AuthenticationException If the session is not authenticated
	 */
	static SessionData getSessionData(HttpServletRequest rRequest,
		boolean bCheckAuthentication) throws AuthenticationException {
		String sSessionId = rRequest.getSession().getId();

		Map<String, SessionData> rSessionMap =
			getSessionMap(rRequest.getServletContext());

		SessionData rSessionData = rSessionMap.get(sSessionId);

		if (bCheckAuthentication && rSessionData == null) {
			throw new AuthenticationException("UserNotAuthenticated");
		}

		return rSessionData;
	}

	/**
	 * Returns the mapping from user names to {@link SessionData} objects.
	 *
	 * @param rServletContext The servlet context to read the map from
	 * @return The session map
	 */
	static Map<String, SessionData> getSessionMap(
		ServletContext rServletContext) {
		return getSessionContext(rServletContext).get(
			SessionData.USER_SESSIONS);
	}

	/**
	 * Sets an error message and status code in a servlet response.
	 *
	 * @param rResponse   The response object
	 * @param nStatusCode The status code
	 * @param sMessage    The error message
	 * @throws IOException If writing to the output stream fails
	 */
	static void setErrorResponse(HttpServletResponse rResponse,
		int nStatusCode,
		String sMessage) throws IOException {
		rResponse.setStatus(nStatusCode);

		ServletOutputStream rOut = rResponse.getOutputStream();

		rOut.print(sMessage);
		rOut.close();
	}

	@Override
	public String authorizeExternalServiceAccess(
		ExternalServiceDefinition rServiceDefinition,
		AuthorizationCallback rCallback, boolean bForceAuth,
		Object... rAccessScopes) throws Exception {
		ExternalService aService =
			ExternalService.create(rServiceDefinition, getUser(),
				getServiceConfiguration());

		String sCallbackUrl = getBaseUrl() + DEFAULT_OAUTH_CALLBACK_URL;

		Object rAuth =
			aService.authorizeAccess(sCallbackUrl, bForceAuth, rAccessScopes);

		String sRequestUrl = null;

		if (rAuth instanceof URL) {
			aService.set(AUTHORIZATION_CALLBACK, rCallback);
			getSessionContext().get(EXTERNAL_SERVICES).add(aService);
			sRequestUrl = rAuth.toString();
		} else if (rAuth instanceof String) {
			rCallback.authorizationSuccess(rAuth.toString());
		} else {
			throw new UnsupportedOperationException(
				"Unsupported service result: " + rAuth);
		}

		return sRequestUrl;
	}

	@Override
	public ExternalServiceRequest createExternalServiceRequest(
		ExternalServiceDefinition rServiceDefinition, AccessType eAccessType,
		String sRequestUrl) throws Exception {
		return ExternalService
			.create(rServiceDefinition, getUser(), getServiceConfiguration())
			.createRequest(eAccessType, sRequestUrl);
	}

	/**
	 * Overridden to logout and cleanup all sessions on shutdown.
	 */
	@Override
	public void destroy() {
		Collection<SessionData> rSessions =
			getClientSessions(getServletContext());

		for (SessionData rSessionData : rSessions) {
			try {
				endSession(rSessionData);
			} catch (Exception e) {
				Log.warnf(e, "Logout of session failed: %s", rSessionData);
			}
		}

		Log.info("Session cleanup finished");

		super.destroy();
	}

	/**
	 * Returns the base URL of this service based on the current request.
	 *
	 * @return The base URL of this service
	 */
	public String getBaseUrl() {
		HttpServletRequest rRequest = getThreadLocalRequest();
		StringBuilder aUrl = new StringBuilder(rRequest.getScheme());

		aUrl.append("://");
		aUrl.append(rRequest.getServerName());

		if (rRequest.getServerPort() != 80 && rRequest.getServerPort() != 443) {
			aUrl.append(':');
			aUrl.append(rRequest.getServerPort());
		}

		aUrl.append(rRequest.getContextPath());
		aUrl.append(rRequest.getServletPath());

		return aUrl.toString();
	}

	/**
	 * Returns the {@link SessionData} for the current session or NULL if no
	 * user is authenticated for the current request. Other than
	 * {@link #getSessionData()} this method will not throw an authentication
	 * exception if no user is authenticated but return NULL instead.
	 *
	 * @return The session data if a user is authenticated for the current
	 * request or NULL for none
	 */
	public SessionData getCurrentSession() {
		try {
			return getSessionData();
		} catch (AuthenticationException e) {
			return null;
		}
	}

	/**
	 * @see SessionManager#getSessionContext()
	 */
	@Override
	public SessionContext getSessionContext() {
		return getSessionContext(getServletContext());
	}

	/**
	 * Returns the session data for the current user. This method first checks
	 * whether the current session is properly authenticated and throws an
	 * exception otherwise.
	 *
	 * @throws AuthenticationException If the current user is not authenticated
	 * @see SessionManager#getSessionData()
	 */
	@Override
	public SessionData getSessionData() throws AuthenticationException {
		return getSessionData(true);
	}

	/**
	 * @see SessionManager#getSessionId()
	 */
	@Override
	public String getSessionId() {
		HttpServletRequest rRequest = getThreadLocalRequest();
		String sId = null;

		if (rRequest != null) {
			HttpSession rSession = rRequest.getSession();

			if (rSession != null) {
				sId = rSession.getId();
			}
		}

		return sId;
	}

	/**
	 * @see SessionManager#getSessions()
	 */
	@Override
	public Collection<SessionData> getSessions() throws Exception {
		return Collections.unmodifiableCollection(
			getSessionMap(getServletContext()).values());
	}

	/**
	 * Handles the {@link AuthenticatedService#CHANGE_PASSWORD} command.
	 *
	 * @param rPasswordChangeRequest A data element containing the password
	 *                               change request
	 * @throws ServiceException        If the authentication cannot be
	 *                                 processed
	 * @throws AuthenticationException If the authentication fails
	 * @throws Exception               If the password change fails
	 */
	public void handleChangePassword(StringDataElement rPasswordChangeRequest)
		throws Exception {
		changePassword(rPasswordChangeRequest);
	}

	/**
	 * Handles the {@link AuthenticatedService#GET_USER_DATA} command.
	 *
	 * @param rIgnored Not used, should always be NULL
	 * @return A data element list containing the user data if the user is
	 * logged in
	 * @throws AuthenticationException If the user is not logged in
	 */
	public DataElementList handleGetUserData(Object rIgnored)
		throws AuthenticationException {
		SessionData rSessionData = getSessionData();

		resetSessionData(rSessionData);

		return getUserData();
	}

	/**
	 * Handles the {@link AuthenticatedService#LOGIN} command.
	 *
	 * @param rLoginData A string data element containing the login credentials
	 * @return A data element list containing the user data if the
	 * authentication was successful
	 * @throws ServiceException        If the authentication cannot be
	 *                                 processed
	 * @throws AuthenticationException If the authentication fails
	 */
	public DataElementList handleLogin(StringDataElement rLoginData)
		throws AuthenticationException, ServiceException {
		return loginUser(rLoginData,
			rLoginData.getProperty(LOGIN_USER_INFO, ""));
	}

	/**
	 * Handles the {@link AuthenticatedService#LOGOUT} command.
	 *
	 * @param rIgnored Not used, should always be NULL
	 */
	public void handleLogout(DataElement<?> rIgnored) {
		logoutCurrentUser();
	}

	/**
	 * Invokes {@link EntityManager#setSessionManager(SessionManager)} and
	 * {@link ServiceContext#setService(AuthenticatedServiceImpl)}.
	 *
	 * @throws ServletException On errors
	 */
	@Override
	public void init() throws ServletException {
		EntityManager.setSessionManager(this);

		ServiceContext rContext = ServiceContext.getInstance();

		if (rContext != null) {
			rContext.setService(this);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public DataElementList loginUser(StringDataElement rLoginData,
		String sClientInfo) throws AuthenticationException, ServiceException {
		String sLoginName = rLoginData.getName();
		boolean bReLogin = rLoginData.getValue() == null;
		E rUser;

		if (bReLogin) {
			SessionData rSessionData = getSessionData();

			endSession(rSessionData);

			if (rSessionData
				.get(SessionData.SESSION_LOGIN_NAME)
				.equals(sLoginName)) {
				rUser = (E) rSessionData.get(SessionData.SESSION_USER);
			} else {
				throw new AuthenticationException("ReLoginNotPossible");
			}
		} else {
			rUser = authenticate(rLoginData);
			rLoginData.setValue(null);
		}

		if (rUser == null) {
			throw new AuthenticationException(
				"Invalid password for " + sLoginName);
		} else {
			HttpServletRequest rRequest = getThreadLocalRequest();

			if (!bReLogin) {
				String sClientAddr = rRequest.getRemoteAddr();
				String sForwardAddr = rRequest.getHeader("X-Forwarded-For");

				if (sForwardAddr != null && sForwardAddr.length() > 0 &&
					!sForwardAddr.equals(sClientAddr)) {
					sClientAddr = sForwardAddr;
				}

				Log.infof("[LOGIN] User %s from %s authenticated in %s\n%s",
					rUser, sClientAddr, getApplicationName(), sClientInfo);
			}

			authorizeUser(rUser, rLoginData);

			Map<String, SessionData> rSessionMap =
				getSessionMap(getServletContext());

			HttpSession rSession = rRequest.getSession();
			String sSessionId = rSession.getId();
			SessionData rSessionData = rSessionMap.get(sSessionId);
			DataElementList aUserData = null;

			String sPreviousSessionId =
				rLoginData.getProperty(SESSION_ID, null);

			if (sPreviousSessionId != null) {
				SessionData rPreviousSessionData =
					rSessionMap.remove(sPreviousSessionId);

				if (rSessionData == null && rPreviousSessionData != null) {
					rSessionData = rPreviousSessionData;
				}

				rSessionMap.put(sSessionId, rSessionData);
			}

			rSession.setAttribute(LOGIN_NAME, sLoginName);

			if (rSessionData == null) {
				rSessionData = createSessionData();
			} else {
				aUserData = rSessionData.get(SessionData.SESSION_USER_DATA);
			}

			if (aUserData == null) {
				aUserData = new DataElementList("UserData", null);
			}

			rSessionData.update(rUser, sLoginName, aUserData);
			initUserData(aUserData, rUser, sLoginName);

			return aUserData;
		}
	}

	@Override
	public void logoutCurrentUser() {
		removeSession(getThreadLocalRequest().getSession());
	}

	/**
	 * @see SessionManager#prepareDownload(DownloadData)
	 */
	@Override
	public String prepareDownload(DownloadData rData) throws Exception {
		String sUrl = DEFAULT_DOWNLOAD_URL + rData.getFileName();

		getSessionData().get(SESSION_DOWNLOADS).put(sUrl, rData);

		return sUrl;
	}

	@Override
	public String prepareUpload(UploadHandler rUploadHandler)
		throws AuthenticationException {
		String sUploadId = Integer.toString(nNextUploadId++);
		String sUploadUrl = DEFAULT_UPLOAD_URL + "?id=" + sUploadId;

		getSessionData().get(SESSION_UPLOADS).put(sUploadId, rUploadHandler);

		return sUploadUrl;
	}

	@Override
	public void removeDownload(String sUrl) {
		try {
			getSessionData().get(SESSION_DOWNLOADS).remove(sUrl);
		} catch (AuthenticationException e) {
			Log.warn("Removing download failed", e);
		}
	}

	/**
	 * Removes a session from the context of this service.
	 *
	 * @param rSession The session to remove
	 */
	public void removeSession(HttpSession rSession) {
		Map<String, SessionData> rSessionMap =
			getSessionMap(getServletContext());

		String sSessionId = rSession.getId();
		SessionData rSessionData = rSessionMap.get(sSessionId);

		if (rSessionData != null) {
			endSession(rSessionData);
			rSessionMap.remove(sSessionId);
		}

		rSession.removeAttribute(LOGIN_NAME);
	}

	@Override
	public void removeUpload(String sUrl) {
		String sId = sUrl.substring(sUrl.indexOf("id=") + 3);

		try {
			getSessionData().get(SESSION_UPLOADS).remove(sId);
		} catch (AuthenticationException e) {
			Log.warn("Removing upload failed", e);
		}
	}

	@Override
	public void revokeExternalServiceAccess(
		ExternalServiceDefinition rServiceDefinition) throws Exception {
		ExternalService aService =
			ExternalService.create(rServiceDefinition, getUser(),
				getServiceConfiguration());

		aService.revokeAccess();
	}

	/**
	 * Adds a log aspect to the logging framework after injecting the session
	 * manager (i.e. _this_ instance) into it.
	 *
	 * @param rLogAspect The log aspect to add
	 */
	protected void addLogAspect(LogAspect<?> rLogAspect) {
		rLogAspect.set(DataRelationTypes.SESSION_MANAGER, this);
		Log.addLogAspect(rLogAspect);
	}

	/**
	 * Must be implemented by subclasses to perform the authentication of a
	 * user. The returned object must be the authenticated entity that
	 * corresponds to the given credentials if the authentication was
	 * successful. The input is a string data element with the login name as
	 * it's name and the password as the value.
	 *
	 * @param rLoginData A string data element containing the login credentials
	 * @return The entity of the authenticated person if the authentication was
	 * successful
	 * @throws AuthenticationException If the authentication fails
	 */
	protected abstract E authenticate(StringDataElement rLoginData)
		throws AuthenticationException;

	/**
	 * Performs the authorization of a user after authentication. What exactly
	 * this means depends on the subclass implementation and is application
	 * dependent. This default implementation does nothing.
	 *
	 * @param rUser      The authenticated user
	 * @param rLoginData Additional properties from the login data if available
	 * @throws AuthenticationException If the authorization fails
	 */
	protected void authorizeUser(E rUser, HasProperties rLoginData)
		throws AuthenticationException {
	}

	/**
	 * Can be implemented by subclasses to support password changes for the
	 * current user. The string data element parameter contains the old
	 * password
	 * as it's name and the new password as it's value. The implementation must
	 * not assume the old password to be correct but instead it MUST always
	 * validate it before performing the password update. The default
	 * implementation does nothing.
	 *
	 * @param rPasswordChangeRequest A data element containing the password
	 *                               change request
	 * @throws Exception Any exception may be thrown if the password change
	 *                   fails
	 */
	protected void changePassword(StringDataElement rPasswordChangeRequest)
		throws Exception {
	}

	/**
	 * Overridden to only allow authentication commands if not authenticated.
	 *
	 * @see CommandServiceImpl#checkCommandExecution(Command, DataElement)
	 */
	@Override
	protected <T extends DataElement<?>> void checkCommandExecution(
		Command<T, ?> rCommand, T rData) throws ServiceException {
		if (!(LOGIN.equals(rCommand) || LOGOUT.equals(rCommand))) {
			// if not performing a login or logout throw an exception if the
			// user is not authenticated
			getSessionData();
		}
	}

	/**
	 * Creates a new session data object and stores it in the current request's
	 * session.
	 *
	 * @return The new session data instance
	 */
	protected SessionData createSessionData() {
		String sSessionId = getThreadLocalRequest().getSession().getId();
		SessionData rSessionData = new SessionData();

		getSessionMap(getServletContext()).put(sSessionId, rSessionData);

		return rSessionData;
	}

	/**
	 * Overridden to implement authenticated download functionality.
	 *
	 * @param rRequest  The request
	 * @param rResponse The response
	 * @throws ServletException On servlet errors
	 * @throws IOException      On I/O errors
	 */
	@Override
	protected void doGet(HttpServletRequest rRequest,
		HttpServletResponse rResponse) throws ServletException, IOException {
		SessionData rSessionData = getSessionMap(getServletContext()).get(
			rRequest.getSession().getId());

		if (rSessionData == null) {
			setErrorResponse(rResponse, HttpServletResponse.SC_UNAUTHORIZED,
				"User not authorized");
		} else if (!processDownloadRequest(rRequest, rResponse,
			rSessionData) &&
			!processExternalServiceResponse(rRequest, rResponse,
				rSessionData)) {
			super.doGet(rRequest, rResponse);
		}
	}

	/**
	 * This method can be overridden by subclasses that need to perform cleanup
	 * operations if a session is no longer needed. The superclass method
	 * should
	 * always be invoked after a subclass has performed it's cleanup.
	 *
	 * @param rSessionData The session data for the session that is logged out
	 */
	protected void endSession(SessionData rSessionData) {
	}

	/**
	 * This method must be implemented by subclasses to return a configuration
	 * object for this service. This must be an implementation of the interface
	 * {@link ProvidesConfiguration}.
	 *
	 * @return The service configuration entity
	 */
	protected abstract ProvidesConfiguration getServiceConfiguration();

	/**
	 * Returns the user entity from the session data.
	 *
	 * @return The user entity
	 * @throws AuthenticationException If the user is not or no longer
	 *                                 authenticated
	 */
	protected Entity getUser() throws AuthenticationException {
		return getSessionData().get(SessionData.SESSION_USER);
	}

	/**
	 * Returns the user data element from the session data.
	 *
	 * @return The data element list containing the user data
	 * @throws AuthenticationException If the user is not or no longer
	 *                                 authenticated
	 */
	protected DataElementList getUserData() throws AuthenticationException {
		return getSessionData().get(SessionData.SESSION_USER_DATA);
	}

	/**
	 * This method can be overridden by subclasses that need to provide
	 * additional informations in the user data that is sent back to the
	 * client.
	 * Subclasses must always invoke the superclass method to inherit the
	 * standard user data.
	 *
	 * <p>Because it is possible for clients to re-login with different
	 * parameters implementations should always expect the user data element to
	 * already contain data and therefore to not always add values but to
	 * update
	 * them if necessary.</p>
	 *
	 * @param rUserData  The list of data elements for the current user
	 * @param rUser      The entity for the user to initialize the data from
	 * @param sLoginName The login name of the current user
	 * @throws ServiceException If initializing the data fails
	 */
	protected void initUserData(DataElementList rUserData, E rUser,
		String sLoginName) throws ServiceException {
		HttpServletRequest rRequest = getThreadLocalRequest();

		rUserData.set(LOGIN_NAME, sLoginName);
		rUserData.set(USER_NAME, EntityFunctions.format(rUser));
		rUserData.setProperty(SESSION_ID, rRequest.getSession().getId());
	}

	/**
	 * Checks for and if necessary processes a download GET request.
	 *
	 * @param rRequest     sUrl The request URL
	 * @param rResponse    The servlet response
	 * @param rSessionData The session data for the current user
	 * @return TRUE if a download request has been detected and processed
	 * @throws IOException      If an IO operation fails
	 * @throws ServletException If handling the request fails
	 */
	protected boolean processDownloadRequest(HttpServletRequest rRequest,
		HttpServletResponse rResponse, SessionData rSessionData) {
		String sUrl = rRequest.getRequestURI();
		boolean bIsDownloadRequest = false;

		if (sUrl != null) {
			Map<String, DownloadData> rSessionDownloads =
				rSessionData.get(SESSION_DOWNLOADS);

			sUrl = getDownloadUrl(sUrl);

			DownloadData rDownloadData = rSessionDownloads.get(sUrl);

			if (rDownloadData != null) {
				try {
					bIsDownloadRequest = true;
					addResponseHeader(rResponse, rDownloadData);
					rResponse.setCharacterEncoding("UTF-8");
					rResponse.setContentType(rDownloadData
						.getFileType()
						.getMimeType()
						.getDefinition());

					// this allows to use Window.Location.assign() for the
					// download URL without actually replacing the window URL
					rResponse.setHeader("Content-Disposition", "attachment");

					writeDownloadDataToResponse(rResponse, rDownloadData);
				} catch (Throwable e) {
					Log.error("Processing of download request failed", e);
				} finally {
					if (rDownloadData.isRemoveAfterDownload()) {
						rSessionDownloads.remove(sUrl);
					}
				}
			}
		}

		return bIsDownloadRequest;
	}

	/**
	 * Checks for and if necessary processes a download GET request.
	 *
	 * @param rRequest     sUrl The request URL
	 * @param rResponse    The servlet response
	 * @param rSessionData The session data for the current user
	 * @return TRUE if a download request has been detected and processed
	 * @throws ServletException If handling the response fails
	 */
	protected boolean processExternalServiceResponse(
		HttpServletRequest rRequest, HttpServletResponse rResponse,
		SessionData rSessionData) throws ServletException {
		String sUrl = rRequest.getRequestURI();

		boolean bIsOAuthResponse =
			(sUrl != null && sUrl.indexOf(DEFAULT_OAUTH_CALLBACK_URL) >= 0);

		if (bIsOAuthResponse) {
			Iterator<ExternalService> rServices =
				getSessionContext().get(EXTERNAL_SERVICES).iterator();

			ExternalService rService = null;

			while (rServices.hasNext()) {
				ExternalService rCheckService = rServices.next();
				String sRequestId =
					rRequest.getParameter(rCheckService.getRequestIdParam());

				if (sRequestId != null &&
					sRequestId.equals(rCheckService.getServiceId())) {
					rService = rCheckService;
					rServices.remove();

					break;
				}
			}

			if (rService != null) {
				AuthorizationCallback rCallback =
					rService.get(AUTHORIZATION_CALLBACK);

				try {
					String sCode = rRequest.getParameter(
						rService.getCallbackCodeRequestParam());

					String sAccessToken = rService.processCallback(sCode);
					String sResponse = String.format(
						"<h2>Server-Freigabe erhalten</h2>" +
							"<p>Der Server hat den Zugriff authorisiert. " +
							"Bitte kehren Sie zur vorherigen Seite zurück, " +
							"um auf die Server-Daten zuzugreifen.</p>");

					rCallback.authorizationSuccess(sAccessToken);
					rResponse.getWriter().println(sResponse);
				} catch (Exception e) {
					try {
						rResponse
							.getWriter()
							.println("Error: " + e.getMessage());
					} catch (IOException eIO) {
						Log.error("Response access error", eIO);
					}

					Log.error("External service response processing failed",
						e);
					rCallback.authorizationFailure(e);
				}
			} else {
				throw new ServletException(
					"No external service for '" + rRequest + "'");
			}
		}

		return bIsOAuthResponse;
	}

	/**
	 * Resets an existing session data for re-use. This method will be invoked
	 * if a user connects again to a session, e.g. after closing the browser
	 * window. Subclasses can then reset the session data, like removing stale
	 * data that cannot be used again.
	 *
	 * <p>Subclasses should always invoke the superclass implementation.</p>
	 *
	 * @param rSessionData The session data to reset
	 */
	protected void resetSessionData(SessionData rSessionData) {
	}

	/**
	 * Internal method to query the {@link SessionData} for the session of the
	 * current request.
	 *
	 * @param bCheckAuthentication TRUE to throw an exception if no user is
	 *                             authenticated for the current session
	 * @return The session data (may be NULL if no session is available and the
	 * check authentication parameter is FALSE)
	 * @throws AuthenticationException If no session data is available and the
	 *                                 check authentication parameter is TRUE
	 */
	SessionData getSessionData(boolean bCheckAuthentication)
		throws AuthenticationException {
		SessionData rSessionData =
			getSessionData(getThreadLocalRequest(), bCheckAuthentication);

		if (rSessionData != null && bCheckAuthentication) {
			checkAuthenticationTimeout(rSessionData);
		}

		return rSessionData;
	}

	/**
	 * adds the header to the {@link HttpServletResponse} based on information
	 * taken from rDownloadData.
	 */
	private void addResponseHeader(HttpServletResponse rResponse,
		DownloadData rDownloadData) {
		String sHeader = String.format("attachment;filename=\"%s\"",
			rDownloadData.getFileName());

		rResponse.addHeader("Content-Disposition", sHeader);
	}

	/**
	 * Checks whether the given session has reached the authentication timeout
	 * of this application. The timeout must be set in the service
	 * configuration
	 * returned by {@link #getServiceConfiguration()} in an extra attribute
	 * with
	 * the type {@link #AUTHENTICATION_TIMEOUT}. If not set it defaults to zero
	 * which disables the timeout.
	 *
	 * @param rSessionData The session to check for the timeout
	 * @throws AuthenticationException If the session timeout has been reached
	 */
	@SuppressWarnings("boxing")
	private void checkAuthenticationTimeout(SessionData rSessionData)
		throws AuthenticationException {
		int nAuthenticationTimeout;

		nAuthenticationTimeout =
			getServiceConfiguration().getConfigValue(AUTHENTICATION_TIMEOUT,
				0);

		if (nAuthenticationTimeout > 0) {
			long nSessionTime = rSessionData.get(SESSION_START_TIME).getTime();

			nSessionTime = (System.currentTimeMillis() - nSessionTime) / 1000;

			if (nSessionTime > nAuthenticationTimeout) {
				throw new AuthenticationException("UserSessionExpired", true);
			}
		}
	}

	/**
	 * Returns the download URL part of a certain URL string.
	 *
	 * @param sUrl The full URL string
	 * @return The download URL part
	 */
	private String getDownloadUrl(String sUrl) {
		int nStart = sUrl.indexOf(DEFAULT_DOWNLOAD_URL);

		if (nStart > 0) {
			sUrl = sUrl.substring(nStart);
		}

		return sUrl;
	}

	/**
	 * Returns true if the given contenType is knwown to be character based.
	 *
	 * @param rContentType The character based data
	 * @return The character based data
	 */
	private boolean isCharacterBasedData(String rContentType) {
		Pattern aCaseInsensitivePattern =
			Pattern.compile(".*(?i)text.*", Pattern.CASE_INSENSITIVE);

		return aCaseInsensitivePattern.matcher(rContentType).matches();
	}

	/**
	 * Uses a {@link ServletOutputStream} to write the data of an HTTP servlet
	 * response.
	 *
	 * @param rResponse The response object
	 * @param rData     The response data
	 */
	private void writeBinaryOutput(HttpServletResponse rResponse, Object rData)
		throws IOException {
		ServletOutputStream rOut = rResponse.getOutputStream();

		// TODO: support additional data formats
		if (rData instanceof byte[]) {
			byte[] rBytes = (byte[]) rData;

			rOut.write(rBytes, 0, rBytes.length);
		} else {
			rOut.print(rData.toString());
		}

		rOut.flush();
		rOut.close();
	}

	/**
	 * Uses a {@link PrintWriter} to write output to the
	 * {@link HttpServletResponse}
	 */
	private void writeCharacterBasedOutput(HttpServletResponse rResponse,
		String sContent) throws IOException {
		PrintWriter aPrintWriter = rResponse.getWriter();

		aPrintWriter.print(sContent);
		aPrintWriter.close();
	}

	/**
	 * Finds out whether the data to write is binary or character-based and
	 * uses
	 * the appropriate method the write the data to the
	 * {@link HttpServletResponse}.
	 */
	private void writeDownloadDataToResponse(HttpServletResponse rResponse,
		DownloadData rDownloadData) throws IOException {
		String sContentType =
			rDownloadData.getFileType().getMimeType().getDefinition();
		Object rData = rDownloadData.createData();

		if (rData != null) {
			if (isCharacterBasedData(sContentType)) {
				writeCharacterBasedOutput(rResponse, rData.toString());
			} else {
				writeBinaryOutput(rResponse, rData);
			}
		}
	}
}
