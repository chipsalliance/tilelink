package org.chipsalliance.hierachicalinclusivecache

import chisel3.{Bool, UInt}
import chisel3.util.BitPat
import chisel3.util.experimental.decode.{TruthTable, decoder}


object StateTransfer extends App {
  val stateMap: collection.mutable.Map[MSHRState, MSHRState] = collection.mutable.Map.empty[MSHRState, MSHRState]
  val valueMap: collection.mutable.Map[Int, Int] = collection.mutable.Map.empty[Int, Int]
  case class MSHRState(
                            sRProbe: RegisterState,
                            sRelease: RegisterState,
                            sPProbe: RegisterState,
                            sAcquire: RegisterState,
                            sGrantAck: RegisterState,
                            sFlush: RegisterState,
                            sProbAck: RegisterState,
                            sExecute: RegisterState,
                            sWriteBack: RegisterState
                          ) {
    // prob first
    def probe: Boolean = (sRProbe, sPProbe) match {
      case (StateN, _) => true
      case (_, StateN) => true
      case _ => false
    }

    def release: Boolean = (sRProbe, sRelease) match {
      case (StateY, StateN) => true
      case _ => false
    }

    def acquire: Boolean = (sRelease, sPProbe, sAcquire) match {
      case (StateY, StateY, StateN) => true
      case _ => false
    }

    def flush: Boolean = (sRelease, sFlush) match {
      case (StateY, StateN) => true
      case _ => false
    }

    def probAck: Boolean = (sPProbe, sProbAck) match {
      case (StateY, StateN) => true
      case _ => false
    }

    def grantAck: Boolean = (sAcquire, sGrantAck) match {
      case (StateY, StateN) => true
      case _ => false
    }

    def execute: Boolean = (sPProbe, sAcquire, sExecute) match {
      case (StateY, StateY, StateN) => true
      case _ => false
    }

    def writeBack: Boolean = (sRProbe, sRelease, sAcquire, sPProbe, sExecute, sWriteBack) match {
      case (StateY, StateY, StateY, StateY, StateY, StateN) => true
      case _ => false
    }

    def allSchedulerFinish: Boolean =
      (sRProbe,sRelease,sPProbe,sAcquire,sGrantAck,sFlush,sProbAck,sExecute,sWriteBack) match {
        case (StateY, StateY, StateY, StateY, StateY, StateY, StateY, StateY, StateY) => true
        case _ => false
      }

    def value: Int =
      Seq(sRProbe,sRelease,sPProbe,sAcquire,sGrantAck,sFlush,sProbAck,sExecute,sWriteBack).map {
        case StateY => 0
        case _ => 1
      }.foldLeft(0) {case (p, b) => (p << 1) + b}

    def nextState = copy(
      sRProbe = if (probe) StateY else sRProbe,
      sRelease = if (release) StateY else sRelease,
      sPProbe = if (probe) StateY else sPProbe,
      sAcquire = if (acquire) StateY else sAcquire,
      sGrantAck = if (grantAck) StateY else sGrantAck,
      sFlush = if (flush) StateY else sFlush,
      sProbAck = if (probAck) StateY else sProbAck,
      sExecute = if (execute) StateY else sExecute,
      sWriteBack = if (writeBack) StateY else sWriteBack,
    )

    def appendMap: Any = {
      if (!(stateMap.keys.toSeq.contains(this) || allSchedulerFinish)) {
        stateMap += this -> nextState
        valueMap += this.value -> nextState.value
        nextState.appendMap
      }
    }

    def asBitPat: BitPat = sRProbe.asBitPat ## sRelease.asBitPat ## sPProbe.asBitPat ## sAcquire.asBitPat ##
      sGrantAck.asBitPat ## sFlush.asBitPat ## sProbAck.asBitPat ## sExecute.asBitPat ## sWriteBack.asBitPat
  }

  val initStateList = InitStateTable.tableList.map { case (channel, initState) =>
    val Seq(sRProbe, sRelease, sPProbe, sAcquire, sFlush, sProbAck, sExecute, _, sWriteBack) = initState
    val stateInstance = MSHRState(
      sRProbe = sRProbe,
      sRelease = sRelease,
      sPProbe = sPProbe,
      sAcquire = sAcquire,
      sGrantAck = sAcquire,
      sFlush = sFlush,
      sProbAck = sProbAck,
      sExecute = sExecute,
      sWriteBack = sWriteBack
    )
    stateInstance
  }.toSet
  initStateList.foreach { state =>
    state.appendMap
  }

  // init table
  val initTable: Iterable[(BitPat, BitPat)] = InitStateTable.tableList.map { case (inputMessage, initState) =>
    val Seq(sRProbe, sRelease, sPProbe, sAcquire, sFlush, sProbAck, sExecute, wGrantAck, sWriteBack) = initState
    // (_: Hit, _:Dirty, _:State, _:HitOtherClient, _: Channel , _: OpCode, _: Parameter)
    val (h, d, s, c, n, o, p) = inputMessage
    h.asBitPat ## d.asBitPat ## s.asBitPat ## c.asBitPat ## n.asBitPat ## o.asBitPat ## p.asBitPat ->
      sRProbe.asBitPat ## sRelease.asBitPat ## sPProbe.asBitPat ## sAcquire.asBitPat ## sFlush.asBitPat ##
        sProbAck.asBitPat ## sExecute.asBitPat ## wGrantAck.asBitPat ## sWriteBack.asBitPat
  }

  val transferTable: Iterable[(BitPat, BitPat)] = stateMap.map { case (previous, next) =>
    previous.asBitPat -> next.asBitPat
  }

  def initDecode(
                  hit: Bool,
                  dirty: Bool,
                  cacheState: UInt,
                  hitState: UInt,
                  channel: UInt,
                  opcode: UInt,
                  param: UInt
                ): UInt = {
    decoder(
      hit ## dirty ## cacheState ## hitState ## channel ## opcode ## param,
      TruthTable(initTable, BitPat.dontCare(9))
    )
  }

  def transferDecode(
                      sRProbe: Bool,
                      sRelease: Bool,
                      sPProbe: Bool,
                      sAcquire: Bool,
                      sFlush: Bool,
                      sProbAck: Bool,
                      sExecute: Bool,
                      wGrantAck: Bool,
                      sWriteBack: Bool,
                ): UInt = {
    decoder(
      sRProbe ## sRelease ## sPProbe ## sAcquire ## sFlush ## sProbAck ## sExecute ## wGrantAck ## sWriteBack,
      TruthTable(transferTable, BitPat.dontCare(9))
    )
  }
}
