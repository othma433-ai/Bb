# Integrated Extractor Features

This source tree uses AL-thmany Sender as the base application and embeds the Extractor workspace.

Integrated features:
- Group/link extraction
- Invite-link scanning
- Publishing
- Extractor data/checkpoint database
- Extractor Accessibility runtime and Shizuku runtime

Entry point: Sender toolbar menu -> الاستخراج والفحص والنشر.

Safety/coordination rule: Extractor operations refuse to start while Sender invitation automation is active, so both engines do not control WhatsApp simultaneously.

The original Sender join engine, database, Shizuku runtime and Accessibility service remain unchanged.
