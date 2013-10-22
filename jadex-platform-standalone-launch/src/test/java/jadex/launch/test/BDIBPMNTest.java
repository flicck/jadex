package jadex.launch.test;

import jadex.base.test.ComponentTestSuite;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 *  Test suite for BDI BPMN agent tests.
 */
public class BDIBPMNTest	extends ComponentTestSuite
{
	/**
	 *  Constructor called by Maven JUnit runner.
	 */
	public BDIBPMNTest()	throws Exception
	{
		// Use bdibpmn application classes directory as classpath root,
		super(new File("../jadex-applications-bdibpmn/target/classes/"),
			new File("../jadex-applications-bdibpmn/target/classes"),
			// Exclude failing tests to allow maven build.
			new String[]
			{
				".bpmn",	// Only execute agents.
				"Carry",
				"Producer",
				"Sentry"
			});
	}
	
	/**
	 *  Static method called by eclipse JUnit runner.
	 */
	public static Test suite() throws Exception
	{
		return new BDIBPMNTest();
	}
	
//	@Override
//	public void run(TestResult result)
//	{
//		// TODO Auto-generated method stub
//		super.run(result);
//		
//		try
//		{
//			Thread.sleep(300000);
//		}
//		catch(InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
