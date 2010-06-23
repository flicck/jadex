package deco4mas.examples.agentNegotiation.sma.workflow.management;

import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IInternalEvent;
import jadex.bdi.runtime.Plan;
import jadex.bridge.CreationInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.commons.IFuture;
import jadex.commons.concurrent.IResultListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import deco4mas.examples.agentNegotiation.evaluate.AgentLogger;
import deco4mas.examples.agentNegotiation.evaluate.ValueLogger;

/**
 * Creates a workflow and init the necessary negotiations
 */
public class PreInstantiateWorkflowPlan extends Plan
{
	/* id of the workflow */
	private static Integer workflowID = new Integer(0);

	public void body()
	{
		try
		{
			// name of new workflow
			final String workflowName = "workflowNet" + workflowID + "(" + getComponentName() + ")";

			// get Loggers
			final Logger workflowLogger = AgentLogger.getTimeEvent(workflowName);
			final Logger providerLogger = AgentLogger.getTimeEvent(this.getComponentName());
			final Logger workflowTimeLogger = AgentLogger.getTimeEvent("workflowTimes (" + this.getComponentName() + ")");

			// create workflow
			Map workflowArgs = new HashMap();
			workflowArgs.put("sma", getComponentIdentifier());

			// listener at component kill / workflow execution
			final IBDIExternalAccess myExternalAccess = this.getExternalAccess();
			IResultListener killListener = new IResultListener()
			{
				/** called when workflow is executed */
				@Override
				public void resultAvailable(Object source, Object result)
				{
					// LOG
					workflowLogger.info("workflow executed");
					providerLogger.info("*** workflow executed ***");
					Long executionTime = getTime() - (Long) getBeliefbase().getBelief("statExetime").getFact();
					Long allTime = getTime() - (Long) getBeliefbase().getBelief("statNegtime").getFact();
					workflowTimeLogger.info("Execution time: " + executionTime);
					ValueLogger.addValue("workflowTime", allTime.doubleValue());
					workflowTimeLogger.info("Complete time: " + allTime);
					workflowTimeLogger.info("---");
					System.out.println("*** workflow executed ***");

					// workflow executed -> init InternalEvent
					IInternalEvent workflowExecuted = myExternalAccess.createInternalEvent("workflowExecuted");
					myExternalAccess.dispatchInternalEvent(workflowExecuted);
				}

				/** called when workflow throw exception */
				@Override
				public void exceptionOccurred(Object source, Exception exception)
				{
					// normally not called / just log
					workflowLogger.severe("workflow not finished");
					providerLogger.severe("workflow not finished");
					workflowTimeLogger.severe("workflow not finished");
					System.out.println("workflow not finished");
				}
			};
			// now create workflow component
			String workflowType = "deco4mas/examples/agentNegotiation/sma/workflow/implementation/"
				+ (String) getBeliefbase().getBelief("workflowName").getFact() + ".bpmn";
			IFuture workflowFuture = ((IComponentManagementService) interpreter.getAgentAdapter().getServiceContainer().getService(
				IComponentManagementService.class)).createComponent(workflowName, workflowType, new CreationInfo(null, workflowArgs, this
				.getComponentIdentifier(), true, false), killListener);
			IComponentIdentifier workflowIdentifier = (IComponentIdentifier) workflowFuture.get(this);
			getBeliefbase().getBelief("workflow").setFact(workflowIdentifier);

			// LOG
			providerLogger.info("workflow [" + workflowName + "] created");
			workflowLogger.info("workflow created");

			// create smas
			IGoal goal = createGoal("defineOffers");
			goal.getParameter("workflow").setValue(workflowIdentifier);
			dispatchSubgoal(goal);
			workflowID++;

			waitForInternalEvent("workflowExecuted");
			IGoal restartWorkflow = createGoal("restartWorkflow");
			dispatchTopLevelGoal(restartWorkflow);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e);
		}
	}
}
