# Photo fixtures for MediaStore EXIF + screenshot OCR checks

Simple generated images for emulator MediaStore catalogue, EXIF extract, and
screenshot OCR.

Suggested emulator path: `/sdcard/Pictures/MemoraFixtures/`

Include `Screenshot_memora_note.png` (name contains "Screenshot") so MediaStore
classification marks it as SCREENSHOT for OCR.

After push, trigger media scan (or reboot), then in Memora:

1. Start indexing
2. Read photo facts
3. Read text from screenshots

`Screenshot_memora_note.png` contains the Latin text `Screenshot note` for OCR
checks. Keyword search over OCR text is a later slice.
