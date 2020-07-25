

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import XModel.XModel
import com.github.tototoshi.csv.CSVWriter
import org.chocosolver.solver.Model
import org.chocosolver.solver.constraints.Propagator
import org.chocosolver.solver.constraints.extension.Tuples
import org.chocosolver.solver.constraints.extension.nary.{PropCompactTable, PropLargeGAC3rm, PropLargeMDDC, PropTableStr2}
import org.chocosolver.solver.search.strategy.Search._
import org.chocosolver.solver.variables.IntVar
import org.chocosolver.solver.variables.impl.BitsetIntVarImpl
import org.chocosolver.util.objects.graphs.MultivaluedDecisionDiagram

import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting
import scala.xml._

object Test {


  val limit_time: Int = 1800000
  var fmt = 0

  def main(args: Array[String]): Unit = {
    val file = XML.loadFile("benchmarks\\Folders.xml")

    val inputRoot: String = (file \\ "inputRoot").text
    val outputRoot: String = (file \\ "outputRoot").text
    val outputFolder: String = (file \\ "outputFolder").text
    val inputFolderNodes: NodeSeq = file \\ "folder"
    val titleLine = ArrayBuffer[String]()


    for (fn <- inputFolderNodes) {
      fmt = (fn \\ "@format").text.toInt
      val folderStr = fn.text
      val inputPath = inputRoot + "/" + folderStr
      val files = getFiles(new File(inputPath))

      titleLine ++= Array(
        "instance                                           ",
        "algorithm 1", "search_time", "nodes", "return", //CT dom/wdeg
        "algorithm 2", "search_time", "nodes", "return", //CT Impact
        "algorithm 3", "search_time", "nodes", "return", //CT random
        "algorithm 4", "search_time", "nodes", "return", //CT activity
        "algorithm 5", "search_time", "nodes", "return", //GAC3rm+
        "algorithm 6", "search_time", "nodes", "return", //STR
        "algorithm 7", "search_time", "nodes", "return", //MDD+
        "date                           ")
      val resFile = new File(outputRoot + "/" + outputFolder + folderStr + ".csv")
      val writer = CSVWriter.open(resFile)
      writer.writeRow(titleLine)
      writer.close()

      Sorting.quickSort(files)
      // println("exp files:")
      files.foreach(f => {

        println(f.getPath)
        val xm = new XModel(f.getPath, true, fmt)
        var dataLine = new ArrayBuffer[String](30)


        dataLine += f.getName
        dataLine ++= Compact_Table_Wdeg(xm)
        dataLine ++= Compact_Table_Impact(xm)
        dataLine ++= Compact_Table_Random(xm)
        dataLine ++= Compact_Table_Activity(xm)
        dataLine ++= GAC3rm(xm)
        dataLine ++= STR(xm)
        dataLine ++= MDD(xm)

        val day = new Date() //时间戳
        val df = new SimpleDateFormat("MM-dd HH:mm")
        dataLine += df.format(day)

        val inner_writer = CSVWriter.open(resFile, true)
        inner_writer.writeRow(dataLine)
        inner_writer.close()
        println(dataLine)
        dataLine.clear()
        System.gc()
        System.gc()
        System.gc()

      })
    }

  }


  def getIntVars(model: Model): Array[IntVar] = {
    val vars = model.getVars
    val intvars = new Array[IntVar](vars.length)
    var v = 0
    model.getVars.foreach(e => {
      intvars(v) = e.asInstanceOf[IntVar]
      v = v + 1
    })
    intvars
  }

  def XModel2ChocoModel(xm: XModel, ptype: String): Model = {
    val model = new Model
    //val xm = new XModel(in.getPath, true, fmt)
    val intvar2 = new Array[BitsetIntVarImpl](xm.num_vars)
    val tuple2 = new Array[Tuples](xm.num_tabs)
    var i = 0
    while (i < xm.num_vars) {
      intvar2(i) = new BitsetIntVarImpl(i + "", xm.vars.get(i).values, model.ref)
      i += 1
    }
    i = 0
    while (i < xm.num_tabs) {
      tuple2(i) = new Tuples(xm.tabs.get(i).tuples, true)
      i += 1
    }
    i = 0
    while (i < xm.num_tabs) {
      val scp = xm.tabs.get(i).scopeInt
      val scope = new Array[IntVar](scp.length)
      var j = 0
      while (j < scp.length) {
        scope(j) = intvar2(scp(j))
        j += 1
      }
      var p: Propagator[IntVar] = null
      if (ptype.equals("CT"))
        p = new PropCompactTable(scope, tuple2(i))
      else if (ptype.equals("STR"))
        p = new PropTableStr2(scope, tuple2(i))
      else if (ptype.equals("MDD"))
        p = new PropLargeMDDC(new MultivaluedDecisionDiagram(scope, tuple2(i)), scope: _*)
      else if (ptype.equals("GAC3rm"))
        p = new PropLargeGAC3rm(scope, tuple2(i))
      else
        throw new ParserException("Wrong Algorithm match!")

      val pro = Array[Propagator[IntVar]](p);
      val c = new org.chocosolver.solver.constraints.Constraint("TABLE", pro: _*)
      model.post(c)
      i += 1
    }
    model
  }

