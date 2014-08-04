package jadex.platform.service.remote.commands;

import jadex.bridge.ClassInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.annotation.Security;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.IRemoteFilter;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ITerminableIntermediateFuture;
import jadex.commons.future.TerminableIntermediateFuture;
import jadex.commons.future.TerminationCommand;
import jadex.commons.transformation.annotations.Alias;
import jadex.platform.service.remote.IRemoteCommand;
import jadex.platform.service.remote.RemoteReferenceModule;
import jadex.platform.service.remote.RemoteServiceManagementService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *  Command for performing a remote service search.
 */
@Alias("jadex.base.service.remote.commands.RemoteSearchCommand")
public class RemoteSearchCommand extends AbstractRemoteCommand
{
	//-------- attributes --------

	/** The providerid (i.e. the component to start with searching). */
	protected IComponentIdentifier providerid;
	
//	/** The serach manager. */
//	protected ISearchManager manager;
//	
//	/** The visit decider. */
//	protected IVisitDecider decider;
//	
//	/** The result selector. */
//	protected IResultSelector selector;
	
	/** The callid. */
	protected String callid;
	
	/** The security level (set by postprocessing). */
	protected String securitylevel;

	/** The type. */
	protected ClassInfo type;
	
	/** The multiple flag. */
	protected boolean multiple;
	
	/** The scope. */
	protected String scope;
	
	/** The filter. */
	protected IRemoteFilter filter;
	
	//-------- constructors --------
	
	/**
	 *  Create a new remote search command.
	 */
	public RemoteSearchCommand()
	{
	}

//	/**
//	 *  Create a new remote search command.
//	 */
//	public RemoteSearchCommand(IComponentIdentifier providerid, ISearchManager manager, 
//		IVisitDecider decider, IResultSelector selector, String callid)
//	{
//		this.providerid = providerid;
//		this.manager = manager;
//		this.decider = decider;
//		this.selector = selector;
//		this.callid = callid;
//	}
	
	/**
	 *  Create a new remote search command.
	 */
	public RemoteSearchCommand(IComponentIdentifier providerid, Class<?> type, 
		boolean multiple, String scope, String callid, IRemoteFilter<?> filter)
	{
		if(type==null)
			System.out.println("type is null");
		
		this.providerid = providerid;
		this.type = new ClassInfo(type);
		this.multiple = multiple;
		this.scope = scope;
		this.callid = callid;
		this.filter = filter;
	}

	//-------- methods --------
	
	/**
	 *  Return security level determined by post-process.
	 */
	public String getSecurityLevel()
	{
		return securitylevel;
	}
	
	/**
	 *  Post-process a received command before execution
	 *  for e.g. setting security level.
	 */
	public IFuture<Void>	postprocessCommand(IInternalAccess component, RemoteReferenceModule rrm, final IComponentIdentifier target)
	{
		final Future<Void> ret = new Future<Void>();
		
		try
		{
			// Try to find security level.
			// Todo: support other result selectors!?
			if(type!=null)
			{
				rrm.getLibraryService().getClassLoader(null).addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
				{
					public void customResultAvailable(ClassLoader result)
					{
						Security	sec	= null;
						List<Class<?>>	classes	= new ArrayList<Class<?>>();
						Class<?> typecl = type.getType(result);
						classes.add(typecl);
						for(int i=0; sec==null && i<classes.size(); i++)
						{
							Class<?>	clazz	= classes.get(i);
							sec	= clazz.getAnnotation(Security.class);
							if(sec==null)
							{
								classes.addAll(Arrays.asList((Class<?>[])clazz.getInterfaces()));
								if(clazz.getSuperclass()!=null)
								{
									classes.add(clazz.getSuperclass());
								}
							}
						}
						// Default to max security if not found.
						securitylevel	= sec!=null ? sec.value() : Security.PASSWORD;
						
						ret.setResult(null);
					}
				});
			}
			else
			{
				ret.setResult(null);
			}
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}

