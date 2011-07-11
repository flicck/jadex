package jadex.base.service.remote.commands;

import jadex.base.service.remote.IRemoteCommand;
import jadex.base.service.remote.ProxyReference;
import jadex.base.service.remote.RemoteReference;
import jadex.base.service.remote.RemoteReferenceModule;
import jadex.base.service.remote.RemoteServiceManagementService;
import jadex.base.service.remote.xml.RMIPreProcessor;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.IMicroExternalAccess;
import jadex.xml.writer.WriteContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  Command for executing a remote method.
 */
public class RemoteMethodInvocationCommand extends AbstractRemoteCommand
{
	//-------- attributes --------
	
	/** The remote reference. */
	protected RemoteReference rr;
	
	/** The method. */
	// Is not transferred, only used for preprocessing
	protected Method method;
	
	/** The methodname. */
	protected String methodname;
	
	/** The parameter types. */
	protected Class[] parametertypes;
	
	/** The parameter values. */
	protected Object[] parametervalues;
	
	/** The declared reference flag for the return value. */
	protected boolean returnisref;
	
	/** The call identifier. */
	protected String callid;
	
	//-------- constructors --------
	
	/**
	 *  Create a new remote method invocation command.
	 */
	public RemoteMethodInvocationCommand()
	{
	}
	
	/**
	 *  Create a new remote method invocation command. 
	 */
	public RemoteMethodInvocationCommand(RemoteReference rr, Method method, 
		Object[] parametervalues, String callid)
	{
		this.rr = rr;
		this.method = method;
		this.methodname = method.getName();
		this.parametertypes = method.getParameterTypes();
		this.parametervalues = parametervalues;
		this.callid = callid;
//		System.out.println("rmi on client: "+callid+" "+methodname);
	}
	
	//-------- methods --------

	/**
	 *  Preprocess command and replace  if they are remote references.
	 */
	public void preprocessCommand(RemoteReferenceModule rrm, IComponentIdentifier target)
	{
		if(parametertypes.length>0)
		{
			RMIPreProcessor preproc = new RMIPreProcessor(rrm);
			boolean[] refs = SServiceProvider.getRemoteReferenceInfo(method, false);
			WriteContext context = new WriteContext(null, target, null, null);
			for(int i=0; i<parametertypes.length; i++)
			{
				if(refs[i] || SServiceProvider.isRemoteReference(parametervalues[i]))
				{
//					System.out.println("found ref: "+parametervalues[i]);
					parametervalues[i] = preproc.preProcess(context, parametervalues[i]);
				}
			}
			returnisref = SServiceProvider.isReturnValueRemoteReference(method, false);
		}
	}
	
	/**
	 *  Execute the command.
	 *  @param lrms The local remote management service.
	 *  @return An optional result command that will be 
	 *  sent back to the command origin. 
	 */
	public IFuture execute(IMicroExternalAccess component, RemoteServiceManagementService rsms)
	{
		final Future ret = new Future();
		
		rsms.getRemoteReferenceModule().getTargetObject(getRemoteReference())
			.addResultListener(new IResultListener() // todo: createResultListener?!
		{
			public void resultAvailable(Object result)
			{
				invokeMethod(result, ret);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setResult(new RemoteResultCommand(null, new RuntimeException(
					"Target object not found: "+getRemoteReference()), callid, false));
			}
		});
		
		
//		if(getRemoteReference().getTargetIdentifier() instanceof String)
//		{
//			Object target = rsms.getRemoteReferenceModule().getTargetObject(getRemoteReference());
//			if(target!=null)
//			{
//				invokeMethod(target, ret);
//			}
//			else
//			{
//				ret.setResult(new RemoteResultCommand(null, new RuntimeException("Target object not found: "+tid), callid));
//			}
//		}
//		else if(getRemoteReference().getTargetIdentifier() instanceof IServiceIdentifier)
//		{
//			IServiceIdentifier sid = (IServiceIdentifier)getRemoteReference().getTargetIdentifier();
//			
//			// fetch service via its id
//			SServiceProvider.getService(component.getServiceProvider(), sid)
//				.addResultListener(new IResultListener()
//			{
//				public void resultAvailable(Object source, Object result)
//				{
//					invokeMethod(result, ret);
//				}
//				
//				public void exceptionOccurred(Object source, Exception exception)
//				{
//					ret.setResult(new RemoteResultCommand(null, exception, callid));
//				}
//			});
//		}
//		else if(getRemoteReference().getTargetIdentifier() instanceof IComponentIdentifier)
//		{
//			final IComponentIdentifier cid = (IComponentIdentifier)getRemoteReference().getTargetIdentifier();
//			
//			// fetch component via target component id
//			SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
//				.addResultListener(new IResultListener()
////				.addResultListener(component.createResultListener(new IResultListener()
//			{
//				public void resultAvailable(Object source, Object result)
//				{
//					IComponentManagementService cms = (IComponentManagementService)result;
//					
//					// fetch target component via component identifier.
//					cms.getExternalAccess(cid).addResultListener(new IResultListener()
//					{
//						public void resultAvailable(Object source, Object result)
//						{
//							IExternalAccess exta = (IExternalAccess)result;
//							
//							// invoke method directly on external access
//							invokeMethod(exta, ret);
//						}
//						
//						public void exceptionOccurred(Object source, Exception exception)
//						{
//							ret.setResult(new RemoteResultCommand(null, exception, callid));
//						}
//					});
//				}
//				
//				public void exceptionOccurred(Object source, Exception exception)
//				{
//					ret.setResult(new RemoteResultCommand(null, exception, callid));
//				}
//			});
//		}
//		else
//		{
//			System.out.println("remote invocation error, not target could be found: "+getRemoteReference().getTargetIdentifier());
//		}
		
		return ret;
	}

