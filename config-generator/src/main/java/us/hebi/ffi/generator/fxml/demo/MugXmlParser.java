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

package us.hebi.ffi.generator.fxml.demo;

/**
 * @author Florian Enner
 * @since 24 Nov 2025
 */

import com.google.mu.util.CharPredicate;
import com.google.mu.util.Substring;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MugXmlParser {

    // Define character predicates for XML
    static final CharPredicate XML_NAME_START =
            CharPredicate.range('a', 'z')
                    .or(CharPredicate.range('A', 'Z'))
                    .or(CharPredicate.is(':'))
                    .or(CharPredicate.is('_'));

    static final CharPredicate XML_NAME_CHAR =
            XML_NAME_START
                    .or(CharPredicate.range('0', '9'))
                    .or(CharPredicate.is('-'))
                    .or(CharPredicate.is('.'));

    static final CharPredicate WHITESPACE =
            CharPredicate.anyOf(" \t\r\n");

    // Parse XML element name
    public static Substring.Pattern xmlName() {
        return Substring.consecutive(XML_NAME_CHAR);
    }

    // Parse attributes - simplified version
    public static Map<String, String> parseAttributes(String attrString) {
        // Pattern to match attribute="value" pairs
        return Substring.between("\"", "\"")
                .repeatedly()
                .match(attrString.trim())
                .collect(Collectors.toMap(
                        match -> {
                            // Extract attribute name before the ="
                            int endPos = match.index() - 2; // skip ="
                            int startPos = endPos;
                            while (startPos > 0 && Character.isLetterOrDigit(attrString.charAt(startPos - 1))) {
                                startPos--;
                            }
                            return attrString.substring(startPos, endPos).trim();
                        },
                        match -> match.toString()
                ));
    }

    // Parse opening tag
    public static XmlElement parseOpenTag(String xml) {
        var tagPattern = Substring.first('<')
                .then(Substring.prefix('>'))
                .in(xml)
                .orElseThrow();

        String tagContent = tagPattern.skip(1, 0).toString().trim();

        // Split tag name and attributes
        var nameEnd = Substring.first(WHITESPACE)
                .in(tagContent)
                .map(m -> m.index())
                .orElse(tagContent.length());

        String tagName = tagContent.substring(0, nameEnd);
        String attrsString = tagContent.substring(nameEnd).trim();

        Map<String, String> attributes = attrsString.isEmpty()
                ? Map.of()
                : parseAttributes(attrsString);

        return new XmlElement(tagName, attributes, List.of());
    }

    // Parse self-closing tag
    public static boolean isSelfClosing(String tagContent) {
        return tagContent.trim().endsWith("/");
    }

    // Parse XML comments
    public static List<String> parseComments(String xml) {
        return Substring.between("<!--", "-->")
                .repeatedly()
                .match(xml)
                .map(Substring.Match::toString)
                .collect(Collectors.toList());
    }

    // Parse text content between tags
    public static String parseTextContent(String xml, int startPos) {
        return Substring.first('>')
                .then(Substring.prefix('<'))
                .in(xml.substring(startPos))
                .map(Substring.Match::toString)
                .orElse("");
    }

    // Data classes
    public record XmlElement(
            String name,
            Map<String, String> attributes,
            List<Object> children // Can be XmlElement or String (text)
    ) {
    }

    public static void main(String[] args) {
        String xml = """
                <root attr="value">
                    <child id="1">Text content</child>
                    <self-closing />
                    <!-- Comment -->
                </root>
                """;

        // Example: Extract all tag names
        List<String> tagNames = xmlName()
                .repeatedly()
                .match(xml)
                .map(Substring.Match::toString)
                .collect(Collectors.toList());

        System.out.println("Tag names: " + tagNames);

        // Example: Extract all comments
        List<String> comments = parseComments(xml);
        System.out.println("Comments: " + comments);
    }
}
