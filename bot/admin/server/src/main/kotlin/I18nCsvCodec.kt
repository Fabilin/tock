/*
 * Copyright (C) 2017/2020 e-voyageurs technologies
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

package ai.tock.bot.admin

import ai.tock.bot.admin.model.I18LabelQuery
import ai.tock.nlp.admin.CsvCodec
import ai.tock.nlp.admin.CsvCodec.csvFormat
import ai.tock.shared.error
import ai.tock.shared.injector
import ai.tock.translator.I18nDAO
import ai.tock.translator.I18nLabel
import ai.tock.translator.I18nLocalizedLabel
import ai.tock.translator.UserInterfaceType
import com.github.salomonbrys.kodein.instance
import mu.KotlinLogging
import org.litote.kmongo.toId
import java.io.StringReader
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

/**
 *
 */
object I18nCsvCodec {

    private val logger = KotlinLogging.logger {}

    private val i18nDAO: I18nDAO by injector.instance()

    fun exportCsv(namespace: String, query: I18LabelQuery? = null): String {
        val sb = StringBuilder()
        val printer = CsvCodec.newPrinter(sb)
        printer.printRecord(
            "Label",
            "Category",
            "Language",
            "Interface",
            "Id",
            "Validated",
            "Connector",
            "Alternatives"
        )
        i18nDAO.getLabels(namespace, query?.toI18nLabelFilter())
            .forEach { label ->
                label.i18n.forEach { localizedLabel ->
                    printer.printRecord(
                        *(listOf(
                            localizedLabel.label,
                            label.category,
                            localizedLabel.locale.language,
                            localizedLabel.interfaceType,
                            label._id,
                            localizedLabel.validated,
                            localizedLabel.connectorId ?: ""
                        ) + localizedLabel.alternatives).toTypedArray()
                    )
                }
            }
        return sb.toString()
    }

    fun importCsv(namespace: String, content: String): Boolean {
        return try {
            csvFormat()
                .parse(StringReader(content))
                .records
                .mapIndexedNotNull { i, it ->
                    if (i == 0) {
                        null
                    } else {
                        I18nLabel(
                            it[4].toId(),
                            namespace,
                            it[1],
                            LinkedHashSet(
                                listOf(
                                    I18nLocalizedLabel(
                                        Locale(it[2]),
                                        UserInterfaceType.valueOf(it[3]),
                                        it[0],
                                        it[5]?.toBoolean() ?: false,
                                        it[6].run { if (isBlank()) null else this },
                                        if (it.size() < 7) emptyList() else (7 until it.size()).mapNotNull { index -> if (it[index].isNullOrBlank()) null else it[index] }
                                    ))
                            )
                        )
                    }
                }
                .filter { it.i18n.any { it.validated } }
                .groupBy { it._id }
                .map { (key, value) ->
                    value
                        .first()
                        .run {
                            val localized = value.flatMap { it.i18n }
                            copy(
                                i18n = LinkedHashSet(localized +
                                        (i18nDAO.getLabelById(key)
                                            ?.i18n
                                            ?.filter { old ->
                                                localized.none {
                                                    old.locale == it.locale && old.interfaceType == it.interfaceType && old.connectorId == it.connectorId
                                                }
                                            }
                                            ?: emptyList())
                                )
                            )
                        }
                }
                .forEach {
                    logger.info { "Save $it" }
                    i18nDAO.save(it)
                }
            true
        } catch (t: Throwable) {
            logger.error(t)
            false
        }
    }
}
