/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.javac;

import org.teavm.model.*;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.InvokeInstruction;

public class SystemExitElimination implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {    
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                transformProgram(method.getProgram());
            }
        }
    }

    private void transformProgram(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getMethod().getClassName().equals("java.lang.System")
                            && invoke.getMethod().getName().equals("exit")) {
                        EmptyInstruction emptyInstruction = new EmptyInstruction();
                        emptyInstruction.setLocation(invoke.getLocation());
                        invoke.replace(emptyInstruction);
                    }
                }
            }
        }
    }
}
