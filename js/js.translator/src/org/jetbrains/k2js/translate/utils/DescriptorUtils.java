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

package org.jetbrains.k2js.translate.utils;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.k2js.translate.context.Namer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isNotAny;

/**
 * @author Pavel Talanov
 */
public final class DescriptorUtils {

    private DescriptorUtils() {
    }

    private static int valueParametersCount(@NotNull FunctionDescriptor functionDescriptor) {
        return functionDescriptor.getValueParameters().size();
    }

    public static boolean hasParameters(@NotNull FunctionDescriptor functionDescriptor) {
        return (valueParametersCount(functionDescriptor) > 0);
    }

    public static boolean isEquals(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getName().equals(OperatorConventions.EQUALS));
    }

    public static boolean isCompareTo(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getName().equals(OperatorConventions.COMPARE_TO));
    }

    public static boolean isConstructorDescriptor(@NotNull CallableDescriptor descriptor) {
        return (descriptor instanceof ConstructorDescriptor);
    }

    @NotNull
    public static FunctionDescriptor getFunctionByName(@NotNull JetScope scope,
                                                       @NotNull String name) {
        Set<FunctionDescriptor> functionDescriptors = scope.getFunctions(name);
        assert functionDescriptors.size() == 1 :
                "In scope " + scope + " supposed to be exactly one " + name + " function.\n" +
                "Found: " + functionDescriptors.size();
        //noinspection LoopStatementThatDoesntLoop
        for (FunctionDescriptor descriptor : functionDescriptors) {
            return descriptor;
        }
        throw new AssertionError("In scope " + scope
                                 + " supposed to be exactly one " + name + " function.");
    }

    //TODO: some strange stuff happening to this method
    //TODO: inspect
    @NotNull
    public static PropertyDescriptor getPropertyByName(@NotNull JetScope scope,
                                                       @NotNull String name) {
        VariableDescriptor variable = scope.getLocalVariable(name);
        if (variable == null) {
            variable = scope.getPropertyByFieldReference("$" + name);
        }
        Set<VariableDescriptor> variables = scope.getProperties(name);
        assert variables.size() == 1 : "Actual size: " + variables.size();
        variable = variables.iterator().next();
        PropertyDescriptor descriptor = (PropertyDescriptor) variable;
        assert descriptor != null : "Must have a descriptor.";
        return descriptor;
    }

    @Nullable
    public static ClassDescriptor findAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() == ClassKind.CLASS) {
                return descriptor;
            }
        }
        return null;
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor) {
        Collection<? extends JetType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
        for (JetType type : superclassTypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (isNotAny(result)) {
                superClassDescriptors.add(result);
            }
        }
        return superClassDescriptors;
    }

    @Nullable
    public static ClassDescriptor getSuperclass(@NotNull ClassDescriptor classDescriptor) {
        return findAncestorClass(getSuperclassDescriptors(classDescriptor));
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        DeclarationDescriptor superClassDescriptor =
                type.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor instanceof ClassDescriptor
                : "Superclass descriptor of a type should be of type ClassDescriptor";
        return (ClassDescriptor) superClassDescriptor;
    }

    @NotNull
    public static VariableDescriptor getVariableDescriptorForVariableAsFunction
            (@NotNull VariableAsFunctionDescriptor descriptor) {
        VariableDescriptor functionVariable = descriptor.getVariableDescriptor();
        assert functionVariable != null;
        return functionVariable;
    }


    public static boolean isVariableAsFunction(@Nullable DeclarationDescriptor referencedDescriptor) {
        return referencedDescriptor instanceof VariableAsFunctionDescriptor;
    }

    @NotNull
    public static DeclarationDescriptor getContainingDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        assert containing != null : "Should be called on objects that have containing declaration.";
        return containing;
    }

    public static boolean isExtensionFunction(@NotNull CallableDescriptor functionDescriptor) {
        return (functionDescriptor.getReceiverParameter().exists());
    }

    @NotNull
    public static String getNameForNamespace(@NotNull NamespaceDescriptor descriptor) {
        String name = descriptor.getName();
        if (name.isEmpty()) {
            return Namer.getAnonymousNamespaceName();
        }
        return name;
    }

    //TODO: why callable descriptor
    @Nullable
    public static DeclarationDescriptor getExpectedThisDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor expectedThisObject = callableDescriptor.getExpectedThisObject();
        if (!expectedThisObject.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(expectedThisObject);
    }

    @NotNull
    public static DeclarationDescriptor getDeclarationDescriptorForReceiver
            (@NotNull ReceiverDescriptor receiverParameter) {
        DeclarationDescriptor declarationDescriptor =
                receiverParameter.getType().getConstructor().getDeclarationDescriptor();
        //TODO: WHY assert?
        assert declarationDescriptor != null;
        return declarationDescriptor.getOriginal();
    }

    @Nullable
    public static DeclarationDescriptor getExpectedReceiverDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor receiverParameter = callableDescriptor.getReceiverParameter();
        if (!receiverParameter.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(receiverParameter);
    }

    //TODO: maybe we have similar routine
    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        while (containing != null) {
            if (containing instanceof ClassDescriptor) {
                return (ClassDescriptor) containing;
            }
            containing = containing.getContainingDeclaration();
        }
        return null;
    }

    @NotNull
    public static List<ClassDescriptor> getAllClassesDefinedInNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        List<ClassDescriptor> classDescriptors = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : namespaceDescriptor.getMemberScope().getAllDescriptors()) {
            if (AnnotationsUtils.isPredefinedObject(descriptor)) {
                continue;
            }
            if (descriptor instanceof ClassDescriptor) {
                classDescriptors.add((ClassDescriptor) descriptor);
            }
        }
        return classDescriptors;
    }

    @Nullable
    public static FunctionDescriptor getOverriddenDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        Set<? extends FunctionDescriptor> overriddenDescriptors = functionDescriptor.getOverriddenDescriptors();
        if (overriddenDescriptors.isEmpty()) {
            return null;
        }
        else {
            //TODO: for now translator can't deal with multiple inheritance good enough
            return overriddenDescriptors.iterator().next();
        }
    }

    //TODO:?
//    @NotNull
//    public static FunctionDescriptor getOverriddenOrThis(@NotNull FunctionDescriptor functionDescriptor) {
//        FunctionDescriptor overriddenDescriptor = getOverriddenDescriptor(functionDescriptor);
//        if (overriddenDescriptor != null) {
//            return overriddenDescriptor;
//        }
//        else {
//            return functionDescriptor;
//        }
//    }
}
