import edu.byu.csl.courier.service.SimpleRPC
import com.typesafe.config._
import collection.JavaConverters._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.sysml.api.mlcontext._
import org.apache.sysml.api.mlcontext.ScriptFactory._
import scala.collection.mutable.{Map => MutMap}

trait HandlerFactory[A] { 
  def apply(spark: SparkSession): Array[Byte]
}

object HandlerFactory {
  implicit val f1 = new HandlerFactory[Initializer] {
    def apply(spark: SparkSession) = new Initializer(spark).apply
  }
  implicit val f2 = new HandlerFactory[UpdateHandler] {
    def apply(spark: SparkSession) = new UpdateHandler(spark).apply
  }
  implicit val f3 = new HandlerFactory[Printer] {
    def apply(spark: SparkSession) = new Printer(spark).apply
  }
  def apply[A:HandlerFactory]: HandlerFactory[A] = 
    implicitly[HandlerFactory[A]]
  def applyMap(handlers: Map[String,HandlerFactory[_]]) = 
    handlers.map { case (k,v) => (k, v.apply(_)) }
}

object State { 
  var symT: MutMap[String,Matrix] = MutMap() 
  def update(k: String, v: Matrix) = symT.put(k,v)
}

class ScriptPlus(name: String)(implicit val spark: SparkSession) {
  val mlctx = new MLContext(spark)
  val conf = ConfigFactory.load("pruner")
  val namespace = "systemml.dml.scripts"
  val path = conf.getString(s"$namespace.$name.path")
  val intentOut = conf.getStringList(s"$namespace.$name.out").asScala
  var script = ScriptFactory.dmlFromFile(path).out(intentOut)

  def chain(f: () => Script): ScriptPlus = { script = f(); this }
  def in(name: String, data: DataFrame) = chain(() => script.in(name,data))
  def in(st: MutMap[String,Matrix]) = chain(() => script.in(st))
  def asVal() = chain(() => script.out(name)) //Name = return value
  def withDf() = in("df", spark.sql("SELECT * FROM Point"))
  def withState() = in(State.symT)
  def execute() = mlctx.execute(script)
}

abstract class Handler(spark: SparkSession) {
  implicit val session = spark
  val state_vars = new ScriptPlus("init").intentOut

  //Abstract
  def apply(): Array[Byte]

  //Shared
  def voidHandler(f: () => Unit) = { f(); "()".getBytes() }
  def updateState(res: MLResults) = state_vars.foreach { v => 
    State.update(v,res.getMatrix(v)) 
  }
  def dmlScript(name: String) = new ScriptPlus(name)
}

class Initializer(spark: SparkSession) extends Handler(spark) {
  def apply() = voidHandler(()=>updateState(dmlScript("init").execute))
}

class UpdateHandler(spark: SparkSession) extends Handler(spark) {
  def apply() = {
    val res = dmlScript("analysis").withDf.withState.asVal.execute
    updateState(res)
    res.getMatrix("analysis").toDF().createOrReplaceTempView("Hist")
    val saturated = spark.sql("""
      SELECT * FROM (
        SELECT __INDEX as Idx, ABS(C3) as Sat 
        FROM Hist
      ) 
      WHERE Sat > 3
      ORDER BY Sat DESC
      """
    )
    saturated.collect().map(_.mkString(" ")).mkString(" ").getBytes()
  }
}

class Printer(spark: SparkSession) extends Handler(spark) {
  def apply() = voidHandler(()=>dmlScript("printer").withState.execute)
}

object Main extends App {
  SimpleRPC.run("0.0.0.0", HandlerFactory.applyMap(Map(
    "Init" -> HandlerFactory[Initializer],
    "Update" -> HandlerFactory[UpdateHandler],
    "Printer" -> HandlerFactory[Printer]
  )))
}
