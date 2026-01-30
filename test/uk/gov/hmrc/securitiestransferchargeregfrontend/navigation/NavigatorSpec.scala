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

package uk.gov.hmrc.securitiestransferchargeregfrontend.navigation

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import repositories.FakeSessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.AbstractNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.queries.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{UserAnswers, Mode}
import play.api.libs.json.JsPath
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Call

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.*
class NavigatorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val testCall = routes.JourneyRecoveryController.onPageLoad()

  class TestNavigator(sessionRepository: SessionRepository) extends AbstractNavigator(sessionRepository) {
    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call] =
      Future.successful(testCall)

    override val errorPage: Page => Call = _ => testCall
  }

  "All navigators should" - {
    

    "successfully go to a page" in {
      val result = new TestNavigator(new FakeSessionRepository()).goTo(testCall)
      result.futureValue mustBe testCall
    }
    "store user answers when navigating from a page that requires data" in {
      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.set(any[UserAnswers]())).thenReturn(Future.successful(true))
      val navigator = new TestNavigator(sessionRepository)
      val testPage: Page & Gettable[Boolean] & Settable[Boolean] = new Page with Gettable[Boolean] with Settable[Boolean] {
        override def path: JsPath = JsPath \ "test"
      }
      val userAnswers = UserAnswers("test-id").set(testPage, true).get
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      whenReady(result) { res =>
        res mustBe testCall
        verify(sessionRepository, times(1)).set(userAnswers)
      }
    }
    "fail when storing user answers fails when navigating from a page that requires data" in {

    }
    "return the success page when data is present for data required navigation" in {

    }
    "return the error page when data is missing for data required navigation" in {

    }
    "return the success page when data is present for data dependent navigation" in {

    }
    "return the error page when data is missing for data dependent navigation" in {

    }
    "call the provided function when data is present for data dependent navigation" in {

    }
  }

}
