/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.storage.controller;

import java.io.IOException;

public class Main {

    public static void main(String args[]) {
        try {
            StorageController service = new StorageController(StorageControllerOptions.fromEnv(System.getenv()));
            service.run();
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error starting storage controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
