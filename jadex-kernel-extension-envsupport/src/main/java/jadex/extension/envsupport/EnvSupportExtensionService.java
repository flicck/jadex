package jadex.extension.envsupport;

import jadex.bridge.IComponentFactoryExtensionService;
import jadex.bridge.service.annotation.Service;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 *  Extension service for loading env support models.
 */
@Service
public class EnvSupportExtensionService implements IComponentFactoryExtensionService
{
	/**
	 *  Get extension. 
	 */
	public IFuture getExtension(String componenttype)
	{
		return new Future(MEnvSpaceType.getXMLMapping());
	}
}
