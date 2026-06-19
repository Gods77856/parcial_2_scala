/**
 * Responsable de convertir los resultados del análisis a texto para mostrar.
 */
object Formatters {

  /**
   * Formatea el análisis NER de un post individual.
   *
   * @param postTitle título del post analizado
   * @param entities  entidades detectadas en ese post
   * @return bloque de texto con el título y las entidades encontradas
   */
  def formatNERResult(postTitle: String, entities: List[NamedEntity]): String = {
    val header = s"""Post: "$postTitle"\nEntidades detectadas:"""

    /* EJERCICIO 3.a - AJUSTE A IMPLEMENTAR
     * Para que el caso vacio tenga un salto real de linea, reemplazar el header
     * anterior por este string escrito en dos lineas:
     *
     * val header = s"""Post: "$postTitle"
     * Entidades detectadas:"""
     */

    val body =
      if (entities.isEmpty) "  (sin entidades detectadas)"
      else entities.map(e => s"  ${e.describe}").mkString("\n")
    s"$header\n$body"
  }

  /* EJERCICIO 3.a - SOLUCION A IMPLEMENTAR
   * Agregar este metodo. Si la lista esta vacia reutiliza el formato original.
   * En caso contrario agrupa por tipo, ordena los tipos y ordena cada grupo por text.
   *
   * def formatGroupedNERResult(postTitle: String, entities: List[NamedEntity]): String = {
   *   if (entities.isEmpty) return formatNERResult(postTitle, entities)
   *
   *   val groups = entities.groupBy(_.entityType).toList.sortBy(_._1).map {
   *     case (entityType, values) =>
   *       val names = values.sortBy(_.text).map(e => s"    ${e.text}").mkString("\n")
   *       s"  $entityType (${values.size}):\n$names"
   *   }.mkString("\n")
   *
   *   s"""Post: "$postTitle"
   * Entidades detectadas:
   * $groups"""
   * }
   */

  /**
   * Formatea un resumen de estadísticas de entidades por tipo.
   *
   * @param counts mapa de entityType → cantidad
   * @return texto con las estadísticas ordenadas por cantidad (de mayor a menor)
   */
  def formatEntityStats(counts: Map[String, Int]): String = {
    val lines = counts.toList
      .sortBy(-_._2)
      .map { case (entityType, count) => s"$entityType: $count" }
    ("=== Estadísticas de entidades ===" :: lines).mkString("\n")
  }
}
