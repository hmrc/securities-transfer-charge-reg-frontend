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
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ItmpName}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects

import javax.inject.Inject
import scala.concurrent.Future

type RetrievalFilterResult[A] = Either[Future[Result], A]
type RetrievalFilterFunction[A, B] = A => RetrievalFilterResult[B]

def retrievalError[A]: String => Future[A] =
  name => Future.failed(new UnauthorizedException(s"Retrieval Error: No $name found in request"))

class RetrievalFilter @Inject() (appConfig: FrontendAppConfig, redirects: Redirects):

  import redirects.*

  val enrolledForSTC: Enrolments => Boolean = es =>
    val stcEnrolment = es.getEnrolment(appConfig.stcEnrolmentKey)
    stcEnrolment.exists(_.isActivated)

  val enrolledFilter: RetrievalFilterFunction[Enrolments, Unit] = enrolments =>
    if (enrolledForSTC(enrolments)) Left(redirects.redirectToServiceF)
    else Right(())

  private val affinityCheckFilter: AffinityGroup => RetrievalFilterFunction[Option[AffinityGroup], Unit] = requiredAffinity =>
    case Some(affinity) if affinity == requiredAffinity => Right(())
    case Some(_)                                        => Left(redirectToRegisterF)
    case None                                           => Left(retrievalError("AffinityGroup"))

  val isOrgFilter: RetrievalFilterFunction[Option[AffinityGroup], Unit] = affinityCheckFilter(AffinityGroup.Organisation)

  val ninoPresentFilter: RetrievalFilterFunction[Option[String], String] =
    case Some(nino) => Right(nino)
    case None       => Left(redirectToIVUpliftF)

  val namePresentFilter: RetrievalFilterFunction[Option[ItmpName], (String, String)] =
    case Some(ItmpName(Some(fn), _, Some(ln))) => Right((fn, ln))
    case _                                     => Left(retrievalError("name"))

  val isAdminUserFilter: RetrievalFilterFunction[Option[CredentialRole], Unit] =
    case Some(User)       => Right(())
    case Some(Assistant)  => Left(redirectToAssistantKOPageF)
    case _                => Left(retrievalError("CredentialRole"))

  val providerIdPresentFilter: RetrievalFilterFunction[Option[Credentials], String] = {
    case Some(Credentials(providerId, _)) => Right(providerId)
    case _ => Left(retrievalError("credentials.providerId"))
  }


object RetrievalFilter:

  private def presenceFilter[A]: String => RetrievalFilterFunction[Option[A], A] = name => maybeA =>
    maybeA
      .map(Right.apply)
      .getOrElse(Left(retrievalError(name)))

  val internalIdPresentFilter: RetrievalFilterFunction[Option[String], String] =
    presenceFilter[String]("internalId")

  val affinityGroupPresentFilter: RetrievalFilterFunction[Option[AffinityGroup], AffinityGroup] =
    presenceFilter[AffinityGroup]("AffinityGroup")