  def GAC3rm(xm: XModel): ArrayBuffer[String] = {

    var line = new ArrayBuffer[String](4)
    try {

      val model = XModel2ChocoModel(xm, "GAC3rm")

      val solver = model.getSolver

      solver.limitTime(limit_time)
      solver.setSearch(domOverWDegSearch(getIntVars(model): _*))

      //solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "GAC3rm_WDeg"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>
        line += "GAC3rm_WDeg"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def STR(xm: XModel): ArrayBuffer[String] = {

    var line = new ArrayBuffer[String](4)
    try {

      val model = XModel2ChocoModel(xm, "STR")

      val solver = model.getSolver

      solver.limitTime(limit_time)
      solver.setSearch(domOverWDegSearch(getIntVars(model): _*))

      //solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "STR_WDeg"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>
        line += "STR_WDeg"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def Compact_Table_Wdeg(xm: XModel): ArrayBuffer[String] = {

    //println("CT:")
    //val model = new Model

    var line = new ArrayBuffer[String](4)
    // val parser = new XCSPParser


    try {

      // parser.model(model, in.getPath, "CT+")


      val model = XModel2ChocoModel(xm, "CT")
      val solver = model.getSolver

      solver.limitTime(limit_time)
      solver.setSearch(domOverWDegSearch(getIntVars(model): _*))

      //solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "CT_WDeg"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>
        line += "CT_WDeg"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def Compact_Table_Activity(xm: XModel): ArrayBuffer[String] = {

    //println("CT:")
    var line = new ArrayBuffer[String](4)
    // val parser = new XCSPParser
    //val model = new Model

    try {
      val model = XModel2ChocoModel(xm, "CT")
      // parser.model(model, in.getPath, "CT+")
      val solver = model.getSolver


      solver.limitTime(limit_time)


      solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "CT_Activity"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>
        line += "CT_Activity"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def Compact_Table_Impact(xm: XModel): ArrayBuffer[String] = {

    //println("CT:")
    // val parser = new XCSPParser
    // val model = new Model
    var line = new ArrayBuffer[String](4)
    try {
      val model = XModel2ChocoModel(xm, "CT")
      //parser.model(model, in.getPath, "CT+")
      val solver = model.getSolver


      solver.limitTime(limit_time)
      solver.setSearch(Impact_Search(getIntVars(model): _*))

      //solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "CT_Impact"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>
        line += "CT_Impact"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def Compact_Table_Random(xm: XModel): ArrayBuffer[String] = {

    //println("CT:")
    // val parser = new XCSPParser
    //val model = new Model
    var line = new ArrayBuffer[String](4)
    try {
      val model = XModel2ChocoModel(xm, "CT")
      // parser.model(model, in.getPath, "CT+")
      val solver = model.getSolver


      solver.limitTime(limit_time)

      solver.setSearch(randomSearch(getIntVars(model), 0))

      //solver.setSearch(activityBasedSearch(getIntVars(model): _*))
      val r = solver.solve


      line += "CT_Random"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>

        line += "CT_Random"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  def MDD(xm: XModel): ArrayBuffer[String] = {

    //println("CT:")
    //val parser = new XCSPParser
    //val model = new Model
    var line = new ArrayBuffer[String](4)
    try {
      val model = XModel2ChocoModel(xm, "MDD")
      // parser.model(model, in.getPath, "MDD+")
      val solver = model.getSolver


      solver.limitTime(limit_time)


      solver.setSearch(domOverWDegSearch(getIntVars(model): _*))


      val r = solver.solve


      line += "MDD_WDeg"
      line += solver.getTimeCount.toString
      line += solver.getNodeCount.toString
      line += r.toString


    }

    catch {

      case e: Exception =>

        line += "MDD_WDeg"
        line += e.getMessage
        line += "exception"
        line += "exception"
      //e.printStackTrace()
    }
    line
  }

  //  def MDD(in: File): Unit = {
  //
  //    println("MDD:")
  //    val parser = new XCSPParser
  //    val model = new Model
  //
  //    try {
  //      parser.model(model, in.getPath, "MDD+")
  //      val solver = model.getSolver()
  //      solver.limitTime(limit_time)
  //      // solver.propagate()
  //      if (solver.solve) {
  //        print("solution: ")
  //
  //        println()
  //      }
  //      println("node: " + solver.getNodeCount)
  //      println("time: " + solver.getTimeCount + "s")
  //    }
  //
  //    catch {
  //
  //      case e: Exception =>
  //        e.printStackTrace()
  //    }
  //  }

  def getFiles(dir: File): Array[File] = {
    dir.listFiles.filter(_.isFile) ++
      dir.listFiles.filter(_.isDirectory).flatMap(getFiles)
  }


}

