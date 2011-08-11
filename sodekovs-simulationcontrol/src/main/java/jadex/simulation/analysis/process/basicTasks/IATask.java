package jadex.simulation.analysis.process.basicTasks;

import java.util.UUID;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.runtime.ITask;
import jadex.simulation.analysis.common.events.task.ATaskEvent;
import jadex.simulation.analysis.common.events.task.IATaskListener;

public interface IATask extends ITask
{
	public Object getMutex();
	
	public UUID getID();
	
	public MActivity getActivity();

	/**
	 * Adds a Listener, who observe the state of the task
	 * @param listener the {@link IATaskListener} to add
	 */
	public void addTaskListener(IATaskListener listener);
	
	/**
	 * Removes a Listener, who observe the state of the task
	 * @param listener the {@link IATaskListener} to remove
	 */
	public void removeTaskListener(IATaskListener listener);
	
	/**
	 * Indicates a event in the task
	 * @param event of the change
	 */
	public void taskChanged(ATaskEvent e);

	void setTaskNumber(Integer taskNumber);

	Integer getTaskNumber();

	void userInteractionRequired(Boolean user);
}
