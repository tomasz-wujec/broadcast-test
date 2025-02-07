package example

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.util.ByteString
import scala.concurrent.ExecutionContext

object StreamsExample extends App {
  implicit val system: ActorSystem = ActorSystem("broadcast-test")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContext = system.dispatcher

  // Simulated input data
  case class DataPacket(id: Int, data: String)
  
  // Source that emits numbers with some delay
  val source = Source(1 to 100)
    .map(i => DataPacket(i, s"Data-$i"))
  
  // Different processing flows with varying speeds
  val fastFlow = Flow[DataPacket].map { packet =>
    s"Fast-${packet.id}"
  }
  
  val mediumFlow = Flow[DataPacket]/* .buffer(Int.MaxValue, OverflowStrategy.backpressure) */.mapAsync(1) { packet =>
    Future {
      Thread.sleep(200) // Simulate some processing
      s"Medium-${packet.id}"
    }
  }
  
  val slowFlow = Flow[DataPacket]/* .buffer(Int.MaxValue, OverflowStrategy.backpressure) */.mapAsync(1) { packet =>
    Future {
      Thread.sleep(500) // Simulate slower processing
      s"Slow-${packet.id}"
    }
  }
  
  val testFlow = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      
      // Create broadcast and merge
      val bcast = builder.add(Broadcast[DataPacket](3))
      val merge = builder.add(Merge[String](3))
      
      // Connect the flows
      bcast ~> fastFlow  ~> merge
      bcast ~> mediumFlow ~> merge
      bcast ~> slowFlow ~> merge
      
      FlowShape(bcast.in, merge.out)
    }
  )
  
  // Run the graph and print results
  source
    .via(testFlow)
    .runForeach { result =>
      println(s"Got result: $result")
    }
    .onComplete { _ =>
      system.terminate()
    }
} 