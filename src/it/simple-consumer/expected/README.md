# Golden corpus — current generated shape (D1 freeze target)

These files are the byte-exact expected output of `generate-metadata` over this IT project's
entities. The integration test diffs actual output against them, so **any change to the generated
shape is a deliberate, reviewed event**, never an accident.

**Known, scheduled defects — do not mistake this corpus for an endorsement:**

- `org/springframework/**` files are generated **into Spring's namespaces** (split packages,
  architecture review W2) and `StaticR2dbcEntityTemplateAccessor_` is a static service locator
  (W3). Both are slated for removal by the runtime-library workstream (WS-4); the metamodel
  contract workstream (WS-3) owns the replacement shape. Until those decisions land, the corpus
  freezes the *current* shape because the future annotation processor must reproduce whatever
  shape users compile against (design doc §3, watch-item 1 / decision D1).

When a shape change is ratified, regenerate these files and update them in the same commit as the
generator change, referencing the ADR that approved it.
