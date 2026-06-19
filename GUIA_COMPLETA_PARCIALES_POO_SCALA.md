# Guía completa para parciales de POO en Scala

Esta guía toma como base el Laboratorio 2 de reconocimiento de entidades nombradas (NER), el material de Programación Orientada a Objetos en Scala y el parcial asociado. Su objetivo es que puedas resolver variantes del mismo tipo de examen cambiando nombres, reglas y tipos concretos, sin depender de memorizar una única solución.

## 1. Qué evalúa realmente este tipo de parcial

Aunque el dominio hable de Reddit, entidades, conferencias o estadísticas, los conocimientos centrales son:

1. Modelar un dominio con una clase abstracta y subclases.
2. Heredar comportamiento común y redefinir solo las excepciones.
3. Usar polimorfismo: llamar al mismo método sobre una lista heterogénea.
4. Encapsular una regla donde se encuentran los datos que necesita.
5. Separar modelo, carga de datos, análisis, presentación e integración.
6. Transformar colecciones con `map`, `filter`, `groupBy`, `sortBy` y `flatMap`.
7. Respetar un contrato de salida textual exacto.
8. Extender un programa existente con cambios mínimos.

La estructura típica del proyecto separa responsabilidades así:

| Archivo | Responsabilidad | Pregunta que responde |
|---|---|---|
| `NamedEntity.scala` | Modelo y comportamiento de cada tipo | ¿Qué es la entidad y cómo se comporta? |
| `Dictionary.scala` | Construcción desde archivos | ¿Cómo convierto una línea en un objeto? |
| `Analyzer.scala` | Selección y agregación | ¿Qué entidades cumplen una condición? |
| `Formatters.scala` | Presentación | ¿Cómo convierto resultados en texto? |
| `FileIO.scala` | Entrada/salida | ¿Cómo leo archivos, JSON o URLs? |
| `Main.scala` | Orquestación | ¿En qué orden colaboran los componentes? |

Regla práctica: si una modificación cambia **cómo se comporta un tipo particular**, normalmente pertenece a la clase de ese tipo. Si cambia **cómo se recorre una colección**, suele pertenecer al analizador. Si cambia **cómo se imprime**, pertenece al formateador.

## 2. Leer la consigna antes de escribir código

Antes de tocar el esqueleto, construí una tabla de requisitos:

| Requisito | Archivo esperado | Tipo de cambio |
|---|---|---|
| Nuevo comportamiento común | Clase base | Método concreto |
| Excepción de una rama | Subclase | `override` |
| Nuevo concepto abstracto | Modelo | Clase abstracta |
| Nuevo tipo instanciable | Modelo | Clase concreta |
| Nuevo archivo de datos | `data/` y fábrica | Archivo + caso de construcción |
| Nuevo filtro | Analizador | Método sobre la colección |
| Nuevo formato | Formateador | Agrupar, ordenar y renderizar |
| Cambio del flujo | `Main` | Integrar sin perder información |

Después marcá las restricciones:

- Archivos que no se pueden modificar.
- Dependencias que no se pueden agregar.
- Métodos que deben coexistir en vez de reemplazarse.
- Reglas heredadas y excepciones explícitas.
- Orden e indentación exactos de la salida.
- Datos usados para el resultado visible y datos usados para estadísticas.

Una frase como “`detectRelevant` no reemplaza a `detectEntities`” es una señal fuerte: hay dos necesidades distintas y probablemente `Main` necesite conservar dos resultados.

## 3. Fundamentos de POO aplicados

### 3.1 Clase abstracta

Una clase abstracta representa un concepto válido para organizar el modelo, pero demasiado general para instanciarlo directamente.

```scala
abstract class Entity(val text: String) {
  // Método abstracto: cada subtipo concreto debe decidir su valor.
  def entityType: String

  // Método concreto: todas las subclases reciben esta implementación.
  def describe: String = s"[$entityType] $text"
}
```

`describe` funciona para todas las subclases porque invoca `entityType` polimórficamente. Cuando el objeto real es una `University`, el despacho dinámico selecciona el `entityType` de `University`.

### 3.2 Clase concreta, constructor y atributo

