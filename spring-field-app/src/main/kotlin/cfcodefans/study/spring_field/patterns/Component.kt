package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.event.KeyEvent

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/component
 * Intent:
 *      The component design pattern enables developers to decouple attributes of an objects.
 *      Essentially allowing a single component ot be inheritable by multiple domains/objects
 *      without linking the objects to each other. In addition to this benefit, the component
 *      design pattern allows developer to write maintainable and comprehensible code
 *      which is less likely to result in monolithic classes
 */
object Component {
    private val log: Logger = LoggerFactory.getLogger(Component::class.java)

    /**
     * The GameObject class has three component class instances that
     * allow the creation of different game objects based on the game design requirements.
     */
    open class GameObject(val inputComponent: IInputComponent,
                          val physicComponent: IPhysicComponent,
                          val graphicComponent: IGraphicComponent,
                          val name: String) {

        var velocity: Int = 0
            private set

        private var coordinate: Int = 0

        /**
         * Updates the three components of the NPC object used in the demo in App
         * note that this is simply a duplicate of update() without the key event for demonstration purposes.
         * <p>This method is usually used in games if the player becomes inactive.
         */
        open fun demoUpdate(): GameObject = apply {
            inputComponent.update(this, 0)
            physicComponent.update(this)
            graphicComponent.update(this)
        }

        /**
         * Updates the three components for objects based on key events.
         * @param e key event from the player
         */
        open fun update(e: Int): GameObject = apply {
            inputComponent.update(this, e)
            physicComponent.update(this)
            graphicComponent.update(this)
        }

        /**
         * update the velocity based on the acceleration of the GameObjcect.
         * @param acceleration the acceleration of the GameObject
         */
        open fun updateVelocity(acceleration: Int): GameObject = apply { this.velocity += acceleration }

        /**
         * Set the c based on the current velocity.
         */
        open fun updateCoordinate(): GameObject = apply { coordinate += this.velocity }
    }

    fun interface IGraphicComponent {
        fun update(gameObj: GameObject): Unit
    }

    open class ObjectGraphicComponent : IGraphicComponent {
        override fun update(gameObj: GameObject) {
            log.info("${gameObj.name}'s current velocity: ${gameObj.velocity}")
        }
    }

    fun interface IInputComponent {
        fun update(gameObj: GameObject, e: Int): Unit
    }

    open class DemoInputComponent : IInputComponent {
        companion object {
            private const val WALK_ACCELERATION: Int = 2
        }

        override fun update(gameObj: GameObject, e: Int) {
            gameObj.updateVelocity(WALK_ACCELERATION)
            log.info("${gameObj.name} has moved right by $WALK_ACCELERATION.")
        }
    }

    open class PlayerInputComponent : IInputComponent {
        companion object {
            private const val WALK_ACCELERATION: Int = 1
        }

        override fun update(gameObj: GameObject, e: Int) {
            when (e) {
                KeyEvent.KEY_LOCATION_LEFT -> {
                    gameObj.updateVelocity(-WALK_ACCELERATION)
                    log.info("${gameObj.name} has moved left by $WALK_ACCELERATION")
                }
                KeyEvent.KEY_LOCATION_RIGHT -> {
                    gameObj.updateVelocity(WALK_ACCELERATION)
                    log.info("${gameObj.name} has moved right by $WALK_ACCELERATION")
                }
                else -> {
                    log.info("${gameObj.name}'s velocity is unchanged due to the invalid input")
                    gameObj.updateVelocity(0)
                }
            }
        }
    }

    fun interface IPhysicComponent {
        fun update(gameObj: GameObject): Unit
    }

    open class ObjectPhysicComponent : IPhysicComponent {
        override fun update(gameObj: GameObject) {
            gameObj.updateCoordinate()
            log.info("${gameObj.name}'s coordinate has been changed.")
        }
    }

    fun createPlayer(): GameObject = GameObject(inputComponent = PlayerInputComponent(),
            physicComponent = ObjectPhysicComponent(),
            graphicComponent = ObjectGraphicComponent(),
            name = "player")

    fun createNPC(): GameObject = GameObject(inputComponent = DemoInputComponent(),
            physicComponent = ObjectPhysicComponent(),
            graphicComponent = ObjectGraphicComponent(),
            name = "npc")

    @JvmStatic
    fun main(args: Array<String>) {
        val player: GameObject = createPlayer()
        val npc: GameObject = createNPC()

        log.info("Player Update: ")
        player.update(KeyEvent.KEY_LOCATION_LEFT)
        log.info("NPC update: ")
        npc.demoUpdate()
    }
}