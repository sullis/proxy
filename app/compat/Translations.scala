package compat

import javax.inject.{Inject, Singleton}

import play.api.Logger

@Singleton
class Translations @Inject() () {

  private[this] val DefaultTranslationCatalog = TranslationCatalog(
    locale = "en_US",
    keys = Map(
      "invalid_cvn" -> "CVN is not valid"
    ),
    fallbackCatalog = None
  )

  private[this] val catalogs = Seq[TranslationCatalog](
    DefaultTranslationCatalog,
    TranslationCatalog(
      locale = "en_FR",
      keys = Map(
        "invalid_cvn" -> "Le NVC n'est pas valide"
      ),
      fallbackCatalog = Some(DefaultTranslationCatalog)
    )
  )

  private[this] val catalogMap: Map[String, TranslationCatalog] = Map(
    catalogs.map { c =>
      c.locale -> c
    }: _*
  )

  def locale(locale: String): TranslationCatalog = {
    catalogMap.getOrElse(
      locale,
      {
        Logger.warn(s"[Translations] No catalog found for locale[$locale] - using default[${DefaultTranslationCatalog.locale}]")
        DefaultTranslationCatalog
      }
    )
  }

}

case class TranslationCatalog(
  locale: String,
  keys: Map[String, String],
  fallbackCatalog: Option[TranslationCatalog]
) {

  def lookup(key: String): Option[String] = {
    keys.get(key) match {
      case Some(value) => Some(value)
      case None => {
        println(s" -- could not find key[$key] - available: ${keys.keys}")
        fallbackCatalog.flatMap(_.lookup(key))
      }
    }
  }

}



