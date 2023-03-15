/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.trait.variable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.Validated;
import org.openrewrite.java.trait.Element;
import org.openrewrite.java.trait.member.Callable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import static java.util.Objects.requireNonNull;

/**
 * Represents a parameter in a {@link org.openrewrite.java.tree.J.MethodDeclaration} or
 * {@link org.openrewrite.java.tree.J.NewClass}.
 */
public interface Parameter extends LocalScopeVariable {

    J.VariableDeclarations.NamedVariable getNamedVariable();

    /**
     * The zero indexed position of the parameter in the method declaration.
     */
    int getPosition();

    boolean isVarArgs();

    static Validated of(Cursor c) {
        if (c.getValue() instanceof J.VariableDeclarations.NamedVariable) {
            Cursor variableDeclarationsCursor = c.getParentTreeCursor();
            Validated validatedCallable = Callable.of(variableDeclarationsCursor.getParentTreeCursor());
            if (validatedCallable.isInvalid()) {
                return validatedCallable;
            }
            return Validated.valid("Parameter ", new ParameterImpl(
                    c,
                    c.getValue(),
                    requireNonNull(validatedCallable.getValue(), "Callable can not be null")
            ));
        }
        return Validated
                .invalid("cursor", c, "Parameter can not be of type: " + c.getValue().getClass());
    }

    @AllArgsConstructor
    class ParameterImpl implements Parameter {
        @Getter(onMethod = @__(@Override))
        Cursor cursor;
        @Getter(onMethod = @__(@Override))
        J.VariableDeclarations.NamedVariable namedVariable;
        @Getter(onMethod = @__(@Override))
        Callable callable;


        @Override
        public String getName() {
            return namedVariable.getSimpleName();
        }

        @Override
        public int getPosition() {
            return callable.getParameters().indexOf(this);
        }

        @Override
        public boolean isVarArgs() {
            if (this.namedVariable.getVariableType() == null) {
                throw new IllegalStateException("Variable type is null for " + this.namedVariable);
            }
            return this.namedVariable.getVariableType().hasFlags(Flag.Varargs);
        }

        @Override
        public boolean equals(Object obj) {
            return Element.equals(this, obj);
        }

    }
}
