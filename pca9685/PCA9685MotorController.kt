package net.orionlab.tankclient.motorshield.pca9685

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider
import com.pi4j.gpio.extension.pca.PCA9685Pin
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinPwmOutput
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CFactory
import org.slf4j.LoggerFactory
import java.util.*

class PCA9685MotorController {
    companion object {
        const val MOTOR_CHASSIS_LEFT = 0
        const val MOTOR_CHASSIS_RIGHT = 1
        const val MOTOR_TOWER_LEFT_RIGHT = 2
        const val MOTOR_WEAPON_UP_DOWN = 3
        const val MOTOR_WEAPON_SHOOT = 4
    }

    private val log = LoggerFactory.getLogger("PCA9685MotorController")
    private var gpioProvider: PCA9685GpioProvider? = null
    private val repeatedTask: TimerTask
    private val motors: Array<PCA9685Motor?> = arrayOfNulls(5)
    private var timer: Timer? = null

    init {
        repeatedTask = object : TimerTask() {
            override fun run() {
                for (i in motors.indices) {
                    this@PCA9685MotorController.runMotorByTicker(i)
                }
            }
        }
        try {
            val bus = I2CFactory.getInstance(I2CBus.BUS_1)
            gpioProvider = PCA9685GpioProvider(bus, 0x40, PCA9685GpioProvider.MIN_FREQUENCY)
            val myOutputs = provisionPwmOutputs()
            gpioProvider?.reset()
            motors[MOTOR_CHASSIS_LEFT] =
                PCA9685Motor("LeftChassis", myOutputs[1], myOutputs[0], myOutputs[2], 0, 100)
            motors[MOTOR_CHASSIS_RIGHT] =
                PCA9685Motor("RightChassis", myOutputs[4], myOutputs[3], myOutputs[5], 0, 100)
            motors[MOTOR_TOWER_LEFT_RIGHT] =
                PCA9685Motor("TowerLeftRight", myOutputs[7], myOutputs[6], myOutputs[8], 0, 100)
            motors[MOTOR_WEAPON_UP_DOWN] =
                PCA9685Motor("WeaponUpDown", myOutputs[10], myOutputs[9], myOutputs[11], 0, 100)
            motors[MOTOR_WEAPON_SHOOT] =
                PCA9685Motor("WeaponShoot", myOutputs[13], myOutputs[12], myOutputs[14], 0, 100)
        } catch (ex: Throwable) {
            log.error("", ex)
        }
    }

    fun start() {
        timer?.cancel()
        timer = Timer("Timer to drive motors")
        timer?.scheduleAtFixedRate(repeatedTask, 50L, 50L)
    }

    fun stop() {
        stopAllMotors()
        timer?.cancel()
        timer = null
        gpioProvider?.reset()
    }

    fun close() {
        try {
            stop()
            gpioProvider?.shutdown()
            gpioProvider = null
        } catch (ex: Throwable) {
            log.error("", ex)
        }
    }


    fun runMotor(motorIndex: Int, direction: PCA9685MotorRunDirection, speedPercent: Int) {
        val motor = motors[motorIndex]
        when (direction) {
            PCA9685MotorRunDirection.RunDirectionForward -> motor?.stateForward(speedPercent)
            PCA9685MotorRunDirection.RunDirectionBackward -> motor?.stateBackward(speedPercent)
            PCA9685MotorRunDirection.RunDirectionStop -> motor?.stateStop()
            else -> Unit
        }
    }

    private fun runMotorByTicker(motorIndex: Int) {
        gpioProvider?.let {
            motors[motorIndex]?.runMotor(it)
        }
    }

    fun stopMotor(motorIndex: Int) {
        motors[motorIndex]?.let { motor ->
            motor.stateStop()
            gpioProvider?.let {
                motor.runMotor(it)
            }
        }
    }

    private fun stopAllMotors() {
        for (i in motors.indices) {
            stopMotor(i)
        }
    }

    private fun provisionPwmOutputs(): Array<GpioPinPwmOutput> {
        val gpio = GpioFactory.getInstance()
        return arrayOf(
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_00, "PCA9685Motor 1 EN_A"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_01, "PCA9685Motor 1 PWM"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_02, "PCA9685Motor 1 EN_B"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_03, "PCA9685Motor 2 EN_A"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_04, "PCA9685Motor 2 PWM"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_05, "PCA9685Motor 2 EN_B"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_06, "PCA9685Motor 3 EN_A"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_07, "PCA9685Motor 3 PWM"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_08, "PCA9685Motor 3 EN_B"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_09, "PCA9685Motor 4 EN_A"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_10, "PCA9685Motor 4 PWM"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_11, "PCA9685Motor 4 EN_B"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_12, "PCA9685Motor 5 EN_A"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_13, "PCA9685Motor 5 PWM"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_14, "PCA9685Motor 5 EN_B"),
            gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_15, "N/A")
        )
    }
}
