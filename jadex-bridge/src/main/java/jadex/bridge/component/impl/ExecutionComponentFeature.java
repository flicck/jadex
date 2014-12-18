package jadex.bridge.component.impl;

import jadex.base.Starter;
import jadex.bridge.ComponentResultListener;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IntermediateComponentResultListener;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeature;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.ComponentSuspendable;
import jadex.bridge.service.component.interceptors.CallAccess;
import jadex.bridge.service.component.interceptors.FutureFunctionality;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.bridge.service.types.clock.ITimer;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.execution.IExecutionService;
import jadex.commons.IResultCommand;
import jadex.commons.Tuple2;
import jadex.commons.concurrent.IExecutable;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.FutureHelper;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISuspendable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *  This feature provides component step execution.
 */
public class ExecutionComponentFeature	extends	AbstractComponentFeature implements IExecutionFeature, IInternalExecutionFeature, IExecutable
{
	//-------- attributes --------
	
	/** The component steps. */
	protected List<Tuple2<IComponentStep<?>, Future<?>>>	steps;
	
	/** The immediate component steps. */
	protected List<Tuple2<IComponentStep<?>, Future<?>>>	isteps;
	
	/** The current timer. */
	protected List<ITimer> timers = new ArrayList<ITimer>();
	
	/** Retained listener notifications when switching threads due to blocking. */
	protected List<Tuple2<Future<?>, IResultListener<?>>>	notifications;
	
	/** Flag for testing double execution. */
	protected volatile boolean executing;
	
	/** The thread currently executing the component (null for none). */
	protected Thread componentthread;
	
	//-------- constructors --------
	
	/**
	 *  Create the feature.
	 */
	public ExecutionComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	//-------- IComponentFeature interface --------
	
	/**
	 *  Get the user interface type of the feature.
	 */
	public Class<?>	getType()
	{
		return IExecutionFeature.class;
	}
	
	/**
	 *  Create an instance of the feature.
	 */
	public IComponentFeature createInstance(IInternalAccess access, ComponentCreationInfo info)
	{
		return new ExecutionComponentFeature(access, info);
	}
	
	//-------- IExecutionFeature interface --------
	
	/**
	 *  Execute a component step.
	 */
	public <T>	IFuture<T> scheduleStep(IComponentStep<T> step)
	{
		final Future<T> ret = createStepFuture(step);
		
		synchronized(this)
		{
			if(steps==null)
			{
				steps	= new LinkedList<Tuple2<IComponentStep<?>,Future<?>>>();
			}
			steps.add(new Tuple2<IComponentStep<?>, Future<?>>(step, ret));
		}

		wakeup();
		
		return ret;
	}
	
	/**
	 *  Execute an immediate component step,
	 *  i.e., the step is executed also when the component is currently suspended.
	 */
	public <T>	IFuture<T> scheduleImmediate(IComponentStep<T> step)
	{
		final Future<T> ret = createStepFuture(step);
		
		synchronized(this)
		{
			if(isteps==null)
			{
				isteps	= new LinkedList<Tuple2<IComponentStep<?>,Future<?>>>();
			}
			isteps.add(new Tuple2<IComponentStep<?>, Future<?>>(step, ret));
		}

		wakeup();
		
		return ret;
	}
	
	/**
	 *  Wait for some time and execute a component step afterwards.
	 */
	public <T> IFuture<T> waitForDelay(final long delay, final IComponentStep<T> step)
	{
		return waitForDelay(delay, step, false);
	}
	
