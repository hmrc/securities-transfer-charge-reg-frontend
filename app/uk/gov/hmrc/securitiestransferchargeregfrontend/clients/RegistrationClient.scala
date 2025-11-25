package uk.gov.hmrc.securitiestransferchargeregfrontend.clients

import com.google.inject.ImplementedBy
import play.api.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[RegistrationClientImpl])
trait RegistrationClient:
  def hasCurrentSubscription: Boolean

class RegistrationClientImpl @Inject()(appConf: FrontendAppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends RegistrationClient with Logging {

  /*  TODO: We will need to implement a check to see if the user has a current subscription
   *  TODO: this will required finding their subscription in EACD and then asking ETMP what the end date on it is.
   */  def hasCurrentSubscription: Boolean = true


}
