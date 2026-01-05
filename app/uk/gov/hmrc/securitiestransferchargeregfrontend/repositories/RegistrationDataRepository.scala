/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregfrontend.repositories

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class RegistrationData(id: String, safeId: Option[String], subscriptionId: Option[String], lastUpdated: Instant = Instant.now)
object RegistrationData {
  implicit val format: Format[RegistrationData] = play.api.libs.json.Json.format[RegistrationData]
}

trait RegistrationDataRepository {
  def getRegistrationData(id: String): Future[RegistrationData]
  def setSafeId(id: String)(safeId: String): Future[Unit]
  def setSubscriptionId(id: String)(subscriptionId: String): Future[Unit]
  def clear(id: String): Future[Unit]
}

@Singleton
class RegistrationDataRepositoryImpl @Inject()( mongoComponent: MongoComponent,
                                                appConfig: FrontendAppConfig,
                                                clock: Clock
                                              )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[RegistrationData](
    collectionName = "registration-data",
    mongoComponent = mongoComponent,
    domainFormat   = RegistrationData.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS),
      )
    )
  ) with RegistrationDataRepository {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def get(id: String): Future[Option[RegistrationData]] =
    collection
      .find(byId(id))
      .headOption()

  private def set(id: String, update: Bson): Future[Unit] =
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.combine(
          update,
          Updates.set("lastUpdated", Instant.now(clock))
        ),
        options = new com.mongodb.client.model.UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  override def getRegistrationData(id: String): Future[RegistrationData] =
    get(id).map {
      case Some(data) => data
      case None       => RegistrationData(id, None, None)
    }

  override def setSafeId(id: String)(safeId: String): Future[Unit] = {
    val update: Bson = Updates.set("safeId", safeId)
    set(id, update)
  }

  override def setSubscriptionId(id: String)(subscriptionId: String): Future[Unit] = {
    val update: Bson = Updates.set("subscriptionId", subscriptionId)
    set(id, update)
  }

  override def clear(id: String): Future[Unit] =
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => ())
}
