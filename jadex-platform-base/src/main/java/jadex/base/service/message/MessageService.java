package jadex.base.service.message;

import jadex.base.AbstractComponentAdapter;
import jadex.base.service.message.transport.ITransport;
import jadex.base.service.message.transport.MessageEnvelope;
import jadex.base.service.message.transport.codecs.CodecFactory;
import jadex.base.service.message.transport.codecs.ICodec;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.ContentException;
import jadex.bridge.DefaultMessageAdapter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInputConnection;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IMessageAdapter;
import jadex.bridge.IOutputConnection;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.MessageFailureException;
import jadex.bridge.ServiceTerminatedException;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.execution.IExecutionService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.message.IContentCodec;
import jadex.bridge.service.types.message.IMessageListener;
import jadex.bridge.service.types.message.IMessageService;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.ICommand;
import jadex.commons.IFilter;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.collection.LRU;
import jadex.commons.collection.MultiCollection;
import jadex.commons.collection.SCollection;
import jadex.commons.concurrent.IExecutable;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.transformation.annotations.Classname;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Logger;


/**
 *  The Message service serves several message-oriented purposes: a) sending and
 *  delivering messages by using transports b) management of transports
 *  (add/remove)
 *  
 *  The message service performs sending and delivering messages by separate actions
 *  that are individually executed on the execution service, i.e. they are delivered
 *  synchronous or asynchronous depending on the execution service mode.
 */
public class MessageService extends BasicService implements IMessageService
{
	//-------- constants --------
	
	/** The default codecs. */
    public static IContentCodec[] DEFCODECS = new IContentCodec[]
    {
        new jadex.base.contentcodecs.JavaXMLContentCodec(),
        new jadex.base.contentcodecs.JadexXMLContentCodec(),
        new jadex.base.contentcodecs.NuggetsXMLContentCodec(),
		new jadex.base.contentcodecs.JadexBinaryContentCodec(),
        
    };

	//-------- attributes --------

	/** The component. */
    protected IExternalAccess component;

	/** The transports. */
	protected List<ITransport> transports;

	/** All addresses of this platform. */
	private String[] addresses;

	/** The message types. */
	protected Map messagetypes;
	
	/** The deliver message action executed by platform executor. */
	protected DeliverMessage delivermsg;
	
	/** The logger. */
	protected Logger	logger;
	
	/** The listeners (listener->filter). */
	protected Map<IMessageListener, IFilter> listeners;
	
	/** The cashed clock service. */
	protected IClockService	clockservice;
	
	/** The library service. */
	protected ILibraryService libservice;
	
	/** The class loader of the message service (only for envelope en/decoding, content is handled by receiver class loader). */
	protected ClassLoader classloader;
	
//	/** The cashed clock service. */
//	protected IComponentManagementService cms;
	
	/** The target managers (platform id->manager). */
	protected LRU<IComponentIdentifier, SendManager> managers;
	
	/** The content codecs. */
	protected List contentcodecs;
	
	/** The codec factory for messages. */
	protected CodecFactory codecfactory;
	
	/** The delivery handler map. */
	protected Map<Byte, ICommand> deliveryhandlers;
	
	
	/** The initiator connections. */
	protected Map<Integer, AbstractConnectionHandler> icons;

	/** The participant connections. */
	protected Map<Integer, AbstractConnectionHandler> pcons;

	//-------- constructors --------

	/**
	 *  Constructor for Outbox.
	 *  @param platform
	 */
	public MessageService(IExternalAccess component, Logger logger, ITransport[] transports, 
		MessageType[] messagetypes)
	{
		this(component, logger, transports, messagetypes, null, null);
	}
	
	/**
	 *  Constructor for Outbox.
	 *  @param platform
	 */
	public MessageService(IExternalAccess component, Logger logger, ITransport[] transports, 
		MessageType[] messagetypes, IContentCodec[] contentcodecs, CodecFactory codecfactory)
	{
		super(component.getServiceProvider().getId(), IMessageService.class, null);

		this.component = component;
		this.transports = SCollection.createArrayList();
		for(int i=0; i<transports.length; i++)
		{
			// Allow nulls to make it easier to exclude transports via platform configuration.
			if(transports[i]!=null)
				this.transports.add(transports[i]);
		}
		this.messagetypes	= SCollection.createHashMap();
		for(int i=0; i<messagetypes.length; i++)
			this.messagetypes.put(messagetypes[i].getName(), messagetypes[i]);		
		this.delivermsg = new DeliverMessage();
		this.logger = logger;
		
		this.managers = new LRU<IComponentIdentifier, SendManager>(800);
		if(contentcodecs!=null)
		{
			for(int i=0; i<contentcodecs.length; i++)
			{
				addContentCodec(contentcodecs[i]);
			}
		}
		this.codecfactory = codecfactory!=null? codecfactory: new CodecFactory();
		
		this.deliveryhandlers = new HashMap<Byte, ICommand>();
		deliveryhandlers.put(MapSendTask.MESSAGE_TYPE_MAP, new MapDeliveryHandler());
		deliveryhandlers.put(StreamSendTask.MESSAGE_TYPE_STREAM, new StreamDeliveryHandler());
		
		this.icons = Collections.synchronizedMap(new HashMap<Integer, AbstractConnectionHandler>());
		this.pcons = Collections.synchronizedMap(new HashMap<Integer, AbstractConnectionHandler>());
	}
	
	//-------- interface methods --------

	/**
	 * 
	 */
	public IInputConnection getParticipantInputConnection(int conid)
	{
		return pcons.get(conid)==null? null: (IInputConnection)pcons.get(conid).getConnection();
	}
	
	/**
	 * 
	 */
	public IOutputConnection getParticipantOutputConnection(int conid)
	{
		return pcons.get(conid)==null? null: (IOutputConnection)pcons.get(conid).getConnection();
	}
	
	/**
	 *  Create a virtual output connection.
	 */
	public OutputConnection internalCreateOutputConnection(IComponentIdentifier sender, IComponentIdentifier receiver)
	{
		UUID uuconid = UUID.randomUUID();
		int conid = uuconid.hashCode();
		OutputConnectionHandler och = new OutputConnectionHandler(this);
		OutputConnection con = new OutputConnection(internalUpdateComponentIdentifier(sender), 
			internalUpdateComponentIdentifier(receiver), conid, true, och);
		icons.put(conid, och);
		return con;
	}
	
	/**
	 *  Create a virtual output connection.
	 */
	public IFuture<IOutputConnection> createOutputConnection(IComponentIdentifier sender, IComponentIdentifier receiver)
	{
		return new Future<IOutputConnection>(internalCreateOutputConnection(sender, receiver));
	}

	/**
	 *  Create a virtual input connection.
	 */
	public InputConnection internalCreateInputConnection(IComponentIdentifier sender, IComponentIdentifier receiver)
	{
		UUID uuconid = UUID.randomUUID();
		int conid = uuconid.hashCode();
		InputConnectionHandler ich = new InputConnectionHandler(this);
		InputConnection con = new InputConnection(internalUpdateComponentIdentifier(sender), 
			internalUpdateComponentIdentifier(receiver), conid, true, ich);
		icons.put(conid, ich);
		return con;
	}
	
