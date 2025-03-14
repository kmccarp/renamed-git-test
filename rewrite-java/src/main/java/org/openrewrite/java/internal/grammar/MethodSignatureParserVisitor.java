/*
 * Copyright 2025 the original author or authors.
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
// Generated from ~/git/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MethodSignatureParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MethodSignatureParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParametersPattern(MethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotDot(MethodSignatureParser.DotDotContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPatternAfterDotDot(MethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalParensTypePattern(MethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
}
