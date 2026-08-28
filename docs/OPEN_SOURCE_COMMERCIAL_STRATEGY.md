> **Repository filing:** Not hashed in `docs/PRODUCT_SOURCE_REGISTRY.md`. Business only; not architectural authority. `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` still governs sequence. `docs/ARCHITECTURE_FREEZE_v1.0.md` still governs architecture.

# Memora Open Source & Commercial Strategy V3.2 — Enterprise Draft

**Status:** Enterprise Draft — final candidate pending adversarial/legal review. **Class A (Public Specification / Contract pack under Apache-2.0) authorized by ADR-047; pack implementation pending.**  
**Version:** 3.2  
**Purpose:** Define Memora's open-source, source-available, proprietary, intellectual-property, commercial licensing, and enterprise governance strategy without modifying the frozen Memora architecture.

**Governing architectural baseline:** `ARCHITECTURE_FREEZE_v1.0.md`

**Canonical technical authorities:**
- `PRODUCT_CONTRACT.md`
- `LOCAL_AI_TECHNICAL_SPEC.md`
- `EXPERIENCE_MEMORY_AMENDMENT_V1.md`
- `ARCHITECTURAL_MIGRATION_SPEC_V1.md`

---

# 1. Strategic Purpose

Memora is being developed as **on-device personal memory infrastructure**, not as a cloud AI service.

The long-term objective is to build a technically strong, privacy-preserving, evidence-backed memory engine that can:

1. power the Memora consumer application;
2. be used by developers and communities where the applicable license permits;
3. provide a reusable technology platform/SDK;
4. support commercial integration by companies under clearly defined commercial rights; and
5. potentially support enterprise and OEM deployments.

The commercial strategy must preserve the central technical proposition:

> **Memora brings intelligence to the user's device rather than requiring the user's personal digital life to be sent to a Memora-operated cloud.**

Memora's commercial strategy must therefore monetize genuine technology and product value without making cloud infrastructure a dependency of Memora Core.

---

# 2. Relationship to the Architecture Freeze

`ARCHITECTURE_FREEZE_v1.0.md` establishes the four canonical architectural authorities:

1. `PRODUCT_CONTRACT.md`
2. `LOCAL_AI_TECHNICAL_SPEC.md`
3. `EXPERIENCE_MEMORY_AMENDMENT_V1.md`
4. `ARCHITECTURAL_MIGRATION_SPEC_V1.md`

Those documents remain the architectural authority.

This strategy document:

- does not redefine the Memory model;
- does not redefine Evidence;
- does not redefine retrieval;
- does not redefine ranking;
- does not introduce cloud dependency;
- does not add MVP capabilities;
- does not alter the migration sequence;
- does not reopen frozen architectural decisions.

If a commercial requirement appears to conflict with the frozen architecture, the architecture governs.

Commercialization must happen **around the architecture**, not by weakening it.

---

# 3. Strategic Pillars

Memora's long-term technology and commercial strategy rests on six pillars.

## 3.1 Frozen Architecture

The technical foundation is governed by the existing Architecture Freeze and migration specification.

## 3.2 On-Device Core

Memora Core must remain capable of operating without a Memora-operated cloud dependency for its fundamental capabilities.

## 3.3 Open and Auditable Where Strategically Appropriate

Selected technology may be made publicly available to increase:

- transparency;
- trust;
- developer adoption;
- community participation;
- interoperability;
- research;
- ecosystem value.

Public availability must always be governed by an explicit license.

## 3.4 Strong Intellectual Property Ownership and Protection

Memora must deliberately preserve ownership and control of its intellectual property.

Public source availability does not mean that all Memora IP becomes unrestricted, nor does it automatically grant rights to use Memora's brand, proprietary technology, or commercial assets.

## 3.5 Commercial Licensing

Companies that require commercial rights beyond those granted by the applicable community/public license can obtain defined commercial rights through a commercial agreement where appropriate.

## 3.6 Enterprise-Grade Governance

Before Memora presents itself as licensable enterprise technology, it must establish appropriate technical, security, IP, dependency, release, and support governance.

---

# 4. Memora Technology Boundary

Memora should be understood as a technology platform with a reference application rather than only as an Android application.

## 4.1 Memora Core

Memora Core is the underlying on-device memory technology.

It should contain the reusable technology required for:

