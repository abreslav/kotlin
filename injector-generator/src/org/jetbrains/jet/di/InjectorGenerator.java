/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.di;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
* @author abreslav
*/
public class InjectorGenerator {

    public static final String INDENT_STEP = "    ";
    private static final String LOCK_NAME = "__lock__";

    private static final Multimap<Class<?>, Field> typeToVariable = HashMultimap.create();
    private static final Set<Field> satisfied = Sets.newHashSet();
    private static final Set<Field> fields = Sets.newLinkedHashSet();
    private static final Set<Parameter> parameters = Sets.newLinkedHashSet();
    private static final Set<Field> backsParameter = Sets.newHashSet();

    public static void main(String[] args) throws IOException {
        // Fields
        addPublicField(TopDownAnalyzer.class);
        addPublicField(BodyResolver.class);
        addPublicField(ControlFlowAnalyzer.class);
        addPublicField(DeclarationsChecker.class);
        addPublicField(DescriptorResolver.class);
        addPublicField(ExpressionTypingServices.class);
        addPublicField(TypeResolver.class);
        addPublicField(CallResolver.class);
        addField(true, JetStandardLibrary.class, null, new GivenExpression("analyzingBootstrapLibrary ? null : JetStandardLibrary.getInstance()"));

        // Parameters
        addPublicParameter(Project.class);
        addPublicParameter(TopDownAnalysisContext.class);
        addParameter(ModuleConfiguration.class);
        addParameter(JetControlFlowDataTraceFactory.class);
        addParameter(false, boolean.class, "analyzingBootstrapLibrary");

        String injectorPackageName = "org.jetbrains.jet.di";
        String injectorClassName = "Injector";

        String targetSourceRoot = "compiler/frontend/src";
        String outputFileName = targetSourceRoot + "/" + injectorPackageName.replace(".", "/") + "/" + injectorClassName + ".java";

        PrintStream out = new PrintStream(new FileOutputStream(outputFileName));
        try {
            for (Field field : Lists.newArrayList(fields)) {
                satisfyDependenciesFor(field);
            }

            String copyright = "injector-generator/copyright.txt";
            out.println(FileUtil.loadFile(new File(copyright)));
            out.println();

            out.println("package " + injectorPackageName + ";");
            out.println();

            generateImports(out, injectorPackageName);
            out.println();
            out.println("public class " + injectorClassName + " {");
//            out.println();
//            out.println("    private static final Object " + LOCK_NAME + " = new Object();");
            out.println();
            generateFields(out);
            out.println();
            generateConstructor(injectorClassName, out);
            out.println();
            generateGetters(out);
            out.println();
            generateMakeFunction(out);
            out.println("}"); // class
        }
        finally {
            out.close();
        }
    }

    private static void addPublicParameter(Class<?> type) {
        addParameter(true, type, var(type));
    }

    private static void addParameter(Class<?> type) {
        addParameter(false, type, var(type));
    }

    private static void addParameter(boolean reexport, @NotNull Class<?> type, @Nullable String name) {
        Field field = addField(reexport, type, name, null);
        Parameter parameter = new Parameter(type, name, field);
        parameters.add(parameter);
        backsParameter.add(field);
        typeToVariable.put(type, field);
    }

    private static Field addPublicField(Class<?> type) {
        return addField(true, type, null, null);
    }

    private static Field addField(Class<?> type) {
        return addField(false, type, null, null);
    }

    private static Field addField(boolean isPublic, Class<?> type, @Nullable String name, @Nullable Expression init) {
        Field field = Field.create(isPublic, type, name == null ? var(type) : name, init);
        fields.add(field);
        typeToVariable.put(type, field);
        return field;
    }

    private static void generateImports(PrintStream out, String injectorPackageName) {
        for (Field field : fields) {
            generateImportDirective(out, field.getType(), injectorPackageName);
        }
        for (Parameter parameter : parameters) {
            generateImportDirective(out, parameter.getType(), injectorPackageName);
        }
    }

    private static void generateImportDirective(PrintStream out, Class<?> type, String injectorPackageName) {
        if (type.isPrimitive()) return;
        String importedPackageName = type.getPackage().getName();
        if ("java.lang".equals(importedPackageName)
            || injectorPackageName.equals(importedPackageName)) {
            return;
        }
        out.println("import " + type.getCanonicalName() + ";");
    }

    private static void generateFields(PrintStream out) {
        for (Field field : fields) {
            String _final = backsParameter.contains(field) ? "final " : "";
            out.println("    private " + _final + field.getType().getSimpleName() + " " + field.getName() + ";");
        }
    }

    private static void generateConstructor(String injectorClassName, PrintStream out) {
        String indent = "        ";

        // Constructor parameters
        if (parameters.isEmpty()) {
            out.println("    public " + injectorClassName + "() {");
        }
        else {
            out.println("    public " + injectorClassName + "(");
            for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext(); ) {
                Parameter parameter = iterator.next();
                out.print(indent + parameter.getType().getSimpleName() + " " + parameter.getName());
                if (iterator.hasNext()) {
                    out.println(",");
                }
            }
            out.println("\n    ) {");
        }

