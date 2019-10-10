package example

import cats.Functor
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Sync, Timer}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2._
import fs2.kafka._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object KafkaStreamConsumer extends StrictLogging {
  case class KafkaStreamConsumerConfig(
    topic: String,
    bootstrapServers: String,
    clientId: String,
    groupId: String,
  )
  def create[F[_] : Sync : ContextShift : ConcurrentEffect : Timer](
    config: KafkaStreamConsumerConfig,
  )(
    implicit
    blockingEc: ExecutionContext,
  ): Stream[F, Unit] = {
    implicit val log: Logger[F] = Slf4jLogger.getLoggerFromSlf4j(logger.underlying)
    val stringDeserializer: Deserializer[F, String] =
      Deserializer.delegate[F, String] {
        new StringDeserializer
      }

    val consumerSettings =
      ConsumerSettings[F, String, String](
        keyDeserializer = stringDeserializer,
        valueDeserializer = stringDeserializer,
      ).withBootstrapServers(config.bootstrapServers)
        .withGroupId(config.groupId)
        .withAutoOffsetReset(AutoOffsetReset.Latest)
        .withClientId(config.clientId)
        .withEnableAutoCommit(false)
        .withBlocker(Blocker.liftExecutionContext(blockingEc))

    consumerStream(consumerSettings)
      .evalTap(_.subscribeTo(config.topic))
      .flatMap { consumer =>
        consumer.partitionedStream
          .map(process[F])
      }
      .parJoinUnbounded
      .groupWithin(10, 60.seconds)
      .evalMap { chunks =>
        CommittableOffsetBatch.fromFoldable(chunks.map { co =>
          CommittableOffset[F](
            topicPartition = co.topicPartition,
            offsetAndMetadata = co.offsetAndMetadata,
            consumerGroupId = config.groupId.some,
            commit = _ => Sync[F].raiseError(new KafkaException),
          )
        }).commit
      }
      .attempts(Stream.emit(2.seconds).repeat)
      .evalMap {
        case Left(error) => log.error(error)("stream failed")
        case Right(_)    => Sync[F].unit
      }
  }

  private def process[F[_] : Functor](
    streamOfRecords: Stream[F, CommittableConsumerRecord[F, String, String]],
  )(
    implicit log: Logger[F],
  ): Stream[F, CommittableOffset[F]] = {
    streamOfRecords.evalMap { crr =>
      log.info(s"consumed: ${ crr.record.value }").as(crr.offset)
    }
  }
}
