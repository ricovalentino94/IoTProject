#include <Servo.h>
#include <NewPing.h>
int pos = 0;

#define servoPin     9
#define TRIGGER_PIN  12  // Arduino pin tied to trigger pin on the ultrasonic sensor.
#define ECHO_PIN     11  // Arduino pin tied to echo pin on the ultrasonic sensor.
#define MAX_DISTANCE 200 // Maximum distance we want to ping for (in centimeters). Maximum sensor distance is rated at 400-500cm.


Servo myservo;  // create servo object to control a servo
NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE); // NewPing setup of pins and maximum distance.

void setup() {
  // put your setup code here, to run once:
   myservo.attach(servoPin);  // attaches the servo on pin 9 to the servo object
    Serial.begin(9600);
   
}

void loop() {
  // put your main code here, to run repeatedly:
  for (pos = 0; pos <= 180; pos += 1) { // goes from 0 degrees to 180 degrees
    // in steps of 1 degree
    myservo.write(pos);              // tell servo to go to position in variable 'pos'
    delay(15);                       // waits 15ms for the servo to reach the position
    if (pos%30==0){
        unsigned int uS = sonar.ping(); // Send ping, get ping time in microseconds (uS).
        Serial.print("The distance at ");
        Serial.print(pos);
        Serial.print("degree is ");
        Serial.print(uS / US_ROUNDTRIP_CM); // Convert ping time to distance in cm and print result (0 = outside set distance range)
        Serial.println("cm");
    }
  }
  for (pos = 180; pos >= 0; pos -= 1) { // goes from 180 degrees to 0 degrees
    myservo.write(pos);              // tell servo to go to position in variable 'pos'
    delay(15);                       // waits 15ms for the servo to reach the position
    if (pos%30==0){
        unsigned int uS = sonar.ping(); // Send ping, get ping time in microseconds (uS).
        Serial.print("The distance at ");
        Serial.print(pos);
        Serial.print("degree is ");
        Serial.print(uS / US_ROUNDTRIP_CM); // Convert ping time to distance in cm and print result (0 = outside set distance range)
        Serial.println("cm");
    }
  }
}
