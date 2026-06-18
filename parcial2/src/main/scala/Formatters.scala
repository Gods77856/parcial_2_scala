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
    // La línea física dentro del string multilínea produce el salto esperado;
    // en un string triple, la secuencia escrita `\n` se conservaría literalmente.
    val header = s"""Post: "$postTitle"
Entidades detectadas:"""
    val body =
      if (entities.isEmpty) "  (sin entidades detectadas)"
      else entities.map(e => s"  ${e.describe}").mkString("\n")
    s"$header\n$body"
  }

  /**
   * Formatea las entidades de un post agrupadas por su tipo dinámico.
   *
   * @param postTitle título del post analizado
   * @param entities  entidades relevantes detectadas
   * @return bloque determinista, ordenado por tipo y luego por texto
   */
  def formatGroupedNERResult(postTitle: String, entities: List[NamedEntity]): String = {
    // Se conserva exactamente el encabezado público del formateador original.
    val header = s"""Post: "$postTitle"
Entidades detectadas:"""

    val body =
      if (entities.isEmpty) {
        // El caso vacío usa el mismo mensaje e indentación que formatNERResult.
        "  (sin entidades detectadas)"
      } else {
        // groupBy construye un grupo por entityType; convertir a List permite
        // imponer un orden estable en vez de depender del orden interno de Map.
        entities.groupBy(_.entityType).toList
          .sortBy { case (entityType, _) => entityType }
          .map { case (entityType, groupedEntities) =>
            // El orden alfabético interno hace que la salida sea legible y testeable.
            val entityLines = groupedEntities
              .sortBy(_.text)
              .map(entity => s"    ${entity.text}")
              .mkString("\n")

            s"  $entityType (${groupedEntities.size}):\n$entityLines"
          }
          .mkString("\n")
      }

    s"$header\n$body"
  }

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
