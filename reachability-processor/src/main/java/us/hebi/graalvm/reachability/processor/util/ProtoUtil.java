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

package us.hebi.graalvm.reachability.processor.util;

import lombok.experimental.UtilityClass;
import us.hebi.quickbuf.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
@UtilityClass
public class ProtoUtil {

    public static <T extends ProtoMessage<T>> T parseJsonObject(Path file, MessageFactory<T> factory) throws IOException {
        var instance = factory.create();
        var json = getJsonSource(file);
        if (json.isPresent()) json.get().readMessage(instance);
        return instance;
    }

    public static <T extends ProtoMessage<T>> RepeatedMessage<T> parseJsonList(Path file, MessageFactory<T> factory) throws IOException {
        var list = RepeatedMessage.newEmptyInstance(factory);
        var json = getJsonSource(file);
        if (json.isPresent()) json.get().readRepeatedMessage(list);
        return list;
    }

    private static Optional<JsonSource> getJsonSource(Path file) throws IOException {
        if (Files.exists(file)) {
            return Optional.of(JsonSource.newInstance(Files.readAllBytes(file)));
        }
        return Optional.empty();
    }

    public static byte[] toJson(ProtoMessage<?> message) throws IOException {
        if (message.isEmpty()) return new byte[]{'{', '}'};
        return JsonSink.newPrettyInstance()
                .writeMessage(message)
                .getBytes()
                .toArray();
    }

    public static byte[] toJson(RepeatedMessage<?> repeated) throws IOException {
        if (repeated.length() == 0) return new byte[]{'[', ']'};
        return JsonSink.newPrettyInstance()
                .writeRepeatedMessage(repeated)
                .getBytes()
                .toArray();
    }

    public static String[] toStringArray(RepeatedString list) {
        String[] array = new String[list.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

}
