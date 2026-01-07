package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.SelectBusinessType

class SelectBusinessTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SelectBusinessType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SelectBusinessType.values.toSeq)

      forAll(gen) {
        selectBusinessType =>

          JsString(selectBusinessType.toString).validate[SelectBusinessType].asOpt.value mustEqual selectBusinessType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!SelectBusinessType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[SelectBusinessType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SelectBusinessType.values.toSeq)

      forAll(gen) {
        selectBusinessType =>

          Json.toJson(selectBusinessType) mustEqual JsString(selectBusinessType.toString)
      }
    }
  }
}
