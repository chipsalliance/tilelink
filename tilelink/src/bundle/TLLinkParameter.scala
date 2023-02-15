// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package bundle

import chisel3.util.isPow2

import upickle.default.{macroRW, ReadWriter => RW}

/** Parameter of a TileLink link bundle
  *
  * All width values are specified in bits
  *
  * @param hasBCEChannels
  *   whether the link has channel B, C and E (i.e. is a TL-C link)
  */
case class TLLinkParameter(
  addressWidth:   Int,
  sourceWidth:    Int,
  sinkWidth:      Int,
  dataWidth:      Int,
  sizeWidth:      Int,
  hasBCEChannels: Boolean) {
  require(addressWidth > 0)
  require(sourceWidth > 0)
  require(sinkWidth > 0)
  require(dataWidth > 0)
  require(sizeWidth > 0)
  require(dataWidth % 8 == 0, "Width of data field must be multiples of 8")
  require(
    isPow2(dataWidth / 8),
    "Width of data field in bytes must be power of 2"
  )

  def channelAParameter: TileLinkChannelAParameter         =
    TileLinkChannelAParameter(addressWidth, sourceWidth, dataWidth, sizeWidth)
  def channelBParameter: Option[TileLinkChannelBParameter] =
    Option.when(hasBCEChannels)(TileLinkChannelBParameter(addressWidth, sourceWidth, dataWidth, sizeWidth))
  def channelCParameter: Option[TileLinkChannelCParameter] =
    Option.when(hasBCEChannels)(TileLinkChannelCParameter(addressWidth, sourceWidth, dataWidth, sizeWidth))
  def channelDParameter: TileLinkChannelDParameter         =
    TileLinkChannelDParameter(sourceWidth, sinkWidth, dataWidth, sizeWidth)
  def channelEParameter: Option[TileLinkChannelEParameter] =
    Option.when(hasBCEChannels)(TileLinkChannelEParameter(sinkWidth))
}

object TLLinkParameter {
  def union(x: TLLinkParameter*): TLLinkParameter =
    TLLinkParameter(
      addressWidth = x.map(_.addressWidth).max,
      sourceWidth = x.map(_.sourceWidth).max,
      sinkWidth = x.map(_.sinkWidth).max,
      dataWidth = x.map(_.dataWidth).max,
      sizeWidth = x.map(_.sizeWidth).max,
      hasBCEChannels = x.map(_.hasBCEChannels).fold(false)(_ || _)
    )

  implicit val rw: RW[TLLinkParameter] = macroRW
}
