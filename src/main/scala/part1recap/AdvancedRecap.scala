package part1recap

import scala.concurrent.Future
import scala.language.implicitConversions

object AdvancedRecap extends App {

  // partial functions
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val pf = (x: Int) =>
    x match {
      case 1 => 42
      case 2 => 65
      case 5 => 999
    }

  // A partial function IS A functions!!!
  val function: Int => Int = partialFunction

  val modifiedList = List(1, 2, 3).map {
    case 1 => 42
    case _ => 0
  }

  // lifting
  val lifted = partialFunction.lift // total function Int => Option[Int]
  lifted(2) // Some(65)
  lifted(5000) // None

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] { case 60 =>
    9000
  }

  pfChain(5) // 999 per partialFunction
  pfChain(60) // 9000
  pfChain(457) // throw a MatchError

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  // Exactly what the Actor's receive method is
  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("confused....")
  }

  // implicits

  implicit val timeout: Int = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int): Unit = f()

  setTimeout(() => println("timeout")) // extra parameter list omitted

  // implicit conversions
  // 1) implicit defs
  case class Person(name: String) {
    def greet = s"Hi, my name is $name"
  }

  // Essentially converts a String to a Person
  implicit def fromStringToPerson(string: String): Person = Person(string)
  "Peter".greet
  // Equivalent to:
  // fromStringToPerson("Peter").greet - automatically by the compiler

  // 2) implicit classes
  implicit class Dog(name: String) {
    def bark: Unit = println(s"[$name] bark!")
  }

  // If the method is defined w/o parenthesis
  // Invoke it w/o parenthesis
  "Lassie".bark
  // new Dog("Lassie").bark - automatically done by the compiler

  // Where to find the given implicits
  // 1. local scope
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(1, 2, 3).sorted) // List(3,2,1)

  // imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    println("hello, future")
  }

  // 2. companion objects of the types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] =
      Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  // companion objects = List & Person
  List(Person("Bob"), Person("Alice")).sorted
  // List(Person(Alice), Person(Bob))
}
