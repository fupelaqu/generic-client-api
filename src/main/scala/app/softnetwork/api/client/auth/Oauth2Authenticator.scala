package app.softnetwork.api.client.auth

import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.Materializer
import app.softnetwork.api.client.ApiConfig
import com.github.dakatsuka.akka.http.oauth2.client.Error.UnauthorizedException
import com.github.dakatsuka.akka.http.oauth2.client.{AccessToken, Client, Config, GrantType}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by smanciot on 02/04/2021.
  */
trait Oauth2Authenticator extends Authenticator with StrictLogging {

  implicit def system: ActorSystem

  def config: Oauth2ApiConfig

  lazy val clientOAuth2: Client = Client(Config(config.apiClientId, config.apiSecret, URI.create(config.api)))

  private[this] var accessToken: Option[AccessToken] = None

  private[this] var expirationTime: Option[Instant] = None

  /**
    *
    * @param request - the http request to authenticate
    * @return the http request authenticated
    */
  override def authenticate(request: HttpRequest): Either[Throwable, HttpRequest] = {
    implicit val ec: ExecutionContext = system.dispatcher

    implicit val mat: Materializer = Materializer(system)

    (accessToken match {
      case Some(token) =>
        if(isExpired){
          if(token.refreshToken.nonEmpty){
            refreshToken(token.refreshToken.get)
          }
          else{
            newAccessToken()
          }
        }
        else{
          Right(token)
        }
      case _ => newAccessToken()
    }) match {
      case Right(token) =>
        Right(request.addHeader(
          RawHeader("Authorization", s"${token.tokenType} ${token.accessToken}")
        ))
      case Left(ex) => Left(ex)
    }
  }

  private[this] def isExpired = {
    accessToken match {
      case Some(token) =>
        expirationTime match {
          case Some(time) => Instant.now().isAfter(time)
          case _ => true
        }
      case _ => true
    }
  }

  private[this] def newAccessToken()(implicit ec: ExecutionContext, mat: Materializer
  ): Either[Throwable, AccessToken] = {
    logger.info("new Access Token required")
    handle(clientOAuth2.getAccessToken(GrantType.ClientCredentials, Map.empty))
  }

  private[this] def refreshToken(refreshToken: String)(implicit ec: ExecutionContext, mat: Materializer
  ): Either[Throwable, AccessToken] = {
    logger.info("Access Token has expired - refreshing token")
    handle(clientOAuth2.getAccessToken(GrantType.RefreshToken, Map("refresh_token" -> refreshToken)))
  }

  private[this] def handle(response: Future[Either[Throwable, AccessToken]])(implicit ec: ExecutionContext
  ): Either[Throwable, AccessToken] = {
    Try(Await.result(response, 10.seconds)) match {
      case Success(s) =>
        s match {
          case Right(token) =>
            accessToken = Some(token)
            expirationTime = Some(Instant.now().plus(token.expiresIn, ChronoUnit.MILLIS).minus(5, ChronoUnit.SECONDS))
            logger.info("Access Token {} expires in {} ms, at {}, Refresh Token {}",
              token.tokenType,
              token.expiresIn,
              expirationTime.get,
              token.refreshToken
            )
            Right(token)
          case Left(x: Throwable) =>
            x match {
              case ex: UnauthorizedException =>
                logger.error("{} -> {},{}", ex.getMessage, ex.code, ex.description)
              case _ => logger.error(x.getMessage, x)
            }
            Left(x)
        }
      case Failure(f) =>
        logger.error(f.getMessage, f)
        Left(f)
    }
  }
}

trait Oauth2ApiConfig extends ApiConfig {
  def apiClientId: String
  def apiSecret: String
  def oauth2Api: String
  def api = baseUrl + oauth2Api
}
