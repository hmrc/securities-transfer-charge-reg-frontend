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

package forms.individuals

import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.DateOfBirthRegFormProvider

import java.time.{LocalDate, ZoneOffset}

class DateOfBirthRegFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form = new DateOfBirthRegFormProvider()()

  ".value" - {

    val today = LocalDate.now(ZoneOffset.UTC)

    val validData = datesBetween(min = today.minusYears(150), max = today.minusYears(18))

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(
      form,
      "value",
      "dateOfBirthReg.error.required.all"
    )

    "reject dates in the future" in {
      val futureDate = today.plusDays(1)

      val result = form.bind(
        Map(
          "value.day" -> futureDate.getDayOfMonth.toString,
          "value.month" -> futureDate.getMonthValue.toString,
          "value.year" -> futureDate.getYear.toString
        )
      )

      result.errors.map(_.message) must contain("dateOfBirthReg.error.futureDate")
    }

    "reject dates where age is under 18" in {
      val under18Date = today.minusYears(18).plusDays(1)

      val result = form.bind(
        Map(
          "value.day" -> under18Date.getDayOfMonth.toString,
          "value.month" -> under18Date.getMonthValue.toString,
          "value.year" -> under18Date.getYear.toString
        )
      )

      result.errors.map(_.message) must contain("dateOfBirthReg.error.under18")
    }

    "accept dates where age is exactly 18" in {
      val exactly18 = today.minusYears(18)

      val result = form.bind(
        Map(
          "value.day" -> exactly18.getDayOfMonth.toString,
          "value.month" -> exactly18.getMonthValue.toString,
          "value.year" -> exactly18.getYear.toString
        )
      )

      result.errors mustBe empty
    }

    "accept dates where age is exactly 150" in {
      val exactly150 = today.minusYears(150)

      val result = form.bind(
        Map(
          "value.day" -> exactly150.getDayOfMonth.toString,
          "value.month" -> exactly150.getMonthValue.toString,
          "value.year" -> exactly150.getYear.toString
        )
      )

      result.errors mustBe empty
    }

    "accept dates where age is 150 years and some days" in {
      val oneFiftyAndSomeDays = today.minusYears(150).minusDays(200)

      val result = form.bind(
        Map(
          "value.day" -> oneFiftyAndSomeDays.getDayOfMonth.toString,
          "value.month" -> oneFiftyAndSomeDays.getMonthValue.toString,
          "value.year" -> oneFiftyAndSomeDays.getYear.toString
        )
      )

      result.errors mustBe empty
    }

    "reject dates where age is over 150 (151+)" in {
      val over150 = today.minusYears(151)

      val result = form.bind(
        Map(
          "value.day" -> over150.getDayOfMonth.toString,
          "value.month" -> over150.getMonthValue.toString,
          "value.year" -> over150.getYear.toString
        )
      )

      result.errors.map(_.message) must contain("dateOfBirthReg.error.pastDate")
    }
  }
}

