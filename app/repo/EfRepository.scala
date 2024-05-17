package repo

import com.google.inject.ImplementedBy
import play.api.libs.json.JsObject
import repo.models.{IdSet, Workset, WorksetId}

import scala.concurrent.Future

@ImplementedBy(classOf[EfRepositoryMongoImpl])
trait EfRepository {
  def getVolumesAggNoPos(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]]
  def getWorksetAggNoPos(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]]
  def getVolumesMetadata(ids: IdSet, fields: List[String] = List.empty): Future[List[JsObject]]

  def createWorkset(ids: IdSet): Future[Workset]
  def deleteWorkset(id: WorksetId): Future[Unit]
  def getWorkset(id: WorksetId): Future[Workset]
}
