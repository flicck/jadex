package jadex.bridge.component;

import java.util.Collection;
import java.util.Map;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.ISearchConstraints;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.annotation.ParameterInfo;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService.CMSStatusEvent;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITuple2Future;

public interface ICMSFeature
{
	//-------- management methods --------
	
	/**
	 *  Load a component model.
	 *  @param name The component name.
	 *  @return The model info of the 
	 */
	@ParameterInfo("modelinfo")
	public IFuture<IModelInfo> loadComponentModel(@ParameterInfo("filename") String filename, @ParameterInfo("rid") IResourceIdentifier rid);
	
	/**
	 *  Create a new component on the platform.
	 *  @param model The model identifier (e.g. file name).
	 *  @param info Additional start information such as parent component or arguments (optional).
	 *  @return The id of the component and the results after the component has been killed.
	 */
	public ITuple2Future<IComponentIdentifier, Map<String, Object>> createComponent(String model, CreationInfo info);
	
	/**
	 *  Create a new component on the platform.
	 *  @param name The component name or null for automatic generation.
	 *  @param model The model identifier (e.g. file name).
	 *  @param info Additional start information such as parent component or arguments (optional).
	 *  @return The id of the component and the results after the component has been killed.
	 */
	public ITuple2Future<IComponentIdentifier, Map<String, Object>> createComponent(String name, String model, CreationInfo info);
	
	/**
	 *  Create a new component on the platform.
	 *  This method allows for retrieving intermediate results of the component via
	 *  status events.
	 *  @param name The component name or null for automatic generation.
	 *  @param model The model identifier (e.g. file name).
	 *  @param info Additional start information such as parent component or arguments (optional).
	 *  @return The status events of the components. Consists of CMSCreatedEvent, (CMSIntermediateResultEvent)*, CMSTerminatedEvent
	 */
	public ISubscriptionIntermediateFuture<CMSStatusEvent> createComponent(CreationInfo info, String name, String model);
	
	/**
	 *  Create a new component on the platform.
	 *  @param name The component name or null for automatic generation.
	 *  @param model The model identifier (e.g. file name).
	 *  @param info Additional start information such as parent component or arguments (optional).
	 *  @param resultlistener The result listener (if any). Will receive the results of the component execution, after the component has terminated.
	 *  @return The id of the component as future result, when the component has been created and initialized.
	 *  @deprecated Use other createComponent methods.
	 */
	public IFuture<IComponentIdentifier> createComponent(String name, String model, CreationInfo info, 
		IResultListener<Collection<Tuple2<String, Object>>> resultlistener);
	
	/**
	 *  Destroy (forcefully terminate) an component on the platform.
	 *  @param componentid	The component to destroy.
	 */
	public IFuture<Map<String, Object>> destroyComponent(IComponentIdentifier componentid);
	
//	/**
//	 *  Suspend the execution of an component.
//	 *  @param componentid The component identifier.
//	 */
//	public IFuture<Void> suspendComponent(IComponentIdentifier componentid);
//	
//	/**
//	 *  Resume the execution of an component.
//	 *  @param componentid The component identifier.
//	 */
//	public IFuture<Void> resumeComponent(IComponentIdentifier componentid);
//	
	//-------- debugging methods --------
	
//	/**
//	 *  Execute a step of a suspended component.
//	 *  @param componentid The component identifier.
//	 *  @param listener Called when the step is finished (result will be the component description).
//	 */
//	public IFuture<Void> stepComponent(IComponentIdentifier componentid, String stepinfo);
//	
//	/**
//	 *  Set breakpoints for a component.
//	 *  Replaces existing breakpoints.
//	 *  To add/remove breakpoints, use current breakpoints from component description as a base.
//	 *  @param componentid The component identifier.
//	 *  @param breakpoints The new breakpoints (if any).
//	 */
//	public IFuture<Void> setComponentBreakpoints(IComponentIdentifier componentid, String[] breakpoints);
	
	//-------- information methods --------
	
//	/**
//	 *  Get the root identifier (platform).
//	 *  @return The root identifier.
//	 */
//	public IFuture<IComponentIdentifier> getRootIdentifier();
//	
//	/**
//	 *  Get the component identifiers.
//	 *  @return The component identifiers.
//	 *  This method should be used with caution when the agent population is large.
//	 */
//	public IFuture<IComponentIdentifier[]> getComponentIdentifiers();
//	
//	/**
//	 *  Get the component description of a single component.
//	 *  @param cid The component identifier.
//	 *  @return The component description of this component.
//	 */
//	public IFuture<IComponentDescription> getComponentDescription(IComponentIdentifier cid);
//	
//	/**
//	 *  Get all component descriptions.
//	 *  @return The component descriptions of the platform.
//	 */
//	public IFuture<IComponentDescription[]> getComponentDescriptions();
//	
//	/**
//	 * Search for components matching the given description.
//	 * @return An array of matching component descriptions.
//	 */
//	public IFuture<IComponentDescription[]> searchComponents(IComponentDescription adesc, ISearchConstraints con);
//	
//	/**
//	 *  Search for components matching the given description.
//	 *  @return An array of matching component descriptions.
//	 */
//	public IFuture<IComponentDescription[]> searchComponents(IComponentDescription adesc, ISearchConstraints con, boolean remote);
//	
//	//-------- listener methods --------
//	
//	/**
//	 *  Add a component listener for all components.
//	 *  The listener is registered for component changes.
//	 */
//	public ISubscriptionIntermediateFuture<CMSStatusEvent> listenToAll();
//	
//	/**
//	 *  Add a component listener for a specific component.
//	 *  The listener is registered for component changes.
//	 *  @param cid	The component to be listened.
//	 */
//	public ISubscriptionIntermediateFuture<CMSStatusEvent> listenToComponent(IComponentIdentifier cid);
	
	//-------- external access methods --------
	
	/**
	 *  Get the external access of a component.
	 *  @param cid The component identifier.
	 *  @param listener The result listener (receives an IExternalAccess object).
	 */
	public IFuture<IExternalAccess> getExternalAccess(IComponentIdentifier cid);
	
	//-------- child component accessors --------
	
//	/**
//	 *  Get the children components of a component.
//	 *  @param cid The component identifier.
//	 *  @return The children component identifiers.
//	 */
//	public IFuture<IComponentIdentifier[]> getChildren(IComponentIdentifier cid);
//	//		public IIntermediateFuture<IComponentIdentifier> getChildren(IComponentIdentifier cid);
//	
//	/**
//	 *  Get the children components of a component.
//	 *  @param cid The component identifier.
//	 *  @return The children component descriptions.
//	 */
//	public IFuture<IComponentDescription[]> getChildrenDescriptions(IComponentIdentifier cid);
	
	//-------- status events --------
	

}