	/**
	 *  Invoke remote method.
	 *  @param target The target object.
	 *  @param ret The result future.
	 */
	public void invokeMethod(Object target, final Future ret)
	{
//		if("addMessageListener".equals(methodname))
//			System.out.println("remote addMessageListener");
		
		try
		{
			// fetch method on service and invoke method
			Method m = target.getClass().getMethod(methodname, parametertypes);
			
//			System.out.println("invoke: "+m);
			
			// Necessary due to Java inner class bug 4071957
			if(target.getClass().isAnonymousClass())
				m.setAccessible(true);
			
			Object res = m.invoke(target, parametervalues);
			
			if(res instanceof IFuture)
			{
				((IFuture)res).addResultListener(new IResultListener()
				{
					public void resultAvailable(Object result)
					{
						ret.setResult(new RemoteResultCommand(result, null, callid, returnisref));
					}
					
					public void exceptionOccurred(Exception exception)
					{
						ret.setResult(new RemoteResultCommand(null, exception, callid, false));
					}
				});
			}
			else
			{
				ret.setResult(new RemoteResultCommand(res, null, callid, returnisref));
			}
		}
		catch(Exception exception)
		{
			if(exception instanceof InvocationTargetException
				&& ((InvocationTargetException)exception).getTargetException() instanceof Exception)
			{
				exception	= (Exception)((InvocationTargetException)exception).getTargetException();
			}
			ret.setResult(new RemoteResultCommand(null, exception, callid, false));
		}
	}
	
	//-------- getter/setter methods --------

	/**
	 *  Get the methodname.
	 *  @return the methodname.
	 */
	public String getMethodName()
	{
		return methodname;
	}

	/**
	 *  Set the methodname.
	 *  @param methodname The methodname to set.
	 */
	public void setMethodName(String methodname)
	{
		this.methodname = methodname;
	}
	
	/**
	 *  Get the parametertypes.
	 *  @return the parametertypes.
	 */
	public Class[] getParameterTypes()
	{
		return parametertypes;
	}
	
	/**
	 *  Set the parametertypes.
	 *  @param parametertypes The parametertypes to set.
	 */
	public void setParameterTypes(Class[] parametertypes)
	{
		this.parametertypes = parametertypes;
	}

	/**
	 *  Get the parametervalues.
	 *  @return the parametervalues.
	 */
	public Object[] getParameterValues()
	{
		return parametervalues;
	}

	/**
	 *  Set the parametervalues.
	 *  @param parametervalues The parametervalues to set.
	 */
	public void setParameterValues(Object[] parametervalues)
	{
		this.parametervalues = parametervalues;
	}

	/**
	 *  Get the callid.
	 *  @return the callid.
	 */
	public String getCallId()
	{
		return callid;
	}

	/**
	 *  Set the callid.
	 *  @param callid The callid to set.
	 */
	public void setCallId(String callid)
	{
//		System.out.println("rmi on server: "+callid);
		this.callid = callid;
	}

	/**
	 *  Get the remote reference.
	 *  @return The remote reference.
	 */
	public RemoteReference getRemoteReference()
	{
		return rr;
	}

	/**
	 *  Set the remote reference.
	 *  @param rr The remote reference to set.
	 */
	public void setRemoteReference(RemoteReference rr)
	{
		this.rr = rr;
	}
	
	/**
	 *  Get the returnisref.
	 *  @return the returnisref.
	 */
	public boolean isReturnValueReference()
	{
		return returnisref;
	}

	/**
	 *  Set the returnisref.
	 *  @param returnisref The returnisref to set.
	 */
	public void setReturnValueReference(boolean returnisref)
	{
		this.returnisref = returnisref;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "RemoteMethodInvocationCommand(remote reference=" + rr + ", methodname="
			+ methodname + ", callid=" + callid + ")";
	}
	
}

