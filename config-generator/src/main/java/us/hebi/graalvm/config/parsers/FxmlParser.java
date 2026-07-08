/*-
 * #%L
 * Native Config Generator
 * %%
 * Copyright (C) 2026 HEBI Robotics
 * %%
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
 * #L%
 */

package us.hebi.graalvm.config.parsers;

import com.google.mu.util.Substring;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static us.hebi.graalvm.config.parsers.MugXmlParser.*;
import static com.google.common.base.Preconditions.*;

/**
 * Parser for JavaFX FXML files using Mug
 *
 * @author Florian Enner
 * @since 21 Nov 2025
 */
public class FxmlParser {

/*    public static void main(String[] args) throws Exception {
        var cfg = FxmlConfig.parseFxml(ScopePresenter.class.getResource("scope.fxml"));
        System.out.println(cfg);


//        printClasses(ScopePresenter.class.getResource("scope.fxml"));
//        if (true) return;

        try (var input = checkNotNull(ScopePresenter.class.getResourceAsStream("scope.fxml"))) {
            var content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var fxmlDoc = parseFxml(content);

            System.out.println("=== FXML Document ===");
            System.out.println("Imports: " + fxmlDoc.imports());
            System.out.println("Controller: " + fxmlDoc.controller());
            System.out.println("Root element: " + fxmlDoc.root().name());
            System.out.println("Comments: " + fxmlDoc.comments().size());

            // Print structure
            printElement(fxmlDoc.root(), 0);
        }
    }*/

    private static void printClasses(URL root) throws IOException {
        try (var input = checkNotNull(root).openStream()) {
            var fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println(">>> " + root + " <<<");

            System.out.println("> Imports:");


            System.out.println("> Controller:");
            Substring.between("fx:controller=\"", "\"")
                    .repeatedly()
                    .match(fxml)
                    .forEach(System.out::println);

            System.out.println("> Nested files:");
            Substring.between("<fx:include ", ">")
                    .repeatedly()
                    .match(fxml)
                    .forEach(element -> {
                        Substring.between("source=\"", "\"")
                                .repeatedly()
                                .match(element.toString())
                                .forEach(path -> {
                                    System.out.println(path.toString());
                                    System.out.println();
                                    try {
                                        var childUrl = root.toURI().resolve(path.toString()).toURL();
                                        System.out.println("childUrl = " + childUrl);
                                        printClasses(childUrl);
                                    } catch (Exception e) {
                                    }
                                });
                    });

        }

    }

    private static void printElement(FxmlElement element, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "<" + element.name() +
                           (element.attributes().isEmpty() ? "" : " " + element.attributes()) + ">");

