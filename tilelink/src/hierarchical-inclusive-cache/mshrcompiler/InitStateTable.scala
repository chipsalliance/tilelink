package org.chipsalliance.hierachicalinclusivecache

import CacheTable._

trait RegisterState

case object StateY extends RegisterState {
  override def toString: String = "Y"
}

case object StateN extends RegisterState {
  override def toString: String = "N"
}

case object StateDC extends RegisterState {
  override def toString: String = "?"
}


object InitStateTable {

  val dirState: Seq[DirectoryState] = DirectoryState.validState
  val requestState: Seq[BundleState] = CacheTable.table
  val tableInput: Iterable[(Hit, Dirty, State, ClientState, Channel, OpCode, Parameter)] =
    dirState.cross(requestState).map { case (ds, rs) =>
      (ds.hit, ds.dirty, ds.state, ds.hitOtherClient, rs.channel, rs.opCode, rs.parameter)
    }.filter( rd =>
      rd match {
        // release require Hit
        case (HitN, _:Dirty, _:State, _:ClientState, C , _: OpCode, _: Parameter) => false
        // AcquireBlock | AcquirePerm BtoT require Hit
        case (HitN, _: Dirty, _: State, _: ClientState, A, AcquireBlock | AcquirePerm, BtoT) => false
        // release will definitely hit the initiator
        case (_, _: Dirty, _: State, NoClient | HitOtherClient, C, _: OpCode, _: Parameter) => false
        // Acquire BtoT will definitely hit the initiator
        case (_: Hit, _: Dirty, _: State, NoClient | HitOtherClient, A, AcquirePerm | AcquireBlock, BtoT) => false
        // release BtoX, Local can only be Tip | Branch
        case (_: Hit, _: Dirty, Trunk | Nothing, _: ClientState, C, _: OpCode, BtoN | BtoB) => false
        // release TtoX, ClientState can only be HitSelf
        case (_: Hit, _: Dirty, _: State, HitAll, C, _: OpCode, TtoB | TtoT | TtoN) => false
        // release TtoX, State can only be Trunk
        case (_: Hit, _: Dirty, Tip | Branch | Nothing, _: ClientState, C, _: OpCode, TtoB | TtoT | TtoN) => false
        // B X channel will not hit itself
        case (_: Hit, _: Dirty, _: State, HitAll | HitSelf, B | X, _: OpCode, _: Parameter) => false
        case (_: Hit, _: Dirty, _: State, _: ClientState, _: Channel, _: OpCode, _: Parameter) => true
      }
    )

  val tableList = tableInput.map { rd =>
    // (_: Hit, _:Dirty, _:State, _:HitOtherClient, _: Channel , _: OpCode, _: Parameter)
    val (h, d, s, c, n, o, p) = rd
    val needT = (n, o, p) match {
      case (A, PutFullData | PutPartialData | ArithmeticData | LogicalData , _) => true
      case (A, Hint , PREFETCH_WRITE) => true
      case (A, AcquireBlock | AcquirePerm , NtoT | BtoT) => true
      case _ => false
    }

    val needAcquire: Boolean = (n, h, s, needT) match {
      case (A, HitN, _, _) => true
      case (A, _, Branch, true) => true
      case _ => false
    }
    val sAcquire = if (needAcquire) StateN else StateY

    // Does a request prove the client need not be probed?
    val skipProbeN = o match {
      case AcquireBlock | AcquirePerm | Get | Hint => true
      case _ => false
    }

    // probe caused by permission transfer
    val needPProb = (n, h, s, c, needT, p) match {
      case (A, HitY, Trunk, HitOtherClient | HitAll, _, _) => true
      case (A, HitY, _, HitSelf, true, _) => !skipProbeN
      case (A, HitY, _, HitOtherClient | HitAll, true, _) => true
      // prob to N & hit client
      case (B, HitY, _, HitOtherClient | HitAll, _, ToN) => true
      // prob and state is Trunk
      case (B, HitY, Trunk, _, _, _) => true
      case _ => false
    }

    val sPProbe = if (needPProb) StateN else StateY

    val needGrantAck = (n, o) match {
      case (A, AcquireBlock | AcquirePerm) => true
      case _ => false
    }
    val wGrantAck = if (needGrantAck) StateN else StateY

    val sExecute = rd match {
      case (_: Hit, _:Dirty, _:State, _:ClientState, X | B , _: OpCode, _: Parameter) => StateY
      case _ => StateN
    }

    val sFlush = rd match {
      case (_: Hit, _:Dirty, _:State, _:ClientState, X , _: OpCode, _: Parameter) => StateN
      case _ => StateY
    }
    val sWriteBack = if (needAcquire || needPProb || needGrantAck) StateN else rd match {
      // for c channel
      // Do we need to go dirty?
      case (_: Hit, DirtyN, _:State, _:ClientState, C, ReleaseData, _: Parameter) => StateN
      // Does our state change?
      case (_: Hit, _:Dirty, Trunk, _:ClientState, C, ReleaseData | Release, TtoB | BtoB) => StateN
      // Do our clients change?
      case (_: Hit, _:Dirty, _:State, _:ClientState, C , ReleaseData | Release, TtoN | BtoN | NtoN) => StateN
      // for b channel
      case (HitY, DirtyY, _:State, _:ClientState, B , _: OpCode, _: Parameter) => StateN
      case (HitY, _, _:State, _:ClientState, B , _: OpCode, ToN) => StateN
      case (HitY, _, Tip | Trunk, _:ClientState, B , _: OpCode, ToB) => StateN
      // for x channel
      // for a channel
      // Becomes dirty?
      case (HitY, DirtyN, _: State, _: ClientState, A , PutFullData | PutPartialData | ArithmeticData | LogicalData, _: Parameter) => StateN
      case _ => StateY
    }

    val sRelease = if (false) StateN else (h, s, n) match {
      // X
      case (HitY, _:State, X) => StateN
      // A
      case (HitN, Branch | Trunk | Tip, A) => StateN
      case _ => StateY
    }

    // probe caused by release
    val sRProbe = (c, n, sRelease) match {
      case (HitSelf | HitOtherClient | HitAll, X, StateN) => StateN
      case (HitSelf | HitOtherClient | HitAll, A, StateN) => StateN
      case _ => StateY
    }

    val sProbAck = n match {
      case A => StateY
      case B => StateN
      case C => StateY
      case X => StateY
      case _ => StateY
    }
    // sRProbe: sRProbe, wRProbeAckFirst, wRProbeAckLast
    // sRelease: sRelease, wReleaseAck
    // sPProbe: sPProbe, wPProbeAckFirst, wPProbeAckLast, wPProbeAck
    // sAcquire: sAcquire, wGrantFirst, wGrantLast, wGrant, sGrantAck
    (rd, Seq(sRProbe, sRelease, sPProbe, sAcquire, sFlush, sProbAck, sExecute, wGrantAck, sWriteBack))
  }
}
