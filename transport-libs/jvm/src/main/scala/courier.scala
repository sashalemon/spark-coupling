package edu.byu.csl.courier
package service

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths}

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import spray.json._

import posix.semaphore.Semaphore
import schema.SchemaProvider
import resource._

object Util {
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    println("(seconds) : " + (t1 - t0) / 1e9)
    result
  }
}

case class DataConfig(schemaName: String, nrows: Int, rowsPerBlock: Int, nblocks: Int, append: Boolean)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val confFormat = jsonFormat5(DataConfig)
}

case class Stats(max: Long, committed: Long, free: Long, used: Long, 
  phase: String, host: String, data_size: Long, iteration: Long)

object StatsP extends DefaultJsonProtocol {
  implicit val statsFormat = jsonFormat8(Stats)
}

object Ring extends Directives with JsonSupport {

  def mapBuffer(name: String, size: Int): ByteBuffer = {
    val path = Paths.get(name)
    val chan = FileChannel.open(path)
    val mem = chan
      .map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path))
      .load()
      .order(ByteOrder.nativeOrder())
    chan.close()
    mem
  }
  def run() {
    implicit val system = ActorSystem("courier")
    implicit val materializer = ActorMaterializer()

    val tf = SchemaProvider.tableProducer
    def handle(conf: DataConfig) {
      val rowsperblock = conf.rowsPerBlock
      val nrows = conf.nrows
      val nblocks = conf.nblocks
      Util.time {
        val reader = tf.get(conf.schemaName)
        if (!conf.append) {
          reader.clear()
        }
        val row_size = reader.record.size()
        val mem = mapBuffer("/dev/shm/packmule", row_size * nblocks * rowsperblock)
        var nleft = nrows
        reader.record.setByteBuffer(mem, 0)
        var segment = 0
        for {
          full <- managed(new Semaphore("/packfull"))
          empty <- managed(new Semaphore("/packempty"))
          done <- managed(new Semaphore("/packack"))
          } {
            for (i <- 0 until math.ceil(nrows/rowsperblock.toDouble).toInt) {
              full._wait()
              val offset = rowsperblock * segment
              val chunk = scala.math.min(rowsperblock, nleft)
              reader.read(offset until (offset + chunk))
              empty._post()
              segment = (segment + 1) % nblocks
              nleft -= chunk
            }
            done._post()
          }
          System.gc()
      }
    }
    class TransferExecutor extends Actor {
      def receive = {
        case conf: DataConfig => handle(conf)
      }
    }
    val txd = system.actorOf(Props(new TransferExecutor()), "txd")

    val route =
      path("data") {
        post {
          entity(as[DataConfig]) { s =>
            txd ! s
            complete(StatusCodes.Accepted)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)
    println("Server online at 8081")
  }
}

object Fake {
  lazy val started = {
    Ring.run()
    true
  }
}

class Courier(session: SparkSession = SparkSession.builder.appName("Courier").getOrCreate()) {

  val spark = session
  import spark.implicits._

  private val sc = spark.sparkContext
  while (sc.getExecutorMemoryStatus.keys.toSeq.length < sys.env("NUM_NODES").toInt) {
    Thread.sleep(100)
  }
  val executors = if (sys.env("NUM_NODES").toInt > 1) {
    sc.getExecutorMemoryStatus.keys.filterNot{_ contains sc.getConf.get("spark.driver.host")}
  } else {
    sc.getExecutorMemoryStatus.keys
  }
  val unitCollection = executors.map{loc => (1, Seq(loc))}.toSeq
  val rdd = sc.makeRDD[Int](unitCollection)
  rdd.foreach { i => println(Fake.started) }
  val tf = SchemaProvider.tableProducer
  def requireDep(name: String) = tf.get(name).register(spark, rdd) 
}

case class AnalysisRequest(analysis: String, dependencies: Array[String])

trait CtrlJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val areqFormat = jsonFormat2(AnalysisRequest)
}

object SimpleRPC extends Directives with CtrlJsonSupport {
  val courier = new Courier()
  def run(address: String, callbacks: Map[String, (SparkSession) => Array[Byte]]) {
    implicit val system = ActorSystem("analysis")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val route =
      path("ready") {
        post {
          entity(as[AnalysisRequest]) { req =>
            val f: Future[Array[Byte]] = Future {
              req.dependencies.foreach { dep => 
                courier.requireDep(dep)
              }
              callbacks(req.analysis)(courier.spark)
            }
            onSuccess(f) { res => complete(HttpEntity(ContentTypes.`application/octet-stream`, res)) }
          }
        }
      }
    val bindingFuture =  Http().bindAndHandle(route, address, 8082)
    println("Server online at 8082")
  }
}