        for (var child : element.children()) {
            if (child instanceof FxmlElement e) {
                printElement(e, depth + 1);
            } else if (child instanceof String text) {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    System.out.println(indent + "  [text: " + trimmed + "]");
                }
            }
        }
    }

    public static FxmlDocument parseFxml(String content) {
        // Parse imports
        List<String> imports = parseImports(content);

        // Parse comments
        List<String> comments = MugXmlParser.parseComments(content);

        // Parse processing instructions (like <?import?>)
        Map<String, String> processingInstructions = parseProcessingInstructions(content);

        // Find controller attribute in root element
        String controller = findController(content);

        // Parse the main element tree
        FxmlElement root = parseElement(content);

        return new FxmlDocument(imports, comments, processingInstructions, controller, root);
    }

    /**
     * Parse <?import package.ClassName?> statements
     */
    private static List<String> parseImports(String fxml) {
        return Substring.between("<?import ", "?>")
                .repeatedly()
                .match(fxml)
                .map(match -> match.toString().trim())
                .collect(Collectors.toList());
    }

    /**
     * Parse processing instructions <?name content?>
     */
    private static Map<String, String> parseProcessingInstructions(String fxml) {
        return Substring.between("<?", "?>")
                .repeatedly()
                .match(fxml)
                .filter(match -> !match.toString().startsWith("import")) // Skip imports
                .collect(Collectors.toMap(
                        match -> {
                            String content = match.toString().trim();
                            int spaceIdx = content.indexOf(' ');
                            return spaceIdx > 0 ? content.substring(0, spaceIdx) : content;
                        },
                        match -> {
                            String content = match.toString().trim();
                            int spaceIdx = content.indexOf(' ');
                            return spaceIdx > 0 ? content.substring(spaceIdx + 1).trim() : "";
                        },
                        (a, b) -> a // Keep first occurrence
                ));
    }

    /**
     * Find fx:controller attribute in the root element
     */
    private static String findController(String fxml) {
        return Substring.first("fx:controller=\"")
                .then(Substring.prefix('"'))
                .in(fxml)
                .map(match -> match.skip(15, 0).toString())
                .orElse(null);
    }

    /**
     * Parse a complete element including nested children
     */
    private static FxmlElement parseElement(String fxml) {
        // Remove comments and processing instructions for cleaner parsing
        String cleaned = Substring.spanningInOrder("<!--", "-->")
                .repeatedly()
                .removeAllFrom(fxml);
        cleaned = Substring.spanningInOrder("<?", "?>")
                .repeatedly()
                .removeAllFrom(cleaned);
        cleaned = cleaned.strip();

        // Find the first opening tag
        var firstTag = Substring.first('<')
                .then(Substring.suffix('>'))
                .in(cleaned)
                .orElseThrow(() -> new IllegalArgumentException("No opening tag found"));

        String tagContent = firstTag.skip(1, 0).toString().trim();

        // Check if self-closing
        if (tagContent.endsWith("/")) {
            return parseSelfClosingElement(tagContent);
        }

        // Parse opening tag
        String tagName = extractTagName(tagContent);
        Map<String, String> attributes = parseTagAttributes(tagContent, tagName);

        // Find matching closing tag
        int contentStart = firstTag.index() + firstTag.length() + 1;
        int closingTagStart = findMatchingClosingTag(cleaned, tagName, contentStart);

        if (closingTagStart < 0) {
            // No closing tag, treat as self-closing
            return new FxmlElement(tagName, attributes, List.of(), false);
        }

        // Parse content between tags
        String innerContent = cleaned.substring(contentStart, closingTagStart);
        List<Object> children = parseChildren(innerContent);

        return new FxmlElement(tagName, attributes, children, false);
    }

    /**
     * Parse self-closing element like <Button />
     */
    private static FxmlElement parseSelfClosingElement(String tagContent) {
        // Remove trailing /
        tagContent = tagContent.substring(0, tagContent.length() - 1).trim();

        String tagName = extractTagName(tagContent);
        Map<String, String> attributes = parseTagAttributes(tagContent, tagName);

        return new FxmlElement(tagName, attributes, List.of(), true);
    }

    /**
     * Extract tag name from tag content
     */
    private static String extractTagName(String tagContent) {
        return Substring.first(XML_NAME_CHAR)
                .in(tagContent)
                .map(Substring.Match::toString)
                .orElse("");
    }

    /**
     * Parse attributes from tag content
     */
    private static Map<String, String> parseTagAttributes(String tagContent, String tagName) {
        // Get everything after the tag name
        int nameEnd = tagContent.indexOf(tagName) + tagName.length();
        if (nameEnd >= tagContent.length()) {
            return Map.of();
        }

        String attrString = tagContent.substring(nameEnd).trim();
        if (attrString.isEmpty() || attrString.equals("/")) {
            return Map.of();
        }

        return parseAttributes(attrString);
    }

    /**
     * Parse attributes with better handling for fx:id, fx:controller, etc.
     */
    private static Map<String, String> parseAttributes(String attrString) {
        Map<String, String> attributes = new LinkedHashMap<>();

        // Pattern: name="value" or name='value'
        var matches = Substring.between("\"", "\"")
                .or(Substring.between("'", "'"))
                .repeatedly()
                .match(attrString)
                .toList();

        for (var match : matches) {
            // Find attribute name before the quote
            int valueStart = match.index() - 1; // Position of opening quote
            int equalsPos = valueStart - 1; // Position of =

            // Find start of attribute name
            int nameStart = equalsPos - 1;
            while (nameStart >= 0 && Character.isWhitespace(attrString.charAt(nameStart))) {
                nameStart--;
            }
            while (nameStart >= 0 && !Character.isWhitespace(attrString.charAt(nameStart))) {
                nameStart--;
            }
            nameStart++;

            if (nameStart < equalsPos) {
                String name = attrString.substring(nameStart, equalsPos).trim();
                String value = match.toString();
                attributes.put(name, value);
            }
        }

        return attributes;
    }

    /**
     * Find matching closing tag for the given tag name
     */
    private static int findMatchingClosingTag(String content, String tagName, int startPos) {
        String closingTag = "</" + tagName + ">";
        int depth = 1;
        int pos = startPos;

        while (depth > 0 && pos < content.length()) {
            // Find next tag
            int nextOpen = content.indexOf("<" + tagName, pos);
            int nextClose = content.indexOf(closingTag, pos);

            if (nextClose < 0) {
                return -1; // No closing tag found
            }

            // Check if there's an opening tag before the closing tag
            if (nextOpen > 0 && nextOpen < nextClose) {
                // Make sure it's actually an opening tag (not part of a closing tag)
                if (nextOpen + tagName.length() + 1 < content.length()) {
                    char charAfterName = content.charAt(nextOpen + tagName.length() + 1);
                    if (charAfterName == ' ' || charAfterName == '>' || charAfterName == '/') {
                        depth++;
                        pos = nextOpen + 1;
                        continue;
                    }
                }
            }

            depth--;
            if (depth == 0) {
                return nextClose;
            }
            pos = nextClose + closingTag.length();
        }

        return -1;
    }

    /**
     * Parse child elements and text content
     */
    private static List<Object> parseChildren(String content) {
        List<Object> children = new ArrayList<>();
        content = content.trim();

        if (content.isEmpty()) {
            return children;
        }

        int pos = 0;
        while (pos < content.length()) {
            // Find next tag
            int tagStart = content.indexOf('<', pos);

            if (tagStart < 0) {
                // No more tags, rest is text
                String text = content.substring(pos).trim();
                if (!text.isEmpty()) {
                    children.add(text);
                }
                break;
            }

            // Add text before tag
            if (tagStart > pos) {
                String text = content.substring(pos, tagStart).trim();
                if (!text.isEmpty()) {
                    children.add(text);
                }
            }

            // Parse element starting at tagStart
            String remaining = content.substring(tagStart);
            try {
                FxmlElement child = parseElement(remaining);
                children.add(child);

                // Calculate how far to advance
                int childEnd = findElementEnd(remaining, child);
                pos = tagStart + childEnd;
            } catch (Exception e) {
                // Skip this tag if parsing fails
                pos = tagStart + 1;
            }
        }

        return children;
    }

    /**
     * Find the end position of an element in the content
     */
    private static int findElementEnd(String content, FxmlElement element) {
        if (element.selfClosing()) {
            return content.indexOf("/>") + 2;
        } else {
            String closingTag = "</" + element.name() + ">";
            int closingPos = findMatchingClosingTag(content, element.name(), 0);
            return closingPos + closingTag.length();
        }
    }

    // ===== Data Classes =====

    public record FxmlDocument(
            List<String> imports,
            List<String> comments,
            Map<String, String> processingInstructions,
            String controller,
            FxmlElement root
    ) {
    }

    public record FxmlElement(
            String name,
            Map<String, String> attributes,
            List<Object> children, // Can be FxmlElement or String (text)
            boolean selfClosing
    ) {
        public boolean isFxInclude() {
            return name.equals("fx:include");
        }

        public boolean isFxDefine() {
            return name.equals("fx:define");
        }

        public Optional<String> getFxId() {
            return Optional.ofNullable(attributes.get("fx:id"));
        }

        public Optional<String> getStyleClass() {
            return Optional.ofNullable(attributes.get("styleClass"));
        }
    }
}
