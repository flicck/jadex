package jadex.base.service.remote;

import jadex.base.fipa.SFipa;
import jadex.base.service.remote.commands.AbstractRemoteCommand;
import jadex.base.service.remote.commands.RemoteResultCommand;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IRemoteServiceManagementService;
import jadex.bridge.MessageType;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.library.ILibraryService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.xml.reader.Reader;
import jadex.xml.writer.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  Remote service management service that hosts the corresponding
 *  service. It basically has the task to forward messages from
 *  remote service management components on other platforms to its service.
 */
public class RemoteServiceManagementAgent extends MicroAgent
{
	//-------- attributes --------
	
	/** The remote management service. */
	protected RemoteServiceManagementService rms;
	
	//-------- constructors --------
	
	/**
	 *  Called once after agent creation.
	 */
	public IFuture	agentCreated()
	{
		final Future	ret	= new Future();
		SServiceProvider.getService(getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(createResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				final ILibraryService libservice = (ILibraryService)result;
				rms = new RemoteServiceManagementService((IMicroExternalAccess)getExternalAccess(), libservice);
				addService("rms", IRemoteServiceManagementService.class, rms, BasicServiceInvocationHandler.PROXYTYPE_DIRECT);
				ret.setResult(null);
			}
		}));
		return ret;
	}
	
//	/**
//	 *  Execute the functional body of the agent.
//	 *  Is only called once.
//	 */
//	public void executeBody()
//	{
//		ICommand gcc = new ICommand()
//		{
//			public void execute(Object args)
//			{
//				System.gc();
//				waitFor(5000, this);
//			}
//		};
//		waitFor(5000, gcc);
//	}
	
	/**
	 *  Called just before the agent is removed from the platform.
	 *  @return The result of the component.
	 */
	public IFuture	agentKilled()
	{
		// Send notifications to other processes that remote references are not needed any longer.
		return rms.getRemoteReferenceModule().shutdown();
	}
	
	/**
	 *  Called, whenever a message is received.
	 *  @param msg The message.
	 *  @param mt The message type.
	 */
	public void messageArrived(final Map msg, final MessageType mt)
	{
		if(SFipa.MESSAGE_TYPE_NAME_FIPA.equals(mt.getName()))
		{
			SServiceProvider.getService(getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(createResultListener(new DefaultResultListener()
			{
				public void resultAvailable(Object result)
				{
					// Hack!!! Manual decoding for using custom class loader.
					final ILibraryService ls = (ILibraryService)result;
					Object content = msg.get(SFipa.CONTENT);
					final String callid = (String)msg.get(SFipa.CONVERSATION_ID);
					
//					System.out.println("received: "+callid);
					
//					if(((String)content).indexOf("store")!=-1)
//						System.out.println("store command: "+callid+" "+getComponentIdentifier());

//					// For debugging.
//					final String orig = (String)content;

					if(content instanceof String)
					{
						// Catch decode problems.
						// Should be ignored or be a warning.
						try
						{	
							List	errors	= new ArrayList();
//							String contentcopy = (String)content;	// for debugging
							content = Reader.objectFromXML(rms.getReader(), (String)content, ls.getClassLoader(), errors);
							
							// For corrupt result (e.g. if class not found) set exception to clean up waiting call.
							if(!errors.isEmpty())
							{
//								System.out.println("Error: "+contentcopy);
								if(content instanceof RemoteResultCommand)
								{
//									System.out.println("corrupt content: "+content);
//									System.out.println("errors: "+errors);
									((RemoteResultCommand)content).setExceptionInfo(new ExceptionInfo(new RuntimeException("Errors during XML decoding: "+errors)));
								}
								else
								{
//									content	= null;
									content = new RemoteResultCommand(null, new RuntimeException("Errors during XML decoding: "+errors), callid, false);
								}
								getLogger().info("Remote service management service could not decode message from: "+msg.get(SFipa.SENDER));
//								getLogger().warning("Remote service management service could not decode message."+orig+"\n"+errors);
							}
						}
						catch(Exception e)
						{
//							content	= null;
							content = new RemoteResultCommand(null, e, callid, false);
							getLogger().info("Remote service management service could not decode message from: "+msg.get(SFipa.SENDER));
//							getLogger().warning("Remote service management service could not decode message."+orig);
//							e.printStackTrace();
						}
					}
					
					if(content instanceof IRemoteCommand)
					{
						final IRemoteCommand com = (IRemoteCommand)content;
						
//						if(content instanceof RemoteResultCommand && ((RemoteResultCommand)content).getMethodName()!=null && ((RemoteResultCommand)content).getMethodName().indexOf("store")!=-1)
//							System.out.println("result of command1: "+com+" "+result);
						
						com.execute((IMicroExternalAccess)getExternalAccess(), rms).addResultListener(createResultListener(new DefaultResultListener()
						{
							public void resultAvailable(Object result)
							{
//								if(((String)orig).indexOf("store")!=-1)
//									System.out.println("result of command2: "+com+" "+result);
								if(result!=null)
								{
									final Object repcontent = result;
									if(repcontent instanceof AbstractRemoteCommand)
										((AbstractRemoteCommand)repcontent).preprocessCommand(rms.getRemoteReferenceModule(), (IComponentIdentifier)msg.get(SFipa.SENDER));
									
									createReply(msg, mt).addResultListener(createResultListener(new DefaultResultListener()
									{
										public void resultAvailable(Object result)
										{
											Map reply = (Map)result;
//											reply.put(SFipa.CONTENT, JavaWriter.objectToXML(repcontent, ls.getClassLoader()));
											String content = Writer.objectToXML(rms.getWriter(), repcontent, ls.getClassLoader(), msg.get(SFipa.SENDER));
											reply.put(SFipa.CONTENT, content);
//											System.out.println("content: "+content);
											
//											System.out.println("reply: "+callid);
											sendMessage(reply, mt);
										}
										public void exceptionOccurred(Exception exception)
										{
											// Terminated, when rms killed in mean time
											if(!(exception instanceof ComponentTerminatedException))
											{
												super.exceptionOccurred(exception);
											}
										}
									}));
								}
							}
							public void exceptionOccurred(Exception exception)
							{
								// Terminated, when rms killed in mean time
								if(!(exception instanceof ComponentTerminatedException))
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}
					else if(content!=null)
					{
						getLogger().info("RMS unexpected message content: "+content);
					}
				}
			}));
		}
	}
}
