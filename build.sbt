name := "sbt_choco"

version := "0.1"

scalaVersion := "2.12.7"

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies += "com.github.tototoshi" % "scala-csv_2.12" % "1.3.5"
libraryDependencies += "org.msgpack" % "msgpack-core" % "0.8.16"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12.0-RC1" % "1.0.6"
libraryDependencies += "org.xcsp" % "xcsp3-tools" % "1.0.0"
libraryDependencies += "net.sf.trove4j" % "trove4j" % "3.0.3"
libraryDependencies += "org.choco-solver" % "choco-sat" % "1.0.2"
libraryDependencies += "org.choco-solver" % "cutoffseq" % "1.0.5"
libraryDependencies += "org.jgrapht" % "jgrapht-core" % "1.3.1"
libraryDependencies += "org.jheaps" % "jheaps" % "0.10"
libraryDependencies += "org.knowm.xchart" % "xchart" % "3.5.4"
libraryDependencies += "org.mockito" % "mockito-core" % "2.28.2"
libraryDependencies += "org.objenesis" % "objenesis" % "2.6"
libraryDependencies += "com.github.cp-profiler" % "cpprof-java" % "1.3.0"
libraryDependencies += "dk.brics.automaton" % "automaton" % "1.11-8"


mainClass in (Compile, run) := Some("amtf.testAllDiff")
