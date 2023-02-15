// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package bundle

import chisel3.util.isPow2

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait TLChannelParameter

object TLChannelParameter        {
  implicit val rw: RW[TLChannelParameter] = RW.merge(
    TileLinkChannelAParameter.rw,
    TileLinkChannelBParameter.rw,
    TileLinkChannelCParameter.rw,
    TileLinkChannelDParameter.rw,
    TileLinkChannelEParameter.rw
  )

  def bundle(p: TLChannelParameter): TLChannel = {
    p match {
      case a: TileLinkChannelAParameter => new TLChannelA(a)
      case b: TileLinkChannelBParameter => new TLChannelB(b)
      case c: TileLinkChannelCParameter => new TLChannelC(c)
      case d: TileLinkChannelDParameter => new TLChannelD(d)
      case e: TileLinkChannelEParameter => new TLChannelE(e)
    }
  }
}

case class TileLinkChannelAParameter(
  addressWidth: Int,
  sourceWidth:  Int,
  dataWidth:    Int,
  sizeWidth:    Int)
    extends TLChannelParameter {
  require(addressWidth > 0)
  require(sourceWidth > 0)
  require(dataWidth > 0)
  require(sizeWidth > 0)
  require(dataWidth % 8 == 0, "Width of data field must be multiples of 8")
  require(
    isPow2(dataWidth / 8),
    "Width of data field in bytes must be power of 2"
  )
}
object TileLinkChannelAParameter {
  implicit val rw: RW[TileLinkChannelAParameter] = macroRW
}

case class TileLinkChannelBParameter(
  addressWidth: Int,
  sourceWidth:  Int,
  dataWidth:    Int,
  sizeWidth:    Int)
    extends TLChannelParameter {
  require(addressWidth > 0)
  require(sourceWidth > 0)
  require(dataWidth > 0)
  require(sizeWidth > 0)
  require(dataWidth % 8 == 0, "Width of data field must be multiples of 8")
  require(
    isPow2(dataWidth / 8),
    "Width of data field in bytes must be power of 2"
  )
}
object TileLinkChannelBParameter {
  implicit val rw: RW[TileLinkChannelBParameter] = macroRW
}

case class TileLinkChannelCParameter(
  addressWidth: Int,
  sourceWidth:  Int,
  dataWidth:    Int,
  sizeWidth:    Int)
    extends TLChannelParameter {
  require(addressWidth > 0)
  require(sourceWidth > 0)
  require(dataWidth > 0)
  require(sizeWidth > 0)
  require(dataWidth % 8 == 0, "Width of data field must be multiples of 8")
  require(
    isPow2(dataWidth / 8),
    "Width of data field in bytes must be power of 2"
  )
}
object TileLinkChannelCParameter {
  implicit val rw: RW[TileLinkChannelCParameter] = macroRW
}

case class TileLinkChannelDParameter(
  sourceWidth: Int,
  sinkWidth:   Int,
  dataWidth:   Int,
  sizeWidth:   Int)
    extends TLChannelParameter {
  require(sourceWidth > 0)
  require(sinkWidth > 0)
  require(dataWidth > 0)
  require(sizeWidth > 0)
  require(dataWidth % 8 == 0, "Width of data field must be multiples of 8")
  require(
    isPow2(dataWidth / 8),
    "Width of data field in bytes must be power of 2"
  )
}
object TileLinkChannelDParameter {
  implicit val rw: RW[TileLinkChannelDParameter] = macroRW
}

case class TileLinkChannelEParameter(sinkWidth: Int) extends TLChannelParameter {
  require(sinkWidth > 0)
}
object TileLinkChannelEParameter {
  implicit val rw: RW[TileLinkChannelEParameter] = macroRW
}
