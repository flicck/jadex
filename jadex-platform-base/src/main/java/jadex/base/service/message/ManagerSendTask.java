package jadex.base.service.message;

import jadex.base.service.message.transport.ITransport;
import jadex.base.service.message.transport.MessageEnvelope;
import jadex.base.service.message.transport.codecs.ICodec;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.IResultCommand;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  The manager send task is responsible for coordinating
 *  the sending of a message to a single destination using
 *  multiple available transports.
 */
public class ManagerSendTask	implements ISendTask
{
	//-------- attributes --------
	
	/** The message. */
	protected Map<String, Object> message;
	
	/** The encoded message envelope. */
	protected byte[] data;

	/** The message prolog. */
	protected byte[] prolog;
	
	/** The message type. */
	protected MessageType messagetype;
	
	/** The codecids. */
	protected byte[] codecids;
	
	/** The codecs. */
	protected ICodec[] codecs;
	
	/** The managed receivers. */
	protected IComponentIdentifier[] receivers;

	/** The transports to be tried. */
	protected List<ITransport> transports;
	
	/** The future for the sending result. */
	protected Future<Void>	future;
	
	/** Is some transport interested in the task? */
	protected int	interest;
	
	/** True, if the token is acquired. */
	protected boolean	acquired;
	
	/** The list of waiting transports. */
	protected List<IResultCommand<IFuture<Void>, Void>>	waiting;
	

	//-------- constructors --------- 

	/**
	 *  Create a new manager send task.
	 */
	public ManagerSendTask(Map<String, Object> message, MessageType messagetype, IComponentIdentifier[] receivers, 
		ITransport[] transports, byte[] codecids, ICodec[] codecs)//, SendManager manager)
	{
		if(codecids==null || codecids.length==0)
			throw new IllegalArgumentException("Codec ids must not null.");
		if(codecs==null || codecs.length==0)
			throw new IllegalArgumentException("Codecs must not null.");
		
		for(int i=0; i<receivers.length; i++)
		{
			if(receivers[i].getAddresses()==null)
				throw new IllegalArgumentException("Addresses must not null");
		}
		
		this.message = message;
		this.messagetype = messagetype;
		this.receivers = receivers;
		this.transports = new ArrayList<ITransport>(Arrays.asList(transports));
		this.codecs = codecs;
		this.codecids = codecids;
		this.future	= new Future<Void>();
	}
	
	//-------- methods used by message service --------
	
	/**
	 *  Get the message.
	 *  @return the message.
	 */
	public Map<String, Object> getMessage()
	{
		return message;
	}

	/**
	 *  Get the messagetype.
	 *  @return the messagetype.
	 */
	public MessageType getMessageType()
	{
		return messagetype;
	}

	/**
	 *  Get the receivers.
	 *  @return the receivers.
	 */
	public IComponentIdentifier[] getReceivers()
	{
		return receivers;
	}
	
	/**
	 *  Get the transports.
	 *  @return the transports.
	 */
	public List<ITransport> getTransports()
	{
		return transports;
	}
	
	/**
	 *  Get the future.
	 */
	public Future<Void>	getFuture()
	{
		return future;
	}
	
	/**
	 *  Transport availability for sending the message.
	 *  Transports announce their interest, such that the message service knows when no transport is available for a send task.
	 */
	public boolean	getInterest()
	{
		return this.interest>0;
	}
	
	/**
	 *  Announce availability of a transport for sending the message.
	 *  Transports should announce their interest, such that the message service knows when no transport is available for a send task.
	 */
	public void	addInterest()
	{
		this.interest++;
	}


	//--------- methods used by transports ---------
	
	/**
	 *  Called by the transport when is is ready to send the message,
	 *  i.e. when a connection is established.
	 *  @param send	The code to be executed to send the message.
	 */
	public void ready(IResultCommand<IFuture<Void>, Void> send)
	{
		boolean	dosend;
		synchronized(this)
		{
			dosend	= !acquired && !future.isDone();
			acquired	= true;
			if(!dosend && !future.isDone())
			{
				if(waiting==null)
				{
					waiting	= new LinkedList<IResultCommand<IFuture<Void>, Void>>();
				}
				waiting.add(send);
			}
		}
		if(dosend)
		{
			try
			{
				send.execute(null).addResultListener(new IResultListener<Void>()
				{
					public void resultAvailable(Void result)
					{
						done(null);
					}
					
					public void exceptionOccurred(Exception exception)
					{
						done(exception);
					}
				});
			}
			catch(Exception e)
			{
				done(e);
			}
		}
	}
	
	/**
	 *  The message sending is done. 
	 *  @param e	The exception (if any). Null denotes successful sending.
	 */
	protected void done(Exception e)
	{
		if(e!=null)
		{
			IResultCommand<IFuture<Void>, Void>	next	= null;
			boolean	nointerest;
			synchronized(this)
			{
				interest--;
				nointerest	= interest==0;
				acquired	= false;
				if(waiting!=null && !waiting.isEmpty())
				{
					next	= waiting.remove(0);
				}
			}
			if(next!=null)
			{
				ready(next);
			}
			else if(nointerest)
			{
				future.setException(e);
			}
		}
		else
		{
			future.setResult(null);
		}
	}

	/**
	 *  Get the encoded message.
	 *  Saves the message to avoid multiple encoding with different transports.
	 */
	public byte[] getData()
	{
		if(data==null)
		{
			synchronized(this)
			{
				if(data==null)
				{
					MessageEnvelope	envelope = new MessageEnvelope(message, Arrays.asList(receivers),  messagetype.getName());
					
					Object enc_msg = envelope;
					for(int i=0; i<codecs.length; i++)
					{
						enc_msg	= codecs[i].encode(enc_msg, getClass().getClassLoader());
					}
					data = (byte[])enc_msg;
				}
			}
		}
		return data;
	}
	
	/**
	 *  Get the prolog bytes.
	 *  Separated from data to avoid array copies.
	 *  Message service expects messages to be delivered in the form {prolog}{data}. 
	 *  @return The prolog bytes.
	 */
	public byte[] getProlog()
	{
		if(prolog==null)
		{
			synchronized(this)
			{
				if(prolog==null)
				{
					prolog = new byte[1+codecids.length];
					prolog[0] = (byte)codecids.length;
					System.arraycopy(codecids, 0, prolog, 1, codecids.length);
				}
			}
		}
		return prolog;
	}
}