```scala
class Person(text: String) extends Entity(text) {
  override def entityType: String = "Person"
}
```

El parámetro `text` se pasa al constructor padre. El padre lo declaró `val`, por lo que queda accesible como atributo público e inmutable.

### 3.3 Herencia en varios niveles

```text
NamedEntity
├── Person
├── Organization
│   └── University
├── Place
├── Technology
│   └── ProgrammingLanguage
└── Event
    └── Conference
```

Una `University` es también una `Organization` y una `NamedEntity`. Por eso puede almacenarse en una `List[NamedEntity]` sin conversiones.

Usá herencia cuando exista una relación conceptual “es-un”. Si dos clases solo comparten accidentalmente una función auxiliar, eso no alcanza para crear una relación padre-hijo.

### 3.4 Método abstracto frente a método concreto

- `def entityType: String` no tiene cuerpo: es abstracto.
- `def isRelevant: Boolean = true` tiene cuerpo: es concreto y heredable.
- Una subclase concreta debe implementar los abstractos pendientes.
- Una subclase redefine un método heredado con `override`.

Plantilla:

```scala
abstract class Base(val value: String) {
  def requiredDecision: String                  // abstracto
  def defaultDecision: Boolean = true           // concreto
}

class Special(value: String) extends Base(value) {
  override def requiredDecision: String = "Special"
  override def defaultDecision: Boolean = false
}
```

### 3.5 Polimorfismo y despacho dinámico

```scala
val entities: List[NamedEntity] = List(
  new Person("Ada Lovelace"),
  new University("MIT"),
  new ProgrammingLanguage("Scala")
)

val descriptions = entities.map(_.describe)
```

El tipo estático de cada elemento es `NamedEntity`, pero el objeto conserva su tipo dinámico. El método redefinido se elige en ejecución.

Evitá este antipatrón:

```scala
// Mala señal cuando el comportamiento podría vivir en las clases.
entity match {
  case _: Person     => /* regla A */
  case _: Technology => /* regla B */
}
```

Preferí:

```scala
entity.matches(text)
```

Así, agregar una nueva subclase no obliga a reescribir el analizador.

### 3.6 Objetos singleton

`object` crea una única instancia y cumple el papel habitual de los métodos estáticos:

```scala
object Analyzer {
  def detect(text: String, entities: List[Entity]): List[Entity] =
    entities.filter(_.matches(text))
}
```

Se usa como `Analyzer.detect(...)`, sin `new`.

### 3.7 Traits: cuándo podrían aparecer en una variante

Un `trait` comparte un contrato o comportamiento entre clases que no necesariamente forman una única cadena de herencia.

```scala
trait Reportable {
  def label: String
  def reportLine: String = s"- $label"
}

class Conference(val label: String)
  extends Event(label)
  with Reportable
```

Usá una clase abstracta cuando necesitás constructor o una identidad padre clara. Usá un trait para una capacidad transversal que varias ramas pueden mezclar.

## 4. Plantilla central: comportamiento por defecto y excepciones

Muchos parciales tienen esta forma:

- La clase base define una regla general.
- Algunas ramas la redefinen.
- Una subclase puede volver a redefinir la decisión del padre.

```scala
abstract class Item(val name: String) {
  def category: String

  // Elección por defecto para todos los tipos no excepcionales.
  def isUseful: Boolean = true
}

class GenericItem(name: String) extends Item(name) {
  override def category: String = "GenericItem"
  override def isUseful: Boolean = false
}

class UsefulSpecialization(name: String) extends GenericItem(name) {
  override def category: String = "UsefulSpecialization"

  // Este override gana sobre el valor heredado de GenericItem.
  override def isUseful: Boolean = true
}
```

La resolución de `isUseful` depende del objeto real:

| Objeto | Implementación elegida |
|---|---|
| `Item` de una subclase sin override | `Item.isUseful` |
| `GenericItem` | `GenericItem.isUseful` |
| `UsefulSpecialization` | `UsefulSpecialization.isUseful` |

Este patrón permite que el analizador permanezca genérico:

```scala
items.filter(item => item.isUseful && item.matches(text))
```

## 5. Matching de texto correctamente

### 5.1 Por qué `contains` uniforme falla