- ingestion;
- extraction;
- understanding;
- evidence-backed Memory construction;
- local embeddings;
- local storage;
- retrieval;
- ranking;
- Explain Mode;
- provenance;
- memory integrity;
- revision handling;
- capability contracts;
- versioning;
- future governed correlation primitives.

The precise implementation boundary must be established from the actual repository during the repository audit.

## 4.2 Memora Reference Application

The Android application is the first reference implementation and consumer product.

It demonstrates:

- real device integration;
- local processing;
- indexing;
- retrieval;
- user experience;
- Explain Mode;
- device-health-aware execution;
- privacy boundaries.

The reference application should be architected as a consumer of the underlying Memora technology rather than being the only location in which the technology exists.

## 4.3 Memora SDK / Integration Surface

A future SDK should expose stable interfaces through which other applications can consume appropriate Memora capabilities without depending on the internal implementation.

The SDK boundary must be derived from the frozen capability contracts and actual implementation, not invented solely for commercialization.

---

# 5. The Public / Proprietary Principle

The central strategic rule is:

> **Memora may make selected source code publicly available without making all Memora intellectual property public, without transferring ownership of that intellectual property, and without granting rights beyond those expressly provided by the applicable license.**

This distinction must remain explicit throughout the project.

The following concepts are different and must never be treated as synonyms:

- public source;
- open-source software;
- source-available software;
- proprietary software;
- commercial license;
- copyright ownership;
- trademark rights;
- patent rights;
- model/data rights.

Publicly accessible code is not automatically "free for any use."

The applicable license determines what users may do with the licensed material.

---

# 6. Intellectual Property Ownership and Protection

IP protection is a first-class strategic requirement.

## 6.1 Ownership

Memora should retain ownership of intellectual property created by Memora, its employees, and appropriately contracted contributors, subject to applicable law and contractual arrangements.

Before public or commercial release, ownership and contribution rights must be documented.

## 6.2 Public Source Does Not Transfer Ownership

Publication of source code does not itself transfer copyright ownership to users, companies, forks, or downstream developers.

A license grants specified rights; it does not constitute a general transfer of ownership unless the relevant legal instrument explicitly provides otherwise.

## 6.3 Proprietary Assets

Potential proprietary Memora assets include, where not expressly released under a public license:

- proprietary implementations;
- tuned `MemoryBuilder` implementations;
- tuned `RecallRanker` implementations;
- calibration methods;
- production model configurations;
- proprietary AI Packs;
- proprietary evaluation corpora;
- proprietary quality-tuning datasets;
- internal evaluation infrastructure;
- enterprise tooling;
- commercial integration tooling;
- internal operational systems;
- unreleased product capabilities;
- commercial documentation and support systems where appropriate.

The exact proprietary boundary must be determined through technical and IP review.

## 6.4 Trademarks and Brand

The Memora name, logos, product marks, and official branding must remain separately controlled.

Access to or use of Memora source code must not by itself grant permission to:

- represent a fork as an official Memora product;
- use the Memora name in a misleading way;
- use Memora trademarks commercially;
- imply certification or endorsement by Memora.

Trademark rights must be governed separately from source-code licensing.

## 6.5 Patents and Other Rights

Any patentable inventions or other registrable intellectual property should be assessed separately from source-code licensing.

Public release decisions should consider whether publication could affect future IP protection.

This document does not make any determination about patentability.

---

# 7. Open Source vs Source-Available

There are two legitimate strategic models.

## Model A — True Open Source

Selected Memora components are released under an OSI-approved open-source license.

Commercial use is permitted to the extent granted by that license.

Memora's commercial opportunity therefore comes from things such as:

- proprietary extensions;
- premium implementations;
- commercial integration;
- enterprise support;
- enterprise tooling;
- commercial releases;
- OEM agreements;
- other rights or services that are not restricted by the open-source license.

### Strategic advantages

- strongest open-source credibility;
- broadest developer adoption;
- easier ecosystem participation;
- easier third-party integration;
- stronger transparency claim.

### Strategic disadvantages

- commercial use cannot simply be prohibited by the open-source license;
- commercial value must come from differentiated assets, services, rights, or productization;
- proprietary boundaries must be carefully maintained.

## Model B — Source-Available Community License

Selected source is publicly inspectable and available to individuals and communities, while the license restricts defined commercial uses.

Commercial organizations must obtain a commercial license for the restricted use.

This should be described as **source-available**, not as Open Source in the strict OSI sense.

### Strategic advantages

