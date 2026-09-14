/*-
 * #%L
 * reachability-processor
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
package us.hebi.graalvm.reachability.processor.parsers;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
public class FxmlParser {

    public static Optional<String> tryReadContent(Path path) {
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the parsed file followed by every fxml file that it references, in discovery order
     */
    public static Set<FxmlFile> parse(Path rootDir, Path file) {
        var files = new LinkedHashMap<Path, FxmlFile>();
        addFxmlFile(rootDir, file, files);
        return new LinkedHashSet<>(files.values());
    }

    private static void addFxmlFile(Path rootDir, Path path, Map<Path, FxmlFile> files) {
        // Ignore files that we already looked at (e.g. via includes)
        if (files.containsKey(path)) {
            return;
        }

        // Ignore files that don't actually exist TODO: throw an error for e.g. a missing fx:include target?
        var contentOpt = tryReadContent(path);
        if (contentOpt.isEmpty()) {
            return;
        }

        var fxmlFile = new FxmlFile(path);
        files.put(path, fxmlFile);

        // Parse the content for a single file
        var relativeResources = new ArrayList<String>();
        try {
            parseContent(contentOpt.get(), fxmlFile, relativeResources);
        } catch (XMLStreamException malformed) {
            // Any malformed issues would fail in FXMLLoader too, so we keep whatever we found until here
        }

        // Resolve any resources that were loaded from within the content.
        // Doing it at the end avoids errors with open readers.
        for (var relativePath : relativeResources) {
            var resource = resolve(rootDir, path, relativePath);
            if (relativePath.toLowerCase().endsWith(".fxml")) {
                addFxmlFile(rootDir, resource, files);
            } else {
                fxmlFile.resources.add(resource);
            }
        }
    }

    /**
     * Parses the FXML content and adds reflectively accessed classes and properties
     */
    private static void parseContent(String content, FxmlFile file, List<String> relativeResources) throws XMLStreamException {
        var reader = createReader(content);
        var elementStack = new ArrayDeque<String>();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.PROCESSING_INSTRUCTION -> {
                    if ("import".equals(reader.getPITarget())) {
                        file.imports.add(reader.getPIData().trim());
                    }
                }
                case XMLStreamConstants.START_ELEMENT ->
                        elementStack.push(onStartElement(reader, elementStack, file, relativeResources));
                case XMLStreamConstants.END_ELEMENT -> elementStack.pop();
            }
        }
        reader.close();
    }

    /**
     * @return the class that encloses the child elements, or {@link #NO_CLASS} for property elements
     */
    private static String onStartElement(XMLStreamReader reader, Deque<String> elementStack, FxmlFile file, List<String> relativeResources) {
        String localName = reader.getLocalName();

        // Special handling for fx:include and fx:root
        if (isFxNamespace(reader.getPrefix(), reader.getNamespaceURI())) {
            if ("include".equals(localName)) {
                tryGetAttribute(reader, "source").ifPresent(relativeResources::add);
            }
            if ("root".equals(localName)) {
                var type = tryGetAttribute(reader, "type");
                if (type.isPresent()) {
                    file.rootType = type.get();
                    addAttributeProperties(reader, type.get(), "type", file, relativeResources);
                    return type.get();
                }
            }

            // abort on stuff like fx:define, fx:copy etc.
            return NO_CLASS;
        }

        // fx:controller is only allowed on the root element, so there can be at most one
        tryGetFxAttribute(reader, "controller").ifPresent(controller -> file.controller = controller);

        // Elements can be properties, e.g., <children> or <GridPane.margin>
        if (isPropertyElement(localName)) {
            addProperty(findEnclosingClass(elementStack), localName, file);
            return NO_CLASS;
        }

        // Attributes are always properties
        addAttributeProperties(reader, localName, "", file, relativeResources);
        return localName;
    }

    private static void addAttributeProperties(XMLStreamReader reader, String className, String ignoredName, FxmlFile file, List<String> relativeResources) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String prefix = reader.getAttributePrefix(i);
            if (prefix != null && !prefix.isEmpty()) {
                continue; // fx:id and friends are not properties
            }
            String name = reader.getAttributeLocalName(i);
            if ("xmlns".equals(name) || name.equals(ignoredName)) {
                continue;
            }
            if (name.startsWith("on")) {
                continue; // event handlers like onAction="#handle" are resolved against the controller
            }
            addProperty(className, name, file);
            tryAddResourceValue(reader.getAttributeValue(i), relativeResources);
        }
    }

    /**
     * Properties can be nested elements. FXMLLoader decides by the character after the
     * last dot, i.e., uppercase is a class construction, and lowercase is a property.
     *
     * @param name localName of the element
     * @return true if the element is considered a property
     */
    private static boolean isPropertyElement(String name) {
        return Character.isLowerCase(name.charAt(name.lastIndexOf('.') + 1));
    }

    /**
     * Adds a property to the file. Dotted properties get resolved against the target class.
     */
    private static void addProperty(String className, String property, FxmlFile file) {
        // Properties with dots belong to a different class, e.g., GridPane.vgrow="ALWAYS"
        int dot = property.lastIndexOf('.');
        if (dot > 0) {
            className = property.substring(0, dot);
            property = property.substring(dot + 1);
        }
        if (NO_CLASS.equals(className)) {
            return;
        }
        file.properties.computeIfAbsent(className, k -> new TreeSet<>()).add(property);
    }

    /**
     * FXMLLoader resolves any value starting with @ as a location relative to the file,
     * e.g. {@code <Image url="@../images/logo.png"/>} or {@code stylesheets="@../styles/style.css"}.
     * Values without the prefix are relative to the working directory, i.e., outside of the jar.
     */
    private static void tryAddResourceValue(String value, List<String> relativeResources) {
        if (value.startsWith("@")) {
            relativeResources.add(value.substring(1));
        }
    }

    private static XMLStreamReader createReader(String content) throws XMLStreamException {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory.createXMLStreamReader(new StringReader(content));
    }

    private static Optional<String> tryGetAttribute(XMLStreamReader reader, String name) {
        return Optional.ofNullable(reader.getAttributeValue(null, name));
    }

    private static Optional<String> tryGetFxAttribute(XMLStreamReader reader, String name) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (name.equals(reader.getAttributeLocalName(i))
                && isFxNamespace(reader.getAttributePrefix(i), reader.getAttributeNamespace(i))) {
                return Optional.of(reader.getAttributeValue(i));
            }
        }
        return Optional.empty();
    }

    private static boolean isFxNamespace(String prefix, String namespaceUri) {
        return "fx".equals(prefix) || (namespaceUri != null && namespaceUri.startsWith(FX_NAMESPACE));
    }

    private static String findEnclosingClass(Deque<String> elementStack) {
        for (String className : elementStack) {
            if (!className.isEmpty()) {
                return className;
            }
        }
        return NO_CLASS;
    }

    private static Path resolve(Path rootDir, Path origin, String path) {
        return path.startsWith("/") ? rootDir.resolve(path.substring(1)) : origin.resolveSibling(path);
    }

    private static final String FX_NAMESPACE = "http://javafx.com/fxml";
    private static final String NO_CLASS = ""; // ArrayDeque does not accept null

}
