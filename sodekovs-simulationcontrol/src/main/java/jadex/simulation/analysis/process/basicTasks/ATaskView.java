package jadex.simulation.analysis.process.basicTasks;

import jadex.commons.future.ISuspendable;
import jadex.commons.future.ThreadSuspendable;
import jadex.simulation.analysis.common.events.task.ATaskEvent;
import jadex.simulation.analysis.common.util.AConstants;

import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ATaskView implements IATaskView
{
	protected JComponent component;
	protected TaskProperties properties;
	protected ISuspendable susThread = new ThreadSuspendable(this);
	protected JInternalFrame parent;

	protected IATask displayedTask;
	protected Object mutex = new Object();

	public ATaskView(final IATask taskObject)
	{
		displayedTask = taskObject;
		taskObject.addTaskListener(this);
		component = new JPanel(new GridBagLayout());
//		component.add(new JLabel("Kein View verf�gbar!"));
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				properties = new TaskProperties();
				if (taskObject.getActivity() != null)
				{
					properties.getTextField("Activit�tsname").setText(taskObject.getActivity().getName());
					properties.getTextField("Activit�tsklasse").setText(taskObject.getActivity().getClazz().getName().toString());

					properties.revalidate();
					properties.repaint();
				}
				
			}
		});

	}

	@Override
	public JComponent getComponent()
	{
		return component;
	}

	@Override
	public TaskProperties getTaskProperties()
	{
		return properties;
	}

	@Override
	public void taskEventOccur(final ATaskEvent event)
	{
	
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (event.getCommand().equals(AConstants.TASK_L�UFT))
				{
					properties.getTextField("Status").setText(AConstants.TASK_L�UFT);
				} else if (event.getCommand().equals(AConstants.TASK_BEENDET))
				{
					properties.getTextField("Status").setText(AConstants.TASK_BEENDET);
				}
				properties.revalidate();
				properties.repaint();
			}
		});
	}

	@Override
	public IATask getDisplayedObject()
	{
		return displayedTask;
	}

	@Override
	public Object getMutex()
	{
		return mutex;
	}

	@Override
	public void setParent(JInternalFrame frame)
	{
		parent = frame;		
	}
}
