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

package uk.gov.hmrc.securitiestransferchargeregfrontend.utils

import play.api.Environment

import java.util.MissingResourceException
import javax.inject.{Inject, Singleton}
import scala.io.Source

@Singleton
class ResourceLoader @Inject()(env: Environment) {
  private val className = getClass[ResourceLoader].toString
  def loadString(path: String): String = {
    val is = env.resourceAsStream(path).getOrElse {
      throw new MissingResourceException(s"Resource not found: $path", className, path)
    }
    try Source.fromInputStream(is, "UTF-8").mkString
    finally is.close()
  }
}

