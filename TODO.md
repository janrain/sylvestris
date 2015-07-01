- move notion of edge pairs into Graph and GraphM
- data validations
- pull PathSegment and NodeWithRelationships out of service and move them into service-common
- separate out into proper sbt sub-projects
- external API client -- We have GET, need PUT and POST
- constrain allowed relationships on entity CRUD routes -- POST is done, need PUT
- Add relationship labels to web API
- how do we migrate current data to this model?
- consider Triggered Views within this world
- identifiers should be unique; add constraints for enforcement
- open-source it; keep the result of this work open-source ready
- tests
- model should be more like an example project, less janrain specific
- add type level tests for GraphM linking (shapeless' illtyped)
- s/Entity/Node/g
- scoverage
- generate RAML
- CI
- generalize JSON serialization. e.g. Pickle[T]
- scalariform
- Json4s. what was the error we ran into? we might have changed enough that it's no longer an issue?
- Handle error cases

=====

Graph Layer - Nodes and Edges
Relationships Layer - Nodes and Relationships (constraint refinement of the lower Graph Layer)
