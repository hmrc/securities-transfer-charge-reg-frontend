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

package uk.gov.hmrc.securitiestransferchargeregfrontend.pages

import play.api.libs.json.JsPath
import uk.gov.hmrc.securitiestransferchargeregfrontend.queries.{Gettable, Settable}

import scala.language.implicitConversions

trait AddressPage[A] extends Page with Gettable[A] with Settable[A]

class AddressPageImpl[A] extends AddressPage[A] {

  override def toString: String = "addressPage"

  override def path: JsPath = JsPath \ toString
}
object AddressPage {
  def apply[A](): AddressPage[A] = {
    new AddressPageImpl()
  }
}