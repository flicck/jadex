package jadex.bpmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class MLane extends MNamedIdElement implements IAssociationTarget
{
	//-------- attributes --------
	
	/** The association description. */
	protected String associationsdescription;
	
	/** The activities description. */
	protected String activitiesdescription;
	
	
	/** The activities. */
	protected List activities;
	
	/** The type. */
	protected String type;
	
	/** The associations. */
	protected List associations;
	
	//-------- methods --------
	
	/**
	 * @return the associationsdescription
	 */
	public String getAssociationsDescription()
	{
		return this.associationsdescription;
	}

	/**
	 * @param associationsdescription the associationsdescription to set
	 */
	public void setAssociationsDescription(String associationsdescription)
	{
		this.associationsdescription = associationsdescription;
	}
	
	/**
	 * @return the activitiesdescription
	 */
	public String getActivitiesDescription()
	{
		return this.activitiesdescription;
	}

	/**
	 * @param activitiesdescription the activitiesdescription to set
	 */
	public void setActivitiesDescription(String activitiesdescription)
	{
		this.activitiesdescription = activitiesdescription;
	}
	
	/**
	 * 
	 */
	public List getActivities()
	{
		return activities;
	}
	
	/**
	 * 
	 */
	public void addActivity(MActivity activity)
	{
		if(activities==null)
			activities = new ArrayList();
		activities.add(activity);
	}
	
	/**
	 * 
	 */
	public void removeActivity(MActivity activity)
	{
		if(activities!=null)
			activities.remove(activity);
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return this.type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}
	
	/**
	 * 
	 */
	public List getAssociations()
	{
		return associations;
	}

	/**
	 * 
	 */
	public void addAssociation(MAssociation association)
	{
		if(associations==null)
			associations = new ArrayList();
		associations.add(association);
	}
	
	/**
	 * 
	 */
	public void removeAssociation(MAssociation association)
	{
		if(associations!=null)
			associations.remove(association);
	}
}
