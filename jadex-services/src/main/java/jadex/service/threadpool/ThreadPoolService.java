package jadex.service.threadpool;

import jadex.commons.IFuture;
import jadex.commons.concurrent.IThreadPool;
import jadex.service.BasicService;

/**
 *  Service wrapper for a threadpool.
 */
public class ThreadPoolService extends BasicService implements IThreadPool
{
	//-------- attributes --------
	
	/** The threadpool. */
	protected IThreadPool threadpool;
	
	//-------- constructors -------- 
	
	/**
	 *  Create a new threadpool service.
	 */
	public ThreadPoolService(IThreadPool threadpool)
	{
		this.threadpool = threadpool;
	}
	
	//-------- methods -------- 

	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 * /
	public synchronized IFuture	startService()
	{
		return super.startService();
	}*/
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public synchronized IFuture	shutdownService()
	{
		threadpool.dispose();
		return super.shutdownService();
	}
	
	/**
	 *  Execute a task in its own thread.
	 *  @param task The task to execute.
	 */
	public synchronized void execute(Runnable task)
	{
//		if(!isValid())
//			throw new RuntimeException("Service invalid: "+this);
		
		threadpool.execute(task);
	}
	
	/**
	 *  Shutdown the thread pool.
	 */
	public void dispose()
	{
		shutdownService();
	}
}
