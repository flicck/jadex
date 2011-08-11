package jadex.simulation.analysis.service.basic.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;

import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bridge.CreationInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.kernelbase.ExternalAccess;
import jadex.micro.annotation.RequiredService;
import jadex.simulation.analysis.common.data.parameter.AParameterEnsemble;
import jadex.simulation.analysis.common.data.parameter.IAParameterEnsemble;
import jadex.simulation.analysis.common.events.service.AServiceEvent;
import jadex.simulation.analysis.common.events.task.ATaskEvent;
import jadex.simulation.analysis.common.events.task.IATaskListener;
import jadex.simulation.analysis.common.util.AConstants;
import jadex.simulation.analysis.process.basicTasks.IATask;
import jadex.simulation.analysis.process.basicTasks.IATaskView;
import jadex.simulation.analysis.service.basic.view.session.ADefaultSessionView;
import jadex.simulation.analysis.service.basic.view.session.IASessionView;
import jadex.simulation.analysis.service.basic.view.session.subprocess.ASubProcessView;
import jadex.simulation.analysis.service.highLevel.IAAllgemeinPlanenService;

public class ASubProcessService extends ABasicAnalysisSessionService
{
	protected Map<UUID, IExternalAccess> sessionValues;

	public ASubProcessService(IExternalAccess access, Class serviceInterface)
	{
		super(access, serviceInterface, true);
		sessionValues = Collections.synchronizedMap(new HashMap<UUID, IExternalAccess>());
	}

	protected IFuture startSubprocess(UUID preSession, String name, String model, Map arguments)
	{
		synchronized (mutex)
		{
			final Future ret = new Future();
			UUID newSession = preSession;
			if (preSession == null) newSession = (UUID) createSession(null).get(susThread);

			final UUID session = newSession;

			IResultListener lis = new IResultListener()
			{
				public void resultAvailable(Object result)
			{
				Map results = null;
				if (result != null)
				{
					results = (Map) result;
				}
				else
				{
					results = new HashMap();
				}
				closeSession(session);
				ret.setResult(results);
			}

				public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
			};

			final IComponentManagementService cms = (IComponentManagementService) SServiceProvider.getService(access.getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(susThread);
			cms.createComponent(name + "(S: " + session.toString() + ")", model,
					new CreationInfo(null, arguments, access.getComponentIdentifier(),
							false, false, false, false, access.getModel().getAllImports(), null), lis).addResultListener(new IResultListener()
						{

							@Override
							public void resultAvailable(Object result)
							{
								final IComponentIdentifier cid = (IComponentIdentifier) result;
								final ExternalAccess access = (ExternalAccess) cms.getExternalAccess(cid).get(susThread);
								((BpmnInterpreter) access.getInterpreter()).setContextVariable("subProcessView", ((ASubProcessView) sessionViews.get(session)));
								((ASubProcessView) sessionViews.get(session)).init(access, session, sessions.get(session));
								sessionValues.put(session, access);
							}

							@Override
							public void exceptionOccurred(Exception exception)
							{
								exception.printStackTrace();
							}
						});
			return ret;
		}
	}

	@Override
	public IFuture createSession(IAParameterEnsemble configuration)
	{
		synchronized (mutex)
		{
			UUID id = UUID.randomUUID();
			if (configuration == null) configuration = new AParameterEnsemble("Session Konfiguration");
			sessions.put(id, configuration);
			configuration.setEditable(false);
			sessionViews.put(id, new ASubProcessView());
			serviceChanged(new AServiceEvent(this, AConstants.SERVICE_SESSION_START, id));
			return new Future(id);
		}
	}

	@Override
	public IFuture getWorkload()
	{
		if (sessions.size() > 0)
		{
			return new Future(new Double(100.0));
		}
		else
		{
			return new Future(new Double(0.0));
		}

	}

}
