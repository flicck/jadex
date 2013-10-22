package jadex.base.test.impl;


import jadex.bridge.IExternalAccess;
import jadex.commons.future.ThreadSuspendable;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 *  This test kills the platform.
 *  Used as last test in the component test suite for cleanup.
 */
public class Cleanup extends TestCase
{
	//-------- attributes --------
	
	/** The platform access. */
	protected IExternalAccess	platform;
	
	/** The timer that stops test suite execution after timeout (needs to be cancelled on cleanup). */
	protected	TimerTask	timer;
	
	//-------- constructors --------
	
	/**
	 *  Create a component test.
	 */
	public Cleanup(IExternalAccess platform, TimerTask timer)
	{
		super("Cleanup");
		this.platform	= platform;
		this.timer	= timer;
	}
	
	//-------- methods --------
	
	/**
	 *  The number of test cases.
	 */
	public int countTestCases()
	{
		return 1;
	}
	
	/**
	 *  Test the component.
	 */
	public void run(TestResult result)
	{
		timer.cancel();
		
		result.startTest(this);

		try
		{
			platform.killComponent().get(new ThreadSuspendable(), 30000);
		}
		catch(Exception e)
		{
			result.addError(this, e);
		}
		
//		try
//		{
//			Thread.sleep(300000);
//		}
//		catch(InterruptedException e)
//		{
//		}
		
		result.endTest(this);
		
		// Remove references to Jadex resources to aid GC cleanup.
		platform	= null;
		timer	= null;
		
		// Java Bug not releasing the last focused window, see:
		// http://www.lucamasini.net/Home/java-in-general-/the-weakness-of-swing-s-memory-model
		// http://bugs.sun.com/view_bug.do?bug_id=4726458
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
//				KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
				
				final JFrame f	= new JFrame("dummy");
				f.getContentPane().add(new JButton("Dummy"), BorderLayout.CENTER);
				f.setSize(100, 100);
				f.setVisible(true);
				
				Timer	t	= new Timer(500, new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						f.dispose();
					}
				});
				t.setRepeats(false);
				t.start();
			}
		});
		
//		// Another bug not releasing the last drawn window.
//		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6857676
//		
//		try
//		{
//			Class<?> clazz	= Class.forName("sun.java2d.pipe.BufferedContext");
//			Field	field	= clazz.getDeclaredField("currentContext");
//			field.setAccessible(true);
//			field.set(null, null);
//		}
//		catch(Throwable e)
//		{
//			e.printStackTrace();
//		}

	}

	public String getName()
	{
		return this.toString();
	}
	
	/**
	 *  Get a string representation of this test.
	 */
	public String toString()
	{
		return "Cleanup";
	}
}
