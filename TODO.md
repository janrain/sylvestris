- CRUD routes -- GET & POST are done, need PUT
    - add relationship labels to web API
- external API client -- We have GET, need PUT, POST, custom lenses
- example service is still kludgey
- how do we migrate current data to this model?
- tests
    - add type level tests for Relationships API  (shapeless' illtyped)
- scoverage
- scalariform
- wart remover
- continuous integration
- refine error handling
- generate RAML
- Json4s. what was the error we ran into? we might have changed enough that it's no longer an issue?
- generalize JSON serialization. e.g. Pickle[T]
- open-source it; keep the result of this work open-source ready
- logging and audit
- µtest? scala.js support?

In the short term utilize current C3 mechanisms for:
===========
- data validations
- triggered views

In the log term, think about data validations as triggered views
