/*
 * Copyright (C) 2017/2021 e-voyageurs technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tock.nlp.front.shared.config

import ai.tock.nlp.core.Entity
import ai.tock.nlp.core.EntityType
import ai.tock.nlp.core.Intent
import ai.tock.nlp.core.sample.SampleContext
import ai.tock.nlp.core.sample.SampleEntity
import ai.tock.nlp.core.sample.SampleExpression
import ai.tock.nlp.front.shared.parser.ParseResult
import ai.tock.shared.security.UserLogin
import org.litote.kmongo.Id
import java.time.Instant
import java.util.Locale

/**
 * A sentence with its classification for a given [Locale] and an [ApplicationDefinition].
 */
data class ClassifiedSentence(
    /**
     * The text of the sentence.
     */
    val text: String,
    /**
     * The locale.
     */
    val language: Locale,
    /**
     * The application id.
     */
    val applicationId: Id<ApplicationDefinition>,
    /**
     * Date of creation of this sentence.
     */
    val creationDate: Instant,
    /**
     * Last update date.
     */
    val updateDate: Instant,
    /**
     * The current status of the sentence.
     */
    val status: ClassifiedSentenceStatus,
    /**
     * The current classification of the sentence.
     */
    val classification: Classification,
    /**
     * If not yet validated, the intent probability of the last evaluation.
     */
    val lastIntentProbability: Double?,
    /**
     * If not yet validated, the average entity probability of the last evaluation.
     */
    val lastEntityProbability: Double?,
    /**
     * The last usage date (for a real user) if any.
     */
    val lastUsage: Instant? = null,
    /**
     * The total number of uses of this sentence.
     */
    val usageCount: Long = 0,
    /**
     * The total number of unknown count generated by this sentence.
     */
    val unknownCount: Long = 0,
    /**
     * Tag the sentence for another person to review
     */
    val forReview: Boolean = false,
    /**
     * Comment to help the other person to review
     */
    val reviewComment: String? = null,
    /**
     * Last person that has qualified the sentence
     */
    val qualifier: UserLogin? = null,
    /**
     * Other intents with significant probabilities.
     */
    val otherIntentsProbabilities: Map<String, Double> = emptyMap()
) {

    constructor(
        result: ParseResult,
        language: Locale,
        applicationId: Id<ApplicationDefinition>,
        intentId: Id<IntentDefinition>,
        lastIntentProbability: Double,
        lastEntityProbability: Double
    ) :
        this(
            result.retainedQuery,
            language,
            applicationId,
            Instant.now(),
            Instant.now(),
            ClassifiedSentenceStatus.inbox,
            Classification(result, intentId),
            lastIntentProbability,
            lastEntityProbability,
            otherIntentsProbabilities = result.otherIntentsProbabilities
        )

    /**
     * Check if the sentence has the same content (status, creation & update dates excluded)
     */
    fun hasSameContent(sentence: ClassifiedSentence?): Boolean {
        return this == sentence?.copy(
            status = status,
            creationDate = creationDate,
            updateDate = updateDate,
            lastIntentProbability = lastIntentProbability,
            lastEntityProbability = lastEntityProbability,
            otherIntentsProbabilities = otherIntentsProbabilities
        )
    }

    /**
     * Build an expression from this sentence.
     *
     * @param intentProvider intent id -> intent provider
     * @param entityTypeProvider entity type name -> entity type provider
     */
    fun toSampleExpression(
        intentProvider: (Id<IntentDefinition>) -> Intent,
        entityTypeProvider: (String) -> EntityType?
    ): SampleExpression {
        return SampleExpression(
            text,
            intentProvider.invoke(classification.intentId),
            classification.entities.mapNotNull {
                toSampleEntity(it, entityTypeProvider)
            },
            SampleContext(language)
        )
    }

    private fun toSampleEntity(entity: ClassifiedEntity, entityTypeProvider: (String) -> EntityType?): SampleEntity? {
        return entityTypeProvider
            .invoke(entity.type)
            ?.run {
                SampleEntity(
                    Entity(this, entity.role),
                    entity.subEntities.mapNotNull { toSampleEntity(it, entityTypeProvider) },
                    entity.start,
                    entity.end
                )
            }
    }
}