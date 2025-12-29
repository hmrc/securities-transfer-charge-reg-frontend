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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.filters

import play.api.mvc.Result
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects

import javax.inject.Inject
import scala.concurrent.Future

type RetrievalFilterResult[A] = Either[Future[Result], A]
type RetrievalFilterFunction[A, B] = A => RetrievalFilterResult[B]

class RetrievalFilter @Inject() (appConfig: FrontendAppConfig,
                                 redirects: Redirects) {

  import redirects.*

  val enrolledForSTC: Enrolments => Boolean = es => {
    val stcEnrolment = es.getEnrolment(appConfig.stcEnrolmentKey)
    stcEnrolment.exists(_.isActivated)
  }

  val enrolledFilter: RetrievalFilterFunction[Enrolments, Unit] = enrolments =>
    if (enrolledForSTC(enrolments)) Left(redirects.redirectToServiceF)
    else Right(())

  val isIndividualFilter: RetrievalFilterFunction[Option[AffinityGroup], Unit] =
    case Some(AffinityGroup.Individual) => Right(())
    case Some(_) => Left(redirectToRegisterF)
    case None => Left(Future.failed(new UnauthorizedException("Retrieval Error: No AffinityGroup found in request")))

  val confidenceLevelFilter: RetrievalFilterFunction[ConfidenceLevel, Unit] = confidenceLevel =>
    if (confidenceLevel >= ConfidenceLevel.L250) Right(())
    else Left(redirectToIVUpliftF)

  val ninoPresentFilter: RetrievalFilterFunction[Option[String], String] =
    case Some(nino) => Right(nino)
    case None => Left(redirectToIVUpliftF)

  val namePresentFilter: RetrievalFilterFunction[Option[ItmpName], (String, String)] =
    case Some(ItmpName(Some(fn), _, Some(ln))) => Right((fn, ln))
    case _                                     => Left(redirectToRegisterF)

}

object RetrievalFilter {

  val internalIdPresentFilter: RetrievalFilterFunction[Option[String], String] = maybeInternalId =>
     maybeInternalId
       .map(Right.apply)
       .getOrElse(Left(Future.failed(new UnauthorizedException("Retrieval Error: No internalId found in request"))))

  val affinityGroupPresentFilter: RetrievalFilterFunction[Option[AffinityGroup], AffinityGroup] = maybeAffinityGroup =>
    maybeAffinityGroup
      .map(Right.apply)
      .getOrElse(Left(Future.failed(new UnauthorizedException("Retrieval Error: No AffinityGroup found in request"))))
  

}

