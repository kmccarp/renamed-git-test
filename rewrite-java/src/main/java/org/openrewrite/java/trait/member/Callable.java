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
package org.openrewrite.java.trait.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.trait.Element;
import org.openrewrite.java.trait.variable.Parameter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;

/**
 * A method or constructor.
 */
public interface Callable extends Element {
    @Nullable
    JavaType getReturnType();

    List<Parameter> getParameters();

    static Validated of(Cursor cursor) {
        if (cursor.getValue() instanceof J.MethodDeclaration) {
            return Validated.valid("Callable", new MethodDeclarationCallable(
                    cursor,
                    cursor.getValue()
            ));
        }
        return Validated
                .invalid(
                        "cursor",
                        cursor,
                        "Callable must be of type " + J.MethodDeclaration.class +
                                " but was " + cursor.getValue().getClass()
                );
    }
}

@AllArgsConstructor
class MethodDeclarationCallable implements Callable {
    @Getter
    Cursor cursor;

    J.MethodDeclaration methodDeclaration;

    @Getter(lazy = true, onMethod = @__(@Override))
    private final List<Parameter> parameters =
            methodDeclaration
                    .getParametersAsVariableDeclarations()
                    .stream()
                    .flatMap(parameter -> {
                        Cursor parmeterCursor = new Cursor(cursor, parameter);
                        return parameter
                                .getVariables()
                                .stream()
                                .flatMap(namedVariable -> stream(
                                        Parameter.of(new Cursor(parmeterCursor, namedVariable)).spliterator(),
                                        false
                                ))
                                .map(validatedParameter -> (Parameter) validatedParameter.getValueNonNullOrThrow());
                    })
                    .collect(Collectors.toList());

    @Override
    public String getName() {
        return methodDeclaration.getSimpleName();
    }

    @Override
    public JavaType getReturnType() {
        if (methodDeclaration.getReturnTypeExpression() == null) {
            return JavaType.Primitive.Void;
        }
        return methodDeclaration.getReturnTypeExpression().getType();
    }

    @Override
    public boolean equals(Object obj) {
        return Element.equals(this, obj);
    }
}
