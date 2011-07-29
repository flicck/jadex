package jadex.commons.future;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *  Default implementation of an intermediate future.
 */
public class IntermediateFuture extends Future	implements	IIntermediateFuture
{
	//-------- attributes --------
	
	/** The intermediate results. */
	protected Collection results;
	
	/** Flag indicating that addIntermediateResult()has been called. */
	protected boolean intermediate;
	
	//-------- constructors--------
	
	/**
	 *  Create a future that is already done.
	 *  @param result	The result, if any.
	 */
	public IntermediateFuture()
	{
	}
	
	/**
	 *  Create a future that is already done.
	 *  @param result	The result, if any.
	 */
	public IntermediateFuture(Collection results)
	{
		super(results);
	}
	
	//-------- IIntermediateFuture interface --------
		
    /**
     *  Get the intermediate results that are available.
     *  @return The current intermediate results (copy of the list).
     */
	public synchronized Collection getIntermediateResults()
	{
		return results!=null ? new ArrayList(results) : Collections.emptyList();
	}
	
	//-------- methods --------
	
	/**
	 *  Add an intermediate result.
	 */
	public void	addIntermediateResult(Object result)
	{
	   	synchronized(this)
		{
        	if(resultavailable)
        	{
        		if(this.exception!=null)
        		{
//        			this.exception.printStackTrace();
            		throw new DuplicateResultException(DuplicateResultException.TYPE_EXCEPTION_EXCEPTION, this, this.exception, exception);
        		}
        		else
        		{
            		throw new DuplicateResultException(DuplicateResultException.TYPE_RESULT_EXCEPTION, this, result, exception);        			
        		}
        	}
	   	
        	intermediate = true;
		
			if(results==null)
				results	= new ArrayList();
			
			results.add(result);
			
			if(listeners!=null)
			{
				// Find intermediate listeners to be notified.
				for(int i=0; i<listeners.size(); i++)
				{
					if(listeners.get(i) instanceof IIntermediateResultListener)
					{
						scheduleNotification((IResultListener)listeners.get(i), true, result);
					}
				}
			}
		}

		startScheduledNotifications();
	}
	
	/**
     *  Set the result. 
     *  Listener notifications occur on calling thread of this method.
     *  @param result The result.
     */
    public void	setResult(Object result)
    {
    	boolean ex = false;
    	synchronized(this)
		{
    		if(results!=null)
    			ex = true;
		}
    	if(ex)
    	{
    		throw new RuntimeException("setResult() only allowed without intermediate results:"+results);
    	}
    	else
    	{
    		if(result!=null && !(result instanceof Collection))
    		{
    			throw new IllegalArgumentException("Result must be collection: "+result);
    		}
    		else
    		{
    			if(result!=null)
    				this.results = (Collection)result;
    			super.setResult(results);
    		}
    	}
    }
    
	/**
     *  Set the result. 
     *  Listener notifications occur on calling thread of this method.
     *  @param result The result.
     */
    public void	setResultIfUndone(Object result)
    {
    	boolean ex = false;
    	synchronized(this)
		{
    		if(results!=null)
    			ex = true;
		}
    	if(ex)
    	{
    		throw new RuntimeException("setResultIfUndone() only allowed without intermediate results:"+results);
    	}
    	else
    	{
    		if(result!=null && !(result instanceof Collection))
    		{
    			throw new IllegalArgumentException("Result must be collection: "+result);
    		}
    		else
    		{
    			this.results = (Collection)result;
    			super.setResultIfUndone(result);
    		}
    	}
    }
    
    /**
     *  Declare that the future is finished.
     */
    public void setFinished()
    {
    	Collection	res;
    	synchronized(this)
    	{
    		res	= getIntermediateResults();
			// Hack!!! Set results to avoid inconsistencies between super.result and this.results,
    		// because getIntermediateResults() returns empty list when results==null.
    		if(results==null)
    		{
    			results	= res;
    		}
    	}
    	super.setResult(res);
    }
    
