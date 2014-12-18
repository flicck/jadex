package jadex.bridge.service.types.factory;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.service.search.LocalServiceRegistry;
import jadex.commons.future.IFuture;

import java.util.Collection;

/**
 *  Interface for operations on the component to be called from the platform.
 */
public interface IPlatformComponentAccess
{
	//-------- methods --------
	
	/**
	 *  Create the component, i.e. instantiate its features.
	 *  This is the first method that is called by the platform.
	 *  
	 *  @param info The component creation info.
	 *  @param templates The component feature templates to be instantiated for this component.
	 */
	public void	create(ComponentCreationInfo info, Collection<IComponentFeatureFactory> templates);
	
	/**
	 *  Perform the initialization of the component.
	 *  Called after creation.
	 *  Tries to switch to a separate thread for the component as soon as possible.
	 *  
	 *  @return A future to indicate when the initialization is done.
	 */
	public IFuture<Void>	init();
	
	/**
	 *  Perform the main execution of the component (if any).
	 *  
	 *  @return A future to indicate when the body is done.
	 */
	public IFuture<Void>	body();
	
	/**
	 *  Perform the shutdown of the component (if any).
	 *  
	 *  @return A future to indicate when the shutdown is done.
	 */
	public IFuture<Void>	shutdown();
	
	/**
	 *  Get the user view of this platform component.
	 *  
	 *  @return An internal access exposing user operations of the component.
	 */
	public IInternalAccess	getInternalAccess();
	
	/**
	 *  Get the exception, if any.
	 *  
	 *  @return The failure reason for use during cleanup, if any.
	 */
	public Exception	getException();
	
	/**
	 *  Get the local platform service registry.
	 *  
	 *  @return The local platform service registry.
	 */
	public LocalServiceRegistry	getServiceRegistry();
}