	/**
	 *  Create a virtual input connection.
	 */
	public IFuture<IInputConnection> createInputConnection(IComponentIdentifier sender, IComponentIdentifier receiver)
	{
		return new Future<IInputConnection>(internalCreateInputConnection(sender, receiver));
	}

	/**
	 *  Send a message.
	 *  @param message The native message.
	 */
	public IFuture<Void> sendMessage(final Map<String, Object> origmsg, final MessageType type, 
		IComponentIdentifier osender, final IResourceIdentifier rid, final byte[] codecids)
	{
		final Map<String, Object> msg = new HashMap<String, Object>(origmsg);
		
		final Future<Void> ret = new Future<Void>();
		final IComponentIdentifier sender = internalUpdateComponentIdentifier(osender);
		
		libservice.getClassLoader(rid)
			.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
		{
			public void customResultAvailable(final ClassLoader cl)
			{
//				IComponentIdentifier sender = adapter.getComponentIdentifier();
				if(sender==null)
				{
					ret.setException(new RuntimeException("Sender must not be null: "+msg));
					return;
				}
			
				// Replace own component identifiers.
				// Now done just before send
//				String[] params = type.getParameterNames();
//				for(int i=0; i<params.length; i++)
//				{
//					Object o = msg.get(params[i]);
//					if(o instanceof IComponentIdentifier)
//					{
//						msg.put(params[i], updateComponentIdentifier((IComponentIdentifier)o));
//					}
//				}
//				String[] paramsets = type.getParameterSetNames();
//				for(int i=0; i<paramsets.length; i++)
//				{
//					Object o = msg.get(paramsets[i]);
//					
//					if(SReflect.isIterable(o))
//					{
//						List rep = new ArrayList();
//						for(Iterator it=SReflect.getIterator(o); it.hasNext(); )
//						{
//							Object item = it.next();
//							if(item instanceof IComponentIdentifier)
//							{
//								rep.add(updateComponentIdentifier((IComponentIdentifier)item));
//							}
//							else
//							{
//								rep.add(item);
//							}
//						}
//						msg.put(paramsets[i], rep);
//					}
//					else if(o instanceof IComponentIdentifier)
//					{
//						msg.put(paramsets[i], updateComponentIdentifier((IComponentIdentifier)o));
//					}
//				}
				
				// Automatically add optional meta information.
				String senid = type.getSenderIdentifier();
				if(msg.get(senid)==null)
					msg.put(senid, sender);
				
				final String idid = type.getIdIdentifier();
				if(msg.get(idid)==null)
					msg.put(idid, SUtil.createUniqueId(sender.getLocalName()));

				final String sd = type.getTimestampIdentifier();
				if(msg.get(sd)==null)
				{
					msg.put(sd, ""+clockservice.getTime());
				}
				
				final String ridid = type.getResourceIdIdentifier();
				if(msg.get(ridid)==null)
				{
					msg.put(ridid, rid);
				}
				
				// Check receivers.
				Object tmp = msg.get(type.getReceiverIdentifier());
				if(tmp==null || SReflect.isIterable(tmp) &&	!SReflect.getIterator(tmp).hasNext())
				{
					ret.setException(new RuntimeException("Receivers must not be empty: "+msg));
					return;
				}
				
				if(SReflect.isIterable(tmp))
				{
					for(Iterator<?> it=SReflect.getIterator(tmp); it.hasNext(); )
					{
						IComponentIdentifier rec = (IComponentIdentifier)it.next();
						if(rec==null)
						{
							ret.setException(new MessageFailureException(msg, type, null, "A receiver nulls: "+msg));
							return;
						}
						// Addresses may only null for local messages, i.e. intra platform communication
						else if(rec.getAddresses()==null && 
							!(rec.getPlatformName().equals(component.getComponentIdentifier().getPlatformName())))
						{
							ret.setException(new MessageFailureException(msg, type, null, "A receiver addresses nulls: "+msg));
							return;
						}
					}
				}
				
				// External access of sender required for content encoding etc.
				SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
					.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
				{
					public void customResultAvailable(IComponentManagementService cms)
					{
						cms.getExternalAccess(sender).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
						{
							public void customResultAvailable(IExternalAccess exta)
							{
//								System.out.println("msgservice calling doSendMessage()");
								doSendMessage(msg, type, exta, cl, ret, codecids);
							}
						});
					}
				});
			}
		});
		
		return ret;
	}

	/**
	 *  Extracted method to be callable from listener.
	 */
	protected void doSendMessage(Map<String, Object> msg, MessageType type, IExternalAccess comp, ClassLoader cl, Future<Void> ret, byte[] codecids)
	{
		Map<String, Object> msgcopy	= new HashMap<String, Object>(msg);
		
		// Conversion via platform specific codecs
		// Hack?! Preprocess content to enhance component identifiers.
		IContentCodec[] compcodecs = getContentCodecs(comp.getModel(), cl);
		List<ITraverseProcessor> procs = Traverser.getDefaultProcessors();
		procs.add(1, new ITraverseProcessor()
		{
			public Object process(Object object, Class<?> clazz,
				List<ITraverseProcessor> processors, Traverser traverser,
				Map<Object, Object> traversed, boolean clone, ClassLoader targetcl, Object context)
			{
				return internalUpdateComponentIdentifier((IComponentIdentifier)object);
			}
			
			public boolean isApplicable(Object object, Class<?> clazz, boolean clone, ClassLoader targetcl)
			{
				return object instanceof IComponentIdentifier;
			}
		});
		
		for(Iterator<String> it=msgcopy.keySet().iterator(); it.hasNext(); )
		{
			String	name	= it.next();
			Object	value	= msgcopy.get(name);
			value = Traverser.traverseObject(value, procs, false, null);
			msgcopy.put(name, value);
			
			IContentCodec codec = type.findContentCodec(compcodecs, msgcopy, name);
			if(codec==null)
				codec = type.findContentCodec(getContentCodecs(), msgcopy, name);
			
			if(codec!=null)
			{
				msgcopy.put(name, codec.encode(value, cl));
			}
			else if(value!=null && !((value instanceof String) || (value instanceof byte[])) 
				&& !(name.equals(type.getSenderIdentifier()) || name.equals(type.getReceiverIdentifier())
					|| name.equals(type.getResourceIdIdentifier())))
			{	
				ret.setException(new ContentException("No content codec found for: "+name+", "+msgcopy));
				return;
			}
		}
		
		IComponentIdentifier sender = (IComponentIdentifier)msgcopy.get(type.getSenderIdentifier());
//		if(sender.getAddresses()==null || sender.getAddresses().length==0)
//			System.out.println("schrott2");
		
		IFilter[] fils;
		IMessageListener[] lis;
		synchronized(this)
		{
			fils = listeners==null? null: listeners.values().toArray(new IFilter[listeners.size()]);
			lis = listeners==null? null: listeners.keySet().toArray(new IMessageListener[listeners.size()]);
		}
		
		if(lis!=null)
		{
			// Hack?!
			IMessageAdapter msgadapter = new DefaultMessageAdapter(msgcopy, type);
			for(int i=0; i<lis.length; i++)
			{
				IMessageListener li = (IMessageListener)lis[i];
				boolean	match	= false;
				try
				{
					match	= fils[i]==null || fils[i].filter(msgadapter);
				}
				catch(Exception e)
				{
					logger.warning("Filter threw exception: "+fils[i]+", "+e);
				}
				if(match)
				{
					try
					{
						li.messageSent(msgadapter);
					}
					catch(Exception e)
					{
						logger.warning("Listener threw exception: "+li+", "+e);
					}
				}
			}
		}
		
		// Sending a message is delegated to SendManagers
		// Each SendManager is responsible for a specific destination
		// in order to decouple sending to different destinations.
		
		// Determine manager tasks
		MultiCollection managers = new MultiCollection();
		String recid = type.getReceiverIdentifier();
		Object tmp	= msgcopy.get(recid);
		if(SReflect.isIterable(tmp))
		{
			for(Iterator<?> it = SReflect.getIterator(tmp); it.hasNext(); )
			{
				IComponentIdentifier cid = (IComponentIdentifier)it.next();
				SendManager sm = getSendManager(cid); 
				managers.put(sm, cid);
			}
		}
		else
		{
			IComponentIdentifier cid = (IComponentIdentifier)tmp;
			SendManager sm = getSendManager(cid); 
			managers.put(sm, cid);
		}
		
		byte[] cids	= codecids;
		if(cids==null || cids.length==0)
			cids = codecfactory.getDefaultCodecIds();
		ICodec[] codecs = getMessageCodecs(cids);
//		ICodec[] codecs = new ICodec[cids.length];
//		for(int i=0; i<codecs.length; i++)
//		{
//			codecs[i] = codecfactory.getCodec(cids[i]);
//		}
		
		CounterResultListener<Void> crl = new CounterResultListener<Void>(managers.size(), false, new DelegationResultListener<Void>(ret));
		for(Iterator<?> it=managers.keySet().iterator(); it.hasNext();)
		{
			SendManager tm = (SendManager)it.next();
			IComponentIdentifier[] recs = (IComponentIdentifier[])managers.getCollection(tm).toArray(new IComponentIdentifier[0]);
			
			MapSendTask task = new MapSendTask(msgcopy, type, recs, getTransports(), cids, codecs);
			tm.addMessage(task).addResultListener(crl);
//			task.getSendManager().addMessage(task).addResultListener(crl);
		}
		
//		sendmsg.addMessage(msgcopy, type, receivers, ret);
	}
	
	/**
	 *  Get array of message codecs for codec ids.
	 */
	public ICodec[] getMessageCodecs(byte[] codecids)
	{
		ICodec[] codecs = new ICodec[codecids.length];
		for(int i=0; i<codecs.length; i++)
		{
			codecs[i] = codecfactory.getCodec(codecids[i]);
		}
		return codecs;
	}
	
	/**
	 *  Get content codecs.
	 *  @return The content codecs.
	 */
	public IContentCodec[] getContentCodecs()
	{
		return contentcodecs==null? DEFCODECS: (IContentCodec[])contentcodecs.toArray(new IContentCodec[contentcodecs.size()]);
	}
	
	/**
	 *  Get a matching content codec.
	 *  @param props The properties.
	 *  @return The content codec.
	 */
	public IContentCodec[] getContentCodecs(IModelInfo model, ClassLoader cl)
	{
		List ret = null;
		Map	props	= model.getProperties();
		if(props!=null)
		{
			for(Iterator it=props.keySet().iterator(); ret==null && it.hasNext();)
			{
				String name = (String)it.next();
				if(name.startsWith("contentcodec."))
				{
					if(ret==null)
						ret	= new ArrayList();
					ret.add(model.getProperty(name, cl));
				}
			}
		}

		return ret!=null? (IContentCodec[])ret.toArray(new IContentCodec[ret.size()]): null;
	}

	/**
	 *  Get the codec factory.
	 *  @return The codec factory.
	 */
	public CodecFactory getCodecFactory()
	{
		return codecfactory;
	}
	
