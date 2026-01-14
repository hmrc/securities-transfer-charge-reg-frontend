/*
 * Copyright 2026 HM Revenue & Customs
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

package repositories

import base.Fixtures
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.{RegistrationData, RegistrationDataRepository}

import scala.concurrent.Future

class FakeRegistrationDataRepository(data: RegistrationData = Fixtures.registrationData) extends RegistrationDataRepository {

  override def getRegistrationData(id: String): Future[RegistrationData] =
    Future.successful(data)

  override def setSafeId(id: String)(safeId: String): Future[Unit] = Future.successful(())

  override def setSubscriptionId(id: String)(subscriptionId: String): Future[Unit] = Future.successful(())

  override def clear(id: String): Future[Unit] = Future.successful(())

  override def setCtUtr(id: String)(ctUtr: String): Future[Unit] = Future.successful(())
}
