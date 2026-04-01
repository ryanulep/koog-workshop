---
name: compose
description: |
  Compose expert skill for UI development. Guides state management decisions
  (@Composable, remember, mutableStateOf, derivedStateOf, State hoisting), view composition
  and structure, Modifier chains, lazy lists, navigation, animation, side effects, theming,
  accessibility, and performance optimization. Backed by actual androidx source code analysis.
  Use this skill whenever the user mentions Compose, @Composable, remember, LaunchedEffect,
  Scaffold, NavHost, MaterialTheme, LazyColumn, Modifier, recomposition, Style, styleable,
  MutableStyleState, or any Jetpack Compose API. Also trigger when the user says "Android UI",
  "Kotlin UI", "compose layout", "compose navigation", "compose animation", "material3",
  "compose styles", "styles api", or asks about modern Android development patterns. Even
  casual mentions like "my compose screen is slow" or "how do I pass data between screens"
  should trigger this skill.
---

# Jetpack Compose Expert Skill

Non-opinionated, practical guidance for writing correct, performant Jetpack Compose code.
Focuses on real pitfalls developers encounter daily, backed by analysis of the actual
`androidx/androidx` source code (branch: `androidx-main`).

## Workflow

When helping with Compose code, follow this checklist:

### 1. Understand the request
- What Compose layer is involved? (Runtime, UI, Foundation, Material3, Navigation)
- Is this a state problem, layout problem, performance problem, or architecture question?

### 2. Consult the right reference
Read the relevant reference file(s) from `references/` before answering:

| Topic | Reference File |
|-------|---------------|
| `@State`, `remember`, `mutableStateOf`, state hoisting, `derivedStateOf`, `snapshotFlow` | `references/state-management.md` |
| Structuring composables, slots, extraction, preview | `references/view-composition.md` |
| Modifier ordering, custom modifiers, `Modifier.Node` | `references/modifiers.md` |
| `LaunchedEffect`, `DisposableEffect`, `SideEffect`, `rememberCoroutineScope` | `references/side-effects.md` |
| `LazyColumn`, `LazyRow`, `LazyGrid`, `Pager`, keys, content types | `references/lists-scrolling.md` |
| `NavHost`, type-safe routes, deep links, shared element transitions | `references/navigation.md` |
| Recomposition skipping, stability, baseline profiles, benchmarking | `references/performance.md` |
| Semantics, content descriptions, traversal order, testing | `references/accessibility.md` |

### 3. Apply and verify
- Write code that follows the patterns in the reference
- Flag any anti-patterns you see in the user's existing code
- Suggest the minimal correct solution — don't over-engineer

## Key Principles

1. **Compose thinks in three phases**: Composition → Layout → Drawing. State reads in each
   phase only trigger work for that phase and later ones.

2. **Recomposition is frequent and cheap** — but only if you help the compiler skip unchanged
   scopes. Use stable types, avoid allocations in composable bodies.

3. **Modifier order matters**. `Modifier.padding(16.dp).background(Color.Red)` is visually
   different from `Modifier.background(Color.Red).padding(16.dp)`.

4. **State should live as low as possible** and be hoisted only as high as needed. Don't put
   everything in a ViewModel just because you can.

5. **Side effects exist to bridge Compose's declarative world with imperative APIs**. Use the
   right one for the job — misusing them causes bugs that are hard to trace.
