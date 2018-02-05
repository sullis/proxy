package lib

sealed trait Envelope extends Product with Serializable

object Envelope {

  case object Request extends Envelope { override def toString = "request" }

  case object Response extends Envelope { override def toString = "response" }

  val all = Seq(Request, Response)

  private[this]
  val byName = all.map(x => x.toString.toLowerCase -> x).toMap

  def fromString(value: String): Option[Envelope] = byName.get(value.toLowerCase)

}
