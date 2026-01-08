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

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.WhatsYourEmailAddressFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.SelectBusinessTypeFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.SelectBusinessTypeView
import views.ViewBaseSpec

class SelectBusinessTypeViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance: SelectBusinessTypeView = app.injector.instanceOf[SelectBusinessTypeView]
  private val formProvider = new SelectBusinessTypeFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedMessages {
    val title = "Select your business type"
    val heading = "Select your business type"
    val continue = "Continue"
  }

  "The SelectBusinessTypeView" - {
    
    "create a view" - {
      val businessTypeView = view()

      "have the correct title" in {
        businessTypeView.title must include(ExpectedMessages.title)
      }

      "have the correct heading" in {
        businessTypeView.select("h1").text() mustBe ExpectedMessages.heading
      }

      "have a continue button with the correct text" in {
        val button = businessTypeView.select(".govuk-button")
        button.text() mustBe ExpectedMessages.continue
      }
    }
  }

}
