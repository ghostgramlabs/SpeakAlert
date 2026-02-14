import sys

with open('build_output.txt', 'rb') as f:
    content = f.read()
    # Try different encodings
    for encoding in ['utf-16le', 'utf-16be', 'utf-8', 'cp1252']:
        try:
            print(f"--- Encoding: {encoding} ---")
            print(content.decode(encoding))
            break
        except Exception:
            continue
