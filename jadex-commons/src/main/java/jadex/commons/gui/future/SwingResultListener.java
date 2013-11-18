package jadex.commons.gui.future;

import jadex.commons.future.IResultListener;
import jadex.commons.future.IUndoneResultListener;
import jadex.commons.gui.SGUI;

import javax.swing.SwingUtilities;


/**
 *  Listener that allows to automatically trigger a timeout when
 *  no result (or exception) was received after some timeout interval.
 */
public class SwingResultListener<E> implements IResultListener<E>
{
	//-------- attributes --------
	
	/** The delegation listener. */
	protected IResultListener<E> listener;
	
	/** Flag if undone methods should be used. */
	protected boolean undone;
	
	//-------- constructors --------

	/**
	 *  Create a new listener.
	 */
	public SwingResultListener(final IResultListener<E> listener)
	{
		this.listener = listener;
	}
	
	//-------- methods --------
	
	/**
	 *  Called when the result is available.
	 * @param result The result.
	 */
	final public void	resultAvailable(final E result)
	{
		// Hack!!! When triggered from shutdown hook, swing might be terminated
		// and invokeLater has no effect (grrr).
		if(!SGUI.HAS_GUI || SwingUtilities.isEventDispatchThread())// || Starter.isShutdown())
//		if(SwingUtilities.isEventDispatchThread())
		{
			customResultAvailable(result);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					customResultAvailable(result);
				}
			});
		}
	}
	
	/**
	 *  Called when an exception occurred.
	 * @param exception The exception.
	 */
	final public void	exceptionOccurred(final Exception exception)
	{
//		exception.printStackTrace();
		// Hack!!! When triggered from shutdown hook, swing might be terminated
		// and invokeLater has no effect (grrr).
		if(!SGUI.HAS_GUI || SwingUtilities.isEventDispatchThread())// || Starter.isShutdown())
//		if(SwingUtilities.isEventDispatchThread())
		{
			customExceptionOccurred(exception);			
		}
		else
		{
//			Thread.dumpStack();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					customExceptionOccurred(exception);
				}
			});
		}
	}
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 */
	public void	customResultAvailable(E result)
	{
		if(undone && listener instanceof IUndoneResultListener)
		{
			((IUndoneResultListener<E>)listener).resultAvailableIfUndone(result);
		}
		else
		{
			listener.resultAvailable(result);
		}
	}
	
	/**
	 *  Called when an exception occurred.
	 *  @param exception The exception.
	 */
	public void	customExceptionOccurred(Exception exception)
	{
		if(undone && listener instanceof IUndoneResultListener)
		{
			((IUndoneResultListener<E>)listener).exceptionOccurredIfUndone(exception);
		}
		else
		{
			listener.exceptionOccurred(exception);
		}
	}
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 */
	public void resultAvailableIfUndone(E result)
	{
		undone = true;
		resultAvailable(result);
	}
	
	/**
	 *  Called when an exception occurred.
	 *  @param exception The exception.
	 */
	public void exceptionOccurredIfUndone(Exception exception)
	{
		undone = true;
		exceptionOccurred(exception);
	}

	/**
	 *  Get the undone.
	 *  @return The undone.
	 */
	public boolean isUndone()
	{
		return undone;
	}
}
