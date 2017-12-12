package jadex.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import jadex.commons.SUtil;
import jadex.commons.collection.WeakKeyValueMap;

/**
 *  Static ASM helper methods.
 */
public class SASM
{
    /** 
	 *  Enables the shared bytecode classloader mode.
	 *  If false, a new classloader is generated for each
	 *  generated class for easier unloading, but
	 *  potentially wastes more memory.
	 */
	public static boolean SHARED_LOADERS_MODE = false;
	
	/** Shared ClassLoader cache. */
	protected static final WeakKeyValueMap<ClassLoader, ByteCodeClassLoader> SHARED_CLASSLOADERS =
		new WeakKeyValueMap<ClassLoader, ByteCodeClassLoader>();
	
	/**
	 *  Push an immediate (constant) integer value onto the stack
	 *  with the best set of instructions.
	 *  
	 *  @param nl The instruction list. 
	 *  @param immediate The immediate value.
	 */
	public static void pushImmediate(InsnList nl, int immediate)
	{
		if (immediate >= -1 && immediate <= 5)
		{
			switch (immediate)
			{
				case -1:
					nl.add(new InsnNode(Opcodes.ICONST_M1));
					break;
				case 0:
					nl.add(new InsnNode(Opcodes.ICONST_0));
					break;
				case 1:
					nl.add(new InsnNode(Opcodes.ICONST_1));
					break;
				case 2:
					nl.add(new InsnNode(Opcodes.ICONST_2));
					break;
				case 3:
					nl.add(new InsnNode(Opcodes.ICONST_3));
					break;
				case 4:
					nl.add(new InsnNode(Opcodes.ICONST_4));
					break;
				case 5:
					nl.add(new InsnNode(Opcodes.ICONST_5));
			}
		}
		else if (immediate <= Byte.MAX_VALUE && immediate >= Byte.MIN_VALUE)
		{
			nl.add(new IntInsnNode(Opcodes.BIPUSH, (int) immediate));
		}
		else if (immediate <= Short.MAX_VALUE && immediate >= Short.MIN_VALUE)
		{
			nl.add(new IntInsnNode(Opcodes.SIPUSH, (int) immediate));
		}
		else
		{
			nl.add(new LdcInsnNode(immediate));
		}
	}
	
	/**
	 *  Push an immediate (constant) long value onto the stack
	 *  with the best set of instructions.
	 *  
	 *  @param nl The instruction list. 
	 *  @param immediate The immediate value.
	 */
	public static void pushImmediate(InsnList nl, long immediate)
	{
		if (immediate == 0L)
		{
			nl.add(new InsnNode(Opcodes.LCONST_0));
		}
		else if (immediate == 1L)
		{
			nl.add(new InsnNode(Opcodes.LCONST_1));
		}
		else if (immediate >= Integer.MIN_VALUE && immediate <= Integer.MAX_VALUE)
		{
			pushImmediate(nl, (int) immediate);
			nl.add(new InsnNode(Opcodes.I2L));
		}
		else
		{
			nl.add(new LdcInsnNode(immediate));
		}
	}
	
	/**
	 *  Make a value to an object.
	 *  @param nl The instruction list.
	 *  @param type The value type.
	 */
	public static void makeObject(InsnList nl, Type type)
	{
		makeObject(nl, type, 1);
	}
	
