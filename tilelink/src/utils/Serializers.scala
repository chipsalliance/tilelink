// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package utils

import upickle.default.{readwriter, ReadWriter => RW}

object Serializers {
  // Inject serializers for chisel3 BitSet and BitPat
  implicit val bitPatSerializer: RW[chisel3.util.BitPat] =
    readwriter[String].bimap(_.rawString, s => { val bs: String = "b" ++ s; chisel3.util.BitPat(bs) })
  implicit val bitSetSerializer: RW[chisel3.util.experimental.BitSet] =
    readwriter[Seq[chisel3.util.BitPat]].bimap(_.terms.toSeq, chisel3.util.experimental.BitSet(_: _*))
}
