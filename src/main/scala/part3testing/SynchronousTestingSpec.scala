package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll {

  // create the actor system without using the one prepared by TestKit
  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  import SynchronousTestingSpec._

  "A counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])

      // sending messages to the TestActorRef happens in the calling thread!!!
      counter ! Inc // counter has ALREADY received the message

      // counter.underlyingActor
      // let you directly reach to the underlying actor instance
      // NOT ActorRef instance!!!
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])

      // makes no difference, still on the calling thread because of TestActorRef
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read)

      // because of CallingThreadDispatcher
      // we can set Duration.Zero
      probe.expectMsg(Duration.Zero, 0) // probe has ALREADY received the message 0
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case Inc  => count += 1
      case Read => sender() ! count
    }
  }
}
