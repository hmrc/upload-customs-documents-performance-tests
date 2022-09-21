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

import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.http.Predef._
import io.gatling.http.action.cookie.AddCookieBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import scala.concurrent.duration.DurationInt
import scala.util.Random

object UploadCustomsDocumentsRequests extends ServicesConfiguration {

  val baseUrl: String         = baseUrlFor("upload-customs-documents-frontend")
  val baseInternalUrl: String = baseUrlFor("upload-customs-documents-frontend-internal")
  val route: String           = "upload-customs-documents"

  val harnessBaseUrl: String         = baseUrlFor("upload-customs-documents-test-harness-frontend")
  val harnessBaseInternalUrl: String = baseUrlFor("upload-customs-documents-test-harness-frontend-internal")
  val harnessRoute: String           = "upload-customs-documents-test-harness"

  val CsrfPattern = """<input type="hidden" name="csrfToken" value="([^"]+)""""

  def saveCsrfToken(): CheckBuilder[RegexCheckType, String, String] = regex(_ => CsrfPattern).saveAs("csrfToken")

  def setupNonce =
    exec { session =>
      session.set("nonce", Random.nextInt(Int.MaxValue).toString())
    }.actionBuilders

  def getTheInitializeFileUploadPage: HttpRequestBuilder =
    http("Get initialization page")
      .get(s"$harnessBaseUrl/$harnessRoute": String)
      .check(saveCsrfToken())
      .check(status.is(200))
      .check(regex("Supply Initialisation JSON with configurable messages to test."))

  def postTheInitializationRequest: HttpRequestBuilder =
    http("Post initialize file upload")
      .post(s"$harnessBaseUrl/$harnessRoute": String)
      .formParam("csrfToken", "${csrfToken}")
      .formParam("url", baseInternalUrl)
      .formParam("userAgent", "cds-reimbursement-claim-frontend")
      .formParam(
        "json",
        s"""
        |{
        |  "config" : {
        |    "nonce" : $${nonce},
        |    "continueUrl" : "$harnessBaseUrl/$harnessRoute/files/$${nonce}",
        |    "backlinkUrl" : "$harnessBaseUrl/$harnessRoute",
        |    "callbackUrl" : "$harnessBaseInternalUrl/internal/receive-file-uploads",
        |    "minimumNumberOfFiles" : 1,
        |    "maximumNumberOfFiles" : 50,
        |    "initialNumberOfEmptyRows" : 1,
        |    "maximumFileSizeBytes" : 10000000,
        |    "allowedContentTypes" : "image/jpeg,image/png,application/pdf,text/plain",
        |    "features" : {
        |      "showUploadMultiple" : true,
        |      "showLanguageSelection" : true,
        |      "showAddAnotherDocumentButton" : false,
        |      "showYesNoQuestionBeforeContinue" : false
        |    },
        |    "content": {
        |      "title": "Performance test of multi file upload"
        |    }
        |  }
        |}""".stripMargin
      )
      .check(status.is(303))
      .check(header("Location").is(s"$baseUrl/$route": String))

  def setJSDetectionCookie: AddCookieBuilder =
    addCookie(
      Cookie("JS-Detection", "true")
        .withDomain(
          baseUrl
            .stripPrefix("http://")
            .stripPrefix("https://")
            .stripSuffix(":10110")
            .stripSuffix(":443")
        )
    )

  def getTheMultiFileUploadPage: HttpRequestBuilder =
    http("Get multi file upload page")
      .get(s"$baseUrl/$route/choose-files": String)
      .check(status.is(200))
      .check(regex("Performance test of multi file upload"))

