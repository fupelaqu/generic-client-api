package app.softnetwork.api.client

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import scala.language.implicitConversions

import scala.util.{Failure, Success, Try}

/**
  * Created by smanciot on 12/04/2021.
  */
trait Completion {

  /**
    *
    * maximum wait time, which may be negative (no waiting is done),
    * [[scala.concurrent.duration.Duration.Inf Duration.Inf]] for unbounded waiting, or a finite positive duration
    */
  def defaultTimeout = 60.seconds

  implicit class AwaitCompletion[T](future: Future[T])(implicit atMost: Duration = defaultTimeout){
    /**
      * Usage:
      * aFuture wait {
      *   case a: A => //...
      *   case other => //...
      * } match {
      *   case Success(s) => s
      *   case Failure(f) => //...
      * }
      * @param fun - the function which will be called
      * @tparam B - the function return type
      * @return a successfull B or an exception
      */
    def await[B](fun: T => B): Try[B] =
      Try(Await.result(future, atMost)) match {
        case Success(s) => Success(fun(s))
        case Failure(f) => Failure(f)
      }
    def complete(): Try[T] = await[T]({t => t})
  }

  implicit def toBoolean(t: Try[Boolean]): Boolean = t match {
    case Success(s) => s
    case Failure(f) => false
  }

  implicit def toSeq[T](t: Try[Seq[T]]): Seq[T] = t match {
    case Success(s) => s
    case Failure(f) => Seq.empty
  }

  implicit def toOption[T](t: Try[Option[T]]): Option[T] = t match {
    case Success(s) => s
    case Failure(f) => None
  }

}
