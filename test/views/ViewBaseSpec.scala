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

import base.SpecBase
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.DataRequest
import views.helper.JsoupHelper

trait ViewBaseSpec extends BeforeAndAfterAll with GuiceOneAppPerSuite with JsoupHelper with SpecBase {

  implicit val request: DataRequest[AnyContent] = fakeDataRequest(emptyUserAnswers)

  override def beforeAll(): Unit =
    super.beforeAll()

  override def afterAll(): Unit =
    super.afterAll()

  implicit def messages: Messages = messages(app)
  
}
