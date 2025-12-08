package connectors

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import play.api.mvc.*


@Singleton
class AlfAddressConnector @Inject() ( ws: WSClient,,
                                      implicit val config: FrontendAppConfig) {

  def initAlfJourneyRequest(): Future[WSResponse] = {
    ws
      .url(config.alfUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
  }

  def alfRetrieveAddress(key: String): Future[WSResponse] = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    ws
      .url(retrieveAddress)
      .get()
  }
  
  private val payload: JsValue = {
    // Implementation to create the payload
    Json.parse("""{
      "version" : 2,
      "options" : {
        "continueUrl" : "http://localhost:9000/register-securities-transfer-charge/address/return",
        "useNewGovUkServiceNavigation" : false
      },
      "labels" : {
        "en" : {
          "appLevelLabels" : {
            "navTitle" : "Address Lookup Example"
          },
          "selectPageLabels" : { },
          "lookupPageLabels" : { },
          "editPageLabels" : { },
          "confirmPageLabels" : { },
          "countryPickerLabels" : { },
          "international" : { },
          "otherLabels" : { }
        },
        "cy" : {
          "appLevelLabels" : {
            "navTitle" : "Address Lookup Example (Welsh)"
          },
          "selectPageLabels" : { },
          "lookupPageLabels" : { },
          "editPageLabels" : { },
          "confirmPageLabels" : { },
          "countryPickerLabels" : { },
          "international" : { },
          "otherLabels" : { }
        }
      }
    }""")
  } 
}


