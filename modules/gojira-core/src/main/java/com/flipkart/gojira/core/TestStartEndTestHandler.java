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
import com.flipkart.gojira.compare.GojiraCompareHandlerRepository;
import com.flipkart.gojira.core.injectors.GuiceInjector;
import com.flipkart.gojira.models.ProfileData;
import com.flipkart.gojira.models.TestData;
import com.flipkart.gojira.models.TestDataType;
import com.flipkart.gojira.models.TestRequestData;
import com.flipkart.gojira.models.TestResponseData;
import com.flipkart.gojira.serde.SerdeHandlerRepository;
import com.flipkart.gojira.sinkstore.SinkException;
import com.flipkart.gojira.sinkstore.handlers.SinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link StartEndTestHandler} for mode {@link Mode#TEST}
 *
 * @param <T> type of test-data
 */
public class TestStartEndTestHandler<T extends TestDataType> implements StartEndTestHandler<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestStartEndTestHandler.class);

  // TODO: Move this global constants file.
  private static final String delim = " | ";
  private static final String RESULT_SUCCESS = "SUCCESS";
  private static final String READ_FAILURE = "READ_FAILED";
  private static final String COMPARE_FAILED = "COMPARE_FAILED";
  private static final String EXECUTION_FAILED = "EXECUTION_FAILED";
  private static final String UNKNOWN_FAILED = "UNKNOWN_FAILED";

  /**
   * compareHandlerRepository for comparing  {@link TestResponseData}
   */
  private GojiraCompareHandlerRepository gojiraCompareHandlerRepository = GuiceInjector
      .getInjector().getInstance(GojiraCompareHandlerRepository.class);
  /**
   * serdeHandlerRepository for de-serializing {@link TestData} and serializing {@link
   * TestResponseData}
   */
  private SerdeHandlerRepository serdeHandlerRepository = GuiceInjector.getInjector()
      .getInstance(SerdeHandlerRepository.class);
  /**
   * sinkHandler for reading {@link TestData} and storing results of test execution
   */
  private SinkHandler sinkHandler = GuiceInjector.getInjector().getInstance(SinkHandler.class);

  /**
   * If id is null or empty, throws a {@link RuntimeException}
   * <p>
   * Reads the test data using {@link SinkHandler#read(String)} and deserializes using {@link
   * SerdeHandlerRepository#getTestDataSerdeHandler()} instance.
   * <p>
   * If testData is null, throws a {@link RuntimeException}
   * <p>
   * Begins execution by calling {@link ProfileRepository#begin(String)} and adds {@link TestData}
   * for execution by calling {@link ProfileRepository#setTestData(TestData)} to make method
   * intercepted and response data recorded in {@link Mode#PROFILE} mode available.
   * <p>
   * In case of any exception, marks {@link ProfileData#profileState} as {@link ProfileState#FAILED}
   * and stores result as {@value READ_FAILURE}.
   * <p>
   * In case {@link SinkHandler#writeResults(String, String)} throws {@link SinkException}, simply
   * logs and throws a {@link RuntimeException}
   *
   * @param id          this is the id, which will be used for synchronizing testing across multiple
   *                    threads within a single request-response scope.
   * @param requestData this is the request-data with which test is initiated
   */
  @Override
  public void start(String id, TestRequestData<T> requestData) {
    if (id == null || id.isEmpty()) {
      throw new RuntimeException("invalid test id.");
    }

    try {
      TestData<TestRequestData<T>, TestResponseData<T>, T> testData = serdeHandlerRepository
          .getTestDataSerdeHandler().deserialize(sinkHandler.read(id), TestData.class);
      if (testData == null) {
        throw new RuntimeException("no data available against mentioned test id: " + id);
      }
      ProfileRepository.begin(id);
      ProfileRepository.setTestData(testData);
    } catch (Exception e) {
      ProfileRepository.setProfileState(ProfileState.FAILED);
      LOGGER.error("unable to fetch data against test id: " + id);
      try {
        sinkHandler.writeResults(id, READ_FAILURE);
      } catch (SinkException se) {
        LOGGER.error(String
            .format("sink write of READ_FAILURE failed for test id : %s with exception: ", id), se);
        throw new RuntimeException(se);
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * If {@link ProfileData#profileState} is not {@link ProfileState#NONE}, compares the {@link
   * TestResponseData} using {@link GojiraCompareHandlerRepository#getResponseDataCompareHandler()}
   * instance after serializing using {@link SerdeHandlerRepository#getReqRespDataSerdeHandler()}.
   * <p>
   * Based on the result of comparison, {@link SinkHandler#writeResults(String, String)} is called.
   * <p>
   * If comparison is successful, {@value RESULT_SUCCESS} is written. On comparison failure, {@value
   * COMPARE_FAILED} is written. On unknown exception, {@value UNKNOWN_FAILED} is written.
   * <p>
   * If {@link SinkHandler#writeResults(String, String)} fails, {@link RuntimeException} is thrown.
   * <p>
   * In the finally block, {@link ProfileRepository#end()} is caled.
   *
   * @param responseData this is the response-data after the request is processed by the client
   *                     application.
   */
  @Override
  public void end(TestResponseData<T> responseData) {
    try {
      String id = ProfileRepository.getTestData().getId();
      if (!ProfileState.NONE.equals(ProfileRepository.getProfileState())) {
        try {
          gojiraCompareHandlerRepository.getResponseDataCompareHandler()
              .compare(serdeHandlerRepository.getReqRespDataSerdeHandler()
                      .serialize(ProfileRepository.getTestData().getResponseData()),
                  serdeHandlerRepository.getReqRespDataSerdeHandler().serialize(responseData));
          sinkHandler.writeResults(id, RESULT_SUCCESS);
          LOGGER.info("RESULT_SUCCESS for " + id);
        } catch (TestCompareException e) {
          LOGGER.error("test compare exception.", e);
          sinkHandler.writeResults(id, COMPARE_FAILED);
          // Solve this to pass stack trace in payload
//                sinkHandler.writeResults(id,COMPARE_FAILED + delim + e + delim+ stackTraceToString(e));
        } catch (Exception e) {
          sinkHandler.writeResults(id, UNKNOWN_FAILED);
          // Solve this to pass stack trace in payload
//                sinkHandler.writeResults(id,UNKNOWN_FAILED + delim + e + delim + stackTraceToString(e));
          LOGGER.error("test unknown failed exception.", e);
        }
      }
    } catch (SinkException e) {
      LOGGER.error("error while saving the result", e);
      // TODO: Throw a well-defined exception.
      throw new RuntimeException(e);
    } finally {
      ProfileRepository.end();
    }
  }
}
