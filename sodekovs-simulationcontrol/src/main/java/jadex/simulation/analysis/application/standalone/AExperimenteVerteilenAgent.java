package jadex.simulation.analysis.application.standalone;

import jadex.bridge.service.annotation.GuiClass;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.NameValue;
import jadex.micro.annotation.Properties;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.simulation.analysis.common.defaultViews.controlComponent.ComponentServiceViewerPanel;
import jadex.simulation.analysis.service.dataBased.engineering.IADatenobjekteErstellenService;
import jadex.simulation.analysis.service.simulation.allocation.IAExperimenteVerteilenService;

@Description("Agent bietet eine IAExperimenteVerteilenService an")
@ProvidedServices({@ProvidedService(type=IAExperimenteVerteilenService.class, implementation=@Implementation(expression="new jadex.simulation.analysis.application.standalone.AExperimenteVerteilenService($component.getExternalAccess())"))})
@GuiClass(ComponentServiceViewerPanel.class)
@Properties(
{
	@NameValue(name="viewerpanel.componentviewerclass", value="\"jadex.simulation.analysis.common.defaultViews.controlComponent.ControlComponentViewerPanel\"")
})
public class AExperimenteVerteilenAgent extends MicroAgent
{	

}
