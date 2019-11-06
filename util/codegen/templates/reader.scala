class {{ record.name }}Reader extends AbstractData {
{% include "record.scala" %}
  val name = "{{ record.name }}"
  val record = new {{ record.name }}()
  def read(range: Range) {
    Warehouse.{{ record.name|lowercase }} += range.map { i =>
      record.setByteBufferPosition(i*record.size())
      {{ record.name }}CC({{ record.case_class_constructor() }})
    }.toArray
  }
  def register(spark: SparkSession, rdd: RDD[Int]) {
    val srdd = rdd.flatMap { i =>
      Warehouse.{{ record.name|lowercase }}.result().flatMap(a => a.toIterator)
    }
    spark.createDataFrame(srdd).createOrReplaceTempView(name)
  }
  def clear() {
    Warehouse.{{ record.name|lowercase }}.clear()
  }
}
