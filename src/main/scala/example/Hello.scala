package example

import cats.effect.{ExitCode, IO, IOApp}
import example.KafkaStreamConsumer.KafkaStreamConsumerConfig

object Hello extends IOApp {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def run(args: List[String]): IO[ExitCode] = {

    val config = KafkaStreamConsumerConfig(
      bootstrapServers = s"localhost:9092",
      topic = "test_topic",
      clientId = "client_id",
      groupId = "same_group",
    )

    for {
      _ <- KafkaStreamConsumer.create[IO](config).compile.drain
    } yield ExitCode.Success
  }
}

