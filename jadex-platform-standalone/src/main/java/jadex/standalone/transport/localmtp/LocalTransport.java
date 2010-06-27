package jadex.standalone.transport.localmtp;

import jadex.base.fipa.ComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IMessageService;
import jadex.commons.collection.SCollection;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.service.IServiceContainer;
import jadex.standalone.transport.ITransport;

import java.util.List;
import java.util.Map;

/**
 *  The local transport for sending messages on the
 *  same platform (just calls the local deliver method).
 */
public class LocalTransport implements ITransport
{
	//-------- constants --------
	
	/** The schema name. */
	public final static String SCHEMA = "local-mtp://";
	
	//-------- attributes --------
	
	/** The message service. */
	protected IMessageService msgservice;
	
	/** The addresses. */
	protected String[] addresses;
	
	/** The platform name. */
//	protected String platformname;
	
	/** The platform. */
	protected IServiceContainer container;
	
	//-------- constructors --------
	
	/**
	 *  Init the transport.
	 *  @param platform The platform.
	 *  @param settings The settings.
	 */
	public LocalTransport(IServiceContainer container)
	{
		container.getService(IMessageService.class).addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				msgservice = (IMessageService)result;
			}
		});
		this.container = container;
		this.addresses = new String[0];
//		this.platformname = platform.getName();
	}

	/**
	 *  Start the transport.
	 */
	public void start()
	{
		// nothing to do.
	}
	
	/**
	 *  Perform cleanup operations (if any).
	 */
	public void shutdown()
	{
		// nothing to do.
	}
	
	//-------- methods --------
	
	/**
	 *  Send a message.
	 *  @param message The message to send.
	 *  @return The component identifiers of the components 
	 *  the message could not be sent to.
	 */
	public IComponentIdentifier[] sendMessage(Map message, String msgtype, IComponentIdentifier[] recs)
	{
		List todeliver = SCollection.createArrayList();
		List undelivered = SCollection.createArrayList();
		
//		IComponentIdentifier[] recs = message.getReceivers();
//		String hap = Configuration.getConfiguration().getProperty(Configuration.PLATFORMNAME);
		
		for(int i=0; i<recs.length; i++)
		{
			if(recs[i].getPlatformName().equals(container.getName()))
				todeliver.add(recs[i]);
			else
				undelivered.add(recs[i]);
		}
		if(todeliver.size()>0)
		{
			msgservice.deliverMessage(message, msgtype, (IComponentIdentifier[])todeliver
				.toArray(new IComponentIdentifier[todeliver.size()]));
		}
		
		return (ComponentIdentifier[])undelivered.toArray(new ComponentIdentifier[undelivered.size()]);
	}
	
	/**
	 *  Returns the prefix of this transport
	 *  @return Transport prefix.
	 */
	public String getServiceSchema()
	{
		return "local:";
	}
	
	/**
	 *  Get the adresses of this transport.
	 *  @return An array of strings representing the addresses 
	 *  of this message transport mechanism.
	 */
	public String[] getAddresses()
	{
		return addresses;
	}

}
