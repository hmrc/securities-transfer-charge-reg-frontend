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
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.UkOrNotFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.UkOrNotView
import views.ViewBaseSpec

class UkOrNotViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = fakeApplication().injector.instanceOf[UkOrNotView]
  private val formProvider = new UkOrNotFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedMessages {
    val title = "Does your company operate in the UK?"
    val heading = "Does your company operate in the UK?"
    val continue = "Continue"
    val caption = "Your company details"
  }

  "The UkOrNotView" - {
    "set up a view" - {
      val ukOrNotPage = view()

      "have the correct title" in {
        ukOrNotPage.title must include(ExpectedMessages.title)
      }

      "have the correct heading" in {
        ukOrNotPage.select("h1").text() must include(ExpectedMessages.heading)
    }

      "display the correct caption text" in {
        ukOrNotPage.select("span.hmrc-caption").text() must include(ExpectedMessages.caption)
      }
    }
  }
}