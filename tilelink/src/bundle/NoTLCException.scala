// SPDX-License-Identifier: Apache-2.0

package org.chipsalliance.tilelink
package bundle

import chisel3.ChiselException
private class NoTLCException(
  channel:       String,
  linkParameter: TLLinkParameter)
    extends ChiselException(
      s"call $channel in TLLink is not present in a TL-UL or TL-UH bus:\n $linkParameter"
    )
