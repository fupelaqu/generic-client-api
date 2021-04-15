package app.softnetwork.api.client

import java.net.URI

import akka.actor.ActorSystem

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source, Flow}
import app.softnetwork.api.client.auth.Authenticator

import com.typesafe.scalalogging.StrictLogging

import de.heikoseeberger.akkahttpjson4s.Json4sSupport

import org.json4s.{Formats, jackson}

import scala.concurrent.{Future, ExecutionContext}

/**
  * Created by smanciot on 01/04/2021.
  */
trait GenericApi extends Completion with Json4sSupport with StrictLogging {_: Authenticator =>
  implicit def system: ActorSystem

  implicit def ec: ExecutionContext

  implicit def mat: Materializer

  def config: ApiConfig

  implicit val serialization = jackson.Serialization

  import app.softnetwork.api.client.serialization._

  implicit def formats: Formats = defaultFormats

  /**
    *
    * @param api - the api URI
    * @param query - the query parameters
    * @param headers - the http headers
    * @tparam Response - the api response type
    * @tparam Error - the api error type
    * @return either an api error or an api response
    */
  protected def doGet[Response: Manifest, Error: Manifest](api: String,
                                                           query: Map[String, String] = Map.empty,
                                                           headers: List[HttpHeader] = List(RawHeader("Accept", "*/*"))
                                                          ): Future[Either[Error, Response]] = {
    executeWithoutRequest[Response, Error](api, HttpMethods.GET, query, headers)
  }

  /**
    *
    * @param api - the api URI
    * @param query - the query parameters
    * @param headers - the http headers
    * @tparam Request - the api request type
    * @tparam Response - the api response type
    * @tparam Error - the api error type
    * @return either an api error or an api response
    */
  protected def doPost[Request <: AnyRef, Response: Manifest, Error: Manifest](api: String,
                                                                               entity: Request,
                                                                               query: Map[String, String] = Map.empty,
                                                                               headers: List[HttpHeader] = List(RawHeader("Accept", "*/*"))
                                                                              ): Future[Either[Error, Response]] = {
    execute[Request, Response, Error](api, entity, HttpMethods.POST, query, headers)
  }

  /**
    *
    * @param api - the api URI
    * @param query - the query parameters
    * @param headers - the http headers
    * @tparam Request - the api request type
    * @tparam Response - the api response type
    * @tparam Error - the api error type
    * @return either an api error or an api response
    */
  protected def doPut[Request <: AnyRef, Response: Manifest, Error: Manifest](api: String,
                                                                              entity: Request,
                                                                              query: Map[String, String] = Map.empty,
                                                                              headers: List[HttpHeader] = List(RawHeader("Accept", "*/*"))
                                                                             ): Future[Either[Error, Response]] = {
    execute[Request, Response, Error](api, entity, HttpMethods.PUT, query, headers)
  }

  /**
    *
    * @param api - the api URI
    * @param query - the query parameters
    * @param headers - the http headers
    * @param ev - an implicit to convert an api error to a Throwable
    * @tparam Request - the api request type
    * @tparam Response - the api response type
    * @tparam Error - the api error type
    * @return either an api error or an api response
    */
  protected def doPatch[Request <: AnyRef, Response: Manifest, Error: Manifest](api: String,
                                                               entity: Request,
                                                               query: Map[String, String] = Map.empty,
                                                               headers: List[HttpHeader] = List(RawHeader("Accept", "*/*")))(
    implicit ev: Error => Throwable): Future[Either[Error, Response]] = {
    execute[Request, Response, Error](api, entity, HttpMethods.PATCH, query, headers)
  }

  /**
    *
    * @param api - the api URI
    * @param query - the query parameters
    * @param headers - the http headers
    * @tparam Response - the api response type
    * @tparam Error - the api error type
    * @return either an api error or an api response
    */
  protected def doDelete[Response: Manifest, Error: Manifest](api: String,
                                                              query: Map[String, String] = Map.empty,
                                                              headers: List[HttpHeader] = List(RawHeader("Accept", "*/*"))
                                                             ): Future[Either[Error, Response]] = {
    execute[Map[String, String], Response, Error](api, Map.empty, HttpMethods.DELETE, query, headers)
  }

