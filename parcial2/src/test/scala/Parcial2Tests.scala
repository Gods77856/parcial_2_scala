/**
 * Pruebas autocontenidas del parcial.
 *
 * Se usan `assert` y un `main` en lugar de agregar una biblioteca de testing,
 * porque la consigna prohíbe sumar dependencias al build.sbt provisto.
 */
object Parcial2Tests {

  private var checksPassed = 0

  /** Ejecuta una verificación y conserva un mensaje útil si algo falla. */
  private def check(description: String)(condition: => Boolean): Unit = {
    assert(condition, s"Falló: $description")
    checksPassed += 1
    println(s"[OK] $description")
  }

  def main(args: Array[String]): Unit = {
    testBaseMatching()
    testSpecializedMatching()
    testRelevanceAndDetection()
    testDictionary()
    testFormattersAndStatistics()
    testProvidedFileIO()

    println(s"\nTodas las pruebas pasaron ($checksPassed verificaciones).")
  }

  /** Cubre límites, case-insensitivity, puntuación y caracteres de regex. */
  private def testBaseMatching(): Unit = {
    val java = new Conference("Java")
    val cpp = new Conference("C++")

    check("la entidad base acepta una palabra aislada") {
      java.matches("Java is great")
    }
    check("la entidad base ignora mayúsculas y minúsculas") {
      java.matches("I prefer java for this example")
    }
    check("la entidad base no acepta una entidad dentro de una palabra mayor") {
      !java.matches("I use JavaScript")
    }
    check("los paréntesis son límites válidos") {
      java.matches("(Java) is verbose")
    }
    check("Pattern.quote permite buscar C++ literalmente") {
      cpp.matches("I love C++ programming")
    }
    check("un dígito adyacente impide una coincidencia parcial") {
      !java.matches("Java17")
    }
  }

  /** Comprueba que el despacho dinámico elija el override de cada rama. */
  private def testSpecializedMatching(): Unit = {
    val python: NamedEntity = new Technology("Python")
    val person: NamedEntity = new Person("Martin Odersky")

    check("Technology acepta palabra completa con capitalización exacta") {
      python.matches("Python is great")
    }
    check("Technology distingue mayúsculas y minúsculas") {
      !python.matches("I love python scripting")
    }
    check("Technology tampoco acepta una palabra más larga") {
      !python.matches("Pythonista")
    }
    check("Person acepta el nombre completo con capitalización exacta") {
      person.matches("Martin Odersky visited EPFL")
    }
    check("Person rechaza el mismo nombre con otra capitalización") {
      !person.matches("martin odersky visited EPFL")
    }
  }

  /** Verifica relevancia heredada y la diferencia entre ambas detecciones. */
  private def testRelevanceAndDetection(): Unit = {
    val dictionary: List[NamedEntity] = List(
      new Person("Martin Odersky"),
      new Organization("Google"),
      new University("MIT"),
      new Place("San Francisco"),
      new Technology("JVM"),
      new ProgrammingLanguage("Scala"),
      new Conference("ICFP")
    )
    val text = "Martin Odersky took Scala and JVM to ICFP at MIT after Google in San Francisco"

    check("las relevancias concretas respetan los overrides de la jerarquía") {
      dictionary.map(_.isRelevant) == List(true, false, true, false, false, true, true)
    }
    check("detectEntities conserva todas las entidades coincidentes") {
      Analyzer.detectEntities(text, dictionary).map(_.text) == dictionary.map(_.text)
    }
    check("detectRelevant combina coincidencia y relevancia") {
      Analyzer.detectRelevant(text, dictionary).map(_.text) ==
        List("Martin Odersky", "MIT", "Scala", "ICFP")
    }
    check("detectEntities elimina el falso positivo Java dentro de JavaScript") {
      Analyzer.detectEntities(
        "I use JavaScript",
        List(new ProgrammingLanguage("Java"), new ProgrammingLanguage("JavaScript"))
      ).map(_.text) == List("JavaScript")
    }
  }

  /** Asegura que la nueva rama esté conectada a la fábrica y a loadAll. */
  private def testDictionary(): Unit = {
    val conferences = Dictionary.loadFromFile("data/conferences.txt", "Conference")
    val all = Dictionary.loadAll()

    check("el diccionario de conferencias contiene al menos cinco entradas") {
      conferences.size >= 5
    }
    check("todas las entradas del archivo crean Conference") {
      conferences.forall(_.isInstanceOf[Conference])
    }
    check("loadAll incorpora exactamente las conferencias del nuevo archivo") {
      all.count(_.entityType == "Conference") == conferences.size
    }
    check("loadAll suma las cinco conferencias a las 65 entidades originales") {
      all.size == 70
    }
  }

  /** Comprueba el contrato textual exacto y los dos niveles de ordenamiento. */
  private def testFormattersAndStatistics(): Unit = {
    val entities: List[NamedEntity] = List(
      new ProgrammingLanguage("Scala"),
      new Person("Zoe"),
      new Conference("ICFP"),
      new Person("Ada")
    )
    val expected =
      """Post: "Ejemplo"
        |Entidades detectadas:
        |  Conference (1):
        |    ICFP
        |  Person (2):
        |    Ada
        |    Zoe
        |  ProgrammingLanguage (1):
        |    Scala""".stripMargin

    check("el formato agrupado ordena tipos y textos alfabéticamente") {
      Formatters.formatGroupedNERResult("Ejemplo", entities) == expected
    }
    check("el formato agrupado vacío coincide con el formato original") {
      Formatters.formatGroupedNERResult("Vacío", Nil) ==
        Formatters.formatNERResult("Vacío", Nil)
    }

    val counts = Analyzer.countByType(entities)
    check("countByType cuenta cada tipo dinámico") {
      counts == Map("ProgrammingLanguage" -> 1, "Person" -> 2, "Conference" -> 1)
    }
    check("formatEntityStats ubica primero el tipo con mayor cantidad") {
      Formatters.formatEntityStats(counts).linesIterator.drop(1).next() == "Person: 2"
    }
  }

  /** Incluye una prueba de regresión mínima para el componente provisto de E/S. */
  private def testProvidedFileIO(): Unit = {
    val json = """{"data":{"children":[{"data":{"title":"Uno"}},{"data":{"title":"Dos"}}]}}"""

    check("FileIO extrae todos los títulos JSON en orden") {
      FileIO.extractPostTitles(json) == List("Uno", "Dos")
    }
    check("FileIO ignora el comentario del diccionario de conferencias") {
      FileIO.readLines("data/conferences.txt").head == "ICFP"
    }
  }
}
