package jadex.android.controlcenter;

import jadex.android.JadexAndroidContext;
import jadex.android.controlcenter.settings.AComponentSettings;
import jadex.android.controlcenter.settings.AServiceSettings;
import jadex.android.controlcenter.settings.ISettings;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.search.BasicResultSelector;
import jadex.bridge.service.search.ISearchManager;
import jadex.bridge.service.search.IVisitDecider;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.SReflect;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.micro.annotation.Binding;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A Simple Control Center for Jadex-Android. Provides Access to configurable
 * Components and Services. Because Android doesn't provide a way to set Option
 * Menus for Child PreferenceScreens, this Activity is instantiated once for
 * every child PreferenceScreen that is displayed. It then displays the child
 * PreferenceScreen and delegates calls to the child Settings Implementation.
 * 
 * (See
 * http://stackoverflow.com/questions/5032141/adding-menus-to-child-preference
 * -screens)
 * 
 * Can be instanciated by creating an Intent an passing the ComponentIdentifier
 * of the Platform to be configured as Extra with the Key: 
 * JadexAndroidControlCenter.EXTRA_PLATFORMID
 */
public class JadexAndroidControlCenter extends PreferenceActivity
{

	private static final String EXTRA_PLATFORMID = "platformId";
	private static final String EXTRA_SHOWCHILDPREFSCREEN = "showChildPrefScreen";
	private static final String EXTRA_SETTINGSKEY = "settingsKey";
//	private SharedPreferences sharedPreferences;
	private PreferenceCategory servicesCat;
	private PreferenceCategory componentsCat;
	private ISettings displayedChildSettings;
	
	/** The platformID to display preferences for */
	private IComponentIdentifier platformId;

	static private Map<String, ISettings> childSettings;

