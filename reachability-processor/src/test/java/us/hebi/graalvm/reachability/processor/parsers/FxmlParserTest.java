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

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
class FxmlParserTest {

    @Test
    void addFxmlFile() throws URISyntaxException {
        var rootDir = Path.of(FxmlParserTest.class.getResource("/").toURI());
        var testDir = Path.of(FxmlParserTest.class.getResource("tests").toURI());

        // the included file is parsed once and comes after the file that references it
        var files = List.copyOf(FxmlParser.parse(rootDir, testDir.resolve("javafx.fxml")));
        assertThat(files).hasSize(2);

        var javafx = files.get(0);
        assertThat(javafx.getPath()).isEqualTo(testDir.resolve("javafx.fxml"));

        assertThat(javafx.getImports()).containsExactlyInAnyOrder(
                "javafx.scene.control.Button",
                "javafx.scene.image.Image",
                "javafx.scene.image.ImageView",
                "javafx.scene.layout.AnchorPane"
        );

        assertThat(javafx.getResources()).containsExactly(testDir.resolve("image.jpg"));

        // class names stay as written in the source, and the controller is a class without properties
        assertThat(javafx.getProperties()).containsOnlyKeys("AnchorPane", "Button", "Image", "ImageView", "nonexisting.Controller");
        assertThat(javafx.getProperties().get("nonexisting.Controller")).isEmpty();
        assertThat(javafx.getProperties().get("Button"))
                .containsExactlyInAnyOrder("layoutX", "layoutY", "mnemonicParsing", "text");
        assertThat(javafx.getProperties().get("ImageView"))
                .containsExactlyInAnyOrder("fitHeight", "fitWidth", "image", "layoutX", "layoutY", "pickOnBounds", "preserveRatio");

        var included = files.get(1);
        assertThat(included.getPath()).isEqualTo(testDir.resolve("included.fxml"));
        assertThat(included.getImports()).containsExactly("javafx.scene.control.Button");
        assertThat(included.getResources()).isEmpty();
        assertThat(included.getProperties()).containsOnlyKeys("Button", "us.hebi.graalvm.reachability.sample.javafx.IncludedFxController");
        assertThat(included.getProperties().get("Button")).containsExactlyInAnyOrder("mnemonicParsing", "text");
    }

    @Test
    void addFxmlFileWithProperties() throws URISyntaxException {
        var rootDir = Path.of(FxmlParserTest.class.getResource("/").toURI());
        var testDir = Path.of(FxmlParserTest.class.getResource("tests").toURI());

        var files = List.copyOf(FxmlParser.parse(rootDir, testDir.resolve("properties.fxml")));
        assertThat(files).hasSize(1);

        var properties = files.get(0).getProperties();
        assertThat(properties).containsOnlyKeys(
                "Button",
                "ButtonBar",
                "ButtonBar.ButtonData",
                "ColumnConstraints",
                "GridPane",
                "javafx.scene.control.Slider",
                "nonexisting.Controller"
        );

        // nested class element, dots stay as written
        assertThat(properties.get("ButtonBar.ButtonData")).isEmpty();

        // fx:id, onAction, and xmlns are not properties
        assertThat(properties.get("Button"))
                .containsExactlyInAnyOrder("alignment", "style", "text");

        assertThat(properties.get("ButtonBar"))
                .containsExactlyInAnyOrder("buttonOrder");

        // fully qualified element without an import
        assertThat(properties.get("javafx.scene.control.Slider"))
                .containsExactlyInAnyOrder("snapToTicks");

        assertThat(properties.get("ColumnConstraints"))
                .containsExactlyInAnyOrder("hgrow");

        assertThat(properties.get("GridPane"))
                .containsExactlyInAnyOrder("children", "columnConstraints", "stylesheets", "vgrow");

        // any @-prefixed value resolves as a resource, not just Image/URL attributes
        assertThat(files.get(0).getResources()).contains(testDir.resolve("custom.css"));
    }

    @Test
    void addFxmlFileWithRootType() throws URISyntaxException {
        var rootDir = Path.of(FxmlParserTest.class.getResource("/").toURI());
        var testDir = Path.of(FxmlParserTest.class.getResource("tests").toURI());

        var files = List.copyOf(FxmlParser.parse(rootDir, testDir.resolve("root.fxml")));
        assertThat(files).hasSize(1);

        var root = files.get(0);

        // the type attribute names the class and is not one of its properties
        assertThat(root.getProperties()).containsOnlyKeys("VBox");
        assertThat(root.getProperties().get("VBox"))
                .containsExactlyInAnyOrder("alignment", "children", "spacing");
    }

}
