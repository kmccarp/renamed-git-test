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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeNamespaceValue extends Recipe {
    private static final String XMLNS_PREFIX = "xmlns";
    private static final String VERSION_PREFIX = "version";
    private static final String SCHEMA_LOCATION_MATCH_PATTERN = "(?m)(.*)(%s)(\\s+)(.*)";
    private static final String SCHEMA_LOCATION_REPLACEMENT_PATTERN = "$1%s$3%s";
    private static final String MSG_TAG_UPDATED = "msg-tag-updated";

    @Override
    public String getDisplayName() {
        return "Change XML attribute of a specific resource version";
    }

    @Override
    public String getDescription() {
        return "Alters XML Attribute value within specified element of a specific resource versions.";
    }

    @Nullable
    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath Expression.",
            example = "property",
            required = false)
    String elementName;

    @Nullable
    @Option(displayName = "Old value",
            description = "Only change the property value if it matches the configured `oldValue`.",
            example = "newfoo.bar.attribute.value.string",
            required = false)
    String oldValue;

    @Option(displayName = "New value",
            description = "The new value to be used for the namespace.",
            example = "newfoo.bar.attribute.value.string")
    String newValue;

    @Nullable
    @Option(displayName = "Resource version",
            description = "The version of resource to change",
            example = "1.1",
            required = false)
    String versionMatcher;

    @Nullable
    @Option(displayName = "Search all namespaces",
            description = "Specify whether evaluate all namespaces. Defaults to true",
            example = "true",
            required = false)
    Boolean searchAllNamespaces;

    @Nullable
    @Option(displayName = "New Resource version",
            description = "The new version of the resource",
            example = "2.0")
    String newVersion;

    @Option(displayName = "Schema location",
            description = "The new value to be used for the namespace schema location.",
            example = "newfoo.bar.attribute.value.string",
            required = false)
    @Nullable
    String newSchemaLocation;

    public static final String XML_SCHEMA_INSTANCE_PREFIX = "xsi";
    public static final String XML_SCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Find the tag that contains the declaration of the {@link #XML_SCHEMA_INSTANCE_URI} namespace.
     *
     * @param cursor the cursor to search from
     * @return the tag that contains the declaration of the given namespace URI.
     */
    public static Xml.Tag findTagContainingXmlSchemaInstanceNamespace(Cursor cursor) {
        while (cursor != null) {
            if (cursor.getValue() instanceof Xml.Document) {
                return ((Xml.Document) cursor.getValue()).getRoot();
            }
            Xml.Tag tag = cursor.firstEnclosing(Xml.Tag.class);
            if (tag != null) {
                if (tag.getNamespaces().containsValue(XML_SCHEMA_INSTANCE_URI)) {
                    return tag;
                }
            }
            cursor = cursor.getParent();
        }

        // Should never happen
        throw new IllegalArgumentException("Could not find tag containing namespace '" + XML_SCHEMA_INSTANCE_URI + "' or the enclosing Xml.Document instance.");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher elementNameMatcher = elementName != null ? new XPathMatcher(elementName) : null;
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (ctx.pollMessage(MSG_TAG_UPDATED, false)) {
                    d = d.withRoot(addOrUpdateSchemaLocation(d.getRoot(), getCursor()));
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (matchesElementName(getCursor()) && matchesVersion(t)) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::maybeReplaceNamespaceAttribute));
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::maybeReplaceVersionAttribute));
                    ctx.putMessage(MSG_TAG_UPDATED, true);
                }

                return t;
            }

            private boolean matchesElementName(Cursor cursor) {
                return elementNameMatcher == null || elementNameMatcher.matches(cursor);
            }

            private boolean matchesVersion(Xml.Tag tag) {
                if (versionMatcher == null) {
                    return true;
                }
                for (Xml.Attribute attribute : tag.getAttributes()) {
                    if (isVersionAttribute(attribute) && isVersionMatch(attribute)) {
                        return true;
                    }
                }
                return false;
            }

            private Xml.Attribute maybeReplaceNamespaceAttribute(Xml.Attribute attribute) {
                if (isXmlnsAttribute(attribute) && isOldValue(attribute)) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    newValue));
                }
                return attribute;
            }

            private Xml.Attribute maybeReplaceVersionAttribute(Xml.Attribute attribute) {
                if (isVersionAttribute(attribute) && newVersion != null) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    newVersion));
                }
                return attribute;
            }

            private boolean isXmlnsAttribute(Xml.Attribute attribute) {
                boolean searchAll = searchAllNamespaces == null || Boolean.TRUE.equals(searchAllNamespaces);
                return searchAll && attribute.getKeyAsString().startsWith(XMLNS_PREFIX) ||
                       !searchAll && attribute.getKeyAsString().equals(XMLNS_PREFIX);
            }

            private boolean isVersionAttribute(Xml.Attribute attribute) {
                return attribute.getKeyAsString().startsWith(VERSION_PREFIX);
            }

            private boolean isOldValue(Xml.Attribute attribute) {
                return oldValue == null || attribute.getValueAsString().equals(oldValue);
            }

            private boolean isVersionMatch(Xml.Attribute attribute) {
                if (versionMatcher == null) {
                    return true;
                }
                String[] versions = versionMatcher.split(",");
                double dversion = Double.parseDouble(attribute.getValueAsString());
                for (String splitVersion : versions) {
                    boolean checkGreaterThan = false;
                    double dversionExpected;
                    if (splitVersion.endsWith("+")) {
                        splitVersion = splitVersion.substring(0, splitVersion.length() - 1);
                        checkGreaterThan = true;
                    }
                    try {
                        dversionExpected = Double.parseDouble(splitVersion);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    if (!checkGreaterThan && dversionExpected == dversion || checkGreaterThan && dversionExpected <= dversion) {
                        return true;
                    }
                }
                return false;
            }

            private Xml.Tag addOrUpdateSchemaLocation(Xml.Tag root, Cursor cursor) {
                if (StringUtils.isBlank(newSchemaLocation)) {
                    return root;
                }
                Xml.Tag newRoot = maybeAddNamespace(root);
                Optional<Xml.Attribute> maybeSchemaLocation = maybeGetSchemaLocation(cursor, newRoot);
                if (maybeSchemaLocation.isPresent() && oldValue != null) {
                    newRoot = updateSchemaLocation(newRoot, maybeSchemaLocation.get());
                } else if (maybeSchemaLocation.isEmpty()) {
                    newRoot = addSchemaLocation(newRoot);
                }
                return newRoot;
            }

            private Optional<Xml.Attribute> maybeGetSchemaLocation(Cursor cursor, Xml.Tag tag) {
                Xml.Tag schemaLocationTag = findTagContainingXmlSchemaInstanceNamespace(cursor);
                Map<String, String> namespaces = tag.getNamespaces();
                for (Xml.Attribute attribute : schemaLocationTag.getAttributes()) {
                    String attributeNamespace = namespaces.get(Xml.extractNamespacePrefix(attribute.getKeyAsString()));
                    if(XML_SCHEMA_INSTANCE_URI.equals(attributeNamespace)
                       && attribute.getKeyAsString().endsWith("schemaLocation")) {
                        return Optional.of(attribute);
                    }
                }

                return Optional.empty();
            }

            private Xml.Tag maybeAddNamespace(Xml.Tag root) {
                Map<String, String> namespaces = root.getNamespaces();
                if (namespaces.containsValue(newValue) && !namespaces.containsValue(XML_SCHEMA_INSTANCE_URI)) {
                    namespaces.put(XML_SCHEMA_INSTANCE_PREFIX, XML_SCHEMA_INSTANCE_URI);
                    root = root.withNamespaces(namespaces);
                }
                return root;
            }

            private Xml.Tag updateSchemaLocation(Xml.Tag newRoot, Xml.Attribute attribute) {
                if(oldValue == null) {
                    return newRoot;
                }
                String oldSchemaLocation = attribute.getValueAsString();
                Matcher pattern = Pattern.compile(SCHEMA_LOCATION_MATCH_PATTERN.formatted(Pattern.quote(oldValue)))
                        .matcher(oldSchemaLocation);
                if (pattern.find()) {
                    String newSchemaLocationValue = pattern.replaceFirst(
                            SCHEMA_LOCATION_REPLACEMENT_PATTERN.formatted(newValue, newSchemaLocation)
                    );
                    Xml.Attribute newAttribute = attribute.withValue(attribute.getValue().withValue(newSchemaLocationValue));
                    newRoot = newRoot.withAttributes(ListUtils.map(newRoot.getAttributes(), a -> a == attribute ? newAttribute : a));
                }
                return newRoot;
            }

            private Xml.Tag addSchemaLocation(Xml.Tag newRoot) {
                return newRoot.withAttributes(
                        ListUtils.concat(
                                newRoot.getAttributes(),
                                new Xml.Attribute(
                                        randomId(),
                                        " ",
                                        Markers.EMPTY,
                                        new Xml.Ident(
                                                randomId(),
                                                "",
                                                Markers.EMPTY,
                                                "%s:schemaLocation".formatted(XML_SCHEMA_INSTANCE_PREFIX)
                                        ),
                                        "",
                                        new Xml.Attribute.Value(
                                                randomId(),
                                                "",
                                                Markers.EMPTY,
                                                Xml.Attribute.Value.Quote.Double,
                                                "%s %s".formatted(newValue, newSchemaLocation)
                                        )
                                )
                        )
                );
            }
        };
    }
}
