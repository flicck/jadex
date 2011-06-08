package jadex.bdi.runtime.impl;

import jadex.bdi.runtime.interpreter.BDIInterpreter;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IResultSelector;
import jadex.bridge.service.ISearchManager;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.IVisitDecider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IServiceInvocationInterceptor;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;

/**
 *  For prefixed access of required services in capability.
 */
public class ServiceContainerProxy implements IServiceContainer
{
	//-------- attributes --------
	
	/** The interpreter. */
	protected BDIInterpreter	interpreter;
	
	/** The scope (rcapability). */
	protected Object	scope;
	
	//-------- constructors --------
	
	/**
	 *  Create a service container proxy.
	 */
	public ServiceContainerProxy(BDIInterpreter interpreter, Object scope)
	{
		this.interpreter	= interpreter;
		this.scope	= scope;
	}
	
	//-------- internal admin methods --------
	
	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 */
	// todo: remove, only call from platform
	public IFuture start()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Shutdown the service.
	 *  @return A future that is done when the service has completed its shutdown.  
	 */
	// todo: remove, only call from platform
	public IFuture shutdown()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Add a service to the container.
	 *  The service is started, if the container is already running.
	 *  @param service The service.
	 */
	public IFuture	addService(IInternalService service)
	{
		return interpreter.getServiceContainer().addService(service);
	}
	

	/**
	 *  Removes a service from the container (shutdowns also the service if the container is running).
	 *  @param service The service identifier.
	 */
	public IFuture	removeService(IServiceIdentifier sid)
	{
		return interpreter.getServiceContainer().removeService(sid);
	}
	
	//-------- internal user methods --------
	
	/**
	 *  Get provided (declared) service.
	 *  @param name The service name.
	 *  @return The service.
	 */
	public IService getProvidedService(String name)
	{
		// todo: possibly use prefix
		return interpreter.getServiceContainer().getProvidedService(name);
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @param class The interface.
	 *  @return The service.
	 */
	public IService[] getProvidedServices(Class clazz)
	{
		return interpreter.getServiceContainer().getProvidedServices(clazz);
	}
	
	/**
	 *  Get a required service of a given name.
	 *  @param name The service name.
	 *  @return The service.
	 */
	public IFuture getRequiredService(String name)
	{
		String prefix = interpreter.findServicePrefix(scope);
		return interpreter.getServiceContainer().getRequiredService(prefix+name);
	}

	/**
	 *  Get a required services of a given name.
	 *  @param name The services name.
	 *  @return The service.
	 */
	public IIntermediateFuture getRequiredServices(String name)
	{
		String prefix = interpreter.findServicePrefix(scope);
		return interpreter.getServiceContainer().getRequiredServices(prefix+name);
	}
	
	/**
	 *  Get a required service.
	 *  @return The service.
	 */
	public IFuture getRequiredService(String name, boolean rebind)
	{
		String prefix = interpreter.findServicePrefix(scope);
		return interpreter.getServiceContainer().getRequiredService(prefix+name, rebind);
	}
	
	/**
	 *  Get a required services.
	 *  @return The services.
	 */
	public IIntermediateFuture getRequiredServices(String name, boolean rebind)
	{
		String prefix = interpreter.findServicePrefix(scope);
		return interpreter.getServiceContainer().getRequiredServices(prefix+name, rebind);
	}
	
	/**
	 *  Add a service interceptor.
	 *  @param interceptor The interceptor.
	 *  @param service The service.
	 *  @param pos The position (0=first).
	 */
	public void addInterceptor(IServiceInvocationInterceptor interceptor, IService service, int pos)
	{
		interpreter.getServiceContainer().addInterceptor(interceptor, service, pos);
	}

	/**
	 *  Remove a service interceptor.
	 *  @param interceptor The interceptor.
	 *  @param service The service.
	 */
	public void removeInterceptor(IServiceInvocationInterceptor interceptor, IService service)
	{
		interpreter.getServiceContainer().removeInterceptor(interceptor, service);
	}

	/**
	 *  Get all services of a type.
	 *  @param type The class.
	 *  @return The corresponding services.
	 */
	public IIntermediateFuture	getServices(ISearchManager manager, IVisitDecider decider, IResultSelector selector)
	{
		return interpreter.getServiceContainer().getServices(manager, decider, selector);
	}
	
	/**
	 *  Get the parent service container.
	 *  @return The parent container.
	 */
	public IFuture	getParent()
	{
		return interpreter.getServiceContainer().getParent();
	}
	
	/**
	 *  Get the children container.
	 *  @return The children container.
	 */
	public IFuture	getChildren()
	{
		return interpreter.getServiceContainer().getChildren();
	}
	
	/**
	 *  Get the globally unique id of the provider.
	 *  @return The id of this provider.
	 */
	public Object	getId()
	{
		return interpreter.getServiceContainer().getId();
	}
	
	/**
	 *  Get the type of the service provider (e.g. enclosing component type).
	 *  @return The type of this provider.
	 */
	public String	getType()
	{
		return interpreter.getServiceContainer().getType();
	}

	/**
	 *  Get the required service infos.
	 */
	public RequiredServiceInfo[] getRequiredServiceInfos()
	{
		return interpreter.getServiceContainer().getRequiredServiceInfos();
	}

	/**
	 *  Set the required services.
	 *  @param required services The required services to set.
	 */
	public void setRequiredServiceInfos(RequiredServiceInfo[] requiredservices)
	{
		interpreter.getServiceContainer().setRequiredServiceInfos(requiredservices);
	}
	
	/**
	 *  Add required services for a given prefix.
	 *  @param prefix The name prefix to use.
	 *  @param required services The required services to set.
	 */
	public void addRequiredServiceInfos(RequiredServiceInfo[] requiredservices)
	{
		interpreter.getServiceContainer().addRequiredServiceInfos(requiredservices);
	}
}
