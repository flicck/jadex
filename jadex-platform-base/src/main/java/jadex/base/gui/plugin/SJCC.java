package jadex.base.gui.plugin;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IResultListener;
import jadex.commons.future.SwingDefaultResultListener;
import jadex.xml.annotation.XMLClassname;

import java.awt.Component;

/**
 *  Static helper methods for JCC plugins.
 */
public class SJCC
{
	public static void	killPlattform(final IExternalAccess exta, final Component ui)
	{
		exta.scheduleStep(new IComponentStep()
		{
			@XMLClassname("kill-platform")
			public Object execute(IInternalAccess ia)
			{
				ia.getRequiredService("cms").addResultListener(new SwingDefaultResultListener(ui)
				{
					public void customResultAvailable(Object result)
					{
						final IComponentManagementService cms = (IComponentManagementService)result;
						Future ret = new Future();
						ret.addResultListener(new SwingDefaultResultListener(ui)
						{
							public void customResultAvailable(Object result)
							{
								final IComponentIdentifier root = (IComponentIdentifier)result;
								cms.resumeComponent(root).addResultListener(new SwingDefaultResultListener(ui)
								{
									public void customResultAvailable(Object result)
									{
										cms.destroyComponent(root);
									}
								});
							}
						});
						getRootIdentifier(exta.getComponentIdentifier(), cms, ret);
					}
				});
				return null;
			}
		});
	}
	
	/**
	 *  Internal method to get the root identifier.
	 */
	protected static void getRootIdentifier(final IComponentIdentifier cid, final IComponentManagementService cms, final Future future)
	{
		cms.getParent(cid).addResultListener(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				if(result==null)
				{
					future.setResult(cid);
				}
				else
				{
					getRootIdentifier((IComponentIdentifier)result, cms, future);
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				future.setException(exception);
			}
		});
	}

}
