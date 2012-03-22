package jadex.micro.testcases.multiinvoke;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Component;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  Agent that uses a multi service.
 */
@RequiredServices(@RequiredService(name="ms", type=IExampleService.class, multiple=true, binding=@Binding(dynamic=true)))
@Results(@Result(name="testresults", clazz=Testcase.class))
@Agent
@ComponentTypes(@ComponentType(name="provider", filename="ProviderAgent.class"))
@Configurations(@Configuration(name="def", components=@Component(type="provider", number="5")))
public class UserAgent
{
	@Agent
	protected MicroAgent agent;
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();
		
		IMultiplexExampleService ser = getMultiService("ms", IMultiplexExampleService.class);
		final int cmpcnt = 5;
		final int rescnt = 5;
		final List<TestReport> reports = new ArrayList<TestReport>();
		
		CounterResultListener<Void> endlis = new CounterResultListener<Void>(8, new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				agent.setResultValue("testresults", new Testcase(8, reports.toArray(new TestReport[reports.size()])));
				ret.setResult(null);
			}

			public void exceptionOccurred(Exception exception)
			{
				resultAvailable(null);
			}
		});
		
		// indirect intermediate future version
		
		TestReport tr = new TestReport("#1a", "Test indirect intermediate future version.");
		reports.add(tr);
		ser.getItem1().addResultListener(new CustomIntermediateResultListener<IFuture<String>>(tr, cmpcnt, endlis));
		tr = new TestReport("#1b", "Test indirect intermediate future version.");
		reports.add(tr);
		ser.getItems1(rescnt).addResultListener(new CustomIntermediateResultListener<IIntermediateFuture<String>>(tr, cmpcnt, endlis));
			
		// indirect future version
		
		tr = new TestReport("#2a", "Test indirect future version.");
		reports.add(tr);
		ser.getItem2().addResultListener(new CustomResultListener<Collection<IFuture<String>>>(tr, cmpcnt, endlis));
		tr = new TestReport("#2b", "Test indirect future version.");
		reports.add(tr);
		ser.getItems2(rescnt).addResultListener(new CustomResultListener<Collection<IIntermediateFuture<String>>>(tr, cmpcnt, endlis));

		// flattened intermediate future version
		
		tr = new TestReport("#3a", "Test flattened intermediate future version.");
		reports.add(tr);
		ser.getItem3().addResultListener(new CustomIntermediateResultListener<String>(tr, cmpcnt, endlis));
		tr = new TestReport("#3b", "Test flattened intermediate future version.");
		reports.add(tr);
		ser.getItems3(rescnt).addResultListener(new CustomIntermediateResultListener<String>(tr, cmpcnt*rescnt, endlis));

		// flattened future version

		tr = new TestReport("#4a", "Test flattened future version.");
		reports.add(tr);
		ser.getItem4().addResultListener(new CustomResultListener<Collection<String>>(tr, cmpcnt, endlis));
		tr = new TestReport("#4b", "Test flattened future version.");
		reports.add(tr);
		ser.getItems4(rescnt).addResultListener(new CustomResultListener<Collection<String>>(tr, cmpcnt*rescnt, endlis));
		
		return ret;
	}
	
	/**
	 *  Custom intermediate listener.
	 */
	public class CustomIntermediateResultListener<T> implements IIntermediateResultListener<T>
	{
		protected int cnt = 0;
		protected int rescnt;
		protected TestReport tr;
		protected IResultListener<Void> endlis;
		
		public CustomIntermediateResultListener(TestReport tr, int rescnt, IResultListener<Void> endlis)
		{
			this.tr = tr;
			this.rescnt = rescnt;
			this.endlis = endlis;
		}
		
		public void intermediateResultAvailable(T result)
		{
			cnt++;
		}
		
		public void finished()
		{
			if(cnt==rescnt)
				tr.setSucceeded(true);
			else
				tr.setReason("Wrong number of results: "+cnt);
			endlis.resultAvailable(null);
		}
		
		public void resultAvailable(Collection<T> result)
		{
			if(result.size()==rescnt)
				tr.setSucceeded(true);
			else
				tr.setReason("Wrong number of results: "+result.size());
			endlis.resultAvailable(null);
		}
		
		public void exceptionOccurred(Exception exception)
		{
			tr.setReason("Exception: "+exception);
			endlis.resultAvailable(null);
		}
	}
	
	/**
	 *  Custom listener.
	 */
	public class CustomResultListener<T> implements IResultListener<T>
	{
		protected int cnt = 0;
		protected int rescnt;
		protected TestReport tr;
		protected IResultListener<Void> endlis;
		
		public CustomResultListener(TestReport tr, int rescnt, IResultListener<Void> endlis)
		{
			this.tr = tr;
			this.rescnt = rescnt;
			this.endlis = endlis;
		}
		
		public void resultAvailable(T result)
		{
			if(result instanceof Collection && ((Collection<?>)result).size()==rescnt)
				tr.setSucceeded(true);
			else
				tr.setReason("Wrong number of results: "+result);
			endlis.resultAvailable(null);
		}
		
		public void exceptionOccurred(Exception exception)
		{
			tr.setReason("Exception: "+exception);
			endlis.resultAvailable(null);
		}
	}

	/**
	 *  Get a multi service.
	 *  @param reqname The required service name.
	 *  @param multitype The interface of the multi service.
	 */
	public <T> T getMultiService(String reqname, Class<T> multitype)
	{
		return (T)Proxy.newProxyInstance(agent.getClassLoader(), new Class[]{multitype}, new MultiServiceInvocationHandler(agent, reqname, multitype));
	}
	
}