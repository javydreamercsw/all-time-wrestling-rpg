# OutcomeMatrix Entry Generation Prompts

Each file in this directory is a prompt you can paste directly into an AI agent (Claude, Gemini, ChatGPT, etc.) to generate a batch of OutcomeMatrix JSON entries.

## How to use

1. Open the prompt file for the category you want to populate.
2. Paste the entire contents into an AI chat session.
3. Save the AI's JSON response as a file under `src/main/resources/outcome_matrices/`.
4. The file name must end in `.json` and the `category` field must match one of the valid enum values.
5. Re-run the app — `DataInitializer.syncOutcomeMatricesFromFiles()` picks up all files in that directory automatically.

## Placeholders

|  Placeholder   |                             Meaning                              |
|----------------|------------------------------------------------------------------|
| `{WRESTLER_1}` | The dominant/heel/higher-momentum wrestler in the current moment |
| `{WRESTLER_2}` | The face/lower-momentum wrestler who is being challenged         |

Both placeholders are substituted at runtime by `OutcomeMatrixService.resolveRoll()`.

## Effect fields (all optional)

|       Field        |  Type   |                                     Meaning                                     |
|--------------------|---------|---------------------------------------------------------------------------------|
| `heatDelta`        | int     | Heat added between {WRESTLER_1} and {WRESTLER_2} (positive = more rivalry heat) |
| `fanDelta`         | long    | Fan count change for the primary wrestler (positive = gain fans)                |
| `tvGradeDelta`     | int     | TV grade steps (+1 = up one letter, -1 = down one letter)                       |
| `grudgeGradeDelta` | int     | Grudge grade steps between the two wrestlers                                    |
| `injuryCaused`     | boolean | Whether this outcome triggers an injury check                                   |

Omit any field that should have no effect (do not include nulls or zeros).

## Valid category values

- `MATCH_FLOW` — momentum shifts and in-match events
- `FINISHER` — finishing-move attempt outcomes
- `POST_MATCH` — events after the bell
- `FEUD_ANGLE` — storyline developments between rivals
- `HIGHLIGHT_REEL` — in-arena TV moments
- `PROMO` — verbal segments and media appearances

## Files in this directory

|     Prompt file     |       Target JSON file        |     Category     |
|---------------------|-------------------------------|------------------|
| `feud_angle.md`     | `feud_angle_<theme>.json`     | `FEUD_ANGLE`     |
| `promo.md`          | `promo_<theme>.json`          | `PROMO`          |
| `highlight_reel.md` | `highlight_reel_<theme>.json` | `HIGHLIGHT_REEL` |
| `match_flow.md`     | `match_flow_<theme>.json`     | `MATCH_FLOW`     |
| `post_match.md`     | `post_match_<theme>.json`     | `POST_MATCH`     |
| `finisher.md`       | `finisher_<theme>.json`       | `FINISHER`       |

