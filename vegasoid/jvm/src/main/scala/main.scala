import edu.byu.csl.courier.service.SimpleRPC
import com.typesafe.config._
import collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.apache.sysml.api.mlcontext._
import org.apache.sysml.api.mlcontext.ScriptFactory._
import scala.collection.mutable.{Map => MutMap}

object Main extends App {

  val conf = ConfigFactory.load("pruner")

  val state_vars = conf
    .getStringList("systemml.dml.scripts.init.out").asScala

  var symT: MutMap[String,Matrix] = MutMap()

  var mlctx_instance: Option[MLContext] = None

  def init_script() = ScriptFactory
    .dmlFromFile(conf.getString("systemml.dml.scripts.init.path"))
    .out(conf.getStringList("systemml.dml.scripts.init.out").asScala)

  def analysis_script() = ScriptFactory
    .dmlFromFile(conf.getString("systemml.dml.scripts.analysis.path"))
    .out(conf.getStringList("systemml.dml.scripts.analysis.out").asScala)

  def getMLContext(spark: SparkSession): MLContext = {
    mlctx_instance match {
      case Some(ctx) => ctx
      case None => {
        val ctx = new MLContext(spark)
        mlctx_instance = Some(ctx)
        ctx
      }
    }
  }

  SimpleRPC.run("0.0.0.0", Map(
    "SystemML" -> { (spark) =>
      val mlctx = getMLContext(spark)
      import spark.implicits._
      val df = spark.sql("SELECT * FROM Point")
      if (symT.isEmpty) {
        val res = mlctx.execute(init_script.in("df",df))
        state_vars.foreach { v =>
          symT(v) = res.getMatrix(v)
        }
      }
      val s4 = analysis_script.in("df", df).in(symT)
      val res = mlctx.execute(s4)
      state_vars.foreach { v =>
        symT(v) = res.getMatrix(v)
      }
      //s4.displaySymbolTable().getBytes()
      "Success?".getBytes()
    }
  ))
}