```scala
text.toLowerCase.contains(entity.text.toLowerCase)
```

Problemas:

- `Java` coincide dentro de `JavaScript`.
- Todas las clases reciben la misma sensibilidad a mayúsculas.
- Caracteres como `C++` complican una regex ingenua.
- La lógica queda fuera del objeto que debería decidirla.

La mejora orientada a objetos es declarar `matches` en la clase base y redefinirlo donde cambie la regla.

### 5.2 Límite de letras o dígitos

La condición “la entidad no debe ser parte de una secuencia más larga de letras o dígitos” se expresa con lookarounds:

```regex
(?<![\p{L}\p{N}])ENTIDAD(?![\p{L}\p{N}])
```

- `\p{L}` representa cualquier letra Unicode.
- `\p{N}` representa cualquier número Unicode.
- `(?<!...)` exige que antes no haya letra ni número.
- `(?!...)` exige que después no haya letra ni número.
- Los lookarounds verifican contexto sin consumir caracteres.

No conviene usar simplemente `\b`. El límite `\b` se basa en la idea de carácter de palabra y puede producir resultados poco intuitivos con nombres que incluyen puntuación, como `C++`, `C#` o `.NET`.

### 5.3 Escapar el texto del diccionario

Nunca insertes directamente una entidad externa en una regex:

```scala
val unsafe = s"...${entityText}..."
```

`+`, `.`, `(`, `[`, `?` y otros caracteres tienen significado especial. La plantilla segura es:

```scala
import java.util.regex.Pattern

val literal = Pattern.quote(entityText)
val regex = s"(?<![\\p{L}\\p{N}])$literal(?![\\p{L}\\p{N}])"
```

Notá el doble `\\` dentro del `String` de Scala: el primer nivel es el escape del lenguaje y el segundo llega al motor de regex.

### 5.4 Case-insensitive sin perder Unicode

```scala
val flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
val found = Pattern.compile(regex, flags).matcher(textToSearch).find()
```

Para una variante case-sensitive, usá `0` como conjunto de flags.

### 5.5 Plantilla reutilizable de matching delimitado

```scala
abstract class Entity(val text: String) {
  def entityType: String

  def matches(candidate: String): Boolean =
    matchesDelimited(candidate, caseInsensitive = true)

  protected final def matchesDelimited(
    candidate: String,
    caseInsensitive: Boolean
  ): Boolean = {
    val literal = Pattern.quote(this.text)
    val regex = s"(?<![\\p{L}\\p{N}])$literal(?![\\p{L}\\p{N}])"
    val flags =
      if (caseInsensitive) Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
      else 0

    Pattern.compile(regex, flags).matcher(candidate).find()
  }
}
```

Una subclase reutiliza el mecanismo, pero cambia la política:

```scala
class Technology(text: String) extends Entity(text) {
  override def entityType: String = "Technology"

  // Mismos límites, pero coincidencia case-sensitive.
  override def matches(candidate: String): Boolean =
    matchesDelimited(candidate, caseInsensitive = false)
}
```

Otra subclase puede necesitar una regla completamente diferente:

```scala
class Person(text: String) extends Entity(text) {
  override def entityType: String = "Person"

  // Busca la frase completa y respeta la capitalización.
  override def matches(candidate: String): Boolean =
    candidate.contains(this.text)
}
```

Usar `this.text` evita confundir el atributo de la entidad con el parámetro del método.

### 5.6 Matriz mínima de pruebas para matching

| Entidad | Texto | Resultado esperado | Motivo |
|---|---|---:|---|
| `Java` | `Java is great` | `true` | Aislada |
| `Java` | `I use JavaScript` | `false` | Letra adyacente |
| `Java` | `(Java) is verbose` | `true` | Puntuación válida |
| `Java` | `java is useful` | depende del tipo | Verifica case policy |
| `C++` | `I love C++ programming` | `true` | Regex escapada |
| `Java` | `Java17` | `false` | Dígito adyacente |
| nombre completo | distinta capitalización | `false` si es sensible | Override especial |

## 6. Extender una jerarquía sin romperla

Si la consigna pide una rama abstracta y un tipo concreto:

