package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionsSpec
    extends TestKit(
      ActorSystem(
        "TimedAssertionsSpec",
        ConfigFactory.load().getConfig("specialTimedAssertionsConfig")
      )
    )
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionsSpec._

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply with the meaning of life in a timely manner" in {
      within(500 millis, 1 second) { // time-boxed test
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence"

        val results: Seq[Int] = {
          // the arguments to receiveWhile specify the stop condition
          // max: stop the receiveWhile after ${max} seconds
          // idle: stop the receiveWhile if there is a ${idle} period
          // messages: the receiveWhile if ${messages} are received
          receiveWhile[Int](max = 2 seconds, idle = 60 millis, messages = 10) {
            case WorkResult(result) => result
          }
        }

        assert(results.sum== 10)
      }
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")

        // TestProbe has it own timeout settings, and
        // expectMsg is NOT controlled by the time-box set by within method

        // follow the stack trace of the failed test case
        // it's quite easy to locate the timeout value in code (which defaults to 3s)

        // customized the timeout setting:
        // specialTimedAssertionsConfig in application.conf
        probe.expectMsg(WorkResult(42))
      }
    }
  }
}

object TimedAssertionsSpec {

  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)

      case "workSequence" =>
        val r = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}
