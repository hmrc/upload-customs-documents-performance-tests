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
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.http.Predef._
import io.gatling.http.check.header.HttpHeaderCheckType
import io.gatling.http.check.header.HttpHeaderRegexCheckType
import io.gatling.http.request.builder.HttpRequestBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AwesomeStubRequests extends ServicesConfiguration {

  val baseUrlAwesomeStubs: String         = baseUrlFor("awesome-stubs")
  val baseUrlAwesomeFrontendStubs: String = baseUrlFor("awesome-stubs-frontend")
  val route: String                       = "agents-external-stubs"

  val postSignInUrl         = s"$baseUrlAwesomeStubs/agents-external-stubs/sign-in"
  val updateUserUrl         = s"$baseUrlAwesomeStubs/agents-external-stubs/users"
  val updateSpecificUserUrl = s"$baseUrlAwesomeStubs/agents-external-stubs/users/$${userId}"

  val loginUrl: String       = s"$baseUrlAwesomeFrontendStubs/$route/gg/sign-in"
  val redirectUrl: String    = s"$baseUrlAwesomeFrontendStubs/$route/user"
  val loginSubmitUrl: String =
    s"$baseUrlAwesomeFrontendStubs/$route/gg/sign-in?continue=${URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)}&origin=UCDF"

  def getLoginPage: HttpRequestBuilder =
    http("Get login stub page")
      .get(s"$loginUrl")
      .check(status.is(200))
      .check(saveCsrfToken)

  def loginUser(userId: String): HttpRequestBuilder =
    http("Authenticate a user")
      .post(s"$postSignInUrl")
      .body(StringBody(s"""{
      |  "userId": "$userId",
      |  "planetId":"cdsrc"
      }""".stripMargin))
      .header("Content-Type", "application/json")
      .check(status.in(201, 202))
      .check(saveUserDetailsUrl)
      .check(saveBearerTokenHeader)
      .check(saveSessionIdHeader)
      .check(savePlanetIdHeader)
      .check(saveUserIdHeader)

  def updateUserRole(eoriValue: String): HttpRequestBuilder =
    http("Update current user to have HMRC-CUS-ORG enrolment")
      .put(updateUserUrl)
      .body(StringBody(s"""{
                          |    "affinityGroup" : "Organisation",
                          |    "enrolments" : { "principal": [
                          |              {
                          |                  "key" : "HMRC-CUS-ORG",
                          |                  "identifiers" : [
                          |                    {
                          |                      "key" : "EORINumber",
                          |                      "value" : "$eoriValue"
                          |                    }
                          |                  ]
                          |              }
                          |          ]
                          |    }
                          |}
    """.stripMargin))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{bearerToken}")
      .check(status.is(202))
      .check(header("Location").is("/agents-external-stubs/users/#{userId}"))

  def postSuccessfulLogin: HttpRequestBuilder =
    http("Login with user credentials")
      .post(loginSubmitUrl)
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("userId", "#{userId}")
      .formParam("planetId", "#{planetId}")
      .check(status.is(303))
      .check(header("Location").is(redirectUrl: String))

  private val csrfPattern           = """<input type="hidden" name="csrfToken" value="([^"]+)"""
  private val userDetailsUrlPattern = s"""([^"]+)"""

  private def saveCsrfToken: CheckBuilder[RegexCheckType, String] =
    regex(_ => csrfPattern).saveAs("csrfToken")

  private def saveBearerTokenHeader: CheckBuilder[HttpHeaderRegexCheckType, Response] =
    headerRegex("Authorization", """Bearer\s([^"]+)""").saveAs("bearerToken")

  private def saveSessionIdHeader: CheckBuilder[HttpHeaderCheckType, Response] =
    header("X-Session-ID").saveAs("sessionId")

  private def savePlanetIdHeader: CheckBuilder[HttpHeaderCheckType, Response] =
    header("X-Planet-ID").saveAs("planetId")

  private def saveUserIdHeader: CheckBuilder[HttpHeaderCheckType, Response] =
    header("X-User-ID").saveAs("userId")

  private def saveUserDetailsUrl: CheckBuilder[HttpHeaderRegexCheckType, Response] =
    headerRegex("Location", userDetailsUrlPattern).saveAs("userDetailsUrl")
}
