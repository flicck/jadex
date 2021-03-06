package jadex.commons.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 *  Animated busy ring.
 *
 */
public class JBusyRing extends JComponent
{
	/** Flag if active. */
	protected boolean active;
	
	/** Widget size. */
	protected Dimension size;
	
	/** Color of the ring. */
	protected Color ringcolor = Color.GRAY; //Color.WHITE; // new Color(120, 190, 255);
	
	/** Start frame time stamp. */
	protected long startframe = System.currentTimeMillis();
	
	/** Animation action. */
	protected ActionListener animationaction;
	
	/** Animation timer. */
	protected Timer animationtimer;
	
//	protected boolean wandering = false;
	
	/** Time for a full rotation of the ring. */
	protected long fullrot = 1000;
	
	/** Time between frames. */
	protected int frametime = 17;
	
	/** Half the size of a stroke. */
	protected double halfstroke = 0.1;
	
	public JBusyRing()
	{
		super();
		setSize(new Dimension(32, 32));
		setBackground(new Color(1.0f, 1.0f, 1.0f, 0.0f));
		animationaction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				repaint();
			}
		};
		animationtimer = new Timer(frametime, animationaction);
		setDoubleBuffered(true);
		animationtimer.setRepeats(true);
	}
	
	/**
	 *  Activates the animation.
	 */
	public void activate()
	{
		startframe = System.currentTimeMillis();
		active = true;
		animationtimer.restart();
	}
	
	/**
	 *  Deactivates the animation.
	 */
	public void deactivate()
	{
		active = false;
		animationtimer.stop();
		repaint();
	}
	
	/**
	 *  Sets the color of the ring.
	 *  
	 *  @param c The color.
	 */
	public void setRingColor(Color c)
	{
		ringcolor = c;
	}
	
	/**
	 *  Sets the rotations per second for the ring.
	 *  
	 *  @param rps The rotations per second.
	 */
	public void setRotationsPerSecond(double rps)
	{
		fullrot = (long) (Math.round(1000.0 / rps));
	}
	
	/**
	 *  Sets the animation frame rate.
	 *  
	 *  @param fps Frames per second.
	 */
	public void setFramesPerSecond(double fps)
	{
		frametime = (int) (Math.round(1000.0 / fps));
	}
	
	/**
	 * 
	 */
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g.clearRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
//		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
//		g.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
//		g.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
		
		if (!active)
			return;
		
//		BufferedImage frame = new BufferedImage(g.getClipBounds().width, g.getClipBounds().height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
//		Graphics2D g2 = frame.createGraphics();
//		double sx = frame.getWidth();
//		double sy = frame.getHeight();
		
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		double sx = Math.max(0, g2.getClipBounds().width - 2);
		double sy = Math.max(0, g2.getClipBounds().height - 2);
//		g2.getTransform().scale(sx, sy);
		
		long diff = System.currentTimeMillis() - startframe;
		double rot = (diff % fullrot) * 360.0 / fullrot;
//		long twofr = fullrot + fullrot;
//		double rot2 = (diff % twofr) * 360.0 / twofr;
		
		AffineTransform oldt = g2.getTransform();
		AffineTransform t = new AffineTransform(oldt);
//		AffineTransform t = new AffineTransform();
		t.translate(1.0, 1.0);
		t.scale(sx, sy);
		g2.setTransform(t);
		
		double stroke = halfstroke + halfstroke;
//		g2.setStroke(new BasicStroke((float)(halfstroke + halfstroke), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
//		g2.setStroke(new BasicStroke((float)(halfstroke + halfstroke), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
//		g2.setStroke(new BasicStroke((float)(halfstroke + halfstroke), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
		g2.setStroke(new BasicStroke((float)(halfstroke + halfstroke), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		
		double segs = 144.0;
		double ringsize = 270.0;
		double ringpos = 360 - rot;
		
//		double segsize = 270.0 / segs;
		double segsize = ringsize / segs;
		for (int i = 0; i < segs; ++i)
		{
			double finput = ((double)i/segs) - 1.0;
			finput *= finput;
			finput *= finput;
			int f = (int) (finput * 255.0);
//			if (i > segs / 2)
//				f = 10;
			Color faded = new Color(ringcolor.getRed(), ringcolor.getGreen(), ringcolor.getBlue(), f);//(int) (255 / segs) * i);
			g2.setColor(faded);
			
			Arc2D arc = new Arc2D.Double(0.0 + halfstroke,
					0.0 + halfstroke,
					1.0 - stroke,
					1.0 - stroke,
					ringpos + segsize * i, segsize, Arc2D.OPEN);
			g2.draw(arc);
		}
		g2.setTransform(oldt);
		g2.dispose();
		
//		g.drawImage(frame, 0, 0, null);
	}
	
	/**
	 * 
	 */
	public Dimension getMaximumSize()
	{
		return size;
	}
	
	/**
	 * 
	 */
	public Dimension getPreferredSize()
	{
		return size;
	}
	
	/**
	 * 
	 */
	public Dimension getMinimumSize()
	{
		return size;
	}
	
	/**
	 * 
	 */
	public void setSize(Dimension d)
	{
		size = d;
	}
	
	/**
	 * 
	 */
	public void setSize(int width, int height)
	{
		setSize(new Dimension(width, height));
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		if (animationtimer.isRunning())
			animationtimer.stop();
	}
}
