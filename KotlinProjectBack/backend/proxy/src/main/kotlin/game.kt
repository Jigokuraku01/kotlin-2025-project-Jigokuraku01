package org.example

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass


sealed interface game<T : game.InfoForSending> {
    enum class GameState{
        SERVER_WINS, CLIENT_WINS, DRAW, ONGOING
    }
    fun printField()

    @Serializable
    sealed class InfoForSending

    fun returnInfoSendingClass(): KSerializer<out InfoForSending>
    fun getPlayerId(name: String):String
    fun makeMove(info: InfoForSending): GameState

    fun returnClassWithCorrectInput(playerId: String): InfoForSending
    abstract class InnerLogic {
        abstract fun checkIfPosIsGood(info: SettingInfo): Boolean
        abstract fun setToPos(info: SettingInfo)
        abstract class SettingInfo
    }
}