/**
 * Responsable de detectar entidades nombradas en texto libre y
 * producir estadísticas sobre ellas.
 */
object Analyzer {

  /**
   * Detecta las entidades del diccionario que aparecen en el texto dado.
   *
   * @param text       texto a analizar (ej: título o cuerpo de un post)
   * @param dictionary lista de entidades conocidas (cargadas desde los diccionarios)
   * @return lista de entidades cuyo texto aparece en el texto analizado
   */
  def detectEntities(text: String, dictionary: List[NamedEntity]): List[NamedEntity] = {
    // El Analyzer delega el criterio de coincidencia al objeto concreto. Este
    // despacho dinámico permite que Person, Technology, etc. apliquen sus reglas.
    dictionary.filter(entity => entity.matches(text))
  }

  /**
   * Detecta únicamente entidades útiles para el informe de un post.
   *
   * Este método coexiste con detectEntities porque el informe visible se filtra,
   * mientras que las estadísticas globales todavía necesitan todas las entidades.
   *
   * @param text       texto a analizar
   * @param dictionary lista heterogénea de entidades conocidas
   * @return entidades relevantes que además coinciden con el texto
   */
  def detectRelevant(text: String, dictionary: List[NamedEntity]): List[NamedEntity] = {
    // `&&` expresa las dos restricciones y evita evaluar matches para objetos que
    // ya fueron descartados por relevancia.
    dictionary.filter(entity => entity.isRelevant && entity.matches(text))
  }

  /**
   * Cuenta cuántas entidades de cada tipo fueron detectadas.
   *
   * @param entities lista de entidades detectadas
   * @return mapa de entityType → cantidad de apariciones
   */
  def countByType(entities: List[NamedEntity]): Map[String, Int] = {
    entities.groupBy(_.entityType).view.mapValues(_.size).toMap
  }
}
