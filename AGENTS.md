- Always pipe ./gradlew into `.gradle_output` and use it to inspect your previous builds.
- To use Exposed correctly with the specific project requirements, Exposed v1:
    - Package structure: Exposed v1 uses org.jetbrains.exposed.v1.* e.g., org.jetbrains.exposed.v1.core.*,
      org.jetbrains.exposed.v1.jdbc.*.
    - Extension functions: Many common DSL functions like selectAll(), update(), insert(), and deleteWhere are in
      org.jetbrains.exposed.v1.jdbc or
      similar specific packages.
- Functions that return `null` as "signal" for errors like abscene of a row should always have a `orNull` function
  postfix.