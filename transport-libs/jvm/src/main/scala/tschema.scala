package edu.byu.csl.courier.schema
import javolution.io.Struct
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import scala.concurrent.SyncVar
import java.net.URL
import com.twitter.chill.KryoInjection

abstract class StructReader {
  val record: Struct
  def read(r: Range)
}

trait TableSource {
  def register(spark: SparkSession, rdd: RDD[Int])
  def clear()
}

abstract class AbstractData extends StructReader with TableSource

trait TableProducer {
  def get(name: String): AbstractData
}

object SchemaProvider {
  lazy val tableProducer: TableProducer = {
    val o = getClass.getClassLoader()
      .loadClass("edu.byu.csl.courier.schema.TableProducerImpl")
      .newInstance()
      val tryDecode: scala.util.Try[Object] = KryoInjection.invert(KryoInjection(o))
      tryDecode.get.asInstanceOf[TableProducer]
  }
}
