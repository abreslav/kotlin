/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class LazySubstitutingClassDescriptor implements ClassDescriptor {

    private final ClassDescriptor original;
    private final TypeSubstitutor originalSubstitutor;
    private TypeSubstitutor newSubstitutor;
    private List<TypeParameterDescriptor> typeParameters;
    private TypeConstructor typeConstructor;
    private JetType superclassType;

    public LazySubstitutingClassDescriptor(ClassDescriptor descriptor, TypeSubstitutor substitutor) {
        this.original = descriptor;
        this.originalSubstitutor = substitutor;
    }

    private TypeSubstitutor getSubstitutor() {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty()) {
                newSubstitutor = originalSubstitutor;
            }
            else {
                typeParameters = Lists.newArrayList();
                newSubstitutor = DescriptorSubstitutor.substituteTypeParameters(original.getTypeConstructor().getParameters(), originalSubstitutor, this, typeParameters);
            }
        }
        return newSubstitutor;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        TypeConstructor originalTypeConstructor = original.getTypeConstructor();
        if (originalSubstitutor.isEmpty()) {
            return originalTypeConstructor;
        }

        if (typeConstructor == null) {
            TypeSubstitutor substitutor = getSubstitutor();

            Collection<JetType> supertypes = Lists.newArrayList();
            for (JetType supertype : originalTypeConstructor.getSupertypes()) {
                supertypes.add(substitutor.substitute(supertype, Variance.INVARIANT));
            }

            typeConstructor = new TypeConstructorImpl(
                    this,
                    originalTypeConstructor.getAnnotations(),
                    originalTypeConstructor.isSealed(),
                    originalTypeConstructor.toString(),
                    typeParameters,
                    supertypes
            );
        }

        return typeConstructor;
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        JetScope memberScope = original.getMemberScope(typeArguments);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        Set<ConstructorDescriptor> r = Sets.newHashSet();
        for (ConstructorDescriptor constructor : original.getConstructors()) {
            r.add((ConstructorDescriptor) constructor.substitute(getSubstitutor()));
        }
        return r;
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return original.getUnsubstitutedPrimaryConstructor();
    }

    @Override
    public boolean hasConstructors() {
        return original.hasConstructors();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return original.getAnnotations();
    }

    @NotNull
    @Override
    public String getName() {
        return original.getName();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return original.getOriginal();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return original.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;
        return new LazySubstitutingClassDescriptor(this, TypeSubstitutor.create(substitutor.getSubstitution(), getSubstitutor().getSubstitution()));
    }

    @Override
    public JetType getClassObjectType() {
        return original.getClassObjectType();
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return original.getClassObjectDescriptor();
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return original.getKind();
    }

    @Override
    @NotNull
    public Modality getModality() {
        return original.getModality();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return original.getVisibility();
    }

    @Override
    public boolean isClassObjectAValue() {
        return original.isClassObjectAValue();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassDescriptor getInnerClassOrObject(String name) {
        return original.getInnerClassOrObject(name);
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getInnerClassesAndObjects() {
        return original.getInnerClassesAndObjects();
    }
}
