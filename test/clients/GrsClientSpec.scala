/*
 * Copyright 2026 HM Revenue & Customs
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

package clients

import base.SpecBase
import controllers.actions.FakeFailingAuthConnector
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, running, status}
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.AuthenticatedIdentifierAction
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import play.api.http.Status.{CREATED, OK}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsInitResult.{GrsInitFailure, GrsInitSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsClientImpl
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import play.api.libs.ws.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

class GrsClientSpec extends SpecBase with MockitoSugar with ScalaFutures {

  def httpClient: HttpClientV2 = mock[HttpClientV2]
  def httpResponse: HttpResponse = mock[HttpResponse]

  val journeyCreationUrl = "http://localhost/journey/create"
  val journeyUrl = "/we/are/going/on/a/journey"
  val configuration: JsValue = JsString("My test config")
  val successJson = Json.obj("journeyStartUrl" -> JsString(journeyUrl))
  
  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  def journeyInitTestSetup( status: Int = CREATED,
                            body: String = successJson.toString): HttpClientV2 = {

    val http = httpClient
    val resp = httpResponse
    val builder = mock[RequestBuilder]
    when(resp.status).thenReturn(status)
    when(resp.body).thenReturn(body)

    when(http.post(url"$journeyCreationUrl")).thenReturn(builder)
    when(builder.withBody[JsValue](configuration)).thenReturn(builder)
    when(builder.execute[HttpResponse]).thenReturn(Future.successful(resp))
    http
  }
                          
  
  "The GRS Client" - {

    "when initialising the GRS journey" - {

      "should return the journey URL when initialisation is successful" in {
        val http = journeyInitTestSetup()
        val expected = GrsInitSuccess(journeyUrl)
        val outcome = new GrsClientImpl(http).createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue mustEqual expected
      }
      "should return an error if initialisation is successful but no journey URL can be found" in {
        val unexpectedJson = Json.obj("not" -> JsString("expected"))
        val http = journeyInitTestSetup(body = unexpectedJson.toString)
        val outcome = new GrsClientImpl(http).createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when response JSON was incorrect.")
        }
      }
      "should return an error if the journey create call does not return CREATED" in {
        val http = journeyInitTestSetup(status = OK)
        val outcome = new GrsClientImpl(http).createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when init create HTTP response code was not CREATED.")
        }
      }
      "should return an error if the journey create call does not return valid json" in {
        val http = journeyInitTestSetup(body = "NOT JSON")
        val outcome = new GrsClientImpl(http).createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when init create HTTP response code was not CREATED.")
        }
      }
    }
  }
}