	/**
	 *  Execute the command.
	 *  @param lrms The local remote management service.
	 *  @return An optional result command that will be 
	 *  sent back to the command origin. 
	 */
	public IIntermediateFuture<IRemoteCommand> execute(final IExternalAccess component, final RemoteServiceManagementService rsms)
	{
		final TerminableIntermediateFuture<IRemoteCommand> ret = new TerminableIntermediateFuture<IRemoteCommand>(new TerminationCommand()
		{
			public void terminated(Exception reason)
			{
				// Todo: terminate ongoing search.
			}
		});
		
//		if(type==null)
//		{
//			ret.setException(new RuntimeException("Incompatible Jadex version exception."));
//			return ret;
//		}
		
//		System.out.println("start rem search: "+callid);
		
		// Remove call when finished
		ret.addResultListener(new IResultListener<Collection<IRemoteCommand>>()
		{
			public void resultAvailable(Collection<IRemoteCommand> result)
			{
//				System.out.println("fin: "+result.size()+" "+callid);
				rsms.removeProcessingCall(callid);
			}
			public void exceptionOccurred(Exception exception)
			{
//				System.out.println("fin exe"+exception);
				rsms.removeProcessingCall(callid+" "+callid);
			}
		});
		
		// Remember invocation for termination invocation
		rsms.putProcessingCall(callid, ret);
		List<Runnable> cmds = rsms.removeFutureCommands(callid);
		if(cmds!=null)
		{
			for(Runnable cmd: cmds)
			{
				cmd.run();
			}
		}
		
		SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
			.addResultListener(new IResultListener<IComponentManagementService>()
//			.addResultListener(component.createResultListener(new IResultListener()
		{
			public void resultAvailable(IComponentManagementService cms)
			{
//				ServiceCall	next	= ServiceCall.getOrCreateNextInvocation();
//				next.setProperty("debugsource", "RemoteSearchCommand.execute()");
				
//				IComponentManagementService cms = (IComponentManagementService)result;
				cms.getExternalAccess((IComponentIdentifier)providerid).addResultListener(new IResultListener<IExternalAccess>()
				{
					public void resultAvailable(IExternalAccess exta)
					{
//						IExternalAccess exta = (IExternalAccess)result;
						
						if(type!=null)
						{
							exta.scheduleStep(new IComponentStep<Void>()
							{
								public IFuture<Void> execute(IInternalAccess ia)
								{
									Class<?> cl = type.getType(ia.getClassLoader(), ia.getModel().getAllImports());
									
									ITerminableIntermediateFuture<IService> res = (ITerminableIntermediateFuture<IService>)SServiceProvider.getServices((IServiceProvider)ia.getServiceContainer(), cl, scope, filter);
									res.addResultListener(new IIntermediateResultListener<IService>()
									{
										int cnt = 0;	
										public void intermediateResultAvailable(IService result)
										{
			//								System.out.println("result command of search: "+callid+" "+result);
											ret.addIntermediateResultIfUndone(new RemoteIntermediateResultCommand(null, result, callid, 
												false, null, false, getNonFunctionalProperties(), ret, cnt++));
										}
										
										public void finished()
										{
			//								System.out.println("result command of search fini: "+callid);
											ret.addIntermediateResultIfUndone(new RemoteIntermediateResultCommand(null, null, callid, 
												false, null, true, getNonFunctionalProperties(), ret, cnt++));
											ret.setFinishedIfUndone();
										}
										
										public void resultAvailable(Collection<IService> result)
										{
			//								System.out.println("rem search end: "+manager+" "+decider+" "+selector+" "+result);
											// Create proxy info(s) for service(s)
											Object content = null;
			//								if(result instanceof Collection)
			//								{
												List<IService> res = new ArrayList<IService>();
												for(Iterator<IService> it=result.iterator(); it.hasNext(); )
												{
													IService service = (IService)it.next();
			//										RemoteServiceManagementService.getProxyInfo(component.getComponentIdentifier(), tmp, 
			//											tmp.getServiceIdentifier(), tmp.getServiceIdentifier().getServiceType());
			//										ProxyInfo pi = getProxyInfo(component.getComponentIdentifier(), tmp);
			//										res.add(pi);
													res.add(service);
												}
												content = res;
			//								}
			//								else //if(result instanceof Object[])
			//								{
			//									IService service = (IService)result;
			////									content = getProxyInfo(component.getComponentIdentifier(), tmp);
			//									content = service;
			//								}
											
			//								ret.setResult(new RemoteResultCommand(content, null , callid, false));
											ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, content, null, callid, 
												false, null, getNonFunctionalProperties()));
											ret.setFinishedIfUndone();
										}
										
										public void exceptionOccurred(Exception exception)
										{
			//								ret.setResult(new RemoteResultCommand(null, exception, callid, false));
											ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, null, exception, callid, 
												false, null, getNonFunctionalProperties()));
											ret.setFinishedIfUndone();
										}
									});
									
									return IFuture.DONE;
								}
							}).addResultListener(new IResultListener<Void>()
							{
								public void resultAvailable(Void result)
								{
								}
								
								public void exceptionOccurred(Exception exception)
								{
									System.out.println("schedule exception: "+exception);
								}
							});
						}
						else
						{
							// start search on target component
	//						System.out.println("rem search start: "+manager+" "+decider+" "+selector);
							exta.getServiceProvider().getServices(type, scope)
//							exta.getServiceProvider().getServices(manager, decider, selector)
								.addResultListener(new IIntermediateResultListener<IService>()
							{
								int cnt = 0;	
								public void intermediateResultAvailable(IService result)
								{
	//								System.out.println("result command of search: "+callid+" "+result);
									ret.addIntermediateResultIfUndone(new RemoteIntermediateResultCommand(null, result, callid, 
										false, null, false, getNonFunctionalProperties(), ret, cnt++));
								}
								
								public void finished()
								{
	//								System.out.println("result command of search fini: "+callid);
									ret.addIntermediateResultIfUndone(new RemoteIntermediateResultCommand(null, null, callid, 
										false, null, true, getNonFunctionalProperties(), ret, cnt++));
									ret.setFinishedIfUndone();
								}
								
								public void resultAvailable(Collection<IService> result)
								{
	//								System.out.println("rem search end: "+manager+" "+decider+" "+selector+" "+result);
									// Create proxy info(s) for service(s)
									Object content = null;
	//								if(result instanceof Collection)
	//								{
										List<IService> res = new ArrayList<IService>();
										for(Iterator<IService> it=result.iterator(); it.hasNext(); )
										{
											IService service = (IService)it.next();
	//										RemoteServiceManagementService.getProxyInfo(component.getComponentIdentifier(), tmp, 
	//											tmp.getServiceIdentifier(), tmp.getServiceIdentifier().getServiceType());
	//										ProxyInfo pi = getProxyInfo(component.getComponentIdentifier(), tmp);
	//										res.add(pi);
											res.add(service);
										}
										content = res;
	//								}
	//								else //if(result instanceof Object[])
	//								{
	//									IService service = (IService)result;
	////									content = getProxyInfo(component.getComponentIdentifier(), tmp);
	//									content = service;
	//								}
									
	//								ret.setResult(new RemoteResultCommand(content, null , callid, false));
									ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, content, null, callid, 
										false, null, getNonFunctionalProperties()));
									ret.setFinishedIfUndone();
								}
								
								public void exceptionOccurred(Exception exception)
								{
	//								ret.setResult(new RemoteResultCommand(null, exception, callid, false));
									ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, null, exception, callid, 
										false, null, getNonFunctionalProperties()));
									ret.setFinishedIfUndone();
								}
							});
						}
					}
					
					public void exceptionOccurred(Exception exception)
					{
//						ret.setResult(new RemoteResultCommand(null, exception, callid, false));
						ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, null, exception, callid, 
							false, null, getNonFunctionalProperties()));
						ret.setFinishedIfUndone();
					}
				});
			}
			
			public void exceptionOccurred(Exception exception)
			{
//				ret.setResult(new RemoteResultCommand(null, exception, callid, false));
				ret.addIntermediateResultIfUndone(new RemoteResultCommand(null, null, exception, callid, 
					false, null, getNonFunctionalProperties()));
				ret.setFinishedIfUndone();
			}
		});
		
		return ret;
	}

	/**
	 *  Get the providerid.
	 *  @return the providerid.
	 */
	public IComponentIdentifier getProviderId()
	{
		return providerid;
	}

	/**
	 *  Set the providerid.
	 *  @param providerid The providerid to set.
	 */
	public void setProviderId(IComponentIdentifier providerid)
	{
		this.providerid = providerid;
	}