- stronger control over commercial exploitation;
- direct commercial licensing path;
- public source remains inspectable;
- clear distinction between community and commercial rights.

### Strategic disadvantages

- reduced compatibility with conventional open-source ecosystems;
- potentially lower developer adoption;
- commercial developers may be less willing to depend on restricted components;
- licensing must be communicated extremely clearly.

## V3.1 Position

The final choice between Model A and Model B is intentionally not made by this draft.

It must be decided before public release after technical, IP, commercial, and qualified legal review.

The final strategy must not describe a component as "Open Source" if its license imposes restrictions that are incompatible with the OSI Open Source Definition.

---

# 8. Public and Auditable Components

The following are strong candidates for public availability, subject to final licensing and dependency review:

- Memory format specification;
- Memory and Evidence definitions;
- capability contracts;
- public SDK contracts;
- plugin contracts;
- source-provider contracts;
- sandbox contracts;
- interoperability formats;
- deterministic data structures;
- documentation;
- selected reference implementations;
- privacy/data-flow documentation;
- evaluation methodology;
- selected developer tooling;
- selected test fixtures that contain no restricted or private material.

The public boundary is not final until the licensing model and dependency audit are complete.

---

# 9. Memory Format as a Strategic Asset

The Memory representation should be treated as a first-class technology/interface, not merely as implementation schema.

The long-term objective should be a documented, versioned, language-agnostic **Memory Format Specification** defining, as appropriate:

- Memory structure;
- Evidence structure;
- provenance;
- integrity;
- versioning;
- identity;
- revision semantics;
- supported extensions;
- future Link/Event extension points;
- compatibility expectations.

The Memory Format Specification should be separable conceptually from any one programming language, database, model provider, or Android implementation.

This creates a potential interoperability and ecosystem boundary while preserving the distinction between:

**the format**

and

**Memora's proprietary implementations that operate on that format.**

---

# 10. Auditable Application Shell and Proprietary Intelligence

Memora should evaluate a deliberate split between an auditable application/integration layer and proprietary intelligence implementations.

The strategic concept is:

### Public/auditable application layer

Potentially includes:

- UI;
- dependency injection;
- capability contracts;
- application data flow;
- privacy boundaries;
- integration interfaces;
- reference application structure.

### Potentially proprietary intelligence layer

Potentially includes:

- concrete `MemoryBuilder` implementation;
- concrete `RecallRanker` implementation;
- tuning;
- calibration;
- production optimization;
- proprietary evaluation assets;
- proprietary model configurations.

This is not a final repository boundary.

It is a strategic principle to evaluate during the repository audit.

The objective is to preserve a strong trust proposition:

> **The way the application handles user data and invokes memory capabilities can be inspectable without requiring every piece of Memora's differentiated intelligence to be publicly released.**

Any final implementation must remain consistent with the frozen architecture.

---

# 11. Commercial Licensing

Commercial licensing should provide clearly defined rights rather than simply acting as a payment mechanism.

Depending on the final licensing model, commercial rights may cover:

- proprietary application integration;
- commercial redistribution;
- OEM/device integration;
- enterprise deployment;
- private modifications;
- commercial SDK use;
- access to proprietary extensions;
- supported commercial releases;
- maintenance;
- integration assistance;
- enterprise support;
- other rights specifically defined by agreement.

Commercial agreements should clearly define, as appropriate:

- permitted use;
- products covered;
- deployment scope;
- redistribution;
- modification;
- sublicensing;
- support;
- maintenance;
- update rights;
- attribution;
- trademark rights;
- termination;
- security obligations;
- intellectual-property ownership;
- applicable warranties and limitations;
- other contractual obligations.

Final legal language requires qualified legal review.

---

# 12. Commercial Use and Misuse

Memora should not rely on a vague concept of "misuse."

The licenses and agreements must precisely define:

- what is permitted;
- what is prohibited;
- what requires a commercial license;
- what constitutes redistribution;
- what constitutes modification;
- what constitutes incorporation into a commercial product;
- what trademark use is permitted;
- what attribution is required;
- what happens after termination of a commercial license.

The public repository should contain clear licensing documentation.

A company must not have to infer commercial rights from README prose.

Where a commercial restriction is intended, it must be implemented through the actual legal license/contract rather than merely through a statement in documentation.

---

# 13. Contributor and IP Governance

Before accepting significant external contributions, Memora should establish a formal contribution policy.

The policy should address:

