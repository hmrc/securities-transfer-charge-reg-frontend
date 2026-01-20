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

import base.Fixtures.*
import base.{Fixtures, SpecBase}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, Enrolments, User, Assistant}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcValidOrgRequest

import scala.concurrent.{ExecutionContext, Future}

class StcValidOrgSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  type RetrievalType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ Option[CredentialRole] ~ Option[Credentials]

  def buildRetrieval(maybeInternalId: Option[String] = Some(Fixtures.user),
                     enrolments: Enrolments = Fixtures.emptyEnrolments,
                     maybeAffinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation),
                     maybeCredentialRole: Option[CredentialRole] = Some(User),
                     maybeCredentials: Option[Credentials] = Some(Credentials(Fixtures.credId, Fixtures.providerType))) =
    new ~(
      new ~(
        new~(
          new~(maybeInternalId, enrolments),
            maybeAffinityGroup),
          maybeCredentialRole),
      maybeCredentials
    )

  def testSetup(application: Application, retrievals: RetrievalType): StcValidOrgAction = {

    val appConfig = application.injector.instanceOf[FrontendAppConfig]

    val authConnector = new FakeAuthConnectorSuccess(retrievals)

    val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
    val filters = application.injector.instanceOf[RetrievalFilter]

    new StcValidOrgActionImpl(authConnector, appConfig, filters, bodyParsers)
  }

  "StcValidOrgAction" - {

    "must build a StcValidIOrgRequest and invoke the block when authorised with correct retrievals" in {

      val application = applicationBuilder().build()
      running(application) {
        val action = testSetup(application, buildRetrieval())

        var captured: Option[StcValidOrgRequest[AnyContentAsEmpty.type]] = None

        val result = action.invokeBlock(FakeRequest(), { stc =>
          captured = Some(stc.asInstanceOf[StcValidOrgRequest[AnyContentAsEmpty.type]])
          Future.successful(Results.Ok)
        })

        status(result) mustBe OK
        captured mustBe defined

        val stcRequest = captured.get
        stcRequest.userId mustBe Fixtures.user
      }
    }

    "must fail with an UnauthorisedException if no internalId is found" in {
      val application = applicationBuilder().build()

      running(application) {

        val action = testSetup(application, buildRetrieval(maybeInternalId = None))

        val thrown = recoverToExceptionIf[UnauthorizedException] {
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))
        }

        thrown.map { ex =>
          ex.getMessage must include("Retrieval Error:")
        }
      }
    }

    "must send the user to the service if they are already enrolled" in {
      val application = applicationBuilder().build()
      val appConfig = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val action = testSetup(application, buildRetrieval(enrolments = Fixtures.alreadyEnrolled))
        val result = action.invokeBlock(FakeRequest(), { _ => Future.successful(Results.Ok) })

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include(appConfig.stcServiceUrl)
      }
    }

    "must fail with an UnauthorisedException if no affinity group is found" in {
      val application = applicationBuilder().build()

      running(application) {
        val action = testSetup(application, buildRetrieval(maybeAffinityGroup = None))

        val thrown = recoverToExceptionIf[UnauthorizedException] {
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))
        }

        thrown.map { ex =>
          ex.getMessage must include("Retrieval Error:")
        }
      }

    }

    "must redirect to registration start if no an affinity group other than Organisation is found" in {
      val application = applicationBuilder().build()
      val appConfig = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val action = testSetup(application, buildRetrieval(maybeAffinityGroup = Some(AffinityGroup.Agent)))
        val result = action.invokeBlock(FakeRequest(), { _ => Future.successful(Results.Ok) })

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include(appConfig.registerUrl)
      }
    }

    "must redirect to any assistant user to the assistant KO page" in {
      val application = applicationBuilder().build()
      val appConfig = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val action = testSetup(application, buildRetrieval(maybeCredentialRole = Some(Assistant)))
        val result = action.invokeBlock(FakeRequest(), { _ => Future.successful(Results.Ok) })

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include(appConfig.assistantKickOutUrl)
      }
    }

    "must fail with an UnauthorisedException if no credentials are found" in {
      val application = applicationBuilder().build()

      running(application) {
        val action = testSetup(application, buildRetrieval(maybeCredentials = None))

        val thrown = recoverToExceptionIf[UnauthorizedException] {
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))
        }

        thrown.map { ex =>
          ex.getMessage must include("Retrieval Error:")
        }
      }

    }

  }
}

