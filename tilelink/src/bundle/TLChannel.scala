// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package bundle

import chisel3._

trait TLChannel extends Bundle {
  val parameter: TLChannelParameter
}

class TLChannelA(val parameter: TileLinkChannelAParameter) extends TLChannel {
  private val maskWidth = parameter.dataWidth / 8
  // NOTE: this field is called a_code in TileLink spec version 1.8.1 p. 15, which is probably a typo
  val opcode:  UInt = UInt(OpCode.width)
  val param:   UInt = UInt(Param.width)
  val size:    UInt = UInt(parameter.sizeWidth.W)
  val source:  UInt = UInt(parameter.sourceWidth.W)
  val address: UInt = UInt(parameter.addressWidth.W)
  val mask:    UInt = UInt(maskWidth.W)
  val data:    UInt = UInt(parameter.dataWidth.W)
  val corrupt: Bool = Bool()
}

class TLChannelB(val parameter: TileLinkChannelBParameter) extends TLChannel {
  private val maskWidth = parameter.dataWidth / 8
  val opcode:  UInt = UInt(OpCode.width)
  val param:   UInt = UInt(Param.width)
  val size:    UInt = UInt(parameter.sizeWidth.W)
  val source:  UInt = UInt(parameter.sourceWidth.W)
  val address: UInt = UInt(parameter.addressWidth.W)
  val mask:    UInt = UInt(maskWidth.W)
  val data:    UInt = UInt(parameter.dataWidth.W)
  val corrupt: Bool = Bool()
}

class TLChannelC(val parameter: TileLinkChannelCParameter) extends TLChannel {
  val opcode:  UInt = UInt(OpCode.width)
  val param:   UInt = UInt(Param.width)
  val size:    UInt = UInt(parameter.sizeWidth.W)
  val source:  UInt = UInt(parameter.sourceWidth.W)
  val address: UInt = UInt(parameter.addressWidth.W)
  val data:    UInt = UInt(parameter.dataWidth.W)
  val corrupt: Bool = Bool()
}

class TLChannelD(val parameter: TileLinkChannelDParameter) extends TLChannel {
  val opcode:  UInt = UInt(OpCode.width)
  val param:   UInt = UInt(Param.width)
  val size:    UInt = UInt(parameter.sizeWidth.W)
  val source:  UInt = UInt(parameter.sourceWidth.W)
  val sink:    UInt = UInt(parameter.sinkWidth.W)
  val denied:  Bool = Bool()
  val data:    UInt = UInt(parameter.dataWidth.W)
  val corrupt: Bool = Bool()
}

class TLChannelE(val parameter: TileLinkChannelEParameter) extends TLChannel {
  val sink: UInt = UInt(parameter.sinkWidth.W)
}
