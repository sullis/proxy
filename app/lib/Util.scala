package lib

object Util {

  def toFlatSeq(data: Map[String, Seq[String]]): Seq[(String, String)] = {
    data.map { case (k, vs) =>
      vs.map(k -> _)
    }.flatten.toSeq
  }

  def removeKeys(
    data: Map[String, Seq[String]],
    keys: Seq[String]
  ): Map[String, Seq[String]] = {
    data.filter { case (k, _) =>
      !keys.contains(k)
    }
  }

}
