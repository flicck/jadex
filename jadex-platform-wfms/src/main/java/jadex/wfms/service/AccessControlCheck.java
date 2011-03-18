package jadex.wfms.service;

import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.ICommand;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.wfms.client.IClient;

import java.security.AccessControlException;

public class AccessControlCheck
{
	private Integer[] actions;
	private IClient client;
	
	public AccessControlCheck(IClient client, Integer[] actions)
	{
		this.actions = actions;
		this.client = client;
	}
	
	public AccessControlCheck(IClient client, Integer action)
	{
		this(client, new Integer[] { action });
	}
	
	public void checkAccess(final Future targetFuture, IServiceContainer provider, final ICommand actionCommand)
	{
		SServiceProvider.getService(provider, IAAAService.class)
			.addResultListener(new DelegationResultListener(targetFuture)
		{
			public void customResultAvailable(Object result)
			{
				for (int i = 0; i < actions.length; ++i)
					if (!((IAAAService) result).accessAction(client, actions[i]))
					{
						targetFuture.setException(new AccessControlException("Not allowed: "+client + " " + actions[i]));
						return;
					}
				
				actionCommand.execute(actions);	
			}
		});
	}
}
