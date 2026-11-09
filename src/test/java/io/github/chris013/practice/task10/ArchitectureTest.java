package io.github.chris013.practice.task10;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Guards the dependency rule with plain reflection, so nobody can quietly make the domain depend on the database
 * or the application depend on an adapter. It checks declarations only (superclass, interfaces, fields, constructor
 * and method signatures). That catches the mistakes people actually make. It does not look inside method bodies.
 */
class ArchitectureTest {

    private static final String BASE = "io.github.chris013.practice.task10";
    private static final String DOMAIN = BASE + ".domain";
    private static final String APPLICATION = BASE + ".application";
    private static final String ADAPTER = BASE + ".adapter";
    private static final String IN_MEMORY = ADAPTER + ".inmemory";

    @Test
    void the_domain_does_not_depend_on_the_application_or_the_adapters() throws Exception {
        assertNoDependency(DOMAIN, APPLICATION);
        assertNoDependency(DOMAIN, ADAPTER);
    }

    @Test
    void the_application_does_not_depend_on_the_adapters() throws Exception {
        assertNoDependency(APPLICATION, ADAPTER);
    }

    @Test
    void every_checked_layer_contains_classes() throws Exception {
        // Without this, a wrong package name would make every rule above pass without checking anything.
        for (String pkg : List.of(DOMAIN, APPLICATION, IN_MEMORY)) {
            assertThat(classesIn(pkg)).as("classes in %s; without them the rules check nothing", pkg).isNotEmpty();
        }
    }

    private static void assertNoDependency(String fromPackage, String forbiddenPackage) throws Exception {
        for (Class<?> from : classesIn(fromPackage)) {
            for (Class<?> used : referencedTypesOf(from)) {
                String usedPackage = used.getPackageName();
                if (usedPackage.equals(forbiddenPackage) || usedPackage.startsWith(forbiddenPackage + ".")) {
                    fail("%s must not depend on %s, but it uses %s", from.getName(), forbiddenPackage, used.getName());
                }
            }
        }
    }

    /** All classes compiled into the given package, from every classpath root (main and test output). */
    private static List<Class<?>> classesIn(String pkg) throws Exception {
        ClassLoader loader = ArchitectureTest.class.getClassLoader();
        String path = pkg.replace('.', '/');
        List<Class<?>> classes = new ArrayList<>();
        Enumeration<URL> roots = loader.getResources(path);
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            if (!"file".equals(root.getProtocol())) {
                throw new AssertionError("expected compiled classes on disk, found " + root);
            }
            try (var files = Files.list(Path.of(root.toURI()))) {
                for (Path file : files.toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".class")) {
                        String className = fileName.substring(0, fileName.length() - ".class".length());
                        classes.add(Class.forName(pkg + "." + className, false, loader));
                    }
                }
            }
        }
        return classes;
    }

    /** Every type that appears in the declarations of the given class, including generic arguments. */
    private static Set<Class<?>> referencedTypesOf(Class<?> type) {
        Set<Class<?>> used = new LinkedHashSet<>();
        collect(type.getGenericSuperclass(), used);
        for (Type implemented : type.getGenericInterfaces()) {
            collect(implemented, used);
        }
        for (Field field : type.getDeclaredFields()) {
            collect(field.getGenericType(), used);
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            collectAll(constructor.getGenericParameterTypes(), used);
        }
        for (Method method : type.getDeclaredMethods()) {
            collect(method.getGenericReturnType(), used);
            collectAll(method.getGenericParameterTypes(), used);
        }
        return Collections.unmodifiableSet(used);
    }

    private static void collectAll(Type[] types, Set<Class<?>> out) {
        for (Type type : types) {
            collect(type, out);
        }
    }

    private static void collect(Type type, Set<Class<?>> out) {
        if (type == null) {
            return;
        }
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                collect(clazz.getComponentType(), out);
            } else {
                out.add(clazz);
            }
        } else if (type instanceof ParameterizedType parameterized) {
            collect(parameterized.getRawType(), out);
            collectAll(parameterized.getActualTypeArguments(), out);
        } else if (type instanceof GenericArrayType array) {
            collect(array.getGenericComponentType(), out);
        } else if (type instanceof WildcardType wildcard) {
            collectAll(wildcard.getUpperBounds(), out);
            collectAll(wildcard.getLowerBounds(), out);
        }
    }
}
