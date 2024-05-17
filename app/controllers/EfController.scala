package controllers

import io.swagger.annotations.ApiParam
import play.api.mvc._
import protocol.WrappedResponse
import repo.EfRepository
import repo.models.{VolumeId, WorksetId}
import utils.Helper.tokenize
import utils.IdUtils._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EfController @Inject()(efRepository: EfRepository,
                             components: ControllerComponents)
                            (implicit val ec: ExecutionContext) extends AbstractController(components) {

  def createWorkset(): Action[String] =
    Action.async(parse.text) { implicit req =>
      render.async {
        case Accepts.Json() =>
          val ids = tokenize(req.body, delims = " \n").toSet
          efRepository.createWorkset(ids).map(WrappedResponse(_))
      }
    }

  def getWorkset(@ApiParam(value = "the workset ID", required = true) wid: WorksetId): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          efRepository
            .getWorkset(wid)
            .map(WrappedResponse(_))
      }
    }

  def deleteWorkset(@ApiParam(value = "the workset ID", required = true) wid: WorksetId): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          efRepository.deleteWorkset(wid).map(_ => WrappedResponse.Empty)
      }
    }

  def getWorksetVolumesAggNoPos(@ApiParam(value = "the workset ID", required = true) wid: WorksetId,
                                @ApiParam(value = "comma-separated list of fields to return") fields: Option[String]): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          efRepository
            .getWorkset(wid)
            .flatMap(workset => efRepository.getVolumesAggNoPos(workset.htids, fields.map(tokenize(_)).getOrElse(List.empty)))
            .map(WrappedResponse(_))
      }
    }

  def getWorksetAggNoPos(@ApiParam(value = "the workset ID", required = true) wid: WorksetId,
                         @ApiParam(value = "comma-separated list of fields to return") fields: Option[String]): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          efRepository
            .getWorkset(wid)
            .flatMap(workset => efRepository.getWorksetAggNoPos(workset.htids, fields.map(tokenize(_)).getOrElse(List.empty)))
            .map(WrappedResponse(_))
      }
    }

  def getWorksetVolumesMetadata(@ApiParam(value = "the workset ID", required = true) wid: WorksetId,
                                @ApiParam(value = "comma-separated list of fields to return") fields: Option[String]): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          efRepository
            .getWorkset(wid)
            .flatMap(workset => efRepository.getVolumesMetadata(workset.htids, fields.map(tokenize(_)).getOrElse(List.empty)))
            .map(WrappedResponse(_))
      }
    }
}
