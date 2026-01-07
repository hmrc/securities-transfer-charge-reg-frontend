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

package views.organisations

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.UpdateDetailsKickOutView
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.UkOrNotKickOutView
import views.ViewBaseSpec

class UkOrNotKickOutViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[UkOrNotKickOutView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedMessages {
    val title = "You cannot use this service"
    val heading = "You cannot use this service"

    val para1Value = "To tell us about a securities transfer you will need to use a UK agent who can provide information to HMRC on your behalf."

  }

  "The RegForSecuritiesTransferChargeView" - {
    "the user is an Individual" - {
      val individualPage = view()

      "have the correct title" in {
        individualPage.title must include(ExpectedMessages.title)
      }

      "have the correct heading" in {
        individualPage.select("h1").text() mustBe ExpectedMessages.heading
      }

      "display the correct paragraph content" in {

        individualPage.para(1) mustBe Some(ExpectedMessages.para1Value)
      }
    }
  }

}
