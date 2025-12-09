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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.{CheckYourDetailsView, DateOfBirthRegView}
import base.SpecBase
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode

import java.time.LocalDate

class DateOfBirthRegViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[DateOfBirthRegView]
  private val formProvider = new DateOfBirthRegFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedDateOfBirthReg {
    val title = "What’s your date of birth?"
    val pageTitle = "Your details"
    val heading = "What’s your date of birth?"
    val hint = "For example, 27 3 2007."

    val continue = "Continue"
  }

  "The DateOfBirthRegView" - {
    "the user is seeing the date of birth page to register" - {
      val dateOfBirthRegPage = view()

      "have the correct title" in {
        dateOfBirthRegPage.title must include(ExpectedDateOfBirthReg.title)
      }

      "have the correct heading" in {
        dateOfBirthRegPage.select("h1").text() mustBe ExpectedDateOfBirthReg.heading
      }

      "have the correct hint" in {
        dateOfBirthRegPage.select("#value-hint").text() mustBe ExpectedDateOfBirthReg.hint
      }

      "have a continue button with the correct text" in {
        val button = dateOfBirthRegPage.select(".govuk-button")
        button.text() mustBe ExpectedDateOfBirthReg.continue
      }
    }
  }

}