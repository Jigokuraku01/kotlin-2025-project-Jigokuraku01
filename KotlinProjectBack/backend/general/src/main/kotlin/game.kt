@file:Suppress("ktlint:standard:filename")

package org.example

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface IGame<T : IGame.InfoForSending> {
    enum class GameState {
        SERVER_WINS,
        CLIENT_WINS,
        DRAW,
        ONGOING,
    }

    @Serializable
    sealed class InfoForSending

    fun checkWinner(): String?

    fun returnInfoSendingClass(): KSerializer<out InfoForSending>

    fun getPlayerId(name: String): String

    fun makeMove(info: InfoForSending): GameState

    fun decerializeJsonFromStringToInfoSending(input: String): InfoForSending

    fun returnClassWithCorrectInput(playerId: String): InfoForSending

    abstract class InnerLogic {
        abstract fun checkIfPosIsGood(info: SettingInfo): Boolean

        abstract fun setToPos(info: SettingInfo)

        abstract class SettingInfo
    }
}
