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
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.ContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.ContactNumberView
import views.ViewBaseSpec
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes

class ContactNumberViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[ContactNumberView]
  private val formProvider = new ContactNumberFormProvider()
  private val form = formProvider()

  val backLinkRoute: Call = routes.ContactEmailAddressController.onPageLoad(NormalMode)

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, backLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedMessages {
    val title = "Enter a contact phone number"
    val heading = "Enter a contact phone number"
    val hint = "For international numbers, include the country code."
  }

  "ContactNumberView" - {
    "create a view" - {
      val page = view()

      "have the correct title" in {
        page.title must include(ExpectedMessages.title)
      }

      "have the correct heading" in {
        page.select("h1").text() mustBe ExpectedMessages.heading
      }

      "have the correct hint" in {
        page.select("#value-hint").text() mustBe ExpectedMessages.hint
      }

    }
  }

}
