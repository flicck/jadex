package jadex.bdi.planlib.protocols.subscribe;

import jadex.base.fipa.SFipa;
import jadex.bdi.runtime.IMessageEvent;
import jadex.bdi.runtime.Plan;

public class SPInitiationPlan extends Plan
{
	public void body()
	{
		IMessageEvent subReq = createMessageEvent("sp_subscribe");
		subReq.getParameter(SFipa.CONTENT).setValue(getParameter("subscription").getValue());

		subReq.getParameterSet("receivers").addValue(getParameter("receiver").getValue());
		if(getParameter("language").getValue()!=null)
			subReq.getParameter("language").setValue(getParameter("language").getValue());
		if(getParameter("ontology").getValue()!=null)
			subReq.getParameter("ontology").setValue(getParameter("ontology").getValue());
		
		getParameter("subscription_id").setValue(subReq.getParameter(SFipa.CONVERSATION_ID));
		
		getWaitqueue().addReply(subReq);
		IMessageEvent reply = sendMessageAndWait(subReq);
		if (!SFipa.AGREE.equals(reply.getParameter(SFipa.PERFORMATIVE).getValue()))
			fail();
	}
}
