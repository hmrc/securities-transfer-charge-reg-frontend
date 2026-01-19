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

package connectors

import base.SpecBase
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.SEE_OTHER
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsInitResult.{GrsInitFailure, GrsInitSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{GrsClient, GrsInitResult}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import scala.concurrent.{ExecutionContext, Future}

class GrsConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val journeyContinueUrl = "http://localhost/journey/continue"
  private val journeyInitUrl = "http://localhost/journey/init"
  private val journeyUrl = "http://we/are/going/on/a/journey"
  private val configurationJson: JsValue = Json.obj("this" -> JsString("is valid json"))

  private val journeyRetrievalUrl = "http://localhost/journey/retrieve"
  private val journeyRetrievalId = "808-42"

  private val validGrsResponse = Json.parse(
    """
      |{
      |  "sautr": "1234567890",
      |  "postcode": "AA11AA",
      |  "identifiersMatch": true,
      |  "businessVerification": {
      |    "verificationStatus": "PASS"
      |  },
      |  "registration": {
      |    "registrationStatus": "REGISTERED",
      |    "registeredBusinessPartnerId": "X00000123456789"
      |  }
      |}""".stripMargin)

  private val responseJsonWithout: String => JsValue = key => validGrsResponse.as[JsObject] - key
  private val responseJsonRegistrationWithout: String => JsValue = key => {
    val updatedReg = (validGrsResponse \ "registration").as[JsObject] - key
    validGrsResponse.as[JsObject] + ("registration" -> updatedReg)
  }
  
  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val ec: ExecutionContext = ExecutionContext.global

  def journeyInitTestSetup(response: GrsInitResult): TestGrsConnector = {
    val grsClient = mock[GrsClient]
    val loader = mock[ResourceLoader]
    when(loader.loadString(anyString())).thenReturn(configurationJson.toString)
    when(grsClient.createGrsJourney(journeyInitUrl)(configurationJson)).thenReturn(Future.successful(response))
    TestGrsConnector(grsClient, loader)
  }

  def journeyResultTestSetup(responseJson: JsValue): TestGrsConnector = {
    val grsClient = mock[GrsClient]
    val loader = mock[ResourceLoader]
    when(loader.loadString(anyString())).thenReturn(configurationJson.toString)
    when(grsClient.retrieveGrsResponse(journeyRetrievalUrl)(journeyRetrievalId)).thenReturn(Future.successful(responseJson))
    TestGrsConnector(grsClient, loader)
  }

  class TestGrsConnector(grsClient: GrsClient,
                         resourceLoader: ResourceLoader) extends AbstractGrsConnector(grsClient, resourceLoader) {
    override def configuration(continueUrl: String): JsValue = configurationJson

    def retrievalUrl: String = journeyRetrievalUrl
  }

  "The GRS Connector" - {

    "when initialising the GRS journey" - {

      "should redirect to the provided URL when the client returns success" in {
        val clientResult = GrsInitSuccess(journeyUrl)
        val connector = journeyInitTestSetup(clientResult)
        val outcome = connector.initGrsJourney(journeyContinueUrl)(journeyInitUrl)
        status(outcome) mustEqual SEE_OTHER
        redirectLocation(outcome).value must startWith(journeyUrl)
      }
      "should return a failed Future when the client returns failure" in {
        val clientResult = GrsInitFailure("oh dear")
        val connector = journeyInitTestSetup(clientResult)
        val outcome = connector.initGrsJourney(journeyContinueUrl)(journeyInitUrl)
        outcome.failed.futureValue mustBe a[GrsException]
      }
    }
    "when getting the result from the GRS journey" - {

      "should return a success when the returned JSON is successfully parsed" in {
        val connector = journeyResultTestSetup(validGrsResponse)
        val outcome = connector.retrieveGrsResults(journeyRetrievalId)
        outcome.futureValue match {
          case GrsSuccess(utr, safeId) =>
            utr mustEqual "1234567890"
            safeId mustEqual "X00000123456789"
          case _ => fail("Unexpected failure to parse result JSON")
        }
      }
      "should return failure if the registration status returned is not registered." in {
        val notRegisteredJson = Json.obj(
          "registrationStatus" -> "NOT REGISTERED",
          "registeredBusinessPartnerId" -> "X00000123456789"
        )
        val overrideJson = Json.obj("registration" -> notRegisteredJson)
        val jsonToReturn = validGrsResponse.as[JsObject].deepMerge(overrideJson)
        val connector = journeyResultTestSetup(jsonToReturn)
        val outcome = connector.retrieveGrsResults(journeyRetrievalId)
        outcome.futureValue match {
          case GrsFailure(_) => succeed
          case _ => fail("Unexpected success when JSON indicated non-registered entity.")
        }
      }
      "should return failure if no UTR can be found in the response." in {
        val jsonToReturn = responseJsonWithout("sautr")
        val connector = journeyResultTestSetup(jsonToReturn)
        val outcome = connector.retrieveGrsResults(journeyRetrievalId)
        outcome.futureValue match {
          case GrsFailure(_) => succeed
          case _ => fail("Unexpected success when JSON did not contain utr key.")
        }
      }
      "should return failure if no registration status can be found in the response." in {
        val jsonToReturn = responseJsonRegistrationWithout("registrationStatus")
        val connector = journeyResultTestSetup(jsonToReturn)
        val outcome = connector.retrieveGrsResults(journeyRetrievalId)
        outcome.futureValue match {
          case GrsFailure(_) => succeed
          case _ => fail("Unexpected success when JSON did not contain registrationStatus key.")
        }
      }
      "should return failure if no registration ID can be found in the response." in {
        val jsonToReturn = responseJsonRegistrationWithout("registeredBusinessPartnerId")
        val connector = journeyResultTestSetup(jsonToReturn)
        val outcome = connector.retrieveGrsResults(journeyRetrievalId)
        outcome.futureValue match {
          case GrsFailure(_) => succeed
          case _ => fail("Unexpected success when JSON did not contain registeredBusinessPartnerId key.")
        }
      }
    }
  }
}
