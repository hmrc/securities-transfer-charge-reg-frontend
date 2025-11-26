/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import base.SpecBase
import play.api.mvc.Results
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.EnrolmentCheckImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class EnrolmentCheckSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  class FakeSuccessAuthConnector[A](value: A) extends AuthConnector {
    val serviceUrl: String = ""
    override def authorise[T](predicate: uk.gov.hmrc.auth.core.authorise.Predicate, retrieval: Retrieval[T])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
      Future.successful(value.asInstanceOf[T])
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""
    override def authorise[T](predicate: uk.gov.hmrc.auth.core.authorise.Predicate, retrieval: Retrieval[T])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
      Future.failed(exceptionToReturn)
  }

  "EnrolmentCheck" - {

    "redirect to service when user is enrolled and has current subscription" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        // create an enrolment that matches the configured key
        val enrolment = Enrolment(appConfig.stcEnrolmentKey, Seq(EnrolmentIdentifier("id", "1")), "Activated")
        val enrolments = Enrolments(Set(enrolment))

        val authConnector = new FakeSuccessAuthConnector[Enrolments](enrolments)

        val registrationClient = new RegistrationClient {
          override def hasCurrentSubscription: Boolean = true
        }

        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val result = action.invokeBlock(FakeRequest(), (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.stcServiceUrl)
      }
    }

    "redirect to register when user is not enrolled" in {
      val application = applicationBuilder()
        .configure()
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        val enrolments = Enrolments(Set.empty)
        val authConnector = new FakeSuccessAuthConnector[Enrolments](enrolments)

        val registrationClient = new RegistrationClient {
          override def hasCurrentSubscription: Boolean = true
        }

        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val result = action.invokeBlock(FakeRequest(), (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerUrl)
      }
    }

    "redirect to IV uplift when user is enrolled but does not have current subscription" in {
      val application = applicationBuilder()
        .configure()
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        // create an enrolment that matches the configured key
        val key = Random().nextInt()
        val enrolment = Enrolment(appConfig.stcEnrolmentKey, Seq(EnrolmentIdentifier("id", key.toString)), "Activated")
        val enrolments = Enrolments(Set(enrolment))

        val authConnector = new FakeSuccessAuthConnector[Enrolments](enrolments)

        val registrationClient = new RegistrationClient {
          override def hasCurrentSubscription: Boolean = false
        }

        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val result = action.invokeBlock(FakeRequest(), (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        // EnrolmentCheckImpl redirects to the registration page when the user lacks a current subscription
        redirectLocation(result) mustBe Some(appConfig.registerUrl)
      }
    }

    "redirect to login on other failures" in {
      val application = applicationBuilder()
        .configure()
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        val authConnector = new FakeFailingAuthConnector(new MissingBearerToken)

        val registrationClient = new RegistrationClient {
          override def hasCurrentSubscription: Boolean = true
        }

        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val result = action.invokeBlock(FakeRequest(), (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.loginUrl)
      }
    }
  }
}