```scala
abstract class Event(text: String) extends NamedEntity(text)

class Conference(text: String) extends Event(text) {
  override def entityType: String = "Conference"
}
```

`Event` puede dejar `entityType` sin implementar porque también es abstracta. `Conference`, al ser concreta, debe resolver todos los métodos abstractos pendientes.

Si no se pide redefinir `matches` ni `isRelevant`, no los redefinas: la herencia ya expresa que se conserva el comportamiento base. Agregar overrides redundantes aumenta el código y puede introducir diferencias accidentales.

Checklist al agregar un tipo cargado desde datos:

1. Crear la clase en el lugar correcto de la jerarquía.
2. Implementar los métodos abstractos pendientes.
3. Decidir qué métodos concretos hereda y cuáles redefine.
4. Agregar el caso a la fábrica del diccionario.
5. Crear el archivo de datos.
6. Agregar ese archivo a `loadAll`.
7. Probar tipo, cantidad, matching y relevancia.

Olvidar los pasos 4 o 6 es uno de los fallos más comunes: la clase compila, pero nunca se crean objetos de ese tipo durante la ejecución real.

## 7. Carga de diccionarios y patrón fábrica

Una función de carga suele combinar E/S con una pequeña fábrica:

```scala
def loadFromFile(path: String, entityType: String): List[NamedEntity] = {
  FileIO.readLines(path).map { name =>
    entityType match {
      case "Person"     => new Person(name)
      case "University" => new University(name)
      case "Conference" => new Conference(name)
    }
  }
}
```

Acá el `match` es razonable: no está reemplazando comportamiento polimórfico, sino traduciendo un identificador externo (`String`) a un constructor concreto.

Para combinar diccionarios:

```scala
def loadAll(): List[NamedEntity] =
  loadFromFile("data/people.txt", "Person") :::
  loadFromFile("data/universities.txt", "University") :::
  loadFromFile("data/conferences.txt", "Conference")
```

`:::` concatena listas. Una alternativa general es construir una lista de configuraciones y usar `flatMap`:

```scala
val sources = List(
  ("data/people.txt", "Person"),
  ("data/universities.txt", "University"),
  ("data/conferences.txt", "Conference")
)

sources.flatMap { case (path, entityType) =>
  loadFromFile(path, entityType)
}
```

En un parcial con esqueleto fijo, preferí el cambio mínimo que siga el estilo existente.

## 8. Transformaciones de colecciones que hay que dominar

### 8.1 `map`: uno a uno

```scala
val names: List[String] = entities.map(_.text)
```

Cada entrada produce exactamente una salida.

### 8.2 `filter`: conservar elementos

```scala
val relevant = entities.filter(_.isRelevant)
```

El orden original se conserva.

### 8.3 Combinar condiciones

```scala
entities.filter(entity => entity.isRelevant && entity.matches(text))
```

`&&` tiene cortocircuito: si `isRelevant` es `false`, no se evalúa `matches`.

### 8.4 `flatMap`: uno a muchos y aplanar

```scala
val allDetected: List[NamedEntity] = titles.flatMap { title =>
  detectEntities(title, dictionary)
}
```

Cada título produce una lista; `flatMap` concatena todas en una sola. Con `map` el resultado sería `List[List[NamedEntity]]`.

### 8.5 `groupBy`: construir grupos

```scala
val byType: Map[String, List[NamedEntity]] =
  entities.groupBy(_.entityType)
```

La clave se calcula para cada objeto mediante polimorfismo.

### 8.6 Contar grupos

```scala
val counts: Map[String, Int] =
  entities.groupBy(_.entityType).view.mapValues(_.size).toMap
```

En Scala 2.13, `mapValues` produce una vista; `toMap` materializa el resultado.

Otra plantilla fácil de razonar es:

```scala
entities
  .groupBy(_.entityType)
  .map { case (entityType, group) => entityType -> group.size }
```

### 8.7 Ordenar

```scala
entities.sortBy(_.text)                 // ascendente por texto
counts.toList.sortBy(-_._2)             // descendente por cantidad
counts.toList.sortBy { case (k, n) => (-n, k) } // desempate estable
```

Un `Map` no debe usarse como fuente de orden de presentación. Convertí a `List` y ordená explícitamente.

