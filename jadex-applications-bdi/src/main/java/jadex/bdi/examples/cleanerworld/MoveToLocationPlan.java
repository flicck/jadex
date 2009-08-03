package jadex.bdi.examples.cleanerworld;

import java.util.HashMap;
import java.util.Map;

import jadex.adapter.base.envsupport.environment.IEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.bdi.runtime.Plan;

/**
 *  The move to a location plan.
 */
public class MoveToLocationPlan extends Plan
{
	//-------- attributes --------
	
	/** The task id. */
	protected Object taskid;
	
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		ISpaceObject myself	= (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
		IVector2 dest = (IVector2)getParameter("location").getValue();
		
		SyncResultListener	res	= new SyncResultListener();
		Map props = new HashMap();
		props.put(MoveTask.PROPERTY_DESTINATION, dest);
		taskid = space.createObjectTask(MoveTask.PROPERTY_TYPENAME, props, myself.getId());
		space.addTaskListener(taskid, myself.getId(), res);
		
		try
		{
			res.waitForResult();
		}
		catch(Exception e)
		{
			fail(e);
		}
	}

	/**
	 *  Remove the task, when the plan is aborted. 
	 */
	public void aborted()
	{
//		System.out.println("aborted: "+this);
		if(taskid!=null)
		{
			ISpaceObject myself	= (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
			IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			space.removeObjectTask(taskid, myself.getId());
		}
	}
	
	/**
	 *  Remove the task, when the plan has failed. 
	 */
	public void failed()
	{
//		System.out.println("failed: "+this);
//		getException().printStackTrace();
		if(taskid!=null)
		{
			ISpaceObject myself	= (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
			IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			space.removeObjectTask(taskid, myself.getId());
		}
	}
}
	
