package org.chipsalliance.hierachicalinclusivecache


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
  pprint.pprintln(initStateList)
}
