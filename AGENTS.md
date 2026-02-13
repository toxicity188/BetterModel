# BetterModel AGENT GUIDE (for Codex, Gemini, and other LLM agents)

This document defines repository-wide operating rules for automated contributors.
Scope: entire repository unless a deeper AGENTS.md overrides this file.

## 0) Mission and Priorities

- Primary mission: act as a maintenance assistant for safe updates and long-term stability.
- Prioritize documentation quality, API clarity, and compatibility over refactoring.
- Do not introduce architectural churn or speculative abstractions.
- Prefer minimal, reversible changes that preserve current behavior.

---

## 1) Module Responsibilities

### MUST
- Respect module boundaries:
  - `api`: architecture contracts, data definitions, pure logic, interfaces, domain exceptions.
  - `core`: implementation of `api`, orchestration, external I/O integration.
  - `platform/*`: platform packaging and platform-specific business integration using `core`.
  - `nms/*`: Minecraft-version-specific low-level adapters only.
- Keep dependency direction one-way where possible: `api -> core -> platform/nms` usage semantics.
- If a feature needs cross-module changes, start from `api` contract, then implement in `core`, then bind in `platform`.

### FORBIDDEN
- Do not place platform/runtime-specific details in `api`.
- Do not move business logic into `nms` unless version coupling is unavoidable.
- Do not bypass module contracts with ad-hoc cross-module shortcuts.

---

## 2) Language and File Placement Rules

### MUST
- Use Modern Java (language level 21) and Kotlin.
- `api` module is Java-only for production code (Kotlin DSL build scripts are allowed).
- Non-`api` modules should be Kotlin-first unless existing local code is explicitly Java-bound (e.g., mixin/accessor interop).
- Follow existing package root `kr.toxicity.model...` and module-specific suffixes.

### FORBIDDEN
- Do not add Kotlin source files to `api/src/main`.
- Do not introduce new language stacks or code generators without explicit request.

---

## 3) Allowed vs Forbidden Change Rules

### MUST
- Preserve existing behavior unless the task explicitly requests behavior change.
- Match local style of touched files (imports, naming, nullability annotations, formatting).
- Keep diffs small and scoped to the requested issue.
- Update/extend Javadoc when touching public Java APIs.

### RECOMMENDED
- Prefer additive changes over invasive rewrites.
- Prefer extension/composition over inheritance.

### FORBIDDEN
- No drive-by refactors.
- No broad renaming/reformatting-only commits mixed with functional changes.
- No “cleanup” changes unrelated to the requested task.

---

## 4) Code Patterns and Conventions

### Java rules

#### MUST
- Add English Javadoc for public API types/methods.
- Use `var` for local variables where legal and clear.
- When using primitive-key/value collections, prefer fastutil specialized types over boxed `java.util` alternatives.
- Declare classes `final` when inheritance is not intended.
- Use `of(...)` for factory method naming.
- Prefer enum-based singleton pattern when a singleton is required.

#### RECOMMENDED
- Prefer `record` for immutable data carriers.
- Interface-based API should expose its own factory method where practical.

#### DISCOURAGED
- Inheritance-heavy designs.

#### FORBIDDEN
- Do not depend on Kotlin classes from Java in `api`.

### Kotlin rules

#### MUST
- For Boolean arguments in calls, use named arguments (`parameter = value`) where available.
- Keep Kotlin style concise and explicit; avoid hidden side effects.

#### RECOMMENDED
- Avoid `abstract class` when interface + default methods or delegation works.

#### FORBIDDEN
- No Kotlin production code in `api` module.

---

## 5) Javadoc Policy

### MUST
- English only.
- Use `@since` matching current `gradle.properties` project version policy.
- Include `@return` for non-void methods.
- For records, document all components using `@param` at type level.
- Include `@throws` for throw-capable public methods.
- Public API docs must include a usage example (`@example` or code block).
- Keep terminology consistent with project domain (Molang, item_display, packet-based rendering, etc.).

---

## 6) Exception Handling Policy

### MUST
- Domain exceptions must be declared in `api` and reused by `core/platform`.
- Use unchecked exceptions for domain errors (`RuntimeException` subclasses).
- Prefer explicit domain exception types over anonymous `RuntimeException` for new logic.

### FORBIDDEN
- No new checked exceptions in public API surface unless explicitly requested.
- No swallowing exceptions without logging/context.

---

## 7) Architectural Boundary Enforcement

### MUST
- Enforce separation of concerns:
  - parsing/model contracts in `api`
  - orchestration and state management in `core`
  - runtime platform hooks in `platform`
  - protocol/version internals in `nms`
- Keep NMS version modules behaviorally equivalent unless version-specific differences are required.

### FORBIDDEN
- Do not leak platform classes into generic API contracts.
- Do not duplicate core logic in multiple platform modules.

---

## 8) Change Management Principles

### MUST
- Before coding, identify smallest viable patch.
- Keep commits logically atomic.
- Use conventional commits (`docs:`, `fix:`, `refactor:`, `chore:`).
- Document why a change is necessary, not only what changed.

### RECOMMENDED
- Prefer one concern per PR.
- If migration is unavoidable, provide transitional compatibility notes.

---

## 9) Minimal Change Principle (Anti-Overengineering)

### MUST
- Solve the concrete problem only.
- Reuse existing utilities/conventions before introducing new abstractions.
- Avoid framework-like internal layers unless repeatedly justified by current code.

### FORBIDDEN
- No speculative extensibility.
- No premature optimization without evidence.

---

## 10) Backward Compatibility Policy

### MUST
- Preserve existing public API behavior by default.
- Treat changes to signatures, semantics, serialization shape, config keys, and command contracts as breaking.
- For unavoidable breaking changes: document impact, migration path, and versioning intent.

### RECOMMENDED
- Prefer deprecation + transition period over hard removal.

---

## 11) Dependency Introduction Policy

### MUST
- Prefer existing dependencies and in-repo utilities.
- Any new dependency requires clear justification: purpose, scope, size, and maintenance cost.
- Add dependency versions through central version catalog/patterns already in use.

### FORBIDDEN
- Do not add dependencies for trivial utilities already present in JDK/Kotlin stdlib/current stack.
- Do not introduce overlapping libraries providing the same capability.

---

## 12) Diff and PR Format Guidelines

### MUST
- Keep PRs reviewable: focused scope, clear title, concise rationale.
- Include:
  - summary of changes
  - impacted modules
  - compatibility notes
  - tests/checks run
- Separate mechanical formatting from logic changes whenever possible.

### RECOMMENDED
- Use checklist format for validation and risk points.

---

## 13) Test Policy

### MUST
- Run the most relevant checks for touched modules.
- Validate both compile-time and behavior-level impact where feasible.
- If tests are absent, run targeted build/lint/verification tasks and report limitations.

### RECOMMENDED
- Prefer adding focused tests only when directly related to changed behavior.
- Keep test fixtures lightweight; do not add broad test frameworks without request.

---

## 14) Existing Code Precedence Rule

### MUST
- Existing local code patterns take precedence over generic best practices when conflicts arise.
- Follow deeper-scope AGENTS.md if present.
- If repository reality conflicts with this guide, preserve behavior first and propose policy alignment separately.

### FORBIDDEN
- Do not force style unification across untouched files.

---

## 15) Final Operational Checklist

Before finishing, verify:
- Scope is minimal and task-aligned.
- Module boundaries are respected.
- Public API docs are updated (if touched).
- Exception policy and compatibility policy are respected.
- Changes are validated with relevant checks.
- PR description contains rationale, risks, and verification results.
