package edu.byu.csl.courier.schema

import java.nio.ByteOrder
import javolution.io.Struct
import javolution.io.Struct._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import scala.collection.mutable.ArrayBuilder

case class PointCC(x:Double,y:Double,category:Int)


object Warehouse {
  lazy val point = ArrayBuilder.make[Array[PointCC]]
}

class PointReader extends AbstractData {
  class Point extends Struct {
    var x = new Float64()
    var y = new Float64()
    var category = new Signed32()
    override def byteOrder(): ByteOrder = {
      ByteOrder.nativeOrder()
    }
  }
  val name = "Point"
  val record = new Point()
  def read(range: Range) {
    Warehouse.point += range.map { i =>
      record.setByteBufferPosition(i*record.size())
      PointCC(record.x.get(),record.y.get(),record.category.get())
    }.toArray
  }
  def register(spark: SparkSession, rdd: RDD[Int]) {
    val srdd = rdd.flatMap { i =>
      Warehouse.point.result().flatMap(a => a.toIterator)
    }
    spark.createDataFrame(srdd).createOrReplaceTempView(name)
  }
  def clear() {
    Warehouse.point.clear()
  }
}

class TableProducerImpl extends TableProducer {
  def get(name: String) = name match {
    case "Point" => new PointReader()
  }
}