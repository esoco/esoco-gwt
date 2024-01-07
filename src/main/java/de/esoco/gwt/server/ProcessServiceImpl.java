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

import de.esoco.data.SessionData;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElement.CopyMode;
import de.esoco.data.element.DataElementList;
import de.esoco.data.process.ProcessDescription;
import de.esoco.data.process.ProcessState;
import de.esoco.data.process.ProcessState.ProcessExecutionMode;
import de.esoco.data.process.ProcessState.ProcessStateFlag;

import de.esoco.entity.ConcurrentEntityModificationException;
import de.esoco.entity.Entity;
import de.esoco.entity.EntityRelationTypes;

import de.esoco.gwt.shared.AuthenticationException;
import de.esoco.gwt.shared.Command;
import de.esoco.gwt.shared.ProcessService;
import de.esoco.gwt.shared.ServiceException;

import de.esoco.lib.expression.monad.Option;
import de.esoco.lib.logging.Log;
import de.esoco.lib.property.InteractionEventType;
import de.esoco.lib.property.UserInterfaceProperties;

import de.esoco.process.InvalidParametersException;
import de.esoco.process.Process;
import de.esoco.process.ProcessDefinition;
import de.esoco.process.ProcessException;
import de.esoco.process.ProcessExecutor;
import de.esoco.process.ProcessFragment;
import de.esoco.process.ProcessManager;
import de.esoco.process.ProcessRelationTypes;
import de.esoco.process.ProcessStep;

import de.esoco.storage.StorageException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.MetaTypes;

import static de.esoco.data.DataRelationTypes.SESSION_MANAGER;
import static de.esoco.data.DataRelationTypes.STORAGE_ADAPTER_REGISTRY;

import static de.esoco.entity.EntityRelationTypes.CONTEXT_MODIFIED_ENTITIES;

import static de.esoco.lib.property.StateProperties.PROPERTIES_CHANGED;
import static de.esoco.lib.property.StateProperties.STRUCTURE_CHANGED;
import static de.esoco.lib.property.StateProperties.VALUE_CHANGED;

import static de.esoco.process.ProcessRelationTypes.AUTO_CONTINUE;
import static de.esoco.process.ProcessRelationTypes.AUTO_UPDATE;
import static de.esoco.process.ProcessRelationTypes.CLIENT_HEIGHT;
import static de.esoco.process.ProcessRelationTypes.CLIENT_INFO;
import static de.esoco.process.ProcessRelationTypes.CLIENT_LOCALE;
import static de.esoco.process.ProcessRelationTypes.CLIENT_WIDTH;
import static de.esoco.process.ProcessRelationTypes.EXTERNAL_SERVICE_ACCESS;
import static de.esoco.process.ProcessRelationTypes.FINAL_STEP;
import static de.esoco.process.ProcessRelationTypes.IMMEDIATE_INTERACTION;
import static de.esoco.process.ProcessRelationTypes.INTERACTION_EVENT_PARAM;
import static de.esoco.process.ProcessRelationTypes.INTERACTION_EVENT_TYPE;
import static de.esoco.process.ProcessRelationTypes.INTERACTION_PARAMS;
import static de.esoco.process.ProcessRelationTypes.OPTIONAL_PROCESS_INPUT_PARAMS;
import static de.esoco.process.ProcessRelationTypes.PROCESS_ID;
import static de.esoco.process.ProcessRelationTypes.PROCESS_INFO;
import static de.esoco.process.ProcessRelationTypes.PROCESS_LIST;
import static de.esoco.process.ProcessRelationTypes.PROCESS_LOCALE;
import static de.esoco.process.ProcessRelationTypes.PROCESS_SESSION_EXPIRED;
import static de.esoco.process.ProcessRelationTypes.PROCESS_STEP_STYLE;
import static de.esoco.process.ProcessRelationTypes.PROCESS_USER;
import static de.esoco.process.ProcessRelationTypes.RELOAD_CURRENT_STEP;
import static de.esoco.process.ProcessRelationTypes.REQUIRED_PROCESS_INPUT_PARAMS;
import static de.esoco.process.ProcessRelationTypes.SPAWN_PROCESSES;
import static de.esoco.process.ProcessRelationTypes.VIEW_PARAMS;

import static org.obrel.core.RelationTypes.newMapType;
import static org.obrel.type.StandardTypes.DESCRIPTION;
import static org.obrel.type.StandardTypes.NAME;

