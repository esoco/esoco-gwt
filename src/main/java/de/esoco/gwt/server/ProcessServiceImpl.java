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

	private static Locale defaultLocale = Locale.ENGLISH;

	private static List<ProcessDefinition> processDefinitions =
		new ArrayList<ProcessDefinition>();

	static {
		RelationTypes.init(ProcessServiceImpl.class);
	}

	private final DataElementFactory dataElementFactory =
		new DataElementFactory(this);

	private Option<Class<? extends ProcessDefinition>> appProcess =
		Option.none();

	/**
	 * Creates a process description to be used by client code for a certain
	 * process definition and registers the definition internally so that it
	 * can
	 * be associated with the index stored in the description.
	 *
	 * @param defClass The process definition class
	 * @return A new process description of the process definition
	 */
	public static ProcessDescription createProcessDescription(
		Class<? extends ProcessDefinition> defClass) {
		return createProcessDescriptions(defClass, null);
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
	 * @param defClass        definition The process definition
	 * @param descriptionList A list to add the description(s) to or NULL to
	 *                        only return the description
	 * @return A new process description of the process definition
	 */
	protected static ProcessDescription createProcessDescriptions(
		Class<? extends ProcessDefinition> defClass,
		List<ProcessDescription> descriptionList) {
		ProcessDefinition definition =
			ProcessManager.getProcessDefinition(defClass);

		int index = processDefinitions.indexOf(definition);

		if (index < 0) {
			index = processDefinitions.size();
			processDefinitions.add(definition);
		}

		boolean inputRequired =
			definition.hasRelation(REQUIRED_PROCESS_INPUT_PARAMS);

		if (!inputRequired &&
			definition.get(OPTIONAL_PROCESS_INPUT_PARAMS).size() > 0 &&
			descriptionList != null) {
			// create a special edit process description with the same ID (!)
			// that will be invoked if a selection is available
			ProcessDescription description =
				new ProcessDescription(definition.get(NAME) + "Edit",
					definition.get(DESCRIPTION), index, true);

			descriptionList.add(description);
		}

		ProcessDescription description =
			new ProcessDescription(definition.get(NAME),
				definition.get(DESCRIPTION), index, inputRequired);

		if (descriptionList != null) {
			descriptionList.add(description);
		}

		return description;
	}

	/**
	 * Sets the default locale to be used if the client locale cannot be
	 * determined.
	 *
	 * @param locale The new default locale
	 */
	public static void setDefaultLocale(Locale locale) {
		defaultLocale = locale;
	}

	@Override
	public ProcessState executeProcess(ProcessDescription description,
		Relatable initParams) throws AuthenticationException,
		ServiceException {
		boolean checkAuthentication = !hasProcessAuthentication() ||
			description.hasFlag(PROCESS_AUTHENTICATED);

		SessionData sessionData = getSessionData(checkAuthentication);

		ProcessExecutionMode executionMode = ProcessExecutionMode.EXECUTE;
		Process process = null;
		Integer id = null;
		ProcessState processState = null;
		Map<Integer, Process> processMap = null;
		boolean hasSessionTimeout = false;

		List<Process> processList = getSessionContext().get(PROCESS_LIST);

		try {
			// this can only happen in the case of process-based authentication
			// in the case that the session has expired
			if (sessionData == null) {
				sessionData = createSessionData();

				if (description instanceof ProcessState) {
					// if a session timeout occurred re-start the app process
					// from a new process description
					description = new ProcessDescription(description);
					hasSessionTimeout = true;
				}
			}

			processMap = sessionData.get(USER_PROCESS_MAP);
			process = getProcess(description, sessionData, initParams);

			id = process.getParameter(PROCESS_ID);

			if (hasSessionTimeout) {
				process.set(PROCESS_SESSION_EXPIRED);
			}

			if (description instanceof ProcessState) {
				processState = (ProcessState) description;

				executionMode = processState.getExecutionMode();
			} else {
				setClientProperties(description, process);

				if (process.get(INTERACTION_EVENT_PARAM) ==
					RELOAD_CURRENT_STEP) {
					executionMode = ProcessExecutionMode.RELOAD;
				}
			}

			process.set(CLIENT_WIDTH, description.getClientWidth());
			process.set(CLIENT_HEIGHT, description.getClientHeight());

			process.executeInteractionCleanupActions();

			ProcessStep previousStep = process.getCurrentStep();

			executeProcess(process, executionMode);

			boolean refresh = process.getCurrentStep() != previousStep ||
				executionMode == ProcessExecutionMode.RELOAD;

			processState = createProcessState(description, process, refresh);

			if (process.isFinished()) {
				processList.remove(process);
				processMap.remove(id);
			}
		} catch (Throwable e) {
			if (process != null) {
				try {
					processState =
						createProcessState(description, process, false);
				} catch (Exception secondary) {
					// Only log secondary exceptions and fall through to
					// standard exception handling
					Log.error("Could not create exception process state",
						secondary);
				}
			}

			ServiceException service = wrapException(e, processState);

			// keep the process on recoverable error for re-execution when the
			// client has tried to resolve the error condition
			if (!service.isRecoverable() && process != null) {
				processList.remove(process);
				processMap.remove(id);
			}

			throw service;
		}

		return processState;
	}

	/**
	 * Handles the {@link ProcessService#EXECUTE_PROCESS} command.
	 *
	 * @param description The description of the process to execute
	 * @return A process state object if the process stopped for an interaction
	 * or NULL if the process has already terminated
	 * @throws ServiceException If the process execution fails
	 */
	public ProcessState handleExecuteProcess(ProcessDescription description)
		throws ServiceException {
		return executeProcess(description, null);
	}

	/**
	 * Cancels all processes that are active in the given session.
	 *
	 * @param sessionData The session data
	 */
	protected void cancelActiveProcesses(SessionData sessionData) {
		List<Process> processList = getSessionContext().get(PROCESS_LIST);

		Map<Integer, Process> processMap = sessionData.get(USER_PROCESS_MAP);

		for (Process process : processMap.values()) {
			process.execute(ProcessExecutionMode.CANCEL);
			processList.remove(process);
		}

		processMap.clear();
	}

	/**
	 * Overridden to allow the execution of a self-authenticating application
	 * process.
	 *
	 * @see CommandServiceImpl#checkCommandExecution(Command, DataElement)
	 */
	@Override
	protected <T extends DataElement<?>> void checkCommandExecution(
		Command<T, ?> command, T data) throws ServiceException {
		if (!hasProcessAuthentication() || !command.equals(EXECUTE_PROCESS) ||
			!data.getName().startsWith(APPLICATION_PROCESS_PATH)) {
			super.checkCommandExecution(command, data);
		}
	}

	/**
	 * Overridden to cancel any running processes of the current user.
	 *
	 * @see AuthenticatedServiceImpl#endSession(SessionData)
	 */
	@Override
	protected void endSession(SessionData sessionData) {
		cancelActiveProcesses(sessionData);

		super.endSession(sessionData);
	}

	/**
	 * Performs the actual invocation of a process execution method.
	 *
	 * @param process The process to execute
	 * @param mode    The execution mode
	 * @throws ProcessException If the process execution fails
	 */
	protected void executeProcess(Process process, ProcessExecutionMode mode)
		throws ProcessException {
		process.execute(mode);
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
		return appProcess.exists();
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
	 * @param process    The process to initialize
	 * @param initParams Optional process initialization parameters or NULL for
	 *                   none
	 * @throws ProcessException If the process initialization fails
	 * @throws ServiceException If the user is not authenticated or the
	 *                          preparing the process context fails
	 */
	protected void initProcess(Process process, Relatable initParams)
		throws ProcessException, ServiceException {
		if (initParams != null) {
			ObjectRelations.copyRelations(initParams, process, true);
		}
	}

	/**
	 * Overridden to cancel any processes that remained active in the given
	 * session when the user closed the browser window.
	 *
	 * @see AuthenticatedServiceImpl#resetSessionData(SessionData)
	 */
	@Override
	protected void resetSessionData(SessionData sessionData) {
		cancelActiveProcesses(sessionData);
	}

	/**
	 * Can be invoked by subclasses to set the (main) application process. This
	 * will also set the return value of {@link #hasProcessAuthentication()} to
	 * true. This method should be invoked before any other process definitions
	 * are created through {@link #createProcessDescriptions(Class, List)}.
	 *
	 * @param processDefinition The class of the application process definition
	 */
	protected void setApplicationProcess(
		Class<? extends ProcessDefinition> processDefinition) {
		appProcess = Option.of(processDefinition);

		// add to process definition list
		createProcessDescriptions(processDefinition, null);
	}

	/**
	 * Wraps exceptions that may occur during the execution of one of the
	 * service methods into a {@link ServiceException} if necessary. In case of
	 * an {@link InvalidParametersException} the service exception will contain
	 * information about the invalid parameters.
	 *
	 * @param e            The exception to handle
	 * @param processState The current process state
	 * @return The resulting {@link ServiceException}
	 */
	protected ServiceException wrapException(Throwable e,
		ProcessState processState) {
		ServiceException result;

		if (e instanceof ServiceException) {
			result = (ServiceException) e;
		} else {
			String message = e.getMessage();

			if (e instanceof InvalidParametersException) {
				Map<RelationType<?>, String> paramMessageMap =
					((InvalidParametersException) e).getInvalidParams();

				Map<String, String> invalidParams =
					new HashMap<String, String>(paramMessageMap.size());

				for (Entry<RelationType<?>, String> entry :
					paramMessageMap.entrySet()) {
					invalidParams.put(entry.getKey().getName(),
						entry.getValue());
				}

				result =
					new ServiceException(message, invalidParams, processState);
			} else if (e.getCause() instanceof ConcurrentEntityModificationException) {
				String entityId =
					((ConcurrentEntityModificationException) e.getCause()).getEntityId();

				Map<String, String> lockedEntity =
					new HashMap<String, String>(1);

				lockedEntity.put(ERROR_LOCKED_ENTITY_ID, entityId);

				result = new ServiceException(ERROR_ENTITY_LOCKED,
					lockedEntity,
					processState);
			} else {
				result = new ServiceException(message, e);
			}
		}

		return result;
	}

	/**
	 * Applies the list of modified entities in a process to the given process
	 * state.
	 *
	 * @param process      The process to read the modification from
	 * @param processState The process state to apply the modifications too
	 */
	private void applyModifiedEntities(Process process,
		ProcessState processState) {
		Map<String, Entity> modifiedEntities =
			process.get(CONTEXT_MODIFIED_ENTITIES);

		if (!modifiedEntities.isEmpty()) {
			StringBuilder locks = new StringBuilder();

			for (Entity lockedEntity : modifiedEntities.values()) {
				if (!lockedEntity.hasFlag(MetaTypes.LOCKED)) {
					locks.append(lockedEntity.getGlobalId());
					locks.append(",");
				}
			}

			if (locks.length() > 0) {
				locks.setLength(locks.length() - 1);

				processState.setProperty(PROCESS_ENTITY_LOCKS,
					locks.toString());
			}
		}
	}

	/**
	 * Checks whether the given process description is for an application
	 * process and a corresponding process already exists in the given process
	 * collection.
	 *
	 * @param description The application process description
	 * @param processes   The processes to search
	 * @return The application process to re-use or NULL for none
	 */
	private Process checkReuseExistingAppProcess(ProcessDescription description,
		Collection<Process> processes) {
		String processName = description.getName();
		Process process = null;

		if (processName.startsWith(APPLICATION_PROCESS_PATH)) {
			if (appProcess.exists() &&
				processName.endsWith(APPLICATION_MAIN_PROCESS)) {
				// will never fail as exists() is checked above
				processName = appProcess.map(p -> p.getName()).orFail();
			}

			for (Process existingProcess : processes) {
				if (processName.endsWith(existingProcess.getName())) {
					process = existingProcess;

					break;
				}
			}
		}

		return process;
	}

	/**
	 * Collects all modified data element from a hierarchy of data elements. If
	 * a data element list is modified it will be added to the target
	 * collection. Otherwise it's children will be checked recursively.
	 *
	 * @param dataElements     The data elements to check for modification
	 * @param modifiedElements A collection to add modified elements to
	 */
	private void collectModifiedDataElements(
		Collection<DataElement<?>> dataElements,
		Collection<DataElement<?>> modifiedElements) {
		for (DataElement<?> dataElement : dataElements) {
			boolean changed = dataElement.hasFlag(VALUE_CHANGED) |
				dataElement.hasFlag(PROPERTIES_CHANGED);

			if (dataElement instanceof DataElementList) {
				if (dataElement.hasFlag(STRUCTURE_CHANGED)) {
					modifiedElements.add(dataElement);
				} else {
					if (changed) {
						modifiedElements.add(
							dataElement.copy(CopyMode.PROPERTIES));
					}

					collectModifiedDataElements(
						((DataElementList) dataElement).getElements(),
						modifiedElements);
				}
			} else if (changed) {
				modifiedElements.add(dataElement);
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
	 * @param interactionStep The process fragment to query the relations from
	 * @param markAsChanged   Whether all data elements should be marked as
	 *                        modified
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	private List<DataElement<?>> createInteractionElements(
		ProcessFragment interactionStep, boolean markAsChanged)
		throws StorageException {
		DataElementFactory factory = getDataElementFactory();

		List<RelationType<?>> interactionParams =
			interactionStep.get(INTERACTION_PARAMS);

		List<DataElement<?>> dataElements =
			new ArrayList<DataElement<?>>(interactionParams.size());

		for (RelationType<?> param : interactionParams) {
			DataElement<?> element =
				factory.getDataElement(interactionStep, param);

			if (element != null) {
				dataElements.add(element);

				if (markAsChanged) {
					element.markAsChanged();
				}
			}
		}

		return dataElements;
	}

	/**
	 * Creates a new process instance and sets the standard parameters on it.
	 *
	 * @param definition  The process definition
	 * @param sessionData The data of the current session
	 * @return The new process instance
	 * @throws ProcessException If creating the instance fails
	 */
	private Process createProcess(ProcessDefinition definition,
		SessionData sessionData) throws ProcessException {
		Process process = ProcessManager.getProcess(definition);
		Entity user = sessionData.get(SessionData.SESSION_USER);

		process.setParameter(SESSION_MANAGER, this);
		process.setParameter(EXTERNAL_SERVICE_ACCESS, this);
		process.setParameter(STORAGE_ADAPTER_REGISTRY, this);
		process.setParameter(PROCESS_USER, user);
		process.setParameter(PROCESS_LOCALE,
			getThreadLocalRequest().getLocale());

		return process;
	}

	/**
	 * Creates a new {@link ProcessState} instance from a certain process.
	 * Invoked by {@link #executeProcess(ProcessDescription, Relatable)}.
	 *
	 * @param description The process definition
	 * @param process     The process
	 * @param refresh     TRUE if all data elements should be refreshed even if
	 *                    not marked as modified
	 * @return A new process state object
	 * @throws StorageException If the creation of a storage-based interaction
	 *                          data element fails
	 */
	@SuppressWarnings("boxing")
	private ProcessState createProcessState(ProcessDescription description,
		Process process, boolean refresh) throws StorageException {
		Integer processId = process.getParameter(PROCESS_ID);
		String processInfo = process.getParameter(PROCESS_INFO);
		ProcessState processState;

		if (process.isFinished()) {
			processState =
				new ProcessState(description, processId, processInfo);
		} else {
			ProcessStep interactionStep = process.getInteractionStep();

			List<DataElement<?>> interactionElements =
				createInteractionElements(interactionStep, refresh);

			List<DataElementList> viewElements =
				createViewDataElements(interactionStep);

			if (!refresh) {
				interactionElements =
					reduceToModifiedElements(interactionElements);
			}

			processState = new ProcessState(description, processId,
				processInfo,
				interactionStep.getName(), interactionElements, viewElements,
				getSpawnProcesses(process),
				getProcessStateFlags(process, interactionStep));

			if (process.hasFlagParameter(MetaTypes.AUTHENTICATED)) {
				processState.setFlag(PROCESS_AUTHENTICATED);
			}

			String style = interactionStep.getParameter(PROCESS_STEP_STYLE);

			if (style != null) {
				processState.setProperty(UserInterfaceProperties.STYLE, style);
			}

			applyModifiedEntities(process, processState);

			// reset modifications after applying to also reset changes from
			// parameter relation listeners that are invoked during application
			interactionStep.resetParameterModifications();
		}

		return processState;
	}

	/**
	 * Creates the data elements for certain relations of a relatable object.
	 * For each relation a single data element will be created by invoking the
	 * method {@link #createDataElement(Relatable, RelationType, Set)}. If that
	 * method returns NULL no data element will be added to the result for the
	 * respective relation.
	 *
	 * @param interactionStep object The related object to query the relations
	 *                        from
	 * @return A new list containing the resulting data elements
	 * @throws StorageException If the initialization of a storage-based data
	 *                          element fails
	 */
	private List<DataElementList> createViewDataElements(
		ProcessFragment interactionStep) throws StorageException {
		DataElementFactory factory = getDataElementFactory();

		Set<RelationType<List<RelationType<?>>>> viewParams =
			interactionStep.get(VIEW_PARAMS);

		List<DataElementList> viewElements = null;

		if (viewParams.size() > 0) {
			viewElements = new ArrayList<>(viewParams.size());

			for (RelationType<List<RelationType<?>>> viewParam : viewParams) {
				viewElements.add(
					(DataElementList) factory.getDataElement(interactionStep,
						viewParam));
			}
		}

		return viewElements;
	}

	/**
	 * Returns the data element factory of this service.
	 *
	 * @return The data element factory
	 */
	private final DataElementFactory getDataElementFactory() {
		return dataElementFactory;
	}

	/**
	 * Returns the process that is associated with a certain process
	 * description
	 * or process state and after preparing it for execution.
	 *
	 * @param description The process description or process state,
	 *                       depending on
	 *                    the current execution state of the process
	 * @param sessionData The data of the current session
	 * @param initParams  Optional process initialization parameters or NULL
	 *                      for
	 *                    none
	 * @return The prepared process
	 * @throws ProcessException         If accessing the process fails
	 * @throws ServiceException         If the client is not authenticated or
	 *                                  preparing the process context fails
	 * @throws StorageException         If accessing a storage fails
	 * @throws IllegalArgumentException If the given process description is
	 *                                  invalid
	 */
	@SuppressWarnings("boxing")
	private Process getProcess(ProcessDescription description,
		SessionData sessionData, Relatable initParams)
		throws ProcessException, ServiceException, StorageException {
		Process process = null;

		Map<Integer, Process> userProcessMap =
			sessionData.get(USER_PROCESS_MAP);

		if (description.getClass() == ProcessDescription.class) {
			// if the user reloads the browser windows the existing process can
			// be re-used instead of creating a new one
			process = checkReuseExistingAppProcess(description,
				userProcessMap.values());

			if (process != null) {
				process.set(INTERACTION_EVENT_PARAM, RELOAD_CURRENT_STEP);
			} else {
				ProcessDefinition definition =
					processDefinitions.get(description.getDescriptionId());

				process = createProcess(definition, sessionData);

				getSessionContext().get(PROCESS_LIST).add(process);
				userProcessMap.put(process.getParameter(PROCESS_ID), process);
				initProcess(process, initParams);
				setProcessInput(process, description.getProcessInput());
			}
		} else if (description instanceof ProcessState) {
			ProcessState processState = (ProcessState) description;

			process = userProcessMap.get(processState.getProcessId());

			if (process == null) {
				throw new IllegalStateException("NoProcessFound");
			}

			updateProcess(process, processState);

			if (hasProcessAuthentication()) {
				initProcess(process, initParams);
			}
		} else {
			throw new IllegalArgumentException(
				"Unknown process reference: " + description);
		}

		return process;
	}

	/**
	 * Returns a set of flags for the current state of a process.
	 *
	 * @param process         The process
	 * @param interactionStep The current interaction step
	 */
	private Set<ProcessStateFlag> getProcessStateFlags(Process process,
		ProcessStep interactionStep) {
		Set<ProcessStateFlag> stepFlags = new HashSet<ProcessStateFlag>();

		if (process.canRollbackToPreviousInteraction()) {
			stepFlags.add(ProcessStateFlag.ROLLBACK);
		}

		if (interactionStep.hasFlag(AUTO_CONTINUE) ||
			interactionStep.hasFlag(AUTO_UPDATE)) {
			stepFlags.add(ProcessStateFlag.AUTO_CONTINUE);
		}

		if (interactionStep.hasFlag(FINAL_STEP)) {
			stepFlags.add(ProcessStateFlag.FINAL_STEP);
		}

		if (interactionStep.hasFlag(IMMEDIATE_INTERACTION)) {
			stepFlags.add(ProcessStateFlag.HAS_IMMEDIATE_INTERACTION);
		}

		return stepFlags;
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
	 * @param process The process to query the new processes from
	 * @return A list of process states or NULL for none
	 */
	private List<ProcessState> getSpawnProcesses(Process process) {
		List<ProcessState> spawnProcesses =
			process.getParameter(SPAWN_PROCESSES);

		if (spawnProcesses.size() > 0) {
			spawnProcesses = new ArrayList<>(spawnProcesses);
		} else {
			spawnProcesses = null;
		}

		process.getParameter(SPAWN_PROCESSES).clear();

		return spawnProcesses;
	}

	/**
	 * Searches all modified elements in a hierarchy of data elements. If a
	 * data
	 * element list is modified it will be added to the target collection.
	 * Otherwise it's children will be checked recursively.
	 *
	 * @param dataElements The data elements to check for modification
	 * @return A list containing the modified elements from the input
	 * collection
	 */
	private List<DataElement<?>> reduceToModifiedElements(
		Collection<DataElement<?>> dataElements) {
		List<DataElement<?>> modifiedElements = new ArrayList<>();

		collectModifiedDataElements(dataElements, modifiedElements);

		return modifiedElements;
	}

	/**
	 * Sets properties of the current client (e.g. info, locale) as process
	 * parameters.
	 *
	 * @param description The process description containing the client
	 *                    properties
	 * @param process     The process to set the parameters in
	 */
	private void setClientProperties(ProcessDescription description,
		Process process) {
		String clientInfo = description.getClientInfo();
		String clientLocale = description.getClientLocale();

		process.set(CLIENT_LOCALE, defaultLocale);

		if (clientInfo != null) {
			process.set(CLIENT_INFO, clientInfo);
		}

		if (clientLocale != null) {
			try {
				process.set(CLIENT_LOCALE,
					Locale.forLanguageTag(clientLocale));
			} catch (Exception e) {
				Log.warn("Unknown client locale: " + clientLocale, e);
			}
		}
	}

	/**
	 * Sets the process input parameters from a data element or a list of data
	 * elements.
	 *
	 * @param process      The process
	 * @param processInput The process input data element(s)
	 * @throws AuthenticationException If the current user is not authenticated
	 * @throws StorageException        If accessing storage data fails
	 */
	private void setProcessInput(Process process, DataElement<?> processInput)
		throws StorageException, AuthenticationException {
		if (processInput != null) {
			List<DataElement<?>> inputValues = new ArrayList<DataElement<?>>();

			if (processInput instanceof DataElementList) {
				for (DataElement<?> inputValue :
					(DataElementList) processInput) {
					inputValues.add(inputValue);
				}
			} else {
				inputValues.add(processInput);
			}

			getDataElementFactory().applyDataElements(inputValues, process);
		}
	}

	/**
	 * Updates a process from a certain process state that has been received
	 * from the client.
	 *
	 * @param process      The process to update
	 * @param processState The process state to update the process from
	 * @throws AuthenticationException If the client is not authenticated
	 * @throws StorageException        If a storage access fails
	 * @throws IllegalStateException   If the process is NULL
	 */
	@SuppressWarnings("boxing")
	private void updateProcess(Process process, ProcessState processState)
		throws AuthenticationException, StorageException {
		ProcessExecutionMode mode = processState.getExecutionMode();

		RelationType<?> interactionParam = null;

		if (mode == ProcessExecutionMode.RELOAD) {
			interactionParam = RELOAD_CURRENT_STEP;
		} else {
			if (mode == ProcessExecutionMode.EXECUTE) {
				List<DataElement<?>> interactionParams =
					processState.getInteractionParams();

				List<DataElementList> viewParams =
					processState.getViewParams();

				DataElementFactory dataElementFactory =
					getDataElementFactory();

				dataElementFactory.applyDataElements(interactionParams,
					process);

				if (!viewParams.isEmpty()) {
					dataElementFactory.applyDataElements(viewParams, process);
				}
			}

			DataElement<?> interactionElement =
				processState.getInteractionElement();

			if (interactionElement != null) {
				interactionParam =
					RelationType.valueOf(interactionElement.getName());

				if (interactionParam == null) {
					throw new IllegalStateException(
						"Unknown interaction parameter: " + interactionParam);
				}

				InteractionEventType eventType =
					processState.getInteractionEventType();

				process.setParameter(INTERACTION_EVENT_TYPE, eventType);
			} else {
				process.deleteRelation(INTERACTION_EVENT_TYPE);
			}
		}

		process.setParameter(INTERACTION_EVENT_PARAM, interactionParam);
	}
}
