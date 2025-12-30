package dev.cypdashuhn.cbg.commands

import dev.cypdashuhn.rooster.localization.tSend
import dev.jorel.commandapi.arguments.CustomArgument
import org.bukkit.command.CommandSender

fun validateFileName(name: String, sender: CommandSender): String {
    val invalidChars = listOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    val reservedNames = listOf(
        "CON",
        "PRN",
        "AUX",
        "NUL",
        "COM1",
        "COM2",
        "COM3",
        "COM4",
        "COM5",
        "COM6",
        "COM7",
        "COM8",
        "COM9",
        "LPT1",
        "LPT2",
        "LPT3",
        "LPT4",
        "LPT5",
        "LPT6",
        "LPT7",
        "LPT8",
        "LPT9"
    )

    if (name in reservedNames) {
        sender.tSend("reserved_name_not_allowed", "name" to name)
        throw CustomArgument.CustomArgumentException.fromMessageBuilder(
            CustomArgument.MessageBuilder("Reserved name not allowed: ").appendArgInput()
        )
    }

    if (name.endsWith('.')) {
        sender.tSend("name_ends_with_dot", "name" to name)
        throw CustomArgument.CustomArgumentException.fromMessageBuilder(
            CustomArgument.MessageBuilder("Name cannot end with a dot: ").appendArgInput()
        )
    }

    name.forEach { char ->
        if (char in invalidChars) {
            sender.tSend("invalid_character_in_name", "char" to char.toString())
            throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                CustomArgument.MessageBuilder("Invalid character '$char' in name: ").appendArgInput()
            )
        }
    }

    return name
}
