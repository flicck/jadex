package jadex.adapter.base.envsupport.environment;

import jadex.commons.IFilter;
import jadex.commons.concurrent.IResultListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *  The list of scheduled agent actions and convenience methods for
 *  executing selected actions.
 *  This implementation is not thread-safe, i.e. methods
 *  should only be called from threads that are already synchronized
 *  with the environment space monitor.
 */
public class AgentActionList 
{
	//-------- attributes --------

	/** The environment space. */
	protected IEnvironmentSpace	space;

	/** The scheduled actions. */
	protected Set	actions;
	
	/** The executed actions where actors still need to be woken up. */
	protected Collection	executed;
	
	//-------- constructors --------
	
	/**
	 *  Create a new action list.
	 */
	public AgentActionList(IEnvironmentSpace space)
	{
		this.space	= space;
	}
	
	//-------- methods --------
	
	/**
	 * Schedules an agent action.
	 * @param action	The action.
	 * @param parameters parameters for the action (may be null)
	 * @param listener the result listener
	 */
	public void scheduleAgentAction(ISpaceAction action, Map parameters, IResultListener listener)
	{
		if(actions==null)
			actions	= new LinkedHashSet();
		
		actions.add(new ActionEntry(action, parameters, listener));
	}

	/**
	 *  Set an ordering used for executing actions.
	 *  @param comp	The comparator representing the ordering.
	 */
	public void	setOrdering(Comparator comp)
	{
		if(actions!=null)
		{
			Set	tmp	= new TreeSet(comp);
			tmp.addAll(actions);
			actions	= tmp;
		}
		else
		{
			actions	= new TreeSet(comp);
		}
	}
	
	/**
	 *  Should be called on environment thread only.
	 *  @param filter	A filter to select only a subset of actions (or null for all actions).
	 *  @param wakeup	Immediately wake up each calling agent after its action has been executed
	 *  (otherwise wakeupAgents() has to be called separately).
	 */
	public void executeActions(IFilter filter, boolean wakeup)
	{
		if(actions!=null && !(actions.isEmpty()))
		{
			for(Iterator it=actions.iterator(); it.hasNext(); )
			{
				ActionEntry entry = (ActionEntry)it.next();
				try
				{
					if(filter==null || filter.filter(entry))
					{
						it.remove();
						try
						{
//							System.out.println("Action: "+entry);
							
							Object ret = entry.action.perform(entry.parameters, space);
							if(entry.listener!=null)
							{
								if(wakeup)
								{
									entry.listener.resultAvailable(ret);
								}
								else
								{
									entry.result	= ret;
									if(executed==null)
										executed	= new ArrayList();
									executed.add(entry);
								}
							}
						}
						catch(Exception e)
						{
							if(entry.listener!=null)
							{
								if(wakeup)
								{
									entry.listener.exceptionOccurred(e);
								}
								else
								{
									entry.exception	= e;
									if(executed==null)
										executed	= new ArrayList();
									executed.add(entry);
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 *  Should be called on environment thread only.
	 *  @param filter	A filter to select only a subset of actions (or null for all actions).
	 *  (otherwise wakeupAgents() has to be called separately).
	 */
	public void wakeupAgents(IFilter filter)
	{
		if(executed!=null && !(executed.isEmpty()))
		{
			for(Iterator it=executed.iterator(); it.hasNext(); )
			{
				ActionEntry entry = (ActionEntry)it.next();
				try
				{
					if(filter==null || filter.filter(entry))
					{
						it.remove();
						if(entry.exception==null)
						{
							entry.listener.resultAvailable(entry.result);
						}
						else
						{
							entry.listener.exceptionOccurred(entry.exception);
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	//-------- helper classes --------
	
	/**
	 *  Entry for a scheduled action.
	 */
	public static class ActionEntry	implements Comparable
	{
		//-------- static part --------
		
		protected static int CNT	= 0;
		
		//-------- attributes --------
		
		/** The action. */
		public ISpaceAction	action;
		
		/** The action parameters. */
		public Map	parameters;
		
		/** The result listener. */
		public IResultListener	listener;
		
		/** The result (set after successful execution). */
		public Object	result;
		
		/** The exception (set after failed execution). */
		public Exception	exception;
		
		/** An id to differentiate otherwise equal actions. */
		public int	id;
		
		//-------- constructors --------
		
		/**
		 *  Convenience constructor for inline entry creation.
		 */
		public ActionEntry(ISpaceAction action, Map parameters, IResultListener listener)
		{
			 this.action	= action;
			 this.parameters	= parameters;
			 this.listener	= listener;
			 synchronized(ActionEntry.class)
			 {
				 this.id	= CNT++;
			 }
		}
		
		//-------- Comparable interface --------
		
		/**
		 *  Compare two action entries.
		 */
		public int compareTo(Object obj)
		{
			return id - ((ActionEntry)obj).id;
		}
		
		//-------- methods --------
		
		/**
		 *  Create a string representation of the action.
		 */
		public String	toString()
		{
			return ""+action+parameters;
		}
	}

	
//	/**
//	 * 
//	 */
//	public static class OwnerFilter implements IFilter
//	{
//		/** The owner. */
//		protected Object owner;
//	
//		/**
//		 *  Test if an object passes the filter.
//		 *  @return True, if passes the filter.
//		 */
//		public boolean filter(Object obj)
//		{
//			boolean ret = false;
//			Entry entry = (Entry)obj;
//			
//			if(entry.getMetainfo() instanceof DefaultEntryMetaInfo)
//			{
//				DefaultEntryMetaInfo mi = (DefaultEntryMetaInfo)entry.getMetainfo();
//				ret = SUtil.equals(owner, mi.getOwner());
//			}
//			
//			return ret;
//		}
//	}
}
