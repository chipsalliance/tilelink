// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2017 SiFive, Inc.

package org.chipsalliance.tilelink
package xbar

import chisel3.util._
import chisel3._

import bundle._

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait TLArbiterPolicy
object TLArbiterPolicy {
  case object Priority extends TLArbiterPolicy {
    implicit val rw: RW[this.type] = macroRW
  }
  case object RoundRobin extends TLArbiterPolicy {
    implicit val rw: RW[this.type] = macroRW
  }
  implicit val rw: RW[TLArbiterPolicy] = RW.merge(Priority.rw, RoundRobin.rw)
}

case class TLArbiterParameter(
  policy:              TLArbiterPolicy,
  inputLinkParameters: Seq[TLChannelParameter],
  outputLinkParameter: TLChannelParameter)
    extends chisel3.experimental.SerializableModuleParameter
object TLArbiterParameter {
  implicit val rw: RW[TLArbiterParameter] = macroRW
}

class TLArbiter(val parameter: TLArbiterParameter)
    extends Module
    with chisel3.experimental.SerializableModule[TLArbiterParameter] {

  // (width, valid_s, select) => ready_s
  val policyImpl: (Integer, UInt, Bool) => UInt = {
    parameter.policy match {
      case TLArbiterPolicy.Priority =>
        (width, valids, _) => (~(scanLeftOr(valids) << 1)(width - 1, 0)).asUInt
      case TLArbiterPolicy.RoundRobin =>
        (width, valids, select) =>
          if (width == 1) 1.U(1.W)
          else {
            val valid = valids(width - 1, 0)
            assert(valid === valids)
            val mask = RegInit(((BigInt(1) << width) - 1).U(width - 1, 0))
            val filter = Cat(scanRightOr(valid & ~mask), valid)
            val unready = (filter >> 1).asUInt | (mask << width).asUInt
            val readys = (~((unready >> width).asUInt & unready(width - 1, 0))).asUInt
            when(select && valid.orR) {
              mask := scanLeftOr(readys & valid)
            }
            readys(width - 1, 0)
          }
    }
  }

  val sink = IO(
    Flipped(
      DecoupledIO(TLChannelParameter.bundle(parameter.outputLinkParameter))
    )
  )
  val sources = parameter.inputLinkParameters.map(p => IO(DecoupledIO(TLChannelParameter.bundle(p))))

  if (parameter.inputLinkParameters.isEmpty) {
    sink.valid := false.B
    sink.bits := DontCare
  } else if (parameter.inputLinkParameters.size == 1) {
    sink <> sources.head
  } else {
    val beatsIn = sources.map(s => TLLink.numBeatsMinus1(s.bits))

    val beatsLeft = RegInit(0.U)
    val idle = beatsLeft === 0.U
    val latch = idle && sink.ready // TODO: winner (if any) claims sink

    // Who wants access to the sink?
    val valids = sources.map(_.valid)

    val readys = VecInit(policyImpl(valids.size, Cat(valids.reverse).asUInt, latch).asBools)
    val winner = VecInit(readys.zip(valids).map { case (r, v) => r && v })

    // confirm policy make sense
    require(readys.size == valids.size)

    // Never two winners
    val prefixOR = winner.scanLeft(false.B)(_ || _).init
    assert(prefixOR.zip(winner).map { case (p, w) => !p || !w }.reduce { _ && _ })
    // If there was any request, there is a winner
    assert(!valids.reduce(_ || _) || winner.reduce(_ || _))

    // Track remaining beats
    val maskedBeats = winner.zip(beatsIn).map { case (w, b) => Mux(w, b, 0.U) }
    val initBeats = maskedBeats.reduce(_ | _) // no winner => 0 beats
    beatsLeft := Mux(latch, initBeats, beatsLeft - sink.fire)

    // The one-hot source granted access in the previous cycle
    val state = RegInit(VecInit(Seq.fill(sources.size)(false.B)))
    val muxState = Mux(idle, winner, state)
    state := muxState

    val allowed = Mux(idle, readys, state)
    sources.zip(allowed).foreach { case (s, r) => s.ready := sink.ready && r }
    sink.valid := Mux(idle, valids.reduce(_ || _), Mux1H(state, valids))
    sink.bits :<= Mux1H(state, sources.map(_.bits))
  }
}
