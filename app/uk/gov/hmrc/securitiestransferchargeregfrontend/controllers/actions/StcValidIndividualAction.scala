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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions

import com.google.inject.Inject
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.filters.RetrievalFilter.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcValidIndividualRequest

import scala.concurrent.{ExecutionContext, Future}

trait StcValidIndividualAction extends ActionBuilder[StcValidIndividualRequest, AnyContent]

class StcValidIndividualActionImpl @Inject()( override val authConnector: AuthConnector,
                                              config: FrontendAppConfig,
                                              retrievalFilter: RetrievalFilter,
                                              val parser: BodyParsers.Default )
                                            ( implicit val executionContext: ExecutionContext)
  extends StcValidIndividualAction with AuthorisedFunctions {

  private[actions] val retrievals = internalId and allEnrolments and affinityGroup and confidenceLevel and nino and itmpName

  // Enrich the request with auth details or redirect to login/unauthorised
  override def invokeBlock[A](request: Request[A], block: StcValidIndividualRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(retrievals) {
      case maybeInternalId ~ enrolments ~ maybeAffinityGroup ~ confidenceLevel ~ maybeNino ~ maybeName =>

        val maybeRequest = for {
          internalId    <- internalIdPresentFilter(maybeInternalId)
          _             <- retrievalFilter.enrolledFilter(enrolments)
          _             <- retrievalFilter.isIndividualFilter(maybeAffinityGroup)
          _             <- retrievalFilter.confidenceLevelFilter(confidenceLevel)
          nino          <- retrievalFilter.ninoPresentFilter(maybeNino)
          ns            <- retrievalFilter.namePresentFilter(maybeName)
        } yield StcValidIndividualRequest(request, internalId, nino, ns._1, ns._2)

        maybeRequest match {
          case Right(authRequest) => block(authRequest)
          case Left(futureResult) => futureResult
        }

    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

