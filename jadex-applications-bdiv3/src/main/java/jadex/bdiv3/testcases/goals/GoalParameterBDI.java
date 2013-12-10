package jadex.bdiv3.testcases.goals;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalParameter;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

/**
 *  Test if changes of goal parameters can be detected in goal conditions.
 */
@Agent
@Results(@Result(name="testresults", clazz=Testcase.class))
public class GoalParameterBDI
{
	/** The bdi agent. */
	@Agent
	protected BDIAgent agent;
		
	/**
	 * 
	 */
	@Goal(excludemode=Goal.ExcludeMode.WhenFailed)
	public class TestGoal
	{
		@GoalParameter
		protected int cnt;
		
//		@GoalParameter
//		protected List<String> elems = new ArrayList<String>();

		
		@GoalTargetCondition(parameters="cnt")
//		@GoalTargetCondition(parameters="elems")
//		@GoalTargetCondition(rawevents=ChangeEvent.PARAMETERCHANGED+".jadex.bdiv3.testcases.goals.GoalParameterBDI$TestGoal.cnt")
//		@GoalTargetCondition(rawevents=ChangeEvent.PARAMETERCHANGED+".*")
		public boolean checkTarget()
		{
			return cnt==2;
//			return elems.size()==10;
		}
		
		public void inc()
		{
			cnt++;
//			elems.add("a");
//			System.out.println("Increased: "+cnt);
		}
	}
	
	@Plan(trigger=@Trigger(goals=TestGoal.class))
	public void inc(TestGoal g, IPlan plan)
	{
		g.inc();
//		plan.waitFor(200).get();
		agent.waitForDelay(200).get();
//		System.out.println("plan end");
	}
	
	@AgentBody
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();

		final TestReport tr = new TestReport("#1", "Test if a goal condition can be triggered by a goal parameter.");
		
		agent.waitForDelay(2000, new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if(!tr.isFinished())
				{
					tr.setFailed("Goal did return");
					agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
				}
				
				ret.setResultIfUndone(null);
				return IFuture.DONE;
			}
		});
		
		agent.dispatchTopLevelGoal(new TestGoal()).get();
		tr.setSucceeded(true);
		agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
		ret.setResultIfUndone(null);
		
		return ret;
	}
	
	// Tests if agent.waitFor() can be used and plan abort works
//	@AgentBody
//	public void body()
//	{
//		TestGoal g = new TestGoal();
//		IFuture<TestGoal> fut = agent.dispatchTopLevelGoal(g);
//		agent.waitForDelay(100).get();
//		g.inc(); // trigger condition during plan wait
//		fut.get();
//		System.out.println("fini");
//	}
}