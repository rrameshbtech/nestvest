# Repository Analysis

- Use the Codebase Memory MCP for repository analysis, including codebase exploration,
  structural discovery, dependency/call-path tracing, and impact analysis.
- For structural discovery, use `search_graph`, then `trace_path` when tracing behavior,
  then `get_code_snippet` to verify relevant source.
- For broader architecture questions, use `get_architecture` or `query_graph`.
- Call `check_index_coverage` for every source file relied on. If coverage is incomplete,
  inspect the reported missed lines directly and qualify conclusions accordingly.
- Use direct source search and file reads for literal text, configuration, documentation,
  and for verifying graph results. If Codebase Memory MCP is unavailable or the graph
  does not cover the relevant source, continue with direct source inspection and state
  the limitation; do not present an unverified graph result as complete.
