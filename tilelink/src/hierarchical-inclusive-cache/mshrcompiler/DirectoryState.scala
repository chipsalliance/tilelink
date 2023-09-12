package org.chipsalliance.hierachicalinclusivecache


trait Hit

trait Dirty

trait State

trait ClientState

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