/**
 * The base class of {@link ProcessService} for service implementations that
 * provide process-related functionality.
 *
 * @author eso
 */
public abstract class ProcessServiceImpl<E extends Entity>
	extends StorageServiceImpl<E> implements ProcessService, ProcessExecutor {

	private static final long serialVersionUID = 1L;

	/**
	 * The process map will be stored in the {@link SessionData} object for a
	 * user's session.
	 */
	private static final RelationType<Map<Integer, Process>> USER_PROCESS_MAP =
		newMapType(false);

	private static Locale rDefaultLocale = Locale.ENGLISH;

	private static List<ProcessDefinition> aProcessDefinitions =
		new ArrayList<ProcessDefinition>();

	static {
		RelationTypes.init(ProcessServiceImpl.class);
	}

	private final DataElementFactory rDataElementFactory =
		new DataElementFactory(this);

	private Option<Class<? extends ProcessDefinition>> aAppProcess =
		Option.none();

	/**
	 * Creates a process description to be used by client code for a certain
	 * process definition and registers the definition internally so that it
	 * can
	 * be associated with the index stored in the description.
	 *
	 * @param rDefClass The process definition class
	 * @return A new process description of the process definition
	 */
	public static ProcessDescription createProcessDescription(
		Class<? extends ProcessDefinition> rDefClass) {
		return createProcessDescriptions(rDefClass, null);
	}

	/**
	 * Creates one or more process descriptions for a certain process
	 * definition. If the second argument is not NULL and the process
	 * definition
	 * relation {@link ProcessRelationTypes#OPTIONAL_PROCESS_INPUT_PARAMS}
	 * contains {@link EntityRelationTypes#ENTITY_ID} two descriptions will be
	 * added to the list, one for the creation of a new entity and one for the
	 * editing of an existing entity.
	 *
	 * @param rDefClass        rDefinition The process definition
	 * @param rDescriptionList A list to add the description(s) to or NULL to
	 *                         only return the description
	 * @return A new process description of the process definition
	 */
	protected static ProcessDescription createProcessDescriptions(
		Class<? extends ProcessDefinition> rDefClass,
		List<ProcessDescription> rDescriptionList) {
		ProcessDefinition rDefinition =
			ProcessManager.getProcessDefinition(rDefClass);

		int nIndex = aProcessDefinitions.indexOf(rDefinition);

		if (nIndex < 0) {
			nIndex = aProcessDefinitions.size();
			aProcessDefinitions.add(rDefinition);
		}

		boolean bInputRequired =
			rDefinition.hasRelation(REQUIRED_PROCESS_INPUT_PARAMS);

		if (!bInputRequired &&
			rDefinition.get(OPTIONAL_PROCESS_INPUT_PARAMS).size() > 0 &&
			rDescriptionList != null) {
			// create a special edit process description with the same ID (!)
			// that will be invoked if a selection is available
			ProcessDescription aDescription =
				new ProcessDescription(rDefinition.get(NAME) + "Edit",
					rDefinition.get(DESCRIPTION), nIndex, true);

			rDescriptionList.add(aDescription);
		}

		ProcessDescription aDescription =
			new ProcessDescription(rDefinition.get(NAME),
				rDefinition.get(DESCRIPTION), nIndex, bInputRequired);

		if (rDescriptionList != null) {
			rDescriptionList.add(aDescription);
		}

		return aDescription;
	}

	/**
	 * Sets the default locale to be used if the client locale cannot be
	 * determined.
	 *
	 * @param rLocale The new default locale
	 */
	public static void setDefaultLocale(Locale rLocale) {
		rDefaultLocale = rLocale;
	}

	@Override
	public ProcessState executeProcess(ProcessDescription rDescription,
		Relatable rInitParams)
		throws AuthenticationException, ServiceException {
		boolean bCheckAuthentication = !hasProcessAuthentication() ||
			rDescription.hasFlag(PROCESS_AUTHENTICATED);

		SessionData rSessionData = getSessionData(bCheckAuthentication);

		ProcessExecutionMode eExecutionMode = ProcessExecutionMode.EXECUTE;
		Process rProcess = null;
		Integer rId = null;
		ProcessState rProcessState = null;
		Map<Integer, Process> rProcessMap = null;
		boolean bHasSessionTimeout = false;

		List<Process> rProcessList = getSessionContext().get(PROCESS_LIST);

		try {
			// this can only happen in the case of process-based authentication
			// in the case that the session has expired
			if (rSessionData == null) {
				rSessionData = createSessionData();

				if (rDescription instanceof ProcessState) {
					// if a session timeout occurred re-start the app process
					// from a new process description
					rDescription = new ProcessDescription(rDescription);
					bHasSessionTimeout = true;
				}
			}

			rProcessMap = rSessionData.get(USER_PROCESS_MAP);
			rProcess = getProcess(rDescription, rSessionData, rInitParams);

			rId = rProcess.getParameter(PROCESS_ID);

			if (bHasSessionTimeout) {
				rProcess.set(PROCESS_SESSION_EXPIRED);
			}

			if (rDescription instanceof ProcessState) {
				rProcessState = (ProcessState) rDescription;

				eExecutionMode = rProcessState.getExecutionMode();
			} else {
				setClientProperties(rDescription, rProcess);

				if (rProcess.get(INTERACTION_EVENT_PARAM) ==
					RELOAD_CURRENT_STEP) {
					eExecutionMode = ProcessExecutionMode.RELOAD;
				}
			}

			rProcess.set(CLIENT_WIDTH, rDescription.getClientWidth());
			rProcess.set(CLIENT_HEIGHT, rDescription.getClientHeight());

			rProcess.executeInteractionCleanupActions();

			ProcessStep rPreviousStep = rProcess.getCurrentStep();

			executeProcess(rProcess, eExecutionMode);

			boolean bRefresh = rProcess.getCurrentStep() != rPreviousStep ||
				eExecutionMode == ProcessExecutionMode.RELOAD;

			rProcessState =
				createProcessState(rDescription, rProcess, bRefresh);

			if (rProcess.isFinished()) {
				rProcessList.remove(rProcess);
				rProcessMap.remove(rId);
			}
		} catch (Throwable e) {
			if (rProcess != null) {
				try {
					rProcessState =
						createProcessState(rDescription, rProcess, false);
				} catch (Exception eSecondary) {
					// Only log secondary exceptions and fall through to
					// standard exception handling
					Log.error("Could not create exception process state",
						eSecondary);
				}
			}

			ServiceException eService = wrapException(e, rProcessState);

			// keep the process on recoverable error for re-execution when the
			// client has tried to resolve the error condition
			if (!eService.isRecoverable() && rProcess != null) {
				rProcessList.remove(rProcess);
				rProcessMap.remove(rId);
			}

			throw eService;
		}

		return rProcessState;
	}

	/**
	 * Handles the {@link ProcessService#EXECUTE_PROCESS} command.
	 *
	 * @param rDescription The description of the process to execute
	 * @return A process state object if the process stopped for an interaction
	 * or NULL if the process has already terminated
	 * @throws ServiceException If the process execution fails
	 */
	public ProcessState handleExecuteProcess(ProcessDescription rDescription)
		throws ServiceException {
		return executeProcess(rDescription, null);
	}

	/**
	 * Cancels all processes that are active in the given session.
	 *
	 * @param rSessionData The session data
	 */
	protected void cancelActiveProcesses(SessionData rSessionData) {
		List<Process> rProcessList = getSessionContext().get(PROCESS_LIST);

		Map<Integer, Process> rProcessMap = rSessionData.get(USER_PROCESS_MAP);

		for (Process rProcess : rProcessMap.values()) {
			rProcess.execute(ProcessExecutionMode.CANCEL);
			rProcessList.remove(rProcess);
		}

		rProcessMap.clear();
	}

	/**
	 * Overridden to allow the execution of a self-authenticating application
	 * process.
	 *
	 * @see CommandServiceImpl#checkCommandExecution(Command, DataElement)
	 */
	@Override
	protected <T extends DataElement<?>> void checkCommandExecution(
		Command<T, ?> rCommand, T rData) throws ServiceException {
		if (!hasProcessAuthentication() || !rCommand.equals(EXECUTE_PROCESS) ||
			!rData.getName().startsWith(APPLICATION_PROCESS_PATH)) {
			super.checkCommandExecution(rCommand, rData);
		}
	}

	/**
	 * Overridden to cancel any running processes of the current user.
	 *
	 * @see AuthenticatedServiceImpl#endSession(SessionData)
	 */
	@Override
	protected void endSession(SessionData rSessionData) {
		cancelActiveProcesses(rSessionData);

		super.endSession(rSessionData);
	}

	/**
	 * Performs the actual invocation of a process execution method.
	 *
	 * @param rProcess The process to execute
	 * @param eMode    The execution mode
	 * @throws ProcessException If the process execution fails
	 */
	protected void executeProcess(Process rProcess, ProcessExecutionMode eMode)
		throws ProcessException {
		rProcess.execute(eMode);
	}

	/**
	 * Indicates whether the application authentication is done by the (main)
	 * application process or by the client side UI. The standard value if
	 * FALSE
	 * but it will be set to TRUE if an application process is set with
	 * {@link #setApplicationProcess(Class)}.
	 *
	 * @return TRUE for process based authentication
	 */
	protected boolean hasProcessAuthentication() {
		return aAppProcess.exists();
	}

	/**
	 * Initializes a new process and associates it with a reference entity
	 * if it
	 * is not NULL. Subclasses that override this method should typically
	 * invoke
	 * the superclass method first. The initialization parameters will be
	 * copied
	 * to the process, overriding any existing parameters.
	 *
	 * @param rProcess    The process to initialize
	 * @param rInitParams Optional process initialization parameters or NULL
	 *                      for
	 *                    none
	 * @throws ProcessException If the process initialization fails
	 * @throws ServiceException If the user is not authenticated or the
	 *                          preparing the process context fails
	 */
	protected void initProcess(Process rProcess, Relatable rInitParams)
		throws ProcessException, ServiceException {
		if (rInitParams != null) {
			ObjectRelations.copyRelations(rInitParams, rProcess, true);
		}
	}

	/**
	 * Overridden to cancel any processes that remained active in the given
	 * session when the user closed the browser window.
	 *
	 * @see AuthenticatedServiceImpl#resetSessionData(SessionData)
	 */
	@Override
	protected void resetSessionData(SessionData rSessionData) {
		cancelActiveProcesses(rSessionData);
	}

	/**
	 * Can be invoked by subclasses to set the (main) application process. This
	 * will also set the return value of {@link #hasProcessAuthentication()} to
	 * true. This method should be invoked before any other process definitions
	 * are created through {@link #createProcessDescriptions(Class, List)}.
	 *
	 * @param rProcessDefinition The class of the application process
	 *                           definition
	 */
	protected void setApplicationProcess(
		Class<? extends ProcessDefinition> rProcessDefinition) {
		aAppProcess = Option.of(rProcessDefinition);

		// add to process definition list
		createProcessDescriptions(rProcessDefinition, null);
	}

	/**
	 * Wraps exceptions that may occur during the execution of one of the
	 * service methods into a {@link ServiceException} if necessary. In case of
	 * an {@link InvalidParametersException} the service exception will contain
	 * information about the invalid parameters.
	 *
	 * @param e             The exception to handle
	 * @param rProcessState The current process state
	 * @return The resulting {@link ServiceException}
	 */
	protected ServiceException wrapException(Throwable e,
		ProcessState rProcessState) {
		ServiceException eResult;

		if (e instanceof ServiceException) {
			eResult = (ServiceException) e;
		} else {
			String sMessage = e.getMessage();

			if (e instanceof InvalidParametersException) {
				Map<RelationType<?>, String> rParamMessageMap =
					((InvalidParametersException) e).getInvalidParams();

				Map<String, String> aInvalidParams =
					new HashMap<String, String>(rParamMessageMap.size());

				for (Entry<RelationType<?>, String> rEntry :
					rParamMessageMap.entrySet()) {
					aInvalidParams.put(rEntry.getKey().getName(),
						rEntry.getValue());
				}

				eResult = new ServiceException(sMessage, aInvalidParams,
					rProcessState);
			} else if (e.getCause() instanceof ConcurrentEntityModificationException) {
				String sEntityId =
					((ConcurrentEntityModificationException) e.getCause()).getEntityId();

				Map<String, String> aLockedEntity =
					new HashMap<String, String>(1);

				aLockedEntity.put(ERROR_LOCKED_ENTITY_ID, sEntityId);

				eResult =
					new ServiceException(ERROR_ENTITY_LOCKED, aLockedEntity,
						rProcessState);
			} else {
				eResult = new ServiceException(sMessage, e);
			}
		}

		return eResult;
	}

	/**
	 * Applies the list of modified entities in a process to the given process
	 * state.
	 *
	 * @param rProcess      The process to read the modification from
	 * @param rProcessState The process state to apply the modifications too
	 */
	private void applyModifiedEntities(Process rProcess,
		ProcessState rProcessState) {
		Map<String, Entity> rModifiedEntities =
			rProcess.get(CONTEXT_MODIFIED_ENTITIES);

		if (!rModifiedEntities.isEmpty()) {
			StringBuilder aLocks = new StringBuilder();

			for (Entity rLockedEntity : rModifiedEntities.values()) {
				if (!rLockedEntity.hasFlag(MetaTypes.LOCKED)) {
					aLocks.append(rLockedEntity.getGlobalId());
					aLocks.append(",");
				}
			}

			if (aLocks.length() > 0) {
				aLocks.setLength(aLocks.length() - 1);

				rProcessState.setProperty(PROCESS_ENTITY_LOCKS,
					aLocks.toString());
			}
		}
	}

	/**
	 * Checks whether the given process description is for an application
	 * process and a corresponding process already exists in the given process
	 * collection.
	 *
	 * @param rDescription The application process description
	 * @param rProcesses   The processes to search
	 * @return The application process to re-use or NULL for none
	 */
	private Process checkReuseExistingAppProcess(
		ProcessDescription rDescription, Collection<Process> rProcesses) {
		String sProcessName = rDescription.getName();
		Process rProcess = null;

		if (sProcessName.startsWith(APPLICATION_PROCESS_PATH)) {
			if (aAppProcess.exists() &&
				sProcessName.endsWith(APPLICATION_MAIN_PROCESS)) {
				// will never fail as exists() is checked above
				sProcessName = aAppProcess.map(p -> p.getName()).orFail();
			}

			for (Process rExistingProcess : rProcesses) {
				if (sProcessName.endsWith(rExistingProcess.getName())) {
					rProcess = rExistingProcess;

					break;
				}
			}
		}

		return rProcess;
	}

	/**
	 * Collects all modified data element from a hierarchy of data elements. If
	 * a data element list is modified it will be added to the target
	 * collection. Otherwise it's children will be checked recursively.
	 *
	 * @param rDataElements     The data elements to check for modification
	 * @param rModifiedElements A collection to add modified elements to
	 */
	private void collectModifiedDataElements(
		Collection<DataElement<?>> rDataElements,
		Collection<DataElement<?>> rModifiedElements) {
		for (DataElement<?> rDataElement : rDataElements) {
			boolean bChanged = rDataElement.hasFlag(VALUE_CHANGED) |
				rDataElement.hasFlag(PROPERTIES_CHANGED);

			if (rDataElement instanceof DataElementList) {
				if (rDataElement.hasFlag(STRUCTURE_CHANGED)) {
					rModifiedElements.add(rDataElement);
				} else {
					if (bChanged) {
						rModifiedElements.add(
							rDataElement.copy(CopyMode.PROPERTIES));
					}

					collectModifiedDataElements(
						((DataElementList) rDataElement).getElements(),
						rModifiedElements);
				}
			} else if (bChanged) {
				rModifiedElements.add(rDataElement);
			}
		}
	}

	/**
	 * Creates the data elements for certain relations of a relatable object.
	 * For each relation a single data element will be created by invoking the
	 * method {@link #createDataElement(Relatable, RelationType, Set)}. If that
	 * method returns NULL no data element will be added to the result for the
	 * respective relation.
	 *
	 * @param rInteractionStep The process fragment to query the relations from
	 * @param bMarkAsChanged   Whether all data elements should be marked as
	 *                         modified
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	private List<DataElement<?>> createInteractionElements(
		ProcessFragment rInteractionStep, boolean bMarkAsChanged)
		throws StorageException {
		DataElementFactory rFactory = getDataElementFactory();

		List<RelationType<?>> rInteractionParams =
			rInteractionStep.get(INTERACTION_PARAMS);

		List<DataElement<?>> aDataElements =
			new ArrayList<DataElement<?>>(rInteractionParams.size());

		for (RelationType<?> rParam : rInteractionParams) {
			DataElement<?> aElement =
				rFactory.getDataElement(rInteractionStep, rParam);

			if (aElement != null) {
				aDataElements.add(aElement);

				if (bMarkAsChanged) {
					aElement.markAsChanged();
				}
			}
		}

		return aDataElements;
	}

	/**
	 * Creates a new process instance and sets the standard parameters on it.
	 *
	 * @param rDefinition  The process definition
	 * @param rSessionData The data of the current session
	 * @return The new process instance
	 * @throws ProcessException If creating the instance fails
	 */
	private Process createProcess(ProcessDefinition rDefinition,
		SessionData rSessionData) throws ProcessException {
		Process rProcess = ProcessManager.getProcess(rDefinition);
		Entity rUser = rSessionData.get(SessionData.SESSION_USER);

		rProcess.setParameter(SESSION_MANAGER, this);
		rProcess.setParameter(EXTERNAL_SERVICE_ACCESS, this);
		rProcess.setParameter(STORAGE_ADAPTER_REGISTRY, this);
		rProcess.setParameter(PROCESS_USER, rUser);
		rProcess.setParameter(PROCESS_LOCALE,
			getThreadLocalRequest().getLocale());

		return rProcess;
	}

	/**
	 * Creates a new {@link ProcessState} instance from a certain process.
	 * Invoked by {@link #executeProcess(ProcessDescription, Relatable)}.
	 *
	 * @param rDescription The process definition
	 * @param rProcess     The process
	 * @param bRefresh     TRUE if all data elements should be refreshed
	 *                        even if
	 *                     not marked as modified
	 * @return A new process state object
	 * @throws StorageException If the creation of a storage-based interaction
	 *                          data element fails
	 */
	@SuppressWarnings("boxing")
	private ProcessState createProcessState(ProcessDescription rDescription,
		Process rProcess, boolean bRefresh) throws StorageException {
		Integer sProcessId = rProcess.getParameter(PROCESS_ID);
		String sProcessInfo = rProcess.getParameter(PROCESS_INFO);
		ProcessState aProcessState;

		if (rProcess.isFinished()) {
			aProcessState =
				new ProcessState(rDescription, sProcessId, sProcessInfo);
		} else {
			ProcessStep rInteractionStep = rProcess.getInteractionStep();

			List<DataElement<?>> aInteractionElements =
				createInteractionElements(rInteractionStep, bRefresh);

			List<DataElementList> aViewElements =
				createViewDataElements(rInteractionStep);

			if (!bRefresh) {
				aInteractionElements =
					reduceToModifiedElements(aInteractionElements);
			}

			aProcessState =
				new ProcessState(rDescription, sProcessId, sProcessInfo,
					rInteractionStep.getName(), aInteractionElements,
					aViewElements, getSpawnProcesses(rProcess),
					getProcessStateFlags(rProcess, rInteractionStep));

			if (rProcess.hasFlagParameter(MetaTypes.AUTHENTICATED)) {
				aProcessState.setFlag(PROCESS_AUTHENTICATED);
			}

			String sStyle = rInteractionStep.getParameter(PROCESS_STEP_STYLE);

			if (sStyle != null) {
				aProcessState.setProperty(UserInterfaceProperties.STYLE,
					sStyle);
			}

			applyModifiedEntities(rProcess, aProcessState);

			// reset modifications after applying to also reset changes from
			// parameter relation listeners that are invoked during application
			rInteractionStep.resetParameterModifications();
		}

		return aProcessState;
	}

	/**
	 * Creates the data elements for certain relations of a relatable object.
	 * For each relation a single data element will be created by invoking the
	 * method {@link #createDataElement(Relatable, RelationType, Set)}. If that
	 * method returns NULL no data element will be added to the result for the
	 * respective relation.
	 *
	 * @param rInteractionStep rObject The related object to query the
	 *                            relations
	 *                         from
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	private List<DataElementList> createViewDataElements(
		ProcessFragment rInteractionStep) throws StorageException {
		DataElementFactory rFactory = getDataElementFactory();

		Set<RelationType<List<RelationType<?>>>> rViewParams =
			rInteractionStep.get(VIEW_PARAMS);

		List<DataElementList> aViewElements = null;

		if (rViewParams.size() > 0) {
			aViewElements = new ArrayList<>(rViewParams.size());

			for (RelationType<List<RelationType<?>>> rViewParam :
				rViewParams) {
				aViewElements.add(
					(DataElementList) rFactory.getDataElement(rInteractionStep,
						rViewParam));
			}
		}

		return aViewElements;
	}

	/**
	 * Returns the data element factory of this service.
	 *
	 * @return The data element factory
	 */
	private final DataElementFactory getDataElementFactory() {
		return rDataElementFactory;
	}

	/**
	 * Returns the process that is associated with a certain process
	 * description
	 * or process state and after preparing it for execution.
	 *
	 * @param rDescription The process description or process state, depending
	 *                     on the current execution state of the process
	 * @param rSessionData The data of the current session
	 * @param rInitParams  Optional process initialization parameters or NULL
	 *                     for none
	 * @return The prepared process
	 * @throws ProcessException         If accessing the process fails
	 * @throws ServiceException         If the client is not authenticated or
	 *                                  preparing the process context fails
	 * @throws StorageException         If accessing a storage fails
	 * @throws IllegalArgumentException If the given process description is
	 *                                  invalid
	 */
	@SuppressWarnings("boxing")
	private Process getProcess(ProcessDescription rDescription,
		SessionData rSessionData, Relatable rInitParams)
		throws ProcessException, ServiceException, StorageException {
		Process rProcess = null;

		Map<Integer, Process> rUserProcessMap =
			rSessionData.get(USER_PROCESS_MAP);

		if (rDescription.getClass() == ProcessDescription.class) {
			// if the user reloads the browser windows the existing process can
			// be re-used instead of creating a new one
			rProcess = checkReuseExistingAppProcess(rDescription,
				rUserProcessMap.values());

			if (rProcess != null) {
				rProcess.set(INTERACTION_EVENT_PARAM, RELOAD_CURRENT_STEP);
			} else {
				ProcessDefinition rDefinition =
					aProcessDefinitions.get(rDescription.getDescriptionId());

				rProcess = createProcess(rDefinition, rSessionData);

				getSessionContext().get(PROCESS_LIST).add(rProcess);
				rUserProcessMap.put(rProcess.getParameter(PROCESS_ID),
					rProcess);
				initProcess(rProcess, rInitParams);
				setProcessInput(rProcess, rDescription.getProcessInput());
			}
		} else if (rDescription instanceof ProcessState) {
			ProcessState rProcessState = (ProcessState) rDescription;

			rProcess = rUserProcessMap.get(rProcessState.getProcessId());

			if (rProcess == null) {
				throw new IllegalStateException("NoProcessFound");
			}

			updateProcess(rProcess, rProcessState);

			if (hasProcessAuthentication()) {
				initProcess(rProcess, rInitParams);
			}
		} else {
			throw new IllegalArgumentException(
				"Unknown process reference: " + rDescription);
		}

		return rProcess;
	}

	/**
	 * Returns a set of flags for the current state of a process.
	 *
	 * @param rProcess         The process
	 * @param rInteractionStep The current interaction step
	 */
	private Set<ProcessStateFlag> getProcessStateFlags(Process rProcess,
		ProcessStep rInteractionStep) {
		Set<ProcessStateFlag> aStepFlags = new HashSet<ProcessStateFlag>();

		if (rProcess.canRollbackToPreviousInteraction()) {
			aStepFlags.add(ProcessStateFlag.ROLLBACK);
		}

		if (rInteractionStep.hasFlag(AUTO_CONTINUE) ||
			rInteractionStep.hasFlag(AUTO_UPDATE)) {
			aStepFlags.add(ProcessStateFlag.AUTO_CONTINUE);
		}

		if (rInteractionStep.hasFlag(FINAL_STEP)) {
			aStepFlags.add(ProcessStateFlag.FINAL_STEP);
		}

		if (rInteractionStep.hasFlag(IMMEDIATE_INTERACTION)) {
			aStepFlags.add(ProcessStateFlag.HAS_IMMEDIATE_INTERACTION);
		}

		return aStepFlags;
	}

	/**
	 * Returns a list of process states for new processes that are to be
	 * spawned
	 * separate from the context of the current process. The process states
	 * must
	 * be stored in the list of the parameter
	 * {@link de.esoco.process.ProcessRelationTypes#NEW_PROCESSES}. This list
	 * will be cleared after it has been queried.
	 *
	 * @param rProcess The process to query the new processes from
	 * @return A list of process states or NULL for none
	 */
	private List<ProcessState> getSpawnProcesses(Process rProcess) {
		List<ProcessState> rSpawnProcesses =
			rProcess.getParameter(SPAWN_PROCESSES);

		if (rSpawnProcesses.size() > 0) {
			rSpawnProcesses = new ArrayList<>(rSpawnProcesses);
		} else {
			rSpawnProcesses = null;
		}

		rProcess.getParameter(SPAWN_PROCESSES).clear();

		return rSpawnProcesses;
	}

	/**
	 * Searches all modified elements in a hierarchy of data elements. If a
	 * data
	 * element list is modified it will be added to the target collection.
	 * Otherwise it's children will be checked recursively.
	 *
	 * @param rDataElements The data elements to check for modification
	 * @return A list containing the modified elements from the input
	 * collection
	 */
	private List<DataElement<?>> reduceToModifiedElements(
		Collection<DataElement<?>> rDataElements) {
		List<DataElement<?>> aModifiedElements = new ArrayList<>();

		collectModifiedDataElements(rDataElements, aModifiedElements);

		return aModifiedElements;
	}

	/**
	 * Sets properties of the current client (e.g. info, locale) as process
	 * parameters.
	 *
	 * @param rDescription The process description containing the client
	 *                     properties
	 * @param rProcess     The process to set the parameters in
	 */
	private void setClientProperties(ProcessDescription rDescription,
		Process rProcess) {
		String sClientInfo = rDescription.getClientInfo();
		String sClientLocale = rDescription.getClientLocale();

		rProcess.set(CLIENT_LOCALE, rDefaultLocale);

		if (sClientInfo != null) {
			rProcess.set(CLIENT_INFO, sClientInfo);
		}

		if (sClientLocale != null) {
			try {
				rProcess.set(CLIENT_LOCALE,
					Locale.forLanguageTag(sClientLocale));
			} catch (Exception e) {
				Log.warn("Unknown client locale: " + sClientLocale, e);
			}
		}
	}

	/**
	 * Sets the process input parameters from a data element or a list of data
	 * elements.
	 *
	 * @param rProcess      The process
	 * @param rProcessInput The process input data element(s)
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	private void setProcessInput(Process rProcess,
		DataElement<?> rProcessInput)
		throws StorageException, AuthenticationException {
		if (rProcessInput != null) {
			List<DataElement<?>> rInputValues =
				new ArrayList<DataElement<?>>();

			if (rProcessInput instanceof DataElementList) {
				for (DataElement<?> rInputValue :
					(DataElementList) rProcessInput) {
					rInputValues.add(rInputValue);
				}
			} else {
				rInputValues.add(rProcessInput);
			}

			getDataElementFactory().applyDataElements(rInputValues, rProcess);
		}
	}

	/**
	 * Updates a process from a certain process state that has been received
	 * from the client.
	 *
	 * @param rProcess      The process to update
	 * @param rProcessState The process state to update the process from
	 * @throws AuthenticationException If the client is not authenticated
	 * @throws StorageException        If a storage access fails
	 * @throws IllegalStateException   If the process is NULL
	 */
	@SuppressWarnings("boxing")
	private void updateProcess(Process rProcess, ProcessState rProcessState)
		throws AuthenticationException, StorageException {
		ProcessExecutionMode eMode = rProcessState.getExecutionMode();

		RelationType<?> rInteractionParam = null;

		if (eMode == ProcessExecutionMode.RELOAD) {
			rInteractionParam = RELOAD_CURRENT_STEP;
		} else {
			if (eMode == ProcessExecutionMode.EXECUTE) {
				List<DataElement<?>> rInteractionParams =
					rProcessState.getInteractionParams();

				List<DataElementList> rViewParams =
					rProcessState.getViewParams();

				DataElementFactory rDataElementFactory =
					getDataElementFactory();

				rDataElementFactory.applyDataElements(rInteractionParams,
					rProcess);

				if (!rViewParams.isEmpty()) {
					rDataElementFactory.applyDataElements(rViewParams,
						rProcess);
				}
			}

			DataElement<?> rInteractionElement =
				rProcessState.getInteractionElement();

			if (rInteractionElement != null) {
				rInteractionParam =
					RelationType.valueOf(rInteractionElement.getName());

				if (rInteractionParam == null) {
					throw new IllegalStateException(
						"Unknown interaction parameter: " + rInteractionParam);
				}

				InteractionEventType eEventType =
					rProcessState.getInteractionEventType();

				rProcess.setParameter(INTERACTION_EVENT_TYPE, eEventType);
			} else {
				rProcess.deleteRelation(INTERACTION_EVENT_TYPE);
			}
		}

		rProcess.setParameter(INTERACTION_EVENT_PARAM, rInteractionParam);
	}
}
