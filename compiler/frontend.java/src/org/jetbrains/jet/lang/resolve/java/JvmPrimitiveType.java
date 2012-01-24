package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.PrimitiveType;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stepan Koltsov
 */
public enum JvmPrimitiveType {
    BOOLEAN(PrimitiveType.BOOLEAN, "boolean", "java.lang.Boolean", Type.BOOLEAN_TYPE),
    CHAR(PrimitiveType.CHAR, "char", "java.lang.Character", Type.CHAR_TYPE),
    BYTE(PrimitiveType.BYTE, "byte", "java.lang.Byte", Type.BYTE_TYPE),
    SHORT(PrimitiveType.SHORT, "short", "java.lang.Short", Type.SHORT_TYPE),
    INT(PrimitiveType.INT, "int", "java.lang.Integer", Type.INT_TYPE),
    FLOAT(PrimitiveType.FLOAT, "float", "java.lang.Float", Type.FLOAT_TYPE),
    LONG(PrimitiveType.LONG, "long", "java.lang.Long", Type.LONG_TYPE),
    DOUBLE(PrimitiveType.DOUBLE, "double", "java.lang.Double", Type.DOUBLE_TYPE),
    ;
    
    private final PrimitiveType primitiveType;
    private final String name;
    private final JvmClassName wrapper;
    private final Type asmType;
    private final char jvmLetter;
    private final Type asmArrayType;
    private final JvmClassName iterator;

    private JvmPrimitiveType(PrimitiveType primitiveType, String name, String wrapperClassName, Type asmType) {
        this.primitiveType = primitiveType;
        this.name = name;
        this.wrapper = new JvmClassName(wrapperClassName);
        this.asmType = asmType;
        this.jvmLetter = asmType.getDescriptor().charAt(0);
        this.asmArrayType = makeArrayType(asmType);
        this.iterator = new JvmClassName("jet." + primitiveType.getTypeName() + "Iterator");
    }
    
    private static Type makeArrayType(Type type) {
        StringBuilder sb = new StringBuilder(2);
        sb.append('[');
        sb.append(type.getDescriptor());
        return Type.getType(sb.toString());
    }

    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public String getName() {
        return name;
    }

    public JvmClassName getWrapper() {
        return wrapper;
    }

    public Type getAsmType() {
        return asmType;
    }

    public Type getAsmArrayType() {
        return asmArrayType;
    }

    public JvmClassName getIterator() {
        return iterator;
    }

    public char getJvmLetter() {
        return jvmLetter;
    }



    private static class MapByAsmTypeHolder {
        private static final Map<Integer, JvmPrimitiveType> map;
        
        static {
            map = new HashMap<Integer, JvmPrimitiveType>();
            for (JvmPrimitiveType jvmPrimitiveType : values()) {
                map.put(jvmPrimitiveType.getAsmType().getSort(), jvmPrimitiveType);
            }
        }
    }

    @Nullable
    public static JvmPrimitiveType getByAsmType(Type type) {
        return MapByAsmTypeHolder.map.get(type.getSort());
    }
    
    
    private static class MapByWrapperAsmTypeHolder {
        private static final Map<Type, JvmPrimitiveType> map;

        static {
            map = new HashMap<Type, JvmPrimitiveType>();
            for (JvmPrimitiveType jvmPrimitiveType : values()) {
                map.put(jvmPrimitiveType.getWrapper().getAsmType(), jvmPrimitiveType);
            }
        }
    }
    
    @Nullable
    public static JvmPrimitiveType getByWrapperAsmType(Type type) {
        return MapByWrapperAsmTypeHolder.map.get(type);
    }

    @Nullable
    public static JvmPrimitiveType getByWrapperClass(JvmClassName className) {
        return getByWrapperAsmType(className.getAsmType());
    }
}