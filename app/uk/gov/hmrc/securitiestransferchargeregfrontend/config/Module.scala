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

package uk.gov.hmrc.securitiestransferchargeregfrontend.config

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{GrsClient, GrsClientImpl}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.{RegistrationClient, RegistrationClientImpl}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.*

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
    bind(classOf[ValidIndividualDataRequiredAction]).to(classOf[ValidIndividualDataRequiredActionImpl])
    bind(classOf[ValidIndividualDataRetrievalAction]).to(classOf[ValidIndividualDataRetrievalActionImpl])
    bind(classOf[ValidOrgDataRetrievalAction]).to(classOf[ValidOrgDataRetrievalActionImpl])
    bind(classOf[ValidOrgDataRequiredAction]).to(classOf[ValidOrgDataRequiredActionImpl])

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    bind(classOf[StcAuthAction]).to(classOf[AuthenticatedStcAction])
    bind(classOf[StcValidIndividualAction]).to(classOf[StcValidIndividualActionImpl])
    bind(classOf[StcValidOrgAction]).to(classOf[StcValidOrgActionImpl])

    bind(classOf[RegistrationClient]).to(classOf[RegistrationClientImpl]).asEagerSingleton()

    bind(classOf[SessionRepository]).to(classOf[SessionRepositoryImpl])
    bind(classOf[EnrolmentCheck]).to(classOf[EnrolmentCheckImpl])
    bind(classOf[AlfAddressConnector]).to(classOf[AlfAddressConnectorImpl])
    
    bind(classOf[RegistrationDataRepository]).to(classOf[RegistrationDataRepositoryImpl])
    bind(classOf[RegistrationConnector]).to(classOf[RegistrationConnectorImpl])
    bind(classOf[SubscriptionConnector]).to(classOf[SubscriptionConnectorImpl])

    bind(classOf[Navigator])
      .annotatedWith(Names.named("individuals"))
      .to(classOf[IndividualsNavigator])

    bind(classOf[Navigator])
      .annotatedWith(Names.named("organisations"))
      .to(classOf[OrgNavigator])
    
    bind(classOf[GrsClient]).to(classOf[GrsClientImpl]).asEagerSingleton()
  }
  
}
