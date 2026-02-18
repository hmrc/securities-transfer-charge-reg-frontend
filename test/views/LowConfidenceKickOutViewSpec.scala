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
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.LowConfidenceKickOutView
import views.ViewBaseSpec

class LowConfidenceKickOutViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[LowConfidenceKickOutView]
  private val appConfig         = app.injector.instanceOf[FrontendAppConfig]

  def view(): Document = Jsoup.parse(
    viewInstance(appConfig)(fakeRequest, messages).body
  )

  object ExpectedData {
    val title = "Before you use the service"
    val heading = "Before you use the service"

    val para1Value = "Before you can tell us about a securities transfer you need to provide more information to confirm your identity."
  }

  "The LowConfidenceKickOutView" - {
    "should" - {
      val page = view()

      "have the correct title" in {
        page.title must include(ExpectedData.title)
      }

      "have the correct heading" in {
        page.select("h1").text() mustBe ExpectedData.heading
      }

      "display the correct paragraph content" in {
        page.para(1) mustBe Some(ExpectedData.para1Value)
      }
    }
  }

}
