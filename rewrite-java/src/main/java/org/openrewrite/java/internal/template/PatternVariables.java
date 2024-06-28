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
package org.openrewrite.java.internal.template;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Loop;
import org.openrewrite.java.tree.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PatternVariables {

    private static final String DEFAULT_LABEL = "!";

    private static class ResultCollector {
        final StringBuilder builder = new StringBuilder();
        boolean instanceOfFound;
    }

    @Nullable
    static String simplifiedPatternVariableCondition(Expression condition, @Nullable J toReplace) {
        ResultCollector resultCollector = new ResultCollector();
        simplifiedPatternVariableCondition0(condition, toReplace, resultCollector);
        return resultCollector.instanceOfFound ? resultCollector.builder.toString() : null;
    }

    private static boolean simplifiedPatternVariableCondition0(J expr, @Nullable J toReplace, ResultCollector collector) {
        if (expr == toReplace) {
            collector.builder.append('§');
            return true;
        }

        if (expr instanceof J.Parentheses<?> parens) {
            collector.builder.append('(');
            try {
                return simplifiedPatternVariableCondition0(parens.getTree(), toReplace, collector);
            } finally {
                collector.builder.append(')');
            }
        } else if (expr instanceof J.Unary unary) {
            switch (unary.getOperator()) {
                case PostIncrement: {
                    boolean found = simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
                    collector.builder.append("++");
                    return found;
                }
                case PostDecrement: {
                    boolean found = simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
                    collector.builder.append("--");
                    return found;
                }
                case PreIncrement:
                    collector.builder.append("++");
                    break;
                case PreDecrement:
                    collector.builder.append("--");
                    break;
                case Positive:
                    collector.builder.append('+');
                    break;
                case Negative:
                    collector.builder.append('-');
                    break;
                case Complement:
                    collector.builder.append('~');
                    break;
                case Not:
                    collector.builder.append('!');
                    break;
                default:
                    throw new IllegalStateException("Unexpected unary operator: " + unary.getOperator());
            }
            return simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
        } else if (expr instanceof J.Binary binary) {
            int length = collector.builder.length();
            boolean result = simplifiedPatternVariableCondition0(binary.getLeft(), toReplace, collector);
            switch (binary.getOperator()) {
                case Addition:
                    collector.builder.append('+');
                    break;
                case Subtraction:
                    collector.builder.append('-');
                    break;
                case Multiplication:
                    collector.builder.append('*');
                    break;
                case Division:
                    collector.builder.append('/');
                    break;
                case Modulo:
                    collector.builder.append('%');
                    break;
                case LessThan:
                    collector.builder.append('<');
                    break;
                case GreaterThan:
                    collector.builder.append('>');
                    break;
                case LessThanOrEqual:
                    collector.builder.append("<=");
                    break;
                case GreaterThanOrEqual:
                    collector.builder.append(">=");
                    break;
                case Equal:
                    collector.builder.append("==");
                    break;
                case NotEqual:
                    collector.builder.append("!=");
                    break;
                case BitAnd:
                    collector.builder.append('&');
                    break;
                case BitOr:
                    collector.builder.append('|');
                    break;
                case BitXor:
                    collector.builder.append('^');
                    break;
                case LeftShift:
                    collector.builder.append("<<");
                    break;
                case RightShift:
                    collector.builder.append(">>");
                    break;
                case UnsignedRightShift:
                    collector.builder.append(">>>");
                    break;
                case Or:
                    collector.builder.append("||");
                    break;
                case And:
                    collector.builder.append("&&");
                    break;
                default:
                    throw new IllegalStateException("Unexpected binary operator: " + binary.getOperator());
            }
            result |= simplifiedPatternVariableCondition0(binary.getRight(), toReplace, collector);
            if (!result) {
                switch (binary.getOperator()) {
                    case LessThan:
                    case GreaterThan:
                    case LessThanOrEqual:
                    case GreaterThanOrEqual:
                    case Equal:
                    case NotEqual:
                    case Or:
                    case And:
                        collector.builder.setLength(length);
                        collector.builder.append("true");
                        return false;
                }
            }
            return result;
        } else if (expr instanceof J.InstanceOf instanceOf) {
            if (instanceOf.getPattern() != null) {
                collector.builder.append("((Object)null) instanceof ").append(instanceOf.getClazz()).append(' ').append(instanceOf.getPattern());
                collector.instanceOfFound = true;
                return true;
            }
            collector.builder.append("true");
        } else if (expr instanceof J.Literal literal) {
            collector.builder.append(literal.getValue());
        } else if (expr instanceof Expression) {
            collector.builder.append("null");
        }
        return false;
    }

    static boolean neverCompletesNormally(Statement statement) {
        return neverCompletesNormally0(statement, new HashSet<>());
    }

    private static boolean neverCompletesNormally0(@Nullable Statement statement, Set<String> labelsToIgnore) {
        if (statement instanceof J.Return || statement instanceof J.Throw) {
            return true;
        } else if (statement instanceof J.Break breakStatement) {
            return breakStatement.getLabel() != null && !labelsToIgnore.contains(breakStatement.getLabel().getSimpleName())
                    || breakStatement.getLabel() == null && !labelsToIgnore.contains(DEFAULT_LABEL);
        } else if (statement instanceof J.Continue continueStatement) {
            return continueStatement.getLabel() != null && !labelsToIgnore.contains(continueStatement.getLabel().getSimpleName())
                    || continueStatement.getLabel() == null && !labelsToIgnore.contains(DEFAULT_LABEL);
        } else if (statement instanceof J.Block) {
            return neverCompletesNormally0(getLastStatement(statement), labelsToIgnore);
        } else if (statement instanceof Loop loop) {
            return neverCompletesNormallyIgnoringLabel(loop.getBody(), DEFAULT_LABEL, labelsToIgnore);
        } else if (statement instanceof J.If if_) {
            return if_.getElsePart() != null
                    && neverCompletesNormally0(if_.getThenPart(), labelsToIgnore)
                    && neverCompletesNormally0(if_.getElsePart().getBody(), labelsToIgnore);
        } else if (statement instanceof J.Switch switch_) {
            if (switch_.getCases().getStatements().isEmpty()) {
                return false;
            }
            Statement defaultCase = null;
            for (Statement case_ : switch_.getCases().getStatements()) {
                if (!neverCompletesNormallyIgnoringLabel(case_, DEFAULT_LABEL, labelsToIgnore)) {
                    return false;
                }
                if (case_ instanceof J.Case case1) {
                    Expression elem = case1.getPattern();
                    if (elem instanceof J.Identifier identifier && identifier.getSimpleName().equals("default")) {
                        defaultCase = case_;
                    }
                }
            }
            return neverCompletesNormallyIgnoringLabel(defaultCase, DEFAULT_LABEL, labelsToIgnore);
        } else if (statement instanceof J.Case case_) {
            if (case_.getStatements().isEmpty()) {
                // fallthrough to next case
                return true;
            }
            return neverCompletesNormally0(getLastStatement(case_), labelsToIgnore);
        } else if (statement instanceof J.Try try_) {
            if (try_.getFinally() != null && !try_.getFinally().getStatements().isEmpty()
                    && neverCompletesNormally0(try_.getFinally(), labelsToIgnore)) {
                return true;
            }
            boolean bodyHasExit = false;
            if (!try_.getBody().getStatements().isEmpty()
                    && !(bodyHasExit = neverCompletesNormally0(try_.getBody(), labelsToIgnore))) {
                return false;
            }
            for (J.Try.Catch catch_ : try_.getCatches()) {
                if (!neverCompletesNormally0(catch_.getBody(), labelsToIgnore)) {
                    return false;
                }
            }
            return bodyHasExit;
        } else if (statement instanceof J.Synchronized synchronized1) {
            return neverCompletesNormally0(synchronized1.getBody(), labelsToIgnore);
        } else if (statement instanceof J.Label label1) {
            String label = label1.getLabel().getSimpleName();
            Statement labeledStatement = label1.getStatement();
            return neverCompletesNormallyIgnoringLabel(labeledStatement, label, labelsToIgnore);
        }
        return false;
    }

    private static boolean neverCompletesNormallyIgnoringLabel(@Nullable Statement statement, String label, Set<String> labelsToIgnore) {
        boolean added = labelsToIgnore.add(label);
        try {
            return neverCompletesNormally0(statement, labelsToIgnore);
        } finally {
            if (added) {
                labelsToIgnore.remove(label);
            }
        }
    }

    @Nullable
    private static Statement getLastStatement(Statement statement) {
        if (statement instanceof J.Block block) {
            List<Statement> statements = block.getStatements();
            return statements.isEmpty() ? null : getLastStatement(statements.getLast());
        } else if (statement instanceof J.Case case1) {
            List<Statement> statements = case1.getStatements();
            return statements.isEmpty() ? null : getLastStatement(statements.getLast());
        } else if (statement instanceof Loop loop) {
            return getLastStatement(loop.getBody());
        }
        return statement;
    }
}