	/**
	 *  Wait for some time and execute a component step afterwards.
	 */
	public <T> IFuture<T> waitForDelay(final long delay, final IComponentStep<T> step, final boolean realtime)
	{
		// todo: remember and cleanup timers in case of component removal.
		
		final Future<T> ret = new Future<T>();
		
		SServiceProvider.getService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(createResultListener(new ExceptionDelegationResultListener<IClockService, T>(ret)
		{
			public void customResultAvailable(IClockService cs)
			{
				ITimedObject	to	= new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleStep(step).addResultListener(createResultListener(new DelegationResultListener<T>(ret)));
					}
					
					public String toString()
					{
						return "waitForDelay[Step]("+getComponent().getComponentIdentifier()+")";
					}
				};
				if(realtime)
				{
					cs.createRealtimeTimer(delay, to);
				}
				else
				{
					cs.createTimer(delay, to);					
				}
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Wait for some time.
	 */
	public IFuture<Void> waitForDelay(final long delay)
	{
		return waitForDelay(delay, false);
	}

	/**
	 *  Wait for some time.
	 */
	public IFuture<Void> waitForDelay(final long delay, final boolean realtime)
	{
		final Future<Void> ret = new Future<Void>();
		
		SServiceProvider.getService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(createResultListener(new ExceptionDelegationResultListener<IClockService, Void>(ret)
		{
			public void customResultAvailable(IClockService cs)
			{
				ITimedObject	to	=  	new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleStep(new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								ret.setResult(null);
								return IFuture.DONE;
							}
						});
					}
					
					public String toString()
					{
						return "waitForDelay("+getComponent().getComponentIdentifier()+")";
					}
				};
				
				if(realtime)
				{
					cs.createRealtimeTimer(delay, to);
				}
				else
				{
					cs.createTimer(delay, to);
				}
			}
		}));
		
		return ret;
	}
	
//	/**
//	 *  Wait for some time.
//	 */
//	public IFuture<ITimer> waitForDelayWithTimer(final long delay, final IComponentStep<?> step)
//	{
//		final Future<ITimer> ret = new Future<ITimer>();
//		
//		IClockService cs = SServiceProvider.getLocalService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
//		ITimedObject	to	=  	new ITimedObject()
//		{
//			public void timeEventOccurred(long currenttime)
//			{
//				scheduleStep(step);
//			}
//			
//			public String toString()
//			{
//				return "waitForDelay("+getComponent().getComponentIdentifier()+")";
//			}
//		};
//		
//		ITimer timer = cs.createTimer(delay, to);
//		ret.setResult(timer);
//		
//		return ret;
//	}
	
	/**
	 *  Wait for the next tick.
	 *  @param time The time.
	 */
	public IFuture<Void> waitForTick(final IComponentStep<Void> run)
	{
//		final Future<TimerWrapper> ret = new Future<TimerWrapper>();
		final Future<Void> ret = new Future<Void>();
		
		IClockService cs = SServiceProvider.getLocalService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		final ITimer[] ts = new ITimer[1];
		ts[0] = cs.createTickTimer(new ITimedObject()
		{
			public void timeEventOccurred(final long currenttime)
			{
				try
				{
					scheduleStep(new ExecuteWaitForStep(ts[0], run));
				}
				catch(ComponentTerminatedException e)
				{
				}
			}
		});
		if(timers==null)
			timers	= new ArrayList<ITimer>();
		timers.add(ts[0]);
//		ret.setResult(new TimerWrapper(ts[0]));
		ret.setResult(null);
		
		return ret;
	}
	
	// todo:?
