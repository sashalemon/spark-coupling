package edu.byu.csl.courier.schema

import java.nio.ByteOrder
import javolution.io.Struct
import javolution.io.Struct._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import scala.collection.mutable.ArrayBuilder

{% for record in structs -%}
case class {{record.name}}CC({{ record.case_class_fields() }})
{% endfor %}

object Warehouse {
  {%- for record in structs %}
  lazy val {{ record.name|lowercase }} = ArrayBuilder.make[Array[{{ record.name }}CC]]
  {%- endfor %}
}

{% for record in structs -%}
  {% include "reader.scala" %}
{% endfor %}
class TableProducerImpl extends TableProducer {
  def get(name: String) = name match {
  {%- include "tablematch.scala" %}
  }
}