- contributor copyright;
- contribution licensing;
- DCO and/or CLA;
- employee contributions;
- contractor contributions;
- third-party contributions;
- code provenance;
- model provenance;
- dataset provenance;
- generated-code provenance where relevant;
- rights needed for future commercial licensing.

The chosen mechanism should preserve Memora's ability to:

- maintain the public project;
- distribute contributions;
- relicense where legally appropriate;
- offer commercial licenses where intended.

No contribution policy should be selected solely for convenience.

It must be reviewed against the final licensing strategy.

---

# 14. Third-Party Dependencies

Before public release, dependencies crossing a public or commercial boundary must undergo a license and provenance review.

Each dependency should be classified according to:

- license;
- commercial-use permissions;
- redistribution permissions;
- source-disclosure obligations;
- attribution requirements;
- modification requirements;
- patent provisions;
- compatibility with Memora's selected license;
- security status.

This applies to:

- Android libraries;
- ML runtimes;
- OCR;
- PDF processing;
- native libraries;
- indexing/vector components;
- model runtimes;
- model weights;
- AI Packs;
- evaluation tooling where distributed.

Technical compatibility is not sufficient for inclusion.

---

# 15. AI Pack and Model IP

The Local AI architecture treats AI Packs as independently versioned local model assets with manifests, compatibility, integrity, licensing, and installation-state requirements.

Accordingly, each AI Pack must have documented:

- model identity;
- version;
- provenance;
- license;
- commercial-use rights;
- redistribution rights;
- modification rights where relevant;
- integrity information;
- compatibility;
- update policy.

Memora's source-code license does not automatically grant rights to third-party model weights or datasets.

A model may remain subject to a separate license even when the surrounding Memora code is public.

---

# 16. No-Cloud Core Principle

Memora Core must not require Memora-operated cloud infrastructure for its fundamental capabilities.

The core architecture should support local:

- ingestion;
- extraction;
- understanding;
- embeddings;
- storage;
- retrieval;
- ranking;
- explanation.

Future downstream companies may independently add:

- cloud synchronization;
- cloud backup;
- remote processing;
- account systems;
- enterprise infrastructure;
- cloud APIs.

Those additions are downstream product decisions.

Memora should not create a cloud dependency merely to establish recurring revenue.

---

# 17. Enterprise Technology Readiness

Memora should not describe the technology as enterprise-ready merely because its architecture is modular.

Enterprise commercialization should be supported by evidence.

Before substantial enterprise licensing, Memora should establish appropriate controls and evidence for:

### Security

- dependency scanning;
- vulnerability management;
- security disclosure;
- signed releases;
- artifact integrity;
- supply-chain provenance;
- permissions review;
- data-flow documentation;
- secure update process.

### Reliability

- indexing failure handling;
- process death;
- cancellation;
- recovery;
- database integrity;
- revision correctness;
- stale-data handling.

### Device operation

- battery behavior;
- thermal behavior;
- storage requirements;
- memory pressure;
- background execution;
- offline operation.

### Retrieval quality

- Recall@K;
- precision/quality metrics;
- latency;
- semantic retrieval;
- literal retrieval;
- mixed-content retrieval.

### Trust

- unsupported-claim rate;
- explanation coverage;
- calibration;
- overconfidence/error rates;
- provenance preservation.

The existing architecture's evaluation requirements remain authoritative.

---

# 18. Repository Strategy

The repository structure must be derived from actual technical boundaries, not pricing or licensing convenience.

The current repository should first be audited.

The audit should identify:

1. current modules;
2. current dependencies;
3. current coupling;
4. Core candidates;
5. Android-specific components;
6. SDK candidates;
7. proprietary candidates;
8. evaluation assets;
9. licensing conflicts;
10. third-party IP.

Only after that audit should final repository boundaries be established.

Possible future boundaries may include:

```text
Memora Core
Memora SDK
Memora Android Reference Application
Memora Evaluation / Tooling
Private Commercial Extensions
```

This is illustrative only.

No repository split should be implemented merely because this strategy document contains the names.

---

# 19. Commercialization Must Not Distort Architecture

The following shortcuts are prohibited as a matter of strategy:

- moving critical functionality to cloud services merely to monetize it;
- creating a separate commercial Memory substrate;
- creating a second commercial retrieval architecture;
- bypassing Evidence requirements for commercial users;
- bypassing provenance requirements;
- allowing proprietary components to silently alter Memory truth;
- embedding provider-specific cloud dependencies into Memora Core;
- weakening Explain Mode because of commercial packaging;
- making commercial licensing a reason to reopen the architecture freeze.

