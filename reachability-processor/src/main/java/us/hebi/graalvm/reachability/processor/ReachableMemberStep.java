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

package us.hebi.graalvm.reachability.processor;

import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.reachability.annotations.Reachable;
import us.hebi.graalvm.reachability.annotations.ReachableMember;
import us.hebi.graalvm.reachability.processor.util.ElementUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 14 July 2026
 */
public class ReachableMemberStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(
                ReachableMember.class.getCanonicalName(),
                "javafx.fxml.FXML"
        );
    }

    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        for (var element : elementMap.values()) {
            if (element instanceof TypeElement typeElement) {
                continue;
            }

            // fine-grained reflection
            if (element.getEnclosingElement() instanceof TypeElement typeElement) {
                var condition = tryGetDefinedCondition(getAnnotationMirror(element, ReachableMember.class))
                        .or(() -> tryGetDefinedCondition(getAnnotationMirror(typeElement, Reachable.class)))
                        .orElse(ElementUtil.getBinaryName(typeElement));
                var metadata = getConditionalMetadata(condition);
                addReflectedFieldOrMethod(metadata, typeElement, element, false);

            } else {
                printWarning("Parent is not a TypeElement: " + element);
            }

        }
    }

    public ReachableMemberStep(Supplier<ProcessingEnvironment> env) {
        super("reachable-member", env);
    }

}
