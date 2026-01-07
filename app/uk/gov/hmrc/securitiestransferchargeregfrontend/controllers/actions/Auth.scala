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
import play.api.mvc.{ActionBuilder, AnyContent}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

class Auth @Inject()(stcAuthAction: StcAuthAction,
                     enrolmentCheck: EnrolmentCheck,
                     dataRetrievalAction: DataRetrievalAction,
                     dataRequiredAction: DataRequiredAction):
  
  val authorised: StcAuthAction = stcAuthAction
  val authorisedAndNotEnrolled: ActionBuilder[StcAuthRequest, AnyContent] = stcAuthAction andThen enrolmentCheck
  val getData: DataRetrievalAction = dataRetrievalAction
  val requireData: DataRequiredAction = dataRequiredAction

class IndividualAuth @Inject()( stcValidIndividualAction: StcValidIndividualAction,
                                validIndividualDataRetrievalAction: ValidIndividualDataRetrievalAction,
                                validIndividualDataRequiredAction: ValidIndividualDataRequiredAction):

  val validIndividual : StcValidIndividualAction = stcValidIndividualAction
  val getData: ValidIndividualDataRetrievalAction = validIndividualDataRetrievalAction
  val requireData: ValidIndividualDataRequiredAction = validIndividualDataRequiredAction

class OrgAuth @Inject()(stcValidOrgAction: StcValidOrgAction,
                        validOrgDataRetrievalAction: ValidOrgDataRetrievalAction,
                        validOrgDataRequiredAction: ValidOrgDataRequiredAction):

  val validOrg : StcValidOrgAction = stcValidOrgAction
  val getData: ValidOrgDataRetrievalAction = validOrgDataRetrievalAction
  val requireData: ValidOrgDataRequiredAction = validOrgDataRequiredAction