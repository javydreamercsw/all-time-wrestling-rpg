#!/usr/bin/env python3
"""Generate outcome_matrices JSON seed files from production drama_event data."""

import json
import os
import re
import subprocess
import sys

# Mapping: event_type -> (filename_stem, chart_name, category, description)
CHART_MAP = [
    ("BETRAYAL",            "feud_angle_betrayal",     "FEUD_ANGLE",    "Feud angle outcomes driven by betrayal and broken trust"),
    ("ALLIANCE_FORMED",     "feud_angle_alliance",     "FEUD_ANGLE",    "Feud angle outcomes driven by new alliances and shifting loyalties"),
    ("CONTRACT_DISPUTE",    "feud_angle_contract",     "FEUD_ANGLE",    "Feud angle outcomes driven by contract and business disputes"),
    ("RETIREMENT_TEASE",    "feud_angle_retirement",   "FEUD_ANGLE",    "Feud angle outcomes involving retirement threats and legacy moments"),
    ("PERSONAL_ISSUE",      "feud_angle_personal",     "FEUD_ANGLE",    "Feud angle outcomes rooted in personal conflicts and rivalries"),
    ("CAMPAIGN_RIVAL",      "feud_angle_rival",        "FEUD_ANGLE",    "Feud angle outcomes from campaign rivalry storylines"),
    ("BACKSTAGE_INCIDENT",  "feud_angle_backstage",    "FEUD_ANGLE",    "Feud angle outcomes stemming from backstage confrontations"),
    ("SOCIAL_MEDIA_DRAMA",  "promo_social_media",      "PROMO",         "Promo outcomes driven by social media controversy"),
    ("MEDIA_CONTROVERSY",   "promo_media",             "PROMO",         "Promo outcomes driven by media controversy and press coverage"),
    ("CHAMPIONSHIP_CHALLENGE","promo_championship",    "PROMO",         "Promo outcomes involving championship challenges and title proclamations"),
    ("FAN_INTERACTION",     "highlight_reel_fan",      "HIGHLIGHT_REEL","Highlight reel outcomes from fan interactions"),
    ("SURPRISE_RETURN",     "highlight_reel_return",   "HIGHLIGHT_REEL","Highlight reel outcomes from surprise returns"),
    ("INJURY_INCIDENT",     "match_flow_injury",       "MATCH_FLOW",    "Match flow outcomes involving injury incidents"),
]

def get_env():
    env_path = os.path.join(os.path.dirname(__file__), "..", ".env")
    env = {}
    with open(env_path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
    return env

def query_mysql(env, sql):
    result = subprocess.run(
        ["mysql", f"-u{env['MYSQL_USER']}", f"-p{env['MYSQL_PASSWORD']}",
         env["MYSQL_DATABASE"], "-N", "-B", "-e", sql],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"MySQL error: {result.stderr}", file=sys.stderr)
        return []
    rows = []
    for line in result.stdout.strip().splitlines():
        rows.append(line.split("\t"))
    return rows

def generate(event_type, env):
    sql = f"""
        SELECT
            de.description,
            COALESCE(w1.name, '') AS primary_name,
            COALESCE(w2.name, '') AS secondary_name,
            COALESCE(de.heat_impact, 0),
            COALESCE(de.fan_impact, 0),
            de.injury_caused
        FROM (
            SELECT description, primary_wrestler_id, secondary_wrestler_id,
                   heat_impact, fan_impact, injury_caused
            FROM drama_event
            WHERE event_type = '{event_type}'
        ) de
        LEFT JOIN wrestler w1 ON de.primary_wrestler_id = w1.wrestler_id
        LEFT JOIN wrestler w2 ON de.secondary_wrestler_id = w2.wrestler_id
    """
    rows = query_mysql(env, sql)
    seen = set()
    entries = []
    dice_roll = 1
    for row in rows:
        if len(row) < 6:
            continue
        description, p_name, s_name, heat, fan, injury = row

        # Replace specific wrestler names with generic placeholders
        template = description
        if p_name:
            template = template.replace(p_name, "FAVORED")
        if s_name:
            template = template.replace(s_name, "UNDERDOG")

        # Drop entries with missing-name artifacts: space before punctuation or double spaces
        if re.search(r' [.,;]', template) or re.search(r'  ', template):
            continue

        # Deduplicate by resulting template text
        key = template.strip()
        if not key or key in seen:
            continue
        seen.add(key)

        entry = {
            "diceRoll": dice_roll,
            "templateText": key,
        }
        heat_val = int(heat) if heat and heat != "0" else None
        fan_val = int(fan) if fan and fan != "0" else None
        if heat_val:
            entry["heatDelta"] = heat_val
        if fan_val:
            entry["fanDelta"] = fan_val
        if injury == "1":
            entry["injuryCaused"] = True

        entries.append(entry)
        dice_roll += 1

    return entries

def main():
    env = get_env()
    out_dir = os.path.join(
        os.path.dirname(__file__), "..",
        "src", "main", "resources", "outcome_matrices"
    )
    os.makedirs(out_dir, exist_ok=True)

    for event_type, stem, category, description in CHART_MAP:
        print(f"Processing {event_type}...", end=" ", flush=True)
        entries = generate(event_type, env)
        if not entries:
            print("no entries found, skipping.")
            continue

        chart_name = stem.replace("_", " ").title()
        payload = {
            "name": chart_name,
            "category": category,
            "description": description,
            "entries": entries,
        }

        out_path = os.path.join(out_dir, f"{stem}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)

        print(f"{len(entries)} unique entries → {stem}.json")

    print("Done.")

if __name__ == "__main__":
    main()
