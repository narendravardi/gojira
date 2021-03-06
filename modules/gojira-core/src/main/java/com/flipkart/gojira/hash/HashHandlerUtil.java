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

package com.flipkart.gojira.hash;

import com.flipkart.gojira.hash.annotations.HashHandler;
import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to get instance of {@link TestHashHandler} for a method argument. TODO: Refactor
 * this.
 */
public class HashHandlerUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(HashHandlerUtil.class);

  private static ConcurrentHashMap<String, TestHashHandler> hashHandlerMap = new ConcurrentHashMap<>();

  public static TestHashHandler getHashHandler(MethodInvocation invocation, int position)
      throws TestHashException {
    try {
      String genericMethodName = invocation.getMethod().toGenericString();
      if (hashHandlerMap.containsKey(genericMethodName + "|" + position)) {
        return hashHandlerMap.get(genericMethodName + "|" + position);
      } else {
        HashHandler annotatedHashHandler = annotatedHashHandler(invocation, position);
        if (annotatedHashHandler != null) {
          hashHandlerMap.put(genericMethodName + "|" + position,
              annotatedHashHandler.hashHandlerClass().newInstance());
          return hashHandlerMap.get(genericMethodName + "|" + position);
        }
        return null;
      }
    } catch (Exception e) {
      LOGGER.error("error trying to get hash handler.", e);
      throw new TestHashException("error trying to get hash handler.", e);
    }
  }

  private static HashHandler annotatedHashHandler(MethodInvocation invocation, int position) {
    Annotation[] annotations = invocation.getMethod().getParameterAnnotations()[position];
    for (Annotation annotation : annotations) {
      if (annotation instanceof HashHandler) {
        return (HashHandler) annotation;
      }
    }
    return null;
  }
}
