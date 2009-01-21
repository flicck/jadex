package jadex.bdi.examples.hunterprey2.environment;

import jadex.bdi.examples.hunterprey2.Configuration;
import jadex.bdi.examples.hunterprey2.Creature;
import jadex.bdi.examples.hunterprey2.Food;
import jadex.bdi.examples.hunterprey2.Location;
import jadex.bdi.examples.hunterprey2.Obstacle;
import jadex.bdi.examples.hunterprey2.WorldObject;
import jadex.bdi.planlib.simsupport.common.graphics.drawable.DrawableCombiner;
import jadex.bdi.planlib.simsupport.common.graphics.drawable.Rectangle;
import jadex.bdi.planlib.simsupport.common.graphics.drawable.TexturedRectangle;
import jadex.bdi.planlib.simsupport.common.graphics.layer.GridLayer;
import jadex.bdi.planlib.simsupport.common.graphics.layer.ILayer;
import jadex.bdi.planlib.simsupport.common.graphics.layer.TiledLayer;
import jadex.bdi.planlib.simsupport.common.math.IVector1;
import jadex.bdi.planlib.simsupport.common.math.Vector2Double;
import jadex.bdi.planlib.simsupport.observer.capability.plugin.IObserverCenterPlugin;
import jadex.bdi.planlib.starter.StartAgentInfo;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;

