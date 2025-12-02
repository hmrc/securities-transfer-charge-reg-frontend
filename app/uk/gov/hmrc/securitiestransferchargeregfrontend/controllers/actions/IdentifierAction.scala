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
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserDetails
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
  extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]


class AuthenticatedIdentifierAction @Inject()(
                                               override val authConnector: AuthConnector,
                                               config: FrontendAppConfig,
                                               val parser: BodyParsers.Default
                                             )(implicit ec: ExecutionContext)
  extends IdentifierAction
    with AuthorisedFunctions
    with Logging {
  
  private val retrievals =
    Retrievals.internalId     and
      Retrievals.affinityGroup  and
      Retrievals.confidenceLevel and
      Retrievals.nino           and
      Retrievals.itmpName

  override def invokeBlock[A](
                               request: Request[A],
                               block: IdentifierRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(retrievals) {
      
      case internalIdOpt ~ affinityOpt ~ confidence ~ maybeNino ~ maybeName =>

        (internalIdOpt, affinityOpt) match {
          case (Some(internalId), Some(affinityGroup)) =>

            val userDetails = UserDetails.fromRetrieval(
              name            = maybeName,
              affinityGroup   = affinityGroup,
              confidenceLevel = confidence,
              nino            = maybeNino
            )

            logger.info(
              s"[AuthenticatedIdentifierAction] Authenticated internalId=$internalId, " +
                s"name=${userDetails.firstName.getOrElse("-")} ${userDetails.lastName.getOrElse("-")}"
            )

            block(
              IdentifierRequest(
                request     = request,
                userId      = internalId,
                userDetails = userDetails
              )
            )

          case _ =>
            throw new UnauthorizedException("Missing internalId or affinityGroup from auth retrievals")
        }

    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))

      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  override protected def executionContext: ExecutionContext = ec
}

class SessionIdentifierAction @Inject()(
                                         val parser: BodyParsers.Default
                                       )
                                       (implicit val executionContext: ExecutionContext) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>

        val dummyUserDetails = UserDetails(
          firstName       = None,
          lastName        = None,
          affinityGroup   = AffinityGroup.Individual,
          confidenceLevel = ConfidenceLevel.L250,
          nino            = None
        )
        
        block(IdentifierRequest(request, session.value, dummyUserDetails))
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
