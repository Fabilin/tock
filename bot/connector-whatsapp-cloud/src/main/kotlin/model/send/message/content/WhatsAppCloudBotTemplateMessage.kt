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

package ai.tock.bot.connector.whatsapp.cloud.model.send.message.content

import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudBotMessage
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudBotMessageType
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudBotRecipientType
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudSendBotMessage
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudSendBotTemplateMessage
import ai.tock.bot.connector.whatsapp.cloud.services.WhatsAppCloudApiService
import ai.tock.bot.engine.message.GenericMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class WhatsAppCloudBotTemplateMessage(
    val template: WhatsAppCloudBotTemplate,
    override val recipientType: WhatsAppCloudBotRecipientType,
    override val userId: String? = null,
) : WhatsAppCloudBotMessage(WhatsAppCloudBotMessageType.template, userId) {
    override suspend fun prepareMessage(apiService: WhatsAppCloudApiService, recipientId: String): WhatsAppCloudSendBotMessage {
        val updatedComponents = coroutineScope {
            template.components.map { component ->
                async {
                    when (component) {
                        is WhatsappTemplateComponent.Carousel -> prepareCarousel(apiService, component)
                        is WhatsappTemplateComponent.Button -> updateButtonPayloads(apiService, component)
                        else -> component
                    }
                }
            }.awaitAll()
        }

        return WhatsAppCloudSendBotTemplateMessage(
            template.copy(components = updatedComponents),
            recipientType,
            recipientId
        )
    }

    private suspend fun prepareCarousel(
        apiService: WhatsAppCloudApiService,
        carousel: WhatsappTemplateComponent.Carousel
    ): WhatsappTemplateComponent.Carousel = coroutineScope {
        val updatedCards = carousel.cards.map { card ->
            async {
                val updatedComponents = card.components.map { component ->
                    async {
                        when (component) {
                            is WhatsappTemplateComponent.Button -> updateButtonPayloads(apiService, component)
                            is WhatsappTemplateComponent.Header -> updateCardHeader(apiService, component)
                            else -> component
                        }
                    }
                }.awaitAll()
                card.copy(components = updatedComponents)
            }
        }.awaitAll()
        carousel.copy(cards = updatedCards)
    }

    private suspend fun updateCardHeader(
        apiService: WhatsAppCloudApiService,
        header: WhatsappTemplateComponent.Header
    ): WhatsappTemplateComponent {
        val resolvedParameters = coroutineScope {
            header.parameters.map { param ->
                async { prepareHeaderParameter(param, apiService) }
            }.awaitAll()
        }
        return header.copy(parameters = resolvedParameters)
    }

    private fun prepareHeaderParameter(
        param: HeaderParameter,
        apiService: WhatsAppCloudApiService
    ): HeaderParameter = (param as? HeaderParameter.Image)?.image?.id?.let { id ->
        HeaderParameter.Image(param.type, ImageId(apiService.getUploadedImageId(id)))
    } ?: param

    private fun updateButtonPayloads(apiService: WhatsAppCloudApiService, button: WhatsappTemplateComponent.Button): WhatsappTemplateComponent.Button =
        button.copy(parameters = button.parameters.map(apiService::shortenPayload))

    override fun toGenericMessage(): GenericMessage =
        GenericMessage(
            texts = mapOf(GenericMessage.TEXT_PARAM to "template"),
        )
}