	static
	{
		childSettings = new HashMap<String, ISettings>();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Serializable platformId = getIntent().getSerializableExtra(EXTRA_PLATFORMID);
		if (platformId == null) {
			Log.d("jadex-android", "ControlCenter: No platformId passed, using a random started platform...");
			this.platformId = JadexAndroidContext.getInstance().getExternalPlatformAccess().getComponentIdentifier();
		} else {
			this.platformId = (IComponentIdentifier) platformId;
		}
		
		// Are we displaying Preferences for a child Prefscreen?
		if (getIntent().getBooleanExtra(EXTRA_SHOWCHILDPREFSCREEN, false))
		{
			String settingsKey = getIntent().getStringExtra(EXTRA_SETTINGSKEY);
			displayedChildSettings = childSettings.get(settingsKey);
			if (displayedChildSettings != null)
			{
				// display child preferences, enables us to control the options
				// menu
				PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
				setPreferenceScreen(root);
				displayedChildSettings.setPreferenceScreen(root);
				this.setTitle(settingsKey);
			} else
			{
				// display error
			}
		} else
		{
			setPreferenceScreen(createPreferenceHierarchy());
			this.setTitle("Control Center");
		}

		// allow long clicks on items
		getListView().setOnItemLongClickListener(new OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				ListView listView = (ListView) parent;
				ListAdapter listAdapter = listView.getAdapter();
				Object obj = listAdapter.getItem(position);
				if (obj != null && obj instanceof View.OnLongClickListener)
				{
					View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
					return longListener.onLongClick(view);
				}
				return false;
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		if (displayedChildSettings != null)
		{
			// child functionality
			return displayedChildSettings.onCreateOptionsMenu(menu);
		} else
		{
			// main functionality
			menu.add("Refresh");
			return true;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (displayedChildSettings != null)
		{
			// child functionality
			return displayedChildSettings.onOptionsItemSelected(item);
		} else
		{
			// main functionality
			refreshControlCenter();
			return true;
		}
	}

	/**
	 * Creates the root preference Hierarchy.
	 * 
	 * @return root {@link PreferenceScreen}.
	 */
	private PreferenceScreen createPreferenceHierarchy()
	{
		final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

		servicesCat = new PreferenceCategory(this);
		servicesCat.setTitle("Services");
		root.addPreference(servicesCat);

		componentsCat = new PreferenceCategory(this);
		componentsCat.setTitle("Components");
		root.addPreference(componentsCat);

		refreshControlCenter();
		return root;
	}

	/**
	 * Initiates a lookup for Services and Components which have an available
	 * GUIClass Annotation and list them.
	 */
	private void refreshControlCenter()
	{
		servicesCat.removeAll();
		componentsCat.removeAll();
		childSettings.clear();
		addDummyPrefs();

		if (JadexAndroidContext.getInstance().isPlatformRunning(platformId))
		{
			// get all viewable services
			IExternalAccess extAcc = JadexAndroidContext.getInstance().getExternalPlatformAccess(platformId);

			IServiceProvider sp = extAcc.getServiceProvider();
			ISearchManager manager = SServiceProvider.getSearchManager(true, Binding.SCOPE_PLATFORM);
			IVisitDecider decider = SServiceProvider.getVisitDecider(false, Binding.SCOPE_PLATFORM);

			BasicResultSelector selector = new BasicResultSelector(ViewableFilter.VIEWABLE_FILTER, false);
			IIntermediateFuture<IService> services = sp.getServices(manager, decider, selector);

			services.addResultListener(new IntermediateDefaultResultListener<IService>()
			{

				@Override
				public void resultAvailable(Collection<IService> result)
				{
					for (IService service : result)
					{
						intermediateResultAvailable(service);
					}
				}

				@Override
				public void intermediateResultAvailable(IService service)
				{
					if (addServiceSettings(servicesCat, service))
					{
						Preference dummyPref = servicesCat.findPreference("dummy");
						if (dummyPref != null)
							servicesCat.removePreference(dummyPref);
					}
				}

			});

			// get all viewable components
			SServiceProvider.getServiceUpwards(extAcc.getServiceProvider(), IComponentManagementService.class).addResultListener(
					new DefaultResultListener<IComponentManagementService>()
					{
						public void resultAvailable(final IComponentManagementService cms)
						{
							cms.getComponentIdentifiers().addResultListener(new DefaultResultListener<IComponentIdentifier[]>()
							{
								public void resultAvailable(IComponentIdentifier[] result)
								{
									for (IComponentIdentifier cid : result)
									{
										cms.getExternalAccess(cid).addResultListener(new DefaultResultListener<IExternalAccess>()
										{
											public void resultAvailable(final IExternalAccess acc)
											{
												Object clid = acc.getModel().getProperty(ViewableFilter.COMPONENTVIEWER_VIEWERCLASS,
														getClassLoader());

												final Class<?> clazz = getGuiClass(clid);

												if (clazz != null)
												{
													runOnUiThread(new Runnable()
													{
														public void run()
														{
															if (addComponentSettings(componentsCat, acc, clazz))
															{
																Preference dummyPref = componentsCat.findPreference("dummy");
																if (dummyPref != null)
																	componentsCat.removePreference(dummyPref);
															}
														}
													});
												}
											}
										});
									}
								}
							});
						}
					});
		}
	}

	/**
	 * Adds dummy preferences to show that no services/components are found.
	 */
	private void addDummyPrefs()
	{
		servicesCat.removeAll();
		componentsCat.removeAll();
		final Preference dummyServicePref = new Preference(this);
		dummyServicePref.setTitle("No viewable Services.");
		dummyServicePref.setKey("dummy");
		dummyServicePref.setEnabled(false);
		servicesCat.addPreference(dummyServicePref);
		final Preference dummyComponentPref = new Preference(this);
		dummyComponentPref.setTitle("No viewable Components.");
		dummyComponentPref.setKey("dummy");
		dummyComponentPref.setEnabled(false);
		componentsCat.addPreference(dummyComponentPref);
	}

	protected boolean addServiceSettings(PreferenceGroup root, IService service)
	{
		final Object clid = service.getPropertyMap() != null
				? service.getPropertyMap().get(ViewableFilter.COMPONENTVIEWER_VIEWERCLASS)
				: null;
		Class<?> guiClass = getGuiClass(clid);
		if (guiClass != null)
		{
			try
			{
				AServiceSettings settings = (AServiceSettings) guiClass.getConstructor(IService.class).newInstance(service);
				addSettings(root, settings);
				return true;
			} catch (InstantiationException e)
			{
				e.printStackTrace();
			} catch (IllegalAccessException e)
			{
				e.printStackTrace();
			} catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			} catch (SecurityException e)
			{
				e.printStackTrace();
			} catch (InvocationTargetException e)
			{
				e.printStackTrace();
			} catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}

	protected boolean addComponentSettings(PreferenceGroup root, IExternalAccess component, Class<?> guiClass)
	{
		try
		{
			AComponentSettings settings = (AComponentSettings) guiClass.getConstructor(IExternalAccess.class).newInstance(component);
			addSettings(root, settings);
			return true;
		} catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (SecurityException e)
		{
			e.printStackTrace();
		} catch (InstantiationException e)
		{
			e.printStackTrace();
		} catch (IllegalAccessException e)
		{
			e.printStackTrace();
		} catch (InvocationTargetException e)
		{
			e.printStackTrace();
		} catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	protected void addSettings(PreferenceGroup root, ISettings settings)
	{
		settings.setPlatformId(this.platformId);
		PreferenceScreen screen = this.getPreferenceManager().createPreferenceScreen(this);
		root.addPreference(screen);
		Intent i = new Intent(this, JadexAndroidControlCenter.class);
		i.putExtra(EXTRA_PLATFORMID, (Serializable) platformId);
		i.putExtra(EXTRA_SHOWCHILDPREFSCREEN, true);
		i.putExtra(EXTRA_SETTINGSKEY, settings.getTitle());
		childSettings.put(settings.getTitle(), settings);
		screen.setIntent(i);
		screen.setKey(settings.getTitle());
		screen.setTitle(settings.getTitle());
		// settings.setPreferenceScreen(screen);
	}

	/**
	 * Returns the class with the given Class name or the first available class
	 * from a Class name Array.
	 * 
	 * @param clid
	 *            Name of the class or Array of names of classes.
	 * @return The first found Class or <code>null</code>.
	 */
	private Class<?> getGuiClass(final Object clid)
	{
		Class<?> guiClass = null;
		if (clid instanceof String)
		{
			Class<?> clazz = SReflect.classForName0((String) clid, getClassLoader());
			if (clazz != null)
			{
				guiClass = clazz;
			}
		} else if (clid instanceof String[])
		{
			for (String className : (String[]) clid)
			{
				Class<?> clazz = SReflect.classForName0(className, getClassLoader());
				if (clazz != null)
				{
					guiClass = clazz;
					break;
				}
			}
		}
		return guiClass;
	}
}