  def postInitiateUpscan(index: Int): HttpRequestBuilder =
    http(s"Post initiate file-$index on Upscan")
      .post(s"$baseUrl/$route/initiate-upscan/file-$index": String)
      .check(jsonPath("$.upscanReference").saveAs(s"upscanReference$index"))
      .check(jsonPath("$.uploadId").saveAs(s"uploadId$index"))
      .check(jsonPath("$.uploadRequest.href").saveAs(s"fileUploadAmazonUrl$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-meta-callback-url").saveAs(s"callBack$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-date").saveAs(s"amazonDate$index"))
      .check(jsonPath("$.uploadRequest.fields.success_action_redirect").saveAs(s"successRedirect$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-credential").saveAs(s"amazonCredential$index"))
      .check(
        jsonPath("$.uploadRequest.fields.x-amz-meta-upscan-initiate-response").saveAs(s"upscanInitiateResponse$index")
      )
      .check(
        jsonPath("$.uploadRequest.fields.x-amz-meta-upscan-initiate-received").saveAs(s"upscanInitiateReceived$index")
      )
      .check(jsonPath("$.uploadRequest.fields.x-amz-meta-request-id").saveAs(s"requestId$index"))
      .check(
        jsonPath("$.uploadRequest.fields.x-amz-meta-original-filename").saveAs(s"amazonMetaOriginalFileName$index")
      )
      .check(jsonPath("$.uploadRequest.fields.x-amz-algorithm").saveAs(s"amazonAlgorithm$index"))
      .check(jsonPath("$.uploadRequest.fields.key").saveAs(s"key$index"))
      .check(jsonPath("$.uploadRequest.fields.acl").saveAs(s"acl$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-signature").saveAs(s"amazonSignature$index"))
      .check(jsonPath("$.uploadRequest.fields.error_action_redirect").saveAs(s"errorRedirect$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-meta-session-id").saveAs(s"sessionId$index"))
      .check(jsonPath("$.uploadRequest.fields.x-amz-meta-consuming-service").saveAs(s"amazonConsumingService$index"))
      .check(jsonPath("$.uploadRequest.fields.policy").saveAs(s"policy$index"))
      .check(status.is(200))

