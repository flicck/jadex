package jadex.bdi.wfms.client.cap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jadex.adapter.base.fipa.Done;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.wfms.AbstractWfmsPlan;
import jadex.bdi.wfms.ontology.RequestBeginActivity;
import jadex.bdi.wfms.ontology.RequestWorkitemList;
import jadex.commons.SReflect;
import jadex.service.library.ILibraryService;
import jadex.wfms.client.IClientActivity;
import jadex.wfms.client.IWorkitem;
import jadex.wfms.client.Workitem;

public class BeginActivityPlan extends AbstractWfmsPlan
{
	public void body()
	{
		RequestBeginActivity rba = new RequestBeginActivity();
		rba.setWorkitem((IWorkitem) getParameter("workitem").getValue());
		
		IGoal baRequestGoal = createGoal("reqcap.rp_initiate");
		baRequestGoal.getParameter("action").setValue(rba);
		baRequestGoal.getParameter("receiver").setValue(getClientInterface());
		
		dispatchSubgoalAndWait(baRequestGoal);
		
		Done done = (Done) baRequestGoal.getParameter("result").getValue();
		IClientActivity activity = ((RequestBeginActivity) done.getAction()).getActivity();
		getParameter("activity").setValue(activity);
	}

}
