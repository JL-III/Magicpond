# Overfishing (fishing-pressure) system

A within-API replacement for mcMMO's fishing anti-exploit, built to deter
autofishing: fish a spot too frequently for too long and the fish stop biting
there until the spot recovers.

This is what lets the **magic pond run without mcMMO**. Previously the pond
bonus was gated by mcMMO's `isExploitingFishing` check; now it's gated by this
system instead. It applies everywhere (the pond is not exempt): a depleted catch
is cancelled, and because the bonus listener runs later with
`ignoreCancelled = true`, depletion suppresses the vanilla fish *and* the pond
bonus together.

## Why not just use mcMMO's check?

mcMMO's `FishingManager` keeps a **single** `lastFishingBoundingBox` per player.
Each catch is compared only to the previous one; a non-overlapping cast resets
the counter (`Fishing_ExploitFix_Options: MoveRange: 3, OverFishLimit: 10`).

Consequences:

- **Trivially bypassed** by alternating two spots >`MoveRange` apart — the
  counter never builds (this is the well-known "mcMMO Overfishing Bypass").
- **Reset-on-one-move** — any jitter zeroes progress.
- **No time dimension** — it can't express "too frequently *for too long*", and
  spots never recover on their own.
- **Chain-drift** — comparing only to the last cast lets a slowly drifting hook
  evade detection.

## The model

The hook's `(x, z)` is snapped to a fixed grid cell (`cell-size`). Each player
keeps an independent, decaying **pressure** value per cell:

```
on catch in cell C:
    decay(C)                          # pressure *= 0.5 ^ (elapsed / half-life)
    if C.depleted:
        allowed = C.pressure < resume-threshold   # hysteresis
        C.depleted = !allowed
    else:
        allowed = C.pressure < pressure-cap
        C.depleted = !allowed
    C.pressure += gain-per-catch       # added even when denied
    if !allowed: cancel the catch (no fish) + notify
```

Because pressure **decays exponentially**, the rate of fishing decides the
outcome on its own:

- **Casual fishing** reaches a low equilibrium far below the cap and never trips.
  (e.g. one catch / 60 s with a 180 s half-life settles at ~4.9 pressure.)
- **Sustained autofishing** climbs past the cap in ~`pressure-cap / gain` catches
  (~10 by default) and stays depleted while the bot keeps casting, because denied
  catches still add pressure.

This fixes every mcMMO weakness: **many** cells are tracked (alternation gains
nothing), grid bucketing is jitter-proof and drift-proof, and the time decay
gives real "frequency over duration" semantics plus automatic recovery.

It is 100% Bukkit/Paper API — `PlayerFishEvent`, `FishHook#getLocation()`, the
scheduler — with no NMS, reflection, or mcMMO internals, so frequent Theatria
updates won't break it.

## Configuration (`config.yml`)

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master switch. |
| `cell-size` | `8` | Grid cell edge in blocks (size of "one spot"). |
| `pressure-cap` | `10.0` | Pressure at which a cell becomes depleted. |
| `resume-threshold` | `6.0` | Recover below this to start catching again (hysteresis). |
| `gain-per-catch` | `1.0` | Pressure added per catch. |
| `recovery-half-life-seconds` | `180` | Seconds for pressure to halve. |
| `notify-player` | `true` | Action-bar message on a denied catch. |
| `sweep-interval-seconds` | `120` | Background decay/eviction interval. |

## Tuning quickly

- **Too punishing for legit players?** Raise `pressure-cap`, raise
  `gain-per-catch`-to-cap ratio, or shorten `recovery-half-life-seconds`.
- **Autofishers lasting too long?** Lower `pressure-cap`, lengthen
  `recovery-half-life-seconds`, or raise `gain-per-catch`.
- **Spot too small/large?** Adjust `cell-size`.

### Magic pond tuning (important)

The pond is meant for concentrated fishing, so `cell-size` is the key lever:
an AFK auto-clicker re-casts to the *same* landing spot (one cell) and depletes
it, while an active player's casts vary across several cells and keep each one
below the cap. A **smaller `cell-size`** sharpens that distinction (more cells to
spread across) at the cost of being stricter on players who fish a very tight
spot; a larger one is more forgiving but easier to AFK.

Honest caveat: a legit player and a bot catch fish at the *same* rate (both are
bound by Minecraft's bite timing), so pressure alone can't perfectly separate
"dedicated grinder standing still" from "bot standing still." The spatial spread
above is the practical discriminator; for a stronger guarantee see the
movement-based idea below.

## Commands & permissions

- `/magicpond info` — current settings + tracked-player count.
- `/magicpond check` — pressure of the cell you're standing in.
- `/magicpond clear` — wipe all tracked pressure.
- `/magicpond reload` — reload `config.yml` (and reschedule the sweep).
- `magicpond.admin` (op) — use the command.
- `magicpond.bypass` (op) — exempt from depletion (useful for staff/testing).

## Verifying in-game

1. Set low test values, e.g. `pressure-cap: 3`, `recovery-half-life-seconds: 20`.
2. `/magicpond reload`, then fish one spot — after ~3 catches the action bar
   should report depletion and catches stop.
3. `/magicpond check` shows pressure climbing and the `[DEPLETED]` flag.
4. Wait for pressure to decay below `resume-threshold`; catches resume.
5. Confirm a second spot (a cell away) is unaffected while the first is depleted
   — the alternation bypass that defeats mcMMO no longer works.

## Notes / future ideas

- State is in-memory (like mcMMO's), so it resets on restart and is **not**
  reset by relogging (no relog-to-clear exploit). Persisting to disk or a
  `PersistentDataContainer` is a straightforward follow-up if needed.
- Currently depletion is binary (catch or no catch). A "diminishing returns"
  variant could instead scale `event.setExpToDrop(...)` / reward chance with
  pressure for a softer feel.
- **Movement-based AFK signal (recommended next step):** also track the player's
  position + look direction (yaw/pitch) between catches. An auto-clicker holds
  the same spot and angle for hundreds of catches; a human inevitably shifts.
  Combining "no movement" with pressure would target true automation while
  letting active players fish the pond as long as they like — a robust,
  still-within-API upgrade over mcMMO's brittle single-slot check.
