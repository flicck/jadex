
package jadex.extension.envsupport.observer.graphics.jmonkey.appstate;

import jadex.extension.envsupport.observer.graphics.drawable3d.special.NiftyScreen;

import java.util.ArrayList;

import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;

import de.lessvoid.nifty.screen.ScreenController;

public interface ICustomStateCreator
{

	ScreenController getScreenController();
	
	ArrayList<AbstractAppState> getCustomAppStates();
	
	ArrayList<NiftyScreen> getNiftyScreens();
}
