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

package views.individuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.CheckYourDetailsView
import views.ViewBaseSpec

class CheckYourDetailsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = fakeApplication().injector.instanceOf[CheckYourDetailsView]
  private val formProvider = new CheckYourDetailsFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, firstName, lastName, nino, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedIndividual {
    val title = "Check your details"
    val heading = "Check your details"
    val continue = "Continue"

    val caption = "Your details"

    val summaryCard1Key = "First name"
    val summaryCard2Key = "Last name"
    val summaryCard3Key = "National insurance number"

    val summaryCard1Value: String = firstName
    val summaryCard2Value: String = lastName
    val summaryCard3Value: String = nino

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

      "display the correct caption text" in {
        individualPage.select("#more-detail-hint").text() mustBe ExpectedIndividual.caption
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