## 9. Detección general y detección relevante

La plantilla polimórfica general es mínima:

```scala
def detectEntities(
  text: String,
  dictionary: List[NamedEntity]
): List[NamedEntity] =
  dictionary.filter(_.matches(text))
```

Como se filtra el diccionario, cada objeto del diccionario aparece como máximo una vez aunque el texto lo mencione varias veces. Esto satisface el requisito típico “detectar una entidad una sola vez”.

La variante relevante agrega una condición, no reemplaza la primera:

```scala
def detectRelevant(
  text: String,
  dictionary: List[NamedEntity]
): List[NamedEntity] =
  dictionary.filter(entity => entity.isRelevant && entity.matches(text))
```

No copies las reglas concretas al analizador:

```scala
// Evitar: el analizador empezaría a conocer todas las subclases.
entity match {
  case _: Place        => false
  case _: Organization => false
  case _               => entity.matches(text)
}
```

## 10. Formatear agrupando y ordenando

El formateador suele exigir:

1. Encabezado fijo.
2. Caso vacío especial.
3. Agrupación por tipo.
4. Grupos ordenados por tipo.
5. Entidades ordenadas por texto dentro de cada grupo.
6. Cantidad en el encabezado del grupo.
7. Indentación exacta.

Plantilla adaptable:

```scala
def formatGrouped(title: String, entities: List[NamedEntity]): String = {
  val header = s"""Post: "$title"
Entidades detectadas:"""

  val body =
    if (entities.isEmpty) {
      "  (sin entidades detectadas)"
    } else {
      entities
        .groupBy(_.entityType)
        .toList
        .sortBy { case (entityType, _) => entityType }
        .map { case (entityType, group) =>
          val lines = group
            .sortBy(_.text)
            .map(entity => s"    ${entity.text}")
            .mkString("\n")

          s"  $entityType (${group.size}):\n$lines"
        }
        .mkString("\n")
    }

  s"$header\n$body"
}
```

### Cuidado con strings triples

En un string normal, `\n` representa un salto:

```scala
val text = "línea 1\nlínea 2"
```

Los strings triples son literales; la forma más clara de incluir el salto es escribirlo físicamente:

```scala
val text = """línea 1
línea 2"""
```

Para resultados multilínea complejos en pruebas, `stripMargin` facilita ver la indentación:

```scala
val expected =
  """Post: "Ejemplo"
    |Entidades detectadas:
    |  Person (1):
    |    Ada""".stripMargin
```

## 11. Integración sin perder datos

Una variante frecuente pide filtrar la salida visible, pero mantener estadísticas sobre el total. Hay que separar ambos flujos:

```scala
val allDetected = titles.flatMap { title =>
  // Resultado reducido para presentar.
  val visible = Analyzer.detectRelevant(title, dictionary)
  println(Formatters.formatGroupedNERResult(title, visible))

  // Resultado completo que flatMap acumula para las estadísticas.
  Analyzer.detectEntities(title, dictionary)
}

val counts = Analyzer.countByType(allDetected)
println(Formatters.formatEntityStats(counts))
```

El último valor del bloque es el que devuelve la función pasada a `flatMap`. Si el último valor fuera `visible`, las estadísticas quedarían filtradas por accidente.

Pensalo como dos consumidores:

| Consumidor | Datos correctos |
|---|---|
| Informe de cada post | Entidades relevantes |
| Estadísticas globales | Todas las entidades detectadas |

## 12. Cómo probar sin agregar dependencias

Si el `build.sbt` no incluye ScalaTest y la consigna prohíbe nuevas librerías, podés crear un objeto de pruebas con `assert`:

```scala
object ExamTests {
  def main(args: Array[String]): Unit = {
    val java = new ProgrammingLanguage("Java")

    assert(java.matches("Java is great"))
    assert(!java.matches("I use JavaScript"))

    println("Pruebas correctas")
  }
}
```

Ubicalo en `src/test/scala/ExamTests.scala` y ejecutá:

```bash
sbt -batch "Test / compile" "Test / runMain ExamTests"
```

### Capas de verificación recomendadas

1. **Compilación limpia**

   ```bash
   sbt -batch clean compile
   ```

