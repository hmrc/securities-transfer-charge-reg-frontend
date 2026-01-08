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

import base.{Fixtures, SpecBase}
import controllers.Execution.trampoline
import org.scalatest.concurrent.IntegrationPatience
import play.api.mvc.{AnyContent, BodyParsers, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import repositories.FakeRegistrationDataRepository
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.EnrolmentCheckImpl
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import scala.concurrent.Future

class EnrolmentCheckSpec extends SpecBase with IntegrationPatience {
  import Fixtures.ec

  // Expose the protected filter method for testing
  class Harness(parser: BodyParsers.Default,
                redirects: Redirects,
                retrievalFilter: RetrievalFilter,
                registrationClient: RegistrationClient,
                registrationDataRepository: RegistrationDataRepository)
               (implicit ec: scala.concurrent.ExecutionContext)
    extends EnrolmentCheckImpl(parser, redirects, retrievalFilter, registrationClient, registrationDataRepository)
  {
    def callFilter[A](req: StcAuthRequest[A]): Future[Option[Result]] = filter(req)
  }

  "EnrolmentCheck" - {

    "must redirect to the service when the user is enrolled and has a current subscription" in {
      val application = applicationBuilder().build()

      running(application) {
        val parser = application.injector.instanceOf[BodyParsers.Default]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val retrievalFilter = application.injector.instanceOf[RetrievalFilter]

        // build an activated enrolment for the configured key
        val enrolment = Enrolment(appConfig.stcEnrolmentKey, Seq(EnrolmentIdentifier("id", "1")), "Activated")
        val enrolments = Enrolments(Set(enrolment))

        val stcReq = Fixtures.fakeStcAuthRequest[AnyContent](request = FakeRequest(), enrolmentsOverride = enrolments)

        val registrationClient = new FakeRegistrationClient(true)
        
        val registrationDataRepository = new FakeRegistrationDataRepository(Fixtures.registrationData)
        val harness = new Harness(parser, redirects, retrievalFilter, registrationClient, registrationDataRepository)

        val maybeResult = harness.callFilter(stcReq)

        val unpackResult: Option[Result] => Result = {
          case Some(result) => result
          case None         => fail("Should not be none")
        }
        
        val futureResult = maybeResult map(unpackResult)
        
        status(futureResult) mustBe SEE_OTHER
        redirectLocation(futureResult) mustBe Some(appConfig.stcServiceUrl)
      }
    }

    "must allow the request to proceed when the user is not enrolled or there is no subscription" in {
      val application = applicationBuilder().build()

      running(application) {
        val parser = application.injector.instanceOf[BodyParsers.Default]
        val redirects = application.injector.instanceOf[Redirects]
        val retrievalFilter = application.injector.instanceOf[RetrievalFilter]

        // empty enrolments
        val enrolments = Enrolments(Set())

        val stcReq = Fixtures.fakeStcAuthRequest[AnyContent](request = FakeRequest(), enrolmentsOverride = enrolments)

        val registrationClient = new FakeRegistrationClient(false)

        val registrationDataRepository = new FakeRegistrationDataRepository(Fixtures.registrationData)
        val harness = new Harness(parser, redirects, retrievalFilter, registrationClient, registrationDataRepository)

        val result = harness.callFilter(stcReq).futureValue

        result must not be defined
      }
    }
  }
}

