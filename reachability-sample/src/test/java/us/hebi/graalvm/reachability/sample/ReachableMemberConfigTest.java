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

import org.junit.jupiter.api.Test;

import static us.hebi.graalvm.reachability.sample.ReachableConfigTest.*;

/**
 * @author Florian Enner
 * @since 11 Jul 2026
 */
class ReachableMemberConfigTest {

    private static String reflectionConfig = readJsonContent("reachable-member", "reflect-config.json");

    @Test
    void testMemberConditionHierarchy() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "ClassLevelCondition"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$MemberConditionHierarchy",
                  "methods": [{
                    "name": "method2",
                    "parameterTypes": []
                  }],
                  "fields": [{
                    "name": "field2"
                  }]
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "FieldLevelCondition"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$MemberConditionHierarchy",
                  "fields": [{
                    "name": "field1"
                  }]
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "MethodLevelCondition"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$MemberConditionHierarchy",
                  "methods": [{
                    "name": "method",
                    "parameterTypes": ["java.lang.String"]
                  }]
                }
                """);
    }

    @Test
    void testReflectConstructors() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectConstructors"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectConstructors",
                  "methods": [{
                    "name": "<init>",
                    "parameterTypes": []
                  }, {
                    "name": "<init>",
                    "parameterTypes": ["java.lang.String"]
                  }]
                }
                """);
    }

    @Test
    void testReflectFields() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectFields"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectFields",
                  "fields": [{
                    "name": "field1"
                  }, {
                    "name": "field2"
                  }]
                }
                """);
    }

    @Test
    void testReflectMethods() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectMethods"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableMemberConfig$ReflectMethods",
                  "methods": [{
                    "name": "convert",
                    "parameterTypes": ["java.lang.String"]
                  }, {
                    "name": "doNothing",
                    "parameterTypes": ["java.lang.String", "java.lang.Object"]
                  }]
                }
                """);
    }

}
