package zio.jwt

import zio.jwt.JwksParsingSpec.getClass

import scala.io.Source

def stringFromFile(
    fileName: String,
  ): String = {
  val source = Source.fromInputStream(getClass.getResourceAsStream(fileName))
  val string =
    try source.mkString
    finally source.close()
  string
}
