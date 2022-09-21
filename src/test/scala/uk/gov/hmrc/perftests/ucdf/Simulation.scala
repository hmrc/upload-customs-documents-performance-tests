/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.perftests.cdsrc

import io.gatling.core.action.builder.ActionBuilder
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner

class Simulation extends PerformanceTestRunner {

  val useAwesomeStubs: Boolean =
    readProperty("useAwesomeStubs", "false").toBoolean

  def LoginTheUser(userId: String, eoriValue: String): List[ActionBuilder] =
    if (useAwesomeStubs)
      List(
        AwesomeStubRequests.getLoginPage,
        AwesomeStubRequests.loginUser(userId),
        AwesomeStubRequests.updateUserRole(eoriValue),
        AwesomeStubRequests.postSuccessfulLogin
      )
    else
      List(
        AuthLoginStubRequests.getMRNAuthLoginPage,
        AuthLoginStubRequests.loginWithAuthLoginStubMRN(eoriValue)
      )

  val UploadCustomsDocumentsMultiFileUpload: List[ActionBuilder] =
    LoginTheUser("user1", "GB000000000000001") ++
      UploadCustomsDocumentsRequests.setupNonce ++
      List[ActionBuilder](
        UploadCustomsDocumentsRequests.getTheInitializeFileUploadPage,
        UploadCustomsDocumentsRequests.postTheInitializationRequest,
        UploadCustomsDocumentsRequests.setJSDetectionCookie,
        UploadCustomsDocumentsRequests.getTheMultiFileUploadPage
      ) ++
      (1 to 13).flatMap { index =>
        List[ActionBuilder](
          UploadCustomsDocumentsRequests.postInitiateUpscan(index),
          UploadCustomsDocumentsRequests.postUploadDocument(index, s"data/image_$index.jpeg"),
          UploadCustomsDocumentsRequests.getUpscanUploadResponse(index)
        ) ++ UploadCustomsDocumentsRequests.getFileVerificationStatus(index) ++
          List[ActionBuilder](
            UploadCustomsDocumentsRequests.getTheSummaryPage(index)
          )
      } ++ List[ActionBuilder](
        UploadCustomsDocumentsRequests.getTheFullSummaryPage,
        UploadCustomsDocumentsRequests.getTheUploadResultsPage
      )

  setup("Upload-Customs-Documents-Multi-File-Upload", "Upload multiple files") withActions
    (UploadCustomsDocumentsMultiFileUpload: _*)

  runSimulation()
}