import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitializeEnvironmentPlan extends Plan
{
	public void body()
	{
		startSimEngine();
		initializeEnvironment();
		insertPlugin();
		createGUI();
			
		// start the sim ticker
		getBeliefbase().getBelief("tick").setFact(new Boolean(true));

	}
	
	
	protected void createGUI()
	{
		boolean gui_show_map = ((Boolean) getBeliefbase().getBelief("gui_show_map").getFact()).booleanValue();
		
		Canvas worldmap = null;
		if (initializeObserver() && ((Boolean) getBeliefbase().getBelief("custom_gui").getFact()).booleanValue())
		{
			worldmap = (Canvas) getBeliefbase().getBelief("worldmap").getFact();
		}

		EnvironmentGui gui;
		try {
			gui = new EnvironmentGui(getExternalAccess(), worldmap, gui_show_map);
//			gui = new EnvironmentGui(getExternalAccess(), null, gui_show_map);
			getBeliefbase().getBelief("gui").setFact(gui);
		} catch (Exception e) {
			fail(e);
		}
		
	}


	/** 
	 * start the sim-engine agent 
	 */
	protected boolean startSimEngine() {
		
		// start the sim-engine agent
		StartAgentInfo simEnvironmentAgentInfo = (StartAgentInfo) getBeliefbase().getBelief("simagent_info").getFact();
		IGoal sg = createGoal("start_agents");
		sg.getParameterSet("agentinfos").addValue(simEnvironmentAgentInfo);
		dispatchSubgoalAndWait(sg);
		Object simAID = null;
		if (sg.isSucceeded())
		{
			try
			{
				simAID = sg.getParameterSet("agentidentifiers").getValues()[0];
			}
			catch (Exception e) {
				fail(e);
			}
		}
		getBeliefbase().getBelief("simagent").setFact(simAID);
		
		// connect the sim-engine
		//String envName = (String) getBeliefbase().getBelief("environment_name").getFact();
		String envName = Configuration.ENVIRONMENT_NAME;
		IGoal connGoal = createGoal("sim_connect_environment");
		connGoal.getParameter("environment_name").setValue(envName);
		dispatchSubgoalAndWait(connGoal);
		
		return connGoal.isSucceeded();
	}
	
	/** 
	 * initialize the environment with obstacles and food 
	 */
	protected void initializeEnvironment()
	{
		Environment env  = new Environment(Configuration.ENVIRONMENT_NAME,
				  Configuration.AREA_SIZE,
				  this.getExternalAccess());

		getBeliefbase().getBelief("environment").setFact(env);
		
		// create obstacles
		int obstacleCount = ((Integer) getBeliefbase().getBelief("obstacle_count").getFact()).intValue();
		for (int i = 0; i < obstacleCount; ++i)
		{
			Location l = env.getEmptyLocation(WorldObject.WORLD_OBJECT_SIZE);
			//Location l = engine.getEmptyLocation(new Vector2Int(0));
			Obstacle obstacle = new Obstacle(l);
			env.addObstacle(obstacle);
		}
		
		// create initial food
		int foodCount = ((Integer) getBeliefbase().getBelief("initial_food").getFact()).intValue();
		for (int i = 0; i < foodCount; ++i)
		{
			Location l = env.getEmptyLocation(WorldObject.WORLD_OBJECT_SIZE);
			//Location l = engine.getEmptyLocation(new Vector2Int(0));
			Food food = new Food(l);
			env.addFood(food);
		}
		
		// Update spawn rate
		env.setFoodrate(((Integer) getBeliefbase().getBelief("food_spawn_rate").getFact()).intValue());

//		// Processes - IGNORE -its not step based!
//		int maxFood = ((Integer) getBeliefbase().getBelief("max_food").getFact()).intValue();
//		if (maxFood <= 0)
//		{
//			maxFood = 1;
//		}
//		IVector1 foodSpawnRate = (IVector1) getBeliefbase().getBelief("food_spawn_rate").getFact();
//		if ((foodSpawnRate.less(Vector1Int.ZERO)) ||
//			(foodSpawnRate.equals(Vector1Int.ZERO)))
//		{
//			foodSpawnRate = new Vector1Int(1);
//		}
//		env.addEnvironmentProcess(new FoodSpawnProcess(maxFood, foodSpawnRate));
		
	}
	
	protected void insertPlugin()
	{
		IObserverCenterPlugin plugin = new EnvironmentObserverPlugin(getExternalAccess());
		getBeliefbase().getBeliefSet("custom_plugins").addFact(plugin);
	}
	
	protected boolean initializeObserver()
	{
		
		getBeliefbase().getBelief("environment_name_obs").setFact(Configuration.ENVIRONMENT_NAME);
		
		Map theme = new HashMap();
		String imgPath = this.getClass().getPackage().getName().replaceAll("environment", "").concat("images.").replaceAll("\\.", "/");

		DrawableCombiner hunterCombiner = new DrawableCombiner();
		String hunterImage = imgPath.concat("hunter.png");
		for (int i=Creature.CREATURE_VISUAL_RANGE.getAsInteger()-1; i > 0 ; i--)
		{
			float alpha = (float) (0.1f + (0.5 * 1 / Creature.CREATURE_VISUAL_RANGE.getAsInteger()));
			hunterCombiner.addDrawable(new Rectangle(new Vector2Double((Creature.CREATURE_VISUAL_RANGE.getAsDouble()-i) * 2.0), false, new Color(1.0f, 1.0f, 0.0f, alpha)), -2);
		}
		hunterCombiner.addDrawable(new TexturedRectangle(Creature.CREATURE_SIZE, false, hunterImage), 0);
		theme.put(Environment.OBJECT_TYPE_HUNTER, hunterCombiner);

		
		DrawableCombiner preyCombiner = new DrawableCombiner();
		String preyImage = imgPath.concat("prey.png");
		for (int i=Creature.CREATURE_VISUAL_RANGE.getAsInteger()-1; i > 0 ; i--)
		{
			float alpha = (float) (0.1f + (0.5 * 1 / Creature.CREATURE_VISUAL_RANGE.getAsInteger()));
			preyCombiner.addDrawable(new Rectangle(new Vector2Double((Creature.CREATURE_VISUAL_RANGE.getAsDouble()-i) * 2.0), false, new Color(1.0f, 1.0f, 0.0f, alpha)), -2);
		}
		preyCombiner.addDrawable(new TexturedRectangle(Creature.CREATURE_SIZE, false, preyImage), 0);
		theme.put(Environment.OBJECT_TYPE_PREY, preyCombiner);
		
		DrawableCombiner foodCombiner = new DrawableCombiner();
		String foodImage = imgPath.concat("food.png");
		foodCombiner.addDrawable(new TexturedRectangle(WorldObject.WORLD_OBJECT_SIZE.copy().multiply(0.7), false, foodImage), -1);
		theme.put(Environment.OBJECT_TYPE_FOOD, foodCombiner);
		
		DrawableCombiner obstacleCombiner = new DrawableCombiner();
		String obstacleImage = imgPath.concat("obstacle.png");
		obstacleCombiner.addDrawable(new TexturedRectangle(WorldObject.WORLD_OBJECT_SIZE.copy().multiply(0.9), false, obstacleImage), 1);
		theme.put(Environment.OBJECT_TYPE_OBSTACLE, obstacleCombiner);
		
		
		Map themes = (Map) getBeliefbase().getBelief("object_themes").getFact();
		themes.put("default",theme);
		
		ILayer background =  new TiledLayer(Configuration.BACKGROUND_TILE_SIZE,
										    Configuration.BACKGROUND_TILE);
		
		List preLayerTheme = new ArrayList();
		preLayerTheme.add(background);
		preLayerTheme.add(new GridLayer(new Vector2Double(1.0), Color.BLACK));
		Map preLayerThemes = (Map) getBeliefbase().getBelief("prelayer_themes").getFact();
		preLayerThemes.put("default", preLayerTheme);
		
		IGoal start = createGoal("simobs_start");
		dispatchSubgoalAndWait(start);
		
		return start.isSucceeded();
	}
	
	
	
}
