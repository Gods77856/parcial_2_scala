object Main {
  def main(args: Array[String]): Unit = {

    val dictionary: List[NamedEntity] = Dictionary.loadAll()
    println(s"Diccionario cargado: ${dictionary.size} entidades.\n")

    val subscriptions = FileIO.readSubscriptions()

    val allDetected: List[NamedEntity] = subscriptions.flatMap { url =>
      println(s"Descargando posts de: $url")
      val json   = FileIO.downloadFeed(url)
      val titles = FileIO.extractPostTitles(json)
      println(s"\n${"=" * 60}\n$url\n${"=" * 60}")
      titles.flatMap { title =>
        val entities = Analyzer.detectEntities(title, dictionary)
        println(Formatters.formatNERResult(title, entities))
        entities

        /* EJERCICIO 3.b - SOLUCION A IMPLEMENTAR
         * Reemplazar las tres lineas anteriores por estas tres. El informe muestra
         * solo entidades relevantes, pero flatMap retorna todas para las estadisticas.
         *
         * val relevant = Analyzer.detectRelevant(title, dictionary)
         * println(Formatters.formatGroupedNERResult(title, relevant))
         * Analyzer.detectEntities(title, dictionary)
         */
      }
    }

    println(s"\n${Formatters.formatEntityStats(Analyzer.countByType(allDetected))}")
  }
}
