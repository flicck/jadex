package jadex.bdi.examples.hunterprey_env.cleverprey;

import jadex.adapter.base.envsupport.environment.ISpaceAction;
import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.environment.space2d.Grid2D;
import jadex.adapter.base.envsupport.environment.space2d.Space2D;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.bdi.examples.hunterprey_env.MoveAction;
import jadex.bdi.runtime.Plan;

import java.util.HashMap;
import java.util.Map;

/**
 *  A plan to explore the map.
 */
public class EatPlan extends Plan
{
	/**
	 *  Plan body.
	 */
	public void body()
	{
		Grid2D	env	= (Grid2D)getBeliefbase().getBelief("env").getFact();
		ISpaceObject	myself	= (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
		ISpaceObject	food	= (ISpaceObject)getParameter("food").getValue();
		
		// Move towards food until position reached.
		while(!myself.getProperty(Space2D.POSITION).equals(food.getProperty(Space2D.POSITION)))
		{
			String	move	= MoveAction.getDirection(env, (IVector2)myself.getProperty(Space2D.POSITION),
				(IVector2)food.getProperty(Space2D.POSITION));
			SyncResultListener srl	= new SyncResultListener();
			Map params = new HashMap();
			params.put(ISpaceAction.ACTOR_ID, getAgentIdentifier());
			params.put(MoveAction.PARAMETER_DIRECTION, move);
			env.performSpaceAction("move", params, srl);
			srl.waitForResult();
		}

		// Eat food.
		SyncResultListener srl	= new SyncResultListener();
		Map params = new HashMap();
		params.put(ISpaceAction.ACTOR_ID, getAgentIdentifier());
		params.put(ISpaceAction.OBJECT_ID, food);
		env.performSpaceAction("eat", params, srl);
		srl.waitForResult();
	}

	/**
	 *  Called, when the plan fails.
	 */
	public void failed()
	{
		// Move failed, forget food until seen again.
		getBeliefbase().getBeliefSet("food").removeFact(getParameter("food").getValue());
	}
}
