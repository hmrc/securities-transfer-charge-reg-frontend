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
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.PartnershipKickOutView
import views.ViewBaseSpec

class PartnershipKickOutViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[PartnershipKickOutView]
  private val appConfig         = app.injector.instanceOf[FrontendAppConfig]

  def view(): Document = Jsoup.parse(
    viewInstance(appConfig)(fakeRequest, messages).body
  )

  object ExpectedMessages {
    val title = "You cannot use this service"
    val heading = "You cannot use this service"

    val para1Value = "You cannot use your business tax account to access this service."
    val para2Value = "You will need to use your personal One Login or Government Gateway account."
    val para3Value = "If you do not have a One Login or Government Gateway account you will need to create one using your personal details."
  }

  "The PartnershipKickOutView" - {
    "set up a view" - {
      val page = view()

      "have the correct title" in {
        page.title must include(ExpectedMessages.title)
      }

      "have the correct heading" in {
        page.select("h1").text() mustBe ExpectedMessages.heading
      }

      "display the correct paragraph content" in {

        page.para(1) mustBe Some(ExpectedMessages.para1Value)
        page.para(2) mustBe Some(ExpectedMessages.para2Value)
        page.para(3) mustBe Some(ExpectedMessages.para3Value)
      }
    }
  }

}
