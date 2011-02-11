package jadex.tools.deployer;

import jadex.base.gui.asynctree.INodeHandler;
import jadex.base.gui.componentviewer.IServiceViewerPanel;
import jadex.base.gui.filetree.DefaultFileFilter;
import jadex.base.gui.filetree.DefaultFileFilterMenuItemConstructor;
import jadex.base.gui.filetree.DefaultNodeHandler;
import jadex.base.gui.filetree.FileTreePanel;
import jadex.base.gui.plugin.IControlCenter;
import jadex.base.service.deployment.IDeploymentService;
import jadex.bridge.IExternalAccess;
import jadex.commons.Properties;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.gui.PopupBuilder;
import jadex.commons.service.IService;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeSelectionModel;

/**
 * 
 */
public class DeploymentServiceViewerPanel implements IServiceViewerPanel
{
	/** The file tree panel. */
	protected FileTreePanel ftp;
	
	/** The service. */
	protected IDeploymentService service;
	
	/** The scroll pane. */
	protected JScrollPane scrollpane;
	
	/**
	 * 
	 */
	public DeploymentServiceViewerPanel(IExternalAccess exta, boolean remote, 
		IDeploymentService service, INodeHandler nodehandler)
	{
		this.service = service;
		
		ftp = new FileTreePanel(exta, remote);
		DefaultFileFilterMenuItemConstructor mic = new DefaultFileFilterMenuItemConstructor(ftp.getModel());
		ftp.setPopupBuilder(new PopupBuilder(new Object[]{mic}));
		DefaultFileFilter ff = new DefaultFileFilter(mic);
		ftp.setFileFilter(ff);
		ftp.addNodeHandler(new DefaultNodeHandler(ftp.getTree()));
		if(nodehandler!=null)
			ftp.addNodeHandler(nodehandler);
		ftp.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		File[] roots = File.listRoots();
		for(int i=0; i<roots.length; i++)
		{
			ftp.addTopLevelNode(roots[i]);
		}
		
		this.scrollpane = new JScrollPane(ftp);
	}
	
	/**
	 *  Set the properties
	 *  @param props
	 *  @return
	 */
	public IFuture setProperties(Properties props)
	{
		return ftp.setProperties(props);
	}
	
	/**
	 *  Get the properties.
	 *  @return
	 */
	public IFuture getProperties()
	{
		return ftp.getProperties();
	}
	
	/**
	 * 
	 */ 
	public IFuture shutdown()
	{
		return new Future(null);
	}
	
	/**
	 * 
	 */
	public String getId()
	{
		return ""+ftp.hashCode();
	}

	/**
	 * 
	 */
	public JComponent getComponent()
	{
		return scrollpane;
	}

	/**
	 * 
	 * @param jcc
	 * @param service
	 * @return
	 */
	public IFuture init(IControlCenter jcc, IService service)
	{
		return new Future(null);
	}
	
	/**
	 *  Get the ftp.
	 *  @return the ftp.
	 */
	public String getSelectedPath()
	{
		String[] sels = ftp.getSelectionPaths();
		return sels.length>0? sels[0]: null;
	}

	/**
	 *  Get the service.
	 *  @return the service.
	 */
	public IDeploymentService getDeploymentService()
	{
		return service;
	}
	
	/**
	 * 
	 */
	public FileTreePanel getFileTreePanel()
	{
		return ftp;
	}
}
