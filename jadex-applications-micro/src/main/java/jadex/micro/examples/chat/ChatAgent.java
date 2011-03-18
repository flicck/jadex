package jadex.micro.examples.chat;

import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.GuiClass;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

/**
 *  Chat micro agent. 
 */
@Description("This agent offers a chat service.")
@ProvidedServices(@ProvidedService(type=IChatService.class, expression="new ChatService($component)"))
@RequiredServices({
	@RequiredService(name="chatservices", type=IChatService.class, 
	multiple=true, scope=RequiredServiceInfo.SCOPE_GLOBAL),
	@RequiredService(name="mychatservice", type=IChatService.class, scope=RequiredServiceInfo.SCOPE_LOCAL)
})
@GuiClass(ChatViewerPanel.class)
public class ChatAgent extends MicroAgent
{
}
