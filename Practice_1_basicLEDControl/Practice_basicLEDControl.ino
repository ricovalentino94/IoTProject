/*
  Blink
  Turns on an LED on for one second, then off for one second, repeatedly.

  Most Arduinos have an on-board LED you can control. On the Uno and
  Leonardo, it is attached to digital pin 13. If you're unsure what
  pin the on-board LED is connected to on your Arduino model, check
  the documentation at http://www.arduino.cc

  This example code is in the public domain.

  modified 8 May 2014
  by Scott Fitzgerald
 */
int i=0;
int delayDuration=0;
int iteration;


// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin 13 as an output.
  pinMode(13, OUTPUT);
}

// the loop function runs over and over again forever
void loop() {
  if(i%3==0){
    delayDuration=500;
    iteration=5;
  }else if (i%3==1){
    delayDuration=250;
    iteration=10;
  }else{
    delayDuration=125;
    iteration=20;
    i=-1;
  }

  for(int j=0;j<iteration;j++){
    digitalWrite(13, HIGH);   // turn the LED on (HIGH is the voltage level)
    delay(delayDuration);              // wait for a second
    digitalWrite(13, LOW);    // turn the LED off by making the voltage LOW
    delay(delayDuration);              // wait for a second
  }
 
  i++;
}
