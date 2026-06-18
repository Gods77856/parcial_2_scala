import java.util.regex.Pattern

/**
 * Clase base abstracta para todas las entidades nombradas.
 *
 * Una entidad nombrada es una expresión del texto que refiere a un objeto
 * del mundo real (persona, lugar, organización, tecnología, etc.).
 *
 * @param text el texto tal como aparece en el corpus
 */
abstract class NamedEntity(val text: String) {

  /**
   * Retorna el tipo de la entidad como String.
   */
  def entityType: String

  /**
   * Indica si esta entidad aparece como una unidad independiente en un texto.
   *
   * La implementación base ignora mayúsculas y minúsculas. Las aserciones
   * negativas impiden aceptar la entidad cuando está pegada a otra letra o
   * dígito; así, por ejemplo, "Java" no coincide dentro de "JavaScript".
   *
   * @param text texto completo en el que se busca la entidad
   */
  def matches(text: String): Boolean =
    matchesDelimited(text, caseInsensitive = true)

  /**
   * Centraliza la regla de delimitación que comparten la entidad base y las
   * tecnologías. Es `protected` para que una subclase pueda reutilizarla sin
   * exponerla como parte de la API pública del modelo.
   */
  protected final def matchesDelimited(text: String, caseInsensitive: Boolean): Boolean = {
    // Pattern.quote trata el nombre como texto literal: caracteres de entidades
    // como "C++" no se interpretan accidentalmente como operadores de regex.
    val literalEntity = Pattern.quote(this.text)
    val regex = s"(?<![\\p{L}\\p{N}])$literalEntity(?![\\p{L}\\p{N}])"

    // UNICODE_CASE hace que el matching case-insensitive sea correcto también
    // para letras Unicode, coherente con el uso de \p{L} en los límites.
    val flags =
      if (caseInsensitive) Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
      else 0

    Pattern.compile(regex, flags).matcher(text).find()
  }

  /**
   * Por defecto una entidad es útil para el informe filtrado. Las ramas que
   * sean demasiado generales redefinen esta decisión polimórficamente.
   */
  def isRelevant: Boolean = true

  /**
   * Retorna una línea de descripción de la entidad para el informe.
   */
  def describe: String = s"[$entityType] $text"
}

class Person(text: String) extends NamedEntity(text) {
  def entityType: String = "Person"

  /**
   * Los nombres de personas requieren el nombre completo y respetan exactamente
   * sus mayúsculas/minúsculas; `contains` busca toda la cadena configurada.
   */
  override def matches(text: String): Boolean = text.contains(this.text)
}

class Organization(text: String) extends NamedEntity(text) {
  def entityType: String = "Organization"

  // Las organizaciones genéricas aparecen con demasiada frecuencia en los posts.
  override def isRelevant: Boolean = false
}

class University(text: String) extends Organization(text) {
  override def entityType: String = "University"

  // Una universidad sí aporta información, aunque Organization sea irrelevante.
  override def isRelevant: Boolean = true
}

class Place(text: String) extends NamedEntity(text) {
  def entityType: String = "Place"

  // Los lugares se excluyen únicamente del informe relevante, no de las estadísticas.
  override def isRelevant: Boolean = false
}

class Technology(text: String) extends NamedEntity(text) {
  def entityType: String = "Technology"

  /**
   * Las tecnologías conservan la regla de límites de palabra, pero distinguen
   * mayúsculas de minúsculas como pide el dominio ("Python" != "python").
   */
  override def matches(text: String): Boolean =
    matchesDelimited(text, caseInsensitive = false)

  // Una tecnología genérica no es suficientemente específica para el informe.
  override def isRelevant: Boolean = false
}

class ProgrammingLanguage(text: String) extends Technology(text) {
  override def entityType: String = "ProgrammingLanguage"

  // Los lenguajes recuperan relevancia aunque hereden de Technology.
  override def isRelevant: Boolean = true
}

/**
 * Rama abstracta para eventos. Al no implementar entityType, obliga a que cada
 * evento concreto declare su tipo y evita instanciar un evento inespecífico.
 */
abstract class Event(text: String) extends NamedEntity(text)

/** Conferencia académica; hereda el matching y la relevancia por defecto. */
class Conference(text: String) extends Event(text) {
  override def entityType: String = "Conference"
}
