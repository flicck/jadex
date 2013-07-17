package jadex.rules.eca;

import jadex.commons.IMethodParameterGuesser;
import jadex.commons.SimpleMethodParameterGuesser;
import jadex.commons.SimpleParameterGuesser;
import jadex.commons.Tuple2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *  Condition implementation that invokes a predefined method.
 */
public class MethodCondition implements ICondition
{
	//-------- attributes --------
	
	/** The object. */
	protected Object object;
	
	/** The method. */
	protected Method method;
	
	/** The invert flag. Inverts method result. */
	protected boolean invert;
	
	/** The parameter guesser. */
	protected IMethodParameterGuesser guesser;
	
	//-------- constructors --------
	
	/**
	 *  Create a new method condition.
	 */
	public MethodCondition(Object object, Method method)
	{
		this(object, method, false);
	}
	
	/**
	 *  Create a new method condition.
	 */
	public MethodCondition(Object object, Method method, IMethodParameterGuesser guesser)
	{
		this(object, method, false, guesser);
	}
	
	/**
	 *  Create a new method condition.
	 */
	public MethodCondition(Object object, Method method, boolean invert)
	{
		this(object, method, invert, null);
	}
	
	/**
	 *  Create a new method condition.
	 */
	public MethodCondition(Object object, Method method, boolean invert, 
		IMethodParameterGuesser guesser)
	{
//		if(object==null)
//			System.out.println("hetre");
		
		this.object = object;
		this.method = method;
		this.invert = invert;
		this.guesser = guesser;
	}

	//-------- methods --------

	/**
	 *  Evaluate the condition.
	 */
	public Tuple2<Boolean, Object> evaluate(IEvent event)
	{
		Tuple2<Boolean, Object> ret = ICondition.FALSE;
		try
		{
			ret	= prepareResult(invokeMethod(event));
			
			if(invert)
			{
				Boolean b = ret.getFirstEntity().booleanValue()? Boolean.FALSE: Boolean.TRUE;
				ret = new Tuple2<Boolean, Object>(b, ret.getSecondEntity());
			}
		}
		catch(Exception e)
		{
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
		return ret;
	}
	
	/**
	 *  Do the method invocation.
	 */
	protected Object	invokeMethod(IEvent event)	throws Exception
	{
		method.setAccessible(true);
		Object[] params = null;
		List<Object> vals = new ArrayList<Object>();
		vals.add(event);
		if(event.getContent()!=null)
		{
			vals.add(event.getContent());
		}
		if(guesser==null)
		{
			params = new SimpleMethodParameterGuesser(vals).guessParameters(method.getParameterTypes());
		}
		else
		{
			// Hack!!! what if already has a parent?
			guesser.getGuesser().setParent(new SimpleParameterGuesser(vals));
			params = guesser.guessParameters(method.getParameterTypes());
		}
			
		return method.invoke(object, params);
	}
	
	/**
	 * 
	 */
	public Tuple2<Boolean, Object> prepareResult(Object res)
	{
		Tuple2<Boolean, Object> ret;
		if(res instanceof Tuple2)
		{
			ret = (Tuple2<Boolean, Object>)res;
		}
		else 
		{
			boolean bs = ((Boolean)res).booleanValue();
			ret = bs? ICondition.TRUE: ICondition.FALSE;
		}
		return ret;
	}
}
