package jadex.base.service.message.transport.httprelaymtp.nio;

import jadex.commons.Tuple2;

import java.nio.channels.SelectionKey;

/**
 *  Handler interface for managing NIO operations.
 */
public interface IHttpRequest
{
	/**
	 *  Get the host/port pair to connect to.
	 */
	public Tuple2<String, Integer>	getAddress();
	
	/**
	 *  Let the request know that it is running on a (potentially closed) idle connection.
	 *  The request might want to reschedule, e.g. only if an error occured on an idle connection.
	 */
	public void	setIdle(boolean idle);
	
	/**
	 *  Reschedule the request in case of connection inactivity?
	 */
	public boolean	reschedule();
	
	/**
	 *  Called before read/write operations.
	 *  Also called after the request has been rescheduled in case of errors.
	 */
	public void	initRequest();
	
	/**
	 *  Write the HTTP request to the NIO connection.
	 *  May be called multiple times, if not all data can be send at once.
	 *  Has to change the interest to OP_READ, once all data is sent.
	 *  
	 *  @return	In case of errors may request to be rescheduled on a new connection:
	 *    -1 no reschedule, 0 immediate reschedule, >0 reschedule after delay (millis.)
	 */
	public int handleWrite(SelectionKey key);
	
	/**
	 *  Receive the HTTP response from the NIO connection.
	 *  May be called multiple times, if not all data can be send at once.
	 *  Has to deregister interest in the connection, once required data is received.
	 *  May close the connection or leave it open for reuse if the server supports keep-alive.
	 *  
	 *  @return	In case of errors may request to be rescheduled on a new connection:
	 *    -1 no reschedule, 0 immediate reschedule, >0 reschedule after delay (millis.)
	 */
	public int	handleRead(SelectionKey key);
}
