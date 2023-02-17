// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2017 SiFive, Inc.

package org.chipsalliance.tilelink
package xbar

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BitSet
import chisel3.util.experimental.decode.decoder

import bundle._

object TLCrossBar {
  private def fanout(
    input:  DecoupledIO[TLChannel],
    select: Seq[Bool]
  ): Seq[DecoupledIO[TLChannel]] = {
    val filtered = Wire(Vec(select.size, chiselTypeOf(input)))
    filtered.zip(select).foreach {
      case (chan, selected) =>
        chan.bits := input.bits
        chan.valid := input.valid && (selected || (select.size == 1).B)
    }
    input.ready := Mux1H(select, filtered.map(_.ready))
    filtered
  }
}

class TLCrossBar(val parameter: TLCrossBarParameter)
    extends Module
    with chisel3.experimental.SerializableModule[TLCrossBarParameter] {

  val masterLinksIO = parameter.masters.map(_.linkParameter).map { link =>
    IO(Flipped(new TLLink(link)))
  }
  val slaveLinksIO = parameter.slaves.map(_.linkParameter).map { link =>
    IO(new TLLink(link))
  }

  val masterLinksRemapped = Wire(
    Vec(parameter.masters.size, new TLLink(parameter.commonLinkParameter))
  )
  val slaveLinksRemapped = Wire(
    Vec(parameter.slaves.size, new TLLink(parameter.commonLinkParameter))
  )

  private def trim(id: UInt, size: Int): UInt = id(log2Ceil(size) - 1, 0)

  // id remapping
  parameter.adReachableIO
    .lazyZip(masterLinksIO)
    .lazyZip(masterLinksRemapped)
    .lazyZip(parameter.srcIdRemapTable)
    .foreach {
      case (connects, io, remapped, range) =>
        if (connects.exists(x => x)) {
          remapped.a :<>= io.a
          remapped.a.bits.source := io.a.bits.source | range.start.U

          io.d :<>= remapped.d
          io.d.bits.source := trim(remapped.d.bits.source, range.size)
        } else {
          remapped.a.valid := false.B
          remapped.a.bits := DontCare
          io.a.ready := false.B
          io.a.bits := DontCare

          io.d.valid := false.B
          io.d.bits := DontCare
          remapped.d.ready := false.B
          remapped.d.bits := DontCare
        }
    }
  parameter.bceReachableIO
    .lazyZip(masterLinksIO)
    .lazyZip(masterLinksRemapped)
    .lazyZip(parameter.srcIdRemapTable)
    .foreach {
      case (connects, io, remapped, range) =>
        if (connects.exists(x => x)) {
          io.b :<>= remapped.b
          io.b.bits.source := trim(remapped.b.bits.source, range.size)

          remapped.c :<>= io.c
          remapped.c.bits.source := io.c.bits.source | range.start.U

          remapped.e :<>= io.e
        } else {
          io.b.valid := false.B
          io.b.bits := DontCare
          remapped.b.ready := false.B
          remapped.b.bits := DontCare

          remapped.c.valid := false.B
          remapped.c.bits := DontCare
          io.c.ready := false.B
          io.c.bits := DontCare

          remapped.e.valid := false.B
          remapped.e.bits := DontCare
          io.e.ready := false.B
          io.e.bits := DontCare
        }
    }

  parameter.adReachableOI
    .lazyZip(slaveLinksIO)
    .lazyZip(slaveLinksRemapped)
    .lazyZip(parameter.sinkIdRemapTable)
    .foreach {
      case (connects, io, remapped, range) =>
        if (connects.exists(x => x)) {
          remapped.a :<>= io.a

          io.d :<>= remapped.d
          io.d.bits.sink := trim(remapped.d.bits.sink, range.size)
        } else {
          remapped.a.valid := false.B
          remapped.a.bits := DontCare
          io.a.ready := false.B
          io.a.bits := DontCare

          io.d.valid := false.B
          io.d.bits := DontCare
          remapped.d.ready := false.B
          remapped.d.bits := DontCare
        }
    }

  parameter.bceReachableOI
    .lazyZip(slaveLinksIO)
    .lazyZip(slaveLinksRemapped)
    .lazyZip(parameter.sinkIdRemapTable)
    .foreach {
      case (connects, io, remapped, range) =>
        if (connects.exists(x => x)) {
          io.b :<>= remapped.b
          remapped.c :<>= io.c
          remapped.e :<>= io.e

          remapped.e.bits.sink := io.e.bits.sink | range.start.U
        } else {
          io.b.valid := false.B
          io.b.bits := DontCare
          remapped.b.ready := false.B
          remapped.b.bits := DontCare

          remapped.c.valid := false.B
          remapped.c.bits := DontCare
          io.c.ready := false.B
          io.c.bits := DontCare

          remapped.e.valid := false.B
          remapped.e.bits := DontCare
          io.e.ready := false.B
          io.e.bits := DontCare
        }
    }

  private def unique(x:       Vector[Boolean]) = x.count(x => x) <= 1
  private def filter[T](data: Seq[T], mask: Seq[Boolean]) = data.zip(mask).filter(_._2).map(_._1)

  // Based on input=>output connectivity, create per-input minimal address decode circuits
  val addressableOs = (parameter.adReachableIO ++ parameter.bceReachableIO).distinct
  val outputPortFns: Map[Vector[Boolean], UInt => Seq[Bool]] =
    addressableOs.map { addressable =>
      if (unique(addressable)) {
        (addressable, (_: UInt) => addressable.map(_.B))
      } else {
        val ports = parameter.slaves.map(_.addressRange)
        val maxBits = log2Ceil(1 + ports.map(_.getWidth).max)
        val maskedPorts = ports.zip(addressable).map {
          case (port, true) => port.intersect(BitPat.dontCare(maxBits))
          case (_, false)   => BitSet.empty
        }
        //noinspection RedundantDefaultArgument
        (addressable, (addr: UInt) => decoder.bitset(addr, maskedPorts, errorBit = false).asBools)
      }
    }.toMap

  val addressA = masterLinksRemapped.map(_.a.bits.address)
  val addressC = masterLinksRemapped.map(_.c.bits.address)

  val requestAIO = parameter.adReachableIO.zip(addressA).map { case (c, a) => outputPortFns(c)(a) }
  val requestCIO = parameter.bceReachableIO.zip(addressC).map { case (c, a) => outputPortFns(c)(a) }
  val requestBOI = slaveLinksRemapped.map { o => parameter.srcIdRemapTable.map { i => i.contains(o.b.bits.source) } }
  val requestDOI = slaveLinksRemapped.map { o => parameter.srcIdRemapTable.map { i => i.contains(o.d.bits.source) } }
  val requestEIO = masterLinksRemapped.map { i => parameter.sinkIdRemapTable.map { o => o.contains(i.e.bits.sink) } }

  val portsAOI = masterLinksRemapped.zip(requestAIO).map { case (i, r) => TLCrossBar.fanout(i.a, r) }.transpose
  val portsBIO = slaveLinksRemapped.zip(requestBOI).map { case (o, r) => TLCrossBar.fanout(o.b, r) }.transpose
  val portsCOI = masterLinksRemapped.zip(requestCIO).map { case (i, r) => TLCrossBar.fanout(i.c, r) }.transpose
  val portsDIO = slaveLinksRemapped.zip(requestDOI).map { case (o, r) => TLCrossBar.fanout(o.d, r) }.transpose
  val portsEOI = masterLinksRemapped.zip(requestEIO).map { case (i, r) => TLCrossBar.fanout(i.e, r) }.transpose

  slaveLinksRemapped.lazyZip(portsAOI).lazyZip(parameter.adReachableOI).foreach {
    case (portO, portI, reachable) =>
      val arbiter = Module(
        new TLArbiter(
          TLArbiterParameter(
            policy = parameter.arbitrationPolicy,
            inputLinkParameters = filter(portI.map(_.bits.parameter), reachable),
            outputLinkParameter = portO.a.bits.parameter
          )
        )
      )
      arbiter.sources.zip(filter(portI, reachable)).foreach { case (o, i) => o :<>= i }
      portO.a :<>= arbiter.sink.asInstanceOf[DecoupledIO[TLChannelA]]
      filter(portI, reachable.map(!_)).foreach { i => i.ready := false.B }
  }
  masterLinksRemapped.lazyZip(portsBIO).lazyZip(parameter.bceReachableIO).foreach {
    case (portI, portO, reachable) =>
      val arbiter = Module(
        new TLArbiter(
          TLArbiterParameter(
            policy = parameter.arbitrationPolicy,
            inputLinkParameters = filter(portO.map(_.bits.parameter), reachable),
            outputLinkParameter = portI.b.bits.parameter
          )
        )
      )
      arbiter.sources.zip(filter(portO, reachable)).foreach { case (i, o) => i :<>= o }
      portI.b :<>= arbiter.sink.asInstanceOf[DecoupledIO[TLChannelB]]
      filter(portO, reachable.map(!_)).foreach { o => o.ready := false.B }
  }
  slaveLinksRemapped.lazyZip(portsCOI).lazyZip(parameter.bceReachableOI).foreach {
    case (portO, portI, reachable) =>
      val arbiter = Module(
        new TLArbiter(
          TLArbiterParameter(
            policy = parameter.arbitrationPolicy,
            inputLinkParameters = filter(portI.map(_.bits.parameter), reachable),
            outputLinkParameter = portO.c.bits.parameter
          )
        )
      )
      arbiter.sources.zip(filter(portI, reachable)).foreach { case (o, i) => o :<>= i }
      portO.c :<>= arbiter.sink.asInstanceOf[DecoupledIO[TLChannelC]]
      filter(portI, reachable.map(!_)).foreach { i => i.ready := false.B }
  }
  masterLinksRemapped.lazyZip(portsDIO).lazyZip(parameter.adReachableIO).foreach {
    case (portI, portO, reachable) =>
      val arbiter = Module(
        new TLArbiter(
          TLArbiterParameter(
            policy = parameter.arbitrationPolicy,
            inputLinkParameters = filter(portO.map(_.bits.parameter), reachable),
            outputLinkParameter = portI.d.bits.parameter
          )
        )
      )
      arbiter.sources.zip(filter(portO, reachable)).foreach { case (i, o) => i :<>= o }
      portI.d :<>= arbiter.sink.asInstanceOf[DecoupledIO[TLChannelD]]
      filter(portO, reachable.map(!_)).foreach { o => o.ready := false.B }
  }
  slaveLinksRemapped.lazyZip(portsEOI).lazyZip(parameter.bceReachableOI).foreach {
    case (portO, portI, reachable) =>
      val arbiter = Module(
        new TLArbiter(
          TLArbiterParameter(
            policy = parameter.arbitrationPolicy,
            inputLinkParameters = filter(portI.map(_.bits.parameter), reachable),
            outputLinkParameter = portO.e.bits.parameter
          )
        )
      )
      arbiter.sources.zip(filter(portI, reachable)).foreach { case (o, i) => o :<>= i }
      portO.e :<>= arbiter.sink.asInstanceOf[DecoupledIO[TLChannelE]]
      filter(portI, reachable.map(!_)).foreach { i => i.ready := false.B }
  }
}
