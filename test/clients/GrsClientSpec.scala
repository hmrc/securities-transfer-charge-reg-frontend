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
import com.fasterxml.jackson.core.JsonParseException
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import play.api.test.Helpers.{redirectLocation, running, status}
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{GrsClientImpl, GrsClient}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsInitResult.{GrsInitFailure, GrsInitSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.AuthenticatedIdentifierAction

import scala.concurrent.{ExecutionContext, Future}

class GrsClientSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private def httpClient: HttpClientV2 = mock[HttpClientV2]
  private def httpResponse: HttpResponse = mock[HttpResponse]

  private val journeyCreationUrl = "http://localhost/journey/create"
  private val journeyUrl = "/we/are/going/on/a/journey"
  private val configuration: JsValue = JsString("My test config")
  private val successJson = Json.obj("journeyStartUrl" -> JsString(journeyUrl))

  private val journeyRetrievalUrl = "http://localhost/journey/retrieve"
  private val journeyRetrievalId = "808-42"
  private val validJourneyRetrieval = Json.obj("this" -> JsString("is valid json"))
  private val invalidJourneyRetrieval = "NOT JSON"

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  def journeyInitTestSetup( status: Int = CREATED,
                            body: String = successJson.toString): GrsClient = {

    val http = httpClient
    val resp = httpResponse
    val builder = mock[RequestBuilder]
    when(resp.status).thenReturn(status)
    when(resp.body).thenReturn(body)

    when(http.post(url"$journeyCreationUrl")).thenReturn(builder)
    when(builder.withBody[JsValue](configuration)).thenReturn(builder)
    when(builder.execute[HttpResponse]).thenReturn(Future.successful(resp))
    GrsClientImpl(http)
  }

  def journeyResultTestSetup(body: String = validJourneyRetrieval.toString): GrsClient = {

    val http = httpClient
    val resp = httpResponse
    val builder = mock[RequestBuilder]
    when(resp.body).thenReturn(body)

    when(http.get(url"$journeyRetrievalUrl/$journeyRetrievalId")).thenReturn(builder)
    when(builder.execute[HttpResponse]).thenReturn(Future.successful(resp))
    new GrsClientImpl(http)

  }
  
  "The GRS Client" - {

    "when initialising the GRS journey" - {

      "should return the journey URL when initialisation is successful" in {
        val client = journeyInitTestSetup()
        val expected = GrsInitSuccess(journeyUrl)
        val outcome = client.createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue mustEqual expected
      }
      "should return an error if initialisation is successful but no journey URL can be found" in {
        val unexpectedJson = Json.obj("not" -> JsString("expected"))
        val client = journeyInitTestSetup(body = unexpectedJson.toString)
        val outcome = client.createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when response JSON was incorrect.")
        }
      }
      "should return an error if the journey create call does not return CREATED" in {
        val client = journeyInitTestSetup(status = OK)
        val outcome = client.createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when init create HTTP response code was not CREATED.")
        }
      }
      "should return an error if the journey create call does not return valid json" in {
        val client = journeyInitTestSetup(body = "NOT JSON")
        val outcome = client.createGrsJourney(journeyCreationUrl)(configuration)
        outcome.futureValue match {
          case GrsInitFailure(_) => succeed
          case _ => fail("Journey creation should not have succeeded when init create HTTP response code was not CREATED.")
        }
      }
    }
    "when getting the result from the GRS journey" - {
      
      "should return a successful future when the response is valid JSON" in {
        val client = journeyResultTestSetup()
        val outcome = client.retrieveGrsResponse(journeyRetrievalUrl)(journeyRetrievalId)
        outcome.futureValue.toString mustEqual validJourneyRetrieval.toString
      }
      "should return a failed future when the response is not valid JSON" in {
        val client = journeyResultTestSetup(invalidJourneyRetrieval)
        val outcome = client.retrieveGrsResponse(journeyRetrievalUrl)(journeyRetrievalId)
        outcome.failed.futureValue mustBe a[JsonParseException]
      }
    }
  }
}