2. **Pruebas de unidad**

   - Cada caso de matching.
   - Cada valor de relevancia.
   - Herencia que vuelve a redefinir una propiedad.
   - Fábrica y carga total.
   - Detección general frente a relevante.
   - Orden exacto del formateador.
   - Caso vacío.
   - Conteos.

3. **Prueba de integración**

   En una terminal:

   ```bash
   cd reddit-mock
   sbt run
   ```

   En otra, desde el proyecto principal:

   ```bash
   sbt run
   ```

4. **Revisión manual de salida**

   - No aparece `Java` dentro de `JavaScript`.
   - Los grupos están ordenados.
   - Los tipos irrelevantes no se muestran por post.
   - Esos tipos sí pueden aparecer en estadísticas.
   - No se imprimen secuencias literales `\n`.

## 13. Estrategia para resolver un parcial parecido

### Paso 1: verificar el estado inicial

```bash
git status --short
sbt -batch compile
```

Si el esqueleto no compila antes de tus cambios, registrá el error. No asumas que todo fallo posterior lo introdujiste vos.

### Paso 2: localizar huecos y restricciones

```bash
rg -n "TODO|???|NotImplementedError" .
```

Leé los archivos completos: un método puede estar implementado, pero la nueva consigna exige cambiar su colaboración con otros objetos.

### Paso 3: modificar primero el modelo

Agregá métodos base, overrides y clases. Compilá. Esto establece la API que usarán analizador, fábrica y formateador.

### Paso 4: conectar la construcción

Agregá el nuevo tipo a la fábrica y a `loadAll`. Probá la cantidad y el tipo dinámico de las instancias.

### Paso 5: implementar transformaciones puras

El analizador y el formateador son fáciles de probar sin red. Terminá esos métodos antes de modificar `Main`.

### Paso 6: integrar al final

Conservá claramente qué lista se imprime y cuál se acumula.

### Paso 7: probar casos normales y bordes

No alcanza con el ejemplo feliz de la consigna. Probá mayúsculas, subcadenas, puntuación, vacío, empate, tipo heredado y caracteres especiales.

### Paso 8: revisar el diff

```bash
git diff --check
git diff
git status --short
```

Confirmá que:

- No agregaste dependencias.
- No modificaste archivos declarados como provistos.
- No eliminaste métodos que debían coexistir.
- No incluiste `target/`, metadatos del IDE ni archivos `._*`.
- Cada requisito tiene una prueba o una verificación manual.

## 14. Errores comunes y cómo diagnosticarlos

### “La clase nueva existe, pero nunca aparece”

Revisá el caso de la fábrica, el archivo de datos y la llamada desde `loadAll`.

### “University es irrelevante aunque debía ser relevante”

Probablemente heredó `false` de `Organization` y faltó un segundo `override` en `University`.

### “Python en minúscula sigue coincidiendo”

Verificá que `Technology.matches` quite los flags case-insensitive y que `ProgrammingLanguage` realmente herede ese método.

### “Java aparece dentro de JavaScript”

El matching todavía usa `contains` o solo controla uno de los límites.

### “C++ lanza PatternSyntaxException o coincide mal”

Falta `Pattern.quote(entityText)`.

### “Los grupos cambian de orden entre ejecuciones”

Se está iterando un `Map` sin convertirlo y ordenarlo.

### “Las estadísticas ya no incluyen lugares u organizaciones”

`Main` está acumulando el resultado de `detectRelevant` en vez del resultado de `detectEntities`.

### “El resultado muestra `\n` en pantalla”

Se escribió `\n` dentro de un string triple. Usá un string normal o un salto físico.

### “El método dice que devuelve List, pero obtengo List[List[...]]”

Se usó `map` donde correspondía `flatMap`.

### “El código funciona, pero no respeta POO”

Buscá `isInstanceOf`, patrones sobre tipos o cadenas largas de `if` en el analizador. Si la decisión varía por subtipo, trasladala a un método polimórfico.

## 15. Respuestas conceptuales frecuentes

### ¿Por qué `describe` funciona sin redefinirlo en cada subclase?

Porque es un método concreto heredado que llama al método polimórfico `entityType`. El cuerpo común se reutiliza y la parte variable se resuelve según el tipo dinámico.

