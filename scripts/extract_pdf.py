"""
Extraction texte d'un PDF (dépendance: pip install pypdf).
Usage:
  python scripts/extract_pdf.py chemin/vers/fichier.pdf [sortie.txt]
"""
import sys
from pathlib import Path

try:
    from pypdf import PdfReader
except ImportError:
    print("Installez: pip install pypdf")
    raise SystemExit(1)


def main() -> None:
    argv = sys.argv[1:]
    if not argv:
        print(__doc__)
        raise SystemExit(2)
    pdf = Path(argv[0]).resolve()
    out = Path(argv[1]).resolve() if len(argv) > 1 else pdf.with_suffix("_extracted.txt")
    reader = PdfReader(str(pdf))
    parts = [f"PAGES: {len(reader.pages)}\n"]
    for i, page in enumerate(reader.pages):
        t = page.extract_text() or ""
        parts.append(f"\n===== PAGE {i + 1} =====\n{t}\n")
    out.write_text("".join(parts), encoding="utf-8")
    print(f"OK -> {out}")


if __name__ == "__main__":
    main()
