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
import us.hebi.graalvm.reachability.annotations.ReachableMember;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class ReachableMemberConfig {

    public static class ReflectFields {
        @ReachableMember
        String field1;

        @ReachableMember
        String field2;
    }

    public static class ReflectConstructors {
        @ReachableMember
        private ReflectConstructors() {
        }

        @ReachableMember
        public ReflectConstructors(String arg) {
        }
    }

    public static class ReflectMethods {
        @ReachableMember
        String convert(String input) {
            return input;
        }

        @ReachableMember
        void doNothing(String input, Object output) {
        }
    }


    @Reachable(conditionName = "ClassLevelCondition")
    public static class MemberConditionHierarchy {

        @ReachableMember(conditionName = "MethodLevelCondition")
        String method(String input) {
            return input;
        }

        @ReachableMember
        void method2() {
        }

        @ReachableMember(conditionName = "FieldLevelCondition")
        String field1;

        @ReachableMember
        String field2;

    }

}
