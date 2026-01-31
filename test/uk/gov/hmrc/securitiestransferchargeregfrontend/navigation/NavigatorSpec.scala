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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsPath
import play.api.mvc.Call
import repositories.FakeSessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.AbstractNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.queries.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class NavigatorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  val testPage: Page & Gettable[Boolean] & Settable[Boolean] = new Page with Gettable[Boolean] with Settable[Boolean] {
    override def path: JsPath = JsPath \ "test"
  }
  private val userAnswers = UserAnswers("test-id").set(testPage, true).get
  private val mockSessionRepository = mock[SessionRepository]
  private val testCall = routes.JourneyRecoveryController.onPageLoad()

  private def testSetup(): TestNavigator = {
    when(mockSessionRepository.set(any[UserAnswers]())).thenReturn(Future.successful(()))
    new TestNavigator(mockSessionRepository)
  }

  class TestNavigator(mockSessionRepository: SessionRepository) extends AbstractNavigator(mockSessionRepository) {
    override val errorPage: Page => Call = _ => testCall

    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call] =
      Future.successful(testCall)
  }

  "All navigators should" - {
    "successfully go to a page" in {
      val result = new TestNavigator(new FakeSessionRepository()).goTo(testCall)
      result.futureValue mustBe testCall
    }
    "store user answers when navigating from a page that requires data" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      whenReady(result) { _ =>
        verify(mockSessionRepository, times(1)).set(userAnswers)
      }
    }
    "return the success page when data is present for data required navigation" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, userAnswers, testCall)
      whenReady(result) { res =>
        res mustBe testCall
      }
    }
    "return the error page when data is missing for data required navigation" in {
      val navigator = testSetup()
      val result = navigator.dataRequired(testPage, UserAnswers(""), testCall)
      for {
        res     <- result
        default <- navigator.defaultPage
      } yield {
        res mustBe default
      }
    }
    "return the success page when data is present for data dependent navigation" in {
      val navigator = testSetup()
      val result = navigator.dataDependent(testPage, userAnswers)(_ => testCall)
      whenReady(result) { res =>
        res mustBe testCall
      }
    }
    "return the error page when data is missing for data dependent navigation" in {
      val navigator = testSetup()
      val result = navigator.dataDependent(testPage, UserAnswers(""))(_ => testCall)
      for {
        res     <- result
        default <- navigator.defaultPage
      } yield {
        res mustBe default
      }
    }
    "call the provided function when data is present for data dependent navigation" in {
      val navigator = testSetup()
      val mockMethod = mock[Boolean => Call]
      when(mockMethod.apply(true)).thenReturn(testCall)
      val result = navigator.dataDependent(testPage, userAnswers)(mockMethod)
        whenReady(result) { _ =>
        verify(mockMethod, times(1)).apply(true)
      }
    }
  }

}
