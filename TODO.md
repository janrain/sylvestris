- create a NodePath data type along with associated json serialization
- clean up Relationship name clash
- example service is still kludgey
- configurize graph impl used in ServiceActor
- refactor SlickGraph per .transact
- add type level tests for Relationships API  (shapeless' illtyped)
- add tests based on gaps and scoverage reports
- scalariform
- refine error handling
- generate RAML
- consider json4s
- generalize JSON serialization. e.g. Pickle[T]
- logging and audit
- µtest? scala.js support?
- prefix projects and associated root folders with sylvestris-
- handle empty ids
- add CHANGELOG.md
- data validations
- triggered views
- consider data validations as triggered views
- consider http4s (scalaz at the moment)
