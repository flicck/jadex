package jadex.bdiv3.actions;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.GoalAPI;
import jadex.bdiv3.runtime.impl.BDIAgentInterpreter;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan.PlanLifecycleState;
import jadex.bridge.IConditionalComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.lang.reflect.Field;

/**
 * 
 */
public class AdoptGoalAction implements IConditionalComponentStep<Void>
{
	/** The goal. */
	protected RGoal goal;
	
	/** The state. */
	protected PlanLifecycleState state;
	
	/**
	 *  Create a new action.
	 */
	public AdoptGoalAction(RGoal goal)
	{
//		System.out.println("adopting: "+goal.getId()+" "+goal.getPojoElement().getClass().getName());
		this.goal = goal;
		if(goal.getParentPlan()!=null)
			this.state = goal.getParentPlan().getLifecycleState();
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		return (state==null|| state.equals(goal.getParentPlan().getLifecycleState())) 
			&& RGoal.GoalLifecycleState.NEW.equals(goal.getLifecycleState());
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public IFuture<Void> execute(IInternalAccess ia)
	{
		Future<Void> ret = new Future<Void>();
		try
		{
			BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
			// todo: observe class and goal itself!
//			goal.observeGoal(ia);
			
			// inject goal elements
			Class<?> cl = goal.getPojoElement().getClass();
			Field[] fields = cl.getDeclaredFields();
			for(Field f: fields)
			{
				if(f.isAnnotationPresent(GoalAPI.class))
				{
					f.setAccessible(true);
					f.set(goal.getPojoElement(), goal);
				}
			}
			
			ip.getCapability().addGoal(goal);
			goal.setLifecycleState(ia, RGoal.GoalLifecycleState.ADOPTED);
			ret.setResult(null);
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}
}
