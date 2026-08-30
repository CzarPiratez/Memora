# UNFYND Core - Capability and applications

**License:** Apache License 2.0  
**Authority:** Aligned with the public Core vision at
[https://www.unfynd.com/core](https://www.unfynd.com/core)

This note describes **what Core is designed to enable** and **how**. It is
direction we are building toward - one infrastructure, many trust boundaries -
not a shipping manifesto for every vertical product.

---

## How Core works (primitives)

Intelligence in Core is the path from a question to a **justified** output.

1. **Ingest and retention** - Multimodal material enters a durable local record.
   Retention is for continuity: what entered yesterday remains available
   tomorrow. Over time, isolated assets become structured knowledge - with
   footing that can be revisited.
2. **Recall** - Surfaces what is relevant across a large multimodal store.
3. **Ranking** - Orders competing candidates when many items could apply.
4. **Evidence-tied answers** - Outputs are produced against that ranked set,
   tied to sources you can open and review - not as unconstrained completion
   detached from the record.
5. **Boundary you set** - Execution next to the data: personal and professional
   devices (phones, laptops, desktops) and controlled environments (hospital and
   lab networks, government systems, defence enclaves, industrial sites, kits
   with weak or denied connectivity). Cloud round-trip is not the default path
   to intelligence.

For builders, that split matters. You can specialize retrieval for a domain,
tighten ranking for a workflow, and keep generation inside a boundary the
organization accepts. The same primitives support a consumer device and a
regulated system.

**Inspectability is part of the loop.** High-stakes environments need to see
what the system used. If a result cannot point back to what it rested on, it is
not ready for the classes of work Core is meant to serve.

**Core provides** ingest/retention, recall/ranking, evidence-tied answers, and
respect for the boundary you set. **You build** the app, workflow, domain rules,
sources, and permissions. Same Core, your product on top.

---

## Industries and application classes

When memory and intelligence can stay with the data, whole categories of product
become buildable - including domains we have not imagined yet.

### Health

Facility-local records, imaging reports, protocols, and notes queried inside the
boundary. Bedside and field settings where connectivity is poor and export is
restricted. Research and trial material that must not leave a controlled store.

### Defence and public safety

Air-gapped and classified environments. Field devices with SOPs, maps, sensor
logs, and after-action material. Investigations that need ranked, inspectable
retrieval across multimodal case files.

### Government

Agency knowledge on controlled networks. Policy, case material, and operational
docs with residency and audit requirements. Diplomatic and consular contexts
where leakage is unacceptable.

### Enterprise and SMB

Institutional memory that never belonged in a consumer chatbot: contracts,
recordings, manuals, mail archives, deal rooms. Mid-market and specialist firms
that need vertical tools without standing up a private AI lab. Clean rooms and
partner corpora that must die with the engagement.

### Research and education

Labs and universities with sensitive participant data or unpublished work.
Long-horizon scholarly and student knowledge on device or institution-owned
hardware.

### Industrial and field operations

Plants, energy, infrastructure: procedures, inspection video, maintenance
history next to machines that disconnect by design. Safety-critical recall from
approved sources with evidence, not freestyle generation.

### Accessibility

Build assistive products on Core: describe a screen, follow speech, keep
preferences and history on the device. Someone’s disability context never has to
leave their phone for the tool to get better at helping them.

### Emergency response

Ship field guides that run when towers are down. Core keeps protocols, maps, and
what was just seen or said on the device, then answers fast with evidence you
can trust in the moment that matters.

### Aging / companion care

Give elders and families a product that remembers what matters at home: people,
routines, what “normal” sounds like, without uploading that life to a vendor.
**Core is the memory and recall underneath**, on their hardware - not a claim
that UNFYND Core itself is the companion product.

---

## Related contracts

Technical contracts for Memory, evidence, seams, and local-first rules live in
[`SPEC.md`](SPEC.md). Openness phases for this pack live in
[`ROADMAP-OPEN.md`](ROADMAP-OPEN.md).
