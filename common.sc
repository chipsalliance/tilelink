import mill._
import mill.scalalib._

// For now TileLink only support compile from source
trait TileLinkModule extends ScalaModule {
  def chisel3Module: ScalaModule
  def chisel3PluginJar: T[PathRef]

  override def moduleDeps = Seq(chisel3Module)

  override def scalacPluginClasspath = T {
    super.scalacPluginClasspath() ++ Some(chisel3PluginJar())
  }

  override def scalacOptions = T {
    super.scalacOptions() ++ Some(s"-Xplugin:${chisel3PluginJar().path}")
  }
}
