/*
 * Copyright 2016 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.ufpr.gres;

import br.ufpr.gres.core.ConcreteBlockCounter;
import br.ufpr.gres.core.MutationInfo;
import br.ufpr.gres.core.MutationIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class ClassContext {

    private ClassInfo classInfo;
    private String sourceFile;

    public final List<MutationIdentifier> target = new ArrayList<>();
    public final List<MutationInfo> mutations = new ArrayList<>();

    private final ConcreteBlockCounter blockCounter = new ConcreteBlockCounter();

    public ClassInfo getClassInfo() {
        return this.classInfo;
    }

    public String getJavaClassName() {
        return this.classInfo.getName().replace("/", ".");
    }

    public String getFileName() {
        return this.sourceFile;
    }

    public void setTargetMutation(final MutationIdentifier target) {
        this.target.add(target);
    }
    
    public void setTargetMutation(final List<MutationIdentifier> target) {
        this.target.addAll(target);
    }

    public List<MutationInfo> getMutationDetails(final MutationIdentifier id) {
        return this.mutations.stream().filter(p -> p.matchesId(id)).collect(Collectors.toList());
    }

    public void registerClass(final ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public void registerSourceFile(final String source) {
        this.sourceFile = source;
    }

    public boolean shouldMutate(final MutationIdentifier newId) {
        return this.target.isEmpty() || this.target.stream().anyMatch(p -> p.matches(newId));
    }

    public List<MutationInfo> getCollectedMutations() {
        return this.mutations;
    }

    public void add(final MutationInfo details) {
        this.mutations.add(details);
    }

    public void registerNewBlock() {
        this.blockCounter.registerNewBlock();

    }

    public void registerFinallyBlockStart() {
        this.blockCounter.registerFinallyBlockStart();
    }

    public void registerFinallyBlockEnd() {
        this.blockCounter.registerFinallyBlockEnd();
    }

    public int getCurrentBlock() {
        return this.blockCounter.getCurrentBlock();
    }

    public boolean isWithinFinallyBlock() {
        return this.blockCounter.isWithinFinallyBlock();
    }
}
