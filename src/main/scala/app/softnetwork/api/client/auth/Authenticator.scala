package app.softnetwork.api.client.auth

import akka.http.scaladsl.model.HttpRequest

/**
  * Created by smanciot on 01/04/2021.
  */
trait Authenticator{
  /**
    *
    * @param request - the http request to authenticate
    * @return the http request authenticated
    */
  def authenticate(request: HttpRequest): Either[Throwable, HttpRequest]
}

trait EmptyAuthenticator extends Authenticator{
  /**
    *
    * @param request - the http request to authenticate
    * @return the http request authenticated
    */
  override def authenticate(request: HttpRequest): Either[Throwable, HttpRequest] = Right(request)
}


