/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.systemtest;

import java.util.function.Predicate;

public class Count<T> implements Predicate<T>
{
    private final int expected;
    private volatile int actual;

    public Count(int expected) {
        this.expected = expected;
    }

    @Override
    public boolean test(T message) {
        if(message != null) {
            ++actual;
        }
        return actual == expected;
    }

    public int actual() {
        return actual;
    }
}
