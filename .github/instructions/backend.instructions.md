---
applyTo: "src/main/java/**/*.java,src/test/java/**/*.java,src/main/resources/application.properties,pom.xml"
---

When editing backend files in this repository:

- Keep proxy request handling, comparison logic, and reporting responsibilities separate.
- Preserve explicit configuration for upstreams, timeouts, comparison rules, and reporting modes.
- Avoid hardcoding environment-specific upstream URLs outside the existing configuration model.
- Keep tests targeted to the affected proxy behavior or comparison logic.
