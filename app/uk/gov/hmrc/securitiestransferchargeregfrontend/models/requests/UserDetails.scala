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

package uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

case class UserDetails(
                        firstName: Option[String],
                        lastName: Option[String],
                        affinityGroup: AffinityGroup,
                        confidenceLevel: ConfidenceLevel,
                        nino: Option[String]
                      )

object UserDetails {

  def fromRetrieval(
                     name: Option[ItmpName],
                     affinityGroup: AffinityGroup,
                     confidenceLevel: ConfidenceLevel,
                     nino: Option[String]
                   ): UserDetails =
    UserDetails(
      firstName = name.flatMap(_.givenName),
      lastName = name.flatMap(_.familyName),
      affinityGroup = affinityGroup,
      confidenceLevel = confidenceLevel,
      nino = nino
    )

  implicit val format: OFormat[UserDetails] = Json.format

  object IndividualUserDetails {
    def unapply(arg: UserDetails): Boolean = arg.affinityGroup == Individual
  }

}
