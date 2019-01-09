/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;

import com.disnodeteam.dogecv.detectors.roverrukus.GoldAlignDetector;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import net.frogbots.ftcopmodetunercommon.opmode.TunableLinearOpMode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.util.concurrent.TimeUnit;


@Autonomous(name = "linearUnitAlignV3(PID)", group = "1v1 me rust")
public class linearUnitAlignPID extends TunableLinearOpMode {
    // Declare OpMode members.
    //infrastructure vars
    private ElapsedTime runtime = new ElapsedTime();
    Robot testbot;
    //Servo markerServo;
    //DcMotor liftMotor;
    BNO055IMU imu; //declare imu
    private DistanceSensor rightRangeSensor;//declare range sensor
    private DistanceSensor leftRangeSensor;
    private DistanceSensor frontRangeSensor;

    //DogeCV
    private GoldAlignDetector goldAlignDetector;
    //Enum with positions for the gold mineral
    enum GoldPosition{
        RIGHT,
        LEFT,
        CENTER
    }
    //Variables used to do encoder driving
    double radius = 3;//inches
    //convert inches to ticks
    public final double inchesToTicks = (560/(2*3.14159*radius));//560 ticks per rev for neverest 20s



    //non infrastructure vars (easily editable, will be edited)
    linearAutoIntegrate.GoldPosition goldLoc = linearAutoIntegrate.GoldPosition.CENTER;

    //position at which the servo holds the idol/marker
    double servoHoldPos;//unknown now

    //position at which the servo drops the idol/marker
    double servoDropPos;//unknown now

    //distance in from the center of robot
    final double forwardOffset = 2;//measure on bot

    //distance from front of bot to wall
    double wallDist;

    //distance to leave in order to turn and not catch on wall
    double turnDist = 2;

    //ticks to lower the robot.
    int liftTicksLower = -13532;//what it was on jankbot

    //inches to drive forward initially just to get away from the lander
    double driveOffLift = 6;

    //degree position before the align with the cube
    double rotBeforeAlign = 0;
    //degree position after the align with the cube
    double rotAfterAlign;
    //theta for sample field -- this is used in our path
    double thetaToSample;

    //use pythag and such to find length of path to the left and right minerals.
    //finding a and b in right tri
    double inchesToCenterMineral = 35;
    double inchesBetweenMinerals = 14.5;
    //finding C in right tri
    double wiggleRoom = 0;
    double inchesToSideMinerals = Math.sqrt(inchesToCenterMineral*inchesToCenterMineral+inchesBetweenMinerals*inchesBetweenMinerals)+wiggleRoom;

    //we want to drive a little more than we really need for the minerals.
    //in inches
    double overshootSides = 2;
    double overshootCenter;//this is unknown as of now because you need theta

    //drive after hitting the center mineral
    double afterCenter = 10;
    double afterSideMineralDist;
    double afterMineralTurn;
    double backUpinches = -20;

    private double maxPower = 0.25; //power of the robot
    private double minPower = 0.09; //least amount of power the robot can have
    double turnPowerNormal = 0.3;
    double turnPowerMin = 0.25;






    //important vars for pid align
    double timePrev = 0;
    double timeCurr = 0;
    double timeDelta = 0;
    double error;
    double pControl;
    double pGain = 0.068;//should be tuned

    double iControl;
    double iGain = 0.0005;//should be tuned

    double controllerOut;


    @Override
    public void runOpMode() {
        //Init button pressed.

        telemetry.addData("Status", "Started Init");
        telemetry.update();
        /*//Init Servo
        markerServo = hardwareMap.get(Servo.class, "idol");
        markerServo.setPosition(servoHoldPos);//set to straight up.*/

        //Init robot
        testbot = new Robot(hardwareMap);
        //Init wheels
        testbot.InitializeMotors();
        //braking,
        testbot.enableBraking();

        /*//Init Lift
        liftMotor = hardwareMap.dcMotor.get("LIFT");
        liftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        //Set lift:
        liftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);*/

        //Init distance
        rightRangeSensor = hardwareMap.get(DistanceSensor.class, "rightRangeSensor");
        leftRangeSensor = hardwareMap.get(DistanceSensor.class,"leftRangeSensor");

        // Initialize things here.

        // Wait for the game to start (driver presses PLAY)
        waitForStart();
        //align();
        // run until the end of the match (driver presses STOP)

        runtime.reset();
        while (opModeIsActive()) {
            //pGain = getDouble("pGain");
            //iGain = getDouble("iGain");
            alignPid();
            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.update();
        }
        testbot.TankDrive(0,0);
    }
    private void alignPid(){

        error = getDeltaRange();

        timePrev = timeCurr;
        timeCurr = runtime.time(TimeUnit.SECONDS);
        //this and timeprev implementation are new
        timeDelta = timeCurr-timeDelta;

        pControl = error*pGain;
        //this is new
        iControl = error*iGain*timeDelta + iControl;// add to integrate

        controllerOut = pControl+iControl;
        if(Math.abs(controllerOut) > maxPower)controllerOut=maxPower*getSign(controllerOut);
        testbot.TankDrive(controllerOut,-controllerOut);

        telemetry.addData("delta",error);
        telemetry.addData("leftsensor",leftRangeSensor.getDistance(DistanceUnit.CM));
        telemetry.addData("rightsensor",rightRangeSensor.getDistance(DistanceUnit.CM));
        telemetry.addData("pGain", pGain);
        telemetry.addData("iGain", iGain);
        telemetry.addData("icontrol", iControl);
        telemetry.addData("controllerOut", controllerOut);
    }
    private double getSign(double s){
        return Math.abs(s)/s;
    }
    private void align() {

        double accuracy = 1; //how accurate this should be to the CM

        double deltaRange = getDeltaRange();// gets the change in distance between the 2 sensors      +1 because inaccurate


        while (Math.ceil(Math.abs(deltaRange)) > accuracy && !isStopRequested()) //while the delta is greater than the accuracy we want
        {

            telemetry.addData("Status: ", "Aligning");
            deltaRange = getDeltaRange();
            telemetry.addData("Delta Range", deltaRange);
            telemetry.update();
            if (deltaRange < 0) //if its negative
            {
                testbot.TankDrive(turnPowerMin, -turnPowerMin);//turn left

            } else if (deltaRange > 0) //if the delta range is positive
            {
                testbot.TankDrive(-turnPowerMin, turnPowerMin);//turn right
            }
        }
        testbot.TankDrive(0,0); //stops
    }
    private double getDeltaRange() {
        double[] distances = getDistances(); //gets the distances
        return distances[0] - distances[1]; //subtracts the 2
    }
    private double[] getDistances() {
        double leftDistance = leftRangeSensor.getDistance(DistanceUnit.CM); //left sensor distance
        double rightDistance = rightRangeSensor.getDistance(DistanceUnit.CM); //right sensor distance
        double[] distances = new double[2]; //makes an array
        distances[0] = leftDistance; //sets positions to values
        distances[1] = rightDistance;
        return distances; //returns the array
    }

