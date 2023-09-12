// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2022 Jiuyang Liu <liu@jiuyang.me>

import mill._
import mill.scalalib._

trait TileLinkModule extends ScalaModule {
  def chiselModule: ScalaModule
  def chiselPluginJar: T[PathRef]
  override def moduleDeps = super.moduleDeps ++ Some(chiselModule)
  override def scalacPluginClasspath = T(super.scalacPluginClasspath() ++ Some(chiselPluginJar()))
  override def scalacOptions = T(super.scalacOptions() ++ Some(s"-Xplugin:${chiselPluginJar().path}"))
}
