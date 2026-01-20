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
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json

case class RegistrationData(id: String,
                            safeId: Option[String] = None,
                            subscriptionId: Option[String] = None,
                            ctUtr: Option[String] = None,
                            lastUpdated: Instant = Instant.now,
                            startedAt: Option[Instant] = None)

object RegistrationData {
  implicit val format: Format[RegistrationData] = Json.format[RegistrationData]
}

trait RegistrationDataRepository {
  def getRegistrationData(id: String): Future[RegistrationData]
  def setSafeId(id: String)(safeId: String): Future[Unit]
  def setSubscriptionId(id: String)(subscriptionId: String): Future[Unit]
  def setStartedAt(id: String): Future[Unit]

  def setCtUtr(id: String)(ctUtr: String): Future[Unit]
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
  ) with RegistrationDataRepository with Logging {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  val emptyRegistrationData: String => RegistrationData = RegistrationData(_)
  
  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def get(id: String): Future[RegistrationData] =
    collection
      .find(byId(id))
      .headOption()
      .flatMap {
        case Some(data) => Future.successful(data)
        case None       => Future.successful(emptyRegistrationData(id))
      }

  private def set(data: RegistrationData): Future[Boolean] = {
    collection
      .replaceOne(
        filter = byId(data.id),
        replacement = data,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  override def getRegistrationData(id: String): Future[RegistrationData] = get(id)

  override def setSafeId(id: String)(safeId: String): Future[Unit] =
    setElement(id, _.copy(safeId = Some(safeId)))

  override def setSubscriptionId(id: String)(subscriptionId: String): Future[Unit] =
    setElement(id, _.copy(subscriptionId = Some(subscriptionId)))

  override def setCtUtr(id: String)(ctUtr: String): Future[Unit] =
    setElement(id, _.copy(ctUtr = Some(ctUtr)))
    
  private def setElement(id: String, updateFn: RegistrationData => RegistrationData): Future[Unit] = for {
    current <- get(id)
    updated = updateFn(current).copy(lastUpdated = clock.instant())
    _      <- set(updated)
  } yield ()

  override def setStartedAt(id: String): Future[Unit] = for {
    current <- get(id)
    _ <- current.startedAt match {
      case Some(_) => Future.unit
      case None =>
        val now = clock.instant()
        val updated = current.copy(
          startedAt   = Some(now),
          lastUpdated = now
        )
        set(updated).map(_ => ())
    }
  } yield ()

  override def clear(id: String): Future[Unit] =
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => ())
}
