/*-
 * #%L
 * config-sample
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

package us.hebi.graalvm.config.sample;

import us.hebi.graalvm.config.Reachable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class ReflectionConfigOptions {

    @Reachable
    public enum ReflectEnum {
        Field1;
    }

    @Reachable
    public static class NestedParentClass {

    }

    @Reachable
    public static class NestedChildClass extends NestedParentClass {

    }

    @Reachable(classes = NestedChildClass.class)
    public static class ReferencingChildClass {

    }

    @Reachable(classes = {
            InetAddress.class,
            InetSocketAddress.class,
    }, classNames = {
            "some.random.class$Nested",
            "other.random.class",
    })
    public static class ReferencingMultipleClasses {

    }

}
