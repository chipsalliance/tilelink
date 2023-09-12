// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2022 Jiuyang Liu <liu@jiuyang.me>

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._

import $file.dependencies.chisel.build
import $file.common

object v {
  val scala = "2.13.11"
}

object chisel extends Chisel

trait Chisel
  extends millbuild.dependencies.chisel.build.Chisel {
  def crossValue = v.scala
  override def millSourcePath = os.pwd / "dependencies" / "chisel"
  def scalaVersion = T(v.scala)
}

object tilelink extends common.TileLinkModule with ScalafmtModule { m =>
  def millSourcePath = os.pwd / "tilelink"
  def scalaVersion = T(v.scala)
  def chiselModule = chisel
  def chiselPluginJar = T(chisel.pluginModule.jar())
}
