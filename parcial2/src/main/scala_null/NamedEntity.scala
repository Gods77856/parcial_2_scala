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

  /* EJERCICIO 1.a - SOLUCION A IMPLEMENTAR
   * Agregar arriba del archivo: import java.util.regex.Pattern
   * Este metodo busca la entidad completa, ignora mayusculas y evita que
   * "Java" coincida dentro de "JavaScript". Pattern.quote permite buscar C++.
   *
   * def matches(text: String): Boolean = {
   *   val regex = s"(?<![\\p{L}\\p{N}])${Pattern.quote(this.text)}(?![\\p{L}\\p{N}])"
   *   Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find()
   * }
   */

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Todas las entidades son relevantes salvo que una subclase indique lo contrario.
   * def isRelevant: Boolean = true
   */

  /**
   * Retorna una línea de descripción de la entidad para el informe.
   */
  def describe: String = s"[$entityType] $text"
}

class Person(text: String) extends NamedEntity(text) {
  def entityType: String = "Person"

  /* EJERCICIO 1.c - SOLUCION A IMPLEMENTAR
   * Busca el nombre completo respetando mayusculas y minusculas.
   * override def matches(text: String): Boolean = text.contains(this.text)
   */
}

class Organization(text: String) extends NamedEntity(text) {
  def entityType: String = "Organization"

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Las organizaciones genericas no son relevantes.
   * override def isRelevant: Boolean = false
   */
}

class University(text: String) extends Organization(text) {
  override def entityType: String = "University"

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Una universidad vuelve a ser relevante aunque su padre no lo sea.
   * override def isRelevant: Boolean = true
   */
}

class Place(text: String) extends NamedEntity(text) {
  def entityType: String = "Place"

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Los lugares no son relevantes para el informe.
   * override def isRelevant: Boolean = false
   */
}

class Technology(text: String) extends NamedEntity(text) {
  def entityType: String = "Technology"

  /* EJERCICIO 1.b - SOLUCION A IMPLEMENTAR
   * Usa la misma regla de palabra completa, pero distingue mayusculas.
   * override def matches(text: String): Boolean = {
   *   val regex = s"(?<![\\p{L}\\p{N}])${Pattern.quote(this.text)}(?![\\p{L}\\p{N}])"
   *   Pattern.compile(regex).matcher(text).find()
   * }
   */

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Las tecnologias genericas no son relevantes.
   * override def isRelevant: Boolean = false
   */
}

class ProgrammingLanguage(text: String) extends Technology(text) {
  override def entityType: String = "ProgrammingLanguage"

  /* EJERCICIO 2.a - SOLUCION A IMPLEMENTAR
   * Un lenguaje vuelve a ser relevante aunque Technology no lo sea.
   * override def isRelevant: Boolean = true
   */
}

/* EJERCICIO 2.b - SOLUCION A IMPLEMENTAR
 * Agregar la rama de eventos. Event queda abstracta porque no define entityType;
 * Conference hereda matches e isRelevant de NamedEntity.
 *
 * abstract class Event(text: String) extends NamedEntity(text)
 *
 * class Conference(text: String) extends Event(text) {
 *   def entityType: String = "Conference"
 * }
 */
