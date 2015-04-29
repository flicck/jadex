package jadex.launch.test.servicecall;

import jadex.bridge.service.annotation.Timeout;
import jadex.commons.future.IFuture;

/**
 *  Service interface for service call benchmark.
 */
public interface IServiceCallService
{
	/**
	 *  Dummy method for service call benchmark.
	 */
	@Timeout(2015)
	public IFuture<Void>	call();
}
