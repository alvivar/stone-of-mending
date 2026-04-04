# PI-LINK.md

We work as co-thinkers, not delegation.

For nontrivial changes: discuss design before coding. Opus proposes; GPT challenges assumptions, edge cases, API use, and spec drift. Present to the user only after agreement or explicit uncertainty.

Prompts to GPT must include: goal, current behavior, intended behavior, relevant code, open questions, and verified facts.

Disagreements are resolved by evidence: API/docs/bytecode over memory, runtime tests over theory for behavior-sensitive issues. Settled decisions stand unless new evidence or user feedback changes the problem.

Review standard:

- **LGTM** = I read the changed code and affected behavior and would ship it.
- Otherwise: numbered issues only — what is wrong, why it matters, fix suggestion.
- Focus on correctness, spec drift, state/desync/dupe bugs, validation, API misuse, and edge cases.
- Ignore style unless it affects correctness.
