package jadex.android.standalone.clientapp;

import jadex.android.IEventReceiver;
import jadex.android.commons.JadexPlatformOptions;
import jadex.android.commons.Logger;
import jadex.android.exception.JadexAndroidPlatformNotStartedError;
import jadex.android.service.JadexPlatformManager;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.platform.IJadexPlatformBinder;
import jadex.commons.SReflect;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * This is an Android Fragment Class which provides needed Functionality and
 * comfort Features for Fragments using the Jadex platform.
 * 
 * Subclasses of this class can be used together with the jadex platformapp.
 * 
 * @author Julian Kalinowski
 */
public class PlatformProvidingClientAppFragment extends ClientAppFragment implements ServiceConnection, JadexPlatformOptions
{

	private Intent serviceIntent;
	private IJadexPlatformBinder platformService;
	protected IComponentIdentifier platformId;

	private boolean platformAutostart;
	private String[] platformKernels;
	private String platformOptions;
	private String platformName;
	
	/**
	 * Constructor
	 */
	public PlatformProvidingClientAppFragment()
	{
		super();
		platformAutostart = false;
		platformKernels = JadexPlatformOptions.DEFAULT_KERNELS;
		platformOptions = "";
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		serviceIntent = new Intent(getActivity(), SReflect.classForName0("jadex.android.service.JadexPlatformService", this.getClass().getClassLoader()));
		bindService(serviceIntent, this, Service.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unbindService(this);
	}

	/**
	 * Sets the autostart parameter for this jadex platform. If true, the
	 * platform will be started during onCreate.
	 * 
	 * @param autostart
	 */
	protected void setPlatformAutostart(boolean autostart)
	{
		if (!isPlatformRunning())
		{
			this.platformAutostart = autostart;
		}
		else
		{
			throw new IllegalStateException("Cannot set autostart, platform already running!");
		}
	}

	/**
	 * Sets the Kernels. See {@link JadexPlatformManager} Constants for
	 * available Kernels.
	 * 
	 * @param kernels
	 */
	protected void setPlatformKernels(String... kernels)
	{
		this.platformKernels = kernels;
	}

	/**
	 * Sets platform options.
	 * 
	 * @param options
	 */
	protected void setPlatformOptions(String options)
	{
		this.platformOptions = options;
	}
	
	/**
	 * Sets the name of the platform that is started by this activity.
	 * 
	 * @param name
	 */
	protected void setPlatformName(String name)
	{
		this.platformName = name;
	}

	protected boolean isPlatformRunning()
	{
		if (platformService != null)
		{
			return platformService.isPlatformRunning(platformId);
		}
		else
		{
			return false;
		}
	}
	
	protected IExternalAccess getPlatformAccess()
	{
		checkIfJadexIsRunning("getPlatformAccess()");
		return platformService.getExternalPlatformAccess(platformId);
	}
	
	/**
	 * Gets the platform service.
	 * @return PlatformService binder
	 */
	protected IJadexPlatformBinder getPlatformService() {
		return platformService;
	}

	protected boolean isPlatformRunning(IComponentIdentifier platformId)
	{
		if (platformService != null)
		{
			return platformService.isPlatformRunning(platformId);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Starts a Component.
	 * 
	 * @param name
	 *            Name of the Component created
	 * @param modelPath
	 *            Path to the Component
	 * @return IFuture<IComponentIdentifier>
	 */
	protected IFuture<IComponentIdentifier> startComponent(final String name, final String modelPath)
	{
		checkIfJadexIsRunning("startComponent()");
		return platformService.startComponent(name, modelPath);
	}
	
	protected void registerEventReceiver(IEventReceiver<?> rec)
	{
		checkIfJadexIsRunning("registerEventReceiver");
		platformService.registerEventReceiver(rec);
	}

	protected void unregisterEventReceiver(IEventReceiver<?> rec)
	{
		checkIfJadexIsRunning("unregisterEventReceiver");
		platformService.unregisterEventReceiver(rec);
	}
	
	private void checkIfJadexIsRunning(String caller)
	{
		if (!platformService.isPlatformRunning(platformId))
		{
			throw new JadexAndroidPlatformNotStartedError(caller);
		}
	}

	public void onServiceConnected(ComponentName name, IBinder service)
	{
		platformService = (IJadexPlatformBinder) service;
		if (platformAutostart)
		{
			startPlatform();
		}
	}

	public void onServiceDisconnected(ComponentName name)
	{
		platformService = null;
	}
	
	/**
	 * Called right before the platform startup.
	 */
	protected void onPlatformStarting()
	{
		setProgressBarIndeterminateVisibility(true);
	}

	/**
	 * Called right after the platform is started.
	 * 
	 * @param result
	 *            The external access to the platform
	 */
	protected void onPlatformStarted(IExternalAccess result)
	{
		this.platformId = result.getComponentIdentifier();
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				setProgressBarIndeterminateVisibility(false);
			}
		});
	}

	/**
	 * Starts the Jadex Platform. To set Parameters, use setPlatformKernels(),
	 * setPlatformOptions() or setPlatformName() before calling this Method.
	 * 
	 * Will be automatically called when setPlatformAutostart(true) was called
	 * in the Constructor.
	 * 
	 * The Lifecycle methods onPlatformStarting() and onPlatformStarted() will
	 * be executed during Startup.
	 */
	final protected void startPlatform()
	{
//		platformService.setPlatformClassLoader(getClass().getClassLoader());
		onPlatformStarting();
		IFuture<IExternalAccess> platform = platformService.startJadexPlatform(platformKernels, platformName, platformOptions);

		platform.addResultListener(new DefaultResultListener<IExternalAccess>()
		{

			@Override
			public void resultAvailable(IExternalAccess result)
			{
				onPlatformStarted(result);
			}
			
			@Override
			public void exceptionOccurred(Exception exception)
			{
				Logger.e(exception);
			}

		});
		
	}

	/**
	 * Stops all running jadex platforms.
	 */
	protected void stopPlatforms()
	{
		checkIfJadexIsRunning("stopPlatforms()");
		platformService.shutdownJadexPlatforms();
	}
	
	/**
	 * Terminates a given Jadex platform.
	 * 
	 * @param platformID
	 *            Identifier of the platform to terminate
	 */
	public void shutdownJadexPlatform(IComponentIdentifier platformID)
	{
		checkIfJadexIsRunning("shutdownJadexPlatform()");
		platformService.shutdownJadexPlatform(platformID);
	}

}
