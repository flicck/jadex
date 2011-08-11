package jadex.simulation.analysis.application.commonsMath;

import jadex.base.gui.componentviewer.DefaultComponentServiceViewerPanel;
import jadex.bridge.service.annotation.GuiClass;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.NameValue;
import jadex.micro.annotation.Properties;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.simulation.analysis.common.defaultViews.controlComponent.ComponentServiceViewerPanel;
import jadex.simulation.analysis.service.continuative.computation.IAKonfidenzService;
import jadex.simulation.analysis.service.continuative.optimisation.IAOptimierungsService;
import jadex.simulation.analysis.service.dataBased.engineering.IADatenobjekteErstellenService;

/**
 *  Agent offering common math services
 */
@Description(" Agent offering common math services")
@ProvidedServices({@ProvidedService(type=IAKonfidenzService.class, implementation=@Implementation(expression="new CommonsMathKonfidenzService($component.getExternalAccess())")),
	@ProvidedService(type=IAOptimierungsService.class, implementation=@Implementation(expression="new CommonsMathOptimierungsService($component.getExternalAccess())"))})
@GuiClass(ComponentServiceViewerPanel.class)
@Properties(
{
	@NameValue(name="viewerpanel.componentviewerclass", value="\"jadex.simulation.analysis.common.defaultViews.controlComponent.ControlComponentViewerPanel\"")
})
public class CommonsMathAgent extends MicroAgent
{	

}
