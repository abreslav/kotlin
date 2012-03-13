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

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadingConflictResolver;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

public class Injector {

    private TopDownAnalyzer topDownAnalyzer;
    private BodyResolver bodyResolver;
    private ControlFlowAnalyzer controlFlowAnalyzer;
    private DeclarationsChecker declarationsChecker;
    private DescriptorResolver descriptorResolver;
    private ExpressionTypingServices expressionTypingServices;
    private TypeResolver typeResolver;
    private CallResolver callResolver;
    private JetStandardLibrary jetStandardLibrary;
    private final Project project;
    private final TopDownAnalysisContext topDownAnalysisContext;
    private final ModuleConfiguration moduleConfiguration;
    private final JetControlFlowDataTraceFactory jetControlFlowDataTraceFactory;
    private final boolean analyzingBootstrapLibrary;
    private DeclarationResolver declarationResolver;
    private AnnotationResolver annotationResolver;
    private ImportsResolver importsResolver;
    private TypeHierarchyResolver typeHierarchyResolver;
    private DelegationResolver delegationResolver;
    private OverrideResolver overrideResolver;
    private OverloadResolver overloadResolver;
    private OverloadingConflictResolver overloadingConflictResolver;

    public Injector(
        Project project,
        TopDownAnalysisContext topDownAnalysisContext,
        ModuleConfiguration moduleConfiguration,
        JetControlFlowDataTraceFactory jetControlFlowDataTraceFactory,
        boolean analyzingBootstrapLibrary
    ) {
        this.project = project;
        this.topDownAnalysisContext = topDownAnalysisContext;
        this.moduleConfiguration = moduleConfiguration;
        this.jetControlFlowDataTraceFactory = jetControlFlowDataTraceFactory;
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
    }

    public TopDownAnalyzer getTopDownAnalyzer() {
        if (this.topDownAnalyzer == null) {
            this.topDownAnalyzer = new TopDownAnalyzer();
            this.topDownAnalyzer.setDeclarationResolver(getDeclarationResolver());
            this.topDownAnalyzer.setTypeHierarchyResolver(getTypeHierarchyResolver());
            this.topDownAnalyzer.setDelegationResolver(getDelegationResolver());
            this.topDownAnalyzer.setOverrideResolver(getOverrideResolver());
            this.topDownAnalyzer.setOverloadResolver(getOverloadResolver());
        }
        return this.topDownAnalyzer;
    }

    public BodyResolver getBodyResolver() {
        if (this.bodyResolver == null) {
            this.bodyResolver = new BodyResolver();
            this.bodyResolver.setContext(getTopDownAnalysisContext());
            this.bodyResolver.setDescriptorResolver(getDescriptorResolver());
            this.bodyResolver.setExpressionTypingServices(getExpressionTypingServices());
            this.bodyResolver.setCallResolver(getCallResolver());
        }
        return this.bodyResolver;
    }

    public ControlFlowAnalyzer getControlFlowAnalyzer() {
        if (this.controlFlowAnalyzer == null) {
            this.controlFlowAnalyzer = new ControlFlowAnalyzer();
            this.controlFlowAnalyzer.setContext(getTopDownAnalysisContext());
            this.controlFlowAnalyzer.setFlowDataTraceFactory(getJetControlFlowDataTraceFactory());
        }
        return this.controlFlowAnalyzer;
    }

    public DeclarationsChecker getDeclarationsChecker() {
        if (this.declarationsChecker == null) {
            this.declarationsChecker = new DeclarationsChecker();
            this.declarationsChecker.setContext(getTopDownAnalysisContext());
        }
        return this.declarationsChecker;
    }

    public DescriptorResolver getDescriptorResolver() {
        if (this.descriptorResolver == null) {
            this.descriptorResolver = new DescriptorResolver();
            this.descriptorResolver.setExpressionTypingServices(getExpressionTypingServices());
            this.descriptorResolver.setTypeResolver(getTypeResolver());
            this.descriptorResolver.setAnnotationResolver(getAnnotationResolver());
        }
        return this.descriptorResolver;
    }

    public ExpressionTypingServices getExpressionTypingServices() {
        if (this.expressionTypingServices == null) {
            this.expressionTypingServices = new ExpressionTypingServices();
            this.expressionTypingServices.setDescriptorResolver(getDescriptorResolver());
            this.expressionTypingServices.setCallResolver(getCallResolver());
            this.expressionTypingServices.setTypeResolver(getTypeResolver());
            this.expressionTypingServices.setProject(getProject());
        }
        return this.expressionTypingServices;
    }

