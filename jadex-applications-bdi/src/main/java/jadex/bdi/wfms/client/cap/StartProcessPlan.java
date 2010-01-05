package jadex.bdi.wfms.client.cap;

import jadex.adapter.base.fipa.Done;
import jadex.bdi.runtime.GoalFailureException;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.wfms.AbstractWfmsPlan;
import jadex.bdi.wfms.ontology.RequestModelNames;
import jadex.bdi.wfms.ontology.RequestProcessStart;

import java.util.Set;

public class StartProcessPlan extends AbstractWfmsPlan
{
	public void body()
	{
		RequestProcessStart rps = new RequestProcessStart((String) getParameter("process_name").getValue());
		
		IGoal startGoal = createGoal("reqcap.rp_initiate");
		startGoal.getParameter("action").setValue(rps);
		startGoal.getParameter("receiver").setValue(getClientInterface());
		dispatchSubgoalAndWait(startGoal);
	}
}