//	/**
//	 *  Get the clock service.
//	 *  @return The clock service.
//	 */
//	public IClockService getClockService()
//	{
//		return clockservice;
//	}
	
//	/**
//	 *  Deliver a message to some components.
//	 */
//	public void deliverMessage(Map<String, Object> msg, String type, IComponentIdentifier[] receivers)
//	{
//		delivermsg.addMessage(new MessageEnvelope(msg, Arrays.asList(receivers), type));
//	}
	
	/**
	 *  Deliver a message to the intended components. Called from transports.
	 *  @param message The native message. 
	 *  (Synchronized because can be called from concurrently executing transports)
	 */
	public void deliverMessage(Object msg)
	{
		delivermsg.addMessage(msg);
	}
	
	/**
	 *  Adds a transport for this outbox.
	 *  @param transport The transport.
	 */
	public void addTransport(ITransport transport)
	{
		transports.add(transport);
		addresses = null;
	}

	/**
	 *  Remove a transport for the outbox.
	 *  @param transport The transport.
	 */
	public void removeTransport(ITransport transport)
	{
		transports.remove(transport);
		transport.shutdown();
		addresses = null;
	}

	/**
	 *  Moves a transport up or down.
	 *  @param up Move up?
	 *  @param transport The transport to move.
	 */
	public synchronized void changeTransportPosition(boolean up, ITransport transport)
	{
		int index = transports.indexOf(transport);
		if(up && index>0)
		{
			ITransport temptrans = (ITransport)transports.get(index - 1);
			transports.set(index - 1, transport);
			transports.set(index, temptrans);
		}
		else if(index!=-1 && index<transports.size()-1)
		{
			ITransport temptrans = (ITransport)transports.get(index + 1);
			transports.set(index + 1, transport);
			transports.set(index, temptrans);
		}
		else
		{
			throw new RuntimeException("Cannot change transport position from "
				+index+(up? " up": " down"));
		}
	}

	/**
	 *  Get the adresses of a component.
	 *  @return The addresses of this component.
	 */
	public String[] internalGetAddresses()
	{
		if(addresses == null)
		{
			ITransport[] trans = (ITransport[])transports.toArray(new ITransport[transports.size()]);
			ArrayList addrs = new ArrayList();
			for(int i = 0; i < trans.length; i++)
			{
				String[] traddrs = trans[i].getAddresses();
				for(int j = 0; traddrs!=null && j<traddrs.length; j++)
					addrs.add(traddrs[j]);
			}
			addresses = (String[])addrs.toArray(new String[addrs.size()]);
		}

		return addresses;
	}
	
	/**
	 *  Get the adresses of a component.
	 *  @return The addresses of this component.
	 */
	public IFuture<String[]> getAddresses()
	{
		return new Future<String[]>(internalGetAddresses());
	}
	
	/**
	 *  Get addresses of all transports.
	 *  @return The address schemes of all transports.
	 */
	public String[] getAddressSchemes()
	{
		ITransport[] trans = (ITransport[])transports.toArray(new ITransport[transports.size()]);
		ArrayList schemes = new ArrayList();
		for(int i = 0; i < trans.length; i++)
		{
			String[] aschemes = trans[i].getServiceSchemas();
			schemes.addAll(Arrays.asList(aschemes));
		}

		return (String[])schemes.toArray(new String[schemes.size()]);
	}

	/**
	 *  Get the transports.
	 *  @return The transports.
	 */
	public ITransport[] getTransports()
	{
		ITransport[] transportsArray = new ITransport[transports.size()];
		return (ITransport[])transports.toArray(transportsArray);
	}
	
	/**
	 *  Get a send target manager for addresses.
	 */
	public SendManager getSendManager(IComponentIdentifier cid)
	{
		SendManager ret = managers.get(cid.getRoot());
		
		if(ret==null)
		{
			ret = new SendManager();
			managers.put(cid.getRoot(), ret);
		}
		
		return ret;
	}

	//-------- IPlatformService interface --------
	
