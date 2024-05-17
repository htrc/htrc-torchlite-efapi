package repo

import akka.stream.Materializer
import exceptions.WorksetNotFoundException
import play.api.Logging
import play.api.libs.json._
import reactivemongo.api.bson._
import repo.models._

import java.time.Instant
import javax.inject.{Inject, Singleton}

// Reactive Mongo imports
import play.modules.reactivemongo.{ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.bson.collection.BSONCollection

// BSON-JSON conversions/collection
import reactivemongo.akkastream.cursorProducer
import reactivemongo.play.json.compat.json2bson.toDocumentReader

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EfRepositoryMongoImpl @Inject()(val reactiveMongoApi: ReactiveMongoApi)
                                     (implicit ec: ExecutionContext, m: Materializer)
  extends EfRepository with ReactiveMongoComponents with Logging {

  protected def featuresAggNoPosCol: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection[BSONCollection]("featuresAggNoPos"))
  protected def metadataCol: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection[BSONCollection]("metadata"))
  protected def worksetsCol: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection[BSONCollection]("worksets"))


  override def getVolumesAggNoPos(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]] = {
    val query = if (ids.isEmpty) document() else document("htid" -> document("$in" -> ids))
    val projFields = BSONDocument(fields.map(f => f -> BSONInteger(1)))
    val projection = document("_id" -> 0) ++ projFields

    featuresAggNoPosCol
      .map(_.find(query, Some(projection)))
      .map(_.cursor[JsObject]())
      .flatMap(_.collect[List]())
  }

  override def getWorksetAggNoPos(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]] =
    ???

  override def getVolumesMetadata(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]] = {
    val query = if (ids.isEmpty) document() else document("htid" -> document("$in" -> ids))
    val projFields = BSONDocument(fields.map(f => f -> BSONInteger(1)))
    val projection = document("_id" -> 0) ++ projFields

    metadataCol
      .map(_.find(query, Some(projection)))
      .map(_.cursor[JsObject]())
      .flatMap(_.collect[List]())
  }

  // db.getCollection("metadata").aggregate([
  //    {
  //        $match: {
  //            htid: { $in: ["hvd.32044103226122", "hvd.32044090301284", "abc.12345123"] }
  //        }
  //    },
  //    {
  //        $group: {
  //            _id: null,
  //            htids: { $push: "$htid" }
  //        }
  //    },
  //    {
  //        $project: {
  //            _id: 0
  //        }
  //    },
  //    {
  //        $addFields: {
  //            created: "$$NOW"
  //        }
  //    },
  //    {
  //        $merge: {
  //            into: "worksets"
  //        }
  //    }
  // ])
  override def createWorkset(ids: IdSet): Future[Workset] = {
    require(ids.nonEmpty)

    val worksetId = BSONObjectID.generate()

    metadataCol
      .flatMap(_
        .aggregateWith[Workset]() { framework =>
          import framework._

          List(
            Match(document("htid" -> document("$in" -> ids))),
            Group(BSONNull)("htids" -> PushField("htid")),
            AddFields(document(
              "_id" -> worksetId,
              "created" -> Instant.now
            )),
            Merge(intoCollection = "worksets", on = List("_id"), None, None, None)
          )
        }
        .headOption
      )
      .flatMap(_ => getWorkset(worksetId.stringify))
  }

  override def deleteWorkset(id: WorksetId): Future[Unit] = {
    val worksetId = BSONObjectID.parse(id).get

    worksetsCol
      .flatMap(_.delete().one(document("_id" -> worksetId)))
      .map(_ => ())
  }

  override def getWorkset(id: WorksetId): Future[Workset] = {
    worksetsCol
      .flatMap(_
        .find(document("_id" -> BSONObjectID.parse(id).get))
        .one[Workset]
        .map {
          case Some(workset) => workset
          case None => throw WorksetNotFoundException(id)
        }
      )
  }
}
