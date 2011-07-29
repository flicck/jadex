package jadex.micro.tutorial;

import java.util.Date;

import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

/**
 *  Chat micro agent that uses the clock service. 
 */
@Description("This agent uses the clock service.")
@Agent
@RequiredServices(@RequiredService(name="clockservice", type=IClockService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)))
public class ChatC2Agent
{
	/** The underlying mirco agent. */
	@Agent
	protected MicroAgent agent;
	
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	@AgentBody
	public void executeBody()
	{
		agent.getServiceContainer().getRequiredService("clockservice")
			.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				IClockService cs = (IClockService)result;
				System.out.println("Time for a chat buddy: "+new Date(cs.getTime()));
			}
		});
	}
}