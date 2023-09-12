package org.chipsalliance.hierachicalinclusivecache

case class MSHRTableParam(
                         isLastLeve: Boolean,
                         supportGet: Boolean,
                         directoryOnly: Boolean
                         ) {

}

trait Channel

trait OpCode

trait Parameter

trait Probe

trait Grow

trait Shrink

trait Report

trait Cap

trait HintParam

trait Arithmetic

trait Logic

case class BundleState(channel: Channel, opCode: OpCode, parameter: Parameter)

case object A extends Channel

case object C extends Channel

case object B extends Channel

case object X extends Channel

// A
case object PutFullData extends OpCode

case object PutPartialData extends OpCode

case object ArithmeticData extends OpCode

case object LogicalData extends OpCode

case object Get extends OpCode

case object Hint extends OpCode

case object AcquireBlock extends OpCode

case object AcquirePerm extends OpCode

case object ReservedOp extends OpCode

// B
case object Probe extends OpCode

// C
case object Release extends OpCode

case object ReleaseData extends OpCode

case object NtoB extends Parameter with Grow

case object NtoT extends Parameter with Grow

case object BtoT extends Parameter with Grow

case object TtoB extends Parameter with Shrink

case object TtoN extends Parameter with Shrink

case object BtoN extends Parameter with Shrink

case object TtoT extends Parameter with Report

case object BtoB extends Parameter with Report

case object NtoN extends Parameter with Report

case object ToN extends Parameter with Cap

case object ToB extends Parameter with Cap

case object ToT extends Parameter with Cap

case object PREFETCH_READ extends Parameter with HintParam

case object PREFETCH_WRITE extends Parameter with HintParam

case object ReservedParam extends Parameter

case object MIN extends Parameter with Arithmetic

case object MAX extends Parameter with Arithmetic

case object MINU extends Parameter with Arithmetic

case object MAXU extends Parameter with Arithmetic

case object ADD extends Parameter with Arithmetic

case object XOR extends Parameter with Logic

case object OR extends Parameter with Logic

case object AND extends Parameter with Logic

case object SWAP extends Parameter with Logic


/** There are two types of Probe:
  * Release Probe should be nested:
  * SinkC > SourceC(middle level cache)/SourceA(last level cache)
  * After this Probe, a write-back is scheduled(Release in SourceC/Put in SourceA),
  * we need to make sure that:
  * Transaction in SinkC(which has a same set) can be processed by a new MSHR before the write-back.
  * This is used for avoid possible dead-lock:
  * cache is evicting a cacheline, when master is also evicting it:
  * cache probes to master hits a outstanding transaction,
  *
  * Permission Transmission Probe:
  *
  *
  */
case object ScheduleProbe

/** sProbe
  * wDirAccess
  * wPFirst
  * wPLast
  */

/** Need to update directory at the first beat of ProbeAck.
  * making sure nested request can access correct directory.
  *
  * caused by: Flush cacheline via MMIO or eviction.
  */
case object WaitFirstReleaseProbe

case object WaitLastReleaseProbe

object CacheTable {
  def bundleState(paramter: MSHRTableParam): Seq[BundleState] = (
    Seq(
      A,
      X,
      B,
      C) cross Seq(
      PutFullData,
      PutPartialData,
      ArithmeticData,
      LogicalData,
      Get,
      Hint,
      AcquireBlock,
      AcquirePerm,
      Release,
      ReleaseData,
      Probe,
      ReservedOp
    ) cross Seq(
      NtoB,
      NtoT,
      BtoT,
      TtoB,
      TtoN,
      BtoN,
      TtoT,
      BtoB,
      NtoN,
      ToN,
      ToB,
      ToT,
      MIN,
      MAX,
      MINU,
      MAXU,
      ADD,
      XOR,
      OR,
      AND,
      SWAP,
      ReservedParam
    ))
    .toSeq
    .map(v => (v._1._1, v._1._2, v._2))
    .filter {
      case (A, AcquireBlock | AcquirePerm, _: Grow) => true
      case (A, ArithmeticData, _: Arithmetic) => true
      case (A, LogicalData, _: Logic) => true
      case (A, Hint, _: HintParam) => true
      case (A, PutFullData | PutPartialData | Get, ReservedParam) => true
      case (C, Release | ReleaseData, NtoN) => false
      case (C, Release | ReleaseData, _: Report | _: Shrink) => true
      case (B, Probe, _: Cap) => !paramter.isLastLeve
      case (X, ReservedOp, ReservedParam) => true
      case _ => false
    } map { case (c, o, p) => BundleState(c, o, p) }

  implicit class Crossable[X](xs: Iterable[X]) {
    def cross[Y](ys: Iterable[Y]) = for {x <- xs; y <- ys} yield (x, y)
  }
  val table = bundleState(MSHRTableParam(isLastLeve = false, supportGet = false, directoryOnly = false))
}