//	/**
//	 *  Get the manager.
//	 *  @return the manager.
//	 */
//	public ISearchManager getSearchManager()
//	{
//		return manager;
//	}
//
//	/**
//	 *  Set the manager.
//	 *  @param manager The manager to set.
//	 */
//	public void setSearchManager(ISearchManager manager)
//	{
//		this.manager = manager;
//	}
//
//	/**
//	 *  Get the decider.
//	 *  @return the decider.
//	 */
//	public IVisitDecider getVisitDecider()
//	{
//		return decider;
//	}
//
//	/**
//	 *  Set the decider.
//	 *  @param decider The decider to set.
//	 */
//	public void setVisitDecider(IVisitDecider decider)
//	{
//		this.decider = decider;
//	}
//
//	/**
//	 *  Get the selector.
//	 *  @return the selector.
//	 */
//	public IResultSelector getResultSelector()
//	{
//		return selector;
//	}
//
//	/**
//	 *  Set the selector.
//	 *  @param selector The selector to set.
//	 */
//	public void setResultSelector(IResultSelector selector)
//	{
//		this.selector = selector;
//	}

	
	
	/**
	 *  Get the callid.
	 *  @return the callid.
	 */
	public String getCallId()
	{
		return callid;
	}

	/**
	 *  Get the type.
	 *  @return The type.
	 */
	public ClassInfo getType()
	{
		return type;
	}

	/**
	 *  Set the type.
	 *  @param type The type to set.
	 */
	public void setType(ClassInfo type)
	{
		this.type = type;
	}

	/**
	 *  Get the multiple.
	 *  @return The multiple.
	 */
	public boolean isMultiple()
	{
		return multiple;
	}

	/**
	 *  Set the multiple.
	 *  @param multiple The multiple to set.
	 */
	public void setMultiple(boolean multiple)
	{
		this.multiple = multiple;
	}

	/**
	 *  Get the scope.
	 *  @return The scope.
	 */
	public String getScope()
	{
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set.
	 */
	public void setScope(String scope)
	{
		this.scope = scope;
	}

	/**
	 *  Set the callid.
	 *  @param callid The callid to set.
	 */
	public void setCallId(String callid)
	{
		this.callid = callid;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "RemoteSearchCommand [providerid=" + providerid + ", type=" + type + ", multiple=" + multiple + ", scope=" + scope + "]";
	}
}
