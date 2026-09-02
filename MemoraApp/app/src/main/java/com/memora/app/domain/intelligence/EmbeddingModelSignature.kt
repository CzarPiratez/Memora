package com.memora.app.domain.intelligence

/**
 * Stable signature for vectors stored in the embedding index (POST_MVP §7).
 *
 * Alias of [ModelVersionIdentity] — embeddings are keyed by model id + version in
 * Room (`model_id`, `model_version`). Use this name in domain and grounding
 * contracts when referring to index compatibility.
 */
typealias EmbeddingModelSignature = ModelVersionIdentity