    //Variables from AutoGabo
    private double globalAngle; //the number of degrees the robot has turned
    Orientation lastAngles = new Orientation(); //sets the last angle to whatever the robot last had. This is just to avoid errors
    //MediaPlayer mediaPlayer = MediaPlayer.create(hardwareMap.appContext, R.raw.undertale);

    private void rotate(double degrees, double maxPower) {
        //I changed degrees to a double because that's what the imu gives and
        //I didn't see a reason not to.
        // Sean 12/11/18

        telemetry.addData("Rotating", true); //informs
        telemetry.update();

        resetAngle(); //sets starting angle and resets the amount turned to 0

        // getAngle() returns + when rotating counter clockwise (left) and - when rotating clockwise (right).
        double thingy = degrees * degrees * degrees;
        double slope = -maxPower / thingy; //gets the slope of the graph that is needed to make y = 0 when totalNeeded to travel is x

        // rotate until turn is completed.
        if (degrees < 0) {
            // On right turn we have to get off zero first.
            while (!isStopRequested() && getAngle() == 0) {
                double currentAngle = getAngle();
                double thingy1 = currentAngle * currentAngle * currentAngle;
                double newPower = slope * thingy1 + maxPower; // the power is the x value in that position
                if (newPower < turnPowerMin) newPower = turnPowerMin;
                if (newPower <= 0) newPower = 0;
                telemetry.addData("Power: ", -newPower);
                telemetry.update();
                testbot.TankDrive(-newPower, newPower);
            }

            while (!isStopRequested() && getAngle() > degrees) {
                double currentAngle = getAngle();
                double thingy3 = currentAngle * currentAngle * currentAngle;
                double newPower = slope * thingy3 + maxPower; // the power is the x value in that position
                if (newPower < turnPowerMin) newPower = turnPowerMin;
                if (newPower <= 0) newPower = 0;
                telemetry.addData("Power: ", -newPower);
                telemetry.update();
                testbot.TankDrive(-newPower, newPower);
            } //once it starts turning slightly more than it should.
        } else {
            // left turn.
            while (!isStopRequested() && getAngle() < degrees) {
                double currentAngle = getAngle();
                double thingy2 = currentAngle * currentAngle * currentAngle;
                double newPower = slope * thingy2 + maxPower; // the power is the x value in that position
                if (newPower < turnPowerMin) newPower = turnPowerMin;
                if (newPower <= 0) newPower = 0;
                telemetry.addData("Power: ", newPower);
                telemetry.update();
                testbot.TankDrive(newPower, -newPower);
            }
        }

        // turn the motors off.
        testbot.TankDrive(0, 0);

        // wait for rotation to stop.
        //sleep(1000);

        // reset angle tracking on new heading.
        //resetAngle();
    }
    private void resetAngle() {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES); //sets lastAngles to current angles

        globalAngle = 0; //global angle is set to 0
    }
    private double getAngle()
    {
        // We experimentally determined the Z axis is the axis we want to use for heading angle.
        // We have to process the angle because the imu works in euler angles so the Z axis is
        // returned as 0 to +180 or 0 to -180 rolling back to -179 or +179 when rotation passes
        // 180 degrees. We detect this transition and track the total cumulative angle of rotation.

        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES); //gets the angle

        double deltaAngle = angles.firstAngle - lastAngles.firstAngle; //deltaAngle is the first angle - the lastangle it got

        if (deltaAngle < -180) //switches it to use 0 to 360
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        globalAngle += deltaAngle; //adds the deltaAngle to the globalAngle

        lastAngles = angles; //lastAngle is the anlges

        return globalAngle; //returns the amount turned
    }
}
