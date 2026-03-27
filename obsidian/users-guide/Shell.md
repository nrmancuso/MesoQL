# Shell

Running `just mesoql` starts the interactive shell — the primary way to use MesoQL.

```
$ just mesoql
MesoQL (type \q to quit)

mesoql>
```

You can also start it explicitly with `just mesoql shell`.

## Line editing

The shell uses JLine, so standard terminal keybindings work:

- Arrow keys to move within a line
- Up/Down to scroll through history
- Ctrl-A / Ctrl-E to jump to start/end of line
- Ctrl-R to reverse-search history
- Ctrl-C to cancel the current line
- Ctrl-D to exit

## History

Command history is saved to `~/.mesoql_history` and persists across sessions. Use the up arrow or
Ctrl-R to recall previous queries.

## Exiting

Any of these will exit the shell:

- `\q`
- `exit`
- `quit`
- Ctrl-D

## One-off queries

If you don't need an interactive session, use `query` to run a single query and exit:

```bash
just mesoql query --inline 'SEARCH storm_events WHERE SEMANTIC("tornado") LIMIT 5'
just mesoql query my_query.mql
```

Add `--json` to get JSON output (useful for piping into `jq`).

## Other commands

| Command | Purpose |
|---|---|
| `just mesoql query` | Execute a single query from file or `--inline` |
| `just mesoql index` | Run ingestion (see [[users-guide/Data Ingestion]]) |
| `just mesoql validate` | Parse and validate a query without executing |
| `just mesoql stats` | Show index document counts and sizes |
| `just mesoql --help` | Show all commands |

## Related

- [[users-guide/Query Language]] — full syntax reference
- [[components/CLI]] — implementation details
