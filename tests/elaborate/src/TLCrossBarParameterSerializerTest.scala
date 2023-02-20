package tests.elaborate

import chisel3.util.experimental.BitSet
import upickle.default._

import org.chipsalliance.tilelink.bundle.TLLinkParameter
import org.chipsalliance.tilelink.xbar._

object TLCrossBarParameterSerializerTest {
  def main(args: Array[String]): Unit = {
    val linkParameter : TLLinkParameter = TLLinkParameter(
      addressWidth = 32,
      sourceWidth = 2,
      sinkWidth = 2,
      dataWidth = 64,
      sizeWidth = 4,
      hasBCEChannels = true
    )

    val masterLinkParameter = Seq(TLCrossBarMasterLinkParameter(
      linkParameter = linkParameter,
      adVisibility = BitSet.fromString("b??"),
      bceVisibility = BitSet.fromString("b??"),
      srcIdRange = TLIdRange(0, 1)
    ))

    val slaveLinkParameter = Seq(TLCrossBarSlaveLinkParameter(
      linkParameter = linkParameter,
      adVisibility = BitSet.fromString("b??"),
      bceVisibility = BitSet.fromString("b??"),
      sinkIdRange = TLIdRange(0, 1),
      addressRange = BitSet.fromRange(0x80000000, 0x10000, 32)
    ),
      TLCrossBarSlaveLinkParameter(
        linkParameter = linkParameter,
        adVisibility = BitSet.fromString("b??"),
        bceVisibility = BitSet.fromString("b??"),
        sinkIdRange = TLIdRange(2, 3),
        addressRange = BitSet.fromRange(0x80010000, 0x10000, 32)
      ))

    val crossBarParameter = TLCrossBarParameter(
      TLArbiterPolicy.RoundRobin,
      masterLinkParameter,
      slaveLinkParameter
    )

    val serializedConfig = ujson.Obj(
      "generator" -> "org.chipsalliance.tilelink.xbar.TLCrossBar",
      "parameter" -> upickle.default.writeJs(crossBarParameter),
    ).render(indent = 2)
    os.write(os.pwd / "config.json", serializedConfig)
  }
}