    public TypeResolver getTypeResolver() {
        if (this.typeResolver == null) {
            this.typeResolver = new TypeResolver();
            this.typeResolver.setDescriptorResolver(getDescriptorResolver());
            this.typeResolver.setAnnotationResolver(getAnnotationResolver());
        }
        return this.typeResolver;
    }

    public CallResolver getCallResolver() {
        if (this.callResolver == null) {
            this.callResolver = new CallResolver();
            this.callResolver.setDescriptorResolver(getDescriptorResolver());
            this.callResolver.setExpressionTypingServices(getExpressionTypingServices());
            this.callResolver.setTypeResolver(getTypeResolver());
            this.callResolver.setOverloadingConflictResolver(getOverloadingConflictResolver());
        }
        return this.callResolver;
    }

    public JetStandardLibrary getJetStandardLibrary() {
        if (this.jetStandardLibrary == null) {
            this.jetStandardLibrary = analyzingBootstrapLibrary ? null : JetStandardLibrary.getInstance();
        }
        return this.jetStandardLibrary;
    }

    public Project getProject() {
        return this.project;
    }

    public TopDownAnalysisContext getTopDownAnalysisContext() {
        return this.topDownAnalysisContext;
    }

    private ModuleConfiguration getModuleConfiguration() {
        return this.moduleConfiguration;
    }

    private JetControlFlowDataTraceFactory getJetControlFlowDataTraceFactory() {
        return this.jetControlFlowDataTraceFactory;
    }

    private boolean isAnalyzingBootstrapLibrary() {
        return this.analyzingBootstrapLibrary;
    }

    private DeclarationResolver getDeclarationResolver() {
        if (this.declarationResolver == null) {
            this.declarationResolver = new DeclarationResolver();
            this.declarationResolver.setContext(getTopDownAnalysisContext());
            this.declarationResolver.setDescriptorResolver(getDescriptorResolver());
            this.declarationResolver.setAnnotationResolver(getAnnotationResolver());
            this.declarationResolver.setImportsResolver(getImportsResolver());
        }
        return this.declarationResolver;
    }

    private AnnotationResolver getAnnotationResolver() {
        if (this.annotationResolver == null) {
            this.annotationResolver = new AnnotationResolver();
            this.annotationResolver.setExpressionTypingServices(getExpressionTypingServices());
            this.annotationResolver.setCallResolver(getCallResolver());
        }
        return this.annotationResolver;
    }

    private ImportsResolver getImportsResolver() {
        if (this.importsResolver == null) {
            this.importsResolver = new ImportsResolver();
            this.importsResolver.setContext(getTopDownAnalysisContext());
            this.importsResolver.setConfiguration(getModuleConfiguration());
        }
        return this.importsResolver;
    }

    private TypeHierarchyResolver getTypeHierarchyResolver() {
        if (this.typeHierarchyResolver == null) {
            this.typeHierarchyResolver = new TypeHierarchyResolver();
            this.typeHierarchyResolver.setContext(getTopDownAnalysisContext());
            this.typeHierarchyResolver.setDescriptorResolver(getDescriptorResolver());
            this.typeHierarchyResolver.setImportsResolver(getImportsResolver());
            this.typeHierarchyResolver.setConfiguration(getModuleConfiguration());
        }
        return this.typeHierarchyResolver;
    }

    private DelegationResolver getDelegationResolver() {
        if (this.delegationResolver == null) {
            this.delegationResolver = new DelegationResolver();
            this.delegationResolver.setContext(getTopDownAnalysisContext());
        }
        return this.delegationResolver;
    }

    private OverrideResolver getOverrideResolver() {
        if (this.overrideResolver == null) {
            this.overrideResolver = new OverrideResolver();
            this.overrideResolver.setContext(getTopDownAnalysisContext());
        }
        return this.overrideResolver;
    }

    private OverloadResolver getOverloadResolver() {
        if (this.overloadResolver == null) {
            this.overloadResolver = new OverloadResolver();
            this.overloadResolver.setContext(getTopDownAnalysisContext());
        }
        return this.overloadResolver;
    }

    private OverloadingConflictResolver getOverloadingConflictResolver() {
        if (this.overloadingConflictResolver == null) {
            this.overloadingConflictResolver = new OverloadingConflictResolver();
        }
        return this.overloadingConflictResolver;
    }


    private static <T> T make(Class<T> theClass) {
        try {                                     
            return theClass.newInstance();        
        }                                         
        catch (InstantiationException e) {        
            throw new IllegalStateException(e);   
        }                                         
        catch (IllegalAccessException e) {        
            throw new IllegalStateException(e);   
        }                                         
    }                                             
}