### ¿Qué ventaja tiene el polimorfismo frente a distinguir clases con `match`?

Cada clase encapsula su comportamiento. El cliente trabaja contra el tipo base y no necesita conocer todas las subclases. Esto reduce acoplamiento y permite extender la jerarquía con menos modificaciones.

### ¿Por qué `Event` puede no implementar `entityType`?

Porque sigue siendo abstracta. Solo una clase concreta está obligada a resolver todos los miembros abstractos heredados.

### ¿Por qué `University` debe redefinir `isRelevant` si el valor base ya era `true`?

Porque su padre inmediato, `Organization`, lo redefinió como `false`. La implementación más específica de la cadena es la elegida.

### ¿Por qué `filter` devuelve una entidad una sola vez aunque aparezca varias veces en el texto?

Porque se recorre cada entrada del diccionario una sola vez y solo se decide conservarla o descartarla. No se recopila una salida por cada ocurrencia textual.

### ¿Cuándo sí es correcto usar `match`?

Cuando se descompone una estructura de datos o se traduce una etiqueta externa a una instancia, como en la fábrica. No es ideal para reemplazar un comportamiento que debería redefinir cada objeto.

## 16. Plantilla condensada para examen

Esta versión sirve como recordatorio rápido. Hay que adaptar nombres y reglas a la consigna concreta.

```scala
import java.util.regex.Pattern

abstract class BaseEntity(val text: String) {
  def entityType: String

  def matches(candidate: String): Boolean =
    boundedMatch(candidate, ignoreCase = true)

  protected final def boundedMatch(
    candidate: String,
    ignoreCase: Boolean
  ): Boolean = {
    val literal = Pattern.quote(text)
    val regex = s"(?<![\\p{L}\\p{N}])$literal(?![\\p{L}\\p{N}])"
    val flags =
      if (ignoreCase) Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
      else 0

    Pattern.compile(regex, flags).matcher(candidate).find()
  }

  def isRelevant: Boolean = true
}

abstract class AbstractBranch(text: String) extends BaseEntity(text)

class ConcreteType(text: String) extends AbstractBranch(text) {
  override def entityType: String = "ConcreteType"
}

object GenericAnalyzer {
  def detect(
    text: String,
    dictionary: List[BaseEntity]
  ): List[BaseEntity] =
    dictionary.filter(_.matches(text))

  def detectRelevant(
    text: String,
    dictionary: List[BaseEntity]
  ): List[BaseEntity] =
    dictionary.filter(entity => entity.isRelevant && entity.matches(text))

  def countByType(entities: List[BaseEntity]): Map[String, Int] =
    entities
      .groupBy(_.entityType)
      .view
      .mapValues(_.size)
      .toMap
}
```

## 17. Checklist final de entrega

- [ ] `sbt -batch clean compile` termina correctamente.
- [ ] Las pruebas automatizadas pasan.
- [ ] El servidor mock y el programa principal funcionan juntos.
- [ ] Todos los casos de la tabla de matching están cubiertos.
- [ ] La clase base tiene defaults y las excepciones usan `override`.
- [ ] La nueva clase está conectada al diccionario y a `loadAll`.
- [ ] Los grupos y elementos se ordenan explícitamente.
- [ ] El caso vacío coincide exactamente con la salida pedida.
- [ ] La presentación usa datos relevantes.
- [ ] Las estadísticas usan todas las detecciones.
- [ ] No se agregaron librerías prohibidas.
- [ ] No se incluyeron artefactos generados.
- [ ] `git diff --check` no reporta errores.
- [ ] El commit contiene todos los archivos pedidos.

La idea más transferible del laboratorio es esta: cuando distintos tipos responden de manera distinta a la misma pregunta, convertí esa pregunta en un método del tipo base y dejá que cada subclase responda mediante herencia y `override`. El resto del programa podrá trabajar con una colección heterogénea como si fuera uniforme, que es justamente el valor práctico del polimorfismo.

"Resuelve este código según esta consigna: [pega el código completo]. Consigna: [tu instrucción detallada, ej: 'corrige bugs, optimiza rendimiento y explica cambios']."