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
import org.obrel.core.ProvidesConfiguration;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

	private static int nextUploadId = 1;

	static {
		RelationTypes.init(AuthenticatedServiceImpl.class);
	}

	/**
	 * Returns the session data structures for all registered clients.
	 *
	 * @param servletContext The servlet context to return the sessions for
	 * @return The client sessions
	 */
	protected static Collection<SessionData> getClientSessions(
		ServletContext servletContext) {
		return getSessionContext(servletContext)
			.get(SessionData.USER_SESSIONS)
			.values();
	}

	/**
	 * Returns the session context from a certain {@link ServletContext}. If no
	 * session context exists yet it will be created.
	 *
	 * @param servletContext The servlet context
	 * @return The session context
	 */
	static SessionContext getSessionContext(ServletContext servletContext) {
		SessionContext sessionContext =
			(SessionContext) servletContext.getAttribute(ATTR_SESSION_CONTEXT);

		if (sessionContext == null) {
			sessionContext = new SessionContext();
			servletContext.setAttribute(ATTR_SESSION_CONTEXT, sessionContext);
		}

		return sessionContext;
	}

	/**
	 * Returns the session data for a request. This method first checks whether
	 * the session is properly authenticated and throws an exception otherwise.
	 *
	 * @param request             The session to return the session data for
	 * @param checkAuthentication TRUE to throw an exception if no user is
	 *                            authenticated for the current session
	 * @return The session data object
	 * @throws AuthenticationException If the session is not authenticated
	 */
	static SessionData getSessionData(HttpServletRequest request,
		boolean checkAuthentication) throws AuthenticationException {
		String sessionId = request.getSession().getId();

		Map<String, SessionData> sessionMap =
			getSessionMap(request.getServletContext());

		SessionData sessionData = sessionMap.get(sessionId);

		if (checkAuthentication && sessionData == null) {
			throw new AuthenticationException("UserNotAuthenticated");
		}

		return sessionData;
	}

	/**
	 * Returns the mapping from user names to {@link SessionData} objects.
	 *
	 * @param servletContext The servlet context to read the map from
	 * @return The session map
	 */
	static Map<String, SessionData> getSessionMap(
		ServletContext servletContext) {
		return getSessionContext(servletContext).get(SessionData.USER_SESSIONS);
	}

	/**
	 * Sets an error message and status code in a servlet response.
	 *
	 * @param response   The response object
	 * @param statusCode The status code
	 * @param message    The error message
	 * @throws IOException If writing to the output stream fails
	 */
	static void setErrorResponse(HttpServletResponse response, int statusCode,
		String message) throws IOException {
		response.setStatus(statusCode);

		ServletOutputStream out = response.getOutputStream();

		out.print(message);
		out.close();
	}

	@Override
	public String authorizeExternalServiceAccess(
		ExternalServiceDefinition serviceDefinition,
		AuthorizationCallback callback, boolean forceAuth,
		Object... accessScopes) throws Exception {
		ExternalService service =
			ExternalService.create(serviceDefinition, getUser(),
				getServiceConfiguration());

		String callbackUrl = getBaseUrl() + DEFAULT_OAUTH_CALLBACK_URL;

		Object auth =
			service.authorizeAccess(callbackUrl, forceAuth, accessScopes);

		String requestUrl = null;

		if (auth instanceof URL) {
			service.set(AUTHORIZATION_CALLBACK, callback);
			getSessionContext().get(EXTERNAL_SERVICES).add(service);
			requestUrl = auth.toString();
		} else if (auth instanceof String) {
			callback.authorizationSuccess(auth.toString());
		} else {
			throw new UnsupportedOperationException(
				"Unsupported service result: " + auth);
		}

		return requestUrl;
	}

	@Override
	public ExternalServiceRequest createExternalServiceRequest(
		ExternalServiceDefinition serviceDefinition, AccessType accessType,
		String requestUrl) throws Exception {
		return ExternalService
			.create(serviceDefinition, getUser(), getServiceConfiguration())
			.createRequest(accessType, requestUrl);
	}

	/**
	 * Overridden to logout and cleanup all sessions on shutdown.
	 */
	@Override
	public void destroy() {
		Collection<SessionData> sessions =
			getClientSessions(getServletContext());

		for (SessionData sessionData : sessions) {
			try {
				endSession(sessionData);
			} catch (Exception e) {
				Log.warnf(e, "Logout of session failed: %s", sessionData);
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
		HttpServletRequest request = getThreadLocalRequest();
		StringBuilder url = new StringBuilder(request.getScheme());

		url.append("://");
		url.append(request.getServerName());

		if (request.getServerPort() != 80 && request.getServerPort() != 443) {
			url.append(':');
			url.append(request.getServerPort());
		}

		url.append(request.getContextPath());
		url.append(request.getServletPath());

		return url.toString();
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
		HttpServletRequest request = getThreadLocalRequest();
		String id = null;

		if (request != null) {
			HttpSession session = request.getSession();

			if (session != null) {
				id = session.getId();
			}
		}

		return id;
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
	 * @param passwordChangeRequest A data element containing the password
	 *                              change request
	 * @throws ServiceException        If the authentication cannot be
	 *                                 processed
	 * @throws AuthenticationException If the authentication fails
	 * @throws Exception               If the password change fails
	 */
	public void handleChangePassword(StringDataElement passwordChangeRequest)
		throws Exception {
		changePassword(passwordChangeRequest);
	}

	/**
	 * Handles the {@link AuthenticatedService#GET_USER_DATA} command.
	 *
	 * @param ignored Not used, should always be NULL
	 * @return A data element list containing the user data if the user is
	 * logged in
	 * @throws AuthenticationException If the user is not logged in
	 */
	public DataElementList handleGetUserData(Object ignored)
		throws AuthenticationException {
		SessionData sessionData = getSessionData();

		resetSessionData(sessionData);

		return getUserData();
	}

	/**
	 * Handles the {@link AuthenticatedService#LOGIN} command.
	 *
	 * @param loginData A string data element containing the login credentials
	 * @return A data element list containing the user data if the
	 * authentication was successful
	 * @throws ServiceException        If the authentication cannot be
	 *                                 processed
	 * @throws AuthenticationException If the authentication fails
	 */
	public DataElementList handleLogin(StringDataElement loginData)
		throws AuthenticationException, ServiceException {
		return loginUser(loginData, loginData.getProperty(LOGIN_USER_INFO,
			""));
	}

	/**
	 * Handles the {@link AuthenticatedService#LOGOUT} command.
	 *
	 * @param ignored Not used, should always be NULL
	 */
	public void handleLogout(DataElement<?> ignored) {
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

		ServiceContext context = ServiceContext.getInstance();

		if (context != null) {
			context.setService(this);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public DataElementList loginUser(StringDataElement loginData,
		String clientInfo) throws ServiceException {
		String loginName = loginData.getName();
		boolean reLogin = loginData.getValue() == null;
		E user;

		if (reLogin) {
			SessionData sessionData = getSessionData();

			endSession(sessionData);

			if (sessionData
				.get(SessionData.SESSION_LOGIN_NAME)
				.equals(loginName)) {
				user = (E) sessionData.get(SessionData.SESSION_USER);
			} else {
				throw new AuthenticationException("ReLoginNotPossible");
			}
		} else {
			user = authenticate(loginData);
			loginData.setValue(null);
		}

		if (user == null) {
			throw new AuthenticationException(
				"Invalid password for " + loginName);
		} else {
			HttpServletRequest request = getThreadLocalRequest();

			if (!reLogin) {
				String clientAddr = request.getRemoteAddr();
				String forwardAddr = request.getHeader("X-Forwarded-For");

				if (forwardAddr != null && forwardAddr.length() > 0 &&
					!forwardAddr.equals(clientAddr)) {
					clientAddr = forwardAddr;
				}

				Log.infof("[LOGIN] User %s from %s authenticated in %s\n%s",
					user, clientAddr, getApplicationName(), clientInfo);
			}

			authorizeUser(user, loginData);

			Map<String, SessionData> sessionMap =
				getSessionMap(getServletContext());

			HttpSession session = request.getSession();
			String sessionId = session.getId();
			SessionData sessionData = sessionMap.get(sessionId);
			DataElementList userData = null;

			String previousSessionId = loginData.getProperty(SESSION_ID, null);

			if (previousSessionId != null) {
				SessionData previousSessionData =
					sessionMap.remove(previousSessionId);

				if (sessionData == null && previousSessionData != null) {
					sessionData = previousSessionData;
				}

				sessionMap.put(sessionId, sessionData);
			}

			session.setAttribute(LOGIN_NAME, loginName);

			if (sessionData == null) {
				sessionData = createSessionData();
			} else {
				userData = sessionData.get(SessionData.SESSION_USER_DATA);
			}

			if (userData == null) {
				userData = new DataElementList("UserData", null);
			}

			sessionData.update(user, loginName, userData);
			initUserData(userData, user, loginName);

			return userData;
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
	public String prepareDownload(DownloadData data) throws Exception {
		String url = DEFAULT_DOWNLOAD_URL + data.getFileName();

		getSessionData().get(SESSION_DOWNLOADS).put(url, data);

		return url;
	}

	@Override
	public String prepareUpload(UploadHandler uploadHandler)
		throws AuthenticationException {
		String uploadId = Integer.toString(nextUploadId++);
		String uploadUrl = DEFAULT_UPLOAD_URL + "?id=" + uploadId;

		getSessionData().get(SESSION_UPLOADS).put(uploadId, uploadHandler);

		return uploadUrl;
	}

	@Override
	public void removeDownload(String url) {
		try {
			getSessionData().get(SESSION_DOWNLOADS).remove(url);
		} catch (AuthenticationException e) {
			Log.warn("Removing download failed", e);
		}
	}

	/**
	 * Removes a session from the context of this service.
	 *
	 * @param session The session to remove
	 */
	public void removeSession(HttpSession session) {
		Map<String, SessionData> sessionMap =
			getSessionMap(getServletContext());

		String sessionId = session.getId();
		SessionData sessionData = sessionMap.get(sessionId);

		if (sessionData != null) {
			endSession(sessionData);
			sessionMap.remove(sessionId);
		}

		session.removeAttribute(LOGIN_NAME);
	}

	@Override
	public void removeUpload(String url) {
		String id = url.substring(url.indexOf("id=") + 3);

		try {
			getSessionData().get(SESSION_UPLOADS).remove(id);
		} catch (AuthenticationException e) {
			Log.warn("Removing upload failed", e);
		}
	}

	@Override
	public void revokeExternalServiceAccess(
		ExternalServiceDefinition serviceDefinition) throws Exception {
		ExternalService service =
			ExternalService.create(serviceDefinition, getUser(),
				getServiceConfiguration());

		service.revokeAccess();
	}

	/**
	 * Adds a log aspect to the logging framework after injecting the session
	 * manager (i.e. _this_ instance) into it.
	 *
	 * @param logAspect The log aspect to add
	 */
	protected void addLogAspect(LogAspect<?> logAspect) {
		logAspect.set(DataRelationTypes.SESSION_MANAGER, this);
		Log.addLogAspect(logAspect);
	}

	/**
	 * Must be implemented by subclasses to perform the authentication of a
	 * user. The returned object must be the authenticated entity that
	 * corresponds to the given credentials if the authentication was
	 * successful. The input is a string data element with the login name as
	 * it's name and the password as the value.
	 *
	 * @param loginData A string data element containing the login credentials
	 * @return The entity of the authenticated person if the authentication was
	 * successful
	 * @throws AuthenticationException If the authentication fails
	 */
	protected abstract E authenticate(StringDataElement loginData)
		throws AuthenticationException;

	/**
	 * Performs the authorization of a user after authentication. What exactly
	 * this means depends on the subclass implementation and is application
	 * dependent. This default implementation does nothing.
	 *
	 * @param user      The authenticated user
	 * @param loginData Additional properties from the login data if available
	 * @throws AuthenticationException If the authorization fails
	 */
	protected void authorizeUser(E user, HasProperties loginData)
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
	 * @param passwordChangeRequest A data element containing the password
	 *                              change request
	 * @throws Exception Any exception may be thrown if the password change
	 *                   fails
	 */
	protected void changePassword(StringDataElement passwordChangeRequest)
		throws Exception {
	}

	/**
	 * Overridden to only allow authentication commands if not authenticated.
	 *
	 * @see CommandServiceImpl#checkCommandExecution(Command, DataElement)
	 */
	@Override
	protected <T extends DataElement<?>> void checkCommandExecution(
		Command<T, ?> command, T data) throws ServiceException {
		if (!(LOGIN.equals(command) || LOGOUT.equals(command))) {
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
		String sessionId = getThreadLocalRequest().getSession().getId();
		SessionData sessionData = new SessionData();

		getSessionMap(getServletContext()).put(sessionId, sessionData);

		return sessionData;
	}

	/**
	 * Overridden to implement authenticated download functionality.
	 *
	 * @param request  The request
	 * @param response The response
	 * @throws ServletException On servlet errors
	 * @throws IOException      On I/O errors
	 */
	@Override
	protected void doGet(HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException {
		SessionData sessionData = getSessionMap(getServletContext()).get(
			request.getSession().getId());

		if (sessionData == null) {
			setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
				"User not authorized");
		} else if (!processDownloadRequest(request, response, sessionData) &&
			!processExternalServiceResponse(request, response, sessionData)) {
			super.doGet(request, response);
		}
	}

	/**
	 * This method can be overridden by subclasses that need to perform cleanup
	 * operations if a session is no longer needed. The superclass method
	 * should
	 * always be invoked after a subclass has performed it's cleanup.
	 *
	 * @param sessionData The session data for the session that is logged out
	 */
	protected void endSession(SessionData sessionData) {
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
	 * @param userData  The list of data elements for the current user
	 * @param user      The entity for the user to initialize the data from
	 * @param loginName The login name of the current user
	 * @throws ServiceException If initializing the data fails
	 */
	protected void initUserData(DataElementList userData, E user,
		String loginName) throws ServiceException {
		HttpServletRequest request = getThreadLocalRequest();

		userData.set(LOGIN_NAME, loginName);
		userData.set(USER_NAME, EntityFunctions.format(user));
		userData.setProperty(SESSION_ID, request.getSession().getId());
	}

	/**
	 * Checks for and if necessary processes a download GET request.
	 *
	 * @param request     url The request URL
	 * @param response    The servlet response
	 * @param sessionData The session data for the current user
	 * @return TRUE if a download request has been detected and processed
	 * @throws IOException      If an IO operation fails
	 * @throws ServletException If handling the request fails
	 */
	protected boolean processDownloadRequest(HttpServletRequest request,
		HttpServletResponse response, SessionData sessionData) {
		String url = request.getRequestURI();
		boolean isDownloadRequest = false;

		if (url != null) {
			Map<String, DownloadData> sessionDownloads =
				sessionData.get(SESSION_DOWNLOADS);

			url = getDownloadUrl(url);

			DownloadData downloadData = sessionDownloads.get(url);

			if (downloadData != null) {
				try {
					isDownloadRequest = true;
					addResponseHeader(response, downloadData);
					response.setCharacterEncoding("UTF-8");
					response.setContentType(downloadData
						.getFileType()
						.getMimeType()
						.getDefinition());

					// this allows to use Window.Location.assign() for the
					// download URL without actually replacing the window URL
					response.setHeader("Content-Disposition", "attachment");

					writeDownloadDataToResponse(response, downloadData);
				} catch (Throwable e) {
					Log.error("Processing of download request failed", e);
				} finally {
					if (downloadData.isRemoveAfterDownload()) {
						sessionDownloads.remove(url);
					}
				}
			}
		}

		return isDownloadRequest;
	}

	/**
	 * Checks for and if necessary processes a download GET request.
	 *
	 * @param request      url The request URL
	 * @param httpResponse The servlet response
	 * @param sessionData  The session data for the current user
	 * @return TRUE if a download request has been detected and processed
	 * @throws ServletException If handling the response fails
	 */
	protected boolean processExternalServiceResponse(HttpServletRequest request,
		HttpServletResponse httpResponse, SessionData sessionData)
		throws ServletException {
		String url = request.getRequestURI();

		boolean isOAuthResponse =
			(url != null && url.indexOf(DEFAULT_OAUTH_CALLBACK_URL) >= 0);

		if (isOAuthResponse) {
			Iterator<ExternalService> services =
				getSessionContext().get(EXTERNAL_SERVICES).iterator();

			ExternalService service = null;

			while (services.hasNext()) {
				ExternalService checkService = services.next();
				String requestId =
					request.getParameter(checkService.getRequestIdParam());

				if (requestId != null &&
					requestId.equals(checkService.getServiceId())) {
					service = checkService;
					services.remove();

					break;
				}
			}

			if (service != null) {
				AuthorizationCallback callback =
					service.get(AUTHORIZATION_CALLBACK);

				try {
					String code = request.getParameter(
						service.getCallbackCodeRequestParam());

					String accessToken = service.processCallback(code);
					String response = "<h2>Server-Freigabe erhalten</h2>" +
						"<p>Der Server hat den Zugriff authorisiert. " +
						"Bitte kehren Sie zur vorherigen Seite zurück, " +
						"um auf die Server-Daten zuzugreifen.</p>";

					callback.authorizationSuccess(accessToken);
					httpResponse.getWriter().println(response);
				} catch (Exception e) {
					try {
						httpResponse
							.getWriter()
							.println("Error: " + e.getMessage());
					} catch (IOException iO) {
						Log.error("Response access error", iO);
					}

					Log.error("External service response processing failed",
						e);
					callback.authorizationFailure(e);
				}
			} else {
				throw new ServletException(
					"No external service for '" + request + "'");
			}
		}

		return isOAuthResponse;
	}

	/**
	 * Resets an existing session data for re-use. This method will be invoked
	 * if a user connects again to a session, e.g. after closing the browser
	 * window. Subclasses can then reset the session data, like removing stale
	 * data that cannot be used again.
	 *
	 * <p>Subclasses should always invoke the superclass implementation.</p>
	 *
	 * @param sessionData The session data to reset
	 */
	protected void resetSessionData(SessionData sessionData) {
	}

	/**
	 * Internal method to query the {@link SessionData} for the session of the
	 * current request.
	 *
	 * @param checkAuthentication TRUE to throw an exception if no user is
	 *                            authenticated for the current session
	 * @return The session data (may be NULL if no session is available and the
	 * check authentication parameter is FALSE)
	 * @throws AuthenticationException If no session data is available and the
	 *                                 check authentication parameter is TRUE
	 */
	SessionData getSessionData(boolean checkAuthentication)
		throws AuthenticationException {
		SessionData sessionData =
			getSessionData(getThreadLocalRequest(), checkAuthentication);

		if (sessionData != null && checkAuthentication) {
			checkAuthenticationTimeout(sessionData);
		}

		return sessionData;
	}

	/**
	 * adds the header to the {@link HttpServletResponse} based on information
	 * taken from downloadData.
	 */
	private void addResponseHeader(HttpServletResponse response,
		DownloadData downloadData) {
		String header = String.format("attachment;filename=\"%s\"",
			downloadData.getFileName());

		response.addHeader("Content-Disposition", header);
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
	 * @param sessionData The session to check for the timeout
	 * @throws AuthenticationException If the session timeout has been reached
	 */
	@SuppressWarnings("boxing")
	private void checkAuthenticationTimeout(SessionData sessionData)
		throws AuthenticationException {
		int authenticationTimeout;

		authenticationTimeout =
			getServiceConfiguration().getConfigValue(AUTHENTICATION_TIMEOUT,
				0);

		if (authenticationTimeout > 0) {
			long sessionTime = sessionData.get(SESSION_START_TIME).getTime();

			sessionTime = (System.currentTimeMillis() - sessionTime) / 1000;

			if (sessionTime > authenticationTimeout) {
				throw new AuthenticationException("UserSessionExpired", true);
			}
		}
	}

	/**
	 * Returns the download URL part of a certain URL string.
	 *
	 * @param url The full URL string
	 * @return The download URL part
	 */
	private String getDownloadUrl(String url) {
		int start = url.indexOf(DEFAULT_DOWNLOAD_URL);

		if (start > 0) {
			url = url.substring(start);
		}

		return url;
	}

	/**
	 * Returns true if the given contenType is knwown to be character based.
	 *
	 * @param contentType The character based data
	 * @return The character based data
	 */
	private boolean isCharacterBasedData(String contentType) {
		Pattern caseInsensitivePattern =
			Pattern.compile(".*(?i)text.*", Pattern.CASE_INSENSITIVE);

		return caseInsensitivePattern.matcher(contentType).matches();
	}

	/**
	 * Uses a {@link ServletOutputStream} to write the data of an HTTP servlet
	 * response.
	 *
	 * @param response The response object
	 * @param data     The response data
	 */
	private void writeBinaryOutput(HttpServletResponse response, Object data)
		throws IOException {
		ServletOutputStream out = response.getOutputStream();

		// TODO: support additional data formats
		if (data instanceof byte[]) {
			byte[] bytes = (byte[]) data;

			out.write(bytes, 0, bytes.length);
		} else {
			out.print(data.toString());
		}

		out.flush();
		out.close();
	}

	/**
	 * Uses a {@link PrintWriter} to write output to the
	 * {@link HttpServletResponse}
	 */
	private void writeCharacterBasedOutput(HttpServletResponse response,
		String content) throws IOException {
		PrintWriter printWriter = response.getWriter();

		printWriter.print(content);
		printWriter.close();
	}

	/**
	 * Finds out whether the data to write is binary or character-based and
	 * uses
	 * the appropriate method the write the data to the
	 * {@link HttpServletResponse}.
	 */
	private void writeDownloadDataToResponse(HttpServletResponse response,
		DownloadData downloadData) throws IOException {
		String contentType =
			downloadData.getFileType().getMimeType().getDefinition();
		Object data = downloadData.createData();

		if (data != null) {
			if (isCharacterBasedData(contentType)) {
				writeCharacterBasedOutput(response, data.toString());
			} else {
				writeBinaryOutput(response, data);
			}
		}
	}
}
