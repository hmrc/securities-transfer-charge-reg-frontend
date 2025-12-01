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
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.CheckYourDetailsView
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode

class CheckYourDetailsViewSpec extends ViewBaseSpec {

  private val viewInstance         = app.injector.instanceOf[CheckYourDetailsView]
  private val formProvider = new CheckYourDetailsFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, firstName, lastName, nino, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedIndividual {
    val title = "Check your details"
    val heading = "Check your details"
    val continue = "Continue"

    val hintValue = "Your details"
    
    val summaryCard1Key = "First name"
    val summaryCard1Value = "TestFirstName"
    val summaryCard2Key = "Last name"
    val summaryCard2Value = "TestLastName"
    val summaryCard3Key = "National insurance number"
    val summaryCard3Value = "AB 12 34 56 C"
  }

  "The CheckYourDetailsView" - {
    "the user is an Individual" - {
      val individualPage = view()

      "have the correct title" in {
        individualPage.title must include(ExpectedIndividual.title)
      }

      "have the correct heading" in {
        individualPage.select("h1").text() mustBe ExpectedIndividual.heading
      }

      "display the correct hint text" in {
        individualPage.hintText mustBe Some(ExpectedIndividual.hintValue)
      }

      "display the correct content in the Summary Card" in {
        individualPage.summaryCardRow(1) mustBe Some(
          SummaryCardRow(ExpectedIndividual.summaryCard1Key, ExpectedIndividual.summaryCard1Value)
        )
        individualPage.summaryCardRow(2) mustBe Some(
          SummaryCardRow(ExpectedIndividual.summaryCard2Key, ExpectedIndividual.summaryCard2Value)
        )
        individualPage.summaryCardRow(3) mustBe Some(
          SummaryCardRow(ExpectedIndividual.summaryCard3Key, ExpectedIndividual.summaryCard3Value)
        )

      }
    }
  }

}
