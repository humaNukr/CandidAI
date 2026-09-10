#!/usr/bin/env python3
"""
highlight_uncovered_lines.py

Parses JaCoCo XML test coverage report and Git diff to emit GitHub Actions workflow warnings
for modified/added lines that are not covered by tests.
Format:
::warning file={file},line={line},title=Uncovered Line::Line {line} is not covered by tests
"""

import argparse
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from typing import Dict, List, Optional, Set


def get_git_diff(base_ref: Optional[str] = None) -> str:
    """Retrieve git diff for changed lines."""
    cmd = []
    if base_ref:
        check_origin = subprocess.run(
            ["git", "rev-parse", "--verify", f"origin/{base_ref}"],
            capture_output=True,
            text=True,
        )
        target_ref = f"origin/{base_ref}" if check_origin.returncode == 0 else base_ref
        cmd = ["git", "diff", "-U0", f"{target_ref}...HEAD"]
    else:
        check_head = subprocess.run(
            ["git", "rev-parse", "--verify", "HEAD~1"],
            capture_output=True,
            text=True,
        )
        if check_head.returncode == 0:
            cmd = ["git", "diff", "-U0", "HEAD~1...HEAD"]
        else:
            cmd = ["git", "diff", "-U0", "HEAD"]

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Warning: git diff command failed ({' '.join(cmd)}): {result.stderr.strip()}", file=sys.stderr)
        return ""
    return result.stdout


def parse_changed_lines(diff_text: str) -> Dict[str, Set[int]]:
    """
    Parse unified diff output to extract mapping of file_path -> set of added/modified line numbers.
    """
    changed_lines: Dict[str, Set[int]] = {}
    current_file: Optional[str] = None
    current_line = 0

    hunk_regex = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")

    for line in diff_text.splitlines():
        if line.startswith("+++ b/"):
            current_file = line[6:].strip().replace("\\", "/")
            if current_file not in changed_lines:
                changed_lines[current_file] = set()
        elif line.startswith("+++ /dev/null"):
            current_file = None
        elif line.startswith("@@ ") and current_file:
            match = hunk_regex.match(line)
            if match:
                new_start = int(match.group(1))
                new_len = int(match.group(2)) if match.group(2) is not None else 1
                current_line = new_start
        elif current_file and current_line > 0:
            if line.startswith("+") and not line.startswith("+++"):
                changed_lines[current_file].add(current_line)
                current_line += 1
            elif line.startswith(" "):
                current_line += 1
            elif line.startswith("-") and not line.startswith("---"):
                pass  # Deleted line does not advance line number in new file

    return changed_lines


def parse_jacoco_uncovered_lines(jacoco_xml_path: str) -> Dict[str, Set[int]]:
    """
    Parse JaCoCo XML report to find uncovered lines (ci == 0 and mi > 0).
    Returns mapping of package-relative file path (e.g. 'ua/edu/ukma/candidai/CandidAiApplication.java')
    to set of uncovered line numbers.
    """
    if not os.path.isfile(jacoco_xml_path):
        print(f"JaCoCo report not found at: {jacoco_xml_path}", file=sys.stderr)
        return {}

    tree = ET.parse(jacoco_xml_path)
    root = tree.getroot()

    uncovered_by_path: Dict[str, Set[int]] = {}

    for package in root.findall(".//package"):
        package_name = package.get("name", "").strip()
        for sourcefile in package.findall("sourcefile"):
            sourcefile_name = sourcefile.get("name", "").strip()
            rel_path = f"{package_name}/{sourcefile_name}".replace("\\", "/").lstrip("/")

            uncovered_lines: Set[int] = set()
            for line_elem in sourcefile.findall("line"):
                nr = int(line_elem.get("nr", "0"))
                ci = int(line_elem.get("ci", "0"))
                mi = int(line_elem.get("mi", "0"))

                if ci == 0 and mi > 0:
                    uncovered_lines.add(nr)

            if uncovered_lines:
                uncovered_by_path[rel_path] = uncovered_lines

    return uncovered_by_path


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Highlight uncovered lines in PR diff using JaCoCo XML report"
    )
    parser.add_argument(
        "--jacoco-xml",
        default="build/reports/jacoco/test/jacocoTestReport.xml",
        help="Path to JaCoCo XML report",
    )
    parser.add_argument(
        "--base-ref",
        default=None,
        help="Git base reference for diff (e.g. main or master)",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Highlight all uncovered lines in JaCoCo report regardless of git diff",
    )

    args = parser.parse_args()

    uncovered_by_path = parse_jacoco_uncovered_lines(args.jacoco_xml)
    if not uncovered_by_path:
        print("No uncovered lines found in JaCoCo report (or report empty).")
        return

    warnings: List[dict] = []

    if args.all:
        for rel_path, lines in uncovered_by_path.items():
            repo_path = f"src/main/java/{rel_path}"
            if not os.path.exists(repo_path):
                repo_path = rel_path

            for line in sorted(lines):
                warnings.append({
                    "file": repo_path,
                    "line": line,
                    "message": f"Line {line} is not covered by tests"
                })
    else:
        diff_text = get_git_diff(args.base_ref)
        changed_lines = parse_changed_lines(diff_text)

        if not changed_lines:
            print("No changed lines detected in git diff.")
            return

        for diff_file, lines in changed_lines.items():
            matching_key = None
            for rel_path in uncovered_by_path:
                if diff_file.endswith(rel_path):
                    matching_key = rel_path
                    break

            if not matching_key:
                continue

            uncovered_in_file = uncovered_by_path[matching_key]
            uncovered_and_modified = lines.intersection(uncovered_in_file)

            for line in sorted(uncovered_and_modified):
                warnings.append({
                    "file": diff_file,
                    "line": line,
                    "message": f"Line {line} is not covered by tests"
                })

    for warn in warnings:
        file_path = warn["file"]
        line_num = warn["line"]
        msg = warn["message"]
        print(f"::warning file={file_path},line={line_num},title=Uncovered Line::{msg}")

    summary_msg = f"Found {len(warnings)} uncovered lines on modified files."
    print(summary_msg)

    step_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary_path:
        try:
            with open(step_summary_path, "a", encoding="utf-8") as f:
                f.write("### JaCoCo Uncovered Lines in PR Diff\n\n")
                if warnings:
                    f.write(f"> [!WARNING]\n> {summary_msg}\n\n")
                    f.write("| File | Line | Issue |\n")
                    f.write("| --- | --- | --- |\n")
                    for warn in warnings:
                        f.write(f"| `{warn['file']}` | {warn['line']} | {warn['message']} |\n")
                else:
                    f.write("All modified lines are covered by tests!\n")
        except Exception as e:
            print(f"Failed to write to GITHUB_STEP_SUMMARY: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
