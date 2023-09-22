package org.chipsalliance.hierachicalinclusivecache

import chisel3.util.BitPat


trait Hit {
  def asBitPat = {
    this match {
      case HitN => BitPat.N()
      case HitY => BitPat.Y()
      case _ => BitPat.dontCare(width = 1)
    }
  }
}

trait Dirty {
  def asBitPat = {
    this match {
      case DirtyN => BitPat.N()
      case DirtyY => BitPat.Y()
      case _ => BitPat.dontCare(width = 1)
    }
  }
}

trait State {
  def asBitPat: BitPat = {
    this match {
      case Branch => BitPat("b01")
      case Nothing => BitPat("b00")
      case Tip => BitPat("b11")
      case Trunk => BitPat("b10")
      case _ => BitPat.dontCare(2)
    }
  }
}

trait ClientState {
  def asBitPat: BitPat = {
    this match {
      case HitAll => BitPat("b11")
      case HitOtherClient => BitPat("b10")
      case HitSelf => BitPat("b01")
      case NoClient => BitPat("b00")
      case _ => BitPat.dontCare(2)
    }
  }
}

// HitAll = HitOtherClient && HitSelf
case object HitAll extends ClientState

case object HitOtherClient extends ClientState

case object HitSelf extends ClientState

case object NoClient extends ClientState

case object HitY extends Hit

case object HitN extends Hit

case object DirtyY extends Dirty

case object DirtyN extends Dirty

case object Tip extends State

case object Trunk extends State

case object Branch extends State

case object Nothing extends State

case class DirectoryState(hit: Hit, dirty: Dirty, state: State, hitOtherClient: ClientState)

object DirectoryState {
  val stateCross: Seq[(Hit, Dirty, State, ClientState)] =
    for {
      x <- Seq(HitY, HitN);
      y <- Seq(DirtyY, DirtyN);
      z <- Seq(Tip, Trunk, Branch, Nothing);
      j <- Seq(HitAll, HitOtherClient, HitSelf, NoClient)
    } yield (x, y, z, j)


  def filterFunction(a: (Hit, Dirty, State, ClientState)): Boolean = {
    a match {
      case (HitY, _: Dirty, Nothing, _) => false
      case (_: Hit, DirtyY, Branch | Nothing, _) => false
      case (_, _, Nothing | Tip, HitSelf | HitOtherClient | HitAll) => false
      case (_, _, Trunk, NoClient | HitAll) => false
      case _ => true
    }
  }

  val allState = stateCross.map { case (h, d, s, c) => DirectoryState(h, d, s, c) }

  val validState: Seq[DirectoryState] = stateCross.filter {
    s => filterFunction(s)
  } map { case (h, d, s, c) => DirectoryState(h, d, s, c) }

  val invalidState: Seq[DirectoryState] = stateCross.filter {
    s => !filterFunction(s)
  } map { case (h, d, s, c) => DirectoryState(h, d, s, c) }
}
