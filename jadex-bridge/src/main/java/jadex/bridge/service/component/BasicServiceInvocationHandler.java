package jadex.bridge.service.component;

import jadex.bridge.IComponentAdapter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IRequiredServiceFetcher;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ThreadSuspendable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 *  Basic service invocation interceptor.
 *  It has a multi collection of interceptors per method.
 *  Executes the list of interceptors one by one.
 *  In case no handler can be found a fallback handler is used.
 */
public class BasicServiceInvocationHandler implements InvocationHandler
{
	//-------- attributes --------

	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The service. */
	protected Object service;

	/** The list of interceptors. */
	protected List interceptors;
	
	//-------- constructors --------
	
	/**
	 *  Create a new invocation handler.
	 */
	public BasicServiceInvocationHandler(IServiceIdentifier sid)
	{
		this.sid = sid;
	}
	
	/**
	 *  Create a new invocation handler.
	 */
	public BasicServiceInvocationHandler(IService service)
	{
		this.service = service;
		this.sid = service.getServiceIdentifier();
	}
	
	/**
	 *  Create a new invocation handler.
	 */
	public BasicServiceInvocationHandler(ServiceInfo service)
	{
		this.service = service;
		this.sid = service.getManagementService().getServiceIdentifier();
	}
	
	//-------- methods --------
	
	/**
	 *  A proxy method has been invoked.
	 */
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
	{
		Object ret = null;

		final ServiceInvocationContext sic = new ServiceInvocationContext(proxy, getInterceptors());
		
		List myargs = args!=null? SUtil.arrayToList(args): null;
		
		if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
		{
			final Future fut = new Future();
			ret = fut;
			sic.invoke(service, method, myargs).addResultListener(new DelegationResultListener(fut)
			{
				public void customResultAvailable(Object result)
				{
					if(sic.getResult() instanceof IFuture)
					{
						((IFuture)sic.getResult()).addResultListener(new DelegationResultListener(fut));
					}
					else
					{
						fut.setResult(sic.getResult());
					}
				}
			});
		}
		else if(method.getReturnType().equals(void.class))
		{
			sic.invoke(service, method, myargs);
		}
		else
		{
			IFuture fut = sic.invoke(service, method, myargs);
			if(fut.isDone())
			{
				ret = sic.getResult();
			}
			else
			{
				System.out.println("Warning, blocking call: "+method.getName()+" "+sid);
				ret = fut.get(new ThreadSuspendable());
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the sid.
	 *  @return the sid.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		return sid;
	}
	
	/**
	 *  Get the service.
	 *  @return The service.
	 */
	public Object getService()
	{
		return service;
	}

	/**
	 *  Add an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addFirstServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList();
		interceptors.add(0, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addServiceInterceptor(IServiceInvocationInterceptor interceptor, int pos)
	{
		if(interceptors==null)
			interceptors = new ArrayList();
		interceptors.add(pos, interceptor);
	}
	
	/**
	 *  Remove an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addServiceInterceptor(int pos)
	{
		if(interceptors!=null)
			interceptors.remove(pos);
	}
	
	/**
	 *  Remove an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void removeServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors!=null)
			interceptors.remove(interceptor);
	}
	
	/**
	 *  Get interceptors.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized IServiceInvocationInterceptor[] getInterceptors()
	{
		return interceptors==null || interceptors.size()==0? null://new IServiceInvocationInterceptor[]{fallback}: 
			(IServiceInvocationInterceptor[])interceptors.toArray(new IServiceInvocationInterceptor[interceptors.size()]);
	}
	
	/**
	 *  Static method for creating a standard service proxy for a provided service.
	 */
	public static IInternalService createProvidedServiceProxy(IExternalAccess ea, IComponentAdapter adapter, Object service, boolean direct)
	{
//		System.out.println("create: "+service.getServiceIdentifier().getServiceType());
		BasicServiceInvocationHandler handler;
		Class type;
		if(service instanceof IService)
		{
			IService ser = (IService)service;
			handler = new BasicServiceInvocationHandler(ser);
			type = ser.getServiceIdentifier().getServiceType();
		}
		else
		{
			Class[] types = service.getClass().getInterfaces();
			if(types.length!=1)
				throw new RuntimeException("Unknown service interface: "+SUtil.arrayToString(types));
			
			type = types[0];
			BasicService mgmntservice = new BasicService(ea.getServiceProvider().getId(), type, null);
			ServiceInfo si = new ServiceInfo(service, mgmntservice);
			handler = new BasicServiceInvocationHandler(si);
			
		}
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
		handler.addFirstServiceInterceptor(new ServiceSelectorInterceptor());
		handler.addFirstServiceInterceptor(new ValidationServiceInterceptor());
		if(!direct)
			handler.addFirstServiceInterceptor(new DecouplingServiceInvocationInterceptor(ea, adapter));
		return (IInternalService)Proxy.newProxyInstance(ea.getModel().getClassLoader(), new Class[]{IInternalService.class, type}, handler); 
	}
	
	/**
	 *  Static method for creating a delegation service proxy for 
	 *  provided service that is not offered by the component itself.
	 */
	public static IInternalService createDelegationProvidedServiceProxy(IExternalAccess ea, IComponentAdapter adapter, IServiceIdentifier sid, 
		RequiredServiceInfo info, RequiredServiceBinding binding)
	{
		BasicServiceInvocationHandler handler = new BasicServiceInvocationHandler(sid);
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
		handler.addFirstServiceInterceptor(new DelegationServiceInvocationInterceptor(ea, info, binding, null));
		return (IInternalService)Proxy.newProxyInstance(ea.getModel().getClassLoader(), new Class[]{IInternalService.class, sid.getServiceType()}, handler); 
	}

	/**
	 *  Static method for creating a standard service proxy for a provided service.
	 */
	public static IInternalService createRequiredServiceProxy(IExternalAccess ea, IComponentAdapter adapter, IInternalService service, 
		IRequiredServiceFetcher fetcher, RequiredServiceInfo info, RequiredServiceBinding binding)
	{
//		System.out.println("create: "+service.getServiceIdentifier().getServiceType());
		BasicServiceInvocationHandler handler = new BasicServiceInvocationHandler(service);
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
		if(binding.isRecover())
			handler.addFirstServiceInterceptor(new RecoverServiceInterceptor(ea, info, binding, fetcher));
		return (IInternalService)Proxy.newProxyInstance(ea.getModel().getClassLoader(), new Class[]{IInternalService.class, service.getServiceIdentifier().getServiceType()}, handler); 
	}
}

/**
 * 
 */
class ServiceInfo
{
	/** The service domain object. */
	protected Object domainservice;
	
	/** The management object. */
	protected BasicService mgmntservice; 
	
	/**
	 * 
	 */
	public ServiceInfo(Object domainservice, BasicService mgmntservice)
	{
		this.domainservice = domainservice;
		this.mgmntservice = mgmntservice;
	}

	/**
	 *  Get the domainservice.
	 *  @return The domainservice.
	 */
	public Object getDomainService()
	{
		return domainservice;
	}

	/**
	 *  Get the mgmntservice.
	 *  @return The mgmntservice.
	 */
	public BasicService getManagementService()
	{
		return mgmntservice;
	}
}
