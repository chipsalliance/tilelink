import mill._
import mill.scalalib._
import mill.define.{TaskModule, Command}
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.TestModule.Utest
import coursier.maven.MavenRepository
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.chiseltest.build
import $file.common

object v {
  val scala = "2.13.10"
  val utest = ivy"com.lihaoyi::utest:latest.integration"
  val mainargs = ivy"com.lihaoyi::mainargs:latest.integration"
}

// Before Chisel5, TileLink only support compile from source with master version of Chisel

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "treadle"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mychiseltest extends dependencies.chiseltest.build.chiseltestCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chiseltest"
  def chisel3Module: Option[PublishModule] = Some(mychisel3)
  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
  def treadleModule: Option[PublishModule] = Some(mytreadle)
  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}

object tilelink extends common.TileLinkModule with ScalafmtModule {
  m =>
  def millSourcePath = os.pwd / "tilelink"
  def scalaVersion = T(v.scala)
  def chisel3Module = mychisel3
  def chisel3PluginJar = T(mychisel3.plugin.jar())
  def publishVersion = T(de.tobiasroeser.mill.vcs.version.VcsVersion.vcsState().format())
  def pomSettings = T(PomSettings(
    description = artifactName(),
    organization = "org.chipsalliance",
    url = "https://github.com/chipsalliance/tilelink",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("chipsalliance", "tilelink"),
    developers = Seq(
      Developer("sequencer", "Jiuyang Liu", "https://github.com/sequencer")
    )
  ))

}

// The CoSim Framework, TBD
object tests extends Module {
  // This should be a CrossModule for testing
  object elaborate extends ScalaModule with ScalafmtModule {
    override def scalaVersion = v.scala

    override def scalacPluginClasspath = T(Agg(mychisel3.plugin.jar()))
    override def scalacOptions = T(super.scalacOptions() ++ Some(mychisel3.plugin.jar()).map(path => s"-Xplugin:${path.path}") ++ Seq("-Ymacro-annotations"))

    override def moduleDeps = Seq(tilelink)
    override def ivyDeps = T(Seq(
      v.mainargs
    ))
    override def mainClass = Some("tests.elaborate.Main")

    // TODO: use Cross to support multiple config files for testing.
    def config: T[PathRef] = T { PathRef(os.pwd / "config.json") }

    // TODO: add config as input to elaborate
    def elaborate = T {
      // class path for `moduleDeps` is only a directory, not a jar, which breaks the cache.
      // so we need to manually add the class files of `moduleDeps` here.
      upstreamCompileOutput()
      mill.modules.Jvm.runLocal(
        finalMainClass(),
        runClasspath().map(_.path),
        Seq(
          "--dir", T.dest.toString,
          "--json", config().path.toString
        ),
      )
      PathRef(T.dest)
    }

    def chiselAnno = T(os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("anno.json") => p }.map(PathRef(_)).get)

    def chirrtl = T(os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("fir") => p }.map(PathRef(_)).get)

    def topName = T(chirrtl().path.last.split('.').head)
  }

  object mfccompile extends Module {

    def compile = T {
      os.proc("firtool",
        elaborate.chirrtl().path,
        s"--annotation-file=${elaborate.chiselAnno().path}",
        "-disable-infer-rw",
        "-dedup",
        "-O=debug",
        "--preserve-values=named",
        "--output-annotation-file=mfc.anno.json",
        s"-o=${T.dest}/concat.sv"
      ).call(T.dest)
      PathRef(T.dest)
    }

    def rtls = T {
      os.read(compile().path / "filelist.f").split("\n").map(str =>
        try {
          os.Path(str)
        } catch {
          case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
            compile().path / str.stripPrefix("./")
        }
      ).filter(p => p.ext == "v" || p.ext == "sv").map(PathRef(_)).toSeq
    }


    def annotations = T {
      os.walk(compile().path).filter(p => p.last.endsWith("mfc.anno.json")).map(PathRef(_))
    }
  }

  /*
  object emulator extends Module {

    def csrcDir = T.source {
      PathRef(millSourcePath / "src")
    }

    def allCHeaderFiles = T.sources {
      os.walk(csrcDir().path).filter(_.ext == "h").map(PathRef(_))
    }

    // TODO: add cosim codes here.
    def allCSourceFiles = T.sources {
      Seq(
      ).map(f => PathRef(csrcDir().path / f))
    }

    def verilatorArgs = T {
      Seq(
        // format: off
        "--x-initial unique",
        "--output-split 100000",
        "--max-num-width 1048576",
        "--main",
        "--timing",
        // format: on
      )
    }

    // TODO: compute topName
    def topName = T("")

    def verilatorThreads = T(8)

    def CMakeListsString = T {
      // format: off
      s"""cmake_minimum_required(VERSION 3.20)
         |project(emulator)
         |set(CMAKE_CXX_STANDARD 17)
         |
         |find_package(args REQUIRED)
         |find_package(glog REQUIRED)
         |find_package(fmt REQUIRED)
         |find_package(verilator REQUIRED)
         |find_package(Threads REQUIRED)
         |set(THREADS_PREFER_PTHREAD_FLAG ON)
         |
         |add_executable(emulator
         |${allCSourceFiles().map(_.path).mkString("\n")}
         |)
         |
         |target_include_directories(emulator PUBLIC ${csrcDir().path.toString})
         |
         |target_link_libraries(emulator PUBLIC $${CMAKE_THREAD_LIBS_INIT})
         |target_link_libraries(emulator PUBLIC libspike fmt::fmt glog::glog)  # note that libargs is header only, nothing to link
         |target_compile_definitions(emulator PRIVATE COSIM_VERILATOR)
         |
         |verilate(emulator
         |  SOURCES
         |  ${mfccompile.rtls().map(_.path.toString).mkString("\n")}
         |  TRACE_FST
         |  TOP_MODULE ${elaborate.topName()}
         |  PREFIX V${elaborate.topName()}
         |  OPT_FAST
         |  THREADS ${verilatorThreads()}
         |  VERILATOR_ARGS ${verilatorArgs().mkString(" ")}
         |)
         |""".stripMargin
      // format: on
    }

    def cmakefileLists = T {
      val path = T.dest / "CMakeLists.txt"
      os.write.over(path, CMakeListsString())
      PathRef(T.dest)
    }

    def buildDir = T {
      PathRef(T.dest)
    }

    def config = T {
      mill.modules.Jvm.runSubprocess(Seq("cmake",
        "-G", "Ninja",
        "-S", cmakefileLists().path,
        "-B", buildDir().path
      ).map(_.toString), Map[String, String](), T.dest)
    }

    def elf = T {
      // either rtl or testbench change should trigger elf rebuild
      mfccompile.rtls()
      allCSourceFiles()
      allCHeaderFiles()
      config()
      mill.modules.Jvm.runSubprocess(Seq("ninja", "-C", buildDir().path).map(_.toString), Map[String, String](), buildDir().path)
      PathRef(buildDir().path / "emulator")
    }
  }
  */
}
