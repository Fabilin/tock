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

package services

import ai.tock.bot.connector.whatsapp.cloud.UserHashedIdCache
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudBotRecipientType
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.WhatsAppCloudSendBotInteractiveMessage
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotAction
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotActionButton
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotActionButtonReply
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotBody
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotHeaderType
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotInteractive
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotInteractiveHeader
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotInteractiveMessage
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotInteractiveType
import ai.tock.bot.connector.whatsapp.cloud.model.send.message.content.WhatsAppCloudBotMediaImage
import ai.tock.bot.connector.whatsapp.cloud.services.SendActionConverter
import ai.tock.bot.connector.whatsapp.cloud.services.WhatsAppCloudApiService
import ai.tock.bot.engine.action.SendSentence
import ai.tock.bot.engine.user.PlayerId
import ai.tock.shared.Executor
import ai.tock.shared.SimpleExecutor
import ai.tock.shared.tockInternalInjector
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendActionConverterTest {
    companion object {
        private val executor = spyk(SimpleExecutor(10))

        @BeforeAll
        @JvmStatic
        fun injectExecutor() {
            tockInternalInjector = KodeinInjector().apply {
                inject(
                    Kodein {
                        import(
                            Kodein.Module {
                                bind<Executor>() with singleton { executor }
                            }
                        )
                    }
                )
            }
        }

        @AfterAll
        @JvmStatic
        fun resetInjection() {
            tockInternalInjector = KodeinInjector()
        }
    }

    private val nextTaskId = AtomicInteger()

    @BeforeEach
    fun setup() {
        every { executor.executeBlocking(any()) } answers {
            // The ScheduledThreadPoolExecutor used by TestExecutor executes tasks scheduled for
            // exactly the same execution time in first-in-first-out (FIFO) order of submission,
            // so we fuzz the execution time to ensure parallel execution
            executor.executeBlocking(Duration.ofMillis(Random.nextLong(10))) {
                firstArg<() -> Unit>()()
            }
        }
    }

    @AfterEach
    fun teardown() {
        clearMocks(executor)
    }

    @Test
    fun `message conversion happens on executor in parallel`() {
        mockkObject(UserHashedIdCache)
        val userId = "4567876543"
        every { UserHashedIdCache.getRealId(userId) } returns userId
        val whatsAppCloudApiService = mockk<WhatsAppCloudApiService> {
            every { getUploadedImageId("fish.png") } answers {
                println("Getting uploaded image id on thread ${Thread.currentThread().name} at ${Instant.now()}")
                Thread.sleep(1000)
                println("Done uploading image id at ${Instant.now()}")
                "test-image-id"
            }
            every { shortenPayload("button1") } answers {
                println("Shortening payload 1 on thread ${Thread.currentThread().name} at ${Instant.now()}")
                Thread.sleep(2000)
                println("Done shortening payload 1 at ${Instant.now()}")
                "button1id"
            }
            every { shortenPayload("button2") } answers {
                println("Shortening payload 2 on thread ${Thread.currentThread().name} at ${Instant.now()}")
                Thread.sleep(500)
                println("Done shortening payload 2 at ${Instant.now()}")
                "button2id"
            }
        }

        val result = SendActionConverter.toBotMessage(
            whatsAppCloudApiService, SendSentence(
                PlayerId("test-user"), "test", PlayerId(userId), text = null, messages = mutableListOf(
                    WhatsAppCloudBotInteractiveMessage(
                        recipientType = WhatsAppCloudBotRecipientType.individual,
                        interactive = WhatsAppCloudBotInteractive(
                            type = WhatsAppCloudBotInteractiveType.button,
                            header = WhatsAppCloudBotInteractiveHeader(
                                WhatsAppCloudBotHeaderType.image,
                                image = WhatsAppCloudBotMediaImage("fish.png")
                            ),
                            body = WhatsAppCloudBotBody("test body"),
                            action = WhatsAppCloudBotAction(
                                buttons = listOf(
                                    WhatsAppCloudBotActionButton(
                                        reply = WhatsAppCloudBotActionButtonReply(
                                            "Button 1",
                                            "button1"
                                        )
                                    ),
                                    WhatsAppCloudBotActionButton(
                                        reply = WhatsAppCloudBotActionButtonReply(
                                            "Button 2",
                                            "button2"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        verify(atLeast = 3) {
            executor.executeBlocking(any())
        }
        assertEquals(WhatsAppCloudSendBotInteractiveMessage(
            interactive = WhatsAppCloudBotInteractive(
                type = WhatsAppCloudBotInteractiveType.button,
                header = WhatsAppCloudBotInteractiveHeader(
                    WhatsAppCloudBotHeaderType.image,
                    image = WhatsAppCloudBotMediaImage("test-image-id")
                ),
                body = WhatsAppCloudBotBody("test body"),
                action = WhatsAppCloudBotAction(
                    buttons = listOf(
                        WhatsAppCloudBotActionButton(
                            reply = WhatsAppCloudBotActionButtonReply(
                                "Button 1",
                                "button1id"
                            )
                        ),
                        WhatsAppCloudBotActionButton(
                            reply = WhatsAppCloudBotActionButtonReply(
                                "Button 2",
                                "button2id"
                            )
                        )
                    )
                )
            ),
            recipientType = WhatsAppCloudBotRecipientType.individual,
            to = userId,
        ), result)
    }
}
