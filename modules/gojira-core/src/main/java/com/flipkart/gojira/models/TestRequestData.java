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

package com.flipkart.gojira.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flipkart.gojira.models.http.HttpTestRequestData;
import com.flipkart.gojira.models.kafka.KafkaTestRequestData;

/**
 * Base request class for different types of {@link TestDataType}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "testRequestDataType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpTestRequestData.class, name = "HTTP"),
    @JsonSubTypes.Type(value = KafkaTestRequestData.class, name = "KAFKA"),
})
public abstract class TestRequestData<T extends TestDataType> {

  final T testDataType;

  protected TestRequestData(T testDataType) {
    this.testDataType = testDataType;
  }

  public String getType() {
    return testDataType.getType();
  }
}
