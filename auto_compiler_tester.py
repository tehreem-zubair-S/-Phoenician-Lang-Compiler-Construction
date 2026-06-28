import os
import re
import subprocess
import sys
from pathlib import Path
import time

try:
    from google import genai
except ImportError:
    genai = None

PROJECT_ROOT = Path(__file__).resolve().parent
GENERATED_DIR = PROJECT_ROOT / "generated_tests"
BUILD_DIR = PROJECT_ROOT / "build" / "auto_tester_classes"

MODEL_NAME = "gemini-2.0-flash"
MAX_FIX_ATTEMPTS = 3

JAVAC_COMMAND = [
    "javac",
    "-encoding",
    "UTF-8",
    "-d",
    str(BUILD_DIR),
    "src/Main.java",
    "src/lexer/*.java",
    "src/parser/*.java",
]

LANGUAGE_GUIDE = r"""
Generate Phoenician language programs for this compiler.

Use ONLY these stable Phase 1 features:
- Variable declaration: 𐤍 name 𐤔 value;
- Print: 𐤇(expression);
- If/else: 𐤀𐤋𐤐 (condition) { ... } 𐤁𐤏𐤃 { ... }
- While: 𐤂𐤌𐤋 (condition) { ... }

Operators:
- assignment: 𐤔
- equality: 𐤔𐤔
- greater: 𐤕
- less-or-equal: 𐤀𐤕
- addition: 𐤐
- subtraction: 𐤑
- multiplication: 𐤒
- division: 𐤓
- modulo: 𐤏

Important grammar rules:
- Every statement must end with a semicolon ;
- Blocks use ASCII braces { }
- Print uses ASCII parentheses ( )
- Identifiers are normal English letters.
- Output ONLY the raw source code. No Markdown fences. No explanations.
"""


def configure_client():
    if genai is None:
        print("Missing dependency: google-genai")
        print("Install it with: pip install google-genai")
        sys.exit(1)

    api_key = os.environ.get("API_KEY")
    if not api_key:
        print("Missing API key. Set it in PowerShell:")
        print('$env:API_KEY="YOUR_KEY_HERE"')
        sys.exit(1)

    return genai.Client(api_key=api_key)


def compile_java_sources():
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    command = " ".join(JAVAC_COMMAND)
    result = subprocess.run(
        command, cwd=PROJECT_ROOT, shell=True, text=True, capture_output=True, encoding="utf-8"
    )
    if result.returncode != 0:
        print("Java compiler build failed.")
        print(result.stdout)
        print(result.stderr)
        sys.exit(result.returncode)


def run_compiler(program_path):
    command = ["java", "-cp", str(BUILD_DIR), "Main", str(program_path)]
    return subprocess.run(
        command, cwd=PROJECT_ROOT, text=True, capture_output=True, encoding="utf-8"
    )


def ask_model(client, task, previous_code=None, compiler_output=None):
    prompt = LANGUAGE_GUIDE + "\n\nTask:\n" + task
    if previous_code and compiler_output:
        prompt += "\n\nThe previous version failed. Fix it.\n"
        prompt += "\nPrevious code:\n" + previous_code
        prompt += "\nCompiler output:\n" + compiler_output

    for attempt in range(3):
        try:
            response = client.models.generate_content(
                model=MODEL_NAME,
                contents=prompt
            )
            return clean_code(response.text)
        except Exception as e:
            if "429" in str(e) or "RESOURCE_EXHAUSTED" in str(e):
                wait = 60 * (attempt + 1)
                print(f"Rate limited. Waiting {wait}s...")
                time.sleep(wait)
            else:
                raise
    raise RuntimeError("Exceeded retry limit due to API quota.")


def clean_code(text):
    text = text.strip()
    text = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    return text.strip() + "\n"


def generate_and_test_program(client, index, task):
    GENERATED_DIR.mkdir(exist_ok=True)
    program_path = GENERATED_DIR / f"llm_sample_{index}.phn"
    code = None
    compiler_output = None

    for attempt in range(1, MAX_FIX_ATTEMPTS + 1):
        print(f"\nGenerating sample {index}, attempt {attempt}...")
        code = ask_model(client, task, code, compiler_output)
        program_path.write_text(code, encoding="utf-8")

        result = run_compiler(program_path)
        compiler_output = result.stdout + result.stderr

        if result.returncode == 0 and "Execution: PASSED" in compiler_output:
            print(f"✅ PASS: {program_path.name}")
            return True, program_path, compiler_output

        print(f"❌ FAIL: {program_path.name}")
        print(extract_failure_summary(compiler_output))

    return False, program_path, compiler_output


def extract_failure_summary(output):
    interesting = []
    for line in output.splitlines():
        if "FAILED" in line or "Error" in line or "error" in line or "failed" in line or "Exception" in line:
            interesting.append(line)
    return "\n".join(interesting[-10:]) if interesting else output[-1000:]


def main():
    compile_java_sources()
    client = configure_client()

    print("\n=== Phoenician LLM Auto-Tester ===")
    print("Type 'exit' to stop.")

    passed = 0
    failed = 0
    index = 1

    while True:
        task = input(f"\n➤ Enter your program idea for LLM (or type 'exit'): ").strip()

        if task.lower() == 'exit':
            break
        if not task:
            continue

        ok, path, _ = generate_and_test_program(client, index, task)
        if ok:
            passed += 1
        else:
            failed += 1
            print(f"Could not make {path.name} pass after {MAX_FIX_ATTEMPTS} attempts.")

        index += 1

    print("\n=== Auto Compiler Tester Summary ===")
    print(f"Generated directory: {GENERATED_DIR}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")


if __name__ == "__main__":
    main()