  protected def executeWithoutResponse[Request <: AnyRef, Error: Manifest](api: String,
                                                                           entity: Request,
                                                                           method: HttpMethod = HttpMethods.GET,
                                                                           query: Map[String, String] = Map.empty,
                                                                           headers: List[HttpHeader] = List.empty
                                                                          ): Future[Either[Error, Unit]] = {
    execute[Request, Unit, Error](api, entity, method, query, headers, withResponse = false)
  }

  protected def executeWithoutRequest[Response : Manifest, Error: Manifest](api: String,
                                                                            method: HttpMethod = HttpMethods.GET,
                                                                            query: Map[String, String] = Map.empty,
                                                                            headers: List[HttpHeader] = List.empty
                                                                           ): Future[Either[Error, Response]] = {
    execute[Map[String, String], Response, Error](api, Map.empty, method, query, headers)
  }

  protected def executeWithoutRequestAndResponse[Error: Manifest](api: String,
                                                                  method: HttpMethod = HttpMethods.GET,
                                                                  query: Map[String, String] = Map.empty,
                                                                  headers: List[HttpHeader] = List.empty
                                                                 ): Future[Either[Error, Unit]] = {
    execute[Map[String, String], Unit, Error](api, Map.empty, method, query, headers, withResponse = false)
  }

  private[this] def execute[Request <: AnyRef, Response: Manifest, Error: Manifest](api: String,
                                                                                    entity: Request,
                                                                                    method: HttpMethod = HttpMethods.GET,
                                                                                    query: Map[String, String] = Map.empty,
                                                                                    headers: List[HttpHeader] = List.empty,
                                                                                    withResponse: Boolean = true
                                                                                   ): Future[Either[Error, Response]] = {
    val serialized = serialization.write[Request](entity)
    logger.debug(s"Request -> $serialized")
    authenticate(HttpRequest(
        method = method,
        uri = Uri(config.baseUrl)
          .withPath(Uri.Path(api))
          .withQuery(Uri.Query(query))
      )
      .withHeaders(headers)
      .withEntity(HttpEntity(ContentTypes.`application/json`, serialized))
    ) flatMap {
      case Right(request) =>
        Source
          .single(request)
          .via(connection)
          .mapAsync(1)(response =>
            if(response.status.isFailure()){
              Unmarshal(response).to[Error].flatMap(error => Future.failed(GenericApiError(error)))
            }
            else{
              Future.successful(response)
            }
          )
          .mapAsync(1)(response =>
            if(withResponse){
              Unmarshal(response).to[Response].flatMap(r => Future.successful(Some(r)))
            }
            else{
              Future.successful(None)
            }
          )
          .runWith(Sink.head)
          .map(Right.apply)
          .recover {
            case ex => Left(ex)
          } flatMap {
          case Left(l) =>
            l match {
              case ex: GenericApiError[Error] => Future.successful(Left(ex.error))
              case ex => Future.failed(ex)
            }
          case Right(r) =>
            r match {
              case Some(s) => Future.successful(Right(s))
              case None => Future.successful(Right(().asInstanceOf[Response]))
            }
        }
      case Left(f) => Future.failed(f)
    }
  }

  private[this] def connection: Flow[HttpRequest, HttpResponse, _] =
    config.site.getScheme match {
      case "http"  => Http().outgoingConnection(config.host, config.port.getOrElse(80))
      case "https" => Http().outgoingConnectionHttps(config.host, config.port.getOrElse(443))
    }

}

trait ApiConfig {
  def baseUrl: String
  lazy val site = URI.create(baseUrl)

  lazy val host = site.getHost

  lazy val port =
    if(site.getPort == -1){
      None
    }
    else {
      Some(site.getPort)
    }
}

private[this] case class GenericApiError[Error: Manifest](error: Error) extends Throwable
