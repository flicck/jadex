package jadex.bdi.examples.hunterprey2.environment;

import jadex.bdi.planlib.simsupport.common.math.IVector1;
import jadex.bdi.planlib.simsupport.common.math.IVector2;
import jadex.bdi.planlib.simsupport.common.math.Vector1Double;
import jadex.bdi.planlib.simsupport.common.math.Vector1Int;
import jadex.bdi.planlib.simsupport.common.math.Vector1Long;
import jadex.bdi.planlib.simsupport.common.math.Vector2Double;
import jadex.bdi.planlib.simsupport.common.math.Vector2Int;

/** Environment configuration.
 */
public class Configuration
{
	public static final String ENVIRONMENT_NAME = "HunterPrey2";
	
	public static final IVector2 AREA_SIZE = new Vector2Int(30);
	
	public static final String BACKGROUND_TILE = "jadex/bdi/examples/hunterprey2/images/background.png";
	public static final IVector2 BACKGROUND_TILE_SIZE = new Vector2Int(30);
	
	/** Maximum distance a cleaner can reach.
	 */
	public static final IVector1 REACH_DISTANCE = new Vector1Int(0);
	
	
}
