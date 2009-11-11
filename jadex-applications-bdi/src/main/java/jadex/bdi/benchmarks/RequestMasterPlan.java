package jadex.bdi.benchmarks;

import jadex.bdi.runtime.Plan;
import jadex.bridge.IComponentExecutionService;

import java.util.HashMap;
import java.util.Map;

/**
 *	Start the partner agent.
 */
public class RequestMasterPlan extends Plan
{
	public void body()
	{
		Map	args	= new HashMap();
		args.put("master", Boolean.FALSE);
		args.put("max", getBeliefbase().getBelief("max").getFact());
		args.put("receiver", getAgentIdentifier());
		
		IComponentExecutionService	ces	= (IComponentExecutionService)getScope().getServiceContainer().getService(IComponentExecutionService.class);
		ces.createComponent(null, "jadex/bdi/benchmarks/RequestPerformance.agent.xml", "default", args, false, null, getAgentIdentifier());
	}	
}
