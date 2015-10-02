package jadex.launch.test;
import java.io.File;

import jadex.base.test.ComponentTestSuite;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import junit.framework.Test;


/**
 *  Test suite for BDI tests.
 */
public class BDIV3Test	extends	ComponentTestSuite
{
	private static final String NOEXCLUDE = "__noexclude__";
	
	/**
	 *  Constructor called by Maven JUnit runner.
	 */
	public BDIV3Test()	throws Exception {
		this(SUtil.findBuildDir(new File("../jadex-applications-bdiv3")));
	}

	/**
	 *  Constructor called by JadexInstrumentor for Android tests.
	 */
	public BDIV3Test(File cproot)	throws Exception
	{
		// Use BDI classes directory as classpath root,
		super(SReflect.isAndroid() ? new File("jadex.bdiv3.testcases") : cproot,
			cproot,
			// Exclude failing tests to allow maven build.
			new String[]
			{
				"INegotiationAgent",	// Not an agent.
				"QuickstartBDI",	// blocks due to opened dialog
				"Ambulance",	// sub agent
				"Commander",	// sub agent
				"FireBrigade",	// sub agent
				"ComponentPlanAgent",	// sub agent
				"Carry",	// sub agent
				"Producer",	// sub agent
				"Sentry",	// sub agent
				"BurnerBDI",	// sub agent
				"CollectorBDI",	// sub agent
				"3d",	// OpenGL problems on termination?
				"CreationBDI",	// should only be run as separate benchmark,
				"WorkpieceBDI",
				SReflect.isAndroid() ? "GuiBDI" : NOEXCLUDE
			});
	}

	/**
	 *  Static method called by eclipse JUnit runner.
	 */
	public static Test suite() throws Exception
	{
		return new BDIV3Test();
	}

//	public void run(TestResult result)
//	{
//		super.run(result);
//		
//		try
//		{
//			Thread.sleep(3000000);
//		}
//		catch(InterruptedException e)
//		{
//		}
//	}
}