  def postUploadDocument(index: Int, fileName: String): HttpRequestBuilder =
    http(s"Post upload document $index to upscan-proxy")
      .post("${fileUploadAmazonUrl" + index + "}")
      .header("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryjoqtomO5urVl5B6N")
      .asMultipartForm
      .bodyPart(StringBodyPart("x-amz-meta-callback-url", "${callBack" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-date", "${amazonDate" + index + "}"))
      .bodyPart(StringBodyPart("success_action_redirect", "${successRedirect" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-credential", "${amazonCredential" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-response", "${upscanInitiateResponse" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-received", "${upscanInitiateReceived" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-request-id", "${requestId" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-algorithm", "${amazonAlgorithm" + index + "}"))
      .bodyPart(StringBodyPart("key", "${key" + index + "}"))
      .bodyPart(StringBodyPart("acl", "${acl" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-signature", "${amazonSignature" + index + "}"))
      .bodyPart(StringBodyPart("error_action_redirect", "${errorRedirect" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-original-filename", fileName))
      .bodyPart(StringBodyPart("x-amz-meta-session-id", "${sessionId" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-consuming-service", "${amazonConsumingService" + index + "}"))
      .bodyPart(StringBodyPart("policy", "${policy" + index + "}"))
      .bodyPart(RawFileBodyPart("file", fileName))
      .check(status.is(303))
      .check(header("Location").saveAs(s"UpscanUploadResponse$index"))

  def getUpscanUploadResponse(index: Int): HttpRequestBuilder =
    http(s"Upscan upload $index redirect")
      .get("${UpscanUploadResponse" + index + "}")
      .check(status.in(201))

  def getFileVerificationStatus(index: Int): List[ActionBuilder] =
    asLongAs(session =>
      session(s"fileStatus$index").asOption[String].forall(s => s == "WAITING" || s == "NOT_UPLOADED")
    )(
      pause(1.second)
        .exec(
          http(s"Get the file $index verification status")
            .get(s"$baseUrl/$route/file-verification/" + "${upscanReference" + s"$index}/status")
            .check(status.is(200))
            .check(jsonPath("$.fileStatus").in("WAITING", "ACCEPTED", "NOT_UPLOADED").saveAs(s"fileStatus$index"))
        )
    ).actionBuilders

  def getTheSummaryPage(index: Int): HttpRequestBuilder =
    http("Get upload summary page")
      .get(s"$baseUrl/$route/summary": String)
      .check(status.is(200))
      .check(regex(s"Documents you’ve uploaded"))
      .check(regex(s"image_$index.jpeg"))

  def getTheFullSummaryPage: HttpRequestBuilder =
    http("Get full upload summary page")
      .get(s"$baseUrl/$route/summary": String)
      .check(status.is(200))
      .check(regex(s"Documents you’ve uploaded"))
      .check(regex("image_1.jpeg"))
      .check(regex("image_2.jpeg"))
      .check(regex("image_3.jpeg"))
      .check(regex("image_4.jpeg"))
      .check(regex("image_5.jpeg"))
      .check(regex("image_6.jpeg"))
      .check(regex("image_7.jpeg"))
      .check(regex("image_8.jpeg"))
      .check(regex("image_9.jpeg"))
      .check(regex("image_10.jpeg"))
      .check(regex("image_11.jpeg"))
      .check(regex("image_12.jpeg"))
      .check(regex("image_13.jpeg"))

  def getTheUploadResultsPage: HttpRequestBuilder =
    http("Get upload results page")
      .get(s"$harnessBaseUrl/$harnessRoute/files/$${nonce}": String)
      .check(status.is(200))
      .check(regex("Meta Data of Files Uploaded via Upscan"))
      .check(regex("image_1.jpeg"))
      .check(regex("9a60a517e8279d398d44e3cbfb42f80aba427133b7e99f989fba761b46d4cf06"))
      .check(regex("image_2.jpeg"))
      .check(regex("bba7ecd1838ca8b826a1c008be188f5f4d27c87dbd59e8e844f64d8584999265"))
      .check(regex("image_3.jpeg"))
      .check(regex("18f8fe6fd4c0babe95da1b83c8ebb653eec5597c8027c6698366f497b13cb4c3"))
      .check(regex("image_4.jpeg"))
      .check(regex("6db0c74de47ea5cd0e04ddeb85af8c283e8c5db2b8d067dfcea2eaca022474e8"))
      .check(regex("image_5.jpeg"))
      .check(regex("a0b44e63a2e0b0e32a11e7f225c48141ee9f357330657db1c7d70b69473ab80e"))
      .check(regex("image_6.jpeg"))
      .check(regex("e8b15355b07195a86cfc077d979bfa511922981f20e71c33db36fc13291797aa"))
      .check(regex("image_7.jpeg"))
      .check(regex("8b475a0f3188c62a5347608f50573afa4564a66156fe0ccce6d8e761346b7ce9"))
      .check(regex("image_8.jpeg"))
      .check(regex("2c4ae122200d96a2b2b0f94b707f9f0c189d90aa63fc5f2d1376b55c00eef25d"))
      .check(regex("image_9.jpeg"))
      .check(regex("d5a8aea6862f22edc8a431676a74d52ebc2c7c7ed3de42a72abccbf7f9a51a3e"))
      .check(regex("image_10.jpeg"))
      .check(regex("747e67cda43865585f31fb0464688d1e58848361482f79513a3d281ec9f9e642"))
      .check(regex("image_11.jpeg"))
      .check(regex("3c59fa9fcd8dac759bb69e8cb2e9dfd153dd36bcee5cc39f4c9291ed0a903ad4"))
      .check(regex("image_12.jpeg"))
      .check(regex("ecdc16270c16b046d693cfe3e17da1d188e10c9312d2f1a29bb9f4132098b960"))
      .check(regex("image_13.jpeg"))
      .check(regex("63927d7ed42fd6632a44f81a7a6753c0da98536fa885d28cfce494ff8f37b66b"))

}