    /**
     *  Add a result listener.
     *  @param listsner The listener.
     */
    public void	addResultListener(IResultListener listener)
    {
    	if(listener==null)
    		throw new RuntimeException();
    	
    	synchronized(this)
    	{
    		if(intermediate && listener instanceof IIntermediateResultListener)
    		{
    			Object[]	inter = results.toArray();
	    		IIntermediateResultListener lis =(IIntermediateResultListener)listener;
	    		for(int i=0; i<inter.length; i++)
	    		{
	    			scheduleNotification(lis, true, inter[i]);
	    		}
    		}
    		
	    	if(resultavailable)
	    	{
	    		scheduleNotification(listener, false, null);
	    	}
	    	else
	    	{
	    		if(listeners==null)
	    			listeners	= new ArrayList();
	    		listeners.add(listener);
	    	}
    	}

    	startScheduledNotifications();
    }
    
    /**
     *  Notify a result listener.
     *  @param listener The listener.
     */
    protected void notifyIntermediateResult(IIntermediateResultListener listener, Object result)
    {
    	listener.intermediateResultAvailable(result);
    }

    /**
     *  Notify a result listener.
     *  @param listener The listener.
     */
    protected void notifyListener(IResultListener listener)
    {
    	scheduleNotification(listener, false, null);
    	startScheduledNotifications();
    }
    
    /**
     *  Notify a result listener.
     *  @param listener The listener.
     */
    protected void doNotifyListener(IResultListener listener)
    {
//    	try
//    	{
			if(exception!=null)
			{
				listener.exceptionOccurred(exception);
			}
			else
			{
				if(listener instanceof IIntermediateResultListener)
				{
					IIntermediateResultListener lis = (IIntermediateResultListener)listener;
					Object[] inter = null;
					synchronized(this)
					{
						if(!intermediate && results!=null)
						{
							inter = results.toArray();
						}
					}
					if(inter!=null)
			    	{
			    		for(int i=0; i<inter.length; i++)
			    		{
			    			notifyIntermediateResult(lis, inter[i]);
			    		}
			    	}
					lis.finished();
				}
				else
				{
					listener.resultAvailable(results); 
				}
			}
//    	}
//    	catch(Exception e)
//    	{
//    		e.printStackTrace();
//    	}
    }
    
    List	scheduled;
    /**
     *  Schedule a listener notification.
     *  @param listener The listener to be notified.
     *  @param intermediate	True for intermediate result, false for final results.
     *  @param result	The intermediate result (if any).
     */
    protected void	scheduleNotification(IResultListener listener, boolean intermediate, Object result)
    {
    	synchronized(this)
    	{
    		if(scheduled==null)
    		{
    			scheduled	= new ArrayList();
    		}
    		scheduled.add(intermediate ? new Object[]{listener, result} : listener);
    	}
    }
    
    boolean	notifying;
    /**
     *  Start scheduled listener notifications if not already running.
     *  Must not be called from synchronized block.
     */
    protected void	startScheduledNotifications()
    {
    	boolean	notify	= false;
    	synchronized(this)
    	{
    		if(!notifying && scheduled!=null)
    		{
    			notifying	= true;
    			notify	= true;
    		}
    	}
    	
    	while(notify)
    	{
    		Object	next	= null;
        	synchronized(this)
        	{
        		if(scheduled.isEmpty())
        		{
        			notify	= false;
        			notifying	= false;
        			scheduled	= null;
        		}
        		else
        		{
        			next	=  scheduled.remove(0);
            	}
        	}
        	
        	if(next!=null)
        	{
        		if(next instanceof IResultListener)
        		{
        			doNotifyListener((IResultListener)next);
        		}
        		else
        		{
        			notifyIntermediateResult((IIntermediateResultListener)((Object[])next)[0], ((Object[])next)[1]);
        		}
        	}
    	}
    }
}
