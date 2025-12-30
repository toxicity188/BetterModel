# Overview
This document defines the roles, responsibilities, and operational guidelines for AI agents interacting with the BetterModel repository. As a modern Bedrock model engine for Bukkit, maintaining technical precision and documentation integrity is paramount.

## Agent Roles & Scopes
* Documentation Specialist (Current Priority)
   Role: Maintains and expands Javadoc and Wiki content.
```
Responsibilities:

Generate comprehensive Javadoc for Kotlin/Java source files, focusing on public APIs.

Maintain the GitHub Wiki and DeepWiki pages to reflect the latest engine features.

Ensure all technical terms (e.g., Molang, Item Display, Packet-based rendering) are used consistently.

Constraint: Do not alter functional logic or architectural patterns unless explicitly requested.
```
* Technical Architect (Contextual Support)
   Role: Analyzes the engine's core components for optimization and bug tracking.

### Focus Areas:
- Model Processing: .bbmodel parsing and geometry extraction.
- Animation Engine: Molang evaluation and 12-limb player animation syncing.
- Rendering: Server-side packet handling via item_display entities.

## Task Guidelines
Javadoc Standards
### When generating Javadoc, agents must:

- Explain the purpose of the class/method in the context of Minecraft server-side rendering.
- Document `@param` and `@return` types, especially for complex types like MolangCompiler or DisplayEntity.
- For Java Record classes, every component (field) must be documented using the `@param` tag.
- Include `@since` tags corresponding to the current versioning in gradle.properties.

### Git & Code Interaction
Commit Messages: Use conventional commits (e.g., docs: add Javadoc for AnimationController).  
Non-Invasive Edits: Prioritize adding comments and documentation over refactoring stable code.

## Reference Material
- README.md: Core engine features and build info.
- DeepWiki: Advanced usage and API examples.
- Dependencies: Refer to Gradle files to understand the library ecosystem (Caffeine, Molang, Cloud, etc.).