//	/**
//	 *  Wait for some time and execute a component step afterwards.
//	 */
//	public IFuture waitForImmediate(long delay, IComponentStep step);
	
	/** Flag to indicate bootstrapping execution of main thread (only for platform, hack???). */
	protected volatile boolean bootstrap;
	
	/** Flag to indicate that the execution service has become available during bootstrapping (only for platform, hack???). */
	protected volatile boolean available;
	
	/**
	 *  Trigger component execution.
	 */
	protected void	wakeup()
	{
		SServiceProvider.getService(component, IExecutionService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener<IExecutionService>()
		{
			public void resultAvailable(IExecutionService exe)
			{
				// Hack!!! service is foudn before it is started, grrr.
				if(((IService)exe).isValid().get(null).booleanValue())	// Hack!!! service is raw
				{
					if(bootstrap)
					{
						// Execution service found during bootstrapping execution -> stop bootstrapping as soon as possible.
						available	= true;
					}
					else
					{
						exe.execute(ExecutionComponentFeature.this);
					}
				}
				else
				{
					exceptionOccurred(null);
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				// Happens during platform bootstrapping -> execute on platform rescue thread.
				if(!bootstrap)
				{
					bootstrap	= true;
					Starter.scheduleRescueStep(getComponent().getComponentIdentifier().getRoot(), new Runnable()
					{
						public void run()
						{
							boolean	again	= true;
							while(!available && again)
							{
								again	= execute();
							}
							bootstrap	= false;
							
							if(again)
							{					
								// Bootstrapping finished -> do real kickoff
								wakeup();
							}
						}
					});
				}
			}
		});
	}

	/**
	 *  Test if current thread is the component thread.
	 *  @return True if the current thread is the component thread.
	 */
	public boolean isComponentThread()
	{
		return Thread.currentThread()==componentthread || 
			IComponentDescription.STATE_TERMINATED.equals(getComponent().getComponentDescription().getState())
				&& Starter.isRescueThread(getComponent().getComponentIdentifier());
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
	{
		return new ComponentResultListener<T>(listener, component);
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IIntermediateResultListener<T> createResultListener(IIntermediateResultListener<T> listener)
	{
		return new IntermediateComponentResultListener<T>(listener, component);
	}
	
	//-------- IInternalExecutionFeature --------
	
	/**
	 *  Block the current thread and allow execution on other threads.
	 *  @param monitor	The monitor to wait for.
	 */
	public void	block(final Object monitor, long timeout)
	{
		throw new UnsupportedOperationException();
		// todo...
		
//		if(!isComponentThread())
//		{
//			throw new RuntimeException("Can only block current component thread: "/*+componentthread+", "*/+Thread.currentThread());
//		}
//		
//		// Retain listener notifications for new component thread.
//		assert notifications==null;
//		notifications	= FutureHelper.removeStackedListeners();
////		System.out.println("removed stack size: "+notifications.size()+", "+getComponentIdentifier());
//		
//		Executor	exe	= Executor.EXECUTOR.get();
//		if(exe==null)
//		{
//			throw new RuntimeException("Cannot block: no executor");
//		}
//		
//		component.beforeBlock();
//		
////		if(getComponentIdentifier().toString().indexOf("@Receiver.EventSystem")!=-1)
////		{
////			System.err.println(getComponentIdentifier()+": !execution1 "+System.identityHashCode(Executor.EXECUTOR.get()));
////		}
//		this.executing	= false;
//		this.componentthread	= null;
////		
////		if(getComponentIdentifier().toString().indexOf("IntermediateTest")!=-1)
//////		if(getModel().getFullName().indexOf("marsworld.sentry")!=-1)
////		{
////			System.out.println("Blocking: "+getComponentIdentifier()+", "+System.currentTimeMillis());
////		}
//		
//		if(blocked==null)
//		{
//			blocked	= new HashMap<Object, Executor>();
//		}
//		blocked.put(monitor, exe);
//		
//		
//		
//		
//		final boolean[]	unblocked	= new boolean[1];
//		
//		if(timeout!=Timeout.NONE)
//		{
//			waitForDelay(timeout)
//				.addResultListener(new IResultListener<Void>()
//			{
//				public void resultAvailable(Void result)
//				{
//					if(!unblocked[0])
//					{
////						if(getComponentIdentifier().toString().indexOf("IntermediateTest")!=-1)
////						{
////							System.out.println("Unblocking after timeout: "+getComponentIdentifier()+", "+System.currentTimeMillis());
////						}
//						
//						// Cannot use timeout exception as component would not be correctly entered.
//						// Todo: allow informing future about timeout.
//						unblock(monitor, null); //new TimeoutException());
//					}
////					else if(getComponentIdentifier().toString().indexOf("IntermediateTest")!=-1)
////					{
////						System.out.println("Not unblocking after timeout (already unblocked): "+getComponentIdentifier()+", "+System.currentTimeMillis());
////					}
//				}
//				
//				public void exceptionOccurred(Exception exception)
//				{
//				}
//			});
//		}
//		
//		exe.blockThread(monitor);
//		
//		unblocked[0]	= true;
//		
//		
//		
//		
//		assert !IComponentDescription.STATE_TERMINATED.equals(desc.getState());
//		
////		if(getComponentIdentifier().toString().indexOf("IntermediateTest")!=-1)
//////		if(getModel().getFullName().indexOf("marsworld.sentry")!=-1)
////		{
////			System.out.println("Unblocked: "+getComponentIdentifier()+", "+System.currentTimeMillis());
////		}
////		
//		synchronized(this)
//		{
//			if(executing)
//			{
//				System.err.println(getComponent().getComponentIdentifier()+": double execution");
//				new RuntimeException("executing: "+getComponent().getComponentIdentifier()).printStackTrace();
//			}
//			this.executing	= true;
//		}
////		if(getComponentIdentifier().toString().indexOf("@Receiver.EventSystem")!=-1)
////		{
////			System.err.println(getComponentIdentifier()+": execution1 "+System.identityHashCode(Executor.EXECUTOR.get()));
////		}
//
//		this.componentthread	= Thread.currentThread();
//		
//		component.afterBlock();
	}
	
	/**
	 *  Unblock the thread waiting for the given monitor
	 *  and cease execution on the current thread.
	 *  @param monitor	The monitor to notify.
	 */
	public void	unblock(Object monitor, Throwable exception)
	{
		// todo...
		
//		if(!isComponentThread())
//		{
//			throw new RuntimeException("Can only unblock from component thread: "/*+componentthread+", "*/+Thread.currentThread());
//		}
//		
//		Executor exe = blocked.remove(monitor);
//		if(blocked.isEmpty())
//		{
//			blocked	= null;
//		}
//				
//		exe.switchThread(monitor, exception);
	}
	
	//-------- IExecutable interface --------
	
	/**
	 *  Execute the executable.
	 *  @return True, if the object wants to be executed again.
	 */
	public boolean execute()
	{
		ISuspendable.SUSPENDABLE.set(new ComponentSuspendable(getComponent()));
		
		synchronized(this)
		{
			if(executing)
			{
				System.err.println(getComponent().getComponentIdentifier()+": double execution"+" "+Thread.currentThread()+" "+componentthread);
				new RuntimeException("executing: "+getComponent().getComponentIdentifier()).printStackTrace();
			}
			executing	= true;
		}

		// Todo: termination and exception!?
//		// Note: wakeup() can be called from arbitrary threads (even when the
//		// component itself is currently running. I.e. it cannot be ensured easily
//		// that an execution task is enqueued and the component has terminated
//		// meanwhile.
//		if(!IComponentDescription.STATE_TERMINATED.equals(getComponent().getComponentDescription().getState()))
//		{
//			if(exception!=null)
//			{
//				this.executing	= false;
//				return false;	// Component already failed: tell executor not to call again. (can happen during failed init)
//			}
	
			// Remember execution thread.
			this.componentthread	= Thread.currentThread();
			IComponentIdentifier.LOCAL.set(getComponent().getComponentIdentifier());
			IInternalExecutionFeature.LOCAL.set(getComponent());
			ClassLoader	cl	= Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(component.getClassLoader());
			
			// Process listener notifications from old component thread.
			boolean notifexecuted	= false;
			if(notifications!=null)
			{
				FutureHelper.addStackedListeners(notifications);
				notifications	= null;
				
				// Todo: termination and exception!?
//				try
				{
					FutureHelper.notifyStackedListeners();
					notifexecuted	= true;
				}
//				catch(Exception e)
//				{
//					fatalError(e);
//				}
//				catch(StepAborted sa)
//				{
//				}
//				catch(Throwable t)
//				{
//					fatalError(new RuntimeException(t));
//				}

			}
	
			// Todo: breakpoints
//			// Suspend when breakpoint is triggered.
//			// Necessary because component wakeup could be called anytime even if is at breakpoint..
//			boolean	breakpoint_triggered	= false;
//			if(!dostep && !IComponentDescription.STATE_SUSPENDED.equals(desc.getState()))
//			{
//				if(component.isAtBreakpoint(desc.getBreakpoints()))
//				{
//					breakpoint_triggered	= true;
//					getCMS().addResultListener(new DefaultResultListener<IComponentManagementService>(logger)
//					{
//						public void resultAvailable(IComponentManagementService cms)
//						{
//							cms.suspendComponent(desc.getName());
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							if(!(exception instanceof ComponentTerminatedException))
//							{
//								super.exceptionOccurred(exception);
//							}
//						}
//					});
//				}
//			}
//			
//			boolean	again	= false;
//			if(!breakpoint_triggered && !extexecuted  && !notifexecuted && (!IComponentDescription.STATE_SUSPENDED.equals(desc.getState()) || dostep))
//			{
//				try
//				{
////					if(getComponentIdentifier()!=null && getComponentIdentifier().getParent()==null)
////						System.out.println("Executing: "+getComponentIdentifier()+", "+Thread.currentThread());
//					again	= component.executeStep();
////					if(getComponentIdentifier()!=null && getComponentIdentifier().getParent()==null)
////						System.out.println("Not Executing: "+getComponentIdentifier()+", "+Thread.currentThread());
//				}
//				catch(Exception e)
//				{
//					fatalError(e);
//				}
//				catch(StepAborted sa)
//				{
//				}
//				catch(Throwable t)
//				{
//					fatalError(new RuntimeException(t));
//				}
//				if(dostep)
//				{
//					dostep	= false;
//					if(stepfuture!=null)
//					{
//						stepfuture.setResult(null);
//					}
//				}
//				
//				// Suspend when breakpoint is triggered.
//				if(!IComponentDescription.STATE_SUSPENDED.equals(desc.getState()))
//				{
//					if(component.isAtBreakpoint(desc.getBreakpoints()))
//					{
//						breakpoint_triggered	= true;
//						getCMS().addResultListener(new DefaultResultListener<IComponentManagementService>(logger)
//						{
//							public void resultAvailable(IComponentManagementService cms)
//							{
//								cms.suspendComponent(desc.getName());
//							}
//							
//							public void exceptionOccurred(Exception exception)
//							{
//								if(!(exception instanceof ComponentTerminatedException))
//								{
//									super.exceptionOccurred(exception);
//								}
//							}
//						});
//					}
//				}
//			}
			
		final Tuple2<IComponentStep<?>, Future<?>>	step;
		synchronized(this)
		{
			if(isteps!=null)
			{
				step	= isteps.remove(0);
				if(isteps.isEmpty())
				{
					isteps	= null;
				}
			}
			else if(steps!=null)
			{
				step	= steps.remove(0);
				if(steps.isEmpty())
				{
					steps	= null;
				}
			}
			else
			{
				step	= null;
			}
		}
		
		boolean	again;
		
		if(step!=null)
		{
			try
			{
				step.getFirstEntity().execute(component)
					.addResultListener(new DelegationResultListener(step.getSecondEntity()));

				if(!step.getSecondEntity().hasResultListener())
				{
					((Future<Object>)step.getSecondEntity()).addResultListener(new IResultListener<Object>()
					{
						public void resultAvailable(Object result)
						{
							getComponent().getLogger().warning("No listener for component step: "+step.getFirstEntity());
						}
						
						public void exceptionOccurred(Exception exception)
						{
							// Todo: fail fast vs robust components.
							
							StringWriter	sw	= new StringWriter();
							exception.printStackTrace(new PrintWriter(sw));
							getComponent().getLogger().severe("No listener for component step exception: "+step.getFirstEntity()+"\n"+sw);
						}
					});
				}
			}
			catch(Exception e)
			{
				// Todo: fail fast vs robust components.
				
				if(!step.getSecondEntity().hasResultListener())
				{
					StringWriter	sw	= new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					getComponent().getLogger().severe("No listener for component step exception: "+step.getFirstEntity()+"\n"+sw);
				}
				
				step.getSecondEntity().setException(e);
			}
			
			synchronized(this)
			{
				again	= isteps!=null || steps!=null;
			}
		}
		else
		{
			again	= false;
		}
		
		// Reset execution state.
		IComponentIdentifier.LOCAL.set(null);
		IInternalExecutionFeature.LOCAL.set(null);
		// Must reset service call settings when thread retreats from components
		CallAccess.resetCurrentInvocation();
		CallAccess.resetNextInvocation();
		Thread.currentThread().setContextClassLoader(cl);
		this.componentthread = null;
		executing	= false;
		ISuspendable.SUSPENDABLE.set(null);

		return again;
	}
	
	/**
	 *  Create intermediate of direct future.
	 */
	protected <T> Future<T> createStepFuture(IComponentStep<T> step)
	{
		Future<T> ret;
		try
		{
			Method method = step.getClass().getMethod("execute", new Class[]{IInternalAccess.class});
			Class<?> clazz = method.getReturnType();
//			ret = FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality(getLogger()));
			// Must not be fetched before properties are available!
			ret = (Future<T>)FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality(new IResultCommand<Logger, Void>()
			{
				public Logger execute(Void args)
				{
					return getComponent().getLogger();
				}
			}));
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Wrap a timer and remove it from the agent when it is cancelled.
	 */
	protected class TimerWrapper implements ITimer 
	{
		//-------- attributes --------
		
		/** The wrapped timer. */
		ITimer	timer;
		
		//-------- constructors--------
		
		/**
		 *  Wrap a timer.
		 */
		public TimerWrapper(ITimer timer)
		{
			this.timer	= timer;
		}
		
		//-------- ITimer interface --------
		
		public void cancel()
		{
			assert timers!=null;
			timers.remove(timer);
			timer.cancel();
		}

		public long getNotificationTime()
		{
			return timer.getNotificationTime();
		}

		public ITimedObject getTimedObject()
		{
			return timer.getTimedObject();
		}

		public void setNotificationTime(long time)
		{
			timer.setNotificationTime(time);
		}

		public boolean equals(Object obj)
		{
			return timer.equals(obj);
		}

		public int hashCode()
		{
			return timer.hashCode();
		}

		public String toString()
		{
			return timer.toString();
		}
	}
	
	/**
	 *  Step to execute a wait for entry.
	 */
	public static class ExecuteWaitForStep implements IComponentStep<Void>
	{
		//-------- attributes --------

		/** The timer. */
		private final ITimer ts;

		/** The component step. */
		private final IComponentStep<Void> run;

		//-------- constructors--------

		/**
		 * This class is constructed with an array of {@link ITimer}s and the {@link IComponentStep}
		 * which is scheduled for execution.
		 * @param ts an array of {@link ITimer}s
		 * @param run the {@link IComponentStep} which is scheduled for execution
		 */
		public ExecuteWaitForStep(ITimer ts, IComponentStep<Void> run)
		{
			this.ts = ts;
			this.run = run;
		}

		//-------- methods --------

		/**
		 * Removes the first entry from the {@link ITimer} array from the micro agents
		 * {@link MicroAgent#timers} {@link List} and executes the {@link IComponentStep}.
		 */
		public IFuture<Void> execute(IInternalAccess ia)
		{
			assert ((ExecutionComponentFeature)ia.getComponentFeature(IExecutionFeature.class)).timers!=null;
			((ExecutionComponentFeature)ia.getComponentFeature(IExecutionFeature.class)).timers.remove(ts);
			run.execute(ia);
			return IFuture.DONE;
		}

		/**
		 * @return "microagent.waitFor_#" plus the hash code of this class
		 */
		public String toString()
		{
			return ts==null? super.toString(): ts.getTimedObject()!=null? ts.getTimedObject().toString(): ts.toString();
		}
		
		/**
		 * Returns the {@link IComponentStep} that is scheduled for execution.
		 * @return The {@link IComponentStep} that is scheduled for execution
		 */
		public IComponentStep<Void> getComponentStep()
		{
			return run;
		}
	}
}