package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ThreadLocalRandom

object ByteCode {
    private val log: Logger = LoggerFactory.getLogger(ByteCode::class.java)

    open class Wizard(var health: Int,
                      var agility: Int,
                      var wisdom: Int,
                      var numberOfPlayedSounds: Int = 0,
                      var numberOfSpawnParticles: Int = 0) {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(Wizard::class.java)
        }

        open fun playSound(): Unit {
            log.info("Playing sound")
            numberOfPlayedSounds++
        }

        open fun spawnParticles(): Unit {
            log.info("Spawning particles")
            numberOfSpawnParticles++
        }
    }

    enum class Instruction(val intVal: Int) {
        LITERAL(1),     //e.g "LITERAL 0", push 0 to stack
        SET_HEALTH(2),  //e.g "SET_HEALTH", pop health and wizard number, call set health
        SET_WISDOM(3),  //e.g "SET_WISDOM", pop wisdom and wizard number, call set wisdom
        SET_AGILITY(4), //e.g "SET_AGILITY", pop agility and wizard number, call set agility
        PLAY_SOUND(5),  //e.g "PLAY_SOUND", pop value as wizard number, call play sound
        SPAWN_PARTICLES(6), //e.g "SPAWN_PARTICLES", pop value as wizard number, call spawn particles
        GET_HEALTH(7),      // e.g. "GET_HEALTH", pop value as wizard number, push wizard's health
        GET_AGILITY(8),     // e.g. "GET_AGILITY", pop value as wizard number, push wizard's agility
        GET_WISDOM(9),      // e.g. "GET_WISDOM", pop value as wizard number, push wizard's wisdom
        ADD(10),            // e.g. "ADD", pop 2 values, push their sum
        DIVIDE(11);         // e.g. "DIVIDE", pop 2 values, push their division

        companion object {
            operator fun get(value: Int): Instruction = entries
                .firstOrNull { v -> v.intVal == value }
                ?: throw IllegalArgumentException("Invalid instruction value: $value")
        }

    }

    private fun randomInt(min: Int, max: Int): Int = ThreadLocalRandom.current().nextInt(min, max + 1)

    open class VirtualMachine {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(VirtualMachine::class.java)
        }

        lateinit var wizards: Array<Wizard>
        private val stack: Stack<Int> = Stack()

        constructor() {
            wizards = arrayOf(
                    Wizard(health = randomInt(3, 32), agility = randomInt(3, 32), wisdom = randomInt(3, 32)),
                    Wizard(health = randomInt(3, 32), agility = randomInt(3, 32), wisdom = randomInt(3, 32)),
            )
        }

        constructor(wizard1: Wizard, wizard2: Wizard) {
            wizards = arrayOf(wizard1, wizard2)
        }

        /**
         * Executes provided bytecode
         */
        open fun execute(bytecodes: IntArray) {
            var i: Int = 0; while (i < bytecodes.size) {
                val ins: Instruction = Instruction.get(bytecodes[i])
                when (ins) {
                    Instruction.LITERAL -> {
                        // Read the next byte from the bytecode
                        val value: Int = bytecodes[++i]
                        // Push the next value to stack
                        stack.push(value)
                    }
                    Instruction.SET_HEALTH -> {
                        val amount = stack.pop()
                        val wizard = stack.pop()
                        setHealth(wizard, amount)
                    }
                    Instruction.SET_WISDOM -> {
                        val amount = stack.pop()
                        val wizard = stack.pop()
                        setWisdom(wizard, amount)
                    }
                    Instruction.SET_AGILITY -> {
                        val amount = stack.pop()
                        val wizard = stack.pop()
                        setAgility(wizard, amount)
                    }
                    Instruction.PLAY_SOUND -> {
                        val wizard = stack.pop()
                        wizards[wizard].playSound()
                    }
                    Instruction.SPAWN_PARTICLES -> {
                        val wizard = stack.pop()
                        wizards[wizard].spawnParticles()
                    }
                    Instruction.GET_HEALTH -> {
                        val wizard = stack.pop()
                        stack.push(getHealth(wizard))
                    }
                    Instruction.GET_AGILITY -> {
                        val wizard = stack.pop()
                        stack.push(getAgility(wizard))
                    }
                    Instruction.GET_WISDOM -> {
                        val wizard = stack.pop()
                        stack.push(getWisdom(wizard))
                    }
                    Instruction.ADD -> {
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(a + b)
                    }
                    Instruction.DIVIDE -> {
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(b / a)
                    }
                }
                log.info("Executed ${ins.name}, Stack contains $stack")
                i++
            }
        }

        fun setHealth(wizard: Int, amount: Int) {
            wizards[wizard].health = amount
        }

        fun setWisdom(wizard: Int, amount: Int) {
            wizards[wizard].wisdom = amount
        }

        fun setAgility(wizard: Int, amount: Int) {
            wizards[wizard].agility = amount
        }

        fun getHealth(wizard: Int): Int = wizards[wizard].health

        fun getWisdom(wizard: Int): Int = wizards[wizard].wisdom

        fun getAgility(wizard: Int): Int = wizards[wizard].agility
    }

    /**
     * Converts instructions represented as String.
     *
     * @param instructions to convert
     * @return array of int representing bytecode
     */
    fun convertToByteCode(instructions: String?): IntArray {
        if (instructions == null || instructions.trim().isEmpty()) {
            return IntArray(0)
        }

        return instructions
            .trim()
            .split(" ".toRegex())
            .dropLastWhile { it.isEmpty() }
            .map { instructionStr ->
                if (isValidInstruction(instructionStr)) Instruction.valueOf(instructionStr).intVal
                else if (isValidInt(instructionStr)) instructionStr.toInt()
                else throw IllegalArgumentException("Invalid instruction or number: ${instructionStr}")
            }.toIntArray()
    }

    private fun isValidInstruction(instruction: String): Boolean = try {
        Instruction.valueOf(instruction)
        true
    } catch (e: java.lang.IllegalArgumentException) {
        false
    }

    private fun isValidInt(value: String): Boolean = try {
        value.toInt()
        true
    } catch (e: NumberFormatException) {
        false
    }

    private const val LITERAL_0 = "LITERAL 0"
    private const val HEALTH_PATTERN = "%s_HEALTH"
    private const val GET_AGILITY = "GET_AGILITY"
    private const val GET_WISDOM = "GET_WISDOM"
    private const val ADD = "ADD"
    private const val LITERAL_2 = "LITERAL 2"
    private const val DIVIDE = "DIVIDE"

    @JvmStatic
    fun main(args: Array<String>) {
        val vm = VirtualMachine(
                Wizard(45, 7, 11, 0, 0),
                Wizard(36, 18, 8, 0, 0))

        vm.execute(convertToByteCode(LITERAL_0));
        vm.execute(convertToByteCode(LITERAL_0));
        vm.execute(convertToByteCode(String.format(HEALTH_PATTERN, "GET")));
        vm.execute(convertToByteCode(LITERAL_0));
        vm.execute(convertToByteCode(GET_AGILITY));
        vm.execute(convertToByteCode(LITERAL_0));
        vm.execute(convertToByteCode(GET_WISDOM));
        vm.execute(convertToByteCode(ADD));
        vm.execute(convertToByteCode(LITERAL_2));
        vm.execute(convertToByteCode(DIVIDE));
        vm.execute(convertToByteCode(ADD));
        vm.execute(convertToByteCode(String.format(HEALTH_PATTERN, "SET")));
    }
}