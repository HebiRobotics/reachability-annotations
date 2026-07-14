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


import us.hebi.graalvm.reachability.annotations.Reachable;
import us.hebi.graalvm.reachability.annotations.MemberAccess;
import us.hebi.graalvm.reachability.annotations.Reachable.Proxy;
import us.hebi.graalvm.reachability.annotations.ReachableFxResources;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class ReachableConfig {

    @Reachable(includeClassHierarchy = true)
    public enum ReflectEnum {
        Field1;
    }

    @Reachable
    public static class NestedParentClass {
    }

    @Reachable(includeClassHierarchy = true)
    public static class NestedChildClass extends NestedParentClass {
    }

    @Reachable(classes = NestedChildClass.class, jniAccessible = true, includeClassHierarchy = true)
    public static class ReferencingChildClass {
    }

    @Reachable(classes = NestedChildClass.class, includeClassHierarchy = false)
    public static class ReferencingChildClassWithoutParent {
    }

    @Reachable(classes = {
            InetAddress.class,
            InetSocketAddress.class,
    }, classNames = {
            "some.random.class$Nested",
            "other.random.class",
    }, includeClassHierarchy = true)
    public static class ReferencingMultipleClasses {
    }

    @Reachable(classNames = "sun.net.www.protocol.jar.JarURLConnection", includeClassHierarchy = true)
    public static class PrivateClassHierarchy {
    }

    @Reachable(memberAccess = {})
    public static class NoMemberAccess {
    }

    @Reachable(memberAccess = {
            MemberAccess.ALL_DECLARED_METHODS,
            MemberAccess.ALL_DECLARED_FIELDS,
            MemberAccess.ALL_DECLARED_CONSTRUCTORS,
            MemberAccess.ALL_PUBLIC_METHODS,
            MemberAccess.ALL_PUBLIC_FIELDS,
            MemberAccess.ALL_PUBLIC_CONSTRUCTORS,
            MemberAccess.UNSAFE_ALLOCATED
    }, jniAccessible = true)
    public static class FullMemberAccess {
    }

    @Reachable(condition = Object.class, classNames = "jdk.internal.vm.annotation.Stable")
    @Reachable(conditionName = "sun.misc.Unsafe", classNames = "sun.misc.Unsafe", memberAccess = MemberAccess.ALL_DECLARED_FIELDS, jniAccessible = true)
    public static class KeepPrivateClass {
    }

    @Reachable(condition = Object.class, classes = String.class)
    @Reachable(condition = InetAddress.class, classes = InetAddress.class)
    public static class KeepPublicClass {
    }

    @Reachable(resources = {
            "images/*.png",
            "images/*.jpg",
    })
    @Reachable(resources = {
            "/assets/*.png",
            "/assets/*.jpg",
    })
    public static class AddResources {
    }

    @Reachable(proxies = {
            @Proxy({"name1", "name2"}),
            @Proxy({"name3", "name4"}),
    })
    public static class AddProxies {
    }

    @ReachableFxResources({
            "/us/hebi/graalvm/reachability/**.css",
            "/us/hebi/graalvm/reachability/**.fxml"
    })
    public static class AddAbsoluteWildcardResources {
    }

    @ReachableFxResources({"**.css", "**.fxml"})
    public static class AddRelativeWildcardResources {
    }

    @ReachableFxResources({
            "javafx/javafx.fxml",
            "javafx/javafx.css",
    })
    public static class AddRelativeResources {
    }

    @ReachableFxResources(value = {
            "javafx/javafx.fxml",
            "javafx/javafx.css",
    }, parseContents = false)
    public static class AddRelativeResourcesNonParsing {
    }

    @ReachableFxResources({
            "hello.fxml",
            "hello.css",
    })
    public static class AddNonexistingResources {
    }

    @ReachableFxResources("hello.fxml")
    @ReachableFxResources("hello.css")
    public static class MultipleResourceAnnotations {
    }

}
