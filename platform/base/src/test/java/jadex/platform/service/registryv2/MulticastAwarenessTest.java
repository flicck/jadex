package jadex.platform.service.registryv2;


import jadex.base.IPlatformConfiguration;
import jadex.base.Starter;
import jadex.base.test.util.STest;
import jadex.commons.security.SSecurity;

/**
 *  Test basic search functionality with multicast awareness only
 *  (i.e. no super peer).
 */
public class MulticastAwarenessTest	extends AbstractSearchQueryTest
{
	//-------- constants --------
	
	/** Client configuration for platform used for searching. */
	public static final IPlatformConfiguration	CLIENTCONF;

	/** Plain provider configuration. */
	public static final IPlatformConfiguration	PROCONF;
	
	/** Fixed custom port for multicast. */
	public static final int customport	= SSecurity.getSecureRandom().nextInt(Short.MAX_VALUE*2-1024)+1025;

	static
	{
		IPlatformConfiguration	baseconf	= STest.getDefaultTestConfig();
		baseconf.setValue("superpeerclient.awaonly", true);
		baseconf.setValue("superpeerclient.pollingrate", WAITFACTOR/2); 	// -> 750 millis.
		baseconf.setValue("passiveawarenessintravm", false);
		baseconf.setValue("passiveawarenessmulticast", true);
		baseconf.setValue("passiveawarenessmulticast.port", customport);
//		baseconf.setDefaultTimeout(Starter.getScaledDefaultTimeout(null, WAITFACTOR*2));
		baseconf.getExtendedPlatformConfiguration().setDebugFutures(true);

		// Remote only -> no simulation please
		baseconf.getExtendedPlatformConfiguration().setSimul(false);
		baseconf.getExtendedPlatformConfiguration().setSimulation(false);
		
		CLIENTCONF	= baseconf.clone();
		CLIENTCONF.setPlatformName("client_*");
		
		PROCONF	= baseconf.clone();
		PROCONF.addComponent(GlobalProviderAgent.class);
		PROCONF.addComponent(LocalProviderAgent.class);
		PROCONF.setPlatformName("provider_*");
	}
	
	//-------- constructors --------
	
	/**
	 *  Create the test.
	 */
	public MulticastAwarenessTest()
	{
		super(true, CLIENTCONF, PROCONF, null, null);
	}
}
