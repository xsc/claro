# Implementation Notes

## Deeply Nested Structures

Before any resolution happens, claro will analyze the value it was given to
collect the initial set of resolvables. This means that the whole tree will be
traversed recursively, resulting in the following points of note regarding very
large trees:

- the stack might overflow during inspection,
- initial inspection, as well as subsequent application steps might show
  degrading performance.

Real-world data should not exhibit excessive nesting, especially not before
resolution, so while users should keep these points in mind, they most probably
won't be affected by them.

## Interface vs. Protocol Implementation

claro will only work with values implementing the `Resolvable` *interface* -
which is automatically done when `claro.data/Resolvable` is used with
`defrecord`, `deftype` or `reify`.  This means that values that "earn" their
resolvability via `extend-type` or `extend-protocol` will not be picked up.

The reason for this is a huge performance gap between `satisfies?` (which has to
create a list of all superclasses for a given value, then intersect it with all
classes implementing a protocol) and `instance?` (which boils down to a simple
reflection call).
