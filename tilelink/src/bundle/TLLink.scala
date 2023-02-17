// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package bundle

import chisel3._
import chisel3.util._
import utils.OH1._

import scala.collection.immutable.SeqMap

object TLLink {
  def hasData(x: TLChannel): Bool = {
    x match {
      case a: TLChannelA => !a.opcode(2)
      //    opcode === PutFullData    ||
      //    opcode === PutPartialData ||
      //    opcode === ArithmeticData ||
      //    opcode === LogicalData
      case b: TLChannelB => !b.opcode(2)
      //    opcode === PutFullData    ||
      //    opcode === PutPartialData ||
      //    opcode === ArithmeticData ||
      //    opcode === LogicalData
      case c: TLChannelC => c.opcode(0)
      //    opcode === AccessAckData ||
      //    opcode === ProbeAckData  ||
      //    opcode === ReleaseData
      case d: TLChannelD => d.opcode(0)
      //    opcode === AccessAckData ||
      //    opcode === GrantData
      case e: TLChannelE => false.B
    }
  }

  def opcode(x: TLChannel): Option[UInt] = {
    x.elements.get("opcode").map(_.asUInt)
  }

  def size(x: TLChannel): Option[UInt] = {
    x.elements.get("size").map(_.asUInt)
  }

  /**
    * Circuit generating total number of data beats in current transaction minus 1.
    */
  def numBeatsMinus1(x: TLChannel): UInt = {
    // exploit the fact that the OH1 encoding of size is exactly size of transaction in bytes minus 1 (2^size - 1)
    x match {
      case a: TLChannelA => (UIntToOH1(a.size, a.parameter.sizeWidth) >> log2Ceil(a.parameter.dataWidth / 8)).asUInt
      case a: TLChannelB => (UIntToOH1(a.size, a.parameter.sizeWidth) >> log2Ceil(a.parameter.dataWidth / 8)).asUInt
      case a: TLChannelC => (UIntToOH1(a.size, a.parameter.sizeWidth) >> log2Ceil(a.parameter.dataWidth / 8)).asUInt
      case a: TLChannelD => (UIntToOH1(a.size, a.parameter.sizeWidth) >> log2Ceil(a.parameter.dataWidth / 8)).asUInt
      case _: TLChannelE => 0.U
    }
  }
}

class TLLink(val parameter: TLLinkParameter) extends Record {
  def a: DecoupledIO[TLChannelA] =
    elements("a").asInstanceOf[DecoupledIO[TLChannelA]]

  def b: DecoupledIO[TLChannelB] =
    elements
      .getOrElse("b", throw new NoTLCException("b", parameter))
      .asInstanceOf[DecoupledIO[TLChannelB]]

  def c: DecoupledIO[TLChannelC] =
    elements
      .getOrElse("c", throw new NoTLCException("c", parameter))
      .asInstanceOf[DecoupledIO[TLChannelC]]

  def d: DecoupledIO[TLChannelD] =
    elements("d").asInstanceOf[DecoupledIO[TLChannelD]]

  def e: DecoupledIO[TLChannelE] =
    elements
      .getOrElse("e", throw new NoTLCException("e", parameter))
      .asInstanceOf[DecoupledIO[TLChannelE]]

  val elements: SeqMap[String, DecoupledIO[Bundle]] =
    SeqMap[String, DecoupledIO[Bundle]](
      "a" -> DecoupledIO(new TLChannelA(parameter.channelAParameter)),
      "d" -> Flipped(DecoupledIO(new TLChannelD(parameter.channelDParameter)))
    ) ++ (
      if (parameter.hasBCEChannels)
        Seq(
          "b" -> Flipped(
            DecoupledIO(new TLChannelB(parameter.channelBParameter.get))
          ),
          "c" -> DecoupledIO(new TLChannelC(parameter.channelCParameter.get)),
          "e" -> DecoupledIO(new TLChannelE(parameter.channelEParameter.get))
        )
      else
        Seq()
    )

}
