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

package controllers.organisations

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.{redirectLocation, status}
import play.api.http.Status.SEE_OTHER

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.{BaseGrsController, routes as orgRoutes}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.{GrsFailure, GrsSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.GrsPage

class BaseGrsControllerSpec extends SpecBase with MockitoSugar with ScalaFutures:

  private def controllerComponents = mock[MessagesControllerComponents]
  private def registrationDataRepository = mock[RegistrationDataRepository]

  private val testUserId = "User-123"
  private val testUtr = "123456789"
  private val testSafeId = "X123456789-00"
  private val success = Future.successful(())
  private val failure = Future.failed(new Exception("FAILED"))
  private val successCall = orgRoutes.AddressController.onPageLoad()
  private val failureCall = orgRoutes.PartnershipKickOutController.onPageLoad()
  private val userAnswers = UserAnswers(testUserId)
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  def testSetup(repoSetResponse: Future[Unit] = success,
                grsResult: GrsResult = GrsSuccess(testUtr, testSafeId)): Future[Result] = {
    val repo = registrationDataRepository
    when(repo.setCtUtr(testUserId)(testUtr)).thenReturn(repoSetResponse)
    when(repo.setSafeId(testUserId)(testSafeId)).thenReturn(repoSetResponse)
    
    val navigator = mock[Navigator]
    when(navigator.nextPage(GrsPage, NormalMode, userAnswers)).thenReturn(Future.successful(successCall))
    when(navigator.errorPage).thenReturn { (_: Page) => failureCall }
    val controller = new BaseGrsController(controllerComponents, repo, navigator)
    controller.processResponse(userAnswers, grsResult)
  }
  
  "BaseGrsControllerSpec processResponse should" - {

    "upon receiving a success response from GRS" - {
      
      "and successfully storing the results in the repository" - {
        
        "should redirect the user to the next page" in {
          val outcome = testSetup()
          status(outcome) mustEqual SEE_OTHER
          redirectLocation(outcome).value must startWith(successCall.url)
        }
      }
      
      "but failing to store the results in the repository" - {
        
        "should fail the exception" in {
          val outcome = testSetup(repoSetResponse = failure)
          outcome.failed.futureValue mustBe a[Exception]
        }
      }
    }
    
    "upon receiving a failure response from GRS" - {

      "should redirect to the KO page" in {
        val outcome = testSetup(grsResult = GrsFailure("OH NO!"))
        redirectLocation(outcome).value must startWith(failureCall.url)
      }
    }
}