Commercialization must preserve the technical contracts.

---

# 20. Enterprise and OEM Use

Potential commercial technology customers may include:

- smartphone manufacturers;
- device manufacturers;
- productivity platforms;
- accessibility technology companies;
- secure document platforms;
- specialized AI applications;
- enterprise software providers.

The commercial proposition should be based on the technology's demonstrated capabilities rather than on generic claims of "AI."

Before pursuing major OEM/enterprise licensing, Memora should be able to demonstrate:

- retrieval quality;
- device performance;
- privacy/data-flow guarantees;
- API stability;
- integration quality;
- security posture;
- provenance;
- release discipline;
- licensing clarity.

---

# 21. IP Due-Diligence Requirements Before Public Release

Before publishing significant Memora technology, the following should be completed:

### Ownership

- employee IP assignments where required;
- contractor agreements;
- contributor terms;
- third-party code provenance.

### Licensing

- final project license;
- dependency license audit;
- model-license audit;
- AI Pack licensing;
- documentation licensing;
- test-fixture licensing.

### Proprietary assets

- proprietary implementation inventory;
- confidential information inventory;
- evaluation asset inventory;
- unreleased model/configuration inventory.

### Brand

- Memora trademark strategy;
- logo ownership;
- official-product policy;
- fork/derivative naming policy.

### Patent/IP review

- identify potentially patent-sensitive inventions;
- determine whether publication timing creates any concern;
- obtain qualified IP advice where necessary.

---

# 22. Governance Hierarchy

The strategic hierarchy is:

```text
FROZEN ARCHITECTURE
        │
        ▼
TECHNICAL IMPLEMENTATION
        │
        ▼
IP / LICENSING BOUNDARIES
        │
        ▼
PUBLIC / COMMUNITY RELEASE
        │
        ▼
CONSUMER PRODUCT + COMMERCIAL TECHNOLOGY
```

The commercial strategy cannot redefine the architecture.

The licensing strategy cannot silently alter technical truth requirements.

The public repository cannot be treated as a substitute for a legal license.

The license cannot grant rights that Memora does not own.

---

# 23. Release Gates

Memora should not treat every form of public release or commercial release as carrying the same risk.

The release process is therefore divided into two distinct release classes:

1. **Public Specification / Contract Release**
2. **Commercial / Enterprise Technology Release**

This distinction allows low-risk, high-trust public artifacts to be released when they are ready without requiring full enterprise commercialization readiness.

## Gate 1 — Architecture

- Architecture Freeze verified;
- migration traceability verified;
- no unauthorized architectural changes.

## Gate 2 — IP and Provenance

- ownership established for the material being released;
- contributor rights reviewed where applicable;
- third-party provenance reviewed;
- no known confidential or restricted material included;
- trademark implications reviewed where applicable.

## Gate 3 — Licensing

- final licensing model selected;
- public components classified;
- proprietary components classified;
- dependency audit completed;
- model/AI Pack licenses audited.

## Gate 4 — Security

- dependency vulnerabilities reviewed;
- release integrity established;
- permissions/data flow reviewed;
- security disclosure process established.

## Gate 5 — Quality

- retrieval benchmarks;
- explanation/trust benchmarks;
- failure/recovery testing;
- device performance testing;
- offline testing.

## Gate 6 — Commercial

- commercial license drafted;
- commercial rights defined;
- support/maintenance model defined;
- enterprise deployment boundaries defined.

### Release Class A — Public Specification / Contract Release

A Public Specification / Contract Release may include carefully selected artifacts such as:

- Memory Format Specification;
- Memory/Evidence definitions;
- capability contracts;
- selected SDK/interface documentation;
- selected developer documentation;
- other explicitly approved public specifications.

Release Class A requires, at minimum:

- **Gate 1 — Architecture**
- **Gate 2 — IP and Provenance**
- **appropriate licensing review for the released material**

Release Class A does not require Memora to already satisfy all enterprise commercialization requirements.

The purpose of this release class is to establish transparency, interoperability, developer understanding, and ecosystem foundations before commercial technology licensing is mature.

### Release Class B — Commercial / Enterprise Technology Release

A Commercial / Enterprise Technology Release requires the complete release-readiness stack:

- **Gate 1 — Architecture**
- **Gate 2 — IP and Provenance**
- **Gate 3 — Licensing**
- **Gate 4 — Security**
- **Gate 5 — Quality**
- **Gate 6 — Commercial**

