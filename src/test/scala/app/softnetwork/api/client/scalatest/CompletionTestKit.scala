package app.softnetwork.api.client.scalatest

import app.softnetwork.api.client.Completion
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Created by smanciot on 12/04/2021.
  */
trait CompletionTestKit extends Completion with Assertions {

  implicit class AwaitAssertion[T](future: Future[T])(implicit atMost: Duration = defaultTimeout){
    def assert(fun: T => Assertion): Assertion =
      Try(Await.result(future, atMost)) match {
        case Success(s) => fun(s)
        case Failure(f) => fail(f.getMessage)
      }
  }

  implicit def toT[T](t: Try[T]): T = t match {
    case Success(s) => s
    case Failure(f) => fail(f.getMessage)
  }

}
