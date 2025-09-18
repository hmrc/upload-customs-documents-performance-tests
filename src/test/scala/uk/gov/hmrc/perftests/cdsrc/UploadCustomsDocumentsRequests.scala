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
import io.gatling.core.check.css.CssCheckType
import io.gatling.http.Predef._
import io.gatling.http.action.cookie.AddCookieBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import jodd.lagarto.dom.NodeSelector
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

  def saveCsrfToken: CheckBuilder[CssCheckType, NodeSelector] = css("input[name='csrfToken']", "value").optional.saveAs("csrfToken")

  def setupNonce: List[ActionBuilder] =
    exec { session =>
      session.set("nonce", Random.nextInt(Int.MaxValue).toString)
    }.actionBuilders

  def getTheInitializeFileUploadPage: HttpRequestBuilder =
    http("Get initialization page")
      .get(s"$harnessBaseUrl/$harnessRoute": String)
      .check(saveCsrfToken)
      .check(status.is(200))
      .check(bodyString.transform(_.contains("Supply Initialisation JSON with configurable messages to test.")).is(true))


  def postTheInitializationRequest: HttpRequestBuilder =
    http("Post initialize file upload")
      .post(s"$harnessBaseUrl/$harnessRoute": String)
      .formParam("csrfToken", "#{csrfToken}")
     .formParam("url", baseInternalUrl)
      .formParam("userAgent", "cds-reimbursement-claim-frontend")
      .formParam(
        "json",
        s"""
        |{
        |  "config" : {
        |    "nonce" : 12345,
        |    "continueUrl" : "$harnessBaseUrl/$harnessRoute/files/12345",
        |    "backlinkUrl" : "$harnessBaseUrl/$harnessRoute",
        |    "callbackUrl" : "$harnessBaseUrl/internal/receive-file-uploads",
        |    "minimumNumberOfFiles" : 1,
        |    "maximumNumberOfFiles" : 50,
        |    "initialNumberOfEmptyRows" : 1,
        |    "maximumFileSizeBytes" : 10000000,
        |    "allowedContentTypes" : "image/jpeg,image/png,application/pdf,text/plain",
        |    "features" : {
        |     "showUploadMultiple" : true,
        |     "showLanguageSelection" : true,
        |     "showAddAnotherDocumentButton" : false,
        |     "showYesNoQuestionBeforeContinue" : false,
        |     "enableMultipleFilesPicker" : true
        |     },
        |     "content": {
        |     "title": "Performance test of multi file upload"
        |     }
        |  }
        |}""".stripMargin
      )
      .check(status.is(303))
      .check(header("Location").is(s"$baseUrl/$route": String))

  def setJSDetectionCookie(): AddCookieBuilder =
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
      .check(status.in(200,303))
      .check(bodyString.transform(_.contains("Performance test of multi file upload")).is(true))

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
      .post("#{fileUploadAmazonUrl" + index + "}")
      .header("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryjoqtomO5urVl5B6N")
      .asMultipartForm
      .bodyPart(StringBodyPart("x-amz-meta-callback-url", "#{callBack" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-date", "#{amazonDate" + index + "}"))
      .bodyPart(StringBodyPart("success_action_redirect", "#{successRedirect" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-credential", "#{amazonCredential" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-response", "#{upscanInitiateResponse" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-received", "#{upscanInitiateReceived" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-request-id", "#{requestId" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-algorithm", "#{amazonAlgorithm" + index + "}"))
      .bodyPart(StringBodyPart("key", "#{key" + index + "}"))
      .bodyPart(StringBodyPart("acl", "#{acl" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-signature", "#{amazonSignature" + index + "}"))
      .bodyPart(StringBodyPart("error_action_redirect", "#{errorRedirect" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-original-filename", fileName))
      .bodyPart(StringBodyPart("x-amz-meta-session-id", "#{sessionId" + index + "}"))
      .bodyPart(StringBodyPart("x-amz-meta-consuming-service", "#{amazonConsumingService" + index + "}"))
      .bodyPart(StringBodyPart("policy", "#{policy" + index + "}"))
      .bodyPart(RawFileBodyPart("file", fileName))
      .check(status.is(303))
      .check(header("Location").saveAs(s"UpscanUploadResponse$index"))

  def getUpscanUploadResponse(index: Int): HttpRequestBuilder =
    http(s"Upscan upload $index redirect")
      .get("#{UpscanUploadResponse" + index + "}")
      .check(status.in(201,204))

  def getFileVerificationStatus(index: Int): List[ActionBuilder] =
    asLongAs(session =>
      session(s"fileStatus$index").asOption[String].forall(s => s == "WAITING" || s == "NOT_UPLOADED")
    )(
      pause(1.second)
        .exec(
          http(s"Get the file $index verification status")
            .get(s"$baseUrl/$route/file-verification/" + "#{upscanReference" + s"$index}/status")
            .check(status.is(200))
            .check(jsonPath("$.fileStatus").in("WAITING", "ACCEPTED", "NOT_UPLOADED","REJECTED").saveAs(s"fileStatus$index"))
        )
    ).actionBuilders

  def getTheSummaryPage(index: Int): HttpRequestBuilder =
    http("Get upload summary page")
      .get(s"$baseUrl/$route/summary": String)
      .check(status.is(200))


  def getTheFullSummaryPage: HttpRequestBuilder =
    http("Get full upload summary page")
      .get(s"$baseUrl/$route/summary": String)
      .check(status.is(200))


  def getTheUploadResultsPage: HttpRequestBuilder =
    http("Get upload results page")
      .get(s"$harnessBaseUrl/$harnessRoute/files/12345": String)
      .check(status.is(200))
      .check(regex("Meta Data of Files Uploaded via Upscan"))


}
