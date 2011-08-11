package jadex.simulation.analysis.process.analyse.tasks;

import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.runtime.ITaskContext;
import jadex.bpmn.runtime.task.ParameterMetaInfo;
import jadex.bpmn.runtime.task.TaskMetaInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.IAExperimentBatch;
import jadex.simulation.analysis.common.events.task.ATaskEvent;
import jadex.simulation.analysis.common.util.AConstants;
import jadex.simulation.analysis.process.basicTasks.ASubProcessTaskView;
import jadex.simulation.analysis.process.basicTasks.ATask;
import jadex.simulation.analysis.process.basicTasks.user.AServiceCallUserTaskView;
import jadex.simulation.analysis.service.basic.view.session.subprocess.ASubProcessView;
import jadex.simulation.analysis.service.highLevel.IAAllgemeinAusfuehrenService;

import java.util.Map;
import java.util.UUID;

public class ExperimentVisualisierenTask extends ATask
{
	public ExperimentVisualisierenTask()
	{
		view = new ASubProcessTaskView(this);
		addTaskListener(view);
	}

	@Override
	public IFuture execute(ITaskContext context, BpmnInterpreter instance)
	{
		super.execute(context, instance);
		IAAllgemeinAusfuehrenService service = (IAAllgemeinAusfuehrenService) SServiceProvider.getService(instance.getServiceProvider(), IAAllgemeinAusfuehrenService.class).get(susThread);
		UUID session = (UUID) service.createSession(null).get(susThread);
		((ASubProcessTaskView)view).init((ASubProcessView) service.getSessionView(session).get(susThread));
		IAExperimentBatch experiments = (IAExperimentBatch) context.getParameterValue("experiments");
		
		Map results = (Map) service.ausführen(session, experiments).get(susThread);
		experiments = (IAExperimentBatch)results.get("experiments");
//		results = (Map) service.nachgelagerteTätigkeiten(null, experiments);
		context.setParameterValue("experiments", (IAExperimentBatch)results.get("experiments"));
		taskChanged(new ATaskEvent(this, context, instance, AConstants.TASK_BEENDET));
		return new Future(null);
	}
	
	/**
	 * Get the meta information about the task.
	 */
	public static TaskMetaInfo getMetaInfo()
	{
		String desc = "Visualsiert ein IAExperimentBatch";
			
		ParameterMetaInfo expmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_INOUT,
				IAExperimentBatch.class, "experiments", null, "Ein IAExperimentBatch");

		return new TaskMetaInfo(desc, new ParameterMetaInfo[] {expmi});
	}

}