This release class applies to commercial technology packages, enterprise SDKs, proprietary extensions, supported commercial distributions, OEM technology licensing, and other offerings that create contractual commercial rights.

The existence of Release Class A must not be interpreted as authorization for commercial redistribution of components that require a separate commercial license.

---

# 24. Decisions Required Before Public Release

The following decisions must be explicitly resolved before public release of source-code components or other artifacts whose rights depend on them:

1. True Open Source vs Source-Available licensing model.
2. Exact public component boundary.
3. Exact proprietary component boundary.
4. Final project license(s).
5. Commercial license structure.
6. Contributor governance mechanism.
7. Third-party dependency policy.
8. AI Pack/model licensing policy.
9. Trademark policy.
10. IP ownership and provenance process.
11. Public repository structure.
12. Enterprise release/security policy.

### Relationship to Release Class A

The Model A vs Model B decision is required before any public release of source-code components whose licensing depends on that decision.

It does **not necessarily gate Release Class A artifacts that are independently licensed as documentation, specifications, contracts, or other non-source materials**, provided that:

- ownership and provenance have been established;
- the artifact is clearly identified;
- an appropriate artifact-specific license or permission framework is in place;
- no unreleased source code, proprietary implementation, confidential information, restricted dependency, or other material requiring a different licensing decision is included.

Release Class A therefore remains capable of serving its intended purpose: establishing transparency, interoperability, developer understanding, and ecosystem foundations before the complete commercial software licensing model is finalized.

These decisions must not be silently made by an implementation agent.

---

# 25. Decisions That Do Not Need to Precede the Migration

The following can be finalized later, provided the architecture and repository are designed to preserve the necessary boundaries:

- commercial pricing;
- individual customer contract terms;
- enterprise support packages;
- OEM pricing;
- specific enterprise SLAs;
- future downstream cloud services;
- specific commercial partnerships.

The migration should not be delayed by decisions that do not affect current technical boundaries.

---

# 26. Immediate Implementation Sequence

The correct sequence is:

### Phase 1 — Finalize Strategy

Resolve the open licensing/IP decisions through adversarial technical, commercial, and qualified legal review.

### Phase 2 — Repository Audit

Inspect the current repository against:

- the frozen architecture;
- the migration specification;
- the final strategy;
- dependency and IP constraints.

**No code changes during the audit.**

### Phase 3 — Boundary Definition

Document:

- Memora Core;
- Memory Format;
- SDK;
- Android reference application;
- public/auditable components;
- proprietary components;
- commercial licensing boundary.

### Phase 4 — Architecture Migration

Begin the existing migration roadmap.

The migration remains governed by `ARCHITECTURAL_MIGRATION_SPEC_V1.md`.

### Phase 5 — Core Hardening

Benchmark, test, harden, document, and stabilize the reusable engine.

### Phase 6A — Public Specification / Contract Release

Where appropriate, release approved public specifications and contracts once Release Class A requirements are satisfied.

This phase may occur before full commercial/enterprise readiness.

### Phase 6B — Commercial / Enterprise Technology Release

Only after the complete Release Class B requirements are satisfied should Memora launch commercial technology licensing, enterprise SDKs, supported commercial distributions, or OEM/strategic technology licensing.

---

# 27. Strategic Principle

Memora should not attempt to protect its business by hiding everything.

Nor should it attempt to build an ecosystem by giving away every asset without a deliberate IP strategy.

The strategic objective is:

> **Make the right technology open and auditable, keep genuine differentiated intellectual property controlled, establish precise licensing boundaries, and build commercial value around technology that companies have a reason to license.**

The central principle is:

> **Public source is a distribution decision. A license is a rights decision. Ownership is an IP decision. Commercial licensing is a business decision. These must never be conflated.**

And across all of them:

> **Memora Core remains on-device and does not require a Memora-operated cloud service.**

---

# 28. Status

**Enterprise Draft — Not Final**

This document:

- does not itself grant any rights;
- does not select a final license;
- does not authorize commercial redistribution;
- does not modify the Architecture Freeze;
- does not replace the migration specification;
- does not establish pricing;
- does not constitute legal advice.

It is intended for:

1. adversarial technical review;
2. licensing/IP review;
3. repository/dependency audit;
4. commercial strategy review;

before becoming the final Memora Open Source & Commercial Strategy.

