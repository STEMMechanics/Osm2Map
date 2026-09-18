package com.wardenfar.osm2map.compat;

import org.junit.Test;
import org.objectweb.asm.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.*;
import static org.junit.Assert.*;

/** Check the precompiled command library, which javac cannot validate. */
public class IntakeApiCompatibilityTest {
    @Test public void bundledIntakeServerLibraryReferencesStillResolve() throws Exception {
        List<String> failures = new ArrayList<>();
        try (JarFile jar = new JarFile("libs/intake-spigot-4.2.35-plugin-with-xyc.jar")) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) continue;
                ClassReader reader = new ClassReader(jar.getInputStream(entry));
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                               String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int opcode, String owner, String method,
                                                                  String descriptor, boolean isInterface) {
                                if (!isServerLibrary(owner)) return;
                                try {
                                    Class<?> type = Class.forName(owner.replace('/', '.'), false, getClass().getClassLoader());
                                    if (!hasMethod(type, method, descriptor)) {
                                        failures.add(entry.getName() + " -> " + owner + "." + method + descriptor);
                                    }
                                } catch (ReflectiveOperationException | LinkageError error) {
                                    failures.add(owner + ": " + error);
                                }
                            }
                            @Override public void visitFieldInsn(int opcode, String owner, String field, String descriptor) {
                                if (!isServerLibrary(owner)) return;
                                try {
                                    Class<?> type = Class.forName(owner.replace('/', '.'), false, getClass().getClassLoader());
                                    Field resolved = type.getField(field);
                                    if (!Type.getDescriptor(resolved.getType()).equals(descriptor)) failures.add(owner + "." + field);
                                } catch (ReflectiveOperationException | LinkageError error) {
                                    failures.add(owner + "." + field + ": " + error);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    private static boolean isServerLibrary(String owner) {
        return owner.startsWith("org/bukkit/") || owner.startsWith("com/google/common/")
                || owner.startsWith("net/md_5/bungee/");
    }

    private static boolean hasMethod(Class<?> type, String name, String descriptor) {
        if (name.equals("<init>")) {
            return Arrays.stream(type.getDeclaredConstructors()).anyMatch(c -> Type.getConstructorDescriptor(c).equals(descriptor));
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor)) return true;
        }
        if (type.getSuperclass() != null && hasMethod(type.getSuperclass(), name, descriptor)) return true;
        for (Class<?> parent : type.getInterfaces()) if (hasMethod(parent, name, descriptor)) return true;
        return false;
    }
}
