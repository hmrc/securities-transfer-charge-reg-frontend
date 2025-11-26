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

package uk.gov.hmrc.securitiestransferchargeregfrontend.clients

import com.google.inject.ImplementedBy
import play.api.Logging

import javax.inject.Inject

@ImplementedBy(classOf[RegistrationClientImpl])
trait RegistrationClient:
  def hasCurrentSubscription: Boolean

class RegistrationClientImpl @Inject() /*(appConf: FrontendAppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) */extends RegistrationClient with Logging {

  /*  TODO: We will need to implement a check to see if the user has a current subscription
   *  TODO: this will required finding their subscription in EACD and then asking ETMP what the end date on it is.
   */  def hasCurrentSubscription: Boolean = true


}
