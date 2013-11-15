package jadex.bdiv3.examples.disastermanagement.firebrigade;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Body;
import jadex.bdiv3.annotation.Capability;
import jadex.bdiv3.annotation.Deliberation;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalCreationCondition;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Plans;
import jadex.bdiv3.annotation.Publish;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.examples.disastermanagement.IClearChemicalsService;
import jadex.bdiv3.examples.disastermanagement.IExtinguishFireService;
import jadex.bdiv3.examples.disastermanagement.movement.IDestinationGoal;
import jadex.bdiv3.examples.disastermanagement.movement.MoveToLocationPlan;
import jadex.bdiv3.examples.disastermanagement.movement.MovementCapa;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bridge.service.annotation.Service;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.math.IVector2;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;

/**
 * 
 */
@Agent
@Service
//@ProvidedServices(
//{
//	@ProvidedService(type=IExtinguishFireService.class, implementation=@Implementation(ExtinguishFireService.class)),
//	@ProvidedService(type=IClearChemicalsService.class, implementation=@Implementation(ClearChemicalsService.class))
//})
@Plans(
{
	@Plan(trigger=@Trigger(goals=FireBrigadeBDI.ExtinguishFire.class), body=@Body(ExtinguishFirePlan.class)),
	@Plan(trigger=@Trigger(goals=FireBrigadeBDI.ClearChemicals.class), body=@Body(ClearChemicalsPlan.class)),
	@Plan(trigger=@Trigger(goals=FireBrigadeBDI.GoHome.class), body=@Body(MoveToLocationPlan.class))
})
@Configurations({@Configuration(name="do_nothing"), @Configuration(name="default")})
public class FireBrigadeBDI
{
	/** The capa. */
	@Capability
	protected MovementCapa movecapa = new MovementCapa();
	
	/** The agent. */
	@Agent
	protected BDIAgent agent;
	
	/**
	 * 
	 */
	@AgentBody
	public void body()
	{
		if("default".equals(agent.getConfiguration()))
		{
			agent.adoptPlan(new FireBrigadePlan());
		}
	}
	
	/**
	 * 
	 */
	@Goal
	public static class GoHome implements IDestinationGoal
	{
		/** The home position. */
		protected IVector2 home;
		
		/**
		 *  Create a new CarryOre. 
		 */
		public GoHome(IVector2 home)
		{
			this.home = home;
		}
		
		@GoalCreationCondition(rawevents={ChangeEvent.GOALADOPTED, ChangeEvent.GOALDROPPED})
		public static GoHome checkCreate(FireBrigadeBDI ag)
		{
			MovementCapa capa = ag.getMoveCapa();
			if(capa.getCapability().getAgent().getGoals().size()==0 && capa.getHomePosition()!=null && capa.getPosition()!=null
				&& capa.getEnvironment().getDistance(capa.getHomePosition(), capa.getPosition()).getAsDouble()>0.001)
			{
				return new GoHome(capa.getHomePosition());
			}
			else
			{
				return null;
			}
		}
		
		/**
		 *  Get the destination.
		 *  @return The destination.
		 */
		public IVector2 getDestination()
		{
			return home;
		}
	}
	
	/**
	 * 
	 */
	@Goal(deliberation=@Deliberation(cardinalityone=true), 
		publish=@Publish(type=IExtinguishFireService.class, method="extinguishFire"))
	public static class ExtinguishFire
	{
		/** The disaster. */
		protected ISpaceObject disaster;

		/**
		 *  Create a new ExtinguishFire. 
		 */
		public ExtinguishFire(ISpaceObject disaster)
		{
			this.disaster = disaster;
		}
		
		/**
		 * 
		 */
		@GoalTargetCondition
		public boolean checkTarget()
		{
			Integer fires = (Integer)getDisaster().getProperty("fire");
			return fires!=null && fires.intValue()==0;
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}
	}
	
	/**
	 * 
	 */
	@Goal(deliberation=@Deliberation(cardinalityone=true), 
		publish=@Publish(type=IClearChemicalsService.class, method="clearChemicals"))
	public static class ClearChemicals
	{
		/** The disaster. */
		protected ISpaceObject disaster;

		/**
		 *  Create a new ExtinguishFire. 
		 */
		public ClearChemicals(ISpaceObject disaster)
		{
			this.disaster = disaster;
		}
		
		/**
		 * 
		 */
		@GoalTargetCondition
		public boolean checkTarget()
		{
			Integer fires = (Integer)getDisaster().getProperty("chemicals");
			return fires!=null && fires.intValue()==0;
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}
	}
	
	/**
	 *  Get the movecapa.
	 *  @return The movecapa.
	 */
	public MovementCapa getMoveCapa()
	{
		return movecapa;
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public BDIAgent getAgent()
	{
		return agent;
	}
}



