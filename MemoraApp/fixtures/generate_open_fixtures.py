from pathlib import Path

OUT = Path(r"c:\Users\DELL\Documents\Memora\MemoraApp\fixtures")


def make_stream(text: str) -> bytes:
    content = f"BT /F1 18 Tf 72 720 Td ({text}) Tj ET\n".encode("latin-1")
    return (
        f"<< /Length {len(content)} >>\nstream\n".encode("ascii")
        + content
        + b"endstream"
    )


def write_pdf(path: Path, page_texts: list[str]) -> None:
    n = len(page_texts)
    objects: list[bytes] = []
    # 1 Catalog, 2 Pages, then page objs, then content objs, then font
    kids = " ".join(f"{3 + i} 0 R" for i in range(n))
    objects.append(b"1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n")
    objects.append(
        f"2 0 obj<< /Type /Pages /Kids [{kids}] /Count {n} >>endobj\n".encode("ascii")
    )
    content_start = 3 + n
    font_obj = content_start + n
    for i in range(n):
        page_num = 3 + i
        content_num = content_start + i
        objects.append(
            (
                f"{page_num} 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                f"/Contents {content_num} 0 R /Resources<< /Font<< /F1 {font_obj} 0 R >> >> >>endobj\n"
            ).encode("ascii")
        )
    for i, text in enumerate(page_texts):
        content_num = content_start + i
        objects.append(
            f"{content_num} 0 obj\n".encode("ascii") + make_stream(text) + b"\n"
        )
    objects.append(
        f"{font_obj} 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n".encode(
            "ascii"
        )
    )

    out = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for obj in objects:
        offsets.append(len(out))
        out.extend(obj)
    xref_pos = len(out)
    out.extend(f"xref\n0 {len(offsets)}\n".encode("ascii"))
    out.extend(b"0000000000 65535 f \n")
    for off in offsets[1:]:
        out.extend(f"{off:010d} 00000 n \n".encode("ascii"))
    out.extend(
        f"trailer<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref_pos}\n%%EOF\n".encode(
            "ascii"
        )
    )
    path.write_bytes(out)
    print(f"wrote {path.name} ({path.stat().st_size} bytes, {n} pages)")


write_pdf(
    OUT / "memora-open-2page.pdf",
    [
        "Memora page one only ALPHA marker",
        "Memora page two BRAVO meet mira",
    ],
)
write_pdf(
    OUT / "memora-open-3page.pdf",
    [
        "Page 1 CHARLIE receipt total",
        "Page 2 DELTA boarding pass gate",
        "Page 3 ECHO meet mira follow-up",
    ],
)
write_pdf(
    OUT / "memora-open-5page.pdf",
    [
        "Page 1 FOXTROT cover sheet",
        "Page 2 GOLF invoice number",
        "Page 3 HOTEL meeting notes",
        "Page 4 INDIA travel plan",
        "Page 5 JULIET meet mira closing",
    ],
)
