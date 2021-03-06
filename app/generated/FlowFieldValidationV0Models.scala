/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.7.67
 * apibuilder 0.14.75 app.apibuilder.io/flow/field-validation/0.7.67/play_2_x_json
 */
package io.flow.field.validation.v0.models {

  sealed trait FieldValidationRule extends _root_.scala.Product with _root_.scala.Serializable

  /**
   * Defines the valid discriminator values for the type FieldValidationRule
   */
  sealed trait FieldValidationRuleDiscriminator extends _root_.scala.Product with _root_.scala.Serializable

  object FieldValidationRuleDiscriminator {

    case object FieldValidationRequired extends FieldValidationRuleDiscriminator { override def toString = "required" }
    case object FieldValidationMin extends FieldValidationRuleDiscriminator { override def toString = "min" }
    case object FieldValidationMax extends FieldValidationRuleDiscriminator { override def toString = "max" }
    case object FieldValidationPattern extends FieldValidationRuleDiscriminator { override def toString = "pattern" }

    final case class UNDEFINED(override val toString: String) extends FieldValidationRuleDiscriminator

    val all: scala.List[FieldValidationRuleDiscriminator] = scala.List(FieldValidationRequired, FieldValidationMin, FieldValidationMax, FieldValidationPattern)

    private[this] val byName: Map[String, FieldValidationRuleDiscriminator] = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): FieldValidationRuleDiscriminator = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[FieldValidationRuleDiscriminator] = byName.get(value.toLowerCase)

  }

  /**
   * @param length Maximum specified length of characters in the form field for text or the maximum
   *        number of elements for a list.
   */
  final case class FieldValidationMax(
    length: Int
  ) extends FieldValidationRule

  /**
   * @param length Minimum specified length of characters in the form field for text or the maximum
   *        number of elements for a list.
   */
  final case class FieldValidationMin(
    length: Int
  ) extends FieldValidationRule

  /**
   * @param pattern Regular expression used to pattern match a valid string
   */
  final case class FieldValidationPattern(
    pattern: String
  ) extends FieldValidationRule

  /**
   * Indicates a field is required
   * 
   * @param unused Field is a placeholder as required by API Builder
   */
  final case class FieldValidationRequired(
    unused: String
  ) extends FieldValidationRule

  /**
   * Provides future compatibility in clients - in the future, when a type is added
   * to the union FieldValidationRule, it will need to be handled in the client code.
   * This implementation will deserialize these future types as an instance of this
   * class.
   * 
   * @param description Information about the type that we received that is undefined in this version of
   *        the client.
   */
  final case class FieldValidationRuleUndefinedType(
    description: String
  ) extends FieldValidationRule

}

package io.flow.field.validation.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.field.validation.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map { str =>
      _root_.java.util.UUID.fromString(str)
    }

    private[v0] implicit val jsonWritesUUID = new Writes[_root_.java.util.UUID] {
      def writes(x: _root_.java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[_root_.org.joda.time.DateTime] {
      def writes(x: _root_.org.joda.time.DateTime) = {
        JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(x))
      }
    }

    private[v0] implicit val jsonReadsJodaLocalDate = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(str)
    }

    private[v0] implicit val jsonWritesJodaLocalDate = new Writes[_root_.org.joda.time.LocalDate] {
      def writes(x: _root_.org.joda.time.LocalDate) = {
        JsString(_root_.org.joda.time.format.ISODateTimeFormat.date.print(x))
      }
    }

    implicit def jsonReadsFieldValidationFieldValidationMax: play.api.libs.json.Reads[FieldValidationMax] = {
      (__ \ "length").read[Int].map { x => new FieldValidationMax(length = x) }
    }

    def jsObjectFieldValidationMax(obj: io.flow.field.validation.v0.models.FieldValidationMax): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "length" -> play.api.libs.json.JsNumber(obj.length)
      )
    }

    implicit def jsonReadsFieldValidationFieldValidationMin: play.api.libs.json.Reads[FieldValidationMin] = {
      (__ \ "length").read[Int].map { x => new FieldValidationMin(length = x) }
    }

    def jsObjectFieldValidationMin(obj: io.flow.field.validation.v0.models.FieldValidationMin): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "length" -> play.api.libs.json.JsNumber(obj.length)
      )
    }

    implicit def jsonReadsFieldValidationFieldValidationPattern: play.api.libs.json.Reads[FieldValidationPattern] = {
      (__ \ "pattern").read[String].map { x => new FieldValidationPattern(pattern = x) }
    }

    def jsObjectFieldValidationPattern(obj: io.flow.field.validation.v0.models.FieldValidationPattern): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "pattern" -> play.api.libs.json.JsString(obj.pattern)
      )
    }

    implicit def jsonReadsFieldValidationFieldValidationRequired: play.api.libs.json.Reads[FieldValidationRequired] = {
      (__ \ "unused").read[String].map { x => new FieldValidationRequired(unused = x) }
    }

    def jsObjectFieldValidationRequired(obj: io.flow.field.validation.v0.models.FieldValidationRequired): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "unused" -> play.api.libs.json.JsString(obj.unused)
      )
    }

    implicit def jsonReadsFieldValidationFieldValidationRule: play.api.libs.json.Reads[FieldValidationRule] = new play.api.libs.json.Reads[FieldValidationRule] {
      def reads(js: play.api.libs.json.JsValue): play.api.libs.json.JsResult[FieldValidationRule] = {
        (js \ "discriminator").asOpt[String].getOrElse { sys.error("Union[FieldValidationRule] requires a discriminator named 'discriminator' - this field was not found in the Json Value") } match {
          case "required" => js.validate[io.flow.field.validation.v0.models.FieldValidationRequired]
          case "min" => js.validate[io.flow.field.validation.v0.models.FieldValidationMin]
          case "max" => js.validate[io.flow.field.validation.v0.models.FieldValidationMax]
          case "pattern" => js.validate[io.flow.field.validation.v0.models.FieldValidationPattern]
          case other => play.api.libs.json.JsSuccess(io.flow.field.validation.v0.models.FieldValidationRuleUndefinedType(other))
        }
      }
    }

    def jsObjectFieldValidationRule(obj: io.flow.field.validation.v0.models.FieldValidationRule): play.api.libs.json.JsObject = {
      obj match {
        case x: io.flow.field.validation.v0.models.FieldValidationRequired => jsObjectFieldValidationRequired(x) ++ play.api.libs.json.Json.obj("discriminator" -> "required")
        case x: io.flow.field.validation.v0.models.FieldValidationMin => jsObjectFieldValidationMin(x) ++ play.api.libs.json.Json.obj("discriminator" -> "min")
        case x: io.flow.field.validation.v0.models.FieldValidationMax => jsObjectFieldValidationMax(x) ++ play.api.libs.json.Json.obj("discriminator" -> "max")
        case x: io.flow.field.validation.v0.models.FieldValidationPattern => jsObjectFieldValidationPattern(x) ++ play.api.libs.json.Json.obj("discriminator" -> "pattern")
        case other => {
          sys.error(s"The type[${other.getClass.getName}] has no JSON writer")
        }
      }
    }

    implicit def jsonWritesFieldValidationFieldValidationRule: play.api.libs.json.Writes[FieldValidationRule] = {
      new play.api.libs.json.Writes[io.flow.field.validation.v0.models.FieldValidationRule] {
        def writes(obj: io.flow.field.validation.v0.models.FieldValidationRule) = {
          jsObjectFieldValidationRule(obj)
        }
      }
    }
  }
}

