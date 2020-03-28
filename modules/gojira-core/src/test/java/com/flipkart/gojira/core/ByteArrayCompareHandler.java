/*
 * Copyright 2020 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.gojira.core;

import com.flipkart.compare.TestCompareException;
import com.flipkart.compare.handlers.TestCompareHandler;
import java.util.Arrays;

/**
 * Created by arunachalam.s on 15/09/17.
 */
public class ByteArrayCompareHandler extends TestCompareHandler {

  @Override
  protected void doCompare(byte[] profiledData, byte[] testData) throws TestCompareException {
    if (!Arrays.equals(profiledData, testData)) {
      throw new TestCompareException("byte arrays not equal.");
    }
  }
}