//	/**
//	 *  Start the service.
//	 */
//	public IFuture startService()
//	{
//		final Future ret = new Future();
//		
//		ITransport[] tps = (ITransport[])transports.toArray(new ITransport[transports.size()]);
//		if(transports.size()==0)
//		{
//			ret.setException(new RuntimeException("MessageService has no working transport for sending messages."));
//		}
//		else
//		{
//			CounterResultListener lis = new CounterResultListener(tps.length, new IResultListener()
//			{
//				public void resultAvailable(Object result)
//				{
//					SServiceProvider.getService(provider, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new IResultListener()
//					{
//						public void resultAvailable(Object result)
//						{
//							clockservice = (IClockService)result;
//							SServiceProvider.getServiceUpwards(provider, IComponentManagementService.class).addResultListener(new IResultListener()
//							{
//								public void resultAvailable(Object result)
//								{
//									cms = (IComponentManagementService)result;
//									MessageService.super.startService().addResultListener(new DelegationResultListener(ret));
//								}
//								
//								public void exceptionOccurred(Exception exception)
//								{
//									ret.setException(exception);
//								}
//							});
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							ret.setException(exception);
//						}
//					});
//				}
//				
//				public void exceptionOccurred(Exception exception)
//				{
//				}
//			});
//			
//			for(int i=0; i<tps.length; i++)
//			{
//				try
//				{
//					tps[i].start().addResultListener(lis);
//				}
//				catch(Exception e)
//				{
//					System.out.println("Could not initialize transport: "+tps[i]+" reason: "+e);
//					transports.remove(tps[i]);
//				}
//			}
//		}
//		
//		return ret;
//	}
	
	/**
	 *  Start the service.
	 */
	public IFuture<Void> startService()
	{
		final Future<Void> ret = new Future<Void>();

		super.startService().addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
				ITransport[] tps = (ITransport[])transports.toArray(new ITransport[transports.size()]);
				if(transports.size()==0)
				{
					ret.setException(new RuntimeException("MessageService has no working transport for sending messages."));
				}
				else
				{
					CollectionResultListener<Void> lis = new CollectionResultListener<Void>(tps.length, true,
						new ExceptionDelegationResultListener<Collection<Void>, Void>(ret)
					{
						public void customResultAvailable(Collection<Void> result)
						{
							if(result.isEmpty())
							{
								ret.setException(new RuntimeException("MessageService has no working transport for sending messages."));
							}
							else
							{
								SServiceProvider.getService(component.getServiceProvider(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
									.addResultListener(new ExceptionDelegationResultListener<IClockService, Void>(ret)
								{
									public void customResultAvailable(IClockService result)
									{
										clockservice = result;
										SServiceProvider.getService(component.getServiceProvider(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
											.addResultListener(new ExceptionDelegationResultListener<ILibraryService, Void>(ret)
										{
											public void customResultAvailable(ILibraryService result)
											{
												libservice = result;
												libservice.getClassLoader(component.getModel().getResourceIdentifier())
													.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
												{
													public void customResultAvailable(ClassLoader result)
													{
														classloader = result;
														startStreamSendAliveBehavior();
														startStreamCheckAliveBehavior();
														ret.setResult(null);
													}
												});
											}
										});
									}
								});
							}
						}
					});
					
					for(int i=0; i<tps.length; i++)
					{
						final ITransport	transport	= tps[i];
						IFuture<Void>	fut	= transport.start();
						fut.addResultListener(lis);
						fut.addResultListener(new IResultListener<Void>()
						{
							public void resultAvailable(Void result)
							{
							}
							
							public void exceptionOccurred(final Exception exception)
							{
								transports.remove(transport);
								component.scheduleStep(new IComponentStep<Void>()
								{
									public IFuture<Void> execute(IInternalAccess ia)
									{
										ia.getLogger().warning("Could not initialize transport: "+transport+" reason: "+exception);
										return IFuture.DONE;
									}
								});
							}
						});
					}
				}
			}
		});
		return ret;
	}
	
	/**
	 *  Called when the platform shuts down. Do necessary cleanup here (if any).
	 */
	public IFuture<Void> shutdownService()
	{
		Future<Void>	ret	= new Future<Void>();
//		ret.addResultListener(new IResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				System.err.println("MessageService shutdown end");
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				System.err.println("MessageService shutdown error");
//				exception.printStackTrace();
//			}
//		});
		SendManager[] tmp = (SendManager[])managers.values().toArray(new SendManager[managers.size()]);
		final SendManager[] sms = (SendManager[])SUtil.arrayToSet(tmp).toArray(new SendManager[0]);
//		System.err.println("MessageService shutdown start: "+(transports.size()+sms.length+1));
		final CounterResultListener<Void>	crl	= new CounterResultListener<Void>(transports.size()+sms.length+1, true, new DelegationResultListener<Void>(ret))
		{
//			public void intermediateResultAvailable(Object result)
//			{
//				System.err.println("MessageService shutdown intermediate result: "+result+", "+cnt);
//				super.intermediateResultAvailable(result);
//			}
//			public boolean intermediateExceptionOccurred(Exception exception)
//			{
//				System.err.println("MessageService shutdown intermediate error: "+exception+", "+cnt);
//				return super.intermediateExceptionOccurred(exception);
//			}
		};
		super.shutdownService().addResultListener(crl);

		SServiceProvider.getService(component.getServiceProvider(), IExecutionService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new ExceptionDelegationResultListener<IExecutionService, Void>(ret)
		{
			public void customResultAvailable(IExecutionService exe)
			{
				for(int i=0; i<sms.length; i++)
				{
//					System.err.println("MessageService executor cancel: "+sms[i]);
					exe.cancel(sms[i]).addResultListener(crl);
				}
				
				for(int i=0; i<transports.size(); i++)
				{
//					System.err.println("MessageService transport shutdown: "+transports.get(i));
					((ITransport)transports.get(i)).shutdown().addResultListener(crl);
				}
			}
		});
		
		return ret;
	}

	/**
	 *  Get the message type.
	 *  @param type The type name.
	 *  @return The message type.
	 */
	public MessageType getMessageType(String type)
	{
		return (MessageType)messagetypes.get(type);
	}
	
	/**
	 *  Add a message listener.
	 *  @param listener The change listener.
	 *  @param filter An optional filter to only receive notifications for matching messages. 
	 */
	public synchronized IFuture<Void> addMessageListener(IMessageListener listener, IFilter filter)
	{
		if(listeners==null)
			listeners = new LinkedHashMap();
		listeners.put(listener, filter);
		return new Future(null);
	}
	
	/**
	 *  Remove a message listener.
	 *  @param listener The change listener.
	 */
	public synchronized IFuture<Void> removeMessageListener(IMessageListener listener)
	{
		listeners.remove(listener);
		return new Future(null);
	}
	
	/**
	 *  Add content codec type.
	 *  @param codec The codec type.
	 */
	public IFuture<Void> addContentCodec(IContentCodec codec)
	{
		if(contentcodecs==null)
			contentcodecs = new ArrayList();
		contentcodecs.add(codec);
		return new Future(null);
	}
	
	/**
	 *  Remove content codec type.
	 *  @param codec The codec type.
	 */
	public IFuture<Void> removeContentCodec(IContentCodec codec)
	{
		if(contentcodecs!=null)
			contentcodecs.remove(codec);
		return new Future(null);
	}
	
	/**
	 *  Add message codec type.
	 *  @param codec The codec type.
	 */
	public IFuture<Void> addMessageCodec(Class codec)
	{
		codecfactory.addCodec(codec);
		return new Future(null);
	}
	
	/**
	 *  Remove message codec type.
	 *  @param codec The codec type.
	 */
	public IFuture<Void> removeMessageCodec(Class codec)
	{
		codecfactory.removeCodec(codec);
		return new Future(null);
	}
	
	/**
	 *  Update component identifier.
	 *  @param cid The component identifier.
	 *  @return The component identifier.
	 */
	public IComponentIdentifier internalUpdateComponentIdentifier(IComponentIdentifier cid)
	{
		ComponentIdentifier ret = null;
		if(cid.getPlatformName().equals(component.getComponentIdentifier().getRoot().getLocalName()))
		{
			ret = new ComponentIdentifier(cid.getName(), internalGetAddresses());
//			System.out.println("Rewritten cid: "+ret+" :"+SUtil.arrayToString(ret.getAddresses()));
		}
		return ret==null? cid: ret;
	}
	
	/**
	 *  Update component identifier.
	 *  @param cid The component identifier.
	 *  @return The component identifier.
	 */
	public IFuture<IComponentIdentifier> updateComponentIdentifier(IComponentIdentifier cid)
	{
		return new Future<IComponentIdentifier>(internalUpdateComponentIdentifier(cid));
	}
	
	//-------- internal methods --------
	
	/**
	 *  Get the component.
	 *  @return The component.
	 */
	public IExternalAccess getComponent()
	{
		return component;
	}
	
	/**
	 * 
	 */
	public void startStreamSendAliveBehavior()
	{
		component.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("sendAlive")
			public IFuture<Void> execute(IInternalAccess ia)
			{
//				System.out.println("sendAlive: "+pcons+" "+icons);
				AbstractConnectionHandler[] mypcons = (AbstractConnectionHandler[])pcons.values().toArray(new AbstractConnectionHandler[0]);
				for(int i=0; i<mypcons.length; i++)
				{
					if(!mypcons[i].isClosed())
					{
						mypcons[i].sendAlive();
					}
				}
				AbstractConnectionHandler[] myicons = (AbstractConnectionHandler[])icons.values().toArray(new AbstractConnectionHandler[0]);
				for(int i=0; i<myicons.length; i++)
				{
					if(!myicons[i].isClosed())
					{
						myicons[i].sendAlive();
					}
				}
				
				waitForRealDelay(StreamSendTask.MIN_LEASETIME, this);
				
				return IFuture.DONE;
			}
		});
	}
	
	/**
	 * 
	 */
	public void startStreamCheckAliveBehavior()
	{
		component.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("checkAlive")
			public IFuture<Void> execute(IInternalAccess ia)
			{
//				final IComponentStep<Void> step = this;
//				final Future<Void> ret = new Future<Void>();
				
				AbstractConnectionHandler[] mypcons = (AbstractConnectionHandler[])pcons.values().toArray(new AbstractConnectionHandler[0]);
				for(int i=0; i<mypcons.length; i++)
				{
					if(!mypcons[i].isConnectionAlive())
					{
//						System.out.println("removed con: "+mypcons[i]);
						mypcons[i].close();
						pcons.remove(new Integer(mypcons[i].getConnectionId()));
					}
				}
				AbstractConnectionHandler[] myicons = (AbstractConnectionHandler[])icons.values().toArray(new AbstractConnectionHandler[0]);
				for(int i=0; i<myicons.length; i++)
				{
					if(!myicons[i].isConnectionAlive())
					{
//						System.out.println("removed con: "+myicons[i]);
						myicons[i].close();
						icons.remove(new Integer(myicons[i].getConnectionId()));
					}
				}
				
				waitForRealDelay(StreamSendTask.MIN_LEASETIME, this);
				
				return IFuture.DONE;
			}
		});
	}
	

	/**
	 *  Deliver a message to the receivers.
	 */
	protected void internalDeliverMessage(Object obj)
	{
		MessageEnvelope	me	= null;
		try
		{
			ICommand handler;
			if(obj instanceof MessageEnvelope)
			{
				me	= (MessageEnvelope)obj;
				handler = deliveryhandlers.get(MapSendTask.MESSAGE_TYPE_MAP);
			}
			else
			{
				byte[]	rawmsg	= (byte[])obj;
				int	idx	= 0;
				byte rmt = rawmsg[idx++];
				handler = deliveryhandlers.get(rmt);
			}
			if(handler==null)
				throw new RuntimeException("Corrupt message, unknown delivery handler code.");
			handler.execute(obj);
		}
		catch(Exception e)
		{
			logger.warning("Message could not be delivered to receivers: "+(me!=null ? me.getReceivers() : "unknown") +", "+e);
		}
	}
	
	/**
	 *  Get the classloader for a resource identifier.
	 */
	protected IFuture<ClassLoader> getRIDClassLoader(Map msg, MessageType mt)
	{
		final Future<ClassLoader> ret = new Future<ClassLoader>();
		
//		MessageType mt = getMessageType(type);
		String ridid = mt.getResourceIdIdentifier();
		final IResourceIdentifier rid = (IResourceIdentifier)msg.get(ridid);
		if(rid!=null)
		{
			IFuture<ILibraryService> fut = SServiceProvider.getServiceUpwards(component.getServiceProvider(), ILibraryService.class);
			fut.addResultListener(new ExceptionDelegationResultListener<ILibraryService, ClassLoader>(ret)
			{
				public void customResultAvailable(ILibraryService ls)
				{
					ls.getClassLoader(rid).addResultListener(new DelegationResultListener<ClassLoader>(ret));
				}
				public void exceptionOccurred(Exception exception)
				{
					super.resultAvailable(null);
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Send message(s) executable.
	 */
	protected class SendManager implements IExecutable
	{
		//-------- attributes --------
		
		/** The list of messages to send. */
		protected List<AbstractSendTask> tasks;
		
		//-------- constructors --------
		
		/**
		 *  Send manager.
		 */
		public SendManager()
		{
			this.tasks = new ArrayList<AbstractSendTask>();
		}
		
		//-------- methods --------
	
		/**
		 *  Send a message.
		 */
		public boolean execute()
		{
			AbstractSendTask	tmp = null;
			boolean isempty;
			
			synchronized(this)
			{
				if(!tasks.isEmpty())
					tmp = tasks.remove(0);
				isempty = tasks.isEmpty();
			}
			final AbstractSendTask	task = tmp;
			
			if(task!=null)
			{
				// Todo: move back to send manager thread after isValid()
				// (hack!!! currently only works because message service is raw)
				// hack!!! doesn't make much sense to check isValid as send manager executes on different thread.
				isValid().addResultListener(new IResultListener<Boolean>()
				{
					public void resultAvailable(Boolean result)
					{
						if(result.booleanValue())
						{
							task.doSendMessage();
						}
						
						// Quit when service was terminated.
						else
						{
	//						System.out.println("send message not executed");
							task.getFuture().setException(new MessageFailureException(task.getMessage(), task.getMessageType(), null, "Message service terminated."));
	//						isempty	= true;
	//						messages.clear();
						}
					}
					
					public void exceptionOccurred(Exception exception)
					{
	//					System.out.println("send message not executed");
						task.getFuture().setException(new MessageFailureException(task.getMessage(), task.getMessageType(), null, "Message service terminated."));
	//					isempty	= true;
	//					messages.clear();
					}
				});
			}
			
			return !isempty;
		}
		
		/**
		 *  Add a message to be sent.
		 *  @param message The message.
		 */
		public IFuture<Void> addMessage(final AbstractSendTask task)
		{
			isValid().addResultListener(new ExceptionDelegationResultListener<Boolean, Void>(task.getFuture())
			{
				public void customResultAvailable(Boolean result)
				{
					if(result.booleanValue())
					{
						synchronized(SendManager.this)
						{
							tasks.add(task);
						}
						
						SServiceProvider.getService(component.getServiceProvider(), IExecutionService.class, 
							RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new ExceptionDelegationResultListener<IExecutionService, Void>(task.getFuture())
						{
							public void customResultAvailable(IExecutionService result)
							{
								result.execute(SendManager.this);
							}
						});
					}
					// Fail when service was shut down. 
					else
					{
//						System.out.println("message not added");
						task.getFuture().setException(new ServiceTerminatedException(getServiceIdentifier()));
					}
				}
			});
			
			return task.getFuture();
		}
	}
	
	/**
	 *  Deliver message(s) executable.
	 */
	protected class DeliverMessage implements IExecutable
	{
		//-------- attributes --------
		
		/** The list of messages to send. */
		protected List<Object> messages;
		
		//-------- constructors --------
		
		/**
		 *  Create a new deliver message executable.
		 */
		public DeliverMessage()
		{
			this.messages = new ArrayList<Object>();
		}
		
		//-------- methods --------
		
		/**
		 *  Deliver the message.
		 */
		public boolean execute()
		{
			Object tmp = null;
			boolean isempty;
			
			synchronized(this)
			{
				if(!messages.isEmpty())
					tmp = messages.remove(0);
				isempty = messages.isEmpty();
			}
			
			if(tmp!=null)
			{
				internalDeliverMessage(tmp);
			}
			
			return !isempty;
		}
		
		/**
		 *  Add a message to be delivered.
		 */
		public void addMessage(Object msg)
		{
			synchronized(this)
			{
				messages.add(msg);
			}
			
			SServiceProvider.getService(component.getServiceProvider(), IExecutionService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(new DefaultResultListener()
			{
				public void resultAvailable(Object result)
				{
					try
					{
						((IExecutionService)result).execute(DeliverMessage.this);
					}
					catch(RuntimeException e)
					{
						// ignore if execution service is shutting down.
					}
				}
			});
		}
	}
	
	int cnt;
	
	/**
	 *  Handle stream messages.
	 */
	class StreamDeliveryHandler implements ICommand
	{
		/**
		 *  Execute the command.
		 */
		public void execute(Object obj)
		{
			try
			{
				byte[] rawmsg = (byte[])obj;
				int mycnt = cnt++;
//				System.out.println("aaaa: "+mycnt+" "+getComponent().getComponentIdentifier());
//				System.out.println("Received binary: "+SUtil.arrayToString(rawmsg));
				int idx = 1;
				byte type = rawmsg[idx++];
				
				byte[] codec_ids = new byte[rawmsg[idx++]];
				byte[] bconid = new byte[4];
				for(int i=0; i<codec_ids.length; i++)
				{
					codec_ids[i] = rawmsg[idx++];
				}
				for(int i=0; i<4; i++)
				{
					bconid[i] = rawmsg[idx++];
				}
				final int conid = SUtil.bytesToInt(bconid);
				
				int seqnumber = -1;
				if(type==StreamSendTask.DATA_OUTPUT_INITIATOR || type==StreamSendTask.DATA_INPUT_PARTICIPANT)
				{
					for(int i=0; i<4; i++)
					{
						bconid[i] = rawmsg[idx++];
					}
					seqnumber = SUtil.bytesToInt(bconid);
	//				System.out.println("seqnr: "+seqnumber);
				}
				
				final Object data;
				if(codec_ids.length==0)
				{
					data = new byte[rawmsg.length-idx];
					System.arraycopy(rawmsg, idx, data, 0, rawmsg.length-idx);
				}
				else
				{
					Object tmp = new ByteArrayInputStream(rawmsg, idx, rawmsg.length-idx);
					for(int i=codec_ids.length-1; i>-1; i--)
					{
						ICodec dec = codecfactory.getCodec(codec_ids[i]);
						tmp = dec.decode(tmp, classloader);
					}
					data = tmp;
				}
	
				// Handle output connection participant side
				if(type==StreamSendTask.INIT_OUTPUT_INITIATOR)
				{
					IComponentIdentifier[] recs = (IComponentIdentifier[])data;
					InputConnectionHandler ich = new InputConnectionHandler(MessageService.this);
					final InputConnection con = new InputConnection(recs[0], recs[1], conid, false, ich);
					pcons.put(new Integer(conid), ich);
//					System.out.println("created for: "+conid+" "+pcons+" "+getComponent().getComponentIdentifier());
					
					ich.initReceived();
					
					final Future<Void> ret = new Future<Void>();
					SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
						.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
					{
						public void customResultAvailable(IComponentManagementService cms)
						{
							AbstractComponentAdapter component = (AbstractComponentAdapter)cms.getComponentAdapter(((IComponentIdentifier[])data)[1]);
							if(component != null)
							{
								component.receiveStream(con);
							}
						}
					});
				}
				else if(type==StreamSendTask.ACKINIT_OUTPUT_PARTICIPANT)
				{
//					System.out.println("CCC: ack init");
					OutputConnectionHandler och = (OutputConnectionHandler)icons.get(new Integer(conid));
					if(och!=null)
					{
						och.ackReceived(StreamSendTask.INIT, data);
					}
					else
					{
						System.out.println("OutputStream not found (ackinit): "+conid);
					}
				}
				else if(type==StreamSendTask.DATA_OUTPUT_INITIATOR)
				{
					InputConnectionHandler ich = (InputConnectionHandler)pcons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.addData(seqnumber, (byte[])data);
					}
					else
					{
						System.out.println("InputStream not found (dai): "+conid+" "+pcons+" "+getComponent().getComponentIdentifier());
					}
				}
				else if(type==StreamSendTask.CLOSE_OUTPUT_INITIATOR)
				{
//					System.out.println("CCC: close");
					InputConnectionHandler ich = (InputConnectionHandler)pcons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.closeReceived(SUtil.bytesToInt((byte[])data));
					}
					else
					{
						System.out.println("InputStream not found (coi): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKCLOSE_OUTPUT_PARTICIPANT)
				{
//					System.out.println("CCC: ackclose");
					OutputConnectionHandler och = (OutputConnectionHandler)icons.get(new Integer(conid));
					if(och!=null)
					{
						och.ackReceived(StreamSendTask.CLOSE, data);
					}
					else
					{
						System.out.println("OutputStream not found (ackclose): "+conid);
					}
				}
				else if(type==StreamSendTask.CLOSEREQ_OUTPUT_PARTICIPANT)
				{
//					System.out.println("CCC: closereq");
					OutputConnectionHandler och = (OutputConnectionHandler)icons.get(new Integer(conid));
					if(och!=null)
					{
						och.closeRequestReceived();
					}
					else
					{
						System.out.println("OutputStream not found (closereq): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKCLOSEREQ_OUTPUT_INITIATOR)
				{
//					System.out.println("CCC: ackclosereq");
					InputConnectionHandler ich = (InputConnectionHandler)pcons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.ackReceived(StreamSendTask.CLOSEREQ, data);
	//					ich.ackCloseRequestReceived();
					}
					else
					{
						System.out.println("OutputStream not found (ackclosereq): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKDATA_OUTPUT_PARTICIPANT)
				{
					// Handle input connection initiator side
					OutputConnectionHandler och = (OutputConnectionHandler)icons.get(new Integer(conid));
					if(och!=null)
					{
						AckInfo ackinfo = (AckInfo)data;
						och.ackDataReceived(ackinfo);
					}
					else
					{
						System.out.println("OutputStream not found (ackdata): "+conid);
					}
				}
				
				else if(type==StreamSendTask.INIT_INPUT_INITIATOR)
				{
					IComponentIdentifier[] recs = (IComponentIdentifier[])data;
					OutputConnectionHandler och = new OutputConnectionHandler(MessageService.this);
					final OutputConnection con = new OutputConnection(recs[0], recs[1], conid, false, och);
					pcons.put(new Integer(conid), och);
	//				System.out.println("created: "+con.hashCode());
					
					och.initReceived();
					
					final Future<Void> ret = new Future<Void>();
					SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
						.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
					{
						public void customResultAvailable(IComponentManagementService cms)
						{
							AbstractComponentAdapter component = (AbstractComponentAdapter)cms.getComponentAdapter(((IComponentIdentifier[])data)[1]);
							if(component != null)
							{
								component.receiveStream(con);
	//							pcons.put(new Integer(conid), con);
							}
						}
					});
				}
				else if(type==StreamSendTask.ACKINIT_INPUT_PARTICIPANT)
				{
					InputConnectionHandler ich = (InputConnectionHandler)icons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.ackReceived(StreamSendTask.INIT, data);
					}
					else
					{
						System.out.println("InputStream not found (ackinit): "+conid);
					}
				}
				else if(type==StreamSendTask.DATA_INPUT_PARTICIPANT)
				{
					InputConnectionHandler ich = (InputConnectionHandler)icons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.addData(seqnumber, (byte[])data);
					}
					else
					{
						System.out.println("InputStream not found (data input): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKDATA_INPUT_INITIATOR)
				{
					OutputConnectionHandler och = (OutputConnectionHandler)pcons.get(new Integer(conid));
					if(och!=null)
					{
						AckInfo ackinfo = (AckInfo)data;
						och.ackDataReceived(ackinfo);	
					}
					else
					{
						System.out.println("OutputStream not found (ackdata): "+conid);
					}
				}
				else if(type==StreamSendTask.CLOSEREQ_INPUT_INITIATOR)
				{
					OutputConnectionHandler och = (OutputConnectionHandler)pcons.get(new Integer(conid));
					if(och!=null)
					{
						och.closeRequestReceived();
					}
					else
					{
						System.out.println("InputStream not found (closereq): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKCLOSEREQ_INPUT_PARTICIPANT)
				{
					InputConnectionHandler ich = (InputConnectionHandler)icons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.ackReceived(StreamSendTask.CLOSEREQ, data);
					}
					else
					{
						System.out.println("InputStream not found (ackclosereq): "+conid);
					}
				}
				else if(type==StreamSendTask.CLOSE_INPUT_PARTICIPANT)
				{
					InputConnectionHandler ich = (InputConnectionHandler)icons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.closeReceived(SUtil.bytesToInt((byte[])data));
					}
					else
					{
						System.out.println("OutputStream not found (closeinput): "+conid);
					}
				}
				else if(type==StreamSendTask.ACKCLOSE_INPUT_INITIATOR)
				{
					OutputConnectionHandler ich = (OutputConnectionHandler)pcons.get(new Integer(conid));
					if(ich!=null)
					{
						ich.ackReceived(StreamSendTask.CLOSE, data);
					}
					else
					{
						System.out.println("InputStream not found (ackclose): "+conid);
					}
				}
				
				// Handle lease time update
				else if(type==StreamSendTask.ALIVE_INITIATOR)
				{
	//				System.out.println("alive initiator");
					AbstractConnectionHandler con = (AbstractConnectionHandler)pcons.get(new Integer(conid));
					if(con!=null)
					{
						con.setAliveTime(System.currentTimeMillis());
					}
					else
					{
						System.out.println("Stream not found (alive ini): "+conid);
					}
				}
				else if(type==StreamSendTask.ALIVE_PARTICIPANT)
				{
	//				System.out.println("alive particpant");
					AbstractConnectionHandler con = (AbstractConnectionHandler)icons.get(new Integer(conid));
					if(con!=null)
					{
						con.setAliveTime(System.currentTimeMillis());
					}
					else
					{
						System.out.println("Stream not found (alive par): "+conid);
					}
				}
	
//				System.out.println("bbbb: "+mycnt+" "+getComponent().getComponentIdentifier());
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 *  Handle map messages, i.e. normal text messages.
	 */
	class MapDeliveryHandler implements ICommand
	{
		/**
		 *  Execute the command.
		 */
		public void execute(Object obj)
		{
			MessageEnvelope me;
			if(obj instanceof MessageEnvelope)
			{
				me	= (MessageEnvelope)obj;
			}
			else
			{
				byte[]	rawmsg	= (byte[])obj;
				int	idx	= 0;
				byte rmt = rawmsg[idx++];
				byte[] codec_ids = new byte[rawmsg[idx++]];
				for(int i=0; i<codec_ids.length; i++)
				{
					codec_ids[i] = rawmsg[idx++];
				}
		
				Object tmp = new ByteArrayInputStream(rawmsg, idx, rawmsg.length-idx);
				for(int i=codec_ids.length-1; i>-1; i--)
				{
					ICodec dec = codecfactory.getCodec(codec_ids[i]);
					tmp = dec.decode(tmp, classloader);
				}
				me	= (MessageEnvelope)tmp;
			}
		
			final Map<String, Object> msg	= me.getMessage();
			String type	= me.getTypeName();
			final IComponentIdentifier[] receivers	= me.getReceivers();
//			System.out.println("Received message: "+SUtil.arrayToString(receivers));
			final MessageType	messagetype	= getMessageType(type);
			final Map	decoded	= new HashMap();	// Decoded messages cached by class loader to avoid decoding the same message more than once, when the same class loader is used.
			
			// Content decoding works as follows:
			// Find correct classloader for each receiver by
			// a) if message contains rid ask library service for classloader (global rids are resolved with maven, locals possibly with peer to peer jar transfer)
			// b) if library service could not resolve rid or message does not contain rid the receiver classloader can be used
			
			final Future<Void> ret = new Future<Void>();
			// todo: what to do with exception here?
//			ret.addResultListener(new IResultListener<Void>()
//			{
//				public void resultAvailable(Void result)
//				{
//				}
//				public void exceptionOccurred(Exception exception)
//				{
//					exception.printStackTrace();
//				}
//			});
			
			getRIDClassLoader(msg, getMessageType(type)).addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
			{
				public void customResultAvailable(final ClassLoader classloader)
				{
					SServiceProvider.getServiceUpwards(component.getServiceProvider(), IComponentManagementService.class)
						.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
					{
						public void customResultAvailable(IComponentManagementService cms)
						{
							for(int i = 0; i < receivers.length; i++)
							{
		//						final int cnt = i; 
								AbstractComponentAdapter component = (AbstractComponentAdapter)cms.getComponentAdapter(receivers[i]);
								if(component != null)
								{
									ClassLoader cl = classloader!=null? classloader: component.getComponentInstance().getClassLoader();
									Map	message	= (Map)decoded.get(cl);
									
									if(message==null)
									{
										if(receivers.length>1)
										{
											message	= new HashMap(msg);
											decoded.put(cl, message);
										}
										else
										{
											// Skip creation of copy when only one receiver.
											message	= msg;
										}
		
										// Conversion via platform specific codecs
										IContentCodec[] compcodecs = getContentCodecs(component.getModel(), cl);
										for(Iterator it=message.keySet().iterator(); it.hasNext(); )
										{
											String name = (String)it.next();
											Object value = message.get(name);
																				
											IContentCodec codec = messagetype.findContentCodec(compcodecs, message, name);
											if(codec==null)
												codec = messagetype.findContentCodec(getContentCodecs(), message, name);
											
											if(codec!=null)
											{
												try
												{
													Object val = codec.decode((byte[])value, cl);
													message.put(name, val);
												}
												catch(Exception e)
												{
													ContentException ce = new ContentException(new String((byte[])value), e);
													message.put(name, ce);
												}
											}
										}
									}
		
									try
									{
										component.receiveMessage(message, messagetype);
									}
									catch(Exception e)
									{
										logger.warning("Message could not be delivered to local receiver: " + receivers[i] + ", "+ message.get(messagetype.getIdIdentifier())+", "+e);
		
										// todo: notify sender that message could not be delivered!
										// Problem: there is no connection back to the sender, so that
										// the only chance is sending a separate failure message.
									}
								}
								else
								{
									logger.warning("Message could not be delivered to local receiver: " + receivers[i] + ", "+ msg.get(messagetype.getIdIdentifier()));
		
									// todo: notify sender that message could not be delivered!
									// Problem: there is no connection back to the sender, so that
									// the only chance is sending a separate failure message.
								}
							}
		
							IFilter[] fils;
							IMessageListener[] lis;
							synchronized(this)
							{
								fils = listeners==null? null: (IFilter[])listeners.values().toArray(new IFilter[listeners.size()]);
								lis = listeners==null? null: (IMessageListener[])listeners.keySet().toArray(new IMessageListener[listeners.size()]);
							}
							
							if(lis!=null)
							{
								// Hack?! Use message decoded for some component. What if listener has different class loader? 
								Map	message	= decoded.isEmpty() ? msg : (Map)decoded.get(decoded.keySet().iterator().next());
								IMessageAdapter msg = new DefaultMessageAdapter(message, messagetype);
								for(int i=0; i<lis.length; i++)
								{
									IMessageListener li = (IMessageListener)lis[i];
									boolean	match	= false;
									try
									{
										match	= fils[i]==null || fils[i].filter(msg);
									}
									catch(Exception e)
									{
										logger.warning("Filter threw exception: "+fils[i]+", "+e);
									}
									if(match)
									{
										try
										{
											li.messageReceived(msg);
										}
										catch(Exception e)
										{
											logger.warning("Listener threw exception: "+li+", "+e);
										}
									}
								}
							}
						}	
					});
				}
			});
		}
	}
	
	/** The (real) system clock timer. */
	protected Timer	timer;
	
	/**
	 *  Wait for a time delay on the (real) system clock.
	 */
	public TimerTask	waitForRealDelay(long delay, final IComponentStep<?> step)
	{
		if(timer==null)
		{
			synchronized(this)
			{
				if(timer==null)
				{
					timer	= new Timer(component.getComponentIdentifier().getName()+".message.timer", true);
				}
			}
		}
		
		TimerTask	ret	= new TimerTask()
		{
			public void run()
			{
				try
				{
					component.scheduleStep(step);
				}
				catch(ComponentTerminatedException cte)
				{
					// ignore and stop timer.
					timer.cancel();
				}
			}
		};
		timer.schedule(ret, delay);
		
		return ret;
	}
}


