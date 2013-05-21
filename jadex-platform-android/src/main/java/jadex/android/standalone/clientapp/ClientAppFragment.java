package jadex.android.standalone.clientapp;

import jadex.android.standalone.clientservice.UniversalClientService.UniversalClientServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Window;

public class ClientAppFragment extends ActivityAdapterFragment
{
	private UniversalClientServiceBinder universalService;

	public ClientAppFragment()
	{
		super();
	}

	/**
	 * This method is called upon instantiation of the Fragment. Tasks that
	 * should be run before the layout of the Activity is set should be
	 * performed here.
	 * 
	 * Note that getActivity() will return null during this method.
	 * 
	 * @param mainActivity
	 */
	public void onPrepare(Activity mainActivity)
	{
		mainActivity.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}

	protected Context getContext()
	{
		return getActivity().getApplicationContext();
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags)
	{
		ComponentName originalComponent = service.getComponent();
		String clientServiceName = originalComponent.getClassName();
		if (clientServiceName.equals("jadex.android.service.JadexPlatformService"))
		{
			return super.bindService(service, conn, flags);
		}
		else
		{
			// individual user service requested
			return universalService.bindClientService(service, conn, flags);
		}
	}

	@Override
	public void unbindService(ServiceConnection conn)
	{
		if (universalService.isClientServiceConnection(conn))
		{
			boolean unbindClientService = universalService.unbindClientService(conn);
		}
		else
		{
			super.unbindService(conn);
		}
	}

	@Override
	public void startService(Intent service)
	{
		ComponentName originalComponent = service.getComponent();
		String clientServiceName = originalComponent.getClassName();
		if (clientServiceName.equals("jadex.android.service.JadexPlatformService"))
		{
			super.startService(service);
		}
		else
		{
			// individual user service requested
			universalService.startClientService(service);
		}
	}

	@Override
	public boolean stopService(Intent service)
	{
		if (universalService.isClientServiceStarted(service))
		{
			return universalService.stopClientService(service);
		}
		else
		{
			return super.stopService(service);
		}
	}

	public void setUniversalClientService(UniversalClientServiceBinder service)
	{
		this.universalService = service;
	}

}
