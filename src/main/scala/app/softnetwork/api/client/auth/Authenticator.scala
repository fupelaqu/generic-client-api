package app.softnetwork.api.client.auth

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

/**
  * Created by smanciot on 01/04/2021.
  */
trait Authenticator{
  /**
    *
    * @param request - the http request to authenticate
    * @return the http request authenticated
    */
  def authenticate(request: HttpRequest): Future[Either[Throwable, HttpRequest]]
}

trait EmptyAuthenticator extends Authenticator{
  /**
    *
    * @param request - the http request to authenticate
    * @return the http request authenticated
    */
  override def authenticate(request: HttpRequest): Future[Either[Throwable, HttpRequest]] =
    Future.successful(Right(request))
}