        // Remember parameters
        for (Parameter parameter : parameters) {
            out.println(indent + "this." + parameter.getField().getName() + " = " + parameter.getName() + ";");
        }

        out.println("    }");
    }

    private static void generateGetters(PrintStream out) {
        String indent0 = "    ";
        String indent1 = indent0 + INDENT_STEP;
        String indent2 = indent1 + INDENT_STEP;
        String indent3 = indent2 + INDENT_STEP;
        String indent4 = indent3 + INDENT_STEP;
        for (Field field : fields) {
            String visibility = field.isPublic() ? "public" : "private";
            out.println(indent0 + visibility + " " + field.getTypeName() + " " + field.getGetterName() + "() {");

            if (!backsParameter.contains(field)) {
                Expression initialization = field.getInitialization();
                assert initialization != null : field;

                // Double-checked locking
                out.println(indent1 + "if (this." + field.getName() + " == null) {");

                out.println(indent2 + "this." + field.getName() + " = " + initialization + ";");
                // Invoke setters
                for (SetterDependency dependency : field.getDependencies()) {
                    out.println(indent2 + "this." + field.getName() + "." + dependency.getSetterName() + "(" + dependency.getDependency().getGetterName() + "());");
                }

                out.println(indent1 + "}"); // Outer if

                /*
                // Double-checked locking
                out.println(indent1 + "if (this." + field.getName() + " == null) {");
                out.println(indent2 + "synchronized (" + LOCK_NAME + ") {");
                out.println(indent3 + "if (this." + field.getName() + " == null) {");

                out.println(indent4 + "this." + field.getName() + " = " + initialization + ";");
                // Invoke setters
                for (SetterDependency dependency : field.getDependencies()) {
                    out.println(indent4 + "this." + field.getName() + "." + dependency.getSetterName() + "(" + dependency.getDependency().getGetterName() + "());");
                }

                out.println(indent3 + "}"); // Inner if
                out.println(indent2 + "}"); // synchronized
                out.println(indent1 + "}"); // Outer if
                */
            }

            out.println(indent1 + "return this." + field.getName() + ";");
            out.println(indent0 + "}");
            out.println();
        }
    }

    private static void satisfyDependenciesFor(Field field) {
        if (!satisfied.add(field)) return;
        if (backsParameter.contains(field)) return;

        if (field.getInitialization() == null) {
            initializeByConstructorCall(field);
        }

        for (Method method : field.getType().getDeclaredMethods()) {
            if (method.getAnnotation(javax.inject.Inject.class) == null
                || !method.getName().startsWith("set")
                || method.getParameterTypes().length != 1) continue;

            Class<?> parameterType = method.getParameterTypes()[0];


            Field dependency = findDependencyOfType(parameterType, field + ": " + method + ": " + fields);

            field.getDependencies().add(new SetterDependency(field, method.getName(), dependency));
        }
    }

    private static Field findDependencyOfType(Class<?> parameterType, String errorMessage) {
        Collection<Field> fields = typeToVariable.get(parameterType);
        Field dependency;
        if (fields.isEmpty()) {
            dependency = addField(parameterType);
            satisfyDependenciesFor(dependency);
        }
        else if (fields.size() == 1) {
            dependency = fields.iterator().next();
        }
        else {
            throw new IllegalArgumentException("Ambiguous dependency: " + errorMessage);
        }
        return dependency;
    }

    private static void initializeByConstructorCall(Field field) {
        Class<?> type = field.getType();

        // Look for constructor
        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0 || !Modifier.isPublic(constructors[0].getModifiers())) {
            throw new IllegalArgumentException("No constructor: " + type.getName());
        }
        Constructor<?> constructor = constructors[0];

        // Find arguments
        ConstructorCall dependency = new ConstructorCall(constructor);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            Field fieldForParameter = findDependencyOfType(parameterType, "constructor: " + constructor + ", parameter: " + parameterType);
            dependency.getConstructorArguments().add(fieldForParameter);
        }

        field.setInitialization(dependency);
    }

    private static String var(Class<?> theClass) {
        return StringUtil.decapitalize(theClass.getSimpleName());
    }

    private static void generateMakeFunction(PrintStream out) {
        out.println("    private static <T> T make(Class<T> theClass) {");
        out.println("        try {                                     ");
        out.println("            return theClass.newInstance();        ");
        out.println("        }                                         ");
        out.println("        catch (InstantiationException e) {        ");
        out.println("            throw new IllegalStateException(e);   ");
        out.println("        }                                         ");
        out.println("        catch (IllegalAccessException e) {        ");
        out.println("            throw new IllegalStateException(e);   ");
        out.println("        }                                         ");
        out.println("    }                                             ");
    }
}