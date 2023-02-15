// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2017 SiFive, Inc.

package org.chipsalliance.tilelink
package xbar

import chisel3._
import chisel3.util._

import upickle.default.{macroRW, ReadWriter => RW}

// A non-empty half-open range; [start, end)
// TODO: remove TLIdRange, enforce id range as power-of-2 limits
case class TLIdRange(start: Int, end: Int) extends Ordered[TLIdRange] {
  require(start >= 0, s"Ids cannot be negative, but got: $start.")
  require(start <= end, "Id ranges cannot be negative.")

  def compare(x: TLIdRange) = {
    val primary   = (this.start - x.start).sign
    val secondary = (x.end - this.end).sign
    if (primary != 0) primary else secondary
  }

  def overlaps(x: TLIdRange) = start < x.end && x.start < end
  def contains(x: TLIdRange) = start <= x.start && x.end <= end

  def contains(x: Int)  = start <= x && x < end
  def contains(x: UInt) =
    if (size == 0) {
      false.B
    } else if (size == 1) { // simple comparison
      x === start.U
    } else {
      // find index of largest different bit
      val largestDeltaBit   = log2Floor(start ^ (end - 1))
      val smallestCommonBit = largestDeltaBit + 1 // may not exist in x
      val uncommonMask      = (1 << smallestCommonBit) - 1
      val uncommonBits      = (x | 0.U(largestDeltaBit.W))(largestDeltaBit, 0)
      // the prefix must match exactly (note: may shift ALL bits away)
      (x >> smallestCommonBit) === (start >> smallestCommonBit).U &&
        // firrtl constant prop range analysis can eliminate these two:
        (start & uncommonMask).U <= uncommonBits &&
        uncommonBits <= ((end - 1) & uncommonMask).U
    }

  def shift(x: Int) = TLIdRange(start + x, end + x)
  def size          = end - start
  def isEmpty       = end == start

  def range = start until end
}
object TLIdRange {
  def overlaps(s: Seq[TLIdRange]) =
    if (s.isEmpty) None
    else {
      val ranges = s.sorted
      ranges.tail.zip(ranges.init).find { case (a, b) => a.overlaps(b) }
    }

  implicit val rw: RW[TLIdRange] = macroRW
}
