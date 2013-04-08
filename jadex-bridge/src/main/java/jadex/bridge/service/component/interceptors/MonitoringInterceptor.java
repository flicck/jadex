package jadex.bridge.service.component.interceptors;

import jadex.bridge.Cause;
import jadex.bridge.IInternalAccess;
import jadex.bridge.ServiceCall;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IServiceInvocationInterceptor;
import jadex.bridge.service.component.ServiceInvocationContext;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService;
import jadex.bridge.service.types.monitoring.MonitoringEvent;
import jadex.commons.SReflect;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.ExceptionResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

/**
 *  Interceptor that creates service call start / end events and sends
 *  them to the monitoring service.
 */
public class MonitoringInterceptor implements IServiceInvocationInterceptor
{
	/** The component. */
	protected IInternalAccess component;
	
	/** The service getter. */
	protected ServiceGetter<IMonitoringService> getter;
	
	/**
	 *  Create a new interceptor.
	 */
	public MonitoringInterceptor(IInternalAccess component)
	{
		this.component = component;
		this.getter = new ServiceGetter<IMonitoringService>(component, IMonitoringService.class, RequiredServiceInfo.SCOPE_PLATFORM);
	}
	
	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context)
	{
		// Do not monitor calls to the monitoring service itself and no constant calls
		
		Boolean mon = (Boolean)context.getServiceCall().getProperty("monitoring");
		
		boolean ret;
		
		if(mon!=null)
		{
			ret = mon.booleanValue();
		}
		else
		{
			ret = !context.getMethod().getDeclaringClass().equals(IMonitoringService.class)
				&& SReflect.isSupertype(IFuture.class, context.getMethod().getReturnType());
			//&& context.getMethod().getName().indexOf("getChildren")==-1;
		}
		
//		System.out.println("isApp: "+context.getMethod()+" "+ret);
//
//		if(context.getMethod().getName().indexOf("getChildren")!=-1)
//			System.out.println("gggggg");
		
//		if(ret)
//			System.out.println("ok: "+context.getMethod().getDeclaringClass()+"."+context.getMethod().getName());
				
//		if(context.getMethod().getName().indexOf("getExternalAccess")!=-1)
//			System.out.println("getExt");
		
		return ret;
	}
	
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(final ServiceInvocationContext context)
	{
		final Future<Void> ret = new Future<Void>();
		
		// Hack, necessary because getService() is not a service call and the first contained
		// service call (getChildren) will reset the call context afterwards :-(
		final ServiceCall cur = CallAccess.getCurrentInvocation();
		final ServiceCall next = CallAccess.getNextInvocation();
		
		ServiceCall sc = CallAccess.getInvocation();
		sc.setProperty(ServiceCall.MONITORING, Boolean.FALSE);
		sc.setProperty(ServiceCall.INHERIT, Boolean.TRUE);
		
		CallAccess.setServiceCall(sc); 
		
//		if(context.getMethod().getName().equals("shutdownService") && component.getComponentIdentifier().getParent()==null)
//			System.out.println("start shut in mon: "+context.getObject());
		
//		if(context.getMethod().getName().indexOf("getExternalAccess")!=-1)
//			System.out.println("getExt");
		
		getter.getService().addResultListener(new ExceptionDelegationResultListener<IMonitoringService, Void>(ret)
		{
			public void customResultAvailable(IMonitoringService monser)
			{
//				if(context.getMethod().getName().equals("shutdownService") && component.getComponentIdentifier().getParent()==null)
//					System.out.println("end shut in mon: "+context.getObject());
				
				CallAccess.setServiceCall(cur); 
				CallAccess.setNextInvocation(next);
				
				// Publish event if monitoring service was found
				if(monser!=null)
				{
					// todo: clock?
					long start = System.currentTimeMillis();
					ServiceCall sc = context.getServiceCall();
					Cause cause = sc==null? null: sc.getCause();
					MonitoringEvent ev = new MonitoringEvent(component.getComponentIdentifier(), component.getComponentDescription().getCreationTime(),
						context.getMethod().getDeclaringClass().getName()+"."+context.getMethod().getName(), 
						IMonitoringEvent.TYPE_SERVICECALL_START, cause, start);
					
//					if(context.getMethod().getName().indexOf("method")!=-1)
//						System.out.println("call method: "+ev.getCause().getChainId());
					
					monser.publishEvent(ev).addResultListener(new ExceptionResultListener<Void>()
					{
						public void exceptionOccurred(Exception e)
						{
							// Reset mon service if error on publish
							getter.resetService();
						}
					});
				}
				
				context.invoke().addResultListener(new ReturnValueResultListener(ret, context));
			}
		});
	
		return ret;
	}
	
	/**
	 *  Listener that handles the end of the call.
	 */
	protected class ReturnValueResultListener extends DelegationResultListener<Void>
	{
		//-------- attributes --------
		
		/** The service invocation context. */
		protected ServiceInvocationContext	sic;
		
		//-------- constructors --------
		
		/**
		 *  Create a result listener.
		 */
		protected ReturnValueResultListener(Future<Void> future, ServiceInvocationContext sic)
		{
			super(future);
			this.sic = sic;
		}
		
		//-------- IResultListener interface --------

		/**
		 *  Called when the service call is finished.
		 */
		public void customResultAvailable(Void result)
		{
			// Hack, necessary because getService() is not a service call and the first contained
			// service call (getChildren) will reset the call context afterwards :-(
			final ServiceCall cur = CallAccess.getCurrentInvocation();
			final ServiceCall next = CallAccess.getNextInvocation();
			
			ServiceCall sc = CallAccess.getInvocation();
			sc.setProperty(ServiceCall.MONITORING, Boolean.FALSE);
			sc.setProperty(ServiceCall.INHERIT, Boolean.TRUE);
			
			CallAccess.setServiceCall(sc); 

			getter.getService().addResultListener(new IResultListener<IMonitoringService>()
			{
				public void resultAvailable(IMonitoringService monser)
				{
					CallAccess.setServiceCall(cur); 
					CallAccess.setNextInvocation(next);

					if(monser!=null)
					{
						// todo: clock?
						long end = System.currentTimeMillis();
						ServiceCall sc = sic.getServiceCall();
						Cause cause = sc==null? null: sc.getCause();
						monser.publishEvent(new MonitoringEvent(component.getComponentIdentifier(), component.getComponentDescription().getCreationTime(),
							sic.getMethod().getDeclaringClass().getName()+"."+sic.getMethod().getName(), IMonitoringEvent.TYPE_SERVICECALL_END, cause, end));
					}
					ReturnValueResultListener.super.customResultAvailable(null);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					CallAccess.setServiceCall(cur); 
					CallAccess.setNextInvocation(next);

					// never happens
					ReturnValueResultListener.super.exceptionOccurred(exception);
				}
			});
		}
	}
}
