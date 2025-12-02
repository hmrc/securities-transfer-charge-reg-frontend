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

package uk.gov.hmrc.securitiestransferchargeregfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.RegForSecuritiesTransferChargeView

class RegForSecuritiesTransferChargeViewSpec extends ViewBaseSpec {

  private val viewInstance         = app.injector.instanceOf[RegForSecuritiesTransferChargeView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedIndividual {
    val title = "Register to tell us about a securities transfer"
    val heading = "Register to tell us about a securities transfer"
    val continue = "Continue"

    val para1Value = "Before using this service you will need to register and confirm or provide some personal details."
    val para2Value = "You will only have to do this the first time you use the online service. These details will not be added to your GOV.UK One Login or Government Gateway account."
  }

  "The RegForSecuritiesTransferChargeView" - {
    "the user is an Individual" - {
      val individualPage = view()

      "have the correct title" in {
        individualPage.title must include(ExpectedIndividual.title)
      }

      "have the correct heading" in {
        individualPage.select("h1").text() mustBe ExpectedIndividual.heading
      }

      "display the correct paragraph content" in {

        individualPage.para(1) mustBe Some(ExpectedIndividual.para1Value)
        individualPage.para(2) mustBe Some(ExpectedIndividual.para2Value)
      }

      "have a continue button with the correct text" in {
        val button = individualPage.select(".govuk-button")
        button.text() mustBe ExpectedIndividual.continue
      }
    }
  }

}
