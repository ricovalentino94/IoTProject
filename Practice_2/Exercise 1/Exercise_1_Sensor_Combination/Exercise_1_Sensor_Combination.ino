#include <NewPing.h>

int buttonPin = 7;
int ledPin = 13;
int pirPin = 8;
int buttonValue =0;
int pirValue=0;

#define TRIGGER_PIN  12  // Arduino pin tied to trigger pin on the ultrasonic sensor.
#define ECHO_PIN     11  // Arduino pin tied to echo pin on the ultrasonic sensor.
#define MAX_DISTANCE 200 // Maximum distance we want to ping for (in centimeters). Maximum sensor distance is rated at 400-500cm.

NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE); // NewPing setup of pins and maximum distance.


void setup() {
  // put your setup code here, to run once:
  pinMode(buttonPin, INPUT);
  pinMode(ledPin, OUTPUT);
  pinMode(pirPin, INPUT);
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  buttonValue = digitalRead(buttonPin);
  digitalWrite(ledPin,!buttonValue);
  if (!buttonValue){
    pirValue=digitalRead(pirPin);
    if (pirValue){
      unsigned int uS = sonar.ping(); // Send ping, get ping time in microseconds (uS).
      Serial.print("There is someone detected at ");
      Serial.print(uS / US_ROUNDTRIP_CM); // Convert ping time to distance in cm and print result (0 = outside set distance range)
      Serial.println("cm");
    }else{
      Serial.println("There is no person detected!");
    }
    delay(500);
  }
  
}