	/**
	 *  Make a value to an object.
	 *  @param nl The instruction list.
	 *  @param type The value type.
	 *  @param pos The position of the value on the registers (default=1, 0 is this).
	 *  @return The updated position value.
	 */
	public static int makeObject(InsnList nl, Type arg, int pos)
	{
		if(arg.getClassName().equals("byte"))
		{
			nl.add(new VarInsnNode(Opcodes.ILOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
		}
		else if(arg.getClassName().equals("short"))
		{
			nl.add(new VarInsnNode(Opcodes.ILOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
		}
		else if(arg.getClassName().equals("int"))
		{
			nl.add(new VarInsnNode(Opcodes.ILOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
		}
		else if(arg.getClassName().equals("char"))
		{
			nl.add(new VarInsnNode(Opcodes.ILOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
		}
		else if(arg.getClassName().equals("boolean"))
		{
			nl.add(new VarInsnNode(Opcodes.ILOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
		}
		else if(arg.getClassName().equals("long"))
		{
			nl.add(new VarInsnNode(Opcodes.LLOAD, pos++));
			pos++;
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
		}
		else if(arg.getClassName().equals("float"))
		{
			nl.add(new VarInsnNode(Opcodes.FLOAD, pos++));
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
		}
		else if(arg.getClassName().equals("double"))
		{
			nl.add(new VarInsnNode(Opcodes.DLOAD, pos++));
			pos++;
			nl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
		}
		else // Object
		{
			nl.add(new VarInsnNode(Opcodes.ALOAD, pos++));
		}
		
		return pos;
	}
	
	/**
	 *  Make a value a basic type.
	 *  @param nl The instruction list.
	 *  @param type The value type.
	 */
	public static void makeBasicType(InsnList nl, Type type)
	{
		if(type.getClassName().equals("byte"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Boolean.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
		}
		else if(type.getClassName().equals("short"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Short.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
		}
		else if(type.getClassName().equals("int"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Integer.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
		}
		else if(type.getClassName().equals("char"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Character.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
		}
		else if(type.getClassName().equals("boolean"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Boolean.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
		}
		else if(type.getClassName().equals("long"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Long.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
		}
		else if(type.getClassName().equals("float"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Float.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
		}
		else if(type.getClassName().equals("double"))
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Double.class)));
			nl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
		}
//		else // Object
//		{
//		}
	}
	
	/**
	 *  Make a suitable return statement.
	 *  @param nl The instruction list.
	 *  @param type The value type.
	 */
	public static void makeReturn(InsnList nl, Type type)
	{
		if(type.getClassName().equals("byte"))
		{
			nl.add(new InsnNode(Opcodes.IRETURN));
		}
		else if(type.getClassName().equals("short"))
		{
			nl.add(new InsnNode(Opcodes.IRETURN));
		}
		else if(type.getClassName().equals("int"))
		{
			nl.add(new InsnNode(Opcodes.IRETURN));
		}
		else if(type.getClassName().equals("char"))
		{
			nl.add(new InsnNode(Opcodes.IRETURN));
		}
		else if(type.getClassName().equals("boolean"))
		{
//			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(boolean.class)));
			nl.add(new InsnNode(Opcodes.IRETURN));
		}
		else if(type.getClassName().equals("long"))
		{
			nl.add(new InsnNode(Opcodes.LRETURN));
		}
		else if(type.getClassName().equals("float"))
		{
			nl.add(new InsnNode(Opcodes.FRETURN));
		}
		else if(type.getClassName().equals("double"))
		{
			nl.add(new InsnNode(Opcodes.DRETURN));
		}
		else if(Type.VOID_TYPE.equals(type))
		{
			nl.add(new InsnNode(Opcodes.RETURN));
		}
		else 
		{
			nl.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
			nl.add(new InsnNode(Opcodes.ARETURN));
		}
	}
	
	
	
	
	
	/**
	 *  Transform byte Array into Class and define it in classloader.
	 *  @return the loaded class or <code>null</code>, if the class is not valid, such as Map.entry "inner Classes".
	 */
	public static Class<?> toClass(String name, byte[] data, ClassLoader loader, ProtectionDomain domain)
	{
		Class<?> ret = null;
		
		ByteCodeClassLoader bcl = getByteCodeClassLoader(loader, true);
		try
		{
			if (domain == null)
				ret = bcl.doDefineClass(name, data, 0, data.length);
			else
				ret = bcl.doDefineClass(name, data, 0, data.length, domain);
		}
		catch(LinkageError e)
		{
			// when same class was already loaded via other filename wrong cache miss:-(
			try
			{
				ret = Class.forName(name, true, bcl);
			}
			catch (Exception e1)
			{
				SUtil.throwUnchecked(e);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get a class node for a class.
	 *  @param clazz The clazz. 
	 *  @return The class node.
	 */
	public static ClassNode getClassNode(Class<?> clazz, ClassLoader classloader)
	{
		ClassNode cns = null;
		
		try
		{
			cns = new ClassNode();
			ClassReader crs = new ClassReader(SUtil.getResource(clazz.getName().replace(".", "/")+".class", classloader));
			crs.accept(cns, 0);
			return cns;
		}
		catch(Exception e)
		{
			SUtil.rethrowAsUnchecked(e);
		}
		
		return cns;
	}
	
	/**
	 *  Generates a ByteCodeClassLoader for loading a generated class.
	 * 
	 *  @param parent Parent ClassLoader.
	 *  @param sharedloader Set true, to use shared loaders.
	 *  @return The ByteCodeClassLoader.
	 */
	public static ByteCodeClassLoader getByteCodeClassLoader(ClassLoader parent)
	{
		return getByteCodeClassLoader(parent, SHARED_LOADERS_MODE);
	}
	
	/**
	 *  Generates a ByteCodeClassLoader for loading a generated class.
	 * 
	 *  @param parent Parent ClassLoader.
	 *  @param sharedloader Set true, to use shared loaders.
	 *  @return The ByteCodeClassLoader.
	 */
	public static ByteCodeClassLoader getByteCodeClassLoader(ClassLoader parent, boolean sharedloaders)
	{
		while (parent instanceof ByteCodeClassLoader)
			parent = parent.getParent();
		
		ByteCodeClassLoader bcl = null;
		
		if (sharedloaders)
		{
			synchronized(SHARED_CLASSLOADERS)
			{
				bcl = SHARED_CLASSLOADERS.get(parent);
				
				if (bcl == null)
				{
					bcl = new ByteCodeClassLoader(parent);
					SHARED_CLASSLOADERS.put(parent, bcl);
				}
			}
		}
		else
		{
			bcl = new ByteCodeClassLoader(parent);
		}
		
		return bcl;
	}
}