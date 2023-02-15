// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2017 SiFive, Inc.

package org.chipsalliance.tilelink
package utils

import chisel3._
import chisel3.util.{Cat, OHToUInt}

object OH1 {
  def OH1ToOH(x: UInt): UInt =
    ((x << 1).asUInt | 1.U) & (~Cat(0.U(1.W), x)).asUInt

  def OH1ToUInt(x: UInt): UInt = OHToUInt(OH1ToOH(x))

  def UIntToOH1(x: UInt, width: Int): UInt =
    (~((-1).S(width.W).asUInt << x)(width - 1, 0)).asUInt

  def UIntToOH1(x: UInt): UInt = UIntToOH1(x, (1 << x.getWidth) - 1)
}
