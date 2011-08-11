package jadex.simulation.analysis.application.netLogo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.nlogo.lite.InterfaceComponent;

import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.IADataObject;
import jadex.simulation.analysis.common.data.IAModel;
import jadex.simulation.analysis.common.data.factories.AModelFactory;
import jadex.simulation.analysis.common.data.parameter.IAParameterEnsemble;
import jadex.simulation.analysis.common.events.service.AServiceEvent;
import jadex.simulation.analysis.service.basic.analysis.IAnalysisService;
import jadex.simulation.analysis.service.basic.analysis.IAnalysisSessionService;
import jadex.simulation.analysis.service.basic.view.DefaultServiceView;
import jadex.simulation.analysis.service.basic.view.session.ADefaultSessionView;
import jadex.simulation.analysis.service.basic.view.session.IASessionView;
import jadex.simulation.analysis.service.basic.view.session.SessionProperties;
import jadex.simulation.analysis.service.basic.view.session.subprocess.ATaskInternalFrame;

public class NetLogoSessionView extends JPanel implements IASessionView
{
	protected IAnalysisSessionService service;
	private SessionProperties prop = null;
//	private JTextArea comp = new JTextArea();
	
	public NetLogoSessionView( IAnalysisSessionService service, final UUID id, final IAParameterEnsemble config)
	{
		super();
		this.service = service;
		prop = new SessionProperties(id, config);

	}
	
	public IFuture showFull(final JComponent comp)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				add(comp, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
				comp.revalidate();
				comp.repaint();
				revalidate();
				repaint();
			}
		});
		return new Future(null);
	}
	
	public IFuture showLite(final JTextArea comp)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				add(comp, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
				comp.revalidate();
				comp.repaint();
				revalidate();
				repaint();
			}
		});
		return new Future(null);
	}

	@Override
	public void serviceEventOccur(AServiceEvent event)
	{
		//omitt
	}

	@Override
	public SessionProperties getSessionProperties()
	{
		return prop ;
	}
	


}