package io.flow.field.validation.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}

    // import models directly for backwards compatibility with prior versions of the generator
    import Core._

    object Core {
      implicit def pathBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.DateTime] = ApibuilderPathBindable(ApibuilderTypes.dateTimeIso8601)
      implicit def queryStringBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.DateTime] = ApibuilderQueryStringBindable(ApibuilderTypes.dateTimeIso8601)

      implicit def pathBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.LocalDate] = ApibuilderPathBindable(ApibuilderTypes.dateIso8601)
      implicit def queryStringBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.LocalDate] = ApibuilderQueryStringBindable(ApibuilderTypes.dateIso8601)
    }

    trait ApibuilderTypeConverter[T] {

      def convert(value: String): T

      def convert(value: T): String

      def example: T

      def validValues: Seq[T] = Nil

      def errorMessage(key: String, value: String, ex: java.lang.Exception): String = {
        val base = s"Invalid value '$value' for parameter '$key'. "
        validValues.toList match {
          case Nil => base + "Ex: " + convert(example)
          case values => base + ". Valid values are: " + values.mkString("'", "', '", "'")
        }
      }
    }

    object ApibuilderTypes {
      val dateTimeIso8601: ApibuilderTypeConverter[_root_.org.joda.time.DateTime] = new ApibuilderTypeConverter[_root_.org.joda.time.DateTime] {
        override def convert(value: String): _root_.org.joda.time.DateTime = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(value)
        override def convert(value: _root_.org.joda.time.DateTime): String = _root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(value)
        override def example: _root_.org.joda.time.DateTime = _root_.org.joda.time.DateTime.now
      }

      val dateIso8601: ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] = new ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] {
        override def convert(value: String): _root_.org.joda.time.LocalDate = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(value)
        override def convert(value: _root_.org.joda.time.LocalDate): String = _root_.org.joda.time.format.ISODateTimeFormat.date.print(value)
        override def example: _root_.org.joda.time.LocalDate = _root_.org.joda.time.LocalDate.now
      }
    }

    final case class ApibuilderQueryStringBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends QueryStringBindable[T] {

      override def bind(key: String, params: Map[String, Seq[String]]): _root_.scala.Option[_root_.scala.Either[String, T]] = {
        params.getOrElse(key, Nil).headOption.map { v =>
          try {
            Right(
              converters.convert(v)
            )
          } catch {
            case ex: java.lang.Exception => Left(
              converters.errorMessage(key, v, ex)
            )
          }
        }
      }

      override def unbind(key: String, value: T): String = {
        s"$key=${converters.convert(value)}"
      }
    }

    final case class ApibuilderPathBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends PathBindable[T] {

      override def bind(key: String, value: String): _root_.scala.Either[String, T] = {
        try {
          Right(
            converters.convert(value)
          )
        } catch {
          case ex: java.lang.Exception => Left(
            converters.errorMessage(key, value, ex)
          )
        }
      }

      override def unbind(key: String, value: T): String = {
        converters.convert(value)
      }
    }

  }

}
