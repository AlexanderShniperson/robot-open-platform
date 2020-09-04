package net.orionlab.tankclient.motorshield.pca9685

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider
import com.pi4j.io.gpio.GpioPinPwmOutput
import com.pi4j.io.gpio.Pin
import org.slf4j.LoggerFactory

class PCA9685Motor(
    val motorName: String,
    val pwmChannel: GpioPinPwmOutput?,
    val forwardChannel: GpioPinPwmOutput,
    val backwardChannel: GpioPinPwmOutput,
    var minSpeed: Int,
    var maxSpeed: Int
) {
    var motorPercentSpeed = 0
    private val motorStopTimeoutMillis = 300
    private var motorLastTimeRun = 0L
    private val log = LoggerFactory.getLogger("PCA9685Motor[$motorName]")

    var stateDirection: PCA9685MotorRunDirection = PCA9685MotorRunDirection.RunDirectionStop
        private set

    val isPwm: Boolean
        get() = pwmChannel != null

    fun stateForward(percentSpeed: Int) {
        motorLastTimeRun = System.currentTimeMillis()
        motorPercentSpeed = if (percentSpeed <= maxSpeed) percentSpeed else maxSpeed
        stateDirection = PCA9685MotorRunDirection.RunDirectionForward
    }

    fun stateBackward(percentSpeed: Int) {
        motorLastTimeRun = System.currentTimeMillis()
        motorPercentSpeed = if (percentSpeed <= maxSpeed) percentSpeed else maxSpeed
        stateDirection = PCA9685MotorRunDirection.RunDirectionBackward
    }

    fun stateStop() {
        motorLastTimeRun = System.currentTimeMillis()
        motorPercentSpeed = 0
        stateDirection = PCA9685MotorRunDirection.RunDirectionStop
    }

    fun stateNone() {
        motorPercentSpeed = 0
        stateDirection = PCA9685MotorRunDirection.RunDirectionNone
    }

    fun runMotor(gpioProvider: PCA9685GpioProvider) {
        try {
            // TODO motor drive protection when no signal send in defined time
            val pwmSpeed = if (!isMotorCanDrive()) {
                log.debug("Cannot be run in state=${stateDirection.name} because of timeout.")
                0
            } else {
                motorPercentSpeed * (PCA9685GpioProvider.PWM_STEPS / maxSpeed)
            }
            if (pwmSpeed == 0) {
                stateStop()
            }
            when (stateDirection) {
                PCA9685MotorRunDirection.RunDirectionStop -> {
                    if (isPwm) {
                        pwmChannel?.pin?.let {
                            setSafeOnOffValues(gpioProvider, it, false)
                        }
                    }
                    setSafeOnOffValues(gpioProvider, forwardChannel.pin, false)
                    setSafeOnOffValues(gpioProvider, backwardChannel.pin, false)
                    stateNone()
                }
                PCA9685MotorRunDirection.RunDirectionForward -> {
                    setSafeOnOffValues(gpioProvider, backwardChannel.pin, false)
                    setSafeOnOffValues(gpioProvider, forwardChannel.pin, true)
                    if (isPwm) {
                        pwmChannel?.pin?.let {
                            gpioProvider.setPwm(it, 0, pwmSpeed)
                        }
                    }
                }
                PCA9685MotorRunDirection.RunDirectionBackward -> {
                    setSafeOnOffValues(gpioProvider, forwardChannel.pin, false)
                    setSafeOnOffValues(gpioProvider, backwardChannel.pin, true)
                    if (isPwm) {
                        pwmChannel?.pin?.let {
                            gpioProvider.setPwm(it, 0, pwmSpeed)
                        }
                    }
                }
                else -> Unit
            }
        } catch (ex: Throwable) {
            stateStop()
            log.error("", ex)
        }
    }

    private fun setSafeOnOffValues(gpioProvider: PCA9685GpioProvider, pin: Pin, isOn: Boolean) {
        val onOffValues = gpioProvider.getPwmOnOffValues(pin)
        val onOffValue = if (isOn) 1 else 0
        if (onOffValues[0] == onOffValue && onOffValues[0] == onOffValues[1]) return
        if (isOn) {
            gpioProvider.setAlwaysOn(pin)
        } else {
            gpioProvider.setAlwaysOff(pin)
        }
    }

    private fun isMotorCanDrive() = System.currentTimeMillis() - motorLastTimeRun < motorStopTimeoutMillis

}