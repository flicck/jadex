package jadex.bdiv3.tutorial.stamp;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Body;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Goals;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Plans;
import jadex.bdiv3.annotation.ServicePlan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

@Agent
@Goals(@Goal(clazz=StampGoal.class))
@RequiredServices(@RequiredService(name="stampser", type=IStampService.class, 
	binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)))
@Plans(@Plan(trigger=@Trigger(goals=StampGoal.class), body=@Body(service=@ServicePlan(name="stampser"))))
public class WorkpieceBDI {
	@AgentBody
	public void body(BDIAgent agent) {
		agent.dispatchTopLevelGoal(new StampGoal(agent.getComponentIdentifier(), 
			"date: "+System.currentTimeMillis())).get();
	}
}