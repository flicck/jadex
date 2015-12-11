package jadex.bridge.service.component.interceptors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import jadex.bridge.ServiceCall;
import jadex.bridge.service.component.ISwitchCall;
import jadex.bridge.service.component.ServiceInvocationContext;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;

/**
 *  Calls a methods on an object and returns the result.
 */
public class MethodInvocationInterceptor extends AbstractApplicableInterceptor
{
	/** If debug is turned on it will print uncatched exceptions within service calls. */
	public static boolean DEBUG = false;
	
	//-------- methods --------
	
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(ServiceInvocationContext sic)
	{
//		if(sic.getMethod().getName().equals("ma1"))
//			System.out.println("ggghhh");
		
		try
		{
			// Must set nextinvoc and service call as it is not unknown if
			// a) the method is directly the business logic or
			// b) the method jumps from required to provided interceptor chain
				
//			if(sic.getMethod().getName().indexOf("method")!=-1)
//				System.out.println("ggggg");
			
			// Problem that the object could be an rmi proxy itself that delegates the call
			// Is this case the switch (current becomes next) must not occur
			
			// Hack, user may want to use proxy as well :-( (and we do have other proxies such as that from service pool)!
			// todo:
//			boolean switchcall = !Proxy.isProxyClass(sic.getObject().getClass());
			 
			boolean switchcall = true;
			
			// is not sufficient as could also be basicinvocationhandler of provided proxy
			if(Proxy.isProxyClass(sic.getObject().getClass()))
			{
				Object handler = Proxy.getInvocationHandler(sic.getObject());
				if(handler instanceof ISwitchCall)
				{
					switchcall = ((ISwitchCall)handler).isSwitchCall();
				}
//				if(handler.getClass().getName().indexOf("RemoteMethodInvocationHandler")!=-1) // HACK!!!
//				{
//					switchcall = false;
//				}
			}
			
			// Roll invocations meta infos before call
			long start = 0;
			if(switchcall)
			{
//				if(sic.getMethod().getName().indexOf("test")!=-1)
//					System.out.println("setting to a: "+sic.getServiceCall());
				start = System.currentTimeMillis();
				
				// Set the saved service calls in the thread locals for the callee
				CallAccess.setCurrentInvocation(sic.getNextServiceCall()); // next becomes current
				CallAccess.resetNextInvocation(); // next is null
				// last is not available
			}
			// No rolling if the method jumps from required to provided interceptor chain
			else
			{
				// Remember context for rmi command (extracts and stores it until return command arrives and non-func can be set)
				if(Proxy.getInvocationHandler(sic.getObject()).getClass().getName().indexOf("RemoteMethodInvocationHandler")!=-1)
					ServiceInvocationContext.SICS.set(sic);
				
//				if(sic.getMethod().getName().indexOf("test")!=-1)
//					System.out.println("setting to b: "+sic.getLastServiceCall());
				
				// Set the saved service calls in the thread locals for the callee
				CallAccess.setCurrentInvocation(sic.getCurrentServiceCall());
				CallAccess.setNextInvocation(sic.getNextServiceCall());
			}

//			if(sic.getMethod().getName().indexOf("method")!=-1)
//				System.out.println("setting to c: "+sic.getLastServiceCall()+" "+ServiceCall.getCurrentInvocation());
			
			Object res = sic.getMethod().invoke(sic.getObject(), sic.getArgumentArray());

			// Restore after call
//			if(sic.getMethod().getName().indexOf("method")!=-1)
//				System.out.println("setting to c: "+sic.getLastServiceCall()+" "+ServiceCall.getCurrentInvocation());
			if(switchcall)
			{
				if(ServiceCall.getCurrentInvocation()!=null)
				{
					final ServiceCall sc = ServiceCall.getCurrentInvocation();
					// Problem with subscription futures: if is first listener it will fetch the initial events :-(
					// todo: use addQuietListener for those
					if(res instanceof IFuture && !(res instanceof ISubscriptionIntermediateFuture))
					{
						final long fstart = start;
						((IFuture<Object>)res).addResultListener(new DelegationResultListener<Object>(null)
						{
							public void customResultAvailable(Object result)
							{
								long dur = System.currentTimeMillis()-fstart;
								sc.setProperty("__duration", Long.valueOf(dur));
							}
							
							public void exceptionOccurred(Exception exception)
							{
								// do nothing
							}
							
							public void commandAvailable(Object command)
							{
								// do nothing and avoid printouts
							}
						});
					}
					else
					{
						long dur = System.currentTimeMillis()-start;
						sc.setProperty("__duration", Long.valueOf(dur));
					}
				}
				
				// Set the invocations
				CallAccess.setLastInvocation(ServiceCall.getCurrentInvocation()); // last will be current 
				CallAccess.setCurrentInvocation(sic.getCurrentServiceCall()); // current will be old current
				CallAccess.resetNextInvocation(); // next is null
				
				sic.setNextCall(CallAccess.getLastInvocation()); // remember invocation made
			}
			
			sic.setResult(res);
		}
		catch(Exception e)
		{
//			System.out.println("e: "+sic.getMethod()+" "+sic.getObject()+" "+sic.getArgumentArray());

			Throwable	t	= e instanceof InvocationTargetException
					? ((InvocationTargetException)e).getTargetException() : e;
			Exception re = t instanceof Exception ? (Exception)t : new RuntimeException(t);
			
			if(DEBUG)
			{
				e.printStackTrace();
			}
			
			if(sic.getMethod().getReturnType().equals(IFuture.class))
			{
				sic.setResult(new Future(re));
			}
			else
			{
//				e.printStackTrace();
				throw re instanceof RuntimeException ? (RuntimeException)re : new RuntimeException(re);
//				{
//					public void printStackTrace()
//					{
//						Thread.dumpStack();
//						super.printStackTrace();
//					}
//				};
			}
		}
		
		return IFuture.DONE;
	}
}