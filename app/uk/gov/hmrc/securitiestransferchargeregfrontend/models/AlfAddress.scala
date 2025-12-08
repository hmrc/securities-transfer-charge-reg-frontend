package uk.gov.hmrc.securitiestransferchargeregfrontend.models

import play.api.libs.json.{Json, Reads, Writes}

case class AlfConfirmedAddress( auditRef: String,
                                id: Option[String],
                                address: AlfAddress)

object AlfConfirmedAddress:
  given Reads[AlfConfirmedAddress] = Json.reads[AlfConfirmedAddress]
  given Writes[AlfConfirmedAddress] = Json.writes[AlfConfirmedAddress]

case class AlfAddress(lines: List[String],
                      postcode: String,
                      country: Country)

object AlfAddress:
  given Reads[AlfAddress] = Json.reads[AlfAddress]
  given Writes[AlfAddress] = Json.writes[AlfAddress]

case class Country( code: String,
                    name: String)

object Country:
  given Reads[Country] = Json.reads[Country]
  given Writes[Country] = Json.writes[Country]
