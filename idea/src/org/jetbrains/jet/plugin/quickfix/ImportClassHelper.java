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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.List;

/**
 * @author svtk
 */
public class ImportClassHelper {
    /**
     * Add import directive corresponding to a type to file when it is needed.
     *
     * @param type type to import
     * @param file file where import directive should be added
     */
    public static void addImportDirectiveIfNeeded(@NotNull JetType type, @NotNull JetFile file) {
        if (JetPluginUtil.checkTypeIsStandard(type, file.getProject()) || ErrorUtils.isErrorType(type)) {
            return;
        }
        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFileWithCache(file, AnalyzerFacadeForJVM.SINGLE_DECLARATION_PROVIDER);
        PsiElement element = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, type.getMemberScope().getContainingDeclaration());
        if (element != null && element.getContainingFile() == file) { //declaration is in the same file, so no import is needed
            return;
        }
        addImportDirective(JetPluginUtil.computeTypeFullName(type), file);
    }

    /**
     * Add import directive into the PSI tree for the given namespace.
     *
     * @param importString full name of the import. Can contain .* if necessary.
     * @param file File where directive should be added.
     */
    public static void addImportDirective(@NotNull String importString, @NotNull JetFile file) {

        // TODO: hack
        if (importString.startsWith(JavaDescriptorResolver.JAVA_ROOT + ".")) {
            importString = importString.substring((JavaDescriptorResolver.JAVA_ROOT + ".").length());
        }

        if (isImportedByDefault(importString, JetPsiUtil.getFQName(file))) {
            return;
        }

        List<JetImportDirective> importDirectives = file.getImportDirectives();

        JetImportDirective newDirective = JetPsiFactory.createImportDirective(file.getProject(), importString);

        if (!importDirectives.isEmpty()) {
            
            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                String importPath = JetPsiUtil.getImportPath(directive);
                if (importPath != null && QualifiedNamesUtil.isImported(importPath, importString)) {
                    return;
                }
            }

            JetImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
            lastDirective.getParent().addAfter(newDirective, lastDirective);
        }
        else {
            List<JetDeclaration> declarations = file.getDeclarations();

            if (!declarations.isEmpty()) {
                JetDeclaration firstDeclaration = declarations.iterator().next();
                firstDeclaration.getParent().addBefore(newDirective, firstDeclaration);
            }
            else {
                file.getNamespaceHeader().getParent().addAfter(newDirective, file.getNamespaceHeader());
            }
        }
    }

    // Check that import is useless
    private static boolean isImportedByDefault(@NotNull String importString, @NotNull String filePackageFqn) {
        if (QualifiedNamesUtil.isOneSegmentFQN(importString) ||
            filePackageFqn.equals(QualifiedNamesUtil.withoutLastSegment(importString))) {

            return true;
        }

        for (String defaultJetImport : DefaultModuleConfiguration.DEFAULT_JET_IMPORTS) {
            if (QualifiedNamesUtil.isImported(defaultJetImport, importString)) {
                return true;
            }
        }

        for (String defaultJavaImport : JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS) {
            if (QualifiedNamesUtil.isImported(defaultJavaImport + ".*", importString)) {
                return true;
            }
        }
        return false;
    }
}
