/*-
 * #%L
 * reachability-sample
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

package us.hebi.graalvm.reachability.sample;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

/**
 * @author Florian Enner
 * @since 11 Jul 2026
 */
public class InjectionConfig {

    public static class ConstructorInjection {
        @Inject
        public ConstructorInjection(String string) {
        }
    }

    public static class MethodInjection {
        @Inject
        public void close() {
        }
    }

    public static class FieldInjection {
        @Inject
        String string;

        @Inject
        InjectionConfig config;
    }

    public static class InjectionSample {

        @Inject
        InjectionSample(String name) {
        }

        @PostConstruct
        void postConstruct() {
        }

        @PreDestroy
        void preDestroy() {
        }

        @Inject
        FieldInjection injectedField;

    }

}
