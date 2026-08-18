# Knowledge Graph Workflow

This repository uses two complementary knowledge-graph tools:

- **graphify** is configured as an online MCP server in `.mcp.json`. It provides
  semantic/code knowledge-graph queries, paths, explanations, and optional wiki
  pages through the connected Claude Code session.
- **code-review-graph** runs locally and maintains a structural AST graph for
  change detection, impact analysis, affected flows, and test coverage.

## Choosing a tool

Use the graphify MCP tools available to the session for broad concepts and
relationships. If the online MCP server is unavailable, do not substitute a
local CLI automatically; use the code-review-graph tools or source files as a
fallback.

Use code-review-graph for change-aware work:

```bash
code-review-graph status --repo .
code-review-graph update --skip-flows --repo .
```

In Claude Code, prefer the code-review-graph MCP tools before reading or
searching source files:

- `get_minimal_context` — compact starting context
- `detect_changes` — risk-scored change analysis
- `get_review_context` — focused source snippets and test gaps
- `get_affected_flows` — impacted execution paths
- `get_impact_radius` — callers and dependents
- `query_graph` — callers, callees, imports, and tests
- `semantic_search_nodes` — locate symbols by name or concept

For architecture questions, use `get_architecture_overview` and
`list_communities`. For a refactor, inspect impact before editing.

## Fallback and scope

The graphs are advisory indexes, not the source of truth. Fall back to
`Read`, `Grep`, or `Glob` when a graph has no relevant result, when exact
source lines are needed for an edit, or when investigating configuration and
documentation files. Always verify important conclusions against the source.

The code-review-graph database is local and ignored by Git. A fresh clone
therefore needs the local tool installed and an initial graph build before
structural queries can provide useful results. The online graphify index is
managed by its MCP service and is not built by repository hooks.

## Automatic updates

The repository hooks update the local code-review-graph after commits and
branch switches when the tool and local database are available. Updates run
asynchronously, use a shared repository lock, and write diagnostics under
`~/.cache/`. A failed or skipped update does not block a commit. Local
`graphify` updates are opt-in via `GRAPHIFY_LOCAL_UPDATE=1`; embeddings are
opt-in via `CODE_REVIEW_GRAPH_EMBED=1` because they require the optional local
embedding dependencies.

The hooks discover tools from `PATH` and do not depend on a particular user's
home directory. The code-review-graph MCP server is pinned to the version
recorded in `.mcp.json` and uses the active project directory.
