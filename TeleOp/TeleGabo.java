package org.firstinspires.ftc.robotcontroller.internal;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

//Created by Gabo on 10/9/18

public class TeleGabo extends OpMode {
    public HardwareMap hwMap; //Declare variables
    public DcMotor BLM;
    public DcMotor BRM;
    public DcMotor FRM;
    public DcMotor FLM;

    ModernRoboticsI2cRangeSensor rangeSensor; //Declares the range sensor


    public double power = 0.5; //declares the power
    public double oldPower; //declares oldPower
    public boolean fullPower = false; //fullPower is set to false at first
    public boolean noPower = false; //no power is false

    public void init() //runs when you press init on phone
    {
        hwMap = hardwareMap; //sets the hardware map to hwMap

        BLM = hwMap.dcMotor.get("BLM"); //gets the motors
        BRM = hwMap.dcMotor.get("BRM");
        FRM = hwMap.dcMotor.get("FRM");
        FLM = hwMap.dcMotor.get("FLM");

        rangeSensor = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "RS"); //gets range sensor

    }

    public void loop() //runs after you press play
    {

    if (gamepad1.right_bumper) //if the right bumber is pressed
    {
        while (gamepad1.right_bumper && power <= 1) ; //while its pressed and power is less than or equal to 1

        power += 0.1; //increase power by 0.1

        if (power >= 1) power = 1; //if the power is above 1 then just set it to 1
    }

        if (gamepad1.left_bumper) //if left bumber is pressed
        {
            while (gamepad1.left_bumper && power >= 0) ; //while its pressed and power is greater than -1

                power -= 0.1; //decreases power by 0.1
            if (power <= 0) power = 0; //if the power is below 0 just set it to 0. we don't want negative power
        }



        if (gamepad1.left_trigger == 1) { //if the left trigger is fully down
            if (!noPower) oldPower = power; //if its put down but noPower is false it sets the oldPower to the current power
            noPower = true; //sets no power to true
        } else if (noPower) //if its not put down but no power is true
        {
            power = oldPower; //sets power back to the way it was
            noPower = false; //no power is back to false
        }

        if (gamepad1.right_trigger == 1) //if right trigger is down
        {
            if (!fullPower) oldPower = power; //if fullpower is false sets the oldpower to power
            fullPower = true; //sets full power to true
        } else if (fullPower){ //if fullpower is true and trigger is not down
            power = oldPower; //sets power to back to normal
            fullPower = false; //full power is false
        }


        double drive = gamepad1.left_stick_y; //drive is y value
        double turn = gamepad1.left_stick_x; //turn is x value
        if (fullPower) power = 1; //if fullPower is true sets power to 1
        if (noPower) power = 0; //if no power is true then set power to 0
        double leftPower = (-drive + turn)* -power; //left power is negative drive plus turn * negative power
        //straight is left positive and right negative
        double rightPower = (-drive - turn)* power; //same but positive power

        move(leftPower, rightPower); //moves robot with the power


        telemetry.addData("Power to left motor ", leftPower); //informs leftPower
        telemetry.addData("Power to right motor ", rightPower); //informs right Power
        telemetry.addData("Power ", power); //talks about power
        telemetry.addData("FullPower? ", fullPower); //tells if its fullpower
        telemetry.addData("No power? ", noPower); //tells if no power is true or false
        telemetry.addData("Distance ", rangeSensor.getDistance(DistanceUnit.CM)); //informs the user of the distance the range sensor is reading

        telemetry.update(); //updates telemetry
    }
    public void move(double leftPower, double rightPower) //move function for ez use
    {
        BLM.setPower(leftPower); //left motors are leftPower
        FLM.setPower(leftPower);
        FRM.setPower(rightPower); //right motors are rightPower
        BRM.setPower(rightPower);
    }

}
