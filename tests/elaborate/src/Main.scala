package tests.elaborate

import chisel3.experimental.SerializableModuleGenerator
import chisel3.stage.phases.Elaborate
import chisel3.hack.Convert
import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.AnnotationSeq
import mainargs.{arg, main, ParserForMethods}
import org.chipsalliance.tilelink.xbar.{TLCrossBar, TLCrossBarParameter}

object Main {
  @main
  def TLCrossBar(
    @arg(short = 'j', name = "json", doc = "path to json configuration") jsonPath: String,
    @arg(short = 'd', name = "dir", doc = "path to output directory") dirPath:     String
  ) = {
    var topName: String = null
    val annos: AnnotationSeq = Seq(
      new Elaborate,
      new Convert
    ).foldLeft(
      Seq(
        ChiselGeneratorAnnotation(() =>
          upickle.default
            .read[
              SerializableModuleGenerator[TLCrossBar, TLCrossBarParameter]
            ](
              ujson.read(
                os.read(
                  os.Path(jsonPath)
                )
              )
            )
            .module()
        )
      ): AnnotationSeq
    ) { case (annos, stage) => stage.transform(annos) }
      .flatMap {
        case FirrtlCircuitAnnotation(circuit) =>
          topName = circuit.main
          os.write(os.Path(dirPath) / s"$topName.fir", circuit.serialize)
          None
        case _: chisel3.stage.DesignAnnotation[_] => None
        case a => Some(a)
      }
    os.write(os.Path(dirPath) / s"$topName.anno.json", firrtl.annotations.JsonProtocol.serialize(annos))
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
